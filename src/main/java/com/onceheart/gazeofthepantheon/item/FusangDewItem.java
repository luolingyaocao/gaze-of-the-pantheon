package com.onceheart.gazeofthepantheon.item;

import com.onceheart.gazeofthepantheon.util.CuriosUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class FusangDewItem extends Item {

    public FusangDewItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide) {
            return InteractionResultHolder.success(stack);
        }

        if (CuriosUtil.convertXiheWrathToKindness(player)) {
            if (!player.isCreative()) {
                stack.shrink(1);
            }
            player.sendSystemMessage(Component.translatable(
                    "message.gazeofthepantheon.fusang_dew.used"));
            return InteractionResultHolder.success(stack);
        } else {
            player.sendSystemMessage(Component.translatable(
                    "message.gazeofthepantheon.fusang_dew.failed"));
            return InteractionResultHolder.fail(stack);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.gazeofthepantheon.fusang_dew.desc")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.gazeofthepantheon.fusang_dew.use")
                .withStyle(ChatFormatting.GOLD));
    }
}