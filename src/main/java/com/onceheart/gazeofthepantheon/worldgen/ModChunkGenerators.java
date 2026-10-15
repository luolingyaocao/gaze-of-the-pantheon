package com.onceheart.gazeofthepantheon.worldgen;

import com.onceheart.gazeofthepantheon.GazeOfThePantheon;
import com.mojang.serialization.Codec;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModChunkGenerators {

    public static final DeferredRegister<Codec<? extends ChunkGenerator>> CHUNK_GENERATORS =
            DeferredRegister.create(Registries.CHUNK_GENERATOR, GazeOfThePantheon.MOD_ID);

    public static final RegistryObject<Codec<? extends ChunkGenerator>> PRIMORDIAL_HUMAN_REALM =
            CHUNK_GENERATORS.register("primordial_human_realm",
                    () -> PrimordialChunkGenerator.CODEC.codec());

    public static void register(IEventBus eventBus) {
        CHUNK_GENERATORS.register(eventBus);
    }
}