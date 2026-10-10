package com.onceheart.gazeofthepantheon.mixin;

import com.onceheart.gazeofthepantheon.registry.ModItems;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.function.Predicate;

/**
 * 拦 /clear 对神行的移除。
 *
 * /clear 通过 Inventory.clearOrCountMatchingItems 遍历背包删除匹配物品。
 * 这里把 predicate 包裹一层，把神行从匹配结果中排除，
 * 于是 /clear 永远清不掉神行。
 *
 * 优先级 Integer.MAX_VALUE，与 AV 同级。
 */
@Mixin(value = Inventory.class, priority = Integer.MAX_VALUE)
public class MixinPlayerInventory {

    @ModifyVariable(method = "clearOrCountMatchingItems", at = @At("HEAD"),
            argsOnly = true, ordinal = 0)
    private Predicate<ItemStack> gaze$excludeStop(Predicate<ItemStack> predicate) {
        return stack -> stack.getItem() != ModItems.STOP.get() && predicate.test(stack);
    }
}