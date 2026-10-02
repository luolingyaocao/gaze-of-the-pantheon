package com.onceheart.gazeofthepantheon.event;

import com.onceheart.gazeofthepantheon.GazeOfThePantheon;
import com.onceheart.gazeofthepantheon.item.AchillesItem;
import com.onceheart.gazeofthepantheon.item.AresItem;
import com.onceheart.gazeofthepantheon.item.HermesItem;
import com.onceheart.gazeofthepantheon.item.HygieiaItem;
import com.onceheart.gazeofthepantheon.item.ThanatosItem;
import com.onceheart.gazeofthepantheon.item.XiheItem;
import com.onceheart.gazeofthepantheon.registry.ModItems;
import com.onceheart.gazeofthepantheon.util.CuriosUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = GazeOfThePantheon.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ThanatosEventHandler {

    private static final String NBT_GIVEN = "gazeofthepantheon_given";
    private static final String NBT_HARDCORE_DEATH = "gazeofthepantheon_hardcore_death";

    private static final Map<UUID, ItemStack> SAVED_KINDNESS = new HashMap<>();
    private static final Map<UUID, ItemStack> SAVED_HYGIEIA = new HashMap<>();
    private static final Map<UUID, ItemStack> SAVED_ARES = new HashMap<>();
    private static final Map<UUID, ItemStack> SAVED_HERMES = new HashMap<>();
    private static final Map<UUID, ItemStack> SAVED_XIHE = new HashMap<>();
    private static final Map<UUID, ItemStack> SAVED_ACHILLES = new HashMap<>();
    private static final Map<UUID, Boolean> WRATH_DEATH = new HashMap<>();

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        CompoundTag data = player.getPersistentData();

        if (data.getBoolean(NBT_HARDCORE_DEATH)) {
            data.putBoolean(NBT_HARDCORE_DEATH, false);
            player.setGameMode(GameType.SPECTATOR);
            player.sendSystemMessage(Component.translatable(
                    "message.gazeofthepantheon.thanatos_wrath.death"));
            return;
        }

        if (!data.getBoolean(NBT_GIVEN)) {
            data.putBoolean(NBT_GIVEN, true);

            CuriosUtil.tryEquipToGaze(player, new ItemStack(ModItems.THANATOS.get()));
            CuriosUtil.tryEquipToGaze(player, new ItemStack(ModItems.HYGIEIA.get()));
            CuriosUtil.tryEquipToGaze(player, new ItemStack(ModItems.ARES.get()));
            CuriosUtil.tryEquipToGaze(player, new ItemStack(ModItems.HERMES.get()));
            CuriosUtil.tryEquipToGaze(player, new ItemStack(ModItems.XIHE.get()));
            CuriosUtil.tryEquipToGaze(player, new ItemStack(ModItems.ACHILLES.get()));

            ItemStack markedDirt = createMarkedDirt();
            if (!player.getInventory().add(markedDirt)) {
                player.drop(markedDirt, false);
            }
        }
    }

    public static ItemStack createMarkedDirt() {
        ItemStack dirt = new ItemStack(Items.DIRT);
        dirt.setHoverName(Component.translatable("item.gazeofthepantheon.marked_dirt")
                .withStyle(ChatFormatting.DARK_RED));

        CompoundTag display = dirt.getOrCreateTagElement("display");
        ListTag lore = new ListTag();
        lore.add(StringTag.valueOf(Component.Serializer.toJson(
                Component.translatable("tooltip.gazeofthepantheon.marked_dirt.desc")
                        .withStyle(ChatFormatting.GRAY))));
        display.put("Lore", lore);

        return dirt;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onDeathKindness(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        ItemStack kindness = CuriosUtil.findKindness(player);
        if (kindness.isEmpty()) return;

        event.setCanceled(true);

        player.setHealth(player.getMaxHealth());
        player.deathTime = 0;
        player.hurtTime = 0;
        player.invulnerableTime = 40;
        player.clearFire();
        player.removeAllEffects();

        player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60, 0, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 4, false, false));

        player.sendSystemMessage(Component.translatable(
                "message.gazeofthepantheon.thanatos_kindness.triggered"));
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onDeathWrath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (event.isCanceled()) return;

        ItemStack wrath = CuriosUtil.findWrath(player);
        if (wrath.isEmpty()) return;

        WRATH_DEATH.put(player.getUUID(), true);
        player.getPersistentData().putBoolean(NBT_HARDCORE_DEATH, true);

        CuriosUtil.removeFromGaze(player, wrath);
        GazeOfThePantheon.LOGGER.debug("Thanatos Wrath: marked hardcore death for {}",
                player.getName().getString());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        Iterator<ItemEntity> it = event.getDrops().iterator();
        while (it.hasNext()) {
            ItemEntity ie = it.next();
            ItemStack stack = ie.getItem();

            if (stack.getItem() instanceof ThanatosItem && ThanatosItem.isBlessed(stack)) {
                SAVED_KINDNESS.put(player.getUUID(), stack.copy());
                it.remove();
            } else if (stack.getItem() instanceof HygieiaItem) {
                SAVED_HYGIEIA.put(player.getUUID(), stack.copy());
                it.remove();
            } else if (stack.getItem() instanceof AresItem) {
                SAVED_ARES.put(player.getUUID(), stack.copy());
                it.remove();
            } else if (stack.getItem() instanceof HermesItem) {
                SAVED_HERMES.put(player.getUUID(), stack.copy());
                it.remove();
            } else if (stack.getItem() instanceof XiheItem) {
                SAVED_XIHE.put(player.getUUID(), stack.copy());
                it.remove();
            } else if (stack.getItem() instanceof AchillesItem) {
                SAVED_ACHILLES.put(player.getUUID(), stack.copy());
                it.remove();
            }
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        ItemStack savedKindness = SAVED_KINDNESS.remove(player.getUUID());
        if (savedKindness != null && !savedKindness.isEmpty()) {
            CuriosUtil.tryEquipToGaze(player, savedKindness);
        }

        ItemStack savedHygieia = SAVED_HYGIEIA.remove(player.getUUID());
        if (savedHygieia != null && !savedHygieia.isEmpty()) {
            CuriosUtil.tryEquipToGaze(player, savedHygieia);
        }

        ItemStack savedAres = SAVED_ARES.remove(player.getUUID());
        if (savedAres != null && !savedAres.isEmpty()) {
            CuriosUtil.tryEquipToGaze(player, savedAres);
        }

        ItemStack savedHermes = SAVED_HERMES.remove(player.getUUID());
        if (savedHermes != null && !savedHermes.isEmpty()) {
            CuriosUtil.tryEquipToGaze(player, savedHermes);
        }

        ItemStack savedXihe = SAVED_XIHE.remove(player.getUUID());
        if (savedXihe != null && !savedXihe.isEmpty()) {
            CuriosUtil.tryEquipToGaze(player, savedXihe);
        }

        ItemStack savedAchilles = SAVED_ACHILLES.remove(player.getUUID());
        if (savedAchilles != null && !savedAchilles.isEmpty()) {
            CuriosUtil.tryEquipToGaze(player, savedAchilles);
        }

        boolean wrathDeath = WRATH_DEATH.remove(player.getUUID()) != null;
        CompoundTag data = player.getPersistentData();
        if (wrathDeath || data.getBoolean(NBT_HARDCORE_DEATH)) {
            data.putBoolean(NBT_HARDCORE_DEATH, false);
            player.setGameMode(GameType.SPECTATOR);
            player.sendSystemMessage(Component.translatable(
                    "message.gazeofthepantheon.thanatos_wrath.death"));
        }
    }
}