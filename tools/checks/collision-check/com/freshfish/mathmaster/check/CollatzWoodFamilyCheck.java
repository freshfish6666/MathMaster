package com.freshfish.mathmaster.check;

import com.freshfish.mathmaster.init.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.SlabType;
import java.util.ArrayList;

/** Resource-backed compatibility checks; never packaged with the mod. */
public final class CollatzWoodFamilyCheck {
    private static int checks;
    private static String prefix;
    public static int run(ServerLevel level) {
        checks = 0;
        for (String color : new String[]{"", "purple_", "blue_"}) {
            prefix = color;
            family(level);
        }
        return checks;
    }
    private static void family(ServerLevel level) {
        var log = new ItemStack(block("log"));
        var plank = new ItemStack(block("planks"));
        require(log.is(ItemTags.LOGS_THAT_BURN) && log.is(ItemTags.LOGS), "vanilla log tags");
        require(plank.is(ItemTags.PLANKS), "vanilla plank tag");
        require(log.getBurnTime(RecipeType.SMELTING)==300, "log fuel");
        require(plank.getBurnTime(RecipeType.SMELTING)==300, "plank fuel");
        var smelt=level.getRecipeManager().getRecipeFor(RecipeType.SMELTING,new SingleRecipeInput(log),level).orElseThrow();
        require(smelt.value().assemble(new SingleRecipeInput(log),level.registryAccess()).is(Items.CHARCOAL), "vanilla charcoal recipe");
        craft(level, "L", block("planks").asItem(),4);
        craft(level, "P", block("button").asItem(),1);
        craft(level, "PPP", block("slab").asItem(),6);
        craft(level, "P  /PP /PPP", block("stairs").asItem(),4);
        craft(level, "PSP/PSP", block("fence").asItem(),3);
        craft(level, "SPS/SPS", block("fence_gate").asItem(),1);
        craft(level, "PP/PP/PP", block("door").asItem(),3);
        craft(level, "PPP/PPP", block("trapdoor").asItem(),2);
        craft(level, "PP", block("pressure_plate").asItem(),1);
        craft(level, "PP/PP", Items.CRAFTING_TABLE,1);
        craft(level, "P/P", Items.STICK,4);
        craft(level, "PPP/P P/PPP", Items.CHEST,1);
        craft(level, "PPP/ S / S ", Items.WOODEN_PICKAXE,1);
        var pos=new BlockPos(115,240,115);
        var blocks=new Block[]{block("planks"),block("slab"),block("stairs"),
                block("fence"),block("fence_gate"),block("door"),
                block("trapdoor"),block("button"),block("pressure_plate")};
        for(var block:blocks) {
            var drops=Block.getDrops(block.defaultBlockState(),level,pos,null);
            require(drops.size()==1 && drops.getFirst().is(block.asItem()) && drops.getFirst().getCount()==1,"self loot "+block);
            require(new ItemStack(block).getBurnTime(RecipeType.SMELTING)>0,"wood family fuel "+block);
        }
        var slab=block("slab").defaultBlockState().setValue(SlabBlock.TYPE,SlabType.DOUBLE);
        var drops=Block.getDrops(slab,level,pos,null);
        require(drops.size()==1 && drops.getFirst().getCount()==2,"double slab drops two");
        require(Block.getDrops(block("door").defaultBlockState().setValue(DoorBlock.HALF,DoubleBlockHalf.UPPER),level,pos,null).isEmpty(),"upper door no duplicate loot");
        var fence=(FenceBlock)block("fence");
        require(fence.connectsTo(Blocks.OAK_FENCE.defaultBlockState(),false,Direction.NORTH),"connect vanilla wooden fence");
        require(!fence.connectsTo(Blocks.NETHER_BRICK_FENCE.defaultBlockState(),false,Direction.NORTH),"do not connect nether brick fence");
        require(fence.connectsTo(block("fence_gate").defaultBlockState(),false,Direction.EAST),"connect gate");
        require(block("log").defaultBlockState().getFlammability(level,pos,Direction.UP)==5,"log burns like wood");
        require(block("planks").defaultBlockState().getFlammability(level,pos,Direction.UP)==20,"plank burns like wood");
    }
    private static Block block(String suffix) {
        return net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(net.minecraft.resources.ResourceLocation.parse("mathmaster:"+prefix+"collatz_"+suffix));
    }
    private static void craft(ServerLevel level,String pattern,Item expected,int count) {
        String[] rows=pattern.split("/",-1);
        var stacks=new ArrayList<ItemStack>();
        for(String row:rows) for(char c:row.toCharArray()) stacks.add(switch(c) {
            case 'P' -> new ItemStack(block("planks"));
            case 'L' -> new ItemStack(block("log"));
            case 'S' -> new ItemStack(Items.STICK);
            default -> ItemStack.EMPTY;
        });
        var input=CraftingInput.of(rows[0].length(),rows.length,stacks);
        var recipe=level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING,input,level).orElseThrow();
        var result=recipe.value().assemble(input,level.registryAccess());
        require(result.is(expected) && result.getCount()==count,"actual crafting "+pattern+" -> "+expected);
    }
    private static void require(boolean ok,String message) {
        if(!ok) throw new AssertionError("Collatz wood: "+message);
        checks++;
    }
}
