package com.yabo.soulbounddolls.neoforge.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.yabo.soulbounddolls.common.PlayerDollProfile;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
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

    @Test
    void resolveCachesTextureAndAvoidsRepeatGameProfileBuilds() {
        AtomicInteger underlyingResolverCalls = new AtomicInteger();
        ResourceLocation texture = ResourceLocation.fromNamespaceAndPath("minecraft", "skins/cached");
        DollSkinManager manager = new DollSkinManager(gameProfile -> {
            underlyingResolverCalls.incrementAndGet();
            return texture;
        }, ResourceLocation.fromNamespaceAndPath("soulbound_dolls", "textures/entity/default_doll.png"));
        PlayerDollProfile profile = PlayerDollProfile.of(
                UUID.fromString("00000000-0000-0000-0000-000000000003"), "Steve", "skin-value", "skin-signature", false, 100L);

        // Simulate a render hot path: many frames resolving the same profile.
        int frames = 1000;
        for (int frame = 0; frame < frames; frame++) {
            assertEquals(texture, manager.resolve(profile));
        }

        // Without the cache this would be 1000 GameProfile builds + insecure-skin lookups.
        // With it, exactly one underlying resolve happens; the other 999 are cache hits.
        assertEquals(1, underlyingResolverCalls.get(),
                "Cached resolve should build the GameProfile only once across " + frames + " frames");
    }

    @Test
    void resolveRecomputesWhenSkinValueChanges() {
        AtomicInteger underlyingResolverCalls = new AtomicInteger();
        ResourceLocation texture = ResourceLocation.fromNamespaceAndPath("minecraft", "skins/changing");
        DollSkinManager manager = new DollSkinManager(gameProfile -> {
            underlyingResolverCalls.incrementAndGet();
            return texture;
        }, ResourceLocation.fromNamespaceAndPath("soulbound_dolls", "textures/entity/default_doll.png"));
        UUID uuid = UUID.fromString("00000000-0000-0000-0000-000000000004");

        PlayerDollProfile original = PlayerDollProfile.of(uuid, "Steve", "skin-v1", "sig-v1", false, 100L);
        manager.resolve(original);
        manager.resolve(original);
        assertEquals(1, underlyingResolverCalls.get(), "Same skin value should hit the cache");

        // A refreshed skin (same UUID, new texture value) must invalidate the cached entry.
        PlayerDollProfile refreshed = PlayerDollProfile.of(uuid, "Steve", "skin-v2", "sig-v2", false, 200L);
        manager.resolve(refreshed);
        assertEquals(2, underlyingResolverCalls.get(), "Changed skin value should recompute and refresh the cache");
    }

    @Test
    void resolveDoesNotCacheTemporaryDefaultPlayerTexture() {
        AtomicInteger underlyingResolverCalls = new AtomicInteger();
        ResourceLocation temporaryDefault = ResourceLocation.withDefaultNamespace("textures/entity/player/wide/steve.png");
        ResourceLocation resolvedSkin = ResourceLocation.withDefaultNamespace("skins/resolved");
        DollSkinManager manager = new DollSkinManager(gameProfile ->
                underlyingResolverCalls.incrementAndGet() == 1 ? temporaryDefault : resolvedSkin,
                ResourceLocation.fromNamespaceAndPath("soulbound_dolls", "textures/entity/default_doll.png"));
        PlayerDollProfile profile = PlayerDollProfile.of(
                UUID.fromString("00000000-0000-0000-0000-000000000005"), "Steve", "skin-value", "skin-signature", false, 100L);

        assertEquals(temporaryDefault, manager.resolve(profile));
        assertEquals(resolvedSkin, manager.resolve(profile));
        assertEquals(resolvedSkin, manager.resolve(profile));
        assertEquals(2, underlyingResolverCalls.get(), "Temporary default texture should not be cached, resolved texture should be cached");
    }

    @Test
    void resolveUsesLoadedPlayerSkinBeforeProfileLookup() {
        AtomicInteger underlyingResolverCalls = new AtomicInteger();
        UUID uuid = UUID.fromString("00000000-0000-0000-0000-000000000006");
        ResourceLocation loadedPlayerSkin = ResourceLocation.withDefaultNamespace("skins/already_loaded");
        DollSkinManager manager = new DollSkinManager(
                gameProfile -> {
                    underlyingResolverCalls.incrementAndGet();
                    return ResourceLocation.withDefaultNamespace("skins/slow_profile_lookup");
                },
                queriedUuid -> queriedUuid.equals(uuid) ? Optional.of(loadedPlayerSkin) : Optional.empty(),
                ResourceLocation.fromNamespaceAndPath("soulbound_dolls", "textures/entity/default_doll.png"));
        PlayerDollProfile profile = PlayerDollProfile.of(uuid, "LocalPlayer", "skin-value", "skin-signature", false, 100L);

        assertEquals(loadedPlayerSkin, manager.resolve(profile));
        assertEquals(0, underlyingResolverCalls.get(), "Loaded client player skin should avoid the slower profile lookup path");
    }

    @Test
    void resolveDerivesSkinLocationFromProfileWhenLookupReturnsTemporaryDefault() {
        AtomicInteger underlyingResolverCalls = new AtomicInteger();
        ResourceLocation temporaryDefault = ResourceLocation.withDefaultNamespace("textures/entity/player/slim/noor.png");
        DollSkinManager manager = new DollSkinManager(gameProfile -> {
            underlyingResolverCalls.incrementAndGet();
            return temporaryDefault;
        }, ResourceLocation.fromNamespaceAndPath("soulbound_dolls", "textures/entity/default_doll.png"));
        PlayerDollProfile profile = PlayerDollProfile.of(
                UUID.fromString("00000000-0000-0000-0000-000000000007"),
                "LocalPlayer",
                textureValue("0123456789abcdef0123456789abcdef01234567"),
                "skin-signature",
                true,
                100L);

        assertEquals(ResourceLocation.withDefaultNamespace("skins/5e0b675fcef4b8463b5e051a5adfd6100012f548"), manager.resolve(profile));
        assertEquals(1, underlyingResolverCalls.get(), "Vanilla lookup should still be started once so the texture can load");
        assertEquals(ResourceLocation.withDefaultNamespace("skins/5e0b675fcef4b8463b5e051a5adfd6100012f548"), manager.resolve(profile));
        assertEquals(1, underlyingResolverCalls.get(), "Derived profile texture should be cached like any resolved skin");
    }

    private static String textureValue(String skinHash) {
        String json = "{\"textures\":{\"SKIN\":{\"url\":\"http://textures.minecraft.net/texture/" + skinHash + "\",\"metadata\":{\"model\":\"slim\"}}}}";
        return Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }
}
