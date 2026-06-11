package com.yabo.soulbounddolls.neoforge;

import com.yabo.soulbounddolls.common.PlayerDollProfile;
import com.yabo.soulbounddolls.neoforge.data.DollPlayerRegistrySavedData;
import com.yabo.soulbounddolls.neoforge.item.PlayerDollItem;
import com.yabo.soulbounddolls.neoforge.skin.DollSkinResolver;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

public final class SoulboundDollsRuntimeEvents {
    private SoulboundDollsRuntimeEvents() {
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        MinecraftServer server = player.server;
        long nowMillis = System.currentTimeMillis();
        DollPlayerRegistrySavedData registry = DollPlayerRegistrySavedData.get(server);
        PlayerDollProfile profile = registry.upsertFromLogin(player, nowMillis);

        if (SoulboundDollsConfig.AUTO_GIVE_OWN_DOLL.get() && !hasBoundDoll(player, profile)) {
            ItemStack ownDoll = PlayerDollItem.createBoundDoll(profile);
            if (!player.getInventory().add(ownDoll)) {
                player.drop(ownDoll, false);
            }
        }

        if (!SoulboundDollsConfig.ENABLE_ONLINE_SKIN_REFRESH.get() || !registry.shouldRetryRefresh(profile.uuid(), nowMillis)) {
            return;
        }

        CompletableFuture
                .supplyAsync(() -> DollSkinResolver.refreshOnline(server, profile, System.currentTimeMillis()))
                .whenComplete((refreshedProfile, throwable) -> server.execute(() -> {
                    DollPlayerRegistrySavedData latestRegistry = DollPlayerRegistrySavedData.get(server);
                    long completedAt = System.currentTimeMillis();
                    if (throwable != null) {
                        latestRegistry.recordRefreshFailure(profile.uuid(), throwable.getMessage(), completedAt);
                        return;
                    }

                    Optional<PlayerDollProfile> resolved = refreshedProfile;
                    if (resolved.isPresent()) {
                        latestRegistry.upsert(resolved.get());
                        latestRegistry.recordRefreshSuccess(profile.uuid(), completedAt);
                    } else {
                        latestRegistry.recordRefreshFailure(profile.uuid(), "Profile refresh returned no textures", completedAt);
                    }
                }));
    }

    private static boolean hasBoundDoll(ServerPlayer player, PlayerDollProfile profile) {
        for (ItemStack stack : player.getInventory().items) {
            PlayerDollProfile stackProfile = stack.get(SoulboundDollsComponents.PLAYER_DOLL_PROFILE.get());
            if (stackProfile != null && stackProfile.uuid().equals(profile.uuid())) {
                return true;
            }
        }
        return false;
    }
}
