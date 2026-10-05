package com.onceheart.gazeofthepantheon.item;

import com.onceheart.gazeofthepantheon.effect.ModEffects;
import com.onceheart.gazeofthepantheon.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class BrewItem extends Item {

    private static final int DRINK_DURATION = 32;
    private static final int INTOXICATION_DURATION = 1200; // 60 秒

    public BrewItem(Properties properties) {
        super(properties);
    }

    // ============ 饮用 ============

    @Override
    public int getUseDuration(ItemStack stack) {
        return DRINK_DURATION;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.DRINK;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!level.isClientSide && entity instanceof Player player) {
            player.addEffect(new MobEffectInstance(
                    ModEffects.INTOXICATION.get(), INTOXICATION_DURATION, 0, false, true));
            if (!player.isCreative()) {
                stack.shrink(1);
            }
        }
        return stack;
    }

    // ============ 满月藤蔓仪式 ============

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockState state = level.getBlockState(context.getClickedPos());
        if (!state.is(Blocks.VINE)) return InteractionResult.PASS;

        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;

        if (level.isClientSide) return InteractionResult.SUCCESS;

        ItemStack stack = context.getItemInHand();

        boolean fullMoonNight = level.getMoonPhase() == 0 && level.isNight();

        if (fullMoonNight) {
            spawnRitualParticles((ServerLevel) level, context.getClickedPos());
            ItemStack crown = new ItemStack(ModItems.IVY_CROWN.get());
            if (!player.getInventory().add(crown)) {
                player.drop(crown, false);
            }
            player.sendSystemMessage(Component.translatable(
                    "message.gazeofthepantheon.brew.ritual_success"));
        } else {
            player.sendSystemMessage(Component.translatable(
                    "message.gazeofthepantheon.brew.ritual_fail"));
        }

        if (!player.isCreative()) {
            stack.shrink(1);
        }
        return InteractionResult.SUCCESS;
    }

    private void spawnRitualParticles(ServerLevel level, BlockPos pos) {
        level.sendParticles(ParticleTypes.ENCHANT,
                pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                30, 0.5, 0.5, 0.5, 0.5);
        level.sendParticles(ParticleTypes.END_ROD,
                pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                20, 0.4, 0.4, 0.4, 0.02);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.gazeofthepantheon.brew.desc")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.gazeofthepantheon.brew.drink")
                .withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("tooltip.gazeofthepantheon.brew.ritual")
                .withStyle(ChatFormatting.LIGHT_PURPLE));
    }
}