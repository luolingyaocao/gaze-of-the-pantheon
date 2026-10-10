package com.onceheart.gazeofthepantheon;

import com.mojang.logging.LogUtils;
import com.onceheart.gazeofthepantheon.effect.ModEffects;
import com.onceheart.gazeofthepantheon.network.ModNetwork;
import com.onceheart.gazeofthepantheon.registry.ModCreativeTabs;
import com.onceheart.gazeofthepantheon.registry.ModItems;
import com.onceheart.gazeofthepantheon.registry.ModMenus;
import com.onceheart.gazeofthepantheon.worldgen.ModFeatures;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(GazeOfThePantheon.MOD_ID)
public class GazeOfThePantheon {
    public static final String MOD_ID = "gazeofthepantheon";
    public static final Logger LOGGER = LogUtils.getLogger();

    public GazeOfThePantheon(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();

        ModItems.register(modEventBus);
        ModCreativeTabs.register(modEventBus);
        ModMenus.register(modEventBus);
        ModEffects.register(modEventBus);
        ModFeatures.register(modEventBus);

        ModNetwork.register();
    }
}