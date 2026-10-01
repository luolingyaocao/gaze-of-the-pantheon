package com.onceheart.gazeofthepantheon.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.onceheart.gazeofthepantheon.GazeOfThePantheon;
import com.onceheart.gazeofthepantheon.event.ThanatosEventHandler;
import com.onceheart.gazeofthepantheon.registry.ModItems;
import com.onceheart.gazeofthepantheon.util.CuriosUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(modid = GazeOfThePantheon.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ModCommands {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {

        dispatcher.register(Commands.literal("decree")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("itemid", StringArgumentType.greedyString())
                        .executes(ctx -> handleDecree(ctx.getSource(),
                                StringArgumentType.getString(ctx, "itemid").trim()))));

        dispatcher.register(Commands.literal("delete")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("itemid", StringArgumentType.greedyString())
                        .executes(ctx -> handleDelete(ctx.getSource(),
                                StringArgumentType.getString(ctx, "itemid").trim()))));

        dispatcher.register(Commands.literal("ilikethemarkdirt")
                .executes(ctx -> handleLikeMarkDirt(ctx.getSource())));
    }

    private static Item parseGazeItem(CommandSourceStack source, String id) {
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
                && item != ModItems.XIHE.get()) {
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