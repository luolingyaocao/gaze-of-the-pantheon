package com.onceheart.gazeofthepantheon.recipe;

import com.onceheart.gazeofthepantheon.GazeOfThePantheon;
import com.onceheart.gazeofthepantheon.registry.ModItems;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraftforge.common.brewing.BrewingRecipeRegistry;
import net.minecraftforge.common.brewing.IBrewingRecipe;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

/**
 * 酿造台配方：虚弱药水 + 小麦 → 精酿。
 *
 * 第一步（水瓶 + 发酵蛛眼 → 虚弱药水）走原版固有配方，这里只注册第二步。
 */
@Mod.EventBusSubscriber(modid = GazeOfThePantheon.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class BrewRecipe {

    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() ->
                BrewingRecipeRegistry.addRecipe(new IBrewingRecipe() {
                    @Override
                    public boolean isInput(ItemStack input) {
                        if (input.getItem() != Items.POTION) return false;
                        return PotionUtils.getPotion(input) == Potions.WEAKNESS;
                    }

                    @Override
                    public boolean isIngredient(ItemStack ingredient) {
                        return ingredient.is(Items.WHEAT);
                    }

                    @Override
                    public ItemStack getOutput(ItemStack input, ItemStack ingredient) {
                        if (!isInput(input) || !isIngredient(ingredient)) {
                            return ItemStack.EMPTY;
                        }
                        return new ItemStack(ModItems.BREW.get());
                    }
                }));
    }
}