package com.onceheart.gazeofthepantheon.mixin;

import com.onceheart.gazeofthepantheon.event.DivineSaveHandler;
import com.onceheart.gazeofthepantheon.util.EdictData;
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
 * 关键：不朽激活时，getHealth / getMaxHealth 永远返回冻结满血，
 * 不看 entityData 里的实际值——因为 AV 会把 entityData 锁成 1.0（半颗心），
 * 若按"值 <= 0 才覆盖"判断，就会漏过这种情况。
 */
@Mixin(value = LivingEntity.class, priority = Integer.MAX_VALUE)
public abstract class MixinLivingEntity {

    /**
     * 拦截 getHealth。
     * 不朽时永远返回冻结满血，无视 entityData 被任何 mod 写成什么。
     */
    @Inject(method = "getHealth", at = @At("HEAD"), cancellable = true)
    private void gaze$forceRealHealth(CallbackInfoReturnable<Float> cir) {
        if (!((Object) this instanceof ServerPlayer player)) return;
        if (!DivineSaveHandler.isImmortalNow(player)) return;
        cir.setReturnValue(EdictData.getImmortalMax(player));
    }

    /**
     * 拦截 getMaxHealth。不朽时永远返回冻结满血。
     */
    @Inject(method = "getMaxHealth", at = @At("HEAD"), cancellable = true)
    private void gaze$forceImmortalMax(CallbackInfoReturnable<Float> cir) {
        if (!((Object) this instanceof ServerPlayer player)) return;
        if (!DivineSaveHandler.isImmortalNow(player)) return;
        cir.setReturnValue(EdictData.getImmortalMax(player));
    }

    /**
     * 拦截 setHealth。
     * 不朽激活时：
     * - NaN / Infinity → 取消
     * - health <= 0 → 取消
     * - health > 0 且 < 满血 → 也取消（不让血量被减小）
     */
    @Inject(method = "setHealth", at = @At("HEAD"), cancellable = true)
    private void gaze$protectSetHealth(float health, CallbackInfo ci) {
        if (!((Object) this instanceof ServerPlayer player)) return;
        if (!DivineSaveHandler.isImmortalNow(player)) return;

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
     */
    @Inject(method = "tickDeath", at = @At("HEAD"), cancellable = true)
    private void gaze$protectTickDeath(CallbackInfo ci) {
        if (!((Object) this instanceof ServerPlayer player)) return;
        if (!DivineSaveHandler.isImmortalNow(player)) return;

        ci.cancel();
        player.deathTime = 0;
    }

    /**
     * 拦截 isDeadOrDying。不朽时永远返回 false。
     */
    @Inject(method = "isDeadOrDying", at = @At("HEAD"), cancellable = true)
    private void gaze$protectIsDeadOrDying(CallbackInfoReturnable<Boolean> cir) {
        if (!((Object) this instanceof ServerPlayer player)) return;
        if (!DivineSaveHandler.isImmortalNow(player)) return;
        cir.setReturnValue(false);
    }
}