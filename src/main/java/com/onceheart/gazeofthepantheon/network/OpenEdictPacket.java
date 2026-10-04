package com.onceheart.gazeofthepantheon.network;

import com.onceheart.gazeofthepantheon.menu.EdictMenu;
import com.onceheart.gazeofthepantheon.util.CuriosUtil;
import com.onceheart.gazeofthepantheon.util.EdictData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkHooks;

import java.util.function.Supplier;

public class OpenEdictPacket {

    public OpenEdictPacket() {
    }

    public static void encode(OpenEdictPacket msg, FriendlyByteBuf buf) {
    }

    public static OpenEdictPacket decode(FriendlyByteBuf buf) {
        return new OpenEdictPacket();
    }

    public static void handle(OpenEdictPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            if (!CuriosUtil.hasDecisionEquipped(player)) return;

            int mask = EdictData.getEffects(player);
            boolean isActive = EdictData.isEdictActive(player);

            NetworkHooks.openScreen(player,
                    new SimpleMenuProvider(
                            (containerId, inv, p) -> new EdictMenu(containerId, inv),
                            Component.translatable("gui.gazeofthepantheon.edict.title")),
                    buf -> {
                        buf.writeInt(mask);
                        buf.writeBoolean(isActive);
                    });
        });
        ctx.get().setPacketHandled(true);
    }
}