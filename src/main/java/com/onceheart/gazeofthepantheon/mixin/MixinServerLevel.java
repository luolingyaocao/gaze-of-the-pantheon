package com.onceheart.gazeofthepantheon.mixin;

import com.onceheart.gazeofthepantheon.worldgen.ModDimensions;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLevel.class)
public abstract class MixinServerLevel {

    /**
     * 鸿蒙人间：每 3 tick 跳过 1 次 setDayTime，
     * 平均每 tick 推进 2/3，一天 = 24000 / (2/3) = 36000 tick = 30 分钟。
     */
    @Redirect(method = "tickTime",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerLevel;setDayTime(J)V"))
    private void gaze$redirectSetDayTime(ServerLevel instance, long newTime) {
        if (instance.dimension().equals(ModDimensions.PRIMORDIAL_HUMAN_REALM)) {
            if (instance.getGameTime() % 3 == 0) {
                return;
            }
        }
        instance.setDayTime(newTime);
    }

    /**
     * 鸿蒙人间：完全取消天气循环推进。
     */
    @Inject(method = "advanceWeatherCycle", at = @At("HEAD"), cancellable = true)
    private void gaze$cancelWeatherCycle(CallbackInfo ci) {
        ServerLevel level = (ServerLevel) (Object) this;
        if (level.dimension().equals(ModDimensions.PRIMORDIAL_HUMAN_REALM)) {
            ci.cancel();
        }
    }
}