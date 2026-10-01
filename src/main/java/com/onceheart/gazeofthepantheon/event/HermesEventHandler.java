package com.onceheart.gazeofthepantheon.event;

import com.onceheart.gazeofthepantheon.GazeOfThePantheon;
import com.onceheart.gazeofthepantheon.registry.ModItems;
import com.onceheart.gazeofthepantheon.util.CuriosUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = GazeOfThePantheon.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class HermesEventHandler {

    private static final UUID WRATH_SPEED_UUID = UUID.fromString("b1e50001-0001-0001-0001-000000000001");
    private static final UUID KIND_SPEED_UUID  = UUID.fromString("b1e50002-0002-0002-0002-000000000002");

    private static final String NBT_GEAR = "gazeofthepantheon_hermes_gear";

    public static final int GEAR_0 = 0;
    public static final int GEAR_25 = 25;
    public static final int GEAR_50 = 50;

    /** 草鞋掉落概率：0.1%，一把下界合金镐约掉 2 个 */
    private static final float SANDALS_DROP_CHANCE = 0.001F;

    /** 读取玩家当前挡位，默认 50 */
    public static int getGear(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        if (!data.contains(NBT_GEAR)) {
            return GEAR_50;
        }
        int g = data.getInt(NBT_GEAR);
        if (g != GEAR_0 && g != GEAR_25 && g != GEAR_50) {
            return GEAR_50;
        }
        return g;
    }

    /** 循环切换挡位：50 → 0 → 25 → 50 */
    public static int cycleGear(ServerPlayer player) {
        int current = getGear(player);
        int next;
        if (current == GEAR_50) next = GEAR_0;
        else if (current == GEAR_0) next = GEAR_25;
        else next = GEAR_50;
        player.getPersistentData().putInt(NBT_GEAR, next);
        return next;
    }

    // ============ 移速处理 ============

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer player)) return;

        boolean hasWrath = !CuriosUtil.findHermesWrath(player).isEmpty();
        boolean hasKindness = !CuriosUtil.findHermesKindness(player).isEmpty();

        if (hasWrath) {
            applyModifier(player, WRATH_SPEED_UUID, "gazeofthepantheon.hermes_wrath_speed", -0.2D);
        } else {
            removeModifier(player, WRATH_SPEED_UUID);
        }

        if (hasKindness) {
            int gear = getGear(player);
            double amount = gear / 100.0D;
            if (amount > 0) {
                applyModifier(player, KIND_SPEED_UUID, "gazeofthepantheon.hermes_kind_speed", amount);
            } else {
                removeModifier(player, KIND_SPEED_UUID);
            }
        } else {
            removeModifier(player, KIND_SPEED_UUID);
        }
    }

    private static void applyModifier(ServerPlayer player, UUID uuid, String name, double amount) {
        AttributeInstance instance = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (instance == null) return;
        AttributeModifier existing = instance.getModifier(uuid);
        if (existing == null || existing.getAmount() != amount
                || existing.getOperation() != AttributeModifier.Operation.MULTIPLY_TOTAL) {
            if (existing != null) instance.removeModifier(uuid);
            instance.addTransientModifier(new AttributeModifier(
                    uuid, name, amount, AttributeModifier.Operation.MULTIPLY_TOTAL));
        }
    }

    private static void removeModifier(ServerPlayer player, UUID uuid) {
        AttributeInstance instance = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (instance != null && instance.getModifier(uuid) != null) {
            instance.removeModifier(uuid);
        }
    }

    // ============ 草鞋掉落 ============

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) return;
        if (player.isCreative()) return;

        BlockState state = event.getState();
        if (!state.is(Blocks.STONE) && !state.is(Blocks.DEEPSLATE)) return;

        if (CuriosUtil.findHermesWrath(player).isEmpty()) return;

        if (player.getRandom().nextFloat() < SANDALS_DROP_CHANCE) {
            ItemStack sandals = new ItemStack(ModItems.HERMES_SANDALS.get());
            if (!player.getInventory().add(sandals)) {
                player.drop(sandals, false);
            }
        }
    }
}