package com.yabo.soulbounddolls.neoforge;

import com.yabo.soulbounddolls.common.PlayerDollProfile;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;

public final class DollSkinDiagnostics {
    public static final String PREFIX = "[SoulboundDollsSkin]";

    private DollSkinDiagnostics() {
    }

    public static String profileSummary(PlayerDollProfile profile) {
        if (profile == null) {
            return "profile=null";
        }
        return "uuid=" + shortUuid(profile.uuid())
                + " name=" + profile.name()
                + " hasSkin=" + profile.hasSkin()
                + " skin=" + redactedField(profile.skinValue())
                + " sig=" + redactedField(profile.skinSignature())
                + " slim=" + profile.slimModel()
                + " updated=" + profile.lastUpdated();
    }

    public static String textureSummary(ResourceLocation texture) {
        return texture == null ? "texture=null" : "texture=" + texture;
    }

    public static String shortUuid(UUID uuid) {
        if (uuid == null) {
            return "null";
        }
        String value = uuid.toString();
        return value.substring(0, Math.min(8, value.length()));
    }

    private static String redactedField(String value) {
        if (value == null || value.isBlank()) {
            return "empty";
        }
        return "len" + value.length() + "/hash" + Integer.toHexString(value.hashCode());
    }
}
