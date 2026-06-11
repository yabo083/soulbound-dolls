package com.yabo.soulbounddolls.neoforge.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.gson.JsonObject;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class PlayerDollItemRendererTest {
    @Test
    void parseTextureAliasesKeepsItemModelTextureAsAtlasSpriteId() throws Exception {
        JsonObject textures = new JsonObject();
        textures.addProperty("dark", "soulbound_dolls:item/player_doll_dark");
        JsonObject model = new JsonObject();
        model.add("textures", textures);

        Map<String, ResourceLocation> aliases = TemplateTextureAliases.parse(model);

        assertEquals(
                ResourceLocation.fromNamespaceAndPath("soulbound_dolls", "item/player_doll_dark"),
                aliases.get("#dark"));
    }

    @Test
    void boundItemsUseEntityModelRenderStrategy() {
        assertEquals(PlayerDollItemRenderStrategy.ENTITY_MODEL, PlayerDollItemRenderStrategy.forBoundProfile(true));
        assertEquals(PlayerDollItemRenderStrategy.TEMPLATE_MODEL, PlayerDollItemRenderStrategy.forBoundProfile(false));
    }

    @Test
    void jsonUvConvertsToSpriteInterpolationCoordinates() {
        assertEquals(0.0F, TemplateUv.spriteCoordinate(0.0F));
        assertEquals(0.5F, TemplateUv.spriteCoordinate(8.0F));
        assertEquals(1.0F, TemplateUv.spriteCoordinate(16.0F));
    }
}
