package com.yabo.soulbounddolls.neoforge;

import com.yabo.soulbounddolls.common.PlayerDollProfile;
import com.yabo.soulbounddolls.neoforge.data.DollPlayerRegistrySavedData;
import com.yabo.soulbounddolls.neoforge.entity.PlayerDollEntity;
import com.yabo.soulbounddolls.neoforge.item.PlayerDollItem;
import com.yabo.soulbounddolls.neoforge.skin.DollSkinResolver;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
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

    /**
     * Event-driven phantom repelling: when a phantom tries to target a player,
     * check if there's a doll nearby. If so, cancel the targeting.
     * This replaces per-tick checks for much better performance.
     */
    @SubscribeEvent
    public static void onPhantomChangeTarget(LivingChangeTargetEvent event) {
        if (!SoulboundDollsConfig.ENABLE_REPEL_PHANTOMS.get()) {
            return;
        }

        // Only handle phantoms targeting living entities
        if (!(event.getEntity() instanceof Phantom phantom)) {
            return;
        }

        LivingEntity newTarget = event.getNewAboutToBeSetTarget();
        if (newTarget == null) {
            return;
        }

        // Check if there's a doll within range of the target
        double range = SoulboundDollsConfig.REPEL_PHANTOMS_RANGE.get();
        AABB searchBox = newTarget.getBoundingBox().inflate(range);

        List<PlayerDollEntity> nearbyDolls = phantom.level()
                .getEntitiesOfClass(PlayerDollEntity.class, searchBox);

        if (!nearbyDolls.isEmpty()) {
            // There's a doll nearby - cancel the targeting
            event.setCanceled(true);
        }
    }

    /**
     * Event-driven undead attraction: when an undead mob spawns or is loaded,
     * add a goal to pathfind toward nearby dolls.
     * This replaces per-tick checks for much better performance.
     */
    @SubscribeEvent
    public static void onMobJoinLevel(EntityJoinLevelEvent event) {
        if (!SoulboundDollsConfig.ENABLE_ATTRACT_UNDEAD.get()) {
            return;
        }

        if (event.getLevel().isClientSide()) {
            return;
        }

        // Only handle undead mobs with pathfinding capability
        if (!(event.getEntity() instanceof net.minecraft.world.entity.PathfinderMob mob)) {
            return;
        }

        if (!mob.isInvertedHealAndHarm()) {
            return; // Not an undead mob
        }

        // Add a low-priority goal to move toward dolls
        // This is evaluated by vanilla AI system, not every tick by us
        double range = SoulboundDollsConfig.ATTRACT_UNDEAD_RANGE.get();
        mob.goalSelector.addGoal(5, new MoveTowardDollGoal(mob, 1.0, (int) range));
    }

    /**
     * AI Goal that makes undead mobs move toward nearby player dolls.
     * Uses vanilla AI system for efficient pathfinding.
     */
    private static class MoveTowardDollGoal extends net.minecraft.world.entity.ai.goal.Goal {
        private final net.minecraft.world.entity.PathfinderMob mob;
        private final double speedModifier;
        private final int searchRange;
        private PlayerDollEntity targetDoll;

        public MoveTowardDollGoal(net.minecraft.world.entity.PathfinderMob mob, double speedModifier, int searchRange) {
            this.mob = mob;
            this.speedModifier = speedModifier;
            this.searchRange = searchRange;
        }

        @Override
        public boolean canUse() {
            if (!SoulboundDollsConfig.ENABLE_ATTRACT_UNDEAD.get()) {
                return false;
            }

            AABB searchBox = mob.getBoundingBox().inflate(searchRange);

            List<PlayerDollEntity> nearbyDolls = mob.level()
                    .getEntitiesOfClass(PlayerDollEntity.class, searchBox);

            if (nearbyDolls.isEmpty()) {
                return false;
            }

            // Find the closest doll
            targetDoll = nearbyDolls.stream()
                    .min((a, b) -> Double.compare(
                            mob.distanceToSqr(a),
                            mob.distanceToSqr(b)))
                    .orElse(null);

            return targetDoll != null;
        }

        @Override
        public boolean canContinueToUse() {
            if (targetDoll == null || targetDoll.isRemoved()) {
                return false;
            }

            return mob.distanceToSqr(targetDoll) < searchRange * searchRange;
        }

        @Override
        public void tick() {
            if (targetDoll != null && !targetDoll.isRemoved()) {
                mob.getNavigation().moveTo(targetDoll, speedModifier);
            }
        }
    }
}
