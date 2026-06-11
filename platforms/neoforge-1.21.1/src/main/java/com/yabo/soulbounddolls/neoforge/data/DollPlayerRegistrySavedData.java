package com.yabo.soulbounddolls.neoforge.data;

import com.yabo.soulbounddolls.common.PlayerDollProfile;
import com.yabo.soulbounddolls.neoforge.skin.DollSkinResolver;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;

public final class DollPlayerRegistrySavedData extends SavedData {
    public static final String DATA_NAME = "soulbound_dolls_players";
    private static final int VERSION = 1;
    private static final long REFRESH_RETRY_DELAY_MILLIS = 60L * 60L * 1000L;

    private final Map<UUID, StoredPlayer> playersByUuid = new HashMap<>();
    private final Map<String, UUID> uuidsByLowercaseName = new HashMap<>();

    public static DollPlayerRegistrySavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(DollPlayerRegistrySavedData::new, DollPlayerRegistrySavedData::load),
                DATA_NAME);
    }

    public static DollPlayerRegistrySavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        DollPlayerRegistrySavedData data = new DollPlayerRegistrySavedData();
        ListTag players = tag.getList("Players", Tag.TAG_COMPOUND);
        for (Tag entry : players) {
            if (entry instanceof CompoundTag playerTag) {
                StoredPlayer player = StoredPlayer.load(playerTag);
                data.playersByUuid.put(player.profile().uuid(), player);
                data.indexName(player.profile());
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt("Version", VERSION);
        ListTag players = new ListTag();
        for (StoredPlayer player : playersByUuid.values()) {
            players.add(player.save());
        }
        tag.put("Players", players);
        return tag;
    }

    public Optional<PlayerDollProfile> find(UUID uuid) {
        StoredPlayer player = playersByUuid.get(uuid);
        return player == null ? Optional.empty() : Optional.of(player.profile());
    }

    public Optional<PlayerDollProfile> findByName(String name) {
        if (name == null) {
            return Optional.empty();
        }
        UUID uuid = uuidsByLowercaseName.get(name.toLowerCase(Locale.ROOT));
        return uuid == null ? Optional.empty() : find(uuid);
    }

    public Collection<PlayerDollProfile> allProfiles() {
        List<PlayerDollProfile> profiles = new ArrayList<>();
        for (StoredPlayer player : playersByUuid.values()) {
            profiles.add(player.profile());
        }
        return Collections.unmodifiableList(profiles);
    }

    public void upsert(PlayerDollProfile profile) {
        StoredPlayer existing = playersByUuid.get(profile.uuid());
        if (existing != null) {
            removeNameIndex(existing.profile());
        }
        playersByUuid.put(profile.uuid(), new StoredPlayer(
                profile,
                existing == null ? 0L : existing.lastRefreshAttempt(),
                existing == null ? "" : existing.lastRefreshFailure()));
        indexName(profile);
        setDirty();
    }

    public PlayerDollProfile upsertFromLogin(ServerPlayer player, long nowMillis) {
        PlayerDollProfile profile = DollSkinResolver.fromGameProfile(player.getGameProfile(), nowMillis);
        upsert(profile);
        return profile;
    }

    public void recordRefreshFailure(UUID uuid, String failure, long nowMillis) {
        StoredPlayer existing = playersByUuid.get(uuid);
        if (existing == null) {
            return;
        }
        playersByUuid.put(uuid, new StoredPlayer(existing.profile(), nowMillis, failure == null ? "" : failure));
        setDirty();
    }

    public void recordRefreshSuccess(UUID uuid, long nowMillis) {
        StoredPlayer existing = playersByUuid.get(uuid);
        if (existing == null) {
            return;
        }
        playersByUuid.put(uuid, new StoredPlayer(existing.profile(), nowMillis, ""));
        setDirty();
    }

    public boolean shouldRetryRefresh(UUID uuid, long nowMillis) {
        StoredPlayer existing = playersByUuid.get(uuid);
        if (existing == null) {
            return true;
        }
        return existing.lastRefreshAttempt() <= 0L
                || nowMillis - existing.lastRefreshAttempt() >= REFRESH_RETRY_DELAY_MILLIS;
    }

    public Optional<String> lastRefreshFailure(UUID uuid) {
        StoredPlayer existing = playersByUuid.get(uuid);
        if (existing == null || existing.lastRefreshFailure().isBlank()) {
            return Optional.empty();
        }
        return Optional.of(existing.lastRefreshFailure());
    }

    private void indexName(PlayerDollProfile profile) {
        uuidsByLowercaseName.put(profile.name().toLowerCase(Locale.ROOT), profile.uuid());
    }

    private void removeNameIndex(PlayerDollProfile profile) {
        uuidsByLowercaseName.remove(profile.name().toLowerCase(Locale.ROOT));
    }

    private record StoredPlayer(PlayerDollProfile profile, long lastRefreshAttempt, String lastRefreshFailure) {
        private CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putUUID("Uuid", profile.uuid());
            tag.putString("Name", profile.name());
            tag.putString("SkinValue", profile.skinValue());
            tag.putString("SkinSignature", profile.skinSignature());
            tag.putBoolean("SlimModel", profile.slimModel());
            tag.putLong("LastUpdated", profile.lastUpdated());
            tag.putLong("LastRefreshAttempt", lastRefreshAttempt);
            tag.putString("LastRefreshFailure", lastRefreshFailure);
            return tag;
        }

        private static StoredPlayer load(CompoundTag tag) {
            PlayerDollProfile profile = PlayerDollProfile.of(
                    tag.getUUID("Uuid"),
                    tag.getString("Name"),
                    tag.getString("SkinValue"),
                    tag.getString("SkinSignature"),
                    tag.getBoolean("SlimModel"),
                    tag.getLong("LastUpdated"));
            return new StoredPlayer(profile, tag.getLong("LastRefreshAttempt"), tag.getString("LastRefreshFailure"));
        }
    }
}
