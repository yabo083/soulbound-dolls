package com.yabo.soulbounddolls.neoforge.compat.curios;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.yabo.soulbounddolls.common.PlayerDollProfile;
import com.yabo.soulbounddolls.neoforge.SoulboundDollsComponents;
import com.yabo.soulbounddolls.neoforge.SoulboundDollsItems;
import com.yabo.soulbounddolls.neoforge.item.PlayerDollItem;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.SharedConstants;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.GameData;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class CuriosDollLookupTest {
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
    void extractsItemStackFromCuriosSlotResultOptional() throws ReflectiveOperationException {
        ItemStack stack = PlayerDollItem.createBoundDoll(PlayerDollProfile.of(UUID.randomUUID(), "Alex", "", "", false, 1L));

        Optional<ItemStack> extracted = CuriosDollLookup.stackFromFindFirstCurioResult(Optional.of(new FakeSlotResult(stack)));

        assertTrue(extracted.isPresent());
        assertEquals(stack, extracted.get());
    }

    record FakeSlotResult(ItemStack stack) {
    }
}
