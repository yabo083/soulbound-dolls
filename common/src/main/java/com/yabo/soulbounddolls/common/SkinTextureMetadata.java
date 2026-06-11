package com.yabo.soulbounddolls.common;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.regex.Pattern;

public final class SkinTextureMetadata {
    private static final Pattern SLIM_MODEL_PATTERN = Pattern.compile("\\\"model\\\"\\s*:\\s*\\\"slim\\\"");

    private SkinTextureMetadata() {
    }

    public static boolean isSlimModel(String textureValue) {
        if (textureValue == null || textureValue.isBlank()) {
            return false;
        }

        try {
            String json = new String(Base64.getDecoder().decode(textureValue), StandardCharsets.UTF_8);
            return SLIM_MODEL_PATTERN.matcher(json).find();
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }
}
