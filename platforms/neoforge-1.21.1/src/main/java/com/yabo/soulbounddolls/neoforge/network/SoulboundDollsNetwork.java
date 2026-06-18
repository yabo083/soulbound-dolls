package com.yabo.soulbounddolls.neoforge.network;

import com.yabo.soulbounddolls.common.DollConstants;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = DollConstants.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class SoulboundDollsNetwork {
    private static final String PROTOCOL_VERSION = "1";

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);

        // Client -> Server: teleport to doll's bound player
        registrar.playToServer(
                TeleportToDollPlayerPacket.TYPE,
                TeleportToDollPlayerPacket.STREAM_CODEC,
                TeleportToDollPlayerPacket::handle
        );
        registrar.playToServer(
                CycleWornDollPosePacket.TYPE,
                CycleWornDollPosePacket.STREAM_CODEC,
                CycleWornDollPosePacket::handle
        );
    }

    private SoulboundDollsNetwork() {
    }
}
