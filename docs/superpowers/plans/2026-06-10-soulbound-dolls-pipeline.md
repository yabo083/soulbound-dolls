# Soulbound Dolls Implementation Pipeline

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a standalone NeoForge 1.21.1 mod named `Soulbound Dolls` that automatically records joined players, creates player-bound dolls, renders player skins, and supports placement, pickup, wearing, interaction, commands, recipes, and configuration.

**Architecture:** Use the local `Mirrors Glide` project as the Gradle/NeoForge 1.21.1 template, but create a separate root project at `E:\Codes\MC\Soulbound Dolls`. Keep pure data/serialization in `common`, and NeoForge registrations, events, commands, entities, renderers, resources, and runtime integration under `platforms/neoforge-1.21.1`. Use Trellis-style artifacts to make agent work reproducible: PRD, spec, task packets, codegraph notes, verification log, and review notes.

**Tech Stack:** Java 21, Minecraft 1.21.1, NeoForge `21.1.18`, ModDevGradle `2.0.95`, JUnit for common tests, CodeGraph for structural navigation, Superpowers for planning/execution discipline, OpenCode subagents for independent task packets.

---

## Current Context

- Historical session ID: `019eb074-9cfd-76e1-8973-ef4637563739`.
- Original requested mod: NeoForge 1.21.1 player doll mod inspired by the doll behavior in `KaleidoscopeMods/KaleidoscopeDeco`, without copying assets.
- Reference finding: KaleidoscopeDeco doll behavior is primarily datapack/resourcepack driven. The useful behavior targets are doll machine/random acquisition, placement, pickup, pat/note feedback, shake animation, wanderer trade, and plus-pack interactions such as eat/explode/throw-style events.
- Local template finding: `E:\Codes\MC\Mirrors Glide` is a working NeoForge 1.21.1 multi-project template with `common` plus `platforms/neoforge-1.21.1`.
- CodeGraph status in this environment: no `.codegraph` directory exists under `E:\Codes\MC`, and no `codegraph_*` tool is exposed in the current OpenCode tool list. The pipeline therefore makes CodeGraph initialization a required gate before implementation agents start editing.

## Trellis Artifacts To Create

- Create: `Soulbound Dolls/.trellis/prd.md` - product requirements, non-goals, user stories, and acceptance criteria.
- Create: `Soulbound Dolls/.trellis/spec.md` - technical design, file ownership, APIs, data formats, and network/client boundaries.
- Create: `Soulbound Dolls/.trellis/tasks/001-bootstrap.md` - Gradle project creation packet.
- Create: `Soulbound Dolls/.trellis/tasks/002-domain-data.md` - common data model and tests packet.
- Create: `Soulbound Dolls/.trellis/tasks/003-registries-items.md` - NeoForge items/components/tabs/resources packet.
- Create: `Soulbound Dolls/.trellis/tasks/004-player-registry.md` - saved data, login events, skin refresh packet.
- Create: `Soulbound Dolls/.trellis/tasks/005-entity-placement.md` - doll entity, placement, pickup, interaction packet.
- Create: `Soulbound Dolls/.trellis/tasks/006-client-rendering.md` - renderer/model/skin texture packet.
- Create: `Soulbound Dolls/.trellis/tasks/007-commands-config.md` - commands and config packet.
- Create: `Soulbound Dolls/.trellis/tasks/008-recipes-data.md` - recipes, loot, language, data generation packet.
- Create: `Soulbound Dolls/.trellis/tasks/009-verification.md` - build, run, server smoke, and review packet.
- Create: `Soulbound Dolls/.trellis/codegraph.md` - CodeGraph initialization results, important symbols, and impact notes.
- Create: `Soulbound Dolls/.trellis/verification-log.md` - command outputs and unresolved risks.

## Pipeline Gates

- [ ] **Gate 0: Prepare isolated project directory**

Run: create `E:\Codes\MC\Soulbound Dolls` only after confirming it does not already exist.

Expected: directory exists and contains no unrelated files.

- [ ] **Gate 1: Initialize CodeGraph**

Run from `E:\Codes\MC` after the project skeleton exists:

```powershell
codegraph init -i
```

Expected: `.codegraph` exists, and `codegraph_status` reports a healthy index. If the current tool environment still lacks `codegraph_*`, record that in `Soulbound Dolls/.trellis/codegraph.md` and use `Glob`/`Grep` only as fallback.

- [ ] **Gate 2: Trellis artifact baseline**

Write PRD/spec/tasks before implementation edits. The implementation agents must read exactly one task packet plus `prd.md`, `spec.md`, and `codegraph.md`.

Expected: every task packet has scope, files, steps, commands, expected output, and rollback notes.

- [ ] **Gate 3: Subagent execution only**

Dispatch one focused subagent per task packet. No two agents may edit the same file group concurrently.

Expected: each subagent returns changed files, verification commands, command results, and residual risks.

- [ ] **Gate 4: Coordinator review between tasks**

After each subagent returns, the coordinator reviews diffs, runs the narrow verification command, updates `verification-log.md`, then dispatches the next non-conflicting task.

Expected: failures are fixed before dependent tasks start.

## File Structure Target

```text
Soulbound Dolls/
  settings.gradle
  build.gradle
  gradle.properties
  gradle/wrapper/gradle-wrapper.properties
  gradle/wrapper/gradle-wrapper.jar
  gradlew
  gradlew.bat
  common/
    build.gradle
    src/main/java/com/yabo/soulbounddolls/common/PlayerDollProfile.java
    src/main/java/com/yabo/soulbounddolls/common/DollConstants.java
    src/test/java/com/yabo/soulbounddolls/common/PlayerDollProfileTest.java
  platforms/neoforge-1.21.1/
    build.gradle
    src/main/java/com/yabo/soulbounddolls/neoforge/SoulboundDollsNeoForge.java
    src/main/java/com/yabo/soulbounddolls/neoforge/SoulboundDollsItems.java
    src/main/java/com/yabo/soulbounddolls/neoforge/SoulboundDollsComponents.java
    src/main/java/com/yabo/soulbounddolls/neoforge/SoulboundDollsEntities.java
    src/main/java/com/yabo/soulbounddolls/neoforge/SoulboundDollsCreativeTab.java
    src/main/java/com/yabo/soulbounddolls/neoforge/SoulboundDollsConfig.java
    src/main/java/com/yabo/soulbounddolls/neoforge/item/PlayerDollItem.java
    src/main/java/com/yabo/soulbounddolls/neoforge/entity/PlayerDollEntity.java
    src/main/java/com/yabo/soulbounddolls/neoforge/data/DollPlayerRegistrySavedData.java
    src/main/java/com/yabo/soulbounddolls/neoforge/skin/DollSkinResolver.java
    src/main/java/com/yabo/soulbounddolls/neoforge/command/SoulboundDollsCommands.java
    src/main/java/com/yabo/soulbounddolls/neoforge/client/PlayerDollRenderer.java
    src/main/java/com/yabo/soulbounddolls/neoforge/client/PlayerDollModel.java
    src/main/resources/META-INF/neoforge.mods.toml
    src/main/resources/assets/soulbound_dolls/lang/en_us.json
    src/main/resources/assets/soulbound_dolls/lang/zh_cn.json
    src/main/resources/assets/soulbound_dolls/models/item/player_doll.json
    src/main/resources/assets/soulbound_dolls/models/item/doll_catalog.json
    src/main/resources/assets/soulbound_dolls/textures/item/player_doll.png
    src/main/resources/assets/soulbound_dolls/textures/entity/default_doll.png
    src/main/resources/data/soulbound_dolls/recipe/player_doll.json
```

## Subagent Work Packets

### Task 1: Bootstrap Project

**Files:** create Gradle skeleton, wrapper files, `common/build.gradle`, `platforms/neoforge-1.21.1/build.gradle`, `META-INF/neoforge.mods.toml`, empty package roots.

- [ ] Copy the structure from `E:\Codes\MC\Mirrors Glide` and rename identifiers to `soulbound_dolls`, `Soulbound Dolls`, and `com.yabo.soulbounddolls`.
- [ ] Remove optional Touhou dependencies from the new project.
- [ ] Set Java 21, Minecraft `1.21.1`, NeoForge `21.1.18`, loader range `[4,)`, and archive name `soulbound-dolls-neoforge-1.21.1`.
- [ ] Run: `./gradlew projects` from `Soulbound Dolls`.
- [ ] Expected: Gradle lists `:common` and `:platforms:neoforge-1.21.1`.

**Subagent prompt:**

```text
Create only the Gradle/NeoForge skeleton for E:\Codes\MC\Soulbound Dolls. Use E:\Codes\MC\Mirrors Glide as the structural template, but remove Touhou dependencies. Do not implement gameplay classes beyond empty mod entry scaffolding needed for Gradle to evaluate. Return created files, command output for ./gradlew projects, and any build blockers.
```

### Task 2: Common Domain Data

**Files:** create `PlayerDollProfile.java`, `DollConstants.java`, `PlayerDollProfileTest.java`.

- [ ] Define immutable profile fields: `UUID uuid`, `String name`, `String skinValue`, `String skinSignature`, `boolean slimModel`, `long lastUpdated`.
- [ ] Add factory methods for player-derived profile and fallback profile.
- [ ] Add validation: UUID required, blank names become `Unknown Player`, missing skin strings become empty strings.
- [ ] Run: `./gradlew :common:test`.
- [ ] Expected: tests pass for normalization, fallback stability, and equality.

**Subagent prompt:**

```text
Implement the common domain model for player doll profiles in Soulbound Dolls. Work only in common/. Add focused JUnit tests before implementation. Do not touch NeoForge runtime code. Return changed files and ./gradlew :common:test output.
```

### Task 3: Registries, Item, Component, Resources

**Files:** create registration classes, `PlayerDollItem.java`, lang files, item models, placeholder textures, recipe shell.

- [ ] Register `player_doll` item with a `PlayerDollProfile` data component.
- [ ] Register `doll_catalog` item or block item as the first catalog surface.
- [ ] Add creative tab entry for both items.
- [ ] Add tooltip text showing bound player and creator/prototype language keys.
- [ ] Run: `./gradlew :platforms:neoforge-1.21.1:compileJava`.
- [ ] Expected: Java compilation succeeds.

**Subagent prompt:**

```text
Implement item/component registration for Soulbound Dolls. Work in platforms/neoforge-1.21.1 and common only if the Task 2 profile needs small serialization adjustments. Register player_doll and doll_catalog, add creative tab, tooltips, lang, item models, and placeholder textures. Return changed files and compileJava output.
```

### Task 4: Player Registry And Skin Refresh

**Files:** create `DollPlayerRegistrySavedData.java`, `DollSkinResolver.java`, login event hooks in `SoulboundDollsNeoForge.java`.

- [ ] On player login, upsert UUID, name, `textures` property value/signature if present, model hint if known, and timestamp.
- [ ] Add async Mojang session/profile lookup behind config flag `enableOnlineSkinRefresh`, default true.
- [ ] Cache failures without blocking login; retry on later login or `/sbdoll refresh`.
- [ ] Run: server compile and unit-like serialization tests if available.
- [ ] Expected: registry serializes and deserializes at least two player records.

**Subagent prompt:**

```text
Implement server saved data and skin refresh plumbing. Keep network calls isolated in DollSkinResolver and never block the login event. Add serialization tests where possible. Do not implement commands except command-facing methods. Return changed files, compile/test output, and how failure caching works.
```

### Task 5: Doll Entity, Placement, Pickup, Interaction

**Files:** create `PlayerDollEntity.java`, entity registration, placement logic in `PlayerDollItem.java`.

- [ ] Right-click block with bound `player_doll` spawns a small doll entity with profile data.
- [ ] Sneak right-click doll picks it back up and preserves profile data.
- [ ] Empty-hand right-click plays pat feedback: note sound, heart particles, and short animation state.
- [ ] Sneak empty-hand right-click triggers shake feedback.
- [ ] Run: `./gradlew :platforms:neoforge-1.21.1:compileJava`.
- [ ] Expected: compile succeeds and entity data is saved/loaded.

**Subagent prompt:**

```text
Implement the server-side player doll entity and item placement/pickup interactions. Do not implement custom client rendering beyond data needed by the entity. Keep Kaleidoscope behavior as inspiration only: pat and shake feedback must use original implementation. Return changed files and compile output.
```

### Task 6: Client Rendering

**Files:** create `PlayerDollRenderer.java`, `PlayerDollModel.java`, client event registration, default entity texture.

- [ ] Render doll as original big-head/small-body model.
- [ ] Use profile skin texture when available through Minecraft skin manager/profile texture APIs.
- [ ] Use `default_doll.png` fallback when skin is missing.
- [ ] Ensure renderer works when joining server without the profile already cached client-side.
- [ ] Run: `./gradlew :platforms:neoforge-1.21.1:compileJava`.
- [ ] Expected: compile succeeds; residual visual risk recorded for in-game smoke test.

**Subagent prompt:**

```text
Implement client model and renderer for PlayerDollEntity. Focus only on client classes and registration. Use player skin when resolvable, otherwise fallback texture. Do not change server placement logic. Return changed files, compile output, and any in-game checks still needed.
```

### Task 7: Commands And Config

**Files:** create `SoulboundDollsCommands.java`, `SoulboundDollsConfig.java`, command registration hooks, language keys.

- [ ] Add `/sbdoll list` for operators to list known players.
- [ ] Add `/sbdoll give <target> <prototype>` for operators to give a bound doll.
- [ ] Add `/sbdoll refresh <prototype>` for operators to refresh skin data.
- [ ] Add config values: `autoGiveOwnDoll`, `enableOnlineSkinRefresh`, `allowPatParticles`, `allowPickupByAnyone`.
- [ ] Run: `./gradlew :platforms:neoforge-1.21.1:compileJava`.
- [ ] Expected: commands compile and permission checks require operator level 2.

**Subagent prompt:**

```text
Implement commands and config only. Commands must use the saved player registry APIs from Task 4 and item creation APIs from Task 3. Do not change entity rendering or placement. Return changed files and compile output.
```

### Task 8: Recipes, Data, Localization

**Files:** resources under `assets/soulbound_dolls` and `data/soulbound_dolls`.

- [ ] Add English and Simplified Chinese names, tooltips, command messages, and config comments.
- [ ] Add recipe for blank `player_doll` using wool, string, paper, and player head or fallback material if player head is not recipe-legal in 1.21.1.
- [ ] Add item model JSON for `player_doll` and `doll_catalog`.
- [ ] Add `pack.mcmeta` if required by NeoForge resource validation.
- [ ] Run: `./gradlew :platforms:neoforge-1.21.1:processResources`.
- [ ] Expected: resources process without JSON errors.

**Subagent prompt:**

```text
Implement resources, recipes, and localization for Soulbound Dolls. Do not edit Java except to add missing lang keys discovered by compile/resource validation. Return changed files and processResources output.
```

### Task 9: Verification And Review

**Files:** update `.trellis/verification-log.md`, optionally create `TESTING.md`.

- [ ] Run: `./gradlew clean build` from `Soulbound Dolls`.
- [ ] Run: `./gradlew :platforms:neoforge-1.21.1:runServer` long enough to confirm dedicated server starts, then stop cleanly.
- [ ] Run: client smoke test if GUI environment is available; otherwise record that manual visual verification remains.
- [ ] Verify no secrets or unrelated workspace files were modified.
- [ ] Record remaining risks and exact commands in `.trellis/verification-log.md`.

**Subagent prompt:**

```text
Verify the completed Soulbound Dolls project. Do not implement new features. Fix only build/resource/test failures that block verification, and report any gameplay risks separately. Return command outputs, changed files, and remaining manual checks.
```

## Parallelization Strategy

- Dispatch Task 1 alone.
- After Task 1 passes, dispatch Task 2 and Trellis artifact writing in parallel if they do not touch the same files.
- Task 3 depends on Task 2.
- Task 4 depends on Task 2 and may run in parallel with Task 8 after Task 3 resource keys stabilize.
- Task 5 depends on Task 3 and Task 4 APIs.
- Task 6 depends on Task 5 entity fields.
- Task 7 depends on Task 3 and Task 4 APIs; it can run in parallel with Task 6 if both avoid shared files.
- Task 9 runs last.

## Coordinator Checklist

- [ ] Before dispatching, paste only the relevant task packet, plus `prd.md`, `spec.md`, and `codegraph.md`, into the subagent prompt.
- [ ] Require every subagent to state whether it edited files outside its scope.
- [ ] After each subagent, run the narrow command listed in that task.
- [ ] Update `.trellis/verification-log.md` with command, result, and failure summary.
- [ ] Re-run CodeGraph after file creation or major refactors and update `.trellis/codegraph.md` with symbol names that later tasks should use.
- [ ] Do not commit unless the user explicitly asks for commits.

## Self-Review

- Spec coverage: bootstrap, player profile data, automatic player registry, skin acquisition fallback, item tooltip, placement, pickup, wearing/rendering-adjacent support, interaction, catalog/commands, config, resources, and verification are all assigned to tasks.
- Placeholder scan: no task contains `TBD`, `TODO`, or an unbounded instruction without a concrete file group and verification command.
- Type consistency: profile type is consistently `PlayerDollProfile`; mod id is consistently `soulbound_dolls`; package root is consistently `com.yabo.soulbounddolls`.
