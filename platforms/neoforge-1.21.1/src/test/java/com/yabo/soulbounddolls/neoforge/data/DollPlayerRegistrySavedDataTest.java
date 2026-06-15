package com.yabo.soulbounddolls.neoforge.data;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.yabo.soulbounddolls.common.PlayerDollProfile;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DollPlayerRegistrySavedDataTest {
    @Test
    void upsertPreservesExistingSkinWhenIncomingProfileHasNoSkin() {
        DollPlayerRegistrySavedData registry = new DollPlayerRegistrySavedData();
        UUID uuid = UUID.fromString("00000000-0000-0000-0000-000000000101");
        PlayerDollProfile existing = PlayerDollProfile.of(uuid, "OldName", "skin-value", "skin-signature", true, 100L);
        PlayerDollProfile loginWithoutTextures = PlayerDollProfile.of(uuid, "NewName", "", "", false, 200L);

        registry.upsert(existing);
        registry.upsert(loginWithoutTextures);

        PlayerDollProfile stored = registry.find(uuid).orElseThrow();
        assertEquals("NewName", stored.name());
        assertEquals("skin-value", stored.skinValue());
        assertEquals("skin-signature", stored.skinSignature());
        assertEquals(true, stored.slimModel());
        assertEquals(200L, stored.lastUpdated());
    }
}
