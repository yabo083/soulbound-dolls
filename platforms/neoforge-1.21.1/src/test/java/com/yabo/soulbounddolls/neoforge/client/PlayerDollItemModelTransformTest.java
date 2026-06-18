package com.yabo.soulbounddolls.neoforge.client;

import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerDollItemModelTransformTest {
    @Test
    void transformCentersEntityModelInItemModelSpace() {
        PoseStack poseStack = new PoseStack();

        PlayerDollItemModelTransform.apply(poseStack);

        Vector3f center = PlayerDollItemModelTransform.modelCenter();
        Vector3f transformedCenter = poseStack.last().pose().transformPosition(center.x(), center.y(), center.z(), new Vector3f());
        assertEquals(0.5F, transformedCenter.x(), 0.0001F);
        assertEquals(0.5F, transformedCenter.y(), 0.0001F);
        assertEquals(0.5F, transformedCenter.z(), 0.0001F);
    }

    @Test
    void transformKeepsEntityModelFrontFacingTemplateFront() {
        PoseStack poseStack = new PoseStack();

        PlayerDollItemModelTransform.apply(poseStack);

        Vector3f frontNormal = poseStack.last().normal().transform(new Vector3f(0.0F, 0.0F, -1.0F));
        assertTrue(frontNormal.z() < -0.99F, () -> "front normal was " + frontNormal);
    }

    @Test
    void negativeHeadSlotYOffsetMovesDollDownInDisplaySpace() {
        PoseStack baseline = new PoseStack();
        PlayerDollItemModelTransform.apply(baseline);
        Vector3f center = PlayerDollItemModelTransform.modelCenter();
        float baselineY = baseline.last().pose().transformPosition(center.x(), center.y(), center.z(), new Vector3f()).y();

        PoseStack adjusted = new PoseStack();
        PlayerDollItemModelTransform.apply(adjusted, new PlayerDollItemPose.Offset(0.0F, -0.5F, 0.0F));
        float adjustedY = adjusted.last().pose().transformPosition(center.x(), center.y(), center.z(), new Vector3f()).y();

        assertTrue(adjustedY < baselineY, () -> "negative Y offset should move down, baseline=" + baselineY + " adjusted=" + adjustedY);
    }
}
