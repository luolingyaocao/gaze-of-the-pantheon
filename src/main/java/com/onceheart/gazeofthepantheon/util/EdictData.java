package com.onceheart.gazeofthepantheon.util;

import com.onceheart.gazeofthepantheon.registry.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 必行敕令的玩家数据工具类。
 * 所有数据都存储在玩家的 persistentData 的 PlayerPersisted 子节点里，
 * 跟随玩家跨维度、跨 respawn 保留。
 */
public class EdictData {

    /** Forge 的持久化子节点 key。写在根下会在 respawn 时丢失，必须写在 PlayerPersisted 里。 */
    private static final String PERSISTED_NBT_TAG = "PlayerPersisted";

    private static final String NBT_EFFECTS = "gazeofthepantheon_edict_effects";
    private static final String NBT_BLESSINGS = "gazeofthepantheon_edict_blessings";
    private static final String NBT_HAS_DEED = "gazeofthepantheon_edict_has_deed";
    private static final String NBT_EDICT_OBTAINED = "gazeofthepantheon_edict_obtained";
    private static final String NBT_DEED_OBTAINED = "gazeofthepantheon_deed_obtained";
    private static final String NBT_DECISION_UNLOCKED = "gazeofthepantheon_decision_unlocked";

    /** 不朽无敌缓存：不朽此刻是否生效（供超热方法 getHealth/getMaxHealth 零开销读取） */
    private static final String NBT_IMMORTAL_CACHE = "gazeofthepantheon_immortal_cache";
    /** 不朽开启时冻结的最大血量 */
    private static final String NBT_IMMORTAL_MAX = "gazeofthepantheon_immortal_max";

    public static final int EFFECT_IMMORTAL = 1;
    public static final int EFFECT_RUIN = 1 << 1;
    public static final int EFFECT_AUTHORITY = 1 << 2;
    public static final int EFFECT_PERISH = 1 << 3;
    public static final int EFFECT_SANCTION = 1 << 4;

    public static final int EFFECT_ALL = EFFECT_IMMORTAL | EFFECT_RUIN | EFFECT_AUTHORITY | EFFECT_PERISH | EFFECT_SANCTION;

    /** 取玩家持久化数据子节点，确保节点存在。所有 NBT 读写都走这里。 */
    private static CompoundTag data(ServerPlayer player) {
        CompoundTag root = player.getPersistentData();
        if (!root.contains(PERSISTED_NBT_TAG, Tag.TAG_COMPOUND)) {
            root.put(PERSISTED_NBT_TAG, new CompoundTag());
        }
        return root.getCompound(PERSISTED_NBT_TAG);
    }

    // ============ 效果开关 ============

    public static int getEffects(ServerPlayer player) {
        CompoundTag d = data(player);
        if (!d.contains(NBT_EFFECTS)) {
            return EFFECT_ALL;
        }
        return d.getInt(NBT_EFFECTS);
    }

    public static void setEffects(ServerPlayer player, int mask) {
        data(player).putInt(NBT_EFFECTS, mask);
    }

    public static boolean isEffectOn(ServerPlayer player, int effect) {
        return (getEffects(player) & effect) != 0;
    }

    public static void toggleEffect(ServerPlayer player, int effect) {
        int current = getEffects(player);
        setEffects(player, current ^ effect);
    }

    // ============ 不朽缓存（热路径） ============

    public static boolean isImmortalNow(ServerPlayer player) {
        return data(player).getBoolean(NBT_IMMORTAL_CACHE);
    }

    public static float getImmortalMax(ServerPlayer player) {
        CompoundTag d = data(player);
        if (!d.contains(NBT_IMMORTAL_MAX)) return 20.0F;
        float v = d.getFloat(NBT_IMMORTAL_MAX);
        return (v > 0.0F && !Float.isNaN(v)) ? v : 20.0F;
    }

    public static void refreshImmortalCache(ServerPlayer player, float realMaxHealth) {
        CompoundTag d = data(player);

        boolean prev = d.getBoolean(NBT_IMMORTAL_CACHE);
        boolean active = isEdictActive(player);
        boolean immortalNow = active && isEffectOn(player, EFFECT_IMMORTAL);

        d.putBoolean(NBT_IMMORTAL_CACHE, immortalNow);

        if (immortalNow && !prev) {
            float max = (realMaxHealth > 0.0F && !Float.isNaN(realMaxHealth))
                    ? realMaxHealth : 20.0F;
            d.putFloat(NBT_IMMORTAL_MAX, max);
        }
    }

    // ============ 祝福注视列表 ============

    public static List<ItemStack> getBlessings(ServerPlayer player) {
        CompoundTag d = data(player);
        List<ItemStack> list = new ArrayList<>();
        if (!d.contains(NBT_BLESSINGS, Tag.TAG_LIST)) {
            return list;
        }
        ListTag listTag = d.getList(NBT_BLESSINGS, Tag.TAG_COMPOUND);
        for (int i = 0; i < listTag.size(); i++) {
            CompoundTag itemTag = listTag.getCompound(i);
            ItemStack stack = ItemStack.of(itemTag);
            if (!stack.isEmpty()) {
                list.add(stack);
            }
        }
        return list;
    }

    public static void setBlessings(ServerPlayer player, List<ItemStack> blessings) {
        ListTag listTag = new ListTag();
        for (ItemStack stack : blessings) {
            if (!stack.isEmpty()) {
                listTag.add(stack.save(new CompoundTag()));
            }
        }
        data(player).put(NBT_BLESSINGS, listTag);
    }

    // ============ 成事在人 ============

    public static boolean hasDeed(ServerPlayer player) {
        return data(player).getBoolean(NBT_HAS_DEED);
    }

    public static void setHasDeed(ServerPlayer player, boolean has) {
        data(player).putBoolean(NBT_HAS_DEED, has);
    }

    // ============ 祝福是否集齐 ============

    public static boolean isBlessingsComplete(ServerPlayer player) {
        Set<Item> current = new HashSet<>();
        for (ItemStack s : getBlessings(player)) {
            current.add(s.getItem());
        }
        return current.containsAll(getAllBlessingItems());
    }

    /** 全部祝福注视的物品列表。以后加新神在这里追加即可。 */
    public static Set<Item> getAllBlessingItems() {
        Set<Item> set = new HashSet<>();
        set.add(ModItems.THANATOS.get());
        set.add(ModItems.HYGIEIA.get());
        set.add(ModItems.ARES.get());
        set.add(ModItems.HERMES.get());
        set.add(ModItems.XIHE.get());
        set.add(ModItems.ACHILLES.get());
        set.add(ModItems.DIONYSUS.get());
        return set;
    }

    // ============ 必行敕令是否真正激活 ============

    public static boolean isEdictActive(ServerPlayer player) {
        return hasDeed(player) && isBlessingsComplete(player);
    }

    // ============ 获得标记（保留，暂未使用） ============

    public static boolean hasObtainedEdict(ServerPlayer player) {
        return data(player).getBoolean(NBT_EDICT_OBTAINED);
    }

    public static void markEdictObtained(ServerPlayer player) {
        data(player).putBoolean(NBT_EDICT_OBTAINED, true);
    }

    public static boolean hasObtainedDeed(ServerPlayer player) {
        return data(player).getBoolean(NBT_DEED_OBTAINED);
    }

    public static void markDeedObtained(ServerPlayer player) {
        data(player).putBoolean(NBT_DEED_OBTAINED, true);
    }

    // ============ 决策栏位解锁 ============

    public static boolean isDecisionUnlocked(ServerPlayer player) {
        return data(player).getBoolean(NBT_DECISION_UNLOCKED);
    }

    public static void setDecisionUnlocked(ServerPlayer player, boolean unlocked) {
        data(player).putBoolean(NBT_DECISION_UNLOCKED, unlocked);
    }
}