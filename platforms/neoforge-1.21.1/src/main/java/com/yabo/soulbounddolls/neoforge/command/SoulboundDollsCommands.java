package com.yabo.soulbounddolls.neoforge.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.yabo.soulbounddolls.common.PlayerDollProfile;
import com.yabo.soulbounddolls.neoforge.data.DollPlayerRegistrySavedData;
import com.yabo.soulbounddolls.neoforge.item.PlayerDollItem;
import com.yabo.soulbounddolls.neoforge.skin.DollSkinResolver;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public final class SoulboundDollsCommands {
    private SoulboundDollsCommands() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(literal("sbdoll")
                .requires(source -> source.hasPermission(2))
                .then(literal("list")
                        .executes(SoulboundDollsCommands::listProfiles))
                .then(literal("give")
                        .then(argument("target", EntityArgument.player())
                                .then(argument("prototype", StringArgumentType.string())
                                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(profileSuggestions(context), builder))
                                        .executes(SoulboundDollsCommands::giveDoll))))
                .then(literal("refresh")
                        .then(argument("prototype", StringArgumentType.string())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(profileSuggestions(context), builder))
                                .executes(SoulboundDollsCommands::refreshProfile))));
    }

    private static int listProfiles(CommandContext<CommandSourceStack> context) {
        DollPlayerRegistrySavedData registry = DollPlayerRegistrySavedData.get(context.getSource().getServer());
        int profileCount = registry.allProfiles().size();
        if (profileCount == 0) {
            context.getSource().sendSuccess(() -> Component.translatable("commands.soulbound_dolls.list.empty"), false);
            return 0;
        }

        String profiles = registry.allProfiles().stream()
                .map(profile -> profile.name() + " (" + profile.uuid() + ")")
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .reduce((left, right) -> left + ", " + right)
                .orElse("");
        context.getSource().sendSuccess(() -> Component.translatable("commands.soulbound_dolls.list.success", profileCount, profiles), false);
        return profileCount;
    }

    private static int giveDoll(CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(context, "target");
        String prototype = StringArgumentType.getString(context, "prototype");
        Optional<PlayerDollProfile> profile = resolveProfile(context.getSource().getServer(), prototype);
        if (profile.isEmpty()) {
            context.getSource().sendFailure(Component.translatable("commands.soulbound_dolls.profile_not_found", prototype));
            return 0;
        }

        ItemStack stack = PlayerDollItem.createBoundDoll(profile.get());
        if (!target.getInventory().add(stack)) {
            target.drop(stack, false);
        }
        context.getSource().sendSuccess(() -> Component.translatable("commands.soulbound_dolls.give.success", profile.get().name(), target.getGameProfile().getName()), true);
        return 1;
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
        try {
            return registry.find(UUID.fromString(prototype));
        } catch (IllegalArgumentException exception) {
            return registry.findByName(prototype);
        }
    }

    private static Iterable<String> profileSuggestions(CommandContext<CommandSourceStack> context) {
        return DollPlayerRegistrySavedData.get(context.getSource().getServer()).allProfiles().stream()
                .map(PlayerDollProfile::name)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }
}
