package com.onceheart.gazeofthepantheon.mixin;

import com.onceheart.gazeofthepantheon.event.DivineSaveHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 不朽的字节码层防护（LivingEntity 层）。
 *
 * 拦截点：
 * - setHealth：拦住把血量设成 0/负数/NaN/Infinity 的调用
 * - tickDeath：拦住 deathTime 被拨到 20 后走完死亡流程导致的 remove(KILLED)
 * - isDeadOrDying：只要不朽激活，永远返回 false
 *
 * 注：ServerPlayer 覆写了 die()，LivingEntity.die 的 Mixin 对玩家不生效，
 *     玩家死亡路径由 Forge 的 LivingDeathEvent 拦截。
 */
@Mixin(LivingEntity.class)
public abstract class MixinLivingEntity {

    /**
     * 拦截 setHealth。
     * 不朽激活时：
     * - NaN / Infinity → 直接取消调用
     * - health <= 0 → 取消调用，如果当前血量也已 <= 0 则补到 1.0
     */
    @Inject(method = "setHealth", at = @At("HEAD"), cancellable = true)
    private void gaze$protectSetHealth(float health, CallbackInfo ci) {
        if (!((Object) this instanceof ServerPlayer player)) return;
        if (!DivineSaveHandler.isImmortalActive(player)) return;

        if (Float.isNaN(health) || Float.isInfinite(health)) {
            ci.cancel();
            return;
        }

        if (health <= 0.0F) {
            ci.cancel();
            if (player.getHealth() <= 0.0F) {
                player.setHealth(1.0F);
            }
        }
    }

    /**
     * 拦截 tickDeath。
     * 原版：deathTime++，如果 >= 20 则 remove(KILLED)。
     * 不朽激活时取消 tickDeath 并把 deathTime 归零，避免模组直接拨 deathTime 触发死亡。
     * 顺便把血量拉满，兜住那些绕过 setHealth 的路径。
     */
    @Inject(method = "tickDeath", at = @At("HEAD"), cancellable = true)
    private void gaze$protectTickDeath(CallbackInfo ci) {
        if (!((Object) this instanceof ServerPlayer player)) return;
        if (!DivineSaveHandler.isImmortalActive(player)) return;

        ci.cancel();
        player.deathTime = 0;

        float hp = player.getHealth();
        if (Float.isNaN(hp) || hp <= 0.0F) {
            player.setHealth(player.getMaxHealth());
        }
    }

    /**
     * 拦截 isDeadOrDying。不朽激活时永远返回 false。
     * 让 tickDeath 的调用条件失效，也让模组的死亡清理逻辑落空。
     */
    @Inject(method = "isDeadOrDying", at = @At("HEAD"), cancellable = true)
    private void gaze$protectIsDeadOrDying(CallbackInfoReturnable<Boolean> cir) {
        if (!((Object) this instanceof ServerPlayer player)) return;
        if (!DivineSaveHandler.isImmortalActive(player)) return;
        cir.setReturnValue(false);
    }
}