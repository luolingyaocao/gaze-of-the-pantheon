package com.onceheart.gazeofthepantheon.util;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.fml.ModList;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 对不朽玩家做全面的状态修复。
 *
 * 部分模组（如秘法涡流）的"代码杀"会绕过所有方法调用，
 * 直接反射修改玩家字段（removalReason / dead / deathTime / health 等），
 * 我们的 Mixin 只能拦方法调用，拦不住字段直写。
 * 因此每 tick 强制重置所有关键字段。
 *
 * 同时反射清理 AV 的三层血量系统 + 锁血状态。
 * AV 的类必须用 AV mod 的类加载器来加载（Forge 的 mod 类加载器隔离）。
 *
 * 所有操作都是 try-catch 保护，失败时静默跳过。
 */
public class PlayerStateResetUtil {

    /** 字段缓存：类名 + 字段名 → Field。加速反复反射。 */
    private static final Map<String, Field> FIELD_CACHE = new ConcurrentHashMap<>();

    /** AV 的 modid */
    private static final String AV_MODID = "arcanevortex";

    /** 缓存的 AV 类加载器 */
    private static ClassLoader avClassLoader = null;
    private static boolean avLoaderResolved = false;

    /**
     * AV 的锁血与三层血量系统的类名 + 清理方法。
     */
    private static final String[][] ARCANEVORTEX_CLEARS = {
            {"com.erchien.arcanevortex.Help.VanShForce.HealthLockManager", "unlockEntityHealth"},
            {"com.erchien.arcanevortex.Core.CoreEntity.HealthMethodHelper", "clear"},
            {"com.erchien.arcanevortex.NativeAgent.NativeAgentHealthMethod", "clear"},
            {"com.erchien.arcanevortex.Agent.HealthMethod.AgentHealthMethodHelper", "clear"},
    };

    /**
     * 对不朽玩家做全面的状态修复。由 PlayerTickEvent.END 每 tick 调用。
     */
    public static void resetAll(ServerPlayer player) {
        // 1. 先清 AV 的锁血与三层血量系统（必须先于读 maxHealth）
        clearArcaneVortexLocks(player);

        // 2. 用属性系统拿真实最大血量，绕过 AV 对 getMaxHealth() 的拦截
        float realMax;
        try {
            var attr = player.getAttribute(Attributes.MAX_HEALTH);
            realMax = (attr != null) ? (float) attr.getValue() : 20.0F;
        } catch (Throwable t) {
            realMax = 20.0F;
        }
        if (realMax <= 0.0F) realMax = 20.0F;

        // 3. 直写 entityData 的 DATA_HEALTH_ID，绕过 setHealth Mixin
        setSyncedHealth(player, realMax);
        // 4. 兼容旧字段直写
        setFloat(player, "health", realMax);

        // 5. 死亡状态清零
        setInt(player, "deathTime", 0);
        setInt(player, "hurtTime", 0);
        setInt(player, "invulnerableTime", 20);
        setFloat(player, "lastHurt", 0.0F);
        setBoolean(player, "dead", false);

        // 6. 移除状态清零
        setBoolean(player, "isAddedToWorld", true);
        setBoolean(player, "canUpdate", true);
        setObject(player, "removalReason", null);

        // 7. 强制重置客户端同步哨兵值，让 ServerPlayer.doTick 下一 tick
        //    一定会重发 ClientboundSetHealthPacket
        setFloat(player, "lastSentHealth", -1.0E8F);

        // 8. 位置修复
        if (isInvalidPosition(player)) {
            repairPosition(player);
        }
    }

    private static boolean isInvalidPosition(ServerPlayer player) {
        double x = player.getX();
        double y = player.getY();
        double z = player.getZ();
        return Double.isNaN(x) || Double.isNaN(y) || Double.isNaN(z)
                || x < -1.0E6 || y < -1.0E6 || z < -1.0E6;
    }

    private static void repairPosition(ServerPlayer player) {
        var server = player.getServer();
        if (server == null) return;
        var overworld = server.overworld();
        var spawn = overworld.getSharedSpawnPos();
        player.teleportTo(overworld,
                spawn.getX() + 0.5, spawn.getY() + 1.0, spawn.getZ() + 0.5,
                player.getYRot(), player.getXRot());
    }

    /** 直写 entityData 的 DATA_HEALTH_ID（绕过 setHealth Mixin） */
    private static void setSyncedHealth(ServerPlayer player, float value) {
        try {
            Field dataField = findField(player.getClass(), "entityData");
            if (dataField == null) return;
            Object entityData = dataField.get(player);
            if (entityData == null) return;

            Field healthIdField = findField(net.minecraft.world.entity.LivingEntity.class, "DATA_HEALTH_ID");
            if (healthIdField == null) return;
            Object accessor = healthIdField.get(null);
            if (accessor == null) return;

            var setMethod = entityData.getClass().getMethod("set",
                    net.minecraft.network.syncher.EntityDataAccessor.class, Object.class);
            setMethod.setAccessible(true);
            setMethod.invoke(entityData, accessor, value);
        } catch (Exception ignored) {
        }
    }

    /** 反射清 AV 的三层血量系统 + 锁血 */
    private static void clearArcaneVortexLocks(ServerPlayer player) {
        for (String[] entry : ARCANEVORTEX_CLEARS) {
            tryInvokeStatic(entry[0], entry[1], player);
        }
    }

    /**
     * 用 AV mod 的类加载器加载类。Forge 的 mod 类加载器互相隔离，
     * 我们必须显式用目标 mod 的类加载器才能看到它的类。
     */
    private static Class<?> loadModClass(String className) {
        try {
            if (!avLoaderResolved) {
                avLoaderResolved = true;
                var opt = ModList.get().getModContainerById(AV_MODID);
                if (opt.isPresent()) {
                    var avMod = opt.get().getMod();
                    avClassLoader = avMod.getClass().getClassLoader();
                }
            }
            if (avClassLoader != null) {
                return Class.forName(className, false, avClassLoader);
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static void tryInvokeStatic(String className, String methodName, Object arg) {
        try {
            Class<?> cls = loadModClass(className);
            if (cls == null) return;
            Class<?> argType = arg.getClass();
            for (Method m : cls.getMethods()) {
                if (!m.getName().equals(methodName)) continue;
                if (m.getParameterCount() != 1) continue;
                Class<?> paramType = m.getParameterTypes()[0];
                if (paramType.isAssignableFrom(argType)) {
                    m.invoke(null, arg);
                    return;
                }
            }
        } catch (Throwable ignored) {
        }
    }

    // ============ 反射字段访问 ============

    private static Field findField(Class<?> clazz, String name) {
        String key = clazz.getName() + "#" + name;
        Field cached = FIELD_CACHE.get(key);
        if (cached != null) return cached;

        Class<?> c = clazz;
        while (c != null && c != Object.class) {
            try {
                Field f = c.getDeclaredField(name);
                f.setAccessible(true);
                FIELD_CACHE.put(key, f);
                return f;
            } catch (NoSuchFieldException ignored) {
                c = c.getSuperclass();
            }
        }
        return null;
    }

    private static void setFloat(Object obj, String fieldName, float value) {
        Field f = findField(obj.getClass(), fieldName);
        if (f == null) return;
        try {
            f.setFloat(obj, value);
        } catch (Exception ignored) {}
    }

    private static void setInt(Object obj, String fieldName, int value) {
        Field f = findField(obj.getClass(), fieldName);
        if (f == null) return;
        try {
            f.setInt(obj, value);
        } catch (Exception ignored) {}
    }

    private static void setBoolean(Object obj, String fieldName, boolean value) {
        Field f = findField(obj.getClass(), fieldName);
        if (f == null) return;
        try {
            f.setBoolean(obj, value);
        } catch (Exception ignored) {}
    }

    private static void setObject(Object obj, String fieldName, Object value) {
        Field f = findField(obj.getClass(), fieldName);
        if (f == null) return;
        try {
            f.set(obj, value);
        } catch (Exception ignored) {}
    }
}