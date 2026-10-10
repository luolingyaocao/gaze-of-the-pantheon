package com.onceheart.gazeofthepantheon.network;

import com.onceheart.gazeofthepantheon.client.StopClientState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 服务端 → 客户端：同步「是否被神行压制」。
 *
 * 只发状态变化，不是每 tick 发。
 * 客户端收到后写入 StopClientState，被键盘 / 鼠标 mixin 读取。
 */
public class StopShieldPacket {

    private final boolean suppressed;

    public StopShieldPacket(boolean suppressed) {
        this.suppressed = suppressed;
    }

    public static void encode(StopShieldPacket msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.suppressed);
    }

    public static StopShieldPacket decode(FriendlyByteBuf buf) {
        return new StopShieldPacket(buf.readBoolean());
    }

    public static void handle(StopShieldPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (ctx.get().getDirection().getReceptionSide().isClient()) {
                StopClientState.setSuppressed(msg.suppressed);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}