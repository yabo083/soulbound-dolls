package com.yabo.soulbounddolls.neoforge.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import net.minecraft.client.renderer.texture.TextureAtlas;
import org.junit.jupiter.api.Test;

class DollProjectileRendererTest {
    @Test
    void textureLocationSatisfiesEntityRendererContract() {
        assertNotNull(DollProjectileRenderer.textureLocation());
        assertEquals(TextureAtlas.LOCATION_BLOCKS, DollProjectileRenderer.textureLocation());
    }
}
