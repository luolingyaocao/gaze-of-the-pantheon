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

public class DeedDropCategory implements IRecipeCategory<DeedDropCategory.Recipe> {

    public static final RecipeType<Recipe> RECIPE_TYPE = RecipeType.create(
            GazeOfThePantheon.MOD_ID, "deed_drop", Recipe.class);

    private static final int WIDTH = 180;
    private static final int HEIGHT = 80;

    private final IDrawable icon;
    private final IDrawable background;

    public DeedDropCategory(IGuiHelper guiHelper) {
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(ModItems.DEED.get()));
        this.background = guiHelper.createBlankDrawable(WIDTH, HEIGHT);
    }

    @Override public RecipeType<Recipe> getRecipeType() { return RECIPE_TYPE; }
    @Override public Component getTitle() { return Component.translatable("jei.gazeofthepantheon.deed_drop"); }
    @Override public IDrawable getBackground() { return background; }
    @Override public IDrawable getIcon() { return icon; }
    @Override public int getWidth() { return WIDTH; }
    @Override public int getHeight() { return HEIGHT; }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, Recipe recipe, IFocusGroup focuses) {
        // 输入：必行敕令 + 末影龙（用龙蛋代替图标）
        builder.addSlot(RecipeIngredientRole.INPUT, 10, 30)
                .addItemStack(new ItemStack(ModItems.EDICT.get()));
        builder.addSlot(RecipeIngredientRole.INPUT, 32, 30)
                .addItemStack(new ItemStack(Items.DRAGON_EGG));
        // 输出：成事在人
        builder.addSlot(RecipeIngredientRole.OUTPUT, 145, 30)
                .addItemStack(new ItemStack(ModItems.DEED.get()));
    }

    @Override
    public void draw(Recipe recipe, IRecipeSlotsView slotsView, GuiGraphics g,
                     double mouseX, double mouseY) {
        var font = Minecraft.getInstance().font;
        Component line1 = Component.translatable("jei.gazeofthepantheon.deed_drop.line1");
        Component line2 = Component.translatable("jei.gazeofthepantheon.deed_drop.line2");
        g.drawString(font, line1, 6, 4, 0x404040, false);
        g.drawString(font, line2, 6, 15, 0x404040, false);
        g.drawString(font, "→", 118, 34, 0x404040, false);
    }

    public static class Recipe {
    }
}