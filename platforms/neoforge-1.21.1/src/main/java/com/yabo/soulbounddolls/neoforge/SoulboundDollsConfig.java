package com.yabo.soulbounddolls.neoforge;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class SoulboundDollsConfig {
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.BooleanValue AUTO_GIVE_OWN_DOLL;
    public static final ModConfigSpec.BooleanValue ENABLE_ONLINE_SKIN_REFRESH;
    public static final ModConfigSpec.BooleanValue ALLOW_PAT_PARTICLES;
    public static final ModConfigSpec.BooleanValue ALLOW_PICKUP_BY_ANYONE;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        AUTO_GIVE_OWN_DOLL = builder
                .comment("Give players their own bound doll when they log in, if they do not already have one.")
                .define("autoGiveOwnDoll", true);
        ENABLE_ONLINE_SKIN_REFRESH = builder
                .comment("Refresh known player skin textures from Mojang services after login or admin command use.")
                .define("enableOnlineSkinRefresh", true);
        ALLOW_PAT_PARTICLES = builder
                .comment("Allow player doll pat interactions to emit heart particles.")
                .define("allowPatParticles", true);
        ALLOW_PICKUP_BY_ANYONE = builder
                .comment("Allow any player to pick up placed player dolls. When false, only the creator may pick them up.")
                .define("allowPickupByAnyone", false);

        SPEC = builder.build();
    }

    private SoulboundDollsConfig() {
    }
}
