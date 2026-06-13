# 在 Soulbound Dolls 中使用 mcui-kit

本文档说明如何在本 mod（Soulbound Dolls）中引用并使用 GUI 组件库
[`mcui-kit`](https://github.com/yabo083/mcui-kit)，供 0.1.3 皮肤浏览器界面开发直接照做。

mcui-kit 的设计与本仓库一致：`core` 是纯 Java、零 Minecraft 依赖的组件逻辑；
`platforms/neoforge-1.21.1` 是把组件画到屏幕的薄适配层。我们这边需要解决三件事：
**编译期怎么引用、运行期怎么让玩家拿到代码、开发期怎么保持实时联编**。

---

## 一、三种生命周期，三种接法

| 阶段 | 问题 | 方案 |
| --- | --- | --- |
| 开发期 | 我改 mcui-kit，灵魂玩偶要立刻用上 | **included build**（实时联编，零发布） |
| 编译期 | `build.gradle` 怎么写依赖 | 正规依赖坐标（被 included build 自动覆盖） |
| 运行期 | 玩家游戏里怎么有 mcui-kit 的代码 | **jar-in-jar 内嵌** 或 **独立前置 mod** |

关键认知：**依赖声明的写法是一份（正规坐标），开发时用 included build 让它变敏捷，
发布时切换到真实坐标——同一行声明，两种行为自动切换。**

---

## 二、开发期：included build（推荐用于 0.1.3）

前提：`mcui-kit` 与 `Soulbound Dolls` 都在 `E:\Codes\MC\` 下，互为相邻目录。

### 1. 在本仓库 `settings.gradle` 末尾加一行

```groovy
includeBuild("../mcui-kit")
```

Gradle 会把相邻的 mcui-kit 仓库当作依赖来源实时编译。你改 mcui-kit 的组件代码，
灵魂玩偶**下次构建立即生效**，无需发布、无需复制。

### 2. 在 `platforms/neoforge-1.21.1/build.gradle` 的 dependencies 里声明

```groovy
dependencies {
    // included build 会把这个坐标解析到本地 ../mcui-kit 的 core 模块
    implementation "com.yabo.mcuikit:mcui-kit-core:0.1.0"
}
```

> 注意：我们只依赖 **core**（纯 Java 组件 + 适配接口）。NeoForge 适配类
> （`NeoForgeUiRenderer`/`SbWidget`）有两种获取方式，见第三节。

---

## 三、运行期：让玩家游戏里有这些代码

core 是纯 Java，会被正常编译进依赖图；但 mcui-kit 的 **NeoForge 适配层**
（`NeoForgeUiRenderer`、`SbWidget`、依赖 `GuiGraphics`/`AbstractWidget`）需要在运行时存在。
两条路线，二选一：

### 方案 A：jar-in-jar 内嵌（推荐，玩家只装一个 jar）

用 NeoForge 的 `jarJar` 把 mcui-kit 打包进灵魂玩偶的 jar。玩家只装灵魂玩偶一个文件，
mcui-kit 自动随附。适合「自用工具库」。开发期仍用 included build 联编。
（具体 `jarJar` 依赖声明在接入 0.1.3 时配置，需要 mcui-kit 发布带坐标的 artifact。）

### 方案 B：独立前置 mod（玩家装两个 jar）

把 mcui-kit 作为独立前置 mod 发布，在本 mod 的 `neoforge.mods.toml` 声明依赖：

```toml
[[dependencies.soulbound_dolls]]
modId = "mcui_kit"
type = "required"
versionRange = "[0.1.0,)"
ordering = "AFTER"
side = "CLIENT"
```

缺失时 NeoForge 会提示玩家去装 mcui-kit。像 owo-lib 那样的经典前置库形态。

> 0.1.3 试运行建议先用 **方案 A 内嵌**，省去玩家额外安装；将来 mcui-kit 成熟、
> 想给别的 mod 共用时再转 **方案 B**。

---

## 四、发布期：从本地切到真实坐标

mcui-kit 已配好 `maven-publish`（发布 `com.yabo.mcuikit:mcui-kit-core`）：

- 本地验证：在 mcui-kit 仓库跑 `./gradlew :core:publishToMavenLocal`，
  artifact 进 `~/.m2`，本 mod 加 `mavenLocal()` 仓库即可拉取（无需 included build）。
- 正式发布：配置 `GITHUB_ACTOR` + `GITHUB_TOKEN`（带 `write:packages` 的 PAT）后
  `./gradlew :core:publish` 发到 GitHub Packages，其他人/CI 加该仓库即可依赖。

一旦发布，**移除 `includeBuild` 那一行**，同一句 `implementation` 依赖声明就会从
本地源码切换到真实远程 artifact——无需改任何业务代码。

---

## 五、最小使用示例（NeoForge Screen 内）

```java
// 在你的 Screen.init() 里
NeoForgeUiRenderer renderer = new NeoForgeUiRenderer(this.font);
Theme theme = Theme.dark();

// 皮肤浏览器网格：4 列、间距 4、行高 32
Grid<SkinEntry> grid = new Grid<>(4, 4, 32, (r, t, entry, b, hovered, selected) -> {
    r.drawBorder(b.x(), b.y(), b.width(), b.height(),
            selected ? t.primary() : (hovered ? t.accent() : t.border()));
    // 这里画皮肤预览：用适配层提供的纹理绘制能力（接入时按需在 UiRenderer 扩展）
});
grid.setItems(loadedSkins);
addRenderableWidget(new SbWidget(grid, renderer, theme, x, y, width, height));

// 搜索框
TextField search = TextField.create().placeholder("搜索皮肤...").onChange(this::filter);
addRenderableWidget(new SbWidget(search, renderer, theme, x, y - 24, 200, 18));
```

> 皮肤预览这种 MC 特有渲染（纹理/实体），原则上在 mcui-kit 的**适配层**加一个
> `UiRenderer` 方法来画，core 组件只管布局与交互——保持 core 的跨端纯净。
> 接入 0.1.3 时可用 mcui-kit 的 `mcui-export` skill 生成具体接线代码。

---

## 六、速查

- 想用某个组件却不确定 API → 读 mcui-kit 的 `docs/components.json`，或调 `mcui-export` skill。
- 想把灵魂玩偶里写好的某个组件回流进库 → 调 mcui-kit 的 `mcui-import` skill。
- 组件清单与代码是否同步 → 调 mcui-kit 的 `mcui-catalog` skill。
