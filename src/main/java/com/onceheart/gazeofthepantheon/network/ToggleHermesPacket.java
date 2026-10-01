package com.onceheart.gazeofthepantheon.network;

import com.onceheart.gazeofthepantheon.event.HermesEventHandler;
import com.onceheart.gazeofthepantheon.util.CuriosUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ToggleHermesPacket {

    public ToggleHermesPacket() {
    }

    public static void encode(ToggleHermesPacket msg, FriendlyByteBuf buf) {
    }

    public static ToggleHermesPacket decode(FriendlyByteBuf buf) {
        return new ToggleHermesPacket();
    }

    public static void handle(ToggleHermesPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            if (CuriosUtil.findHermesKindness(player).isEmpty()) return;

            int newGear = HermesEventHandler.cycleGear(player);
            player.sendSystemMessage(Component.translatable(
                    "message.gazeofthepantheon.hermes.gear_changed", newGear));
        });
        ctx.get().setPacketHandled(true);
    }
}