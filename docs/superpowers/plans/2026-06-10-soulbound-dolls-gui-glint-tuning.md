# Soulbound Dolls GUI Glint Tuning Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Center bound dynamic doll icons in inventory and add a configurable glint for bound player dolls.

**Architecture:** Keep hand transforms and placed entity rendering unchanged. Adjust only the dynamic `GUI` transform in `PlayerDollItemRenderer`, add one common config value in `SoulboundDollsConfig`, and make `PlayerDollItem.isFoil(...)` depend on the config and bound profile data.

**Tech Stack:** Java 21, Minecraft 1.21.1, NeoForge 21.1.x, `ModConfigSpec`, custom `BlockEntityWithoutLevelRenderer`.

---

### Task 1: Center Dynamic GUI Icon

**Files:**
- Modify: `E:\Codes\MC\Soulbound Dolls\platforms\neoforge-1.21.1\src\main\java\com\yabo\soulbounddolls\neoforge\client\PlayerDollItemRenderer.java`

- [ ] **Step 1: Adjust only GUI translation**

Change the `GUI` branch in `applyDisplayTransform(...)` from:

```java
poseStack.translate(displayOffset(0.5F), displayOffset(0.5F), displayOffset(0.5F));
```

to:

```java
poseStack.translate(displayOffset(-1.5F), displayOffset(2.0F), displayOffset(0.5F));
```

Do not alter first-person hand transforms.

- [ ] **Step 2: Compile**

Run: `.\gradlew.bat :platforms:neoforge-1.21.1:compileJava`

Expected: `BUILD SUCCESSFUL`.

### Task 2: Add Configurable Bound Doll Glint

**Files:**
- Modify: `E:\Codes\MC\Soulbound Dolls\platforms\neoforge-1.21.1\src\main\java\com\yabo\soulbounddolls\neoforge\SoulboundDollsConfig.java`
- Modify: `E:\Codes\MC\Soulbound Dolls\platforms\neoforge-1.21.1\src\main\java\com\yabo\soulbounddolls\neoforge\item\PlayerDollItem.java`

- [ ] **Step 1: Add config value**

Add this field to `SoulboundDollsConfig`:

```java
public static final ModConfigSpec.BooleanValue ENABLE_BOUND_DOLL_GLINT;
```

Define it in the static builder block after `ALLOW_PICKUP_BY_ANYONE`:

```java
ENABLE_BOUND_DOLL_GLINT = builder
        .comment("Render enchanted glint on player doll items that are bound to a player profile.")
        .define("enableBoundDollGlint", true);
```

- [ ] **Step 2: Override item foil**

Import `SoulboundDollsConfig` in `PlayerDollItem`, then add:

```java
@Override
public boolean isFoil(ItemStack stack) {
    return SoulboundDollsConfig.ENABLE_BOUND_DOLL_GLINT.get()
            && stack.has(SoulboundDollsComponents.PLAYER_DOLL_PROFILE.get());
}
```

This makes bound dolls glint by default and leaves unbound dolls plain.

- [ ] **Step 3: Compile**

Run: `.\gradlew.bat :platforms:neoforge-1.21.1:compileJava`

Expected: `BUILD SUCCESSFUL`.

### Task 3: Build, Deploy, And Document

**Files:**
- Modify: `E:\Codes\MC\Soulbound Dolls\.trellis\verification-log.md`
- Modify: `E:\Codes\MC\Soulbound Dolls\USER_GUIDE.md`
- Modify: `E:\Codes\MC\Soulbound Dolls\USER_GUIDE_CN.md`

- [ ] **Step 1: Build**

Run: `.\gradlew.bat :platforms:neoforge-1.21.1:build`

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 2: Deploy**

Copy:

```text
E:\Codes\MC\Soulbound Dolls\platforms\neoforge-1.21.1\build\libs\soulbound-dolls-neoforge-1.21.1-0.1.0-dev.jar
```

to:

```text
E:\SteamLibrary\steamapps\common\PCL2\mc_a\.minecraft\versions\Mechanomania\mods\soulbound-dolls-neoforge-1.21.1-0.1.0-dev.jar
```

- [ ] **Step 3: Update docs**

Document `enableBoundDollGlint = true` in English and Chinese guides.

---

## Self-Review

- Spec coverage: GUI centering and configurable bound glint are covered.
- Placeholder scan: no placeholders remain.
- Type consistency: file paths and class names match inspected project files.
