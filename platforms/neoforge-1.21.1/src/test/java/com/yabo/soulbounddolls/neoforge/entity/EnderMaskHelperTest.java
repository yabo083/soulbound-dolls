package com.yabo.soulbounddolls.neoforge.entity;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class EnderMaskHelperTest {
    @Test
    void headSlotDollProtectsWithoutCurios() {
        assertTrue(EnderMaskHelper.isProtected(true, () -> false));
    }

    @Test
    void curiosCanProtectWhenHeadSlotDoesNot() {
        assertTrue(EnderMaskHelper.isProtected(false, () -> true));
    }

    @Test
    void noDollDoesNotProtect() {
        assertFalse(EnderMaskHelper.isProtected(false, () -> false));
    }
}
