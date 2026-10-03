package com.onceheart.gazeofthepantheon.registry;

import com.onceheart.gazeofthepantheon.GazeOfThePantheon;
import com.onceheart.gazeofthepantheon.menu.EdictMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModMenus {

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, GazeOfThePantheon.MOD_ID);

    public static final RegistryObject<MenuType<EdictMenu>> EDICT_MENU =
            MENUS.register("edict_menu",
                    () -> IForgeMenuType.create((containerId, inv, buf) ->
                            new EdictMenu(containerId, inv, buf)));

    public static void register(IEventBus eventBus) {
        MENUS.register(eventBus);
    }
}