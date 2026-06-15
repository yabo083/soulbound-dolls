# Changelog

## 0.1.2 - 2026-06-15

### Added
- **Doll as Helmet**: Player dolls can now be equipped as helmet armor by any entity with a head slot (players and mobs). Worn dolls also act as an Enderman mask, preventing look anger when `enableEnderMaskProtection` is enabled (default: true). Controlled by `allowDollAsHelmet` config (default: true).
- **Teleport to Player**: Press V (configurable keybind) while holding a bound doll to teleport near the bound player. Requires the player to be online and in the same dimension. Has configurable cooldown (`teleportCooldownSeconds`, default: 60). Controlled by `enableTeleportToPlayer` config (default: true).
- **Throwable Dolls**: Hold right-click to charge and throw a doll item, dealing damage on impact. Damage is configurable (`throwDollDamage`, default: 4.0). The doll drops at the impact location. Controlled by `enableThrowDoll` config (default: true).
- **Attract Undead Mobs**: Placed dolls attract undead mobs (zombies, skeletons, etc.) within a configurable range (`attractUndeadRange`, default: 24 blocks), similar to turtle eggs. Zombies steal and carry up to 3 dolls, gain sunlight protection while carrying, and drop carried dolls on death. Controlled by `enableAttractUndead` config (default: true).
- **Repel Phantoms**: Placed dolls repel phantoms within a configurable range (`repelPhantomsRange`, default: 32 blocks), clearing their targets and preventing attacks. Controlled by `enableRepelPhantoms` config (default: true).

### Technical
- Add `DollProjectileEntity` for throwable doll mechanics.
- Add networking system with `TeleportToDollPlayerPacket` for client-server communication.
- Add client-side key binding system with configurable teleport key.
- Add server-side cooldown tracking for teleport feature.
- All new features are toggleable via configuration with sensible defaults.

### Fixed
- Convert `gradlew.bat` to CRLF line endings to fix Windows cmd.exe parsing issues.
- Add `.gitattributes` to enforce CRLF for `*.bat` files and LF for shell scripts.

## 0.1.1 - 2026-06-13

### Performance
- Cache resolved doll skin textures per player in `DollSkinManager`. The entity and item
  renderers call `resolve()` every frame; previously each call built a new `GameProfile` and
  `Property`. Benchmark (`DollSkinManagerTest`): across 1000 render frames for one doll the
  underlying skin lookup now runs **once instead of 1000 times** (999 fewer `GameProfile`
  allocations per 1000 frames per doll). The cache invalidates automatically when a player's
  skin value changes.
- Make the online skin-refresh TTL configurable (`skinRefreshTtlMinutes`, default 60). Cached
  player skins are reused within this window instead of re-querying Mojang on every login,
  reducing startup and login network traffic.

### Added
- `/sbdoll give` now resolves any player that has logged into the server via the native profile
  cache (`usercache.json`), not just players already in the doll registry. Works on offline-mode
  servers and persists the generated profile.
- `/sbdoll give <target> <name...>` accepts multiple space-separated names for batch handouts, and
  `/sbdoll giveall <target>` hands out a doll for every known player at once.
- Placed dolls self-heal a missing skin once: if a doll was created before its owner's textures were
  available (common on LAN/offline first join), it fetches the owner's skin from Mojang a single time
  (never per tick) and hot-updates. Manual `give`/`refresh` still work.
- Permission-based command access: operators (level 2) can always use `/sbdoll`, and can delegate
  access to specific players with `/sbdoll permission grant|revoke|list <player>` without granting
  full operator status. Granted players cannot manage permissions themselves. The granted set is
  saved with the world.

### Changed
- `/sbdoll list` now prints a styled multi-line list of all known players (registry + login history),
  marking whether each has a captured skin (✔) or is still on the default skin (✘).
- Operators (permission level 2, incl. single-player with cheats) can pick up any placed doll,
  including dolls bound to other players, regardless of `allowPickupByAnyone`.
- Doll interactions no longer overlap: left-click (attack) always shakes the doll, and sneak +
  right-click always picks it up. Previously pickup fired on both sneak + attack and
  sneak + right-click.

### Docs
- Document in-game doll interactions (place, pose cycle, pat, shake, pickup) and the command
  permission model in the README.
- Add `PROJECT_ARCHITECTURE.md` describing module topology, render pipeline, and skin caching.

## 0.1.0 - 2026-06-11

- Initial NeoForge 1.21.1 release.
- Add player-bound doll items and placed player doll entities.
- Render bound doll items with the same player skin model used by placed dolls.
- Support inventory, hand-held, dropped item, and item-frame display transforms.
- Add local build/test workflow and release publishing scaffolding for Modrinth and CurseForge.
