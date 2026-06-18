package com.yabo.soulbounddolls.neoforge.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.item.ItemDisplayContext;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

class DollDisplayConfigTest {
    @Test
    void defaultTransformsAreIdentityValues() {
        DollDisplayConfig.DisplayTransform gui = DollDisplayConfig.defaultTransforms().get(ItemDisplayContext.GUI);

        assertEquals(0.0F, gui.rotation().x());
        assertEquals(0.0F, gui.rotation().y());
        assertEquals(0.0F, gui.rotation().z());
        assertEquals(0.0F, gui.translation().x());
        assertEquals(0.0F, gui.translation().y());
        assertEquals(0.0F, gui.translation().z());
        assertEquals(1.0F, gui.scale().x());
        assertEquals(1.0F, gui.scale().y());
        assertEquals(1.0F, gui.scale().z());
    }

    @Test
    void parseDisplayTransformsKeepsIdentityForMissingContexts() {
        JsonObject display = new JsonObject();
        JsonObject fixed = new JsonObject();
        fixed.add("scale", vector(0.25F, 0.5F, 0.75F));
        display.add("fixed", fixed);

        DollDisplayConfig config = DollDisplayConfig.parse(display);

        assertEquals(0.25F, config.transform(ItemDisplayContext.FIXED).scale().x());
        assertEquals(0.5F, config.transform(ItemDisplayContext.FIXED).scale().y());
        assertEquals(0.75F, config.transform(ItemDisplayContext.FIXED).scale().z());
        assertEquals(1.0F, config.transform(ItemDisplayContext.GUI).scale().x());
    }

    @Test
    void headDisplayTransformLiftsVariantPosesAboveHelmetSlot() {
        JsonObject display = new JsonObject();
        JsonObject head = new JsonObject();
        head.add("translation", vector(0.0F, 14.0F, 0.0F));
        head.add("scale", vector(1.0F, 1.1F, 1.0F));
        display.add("head", head);

        DollDisplayConfig.DisplayTransform transform = DollDisplayConfig.parse(display).transform(ItemDisplayContext.HEAD);

        assertEquals(14.0F, transform.translation().y(), 0.0001F);
        assertEquals(1.1F, transform.scale().y(), 0.0001F);
    }

    @Test
    void displayTransformKeepsModelCenterAlignedWithVanillaItemRendererCentering() {
        DollDisplayConfig.DisplayTransform transform = new DollDisplayConfig.DisplayTransform(
                new DollDisplayConfig.Vector3(0.0F, 0.0F, 0.0F),
                new DollDisplayConfig.Vector3(0.0F, 0.0F, 0.0F),
                new DollDisplayConfig.Vector3(0.5F, 0.5F, 0.5F));
        PoseStack poseStack = new PoseStack();
        poseStack.translate(-0.5F, -0.5F, -0.5F);

        transform.apply(poseStack);

        Vector3f transformedCenter = poseStack.last().pose().transformPosition(0.5F, 0.5F, 0.5F, new Vector3f());
        assertEquals(0.0F, transformedCenter.x(), 0.0001F);
        assertEquals(0.0F, transformedCenter.y(), 0.0001F);
        assertEquals(0.0F, transformedCenter.z(), 0.0001F);
    }

    private static com.google.gson.JsonArray vector(float x, float y, float z) {
        com.google.gson.JsonArray array = new com.google.gson.JsonArray();
        array.add(x);
        array.add(y);
        array.add(z);
        return array;
    }
}
