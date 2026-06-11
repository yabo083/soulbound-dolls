package com.yabo.soulbounddolls.neoforge.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.yabo.soulbounddolls.common.PlayerDollProfile;
import com.yabo.soulbounddolls.neoforge.entity.PlayerDollEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public final class PlayerDollRenderer extends EntityRenderer<PlayerDollEntity> {
    private final PlayerDollModel model;

    public PlayerDollRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new PlayerDollModel(context.bakeLayer(PlayerDollModel.LAYER_LOCATION));
        this.shadowRadius = 0.18F;
    }

    @Override
    public void render(PlayerDollEntity doll, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        /*位置矩阵栈*/
        poseStack.pushPose();
        /*上提玩偶*/
        poseStack.translate(0.0D, 0.78D, 0.0D);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - entityYaw));
        /*缩小玩偶，玩偶就该有玩偶的样子*/
        poseStack.scale(0.62F, 0.62F, 0.62F);

        int shakeTicks = doll.getShakeTicks();
        if (shakeTicks > 0) {
            float shake = (shakeTicks - partialTick) / 18.0F;
            poseStack.mulPose(Axis.ZP.rotationDegrees(Mth.sin((doll.tickCount + partialTick) * 1.4F) * 5.0F * shake));
        }

        int patTicks = doll.getPatTicks();
        if (patTicks > 0) {
            float pat = (patTicks - partialTick) / 12.0F;
            poseStack.translate(0.0D, -0.03D * Mth.sin((doll.tickCount + partialTick) * 0.9F) * pat, 0.0D);
        }

        poseStack.scale(-1.0F, -1.0F, 1.0F);
        model.setupAnim(doll, 0.0F, 0.0F, doll.tickCount + partialTick, 0.0F, 0.0F);
        VertexConsumer buffer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(getTextureLocation(doll)));
        model.renderToBuffer(poseStack, buffer, packedLight, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();

        super.render(doll, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(PlayerDollEntity doll) {
        PlayerDollProfile profile = doll.getProfile();
        return DollSkinManager.getInstance().resolve(profile);
    }
}
