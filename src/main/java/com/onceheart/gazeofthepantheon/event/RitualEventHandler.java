package com.onceheart.gazeofthepantheon.event;

import com.onceheart.gazeofthepantheon.GazeOfThePantheon;
import com.onceheart.gazeofthepantheon.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = GazeOfThePantheon.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class RitualEventHandler {

    /** 金色粒子 */
    private static final DustParticleOptions GOLD_DUST = new DustParticleOptions(
            new Vector3f(1.0F, 0.84F, 0.0F), 1.0F);

    /** 仪式状态：玩家 UUID -> 开始 tick */
    private static final Map<UUID, Long> ACTIVE_RITUALS = new HashMap<>();

    /** 仪式持续时间（3 秒 = 60 tick） */
    private static final int RITUAL_DURATION = 60;
    /** 螺旋粒子阶段（2 秒 = 40 tick） */
    private static final int SPIRAL_PHASE = 40;

    /**
     * 尝试启动仪式。成功返回 true，失败返回 false。
     */
    public static boolean tryStartRitual(Player player, ItemStack featherStack) {
        if (!(player instanceof ServerPlayer serverPlayer)) return false;

        // 已经在仪式中
        if (ACTIVE_RITUALS.containsKey(player.getUUID())) return false;

        // 检查时间：4:30-7:00（23500~24000 或 0~1000）
        long dayTime = player.level().getDayTime() % 24000;
        if (!(dayTime >= 23500 || dayTime <= 1000)) return false;

        // 检查朝向：东方（yaw ≈ -90）
        float yaw = Mth.wrapDegrees(player.getYRot());
        if (yaw < -135.0F || yaw > -45.0F) return false;

        // 检查脚下是金块
        BlockPos goldPos = player.blockPosition().below();
        if (!player.level().getBlockState(goldPos).is(Blocks.GOLD_BLOCK)) return false;

        // 检查结构
        if (!checkStructure(player.level(), goldPos)) return false;

        // 消耗翎羽
        if (!player.isCreative()) {
            featherStack.shrink(1);
        }

        // 开始仪式
        ACTIVE_RITUALS.put(player.getUUID(), (long) player.tickCount);
        return true;
    }

    /** 检查扶桑祭祀结构 */
    private static boolean checkStructure(net.minecraft.world.level.Level level, BlockPos goldPos) {
        // goldPos 是金块位置，相对原点 (0,1,0)
        // 原点 = goldPos.offset(0, -1, 0)
        BlockPos origin = goldPos.offset(0, -1, 0);

        // y=0：5×5 平滑石英
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                if (!level.getBlockState(origin.offset(x, 0, z)).is(Blocks.SMOOTH_QUARTZ)) {
                    return false;
                }
            }
        }

        // y=1：8 泥土（金块周围）+ 8 钻石块
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                if (x == 0 && z == 0) continue;
                BlockPos dirtPos = goldPos.offset(x, 0, z);
                if (!level.getBlockState(dirtPos).is(Blocks.DIRT)) return false;
                // 泥土上方是树苗
                if (!level.getBlockState(dirtPos.above()).is(BlockTags.SAPLINGS)) return false;
            }
        }
        int[][] diamondPositions = {
                {2, 2}, {2, 0}, {2, -2},
                {0, 2}, {0, -2},
                {-2, -2}, {-2, 0}, {-2, 2}
        };
        for (int[] p : diamondPositions) {
            if (!level.getBlockState(origin.offset(p[0], 1, p[1])).is(Blocks.DIAMOND_BLOCK)) {
                return false;
            }
        }

        // y=1~3：4 根铁块柱
        for (int x : new int[]{-3, 3}) {
            for (int z : new int[]{-3, 3}) {
                for (int y = 1; y <= 3; y++) {
                    if (!level.getBlockState(origin.offset(x, y, z)).is(Blocks.IRON_BLOCK)) {
                        return false;
                    }
                }
            }
        }

        // y=4：4 个荧石
        for (int x : new int[]{-3, 3}) {
            for (int z : new int[]{-3, 3}) {
                if (!level.getBlockState(origin.offset(x, 4, z)).is(Blocks.GLOWSTONE)) {
                    return false;
                }
            }
        }

        return true;
    }

    /** 每 tick 推进仪式 */
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (ACTIVE_RITUALS.isEmpty()) return;

        var iterator = ACTIVE_RITUALS.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            UUID uuid = entry.getKey();
            long startTick = entry.getValue();

            ServerPlayer player = event.getServer().getPlayerList().getPlayer(uuid);
            if (player == null) {
                // 玩家离线，取消仪式
                iterator.remove();
                continue;
            }

            long elapsed = player.tickCount - startTick;

            if (elapsed < SPIRAL_PHASE) {
                // 螺旋粒子阶段（0-40 tick）
                spawnSpiralParticles(player, (int) elapsed);
            } else if (elapsed < RITUAL_DURATION) {
                // 破碎粒子阶段（40-60 tick）
                spawnBurstParticles(player);
            } else {
                // 仪式完成
                completeRitual(player);
                iterator.remove();
            }
        }
    }

    /** 螺旋粒子：4 个粒子绕玩家旋转上升并汇聚 */
    private static void spawnSpiralParticles(ServerPlayer player, int tick) {
        ServerLevel level = player.serverLevel();
        double progress = tick / (double) SPIRAL_PHASE;
        double radius = 1.5 * (1.0 - progress);
        double height = progress * 4.0;
        double baseAngle = tick * 0.15;

        for (int i = 0; i < 4; i++) {
            double angle = baseAngle + i * Math.PI / 2.0;
            double x = player.getX() + Math.cos(angle) * radius;
            double z = player.getZ() + Math.sin(angle) * radius;
            double y = player.getY() + height;
            level.sendParticles(GOLD_DUST, x, y, z, 1, 0, 0, 0, 0);
        }
    }

    /** 破碎粒子：类似末影之眼破碎，但为金色 */
    private static void spawnBurstParticles(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        for (int i = 0; i < 6; i++) {
            double theta = player.getRandom().nextDouble() * Math.PI * 2.0;
            double phi = (player.getRandom().nextDouble() - 0.5) * Math.PI;
            double speed = 0.3 + player.getRandom().nextDouble() * 0.3;
            double vx = Math.cos(theta) * Math.cos(phi) * speed;
            double vy = Math.sin(phi) * speed;
            double vz = Math.sin(theta) * Math.cos(phi) * speed;
            level.sendParticles(GOLD_DUST,
                    player.getX(), player.getY() + 1.0, player.getZ(),
                    1, vx, vy, vz, 0.5);
        }
    }

    /** 仪式完成：给朝露 + 结构变地狱岩 */
    private static void completeRitual(ServerPlayer player) {
        // 找到金块位置
        BlockPos goldPos = player.blockPosition().below();
        if (!player.level().getBlockState(goldPos).is(Blocks.GOLD_BLOCK)) {
            // 玩家移动了，从结构原点向下找金块
            goldPos = findGoldBlock(player);
        }

        // 给朝露
        ItemStack dew = new ItemStack(ModItems.FUSANG_DEW.get());
        if (!player.getInventory().add(dew)) {
            player.drop(dew, false);
        }

        // 结构变地狱岩
        if (goldPos != null) {
            replaceStructureWithNetherrack(player.serverLevel(), goldPos);
        }

        GazeOfThePantheon.LOGGER.debug("Fusang ritual completed for {}",
                player.getName().getString());
    }

    /** 从玩家当前位置向下寻找金块 */
    private static BlockPos findGoldBlock(ServerPlayer player) {
        BlockPos pos = player.blockPosition();
        for (int y = 0; y < 5; y++) {
            BlockPos check = pos.below(y);
            if (player.level().getBlockState(check).is(Blocks.GOLD_BLOCK)) {
                return check;
            }
        }
        return null;
    }

    /** 将整个扶桑祭祀结构替换为地狱岩 */
    private static void replaceStructureWithNetherrack(ServerLevel level, BlockPos goldPos) {
        BlockPos origin = goldPos.offset(0, -1, 0);

        // y=0：5×5
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                level.setBlockAndUpdate(origin.offset(x, 0, z),
                        Blocks.NETHERRACK.defaultBlockState());
            }
        }

        // y=1：金块 + 8 泥土 + 8 钻石块
        level.setBlockAndUpdate(goldPos, Blocks.NETHERRACK.defaultBlockState());
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                if (x == 0 && z == 0) continue;
                level.setBlockAndUpdate(goldPos.offset(x, 0, z),
                        Blocks.NETHERRACK.defaultBlockState());
            }
        }
        int[][] diamondPositions = {
                {2, 2}, {2, 0}, {2, -2},
                {0, 2}, {0, -2},
                {-2, -2}, {-2, 0}, {-2, 2}
        };
        for (int[] p : diamondPositions) {
            level.setBlockAndUpdate(origin.offset(p[0], 1, p[1]),
                    Blocks.NETHERRACK.defaultBlockState());
        }

        // y=1~3：4 根铁块柱
        for (int x : new int[]{-3, 3}) {
            for (int z : new int[]{-3, 3}) {
                for (int y = 1; y <= 3; y++) {
                    level.setBlockAndUpdate(origin.offset(x, y, z),
                            Blocks.NETHERRACK.defaultBlockState());
                }
            }
        }

        // y=4：4 荧石
        for (int x : new int[]{-3, 3}) {
            for (int z : new int[]{-3, 3}) {
                level.setBlockAndUpdate(origin.offset(x, 4, z),
                        Blocks.NETHERRACK.defaultBlockState());
            }
        }

        // 树苗位置（y=2 的 8 个泥土上方）
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                if (x == 0 && z == 0) continue;
                level.setBlockAndUpdate(goldPos.offset(x, 1, z),
                        Blocks.NETHERRACK.defaultBlockState());
            }
        }
    }
}