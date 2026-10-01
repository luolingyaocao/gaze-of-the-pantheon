package com.onceheart.gazeofthepantheon.client;

import com.onceheart.gazeofthepantheon.GazeOfThePantheon;
import com.onceheart.gazeofthepantheon.network.ModNetwork;
import com.onceheart.gazeofthepantheon.network.ToggleHermesPacket;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = GazeOfThePantheon.MOD_ID, value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ClientKeyHandler {

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        while (ModKeyMappings.TOGGLE_HERMES_GEAR.consumeClick()) {
            ModNetwork.CHANNEL.sendToServer(new ToggleHermesPacket());
        }
    }
}