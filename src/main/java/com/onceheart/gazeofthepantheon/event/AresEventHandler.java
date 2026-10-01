package com.onceheart.gazeofthepantheon.event;

import com.onceheart.gazeofthepantheon.GazeOfThePantheon;
import com.onceheart.gazeofthepantheon.util.CuriosUtil;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = GazeOfThePantheon.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class AresEventHandler {

    // ============ 属性修饰符 UUID ============
    private static final UUID WRATH_ATTACK_DAMAGE_UUID = UUID.fromString("a4e50001-0001-0001-0001-000000000001");
    private static final UUID WRATH_ARMOR_UUID         = UUID.fromString("a4e50002-0002-0002-0002-000000000002");
    private static final UUID WRATH_ARMOR_TOUGH_UUID   = UUID.fromString("a4e50003-0003-0003-0003-000000000003");

    private static final UUID KIND_ATTACK_DAMAGE_UUID  = UUID.fromString("a4e51001-0001-0001-0001-000000000001");
    private static final UUID KIND_ARMOR_UUID          = UUID.fromString("a4e51002-0002-0002-0002-000000000002");
    private static final UUID KIND_ARMOR_TOUGH_UUID    = UUID.fromString("a4e51003-0003-0003-0003-000000000003");

    /** 中立生物白名单：佩戴纷争时直接敌对 */
    private static final Set<EntityType<?>> NEUTRAL_MOBS = Set.of(
            EntityType.ENDERMAN, EntityType.ZOMBIFIED_PIGLIN, EntityType.IRON_GOLEM,
            EntityType.SNOW_GOLEM, EntityType.WOLF, EntityType.LLAMA,
            EntityType.TRADER_LLAMA, EntityType.BEE, EntityType.PANDA,
            EntityType.POLAR_BEAR, EntityType.DOLPHIN, EntityType.GOAT,
            EntityType.PIGLIN, EntityType.SPIDER, EntityType.CAVE_SPIDER
    );

    /** 纷争：中立生物强制敌对的检测半径 */
    private static final double WRATH_AGGRO_RADIUS = 20.0D;

    // ============ 属性维护 ============
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer player)) return;

        boolean hasWrath = !CuriosUtil.findAresWrath(player).isEmpty();
        boolean hasKindness = !CuriosUtil.findAresKindness(player).isEmpty();

        if (hasWrath) {
            applyModifier(player, Attributes.ATTACK_DAMAGE, WRATH_ATTACK_DAMAGE_UUID,
                    "gazeofthepantheon.ares_wrath_attack", -0.3D, AttributeModifier.Operation.MULTIPLY_TOTAL);
            applyModifier(player, Attributes.ARMOR, WRATH_ARMOR_UUID,
                    "gazeofthepantheon.ares_wrath_armor", -0.5D, AttributeModifier.Operation.MULTIPLY_TOTAL);
            applyModifier(player, Attributes.ARMOR_TOUGHNESS, WRATH_ARMOR_TOUGH_UUID,
                    "gazeofthepantheon.ares_wrath_tough", -0.5D, AttributeModifier.Operation.MULTIPLY_TOTAL);
            if (player.tickCount % 20 == 0) forceNeutralMobsHostile(player);
        } else {
            removeModifier(player, Attributes.ATTACK_DAMAGE, WRATH_ATTACK_DAMAGE_UUID);
            removeModifier(player, Attributes.ARMOR, WRATH_ARMOR_UUID);
            removeModifier(player, Attributes.ARMOR_TOUGHNESS, WRATH_ARMOR_TOUGH_UUID);
        }

        if (hasKindness) {
            applyModifier(player, Attributes.ATTACK_DAMAGE, KIND_ATTACK_DAMAGE_UUID,
                    "gazeofthepantheon.ares_kind_attack", 0.5D, AttributeModifier.Operation.MULTIPLY_TOTAL);
            applyModifier(player, Attributes.ARMOR, KIND_ARMOR_UUID,
                    "gazeofthepantheon.ares_kind_armor", 0.25D, AttributeModifier.Operation.MULTIPLY_TOTAL);
            applyModifier(player, Attributes.ARMOR_TOUGHNESS, KIND_ARMOR_TOUGH_UUID,
                    "gazeofthepantheon.ares_kind_tough", 0.5D, AttributeModifier.Operation.MULTIPLY_TOTAL);
        } else {
            removeModifier(player, Attributes.ATTACK_DAMAGE, KIND_ATTACK_DAMAGE_UUID);
            removeModifier(player, Attributes.ARMOR, KIND_ARMOR_UUID);
            removeModifier(player, Attributes.ARMOR_TOUGHNESS, KIND_ARMOR_TOUGH_UUID);
        }
    }

    private static void applyModifier(ServerPlayer player, net.minecraft.world.entity.ai.attributes.Attribute attr,
                                      UUID uuid, String name, double amount,
                                      AttributeModifier.Operation op) {
        AttributeInstance instance = player.getAttribute(attr);
        if (instance == null) return;
        AttributeModifier existing = instance.getModifier(uuid);
        if (existing == null || existing.getAmount() != amount || existing.getOperation() != op) {
            if (existing != null) instance.removeModifier(uuid);
            instance.addTransientModifier(new AttributeModifier(uuid, name, amount, op));
        }
    }

    private static void removeModifier(ServerPlayer player, net.minecraft.world.entity.ai.attributes.Attribute attr, UUID uuid) {
        AttributeInstance instance = player.getAttribute(attr);
        if (instance != null && instance.getModifier(uuid) != null) instance.removeModifier(uuid);
    }

    // ============ 纷争：中立生物强制敌对 ============
    private static void forceNeutralMobsHostile(ServerPlayer player) {
        AABB area = player.getBoundingBox().inflate(WRATH_AGGRO_RADIUS);
        List<Mob> mobs = player.level().getEntitiesOfClass(Mob.class, area);
        for (Mob mob : mobs) {
            if (!NEUTRAL_MOBS.contains(mob.getType())) continue;
            if (!mob.canAttack(player)) continue;
            if (mob.getTarget() == player) continue;
            if (player.distanceToSqr(mob) > WRATH_AGGRO_RADIUS * WRATH_AGGRO_RADIUS) continue;
            mob.setTarget(player);
        }
    }

    // ============ 战佑：敌对生物索敌半径减半（核心实现） ============
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onLivingChangeTarget(LivingChangeTargetEvent event) {
        if (!(event.getEntity() instanceof Mob mob)) return;
        if (!(mob instanceof Enemy)) return;

        var newTarget = event.getNewTarget();
        if (!(newTarget instanceof ServerPlayer player)) return;

        // 玩家是否佩戴战佑
        if (CuriosUtil.findAresKindness(player).isEmpty()) return;

        double followRange = mob.getAttributeValue(Attributes.FOLLOW_RANGE);
        double halfRange = followRange / 2.0D;
        if (mob.distanceToSqr(player) > halfRange * halfRange) {
            event.setCanceled(true);
        }
    }

    // ============ 受伤倍率 ============
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (event.getAmount() <= 0) return;

        boolean hasWrath = !CuriosUtil.findAresWrath(player).isEmpty();
        boolean hasKindness = !CuriosUtil.findAresKindness(player).isEmpty();

        if (hasWrath) {
            event.setAmount(event.getAmount() * 1.5F);
        } else if (hasKindness) {
            event.setAmount(event.getAmount() * 0.5F);
        }
    }
}