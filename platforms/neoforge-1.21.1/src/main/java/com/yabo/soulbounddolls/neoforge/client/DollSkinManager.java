package com.yabo.soulbounddolls.neoforge.client;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.yabo.soulbounddolls.common.DollConstants;
import com.yabo.soulbounddolls.common.PlayerDollProfile;
import java.util.Objects;
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

    DollSkinManager(Function<GameProfile, ResourceLocation> skinResolver, ResourceLocation defaultTexture) {
        this.skinResolver = Objects.requireNonNull(skinResolver, "skinResolver");
        this.defaultTexture = Objects.requireNonNull(defaultTexture, "defaultTexture");
    }

    public static DollSkinManager getInstance() {
        return INSTANCE;
    }

    public ResourceLocation resolve(PlayerDollProfile profile) {
        if (profile == null || !profile.hasSkin()) {
            return defaultTexture;
        }
        return skinResolver.apply(toGameProfile(profile));
    }

    static GameProfile toGameProfile(PlayerDollProfile profile) {
        GameProfile gameProfile = new GameProfile(profile.uuid(), profile.name());
        gameProfile.getProperties().put("textures", new Property("textures", profile.skinValue(), profile.skinSignature()));
        return gameProfile;
    }
}
