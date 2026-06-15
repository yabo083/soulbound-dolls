package com.yabo.soulbounddolls.neoforge.item;

import com.yabo.soulbounddolls.common.PlayerDollProfile;
import com.yabo.soulbounddolls.neoforge.SoulboundDollsComponents;
import com.yabo.soulbounddolls.neoforge.SoulboundDollsItems;
import net.minecraft.SharedConstants;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.registries.GameData;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DollStackHelperTest {
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
    void recognizesBoundPlayerDollStack() {
        PlayerDollProfile profile = PlayerDollProfile.of(UUID.randomUUID(), "Alex", "", "", false, 1L);
        ItemStack stack = PlayerDollItem.createBoundDoll(profile);

        assertTrue(DollStackHelper.isBoundPlayerDoll(stack));
    }

    @Test
    void rejectsUnboundOrUnrelatedStacks() {
        assertFalse(DollStackHelper.isBoundPlayerDoll(new ItemStack(SoulboundDollsItems.PLAYER_DOLL.get())));
        assertFalse(DollStackHelper.isBoundPlayerDoll(new ItemStack(Items.STONE)));
        assertFalse(DollStackHelper.isBoundPlayerDoll(ItemStack.EMPTY));
    }
}
