package com.yabo.soulbounddolls.neoforge.entity;

import com.yabo.soulbounddolls.common.PlayerDollProfile;
import com.yabo.soulbounddolls.neoforge.SoulboundDollsComponents;
import com.yabo.soulbounddolls.neoforge.SoulboundDollsConfig;
import com.yabo.soulbounddolls.neoforge.item.PlayerDollItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.UUID;

/**
 * A throwable projectile created when a player charges and throws a doll item.
 * Deals configured damage on hit and drops the doll item afterward.
 */
public class DollProjectileEntity extends ThrowableItemProjectile {
    private static final String PROFILE_UUID_TAG = "ProfileUuid";
    private static final String PROFILE_NAME_TAG = "ProfileName";
    private static final String SKIN_VALUE_TAG = "SkinValue";
    private static final String SKIN_SIGNATURE_TAG = "SkinSignature";
    private static final String SLIM_MODEL_TAG = "SlimModel";
    private static final String LAST_UPDATED_TAG = "LastUpdated";

    private static final EntityDataAccessor<String> PROFILE_UUID = SynchedEntityData.defineId(DollProjectileEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> PROFILE_NAME = SynchedEntityData.defineId(DollProjectileEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> SKIN_VALUE = SynchedEntityData.defineId(DollProjectileEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> SKIN_SIGNATURE = SynchedEntityData.defineId(DollProjectileEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Boolean> SLIM_MODEL = SynchedEntityData.defineId(DollProjectileEntity.class, EntityDataSerializers.BOOLEAN);

    private PlayerDollProfile profile;

    public DollProjectileEntity(EntityType<? extends DollProjectileEntity> entityType, Level level) {
        super(entityType, level);
    }

    public DollProjectileEntity(EntityType<? extends DollProjectileEntity> entityType, LivingEntity shooter, Level level, PlayerDollProfile profile) {
        super(entityType, shooter, level);
        this.profile = profile;
        if (profile != null) {
            setProfileData(profile);
        }
    }

    @Override
    protected Item getDefaultItem() {
        return null; // We handle item creation ourselves
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(PROFILE_UUID, new UUID(0L, 0L).toString());
        builder.define(PROFILE_NAME, PlayerDollProfile.UNKNOWN_PLAYER_NAME);
        builder.define(SKIN_VALUE, "");
        builder.define(SKIN_SIGNATURE, "");
        builder.define(SLIM_MODEL, false);
    }

    private void setProfileData(PlayerDollProfile profile) {
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

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        Entity target = result.getEntity();
        Entity owner = getOwner();

        // Deal damage to the hit entity
        double damage = SoulboundDollsConfig.THROW_DOLL_DAMAGE.get();
        if (owner instanceof LivingEntity livingOwner) {
            target.hurt(damageSources().thrown(this, livingOwner), (float) damage);
        } else {
            target.hurt(damageSources().thrown(this, this), (float) damage);
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);

        // Drop the doll item at impact location
        if (!level().isClientSide) {
            ItemStack dollStack = PlayerDollItem.createBoundDoll(getProfile());
            spawnAtLocation(dollStack);
            discard();
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        PlayerDollProfile savedProfile = getProfile();
        tag.putUUID(PROFILE_UUID_TAG, savedProfile.uuid());
        tag.putString(PROFILE_NAME_TAG, savedProfile.name());
        tag.putString(SKIN_VALUE_TAG, savedProfile.skinValue());
        tag.putString(SKIN_SIGNATURE_TAG, savedProfile.skinSignature());
        tag.putBoolean(SLIM_MODEL_TAG, savedProfile.slimModel());
        tag.putLong(LAST_UPDATED_TAG, savedProfile.lastUpdated());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        UUID savedUuid = tag.hasUUID(PROFILE_UUID_TAG) ? tag.getUUID(PROFILE_UUID_TAG) : new UUID(0L, 0L);
        this.profile = PlayerDollProfile.of(
                savedUuid,
                tag.getString(PROFILE_NAME_TAG),
                tag.getString(SKIN_VALUE_TAG),
                tag.getString(SKIN_SIGNATURE_TAG),
                tag.getBoolean(SLIM_MODEL_TAG),
                tag.getLong(LAST_UPDATED_TAG));
        setProfileData(profile);
    }

    private UUID readSyncedUuid() {
        try {
            return UUID.fromString(entityData.get(PROFILE_UUID));
        } catch (IllegalArgumentException exception) {
            return new UUID(0L, 0L);
        }
    }
}
