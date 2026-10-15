package com.onceheart.gazeofthepantheon.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public class PrimordialHumanRealmEffects extends DimensionSpecialEffects {

    public PrimordialHumanRealmEffects() {
        super(192.0F, true, SkyType.NORMAL, false, false);
    }

    @Override
    public Vec3 getBrightnessDependentFogColor(Vec3 biomeFogColor, float daylight) {
        return biomeFogColor;
    }

    @Override
    public boolean isFoggyAt(int x, int y) {
        return false;
    }

    @Override
    public boolean renderClouds(ClientLevel level, int ticks, float partialTick,
                                PoseStack poseStack, double camX, double camY, double camZ,
                                Matrix4f projectionMatrix) {
        return true;
    }
}