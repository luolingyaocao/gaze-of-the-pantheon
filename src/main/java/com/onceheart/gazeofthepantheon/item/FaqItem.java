package com.onceheart.gazeofthepantheon.item;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 「跟你爆了」。
 *
 * 右键：以自己为球心，325 格球半径内的所有玩家全部被 kick（含创造/旁观/OP）。
 * 使用者本人豁免。
 * 跨维度只按坐标算——别的维度只要坐标在这个球里，一样踢。
 */
public class FaqItem extends Item {

    /** 踢人半径（球） */
    private static final double RADIUS = 325.0D;
    private static final double RADIUS_SQ = RADIUS * RADIUS;

    public FaqItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide) {
            return InteractionResultHolder.success(stack);
        }

        if (!(player instanceof ServerPlayer self)) {
            return InteractionResultHolder.pass(stack);
        }

        MinecraftServer server = self.getServer();
        if (server == null) {
            return InteractionResultHolder.pass(stack);
        }

        double sx = self.getX();
        double sy = self.getY();
        double sz = self.getZ();

        for (ServerPlayer target : server.getPlayerList().getPlayers()) {
            if (target == self) continue;

            double dx = target.getX() - sx;
            double dy = target.getY() - sy;
            double dz = target.getZ() - sz;
            if (dx * dx + dy * dy + dz * dz <= RADIUS_SQ) {
                target.connection.disconnect(
                        Component.translatable("multiplayer.disconnect.kicked"));
            }
        }

        return InteractionResultHolder.success(stack);
    }
}