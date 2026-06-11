# Soulbound Dolls Item Display And Interactions Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make player doll items readable in inventory/hand display, add common entity affordances, and allow sane stacking without changing placed entity size.

**Architecture:** Keep placed entity rendering unchanged. Add per-`ItemDisplayContext` transforms inside `PlayerDollItemRenderer`, implement pick/attack affordances in `PlayerDollEntity`, and change only item max stack size in `SoulboundDollsItems`.

**Tech Stack:** Java 21, Minecraft 1.21.1, NeoForge 21.1.x, custom `BlockEntityWithoutLevelRenderer`.

---

### Task 1: Dynamic Item Display Transforms

**Files:**
- Modify: `E:\Codes\MC\Soulbound Dolls\platforms\neoforge-1.21.1\src\main\java\com\yabo\soulbounddolls\neoforge\client\PlayerDollItemRenderer.java`

- [ ] **Step 1: Replace the single transform with display-context-specific transforms**

Add a private helper used by `renderByItem` before `model.setupDollAnim(...)`:

```java
private static void applyDisplayTransform(ItemDisplayContext displayContext, PoseStack poseStack) {
    switch (displayContext) {
        case GUI -> {
            poseStack.translate(0.5F, 1.22F, 0.5F);
            poseStack.scale(0.078F, -0.078F, 0.078F);
        }
        case FIRST_PERSON_LEFT_HAND, FIRST_PERSON_RIGHT_HAND -> {
            poseStack.translate(0.5F, 1.18F, 0.5F);
            poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(8.0F));
            poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(displayContext == ItemDisplayContext.FIRST_PERSON_LEFT_HAND ? -22.0F : 22.0F));
            poseStack.scale(0.072F, -0.072F, 0.072F);
        }
        case THIRD_PERSON_LEFT_HAND, THIRD_PERSON_RIGHT_HAND -> {
            poseStack.translate(0.5F, 1.12F, 0.5F);
            poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(displayContext == ItemDisplayContext.THIRD_PERSON_LEFT_HAND ? -35.0F : 35.0F));
            poseStack.scale(0.064F, -0.064F, 0.064F);
        }
        case GROUND -> {
            poseStack.translate(0.5F, 0.88F, 0.5F);
            poseStack.scale(0.052F, -0.052F, 0.052F);
        }
        case FIXED -> {
            poseStack.translate(0.5F, 1.1F, 0.5F);
            poseStack.scale(0.064F, -0.064F, 0.064F);
        }
        default -> {
            poseStack.translate(0.5F, 1.08F, 0.5F);
            poseStack.scale(0.064F, -0.064F, 0.064F);
        }
    }
}
```

- [ ] **Step 2: Use the helper**

Replace:

```java
poseStack.translate(0.5F, 1.08F, 0.5F);
poseStack.scale(0.047F, -0.047F, 0.047F);
```

with:

```java
applyDisplayTransform(displayContext, poseStack);
```

- [ ] **Step 3: Compile**

Run: `.\gradlew.bat :platforms:neoforge-1.21.1:compileJava`

Expected: `BUILD SUCCESSFUL`.

### Task 2: Entity Pick And Left-Click Affordances

**Files:**
- Modify: `E:\Codes\MC\Soulbound Dolls\platforms\neoforge-1.21.1\src\main\java\com\yabo\soulbounddolls\neoforge\entity\PlayerDollEntity.java`

- [ ] **Step 1: Add middle-click pick result**

Add override:

```java
@Override
public ItemStack getPickResult() {
    return PlayerDollItem.createBoundDoll(getProfile());
}
```

- [ ] **Step 2: Add left-click behavior without damaging the doll**

Add override:

```java
@Override
public boolean skipAttackInteraction(Entity attacker) {
    if (attacker instanceof Player player && player.isShiftKeyDown()) {
        tryPickup(player);
        return true;
    }
    playShakeFeedback();
    return true;
}
```

This makes regular left-click shake the doll and shift-left-click pick it up using the existing permission logic.

- [ ] **Step 3: Compile**

Run: `.\gradlew.bat :platforms:neoforge-1.21.1:compileJava`

Expected: `BUILD SUCCESSFUL`.

### Task 3: Stackable Player Doll Item

**Files:**
- Modify: `E:\Codes\MC\Soulbound Dolls\platforms\neoforge-1.21.1\src\main\java\com\yabo\soulbounddolls\neoforge\SoulboundDollsItems.java`

- [ ] **Step 1: Increase stack size**

Change:

```java
() -> new PlayerDollItem(new Item.Properties().stacksTo(1))
```

to:

```java
() -> new PlayerDollItem(new Item.Properties().stacksTo(16))
```

Data components naturally prevent different bound profiles from mixing in the same stack.

- [ ] **Step 2: Build full platform jar**

Run: `.\gradlew.bat :platforms:neoforge-1.21.1:build`

Expected: `BUILD SUCCESSFUL`.

### Task 4: Deployment And Verification Notes

**Files:**
- Modify: `E:\Codes\MC\Soulbound Dolls\.trellis\verification-log.md`

- [ ] **Step 1: Deploy jar**

Copy:

```text
E:\Codes\MC\Soulbound Dolls\platforms\neoforge-1.21.1\build\libs\soulbound-dolls-neoforge-1.21.1-0.1.0-dev.jar
```

to:

```text
E:\SteamLibrary\steamapps\common\PCL2\mc_a\.minecraft\versions\Mechanomania\mods\soulbound-dolls-neoforge-1.21.1-0.1.0-dev.jar
```

- [ ] **Step 2: Record verification**

Append an entry describing item transform tuning, pick result, left-click interaction, stack size, build result, and deployed jar timestamp.

Manual in-game checks after deployment:

```text
1. Bound Player Doll is larger and readable in inventory GUI.
2. Bound Player Doll is larger when held in first person and third person.
3. Placed entity size is unchanged.
4. Middle-clicking a placed doll gives a bound Player Doll item.
5. Left-clicking a placed doll shakes it instead of damaging/removing it.
6. Shift-left-clicking a placed doll picks it up when permissions allow.
7. Identical Player Doll stacks can stack up to 16; different bound profiles do not merge.
```

---

## Self-Review

- Spec coverage: item display, middle-click copy, left-click interaction, and stacking are covered. Placed entity size is explicitly unchanged.
- Placeholder scan: no placeholders remain.
- Type consistency: paths and class names match the existing project files inspected before this plan.
