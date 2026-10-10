package com.onceheart.gazeofthepantheon.mixin;

import com.onceheart.gazeofthepantheon.registry.ModItems;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 保护掉落物形式的神行不被火 / 爆炸销毁。
 *
 * ItemEntity 的所有伤害路径（火、岩浆、爆炸）都走 hurt()，
 * 在此 HEAD 处直接返回 false，令其不受伤、不销毁。
 *
 * 不拦：5 分钟自然消失、仙人掌、虚空、漏斗抽取。
 */
@Mixin(value = ItemEntity.class, priority = Integer.MAX_VALUE)
public class MixinItemEntity {

    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
    private void gaze$protectStop(DamageSource source, float amount,
                                  CallbackInfoReturnable<Boolean> cir) {
        ItemEntity self = (ItemEntity) (Object) this;
        if (self.getItem().getItem() == ModItems.STOP.get()) {
            cir.setReturnValue(false);
        }
    }
}