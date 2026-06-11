package com.yabo.soulbounddolls.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PlayerDollProfileTest {
    private static final UUID PLAYER_UUID = UUID.fromString("12345678-1234-1234-1234-123456789abc");

    @Test
    void ofRequiresUuid() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> PlayerDollProfile.of(null, "Alex", "skin", "signature", false, 100L));

        assertEquals("uuid is required", exception.getMessage());
    }

    @Test
    void ofNormalizesMissingNamesAndSkinFields() {
        PlayerDollProfile profile = PlayerDollProfile.of(PLAYER_UUID, "  ", null, null, true, 200L);

        assertEquals(PLAYER_UUID, profile.uuid());
        assertEquals("Unknown Player", profile.name());
        assertEquals("", profile.skinValue());
        assertEquals("", profile.skinSignature());
        assertTrue(profile.slimModel());
        assertEquals(200L, profile.lastUpdated());
        assertFalse(profile.hasSkin());
    }

    @Test
    void fallbackIsDeterministicUnknownProfile() {
        PlayerDollProfile first = PlayerDollProfile.fallback(PLAYER_UUID);
        PlayerDollProfile second = PlayerDollProfile.fallback(PLAYER_UUID);

        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
        assertEquals(PLAYER_UUID, first.uuid());
        assertEquals("Unknown Player", first.name());
        assertEquals("", first.skinValue());
        assertEquals("", first.skinSignature());
        assertFalse(first.slimModel());
        assertEquals(0L, first.lastUpdated());
    }

    @Test
    void fromPlayerUsesOptionalSkinFields() {
        PlayerDollProfile profile = PlayerDollProfile.fromPlayer(
                PLAYER_UUID,
                "Alex",
                Optional.of("skin-value"),
                Optional.empty(),
                false,
                300L);

        assertEquals("Alex", profile.name());
        assertEquals("skin-value", profile.skinValue());
        assertEquals("", profile.skinSignature());
        assertFalse(profile.slimModel());
        assertEquals(300L, profile.lastUpdated());
    }

    @Test
    void hasSkinRequiresNonblankSkinValue() {
        assertFalse(PlayerDollProfile.of(PLAYER_UUID, "Alex", " ", "signature", false, 1L).hasSkin());
        assertTrue(PlayerDollProfile.of(PLAYER_UUID, "Alex", "skin-value", "", false, 1L).hasSkin());
    }

    @Test
    void equalsAndHashCodeUseUuidOnly() {
        PlayerDollProfile first = PlayerDollProfile.of(
                PLAYER_UUID,
                "Alex",
                "skin-value",
                "skin-signature",
                false,
                100L);
        PlayerDollProfile second = PlayerDollProfile.of(
                PLAYER_UUID,
                "Steve",
                "updated-skin-value",
                "updated-skin-signature",
                true,
                200L);

        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
    }

    @Test
    void profilesWithDifferentUuidsAreNotEqual() {
        PlayerDollProfile first = PlayerDollProfile.of(PLAYER_UUID, "Alex", "skin", "signature", false, 100L);
        PlayerDollProfile second = PlayerDollProfile.of(
                UUID.fromString("87654321-4321-4321-4321-cba987654321"),
                "Alex",
                "skin",
                "signature",
                false,
                100L);

        assertNotEquals(first, second);
    }

    @Test
    void withNamePreservesSkinFieldsAndUpdatesTimestamp() {
        PlayerDollProfile original = PlayerDollProfile.of(PLAYER_UUID, "Alex", "skin", "signature", true, 100L);

        PlayerDollProfile updated = original.withName(null, 400L);

        assertEquals(PLAYER_UUID, updated.uuid());
        assertEquals("Unknown Player", updated.name());
        assertEquals("skin", updated.skinValue());
        assertEquals("signature", updated.skinSignature());
        assertTrue(updated.slimModel());
        assertEquals(400L, updated.lastUpdated());
    }

    @Test
    void withSkinPreservesUuidAndNameAndUpdatesSkinFields() {
        PlayerDollProfile original = PlayerDollProfile.of(PLAYER_UUID, "Alex", "old", "old-signature", false, 100L);

        PlayerDollProfile updated = original.withSkin(null, "new-signature", true, 500L);

        assertEquals(PLAYER_UUID, updated.uuid());
        assertEquals("Alex", updated.name());
        assertEquals("", updated.skinValue());
        assertEquals("new-signature", updated.skinSignature());
        assertTrue(updated.slimModel());
        assertEquals(500L, updated.lastUpdated());
    }
}
