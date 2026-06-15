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
  item/PlayerDollItem                娃娃物品：放置实体、绑定资料、tooltip、装备、扔出
  entity/PlayerDollEntity            放置的娃娃实体：姿势、拍摸/摇晃/拾取交互、驱逐幻翼、吸引亡灵
  entity/DollProjectileEntity        投射物实体：扔出的玩偶，造成伤害后掉落
  skin/DollSkinResolver              服务端皮肤解析：从 GameProfile / session service 取纹理
  network/SoulboundDollsNetwork      网络包注册器（0.1.2 新增）
  network/TeleportToDollPlayerPacket 传送请求包：客户端→服务端（0.1.2 新增）
  client/DollSkinManager             客户端按 UUID 缓存皮肤纹理（见 §5）
  client/DollSkinCacheLifecycle      客户端断开连接时清空皮肤缓存（会话级）
  client/SoulboundDollsKeyBindings   按键绑定注册（0.1.2 新增）
  client/SoulboundDollsClientInputHandler  按键输入处理（0.1.2 新增）
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
| **0.1.2 新增特性** | | |
| `allowDollAsHelmet` | `true` | 允许玩偶作为头盔装备 |
| `enableTeleportToPlayer` | `true` | 允许传送到绑定玩家 |
| `teleportCooldownSeconds` | `60` | 传送冷却时间（秒） |
| `enableThrowDoll` | `true` | 允许扔出玩偶造成伤害 |
| `throwDollDamage` | `4.0` | 扔出玩偶的伤害值 |
| `enableAttractUndead` | `true` | 放置的玩偶吸引亡灵生物 |
| `attractUndeadRange` | `24.0` | 吸引亡灵生物的范围（方块） |
| `enableRepelPhantoms` | `true` | 放置的玩偶驱逐幻翼 |
| `repelPhantomsRange` | `32.0` | 驱逐幻翼的范围（方块） |
| `enableEnderMaskProtection` | `true` | 头部/兼容饰品槽佩戴绑定玩偶时提供 Enderman 注视保护 |

## 7. 权限与命令访问模型

命令始终注册，访问由权限控制（无配置开关）：
- **操作员**（开作弊单人世界，或权限等级 2 的服务器 OP/腐竹）始终可用全部命令。
- 操作员可用 `/sbdoll permission grant|revoke|list <玩家>` 把使用权委派给特定玩家，
  无需给完整 OP。授权集合由 `DollCommandPermissions`（SavedData）随世界持久化。
- `canUseCommands` 谓词：`hasPermission(2)` 或在授权集合中。
- `permission` 子命令本身用 `requires(hasPermission(2))` 限定，**仅操作员可管理授权**，
  被授权玩家无法提权给他人。
- grant/revoke 后调用 `commands.sendCommands(target)` 重发命令树，使在线玩家即时生效。

## 8. 交互方式

### 8.1 实体交互（放置的玩偶）
- 手持娃娃右键方块 = 放置；手持物品右键娃娃 = 切换姿势；空手右键 = 拍摸。
- **左键（攻击）= 摇晃**；**潜行 + 右键 = 拾取**。拾取受 `allowPickupByAnyone` 限制，但
  操作员（权限等级 2 / 开作弊单人）可拾取任意玩偶（含他人绑定的）。

### 8.2 物品交互（手持玩偶）- 0.1.2 新增
- **作为头盔装备**：玩偶物品实现 `Equipable` 接口，可装备到头部槽位（`allowDollAsHelmet` 控制）。佩戴绑定玩偶时，
  `EnderMaskHelper` 会阻止 Enderman 因玩家注视而被激怒（`enableEnderMaskProtection` 控制；兼容饰品槽通过可选反射查询）。
- **扔出玩偶**：长按右键蓄力（最少 10 tick），松开后扔出 `DollProjectileEntity`。投射物击中实体时造成伤害（`throwDollDamage`），
  击中后掉落玩偶物品。使用弓箭蓄力动画，有 1 秒冷却时间（`enableThrowDoll` 控制）。
- **传送到玩家**：手持绑定玩偶时按 V 键（可配置），向服务器发送 `TeleportToDollPlayerPacket`。
  服务器检查目标玩家在线且同维度，在目标玩家 3 格范围内寻找安全位置并传送。有冷却时间限制（`teleportCooldownSeconds`）。

### 8.3 按键绑定
- `key.soulbound_dolls.teleport_to_player`（默认 V 键）：传送到玩偶绑定的玩家。

## 9. 网络通信（0.1.2 新增）

使用 NeoForge 1.21.1 的 `CustomPacketPayload` 系统：

- `SoulboundDollsNetwork`：在 MOD 总线注册 payload handlers，协议版本 "1"。
- `TeleportToDollPlayerPacket`（客户端 → 服务端）：
  - 空 record（无参数），客户端按键触发时发送。
  - 服务端处理逻辑：验证配置、检查冷却、验证玩偶物品、查找目标玩家、传送并播放音效。
  - 冷却数据存储在 `Map<UUID, Long>` 中（服务端内存，非持久化）。

## 10. 实体 AI 行为（0.1.2 新增）

### 10.1 驱逐幻翼（`PlayerDollEntity.repelPhantoms()`）
- 每 tick 在 `repelPhantomsRange` 范围内搜索 `Phantom` 实体。
- 清除幻翼的攻击目标（`setTarget(null)`），使其无法攻击附近玩家。
- 性能考虑：仅在服务端运行，使用 AABB 范围查询。

### 10.2 吸引亡灵与僵尸搬运（`ZombieMoveToDollGoal` / `ZombieDollCarryHelper`）
- `SoulboundDollsRuntimeEvents` 在亡灵实体加入世界时注册目标 AI，避免全局每 tick 维护；`getAvailableGoals()` 用于重载去重。
- `ZombieMoveToDollGoal` 只让未携带玩偶的僵尸搜索目标，优先选择最近的放置玩偶或掉落的绑定玩偶，并声明 `Goal.Flag.MOVE` 独占移动控制。
- 僵尸接近后会偷取/搬运玩偶，最多 3 个：头部、主手、副手优先作为可见槽位，额外玩偶使用隐藏 NBT 列表 `soulbound_dolls:carried_dolls` 存储。
- 携带任意玩偶的僵尸获得阳光保护；如果头部为空，辅助逻辑会把手持或隐藏玩偶提升到头部槽位以维持可见保护。
- 僵尸死亡时会保证掉落所有可见和隐藏携带的玩偶，避免玩家物品被吞。

## 11. 明确推迟的扩展点

- **YSM (Yes Steve Model) 模型覆盖**：本版本仅保证与 YSM 共存、互不影响；
  将玩家 YSM 模型覆盖到娃娃上（指令+按键两种模式）留待后续版本。
- **更多交互按键**：当前仅实现传送按键。未来可扩展更多自定义按键功能（如远程切换姿势、召回玩偶等）。
- **生物 AI 增强**：未来版本可添加更复杂的生物行为（如让玩偶跟随玩家、执行简单任务等）。

## 12. 架构扩展性设计（0.1.2 考虑）

为支持未来功能（版本计划中的生物 AI、特殊动作等），当前架构已预留扩展点：

- **模块化配置系统**：所有特性通过配置开关控制，易于添加新特性。
- **网络层抽象**：`SoulboundDollsNetwork` 可轻松注册新 packet 类型。
- **实体 tick 扩展**：`PlayerDollEntity.tick()` 中的特性调用模式易于添加新行为。
- **按键系统**：`SoulboundDollsKeyBindings` 可注册更多按键，`ClientInputHandler` 可处理多个按键。
