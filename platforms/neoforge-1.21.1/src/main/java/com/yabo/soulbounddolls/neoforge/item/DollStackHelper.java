package com.yabo.soulbounddolls.neoforge.item;

import com.yabo.soulbounddolls.neoforge.SoulboundDollsComponents;
import net.minecraft.world.item.ItemStack;

public final class DollStackHelper {
    private DollStackHelper() {
    }

    public static boolean isBoundPlayerDoll(ItemStack stack) {
        return !stack.isEmpty() && stack.get(SoulboundDollsComponents.PLAYER_DOLL_PROFILE.get()) != null;
    }
}
