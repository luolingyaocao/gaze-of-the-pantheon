package com.onceheart.gazeofthepantheon.menu;

import com.onceheart.gazeofthepantheon.registry.ModMenus;
import com.onceheart.gazeofthepantheon.util.EdictData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class EdictMenu extends AbstractContainerMenu {

    public static final int BLESSING_SLOTS = 54;
    public static final int DEED_SLOT_INDEX = 54;
    public static final int TOTAL_SLOTS = 55;

    /** 决策 UI 的格子布局参数 */
    private static final int GRID_X = 8;
    private static final int GRID_Y = 18;
    private static final int DEED_X = 184;
    private static final int DEED_Y = 18;

    private final Container container;
    private final Player player;

    /** 客户端构造 —— 由网络包调用 */
    public EdictMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory, new SimpleContainer(TOTAL_SLOTS));
    }

    /** 服务端构造 */
    public EdictMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, new SimpleContainer(TOTAL_SLOTS));
    }

    public EdictMenu(int containerId, Inventory playerInventory, Container container) {
        super(ModMenus.EDICT_MENU.get(), containerId);
        this.player = playerInventory.player;
        this.container = container;

        // ============ 54 格祝福注视槽（9×6） ============
        for (int row = 0; row < 6; row++) {
            for (int col = 0; col < 9; col++) {
                int index = row * 9 + col;
                int x = GRID_X + col * 18;
                int y = GRID_Y + row * 18;
                this.addSlot(new BlessingSlot(container, index, x, y));
            }
        }

        // ============ 成事在人特殊槽 ============
        this.addSlot(new DeedSlot(container, DEED_SLOT_INDEX, DEED_X, DEED_Y));

        // ============ 玩家背包 3×9 ============
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory,
                        col + row * 9 + 9,
                        GRID_X + col * 18,
                        140 + row * 18));
            }
        }

        // ============ 玩家快捷栏 1×9 ============
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col,
                    GRID_X + col * 18,
                    198));
        }

        // 服务端：从 EdictData 载入已有数据
        if (player instanceof ServerPlayer sp) {
            loadFromPlayerData(sp);
        }
    }

    /** 从玩家 persistentData 载入祝福注视和成事在人 */
    private void loadFromPlayerData(ServerPlayer sp) {
        var blessings = EdictData.getBlessings(sp);
        for (int i = 0; i < Math.min(blessings.size(), BLESSING_SLOTS); i++) {
            container.setItem(i, blessings.get(i));
        }
        if (EdictData.hasDeed(sp)) {
            container.setItem(DEED_SLOT_INDEX, new ItemStack(
                    com.onceheart.gazeofthepantheon.registry.ModItems.DEED.get()));
        }
    }

    /** 关闭界面时把内容写回玩家数据 */
    @Override
    public void removed(Player player) {
        super.removed(player);
        if (player instanceof ServerPlayer sp) {
            saveToPlayerData(sp);
        }
    }

    private void saveToPlayerData(ServerPlayer sp) {
        // 保存 54 格祝福注视
        java.util.List<ItemStack> blessings = new java.util.ArrayList<>();
        for (int i = 0; i < BLESSING_SLOTS; i++) {
            ItemStack s = container.getItem(i);
            if (!s.isEmpty()) blessings.add(s.copy());
        }
        EdictData.setBlessings(sp, blessings);

        // 保存成事在人状态
        ItemStack deedStack = container.getItem(DEED_SLOT_INDEX);
        EdictData.setHasDeed(sp, !deedStack.isEmpty());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        // 简化实现：禁止 shift 快速移动，避免数据同步问题
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    // ============ 自定义槽类 ============

    /** 祝福注视专用槽：只接受祝福版的注视饰品 */
    private static class BlessingSlot extends Slot {
        public BlessingSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return isBlessedGaze(stack);
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    }

    /** 成事在人专用槽：只接受成事在人 */
    private static class DeedSlot extends Slot {
        public DeedSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return stack.getItem() ==
                    com.onceheart.gazeofthepantheon.registry.ModItems.DEED.get();
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    }

    // ============ 判断是否祝福注视 ============

    public static boolean isBlessedGaze(ItemStack stack) {
        if (stack.isEmpty()) return false;
        var item = stack.getItem();
        if (item instanceof com.onceheart.gazeofthepantheon.item.ThanatosItem) {
            return com.onceheart.gazeofthepantheon.item.ThanatosItem.isBlessed(stack);
        }
        if (item instanceof com.onceheart.gazeofthepantheon.item.HygieiaItem) {
            return com.onceheart.gazeofthepantheon.item.HygieiaItem.isBlessed(stack);
        }
        if (item instanceof com.onceheart.gazeofthepantheon.item.AresItem) {
            return com.onceheart.gazeofthepantheon.item.AresItem.isBlessed(stack);
        }
        if (item instanceof com.onceheart.gazeofthepantheon.item.HermesItem) {
            return com.onceheart.gazeofthepantheon.item.HermesItem.isBlessed(stack);
        }
        if (item instanceof com.onceheart.gazeofthepantheon.item.XiheItem) {
            return com.onceheart.gazeofthepantheon.item.XiheItem.isBlessed(stack);
        }
        if (item instanceof com.onceheart.gazeofthepantheon.item.AchillesItem) {
            return com.onceheart.gazeofthepantheon.item.AchillesItem.isBlessed(stack);
        }
        return false;
    }
}