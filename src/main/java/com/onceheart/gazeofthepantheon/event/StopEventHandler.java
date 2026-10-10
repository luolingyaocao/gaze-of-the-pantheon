package com.onceheart.gazeofthepantheon.event;

import com.onceheart.gazeofthepantheon.GazeOfThePantheon;
import com.onceheart.gazeofthepantheon.network.ModNetwork;
import com.onceheart.gazeofthepantheon.network.StopShieldPacket;
import com.onceheart.gazeofthepantheon.registry.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 神行（stop）主逻辑。
 *
 * 覆盖：
 * - 每 tick：799 格内非持有实体的速度归零
 * - 每 tick：799 格内未持有神行的玩家 → 客户端输入压制（StopShieldPacket）
 * - Q 键丢弃拦截（物品加回背包）
 * - 死亡掉落摘除 + 重生恢复（PlayerPersisted NBT）
 * - 火 / 爆炸由 MixinItemEntity 处理
 * - /clear 由 MixinPlayerInventory 处理
 *
 * 不拦：5 分钟自然消失、仙人掌、虚空、漏斗抽取、玩家主动放入容器。
 */
@Mod.EventBusSubscriber(modid = GazeOfThePantheon.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class StopEventHandler {

    private static final double RADIUS_SQ = 799.0 * 799.0;

    private static final String PERSISTED_NBT_TAG = "PlayerPersisted";
    private static final String NBT_STOP_OWNED = "gazeofthepantheon_stop_owned";

    /** 当前持有神行的玩家 UUID。每秒刷新一次。 */
    private static final Set<UUID> stopHolders = new HashSet<>();

    /** 当前被压制的玩家 UUID。仅用于状态变化时发包。 */
    private static final Set<UUID> suppressedPlayers = new HashSet<>();

    private static CompoundTag persisted(ServerPlayer player) {
        CompoundTag root = player.getPersistentData();
        if (!root.contains(PERSISTED_NBT_TAG, Tag.TAG_COMPOUND)) {
            root.put(PERSISTED_NBT_TAG, new CompoundTag());
        }
        return root.getCompound(PERSISTED_NBT_TAG);
    }

    private static boolean hasStopInInventory(ServerPlayer player) {
        var inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (inv.getItem(i).getItem() == ModItems.STOP.get()) return true;
        }
        return false;
    }

    // ============ 每秒刷新持有者集合 ============

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer player)) return;
        if (player.tickCount % 20 != 0) return;

        if (hasStopInInventory(player)) {
            stopHolders.add(player.getUUID());
        } else {
            stopHolders.remove(player.getUUID());
        }
    }

    // ============ 每 tick：速度归零 + 压制同步 ============

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        MinecraftServer server = event.getServer();

        // 收集全部世界的持有者（跨世界持有神行同样算数）
        List<ServerPlayer> holders = new ArrayList<>();
        for (ServerLevel level : server.getAllLevels()) {
            for (ServerPlayer p : level.players()) {
                if (stopHolders.contains(p.getUUID())) holders.add(p);
            }
        }

        // ---- 玩家压制：对所有在线玩家重新判定 ----
        // 关键：即便 holders 为空，也要进入这段循环，把已压制的玩家解除
        for (ServerPlayer sp : server.getPlayerList().getPlayers()) {
            boolean suppressed = false;
            if (!stopHolders.contains(sp.getUUID())) {
                for (ServerPlayer h : holders) {
                    if (sp.level() == h.level() && sp.distanceToSqr(h) <= RADIUS_SQ) {
                        suppressed = true;
                        break;
                    }
                }
            }
            updateSuppression(sp, suppressed);
            if (suppressed) {
                sp.setDeltaMovement(Vec3.ZERO);
            }
        }

        // ---- 非玩家实体冻结：没有持有者则跳过 ----
        if (holders.isEmpty()) return;

        for (ServerLevel level : server.getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (entity instanceof ServerPlayer) continue;
                for (ServerPlayer h : holders) {
                    if (entity.level() == h.level()
                            && entity.distanceToSqr(h) <= RADIUS_SQ) {
                        freezeEntity(entity);
                        break;
                    }
                }
            }
        }
    }

    private static void freezeEntity(Entity entity) {
        entity.setDeltaMovement(Vec3.ZERO);
        entity.hasImpulse = false;
        if (entity instanceof LivingEntity le) {
            le.setSprinting(false);
        }
    }

    private static void updateSuppression(ServerPlayer player, boolean shouldSuppress) {
        UUID id = player.getUUID();
        boolean was = suppressedPlayers.contains(id);
        if (shouldSuppress == was) return;

        if (shouldSuppress) {
            suppressedPlayers.add(id);
            ModNetwork.CHANNEL.send(
                    PacketDistributor.PLAYER.with(() -> player),
                    new StopShieldPacket(true));
        } else {
            suppressedPlayers.remove(id);
            ModNetwork.CHANNEL.send(
                    PacketDistributor.PLAYER.with(() -> player),
                    new StopShieldPacket(false));
        }
    }

    // ============ 玩家登出：清理 ============

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        UUID id = player.getUUID();
        stopHolders.remove(id);
        suppressedPlayers.remove(id);
    }

    // ============ Q 键：拦丢弃 ============

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onItemToss(ItemTossEvent event) {
        ItemStack stack = event.getEntity().getItem();
        if (stack.getItem() != ModItems.STOP.get()) return;

        event.setCanceled(true);
        Player player = event.getPlayer();
        ItemStack returned = stack.copy();
        if (!player.getInventory().add(returned)) {
            player.getInventory().setItem(0, returned);
        }
    }

    // ============ 死亡：摘除掉落 + 标记 ============

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        Iterator<ItemEntity> it = event.getDrops().iterator();
        while (it.hasNext()) {
            ItemEntity ie = it.next();
            if (ie.getItem().getItem() == ModItems.STOP.get()) {
                it.remove();
                persisted(player).putBoolean(NBT_STOP_OWNED, true);
            }
        }
    }

    // ============ 重生：从 NBT 恢复 ============

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        CompoundTag data = persisted(player);
        if (data.getBoolean(NBT_STOP_OWNED)) {
            data.putBoolean(NBT_STOP_OWNED, false);
            ItemStack stop = new ItemStack(ModItems.STOP.get());
            if (!player.getInventory().add(stop)) {
                player.getInventory().setItem(0, stop);
            }
        }
    }
}