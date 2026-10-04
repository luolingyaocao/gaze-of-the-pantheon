package com.onceheart.gazeofthepantheon.compat.jei;

import com.onceheart.gazeofthepantheon.GazeOfThePantheon;
import com.onceheart.gazeofthepantheon.registry.ModItems;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class EdictDropCategory implements IRecipeCategory<EdictDropCategory.Recipe> {

    public static final RecipeType<Recipe> RECIPE_TYPE = RecipeType.create(
            GazeOfThePantheon.MOD_ID, "edict_drop", Recipe.class);

    private static final int WIDTH = 150;
    private static final int HEIGHT = 60;
    private static final int SLOT_Y = 24;

    private final IDrawable icon;
    private final IDrawable background;

    public EdictDropCategory(IGuiHelper guiHelper) {
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(Items.WITHER_SKELETON_SKULL));
        this.background = guiHelper.createBlankDrawable(WIDTH, HEIGHT);
    }

    @Override public RecipeType<Recipe> getRecipeType() { return RECIPE_TYPE; }
    @Override public Component getTitle() { return Component.translatable("jei.gazeofthepantheon.edict_drop"); }
    @Override public IDrawable getBackground() { return background; }
    @Override public IDrawable getIcon() { return icon; }
    @Override public int getWidth() { return WIDTH; }
    @Override public int getHeight() { return HEIGHT; }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, Recipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 20, SLOT_Y)
                .addItemStack(new ItemStack(Items.WITHER_SKELETON_SKULL));
        builder.addSlot(RecipeIngredientRole.INPUT, 44, SLOT_Y)
                .addItemStack(new ItemStack(Items.SOUL_SAND));
        builder.addSlot(RecipeIngredientRole.OUTPUT, 110, SLOT_Y)
                .addItemStack(new ItemStack(ModItems.EDICT.get()));
    }

    @Override
    public void draw(Recipe recipe, IRecipeSlotsView slotsView, GuiGraphics g,
                     double mouseX, double mouseY) {
        var font = Minecraft.getInstance().font;
        Component line1 = Component.translatable("jei.gazeofthepantheon.edict_drop.desc");
        g.drawString(font, line1, 6, 4, 0x404040, false);
        g.drawString(font, "→", 76, SLOT_Y + 4, 0x404040, false);
    }

    public static class Recipe {
    }
}