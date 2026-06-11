package com.yabo.soulbounddolls.neoforge.item;

import com.yabo.soulbounddolls.common.PlayerDollProfile;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlayerDollItemTest {
    @Test
    void boundDollNameUsesPlayerName() {
        PlayerDollProfile profile = PlayerDollProfile.of(
                UUID.fromString("00000000-0000-0000-0000-000000000123"),
                "Steve",
                "skin-value",
                "skin-signature",
                false,
                1L);

        assertEquals("Steve的玩偶", PlayerDollItem.boundDollName(profile).getString());
    }
}
