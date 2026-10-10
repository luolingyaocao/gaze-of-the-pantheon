package com.onceheart.gazeofthepantheon.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 神行 / Stop。
 *
 * 无配方、不可合成，仅能创造模式拿取或 /give 获得。
 * 实际效果由 StopEventHandler 每 tick 执行。
 */
public class StopItem extends Item {

    public StopItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.gazeofthepantheon.stop.desc")
                .withStyle(ChatFormatting.DARK_RED));
    }
}