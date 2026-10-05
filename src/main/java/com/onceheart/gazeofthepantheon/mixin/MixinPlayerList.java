package com.onceheart.gazeofthepantheon.mixin;

import com.onceheart.gazeofthepantheon.event.DivineSaveHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 拦截 PlayerList.remove(ServerPlayer)。
 */
@Mixin(value = PlayerList.class, priority = Integer.MAX_VALUE)
public abstract class MixinPlayerList {

    @Inject(
            method = "remove(Lnet/minecraft/server/level/ServerPlayer;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void gaze$protectRemove(ServerPlayer player, CallbackInfo ci) {
        if (player == null) return;
        if (!DivineSaveHandler.isImmortalNow(player)) return;
        if (player.hasDisconnected()) return;
        ci.cancel();
    }
}