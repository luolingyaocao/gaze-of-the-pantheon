package com.onceheart.gazeofthepantheon.item;

import com.onceheart.gazeofthepantheon.worldgen.ModDimensions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class TestTpUnfixedHeavenItem extends Item {

    public TestTpUnfixedHeavenItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide) {
            return InteractionResultHolder.sidedSuccess(stack, true);
        }
        if (!(player instanceof ServerPlayer sp)) {
            return InteractionResultHolder.pass(stack);
        }

        ServerLevel target = sp.server.getLevel(ModDimensions.THE_UNFIXED_HEAVEN);
        if (target == null || sp.level() == target) {
            return InteractionResultHolder.pass(stack);
        }

        sp.teleportTo(target, 0.5, 100.0, 0.5, sp.getYRot(), sp.getXRot());
        return InteractionResultHolder.success(stack);
    }
}