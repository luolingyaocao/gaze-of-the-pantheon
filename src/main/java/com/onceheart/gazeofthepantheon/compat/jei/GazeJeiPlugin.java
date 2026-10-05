package com.onceheart.gazeofthepantheon.compat.jei;

import com.onceheart.gazeofthepantheon.GazeOfThePantheon;
import com.onceheart.gazeofthepantheon.registry.ModItems;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

@JeiPlugin
public class GazeJeiPlugin implements IModPlugin {

    private static final ResourceLocation UID =
            new ResourceLocation(GazeOfThePantheon.MOD_ID, "jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(
                new FusangRitualCategory(registration.getJeiHelpers().getGuiHelper()),
                new HermesSandalsCategory(registration.getJeiHelpers().getGuiHelper()),
                new EdictDropCategory(registration.getJeiHelpers().getGuiHelper()),
                new DeedDropCategory(registration.getJeiHelpers().getGuiHelper()),
                new IvyRitualCategory(registration.getJeiHelpers().getGuiHelper())
        );
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(FusangRitualCategory.RECIPE_TYPE,
                List.of(new FusangRitualCategory.Recipe()));
        registration.addRecipes(HermesSandalsCategory.RECIPE_TYPE,
                List.of(new HermesSandalsCategory.Recipe()));
        registration.addRecipes(EdictDropCategory.RECIPE_TYPE,
                List.of(new EdictDropCategory.Recipe()));
        registration.addRecipes(DeedDropCategory.RECIPE_TYPE,
                List.of(new DeedDropCategory.Recipe()));
        registration.addRecipes(IvyRitualCategory.RECIPE_TYPE,
                List.of(new IvyRitualCategory.Recipe()));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(
                new ItemStack(ModItems.GOLDEN_CROW_FEATHER.get()),
                FusangRitualCategory.RECIPE_TYPE);
        registration.addRecipeCatalyst(
                new ItemStack(ModItems.HERMES.get()),
                HermesSandalsCategory.RECIPE_TYPE);
        registration.addRecipeCatalyst(
                new ItemStack(Items.WITHER_SKELETON_SKULL),
                EdictDropCategory.RECIPE_TYPE);
        registration.addRecipeCatalyst(
                new ItemStack(Items.DRAGON_EGG),
                DeedDropCategory.RECIPE_TYPE);
        registration.addRecipeCatalyst(
                new ItemStack(ModItems.BREW.get()),
                IvyRitualCategory.RECIPE_TYPE);
    }
}