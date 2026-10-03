package com.onceheart.gazeofthepantheon.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.onceheart.gazeofthepantheon.menu.EdictMenu;
import com.onceheart.gazeofthepantheon.network.ModNetwork;
import com.onceheart.gazeofthepantheon.network.ToggleEdictEffectPacket;
import com.onceheart.gazeofthepantheon.util.EdictData;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class EdictScreen extends AbstractContainerScreen<EdictMenu> {

    /** 箱子界面背景（原版 generic_54） */
    private static final ResourceLocation TEXTURE =
            new ResourceLocation("minecraft", "textures/gui/container/generic_54.png");

    /** 按钮位置 */
    private static final int BUTTON_X = 6;
    private static final int BUTTON_Y_START = 18;
    private static final int BUTTON_W = 90;
    private static final int BUTTON_H = 18;
    private static final int BUTTON_SPACING = 22;

    /** 五种开关 */
    private static final int[] EFFECTS = {
            EdictData.EFFECT_IMMORTAL,
            EdictData.EFFECT_RUIN,
            EdictData.EFFECT_AUTHORITY,
            EdictData.EFFECT_PERISH,
            EdictData.EFFECT_SANCTION
    };

    /** 每种效果对应的语言键 */
    private static final String[] EFFECT_NAMES = {
            "edict.effect.immortal",
            "edict.effect.ruin",
            "edict.effect.authority",
            "edict.effect.perish",
            "edict.effect.sanction"
    };

    /** 缓存的开关状态，客户端本地维护，点击时同步给服务端 */
    private int localMask;

    public EdictScreen(EdictMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 220;
        this.imageHeight = 222;
        this.inventoryLabelY = this.imageHeight - 94;
        this.localMask = EdictData.EFFECT_ALL;
    }

    @Override
    protected void init() {
        super.init();
        // 请求一次当前开关状态（由 ToggleEdictEffectPacket 的空包触发服务端回发）
        // 这里直接同步当前缓存的掩码，服务端才是权威
        rebuildButtons();
    }

    /** 生成五个开关按钮 */
    private void rebuildButtons() {
        this.clearWidgets();
        for (int i = 0; i < EFFECTS.length; i++) {
            final int effect = EFFECTS[i];
            final String key = EFFECT_NAMES[i];
            int y = BUTTON_Y_START + i * BUTTON_SPACING;
            Button btn = Button.builder(
                    getButtonLabel(key, effect),
                    b -> onEffectToggle(effect)
            ).pos(this.leftPos + BUTTON_X, this.topPos + y).size(BUTTON_W, BUTTON_H).build();
            this.addRenderableWidget(btn);
        }
    }

    private Component getButtonLabel(String key, int effect) {
        boolean on = (localMask & effect) != 0;
        return Component.translatable("edict.effect." + key + (on ? ".on" : ".off"));
    }

    private void onEffectToggle(int effect) {
        // 客户端立即切换显示，网络包同步给服务端
        localMask ^= effect;
        ModNetwork.CHANNEL.sendToServer(new ToggleEdictEffectPacket(effect));
        rebuildButtons();
    }

    /** 从服务端同步开关状态 */
    public void updateMask(int mask) {
        this.localMask = mask;
        rebuildButtons();
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // 背景
        this.renderBackground(g);
        // 绘制容器贴图
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        g.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
        // 槽位和标签
        super.render(g, mouseX, mouseY, partialTick);
        // tooltip
        this.renderTooltip(g, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        // 贴图在 render 里已经画过，这里留空
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0x404040, false);
        g.drawString(this.font, this.playerInventoryTitle,
                this.inventoryLabelX, this.inventoryLabelY, 0x404040, false);
    }
}