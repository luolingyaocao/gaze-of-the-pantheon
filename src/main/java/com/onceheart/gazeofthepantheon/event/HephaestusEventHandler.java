package com.onceheart.gazeofthepantheon.event;

import com.onceheart.gazeofthepantheon.GazeOfThePantheon;
import com.onceheart.gazeofthepantheon.util.CuriosUtil;
import com.onceheart.gazeofthepantheon.util.EdictData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 赫菲斯托斯神系逻辑。
 *
 * - 炉渣（诅咒）：常驻挖掘疲劳 I，敕令激活与否都生效
 * - 烈焰（祝福）：常驻急迫 III，敕令激活时失效
 *
 * 药水时长 40 tick，每 20 tick 刷新一次，避免闪烁。
 */
@Mod.EventBusSubscriber(modid = GazeOfThePantheon.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class HephaestusEventHandler {

    private static final int REFRESH_INTERVAL = 20;
    private static final int EFFECT_DURATION = 40;

    private static final int WRATH_AMPLIFIER = 0; // 挖掘疲劳 I
    private static final int KINDNESS_AMPLIFIER = 2; // 急迫 III

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer player)) return;
        if (player.tickCount % REFRESH_INTERVAL != 0) return;

        boolean hasWrath = !CuriosUtil.findHephaestusWrath(player).isEmpty();
        boolean hasKindness = !CuriosUtil.findHephaestusKindness(player).isEmpty();

        // ---- 炉渣：诅咒，永远生效 ----
        if (hasWrath) {
            player.addEffect(new MobEffectInstance(
                    MobEffects.DIG_SLOWDOWN, EFFECT_DURATION,
                    WRATH_AMPLIFIER, true, false));
        }

        // ---- 烈焰：祝福，敕令激活时失效 ----
        boolean kindnessActive = hasKindness && !EdictData.isEdictActive(player);
        if (kindnessActive) {
            player.addEffect(new MobEffectInstance(
                    MobEffects.DIG_SPEED, EFFECT_DURATION,
                    KINDNESS_AMPLIFIER, true, false));
        }
    }
}