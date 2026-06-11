# Player Doll Item Entity Render Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make bound player doll items render with the same model and skin behavior as placed player doll entities, and name bound items `<playername>的玩偶`.

**Architecture:** Replace the item-only hand-rendered template skin path with the existing `PlayerDollModel` entity model inside `PlayerDollItemRenderer`. Keep JSON `display` transforms as the item placement control plane, but use the entity model for geometry and player skin UVs. Change bound item display naming in `PlayerDollItem` without changing stored profile data.

**Tech Stack:** Java 21, NeoForge 1.21.1, Minecraft client renderer, JUnit 5.

---

### Task 1: Bound Item Name

**Files:**
- Modify: `platforms/neoforge-1.21.1/src/main/java/com/yabo/soulbounddolls/neoforge/item/PlayerDollItem.java`
- Test: `platforms/neoforge-1.21.1/src/test/java/com/yabo/soulbounddolls/neoforge/item/PlayerDollItemTest.java`

- [ ] **Step 1: Write the failing name-format test**

```java
package com.yabo.soulbounddolls.neoforge.item;

import com.yabo.soulbounddolls.common.PlayerDollProfile;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlayerDollItemTest {
    @Test
    void boundDollNameUsesPlayerName() {
        PlayerDollProfile profile = PlayerDollProfile.of(
                UUID.fromString("00000000-0000-0000-0000-000000000123"),
                "Steve",
                "skin-value",
                "skin-signature",
                false,
                1L);

        assertEquals("Steve的玩偶", PlayerDollItem.boundDollName(profile).getString());
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew :platforms:neoforge-1.21.1:test --tests com.yabo.soulbounddolls.neoforge.item.PlayerDollItemTest.boundDollNameUsesPlayerName`

Expected: FAIL because `PlayerDollItem.boundDollName` does not exist.

- [ ] **Step 3: Add the name helper and use it from `getName`**

```java
static Component boundDollName(PlayerDollProfile profile) {
    return Component.literal(profile.name() + "的玩偶");
}

@Override
public Component getName(ItemStack stack) {
    PlayerDollProfile profile = stack.get(SoulboundDollsComponents.PLAYER_DOLL_PROFILE.get());
    if (profile == null) {
        return Component.translatable("item.soulbound_dolls.player_doll.unbound_name");
    }
    return boundDollName(profile);
}
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `./gradlew :platforms:neoforge-1.21.1:test --tests com.yabo.soulbounddolls.neoforge.item.PlayerDollItemTest.boundDollNameUsesPlayerName`

Expected: PASS.

### Task 2: Item Renderer Uses Entity Model

**Files:**
- Modify: `platforms/neoforge-1.21.1/src/main/java/com/yabo/soulbounddolls/neoforge/client/PlayerDollItemRenderer.java`
- Create: `platforms/neoforge-1.21.1/src/main/java/com/yabo/soulbounddolls/neoforge/client/PlayerDollItemRenderStrategy.java`
- Create: `platforms/neoforge-1.21.1/src/main/java/com/yabo/soulbounddolls/neoforge/client/PlayerDollItemModelTransform.java`
- Modify: `platforms/neoforge-1.21.1/src/test/java/com/yabo/soulbounddolls/neoforge/client/PlayerDollItemRendererTest.java`

- [ ] **Step 1: Write the failing render-strategy test**

```java
@Test
void boundItemsUseEntityModelRenderStrategy() {
    assertEquals(PlayerDollItemRenderStrategy.ENTITY_MODEL, PlayerDollItemRenderStrategy.forBoundProfile(true));
    assertEquals(PlayerDollItemRenderStrategy.TEMPLATE_MODEL, PlayerDollItemRenderStrategy.forBoundProfile(false));
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew :platforms:neoforge-1.21.1:test --tests com.yabo.soulbounddolls.neoforge.client.PlayerDollItemRendererTest.boundItemsUseEntityModelRenderStrategy`

Expected: FAIL because `PlayerDollItemRenderStrategy` does not exist.

- [ ] **Step 3: Add strategy seam and entity model fields**

Create `PlayerDollItemRenderStrategy.java`:

```java
package com.yabo.soulbounddolls.neoforge.client;

enum PlayerDollItemRenderStrategy {
    ENTITY_MODEL,
    TEMPLATE_MODEL;

    static PlayerDollItemRenderStrategy forBoundProfile(boolean boundToPlayer) {
        return boundToPlayer ? ENTITY_MODEL : TEMPLATE_MODEL;
    }
}
```

Add to `PlayerDollItemRenderer`:

```java
private final EntityModelSet entityModelSet;
private PlayerDollModel playerDollModel;

public PlayerDollItemRenderer(BlockEntityRenderDispatcher blockEntityRenderDispatcher, EntityModelSet entityModelSet) {
    super(blockEntityRenderDispatcher, entityModelSet);
    this.entityModelSet = entityModelSet;
}
```

- [ ] **Step 4: Render bound items through `PlayerDollModel`**

Add a transform helper that maps `PlayerDollModel` local entity-model coordinates into item model `0..1` space without adding the entity world yaw:

```java
package com.yabo.soulbounddolls.neoforge.client;

import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Vector3f;

final class PlayerDollItemModelTransform {
    private static final float SCALE = 0.62F;
    private static final Vector3f MODEL_CENTER = new Vector3f(0.0F, 0.4921875F, -0.075F);

    private PlayerDollItemModelTransform() {
    }

    static void apply(PoseStack poseStack) {
        poseStack.translate(0.5F + MODEL_CENTER.x() * SCALE, 0.5F + MODEL_CENTER.y() * SCALE, 0.5F - MODEL_CENTER.z() * SCALE);
        poseStack.scale(-SCALE, -SCALE, SCALE);
    }
}
```

Add a lazy model getter and render path:

```java
private PlayerDollModel getPlayerDollModel() {
    if (playerDollModel == null) {
        playerDollModel = new PlayerDollModel(entityModelSet.bakeLayer(PlayerDollModel.LAYER_LOCATION));
    }
    return playerDollModel;
}

private void renderEntityModel(ResourceLocation skinTexture, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
    poseStack.pushPose();
    PlayerDollItemModelTransform.apply(poseStack);
    PlayerDollModel model = getPlayerDollModel();
    model.setupDollAnim(PlayerDollEntity.DollPose.STANDING, 0, 0.0F, 0.0F, 0.0F);
    VertexConsumer buffer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(skinTexture));
    model.renderToBuffer(poseStack, buffer, packedLight, OverlayTexture.NO_OVERLAY);
    poseStack.popPose();
}
```

In `renderByItem`, after applying display transform, if `renderStrategy(profile != null && profile.hasSkin()) == ENTITY_MODEL`, resolve skin and call `renderEntityModel`; otherwise keep the existing template path for unbound/default dolls.

- [ ] **Step 5: Run the focused renderer tests**

Run: `./gradlew :platforms:neoforge-1.21.1:test --tests com.yabo.soulbounddolls.neoforge.client.PlayerDollItemRendererTest`

Expected: PASS.

### Task 3: Verify and Copy Test Jar


**Files:**
- No additional source files.

- [ ] **Step 1: Run full platform build**

Run: `./gradlew :platforms:neoforge-1.21.1:build`

Expected: BUILD SUCCESSFUL.

- [ ] **Step 2: Copy jar to configured test mods directory**

Run: `./gradlew :platforms:neoforge-1.21.1:copyJarToTest`

Expected: BUILD SUCCESSFUL and a `Copied soulbound-dolls-neoforge-1.21.1-...jar` message.

---

## Self-Review

- Spec coverage: item rendering uses placed entity model for bound dolls; item naming uses `<playername>的玩偶`; JSON display remains the placement control plane.
- Placeholder scan: no TBD/TODO placeholders.
- Type consistency: `PlayerDollItem.boundDollName` and `PlayerDollItemRenderStrategy.forBoundProfile` are defined before use.
