package com.onceheart.gazeofthepantheon.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.onceheart.gazeofthepantheon.GazeOfThePantheon;
import com.onceheart.gazeofthepantheon.event.ThanatosEventHandler;
import com.onceheart.gazeofthepantheon.item.AchillesItem;
import com.onceheart.gazeofthepantheon.item.AresItem;
import com.onceheart.gazeofthepantheon.item.DionysusItem;
import com.onceheart.gazeofthepantheon.item.HermesItem;
import com.onceheart.gazeofthepantheon.item.HygieiaItem;
import com.onceheart.gazeofthepantheon.item.ThanatosItem;
import com.onceheart.gazeofthepantheon.item.XiheItem;
import com.onceheart.gazeofthepantheon.registry.ModItems;
import com.onceheart.gazeofthepantheon.util.CuriosUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

@Mod.EventBusSubscriber(modid = GazeOfThePantheon.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ModCommands {

    private static final List<String> GAZE_ITEM_IDS = List.of(
            GazeOfThePantheon.MOD_ID + ":thanatos",
            GazeOfThePantheon.MOD_ID + ":hygieia",
            GazeOfThePantheon.MOD_ID + ":ares",
            GazeOfThePantheon.MOD_ID + ":hermes",
            GazeOfThePantheon.MOD_ID + ":xihe",
            GazeOfThePantheon.MOD_ID + ":achilles",
            GazeOfThePantheon.MOD_ID + ":dionysus"
    );

    private static final SuggestionProvider<CommandSourceStack> GAZE_SUGGESTIONS =
            (ctx, builder) -> SharedSuggestionProvider.suggest(GAZE_ITEM_IDS, builder);

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {

        dispatcher.register(Commands.literal("decree")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("itemid", StringArgumentType.greedyString())
                        .suggests(GAZE_SUGGESTIONS)
                        .executes(ctx -> handleDecree(ctx.getSource(),
                                StringArgumentType.getString(ctx, "itemid").trim()))));

        dispatcher.register(Commands.literal("delete")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("itemid", StringArgumentType.greedyString())
                        .suggests(GAZE_SUGGESTIONS)
                        .executes(ctx -> handleDelete(ctx.getSource(),
                                StringArgumentType.getString(ctx, "itemid").trim()))));

        dispatcher.register(Commands.literal("ilikethemarkdirt")
                .executes(ctx -> handleLikeMarkDirt(ctx.getSource())));

        dispatcher.register(Commands.literal("invoco")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("xihe")
                        .then(Commands.literal("fusang_oblation")
                                .executes(ctx -> handleInvocoFusang(ctx.getSource())))));

        dispatcher.register(Commands.literal("whogazesatme")
                .executes(ctx -> handleWhoGazesAtMe(ctx.getSource())));
    }

    // ============ /invoco xihe fusang_oblation ============

    private static int handleInvocoFusang(CommandSourceStack source) {
        ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (Exception e) {
            source.sendFailure(Component.literal("§c该指令只能由玩家执行"));
            return 0;
        }

        ServerLevel level = player.serverLevel();
        BlockPos playerPos = player.blockPosition();

        BlockPos goldPos = playerPos.offset(4, 1, 0);
        BlockPos origin = goldPos.offset(0, -1, 0);

        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                level.setBlockAndUpdate(origin.offset(x, 0, z),
                        Blocks.SMOOTH_QUARTZ.defaultBlockState());
            }
        }

        int[][] diamondPositions = {
                {2, 2}, {2, 0}, {2, -2},
                {0, 2}, {0, -2},
                {-2, -2}, {-2, 0}, {-2, 2}
        };
        for (int[] p : diamondPositions) {
            level.setBlockAndUpdate(origin.offset(p[0], 1, p[1]),
                    Blocks.DIAMOND_BLOCK.defaultBlockState());
        }

        level.setBlockAndUpdate(goldPos, Blocks.GOLD_BLOCK.defaultBlockState());
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                if (x == 0 && z == 0) continue;
                level.setBlockAndUpdate(goldPos.offset(x, 0, z),
                        Blocks.DIRT.defaultBlockState());
            }
        }

        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                if (x == 0 && z == 0) continue;
                level.setBlockAndUpdate(goldPos.offset(x, 1, z),
                        Blocks.ACACIA_SAPLING.defaultBlockState());
            }
        }

        for (int x : new int[]{-3, 3}) {
            for (int z : new int[]{-3, 3}) {
                for (int y = 1; y <= 3; y++) {
                    level.setBlockAndUpdate(origin.offset(x, y, z),
                            Blocks.IRON_BLOCK.defaultBlockState());
                }
            }
        }

        for (int x : new int[]{-3, 3}) {
            for (int z : new int[]{-3, 3}) {
                level.setBlockAndUpdate(origin.offset(x, 4, z),
                        Blocks.GLOWSTONE.defaultBlockState());
            }
        }

        player.sendSystemMessage(Component.translatable(
                "message.gazeofthepantheon.command.invoco.fusang"));
        return 1;
    }

    // ============ /whogazesatme ============

    private static int handleWhoGazesAtMe(CommandSourceStack source) {
        ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (Exception e) {
            source.sendFailure(Component.literal("§c该指令只能由玩家执行"));
            return 0;
        }

        var handlerOpt = CuriosUtil.getGazeHandler(player);
        if (handlerOpt.isEmpty()) {
            player.sendSystemMessage(Component.translatable(
                    "message.gazeofthepantheon.whogazesatme.empty"));
            return 1;
        }

        var stacks = handlerOpt.get().getStacks();
        boolean found = false;
        for (int i = 0; i < stacks.getSlots(); i++) {
            ItemStack s = stacks.getStackInSlot(i);
            if (s.isEmpty()) continue;
            Component line = gazeLine(s);
            if (line != null) {
                player.sendSystemMessage(line);
                found = true;
            }
        }

        if (!found) {
            player.sendSystemMessage(Component.translatable(
                    "message.gazeofthepantheon.whogazesatme.empty"));
        }
        return 1;
    }

    private static Component gazeLine(ItemStack stack) {
        if (stack.getItem() instanceof ThanatosItem) {
            return Component.translatable(ThanatosItem.isBlessed(stack)
                    ? "message.gazeofthepantheon.gaze.thanatos_kindness"
                    : "message.gazeofthepantheon.gaze.thanatos_wrath");
        }
        if (stack.getItem() instanceof HygieiaItem) {
            return Component.translatable(HygieiaItem.isBlessed(stack)
                    ? "message.gazeofthepantheon.gaze.hygieia_kindness"
                    : "message.gazeofthepantheon.gaze.hygieia_wrath");
        }
        if (stack.getItem() instanceof AresItem) {
            return Component.translatable(AresItem.isBlessed(stack)
                    ? "message.gazeofthepantheon.gaze.ares_kindness"
                    : "message.gazeofthepantheon.gaze.ares_wrath");
        }
        if (stack.getItem() instanceof HermesItem) {
            return Component.translatable(HermesItem.isBlessed(stack)
                    ? "message.gazeofthepantheon.gaze.hermes_kindness"
                    : "message.gazeofthepantheon.gaze.hermes_wrath");
        }
        if (stack.getItem() instanceof XiheItem) {
            return Component.translatable(XiheItem.isBlessed(stack)
                    ? "message.gazeofthepantheon.gaze.xihe_kindness"
                    : "message.gazeofthepantheon.gaze.xihe_wrath");
        }
        if (stack.getItem() instanceof AchillesItem) {
            return Component.translatable(AchillesItem.isBlessed(stack)
                    ? "message.gazeofthepantheon.gaze.achilles_kindness"
                    : "message.gazeofthepantheon.gaze.achilles_wrath");
        }
        if (stack.getItem() instanceof DionysusItem) {
            return Component.translatable(DionysusItem.isBlessed(stack)
                    ? "message.gazeofthepantheon.gaze.dionysus_kindness"
                    : "message.gazeofthepantheon.gaze.dionysus_wrath");
        }
        return null;
    }

    // ============ decree / delete 通用 ============

    private static Item parseGazeItem(CommandSourceStack source, String id) {
        if (!id.contains(":")) {
            id = GazeOfThePantheon.MOD_ID + ":" + id;
        }

        ResourceLocation rl = ResourceLocation.tryParse(id);
        if (rl == null) {
            source.sendFailure(Component.literal("§c无效的物品 ID：" + id));
            return null;
        }

        Item item = ForgeRegistries.ITEMS.getValue(rl);
        if (item == null) {
            source.sendFailure(Component.literal("§c找不到物品：" + id));
            return null;
        }

        if (item != ModItems.THANATOS.get()
                && item != ModItems.HYGIEIA.get()
                && item != ModItems.ARES.get()
                && item != ModItems.HERMES.get()
                && item != ModItems.XIHE.get()
                && item != ModItems.ACHILLES.get()
                && item != ModItems.DIONYSUS.get()) {
            source.sendFailure(Component.literal("§c该物品不是注视饰品：" + id));
            return null;
        }

        return item;
    }

    private static int handleDecree(CommandSourceStack source, String id) {
        ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (Exception e) {
            source.sendFailure(Component.literal("§c该指令只能由玩家执行"));
            return 0;
        }

        Item item = parseGazeItem(source, id);
        if (item == null) return 0;

        if (CuriosUtil.convertToBlessed(player, item)) {
            player.sendSystemMessage(Component.translatable(
                    "message.gazeofthepantheon.command.decree.to_blessed", id));
            return 1;
        }
        if (CuriosUtil.convertToWrath(player, item)) {
            player.sendSystemMessage(Component.translatable(
                    "message.gazeofthepantheon.command.decree.to_wrath", id));
            return 1;
        }

        source.sendFailure(Component.translatable(
                "message.gazeofthepantheon.command.decree.not_found", id));
        return 0;
    }

    private static int handleDelete(CommandSourceStack source, String id) {
        ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (Exception e) {
            source.sendFailure(Component.literal("§c该指令只能由玩家执行"));
            return 0;
        }

        Item item = parseGazeItem(source, id);
        if (item == null) return 0;

        if (CuriosUtil.deleteGazeItem(player, item)) {
            player.sendSystemMessage(Component.translatable(
                    "message.gazeofthepantheon.command.delete.success", id));
            return 1;
        }

        source.sendFailure(Component.translatable(
                "message.gazeofthepantheon.command.delete.not_found", id));
        return 0;
    }

    private static int handleLikeMarkDirt(CommandSourceStack source) {
        ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (Exception e) {
            source.sendFailure(Component.literal("§c该指令只能由玩家执行"));
            return 0;
        }

        ItemStack dirt = ThanatosEventHandler.createMarkedDirt();
        if (!player.getInventory().add(dirt)) {
            player.drop(dirt, false);
        }
        return 1;
    }
}