package com.onceheart.gazeofthepantheon.registry;

import com.onceheart.gazeofthepantheon.GazeOfThePantheon;
import com.onceheart.gazeofthepantheon.item.AchillesItem;
import com.onceheart.gazeofthepantheon.item.AresItem;
import com.onceheart.gazeofthepantheon.item.BrewItem;
import com.onceheart.gazeofthepantheon.item.DeathsRecognitionItem;
import com.onceheart.gazeofthepantheon.item.DeedItem;
import com.onceheart.gazeofthepantheon.item.DionysusItem;
import com.onceheart.gazeofthepantheon.item.EdictItem;
import com.onceheart.gazeofthepantheon.item.FaqItem;
import com.onceheart.gazeofthepantheon.item.FusangDewItem;
import com.onceheart.gazeofthepantheon.item.GoldenCrowFeatherItem;
import com.onceheart.gazeofthepantheon.item.HermesItem;
import com.onceheart.gazeofthepantheon.item.HermesSandalsItem;
import com.onceheart.gazeofthepantheon.item.HygieiaItem;
import com.onceheart.gazeofthepantheon.item.IvyCrownItem;
import com.onceheart.gazeofthepantheon.item.MedicineGodsRecognitionItem;
import com.onceheart.gazeofthepantheon.item.MessengersRecognitionItem;
import com.onceheart.gazeofthepantheon.item.MoonMirrorItem;
import com.onceheart.gazeofthepantheon.item.SoulContractItem;
import com.onceheart.gazeofthepantheon.item.StyxInfusionItem;
import com.onceheart.gazeofthepantheon.item.ThanatosItem;
import com.onceheart.gazeofthepantheon.item.WarGodsRecognitionItem;
import com.onceheart.gazeofthepantheon.item.XiheItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, GazeOfThePantheon.MOD_ID);

    // ============ 注视饰品（不可摧毁） ============

    public static final RegistryObject<Item> THANATOS = ITEMS.register("thanatos",
            () -> new ThanatosItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));

    public static final RegistryObject<Item> HYGIEIA = ITEMS.register("hygieia",
            () -> new HygieiaItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));

    public static final RegistryObject<Item> ARES = ITEMS.register("ares",
            () -> new AresItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));

    public static final RegistryObject<Item> HERMES = ITEMS.register("hermes",
            () -> new HermesItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));

    public static final RegistryObject<Item> XIHE = ITEMS.register("xihe",
            () -> new XiheItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));

    public static final RegistryObject<Item> ACHILLES = ITEMS.register("achilles",
            () -> new AchillesItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));

    public static final RegistryObject<Item> DIONYSUS = ITEMS.register("dionysus",
            () -> new DionysusItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));

    // ============ 必行敕令与成事在人 ============

    public static final RegistryObject<Item> EDICT = ITEMS.register("edict",
            () -> new EdictItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));

    public static final RegistryObject<Item> DEED = ITEMS.register("deed",
            () -> new DeedItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));

    // ============ 认可与转化物品 ============

    public static final RegistryObject<Item> SOUL_CONTRACT = ITEMS.register("soul_contract",
            () -> new SoulContractItem(new Item.Properties().stacksTo(16).rarity(Rarity.RARE)));

    public static final RegistryObject<Item> DEATHS_RECOGNITION = ITEMS.register("deaths_recognition",
            () -> new DeathsRecognitionItem(new Item.Properties().stacksTo(16).rarity(Rarity.EPIC)));

    public static final RegistryObject<Item> MEDICINE_GODS_RECOGNITION = ITEMS.register("medicine_gods_recognition",
            () -> new MedicineGodsRecognitionItem(new Item.Properties().stacksTo(16).rarity(Rarity.EPIC)));

    public static final RegistryObject<Item> WAR_GODS_RECOGNITION = ITEMS.register("war_gods_recognition",
            () -> new WarGodsRecognitionItem(new Item.Properties().stacksTo(16).rarity(Rarity.EPIC)));

    public static final RegistryObject<Item> MESSENGERS_RECOGNITION = ITEMS.register("messengers_recognition",
            () -> new MessengersRecognitionItem(new Item.Properties().stacksTo(16).rarity(Rarity.EPIC)));

    public static final RegistryObject<Item> STYX_INFUSION = ITEMS.register("styx_infusion",
            () -> new StyxInfusionItem(new Item.Properties().stacksTo(16).rarity(Rarity.EPIC)));

    public static final RegistryObject<Item> FUSANG_DEW = ITEMS.register("fusang_dew",
            () -> new FusangDewItem(new Item.Properties().stacksTo(16).rarity(Rarity.EPIC)));

    public static final RegistryObject<Item> IVY_CROWN = ITEMS.register("ivy_crown",
            () -> new IvyCrownItem(new Item.Properties().stacksTo(16).rarity(Rarity.EPIC)));

    // ============ 材料与工具 ============

    public static final RegistryObject<Item> HERMES_SANDALS = ITEMS.register("hermes_sandals",
            () -> new HermesSandalsItem(new Item.Properties().stacksTo(16).rarity(Rarity.RARE)));

    public static final RegistryObject<Item> GOLDEN_CROW_FEATHER = ITEMS.register("golden_crow_feather",
            () -> new GoldenCrowFeatherItem(new Item.Properties().stacksTo(16).rarity(Rarity.RARE)));

    public static final RegistryObject<Item> BREW = ITEMS.register("brew",
            () -> new BrewItem(new Item.Properties().stacksTo(16).rarity(Rarity.RARE)));

    public static final RegistryObject<Item> MOON_MIRROR = ITEMS.register("moon_mirror",
            () -> new MoonMirrorItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));

    // ============ 彩蛋 ============

    public static final RegistryObject<Item> FAQ = ITEMS.register("faq",
            () -> new FaqItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}