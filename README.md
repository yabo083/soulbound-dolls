# Soulbound Dolls

[![Build](https://github.com/yabo083/soulbound-dolls/actions/workflows/build.yml/badge.svg)](https://github.com/yabo083/soulbound-dolls/actions/workflows/build.yml)
[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.1-62B47A)](https://www.minecraft.net/)
[![NeoForge](https://img.shields.io/badge/NeoForge-21.1.18-EB6F2D)](https://neoforged.net/)
[![Java](https://img.shields.io/badge/Java-21-007396)](https://adoptium.net/)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

Soulbound Dolls is a NeoForge 1.21.1 mod that turns known players into small, placeable player-skin dolls. It records player profiles on the server, lets operators issue bound dolls, and renders bound items and placed entities with player skins when texture data is available.

[中文说明](README_zh.md) | [User Guide](USER_GUIDE.md) | [Release Guide](docs/release.md) | [Changelog](CHANGELOG.md)

## Highlights

- Player-bound doll items with UUID-backed profile data.
- Placeable `Player Doll` entities with sitting, standing, and cute idle poses.
- Dynamic player-skin rendering for bound doll items and placed dolls.
- Server-side known-player registry saved with the world.
- Operator commands for listing profiles, giving dolls, and refreshing skins.
- Configurable auto-give, online skin refresh, pat particles, and pickup permissions.
- English and Simplified Chinese user documentation.

## Compatibility

| Component | Version |
| --- | --- |
| Minecraft | `1.21.1` |
| NeoForge | `21.1.18+` |
| Java | `21` |
| Mod loader | NeoForge only |

## Install

1. Install a Minecraft `1.21.1` NeoForge client or server.
2. Download the built `soulbound-dolls-neoforge-1.21.1-*.jar` from a trusted release source.
3. Put the jar in the instance `mods` directory.
4. Start the game or dedicated server.

Publishing to Modrinth and CurseForge is scaffolded but not active until project IDs and tokens are configured by the maintainer.

## Player Flow

1. A player joins a world or server.
2. The server records their UUID, name, and available skin texture data.
3. If `autoGiveOwnDoll` is enabled, the player receives their own bound doll once.
4. A blank `Player Doll` can also be crafted and binds to the placing player on first placement.
5. Placed dolls can be patted, shaken, pose-cycled, copied in creative mode, and picked up with permission checks.

## Commands

All commands require permission level 2.

```text
/sbdoll list
/sbdoll give <target> <prototype>
/sbdoll refresh <prototype>
```

`<prototype>` can be a known player name or UUID.

## Config

The common config is generated at:

```text
config/soulbound_dolls-common.toml
```

| Option | Default | Purpose |
| --- | --- | --- |
| `autoGiveOwnDoll` | `true` | Give players their own bound doll on login if missing. |
| `enableOnlineSkinRefresh` | `true` | Refresh known player skins from Mojang services. |
| `allowPatParticles` | `true` | Emit heart particles when dolls are patted. |
| `allowPickupByAnyone` | `false` | Allow anyone to pick up placed dolls instead of only the creator. |

## Build From Source

Requirements:

- JDK 21
- Git
- Internet access for Gradle dependency resolution

```powershell
.\gradlew.bat :platforms:neoforge-1.21.1:build
```

Built jars are written under:

```text
platforms/neoforge-1.21.1/build/libs/
```

Useful verification commands:

```powershell
.\gradlew.bat :common:test
.\gradlew.bat :platforms:neoforge-1.21.1:build
```

## Repository Layout

```text
common/                         Pure Java domain model and tests
platforms/neoforge-1.21.1/      NeoForge runtime, resources, renderer, commands
docs/                           Release and development notes
USER_GUIDE.md                   English gameplay and admin guide
USER_GUIDE_CN.md                Simplified Chinese gameplay and admin guide
```

## Current Limitations

- `Doll Catalog` is registered but does not yet open a complete catalog UI.
- Visual polish still depends on manual in-game checks across inventory, hand, dropped item, and placed entity contexts.
- No GameTest suite exists yet, so interaction flows still need real client/server smoke testing.
- Public release publishing is intentionally held until Modrinth and CurseForge project setup is complete.

## License

This project is licensed under the [MIT License](LICENSE).
