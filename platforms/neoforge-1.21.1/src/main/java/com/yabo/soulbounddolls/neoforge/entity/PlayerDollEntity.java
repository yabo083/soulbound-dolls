package com.yabo.soulbounddolls.neoforge.entity;

import com.yabo.soulbounddolls.common.PlayerDollProfile;
import com.yabo.soulbounddolls.neoforge.SoulboundDollsConfig;
import com.yabo.soulbounddolls.neoforge.data.DollPlayerRegistrySavedData;
import com.yabo.soulbounddolls.neoforge.item.PlayerDollItem;
import com.yabo.soulbounddolls.neoforge.skin.DollSkinResolver;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class PlayerDollEntity extends Entity {
    private static final String PROFILE_UUID_TAG = "ProfileUuid";
    private static final String PROFILE_NAME_TAG = "ProfileName";
    private static final String SKIN_VALUE_TAG = "SkinValue";
    private static final String SKIN_SIGNATURE_TAG = "SkinSignature";
    private static final String SLIM_MODEL_TAG = "SlimModel";
    private static final String LAST_UPDATED_TAG = "LastUpdated";
    private static final String CREATOR_UUID_TAG = "CreatorUuid";
    private static final String CREATOR_NAME_TAG = "CreatorName";
    private static final String DOLL_POSE_TAG = "DollPose";

    private static final EntityDataAccessor<String> PROFILE_UUID = SynchedEntityData.defineId(PlayerDollEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> PROFILE_NAME = SynchedEntityData.defineId(PlayerDollEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> SKIN_VALUE = SynchedEntityData.defineId(PlayerDollEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> SKIN_SIGNATURE = SynchedEntityData.defineId(PlayerDollEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Boolean> SLIM_MODEL = SynchedEntityData.defineId(PlayerDollEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> SHAKE_TICKS = SynchedEntityData.defineId(PlayerDollEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> PAT_TICKS = SynchedEntityData.defineId(PlayerDollEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DOLL_POSE = SynchedEntityData.defineId(PlayerDollEntity.class, EntityDataSerializers.INT);

    private PlayerDollProfile profile;
    private UUID creatorUuid;
    private String creatorName = "";
    // One-shot guards so the server-side skin self-heal runs at most once per loaded entity,
    // never per tick. skinHealAttempted flips as soon as we either succeed or dispatch the request.
    private boolean skinHealAttempted;
    private boolean skinHealInFlight;

    public PlayerDollEntity(EntityType<? extends PlayerDollEntity> entityType, Level level) {
        super(entityType, level);
    }

    public void setProfile(PlayerDollProfile profile) {
        this.profile = profile;
        this.entityData.set(PROFILE_UUID, profile.uuid().toString());
        this.entityData.set(PROFILE_NAME, profile.name());
        this.entityData.set(SKIN_VALUE, profile.skinValue());
        this.entityData.set(SKIN_SIGNATURE, profile.skinSignature());
        this.entityData.set(SLIM_MODEL, profile.slimModel());
    }

    public PlayerDollProfile getProfile() {
        if (profile == null) {
            profile = PlayerDollProfile.of(
                    readSyncedUuid(),
                    entityData.get(PROFILE_NAME),
                    entityData.get(SKIN_VALUE),
                    entityData.get(SKIN_SIGNATURE),
                    entityData.get(SLIM_MODEL),
                    0L);
        }
        return profile;
    }

    public Optional<UUID> getCreatorUuid() {
        return Optional.ofNullable(creatorUuid);
    }

    public String getCreatorName() {
        return creatorName;
    }

    public int getShakeTicks() {
        return entityData.get(SHAKE_TICKS);
    }

    public int getPatTicks() {
        return entityData.get(PAT_TICKS);
    }

    public DollPose getDollPose() {
        return DollPose.byId(entityData.get(DOLL_POSE));
    }

    public void setCreator(Player player) {
        this.creatorUuid = player.getUUID();
        this.creatorName = player.getGameProfile().getName();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(PROFILE_UUID, new UUID(0L, 0L).toString());
        builder.define(PROFILE_NAME, PlayerDollProfile.UNKNOWN_PLAYER_NAME);
        builder.define(SKIN_VALUE, "");
        builder.define(SKIN_SIGNATURE, "");
        builder.define(SLIM_MODEL, false);
        builder.define(SHAKE_TICKS, 0);
        builder.define(PAT_TICKS, 0);
        builder.define(DOLL_POSE, DollPose.SITTING.id);
    }

    @Override
    public void tick() {
        super.tick();
        decrementSyncedTimer(SHAKE_TICKS);
        decrementSyncedTimer(PAT_TICKS);
        maybeHealMissingSkin();
    }

    /**
     * If a placed doll has no skin yet (e.g. it was auto-given on a LAN/offline login before the
     * owner's textures were fetched), pull the owner's skin from Mojang exactly once and hot-update
     * the doll. Guarded so this never runs per tick: it fires at most one async request per loaded
     * entity. Manual {@code /sbdoll give|refresh} still works independently.
     */
    private void maybeHealMissingSkin() {
        if (skinHealAttempted || skinHealInFlight || level().isClientSide) {
            return;
        }
        PlayerDollProfile current = getProfile();
        if (current.hasSkin() || !SoulboundDollsConfig.ENABLE_ONLINE_SKIN_REFRESH.get()) {
            skinHealAttempted = true;
            return;
        }
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }

        skinHealInFlight = true;
        MinecraftServer server = serverLevel.getServer();
        CompletableFuture
                .supplyAsync(() -> DollSkinResolver.refreshOnline(server, current, System.currentTimeMillis()))
                .whenComplete((refreshed, throwable) -> server.execute(() -> {
                    skinHealInFlight = false;
                    skinHealAttempted = true;
                    if (throwable != null || refreshed.isEmpty() || isRemoved()) {
                        return;
                    }
                    PlayerDollProfile healed = refreshed.get();
                    setProfile(healed);
                    DollPlayerRegistrySavedData.get(server).upsert(healed);
                }));
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public ItemStack getPickResult() {
        return PlayerDollItem.createBoundDoll(getProfile());
    }

    @Override
    public boolean skipAttackInteraction(Entity attacker) {
        // Left-click (attack) always shakes the doll; pickup is handled by sneak + right-click.
        playShakeFeedback();
        return true;
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            return tryPickup(player);
        }

        if (held.isEmpty()) {
            playPatFeedback();
            return InteractionResult.sidedSuccess(level().isClientSide);
        }

        cyclePose(player);
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        PlayerDollProfile savedProfile = getProfile();
        tag.putUUID(PROFILE_UUID_TAG, savedProfile.uuid());
        tag.putString(PROFILE_NAME_TAG, savedProfile.name());
        tag.putString(SKIN_VALUE_TAG, savedProfile.skinValue());
        tag.putString(SKIN_SIGNATURE_TAG, savedProfile.skinSignature());
        tag.putBoolean(SLIM_MODEL_TAG, savedProfile.slimModel());
        tag.putLong(LAST_UPDATED_TAG, savedProfile.lastUpdated());

        if (creatorUuid != null) {
            tag.putUUID(CREATOR_UUID_TAG, creatorUuid);
            tag.putString(CREATOR_NAME_TAG, creatorName);
        }
        tag.putString(DOLL_POSE_TAG, getDollPose().serializedName);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        UUID savedUuid = tag.hasUUID(PROFILE_UUID_TAG) ? tag.getUUID(PROFILE_UUID_TAG) : new UUID(0L, 0L);
        setProfile(PlayerDollProfile.of(
                savedUuid,
                tag.getString(PROFILE_NAME_TAG),
                tag.getString(SKIN_VALUE_TAG),
                tag.getString(SKIN_SIGNATURE_TAG),
                tag.getBoolean(SLIM_MODEL_TAG),
                tag.getLong(LAST_UPDATED_TAG)));

        if (tag.hasUUID(CREATOR_UUID_TAG)) {
            creatorUuid = tag.getUUID(CREATOR_UUID_TAG);
            creatorName = tag.getString(CREATOR_NAME_TAG);
        } else {
            creatorUuid = null;
            creatorName = "";
        }
        entityData.set(DOLL_POSE, DollPose.byName(tag.getString(DOLL_POSE_TAG)).id);
    }

    private void cyclePose(Player player) {
        if (level().isClientSide) {
            return;
        }

        DollPose nextPose = getDollPose().next();
        entityData.set(DOLL_POSE, nextPose.id);
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.displayClientMessage(Component.translatable("entity.soulbound_dolls.player_doll.pose." + nextPose.serializedName), true);
        }
    }

    private InteractionResult tryPickup(Player player) {
        // Operators / single-player-with-cheats (permission level 2) can always pick up any doll,
        // even ones bound to other players. Otherwise the creator-only rule applies unless the
        // server allows pickup by anyone.
        boolean operator = player.hasPermissions(2);
        if (!operator && !SoulboundDollsConfig.ALLOW_PICKUP_BY_ANYONE.get()
                && creatorUuid != null && !creatorUuid.equals(player.getUUID())) {
            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.displayClientMessage(Component.translatable("entity.soulbound_dolls.player_doll.pickup_denied"), true);
            }
            return InteractionResult.CONSUME;
        }

        if (!level().isClientSide) {
            ItemStack dollStack = PlayerDollItem.createBoundDoll(getProfile());
            if (!player.getInventory().add(dollStack)) {
                player.drop(dollStack, false);
            }
            discard();
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    private void playPatFeedback() {
        entityData.set(PAT_TICKS, 12);
        level().playSound(null, blockPosition(), SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.NEUTRAL, 0.55F, 1.55F);
        if (SoulboundDollsConfig.ALLOW_PAT_PARTICLES.get() && level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.HEART, getX(), getY() + 0.65D, getZ(), 4, 0.18D, 0.15D, 0.18D, 0.01D);
        }
    }

    private void playShakeFeedback() {
        entityData.set(SHAKE_TICKS, 18);
        level().playSound(null, blockPosition(), SoundEvents.WOOL_HIT, SoundSource.NEUTRAL, 0.6F, 1.2F);
    }

    private void decrementSyncedTimer(EntityDataAccessor<Integer> accessor) {
        int ticks = entityData.get(accessor);
        if (ticks > 0) {
            entityData.set(accessor, ticks - 1);
        }
    }

    private UUID readSyncedUuid() {
        try {
            return UUID.fromString(entityData.get(PROFILE_UUID));
        } catch (IllegalArgumentException exception) {
            return new UUID(0L, 0L);
        }
    }

    public enum DollPose {
        SITTING(0, "sitting"),
        STANDING(1, "standing"),
        CUTE_IDLE(2, "cute_idle");

        private static final DollPose[] VALUES = values();

        private final int id;
        private final String serializedName;

        DollPose(int id, String serializedName) {
            this.id = id;
            this.serializedName = serializedName;
        }

        private DollPose next() {
            return VALUES[(ordinal() + 1) % VALUES.length];
        }

        private static DollPose byId(int id) {
            for (DollPose pose : VALUES) {
                if (pose.id == id) {
                    return pose;
                }
            }
            return SITTING;
        }

        private static DollPose byName(String name) {
            for (DollPose pose : VALUES) {
                if (pose.serializedName.equals(name)) {
                    return pose;
                }
            }
            return SITTING;
        }
    }
}
