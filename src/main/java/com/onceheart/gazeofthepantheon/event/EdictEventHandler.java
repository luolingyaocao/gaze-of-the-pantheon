package com.onceheart.gazeofthepantheon.event;

import com.onceheart.gazeofthepantheon.GazeOfThePantheon;
import com.onceheart.gazeofthepantheon.util.CuriosUtil;
import com.onceheart.gazeofthepantheon.util.EdictData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

@Mod.EventBusSubscriber(modid = GazeOfThePantheon.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class EdictEventHandler {

    /** 天威检测半径 */
    private static final double AUTHORITY_RADIUS = 50.0D;

    /** 天威飞行掉血间隔（5 秒 = 100 tick） */
    private static final int AUTHORITY_FLIGHT_TICK = 100;

    // ============ 通用工具：造成真实伤害 ============

    /** 对目标造成无视护甲、抗性、无敌标记的真实伤害 */
    public static void dealTrueDamage(LivingEntity target, float amount, net.minecraft.world.damagesource.DamageSource source) {
        if (target.isDeadOrDying()) return;

        // 清除无敌帧和无敌标记，让伤害真正落下
        target.invulnerableTime = 0;
        if (target.isInvulnerable()) {
            target.setInvulnerable(false);
        }

        float newHealth = target.getHealth() - amount;
        if (newHealth <= 0.0F) {
            target.setHealth(0.0F);
            // 用 die 触发正常死亡逻辑，保证掉落经验和掉落物
            target.die(source);
        } else {
            target.setHealth(newHealth);
        }
    }

    // ============ 每 tick 效果：天威 ============

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer player)) return;

        if (!CuriosUtil.hasDecisionEquipped(player)) return;
        if (!EdictData.isEdictActive(player)) return;

        // 天威
        if (EdictData.isEffectOn(player, EdictData.EFFECT_AUTHORITY)) {
            // 每 20 tick 给予周围 50 格内非玩家生物虚弱 III
            if (player.tickCount % 20 == 0) {
                AABB area = player.getBoundingBox().inflate(AUTHORITY_RADIUS);
                List<LivingEntity> entities = player.level().getEntitiesOfClass(LivingEntity.class, area);
                for (LivingEntity e : entities) {
                    if (e instanceof Player) continue;
                    if (e == player) continue;
                    e.addEffect(new MobEffectInstance(
                            MobEffects.WEAKNESS, 60, 2, false, false));
                }
            }

            // 创造模式飞行（不覆盖玩家原有的飞行权限）
            if (!player.isCreative() && !player.isSpectator()) {
                if (!player.getAbilities().mayfly) {
                    player.getAbilities().mayfly = true;
                    player.onUpdateAbilities();
                }

                // 处于飞行状态时每 5 秒掉 1 点血
                if (player.getAbilities().flying && player.tickCount % AUTHORITY_FLIGHT_TICK == 0) {
                    float newHp = player.getHealth() - 1.0F;
                    if (newHp > 0.0F) {
                        player.setHealth(newHp);
                    }
                }
            }
        } else {
            // 关掉天威时收回飞行权限（仅当不是创造/旁观模式）
            if (!player.isCreative() && !player.isSpectator()
                    && player.getAbilities().mayfly) {
                player.getAbilities().mayfly = false;
                player.getAbilities().flying = false;
                player.onUpdateAbilities();
            }
        }
    }

    // ============ 不朽：免疫一切死亡 ============

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        if (!CuriosUtil.hasDecisionEquipped(player)) return;
        if (!EdictData.isEdictActive(player)) return;
        if (!EdictData.isEffectOn(player, EdictData.EFFECT_IMMORTAL)) return;

        // 取消死亡
        event.setCanceled(true);

        // 若掉入虚空，传送回安全位置
        if (player.getY() < player.level().getMinBuildHeight() - 10) {
            player.teleportTo(player.serverLevel(), 0.5, 80.0, 0.5, player.getYRot(), player.getXRot());
        }

        // 恢复生命
        player.setHealth(player.getMaxHealth());
        player.deathTime = 0;
        player.hurtTime = 0;
        player.invulnerableTime = 60;
        player.clearFire();

        // 抗性提升 V（5 秒）
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 100, 4, false, false));
        // 力量 X（10 秒）
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 200, 9, false, false));
        // 瞬间治疗 X（15 秒持续）
        player.addEffect(new MobEffectInstance(MobEffects.HEAL, 300, 9, false, false));

        player.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
                "message.gazeofthepantheon.edict.immortal"));
    }

    // ============ 破败 + 殁亡：玩家攻击时生效 ============

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingHurt(LivingHurtEvent event) {
        // 只处理"玩家攻击目标"的情况
        if (!(event.getSource().getEntity() instanceof ServerPlayer attacker)) return;
        LivingEntity target = event.getEntity();
        if (target == attacker) return;

        if (!CuriosUtil.hasDecisionEquipped(attacker)) return;
        if (!EdictData.isEdictActive(attacker)) return;

        // 殁亡：目标血量 ≤ 20% 时处决
        if (EdictData.isEffectOn(attacker, EdictData.EFFECT_PERISH)) {
            float threshold = target.getMaxHealth() * 0.2F;
            if (target.getHealth() <= threshold) {
                event.setCanceled(true);
                dealTrueDamage(target, Float.MAX_VALUE, event.getSource());
                return;
            }
        }

        // 破败：将伤害转为真伤
        if (EdictData.isEffectOn(attacker, EdictData.EFFECT_RUIN)) {
            event.setCanceled(true);
            dealTrueDamage(target, event.getAmount(), event.getSource());
        }
    }

    // ============ 制裁：受击返还真伤 ============

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onPlayerHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (event.getAmount() <= 0) return;
        if (!CuriosUtil.hasDecisionEquipped(player)) return;
        if (!EdictData.isEdictActive(player)) return;
        if (!EdictData.isEffectOn(player, EdictData.EFFECT_SANCTION)) return;

        // 找到攻击者
        var attackerEntity = event.getSource().getEntity();
        if (!(attackerEntity instanceof LivingEntity attacker)) return;
        if (attacker == player) return;

        float reflected = event.getAmount() * 50.0F;
        // 延迟到下一 tick 造成反伤，避免与当前事件冲突
        var target = attacker;
        var source = player.damageSources().magic();
        var level = player.serverLevel();
        level.getServer().execute(() -> {
            if (target.isAlive()) {
                dealTrueDamage(target, reflected, source);
            }
        });
    }
}