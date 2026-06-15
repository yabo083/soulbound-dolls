package com.yabo.soulbounddolls.neoforge.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import net.minecraft.world.item.ItemDisplayContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerDollModelResourceTest {
    private static final String MODEL_RESOURCE = "assets/soulbound_dolls/models/item/player_doll_3d.json";
    private static final Set<String> EXPANDED_ROTATION_KEYS = Set.of("x", "y", "z");

    @Test
    void playerDoll3dUsesVanillaElementRotationFormat() {
        JsonObject root = loadModel();

        for (JsonElement element : root.getAsJsonArray("elements")) {
            JsonObject elementObject = element.getAsJsonObject();
            JsonElement rotationElement = elementObject.get("rotation");
            if (rotationElement == null) {
                continue;
            }

            JsonObject rotation = rotationElement.getAsJsonObject();
            String name = elementObject.has("name") ? elementObject.get("name").getAsString() : "<unnamed>";
            assertFalse(EXPANDED_ROTATION_KEYS.stream().anyMatch(rotation::has),
                    () -> name + " uses Blockbench expanded rotation keys instead of angle/axis");
            assertTrue(rotation.has("angle"), () -> name + " rotation is missing angle");
            assertTrue(rotation.has("axis"), () -> name + " rotation is missing axis");
        }
    }

    @Test
    void displayFallbacksDoNotCarryTunedModelPlacement() {
        for (ItemDisplayContext context : ItemDisplayContext.values()) {
            DollDisplayConfig.DisplayTransform transform = DollDisplayConfig.defaultTransforms().get(context);
            assertNotNull(transform, () -> context + " is missing a fallback transform");
            assertVector(transform.rotation(), 0.0F, 0.0F, 0.0F, context + " rotation");
            assertVector(transform.translation(), 0.0F, 0.0F, 0.0F, context + " translation");
            assertVector(transform.scale(), 1.0F, 1.0F, 1.0F, context + " scale");
        }
    }

    @Test
    void thirdPersonHandDisplayKeepsDollUpright() {
        JsonObject display = loadModel().getAsJsonObject("display");

        assertDisplayRotationX(display, "thirdperson_righthand", 0.0F);
        assertDisplayRotationX(display, "thirdperson_lefthand", 0.0F);
    }

    @Test
    void curiosHeadTagAllowsDollInHeadSlotWithoutApiDependency() {
        JsonObject tag = loadJson("data/curios/tags/item/head.json");

        assertFalse(tag.has("replace") && tag.get("replace").getAsBoolean());
        boolean found = false;
        for (JsonElement element : tag.getAsJsonArray("values")) {
            if ("soulbound_dolls:player_doll".equals(element.getAsString())) {
                found = true;
                break;
            }
        }
        assertTrue(found, "head tag must include soulbound_dolls:player_doll");
    }

    private static void assertVector(DollDisplayConfig.Vector3 vector, float x, float y, float z, String label) {
        assertEquals(x, vector.x(), 0.0001F, label + " x");
        assertEquals(y, vector.y(), 0.0001F, label + " y");
        assertEquals(z, vector.z(), 0.0001F, label + " z");
    }

    private static void assertDisplayRotationX(JsonObject display, String key, float x) {
        JsonElement rotation = display.getAsJsonObject(key).get("rotation");
        assertNotNull(rotation, () -> key + " is missing rotation");
        assertEquals(x, rotation.getAsJsonArray().get(0).getAsFloat(), 0.0001F, key + " rotation x");
    }

    private static JsonObject loadModel() {
        return loadJson(MODEL_RESOURCE);
    }

    private static JsonObject loadJson(String resourcePath) {
        InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourcePath);
        assertNotNull(stream, () -> "Missing test resource " + resourcePath);
        try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (Exception exception) {
            throw new AssertionError("Could not load " + resourcePath, exception);
        }
    }
}
