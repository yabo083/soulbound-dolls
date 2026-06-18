package com.yabo.soulbounddolls.neoforge.item;

import com.yabo.soulbounddolls.neoforge.SoulboundDollsComponents;
import com.yabo.soulbounddolls.neoforge.SoulboundDollsItems;
import net.minecraft.world.item.ItemStack;

public final class DollStackHelper {
    private DollStackHelper() {
    }

    public static boolean isBoundPlayerDoll(ItemStack stack) {
        return isPlayerDoll(stack)
                && stack.get(SoulboundDollsComponents.PLAYER_DOLL_PROFILE.get()) != null;
    }

    public static boolean isPlayerDoll(ItemStack stack) {
        return stack.is(SoulboundDollsItems.PLAYER_DOLL.get());
    }

    public static ItemStack singleHeadSlotDoll(ItemStack stack) {
        if (!isPlayerDoll(stack)) {
            return stack;
        }
        return stack.copyWithCount(1);
    }
}
