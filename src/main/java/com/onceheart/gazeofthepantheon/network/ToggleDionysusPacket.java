package com.onceheart.gazeofthepantheon.network;

import com.onceheart.gazeofthepantheon.event.DionysusEventHandler;
import com.onceheart.gazeofthepantheon.util.CuriosUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ToggleDionysusPacket {

    public ToggleDionysusPacket() {
    }

    public static void encode(ToggleDionysusPacket msg, FriendlyByteBuf buf) {
    }

    public static ToggleDionysusPacket decode(FriendlyByteBuf buf) {
        return new ToggleDionysusPacket();
    }

    public static void handle(ToggleDionysusPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            if (CuriosUtil.findDionysusKindness(player).isEmpty()) return;

            boolean on = DionysusEventHandler.toggle(player);
            player.sendSystemMessage(Component.translatable(
                    on ? "message.gazeofthepantheon.dionysus.toggle_on"
                            : "message.gazeofthepantheon.dionysus.toggle_off"));
        });
        ctx.get().setPacketHandled(true);
    }
}