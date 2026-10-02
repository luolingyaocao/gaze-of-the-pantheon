package com.onceheart.gazeofthepantheon.item;

import com.onceheart.gazeofthepantheon.util.CuriosUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.List;

public class XiheItem extends Item implements ICurioItem {

    public static final String NBT_BLESSED = "Blessed";

    /** 装备诅咒版时给予的抗火持续时间（5 分钟 = 6000 tick） */
    private static final int WRATH_FIRE_RESIST_DURATION = 6000;

    public XiheItem(Properties properties) {
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
                ? "item.gazeofthepantheon.xihe_kindness"
                : "item.gazeofthepantheon.xihe_wrath");
    }

    @Override
    public boolean canEquip(SlotContext slotContext, ItemStack stack) {
        if (!"gaze".equals(slotContext.identifier())) return false;
        if (slotContext.entity() instanceof Player player) {
            return !CuriosUtil.hasDeityEquipped(player, XiheItem.class);
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

    /** 装备瞬间：如果装备的是诅咒版，给一次 5 分钟抗火并提示 */
    @Override
    public void onEquip(SlotContext slotContext, ItemStack prevStack, ItemStack stack) {
        if (!(slotContext.entity() instanceof ServerPlayer player)) return;
        if (isBlessed(stack)) return;

        player.addEffect(new MobEffectInstance(
                MobEffects.FIRE_RESISTANCE,
                WRATH_FIRE_RESIST_DURATION,
                0,
                false, false));

        player.sendSystemMessage(Component.translatable(
                "message.gazeofthepantheon.xihe_wrath.grace_period"));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide) {
            return InteractionResultHolder.success(stack);
        }

        if (CuriosUtil.hasDeityEquipped(player, XiheItem.class)) {
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
            tooltip.add(Component.translatable("tooltip.gazeofthepantheon.xihe_kindness.desc")
                    .withStyle(ChatFormatting.GOLD));
            tooltip.add(Component.translatable("tooltip.gazeofthepantheon.xihe_kindness.effect")
                    .withStyle(ChatFormatting.YELLOW));
        } else {
            tooltip.add(Component.translatable("tooltip.gazeofthepantheon.xihe_wrath.desc")
                    .withStyle(ChatFormatting.DARK_RED));
            tooltip.add(Component.translatable("tooltip.gazeofthepantheon.xihe_wrath.effect")
                    .withStyle(ChatFormatting.RED));
            tooltip.add(Component.translatable("tooltip.gazeofthepantheon.xihe_wrath.locked")
                    .withStyle(ChatFormatting.GRAY));
        }
    }
}