package com.yabo.soulbounddolls.neoforge.entity;

import com.yabo.soulbounddolls.common.PlayerDollProfile;
import com.yabo.soulbounddolls.neoforge.SoulboundDollsComponents;
import com.yabo.soulbounddolls.neoforge.SoulboundDollsItems;
import com.yabo.soulbounddolls.neoforge.item.PlayerDollItem;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import net.minecraft.SharedConstants;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.GameData;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ZombieDollCarryHelperTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        GameData.unfreezeData();
        registerTestEntries();
    }

    @AfterAll
    static void refreezeMinecraft() {
        GameData.freezeData();
    }

    private static void registerTestEntries() {
        ResourceLocation playerDollId = SoulboundDollsItems.PLAYER_DOLL.getId();
        if (!BuiltInRegistries.ITEM.containsKey(playerDollId)) {
            Registry.register(BuiltInRegistries.ITEM, playerDollId, new PlayerDollItem(new Item.Properties().stacksTo(16)));
        }

        ResourceLocation profileComponentId = SoulboundDollsComponents.PLAYER_DOLL_PROFILE.getId();
        if (!BuiltInRegistries.DATA_COMPONENT_TYPE.containsKey(profileComponentId)) {
            Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, profileComponentId, DataComponentType.<PlayerDollProfile>builder()
                    .persistent(SoulboundDollsComponents.PLAYER_DOLL_PROFILE_CODEC)
                    .networkSynchronized(SoulboundDollsComponents.PLAYER_DOLL_PROFILE_STREAM_CODEC)
                    .build());
        }
    }

    @Test
    void choosesFirstEmptyVisibleSlotWithoutReplacingGear() {
        assertEquals(EquipmentSlot.HEAD, ZombieDollCarryHelper.firstEmptyVisibleSlot(false, false, false).orElseThrow());
        assertEquals(EquipmentSlot.MAINHAND, ZombieDollCarryHelper.firstEmptyVisibleSlot(true, false, false).orElseThrow());
        assertEquals(EquipmentSlot.OFFHAND, ZombieDollCarryHelper.firstEmptyVisibleSlot(true, true, false).orElseThrow());
        assertTrue(ZombieDollCarryHelper.firstEmptyVisibleSlot(true, true, true).isEmpty());
    }

    @Test
    void respectsTotalCarryCapOfThree() {
        assertTrue(ZombieDollCarryHelper.hasRoom(0));
        assertTrue(ZombieDollCarryHelper.hasRoom(2));
        assertFalse(ZombieDollCarryHelper.hasRoom(3));
    }

    @Test
    void carryingAnyDollMakesZombieSunProtected() {
        assertFalse(ZombieDollCarryHelper.isSunProtected(0));
        assertTrue(ZombieDollCarryHelper.isSunProtected(1));
        assertTrue(ZombieDollCarryHelper.isSunProtected(3));
    }

    @Test
    void hiddenCarryListNeverExceedsRemainingCap() {
        List<ItemStack> stored = List.of(boundDoll("A"), boundDoll("B"), boundDoll("C"));

        assertEquals(0, ZombieDollCarryHelper.hiddenCapacity(3));
        assertEquals(1, ZombieDollCarryHelper.hiddenCapacity(2));
        assertEquals(3, ZombieDollCarryHelper.hiddenCapacity(0));
        assertEquals(3, ZombieDollCarryHelper.trimHiddenStoredDolls(stored, 0).size());
        assertEquals(1, ZombieDollCarryHelper.trimHiddenStoredDolls(stored, 2).size());
    }

    @Test
    void choosesVisibleDollSlotForHeadPromotion() {
        assertEquals(EquipmentSlot.MAINHAND, ZombieDollCarryHelper.firstVisibleDollSlotForHeadPromotion(true, true).orElseThrow());
        assertEquals(EquipmentSlot.MAINHAND, ZombieDollCarryHelper.firstVisibleDollSlotForHeadPromotion(true, false).orElseThrow());
        assertEquals(EquipmentSlot.OFFHAND, ZombieDollCarryHelper.firstVisibleDollSlotForHeadPromotion(false, true).orElseThrow());
        assertTrue(ZombieDollCarryHelper.firstVisibleDollSlotForHeadPromotion(false, false).isEmpty());
    }

    private static ItemStack boundDoll(String name) {
        PlayerDollProfile profile = PlayerDollProfile.of(
                UUID.nameUUIDFromBytes(name.getBytes(StandardCharsets.UTF_8)),
                name,
                "",
                "",
                false,
                1L);
        return PlayerDollItem.createBoundDoll(profile);
    }
}
