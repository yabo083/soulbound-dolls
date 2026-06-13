# Soulbound Dolls — 架构文档 (PROJECT_ARCHITECTURE.md)

> 本文档面向人类读者，描述模组的宏观架构、模块职责与关键数据流。
> 不记录单行代码级变更；仅在核心特性、拓扑结构或模块职责发生改变时同步。

## 1. 总览

Soulbound Dolls 是一个 **NeoForge 1.21.1 / Java 21** 模组：把服务器里登录过的玩家变成
可放置、可互动的玩家皮肤娃娃。运行时只依赖 NeoForge，皮肤数据来自 Minecraft 原生
profile 缓存与 session service，不引入第三方网络/HTTP 依赖。

## 2. 模块拓扑

```
common/                              纯 Java 领域模型（无 Minecraft 依赖，可单元测试）
  PlayerDollProfile                  娃娃资料记录：uuid/name/skinValue/skinSignature/slim/lastUpdated
  SkinTextureMetadata                皮肤纹理元数据解析（如 slim 判定）
  DollConstants                      MOD_ID 等共享常量

platforms/neoforge-1.21.1/           NeoForge 运行时集成
  SoulboundDollsNeoForge             @Mod 入口：注册配置、组件、实体、物品、创造标签、事件总线
  SoulboundDollsConfig               COMMON 配置规范（toml）
  SoulboundDollsComponents/Entities/Items/CreativeTab   DeferredRegister 注册
  SoulboundDollsRuntimeEvents        服务端事件：玩家登录时记录资料、按 TTL 异步刷新皮肤
  command/SoulboundDollsCommands     /sbdoll 命令树（权限制，可委派）
  data/DollPlayerRegistrySavedData   世界存档级已知玩家注册表（name↔uuid↔profile）
  data/DollCommandPermissions        世界存档级命令授权集合（被腐竹授权的玩家 UUID）
  item/PlayerDollItem                娃娃物品：放置实体、绑定资料、tooltip
  entity/PlayerDollEntity            放置的娃娃实体：姿势、拍摸/摇晃/拾取交互、DollPose 枚举
  skin/DollSkinResolver              服务端皮肤解析：从 GameProfile / session service 取纹理
  client/DollSkinManager             客户端按 UUID 缓存皮肤纹理（见 §5）
  client/DollSkinCacheLifecycle      客户端断开连接时清空皮肤缓存（会话级）
  client/*                           其余客户端渲染（见 §4）
```

公共领域代码通过 `sourceSets.main.java.srcDir rootProject.file("common/src/main/java")`
被编入 NeoForge 模块。

## 3. 核心数据流

### 3.1 玩家登录 → 资料记录与皮肤缓存
`SoulboundDollsRuntimeEvents.onPlayerLoggedIn`：
1. `DollPlayerRegistrySavedData.upsertFromLogin` 记录/更新玩家资料（存档持久化）。
2. 若 `autoGiveOwnDoll` 且玩家无绑定娃娃，发放一个。
3. 若 `enableOnlineSkinRefresh` 且 `shouldRetryRefresh`（超过 TTL）才异步打
   session service 刷新皮肤 —— **TTL 缓存避免每次登录/启动重复请求 Mojang**。

### 3.2 命令发放娃娃
`/sbdoll give <target> <prototype>` → `resolveProfile`：
1. 先查注册表（UUID 或名字，无网络）。
2. 未命中则回退服务器原生 `getProfileCache()`（`usercache.json`，含所有登录过的玩家）。
   命中后若 GameProfile 已带纹理直接构造资料；否则经 session service 补齐一次，再 `upsert` 存档。

### 3.3 放置玩偶的皮肤自愈（一次性，非每 tick）
`PlayerDollEntity.maybeHealMissingSkin`（服务端）：玩偶若无皮肤（如 LAN/离线首次登录时
属主纹理尚未就绪就被 auto-give），在加载后**仅尝试一次**异步拉取属主皮肤并热更新
（`skinHealAttempted`/`skinHealInFlight` 一次性标志，绝不每 tick 发请求）。手动
`/sbdoll give|refresh` 仍可独立触发。

## 4. 客户端渲染管线

```
PlayerDollEntity ──► PlayerDollRenderer ──► PlayerDollModel (setupAnim/姿势动画)
                                       └──► DollSkinManager.resolve(profile) ──► 纹理
PlayerDollItem   ──► PlayerDollItemRenderer (BEWLR) ──► 实体模型 或 模板模型(JSON)
                                       └──► DollSkinManager.resolve(profile) ──► 纹理
```

- `PlayerDollModel`：6 部件（头/身/双臂/双腿），三种姿势（坐/站/可爱待机）+ 待机摆动 + 拍摸反馈。
- `PlayerDollItemRenderer`：物品按是否绑定资料选择实体模型或 Blockbench JSON 模板模型渲染；
  支持物品栏/手持/掉落物/展示框变换（`DollDisplayConfig`）。

## 5. 皮肤缓存设计（性能关键）

`DollSkinManager.resolve()` 被两个渲染器**每帧**调用。为避免逐帧分配：
- 维护 `Map<UUID, CachedSkin>`（`CachedSkin = {skinValue, texture}`）。
- 命中且 `skinValue` 未变 → 直接返回纹理，**零分配**；否则重算并刷新缓存。
- 失效判据为 `skinValue`：皮肤刷新后（值变化）自动失效，下一帧重算。
- 缓存为会话级派生数据，不落盘；`DollSkinCacheLifecycle` 在客户端断开连接时调用
  `invalidateAll()` 清空，避免跨服务器残留。每条仅存 `skinValue` + `ResourceLocation`
  指针（真正的纹理位图由原版 `SkinManager` 持有），单玩家约 1 KB。

基准（`DollSkinManagerTest`）：1000 帧同一娃娃，底层皮肤解析从 1000 次降为 **1 次**。

服务端侧的“缓存”是 `DollPlayerRegistrySavedData` 的存档持久化 + `skinRefreshTtlMinutes`
TTL（默认 60 分钟）：在 TTL 窗口内复用已存资料，不重复请求 Mojang。

## 6. 配置项（COMMON，`config/soulbound_dolls-common.toml`）

| 配置项 | 默认 | 作用 |
| --- | --- | --- |
| `autoGiveOwnDoll` | `true` | 登录自动发放本人娃娃 |
| `enableOnlineSkinRefresh` | `true` | 登录/命令后在线刷新皮肤 |
| `allowPatParticles` | `true` | 拍摸爱心粒子 |
| `allowPickupByAnyone` | `false` | 任意玩家可拾取 |
| `skinRefreshTtlMinutes` | `60` | 同一玩家在线皮肤刷新最小间隔（TTL 缓存窗口） |

## 7. 权限与命令访问模型

命令始终注册，访问由权限控制（无配置开关）：
- **操作员**（开作弊单人世界，或权限等级 2 的服务器 OP/腐竹）始终可用全部命令。
- 操作员可用 `/sbdoll permission grant|revoke|list <玩家>` 把使用权委派给特定玩家，
  无需给完整 OP。授权集合由 `DollCommandPermissions`（SavedData）随世界持久化。
- `canUseCommands` 谓词：`hasPermission(2)` 或在授权集合中。
- `permission` 子命令本身用 `requires(hasPermission(2))` 限定，**仅操作员可管理授权**，
  被授权玩家无法提权给他人。
- grant/revoke 后调用 `commands.sendCommands(target)` 重发命令树，使在线玩家即时生效。

## 8. 交互方式（无自定义按键）

当前使用原版鼠标/潜行交互，各动作互不重叠：
- 手持娃娃右键方块 = 放置；手持物品右键娃娃 = 切换姿势；空手右键 = 拍摸。
- **左键（攻击）= 摇晃**；**潜行 + 右键 = 拾取**。拾取受 `allowPickupByAnyone` 限制，但
  操作员（权限等级 2 / 开作弊单人）可拾取任意玩偶（含他人绑定的）。

**自定义 KeyMapping 与网络包层尚未引入**，是后续版本的扩展点。

## 9. 明确推迟的扩展点

- **YSM (Yes Steve Model) 模型覆盖**：本版本仅保证与 YSM 共存、互不影响；
  将玩家 YSM 模型覆盖到娃娃上（指令+按键两种模式）留待后续版本。
- **全面可自定义按键**：把现有鼠标交互重构为原版可改键的 `KeyMapping`，需新建
  客户端按键 → 网络包 → 服务端执行的链路。
- 这些扩展点之所以推迟，是为先夯实性能与原版皮肤兼容这一坚实地基。
