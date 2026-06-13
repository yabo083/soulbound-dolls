package com.yabo.soulbounddolls.neoforge.data;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * World-persisted set of player UUIDs that an operator has granted access to the {@code /sbdoll}
 * command tree. Operators (permission level 2) always have access regardless of this set; this lets
 * a server owner delegate command use to specific players without granting full operator status.
 */
public final class DollCommandPermissions extends SavedData {
    public static final String DATA_NAME = "soulbound_dolls_command_perms";

    private final Set<UUID> grantedPlayers = new HashSet<>();

    public static DollCommandPermissions get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(DollCommandPermissions::new, DollCommandPermissions::load),
                DATA_NAME);
    }

    public static DollCommandPermissions load(CompoundTag tag, HolderLookup.Provider registries) {
        DollCommandPermissions data = new DollCommandPermissions();
        ListTag granted = tag.getList("Granted", Tag.TAG_INT_ARRAY);
        for (Tag entry : granted) {
            try {
                data.grantedPlayers.add(net.minecraft.nbt.NbtUtils.loadUUID(entry));
            } catch (IllegalArgumentException ignored) {
                // Skip malformed UUID entries rather than failing the whole load.
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag granted = new ListTag();
        for (UUID uuid : grantedPlayers) {
            granted.add(net.minecraft.nbt.NbtUtils.createUUID(uuid));
        }
        tag.put("Granted", granted);
        return tag;
    }

    public boolean isGranted(UUID uuid) {
        return uuid != null && grantedPlayers.contains(uuid);
    }

    /** Returns true if the player was newly granted (false if already granted). */
    public boolean grant(UUID uuid) {
        boolean added = grantedPlayers.add(uuid);
        if (added) {
            setDirty();
        }
        return added;
    }

    /** Returns true if the player was actually revoked (false if not present). */
    public boolean revoke(UUID uuid) {
        boolean removed = grantedPlayers.remove(uuid);
        if (removed) {
            setDirty();
        }
        return removed;
    }

    public Set<UUID> granted() {
        return Collections.unmodifiableSet(grantedPlayers);
    }
}
