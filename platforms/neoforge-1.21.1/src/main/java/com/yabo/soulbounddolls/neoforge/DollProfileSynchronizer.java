package com.yabo.soulbounddolls.neoforge;

import com.yabo.soulbounddolls.common.PlayerDollProfile;
import java.util.Optional;

public final class DollProfileSynchronizer {
    private DollProfileSynchronizer() {
    }

    public static Optional<PlayerDollProfile> updatedFromRegistry(PlayerDollProfile current, Optional<PlayerDollProfile> registryProfile) {
        if (current == null || registryProfile.isEmpty()) {
            return Optional.empty();
        }
        PlayerDollProfile registry = registryProfile.get();
        if (!current.uuid().equals(registry.uuid()) || !registry.hasSkin()) {
            return Optional.empty();
        }
        if (registry.lastUpdated() < current.lastUpdated()) {
            return Optional.empty();
        }
        if (current.skinValue().equals(registry.skinValue())
                && current.skinSignature().equals(registry.skinSignature())
                && current.slimModel() == registry.slimModel()) {
            return Optional.empty();
        }
        return Optional.of(registry);
    }
}
