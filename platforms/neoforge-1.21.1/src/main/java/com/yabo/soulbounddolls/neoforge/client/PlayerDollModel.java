package com.yabo.soulbounddolls.neoforge.client;

import com.yabo.soulbounddolls.common.DollConstants;
import com.yabo.soulbounddolls.neoforge.entity.PlayerDollEntity;
import com.yabo.soulbounddolls.neoforge.entity.PlayerDollEntity.DollPose;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public final class PlayerDollModel extends EntityModel<PlayerDollEntity> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(DollConstants.MOD_ID, "player_doll"),
            "main");

    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart body;
    private final ModelPart leftArm;
    private final ModelPart rightArm;
    private final ModelPart leftLeg;
    private final ModelPart rightLeg;

    public PlayerDollModel(ModelPart root) {
        this.root = root;
        this.head = root.getChild("head");
        this.body = root.getChild("body");
        this.leftArm = root.getChild("left_arm");
        this.rightArm = root.getChild("right_arm");
        this.leftLeg = root.getChild("left_leg");
        this.rightLeg = root.getChild("right_leg");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("head", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F)
                        .texOffs(32, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.45F)),
                PartPose.offsetAndRotation(0.0F, 4.5F, -1.2F, 0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("body", CubeListBuilder.create()
                        .texOffs(16, 16).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 8.0F, 4.0F)
                        .texOffs(16, 32).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 8.0F, 4.0F, new CubeDeformation(0.25F)),
                PartPose.offset(0.0F, 4.5F, -1.2F));
        root.addOrReplaceChild("right_arm", CubeListBuilder.create()
                        .texOffs(40, 16).addBox(-3.0F, -2.0F, -2.0F, 4.0F, 8.0F, 4.0F)
                        .texOffs(40, 32).addBox(-3.0F, -2.0F, -2.0F, 4.0F, 8.0F, 4.0F, new CubeDeformation(0.2F)),
                PartPose.offset(-5.0F, 6.2F, -1.2F));
        root.addOrReplaceChild("left_arm", CubeListBuilder.create().mirror()
                        .texOffs(32, 48).addBox(-1.0F, -2.0F, -2.0F, 4.0F, 8.0F, 4.0F)
                        .texOffs(48, 48).addBox(-1.0F, -2.0F, -2.0F, 4.0F, 8.0F, 4.0F, new CubeDeformation(0.2F)),
                PartPose.offset(5.0F, 6.2F, -1.2F));
        root.addOrReplaceChild("right_leg", CubeListBuilder.create()
                        .texOffs(0, 16).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 7.0F, 4.0F)
                        .texOffs(0, 32).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 7.0F, 4.0F, new CubeDeformation(0.2F)),
                PartPose.offset(-2.0F, 12.5F, -1.2F));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create().mirror()
                        .texOffs(16, 48).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 7.0F, 4.0F)
                        .texOffs(0, 48).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 7.0F, 4.0F, new CubeDeformation(0.2F)),
                PartPose.offset(2.0F, 12.5F, -1.2F));

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(PlayerDollEntity doll, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        setupDollAnim(doll.getDollPose(), doll.getPatTicks(), ageInTicks, netHeadYaw, headPitch);
    }

    public void setupDollAnim(DollPose dollPose, int patTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        root.getAllParts().forEach(ModelPart::resetPose);
        head.yRot = netHeadYaw * Mth.DEG_TO_RAD;
        head.xRot = headPitch * Mth.DEG_TO_RAD;

        float idle = Mth.sin(ageInTicks * 0.16F) * 0.05F;
        body.zRot = idle;
        head.zRot = -idle * 0.5F;

        if (dollPose == DollPose.STANDING) {
            leftArm.xRot = -0.18F - idle;
            rightArm.xRot = -0.18F + idle;
            leftArm.zRot = -0.12F;
            rightArm.zRot = 0.12F;
            leftLeg.xRot = 0.0F;
            rightLeg.xRot = 0.0F;
            leftLeg.yRot = -0.03F;
            rightLeg.yRot = 0.03F;
        } else if (dollPose == DollPose.CUTE_IDLE) {
            head.zRot = -0.16F - idle * 0.5F;
            leftArm.xRot = -0.92F - idle;
            rightArm.xRot = -0.34F + idle;
            leftArm.zRot = -0.36F;
            rightArm.zRot = 0.42F;
            leftLeg.xRot = -0.22F;
            rightLeg.xRot = -0.36F;
            leftLeg.yRot = -0.22F;
            rightLeg.yRot = 0.28F;
        } else {
            leftArm.xRot = -0.58F - idle;
            rightArm.xRot = -0.58F + idle;
            leftArm.zRot = -0.18F;
            rightArm.zRot = 0.18F;
            leftLeg.xRot = -1.42F;
            rightLeg.xRot = -1.42F;
            leftLeg.yRot = -0.1F;
            rightLeg.yRot = 0.1F;
        }

        if (patTicks > 0) {
            float pat = patTicks / 12.0F;
            head.y += Mth.sin(ageInTicks * 0.9F) * 0.45F * pat;
            head.xRot -= 0.18F * pat;
        }
    }

    @Override
    public void renderToBuffer(com.mojang.blaze3d.vertex.PoseStack poseStack, com.mojang.blaze3d.vertex.VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
        root.render(poseStack, buffer, packedLight, packedOverlay, color);
    }
}
