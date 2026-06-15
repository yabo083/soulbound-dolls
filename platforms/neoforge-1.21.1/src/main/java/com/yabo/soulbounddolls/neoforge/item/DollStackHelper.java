package com.yabo.soulbounddolls.neoforge.item;

import com.yabo.soulbounddolls.neoforge.SoulboundDollsComponents;
import com.yabo.soulbounddolls.neoforge.SoulboundDollsItems;
import net.minecraft.world.item.ItemStack;

public final class DollStackHelper {
    private DollStackHelper() {
    }

    public static boolean isBoundPlayerDoll(ItemStack stack) {
        return stack.is(SoulboundDollsItems.PLAYER_DOLL.get())
                && stack.get(SoulboundDollsComponents.PLAYER_DOLL_PROFILE.get()) != null;
    }
}
