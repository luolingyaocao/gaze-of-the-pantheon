package com.onceheart.gazeofthepantheon.worldgen;

import com.onceheart.gazeofthepantheon.GazeOfThePantheon;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModFeatures {

    public static final DeferredRegister<Feature<?>> FEATURES =
            DeferredRegister.create(ForgeRegistries.FEATURES, GazeOfThePantheon.MOD_ID);

    public static final RegistryObject<Feature<NoneFeatureConfiguration>> OLYMPUS_MONS =
            FEATURES.register("olympus_mons", () -> new OlympusMonsFeature(NoneFeatureConfiguration.CODEC));

    public static void register(IEventBus eventBus) {
        FEATURES.register(eventBus);
    }
}