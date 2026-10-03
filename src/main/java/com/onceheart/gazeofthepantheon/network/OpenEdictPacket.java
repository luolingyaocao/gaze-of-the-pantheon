package com.onceheart.gazeofthepantheon.network;

import com.onceheart.gazeofthepantheon.menu.EdictMenu;
import com.onceheart.gazeofthepantheon.util.CuriosUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkHooks;

import java.util.function.Supplier;

/**
 * 客户端 → 服务端：请求打开决策 UI。
 * 服务端验证决策栏位是否装备了必行敕令。
 */
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

            // 必须装备必行敕令才能打开
            if (!CuriosUtil.hasDecisionEquipped(player)) return;

            NetworkHooks.openScreen(player,
                    new SimpleMenuProvider(
                            (containerId, inv, p) -> new EdictMenu(containerId, inv),
                            Component.translatable("gui.gazeofthepantheon.edict.title")));
        });
        ctx.get().setPacketHandled(true);
    }
}