package com.onceheart.gazeofthepantheon.client;

import com.onceheart.gazeofthepantheon.GazeOfThePantheon;
import com.onceheart.gazeofthepantheon.worldgen.ModDimensions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterDimensionSpecialEffectsEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

public class PrimordialHumanRealmClient {

    @Mod.EventBusSubscriber(modid = GazeOfThePantheon.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ModBusHandler {

        @SubscribeEvent
        public static void registerDimensionEffects(RegisterDimensionSpecialEffectsEvent event) {
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(
                    GazeOfThePantheon.MOD_ID, "primordial_human_realm_type");
            event.register(id, new PrimordialHumanRealmEffects());
        }
    }

    @Mod.EventBusSubscriber(modid = GazeOfThePantheon.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static class ForgeBusHandler {

        @SubscribeEvent
        public static void onRenderFog(ViewportEvent.RenderFog event) {
            Minecraft mc = Minecraft.getInstance();
            ClientLevel level = mc.level;
            if (level == null) return;

            if (!level.dimension().equals(ModDimensions.PRIMORDIAL_HUMAN_REALM)) return;

            double camY = event.getCamera().getPosition().y;
            if (camY > 140.0) {
                event.setNearPlaneDistance(20.0F);
                event.setFarPlaneDistance(80.0F);
                event.setCanceled(true);
            }
        }
    }
}