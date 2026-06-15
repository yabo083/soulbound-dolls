package com.yabo.soulbounddolls.neoforge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.yabo.soulbounddolls.common.PlayerDollProfile;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DollProfileSynchronizerTest {
    private static final UUID PLAYER_UUID = UUID.fromString("00000000-0000-0000-0000-000000000201");

    @Test
    void choosesNewerRegistrySkinForStalePlacedDoll() {
        PlayerDollProfile placed = PlayerDollProfile.of(PLAYER_UUID, "Alex", "old-skin", "old-signature", false, 100L);
        PlayerDollProfile registry = PlayerDollProfile.of(PLAYER_UUID, "Alex", "new-skin", "new-signature", true, 200L);

        Optional<PlayerDollProfile> update = DollProfileSynchronizer.updatedFromRegistry(placed, Optional.of(registry));

        assertEquals(Optional.of(registry), update);
    }

    @Test
    void keepsPlacedDollWhenRegistrySkinIsOlder() {
        PlayerDollProfile placed = PlayerDollProfile.of(PLAYER_UUID, "Alex", "current-skin", "current-signature", true, 200L);
        PlayerDollProfile registry = PlayerDollProfile.of(PLAYER_UUID, "Alex", "old-skin", "old-signature", false, 100L);

        assertTrue(DollProfileSynchronizer.updatedFromRegistry(placed, Optional.of(registry)).isEmpty());
    }

    @Test
    void ignoresRegistryProfilesWithoutSkin() {
        PlayerDollProfile placed = PlayerDollProfile.of(PLAYER_UUID, "Alex", "current-skin", "current-signature", true, 200L);
        PlayerDollProfile registry = PlayerDollProfile.of(PLAYER_UUID, "Alex", "", "", false, 300L);

        assertTrue(DollProfileSynchronizer.updatedFromRegistry(placed, Optional.of(registry)).isEmpty());
    }
}
