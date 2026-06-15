package com.yabo.soulbounddolls.neoforge.client;

import com.yabo.soulbounddolls.common.DollConstants;
import com.yabo.soulbounddolls.neoforge.SoulboundDollsEntities;
import com.yabo.soulbounddolls.neoforge.SoulboundDollsItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;

@EventBusSubscriber(modid = DollConstants.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class SoulboundDollsClientEvents {
    private static PlayerDollItemRenderer playerDollItemRenderer;

    private SoulboundDollsClientEvents() {
    }

    private static PlayerDollItemRenderer getPlayerDollItemRenderer() {
        if (playerDollItemRenderer == null) {
            Minecraft minecraft = Minecraft.getInstance();
            playerDollItemRenderer = new PlayerDollItemRenderer(
                    minecraft.getBlockEntityRenderDispatcher(),
                    minecraft.getEntityModels());
        }
        return playerDollItemRenderer;
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(SoulboundDollsEntities.PLAYER_DOLL.get(), PlayerDollRenderer::new);
        event.registerEntityRenderer(SoulboundDollsEntities.DOLL_PROJECTILE.get(), DollProjectileRenderer::new);
    }

    @SubscribeEvent
    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(PlayerDollModel.LAYER_LOCATION, PlayerDollModel::createBodyLayer);
    }

    @SubscribeEvent
    public static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(PlayerDollItemRenderer.STATIC_MODEL);
    }

    @SubscribeEvent
    public static void registerClientReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(getPlayerDollItemRenderer());
    }

    @SubscribeEvent
    public static void registerClientExtensions(RegisterClientExtensionsEvent event) {
        BlockEntityWithoutLevelRenderer renderer = getPlayerDollItemRenderer();
        event.registerItem(new IClientItemExtensions() {
            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return renderer;
            }
        }, SoulboundDollsItems.PLAYER_DOLL.get());
    }
}
