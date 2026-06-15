package com.yabo.soulbounddolls.neoforge.client;

import com.google.common.hash.Hashing;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.yabo.soulbounddolls.common.DollConstants;
import com.yabo.soulbounddolls.common.PlayerDollProfile;
import com.yabo.soulbounddolls.neoforge.DollSkinDiagnostics;
import com.yabo.soulbounddolls.neoforge.SoulboundDollsNeoForge;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.texture.HttpTexture;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;

public final class DollSkinManager {
    public static final ResourceLocation DEFAULT_DOLL_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            DollConstants.MOD_ID,
            "textures/entity/default_doll.png");
    private static final String DEFAULT_PLAYER_TEXTURE_PREFIX = "textures/entity/player/";
    private static final DollSkinManager INSTANCE = new DollSkinManager(
            gameProfile -> Minecraft.getInstance().getSkinManager().getInsecureSkin(gameProfile).texture(),
            DollSkinManager::resolveLoadedPlayerSkin,
            DollSkinManager::registerPackedProfileSkin,
            DEFAULT_DOLL_TEXTURE);

    private final Function<GameProfile, ResourceLocation> skinResolver;
    private final Function<UUID, Optional<ResourceLocation>> loadedPlayerSkinResolver;
    private final Function<PlayerDollProfile, Optional<ResourceLocation>> profileTextureResolver;
    private final ResourceLocation defaultTexture;
    private final Map<UUID, CachedSkin> cache = new ConcurrentHashMap<>();

    DollSkinManager(Function<GameProfile, ResourceLocation> skinResolver, ResourceLocation defaultTexture) {
        this(skinResolver, uuid -> Optional.empty(), defaultTexture);
    }

    DollSkinManager(
            Function<GameProfile, ResourceLocation> skinResolver,
            Function<UUID, Optional<ResourceLocation>> loadedPlayerSkinResolver,
            ResourceLocation defaultTexture) {
        this(skinResolver, loadedPlayerSkinResolver, DollSkinManager::derivePackedProfileSkinLocation, defaultTexture);
    }

    DollSkinManager(
            Function<GameProfile, ResourceLocation> skinResolver,
            Function<UUID, Optional<ResourceLocation>> loadedPlayerSkinResolver,
            Function<PlayerDollProfile, Optional<ResourceLocation>> profileTextureResolver,
            ResourceLocation defaultTexture) {
        this.skinResolver = Objects.requireNonNull(skinResolver, "skinResolver");
        this.loadedPlayerSkinResolver = Objects.requireNonNull(loadedPlayerSkinResolver, "loadedPlayerSkinResolver");
        this.profileTextureResolver = Objects.requireNonNull(profileTextureResolver, "profileTextureResolver");
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
            logResolveProfile("no-profile-or-skin", profile);
            return defaultTexture;
        }

        UUID uuid = profile.uuid();
        String skinValue = profile.skinValue();
        CachedSkin cached = cache.get(uuid);
        if (cached != null && cached.matches(skinValue) && !cached.allowsLoadedPlayerRefresh()) {
            logResolveTexture("cache-hit", profile, cached.texture());
            return cached.texture();
        }

        Optional<ResourceLocation> loadedPlayerSkin = loadedPlayerSkinResolver.apply(uuid);
        if (loadedPlayerSkin.isPresent() && !isTemporaryDefaultPlayerTexture(loadedPlayerSkin.get())) {
            ResourceLocation texture = loadedPlayerSkin.get();
            cache.put(uuid, new CachedSkin(skinValue, texture, false));
            logResolveTexture("loaded-player-hit", profile, texture);
            return texture;
        }
        loadedPlayerSkin.ifPresent(texture -> logResolveTexture("loaded-player-temporary-default", profile, texture));
        if (cached != null && cached.matches(skinValue)) {
            logResolveTexture("cache-hit", profile, cached.texture());
            return cached.texture();
        }

        ResourceLocation texture = skinResolver.apply(toGameProfile(profile));
        if (!isTemporaryDefaultPlayerTexture(texture)) {
            cache.put(uuid, new CachedSkin(skinValue, texture, false));
            logResolveTexture("profile-lookup-hit", profile, texture);
        } else {
            Optional<ResourceLocation> packedTexture = profileTextureResolver.apply(profile);
            if (packedTexture.isPresent()) {
                ResourceLocation packedLocation = packedTexture.get();
                cache.put(uuid, new CachedSkin(skinValue, packedLocation, true));
                logResolveTexture("profile-property-hit", profile, packedLocation);
                return packedLocation;
            }
            logResolveTexture("profile-lookup-temporary-default", profile, texture);
        }
        return texture;
    }

    private static void logResolveProfile(String event, PlayerDollProfile profile) {
        if (SoulboundDollsNeoForge.LOGGER.isDebugEnabled()) {
            SoulboundDollsNeoForge.LOGGER.debug(
                    "{} client-resolve {} {}",
                    DollSkinDiagnostics.PREFIX,
                    event,
                    DollSkinDiagnostics.profileSummary(profile));
        }
    }

    private static void logResolveTexture(String event, PlayerDollProfile profile, ResourceLocation texture) {
        if (SoulboundDollsNeoForge.LOGGER.isDebugEnabled()) {
            SoulboundDollsNeoForge.LOGGER.debug(
                    "{} client-resolve {} {} {}",
                    DollSkinDiagnostics.PREFIX,
                    event,
                    DollSkinDiagnostics.profileSummary(profile),
                    DollSkinDiagnostics.textureSummary(texture));
        }
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

    private static Optional<ResourceLocation> resolveLoadedPlayerSkin(UUID uuid) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return Optional.empty();
        }
        for (AbstractClientPlayer player : minecraft.level.players()) {
            if (player.getUUID().equals(uuid)) {
                return Optional.of(player.getSkin().texture());
            }
        }
        return Optional.empty();
    }

    private static Optional<ResourceLocation> registerPackedProfileSkin(PlayerDollProfile profile) {
        Optional<PackedSkinTexture> packedTexture = packedSkinTexture(profile);
        packedTexture.ifPresent(texture -> {
            Minecraft minecraft = Minecraft.getInstance();
            Path skinCachePath = minecraft.gameDirectory.toPath()
                    .resolve(DollConstants.MOD_ID)
                    .resolve("skins")
                    .resolve(texture.hash().length() > 2 ? texture.hash().substring(0, 2) : "xx")
                    .resolve(texture.hash());
            minecraft.getTextureManager().register(
                    texture.location(),
                    new HttpTexture(skinCachePath.toFile(), texture.url(), DefaultPlayerSkin.getDefaultTexture(), true, null));
        });
        return packedTexture.map(PackedSkinTexture::location);
    }

    static Optional<ResourceLocation> derivePackedProfileSkinLocation(PlayerDollProfile profile) {
        return packedSkinTexture(profile).map(PackedSkinTexture::location);
    }

    private static Optional<PackedSkinTexture> packedSkinTexture(PlayerDollProfile profile) {
        if (profile == null || !profile.hasSkin()) {
            return Optional.empty();
        }
        try {
            String json = new String(Base64.getDecoder().decode(profile.skinValue()), StandardCharsets.UTF_8);
            JsonElement root = JsonParser.parseString(json);
            if (!root.isJsonObject()) {
                return Optional.empty();
            }
            JsonElement textures = root.getAsJsonObject().get("textures");
            if (textures == null || !textures.isJsonObject()) {
                return Optional.empty();
            }
            JsonElement skin = textures.getAsJsonObject().get("SKIN");
            if (skin == null || !skin.isJsonObject()) {
                return Optional.empty();
            }
            JsonObject skinObject = skin.getAsJsonObject();
            JsonElement url = skinObject.get("url");
            if (url == null || !url.isJsonPrimitive()) {
                return Optional.empty();
            }
            String skinUrl = url.getAsString();
            return skinHash(skinUrl).map(hash -> new PackedSkinTexture(hash, skinUrl, vanillaSkinLocation(hash)));
        } catch (RuntimeException exception) {
            return Optional.empty();
        }
    }

    private static Optional<String> skinHash(String skinUrl) {
        try {
            URI uri = URI.create(skinUrl);
            String scheme = uri.getScheme();
            String host = uri.getHost();
            if (scheme == null || host == null) {
                return Optional.empty();
            }
            if ((!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme))
                    || !"textures.minecraft.net".equalsIgnoreCase(host)) {
                return Optional.empty();
            }
            String path = uri.getPath();
            int lastSlash = path.lastIndexOf('/');
            String hash = lastSlash >= 0 ? path.substring(lastSlash + 1) : path;
            if (!hash.matches("[0-9a-fA-F]{40}")) {
                return Optional.empty();
            }
            return Optional.of(hash.toLowerCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    private static ResourceLocation vanillaSkinLocation(String skinHash) {
        return ResourceLocation.withDefaultNamespace("skins/" + Hashing.sha1().hashUnencodedChars(skinHash));
    }

    private static boolean isTemporaryDefaultPlayerTexture(ResourceLocation texture) {
        return "minecraft".equals(texture.getNamespace()) && texture.getPath().startsWith(DEFAULT_PLAYER_TEXTURE_PREFIX);
    }

    private record PackedSkinTexture(String hash, String url, ResourceLocation location) {
    }

    private record CachedSkin(String skinValue, ResourceLocation texture, boolean allowsLoadedPlayerRefresh) {
        private boolean matches(String currentSkinValue) {
            return skinValue.equals(currentSkinValue);
        }
    }
}
