package com.freshfish.mathmaster.axiom;

import com.freshfish.mathmaster.init.ModAttachments;
import com.freshfish.mathmaster.init.ModItems;
import com.freshfish.mathmaster.item.StudyNoteItem;
import com.freshfish.mathmaster.network.UseAxiomSkillPayload;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import top.theillusivec4.curios.api.CuriosApi;

/** Actual Player.attack (including the mixin), owner packets, mitigation and delayed payment. */
public final class PrimeComboSkillCheck {
    private static int checks;
    private static final PrimeComboSkill HANDLER=new PrimeComboSkill();

    public static int run(ServerLevel level) throws Exception {
        checks=0;
        require(AxiomDefinition.PRIME_COMBO.ordinal()==13 && AxiomDefinition.RETURN.ordinal()==12
                && AxiomDefinition.PRIME_COMBO.category()==AxiomDefinition.AxiomCategory.NUMBER_THEORY
                && AxiomDefinition.PRIME_COMBO.isActive()
                && AxiomDefinition.PRIME_COMBO.acceptsMaterial(new ItemStack(Items.IRON_INGOT)),"registration and legacy ordinal");
        for (int x=1;x<=6;x++) require(AxiomDefinition.PRIME_COMBO.acceptsNoteLevel(x)==(x>=2 && x<=5),"note range");
        for (int x=2;x<=5;x++) {
            require(PrimeComboSkill.doubleChance(x)==10*(x-2) && PrimeComboSkill.cooldownTicks(x)==(80-10*x)*20,"level values");
            require(StudyNoteItem.getAxiom(StudyNoteItem.createAxiomNote(AxiomDefinition.PRIME_COMBO,x)).orElseThrow()
                    ==AxiomDefinition.PRIME_COMBO,"note creation");
        }
        float[] health={0,2,2.1F,3,3.1F,19,20,30}; int[] limits={0,0,2,2,3,17,19,29};
        for (int i=0;i<health.length;i++) require(PrimeComboSkill.cap(health[i])==limits[i],"strict max-health prime cap");
        var legacy=new AxiomSkillData();var oldTag=new CompoundTag();oldTag.putInt("return_cooldown",9);
        legacy.deserializeNBT(level.registryAccess(),oldTag);
        require(legacy.getPrimeComboCooldownTicks()==0 && legacy.getReturnCooldownTicks()==9,"old cooldown loading");
        legacy.setPrimeComboCooldownTicks(17);var saved=legacy.serializeNBT(level.registryAccess());
        var restored=new AxiomSkillData();restored.deserializeNBT(level.registryAccess(),saved);
        require(restored.serializeNBT(level.registryAccess()).equals(saved),"cooldown save round trip");

        try (var fixture=new SkillCheckPlayer(level,"PrimeComboCheck");var shieldFixture=new SkillCheckPlayer(level,"PrimeShield")) {
            var player=fixture.player;var shieldPlayer=shieldFixture.player;
            var cow=EntityType.COW.create(level);var other=EntityType.COW.create(level);
            cow.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);other.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);
            cow.setNoAi(true);other.setNoAi(true);
            player.setPos(400,245,400);cow.setPos(400,245,402);other.setPos(401,245,402);
            player.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(6);
            player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.STICK));
            player.setOnGround(false);player.fallDistance=0;
            var immunity=ServerPlayer.class.getDeclaredField("spawnInvulnerableTime");immunity.setAccessible(true);
            immunity.setInt(player,0);immunity.setInt(shieldPlayer,0);
            try {
                equip(player,2,false);
                reset(cow);float base=attack(player,cow,100);
                start(player,2,64);require(fixture.primeComboPackets.getLast().prime()==2
                        && fixture.primeComboPackets.getLast().remainingTicks()==40,"activation owner snapshot");
                int[] sequence={2,3,5,7,11,13,17,19,19};
                for (int i=0;i<sequence.length;i++) {
                    LivingEntity target=(i&1)==0?cow:other;reset(target);
                    close(attack(player,target,100),base+sequence[i],"actual prime bonus across changing targets");
                    require(cores(player)==63-i && pollution(player)==i+1,"one core and one pollution per hit");
                }
                require(fixture.primeComboPackets.getLast().prime()==19,"cap persists");
                player.getAttribute(Attributes.MAX_HEALTH).setBaseValue(30);
                reset(cow);close(attack(player,cow,100),base+23,"increased max health opens higher primes");
                player.getAttribute(Attributes.MAX_HEALTH).setBaseValue(19);
                reset(cow);close(attack(player,cow,100),base+17,"reduced max health immediately clamps base bonus");
                player.getAttribute(Attributes.MAX_HEALTH).setBaseValue(20);
                var data=player.getData(ModAttachments.AXIOM_SKILL_DATA);
                int packets=fixture.primeComboPackets.size();PrimeComboSkill.use(player,2);
                require(fixture.primeComboPackets.size()==packets && data.getPrimeComboCooldownTicks()==1200,"active use cannot refresh or restart");

                start(player,2,64);reset(cow);float strength=strength(player,0);
                close(attack(player,cow,0),(base+2)*(.2F+.8F*strength*strength),"bonus shares vanilla attack cooldown scaling");
                require(cores(player)==63 && pollution(player)==1,"weak effective hit charges once");
                // Durability changes preserve the original stack, but changing slots or stack identity interrupts.
                player.getMainHandItem().setDamageValue(1);tick(player);
                require(active(player),"ordinary component/durability change does not interrupt");
                player.getInventory().selected=1;tick(player);require(!active(player),"slot switch interrupts");
                player.getInventory().selected=0;
                start(player,2,64);
                player.connection.handleSetCarriedItem(new net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket(1));
                player.connection.handleSetCarriedItem(new net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket(0));
                require(!active(player),"switch away and back in the same tick still interrupts");
                start(player,2,64);player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.STICK));tick(player);
                require(!active(player),"same item different stack interrupts");

                start(player,2,64);reset(cow);
                Consumer<LivingIncomingDamageEvent> cancel=event -> {if(event.getEntity()==cow)event.setCanceled(true);};
                NeoForge.EVENT_BUS.addListener(cancel);
                try { attack(player,cow,100); } finally { NeoForge.EVENT_BUS.unregister(cancel); }
                require(cores(player)==64 && pollution(player)==0 && active(player),"cancelled attack is free and does not interrupt");
                reset(cow);close(attack(player,cow,100),base+2,"cancelled attack leaves first bonus unchanged");
                cow.invulnerableTime=20; // Repeating the same low attack is rejected despite its preview bonus.
                var previous=cow.getHealth();attack(player,cow,0);
                require(cow.getHealth()==previous && cores(player)==63 && pollution(player)==1,"rejected invulnerability hit is free");
                reset(cow);Consumer<LivingDamageEvent.Pre> zero=event -> {if(event.getEntity()==cow)event.setNewDamage(0);};
                NeoForge.EVENT_BUS.addListener(zero);
                try { attack(player,cow,100); } finally { NeoForge.EVENT_BUS.unregister(zero); }
                require(cores(player)==63 && pollution(player)==1,"zero final damage is free");
                reset(cow);cow.getAttribute(Attributes.MAX_ABSORPTION).setBaseValue(20);cow.setAbsorptionAmount(20);
                float before=cow.getAbsorptionAmount();attack(player,cow,100);
                require(cow.getHealth()==1000 && cow.getAbsorptionAmount()<before && cores(player)==62
                        && pollution(player)==2,"absorption is a valid hit");cow.setAbsorptionAmount(0);

                start(player,2,64);reset(cow);cow.hurt(level.damageSources().playerAttack(player),1);
                require(cores(player)==64 && pollution(player)==0,"ordinary direct hurt outside primary attack cannot advance");
                reset(cow);cow.hurt(level.damageSources().arrow(EntityType.ARROW.create(level),player),1);
                require(cores(player)==64 && pollution(player)==0,"projectiles cannot advance");

                shieldPlayer.setPos(400,245,402);shieldPlayer.setYRot(180);shieldPlayer.setYHeadRot(180);
                shieldPlayer.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(Items.SHIELD));
                shieldPlayer.startUsingItem(InteractionHand.OFF_HAND);
                for(int i=0;i<6;i++)shieldPlayer.doTick();
                shieldPlayer.setPos(400,245,402);shieldPlayer.setYRot(180);shieldPlayer.setYHeadRot(180);
                require(shieldPlayer.isBlocking(),"shield fixture is actively blocking");
                shieldPlayer.invulnerableTime=0;float shieldHealth=shieldPlayer.getHealth();attack(player,shieldPlayer,100);
                require(shieldPlayer.getHealth()==shieldHealth && cores(player)==64 && pollution(player)==0,"full shield block is free");
                shieldPlayer.stopUsingItem();

                // Deterministic seeds cover both level V outcomes, and the first hit never draws a double.
                for (boolean twice:new boolean[]{false,true}) {
                    start(player,5,64);reset(cow);attack(player,cow,100);
                    require(cores(player)==63 && fixture.primeComboPackets.getLast().prime()==2,"level V first hit fixed at two");
                    player.getRandom().setSeed(seed(twice,30));reset(cow);
                    close(attack(player,cow,100),base+(twice?5:3),"double advances twice in one hit");
                    require(cores(player)==(twice?61:62) && pollution(player)==2,"double core cost but one pollution");
                }
                start(player,5,2);reset(cow);attack(player,cow,100);player.getRandom().setSeed(seed(true,30));reset(cow);
                close(attack(player,cow,100),base+3,"one remaining core falls back to single advancement");
                require(cores(player)==0 && pollution(player)==2,"fallback charges one");
                reset(cow);close(attack(player,cow,100),base,"no core returns to ordinary attack");require(!active(player),"no-core attack ends combo");

                // Real entropy handler runs after the additive term, and armor handles the combined damage once.
                for (int seed=0;seed<30;seed++) {
                    start(player,2,64);equip(player,2,true);player.getRandom().setSeed(seed);reset(cow);
                    float factor=ShannonEntropySkill.multiplier(5,true,RandomSource.create(seed).nextInt(100));
                    close(attack(player,cow,100),(base+2)*factor,"entropy affects original and prime damage together");
                    require(cores(player)==63 && pollution(player)==1,"entropy does not alter payment");
                }
                start(player,2,64);reset(cow);cow.getAttribute(Attributes.ARMOR).setBaseValue(20);
                float expected=net.minecraft.world.damagesource.CombatRules.getDamageAfterAbsorb(cow,base+2,
                        level.damageSources().playerAttack(player),20,0);
                close(attack(player,cow,100),expected,"armor mitigates combined damage once");cow.getAttribute(Attributes.ARMOR).setBaseValue(0);

                // Sweep damage is ordinary damage, never a second prime payment or progression.
                start(player,2,64);player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.IRON_SWORD));
                PrimeComboSkill.stop(player);data.clearCooldowns();PrimeComboSkill.use(player,2);
                player.setOnGround(true);other.setPos(400.8,245,402);level.addFreshEntity(cow);level.addFreshEntity(other);
                reset(cow);reset(other);attack(player,cow,100);
                require(other.getHealth()<1000 && cores(player)==63 && pollution(player)==1
                        && fixture.primeComboPackets.getLast().prime()==2,"sweep only charges and advances main target");
                player.setOnGround(false);player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.STICK));

                for (int x=2;x<=5;x++) {
                    start(player,x,64);require(data.getPrimeComboCooldownTicks()==PrimeComboSkill.cooldownTicks(x),"activation cooldown");
                    elapse(player,x*20-1);tick(player);require(active(player),"idle before boundary");
                    elapse(player,1);tick(player);require(!active(player),"idle exact boundary");
                }
                start(player,2,64);equip(player,0,false);tick(player);require(!active(player),"unequip interrupts");
                start(player,2,64);
                Consumer<LivingIncomingDamageEvent> cancelInjury=event -> {if(event.getEntity()==player)event.setCanceled(true);};
                NeoForge.EVENT_BUS.addListener(cancelInjury);
                try {player.invulnerableTime=0;player.hurt(level.damageSources().fall(),1);}
                finally {NeoForge.EVENT_BUS.unregister(cancelInjury);}
                require(active(player),"cancelled injury preserves combo");
                start(player,2,64);player.invulnerableTime=0;player.hurt(level.damageSources().fall(),1);
                require(!active(player),"environmental injury interrupts");
                start(player,2,64);player.getAttribute(Attributes.MAX_ABSORPTION).setBaseValue(20);player.setAbsorptionAmount(20);
                player.invulnerableTime=0;player.hurt(level.damageSources().fall(),1);
                require(!active(player),"absorbed injury interrupts");player.setAbsorptionAmount(0);
                start(player,2,64);reset(cow);
                Consumer<LivingDamageEvent.Post> thorns=event -> {
                    if(event.getEntity()==cow) {player.invulnerableTime=0;player.hurt(level.damageSources().thorns(cow),1);}
                };
                NeoForge.EVENT_BUS.addListener(thorns);
                try {attack(player,cow,100);} finally {NeoForge.EVENT_BUS.unregister(thorns);}
                require(!active(player) && cores(player)==63 && pollution(player)==1,
                        "nested retaliation ends state while charging successful outgoing hit exactly once");
                start(player,2,64);
                player.getData(ModAttachments.AXIOM_SKILL_DATA).clearCooldowns();
                player.getData(ModAttachments.AXIOM_SKILL_DATA).setPrimeComboCooldownTicks(2);
                NeoForge.EVENT_BUS.post(new PlayerTickEvent.Post(player));
                require(player.getData(ModAttachments.AXIOM_SKILL_DATA).getPrimeComboCooldownTicks()==1,"shared tick decrements once");
                CuriosApi.getCuriosInventory(player).orElseThrow().setEquippedCurio(
                        com.freshfish.mathmaster.item.EquippedMathematicalRing.SLOT_ID,0,new ItemStack(ModItems.MATHEMATICAL_RING.get()));
                PrimeComboSkill.stop(player);
                var use=UseAxiomSkillPayload.class.getDeclaredMethod("useSelectedSkill",ServerPlayer.class);use.setAccessible(true);use.invoke(null,player);
                require(active(player) && player.getData(ModAttachments.AXIOM_SKILL_DATA).getPrimeComboCooldownTicks()==0,
                        "real selected skill entry honors ring bypass");
                reset(cow);attack(player,cow,100);
                require(cores(player)==63 && pollution(player)==1,"ring does not waive core or pollution costs");
                CuriosApi.getCuriosInventory(player).orElseThrow().setEquippedCurio(
                        com.freshfish.mathmaster.item.EquippedMathematicalRing.SLOT_ID,0,ItemStack.EMPTY);
                start(player,2,64);
                for(int i=0;i<20;i++) {elapse(player,35);reset(cow);attack(player,cow,100);require(active(player),"continual hits have no total-duration limit");}
                start(player,2,64);HANDLER.onDimensionChange(new PlayerEvent.PlayerChangedDimensionEvent(player,
                        net.minecraft.world.level.Level.OVERWORLD,net.minecraft.world.level.Level.NETHER));
                require(!active(player),"dimension event cleans up");
                start(player,2,64);HANDLER.onLogout(new PlayerEvent.PlayerLoggedOutEvent(player));require(!active(player),"logout cleans up");
                start(player,2,64);player.getData(ModAttachments.DIGITAL_POLLUTION).deserializeNBT(level.registryAccess(),value(99));
                boolean keep=level.getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_KEEPINVENTORY);
                level.getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_KEEPINVENTORY).set(true,level.getServer());
                try {
                    reset(cow);attack(player,cow,100);
                    require(!player.isAlive() && !active(player) && cores(player)==63
                            && fixture.primeComboPackets.getLast().prime()==0,"lethal pollution charges once without resurrecting state or HUD");
                } finally { level.getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_KEEPINVENTORY).set(keep,level.getServer()); }
            } finally { PrimeComboSkill.stop(player);cow.discard();other.discard(); }
        }
        return checks;
    }

    private static void start(ServerPlayer player,int level,int cores) {
        PrimeComboSkill.stop(player);player.getData(ModAttachments.AXIOM_SKILL_DATA).clearCooldowns();
        player.getData(ModAttachments.DIGITAL_POLLUTION).reset();equip(player,level,false);
        for(int i=1;i<player.getInventory().getContainerSize();i++)player.getInventory().setItem(i,ItemStack.EMPTY);
        player.getInventory().setItem(9,new ItemStack(ModItems.PRIME_CORE.get(),cores));
        PrimeComboSkill.use(player,level);
    }
    private static void equip(ServerPlayer player,int grade,boolean entropy) {
        ItemStack stack=ItemStack.EMPTY;
        if(grade>0) {
            stack=new ItemStack(ModItems.AXIOM_CASE.get());var notes=new java.util.ArrayList<ItemStack>();
            notes.add(StudyNoteItem.createAxiomNote(AxiomDefinition.PRIME_COMBO,grade));
            if(entropy)notes.add(StudyNoteItem.createAxiomNote(AxiomDefinition.SHANNON_ENTROPY,5));
            stack.set(DataComponents.CONTAINER,ItemContainerContents.fromItems(notes));
        }
        CuriosApi.getCuriosInventory(player).orElseThrow().setEquippedCurio(EquippedAxiomCase.SLOT_ID,0,stack);
    }
    private static void reset(LivingEntity target) { target.invulnerableTime=0;target.setHealth(target.getMaxHealth());target.setAbsorptionAmount(0); }
    private static float attack(ServerPlayer player,LivingEntity target,int cooldown) throws Exception {
        strength(player,cooldown);float old=target.getHealth();player.attack(target);return old-target.getHealth();
    }
    private static float strength(ServerPlayer player,int ticks) throws Exception {
        var field=LivingEntity.class.getDeclaredField("attackStrengthTicker");field.setAccessible(true);field.setInt(player,ticks);
        return player.getAttackStrengthScale(.5F);
    }
    private static int cores(ServerPlayer player) { int total=0;for(int i=0;i<player.getInventory().getContainerSize();i++)
        if(player.getInventory().getItem(i).is(ModItems.PRIME_CORE.get()))total+=player.getInventory().getItem(i).getCount();return total; }
    private static int pollution(ServerPlayer player) { return player.getData(ModAttachments.DIGITAL_POLLUTION).getValue(); }
    private static boolean active(ServerPlayer player) throws Exception { return sessions().containsKey(player); }
    private static java.util.Map<?,?> sessions() throws Exception {
        var field=PrimeComboSkill.class.getDeclaredField("SESSIONS");field.setAccessible(true);return (java.util.Map<?,?>)field.get(null);
    }
    private static void elapse(ServerPlayer player,int ticks) throws Exception {
        Object session=sessions().get(player);var field=session.getClass().getDeclaredField("lastHit");field.setAccessible(true);
        field.setLong(session,field.getLong(session)-ticks);
    }
    private static void tick(ServerPlayer player) { HANDLER.onTick(new PlayerTickEvent.Post(player)); }
    private static long seed(boolean twice,int chance) { for(long seed=0;;seed++)if((RandomSource.create(seed).nextInt(100)<chance)==twice)return seed; }
    private static CompoundTag value(int pollution) { var tag=new CompoundTag();tag.putInt("value",pollution);return tag; }
    private static void close(float actual,float expected,String message) { require(Math.abs(actual-expected)<.002F,message+" actual="+actual+" expected="+expected); }
    private static void require(boolean ok,String message) { if(!ok)throw new AssertionError("Prime combo: "+message);checks++; }
}
