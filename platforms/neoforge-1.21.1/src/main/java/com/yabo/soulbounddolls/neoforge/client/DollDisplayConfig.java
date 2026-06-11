package com.yabo.soulbounddolls.neoforge.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.world.item.ItemDisplayContext;

public final class DollDisplayConfig {
    private static final Map<ItemDisplayContext, DisplayTransform> DEFAULT_TRANSFORMS = createDefaultTransforms();

    private final Map<ItemDisplayContext, DisplayTransform> transforms;

    private DollDisplayConfig(Map<ItemDisplayContext, DisplayTransform> transforms) {
        this.transforms = Map.copyOf(transforms);
    }

    public static DollDisplayConfig defaults() {
        return new DollDisplayConfig(DEFAULT_TRANSFORMS);
    }

    static Map<ItemDisplayContext, DisplayTransform> defaultTransforms() {
        return DEFAULT_TRANSFORMS;
    }

    public static DollDisplayConfig parse(JsonObject display) {
        Map<ItemDisplayContext, DisplayTransform> transforms = new EnumMap<>(DEFAULT_TRANSFORMS);
        parseDisplayTransform(display, "gui", ItemDisplayContext.GUI, transforms);
        parseDisplayTransform(display, "firstperson_righthand", ItemDisplayContext.FIRST_PERSON_RIGHT_HAND, transforms);
        parseDisplayTransform(display, "firstperson_lefthand", ItemDisplayContext.FIRST_PERSON_LEFT_HAND, transforms);
        parseDisplayTransform(display, "thirdperson_righthand", ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, transforms);
        parseDisplayTransform(display, "thirdperson_lefthand", ItemDisplayContext.THIRD_PERSON_LEFT_HAND, transforms);
        parseDisplayTransform(display, "ground", ItemDisplayContext.GROUND, transforms);
        parseDisplayTransform(display, "head", ItemDisplayContext.HEAD, transforms);
        parseDisplayTransform(display, "fixed", ItemDisplayContext.FIXED, transforms);
        return new DollDisplayConfig(transforms);
    }

    public DisplayTransform transform(ItemDisplayContext context) {
        return transforms.getOrDefault(context, DEFAULT_TRANSFORMS.get(ItemDisplayContext.NONE));
    }

    public void apply(ItemDisplayContext context, PoseStack poseStack) {
        transform(context).apply(poseStack);
    }

    private static void parseDisplayTransform(JsonObject display, String key, ItemDisplayContext context, Map<ItemDisplayContext, DisplayTransform> transforms) {
        JsonElement element = display.get(key);
        if (element == null || !element.isJsonObject()) {
            return;
        }
        JsonObject transform = element.getAsJsonObject();
        transforms.put(context, new DisplayTransform(
                parseVector(transform, "rotation", 0.0F, 0.0F, 0.0F),
                parseVector(transform, "translation", 0.0F, 0.0F, 0.0F),
                parseVector(transform, "scale", 1.0F, 1.0F, 1.0F)));
    }

    static Vector3 parseVector(JsonObject transform, String key, float defaultX, float defaultY, float defaultZ) {
        JsonElement element = transform.get(key);
        if (element == null) {
            return new Vector3(defaultX, defaultY, defaultZ);
        }
        if (!element.isJsonArray()) {
            throw new IllegalArgumentException("Expected array for " + key);
        }
        JsonArray array = element.getAsJsonArray();
        if (array.size() != 3) {
            throw new IllegalArgumentException("Expected three values for " + key);
        }
        return new Vector3(array.get(0).getAsFloat(), array.get(1).getAsFloat(), array.get(2).getAsFloat());
    }

    private static Map<ItemDisplayContext, DisplayTransform> createDefaultTransforms() {
        Map<ItemDisplayContext, DisplayTransform> transforms = new EnumMap<>(ItemDisplayContext.class);
        DisplayTransform identity = new DisplayTransform(new Vector3(0.0F, 0.0F, 0.0F), new Vector3(0.0F, 0.0F, 0.0F), new Vector3(1.0F, 1.0F, 1.0F));
        for (ItemDisplayContext context : ItemDisplayContext.values()) {
            transforms.put(context, identity);
        }
        return Map.copyOf(transforms);
    }

    public record DisplayTransform(Vector3 rotation, Vector3 translation, Vector3 scale) {
        void apply(PoseStack poseStack) {
            // ItemRenderer has already centered builtin/entity items before calling the custom renderer.
            poseStack.translate(0.5F, 0.5F, 0.5F);
            poseStack.translate(translation.x() / 16.0F,  translation.y() / 16.0F,  translation.z() / 16.0F);
            poseStack.mulPose(Axis.XP.rotationDegrees(rotation.x()));
            poseStack.mulPose(Axis.YP.rotationDegrees(rotation.y()));
            poseStack.mulPose(Axis.ZP.rotationDegrees(rotation.z()));
            poseStack.scale(scale.x(), scale.y(), scale.z());
            poseStack.translate(-0.5F, -0.5F, -0.5F);
        }
    }

    public record Vector3(float x, float y, float z) {
    }
}
