package com.yabo.soulbounddolls.neoforge.client;

import com.yabo.soulbounddolls.common.DollConstants;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

/**
 * Clears the per-player doll skin cache when the client disconnects from a world or server, so
 * stale {@link net.minecraft.resources.ResourceLocation}s from a previous session do not linger.
 * The cache is purely session-level derived state, so it is never persisted.
 */
@EventBusSubscriber(modid = DollConstants.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class DollSkinCacheLifecycle {
    private DollSkinCacheLifecycle() {
    }

    @SubscribeEvent
    public static void onClientLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        DollSkinManager.getInstance().invalidateAll();
    }
}
