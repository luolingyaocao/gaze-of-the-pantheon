package com.onceheart.gazeofthepantheon.event;

import com.onceheart.gazeofthepantheon.GazeOfThePantheon;
import com.onceheart.gazeofthepantheon.util.CuriosUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 塔纳托斯神系专属逻辑。
 *
 * 覆盖：
 * - 愠怒（诅咒）：死亡时销毁愠怒、标记硬核死亡
 * - 善意（祝福）：死亡时取消事件并救赎
 * - 硬核死亡：重生时切旁观模式
 * - 位置记录 tick：驱动 DivineSaveHandler 的虚空传送
 * - 标记泥土工厂：供 FirstJoinHandler 首登发放
 *
 * 通用掉落保存 / 重生恢复见 GazePersistenceHandler。
 * 首登发放 / 首登降雨见 FirstJoinHandler。
 */
@Mod.EventBusSubscriber(modid = GazeOfThePantheon.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ThanatosEventHandler {

    /** Forge 的持久化子节点 key。写在根下会在 respawn 时丢失，必须写在 PlayerPersisted 里。 */
    private static final String PERSISTED_NBT_TAG = "PlayerPersisted";

    private static final String NBT_HARDCORE_DEATH = "gazeofthepantheon_hardcore_death";

    /** 本会话内的愠怒死亡标记，重生时读取。 */
    private static final Map<UUID, Boolean> WRATH_DEATH = new HashMap<>();

    /** 取玩家持久化数据子节点，确保节点存在。所有 NBT 读写都走这里。 */
    private static CompoundTag persisted(ServerPlayer player) {
        CompoundTag root = player.getPersistentData();
        if (!root.contains(PERSISTED_NBT_TAG, Tag.TAG_COMPOUND)) {
            root.put(PERSISTED_NBT_TAG, new CompoundTag());
        }
        return root.getCompound(PERSISTED_NBT_TAG);
    }

    // ============ 标记泥土工厂 ============

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

    // ============ 重生：愠怒死亡 → 旁观模式 ============

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

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