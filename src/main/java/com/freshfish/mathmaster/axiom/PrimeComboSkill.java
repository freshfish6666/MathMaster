package com.freshfish.mathmaster.axiom;

import com.freshfish.mathmaster.init.ModAttachments;
import com.freshfish.mathmaster.init.ModItems;
import com.freshfish.mathmaster.network.PrimeComboStatePayload;
import com.freshfish.mathmaster.pollution.DigitalPollutionManager;
import java.util.IdentityHashMap;
import java.util.Map;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** Primary Player.attack only. A scoped hit lets mitigation/cancellation finish before charging. */
public final class PrimeComboSkill {
    private static final Map<ServerPlayer, Session> SESSIONS = new IdentityHashMap<>();
    private static final ThreadLocal<Hit> CURRENT_HIT = new ThreadLocal<>();

    private static final class Session {
        final ItemStack hand;
        final Item item;
        final int slot;
        final ResourceKey<Level> dimension;
        long lastHit;
        int prime;
        Session(ServerPlayer player) {
            hand = player.getMainHandItem(); item = hand.getItem(); slot = player.getInventory().selected;
            dimension = player.level().dimension(); lastHit = player.level().getGameTime();
        }
    }

    private static final class Hit {
        final ServerPlayer player;
        final Entity target;
        final DamageSource source;
        final Session session;
        final int prime, cost;
        final float scale;
        boolean added, effective;
        Hit(ServerPlayer player, Entity target, DamageSource source, Session session, int prime, int cost) {
            this.player=player; this.target=target; this.source=source; this.session=session;
            this.prime=prime; this.cost=cost;
            float strength=player.getAttackStrengthScale(.5F);
            scale=.2F+.8F*strength*strength;
        }
        boolean matches(Entity entity, DamageSource damage) { return target==entity && source==damage; }
    }

    public static int doubleChance(int level) { return 10 * (Math.clamp(level,2,5)-2); }
    public static int cooldownTicks(int level) { return (80-10*Math.clamp(level,2,5))*20; }

    public static void use(ServerPlayer player, int level) {
        if (!player.isAlive() || player.isSpectator()) return;
        if (validSession(player)!=null) { message(player,"active"); return; }
        var data=player.getData(ModAttachments.AXIOM_SKILL_DATA);
        if (data.getPrimeComboCooldownTicks()>0) {
            player.displayClientMessage(Component.translatable("message.mathmaster.axiom.prime_combo.cooldown",
                    (data.getPrimeComboCooldownTicks()+19)/20),true);
            return;
        }
        if (countCores(player)==0) { message(player,"no_core"); return; }
        if (cap(player.getMaxHealth())<2) { message(player,"no_prime"); return; }
        SESSIONS.put(player,new Session(player));
        data.setPrimeComboCooldownTicks(AxiomCooldownHandler.applyRingBypass(player,cooldownTicks(level)));
        sync(player,level);
        message(player,"started");
    }

    /** Called by the one primary hurt invocation in Player.attack; sweep damage never enters here. */
    public static boolean hurtPrimary(Player attacker, Entity target, DamageSource source, float amount) {
        if (!(attacker instanceof ServerPlayer player) || !(target instanceof LivingEntity)
                || attacker.isAutoSpinAttack()) return target.hurt(source,amount);
        Session session=validSession(player);
        if (session==null) return target.hurt(source,amount);
        int available=countCores(player), limit=cap(player.getMaxHealth());
        if (available==0 || limit<2) { stop(player); return target.hurt(source,amount); }
        int current=Math.min(session.prime,limit);
        int prime=current==0 ? 2 : advance(current,limit);
        int cost=1;
        int level=AxiomEffectManager.getEquippedLevel(player,AxiomDefinition.PRIME_COMBO);
        // First hit stays at two; there is no useless second charge at or one step below the cap.
        if (current>0 && prime<limit && available>=2 && doubleChance(level)>0
                && player.getRandom().nextInt(100)<doubleChance(level)) {
            prime=advance(prime,limit); cost=2;
        }
        Hit hit=new Hit(player,target,source,session,prime,cost);
        Hit previous=CURRENT_HIT.get(); CURRENT_HIT.set(hit);
        try {
            boolean hurt=target.hurt(source,amount);
            if (hit.added && hit.effective) {
                consumeCores(player,hit.cost);
                // Never resurrect a session interrupted by thorns/death/other nested damage.
                if (SESSIONS.get(player)==session) {
                    session.prime=hit.prime; session.lastHit=player.level().getGameTime();
                }
                DigitalPollutionManager.add(player,1);
                if (validSession(player)!=null) sync(player,level);
            }
            return hurt;
        } finally {
            if (previous==null) CURRENT_HIT.remove(); else CURRENT_HIT.set(previous);
        }
    }

    // Registered before ShannonEntropySkill at the same LOWEST priority, after LOW intellect scaling.
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public void onIncomingDamage(LivingIncomingDamageEvent event) {
        Hit hit=CURRENT_HIT.get();
        if (hit==null || hit.added || !hit.matches(event.getEntity(),event.getSource())
                || event.isCanceled() || event.getAmount()<=0 || !Float.isFinite(event.getAmount())) return;
        event.setAmount(event.getAmount()+hit.prime*hit.scale);
        hit.added=true;
    }

    @SubscribeEvent
    public void onDamage(LivingDamageEvent.Post event) {
        boolean effective=event.getNewDamage()>0 || event.getReduction(DamageContainer.Reduction.ABSORPTION)>0;
        if (!effective) return;
        Hit hit=CURRENT_HIT.get();
        if (hit!=null && hit.matches(event.getEntity(),event.getSource())) hit.effective=true;
        if (event.getEntity() instanceof ServerPlayer player) stop(player);
    }

    @SubscribeEvent public void onTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player && SESSIONS.containsKey(player)) validSession(player);
    }
    @SubscribeEvent public void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) stop(player);
    }
    @SubscribeEvent public void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) stop(player);
    }
    @SubscribeEvent public void onDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) stop(player);
    }
    @SubscribeEvent public void onServerStop(ServerStoppedEvent event) { SESSIONS.clear(); CURRENT_HIT.remove(); }

    private static Session validSession(ServerPlayer player) {
        Session session=SESSIONS.get(player);
        if (session==null) return null;
        int level=AxiomEffectManager.getEquippedLevel(player,AxiomDefinition.PRIME_COMBO);
        if (!player.isAlive() || player.isRemoved() || player.isSpectator() || level<2
                || player.level().dimension()!=session.dimension || player.getInventory().selected!=session.slot
                || player.getMainHandItem()!=session.hand
                || player.getMainHandItem().getItem()!=session.item
                || player.level().getGameTime()-session.lastHit>=level*20L) {
            stop(player); return null;
        }
        return session;
    }

    public static void stop(ServerPlayer player) {
        if (SESSIONS.remove(player)!=null && player.connection!=null)
            PacketDistributor.sendToPlayer(player,new PrimeComboStatePayload(0,0));
    }

    private static void sync(ServerPlayer player, int level) {
        Session session=SESSIONS.get(player);
        if (session!=null) PacketDistributor.sendToPlayer(player,new PrimeComboStatePayload(
                Math.min(session.prime==0?2:session.prime,cap(player.getMaxHealth())),
                (int)Math.max(0,level*20L-(player.level().getGameTime()-session.lastHit))));
    }

    private static int countCores(ServerPlayer player) {
        int count=0;
        for (int i=0;i<player.getInventory().getContainerSize();i++) {
            ItemStack stack=player.getInventory().getItem(i);
            if (stack.is(ModItems.PRIME_CORE.get())) count+=stack.getCount();
        }
        return count;
    }

    private static void consumeCores(ServerPlayer player, int count) {
        for (int i=0;i<player.getInventory().getContainerSize() && count>0;i++) {
            ItemStack stack=player.getInventory().getItem(i);
            if (!stack.is(ModItems.PRIME_CORE.get())) continue;
            int take=Math.min(count,stack.getCount()); stack.shrink(take); count-=take;
        }
        player.getInventory().setChanged();
    }

    static int cap(float health) {
        if (!Float.isFinite(health) || health<=2) return 0;
        int candidate=(int)Math.min(Integer.MAX_VALUE-1L,(long)Math.ceil(health)-1);
        if (candidate>2 && (candidate&1)==0) candidate--;
        while (candidate>=2 && !isPrime(candidate)) candidate-=candidate==3?1:2;
        return Math.max(0,candidate);
    }

    static int advance(int current,int cap) {
        if (current>=cap) return cap;
        int candidate=current+1;
        while (candidate<cap && !isPrime(candidate)) candidate++;
        return Math.min(candidate,cap);
    }
    private static boolean isPrime(int number) {
        if (number<2) return false;
        if (number%2==0) return number==2;
        for (int divisor=3;(long)divisor*divisor<=number;divisor+=2) if (number%divisor==0) return false;
        return true;
    }
    private static void message(ServerPlayer player,String suffix) {
        player.displayClientMessage(Component.translatable("message.mathmaster.axiom.prime_combo."+suffix),true);
    }
}
