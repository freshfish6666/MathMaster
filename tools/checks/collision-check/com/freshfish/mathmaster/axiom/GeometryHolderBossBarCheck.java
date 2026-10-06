package com.freshfish.mathmaster.axiom;

import com.freshfish.mathmaster.entity.GeometryHolderEntity;
import com.freshfish.mathmaster.init.ModEntities;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.BossEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;

public final class GeometryHolderBossBarCheck {
    private static int checks;
    public static int run(ServerLevel level) throws Exception {
        checks=0;
        try(var first=new SkillCheckPlayer(level,"GeometryBarOne");var second=new SkillCheckPlayer(level,"GeometryBarTwo")) {
            var boss=ModEntities.GEOMETRY_HOLDER.get().create(level);boss.setPos(220,245,220);boss.setNoAi(true);
            var field=GeometryHolderEntity.class.getDeclaredField("bossBar");field.setAccessible(true);
            var bar=(ServerBossEvent)field.get(boss);
            try {
                require(bar.getId().equals(boss.getUUID()) && bar.getPlayers().isEmpty(),"entity UUID identifies bar without extra network data");
                first.player.setPos(268,245,220);second.player.setPos(230,245,220);
                boss.startSeenByPlayer(first.player);boss.startSeenByPlayer(second.player);
                require(bar.getPlayers().size()==2 && bar.getProgress()==1,"tracked viewers share full bar within inclusive forty-eight range");
                require(!bar.shouldPlayBossMusic() && !bar.shouldDarkenScreen() && !bar.shouldCreateWorldFog(),"bar leaves music sky and fog untouched");
                first.player.setPos(268.01,245,220);boss.tick();
                require(!bar.getPlayers().contains(first.player) && bar.getPlayers().contains(second.player),"leaving range removes only that viewer");
                first.player.setPos(240,245,220);boss.tick();
                require(bar.getPlayers().size()==2,"still-tracked player reentering range regains bar");
                boss.setHealth(100);boss.tick();require(bar.getProgress()==.5F,"real health updates progress");
                boss.setCustomName(Component.literal("Geometry Test"));boss.tick();
                require(bar.getName().getString().equals("Geometry Test"),"custom names update display");
                boss.setHealth(80);boss.tick();require(bar.getColor()==BossEvent.BossBarColor.BLUE,"exact stage boundary stays blue");
                boss.setHealth(79);boss.tick();require(bar.getColor()==BossEvent.BossBarColor.RED,"second stage provides red vanilla fallback");
                boss.stopSeenByPlayer(first.player);require(bar.getPlayers().size()==1,"stop tracking removes viewer");
                boss.setHealth(0);boss.tick();require(!bar.isVisible() && bar.getPlayers().isEmpty(),"death hides bar and clears its viewers");
                boss.setHealth(200);boss.tick();boss.startSeenByPlayer(first.player);
                require(bar.isVisible() && bar.getPlayers().size()==2,"valid tracked living boss restores display");
                CompoundTag tag=new CompoundTag();boss.save(tag);
                var restored=ModEntities.GEOMETRY_HOLDER.get().create(level);restored.load(tag);
                try {
                    var restoredBar=(ServerBossEvent)field.get(restored);
                    require(restoredBar.getPlayers().isEmpty() && restoredBar.getId().equals(restored.getUUID()),"reload derives identity and keeps no stale viewers");
                } finally {restored.discard();}
                boss.discard();require(bar.getPlayers().isEmpty(),"discard unload clears all boss bars");
            } finally {boss.discard();}
        }
        return checks;
    }
    private static void require(boolean ok,String message) {if(!ok)throw new AssertionError("Geometry boss bar: "+message);checks++;}
}
