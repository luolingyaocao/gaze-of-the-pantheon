package com.onceheart.gazeofthepantheon.registry;

import com.onceheart.gazeofthepantheon.GazeOfThePantheon;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, GazeOfThePantheon.MOD_ID);

    public static final RegistryObject<CreativeModeTab> MAIN_TAB = CREATIVE_MODE_TABS.register("main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.gazeofthepantheon"))
                    .icon(() -> new ItemStack(ModItems.THANATOS.get()))
                    .displayItems((parameters, output) -> {
                        // 注视饰品
                        output.accept(ModItems.THANATOS.get());
                        output.accept(ModItems.HYGIEIA.get());
                        output.accept(ModItems.ARES.get());
                        output.accept(ModItems.HERMES.get());
                        output.accept(ModItems.XIHE.get());
                        output.accept(ModItems.ACHILLES.get());
                        output.accept(ModItems.DIONYSUS.get());
                        output.accept(ModItems.HEPHAESTUS.get());
                        // 必行敕令与成事在人
                        output.accept(ModItems.EDICT.get());
                        output.accept(ModItems.DEED.get());
                        // 认可与转化物品
                        output.accept(ModItems.SOUL_CONTRACT.get());
                        output.accept(ModItems.DEATHS_RECOGNITION.get());
                        output.accept(ModItems.MEDICINE_GODS_RECOGNITION.get());
                        output.accept(ModItems.WAR_GODS_RECOGNITION.get());
                        output.accept(ModItems.MESSENGERS_RECOGNITION.get());
                        output.accept(ModItems.STYX_INFUSION.get());
                        output.accept(ModItems.FUSANG_DEW.get());
                        output.accept(ModItems.IVY_CROWN.get());
                        output.accept(ModItems.FORGEFIRE_HEART.get());
                        // 材料与工具
                        output.accept(ModItems.HERMES_SANDALS.get());
                        output.accept(ModItems.GOLDEN_CROW_FEATHER.get());
                        output.accept(ModItems.BREW.get());
                        output.accept(ModItems.MOON_MIRROR.get());
                        // 彩蛋
                        output.accept(ModItems.STOP.get());
                    })
                    .build());

    public static final RegistryObject<CreativeModeTab> PRIMORDIAL_HUMAN_REALM_TAB = CREATIVE_MODE_TABS.register("primordial_human_realm",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.gazeofthepantheon.primordial_human_realm"))
                    .icon(() -> new ItemStack(ModItems.PRIMORDIAL_MATTER.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.RARIFIED_ESSENCE.get());
                        output.accept(ModItems.TURBID_ESSENCE.get());
                        output.accept(ModItems.PRIMORDIAL_MATTER.get());
                    })
                    .build());

    public static final RegistryObject<CreativeModeTab> DEV_TEST_TAB = CREATIVE_MODE_TABS.register("dev_test",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.gazeofthepantheon.dev_test"))
                    .icon(() -> new ItemStack(ModItems.TEST_TP_UNFIXED_HEAVEN.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.TEST_TP_UNFIXED_HEAVEN.get());
                        output.accept(ModItems.TEST_TP_PRIMORDIAL_HUMAN_REALM.get());
                    })
                    .build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}