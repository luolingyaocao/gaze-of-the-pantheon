package com.onceheart.gazeofthepantheon.network;

import com.onceheart.gazeofthepantheon.client.EdictScreen;
import com.onceheart.gazeofthepantheon.util.EdictData;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 双向包：
 * - 客户端 → 服务端：请求切换指定效果的开关
 * - 服务端 → 客户端：同步当前开关的完整掩码
 */
public class ToggleEdictEffectPacket {

    /** -1 表示同步请求；其他值表示要切换的效果位 */
    private final int effect;
    /** 完整掩码，仅在服务端→客户端同步时有效 */
    private final int mask;

    public ToggleEdictEffectPacket(int effect) {
        this.effect = effect;
        this.mask = 0;
    }

    public ToggleEdictEffectPacket(int effect, int mask) {
        this.effect = effect;
        this.mask = mask;
    }

    public static void encode(ToggleEdictEffectPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.effect);
        buf.writeInt(msg.mask);
    }

    public static ToggleEdictEffectPacket decode(FriendlyByteBuf buf) {
        return new ToggleEdictEffectPacket(buf.readInt(), buf.readInt());
    }

    public static void handle(ToggleEdictEffectPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (ctx.get().getDirection().getReceptionSide().isServer()) {
                // 服务端：执行切换
                ServerPlayer player = ctx.get().getSender();
                if (player == null) return;

                EdictData.toggleEffect(player, msg.effect);
                int newMask = EdictData.getEffects(player);

                // 回传给客户端，让按钮状态与服务端一致
                ModNetwork.CHANNEL.send(
                        net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player),
                        new ToggleEdictEffectPacket(0, newMask));
            } else {
                // 客户端：更新 UI 掩码
                if (FMLEnvironment.dist == Dist.CLIENT) {
                    handleClient(msg);
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }

    private static void handleClient(ToggleEdictEffectPacket msg) {
        if (Minecraft.getInstance().screen instanceof EdictScreen screen) {
            screen.updateMask(msg.mask);
        }
    }
}