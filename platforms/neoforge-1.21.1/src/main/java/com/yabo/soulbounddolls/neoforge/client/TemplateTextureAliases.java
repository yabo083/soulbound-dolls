package com.yabo.soulbounddolls.neoforge.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;

final class TemplateTextureAliases {
    private TemplateTextureAliases() {
    }

    static Map<String, ResourceLocation> parse(JsonObject root) {
        Map<String, ResourceLocation> textures = new java.util.HashMap<>();
        JsonElement texturesElement = root.get("textures");
        if (texturesElement != null && texturesElement.isJsonObject()) {
            for (Map.Entry<String, JsonElement> entry : texturesElement.getAsJsonObject().entrySet()) {
                if (entry.getValue().isJsonPrimitive()) {
                    ResourceLocation location = ResourceLocation.tryParse(entry.getValue().getAsString());
                    if (location != null) {
                        textures.put("#" + entry.getKey(), location);
                    }
                }
            }
        }
        return Map.copyOf(textures);
    }
}
