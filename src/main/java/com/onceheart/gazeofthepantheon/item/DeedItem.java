package com.onceheart.gazeofthepantheon.item;

import com.onceheart.gazeofthepantheon.util.CuriosUtil;
import com.onceheart.gazeofthepantheon.util.EdictData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 成事在人。
 * 放入必行敕令后，五种效果才会真正生效。
 * 不占决策栏位，而是存在于玩家的 persistentData 里。
 */
public class DeedItem extends Item {

    public DeedItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide) {
            return InteractionResultHolder.success(stack);
        }

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.fail(stack);
        }

        // 决策栏位未装备必行敕令
        if (!CuriosUtil.hasDecisionEquipped(player)) {
            player.sendSystemMessage(Component.translatable(
                    "message.gazeofthepantheon.deed.no_edict"));
            return InteractionResultHolder.fail(stack);
        }

        // 已经放入过了
        if (EdictData.hasDeed(serverPlayer)) {
            player.sendSystemMessage(Component.translatable(
                    "message.gazeofthepantheon.deed.already"));
            return InteractionResultHolder.fail(stack);
        }

        // 放入成事在人
        EdictData.setHasDeed(serverPlayer, true);
        if (!player.isCreative()) {
            stack.shrink(1);
        }

        player.sendSystemMessage(Component.translatable(
                "message.gazeofthepantheon.deed.placed"));
        return InteractionResultHolder.success(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.gazeofthepantheon.deed.desc")
                .withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("tooltip.gazeofthepantheon.deed.use")
                .withStyle(ChatFormatting.YELLOW));
    }
}