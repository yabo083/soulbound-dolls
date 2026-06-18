package com.yabo.soulbounddolls.neoforge.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.yabo.soulbounddolls.common.PlayerDollProfile;
import com.yabo.soulbounddolls.neoforge.SoulboundDollsComponents;
import com.yabo.soulbounddolls.neoforge.SoulboundDollsItems;
import com.yabo.soulbounddolls.neoforge.entity.PlayerDollEntity.DollPose;
import com.yabo.soulbounddolls.neoforge.item.PlayerDollItem;
import java.util.UUID;
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
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class CycleWornDollPosePacketTest {
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
        ResourceLocation poseComponentId = SoulboundDollsComponents.PLAYER_DOLL_POSE.getId();
        if (!BuiltInRegistries.DATA_COMPONENT_TYPE.containsKey(poseComponentId)) {
            Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, poseComponentId, DataComponentType.<Integer>builder()
                    .persistent(com.mojang.serialization.Codec.INT)
                    .networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.VAR_INT)
                    .build());
        }
    }

    @Test
    void cyclesBoundHeadDollPoseComponent() {
        ItemStack stack = boundDoll();

        assertTrue(CycleWornDollPosePacket.cyclePose(stack));
        assertEquals(DollPose.STANDING.id(), stack.get(SoulboundDollsComponents.PLAYER_DOLL_POSE.get()));

        assertTrue(CycleWornDollPosePacket.cyclePose(stack));
        assertEquals(DollPose.CUTE_IDLE.id(), stack.get(SoulboundDollsComponents.PLAYER_DOLL_POSE.get()));
    }

    @Test
    void resetPoseRemovesWornPoseComponentForStacking() {
        ItemStack stack = boundDoll();

        assertTrue(CycleWornDollPosePacket.cyclePose(stack));
        assertTrue(stack.has(SoulboundDollsComponents.PLAYER_DOLL_POSE.get()));
        assertTrue(CycleWornDollPosePacket.resetPose(stack));

        assertFalse(stack.has(SoulboundDollsComponents.PLAYER_DOLL_POSE.get()));
    }

    @Test
    void ignoresUnboundOrUnrelatedHeadItems() {
        assertFalse(CycleWornDollPosePacket.cyclePose(new ItemStack(SoulboundDollsItems.PLAYER_DOLL.get())));
        assertFalse(CycleWornDollPosePacket.cyclePose(new ItemStack(Items.STONE)));
    }

    private static ItemStack boundDoll() {
        PlayerDollProfile profile = PlayerDollProfile.of(UUID.randomUUID(), "Alex", "", "", false, 1L);
        return PlayerDollItem.createBoundDoll(profile);
    }
}
