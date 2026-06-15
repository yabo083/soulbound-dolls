package com.yabo.soulbounddolls.neoforge;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class SoulboundDollsConfig {
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.BooleanValue AUTO_GIVE_OWN_DOLL;
    public static final ModConfigSpec.BooleanValue ENABLE_ONLINE_SKIN_REFRESH;
    public static final ModConfigSpec.BooleanValue ALLOW_PAT_PARTICLES;
    public static final ModConfigSpec.BooleanValue ALLOW_PICKUP_BY_ANYONE;
    public static final ModConfigSpec.IntValue SKIN_REFRESH_TTL_MINUTES;

    // 0.1.2 features
    public static final ModConfigSpec.BooleanValue ALLOW_DOLL_AS_HELMET;
    public static final ModConfigSpec.BooleanValue ENABLE_TELEPORT_TO_PLAYER;
    public static final ModConfigSpec.IntValue TELEPORT_COOLDOWN_SECONDS;
    public static final ModConfigSpec.BooleanValue ENABLE_THROW_DOLL;
    public static final ModConfigSpec.DoubleValue THROW_DOLL_DAMAGE;
    public static final ModConfigSpec.BooleanValue ENABLE_ATTRACT_UNDEAD;
    public static final ModConfigSpec.DoubleValue ATTRACT_UNDEAD_RANGE;
    public static final ModConfigSpec.BooleanValue ENABLE_REPEL_PHANTOMS;
    public static final ModConfigSpec.DoubleValue REPEL_PHANTOMS_RANGE;
    public static final ModConfigSpec.BooleanValue ENABLE_ENDER_MASK_PROTECTION;

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
        SKIN_REFRESH_TTL_MINUTES = builder
                .comment(
                        "Minimum minutes between online skin refreshes for the same player. Cached skins are reused",
                        "within this window so the server does not re-query Mojang on every login. Higher values",
                        "reduce network traffic; lower values pick up skin changes sooner.")
                .defineInRange("skinRefreshTtlMinutes", 60, 1, 7 * 24 * 60);

        // 0.1.2 feature configurations
        ALLOW_DOLL_AS_HELMET = builder
                .comment("Allow player dolls to be equipped as helmet armor by players and other entities.")
                .define("allowDollAsHelmet", true);

        ENABLE_TELEPORT_TO_PLAYER = builder
                .comment("Enable teleportation to the bound player when right-clicking a doll item with a keybind.",
                        "The target player must be online and in the same dimension.")
                .define("enableTeleportToPlayer", true);

        TELEPORT_COOLDOWN_SECONDS = builder
                .comment("Cooldown in seconds between teleport attempts to prevent abuse.")
                .defineInRange("teleportCooldownSeconds", 60, 0, 3600);

        ENABLE_THROW_DOLL = builder
                .comment("Allow players to throw doll items (hold right-click to charge) dealing damage on impact.")
                .define("enableThrowDoll", true);

        THROW_DOLL_DAMAGE = builder
                .comment("Damage dealt when a thrown doll hits an entity.")
                .defineInRange("throwDollDamage", 4.0, 0.0, 20.0);

        ENABLE_ATTRACT_UNDEAD = builder
                .comment("Placed dolls attract undead mobs (similar to turtle eggs), making them pathfind toward the doll.")
                .define("enableAttractUndead", true);

        ATTRACT_UNDEAD_RANGE = builder
                .comment("Range in blocks within which undead mobs are attracted to placed dolls.")
                .defineInRange("attractUndeadRange", 24.0, 0.0, 64.0);

        ENABLE_REPEL_PHANTOMS = builder
                .comment("Placed dolls repel phantoms within range, preventing them from targeting nearby players.")
                .define("enableRepelPhantoms", true);

        REPEL_PHANTOMS_RANGE = builder
                .comment("Range in blocks within which phantoms are repelled by placed dolls.")
                .defineInRange("repelPhantomsRange", 32.0, 0.0, 128.0);

        ENABLE_ENDER_MASK_PROTECTION = builder
                .comment("Treat a bound player doll worn in the head slot or optional compatible accessory slot as an enderman mask.")
                .define("enableEnderMaskProtection", true);

        SPEC = builder.build();
    }

    private SoulboundDollsConfig() {
    }
}
