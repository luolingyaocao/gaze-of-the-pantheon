package com.onceheart.gazeofthepantheon.mixin;

import com.onceheart.gazeofthepantheon.event.DivineSaveHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 不朽的字节码层防护（Entity 层）。
 *
 * 拦截点：
 * - setRemoved：拦 removalReason == KILLED（所有移除路径的汇聚点，final 方法）
 * - isAlive：永远返回 true
 * - isRemoved：永远返回 false（setRemoved 被拦后的兜底）
 */
@Mixin(Entity.class)
public abstract class MixinEntity {

    /**
     * 拦截 setRemoved。
     *
     * setRemoved 是 final 且被所有移除路径调用（remove / removePlayerImmediately /
     * 直接调），是唯一需要拦的方法。只拦 KILLED：
     * - DISCARDED / UNLOADED_TO_CHUNK / UNLOADED_WITH_PLAYER / CHANGED_DIMENSION
     *   都是正常游戏流程，拦了会出大问题（跨维度会直接废）。
     */
    @Inject(method = "setRemoved", at = @At("HEAD"), cancellable = true)
    private void gaze$protectSetRemoved(Entity.RemovalReason reason, CallbackInfo ci) {
        if (reason != Entity.RemovalReason.KILLED) return;
        if (!((Object) this instanceof ServerPlayer player)) return;
        if (!DivineSaveHandler.isImmortalActive(player)) return;
        ci.cancel();
    }

    /**
     * 拦截 isAlive。原版是 !this.isRemoved()，强制返回 true，
     * 让那些用 isAlive 判断后执行清理的模组认为不朽玩家始终存活。
     */
    @Inject(method = "isAlive", at = @At("HEAD"), cancellable = true)
    private void gaze$protectIsAlive(CallbackInfoReturnable<Boolean> cir) {
        if (!((Object) this instanceof ServerPlayer player)) return;
        if (!DivineSaveHandler.isImmortalActive(player)) return;
        cir.setReturnValue(true);
    }

    /**
     * 拦截 isRemoved。
     * setRemoved 被拦成功后 removalReason 保持 null，isRemoved 原版就会返回 false。
     * 这里作为兜底——万一某模组反射写了 removalReason 字段，这层还能挡后续读取。
     */
    @Inject(method = "isRemoved", at = @At("HEAD"), cancellable = true)
    private void gaze$protectIsRemoved(CallbackInfoReturnable<Boolean> cir) {
        if (!((Object) this instanceof ServerPlayer player)) return;
        if (!DivineSaveHandler.isImmortalActive(player)) return;
        cir.setReturnValue(false);
    }
}