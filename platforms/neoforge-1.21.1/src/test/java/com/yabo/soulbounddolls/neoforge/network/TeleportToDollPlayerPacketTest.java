package com.yabo.soulbounddolls.neoforge.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

class TeleportToDollPlayerPacketTest {
    @Test
    void safeTeleportCandidatesCoverThreeBlockSquareAroundTarget() {
        BlockPos center = new BlockPos(10, 64, -10);

        assertEquals(49, TeleportToDollPlayerPacket.safeTeleportCandidates(center).size());
        assertTrue(TeleportToDollPlayerPacket.safeTeleportCandidates(center).contains(center.offset(-3, 0, -3)));
        assertTrue(TeleportToDollPlayerPacket.safeTeleportCandidates(center).contains(center.offset(3, 0, 3)));
    }

    @Test
    void pruneExpiredCooldownsRemovesOnlyExpiredEntries() {
        UUID expired = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID active = UUID.fromString("00000000-0000-0000-0000-000000000002");
        Map<UUID, Long> cooldowns = new HashMap<>();
        cooldowns.put(expired, 1_000L);
        cooldowns.put(active, 1_900L);

        TeleportToDollPlayerPacket.pruneExpiredCooldowns(cooldowns, 2_000L, 500L);

        assertFalse(cooldowns.containsKey(expired));
        assertTrue(cooldowns.containsKey(active));
    }
}
