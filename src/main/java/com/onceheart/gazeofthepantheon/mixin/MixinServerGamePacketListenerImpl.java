package com.onceheart.gazeofthepantheon.mixin;

import com.onceheart.gazeofthepantheon.event.DivineSaveHandler;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundPlayerCombatKillPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;

/**
 * 拦截两类"客户端不该收到的包"。
 */
@Mixin(value = ServerGamePacketListenerImpl.class, priority = Integer.MAX_VALUE)
public abstract class MixinServerGamePacketListenerImpl {

    @Inject(
            method = "send(Lnet/minecraft/network/protocol/Packet;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void gaze$protectSendShort(Packet<?> packet, CallbackInfo ci) {
        ServerPlayer player = gaze$getPlayer((ServerGamePacketListenerImpl) (Object) this);
        if (gaze$shouldBlock(packet, player)) ci.cancel();
    }

    @Inject(
            method = "send(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketSendListener;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void gaze$protectSendLong(Packet<?> packet, PacketSendListener listener, CallbackInfo ci) {
        ServerPlayer player = gaze$getPlayer((ServerGamePacketListenerImpl) (Object) this);
        if (gaze$shouldBlock(packet, player)) ci.cancel();
    }

    private static boolean gaze$shouldBlock(Packet<?> packet, ServerPlayer player) {
        if (player == null) return false;
        if (!DivineSaveHandler.isImmortalNow(player)) return false;

        if (packet instanceof ClientboundRemoveEntitiesPacket removePacket) {
            int myId = player.getId();
            for (int id : removePacket.getEntityIds()) {
                if (id == myId) return true;
            }
            return false;
        }

        if (packet instanceof ClientboundPlayerCombatKillPacket) {
            return true;
        }

        return false;
    }

    private static ServerPlayer gaze$getPlayer(ServerGamePacketListenerImpl self) {
        try {
            Class<?> c = self.getClass();
            while (c != null && c != Object.class) {
                try {
                    Field f = c.getDeclaredField("player");
                    f.setAccessible(true);
                    Object v = f.get(self);
                    if (v instanceof ServerPlayer sp) return sp;
                } catch (NoSuchFieldException ignored) {
                }
                c = c.getSuperclass();
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}