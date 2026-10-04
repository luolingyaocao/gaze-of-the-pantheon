package com.onceheart.gazeofthepantheon.event;

import com.onceheart.gazeofthepantheon.GazeOfThePantheon;
import com.onceheart.gazeofthepantheon.util.CuriosUtil;
import com.onceheart.gazeofthepantheon.util.EdictData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = GazeOfThePantheon.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class AchillesEventHandler {

    private static final int RESISTANCE_INTERVAL = 200;
    private static final int RESISTANCE_DURATION = 60;

    private static final Map<UUID, Float> RAW_DAMAGE = new HashMap<>();

    // ============ 坚毅：祝福，必行敕令激活时失效 ============

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer player)) return;

        boolean hasKindness = !CuriosUtil.findAchillesKindness(player).isEmpty();
        boolean kindnessActive = hasKindness && !EdictData.isEdictActive(player);

        if (kindnessActive) {
            if (player.tickCount % RESISTANCE_INTERVAL == 0) {
                player.addEffect(new MobEffectInstance(
                        MobEffects.DAMAGE_RESISTANCE,
                        RESISTANCE_DURATION, 3, false, false));
            }
        }
    }

    // ============ 踵：诅咒，永远生效 ============

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (event.getAmount() <= 0) return;
        if (CuriosUtil.findAchillesWrath(player).isEmpty()) return;

        RAW_DAMAGE.put(player.getUUID(), event.getAmount());
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingDamage(LivingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        Float raw = RAW_DAMAGE.remove(player.getUUID());
        if (raw == null) return;
        if (CuriosUtil.findAchillesWrath(player).isEmpty()) return;

        event.setAmount(raw);
    }
}