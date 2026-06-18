package com.yabo.soulbounddolls.neoforge.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.gson.JsonObject;
import com.yabo.soulbounddolls.neoforge.entity.PlayerDollEntity.DollPose;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class PlayerDollItemRendererTest {
    @Test
    void parseTextureAliasesKeepsItemModelTextureAsAtlasSpriteId() throws Exception {
        JsonObject textures = new JsonObject();
        textures.addProperty("dark", "soulbound_dolls:item/player_doll_dark");
        JsonObject model = new JsonObject();
        model.add("textures", textures);

        Map<String, ResourceLocation> aliases = TemplateTextureAliases.parse(model);

        assertEquals(
                ResourceLocation.fromNamespaceAndPath("soulbound_dolls", "item/player_doll_dark"),
                aliases.get("#dark"));
    }

    @Test
    void boundItemsUseEntityModelRenderStrategy() {
        assertEquals(PlayerDollItemRenderStrategy.ENTITY_MODEL, PlayerDollItemRenderStrategy.forBoundProfile(true));
        assertEquals(PlayerDollItemRenderStrategy.TEMPLATE_MODEL, PlayerDollItemRenderStrategy.forBoundProfile(false));
    }

    @Test
    void missingCarriedPoseFallsBackToStandingItemPose() {
        assertEquals(DollPose.STANDING, PlayerDollItemPose.fromComponent(null));
    }

    @Test
    void missingHeadSlotPoseFallsBackToSittingPose() {
        assertEquals(DollPose.SITTING, PlayerDollItemPose.fromHeadComponent(null));
    }

    @Test
    void carriedPoseComponentSelectsMatchingDollPose() {
        assertEquals(DollPose.SITTING, PlayerDollItemPose.fromComponent(0));
        assertEquals(DollPose.STANDING, PlayerDollItemPose.fromComponent(1));
        assertEquals(DollPose.CUTE_IDLE, PlayerDollItemPose.fromComponent(2));
        assertEquals(DollPose.SITTING, PlayerDollItemPose.fromComponent(999));
    }

    @Test
    void sittingPoseUsesLowerHeadSlotYOffset() {
        assertEquals(-0.08F, PlayerDollItemPose.headYOffset(DollPose.SITTING), 0.0001F);
        assertEquals(0.0F, PlayerDollItemPose.headYOffset(DollPose.STANDING), 0.0001F);
        assertEquals(0.0F, PlayerDollItemPose.headYOffset(DollPose.CUTE_IDLE), 0.0001F);
    }

    @Test
    void headOffsetCombinesEntityKindAndPoseVectors() {
        PlayerDollItemPose.Offset entityOffset = new PlayerDollItemPose.Offset(0.1F, 0.2F, 0.3F);
        PlayerDollItemPose.Offset poseOffset = new PlayerDollItemPose.Offset(-0.2F, -0.08F, 0.4F);

        PlayerDollItemPose.Offset combined = PlayerDollItemPose.combineOffsets(entityOffset, poseOffset);

        assertEquals(-0.1F, combined.x(), 0.0001F);
        assertEquals(0.12F, combined.y(), 0.0001F);
        assertEquals(0.7F, combined.z(), 0.0001F);
    }

    @Test
    void configOffsetTreatsNumbersAsHeadTuningUnits() {
        PlayerDollItemPose.Offset offset = PlayerDollItemPose.configOffset(() -> 1.0D, () -> -1.0D, () -> 2.0D);

        assertEquals(1.0F, offset.x(), 0.0001F);
        assertEquals(-1.0F, offset.y(), 0.0001F);
        assertEquals(2.0F, offset.z(), 0.0001F);
    }

    @Test
    void jsonUvConvertsToSpriteInterpolationCoordinates() {
        assertEquals(0.0F, TemplateUv.spriteCoordinate(0.0F));
        assertEquals(0.5F, TemplateUv.spriteCoordinate(8.0F));
        assertEquals(1.0F, TemplateUv.spriteCoordinate(16.0F));
    }
}
