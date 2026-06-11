package com.yabo.soulbounddolls.neoforge.skin;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.yggdrasil.ProfileResult;
import com.yabo.soulbounddolls.common.PlayerDollProfile;
import com.yabo.soulbounddolls.common.SkinTextureMetadata;
import java.util.Optional;
import net.minecraft.server.MinecraftServer;

public final class DollSkinResolver {
    private static final String TEXTURES_PROPERTY = "textures";

    private DollSkinResolver() {
    }

    public static Optional<Property> texturesProperty(GameProfile gameProfile) {
        if (gameProfile == null) {
            return Optional.empty();
        }
        return gameProfile.getProperties().get(TEXTURES_PROPERTY).stream().findFirst();
    }

    public static PlayerDollProfile fromGameProfile(GameProfile gameProfile, long nowMillis) {
        Optional<Property> textures = texturesProperty(gameProfile);
        String skinValue = textures.map(Property::value).orElse("");
        String skinSignature = textures.flatMap(property -> Optional.ofNullable(property.signature())).orElse("");
        return PlayerDollProfile.fromPlayer(
                gameProfile.getId(),
                gameProfile.getName(),
                Optional.of(skinValue),
                Optional.of(skinSignature),
                SkinTextureMetadata.isSlimModel(skinValue),
                nowMillis);
    }

    public static Optional<PlayerDollProfile> refreshOnline(MinecraftServer server, PlayerDollProfile current, long nowMillis) {
        if (server == null || current == null) {
            return Optional.empty();
        }
        ProfileResult onlineProfile = server.getSessionService().fetchProfile(current.uuid(), true);
        if (onlineProfile != null && texturesProperty(onlineProfile.profile()).isPresent()) {
            return Optional.of(fromGameProfile(onlineProfile.profile(), nowMillis));
        }

        Optional<GameProfile> cachedProfile = server.getProfileCache().get(current.uuid());
        return cachedProfile
                .filter(gameProfile -> texturesProperty(gameProfile).isPresent())
                .map(gameProfile -> fromGameProfile(gameProfile, nowMillis));
    }
}
