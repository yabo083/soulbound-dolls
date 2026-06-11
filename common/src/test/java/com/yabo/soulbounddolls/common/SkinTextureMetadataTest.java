package com.yabo.soulbounddolls.common;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.Test;

class SkinTextureMetadataTest {
    @Test
    void detectsSlimModelFromTextureJson() {
        String json = "{\"textures\":{\"SKIN\":{\"url\":\"http://textures.example/skin\",\"metadata\":{\"model\":\"slim\"}}}}";

        assertTrue(SkinTextureMetadata.isSlimModel(encode(json)));
    }

    @Test
    void treatsMissingOrClassicModelAsNotSlim() {
        String missingMetadata = "{\"textures\":{\"SKIN\":{\"url\":\"http://textures.example/skin\"}}}";
        String classic = "{\"textures\":{\"SKIN\":{\"metadata\":{\"model\":\"classic\"}}}}";

        assertFalse(SkinTextureMetadata.isSlimModel(encode(missingMetadata)));
        assertFalse(SkinTextureMetadata.isSlimModel(encode(classic)));
    }

    @Test
    void treatsMalformedTextureValueAsNotSlim() {
        assertFalse(SkinTextureMetadata.isSlimModel("not-base64"));
        assertFalse(SkinTextureMetadata.isSlimModel(""));
        assertFalse(SkinTextureMetadata.isSlimModel(null));
    }

    private static String encode(String json) {
        return Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }
}
