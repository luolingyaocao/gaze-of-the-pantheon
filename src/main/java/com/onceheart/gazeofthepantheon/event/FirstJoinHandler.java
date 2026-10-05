package com.onceheart.gazeofthepantheon.event;

import com.onceheart.gazeofthepantheon.GazeOfThePantheon;
import com.onceheart.gazeofthepantheon.registry.ModItems;
import com.onceheart.gazeofthepantheon.util.CuriosUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 首次进世界时的初始化逻辑。
 *
 * 覆盖：
 * - 六系诅咒注视发放（进注视栏位）
 * - 标记泥土发放（进背包，装不下就掉地上）
 * - 羲和首登降雨（委托 XiheEventHandler）
 *
 * 硬核死亡（愠怒死亡后的旁观模式）判定也在这里，因为共用同一张 NBT_GIVEN 组。
 */
@Mod.EventBusSubscriber(modid = GazeOfThePantheon.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class FirstJoinHandler {

    /** Forge 的持久化子节点 key。写在根下会在 respawn 时丢失，必须写在 PlayerPersisted 里。 */
    private static final String PERSISTED_NBT_TAG = "PlayerPersisted";

    private static final String NBT_GIVEN = "gazeofthepantheon_given";
    private static final String NBT_HARDCORE_DEATH = "gazeofthepantheon_hardcore_death";

    /** 取玩家持久化数据子节点，确保节点存在。所有 NBT 读写都走这里。 */
    private static CompoundTag persisted(ServerPlayer player) {
        CompoundTag root = player.getPersistentData();
        if (!root.contains(PERSISTED_NBT_TAG, Tag.TAG_COMPOUND)) {
            root.put(PERSISTED_NBT_TAG, new CompoundTag());
        }
        return root.getCompound(PERSISTED_NBT_TAG);
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        CompoundTag data = persisted(player);

        // 愠怒死亡遗留：进旁观模式，不再发放
        if (data.getBoolean(NBT_HARDCORE_DEATH)) {
            data.putBoolean(NBT_HARDCORE_DEATH, false);
            player.setGameMode(GameType.SPECTATOR);
            player.sendSystemMessage(Component.translatable(
                    "message.gazeofthepantheon.thanatos_wrath.death"));
            return;
        }

        if (data.getBoolean(NBT_GIVEN)) return;

        data.putBoolean(NBT_GIVEN, true);

        CuriosUtil.tryEquipToGaze(player, new ItemStack(ModItems.THANATOS.get()));
        CuriosUtil.tryEquipToGaze(player, new ItemStack(ModItems.HYGIEIA.get()));
        CuriosUtil.tryEquipToGaze(player, new ItemStack(ModItems.ARES.get()));
        CuriosUtil.tryEquipToGaze(player, new ItemStack(ModItems.HERMES.get()));
        CuriosUtil.tryEquipToGaze(player, new ItemStack(ModItems.XIHE.get()));
        CuriosUtil.tryEquipToGaze(player, new ItemStack(ModItems.ACHILLES.get()));

        ItemStack markedDirt = ThanatosEventHandler.createMarkedDirt();
        if (!player.getInventory().add(markedDirt)) {
            player.drop(markedDirt, false);
        }

        // 主世界开始下雨，贴合羲和诅咒的文案设计
        XiheEventHandler.triggerFirstJoinRain(player);
    }
}