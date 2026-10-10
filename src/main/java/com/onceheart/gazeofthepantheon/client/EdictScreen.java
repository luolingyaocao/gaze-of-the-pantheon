package com.onceheart.gazeofthepantheon.client;

import com.onceheart.gazeofthepantheon.menu.EdictMenu;
import com.onceheart.gazeofthepantheon.network.ModNetwork;
import com.onceheart.gazeofthepantheon.network.ToggleEdictEffectPacket;
import com.onceheart.gazeofthepantheon.util.EdictData;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class EdictScreen extends AbstractContainerScreen<EdictMenu> {

    private static final int BUTTON_X = 6;
    private static final int BUTTON_Y_START = 24;
    private static final int BUTTON_W = 54;
    private static final int BUTTON_H = 20;
    private static final int BUTTON_SPACING = 2;

    private static final int[] EFFECTS = {
            EdictData.EFFECT_IMMORTAL,
            EdictData.EFFECT_RUIN,
            EdictData.EFFECT_AUTHORITY,
            EdictData.EFFECT_PERISH,
            EdictData.EFFECT_SANCTION
    };

    private static final String[] EFFECT_KEYS = {
            "immortal", "ruin", "authority", "perish", "sanction"
    };

    private int localMask;
    private boolean lastShouldShow;

    public EdictScreen(EdictMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 290;
        this.imageHeight = 236;
        this.titleLabelX = 6;
        this.titleLabelY = 6;
        this.inventoryLabelX = EdictMenu.INVENTORY_X;
        this.inventoryLabelY = EdictMenu.INVENTORY_Y - 11;
        this.localMask = menu.getSyncedMask();
        this.lastShouldShow = shouldShowButtons();
    }

    @Override
    protected void init() {
        super.init();
        this.localMask = this.menu.getSyncedMask();
        this.lastShouldShow = shouldShowButtons();
        rebuildButtons();
    }

    /** 按钮显示条件：成事在人已放入 + 全部神系祝福都在格子里 */
    private boolean shouldShowButtons() {
        return hasDeedInSlot() && allBlessingsInSlots();
    }

    private boolean hasDeedInSlot() {
        Slot slot = this.menu.getSlot(EdictMenu.DEED_SLOT_INDEX);
        return !slot.getItem().isEmpty();
    }

    /** 检查 54 格中是否集齐全部神系的祝福 */
    private boolean allBlessingsInSlots() {
        java.util.Set<Class<?>> found = new java.util.HashSet<>();
        for (int i = 0; i < EdictMenu.BLESSING_SLOTS; i++) {
            ItemStack s = this.menu.getSlot(i).getItem();
            if (s.isEmpty()) continue;
            Class<?> deity = deityOf(s);
            if (deity != null) found.add(deity);
        }
        return found.size() >= EdictData.getAllBlessingItems().size();
    }

    private static Class<?> deityOf(ItemStack stack) {
        var item = stack.getItem();
        if (item instanceof com.onceheart.gazeofthepantheon.item.ThanatosItem)
            return com.onceheart.gazeofthepantheon.item.ThanatosItem.class;
        if (item instanceof com.onceheart.gazeofthepantheon.item.HygieiaItem)
            return com.onceheart.gazeofthepantheon.item.HygieiaItem.class;
        if (item instanceof com.onceheart.gazeofthepantheon.item.AresItem)
            return com.onceheart.gazeofthepantheon.item.AresItem.class;
        if (item instanceof com.onceheart.gazeofthepantheon.item.HermesItem)
            return com.onceheart.gazeofthepantheon.item.HermesItem.class;
        if (item instanceof com.onceheart.gazeofthepantheon.item.XiheItem)
            return com.onceheart.gazeofthepantheon.item.XiheItem.class;
        if (item instanceof com.onceheart.gazeofthepantheon.item.AchillesItem)
            return com.onceheart.gazeofthepantheon.item.AchillesItem.class;
        if (item instanceof com.onceheart.gazeofthepantheon.item.DionysusItem)
            return com.onceheart.gazeofthepantheon.item.DionysusItem.class;
        return null;
    }

    private void rebuildButtons() {
        this.clearWidgets();
        if (!shouldShowButtons()) return;

        for (int i = 0; i < EFFECTS.length; i++) {
            final int effect = EFFECTS[i];
            final String key = EFFECT_KEYS[i];
            int y = BUTTON_Y_START + i * (BUTTON_H + BUTTON_SPACING);
            Button btn = Button.builder(
                    label(key, effect),
                    b -> onEffectToggle(effect)
            ).pos(this.leftPos + BUTTON_X, this.topPos + y).size(BUTTON_W, BUTTON_H).build();
            this.addRenderableWidget(btn);
        }
    }

    private Component label(String key, int effect) {
        boolean on = (localMask & effect) != 0;
        return Component.translatable("edict.effect." + key + (on ? ".on" : ".off"));
    }

    private void onEffectToggle(int effect) {
        localMask ^= effect;
        ModNetwork.CHANNEL.sendToServer(new ToggleEdictEffectPacket(effect));
        rebuildButtons();
    }

    public void updateMask(int mask) {
        this.localMask = mask;
        rebuildButtons();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        boolean now = shouldShowButtons();
        if (now != lastShouldShow) {
            lastShouldShow = now;
            rebuildButtons();
        }
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        int w = this.imageWidth;
        int h = this.imageHeight;

        g.fill(x, y, x + w, y + h, 0xFFC6C6C6);
        g.renderOutline(x - 1, y - 1, w + 2, h + 2, 0xFF000000);
        g.fill(x, y, x + w - 1, y + 1, 0xFFFFFFFF);
        g.fill(x, y, x + 1, y + h - 1, 0xFFFFFFFF);
        g.fill(x + w - 1, y, x + w, y + h, 0xFF555555);
        g.fill(x, y + h - 1, x + w, y + h, 0xFF555555);

        for (Slot slot : this.menu.slots) {
            drawSlotBackground(g, x + slot.x, y + slot.y);
        }
    }

    private void drawSlotBackground(GuiGraphics g, int sx, int sy) {
        g.fill(sx - 1, sy - 1, sx + 17, sy + 17, 0xFF8B8B8B);
        g.fill(sx, sy, sx + 16, sy + 16, 0xFF373737);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g);
        super.render(g, mouseX, mouseY, partialTick);
        this.renderTooltip(g, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0x404040, false);
        g.drawString(this.font, this.playerInventoryTitle,
                this.inventoryLabelX, this.inventoryLabelY, 0x404040, false);
    }
}