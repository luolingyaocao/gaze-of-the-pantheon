package com.onceheart.gazeofthepantheon.event;

import com.onceheart.gazeofthepantheon.GazeOfThePantheon;
import com.onceheart.gazeofthepantheon.registry.ModItems;
import com.onceheart.gazeofthepantheon.util.CuriosUtil;
import com.onceheart.gazeofthepantheon.util.EdictData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Mod.EventBusSubscriber(modid = GazeOfThePantheon.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class EdictBossDropHandler {

    private static final int DEED_OFFSET_Y = 15;

    private static final DustParticleOptions GOLD_DUST = new DustParticleOptions(
            new Vector3f(1.0F, 0.84F, 0.0F), 1.0F);

    // ============ 击败凋零：掉落必行敕令 ============

    @SubscribeEvent
    public static void onWitherDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof WitherBoss wither)) return;
        if (!(wither.level() instanceof ServerLevel level)) return;

        ServerPlayer killer = getKiller(event);
        if (killer == null) return;

        // 已经拥有必行敕令（决策栏位或背包中）就不掉
        if (hasEdictAnywhere(killer)) return;

        ItemStack edict = new ItemStack(ModItems.EDICT.get());
        ItemEntity ie = new ItemEntity(level,
                wither.getX(), wither.getY() + 0.5, wither.getZ(), edict);
        ie.setPickUpDelay(20);
        level.addFreshEntity(ie);

        killer.sendSystemMessage(Component.translatable(
                "message.gazeofthepantheon.edict.dropped"));
    }

    // ============ 击败末影龙：掉落成事在人 ============

    @SubscribeEvent
    public static void onDragonDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof EnderDragon dragon)) return;
        if (!(dragon.level() instanceof ServerLevel level)) return;

        ServerPlayer killer = getKiller(event);
        if (killer == null) return;

        // 必须装备着必行敕令
        if (!CuriosUtil.hasDecisionEquipped(killer)) return;

        // 已经拥有成事在人（背包中或已放入敕令）就不掉
        if (hasDeedAnywhere(killer)) return;

        // 检查祝福注视是否齐全
        List<Component> missing = findMissingBlessings(killer);
        if (!missing.isEmpty()) {
            killer.sendSystemMessage(Component.translatable(
                    "message.gazeofthepantheon.deed.missing_header"));
            for (Component line : missing) {
                killer.sendSystemMessage(line);
            }
            return;
        }

        // 掉落
        killer.sendSystemMessage(Component.translatable(
                "message.gazeofthepantheon.deed.summoned"));

        ServerLevel endLevel = killer.server.getLevel(ServerLevel.END);
        if (endLevel == null) return;

        BlockPos spawnPos = new BlockPos(0, 60 + DEED_OFFSET_Y, 0);
        spawnFloatingDeed(endLevel, spawnPos);
    }

    private static void spawnFloatingDeed(ServerLevel level, BlockPos pos) {
        ItemStack deed = new ItemStack(ModItems.DEED.get());
        double x = pos.getX() + 0.5;
        double y = pos.getY() + 0.5;
        double z = pos.getZ() + 0.5;

        ItemEntity ie = new ItemEntity(level, x, y, z, deed);
        ie.setNoGravity(true);
        ie.setNeverPickUp();
        ie.setPickUpDelay(20);
        ie.getPersistentData().putBoolean("gazeofthepantheon_floating_deed", true);
        level.addFreshEntity(ie);

        level.sendParticles(GOLD_DUST, x, y, z, 30, 1.0, 1.0, 1.0, 0.01);
        level.sendParticles(ParticleTypes.END_ROD, x, y, z, 20, 1.0, 1.0, 1.0, 0.05);
    }

    // ============ 工具方法 ============

    private static ServerPlayer getKiller(LivingDeathEvent event) {
        var source = event.getSource();
        var entity = source.getEntity();
        if (!(entity instanceof ServerPlayer player)) return null;
        return player;
    }

    private static boolean hasEdictAnywhere(ServerPlayer player) {
        if (CuriosUtil.hasDecisionEquipped(player)) return true;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack s = player.getInventory().getItem(i);
            if (s.getItem() == ModItems.EDICT.get()) return true;
        }
        return false;
    }

    private static boolean hasDeedAnywhere(ServerPlayer player) {
        if (EdictData.hasDeed(player)) return true;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack s = player.getInventory().getItem(i);
            if (s.getItem() == ModItems.DEED.get()) return true;
        }
        return false;
    }

    /** 找出还差哪些祝福注视 */
    private static List<Component> findMissingBlessings(ServerPlayer player) {
        Set<net.minecraft.world.item.Item> current = new HashSet<>();
        for (ItemStack s : EdictData.getBlessings(player)) {
            current.add(s.getItem());
        }

        // 全部祝福注视及其对应的显示键
        java.util.Map<net.minecraft.world.item.Item, String> allBlessings = new java.util.LinkedHashMap<>();
        allBlessings.put(ModItems.THANATOS.get(), "message.gazeofthepantheon.gaze.thanatos_kindness");
        allBlessings.put(ModItems.HYGIEIA.get(), "message.gazeofthepantheon.gaze.hygieia_kindness");
        allBlessings.put(ModItems.ARES.get(), "message.gazeofthepantheon.gaze.ares_kindness");
        allBlessings.put(ModItems.HERMES.get(), "message.gazeofthepantheon.gaze.hermes_kindness");
        allBlessings.put(ModItems.XIHE.get(), "message.gazeofthepantheon.gaze.xihe_kindness");
        allBlessings.put(ModItems.ACHILLES.get(), "message.gazeofthepantheon.gaze.achilles_kindness");

        List<Component> missing = new ArrayList<>();
        for (var e : allBlessings.entrySet()) {
            if (!current.contains(e.getKey())) {
                missing.add(Component.translatable(e.getValue()));
            }
        }
        return missing;
    }
}