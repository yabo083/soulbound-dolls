package com.yabo.soulbounddolls.neoforge.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.yabo.soulbounddolls.common.PlayerDollProfile;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class DollSkinManagerTest {
    @Test
    void resolveReturnsDefaultTextureWhenProfileHasNoSkin() {
        ResourceLocation defaultTexture = ResourceLocation.fromNamespaceAndPath("soulbound_dolls", "textures/entity/default_doll.png");
        DollSkinManager manager = new DollSkinManager(gameProfile -> ResourceLocation.fromNamespaceAndPath("test", "unexpected"), defaultTexture);
        PlayerDollProfile profile = PlayerDollProfile.of(UUID.fromString("00000000-0000-0000-0000-000000000001"), "Alex", "", "", false, 100L);

        assertEquals(defaultTexture, manager.resolve(profile));
    }

    @Test
    void resolveBuildsGameProfileWithTexturesProperty() {
        AtomicReference<com.mojang.authlib.GameProfile> capturedProfile = new AtomicReference<>();
        ResourceLocation expectedTexture = ResourceLocation.fromNamespaceAndPath("minecraft", "skins/alex");
        DollSkinManager manager = new DollSkinManager(gameProfile -> {
            capturedProfile.set(gameProfile);
            return expectedTexture;
        }, ResourceLocation.fromNamespaceAndPath("soulbound_dolls", "textures/entity/default_doll.png"));
        UUID uuid = UUID.fromString("00000000-0000-0000-0000-000000000002");
        PlayerDollProfile profile = PlayerDollProfile.of(uuid, "Alex", "skin-value", "skin-signature", false, 100L);

        assertEquals(expectedTexture, manager.resolve(profile));
        assertEquals(uuid, capturedProfile.get().getId());
        assertEquals("Alex", capturedProfile.get().getName());
        assertEquals("skin-value", capturedProfile.get().getProperties().get("textures").stream().findFirst().orElseThrow().value());
        assertEquals("skin-signature", capturedProfile.get().getProperties().get("textures").stream().findFirst().orElseThrow().signature());
    }
}
