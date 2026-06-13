package com.yabo.soulbounddolls.neoforge.client;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.yabo.soulbounddolls.common.DollConstants;
import com.yabo.soulbounddolls.common.PlayerDollProfile;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

public final class DollSkinManager {
    public static final ResourceLocation DEFAULT_DOLL_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            DollConstants.MOD_ID,
            "textures/entity/default_doll.png");
    private static final DollSkinManager INSTANCE = new DollSkinManager(
            gameProfile -> Minecraft.getInstance().getSkinManager().getInsecureSkin(gameProfile).texture(),
            DEFAULT_DOLL_TEXTURE);

    private final Function<GameProfile, ResourceLocation> skinResolver;
    private final ResourceLocation defaultTexture;
    private final Map<UUID, CachedSkin> cache = new ConcurrentHashMap<>();

    DollSkinManager(Function<GameProfile, ResourceLocation> skinResolver, ResourceLocation defaultTexture) {
        this.skinResolver = Objects.requireNonNull(skinResolver, "skinResolver");
        this.defaultTexture = Objects.requireNonNull(defaultTexture, "defaultTexture");
    }

    public static DollSkinManager getInstance() {
        return INSTANCE;
    }

    /**
     * Resolves the doll texture for a profile. Called every frame by the entity and item renderers,
     * so resolved locations are cached per UUID and only recomputed when the profile's skin value
     * changes. A cache hit returns without allocating a {@link GameProfile}.
     */
    public ResourceLocation resolve(PlayerDollProfile profile) {
        if (profile == null || !profile.hasSkin()) {
            return defaultTexture;
        }

        UUID uuid = profile.uuid();
        String skinValue = profile.skinValue();
        CachedSkin cached = cache.get(uuid);
        if (cached != null && cached.matches(skinValue)) {
            return cached.texture();
        }

        ResourceLocation texture = skinResolver.apply(toGameProfile(profile));
        cache.put(uuid, new CachedSkin(skinValue, texture));
        return texture;
    }

    /** Clears all cached textures; used by tests and on resource reloads. */
    public void invalidateAll() {
        cache.clear();
    }

    static GameProfile toGameProfile(PlayerDollProfile profile) {
        GameProfile gameProfile = new GameProfile(profile.uuid(), profile.name());
        gameProfile.getProperties().put("textures", new Property("textures", profile.skinValue(), profile.skinSignature()));
        return gameProfile;
    }

    private record CachedSkin(String skinValue, ResourceLocation texture) {
        private boolean matches(String currentSkinValue) {
            return skinValue.equals(currentSkinValue);
        }
    }
}
