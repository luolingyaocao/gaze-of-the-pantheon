package com.onceheart.gazeofthepantheon.client;

import com.onceheart.gazeofthepantheon.GazeOfThePantheon;
import com.onceheart.gazeofthepantheon.registry.ModItems;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 客户端物品染色。
 *
 * 精酿复用原版药水模型（potion_overlay + potion），
 * 把 overlay 层（tintIndex 0，液体形状）染成琥珀黄，模拟啤酒。
 */
@Mod.EventBusSubscriber(modid = GazeOfThePantheon.MOD_ID, value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.MOD)
public class ClientColorHandler {

    /** 琥珀黄。想要更浓/更淡自己调这三个字节。 */
    private static final int BEER_AMBER = 0xE8B93A;

    @SubscribeEvent
    public static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        event.register((stack, tintIndex) -> tintIndex == 0 ? BEER_AMBER : 0xFFFFFF,
                ModItems.BREW.get());
    }
}