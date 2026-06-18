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
- 管理员命令（基于权限、可委派）可列出资料、发放娃娃、刷新皮肤。
- 客户端按玩家缓存皮肤纹理，服务端在线刷新 TTL 可配置。
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
5. 放置后的娃娃可以拍摸、摇晃、切换姿势、创造中键复制、戴在头上，并按权限拾取。

## 操作方式

娃娃主要使用原版鼠标和潜行交互；传送到绑定玩家、切换头戴玩偶姿势使用按键绑定。

| 操作 | 输入 | 效果 |
| --- | --- | --- |
| 放置娃娃 | 手持娃娃右键点击方块表面 | 生成绑定该资料的 `Player Doll` 实体。 |
| 切换姿势 | 手持任意物品右键已放置娃娃 | 循环 坐姿 → 站立 → 可爱待机。 |
| 佩戴玩偶 | 头部槽为空时手持玩偶右键空气 | 只装备 1 个玩偶到头部槽。 |
| 切换头戴姿势 | 佩戴玩偶时按 B | 循环 坐姿 → 站立 → 可爱待机。 |
| 拍摸 | 空手右键已放置娃娃 | 播放音效并生成爱心粒子（若 `allowPatParticles` 开启）。 |
| 摇晃 | 潜行 + 攻击（左键）已放置娃娃 | 播放摇晃动画和音效。 |
| 拾取 | 潜行 + 右键已放置娃娃 | 把娃娃收回物品栏，受 `allowPickupByAnyone` 限制。 |

> 说明：头戴玩偶姿势切换默认按键是 B，可在 Minecraft 按键设置中修改。

## 管理员命令

命令没有配置开关，改为基于权限控制：

- **操作员**（开启作弊的单人世界，或权限等级 2 的服务器操作员/腐竹）始终可用。
- 操作员可用 `/sbdoll permission grant <玩家>` 把使用权授予特定玩家，**无需**给对方完整 OP；
  用 `revoke` 收回，用 `list` 查看已授权玩家。
- 被授权玩家可用 `list`、`give`、`refresh`，但**不能**用 `permission`（不能继续提权）。

```text
/sbdoll list
/sbdoll give <target> <prototype...>
/sbdoll giveall <target>
/sbdoll refresh <prototype>
/sbdoll permission grant <玩家>      （仅操作员）
/sbdoll permission revoke <玩家>     （仅操作员）
/sbdoll permission list              （仅操作员）
```

`<prototype>` 可以是玩家名称或 UUID。`give` 支持多个以空格分隔的名字批量发放；`giveall` 一次性
为所有已知玩家各发一个娃娃。`give` 可解析**任何登录过本服务器的玩家**（通过原生 `usercache.json`
资料缓存），不限于已在娃娃注册表中的玩家 —— 因此即使在离线模式服务器上，也能为任何曾经加入过的
玩家发放娃娃。`list` 会列出所有已知玩家，并标注是否已捕获皮肤（✔）或仍是默认皮肤（✘）。

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
| `skinRefreshTtlMinutes` | `60` | 同一玩家两次在线皮肤刷新之间的最小分钟数；在此窗口内复用缓存皮肤。 |
| `allowDollAsHelmet` | `true` | 允许玩家玩偶装备到头部槽。 |
| `enableEnderMaskProtection` | `true` | 将头戴或兼容饰品槽中的玩偶视为末影人面具。 |

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
- 本版本对 Yes Steve Model (YSM) 仅做共存兼容；把玩家的 YSM 模型覆盖到娃娃上计划在后续版本实现。
- 在 Modrinth 和 CurseForge 项目配置完成前，不触发正式发布。

## 许可证

本项目使用 [MIT License](LICENSE)。
