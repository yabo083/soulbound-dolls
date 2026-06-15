package com.yabo.soulbounddolls.neoforge.entity;

import com.yabo.soulbounddolls.neoforge.item.DollStackHelper;
import java.util.function.BooleanSupplier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;

public final class EnderMaskHelper {
    private EnderMaskHelper() {
    }

    public static boolean isProtected(boolean headSlotHasDoll, BooleanSupplier optionalSlotLookup) {
        return headSlotHasDoll || optionalSlotLookup.getAsBoolean();
    }

    public static boolean isProtected(Player player, BooleanSupplier optionalSlotLookup) {
        return isProtected(DollStackHelper.isBoundPlayerDoll(player.getItemBySlot(EquipmentSlot.HEAD)), optionalSlotLookup);
    }
}
