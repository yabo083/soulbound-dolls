package com.yabo.soulbounddolls.neoforge;

import com.yabo.soulbounddolls.common.PlayerDollProfile;
import com.yabo.soulbounddolls.neoforge.compat.curios.CuriosDollLookup;
import com.yabo.soulbounddolls.neoforge.data.DollPlayerRegistrySavedData;
import com.yabo.soulbounddolls.neoforge.entity.EnderMaskHelper;
import com.yabo.soulbounddolls.neoforge.entity.PlayerDollEntity;
import com.yabo.soulbounddolls.neoforge.entity.ZombieDollCarryHelper;
import com.yabo.soulbounddolls.neoforge.entity.ZombieMoveToDollGoal;
import com.yabo.soulbounddolls.neoforge.item.PlayerDollItem;
import com.yabo.soulbounddolls.neoforge.skin.DollSkinResolver;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.EnderManAngerEvent;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
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

    @SubscribeEvent
    public static void onEnderManAnger(EnderManAngerEvent event) {
        if (!SoulboundDollsConfig.ENABLE_ENDER_MASK_PROTECTION.get()) {
            return;
        }

        if (EnderMaskHelper.isProtected(event.getPlayer(), () -> CuriosDollLookup.hasEquippedDoll(event.getPlayer()))) {
            event.setCanceled(true);
        }
    }

    /**
     * Event-driven zombie attraction: when a zombie spawns or is loaded,
     * add a goal to pathfind toward nearby dolls and doll item drops.
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

        if (event.getEntity() instanceof Zombie zombie) {
            double range = SoulboundDollsConfig.ATTRACT_UNDEAD_RANGE.get();
            if (!hasZombieMoveToDollGoal(zombie)) {
                zombie.goalSelector.addGoal(5, new ZombieMoveToDollGoal(zombie, 1.0D, range));
            }
            ZombieDollCarryHelper.ensureHeadDollForSunProtection(zombie);
        }
    }

    private static boolean hasZombieMoveToDollGoal(Zombie zombie) {
        return zombie.goalSelector.getAvailableGoals().stream()
                .anyMatch(goal -> goal.getGoal() instanceof ZombieMoveToDollGoal);
    }

    @SubscribeEvent
    public static void onLivingEquipmentChange(LivingEquipmentChangeEvent event) {
        if (!SoulboundDollsConfig.ENABLE_ATTRACT_UNDEAD.get()) {
            return;
        }

        if (event.getEntity().level().isClientSide()) {
            return;
        }

        if (event.getEntity() instanceof Zombie zombie && event.getSlot() == EquipmentSlot.HEAD) {
            ZombieDollCarryHelper.ensureHeadDollForSunProtection(zombie);
        }
    }

    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        if (!SoulboundDollsConfig.ENABLE_ATTRACT_UNDEAD.get()) {
            return;
        }

        if (!(event.getEntity() instanceof Zombie zombie)) {
            return;
        }

        for (ItemStack hiddenDoll : ZombieDollCarryHelper.takeHiddenStoredDolls(zombie)) {
            event.getDrops().add(new ItemEntity(zombie.level(), zombie.getX(), zombie.getY(), zombie.getZ(), hiddenDoll));
        }
    }

}
