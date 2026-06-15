# Jade HUD And Skin Diagnostics Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add optional Jade HUD support for player dolls and add diagnostic logs to identify why the local player's doll skin still loads late.

**Architecture:** Jade integration is optional and isolated in a client plugin class registered only when Jade is present. Skin diagnostics are compact structured logs at profile persistence, entity sync/save/load, and client skin resolution boundaries.

**Tech Stack:** NeoForge 1.21.1, Java 21, Jade API 15.10.5+neoforge, SLF4J logger already exposed by `SoulboundDollsNeoForge.LOGGER`, Gradle `copyJarToTest`.

---

### Task 1: Add Optional Jade Dependency And Plugin Metadata

**Files:**
- Modify: `settings.gradle`
- Modify: `platforms/neoforge-1.21.1/build.gradle`
- Modify: `platforms/neoforge-1.21.1/src/main/resources/META-INF/neoforge.mods.toml`
- Create: `platforms/neoforge-1.21.1/src/main/resources/META-INF/jade/soulbound_dolls.json`

- [ ] Add Modrinth Maven repository.
- [ ] Add compile-only Jade API dependency for `15.10.5+neoforge`.
- [ ] Declare Jade as an optional client dependency.
- [ ] Add Jade plugin metadata pointing to `com.yabo.soulbounddolls.neoforge.compat.jade.SoulboundDollsJadePlugin`.

### Task 2: Implement Jade Entity Component Provider

**Files:**
- Create: `platforms/neoforge-1.21.1/src/main/java/com/yabo/soulbounddolls/neoforge/compat/jade/SoulboundDollsJadePlugin.java`
- Modify: `platforms/neoforge-1.21.1/src/main/resources/assets/soulbound_dolls/lang/en_us.json`
- Modify: `platforms/neoforge-1.21.1/src/main/resources/assets/soulbound_dolls/lang/zh_cn.json`

- [ ] Register an `IEntityComponentProvider` for `PlayerDollEntity`.
- [ ] Append `jade.soulbound_dolls.bound_player` with `profile.name()`.
- [ ] Keep provider side-effect-free and client-only through Jade metadata.

### Task 3: Add Phase-1 Skin Diagnostics

**Files:**
- Modify: `platforms/neoforge-1.21.1/src/main/java/com/yabo/soulbounddolls/neoforge/client/DollSkinManager.java`
- Modify: `platforms/neoforge-1.21.1/src/main/java/com/yabo/soulbounddolls/neoforge/data/DollPlayerRegistrySavedData.java`
- Modify: `platforms/neoforge-1.21.1/src/main/java/com/yabo/soulbounddolls/neoforge/entity/PlayerDollEntity.java`

- [ ] Log registry `upsert` inputs and merge results.
- [ ] Log entity `setProfile`, `readAdditionalSaveData`, `addAdditionalSaveData`, and client synced-data changes.
- [ ] Log client skin resolution decisions: no skin, cache hit, loaded-player hit, loaded-player temporary default, profile lookup resolved, profile lookup temporary default.
- [ ] Include UUID short form, name, hasSkin, skin value hash/length, side, and texture path without logging full skin payloads.

### Task 4: Verify And Copy Jar

**Files:**
- No code files.

- [ ] Run `./gradlew test`.
- [ ] Run `./gradlew build`.
- [ ] Run `./gradlew :platforms:neoforge-1.21.1:copyJarToTest`.
- [ ] Report exact commands and whether copy succeeded.

### Manual Test Instructions

- [ ] Launch the Mechanomania test instance.
- [ ] Enter the affected save.
- [ ] Look at your own placed doll and a LAN friend's doll.
- [ ] Search logs for `[SoulboundDollsSkin]` and compare first 10 seconds after world join.
