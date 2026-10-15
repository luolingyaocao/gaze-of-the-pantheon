package com.onceheart.gazeofthepantheon.registry;

import com.onceheart.gazeofthepantheon.GazeOfThePantheon;
import com.onceheart.gazeofthepantheon.block.PrimordialMatterBlock;
import com.onceheart.gazeofthepantheon.block.RarifiedEssenceBlock;
import com.onceheart.gazeofthepantheon.block.TurbidEssenceBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlocks {

    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, GazeOfThePantheon.MOD_ID);

    public static final RegistryObject<Block> RARIFIED_ESSENCE = BLOCKS.register("rarified_essence",
            () -> new RarifiedEssenceBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.SNOW)
                    .strength(3.0F, 6.0F)
                    .requiresCorrectToolForDrops()
                    .lightLevel(state -> 15)
                    .sound(SoundType.STONE)));

    public static final RegistryObject<Block> TURBID_ESSENCE = BLOCKS.register("turbid_essence",
            () -> new TurbidEssenceBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK)
                    .strength(3.0F, 6.0F)
                    .requiresCorrectToolForDrops()
                    .lightLevel(state -> 15)
                    .sound(SoundType.STONE)));

    public static final RegistryObject<Block> PRIMORDIAL_MATTER = BLOCKS.register("primordial_matter",
            () -> new PrimordialMatterBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(33.25F, 1200.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.ANCIENT_DEBRIS)));

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}