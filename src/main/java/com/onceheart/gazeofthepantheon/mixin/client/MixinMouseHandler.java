package com.onceheart.gazeofthepantheon.mixin.client;

import com.onceheart.gazeofthepantheon.client.StopClientState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 客户端鼠标拦截：被神行压制时，吞掉左右键、滚轮。
 *
 * 不拦 onMove（视角转动），这是设计约定。
 * 有 GUI 打开时不拦。
 */
@Mixin(value = MouseHandler.class, priority = Integer.MAX_VALUE)
public class MixinMouseHandler {

    @Inject(method = "onPress", at = @At("HEAD"), cancellable = true)
    private void gaze$blockPress(long windowPointer, int button, int action,
                                 int modifiers, CallbackInfo ci) {
        if (Minecraft.getInstance().screen != null) return;
        if (!StopClientState.isSuppressed()) return;
        if (action == GLFW.GLFW_RELEASE) return;
        ci.cancel();
    }

    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void gaze$blockScroll(long windowPointer, double xOffset, double yOffset,
                                  CallbackInfo ci) {
        if (Minecraft.getInstance().screen != null) return;
        if (!StopClientState.isSuppressed()) return;
        ci.cancel();
    }
}