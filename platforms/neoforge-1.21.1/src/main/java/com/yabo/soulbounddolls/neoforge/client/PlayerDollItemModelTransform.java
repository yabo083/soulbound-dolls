package com.yabo.soulbounddolls.neoforge.client;

import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Vector3f;

final class PlayerDollItemModelTransform {
    private static final float SCALE = 0.62F;
    private static final Vector3f MODEL_CENTER = new Vector3f(0.0F, 0.4921875F, -0.075F);

    private PlayerDollItemModelTransform() {
    }

    static void apply(PoseStack poseStack) {
        poseStack.translate(0.5F + MODEL_CENTER.x() * SCALE, 0.5F + MODEL_CENTER.y() * SCALE, 0.5F - MODEL_CENTER.z() * SCALE);
        poseStack.scale(-SCALE, -SCALE, SCALE);
    }

    static Vector3f modelCenter() {
        return new Vector3f(MODEL_CENTER);
    }
}
