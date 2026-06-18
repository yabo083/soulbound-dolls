package com.yabo.soulbounddolls.neoforge.compat.curios;

import com.yabo.soulbounddolls.neoforge.SoulboundDollsNeoForge;
import com.yabo.soulbounddolls.neoforge.item.DollStackHelper;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;

public final class CuriosDollLookup {
    private static final String CURIOS_API_CLASS = "top.theillusivec4.curios.api.CuriosApi";

    private CuriosDollLookup() {
    }

    public static boolean hasEquippedDoll(Player player) {
        return findEquippedDoll(player).isPresent();
    }

    public static boolean isEquippedDoll(Player player, ItemStack stack) {
        return !stack.isEmpty() && findEquippedDoll(player)
                .map(equippedStack -> ItemStack.matches(equippedStack, stack))
                .orElse(false);
    }

    private static Optional<ItemStack> findEquippedDoll(Player player) {
        if (!ModList.get().isLoaded("curios")) {
            return Optional.empty();
        }

        try {
            Class<?> curiosApi = Class.forName(CURIOS_API_CLASS);
            Method getCuriosInventory = curiosApi.getMethod("getCuriosInventory", LivingEntity.class);
            Object inventoryResult = getCuriosInventory.invoke(null, player);
            if (!(inventoryResult instanceof Optional<?> optionalInventory) || optionalInventory.isEmpty()) {
                return Optional.empty();
            }

            Object inventory = optionalInventory.get();
            Method findFirstCurio = inventory.getClass().getMethod("findFirstCurio", Predicate.class);
            Object found = findFirstCurio.invoke(inventory, (Predicate<ItemStack>) DollStackHelper::isBoundPlayerDoll);
            if (found instanceof Optional<?> optionalFound && optionalFound.orElse(null) instanceof ItemStack stack) {
                return Optional.of(stack);
            }
        } catch (ReflectiveOperationException | LinkageError | ClassCastException exception) {
            SoulboundDollsNeoForge.LOGGER.debug("Curios doll lookup failed", exception);
        }
        return Optional.empty();
    }
}
