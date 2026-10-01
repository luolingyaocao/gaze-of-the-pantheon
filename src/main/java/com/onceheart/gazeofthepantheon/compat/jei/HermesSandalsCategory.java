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

import java.util.List;

public class HermesSandalsCategory implements IRecipeCategory<HermesSandalsCategory.Recipe> {

    public static final RecipeType<Recipe> RECIPE_TYPE = RecipeType.create(
            GazeOfThePantheon.MOD_ID, "hermes_sandals", Recipe.class);

    private static final int WIDTH = 150;
    private static final int HEIGHT = 60;
    private static final int SLOT_Y = 24;

    private final IDrawable icon;
    private final IDrawable background;

    public HermesSandalsCategory(IGuiHelper guiHelper) {
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(ModItems.HERMES_SANDALS.get()));
        this.background = guiHelper.createBlankDrawable(WIDTH, HEIGHT);
    }

    @Override
    public RecipeType<Recipe> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.gazeofthepantheon.hermes_sandals");
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
        // 输入：石头、深板岩
        builder.addSlot(RecipeIngredientRole.INPUT, 20, SLOT_Y)
                .addItemStack(new ItemStack(Items.STONE));
        builder.addSlot(RecipeIngredientRole.INPUT, 44, SLOT_Y)
                .addItemStack(new ItemStack(Items.DEEPSLATE));
        // 输出：草鞋
        builder.addSlot(RecipeIngredientRole.OUTPUT, 110, SLOT_Y)
                .addItemStack(new ItemStack(ModItems.HERMES_SANDALS.get()));
    }

    @Override
    public void draw(Recipe recipe, IRecipeSlotsView slotsView, GuiGraphics g,
                     double mouseX, double mouseY) {
        var font = Minecraft.getInstance().font;

        // 顶部：条件说明
        Component line1 = Component.translatable("jei.gazeofthepantheon.hermes_sandals.condition");
        g.drawString(font, line1, 20, 4, 0x404040, false);

        // 底部：概率说明
        Component line2 = Component.translatable("jei.gazeofthepantheon.hermes_sandals.chance");
        g.drawString(font, line2, 20, HEIGHT - 12, 0x404040, false);

        // 中间：箭头
        g.drawString(font, "→", 76, SLOT_Y + 4, 0x404040, false);
    }

    @Override
    public List<Component> getTooltipStrings(Recipe recipe, IRecipeSlotsView slotsView,
                                             double mouseX, double mouseY) {
        // 悬停在条件文本上显示更详细的说明
        if (mouseY >= 0 && mouseY <= 14 && mouseX >= 20 && mouseX <= WIDTH - 20) {
            return List.of(Component.translatable("jei.gazeofthepantheon.hermes_sandals.condition.tooltip"));
        }
        return List.of();
    }

    /** 配方标记 —— 固定，不需要参数 */
    public static class Recipe {
    }
}