package com.onceheart.gazeofthepantheon.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.onceheart.gazeofthepantheon.GazeOfThePantheon;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = GazeOfThePantheon.MOD_ID, value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModKeyMappings {

    public static final String CATEGORY = "key.categories.gazeofthepantheon";

    public static final KeyMapping TOGGLE_HERMES_GEAR = new KeyMapping(
            "key.gazeofthepantheon.toggle_hermes_gear",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_H,
            CATEGORY
    );

    public static final KeyMapping OPEN_EDICT = new KeyMapping(
            "key.gazeofthepantheon.open_edict",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_V,
            CATEGORY
    );

    public static final KeyMapping TOGGLE_DIONYSUS = new KeyMapping(
            "key.gazeofthepantheon.toggle_dionysus",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_I,
            CATEGORY
    );

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(TOGGLE_HERMES_GEAR);
        event.register(OPEN_EDICT);
        event.register(TOGGLE_DIONYSUS);
    }
}