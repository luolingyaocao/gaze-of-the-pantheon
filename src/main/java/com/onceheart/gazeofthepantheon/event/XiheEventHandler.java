package com.onceheart.gazeofthepantheon.event;

import com.onceheart.gazeofthepantheon.GazeOfThePantheon;
import com.onceheart.gazeofthepantheon.util.CuriosUtil;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = GazeOfThePantheon.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class XiheEventHandler {

    /** 火焰不灭：当剩余着火时间即将归零时重置 */
    @SubscribeEvent
    public static void onLivingTick(net.minecraftforge.event.entity.living.LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        boolean hasWrath = !CuriosUtil.findXiheWrath(player).isEmpty();

        if (hasWrath) {
            // 火焰永不自然熄灭（在水中/雨中仍可被扑灭）
            if (player.isOnFire() && player.getRemainingFireTicks() <= 1
                    && !player.isInWaterOrRain()) {
                player.setRemainingFireTicks(20);
            }

            // 阳光下自燃
            Level level = player.level();
            if (!level.isClientSide
                    && level.isDay()
                    && !level.isRaining()
                    && !player.isInWaterOrRain()
                    && level.canSeeSky(player.blockPosition())) {
                player.setSecondsOnFire(8);
            }
        }
    }

    /** 恩泽：火焰免疫 + 夜视 + 着火回血 */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer player)) return;

        boolean hasKindness = !CuriosUtil.findXiheKindness(player).isEmpty();

        if (hasKindness) {
            // 夜视（隐藏粒子 + 隐藏图标）
            if (player.tickCount % 20 == 0) {
                player.addEffect(new MobEffectInstance(
                        MobEffects.NIGHT_VISION, 400, 0, false, false));
            }

            // 着火时每 40 tick 回复最大生命值的 5%
            if (player.isOnFire() && player.tickCount % 40 == 0) {
                float maxHealth = player.getMaxHealth();
                float newHealth = Math.min(maxHealth, player.getHealth() + maxHealth * 0.05F);
                player.setHealth(newHealth);
            }
        }
    }

    /** 恩泽：免疫火焰伤害（按抗火标准，但不显示粒子） */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (event.getAmount() <= 0) return;

        if (CuriosUtil.findXiheKindness(player).isEmpty()) return;

        if (event.getSource().is(DamageTypeTags.IS_FIRE)) {
            event.setCanceled(true);
        }
    }
}