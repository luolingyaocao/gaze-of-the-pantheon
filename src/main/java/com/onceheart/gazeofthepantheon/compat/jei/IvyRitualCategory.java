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

public class IvyRitualCategory implements IRecipeCategory<IvyRitualCategory.Recipe> {

    public static final RecipeType<Recipe> RECIPE_TYPE = RecipeType.create(
            GazeOfThePantheon.MOD_ID, "ivy_ritual", Recipe.class);

    private static final int PADDING = 8;
    private static final int WIDTH = 160;
    private static final int HEIGHT = 70;
    private static final int SLOT_AREA = 30;

    private final IDrawable icon;
    private final IDrawable background;

    public IvyRitualCategory(IGuiHelper guiHelper) {
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(Items.VINE));
        this.background = guiHelper.createBlankDrawable(WIDTH, HEIGHT);
    }

    @Override
    public RecipeType<Recipe> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.gazeofthepantheon.ivy_ritual");
    }

    @Override
    public IDrawable getBackground() {
        return background;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public int getWidth() {
        return WIDTH;
    }

    @Override
    public int getHeight() {
        return HEIGHT;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, Recipe recipe, IFocusGroup focuses) {
        int y = HEIGHT - SLOT_AREA - 4;
        builder.addSlot(RecipeIngredientRole.INPUT, PADDING, y)
                .addItemStack(new ItemStack(ModItems.BREW.get()));
        builder.addSlot(RecipeIngredientRole.CATALYST, PADDING + 40, y)
                .addItemStack(new ItemStack(Items.VINE));
        builder.addSlot(RecipeIngredientRole.OUTPUT, PADDING + 90, y)
                .addItemStack(new ItemStack(ModItems.IVY_CROWN.get()));
    }

    @Override
    public void draw(Recipe recipe, IRecipeSlotsView slotsView, GuiGraphics g,
                     double mouseX, double mouseY) {
        var font = Minecraft.getInstance().font;
        g.drawString(font,
                Component.translatable("jei.gazeofthepantheon.ivy_ritual.condition"),
                8, 6, 0xFF404040, false);
        g.drawString(font,
                Component.translatable("jei.gazeofthepantheon.ivy_ritual.action"),
                8, 18, 0xFF404040, false);
    }

    /** 配方标记 —— 结构固定，不需要参数 */
    public static class Recipe {
    }
}