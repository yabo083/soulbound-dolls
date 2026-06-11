# Blockbench JSON 与绑定玩家玩偶渲染问题调查

日期：2026-06-11

## 一句话定位

迄今为止我们一直在解决的不是“某个角度数值调错了”，而是一个架构错位：

`player_doll_3d.json` 是 Minecraft baked item model / Blockbench 方块模型模板，而绑定玩家玩偶最初走的是自定义 Java 渲染路径。自定义渲染路径没有天然执行 Minecraft item model pipeline，所以它不会自动继承 Blockbench JSON 的元素、display、UV、贴图 atlas、变换顺序和资源重载语义。

结果就是：未绑定玩偶看起来听 JSON 的，绑定玩家玩偶看起来不听 JSON 的。

## 现象摘要

用户已经在 Blockbench 中把 `player_doll_3d.json` 调到满意：

- 物品栏姿态满意。
- 第一人称姿态满意。
- 第三人称姿态满意。
- 未绑定/静态模板能正确表现这些调整。

但绑定玩家皮肤后的玩偶反复出现：

- 物品栏方向不一致或背面朝前。
- 位置偏移、缩放不一致。
- 第一/第三人称手持姿态不跟模板。
- 改成手写模板几何后，出现黑紫 missing texture 方块。

这些现象都指向同一件事：绑定路径没有真正复用 vanilla item model 渲染流程，而是在 Java 里重新实现了其中一部分。

## 关键文件

- `platforms/neoforge-1.21.1/src/main/resources/assets/soulbound_dolls/models/item/player_doll.json`
- `platforms/neoforge-1.21.1/src/main/resources/assets/soulbound_dolls/models/item/player_doll_3d.json`
- `platforms/neoforge-1.21.1/src/main/java/com/yabo/soulbounddolls/neoforge/client/PlayerDollItemRenderer.java`
- `platforms/neoforge-1.21.1/src/main/java/com/yabo/soulbounddolls/neoforge/client/PlayerDollModel.java`
- `platforms/neoforge-1.21.1/src/main/java/com/yabo/soulbounddolls/neoforge/client/SoulboundDollsClientEvents.java`
- `platforms/neoforge-1.21.1/src/main/java/com/yabo/soulbounddolls/neoforge/client/TemplateTextureAliases.java`

## 当前模型结构

`player_doll.json` 是物品入口模型，目前应保持中性：

```json
{
  "parent": "builtin/entity"
}
```

它的作用是让物品走自定义 `BlockEntityWithoutLevelRenderer`。

`player_doll_3d.json` 是用户可在 Blockbench 中编辑的模板，包含：

- `textures`
- `elements`
- 每个 element 的 `from` / `to`
- 每个 face 的 `uv` / `texture`
- element `rotation`
- item `display`

用户直觉上认为：既然这个 JSON 已经能表达模型如何摆放，那么绑定玩家玩偶也应该直接听这个 JSON。

这个直觉是对的，但前提是绑定渲染路径必须真的走同一套 item model pipeline，或者完整模拟它。

## 迄今尝试过的修复路线

### 路线 1：Java 玩家模型 + 手写 display 变换

最初绑定玩偶使用 `PlayerDollModel`：

- 优点：容易套玩家皮肤。
- 缺点：它是 Java `ModelPart`，不是 `player_doll_3d.json` 的 baked item model。

当时只从 `player_doll_3d.json` 读取 `display`，然后在 Java 里额外做：

- GUI 180 度翻转。
- 硬编码缩放。
- 坐标偏移。
- GUI standing / hand sitting pose 切换。

问题：

- JSON 只控制整体 display，不控制身体部件。
- Blockbench 里调的 `elements`、部件旋转、部件大小完全不会影响绑定模型。
- Java 自己补的 180 度和缩放容易和用户 JSON 重复或冲突。

结论：这条路线只能调近似，不可能真正做到“绑定模型按 Blockbench 模板摆”。

### 路线 2：绑定路径读取 display，移除多余 Java 翻转

之后修过：

- 移除 GUI 额外 180 度。
- 把 Java 模型映射进模板 0..16 坐标。
- 解析 `display.head`。
- 同步 fallback display 值。

这解决了一部分方向问题，但本质仍然是：

`player_doll_3d.json display` + `PlayerDollModel Java body`

不是：

`player_doll_3d.json display` + `player_doll_3d.json elements`

所以用户继续看到绑定模型无法完全跟随 BB 模板。

结论：这仍然是错位架构，只是错位程度变小。

### 路线 3：手写解析 `player_doll_3d.json` elements 并渲染 cuboid

最近一次实现把绑定路径改为：

- 解析 `player_doll_3d.json` 的 `elements`。
- 解析 `from` / `to`。
- 解析 face UV。
- 解析 texture alias。
- 解析 element rotation。
- 手写 `VertexConsumer` 渲染 cuboid。
- 玩家身体部件使用玩家皮肤。
- 装饰部件使用模板贴图。

这一步方向是正确的：绑定模型终于开始使用 Blockbench 模板几何。

但它引出了下一层问题：我们开始手写 vanilla item model renderer 的一部分，于是必须处理：

- Minecraft item model texture alias 是 atlas sprite id，不一定是直接 PNG texture。
- face UV 的含义是 sprite 局部 UV，不是直接任意 texture 的像素坐标。
- atlas sprite 渲染和 direct texture 渲染的 UV 取值不同。
- element rotation 的变换顺序必须和 vanilla baked model 一致。
- display transform 的变换顺序也必须和 vanilla 一致。
- `cullface`、tint、shade、ambient occlusion 等 baked model 细节可能被忽略。

截图中的黑紫块就是这条路线暴露出的典型问题：绑定路径把 item model texture alias 当成了 direct texture 使用，Minecraft 找不到或不能按预期绑定，于是显示 missing texture。

结论：这条路线更接近目标，但风险是我们正在重写 Minecraft baked item model renderer。

## 当前工作区状态

注意：当前工作区里已有一个由 subagent 完成但尚未经过完整 review/deploy 的 missing-texture 修复。

该修复报告如下：

- 新增 `TemplateTextureAliases.java`。
- 修改 `PlayerDollItemRenderer.java`。
- 修改 `platforms/neoforge-1.21.1/build.gradle`，给 NeoForge platform 测试加了 JUnit/classpath。
- 新增 `PlayerDollItemRendererTest.java`。
- 将模板 texture alias 保留为 atlas sprite id，例如 `soulbound_dolls:item/player_doll_dark`。
- 对非玩家装饰面改用 `TextureAtlas.LOCATION_BLOCKS` 上的 sprite。
- 玩家皮肤部件继续用 direct player skin texture。
- subagent 报告：targeted test 和 platform build 通过。

但这个修复还没有完成当前项目工作流中的：

- spec review。
- code quality review。
- coordinator 重新 build。
- 部署到 Mechanomania。
- verification log 记录。

因此明天研究时要把它当成“工作区候选修复”，不是已验证发布版本。

## 为什么“按理来说简单”的事在这里不简单

如果是普通静态物品，确实简单：

```text
Blockbench JSON -> Minecraft model bakery -> BakedModel -> ItemRenderer -> 正确显示
```

但绑定玩家玩偶要求“同一个模型几何 + 动态玩家皮肤”。这让问题变成：

```text
Blockbench JSON 几何
+ 玩家皮肤动态贴图
+ item display transform
+ item model atlas texture
+ custom renderer
```

普通 Minecraft item JSON 不会自动把玩家皮肤动态套到某个 item model 的指定 cuboid 上。

所以我们选择自定义 renderer 后，就脱离了 vanilla 自动处理的部分。每脱离一部分，就要自己复刻一部分：

- display transform
- element geometry
- element rotation
- face UV
- texture atlas sprite
- resource reload
- render type / buffer

之前的问题不是 BB JSON 写错了，而是绑定路径没有走 BB JSON 的完整语义。

## 最可能的正确长期方案

下面几个方向建议明天优先研究。

### 方案 A：保留 custom renderer，但用 vanilla baked model 处理静态模板，再只替换玩家皮肤层

目标：尽量不要手写 item model renderer。

思路：

- 让 `player_doll_3d.json` 继续被 Minecraft 烘焙成 `BakedModel`。
- 对装饰/非玩家贴图部分，直接调用 vanilla `ItemRenderer.renderModelLists(...)` 或等价路径。
- 只对需要玩家皮肤的部件额外叠加一层自定义渲染。

优点：

- Blockbench display/element/rotation/atlas 行为由 vanilla 保证。
- 装饰贴图不会 missing texture。

缺点：

- 玩家皮肤部件和静态模板部件可能重叠，需要把模板拆层或标记哪些 element 不由 baked model 渲染。
- Minecraft 的 `BakedModel` 不容易按 element name 选择性剔除。

### 方案 B：拆两个 JSON：基础装饰 baked model + 玩家皮肤 dynamic overlay

目标：减少手写渲染范围。

思路：

- `player_doll_3d.json` 仍是总模板或展示模板。
- 派生/手工维护一个只含装饰部分的 baked model。
- 玩家皮肤身体部分由 custom renderer 根据同一模板或单独 mapping 渲染。

优点：

- 静态贴图由 vanilla 处理。
- 玩家皮肤层独立。

缺点：

- 违背“一个 BB 文件控制全部”的理想。
- 需要同步两个模型或做构建期生成。

### 方案 C：继续手写 renderer，但完整实现 atlas sprite UV

目标：接受 custom renderer 负责全部模板渲染。

必须查清：

- 1.21.1 中 item model texture alias 对应哪个 atlas。
- `Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(spriteId)` 是否是正确拿法。
- 对 atlas sprite，`uv` 应通过 `sprite.getU(...)` / `sprite.getV(...)` 映射，而不是简单 `u / 16`。
- 玩家皮肤 direct texture 则继续使用 `u / 64`、`v / 64`。

优点：

- 一个 JSON 可控性最强。
- 可以按 element name 把某些部件映射到玩家皮肤。

缺点：

- 维护成本高。
- 要持续追 vanilla baked model 行为。
- 任何 UV/旋转/RenderType 细节都可能出错。

### 方案 D：研究是否能做动态材质/运行时生成贴图，让普通 baked model 直接使用玩家皮肤贴图

目标：回到 vanilla baked model pipeline。

思路：

- 不手写 cuboid。
- 动态生成/注册一张 item texture，把玩家皮肤按模板需要的方式贴到 `#skin` 所需区域。
- 然后让 baked item model 正常渲染。

优点：

- `display`、`elements`、rotation、atlas 都完全交给 vanilla。
- 最符合“BB JSON 控制摆放”。

缺点：

- 需要研究动态纹理注册和每个 ItemStack/Profile 的纹理缓存。
- 同时显示多个玩家玩偶时需要多张动态贴图或 atlas 外 texture 处理。

## 明天建议的调查顺序

1. 先确认 NeoForge 1.21.1 custom item renderer 是否能安全调用 vanilla `ItemRenderer` 渲染某个 `BakedModel`，且不重复套 `display`。
2. 查 `BakedModel` / `BakedQuad` 是否能访问来源 face / texture / tint / direction，但通常拿不到 Blockbench element name。
3. 查 item model texture alias 的正确 sprite atlas：`TextureAtlas.LOCATION_BLOCKS` 是否覆盖 `textures/item/*`。
4. 查 `TextureAtlasSprite` 在 1.21.1 的 UV API：如何把 JSON face UV `[0, 0, 16, 16]` 映射到 atlas sprite 上。
5. 查是否有现成 mod 范例：动态玩家头、custom BEWLR、item model + dynamic texture。
6. 决定长期方案：手写 renderer 继续完善，还是回到 vanilla baked model pipeline + 动态贴图。

## 判断标准

真正解决后应满足：

- 改 `player_doll_3d.json` 的 `display.gui`，绑定和未绑定物品栏同步变化。
- 改 `firstperson_righthand`，绑定和未绑定手持同步变化。
- 改某个 element 的 `from/to/rotation`，绑定和未绑定几何同步变化。
- `F3+T` 后绑定模型重新读取新 JSON。
- 不出现 missing texture 黑紫块。
- 玩家皮肤头、身体、左右手、左右腿 UV 方向正确。

如果只能满足 display 同步，但 element 不同步，说明还停留在路线 1/2。

如果 element 同步但贴图 missing，说明路线 3 的 atlas/UV 部分还没完成。

## 当前建议结论

短期：可以继续修当前手写 renderer 的 atlas sprite UV 问题，先消除黑紫块。

长期：更值得研究“让 vanilla baked model 负责摆放和静态贴图，只解决玩家皮肤动态贴图”这条路。因为用户真正想要的是 Blockbench JSON 指挥 Minecraft 如何摆放，而不是我们在 Java 里复刻一个不完全的模型渲染器。
