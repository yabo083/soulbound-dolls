package com.yabo.soulbounddolls.neoforge.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.yabo.soulbounddolls.common.DollConstants;
import com.yabo.soulbounddolls.neoforge.network.TeleportToDollPlayerPacket;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.common.util.Lazy;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = DollConstants.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class SoulboundDollsKeyBindings {
    public static final Lazy<KeyMapping> TELEPORT_TO_PLAYER = Lazy.of(() -> new KeyMapping(
            "key.soulbound_dolls.teleport_to_player",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_V, // Default: V key
            "key.categories.soulbound_dolls"
    ));

    @SubscribeEvent
    public static void register(RegisterKeyMappingsEvent event) {
        event.register(TELEPORT_TO_PLAYER.get());
    }

    private SoulboundDollsKeyBindings() {
    }
}
