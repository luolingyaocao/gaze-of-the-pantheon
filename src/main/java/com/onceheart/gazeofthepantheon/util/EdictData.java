package com.onceheart.gazeofthepantheon.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * 必行敕令的玩家数据工具类。
 * 所有数据都存储在玩家的 persistentData 里，跟随玩家而不是物品。
 */
public class EdictData {

    // ============ NBT 键名 ============

    /** 五种效果的开关状态，用位掩码存 */
    private static final String NBT_EFFECTS = "gazeofthepantheon_edict_effects";

    /** 内部的祝福注视列表 */
    private static final String NBT_BLESSINGS = "gazeofthepantheon_edict_blessings";

    /** 成事在人是否已放入 */
    private static final String NBT_HAS_DEED = "gazeofthepantheon_edict_has_deed";

    /** 是否已经获得过必行敕令（用于"最多一个"判定） */
    private static final String NBT_EDICT_OBTAINED = "gazeofthepantheon_edict_obtained";

    /** 是否已经获得过成事在人 */
    private static final String NBT_DEED_OBTAINED = "gazeofthepantheon_deed_obtained";

    /** 决策栏位是否解锁 */
    private static final String NBT_DECISION_UNLOCKED = "gazeofthepantheon_decision_unlocked";

    // ============ 效果位掩码 ============

    public static final int EFFECT_IMMORTAL = 1;      // 不朽
    public static final int EFFECT_RUIN = 1 << 1;     // 破败
    public static final int EFFECT_AUTHORITY = 1 << 2; // 天威
    public static final int EFFECT_PERISH = 1 << 3;   // 殁亡
    public static final int EFFECT_SANCTION = 1 << 4; // 制裁

    public static final int EFFECT_ALL = EFFECT_IMMORTAL | EFFECT_RUIN | EFFECT_AUTHORITY | EFFECT_PERISH | EFFECT_SANCTION;

    // ============ 效果开关 ============

    /** 读取玩家的效果位掩码，默认全开 */
    public static int getEffects(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        if (!data.contains(NBT_EFFECTS)) {
            return EFFECT_ALL;
        }
        return data.getInt(NBT_EFFECTS);
    }

    public static void setEffects(ServerPlayer player, int mask) {
        player.getPersistentData().putInt(NBT_EFFECTS, mask);
    }

    public static boolean isEffectOn(ServerPlayer player, int effect) {
        return (getEffects(player) & effect) != 0;
    }

    public static void toggleEffect(ServerPlayer player, int effect) {
        int current = getEffects(player);
        setEffects(player, current ^ effect);
    }

    // ============ 祝福注视列表 ============

    /** 读取内部的祝福注视列表（只读副本） */
    public static List<ItemStack> getBlessings(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        List<ItemStack> list = new ArrayList<>();
        if (!data.contains(NBT_BLESSINGS, Tag.TAG_LIST)) {
            return list;
        }
        ListTag listTag = data.getList(NBT_BLESSINGS, Tag.TAG_COMPOUND);
        for (int i = 0; i < listTag.size(); i++) {
            CompoundTag itemTag = listTag.getCompound(i);
            ItemStack stack = ItemStack.of(itemTag);
            if (!stack.isEmpty()) {
                list.add(stack);
            }
        }
        return list;
    }

    /** 保存祝福注视列表 */
    public static void setBlessings(ServerPlayer player, List<ItemStack> blessings) {
        ListTag listTag = new ListTag();
        for (ItemStack stack : blessings) {
            if (!stack.isEmpty()) {
                listTag.add(stack.save(new CompoundTag()));
            }
        }
        player.getPersistentData().put(NBT_BLESSINGS, listTag);
    }

    // ============ 成事在人 ============

    public static boolean hasDeed(ServerPlayer player) {
        return player.getPersistentData().getBoolean(NBT_HAS_DEED);
    }

    public static void setHasDeed(ServerPlayer player, boolean has) {
        player.getPersistentData().putBoolean(NBT_HAS_DEED, has);
    }

    /** 五种效果是否真正生效：需要成事在人已放入 */
    public static boolean isEdictActive(ServerPlayer player) {
        return hasDeed(player);
    }

    // ============ 获得标记 ============

    public static boolean hasObtainedEdict(ServerPlayer player) {
        return player.getPersistentData().getBoolean(NBT_EDICT_OBTAINED);
    }

    public static void markEdictObtained(ServerPlayer player) {
        player.getPersistentData().putBoolean(NBT_EDICT_OBTAINED, true);
    }

    public static boolean hasObtainedDeed(ServerPlayer player) {
        return player.getPersistentData().getBoolean(NBT_DEED_OBTAINED);
    }

    public static void markDeedObtained(ServerPlayer player) {
        player.getPersistentData().putBoolean(NBT_DEED_OBTAINED, true);
    }

    // ============ 决策栏位解锁 ============

    public static boolean isDecisionUnlocked(ServerPlayer player) {
        return player.getPersistentData().getBoolean(NBT_DECISION_UNLOCKED);
    }

    public static void setDecisionUnlocked(ServerPlayer player, boolean unlocked) {
        player.getPersistentData().putBoolean(NBT_DECISION_UNLOCKED, unlocked);
    }
}