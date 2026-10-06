package com.freshfish.mathmaster.axiom;

import com.freshfish.mathmaster.entity.GeometryHolderSkillPool;
import com.freshfish.mathmaster.entity.GeometryHolderEntity;
import com.freshfish.mathmaster.init.ModEntities;
import net.minecraft.util.RandomSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.nbt.CompoundTag;

public final class GeometryHolderSkillPoolCheck {
    private static int checks;
    public static int run(ServerLevel level) throws Exception {
        checks=0;
        RandomSource random=RandomSource.create(13020010);
        var near=new GeometryHolderSkillPool(1,2);
        int repeats=0;boolean[] starts=new boolean[2];
        for(int i=0;i<10000;i++) {
            near.reset();int first=near.pick(random);starts[first-1]=true;
            if(near.pick(random)==first)repeats++;
        }
        require(starts[0] && starts[1],"either near skill can start a fight");
        require(repeats>800 && repeats<1400,"recent skill can repeat but unused near skill is strongly favored");
        var far=new GeometryHolderSkillPool(5,7,9);
        int[] counts=new int[3];int same=0,last=-1,seen=0;
        for(int i=0;i<10000;i++) {
            int selected=far.pick(random);int index=selected==5 ? 0 : selected==7 ? 1 : 2;
            counts[index]++;seen|=1<<index;if(selected==last)same++;last=selected;
        }
        require(seen==7 && java.util.Arrays.stream(counts).allMatch(n -> n>2800 && n<3800),
                "all remote skills remain available across repeated pool rounds");
        require(same>100 && same<1800,"random remote sequence allows repeats without fixed rotation");
        near.reset();random.setSeed(52);int independent=near.pick(random);
        for(int i=0;i<50;i++)far.pick(random);
        var used=GeometryHolderSkillPool.class.getDeclaredField("used");used.setAccessible(true);
        require(used.getInt(near)==(1<<(independent-1)),"far selections do not reset near history");
        near.reset();require(used.getInt(near)==0,"reload reset clears transient skill history");
        try(var fixture=new SkillCheckPlayer(level,"GeometryPoolCheck")) {
            var boss=ModEntities.GEOMETRY_HOLDER.get().create(level);
            try {
                boss.setPos(220,245,220);fixture.player.setPos(230,245,220);
                require(boss.getHealth()==200 && boss.getMaxHealth()==200 && boss.getArmorValue()==20,"fresh entity has finalized attributes");
                boss.setHealth(80);require(!boss.isSecondPhase(),"exact eighty health stays in stage one");
                boss.setHealth(79);require(boss.isSecondPhase(),"below eighty health enters stage two");
                boss.setHealth(200);boss.invulnerableTime=0;
                require(boss.hurt(boss.damageSources().playerAttack(fixture.player),4) && boss.getHealth()<200 && boss.getHealth()>196,
                        "twenty armor reduces real ordinary damage");
                float damaged=boss.getHealth();CompoundTag tag=new CompoundTag();boss.save(tag);
                var restored=ModEntities.GEOMETRY_HOLDER.get().create(level);restored.load(tag);
                try {require(restored.getMaxHealth()==200 && restored.getArmorValue()==20 && restored.getHealth()==damaged,
                        "save reload retains finalized attributes and current health");} finally {restored.discard();}
            } finally {boss.discard();}
        }
        return checks;
    }
    private static void require(boolean ok,String message) {if(!ok)throw new AssertionError("Geometry skill pool: "+message);checks++;}
}
