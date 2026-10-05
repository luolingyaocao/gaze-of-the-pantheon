package com.onceheart.gazeofthepantheon.item;

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

public class MoonMirrorItem extends Item {

    private static final String[] MOON_PHASE_KEYS = {
            "moonphase.gazeofthepantheon.full",
            "moonphase.gazeofthepantheon.waning_gibbous",
            "moonphase.gazeofthepantheon.third_quarter",
            "moonphase.gazeofthepantheon.waning_crescent",
            "moonphase.gazeofthepantheon.new_moon",
            "moonphase.gazeofthepantheon.waxing_crescent",
            "moonphase.gazeofthepantheon.first_quarter",
            "moonphase.gazeofthepantheon.waxing_gibbous"
    };

    public MoonMirrorItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide) {
            return InteractionResultHolder.success(stack);
        }

        int phase = level.getMoonPhase();
        int daysUntilFull = (phase == 0) ? 8 : (8 - phase);

        player.sendSystemMessage(Component.translatable(
                "message.gazeofthepantheon.moon_mirror.phase",
                Component.translatable(MOON_PHASE_KEYS[phase])));
        player.sendSystemMessage(Component.translatable(
                "message.gazeofthepantheon.moon_mirror.next_full",
                daysUntilFull));

        return InteractionResultHolder.success(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.gazeofthepantheon.moon_mirror.desc")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.gazeofthepantheon.moon_mirror.use")
                .withStyle(ChatFormatting.AQUA));
    }
}