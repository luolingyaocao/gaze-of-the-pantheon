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

public class FusangRitualCategory implements IRecipeCategory<FusangRitualCategory.Recipe> {

    public static final RecipeType<Recipe> RECIPE_TYPE = RecipeType.create(
            GazeOfThePantheon.MOD_ID, "fusang_ritual", Recipe.class);

    private static final int CELL = 14;
    private static final int GRID = 7;
    private static final int GRID_SIZE = CELL * GRID;
    private static final int PADDING = 8;
    private static final int WIDTH = GRID_SIZE + PADDING * 2;
    private static final int SLOT_AREA = 30;
    private static final int HEIGHT = GRID_SIZE + PADDING + SLOT_AREA;

    private final IDrawable icon;

    public FusangRitualCategory(IGuiHelper guiHelper) {
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(Items.GOLD_BLOCK));
    }

    @Override
    public RecipeType<Recipe> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.gazeofthepantheon.fusang_ritual");
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
        int y = GRID_SIZE + PADDING + 6;
        builder.addSlot(RecipeIngredientRole.INPUT, PADDING, y)
                .addItemStack(new ItemStack(ModItems.GOLDEN_CROW_FEATHER.get()));
        builder.addSlot(RecipeIngredientRole.OUTPUT, PADDING + 60, y)
                .addItemStack(new ItemStack(ModItems.FUSANG_DEW.get()));
    }

    @Override
    public void draw(Recipe recipe, IRecipeSlotsView slotsView, GuiGraphics g,
                     double mouseX, double mouseY) {
        int originX = PADDING;
        int originY = 4;

        // 画 7×7 俯视图
        for (int gx = 0; gx < GRID; gx++) {
            for (int gz = 0; gz < GRID; gz++) {
                int worldX = gx - 3;
                int worldZ = gz - 3;
                int px = originX + gx * CELL;
                int py = originY + gz * CELL;

                int color = blockColor(worldX, worldZ);
                if (color == 0) continue;

                g.fill(px, py, px + CELL - 1, py + CELL - 1, color);
                g.renderOutline(px, py, CELL - 1, CELL - 1, 0xFF000000);
            }
        }

        // 悬停高亮 + tooltip
        if (mouseX >= originX && mouseX < originX + GRID_SIZE
                && mouseY >= originY && mouseY < originY + GRID_SIZE) {
            int gx = (int) ((mouseX - originX) / CELL);
            int gz = (int) ((mouseY - originY) / CELL);
            int worldX = gx - 3;
            int worldZ = gz - 3;
            int px = originX + gx * CELL;
            int py = originY + gz * CELL;
            g.renderOutline(px, py, CELL - 1, CELL - 1, 0xFFFFFFFF);

            Component tip = blockTooltip(worldX, worldZ);
            if (tip != null) {
                g.renderTooltip(Minecraft.getInstance().font, tip,
                        (int) mouseX, (int) mouseY);
            }
        }
    }

    /** 每个水平位置上显示什么方块的颜色 */
    private int blockColor(int x, int z) {
        // 四角柱（铁块 + 顶端荧石）
        if ((x == -3 || x == 3) && (z == -3 || z == 3)) return 0xFFD8D8D8;
        // 钻石块
        if (Math.abs(x) == 2 && Math.abs(z) == 2) return 0xFF4AEDD9;
        if (Math.abs(x) == 2 && z == 0) return 0xFF4AEDD9;
        if (x == 0 && Math.abs(z) == 2) return 0xFF4AEDD9;
        // 金块
        if (x == 0 && z == 0) return 0xFFFFD700;
        // 泥土环（上方种树苗）
        if (x >= -1 && x <= 1 && z >= -1 && z <= 1) return 0xFF8B4513;
        // 石英底
        if (x >= -2 && x <= 2 && z >= -2 && z <= 2) return 0xFFFFF4E3;
        return 0;
    }

    /** 悬停提示 */
    private Component blockTooltip(int x, int z) {
        if ((x == -3 || x == 3) && (z == -3 || z == 3))
            return Component.translatable("jei.gazeofthepantheon.block.iron_pillar");
        if ((Math.abs(x) == 2 && Math.abs(z) == 2)
                || (Math.abs(x) == 2 && z == 0)
                || (x == 0 && Math.abs(z) == 2))
            return Component.translatable("jei.gazeofthepantheon.block.diamond");
        if (x == 0 && z == 0)
            return Component.translatable("jei.gazeofthepantheon.block.gold");
        if (x >= -1 && x <= 1 && z >= -1 && z <= 1)
            return Component.translatable("jei.gazeofthepantheon.block.dirt_sapling");
        if (x >= -2 && x <= 2 && z >= -2 && z <= 2)
            return Component.translatable("jei.gazeofthepantheon.block.quartz");
        return null;
    }

    /** 配方标记 —— 结构固定，不需要参数 */
    public static class Recipe {
    }
}