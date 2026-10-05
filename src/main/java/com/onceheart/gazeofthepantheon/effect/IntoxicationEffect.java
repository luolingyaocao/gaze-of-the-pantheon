package com.onceheart.gazeofthepantheon.effect;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * 醉酒。
 *
 * 效果（+10% / +5% 伤害修正不在这里，见 DionysusEventHandler）：
 * - 每 10 秒扣除 1 颗心（2 点生命值）
 * - 免疫酩酊诅咒的反胃（在 DionysusEventHandler 里判断）
 * - 粒子颜色：黄色（由 MobEffect#getColor 决定）
 * - HUD 图标：需将原版 darkness 纹理复制到：
 *   assets/gazeofthepantheon/textures/mob_effect/intoxication.png
 */
public class IntoxicationEffect extends MobEffect {

    private static final int TICK_INTERVAL = 200;   // 10 秒
    private static final float DAMAGE_PER_TICK = 2.0F;  // 1 颗心

    public IntoxicationEffect() {
        super(MobEffectCategory.HARMFUL, 0xFFFF00);
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        if (!(entity instanceof ServerPlayer player)) return;
        player.hurt(player.damageSources().magic(), DAMAGE_PER_TICK);
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return duration % TICK_INTERVAL == 0;
    }
}