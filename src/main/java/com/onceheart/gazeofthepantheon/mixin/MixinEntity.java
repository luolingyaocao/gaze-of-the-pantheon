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
 */
@Mixin(value = Entity.class, priority = Integer.MAX_VALUE)
public abstract class MixinEntity {

    @Inject(method = "setRemoved", at = @At("HEAD"), cancellable = true)
    private void gaze$protectSetRemoved(Entity.RemovalReason reason, CallbackInfo ci) {
        if (reason != Entity.RemovalReason.KILLED) return;
        if (!((Object) this instanceof ServerPlayer player)) return;
        if (!DivineSaveHandler.isImmortalNow(player)) return;
        ci.cancel();
    }

    @Inject(method = "isAlive", at = @At("HEAD"), cancellable = true)
    private void gaze$protectIsAlive(CallbackInfoReturnable<Boolean> cir) {
        if (!((Object) this instanceof ServerPlayer player)) return;
        if (!DivineSaveHandler.isImmortalNow(player)) return;
        cir.setReturnValue(true);
    }

    @Inject(method = "isRemoved", at = @At("HEAD"), cancellable = true)
    private void gaze$protectIsRemoved(CallbackInfoReturnable<Boolean> cir) {
        if (!((Object) this instanceof ServerPlayer player)) return;
        if (!DivineSaveHandler.isImmortalNow(player)) return;
        cir.setReturnValue(false);
    }
}