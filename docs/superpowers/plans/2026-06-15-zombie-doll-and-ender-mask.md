# Zombie Doll And Ender Mask Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement zombie doll stealing/carrying/drop behavior and Enderman look protection from worn dolls without making Jade, Curios, or any other integration mod mandatory.

**Architecture:** Runtime events stay thin and delegate testable decisions to small helpers. Zombie carrying uses visible equipment slots first, then persistent entity NBT for hidden carried dolls. Curios support is isolated behind an optional compat class that is only called after `ModList.get().isLoaded("curios")`, while Jade remains a separate optional client plugin.

**Tech Stack:** NeoForge 1.21.1, Java 21, JUnit 5, optional `compileOnly` APIs for Jade and Curios only, NeoForge `EnderManAngerEvent`, `LivingDropsEvent`, `EntityMobGriefingEvent`, and `EntityJoinLevelEvent`.

---

## File Structure

- Modify: `platforms/neoforge-1.21.1/build.gradle` to add a `compileOnly` Curios API dependency only if a resolvable coordinate is confirmed; do not add `implementation`, `runtimeOnly`, or bundled dependencies.
- Modify: `platforms/neoforge-1.21.1/src/main/resources/META-INF/neoforge.mods.toml` to keep Jade optional and add Curios metadata only as `type = "optional"`, never `required`.
- Create: `platforms/neoforge-1.21.1/src/main/java/com/yabo/soulbounddolls/neoforge/item/DollStackHelper.java` for identifying bound player doll stacks.
- Create: `platforms/neoforge-1.21.1/src/main/java/com/yabo/soulbounddolls/neoforge/entity/ZombieDollCarryHelper.java` for cap checks, slot selection, persistent hidden doll storage, sunburn checks, and death-drop extraction.
- Create: `platforms/neoforge-1.21.1/src/main/java/com/yabo/soulbounddolls/neoforge/entity/ZombieMoveToDollGoal.java` for pathfinding to placed dolls and dropped doll items.
- Create: `platforms/neoforge-1.21.1/src/main/java/com/yabo/soulbounddolls/neoforge/compat/curios/CuriosDollLookup.java` for all Curios imports and slot scanning.
- Create: `platforms/neoforge-1.21.1/src/main/java/com/yabo/soulbounddolls/neoforge/entity/EnderMaskHelper.java` for head-slot and optional Curios protection checks.
- Modify: `platforms/neoforge-1.21.1/src/main/java/com/yabo/soulbounddolls/neoforge/SoulboundDollsRuntimeEvents.java` to wire zombie AI, sunlight immunity, death drops, and Enderman anger cancellation.
- Modify: `platforms/neoforge-1.21.1/src/main/java/com/yabo/soulbounddolls/neoforge/item/PlayerDollItem.java` to add the Enderman protection tooltip when enabled.
- Modify: `platforms/neoforge-1.21.1/src/main/java/com/yabo/soulbounddolls/neoforge/SoulboundDollsConfig.java` to add an Ender mask feature toggle if one does not already exist.
- Modify: `platforms/neoforge-1.21.1/src/main/resources/assets/soulbound_dolls/lang/en_us.json` and `platforms/neoforge-1.21.1/src/main/resources/assets/soulbound_dolls/lang/zh_cn.json` for tooltip translations.
- Add tests under `platforms/neoforge-1.21.1/src/test/java/com/yabo/soulbounddolls/neoforge/` for pure helpers and optional dependency policy.

## Dependency Policy

- [ ] Keep Jade as `compileOnly "maven.modrinth:jade:15.10.5+neoforge"` and `type = "optional"` in `neoforge.mods.toml`.
- [ ] Add Curios only as `compileOnly`, using a coordinate verified from the local or remote API source.
- [ ] Add Curios metadata only with `type = "optional"`, `ordering = "AFTER"`, and `side = "BOTH"` or the narrowest side that works.
- [ ] Keep every Curios import inside `com.yabo.soulbounddolls.neoforge.compat.curios`.
- [ ] Ensure non-compat classes reference Curios only through `ModList.get().isLoaded("curios")` and `CuriosDollLookup`.
- [ ] Do not add `implementation`, `runtimeOnly`, `jarJar`, bundled jars, required dependency metadata, or direct hard loads for Jade, Curios, or any other third-party mod.

### Task 1: Add Doll Stack Recognition Helper

**Files:**
- Create: `platforms/neoforge-1.21.1/src/main/java/com/yabo/soulbounddolls/neoforge/item/DollStackHelper.java`
- Test: `platforms/neoforge-1.21.1/src/test/java/com/yabo/soulbounddolls/neoforge/item/DollStackHelperTest.java`

- [ ] **Step 1: Write failing tests for bound doll detection**

```java
@Test
void recognizesBoundPlayerDollStack() {
    PlayerDollProfile profile = new PlayerDollProfile(UUID.randomUUID(), "Alex", Optional.empty());
    ItemStack stack = PlayerDollItem.createBoundDoll(profile);

    assertTrue(DollStackHelper.isBoundPlayerDoll(stack));
}

@Test
void rejectsUnboundOrUnrelatedStacks() {
    assertFalse(DollStackHelper.isBoundPlayerDoll(new ItemStack(SoulboundDollsItems.PLAYER_DOLL.get())));
    assertFalse(DollStackHelper.isBoundPlayerDoll(new ItemStack(Items.STONE)));
    assertFalse(DollStackHelper.isBoundPlayerDoll(ItemStack.EMPTY));
}
```

- [ ] **Step 2: Run helper test and verify it fails**

Run: `./gradlew :platforms:neoforge-1.21.1:test --tests "com.yabo.soulbounddolls.neoforge.item.DollStackHelperTest"`
Expected: FAIL because `DollStackHelper` does not exist.

- [ ] **Step 3: Implement minimal helper**

```java
package com.yabo.soulbounddolls.neoforge.item;

import com.yabo.soulbounddolls.neoforge.SoulboundDollsComponents;
import net.minecraft.world.item.ItemStack;

public final class DollStackHelper {
    private DollStackHelper() {
    }

    public static boolean isBoundPlayerDoll(ItemStack stack) {
        return !stack.isEmpty() && stack.get(SoulboundDollsComponents.PLAYER_DOLL_PROFILE.get()) != null;
    }
}
```

- [ ] **Step 4: Run helper test and verify it passes**

Run: `./gradlew :platforms:neoforge-1.21.1:test --tests "com.yabo.soulbounddolls.neoforge.item.DollStackHelperTest"`
Expected: PASS.

### Task 2: Add Zombie Carry Accounting Helper

**Files:**
- Create: `platforms/neoforge-1.21.1/src/main/java/com/yabo/soulbounddolls/neoforge/entity/ZombieDollCarryHelper.java`
- Test: `platforms/neoforge-1.21.1/src/test/java/com/yabo/soulbounddolls/neoforge/entity/ZombieDollCarryHelperTest.java`

- [ ] **Step 1: Write tests for visible slot selection and cap**

```java
@Test
void choosesFirstEmptyVisibleSlotWithoutReplacingGear() {
    assertEquals(EquipmentSlot.HEAD, ZombieDollCarryHelper.firstEmptyVisibleSlot(false, false, false).orElseThrow());
    assertEquals(EquipmentSlot.MAINHAND, ZombieDollCarryHelper.firstEmptyVisibleSlot(true, false, false).orElseThrow());
    assertEquals(EquipmentSlot.OFFHAND, ZombieDollCarryHelper.firstEmptyVisibleSlot(true, true, false).orElseThrow());
    assertTrue(ZombieDollCarryHelper.firstEmptyVisibleSlot(true, true, true).isEmpty());
}

@Test
void respectsTotalCarryCapOfThree() {
    assertTrue(ZombieDollCarryHelper.hasRoom(0));
    assertTrue(ZombieDollCarryHelper.hasRoom(2));
    assertFalse(ZombieDollCarryHelper.hasRoom(3));
}
```

- [ ] **Step 2: Run helper test and verify it fails**

Run: `./gradlew :platforms:neoforge-1.21.1:test --tests "com.yabo.soulbounddolls.neoforge.entity.ZombieDollCarryHelperTest"`
Expected: FAIL because `ZombieDollCarryHelper` does not exist.

- [ ] **Step 3: Implement pure slot and cap methods**

```java
public static final int MAX_CARRIED_DOLLS = 3;

public static boolean hasRoom(int carriedCount) {
    return carriedCount < MAX_CARRIED_DOLLS;
}

public static Optional<EquipmentSlot> firstEmptyVisibleSlot(boolean headOccupied, boolean mainHandOccupied, boolean offhandOccupied) {
    if (!headOccupied) {
        return Optional.of(EquipmentSlot.HEAD);
    }
    if (!mainHandOccupied) {
        return Optional.of(EquipmentSlot.MAINHAND);
    }
    if (!offhandOccupied) {
        return Optional.of(EquipmentSlot.OFFHAND);
    }
    return Optional.empty();
}
```

- [ ] **Step 4: Run helper test and verify it passes**

Run: `./gradlew :platforms:neoforge-1.21.1:test --tests "com.yabo.soulbounddolls.neoforge.entity.ZombieDollCarryHelperTest"`
Expected: PASS.

### Task 3: Implement Zombie Runtime Carry And Drop Behavior

**Files:**
- Modify: `platforms/neoforge-1.21.1/src/main/java/com/yabo/soulbounddolls/neoforge/entity/ZombieDollCarryHelper.java`
- Modify: `platforms/neoforge-1.21.1/src/main/java/com/yabo/soulbounddolls/neoforge/SoulboundDollsRuntimeEvents.java`
- Test: `platforms/neoforge-1.21.1/src/test/java/com/yabo/soulbounddolls/neoforge/entity/ZombieDollCarryHelperTest.java`

- [ ] **Step 1: Add tests for hidden stack serialization boundaries**

```java
@Test
void hiddenCarryListNeverExceedsRemainingCap() {
    List<ItemStack> stored = List.of(boundDoll("A"), boundDoll("B"), boundDoll("C"));

    assertEquals(0, ZombieDollCarryHelper.hiddenCapacity(3));
    assertEquals(1, ZombieDollCarryHelper.hiddenCapacity(2));
    assertEquals(3, ZombieDollCarryHelper.hiddenCapacity(0));
    assertEquals(3, ZombieDollCarryHelper.trimHiddenStoredDolls(stored, 0).size());
    assertEquals(1, ZombieDollCarryHelper.trimHiddenStoredDolls(stored, 2).size());
}
```

- [ ] **Step 2: Run focused tests and verify failure**

Run: `./gradlew :platforms:neoforge-1.21.1:test --tests "com.yabo.soulbounddolls.neoforge.entity.ZombieDollCarryHelperTest"`
Expected: FAIL until hidden capacity helpers exist.

- [ ] **Step 3: Implement runtime carry methods**

Add methods that count visible bound dolls in `EquipmentSlot.HEAD`, `EquipmentSlot.MAINHAND`, and `EquipmentSlot.OFFHAND`; equip incoming doll stacks only into empty visible slots; put overflow into `zombie.getPersistentData()` under a namespaced key; and set visible drop chances to `1.0F` for slots this helper equips.

- [ ] **Step 4: Wire guaranteed death drops**

Subscribe to `LivingDropsEvent`. If the entity is a `Zombie`, append every hidden stored doll stack to the drops and clear hidden storage. Visible carried dolls rely on the helper's guaranteed drop chance.

- [ ] **Step 5: Wire sunlight immunity**

Subscribe to `EntityMobGriefingEvent` or the narrowest available NeoForge event for sun burn prevention. If the entity is a `Zombie` carrying at least one bound doll, cancel only the sunlight burn behavior; do not grant general damage immunity.

- [ ] **Step 6: Run focused tests**

Run: `./gradlew :platforms:neoforge-1.21.1:test --tests "com.yabo.soulbounddolls.neoforge.entity.ZombieDollCarryHelperTest"`
Expected: PASS.

### Task 4: Add Zombie Goal For Placed And Dropped Dolls

**Files:**
- Create: `platforms/neoforge-1.21.1/src/main/java/com/yabo/soulbounddolls/neoforge/entity/ZombieMoveToDollGoal.java`
- Modify: `platforms/neoforge-1.21.1/src/main/java/com/yabo/soulbounddolls/neoforge/SoulboundDollsRuntimeEvents.java`

- [ ] **Step 1: Replace generic undead goal registration with zombie-specific registration**

Register `ZombieMoveToDollGoal` only when `event.getEntity() instanceof Zombie zombie`, server side, and `ENABLE_ATTRACT_UNDEAD` is true.

- [ ] **Step 2: Implement nearest target search**

Search within `SoulboundDollsConfig.ATTRACT_UNDEAD_RANGE.get()` for both `PlayerDollEntity` and `ItemEntity` whose stack is a bound player doll. Ignore targets when `ZombieDollCarryHelper.hasRoom(zombie)` is false.

- [ ] **Step 3: Implement placed doll conversion**

When close enough to a `PlayerDollEntity`, create the same bound doll stack players receive, call `ZombieDollCarryHelper.tryCarry(zombie, stack)`, and discard the entity only if carry succeeds.

- [ ] **Step 4: Implement dropped item consumption**

When close enough to a matching `ItemEntity`, split one item from the item stack, carry it, and discard the item entity only when its stack becomes empty.

- [ ] **Step 5: Keep AI event-driven**

Do not add global per-tick scans. All repeated target checks stay inside the vanilla goal lifecycle.

### Task 5: Add Optional Curios Ender Mask Lookup

**Files:**
- Modify: `platforms/neoforge-1.21.1/build.gradle`
- Modify: `platforms/neoforge-1.21.1/src/main/resources/META-INF/neoforge.mods.toml`
- Create: `platforms/neoforge-1.21.1/src/main/java/com/yabo/soulbounddolls/neoforge/compat/curios/CuriosDollLookup.java`
- Create: `platforms/neoforge-1.21.1/src/main/java/com/yabo/soulbounddolls/neoforge/entity/EnderMaskHelper.java`
- Test: `platforms/neoforge-1.21.1/src/test/java/com/yabo/soulbounddolls/neoforge/entity/EnderMaskHelperTest.java`

- [ ] **Step 1: Write tests for optional lookup injection**

```java
@Test
void headSlotDollProtectsWithoutCurios() {
    assertTrue(EnderMaskHelper.isProtected(true, () -> false));
}

@Test
void curiosCanProtectWhenHeadSlotDoesNot() {
    assertTrue(EnderMaskHelper.isProtected(false, () -> true));
}

@Test
void noDollDoesNotProtect() {
    assertFalse(EnderMaskHelper.isProtected(false, () -> false));
}
```

- [ ] **Step 2: Run helper tests and verify failure**

Run: `./gradlew :platforms:neoforge-1.21.1:test --tests "com.yabo.soulbounddolls.neoforge.entity.EnderMaskHelperTest"`
Expected: FAIL because helper does not exist.

- [ ] **Step 3: Add Curios only as optional dependency**

Add only `compileOnly` for Curios. If adding metadata, use only:

```toml
[[dependencies.${mod_id}]]
modId = "curios"
type = "optional"
ordering = "AFTER"
side = "BOTH"
```

- [ ] **Step 4: Implement `EnderMaskHelper` without Curios imports**

`EnderMaskHelper` should check the head equipment slot for `DollStackHelper.isBoundPlayerDoll(stack)`, then call the supplied Curios lookup only if the head check failed.

- [ ] **Step 5: Implement `CuriosDollLookup` as the only Curios import boundary**

Use `ModList.get().isLoaded("curios")` before calling Curios APIs. Return `false` immediately when Curios is absent. Keep all `top.theillusivec4.curios` imports in this class only.

- [ ] **Step 6: Run helper tests**

Run: `./gradlew :platforms:neoforge-1.21.1:test --tests "com.yabo.soulbounddolls.neoforge.entity.EnderMaskHelperTest"`
Expected: PASS.

### Task 6: Wire Enderman Anger Cancellation And Tooltip

**Files:**
- Modify: `platforms/neoforge-1.21.1/src/main/java/com/yabo/soulbounddolls/neoforge/SoulboundDollsRuntimeEvents.java`
- Modify: `platforms/neoforge-1.21.1/src/main/java/com/yabo/soulbounddolls/neoforge/SoulboundDollsConfig.java`
- Modify: `platforms/neoforge-1.21.1/src/main/java/com/yabo/soulbounddolls/neoforge/item/PlayerDollItem.java`
- Modify: `platforms/neoforge-1.21.1/src/main/resources/assets/soulbound_dolls/lang/en_us.json`
- Modify: `platforms/neoforge-1.21.1/src/main/resources/assets/soulbound_dolls/lang/zh_cn.json`

- [ ] **Step 1: Add config toggle**

Add `ENABLE_ENDER_MASK_PROTECTION`, default `true`, with a comment that worn dolls prevent look-based Enderman anger but not retaliation after attacks.

- [ ] **Step 2: Subscribe to `EnderManAngerEvent`**

If the toggle is enabled and `EnderMaskHelper.isProtected(player, () -> CuriosDollLookup.hasEquippedDoll(player))` returns true, cancel the event. Do not cancel `LivingChangeTargetEvent` for Endermen.

- [ ] **Step 3: Add tooltip translation key**

Add `item.soulbound_dolls.player_doll.ender_mask` to English and Chinese language files. The Chinese value must be exactly `我很可爱，请不要生气~`.

- [ ] **Step 4: Append tooltip only when feature is enabled**

In `PlayerDollItem`, append the translatable Ender mask line when `ENABLE_ENDER_MASK_PROTECTION` is true.

### Task 7: Add Optional Dependency Policy Tests

**Files:**
- Create: `platforms/neoforge-1.21.1/src/test/java/com/yabo/soulbounddolls/neoforge/DependencyPolicyTest.java`

- [ ] **Step 1: Write tests that reject hard third-party mod dependencies**

```java
@Test
void thirdPartyModDependenciesAreNotRequired() throws IOException {
    String modsToml = Files.readString(Path.of("src/main/resources/META-INF/neoforge.mods.toml"));

    assertFalse(modsToml.contains("modId = \"jade\"\n type = \"required\""));
    assertFalse(modsToml.contains("modId = \"curios\"\n type = \"required\""));
    assertTrue(modsToml.contains("modId = \"jade\""));
    assertTrue(modsToml.contains("type = \"optional\""));
}

@Test
void buildDoesNotBundleOptionalIntegrationMods() throws IOException {
    String buildGradle = Files.readString(Path.of("build.gradle"));

    assertFalse(buildGradle.contains("implementation \"maven.modrinth:jade"));
    assertFalse(buildGradle.contains("runtimeOnly \"maven.modrinth:jade"));
    assertFalse(buildGradle.contains("implementation \"top.theillusivec4.curios"));
    assertFalse(buildGradle.contains("runtimeOnly \"top.theillusivec4.curios"));
}
```

- [ ] **Step 2: Run dependency policy test**

Run: `./gradlew :platforms:neoforge-1.21.1:test --tests "com.yabo.soulbounddolls.neoforge.DependencyPolicyTest"`
Expected: PASS after optional dependency metadata and Gradle changes are correct.

### Task 8: Full Verification

**Files:**
- No code files.

- [ ] Run `./gradlew test`.
- [ ] Run `./gradlew build`.
- [ ] Run `./gradlew :platforms:neoforge-1.21.1:copyJarToTest`.
- [ ] Verify the built mod starts in a test instance without Curios installed.
- [ ] Verify Jade tooltip still works only when Jade is installed.
- [ ] Verify Curios Ender mask behavior only when Curios is installed.
- [ ] Report exact commands and outcomes.

## Manual Test Instructions

- [ ] Launch without Jade and without Curios; confirm the mod loads and dolls still work.
- [ ] Launch with Jade only; confirm doll HUD text appears and Curios is not required.
- [ ] Launch with Curios only; equip a doll in a Curios slot and confirm looking at Endermen does not anger them.
- [ ] Launch with neither integration mod; wear a doll in the head slot and confirm looking at Endermen does not anger them.
- [ ] Attack an Enderman while protected and confirm normal retaliation still happens.
- [ ] Spawn a zombie with occupied head/mainhand/offhand slots; confirm it does not overwrite gear.
- [ ] Spawn a zombie near placed and dropped bound dolls; confirm it carries up to three and drops all carried dolls on death.

## Self-Review

- Spec coverage: Zombie carry cap, no equipment overwrite, placed and dropped doll stealing, guaranteed drops, sunlight immunity, Enderman look protection, active retaliation preservation, tooltip text, and optional Curios/Jade policy are all mapped to tasks.
- Placeholder scan: No `TBD`, `TODO`, or unspecified integration behavior remains.
- Type consistency: Helper names are consistent across tests and wiring tasks.
- Optional dependency check: Plan explicitly rejects required third-party mod dependencies and adds a policy test for Gradle and `neoforge.mods.toml`.
