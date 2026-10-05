package com.onceheart.gazeofthepantheon.event;

import com.onceheart.gazeofthepantheon.GazeOfThePantheon;
import com.onceheart.gazeofthepantheon.effect.ModEffects;
import com.onceheart.gazeofthepantheon.util.CuriosUtil;
import com.onceheart.gazeofthepantheon.util.EdictData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Mod.EventBusSubscriber(modid = GazeOfThePantheon.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class DionysusEventHandler {

    private static final String NBT_TOGGLE = "gazeofthepantheon_dionysus_toggle";

    /** 酩酊：每 20 秒触发一次 */
    private static final int WRATH_INTERVAL = 400;
    /** 酩酊：反胃持续 10 秒 */
    private static final int WRATH_CONFUSION_DURATION = 200;
    /** 酩酊：反胃等级 II（amplifier = 1） */
    private static final int WRATH_CONFUSION_AMPLIFIER = 1;

    /** 澄明：每秒扫一次 */
    private static final int CLARITY_INTERVAL = 20;

    /** 醉酒：造成伤害 ×1.10 */
    private static final float INTOX_DEALT_MULT = 1.10F;
    /** 醉酒：受到伤害 ×1.05 */
    private static final float INTOX_TAKEN_MULT = 1.05F;

    /** 澄明白名单：本 mod 用过的全部效果（含负面）。 */
    private static Set<MobEffect> whitelist;

    private static Set<MobEffect> whitelist() {
        if (whitelist == null) {
            Set<MobEffect> set = new HashSet<>();
            // 敕令·不朽
            set.add(MobEffects.DAMAGE_RESISTANCE);
            set.add(MobEffects.DAMAGE_BOOST);
            set.add(MobEffects.HEAL);
            // 羲和·恩泽
            set.add(MobEffects.NIGHT_VISION);
            // 善意救赎
            set.add(MobEffects.DARKNESS);
            set.add(MobEffects.MOVEMENT_SLOWDOWN);
            // 酩酊诅咒
            set.add(MobEffects.CONFUSION);
            // 敕令·天威
            set.add(MobEffects.WEAKNESS);
            // 醉酒
            set.add(ModEffects.INTOXICATION.get());
            whitelist = set;
        }
        return whitelist;
    }

    // ============ I 键开关 ============

    public static boolean isToggleOn(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        if (!data.contains(NBT_TOGGLE)) return true; // 默认开
        return data.getBoolean(NBT_TOGGLE);
    }

    public static void setToggle(ServerPlayer player, boolean on) {
        player.getPersistentData().putBoolean(NBT_TOGGLE, on);
    }

    public static boolean toggle(ServerPlayer player) {
        boolean next = !isToggleOn(player);
        setToggle(player, next);
        return next;
    }

    // ============ 状态判断 ============

    public static boolean hasIntoxication(ServerPlayer player) {
        return player.hasEffect(ModEffects.INTOXICATION.get());
    }

    /** 佩戴了澄明（祝福版） */
    public static boolean isClarityEquipped(ServerPlayer player) {
        return !CuriosUtil.findDionysusKindness(player).isEmpty();
    }

    /** 澄明此刻是否生效：佩戴 + I 键开 + 敕令未激活 */
    public static boolean isClarityActive(ServerPlayer player) {
        if (!isClarityEquipped(player)) return false;
        if (!isToggleOn(player)) return false;
        if (EdictData.isEdictActive(player)) return false;
        return true;
    }

    // ============ Tick ============

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer player)) return;

        // ---- 酩酊：每 20 秒给 10 秒反胃 II（醉酒时不触发） ----
        if (!CuriosUtil.findDionysusWrath(player).isEmpty()
                && player.tickCount % WRATH_INTERVAL == 0
                && !hasIntoxication(player)) {
            player.addEffect(new MobEffectInstance(
                    MobEffects.CONFUSION, WRATH_CONFUSION_DURATION,
                    WRATH_CONFUSION_AMPLIFIER, false, true));
        }

        // ---- 澄明：每秒清 buff ----
        if (player.tickCount % CLARITY_INTERVAL == 0 && isClarityActive(player)) {
            clearForeignEffects(player);
        }
    }

    /** 清除不在白名单、且非原版 BENEFICIAL 的效果。 */
    private static void clearForeignEffects(ServerPlayer player) {
        Set<MobEffect> allowed = whitelist();
        List<MobEffect> toRemove = new ArrayList<>();

        for (MobEffectInstance inst : player.getActiveEffects()) {
            MobEffect effect = inst.getEffect();
            if (allowed.contains(effect)) continue;
            if (isVanillaBeneficial(effect)) continue;
            toRemove.add(effect);
        }

        for (MobEffect effect : toRemove) {
            player.removeEffect(effect);
        }
    }

    private static boolean isVanillaBeneficial(MobEffect effect) {
        ResourceLocation key = ForgeRegistries.MOB_EFFECTS.getKey(effect);
        if (key == null || !"minecraft".equals(key.getNamespace())) return false;
        return effect.getCategory() == MobEffectCategory.BENEFICIAL;
    }

    // ============ 醉酒：伤害修正（独立乘区） ============

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingHurt(LivingHurtEvent event) {
        // 造成伤害 ×1.10
        if (event.getSource().getEntity() instanceof ServerPlayer attacker
                && hasIntoxication(attacker)) {
            event.setAmount(event.getAmount() * INTOX_DEALT_MULT);
        }
        // 受到伤害 ×1.05
        if (event.getEntity() instanceof ServerPlayer victim
                && hasIntoxication(victim)) {
            event.setAmount(event.getAmount() * INTOX_TAKEN_MULT);
        }
    }
}