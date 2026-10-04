package com.onceheart.gazeofthepantheon.event;

import com.onceheart.gazeofthepantheon.GazeOfThePantheon;
import com.onceheart.gazeofthepantheon.util.CuriosUtil;
import com.onceheart.gazeofthepantheon.util.EdictData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
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

    // ============ 登录 / 重生：刷新激活缓存 ============

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        MinecraftServer server = player.getServer();
        if (server == null) return;
        // 延后一 tick，确保 Curios 栏位已加载完毕
        server.execute(() -> DivineSaveHandler.refreshActiveCache(player));
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        MinecraftServer server = player.getServer();
        if (server == null) return;
        server.execute(() -> DivineSaveHandler.refreshActiveCache(player));
    }

    // ============ 每 tick：tick 兜底 + 天威 ============

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer player)) return;

        // tick 兜底：血量归零或 NaN 时手动拉回
        float hp = player.getHealth();
        if (Float.isNaN(hp) || hp <= 0.0F) {
            boolean immortal = DivineSaveHandler.isImmortalActive(player);
            boolean kindness = DivineSaveHandler.isKindnessActive(player);
            // 不朽：isDeadOrDying 被 Mixin 强制 false，直接回血
            // 善意：只在未进入死亡流程时回血（进了的话 LivingDeathEvent 会拦）
            if (immortal || (kindness && !player.isDeadOrDying())) {
                player.setHealth(player.getMaxHealth());
                player.deathTime = 0;
                player.hurtTime = 0;
            }
        }

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
        if (event.isCanceled()) return;

        if (!DivineSaveHandler.isImmortalActive(player)) return;

        event.setCanceled(true);
        DivineSaveHandler.applyImmortalSave(player);
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