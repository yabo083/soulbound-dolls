package com.yabo.soulbounddolls.neoforge.client;

import com.yabo.soulbounddolls.common.DollConstants;
import com.yabo.soulbounddolls.neoforge.network.CycleWornDollPosePacket;
import com.yabo.soulbounddolls.neoforge.network.TeleportToDollPlayerPacket;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Client-side event handler for key inputs (FORGE event bus).
 */
@EventBusSubscriber(modid = DollConstants.MOD_ID, value = Dist.CLIENT)
public final class SoulboundDollsClientInputHandler {
    private SoulboundDollsClientInputHandler() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        // Check if teleport key was pressed
        while (SoulboundDollsKeyBindings.TELEPORT_TO_PLAYER.get().consumeClick()) {
            // Send packet to server
            PacketDistributor.sendToServer(new TeleportToDollPlayerPacket());
        }
        while (SoulboundDollsKeyBindings.CYCLE_WORN_DOLL_POSE.get().consumeClick()) {
            PacketDistributor.sendToServer(new CycleWornDollPosePacket());
        }
    }
}
