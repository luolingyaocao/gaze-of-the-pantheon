package com.onceheart.gazeofthepantheon.item;

import com.onceheart.gazeofthepantheon.util.CuriosUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.List;
import java.util.UUID;

public class EdictItem extends Item implements ICurioItem {

    // ============ 属性修饰符 UUID ============
    private static final UUID ARMOR_UUID        = UUID.fromString("ed1c7001-0001-0001-0001-000000000001");
    private static final UUID TOUGH_UUID        = UUID.fromString("ed1c7002-0002-0002-0002-000000000002");
    private static final UUID ATTACK_DMG_UUID   = UUID.fromString("ed1c7003-0003-0003-0003-000000000003");
    private static final UUID ATTACK_SPD_UUID   = UUID.fromString("ed1c7004-0004-0004-0004-000000000004");
    private static final UUID KNOCKBACK_UUID    = UUID.fromString("ed1c7005-0005-0005-0005-000000000005");

    public EdictItem(Properties properties) {
        super(properties);
    }

    // ============ Curios 栏位限制 ============

    @Override
    public boolean canEquip(SlotContext slotContext, ItemStack stack) {
        return "decision".equals(slotContext.identifier());
    }

    @Override
    public boolean canUnequip(SlotContext slotContext, ItemStack stack) {
        return true;
    }

    // ============ 属性维护 ============

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        if (!(slotContext.entity() instanceof ServerPlayer player)) return;
        if (player.tickCount % 20 != 0) return;
        applyAttributes(player);
    }

    @Override
    public void onEquip(SlotContext slotContext, ItemStack prevStack, ItemStack stack) {
        if (slotContext.entity() instanceof ServerPlayer player) {
            applyAttributes(player);
        }
    }

    @Override
    public void onUnequip(SlotContext slotContext, ItemStack newStack, ItemStack stack) {
        if (slotContext.entity() instanceof ServerPlayer player) {
            removeAttributes(player);
        }
    }

    private static void applyAttributes(ServerPlayer player) {
        apply(player, Attributes.ARMOR, ARMOR_UUID,
                "gazeofthepantheon.edict_armor", 20.0, AttributeModifier.Operation.ADDITION);
        apply(player, Attributes.ARMOR_TOUGHNESS, TOUGH_UUID,
                "gazeofthepantheon.edict_tough", 20.0, AttributeModifier.Operation.ADDITION);
        apply(player, Attributes.ATTACK_DAMAGE, ATTACK_DMG_UUID,
                "gazeofthepantheon.edict_attack", 2.0, AttributeModifier.Operation.MULTIPLY_TOTAL);
        apply(player, Attributes.ATTACK_SPEED, ATTACK_SPD_UUID,
                "gazeofthepantheon.edict_attack_speed", 10.0, AttributeModifier.Operation.ADDITION);
        apply(player, Attributes.ATTACK_KNOCKBACK, KNOCKBACK_UUID,
                "gazeofthepantheon.edict_knockback", 10.0, AttributeModifier.Operation.ADDITION);
    }

    private static void removeAttributes(ServerPlayer player) {
        remove(player, Attributes.ARMOR, ARMOR_UUID);
        remove(player, Attributes.ARMOR_TOUGHNESS, TOUGH_UUID);
        remove(player, Attributes.ATTACK_DAMAGE, ATTACK_DMG_UUID);
        remove(player, Attributes.ATTACK_SPEED, ATTACK_SPD_UUID);
        remove(player, Attributes.ATTACK_KNOCKBACK, KNOCKBACK_UUID);
    }

    private static void apply(ServerPlayer player, Attribute attr, UUID uuid,
                              String name, double amount, AttributeModifier.Operation op) {
        AttributeInstance inst = player.getAttribute(attr);
        if (inst == null) return;
        AttributeModifier existing = inst.getModifier(uuid);
        if (existing == null || existing.getAmount() != amount || existing.getOperation() != op) {
            if (existing != null) inst.removeModifier(uuid);
            inst.addTransientModifier(new AttributeModifier(uuid, name, amount, op));
        }
    }

    private static void remove(ServerPlayer player, Attribute attr, UUID uuid) {
        AttributeInstance inst = player.getAttribute(attr);
        if (inst != null && inst.getModifier(uuid) != null) {
            inst.removeModifier(uuid);
        }
    }

    // ============ 右键装备到决策栏位 ============

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide) {
            return InteractionResultHolder.success(stack);
        }

        if (CuriosUtil.hasDecisionEquipped(player)) {
            return InteractionResultHolder.fail(stack);
        }

        if (CuriosUtil.tryEquipToDecision(player, stack.copy())) {
            if (!player.isCreative()) {
                stack.shrink(1);
            }
            return InteractionResultHolder.success(stack);
        }
        return InteractionResultHolder.fail(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.gazeofthepantheon.edict.desc")
                .withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("tooltip.gazeofthepantheon.edict.stats")
                .withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.translatable("tooltip.gazeofthepantheon.edict.hint")
                .withStyle(ChatFormatting.GRAY));
    }
}