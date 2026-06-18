package com.yabo.soulbounddolls.neoforge.entity;

import com.yabo.soulbounddolls.common.DollConstants;
import com.yabo.soulbounddolls.common.PlayerDollProfile;
import com.yabo.soulbounddolls.neoforge.SoulboundDollsComponents;
import com.yabo.soulbounddolls.neoforge.item.DollStackHelper;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;

public final class ZombieDollCarryHelper {
    public static final int MAX_CARRIED_DOLLS = 3;
    private static final String HIDDEN_CARRIED_DOLLS_TAG = DollConstants.MOD_ID + ":carried_dolls";
    private static final String FRIENDLY_NAME_TAG = DollConstants.MOD_ID + ":friendly_name";
    private static final String FRIENDLY_TRUCE_RESET_TAG = DollConstants.MOD_ID + ":friendly_truce_reset";
    private static final Component FRIENDLY_ZOMBIE_NAME = Component.translatable("entity.soulbound_dolls.friendly_zombie");
    private static final List<EquipmentSlot> VISIBLE_CARRY_SLOTS = List.of(
            EquipmentSlot.HEAD,
            EquipmentSlot.MAINHAND,
            EquipmentSlot.OFFHAND);

    private ZombieDollCarryHelper() {
    }

    public static boolean hasRoom(int carriedCount) {
        return carriedCount < MAX_CARRIED_DOLLS;
    }

    public static boolean hasRoom(Zombie zombie) {
        return hasRoom(carriedDollCount(zombie));
    }

    public static boolean isSunProtected(int carriedDollCount) {
        return carriedDollCount > 0;
    }

    public static int carriedDollCount(Zombie zombie) {
        return countVisibleBoundDolls(zombie) + countHiddenStoredDolls(zombie);
    }

    public static int countVisibleBoundDolls(Zombie zombie) {
        int count = 0;
        for (EquipmentSlot slot : VISIBLE_CARRY_SLOTS) {
            if (DollStackHelper.isBoundPlayerDoll(zombie.getItemBySlot(slot))) {
                count++;
            }
        }
        return count;
    }

    public static int countHiddenStoredDolls(Zombie zombie) {
        return hiddenStoredDolls(zombie).size();
    }

    public static int hiddenCapacity(int visibleCarriedCount) {
        return Math.max(0, MAX_CARRIED_DOLLS - Math.max(0, visibleCarriedCount));
    }

    public static List<ItemStack> trimHiddenStoredDolls(List<ItemStack> storedDolls, int visibleCarriedCount) {
        int capacity = hiddenCapacity(visibleCarriedCount);
        List<ItemStack> trimmed = new ArrayList<>(Math.min(storedDolls.size(), capacity));
        for (ItemStack storedDoll : storedDolls) {
            if (trimmed.size() >= capacity) {
                break;
            }
            if (!storedDoll.isEmpty()) {
                trimmed.add(storedDoll.copyWithCount(1));
            }
        }
        return trimmed;
    }

    public static boolean tryAcceptDoll(Zombie zombie, ItemStack stack) {
        if (!DollStackHelper.isBoundPlayerDoll(stack) || !hasRoom(zombie)) {
            return false;
        }

        ItemStack carriedDoll = withRandomCarriedPose(zombie, stack.copyWithCount(1));
        Optional<EquipmentSlot> visibleSlot = firstEmptyVisibleSlot(
                !zombie.getItemBySlot(EquipmentSlot.HEAD).isEmpty(),
                !zombie.getItemBySlot(EquipmentSlot.MAINHAND).isEmpty(),
                !zombie.getItemBySlot(EquipmentSlot.OFFHAND).isEmpty());

        if (visibleSlot.isPresent()) {
            setGuaranteedDollDrop(zombie, visibleSlot.get(), carriedDoll);
            stack.shrink(1);
            zombie.clearFire();
            applyFriendlyState(zombie);
            return true;
        }

        int visibleCarriedCount = countVisibleBoundDolls(zombie);
        List<ItemStack> hiddenDolls = hiddenStoredDolls(zombie);
        if (hiddenDolls.size() >= hiddenCapacity(visibleCarriedCount)) {
            return false;
        }

        hiddenDolls.add(carriedDoll);
        storeHiddenDolls(zombie, hiddenDolls, visibleCarriedCount);
        stack.shrink(1);
        zombie.clearFire();
        applyFriendlyState(zombie);
        return true;
    }

    public static List<ItemStack> takeHiddenStoredDolls(Zombie zombie) {
        List<ItemStack> hiddenDolls = hiddenStoredDolls(zombie);
        zombie.getPersistentData().remove(HIDDEN_CARRIED_DOLLS_TAG);
        return hiddenDolls;
    }

    public static List<ItemStack> hiddenStoredDolls(Zombie zombie) {
        ListTag storedTags = zombie.getPersistentData().getList(HIDDEN_CARRIED_DOLLS_TAG, Tag.TAG_COMPOUND);
        List<ItemStack> storedDolls = new ArrayList<>(storedTags.size());
        for (Tag storedTag : storedTags) {
            ItemStack.parse(zombie.registryAccess(), storedTag)
                    .filter(DollStackHelper::isBoundPlayerDoll)
                    .map(storedDoll -> storedDoll.copyWithCount(1))
                    .ifPresent(storedDolls::add);
        }
        return trimHiddenStoredDolls(storedDolls, countVisibleBoundDolls(zombie));
    }

    public static boolean shouldAvoidTargetingOwner(ItemStack headDoll, UUID targetUuid, boolean ownerRecentlyAttackedZombie) {
        return shouldAvoidTargetingOwner(headDoll, targetUuid, ownerRecentlyAttackedZombie, false);
    }

    public static boolean shouldAvoidTargetingOwner(
            ItemStack headDoll,
            UUID targetUuid,
            boolean ownerRecentlyAttackedZombie,
            boolean truceReset) {
        return !truceReset && !ownerRecentlyAttackedZombie && isOwnedBy(headDoll, targetUuid);
    }

    public static boolean shouldAvoidTargetingOwner(Zombie zombie, UUID targetUuid) {
        return shouldAvoidTargetingOwner(
                zombie.getItemBySlot(EquipmentSlot.HEAD),
                targetUuid,
                isRecentlyHurtBy(zombie, targetUuid),
                zombie.getPersistentData().getBoolean(FRIENDLY_TRUCE_RESET_TAG));
    }

    public static List<ItemStack> carriedDolls(Zombie zombie) {
        List<ItemStack> carriedDolls = new ArrayList<>();
        for (EquipmentSlot slot : VISIBLE_CARRY_SLOTS) {
            ItemStack stack = zombie.getItemBySlot(slot);
            if (DollStackHelper.isBoundPlayerDoll(stack)) {
                carriedDolls.add(stack);
            }
        }
        carriedDolls.addAll(hiddenStoredDolls(zombie));
        return List.copyOf(carriedDolls);
    }

    public static ItemStack withDefaultDroppedPose(ItemStack stack) {
        if (!DollStackHelper.isBoundPlayerDoll(stack)) {
            return stack;
        }

        ItemStack normalized = stack.copyWithCount(stack.getCount());
        normalized.remove(SoulboundDollsComponents.PLAYER_DOLL_POSE.get());
        return normalized;
    }

    public static boolean shouldUseFriendlyName(ItemStack headStack) {
        return DollStackHelper.isBoundPlayerDoll(headStack);
    }

    public static void applyFriendlyState(Zombie zombie) {
        if (!shouldUseFriendlyName(zombie.getItemBySlot(EquipmentSlot.HEAD))) {
            clearFriendlyName(zombie, false);
            return;
        }

        if (zombie.getPersistentData().getBoolean(FRIENDLY_TRUCE_RESET_TAG)) {
            return;
        }

        zombie.getPersistentData().putBoolean(FRIENDLY_NAME_TAG, true);
        zombie.setCustomName(FRIENDLY_ZOMBIE_NAME);
        zombie.setCustomNameVisible(true);
        zombie.setTarget(null);
    }

    public static void resetFriendlyState(Zombie zombie) {
        if (!zombie.getPersistentData().getBoolean(FRIENDLY_NAME_TAG)) {
            return;
        }

        clearFriendlyName(zombie, true);
    }

    private static void clearFriendlyName(Zombie zombie, boolean markTruceReset) {
        zombie.getPersistentData().remove(FRIENDLY_NAME_TAG);
        if (markTruceReset) {
            zombie.getPersistentData().putBoolean(FRIENDLY_TRUCE_RESET_TAG, true);
        }
        if (FRIENDLY_ZOMBIE_NAME.equals(zombie.getCustomName())) {
            zombie.setCustomName(null);
            zombie.setCustomNameVisible(false);
        }
    }

    public static int carriedPoseIdFromSeed(int seed) {
        return Math.floorMod(seed, PlayerDollEntity.DollPose.COUNT);
    }

    private static void storeHiddenDolls(Zombie zombie, List<ItemStack> hiddenDolls, int visibleCarriedCount) {
        ListTag storedTags = new ListTag();
        for (ItemStack hiddenDoll : trimHiddenStoredDolls(hiddenDolls, visibleCarriedCount)) {
            storedTags.add(hiddenDoll.save(zombie.registryAccess()));
        }
        if (storedTags.isEmpty()) {
            zombie.getPersistentData().remove(HIDDEN_CARRIED_DOLLS_TAG);
            return;
        }
        zombie.getPersistentData().put(HIDDEN_CARRIED_DOLLS_TAG, storedTags);
    }

    public static Optional<EquipmentSlot> firstVisibleDollSlotForHeadPromotion(boolean mainHandHasDoll, boolean offhandHasDoll) {
        if (mainHandHasDoll) {
            return Optional.of(EquipmentSlot.MAINHAND);
        }
        if (offhandHasDoll) {
            return Optional.of(EquipmentSlot.OFFHAND);
        }
        return Optional.empty();
    }

    public static boolean ensureHeadDollForSunProtection(Zombie zombie) {
        if (DollStackHelper.isBoundPlayerDoll(zombie.getItemBySlot(EquipmentSlot.HEAD))) {
            zombie.clearFire();
            applyFriendlyState(zombie);
            return false;
        }

        if (!zombie.getItemBySlot(EquipmentSlot.HEAD).isEmpty()) {
            return false;
        }

        Optional<EquipmentSlot> promotionSlot = firstVisibleDollSlotForHeadPromotion(
                DollStackHelper.isBoundPlayerDoll(zombie.getItemBySlot(EquipmentSlot.MAINHAND)),
                DollStackHelper.isBoundPlayerDoll(zombie.getItemBySlot(EquipmentSlot.OFFHAND)));
        if (promotionSlot.isPresent()) {
            EquipmentSlot slot = promotionSlot.get();
            ItemStack promotedDoll = zombie.getItemBySlot(slot).copyWithCount(1);
            zombie.setItemSlot(slot, ItemStack.EMPTY);
            setGuaranteedDollDrop(zombie, EquipmentSlot.HEAD, promotedDoll);
            zombie.clearFire();
            applyFriendlyState(zombie);
            return true;
        }

        List<ItemStack> hiddenDolls = hiddenStoredDolls(zombie);
        if (hiddenDolls.isEmpty()) {
            return false;
        }

        ItemStack promotedDoll = hiddenDolls.remove(0);
        setGuaranteedDollDrop(zombie, EquipmentSlot.HEAD, promotedDoll);
        storeHiddenDolls(zombie, hiddenDolls, 1);
        zombie.clearFire();
        applyFriendlyState(zombie);
        return true;
    }

    private static void setGuaranteedDollDrop(Zombie zombie, EquipmentSlot slot, ItemStack stack) {
        zombie.setItemSlot(slot, stack.copyWithCount(1));
        zombie.setGuaranteedDrop(slot);
    }

    private static ItemStack withRandomCarriedPose(Zombie zombie, ItemStack stack) {
        stack.set(SoulboundDollsComponents.PLAYER_DOLL_POSE.get(), carriedPoseIdFromSeed(zombie.getRandom().nextInt()));
        return stack;
    }

    private static boolean isOwnedBy(ItemStack stack, UUID targetUuid) {
        PlayerDollProfile profile = stack.get(SoulboundDollsComponents.PLAYER_DOLL_PROFILE.get());
        return profile != null && profile.uuid().equals(targetUuid);
    }

    private static boolean isRecentlyHurtBy(Zombie zombie, UUID targetUuid) {
        return zombie.getLastHurtByMob() != null && zombie.getLastHurtByMob().getUUID().equals(targetUuid);
    }

    public static Optional<EquipmentSlot> firstEmptyVisibleSlot(boolean headOccupied, boolean mainHandOccupied, boolean offhandOccupied) {
        if (!headOccupied) {
            return Optional.of(EquipmentSlot.HEAD);
        }
        if (!mainHandOccupied) {
            return Optional.of(EquipmentSlot.MAINHAND);
        }
        if (!offhandOccupied) {
            return Optional.of(EquipmentSlot.OFFHAND);
        }
        return Optional.empty();
    }
}
