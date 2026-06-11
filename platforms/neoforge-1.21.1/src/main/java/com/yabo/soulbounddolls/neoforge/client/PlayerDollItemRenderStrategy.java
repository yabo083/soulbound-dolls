package com.yabo.soulbounddolls.neoforge.client;

enum PlayerDollItemRenderStrategy {
    ENTITY_MODEL,
    TEMPLATE_MODEL;

    static PlayerDollItemRenderStrategy forBoundProfile(boolean boundToPlayer) {
        return boundToPlayer ? ENTITY_MODEL : TEMPLATE_MODEL;
    }
}
