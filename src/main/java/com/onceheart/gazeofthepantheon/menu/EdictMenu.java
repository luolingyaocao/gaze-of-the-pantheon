package com.onceheart.gazeofthepantheon.menu;

import com.onceheart.gazeofthepantheon.event.DivineSaveHandler;
import com.onceheart.gazeofthepantheon.item.AchillesItem;
import com.onceheart.gazeofthepantheon.item.AresItem;
import com.onceheart.gazeofthepantheon.item.HermesItem;
import com.onceheart.gazeofthepantheon.item.HygieiaItem;
import com.onceheart.gazeofthepantheon.item.ThanatosItem;
import com.onceheart.gazeofthepantheon.item.XiheItem;
import com.onceheart.gazeofthepantheon.registry.ModItems;
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
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

import java.util.ArrayList;
import java.util.List;

public class EdictMenu extends AbstractContainerMenu {

    public static final int BLESSING_SLOTS = 54;
    public static final int DEED_SLOT_INDEX = 54;
    public static final int TOTAL_SLOTS = 55;
    public static final int GAZE_SLOTS = 6;

    public static final int BLESSING_X = 66;
    public static final int BLESSING_Y = 24;
    public static final int DEED_X = 234;
    public static final int DEED_Y = 24;
    public static final int GAZE_X = 260;
    public static final int GAZE_Y = 24;
    public static final int INVENTORY_X = 66;
    public static final int INVENTORY_Y = 148;
    public static final int HOTBAR_Y = 206;

    private final Container container;
    private final Player player;

    private int syncedMask;
    private boolean syncedIsActive;

    /** 上次保存时的内容哈希，用于检测变化 */
    private int lastContentHash = -1;

    public EdictMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buf) {
        this(containerId, playerInventory, new SimpleContainer(TOTAL_SLOTS));
        this.syncedMask = buf.readInt();
        this.syncedIsActive = buf.readBoolean();
    }

    public EdictMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, new SimpleContainer(TOTAL_SLOTS));
        if (playerInventory.player instanceof ServerPlayer sp) {
            this.syncedMask = EdictData.getEffects(sp);
            this.syncedIsActive = EdictData.isEdictActive(sp);
        } else {
            this.syncedMask = EdictData.EFFECT_ALL;
            this.syncedIsActive = false;
        }
    }

    public EdictMenu(int containerId, Inventory playerInventory, Container container) {
        super(ModMenus.EDICT_MENU.get(), containerId);
        this.player = playerInventory.player;
        this.container = container;

        for (int row = 0; row < 6; row++) {
            for (int col = 0; col < 9; col++) {
                int index = row * 9 + col;
                int x = BLESSING_X + col * 18;
                int y = BLESSING_Y + row * 18;
                this.addSlot(new BlessingSlot(container, index, x, y));
            }
        }

        this.addSlot(new DeedSlot(container, DEED_SLOT_INDEX, DEED_X, DEED_Y));

        CuriosApi.getCuriosInventory(player).resolve().ifPresent(handler -> {
            handler.getStacksHandler("gaze").ifPresent(gazeHandler -> {
                IDynamicStackHandler gazeStacks = gazeHandler.getStacks();
                GazeContainerWrapper wrapper = new GazeContainerWrapper(gazeStacks, player);
                for (int i = 0; i < gazeStacks.getSlots(); i++) {
                    int y = GAZE_Y + i * 18;
                    this.addSlot(new GazeSlot(wrapper, i, GAZE_X, y));
                }
            });
        });

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory,
                        col + row * 9 + 9,
                        INVENTORY_X + col * 18,
                        INVENTORY_Y + row * 18));
            }
        }

        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col,
                    INVENTORY_X + col * 18,
                    HOTBAR_Y));
        }

        if (player instanceof ServerPlayer sp) {
            loadFromPlayerData(sp);
            lastContentHash = computeContentHash();
        }
    }

    public int getSyncedMask() {
        return syncedMask;
    }

    public boolean isSyncedActive() {
        return syncedIsActive;
    }

    public void setSyncedMask(int mask) {
        this.syncedMask = mask;
    }

    private void loadFromPlayerData(ServerPlayer sp) {
        var blessings = EdictData.getBlessings(sp);
        for (int i = 0; i < Math.min(blessings.size(), BLESSING_SLOTS); i++) {
            container.setItem(i, blessings.get(i));
        }
        if (EdictData.hasDeed(sp)) {
            container.setItem(DEED_SLOT_INDEX, new ItemStack(ModItems.DEED.get()));
        }
    }

    /** 实时检测内容变化并保存 */
    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (player instanceof ServerPlayer sp) {
            int hash = computeContentHash();
            if (hash != lastContentHash) {
                lastContentHash = hash;
                saveToPlayerData(sp);
            }
        }
    }

    private int computeContentHash() {
        int h = 1;
        for (int i = 0; i < BLESSING_SLOTS; i++) {
            ItemStack s = container.getItem(i);
            h = 31 * h + (s.isEmpty() ? 0 : s.getItem().hashCode() * 31 + s.getCount());
        }
        ItemStack deed = container.getItem(DEED_SLOT_INDEX);
        h = 31 * h + (deed.isEmpty() ? 0 : deed.getItem().hashCode() * 31 + deed.getCount());
        return h;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (player instanceof ServerPlayer sp) {
            saveToPlayerData(sp);
        }
    }

    private void saveToPlayerData(ServerPlayer sp) {
        List<ItemStack> blessings = new ArrayList<>();
        for (int i = 0; i < BLESSING_SLOTS; i++) {
            ItemStack s = container.getItem(i);
            if (!s.isEmpty()) blessings.add(s.copy());
        }
        EdictData.setBlessings(sp, blessings);

        ItemStack deedStack = container.getItem(DEED_SLOT_INDEX);
        EdictData.setHasDeed(sp, !deedStack.isEmpty());

        // 决策 UI 内容变化后刷新激活缓存
        DivineSaveHandler.refreshActiveCache(sp);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    // ============ 祝福注视槽 ============

    private static class BlessingSlot extends Slot {
        public BlessingSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            if (!isBlessedGaze(stack)) return false;
            Class<?> deity = deityClass(stack);
            if (deity == null) return false;
            for (int i = 0; i < container.getContainerSize(); i++) {
                if (i == this.index) continue;
                ItemStack other = container.getItem(i);
                if (other.isEmpty()) continue;
                if (deity.isInstance(other.getItem())) return false;
            }
            return true;
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    }

    // ============ 成事在人槽 ============

    private static class DeedSlot extends Slot {
        public DeedSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return stack.getItem() == ModItems.DEED.get();
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    }

    // ============ 注视槽 ============

    private static class GazeSlot extends Slot {
        private final GazeContainerWrapper wrapper;

        public GazeSlot(GazeContainerWrapper wrapper, int index, int x, int y) {
            super(wrapper, index, x, y);
            this.wrapper = wrapper;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            if (!isAnyGaze(stack)) return false;
            Class<?> deity = deityClass(stack);
            if (deity == null) return false;
            for (int i = 0; i < wrapper.getContainerSize(); i++) {
                if (i == this.getSlotIndex()) continue;
                ItemStack other = wrapper.getItem(i);
                if (other.isEmpty()) continue;
                if (deity.isInstance(other.getItem())) return false;
            }
            return true;
        }

        @Override
        public boolean mayPickup(Player player) {
            ItemStack stack = getItem();
            if (stack.isEmpty()) return true;
            if (stack.getItem() instanceof ICurioItem curio) {
                return curio.canUnequip(new SlotContext(
                        "gaze", player, getSlotIndex(), false, false), stack);
            }
            return true;
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    }

    // ============ 注视栏位容器包装 ============

    private static class GazeContainerWrapper implements Container {
        private final IDynamicStackHandler handler;
        private final Player player;

        public GazeContainerWrapper(IDynamicStackHandler handler, Player player) {
            this.handler = handler;
            this.player = player;
        }

        @Override
        public int getContainerSize() {
            return handler.getSlots();
        }

        @Override
        public boolean isEmpty() {
            for (int i = 0; i < handler.getSlots(); i++) {
                if (!handler.getStackInSlot(i).isEmpty()) return false;
            }
            return true;
        }

        @Override
        public ItemStack getItem(int slot) {
            return handler.getStackInSlot(slot);
        }

        @Override
        public ItemStack removeItem(int slot, int amount) {
            return handler.extractItem(slot, amount, false);
        }

        @Override
        public ItemStack removeItemNoUpdate(int slot) {
            ItemStack stack = handler.getStackInSlot(slot);
            handler.setStackInSlot(slot, ItemStack.EMPTY);
            return stack;
        }

        @Override
        public void setItem(int slot, ItemStack stack) {
            handler.setStackInSlot(slot, stack);
        }

        @Override
        public void setChanged() {
        }

        @Override
        public boolean stillValid(Player p) {
            return p == this.player;
        }

        @Override
        public void clearContent() {
            for (int i = 0; i < handler.getSlots(); i++) {
                handler.setStackInSlot(i, ItemStack.EMPTY);
            }
        }
    }

    // ============ 工具方法 ============

    public static boolean isBlessedGaze(ItemStack stack) {
        if (stack.isEmpty()) return false;
        var item = stack.getItem();
        if (item instanceof ThanatosItem) return ThanatosItem.isBlessed(stack);
        if (item instanceof HygieiaItem) return HygieiaItem.isBlessed(stack);
        if (item instanceof AresItem) return AresItem.isBlessed(stack);
        if (item instanceof HermesItem) return HermesItem.isBlessed(stack);
        if (item instanceof XiheItem) return XiheItem.isBlessed(stack);
        if (item instanceof AchillesItem) return AchillesItem.isBlessed(stack);
        return false;
    }

    public static boolean isAnyGaze(ItemStack stack) {
        if (stack.isEmpty()) return false;
        var item = stack.getItem();
        return item instanceof ThanatosItem
                || item instanceof HygieiaItem
                || item instanceof AresItem
                || item instanceof HermesItem
                || item instanceof XiheItem
                || item instanceof AchillesItem;
    }

    private static Class<?> deityClass(ItemStack stack) {
        if (stack.getItem() instanceof ThanatosItem) return ThanatosItem.class;
        if (stack.getItem() instanceof HygieiaItem) return HygieiaItem.class;
        if (stack.getItem() instanceof AresItem) return AresItem.class;
        if (stack.getItem() instanceof HermesItem) return HermesItem.class;
        if (stack.getItem() instanceof XiheItem) return XiheItem.class;
        if (stack.getItem() instanceof AchillesItem) return AchillesItem.class;
        return null;
    }
}