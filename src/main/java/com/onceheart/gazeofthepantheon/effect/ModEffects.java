package com.onceheart.gazeofthepantheon.effect;

import com.onceheart.gazeofthepantheon.GazeOfThePantheon;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEffects {

    public static final DeferredRegister<MobEffect> EFFECTS =
            DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, GazeOfThePantheon.MOD_ID);

    public static final RegistryObject<MobEffect> INTOXICATION =
            EFFECTS.register("intoxication", IntoxicationEffect::new);

    public static void register(IEventBus eventBus) {
        EFFECTS.register(eventBus);
    }
}