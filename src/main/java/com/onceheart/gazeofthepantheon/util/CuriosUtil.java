package com.onceheart.gazeofthepantheon.util;

import com.onceheart.gazeofthepantheon.item.AchillesItem;
import com.onceheart.gazeofthepantheon.item.AresItem;
import com.onceheart.gazeofthepantheon.item.HermesItem;
import com.onceheart.gazeofthepantheon.item.HygieiaItem;
import com.onceheart.gazeofthepantheon.item.ThanatosItem;
import com.onceheart.gazeofthepantheon.item.XiheItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;

import java.util.Optional;

public class CuriosUtil {

    public static final String GAZE = "gaze";
    public static final String DECISION = "decision";

    // ============ 通用处理器 ============

    public static Optional<ICurioStacksHandler> getGazeHandler(Player player) {
        var invOpt = CuriosApi.getCuriosInventory(player).resolve();
        if (invOpt.isEmpty()) return Optional.empty();
        return invOpt.get().getStacksHandler(GAZE);
    }

    public static Optional<ICurioStacksHandler> getDecisionHandler(Player player) {
        var invOpt = CuriosApi.getCuriosInventory(player).resolve();
        if (invOpt.isEmpty()) return Optional.empty();
        return invOpt.get().getStacksHandler(DECISION);
    }

    // ============ 决策栏位 ============

    public static boolean hasDecisionEquipped(Player player) {
        var handlerOpt = getDecisionHandler(player);
        if (handlerOpt.isEmpty()) return false;
        var stacks = handlerOpt.get().getStacks();
        for (int i = 0; i < stacks.getSlots(); i++) {
            ItemStack s = stacks.getStackInSlot(i);
            if (s.getItem() == com.onceheart.gazeofthepantheon.registry.ModItems.EDICT.get()) {
                return true;
            }
        }
        return false;
    }

    public static boolean tryEquipToDecision(Player player, ItemStack stack) {
        var handlerOpt = getDecisionHandler(player);
        if (handlerOpt.isEmpty()) return false;
        var stacks = handlerOpt.get().getStacks();
        for (int i = 0; i < stacks.getSlots(); i++) {
            if (stacks.getStackInSlot(i).isEmpty()) {
                stacks.setStackInSlot(i, stack);
                return true;
            }
        }
        return false;
    }

    // ============ 神系通用检查 ============

    public static boolean hasDeityEquipped(Player player, Class<? extends Item> deityClass) {
        var handlerOpt = getGazeHandler(player);
        if (handlerOpt.isEmpty()) return false;
        var stacks = handlerOpt.get().getStacks();
        for (int i = 0; i < stacks.getSlots(); i++) {
            ItemStack s = stacks.getStackInSlot(i);
            if (deityClass.isInstance(s.getItem())) {
                return true;
            }
        }
        return false;
    }

    // ============ 塔纳托斯 ============

    public static ItemStack findWrath(Player player) {
        return findWrath(player, com.onceheart.gazeofthepantheon.registry.ModItems.THANATOS.get());
    }

    public static ItemStack findKindness(Player player) {
        return findKindness(player, com.onceheart.gazeofthepantheon.registry.ModItems.THANATOS.get());
    }

    public static boolean destroyWrathInGaze(Player player) {
        var handlerOpt = getGazeHandler(player);
        if (handlerOpt.isEmpty()) return false;
        var stacks = handlerOpt.get().getStacks();
        for (int i = 0; i < stacks.getSlots(); i++) {
            ItemStack s = stacks.getStackInSlot(i);
            if (s.getItem() instanceof ThanatosItem && !ThanatosItem.isBlessed(s)) {
                stacks.setStackInSlot(i, ItemStack.EMPTY);
                return true;
            }
        }
        return false;
    }

    public static boolean convertWrathToKindness(Player player) {
        return convertToBlessed(player, com.onceheart.gazeofthepantheon.registry.ModItems.THANATOS.get());
    }

    // ============ 许癸厄亚 ============

    public static ItemStack findHygieiaWrath(Player player) {
        return findWrath(player, com.onceheart.gazeofthepantheon.registry.ModItems.HYGIEIA.get());
    }

    public static ItemStack findHygieiaKindness(Player player) {
        return findKindness(player, com.onceheart.gazeofthepantheon.registry.ModItems.HYGIEIA.get());
    }

    public static boolean convertHygieiaWrathToKindness(Player player) {
        return convertToBlessed(player, com.onceheart.gazeofthepantheon.registry.ModItems.HYGIEIA.get());
    }

    // ============ 阿瑞斯 ============

    public static ItemStack findAresWrath(Player player) {
        return findWrath(player, com.onceheart.gazeofthepantheon.registry.ModItems.ARES.get());
    }

    public static ItemStack findAresKindness(Player player) {
        return findKindness(player, com.onceheart.gazeofthepantheon.registry.ModItems.ARES.get());
    }

    public static boolean convertAresWrathToKindness(Player player) {
        return convertToBlessed(player, com.onceheart.gazeofthepantheon.registry.ModItems.ARES.get());
    }

    // ============ 赫尔墨斯 ============

    public static ItemStack findHermesWrath(Player player) {
        return findWrath(player, com.onceheart.gazeofthepantheon.registry.ModItems.HERMES.get());
    }

    public static ItemStack findHermesKindness(Player player) {
        return findKindness(player, com.onceheart.gazeofthepantheon.registry.ModItems.HERMES.get());
    }

    public static boolean convertHermesWrathToKindness(Player player) {
        return convertToBlessed(player, com.onceheart.gazeofthepantheon.registry.ModItems.HERMES.get());
    }

    // ============ 羲和 ============

    public static ItemStack findXiheWrath(Player player) {
        return findWrath(player, com.onceheart.gazeofthepantheon.registry.ModItems.XIHE.get());
    }

    public static ItemStack findXiheKindness(Player player) {
        return findKindness(player, com.onceheart.gazeofthepantheon.registry.ModItems.XIHE.get());
    }

    public static boolean convertXiheWrathToKindness(Player player) {
        return convertToBlessed(player, com.onceheart.gazeofthepantheon.registry.ModItems.XIHE.get());
    }

    // ============ 阿喀琉斯 ============

    public static ItemStack findAchillesWrath(Player player) {
        return findWrath(player, com.onceheart.gazeofthepantheon.registry.ModItems.ACHILLES.get());
    }

    public static ItemStack findAchillesKindness(Player player) {
        return findKindness(player, com.onceheart.gazeofthepantheon.registry.ModItems.ACHILLES.get());
    }

    public static boolean convertAchillesWrathToKindness(Player player) {
        return convertToBlessed(player, com.onceheart.gazeofthepantheon.registry.ModItems.ACHILLES.get());
    }

    // ============ 通用查找 ============

    /** 查找未祝福的注视：仅注视栏位 */
    public static ItemStack findWrath(Player player, Item item) {
        var handlerOpt = getGazeHandler(player);
        if (handlerOpt.isEmpty()) return ItemStack.EMPTY;
        var stacks = handlerOpt.get().getStacks();
        for (int i = 0; i < stacks.getSlots(); i++) {
            ItemStack s = stacks.getStackInSlot(i);
            if (s.getItem() != item) continue;
            if (isBlessed(s)) continue;
            return s;
        }
        return ItemStack.EMPTY;
    }

    /** 查找已祝福的注视：注视栏位 + 决策 UI 都查 */
    public static ItemStack findKindness(Player player, Item item) {
        // 先查注视栏位
        var handlerOpt = getGazeHandler(player);
        if (!handlerOpt.isEmpty()) {
            var stacks = handlerOpt.get().getStacks();
            for (int i = 0; i < stacks.getSlots(); i++) {
                ItemStack s = stacks.getStackInSlot(i);
                if (s.getItem() != item) continue;
                if (!isBlessed(s)) continue;
                return s;
            }
        }
        // 再查决策 UI
        if (player instanceof ServerPlayer sp) {
            for (ItemStack s : EdictData.getBlessings(sp)) {
                if (s.getItem() == item) return s;
            }
        }
        return ItemStack.EMPTY;
    }

    /** 判断是否已祝福 */
    private static boolean isBlessed(ItemStack s) {
        var item = s.getItem();
        if (item instanceof ThanatosItem) return ThanatosItem.isBlessed(s);
        if (item instanceof HygieiaItem) return HygieiaItem.isBlessed(s);
        if (item instanceof AresItem) return AresItem.isBlessed(s);
        if (item instanceof HermesItem) return HermesItem.isBlessed(s);
        if (item instanceof XiheItem) return XiheItem.isBlessed(s);
        if (item instanceof AchillesItem) return AchillesItem.isBlessed(s);
        return false;
    }

    // ============ 通用转化 / 删除 ============

    public static boolean convertToBlessed(Player player, Item item) {
        var handlerOpt = getGazeHandler(player);
        if (handlerOpt.isEmpty()) return false;
        var stacks = handlerOpt.get().getStacks();
        for (int i = 0; i < stacks.getSlots(); i++) {
            ItemStack s = stacks.getStackInSlot(i);
            if (s.getItem() != item) continue;
            if (s.getItem() instanceof ThanatosItem) {
                if (ThanatosItem.isBlessed(s)) return false;
                ThanatosItem.setBlessed(s, true);
                return true;
            } else if (s.getItem() instanceof HygieiaItem) {
                if (HygieiaItem.isBlessed(s)) return false;
                HygieiaItem.setBlessed(s, true);
                return true;
            } else if (s.getItem() instanceof AresItem) {
                if (AresItem.isBlessed(s)) return false;
                AresItem.setBlessed(s, true);
                return true;
            } else if (s.getItem() instanceof HermesItem) {
                if (HermesItem.isBlessed(s)) return false;
                HermesItem.setBlessed(s, true);
                return true;
            } else if (s.getItem() instanceof XiheItem) {
                if (XiheItem.isBlessed(s)) return false;
                XiheItem.setBlessed(s, true);
                return true;
            } else if (s.getItem() instanceof AchillesItem) {
                if (AchillesItem.isBlessed(s)) return false;
                AchillesItem.setBlessed(s, true);
                return true;
            }
        }
        return false;
    }

    public static boolean convertToWrath(Player player, Item item) {
        var handlerOpt = getGazeHandler(player);
        if (handlerOpt.isEmpty()) return false;
        var stacks = handlerOpt.get().getStacks();
        for (int i = 0; i < stacks.getSlots(); i++) {
            ItemStack s = stacks.getStackInSlot(i);
            if (s.getItem() != item) continue;
            if (s.getItem() instanceof ThanatosItem) {
                if (!ThanatosItem.isBlessed(s)) return false;
                ThanatosItem.setBlessed(s, false);
                return true;
            } else if (s.getItem() instanceof HygieiaItem) {
                if (!HygieiaItem.isBlessed(s)) return false;
                HygieiaItem.setBlessed(s, false);
                return true;
            } else if (s.getItem() instanceof AresItem) {
                if (!AresItem.isBlessed(s)) return false;
                AresItem.setBlessed(s, false);
                return true;
            } else if (s.getItem() instanceof HermesItem) {
                if (!HermesItem.isBlessed(s)) return false;
                HermesItem.setBlessed(s, false);
                return true;
            } else if (s.getItem() instanceof XiheItem) {
                if (!XiheItem.isBlessed(s)) return false;
                XiheItem.setBlessed(s, false);
                return true;
            } else if (s.getItem() instanceof AchillesItem) {
                if (!AchillesItem.isBlessed(s)) return false;
                AchillesItem.setBlessed(s, false);
                return true;
            }
        }
        return false;
    }

    public static boolean deleteGazeItem(Player player, Item item) {
        var handlerOpt = getGazeHandler(player);
        if (handlerOpt.isEmpty()) return false;
        var stacks = handlerOpt.get().getStacks();
        for (int i = 0; i < stacks.getSlots(); i++) {
            ItemStack s = stacks.getStackInSlot(i);
            if (s.getItem() == item) {
                stacks.setStackInSlot(i, ItemStack.EMPTY);
                return true;
            }
        }
        return false;
    }

    // ============ 通用装备 ============

    public static boolean tryEquipToGaze(Player player, ItemStack stack) {
        var handlerOpt = getGazeHandler(player);
        if (handlerOpt.isEmpty()) return false;
        var stacks = handlerOpt.get().getStacks();
        for (int i = 0; i < stacks.getSlots(); i++) {
            if (stacks.getStackInSlot(i).isEmpty()) {
                stacks.setStackInSlot(i, stack);
                return true;
            }
        }
        return false;
    }

    public static void removeFromGaze(Player player, ItemStack stack) {
        var handlerOpt = getGazeHandler(player);
        if (handlerOpt.isEmpty()) return;
        var stacks = handlerOpt.get().getStacks();
        for (int i = 0; i < stacks.getSlots(); i++) {
            if (stacks.getStackInSlot(i) == stack) {
                stacks.setStackInSlot(i, ItemStack.EMPTY);
                return;
            }
        }
    }
}