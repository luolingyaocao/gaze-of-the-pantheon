package com.onceheart.gazeofthepantheon.event;

import com.onceheart.gazeofthepantheon.util.EdictData;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 集中处理所有"免死"逻辑。
 *
 * 覆盖：
 * - 位置记录（玩家踩地时记下精确坐标与维度）
 * - 虚空传送（掉出世界底部时传回最后站立位置；无记录则回当前维度出生点）
 * - 状态恢复（满血、清火、重置受伤状态、清除全部药水）
 * - 善意的效果（黑暗 I + 缓慢 V）
 * - 不朽的效果（抗性 V + 力量 X + 瞬间治疗 X）
 *
 * 所有 NBT 都写在 PlayerPersisted 子节点，跨维度、跨 respawn 保留。
 */
public class DivineSaveHandler {

    /** Forge 的持久化子节点 key。写在根下会在 respawn 时丢失，必须写在 PlayerPersisted 里。 */
    private static final String PERSISTED_NBT_TAG = "PlayerPersisted";

    /** 激活状态缓存，解决"死亡瞬间状态被破坏"的问题 */
    public static final String NBT_ACTIVE_CACHE = "gazeofthepantheon_edict_active_cache";

    /** 最后站立位置记录 */
    private static final String NBT_LAST_DIM = "gazeofthepantheon_last_ground_dim";
    private static final String NBT_LAST_X = "gazeofthepantheon_last_ground_x";
    private static final String NBT_LAST_Y = "gazeofthepantheon_last_ground_y";
    private static final String NBT_LAST_Z = "gazeofthepantheon_last_ground_z";

    /** 记录格式版本，防止旧 NBT 污染 */
    private static final String NBT_LAST_VER = "gazeofthepantheon_last_ground_ver";
    private static final int LAST_VER = 4;

    /** 虚空判定：低于世界底部多少格算掉虚空 */
    private static final int VOID_MARGIN = 10;

    /** 传送时抬高的格数，让玩家自由落体落地 */
    private static final double TELEPORT_LIFT = 2.0D;

    /** 取玩家持久化数据子节点，确保节点存在。所有 NBT 读写都走这里。 */
    private static CompoundTag persisted(ServerPlayer player) {
        CompoundTag root = player.getPersistentData();
        if (!root.contains(PERSISTED_NBT_TAG, Tag.TAG_COMPOUND)) {
            root.put(PERSISTED_NBT_TAG, new CompoundTag());
        }
        return root.getCompound(PERSISTED_NBT_TAG);
    }

    // ============ 位置记录 ============

    /** 玩家踩地时记录精确坐标与维度。由 PlayerTick 每 tick 调用。 */
    public static void recordGroundPosition(ServerPlayer player) {
        if (!player.onGround()) return;

        CompoundTag data = persisted(player);
        data.putInt(NBT_LAST_VER, LAST_VER);
        data.putString(NBT_LAST_DIM, player.level().dimension().location().toString());
        data.putDouble(NBT_LAST_X, player.getX());
        data.putDouble(NBT_LAST_Y, player.getY());
        data.putDouble(NBT_LAST_Z, player.getZ());
    }

    private static boolean hasGroundRecord(ServerPlayer player) {
        CompoundTag data = persisted(player);
        if (data.getInt(NBT_LAST_VER) != LAST_VER) return false;
        return data.contains(NBT_LAST_DIM)
                && data.contains(NBT_LAST_X)
                && data.contains(NBT_LAST_Y)
                && data.contains(NBT_LAST_Z);
    }

    // ============ 判断 ============

    /** 不朽此刻是否生效：有缓存标记（上一 tick 敕令是激活的）且不朽开关打开 */
    public static boolean isImmortalActive(ServerPlayer player) {
        boolean cached = persisted(player).getBoolean(NBT_ACTIVE_CACHE);
        if (!cached) return false;
        return EdictData.isEffectOn(player, EdictData.EFFECT_IMMORTAL);
    }

    /**
     * 不朽无敌此刻是否生效——**热路径专用，零开销**。
     * 只读一个 NBT boolean，不查 Curios、不查 effect mask。
     * 由 refreshActiveCache 在敕令状态变化时刷新。
     */
    public static boolean isImmortalNow(ServerPlayer player) {
        return EdictData.isImmortalNow(player);
    }

    /** 善意此刻是否生效：佩戴了善意且必行敕令未激活 */
    public static boolean isKindnessActive(ServerPlayer player) {
        if (EdictData.isEdictActive(player)) return false;
        return !com.onceheart.gazeofthepantheon.util.CuriosUtil
                .findKindness(player).isEmpty();
    }

    /**
     * 刷新激活缓存。在决策 UI 内容变化、敕令装备/卸下、登录、重生时调用。
     *
     * 同时刷新不朽无敌缓存（immortal_cache / immortal_max），
     * 用于 getHealth / getMaxHealth 的超热路径读取。
     */
    public static void refreshActiveCache(ServerPlayer player) {
        boolean active = EdictData.isEdictActive(player);
        persisted(player).putBoolean(NBT_ACTIVE_CACHE, active);

        // 从属性系统读取真实最大血量（不能用 getMaxHealth，会被 mod 污染）
        float realMax = 20.0F;
        try {
            var attr = player.getAttribute(Attributes.MAX_HEALTH);
            if (attr != null) {
                float v = (float) attr.getValue();
                if (v > 0.0F && !Float.isNaN(v)) realMax = v;
            }
        } catch (Throwable ignored) {
        }

        EdictData.refreshImmortalCache(player, realMax);
    }

    // ============ 虚空判断 ============

    public static boolean isInVoid(ServerPlayer player) {
        return player.getY() < player.level().getMinBuildHeight() - VOID_MARGIN;
    }

    // ============ 应用救赎 ============

    /** 善意的救赎 */
    public static void applyKindnessSave(ServerPlayer player) {
        teleportFromVoid(player);
        restoreState(player);

        player.addEffect(new MobEffectInstance(
                MobEffects.DARKNESS, 60, 0, false, false));
        player.addEffect(new MobEffectInstance(
                MobEffects.MOVEMENT_SLOWDOWN, 60, 4, false, false));

        player.sendSystemMessage(Component.translatable(
                "message.gazeofthepantheon.thanatos_kindness.triggered"));
    }

    /** 不朽的救赎 */
    public static void applyImmortalSave(ServerPlayer player) {
        teleportFromVoid(player);
        restoreState(player);

        player.addEffect(new MobEffectInstance(
                MobEffects.DAMAGE_RESISTANCE, 100, 4, false, false));
        player.addEffect(new MobEffectInstance(
                MobEffects.DAMAGE_BOOST, 200, 9, false, false));
        player.addEffect(new MobEffectInstance(
                MobEffects.HEAL, 300, 9, false, false));

        player.sendSystemMessage(Component.translatable(
                "message.gazeofthepantheon.edict.immortal"));
    }

    // ============ 虚空传送 ============

    /**
     * 虚空传送。
     * 优先传回记录的最后站立位置（含维度），抬高 TELEPORT_LIFT 格让玩家自由落体；
     * 无记录或记录维度已不存在时，退回当前维度出生点。
     */
    private static void teleportFromVoid(ServerPlayer player) {
        if (!isInVoid(player)) return;

        MinecraftServer server = player.getServer();
        if (server == null) return;

        ServerLevel targetLevel = null;
        double tx = 0.0D, ty = 0.0D, tz = 0.0D;

        if (hasGroundRecord(player)) {
            CompoundTag data = persisted(player);
            String recordedDim = data.getString(NBT_LAST_DIM);
            ServerLevel recorded = resolveLevel(server, recordedDim);
            if (recorded != null) {
                targetLevel = recorded;
                tx = data.getDouble(NBT_LAST_X);
                ty = data.getDouble(NBT_LAST_Y) + TELEPORT_LIFT;
                tz = data.getDouble(NBT_LAST_Z);
            }
        }

        if (targetLevel == null) {
            ServerLevel current = player.serverLevel();
            Vec3 spawn = Vec3.atBottomCenterOf(current.getSharedSpawnPos());
            targetLevel = current;
            tx = spawn.x;
            ty = spawn.y;
            tz = spawn.z;
        }

        player.teleportTo(targetLevel, tx, ty, tz,
                player.getYRot(), player.getXRot());
    }

    private static ServerLevel resolveLevel(MinecraftServer server, String dimId) {
        try {
            ResourceKey<Level> key = ResourceKey.create(
                    Registries.DIMENSION, new ResourceLocation(dimId));
            return server.getLevel(key);
        } catch (Exception e) {
            return null;
        }
    }

    // ============ 状态恢复 ============

    private static void restoreState(ServerPlayer player) {
        player.setHealth(player.getMaxHealth());
        player.deathTime = 0;
        player.hurtTime = 0;
        player.invulnerableTime = 60;
        player.clearFire();
        player.resetFallDistance();
        player.removeAllEffects();
    }
}