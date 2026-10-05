package com.onceheart.gazeofthepantheon.event;

import com.onceheart.gazeofthepantheon.GazeOfThePantheon;
import com.onceheart.gazeofthepantheon.item.AchillesItem;
import com.onceheart.gazeofthepantheon.item.AresItem;
import com.onceheart.gazeofthepantheon.item.HermesItem;
import com.onceheart.gazeofthepantheon.item.HygieiaItem;
import com.onceheart.gazeofthepantheon.item.ThanatosItem;
import com.onceheart.gazeofthepantheon.item.XiheItem;
import com.onceheart.gazeofthepantheon.registry.ModItems;
import com.onceheart.gazeofthepantheon.util.CuriosUtil;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * 注视饰品与必行敕令的死亡保存 / 重生恢复。
 *
 * 覆盖六个神系的注视饰品（诅咒或祝福）与必行敕令：
 * 死亡时从掉落物中摘出并缓存，重生时按原栏位放回。
 *
 * 硬核死亡（愠怒死亡后的旁观模式）不在这里，见 ThanatosEventHandler。
 */
@Mod.EventBusSubscriber(modid = GazeOfThePantheon.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class GazePersistenceHandler {

    private static final Map<UUID, ItemStack> SAVED_KINDNESS = new HashMap<>();
    private static final Map<UUID, ItemStack> SAVED_HYGIEIA = new HashMap<>();
    private static final Map<UUID, ItemStack> SAVED_ARES = new HashMap<>();
    private static final Map<UUID, ItemStack> SAVED_HERMES = new HashMap<>();
    private static final Map<UUID, ItemStack> SAVED_XIHE = new HashMap<>();
    private static final Map<UUID, ItemStack> SAVED_ACHILLES = new HashMap<>();
    private static final Map<UUID, ItemStack> SAVED_EDICT = new HashMap<>();

    // ============ 掉落：摘出注视饰品与必行敕令 ============

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        Iterator<ItemEntity> it = event.getDrops().iterator();
        while (it.hasNext()) {
            ItemEntity ie = it.next();
            ItemStack stack = ie.getItem();

            if (stack.getItem() instanceof ThanatosItem && ThanatosItem.isBlessed(stack)) {
                SAVED_KINDNESS.put(player.getUUID(), stack.copy());
                it.remove();
            } else if (stack.getItem() instanceof HygieiaItem) {
                SAVED_HYGIEIA.put(player.getUUID(), stack.copy());
                it.remove();
            } else if (stack.getItem() instanceof AresItem) {
                SAVED_ARES.put(player.getUUID(), stack.copy());
                it.remove();
            } else if (stack.getItem() instanceof HermesItem) {
                SAVED_HERMES.put(player.getUUID(), stack.copy());
                it.remove();
            } else if (stack.getItem() instanceof XiheItem) {
                SAVED_XIHE.put(player.getUUID(), stack.copy());
                it.remove();
            } else if (stack.getItem() instanceof AchillesItem) {
                SAVED_ACHILLES.put(player.getUUID(), stack.copy());
                it.remove();
            } else if (stack.getItem() == ModItems.EDICT.get()) {
                SAVED_EDICT.put(player.getUUID(), stack.copy());
                it.remove();
            }
        }
    }

    // ============ 重生：按原栏位恢复 ============

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        restoreGaze(player, SAVED_KINDNESS.remove(player.getUUID()));
        restoreGaze(player, SAVED_HYGIEIA.remove(player.getUUID()));
        restoreGaze(player, SAVED_ARES.remove(player.getUUID()));
        restoreGaze(player, SAVED_HERMES.remove(player.getUUID()));
        restoreGaze(player, SAVED_XIHE.remove(player.getUUID()));
        restoreGaze(player, SAVED_ACHILLES.remove(player.getUUID()));

        ItemStack savedEdict = SAVED_EDICT.remove(player.getUUID());
        if (savedEdict != null && !savedEdict.isEmpty()) {
            CuriosUtil.tryEquipToDecision(player, savedEdict);
        }
    }

    private static void restoreGaze(ServerPlayer player, ItemStack stack) {
        if (stack == null || stack.isEmpty()) return;
        CuriosUtil.tryEquipToGaze(player, stack);
    }
}