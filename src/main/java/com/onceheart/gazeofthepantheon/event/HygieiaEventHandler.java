package com.onceheart.gazeofthepantheon.event;

import com.onceheart.gazeofthepantheon.GazeOfThePantheon;
import com.onceheart.gazeofthepantheon.util.CuriosUtil;
import com.onceheart.gazeofthepantheon.util.EdictData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.event.entity.player.PlayerWakeUpEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = GazeOfThePantheon.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class HygieiaEventHandler {

    private static final UUID HEALTH_BONUS_UUID = UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890");
    private static final String HEALTH_BONUS_NAME = "gazeofthepantheon.hygieia_health_bonus";

    private static final int CAMPFIRE_HEAL_INTERVAL = 80;

    // ============ 衰竭 + 丰饶：每 tick 处理 ============

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer player)) return;

        boolean hasWrath = !CuriosUtil.findHygieiaWrath(player).isEmpty();
        boolean hasKindness = !CuriosUtil.findHygieiaKindness(player).isEmpty();

        // ---- 衰竭：诅咒，永远生效 ----
        if (hasWrath) {
            if (player.tickCount % CAMPFIRE_HEAL_INTERVAL == 0) {
                if (isNearCampfire(player)) {
                    if (player.getHealth() < player.getMaxHealth()) {
                        player.setHealth(Math.min(player.getMaxHealth(), player.getHealth() + 1.0F));
                    }
                }
            }
        }

        // ---- 丰饶：祝福，必行敕令激活时失效 ----
        boolean kindnessActive = hasKindness && !EdictData.isEdictActive(player);

        if (kindnessActive) {
            // 生命提升 +100%
            AttributeInstance maxHealth = player.getAttribute(Attributes.MAX_HEALTH);
            if (maxHealth != null) {
                AttributeModifier existing = maxHealth.getModifier(HEALTH_BONUS_UUID);
                if (existing == null || existing.getAmount() != 1.0D
                        || existing.getOperation() != AttributeModifier.Operation.MULTIPLY_TOTAL) {
                    if (existing != null) {
                        maxHealth.removeModifier(HEALTH_BONUS_UUID);
                    }
                    maxHealth.addTransientModifier(new AttributeModifier(
                            HEALTH_BONUS_UUID,
                            HEALTH_BONUS_NAME,
                            1.0D,
                            AttributeModifier.Operation.MULTIPLY_TOTAL));
                }
            }

            // 每 5 秒吸收 II
            if (player.tickCount % 100 == 0) {
                player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 120, 1, false, false));
            }

            // 每 10 秒瞬间回复 II
            if (player.tickCount % 200 == 0) {
                player.addEffect(new MobEffectInstance(MobEffects.HEAL, 20, 1, false, false));
            }
        } else {
            AttributeInstance maxHealth = player.getAttribute(Attributes.MAX_HEALTH);
            if (maxHealth != null && maxHealth.getModifier(HEALTH_BONUS_UUID) != null) {
                maxHealth.removeModifier(HEALTH_BONUS_UUID);
            }
        }
    }

    // ============ 衰竭：拦截自然回血（诅咒，永远生效） ============

    @SubscribeEvent
    public static void onLivingHeal(LivingHealEvent event) {
        if (event.getEntity().level().isClientSide) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        if (CuriosUtil.findHygieiaWrath(player).isEmpty()) return;

        if (player.hasEffect(MobEffects.REGENERATION) || player.hasEffect(MobEffects.HEAL)) {
            return;
        }

        event.setCanceled(true);
    }

    // ============ 衰竭：自然睡醒后回满血（诅咒，永远生效） ============

    @SubscribeEvent
    public static void onPlayerWakeUp(PlayerWakeUpEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (CuriosUtil.findHygieiaWrath(player).isEmpty()) return;
        if (player.getSleepTimer() < 100) return;

        player.setHealth(player.getMaxHealth());
        player.sendSystemMessage(Component.translatable(
                "message.gazeofthepantheon.hygieia_wrath.wake_up"));
    }

    // ============ 工具方法 ============

    private static boolean isNearCampfire(ServerPlayer player) {
        BlockPos center = player.blockPosition();
        int radius = 10;
        int radiusSq = radius * radius;

        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    if (x * x + y * y + z * z > radiusSq) continue;
                    BlockPos pos = center.offset(x, y, z);
                    BlockState state = player.level().getBlockState(pos);
                    if (state.is(Blocks.CAMPFIRE) || state.is(Blocks.SOUL_CAMPFIRE)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}