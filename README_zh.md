# Soulbound Dolls

[![Build](https://github.com/yabo083/soulbound-dolls/actions/workflows/build.yml/badge.svg)](https://github.com/yabo083/soulbound-dolls/actions/workflows/build.yml)
[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.1-62B47A)](https://www.minecraft.net/)
[![NeoForge](https://img.shields.io/badge/NeoForge-21.1.18-EB6F2D)](https://neoforged.net/)
[![Java](https://img.shields.io/badge/Java-21-007396)](https://adoptium.net/)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

Soulbound Dolls 是一个 NeoForge 1.21.1 模组，用来把服务器里已知玩家变成小型、可放置、可互动的玩家皮肤娃娃。它会在服务端记录玩家资料，支持管理员发放绑定娃娃，并在有皮肤数据时用玩家皮肤渲染物品和放置实体。

[English](README.md) | [中文用户指南](USER_GUIDE_CN.md) | [发布说明](docs/release.md) | [更新日志](CHANGELOG.md)

## 亮点

- 基于 UUID 的玩家绑定娃娃物品。
- 可放置的 `Player Doll` 实体，支持坐姿、站立、可爱待机三种姿势。
- 绑定娃娃物品和放置实体支持动态玩家皮肤渲染。
- 世界/服务器级已知玩家资料记录。
- 管理员命令可列出资料、发放娃娃、刷新皮肤。
- 可配置自动发放、在线皮肤刷新、拍摸粒子、拾取权限。
- 提供英文和简体中文用户文档。

## 兼容性

| 组件 | 版本 |
| --- | --- |
| Minecraft | `1.21.1` |
| NeoForge | `21.1.18+` |
| Java | `21` |
| 加载器 | 仅 NeoForge |

## 安装

1. 安装 Minecraft `1.21.1` NeoForge 客户端或服务器。
2. 从可信 release 来源下载 `soulbound-dolls-neoforge-1.21.1-*.jar`。
3. 将 jar 放入实例的 `mods` 目录。
4. 启动游戏或专用服务器。

Modrinth 和 CurseForge 发布流水线已经预留，但在维护者完成项目 ID 和 token 配置前不会正式发布。

## 玩家流程

1. 玩家进入世界或服务器。
2. 服务端记录玩家 UUID、名称和可用皮肤纹理数据。
3. 如果 `autoGiveOwnDoll` 开启，玩家会在登录时获得自己的绑定娃娃。
4. 空白 `Player Doll` 也可以合成，并在首次放置时绑定为放置者。
5. 放置后的娃娃可以拍摸、摇晃、切换姿势、创造中键复制，并按权限拾取。

## 管理员命令

所有命令都需要权限等级 2。

```text
/sbdoll list
/sbdoll give <target> <prototype>
/sbdoll refresh <prototype>
```

`<prototype>` 可以是已知玩家名称或 UUID。

## 配置

通用配置文件会生成在：

```text
config/soulbound_dolls-common.toml
```

| 配置项 | 默认值 | 用途 |
| --- | --- | --- |
| `autoGiveOwnDoll` | `true` | 玩家登录时，如果还没有自己的绑定娃娃，就自动发放。 |
| `enableOnlineSkinRefresh` | `true` | 从 Mojang 服务刷新已知玩家皮肤。 |
| `allowPatParticles` | `true` | 拍摸娃娃时生成爱心粒子。 |
| `allowPickupByAnyone` | `false` | 允许任意玩家拾取已放置娃娃，否则仅创建者可拾取。 |

## 从源码构建

需要：

- JDK 21
- Git
- 可访问 Gradle 依赖源的网络环境

```powershell
.\gradlew.bat :platforms:neoforge-1.21.1:build
```

构建产物位于：

```text
platforms/neoforge-1.21.1/build/libs/
```

常用验证命令：

```powershell
.\gradlew.bat :common:test
.\gradlew.bat :platforms:neoforge-1.21.1:build
```

## 仓库结构

```text
common/                         纯 Java 领域模型和测试
platforms/neoforge-1.21.1/      NeoForge 运行时代码、资源、渲染器、命令
docs/                           发布和开发说明
USER_GUIDE.md                   英文玩法和管理员指南
USER_GUIDE_CN.md                中文玩法和管理员指南
```

## 当前限制

- `Doll Catalog` 已注册，但还没有完整图鉴 UI。
- 视觉效果仍需在物品栏、手持、掉落物、放置实体等场景做实机确认。
- 目前没有 GameTest 自动化测试，交互流程仍需真实客户端/服务器冒烟测试。
- 在 Modrinth 和 CurseForge 项目配置完成前，不触发正式发布。

## 许可证

本项目使用 [MIT License](LICENSE)。
