package com.yabo.soulbounddolls.neoforge.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.yabo.soulbounddolls.common.DollConstants;
import com.yabo.soulbounddolls.common.PlayerDollProfile;
import com.yabo.soulbounddolls.neoforge.SoulboundDollsComponents;
import com.yabo.soulbounddolls.neoforge.SoulboundDollsConfig;
import com.yabo.soulbounddolls.neoforge.entity.PlayerDollEntity.DollPose;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.joml.Vector3f;

public final class PlayerDollItemRenderer extends BlockEntityWithoutLevelRenderer {
    public static final ModelResourceLocation STATIC_MODEL = ModelResourceLocation.standalone(
            ResourceLocation.fromNamespaceAndPath(DollConstants.MOD_ID, "item/player_doll_3d"));
    private static final ResourceLocation DISPLAY_TEMPLATE = ResourceLocation.fromNamespaceAndPath(
            DollConstants.MOD_ID, "models/item/player_doll_3d.json");
    private static final int WHITE = 0xFFFFFFFF;

    private DollDisplayConfig displayConfig;
    private TemplateModel templateModel;
    private final EntityModelSet entityModelSet;
    private PlayerDollModel playerDollModel;

    public PlayerDollItemRenderer(BlockEntityRenderDispatcher blockEntityRenderDispatcher, EntityModelSet entityModelSet) {
        super(blockEntityRenderDispatcher, entityModelSet);
        this.entityModelSet = entityModelSet;
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        PlayerDollProfile profile = stack.get(SoulboundDollsComponents.PLAYER_DOLL_PROFILE.get());
        PlayerDollItemRenderStrategy renderStrategy = PlayerDollItemRenderStrategy.forBoundProfile(profile != null);
        TemplateModel templateModel = renderStrategy == PlayerDollItemRenderStrategy.TEMPLATE_MODEL ? getTemplateModel() : null;
        if (renderStrategy == PlayerDollItemRenderStrategy.TEMPLATE_MODEL && templateModel == null) {
            renderStaticFallback(stack, displayContext, poseStack, bufferSource, packedLight, packedOverlay);
            return;
        }

        ResourceLocation skinTexture = profile != null ? DollSkinManager.getInstance().resolve(profile) : null;

        boolean rendered = false;
        poseStack.pushPose();
        try {
            applyDisplayTransform(displayContext, poseStack);
            if (renderStrategy == PlayerDollItemRenderStrategy.ENTITY_MODEL) {
                renderEntityModel(stack, displayContext, skinTexture, poseStack, bufferSource, packedLight);
            } else {
                renderTemplateModel(templateModel, poseStack, bufferSource, packedLight);
            }
            rendered = true;
        } catch (RuntimeException ignored) {
        } finally {
            poseStack.popPose();
        }

        if (!rendered) {
            renderStaticFallback(stack, displayContext, poseStack, bufferSource, packedLight, packedOverlay);
        }
    }

    private DollDisplayConfig getDisplayConfig() {
        if (displayConfig == null) {
            displayConfig = loadDisplayConfig(Minecraft.getInstance().getResourceManager());
        }
        return displayConfig;
    }

    @Override
    public void onResourceManagerReload(ResourceManager resourceManager) {
        displayConfig = loadDisplayConfig(resourceManager);
        templateModel = loadTemplateModel(resourceManager);
        playerDollModel = null;
    }

    private void applyDisplayTransform(ItemDisplayContext displayContext, PoseStack poseStack) {
        getDisplayConfig().apply(displayContext, poseStack);
    }

    private static DollDisplayConfig loadDisplayConfig(ResourceManager resourceManager) {
        try {
            Optional<Resource> resource = resourceManager.getResource(DISPLAY_TEMPLATE);
            if (resource.isEmpty()) {
                return DollDisplayConfig.defaults();
            }
            try (Reader reader = resource.get().openAsReader()) {
                JsonElement root = JsonParser.parseReader(reader);
                if (!root.isJsonObject()) {
                    return DollDisplayConfig.defaults();
                }
                JsonElement display = root.getAsJsonObject().get("display");
                if (display == null || !display.isJsonObject()) {
                    return DollDisplayConfig.defaults();
                }
                return DollDisplayConfig.parse(display.getAsJsonObject());
            }
        } catch (IOException | RuntimeException ignored) {
            return DollDisplayConfig.defaults();
        }
    }

    private TemplateModel getTemplateModel() {
        if (templateModel == null) {
            templateModel = loadTemplateModel(Minecraft.getInstance().getResourceManager());
        }
        return templateModel;
    }

    private static TemplateModel loadTemplateModel(ResourceManager resourceManager) {
        try {
            Optional<Resource> resource = resourceManager.getResource(DISPLAY_TEMPLATE);
            if (resource.isEmpty()) {
                return null;
            }
            try (Reader reader = resource.get().openAsReader()) {
                JsonElement root = JsonParser.parseReader(reader);
                if (!root.isJsonObject()) {
                    return null;
                }
                JsonObject rootObject = root.getAsJsonObject();
                JsonElement elementsElement = rootObject.get("elements");
                if (elementsElement == null || !elementsElement.isJsonArray()) {
                    return null;
                }
                List<TemplateElement> elements = new ArrayList<>();
                for (JsonElement element : elementsElement.getAsJsonArray()) {
                    if (element.isJsonObject()) {
                        elements.add(parseTemplateElement(element.getAsJsonObject()));
                    }
                }
                if (elements.isEmpty()) {
                    return null;
                }
                return new TemplateModel(List.copyOf(elements), parseTextureAliases(rootObject));
            }
        } catch (IOException | RuntimeException ignored) {
            return null;
        }
    }

    private static TemplateElement parseTemplateElement(JsonObject element) {
        String name = element.has("name") ? element.get("name").getAsString() : "";
        Vector3 from = parseVector(element, "from", 0.0F, 0.0F, 0.0F);
        Vector3 to = parseVector(element, "to", 0.0F, 0.0F, 0.0F);
        ElementRotation rotation = parseElementRotation(element.get("rotation"));
        Map<Face, TemplateFace> faces = new EnumMap<>(Face.class);
        JsonElement facesElement = element.get("faces");
        if (facesElement != null && facesElement.isJsonObject()) {
            JsonObject facesObject = facesElement.getAsJsonObject();
            for (Face face : Face.values()) {
                JsonElement faceElement = facesObject.get(face.jsonName());
                if (faceElement != null && faceElement.isJsonObject()) {
                    faces.put(face, parseTemplateFace(faceElement.getAsJsonObject()));
                }
            }
        }
        return new TemplateElement(name, from, to, rotation, Map.copyOf(faces));
    }

    private static TemplateFace parseTemplateFace(JsonObject face) {
        JsonElement uvElement = face.get("uv");
        if (uvElement == null || !uvElement.isJsonArray()) {
            throw new IllegalArgumentException("Expected face uv array");
        }
        JsonArray uvArray = uvElement.getAsJsonArray();
        if (uvArray.size() != 4) {
            throw new IllegalArgumentException("Expected four face uv values");
        }
        String texture = face.has("texture") ? face.get("texture").getAsString() : "#skin";
        return new TemplateFace(new Uv(
                uvArray.get(0).getAsFloat(),
                uvArray.get(1).getAsFloat(),
                uvArray.get(2).getAsFloat(),
                uvArray.get(3).getAsFloat()), texture);
    }

    private static ElementRotation parseElementRotation(JsonElement rotationElement) {
        if (rotationElement == null || !rotationElement.isJsonObject()) {
            return null;
        }
        JsonObject rotation = rotationElement.getAsJsonObject();
        Vector3 origin = parseVector(rotation, "origin", 8.0F, 8.0F, 8.0F);
        Vector3 degrees;
        if (rotation.has("angle") && rotation.has("axis")) {
            float angle = rotation.get("angle").getAsFloat();
            String axis = rotation.get("axis").getAsString();
            degrees = switch (axis) {
                case "x" -> new Vector3(angle, 0.0F, 0.0F);
                case "y" -> new Vector3(0.0F, angle, 0.0F);
                case "z" -> new Vector3(0.0F, 0.0F, angle);
                default -> new Vector3(0.0F, 0.0F, 0.0F);
            };
        } else {
            degrees = new Vector3(
                    rotation.has("x") ? rotation.get("x").getAsFloat() : 0.0F,
                    rotation.has("y") ? rotation.get("y").getAsFloat() : 0.0F,
                    rotation.has("z") ? rotation.get("z").getAsFloat() : 0.0F);
        }
        if (degrees.x() == 0.0F && degrees.y() == 0.0F && degrees.z() == 0.0F) {
            return null;
        }
        return new ElementRotation(degrees, origin);
    }

    private static Map<String, ResourceLocation> parseTextureAliases(JsonObject root) {
        return TemplateTextureAliases.parse(root);
    }

    private static void renderTemplateModel(TemplateModel templateModel, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        for (TemplateElement element : templateModel.elements()) {
            poseStack.pushPose();
            applyElementRotation(element.rotation(), poseStack);
            renderElement(templateModel, element, poseStack, bufferSource, packedLight);
            poseStack.popPose();
        }
    }

    private PlayerDollModel getPlayerDollModel() {
        if (playerDollModel == null) {
            playerDollModel = new PlayerDollModel(entityModelSet.bakeLayer(PlayerDollModel.LAYER_LOCATION));
        }
        return playerDollModel;
    }

    private void renderEntityModel(ItemStack stack, ItemDisplayContext displayContext, ResourceLocation skinTexture, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();
        var dollPose = displayContext == ItemDisplayContext.HEAD
                ? PlayerDollItemPose.fromHeadComponent(stack.get(SoulboundDollsComponents.PLAYER_DOLL_POSE.get()))
                : PlayerDollItemPose.fromComponent(stack.get(SoulboundDollsComponents.PLAYER_DOLL_POSE.get()));
        PlayerDollItemPose.Offset headOffset = displayContext == ItemDisplayContext.HEAD
                ? PlayerDollItemPose.combineOffsets(baseHeadOffset(), poseOffset(dollPose))
                : PlayerDollItemPose.Offset.ZERO;
        PlayerDollItemModelTransform.apply(poseStack, headOffset);
        PlayerDollModel model = getPlayerDollModel();
        model.setupDollAnim(dollPose, 0, 0.0F, 0.0F, 0.0F);
        VertexConsumer buffer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(skinTexture));
        model.renderToBuffer(poseStack, buffer, packedLight, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
    }

    private static PlayerDollItemPose.Offset baseHeadOffset() {
        return scalePixels(PlayerDollItemPose.configOffset(
                SoulboundDollsConfig.WORN_DOLL_HEAD_OFFSET_X::get,
                SoulboundDollsConfig.WORN_DOLL_HEAD_OFFSET_Y::get,
                SoulboundDollsConfig.WORN_DOLL_HEAD_OFFSET_Z::get));
    }

    private static PlayerDollItemPose.Offset poseOffset(DollPose pose) {
        return switch (pose) {
            case SITTING -> scalePixels(PlayerDollItemPose.configOffset(
                    SoulboundDollsConfig.WORN_DOLL_SITTING_OFFSET_X::get,
                    SoulboundDollsConfig.WORN_DOLL_SITTING_OFFSET_Y::get,
                    SoulboundDollsConfig.WORN_DOLL_SITTING_OFFSET_Z::get));
            case STANDING -> scalePixels(PlayerDollItemPose.configOffset(
                    SoulboundDollsConfig.WORN_DOLL_STANDING_OFFSET_X::get,
                    SoulboundDollsConfig.WORN_DOLL_STANDING_OFFSET_Y::get,
                    SoulboundDollsConfig.WORN_DOLL_STANDING_OFFSET_Z::get));
            case CUTE_IDLE -> scalePixels(PlayerDollItemPose.configOffset(
                    SoulboundDollsConfig.WORN_DOLL_CUTE_IDLE_OFFSET_X::get,
                    SoulboundDollsConfig.WORN_DOLL_CUTE_IDLE_OFFSET_Y::get,
                    SoulboundDollsConfig.WORN_DOLL_CUTE_IDLE_OFFSET_Z::get));
        };
    }

    private static PlayerDollItemPose.Offset scalePixels(PlayerDollItemPose.Offset offset) {
        return new PlayerDollItemPose.Offset(offset.x() / 16.0F, offset.y() / 16.0F, offset.z() / 16.0F);
    }

    private static void applyElementRotation(ElementRotation rotation, PoseStack poseStack) {
        if (rotation == null) {
            return;
        }
        poseStack.translate(rotation.origin().x() / 16.0F, rotation.origin().y() / 16.0F, rotation.origin().z() / 16.0F);
        poseStack.mulPose(Axis.XP.rotationDegrees(rotation.degrees().x()));
        poseStack.mulPose(Axis.YP.rotationDegrees(rotation.degrees().y()));
        poseStack.mulPose(Axis.ZP.rotationDegrees(rotation.degrees().z()));
        poseStack.translate(-rotation.origin().x() / 16.0F, -rotation.origin().y() / 16.0F, -rotation.origin().z() / 16.0F);
    }

    private static void renderElement(TemplateModel templateModel, TemplateElement element, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        for (Map.Entry<Face, TemplateFace> entry : element.faces().entrySet()) {
            Face face = entry.getKey();
            TemplateFace templateFace = entry.getValue();
            ResourceLocation spriteLocation = templateModel.textures().get(templateFace.texture());
            if (spriteLocation != null) {
                TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(spriteLocation);
                VertexConsumer buffer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(TextureAtlas.LOCATION_BLOCKS));
                renderSpriteFace(element.from(), element.to(), face, templateFace.uv(), sprite, poseStack.last(), buffer, packedLight);
            }
        }
    }

    private static void renderFace(Vector3 from, Vector3 to, Face face, Uv uv, float textureSize, PoseStack.Pose pose, VertexConsumer buffer, int packedLight) {
        Vector3f[] vertices = faceVertices(from, to, face);
        Vector3f normal = pose.transformNormal(face.normalX(), face.normalY(), face.normalZ(), new Vector3f());
        addVertex(pose, buffer, vertices[0], uv.u2(), uv.v1(), textureSize, packedLight, normal);
        addVertex(pose, buffer, vertices[1], uv.u1(), uv.v1(), textureSize, packedLight, normal);
        addVertex(pose, buffer, vertices[2], uv.u1(), uv.v2(), textureSize, packedLight, normal);
        addVertex(pose, buffer, vertices[3], uv.u2(), uv.v2(), textureSize, packedLight, normal);
    }

    private static void renderSpriteFace(Vector3 from, Vector3 to, Face face, Uv uv, TextureAtlasSprite sprite, PoseStack.Pose pose, VertexConsumer buffer, int packedLight) {
        Vector3f[] vertices = faceVertices(from, to, face);
        Vector3f normal = pose.transformNormal(face.normalX(), face.normalY(), face.normalZ(), new Vector3f());
        addVertex(pose, buffer, vertices[0], sprite.getU(TemplateUv.spriteCoordinate(uv.u2())), sprite.getV(TemplateUv.spriteCoordinate(uv.v1())), packedLight, normal);
        addVertex(pose, buffer, vertices[1], sprite.getU(TemplateUv.spriteCoordinate(uv.u1())), sprite.getV(TemplateUv.spriteCoordinate(uv.v1())), packedLight, normal);
        addVertex(pose, buffer, vertices[2], sprite.getU(TemplateUv.spriteCoordinate(uv.u1())), sprite.getV(TemplateUv.spriteCoordinate(uv.v2())), packedLight, normal);
        addVertex(pose, buffer, vertices[3], sprite.getU(TemplateUv.spriteCoordinate(uv.u2())), sprite.getV(TemplateUv.spriteCoordinate(uv.v2())), packedLight, normal);
    }

    private static Vector3f[] faceVertices(Vector3 from, Vector3 to, Face face) {
        float x1 = from.x();
        float y1 = from.y();
        float z1 = from.z();
        float x2 = to.x();
        float y2 = to.y();
        float z2 = to.z();
        return switch (face) {
            case DOWN -> vertices(vertex(x2, y1, z2), vertex(x1, y1, z2), vertex(x1, y1, z1), vertex(x2, y1, z1));
            case UP -> vertices(vertex(x2, y2, z1), vertex(x1, y2, z1), vertex(x1, y2, z2), vertex(x2, y2, z2));
            case WEST -> vertices(vertex(x1, y1, z1), vertex(x1, y1, z2), vertex(x1, y2, z2), vertex(x1, y2, z1));
            case NORTH -> vertices(vertex(x2, y1, z1), vertex(x1, y1, z1), vertex(x1, y2, z1), vertex(x2, y2, z1));
            case EAST -> vertices(vertex(x2, y1, z2), vertex(x2, y1, z1), vertex(x2, y2, z1), vertex(x2, y2, z2));
            case SOUTH -> vertices(vertex(x1, y1, z2), vertex(x2, y1, z2), vertex(x2, y2, z2), vertex(x1, y2, z2));
        };
    }

    private static Vector3f vertex(float x, float y, float z) {
        return new Vector3f(x / 16.0F, y / 16.0F, z / 16.0F);
    }

    private static Vector3f[] vertices(Vector3f first, Vector3f second, Vector3f third, Vector3f fourth) {
        return new Vector3f[]{first, second, third, fourth};
    }

    private static void addVertex(PoseStack.Pose pose, VertexConsumer buffer, Vector3f vertex, float u, float v, float textureSize, int packedLight, Vector3f normal) {
        addVertex(pose, buffer, vertex, u / textureSize, v / textureSize, packedLight, normal);
    }

    private static void addVertex(PoseStack.Pose pose, VertexConsumer buffer, Vector3f vertex, float u, float v, int packedLight, Vector3f normal) {
        Vector3f transformed = pose.pose().transformPosition(vertex.x(), vertex.y(), vertex.z(), new Vector3f());
        buffer.addVertex(transformed.x(), transformed.y(), transformed.z(), WHITE, u, v, OverlayTexture.NO_OVERLAY, packedLight, normal.x(), normal.y(), normal.z());
    }

    private static Vector3 parseVector(JsonObject transform, String key, float defaultX, float defaultY, float defaultZ) {
        JsonElement element = transform.get(key);
        if (element == null) {
            return new Vector3(defaultX, defaultY, defaultZ);
        }
        if (!element.isJsonArray()) {
            throw new IllegalArgumentException("Expected array for " + key);
        }
        JsonArray array = element.getAsJsonArray();
        if (array.size() != 3) {
            throw new IllegalArgumentException("Expected three values for " + key);
        }
        return new Vector3(array.get(0).getAsFloat(), array.get(1).getAsFloat(), array.get(2).getAsFloat());
    }

    private void renderStaticFallback(ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        Minecraft minecraft = Minecraft.getInstance();
        BakedModel fallbackModel = minecraft.getModelManager().getModel(STATIC_MODEL);
        ItemRenderer itemRenderer = minecraft.getItemRenderer();
        RenderType renderType = RenderType.cutout();
        VertexConsumer buffer = bufferSource.getBuffer(renderType);
        poseStack.pushPose();
        applyDisplayTransform(displayContext, poseStack);
        itemRenderer.renderModelLists(fallbackModel, stack, packedLight, packedOverlay, poseStack, buffer);
        poseStack.popPose();
    }

    record Vector3(float x, float y, float z) {
    }

    record TemplateModel(List<TemplateElement> elements, Map<String, ResourceLocation> textures) {
    }

    record TemplateElement(String name, Vector3 from, Vector3 to, ElementRotation rotation, Map<Face, TemplateFace> faces) {
    }

    record TemplateFace(Uv uv, String texture) {
    }

    record ElementRotation(Vector3 degrees, Vector3 origin) {
    }

    record Uv(float u1, float v1, float u2, float v2) {
    }

    enum Face {
        DOWN("down", 0.0F, -1.0F, 0.0F),
        UP("up", 0.0F, 1.0F, 0.0F),
        WEST("west", -1.0F, 0.0F, 0.0F),
        NORTH("north", 0.0F, 0.0F, -1.0F),
        EAST("east", 1.0F, 0.0F, 0.0F),
        SOUTH("south", 0.0F, 0.0F, 1.0F);

        private final String jsonName;
        private final float normalX;
        private final float normalY;
        private final float normalZ;

        Face(String jsonName, float normalX, float normalY, float normalZ) {
            this.jsonName = jsonName;
            this.normalX = normalX;
            this.normalY = normalY;
            this.normalZ = normalZ;
        }

        String jsonName() {
            return jsonName;
        }

        float normalX() {
            return normalX;
        }

        float normalY() {
            return normalY;
        }

        float normalZ() {
            return normalZ;
        }
    }
}
