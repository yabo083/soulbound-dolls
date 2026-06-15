package com.yabo.soulbounddolls.neoforge.client;

import com.yabo.soulbounddolls.neoforge.entity.DollProjectileEntity;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemStack;

/**
 * Renderer for the thrown doll projectile.
 * Renders the doll item in flight using the item renderer.
 */
public class DollProjectileRenderer extends EntityRenderer<DollProjectileEntity> {
    private final ItemRenderer itemRenderer;

    public DollProjectileRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(DollProjectileEntity entity, float entityYaw, float partialTicks,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();

        // Scale down slightly for visual clarity
        poseStack.scale(0.5F, 0.5F, 0.5F);

        // Rotate the item to make it spin slightly in flight
        float rotation = (entity.tickCount + partialTicks) * 20.0F;
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(rotation));
        poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(rotation * 0.5F));

        // Create the item stack to render
        ItemStack dollStack = com.yabo.soulbounddolls.neoforge.item.PlayerDollItem.createBoundDoll(entity.getProfile());

        // Render the item
        itemRenderer.renderStatic(
                dollStack,
                ItemDisplayContext.GROUND,
                packedLight,
                net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,
                poseStack,
                buffer,
                entity.level(),
                entity.getId()
        );

        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(DollProjectileEntity entity) {
        // Not used since we render via ItemRenderer
        return null;
    }
}
