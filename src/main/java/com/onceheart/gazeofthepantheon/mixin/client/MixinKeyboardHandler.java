package com.onceheart.gazeofthepantheon.mixin.client;

import com.onceheart.gazeofthepantheon.client.StopClientState;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 客户端键盘拦截：被神行压制时，吞掉除 ESC / T / / 之外的所有按键。
 *
 * - 有 GUI 打开时（聊天、背包、ESC 菜单）不拦，否则会锁死玩家
 * - 只拦「按下」和「重复」，放行「释放」，避免按键卡住
 * - ESC / T / / 为白名单：保留菜单、聊天、指令
 */
@Mixin(value = KeyboardHandler.class, priority = Integer.MAX_VALUE)
public class MixinKeyboardHandler {

    @Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
    private void gaze$blockInput(long windowPointer, int key, int scanCode,
                                 int action, int modifiers, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null) return;

        if (!StopClientState.isSuppressed()) return;

        if (action == GLFW.GLFW_RELEASE) return;

        if (key == GLFW.GLFW_KEY_ESCAPE) return;
        if (key == GLFW.GLFW_KEY_T) return;
        if (key == GLFW.GLFW_KEY_SLASH) return;

        ci.cancel();
    }
}