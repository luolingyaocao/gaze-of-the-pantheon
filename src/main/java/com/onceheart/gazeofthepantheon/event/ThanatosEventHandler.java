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
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
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

    /** Forge 的持久化子节点 key。写在根下会在 respawn 时丢失，必须写在 PlayerPersisted 里。 */
    private static final String PERSISTED_NBT_TAG = "PlayerPersisted";

    private static final String NBT_GIVEN = "gazeofthepantheon_given";
    private static final String NBT_HARDCORE_DEATH = "gazeofthepantheon_hardcore_death";

    /** 首次进世界时主世界降雨持续时间（5 分钟 = 6000 tick，与羲和诅咒的抗火时长对齐） */
    private static final int WRATH_RAIN_DURATION = 6000;

    private static final Map<UUID, ItemStack> SAVED_KINDNESS = new HashMap<>();
    private static final Map<UUID, ItemStack> SAVED_HYGIEIA = new HashMap<>();
    private static final Map<UUID, ItemStack> SAVED_ARES = new HashMap<>();
    private static final Map<UUID, ItemStack> SAVED_HERMES = new HashMap<>();
    private static final Map<UUID, ItemStack> SAVED_XIHE = new HashMap<>();
    private static final Map<UUID, ItemStack> SAVED_ACHILLES = new HashMap<>();
    private static final Map<UUID, ItemStack> SAVED_EDICT = new HashMap<>();
    private static final Map<UUID, Boolean> WRATH_DEATH = new HashMap<>();

    /** 取玩家持久化数据子节点，确保节点存在。所有 NBT 读写都走这里。 */
    private static CompoundTag persisted(ServerPlayer player) {
        CompoundTag root = player.getPersistentData();
        if (!root.contains(PERSISTED_NBT_TAG, Tag.TAG_COMPOUND)) {
            root.put(PERSISTED_NBT_TAG, new CompoundTag());
        }
        return root.getCompound(PERSISTED_NBT_TAG);
    }

    // ============ 登录：首次给予全部诅咒注视 ============

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        CompoundTag data = persisted(player);

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

            // 主世界开始下雨，贴合羲和诅咒的文案设计
            ServerLevel overworld = player.server.getLevel(Level.OVERWORLD);
            if (overworld != null) {
                overworld.setWeatherParameters(WRATH_RAIN_DURATION, 0, true, false);
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

    // ============ 每 tick：位置记录 + 善意虚空兜底 ============

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer player)) return;

        // 每 tick 记录最后站立位置（玩家踩地时写入）
        DivineSaveHandler.recordGroundPosition(player);

        // 每 20 tick 检测一次虚空
        if (player.tickCount % 20 != 0) return;

        // 善意玩家在虚空：立刻救赎
        if (DivineSaveHandler.isKindnessActive(player)
                && DivineSaveHandler.isInVoid(player)) {
            DivineSaveHandler.applyKindnessSave(player);
        }
    }

    // ============ 死亡：善意复活 ============

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onDeathKindness(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (event.isCanceled()) return;

        if (!DivineSaveHandler.isKindnessActive(player)) return;

        event.setCanceled(true);
        DivineSaveHandler.applyKindnessSave(player);
    }

    // ============ 死亡：愠怒处理（诅咒永远生效） ============

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onDeathWrath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (event.isCanceled()) return;

        ItemStack wrath = CuriosUtil.findWrath(player);
        if (wrath.isEmpty()) return;

        WRATH_DEATH.put(player.getUUID(), true);
        persisted(player).putBoolean(NBT_HARDCORE_DEATH, true);

        CuriosUtil.removeFromGaze(player, wrath);
        GazeOfThePantheon.LOGGER.debug("Thanatos Wrath: marked hardcore death for {}",
                player.getName().getString());
    }

    // ============ 掉落：注视饰品 + 必行敕令在栏位中不掉落 ============

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
            } else if (stack.getItem() == ModItems.EDICT.get()) {
                SAVED_EDICT.put(player.getUUID(), stack.copy());
                it.remove();
            }
        }
    }

    // ============ 重生：恢复饰品 ============

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

        ItemStack savedEdict = SAVED_EDICT.remove(player.getUUID());
        if (savedEdict != null && !savedEdict.isEmpty()) {
            CuriosUtil.tryEquipToDecision(player, savedEdict);
        }

        boolean wrathDeath = WRATH_DEATH.remove(player.getUUID()) != null;
        CompoundTag data = persisted(player);
        if (wrathDeath || data.getBoolean(NBT_HARDCORE_DEATH)) {
            data.putBoolean(NBT_HARDCORE_DEATH, false);
            player.setGameMode(GameType.SPECTATOR);
            player.sendSystemMessage(Component.translatable(
                    "message.gazeofthepantheon.thanatos_wrath.death"));
        }
    }
}