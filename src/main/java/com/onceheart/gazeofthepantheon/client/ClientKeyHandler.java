package com.onceheart.gazeofthepantheon.client;

import com.onceheart.gazeofthepantheon.GazeOfThePantheon;
import com.onceheart.gazeofthepantheon.network.ModNetwork;
import com.onceheart.gazeofthepantheon.network.OpenEdictPacket;
import com.onceheart.gazeofthepantheon.network.ToggleDionysusPacket;
import com.onceheart.gazeofthepantheon.network.ToggleHermesPacket;
import net.minecraft.client.Minecraft;
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

        // H 键：切换赫尔墨斯奔袭
        while (ModKeyMappings.TOGGLE_HERMES_GEAR.consumeClick()) {
            ModNetwork.CHANNEL.sendToServer(new ToggleHermesPacket());
        }

        // I 键：切换澄明开关
        while (ModKeyMappings.TOGGLE_DIONYSUS.consumeClick()) {
            ModNetwork.CHANNEL.sendToServer(new ToggleDionysusPacket());
        }

        // V 键：打开/关闭决策 UI
        while (ModKeyMappings.OPEN_EDICT.consumeClick()) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.screen instanceof EdictScreen) {
                mc.setScreen(null);
            } else if (mc.screen == null) {
                ModNetwork.CHANNEL.sendToServer(new OpenEdictPacket());
            }
        }
    }
}