package com.onceheart.gazeofthepantheon.worldgen;

import com.onceheart.gazeofthepantheon.GazeOfThePantheon;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;

public class ModDimensions {

    public static final ResourceKey<Level> PRIMORDIAL_HUMAN_REALM = ResourceKey.create(
            Registries.DIMENSION,
            ResourceLocation.fromNamespaceAndPath(GazeOfThePantheon.MOD_ID, "primordial_human_realm"));

    public static final ResourceKey<LevelStem> PRIMORDIAL_HUMAN_REALM_STEM = ResourceKey.create(
            Registries.LEVEL_STEM,
            ResourceLocation.fromNamespaceAndPath(GazeOfThePantheon.MOD_ID, "primordial_human_realm"));

    public static final ResourceKey<DimensionType> PRIMORDIAL_HUMAN_REALM_TYPE = ResourceKey.create(
            Registries.DIMENSION_TYPE,
            ResourceLocation.fromNamespaceAndPath(GazeOfThePantheon.MOD_ID, "primordial_human_realm_type"));
}