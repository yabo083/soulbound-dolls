package com.yabo.soulbounddolls.neoforge.entity;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.Test;

class DollProjectileEntityTest {
    @Test
    void defaultItemIsNotNullForThrowableItemProjectileConstruction() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();

        assertNotNull(DollProjectileDefaults.defaultItem(),
                "ThrowableItemProjectile creates a synced ItemStack from getDefaultItem during construction");
    }
}
