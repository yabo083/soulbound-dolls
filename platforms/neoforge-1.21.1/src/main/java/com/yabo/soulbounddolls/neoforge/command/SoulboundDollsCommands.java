package com.yabo.soulbounddolls.neoforge.command;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.yabo.soulbounddolls.common.PlayerDollProfile;
import com.yabo.soulbounddolls.neoforge.data.DollCommandPermissions;
import com.yabo.soulbounddolls.neoforge.data.DollPlayerRegistrySavedData;
import com.yabo.soulbounddolls.neoforge.item.PlayerDollItem;
import com.yabo.soulbounddolls.neoforge.skin.DollSkinResolver;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public final class SoulboundDollsCommands {
    /** Operator permission level (4 = single-player cheats / server owner & ops). */
    private static final int OP_PERMISSION_LEVEL = 2;

    private SoulboundDollsCommands() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(literal("sbdoll")
                // Usable by operators (single-player with cheats, or server ops) OR by players an
                // operator has explicitly granted via "/sbdoll permission grant".
                .requires(SoulboundDollsCommands::canUseCommands)
                .then(literal("list")
                        .executes(SoulboundDollsCommands::listProfiles))
                .then(literal("give")
                        .then(argument("target", EntityArgument.player())
                                .then(argument("prototypes", StringArgumentType.greedyString())
                                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(profileSuggestions(context), builder))
                                        .executes(SoulboundDollsCommands::giveDoll))))
                .then(literal("giveall")
                        .then(argument("target", EntityArgument.player())
                                .executes(SoulboundDollsCommands::giveAllDolls)))
                .then(literal("refresh")
                        .then(argument("prototype", StringArgumentType.string())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(profileSuggestions(context), builder))
                                .executes(SoulboundDollsCommands::refreshProfile)))
                // Managing who may use the commands is restricted to operators only, so a granted
                // player cannot escalate by granting others.
                .then(literal("permission")
                        .requires(source -> source.hasPermission(OP_PERMISSION_LEVEL))
                        .then(literal("grant")
                                .then(argument("target", EntityArgument.player())
                                        .executes(SoulboundDollsCommands::grantPermission)))
                        .then(literal("revoke")
                                .then(argument("target", EntityArgument.player())
                                        .executes(SoulboundDollsCommands::revokePermission)))
                        .then(literal("list")
                                .executes(SoulboundDollsCommands::listPermissions))));
    }

    private static boolean canUseCommands(CommandSourceStack source) {
        if (source.hasPermission(OP_PERMISSION_LEVEL)) {
            return true;
        }
        Entity entity = source.getEntity();
        if (entity instanceof ServerPlayer player) {
            return DollCommandPermissions.get(source.getServer()).isGranted(player.getUUID());
        }
        return false;
    }

    private static int grantPermission(CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(context, "target");
        boolean added = DollCommandPermissions.get(context.getSource().getServer()).grant(target.getUUID());
        String name = target.getGameProfile().getName();
        // Resend the command tree so the newly-granted player can immediately see and use /sbdoll.
        target.server.getCommands().sendCommands(target);
        if (added) {
            context.getSource().sendSuccess(() -> Component.translatable("commands.soulbound_dolls.permission.grant.success", name), true);
        } else {
            context.getSource().sendSuccess(() -> Component.translatable("commands.soulbound_dolls.permission.grant.already", name), false);
        }
        return 1;
    }

    private static int revokePermission(CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(context, "target");
        boolean removed = DollCommandPermissions.get(context.getSource().getServer()).revoke(target.getUUID());
        String name = target.getGameProfile().getName();
        // Resend the command tree so the revoked player immediately loses /sbdoll visibility.
        target.server.getCommands().sendCommands(target);
        if (removed) {
            context.getSource().sendSuccess(() -> Component.translatable("commands.soulbound_dolls.permission.revoke.success", name), true);
        } else {
            context.getSource().sendSuccess(() -> Component.translatable("commands.soulbound_dolls.permission.revoke.absent", name), false);
        }
        return 1;
    }

    private static int listPermissions(CommandContext<CommandSourceStack> context) {
        MinecraftServer server = context.getSource().getServer();
        java.util.Set<UUID> granted = DollCommandPermissions.get(server).granted();
        if (granted.isEmpty()) {
            context.getSource().sendSuccess(() -> Component.translatable("commands.soulbound_dolls.permission.list.empty"), false);
            return 0;
        }
        String names = granted.stream()
                .map(uuid -> server.getProfileCache() == null ? null : server.getProfileCache().get(uuid).map(GameProfile::getName).orElse(uuid.toString()))
                .filter(java.util.Objects::nonNull)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .reduce((left, right) -> left + ", " + right)
                .orElse("");
        context.getSource().sendSuccess(() -> Component.translatable("commands.soulbound_dolls.permission.list.success", granted.size(), names), false);
        return granted.size();
    }

    private static int listProfiles(CommandContext<CommandSourceStack> context) {
        MinecraftServer server = context.getSource().getServer();
        DollPlayerRegistrySavedData registry = DollPlayerRegistrySavedData.get(server);

        // Merge registry profiles (which know whether a skin is captured) with every player in the
        // native profile cache (everyone who has joined). Registry entries win on name collision.
        java.util.Map<String, Boolean> nameToHasSkin = new java.util.TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (String cachedName : profileCacheNames(server)) {
            nameToHasSkin.putIfAbsent(cachedName, Boolean.FALSE);
        }
        for (PlayerDollProfile profile : registry.allProfiles()) {
            nameToHasSkin.put(profile.name(), profile.hasSkin());
        }

        if (nameToHasSkin.isEmpty()) {
            context.getSource().sendSuccess(() -> Component.translatable("commands.soulbound_dolls.list.empty"), false);
            return 0;
        }

        int count = nameToHasSkin.size();
        context.getSource().sendSuccess(
                () -> Component.translatable("commands.soulbound_dolls.list.header", count).withStyle(ChatFormatting.GOLD),
                false);
        nameToHasSkin.forEach((name, hasSkin) -> {
            ChatFormatting color = hasSkin ? ChatFormatting.GREEN : ChatFormatting.GRAY;
            String marker = hasSkin ? "✔" : "✘";
            context.getSource().sendSuccess(
                    () -> Component.literal(" " + marker + " ").withStyle(color)
                            .append(Component.literal(name).withStyle(ChatFormatting.WHITE)),
                    false);
        });
        return count;
    }

    private static int giveDoll(CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(context, "target");
        MinecraftServer server = context.getSource().getServer();
        String[] prototypes = StringArgumentType.getString(context, "prototypes").trim().split("\\s+");

        int given = 0;
        for (String prototype : prototypes) {
            if (prototype.isBlank()) {
                continue;
            }
            Optional<PlayerDollProfile> profile = resolveProfile(server, prototype);
            if (profile.isEmpty()) {
                context.getSource().sendFailure(Component.translatable("commands.soulbound_dolls.profile_not_found", prototype));
                continue;
            }
            giveOneDoll(target, profile.get());
            given++;
        }

        int total = given;
        if (total > 0) {
            context.getSource().sendSuccess(
                    () -> Component.translatable("commands.soulbound_dolls.give.batch_success", total, target.getGameProfile().getName()),
                    true);
        }
        return total;
    }

    private static int giveAllDolls(CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(context, "target");
        MinecraftServer server = context.getSource().getServer();

        // Union of every name we know about: registry + native profile cache.
        java.util.Set<String> names = new java.util.TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        names.addAll(profileCacheNames(server));
        DollPlayerRegistrySavedData.get(server).allProfiles().forEach(profile -> names.add(profile.name()));

        int given = 0;
        for (String name : names) {
            Optional<PlayerDollProfile> profile = resolveProfile(server, name);
            if (profile.isPresent()) {
                giveOneDoll(target, profile.get());
                given++;
            }
        }

        int total = given;
        context.getSource().sendSuccess(
                () -> Component.translatable("commands.soulbound_dolls.give.batch_success", total, target.getGameProfile().getName()),
                true);
        return total;
    }

    private static void giveOneDoll(ServerPlayer target, PlayerDollProfile profile) {
        ItemStack stack = PlayerDollItem.createBoundDoll(profile);
        if (!target.getInventory().add(stack)) {
            target.drop(stack, false);
        }
    }

    private static int refreshProfile(CommandContext<CommandSourceStack> context) {
        String prototype = StringArgumentType.getString(context, "prototype");
        MinecraftServer server = context.getSource().getServer();
        Optional<PlayerDollProfile> profile = resolveProfile(server, prototype);
        if (profile.isEmpty()) {
            context.getSource().sendFailure(Component.translatable("commands.soulbound_dolls.profile_not_found", prototype));
            return 0;
        }

        context.getSource().sendSuccess(() -> Component.translatable("commands.soulbound_dolls.refresh.started", profile.get().name()), false);
        CompletableFuture
                .supplyAsync(() -> DollSkinResolver.refreshOnline(server, profile.get(), System.currentTimeMillis()))
                .whenComplete((refreshedProfile, throwable) -> server.execute(() -> applyRefreshResult(context.getSource(), profile.get(), refreshedProfile, throwable)));
        return 1;
    }

    private static void applyRefreshResult(CommandSourceStack source, PlayerDollProfile originalProfile, Optional<PlayerDollProfile> refreshedProfile, Throwable throwable) {
        DollPlayerRegistrySavedData registry = DollPlayerRegistrySavedData.get(source.getServer());
        long completedAt = System.currentTimeMillis();
        if (throwable != null) {
            registry.recordRefreshFailure(originalProfile.uuid(), throwable.getMessage(), completedAt);
            source.sendFailure(Component.translatable("commands.soulbound_dolls.refresh.failed", originalProfile.name()));
            return;
        }

        if (refreshedProfile.isPresent()) {
            registry.upsert(refreshedProfile.get());
            registry.recordRefreshSuccess(originalProfile.uuid(), completedAt);
            source.sendSuccess(() -> Component.translatable("commands.soulbound_dolls.refresh.success", refreshedProfile.get().name()), true);
        } else {
            registry.recordRefreshFailure(originalProfile.uuid(), "Profile refresh returned no textures", completedAt);
            source.sendFailure(Component.translatable("commands.soulbound_dolls.refresh.failed", originalProfile.name()));
        }
    }

    private static Optional<PlayerDollProfile> resolveProfile(MinecraftServer server, String prototype) {
        DollPlayerRegistrySavedData registry = DollPlayerRegistrySavedData.get(server);

        // 1. Already-known profile by UUID or name (no network, no allocation beyond lookup).
        Optional<PlayerDollProfile> known = lookupKnownProfile(registry, prototype);
        if (known.isPresent()) {
            return known;
        }

        // 2. Fall back to the server's native profile cache (usercache.json), which holds the
        //    name<->UUID of every player that has logged into this server. If the cached game
        //    profile already carries skin textures we build and persist a doll profile from it;
        //    otherwise we fetch textures from the session service once and cache the result.
        Optional<GameProfile> cachedProfile = lookupProfileCache(server, prototype);
        if (cachedProfile.isEmpty()) {
            return Optional.empty();
        }

        long nowMillis = System.currentTimeMillis();
        GameProfile gameProfile = cachedProfile.get();
        PlayerDollProfile profile;
        if (DollSkinResolver.texturesProperty(gameProfile).isPresent()) {
            profile = DollSkinResolver.fromGameProfile(gameProfile, nowMillis);
        } else {
            PlayerDollProfile placeholder = PlayerDollProfile.fromPlayer(
                    gameProfile.getId(), gameProfile.getName(), Optional.empty(), Optional.empty(), false, nowMillis);
            profile = DollSkinResolver.refreshOnline(server, placeholder, nowMillis).orElse(placeholder);
        }
        registry.upsert(profile);
        return Optional.of(profile);
    }

    private static Optional<PlayerDollProfile> lookupKnownProfile(DollPlayerRegistrySavedData registry, String prototype) {
        try {
            Optional<PlayerDollProfile> byUuid = registry.find(UUID.fromString(prototype));
            if (byUuid.isPresent()) {
                return byUuid;
            }
        } catch (IllegalArgumentException ignored) {
            // Not a UUID; fall through to name lookup.
        }
        return registry.findByName(prototype);
    }

    private static Optional<GameProfile> lookupProfileCache(MinecraftServer server, String prototype) {
        try {
            return server.getProfileCache().get(UUID.fromString(prototype));
        } catch (IllegalArgumentException ignored) {
            return server.getProfileCache() == null ? Optional.empty() : server.getProfileCache().get(prototype);
        }
    }

    private static Iterable<String> profileSuggestions(CommandContext<CommandSourceStack> context) {
        return profileCacheNames(context.getSource().getServer()).stream()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    /**
     * Names of every player known as login history. The doll registry records every login via
     * {@code upsertFromLogin}, so it is the enumerable, mod-owned source of "players who have joined
     * this server". (The native profile cache is not publicly enumerable; specific names typed by an
     * operator are still resolved against it on demand in {@link #resolveProfile}.)
     */
    private static java.util.Set<String> profileCacheNames(MinecraftServer server) {
        java.util.Set<String> names = new java.util.TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        DollPlayerRegistrySavedData.get(server).allProfiles().forEach(profile -> names.add(profile.name()));
        return names;
    }
}
