package com.onceheart.gazeofthepantheon.item;

import com.onceheart.gazeofthepantheon.util.CuriosUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.List;

public class AchillesItem extends Item implements ICurioItem {

    public static final String NBT_BLESSED = "Blessed";

    public AchillesItem(Properties properties) {
        super(properties);
    }

    public static boolean isBlessed(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.getBoolean(NBT_BLESSED);
    }

    public static void setBlessed(ItemStack stack, boolean blessed) {
        stack.getOrCreateTag().putBoolean(NBT_BLESSED, blessed);
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable(isBlessed(stack)
                ? "item.gazeofthepantheon.achilles_kindness"
                : "item.gazeofthepantheon.achilles_wrath");
    }

    @Override
    public boolean canEquip(SlotContext slotContext, ItemStack stack) {
        if (!"gaze".equals(slotContext.identifier())) return false;
        if (slotContext.entity() instanceof Player player) {
            return !CuriosUtil.hasDeityEquipped(player, AchillesItem.class);
        }
        return true;
    }

    @Override
    public boolean canUnequip(SlotContext slotContext, ItemStack stack) {
        if (isBlessed(stack)) return true;
        if (slotContext.entity() instanceof Player player) {
            return player.isCreative();
        }
        return true;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide) {
            return InteractionResultHolder.success(stack);
        }

        if (CuriosUtil.hasDeityEquipped(player, AchillesItem.class)) {
            return InteractionResultHolder.fail(stack);
        }

        if (CuriosUtil.tryEquipToGaze(player, stack.copy())) {
            if (!player.isCreative()) {
                stack.shrink(1);
            }
            return InteractionResultHolder.success(stack);
        }
        return InteractionResultHolder.fail(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        if (isBlessed(stack)) {
            tooltip.add(Component.translatable("tooltip.gazeofthepantheon.achilles_kindness.desc")
                    .withStyle(ChatFormatting.GOLD));
            tooltip.add(Component.translatable("tooltip.gazeofthepantheon.achilles_kindness.effect")
                    .withStyle(ChatFormatting.YELLOW));
        } else {
            tooltip.add(Component.translatable("tooltip.gazeofthepantheon.achilles_wrath.desc")
                    .withStyle(ChatFormatting.DARK_RED));
            tooltip.add(Component.translatable("tooltip.gazeofthepantheon.achilles_wrath.effect")
                    .withStyle(ChatFormatting.RED));
            tooltip.add(Component.translatable("tooltip.gazeofthepantheon.achilles_wrath.locked")
                    .withStyle(ChatFormatting.GRAY));
        }
    }
}