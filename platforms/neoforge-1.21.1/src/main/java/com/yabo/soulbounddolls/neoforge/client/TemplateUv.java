package com.yabo.soulbounddolls.neoforge.client;

final class TemplateUv {
    private TemplateUv() {
    }

    static float spriteCoordinate(float jsonUv) {
        return jsonUv / 16.0F;
    }
}
