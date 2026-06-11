package com.yabo.soulbounddolls.common;

import java.util.Optional;
import java.util.UUID;

public record PlayerDollProfile(
        UUID uuid,
        String name,
        String skinValue,
        String skinSignature,
        boolean slimModel,
        long lastUpdated) {
    public static final String UNKNOWN_PLAYER_NAME = "Unknown Player";

    public PlayerDollProfile {
        if (uuid == null) {
            throw new IllegalArgumentException("uuid is required");
        }
        name = normalizeName(name);
        skinValue = normalizeSkinField(skinValue);
        skinSignature = normalizeSkinField(skinSignature);
    }

    public static PlayerDollProfile fallback(UUID uuid) {
        return of(uuid, UNKNOWN_PLAYER_NAME, "", "", false, 0L);
    }

    public static PlayerDollProfile fromPlayer(
            UUID uuid,
            String name,
            Optional<String> skinValue,
            Optional<String> skinSignature,
            boolean slimModel,
            long nowMillis) {
        return of(
                uuid,
                name,
                skinValue.orElse(""),
                skinSignature.orElse(""),
                slimModel,
                nowMillis);
    }

    public static PlayerDollProfile of(
            UUID uuid,
            String name,
            String skinValue,
            String skinSignature,
            boolean slimModel,
            long lastUpdated) {
        return new PlayerDollProfile(uuid, name, skinValue, skinSignature, slimModel, lastUpdated);
    }

    public boolean hasSkin() {
        return !skinValue.isBlank();
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj || obj instanceof PlayerDollProfile other && uuid.equals(other.uuid);
    }

    @Override
    public int hashCode() {
        return uuid.hashCode();
    }

    public PlayerDollProfile withName(String name, long nowMillis) {
        return of(uuid, name, skinValue, skinSignature, slimModel, nowMillis);
    }

    public PlayerDollProfile withSkin(String skinValue, String skinSignature, boolean slimModel, long nowMillis) {
        return of(uuid, name, skinValue, skinSignature, slimModel, nowMillis);
    }

    private static String normalizeName(String name) {
        if (name == null || name.isBlank()) {
            return UNKNOWN_PLAYER_NAME;
        }
        return name;
    }

    private static String normalizeSkinField(String skinField) {
        return skinField == null ? "" : skinField;
    }
}
