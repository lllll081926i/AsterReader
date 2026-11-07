# Aster Reader — Design System / `design.md`

> 版本：Final v3.0 · 2026-09-29
> 本文件是 **整个应用唯一的视觉、动效、交互设计真源（single source of truth）**。任何新页面、组件、动画、主题、平板适配都先改本文件，再写代码。

## 0. 设计总纲

Aster Reader 的视觉方向不是“做得像某个现有阅读器”，而是把以下优点重新组织成一致的 Android 原生体验：

- MH-ARK 截图中的低噪声、圆润、轻量、留白充分的 Material 风格。
- Book's Story 的 Material You / Compose 原生感。
- FrogReader 的现代 Android 信息结构。
- Material 3 Expressive 的动态形状、MotionScheme、共享元素和响应式组件。

最终设计关键词：**安静、柔和、可预测、现代、原生、可触摸、内容优先。**

### 0.1 绝对规则

1. **Android 12+ 全局默认使用 Monet / Dynamic Color。** 不另外发明一套强品牌主色去覆盖系统壁纸颜色。
2. UI 主题与“书页主题”分离：用户壁纸变色，不应随意改变漫画阅读背景。
3. 所有页面默认 edge-to-edge；所有交互必须避让 system bars / display cutout / gesture insets。
4. 所有主要动效统一从 Material Theme 的 motion token 读取，禁止每个页面自己随手写 `tween(300)`。
5. 任何安全锁定内容，在认证前不得先显示真实封面、标题、缩略图，再盖一层模糊。
6. 触控目标不小于 48dp；图标必须有语义；高字体缩放仍可操作。
7. 手机、横屏、平板、折叠屏是同一设计系统，不是四套 UI。

---

## 1. 全局主题：Monet Dynamic Color

### 1.1 Android 12+

使用：

- `dynamicLightColorScheme(context)`
- `dynamicDarkColorScheme(context)`

应用启动时由 `AsterTheme` 在最外层统一决定颜色方案。

默认策略：

```text
系统浅色 -> dynamicLightColorScheme(context)
系统深色 -> dynamicDarkColorScheme(context)
系统不支持动态色/厂商实现异常 -> Aster emergency fallback scheme
```

### 1.2 Emergency fallback（仅异常兜底）

项目最低版本固定为 **Android 12 / API 31**，正常设备均应使用系统 Monet Dynamic Color。仍保留一套 Aster emergency fallback，仅用于自动化测试、OEM 动态色实现异常或 API 返回失败等极端情况；它不是 Android 11 及以下兼容主题，也不得作为默认主题入口。

要求：

- fallback 只能由 Theme 层自动触发，不暴露为“品牌主题”覆盖 Monet。
- 不超过 1 个 seed；Surface 系列优先于高饱和品牌色。
- 深色模式避免纯黑大面积卡片；仅 Reader 的 OLED 黑色阅读主题可以使用纯黑。

### 1.3 颜色角色

不在业务代码写十六进制颜色。业务组件只能读取语义 token：

- `primary / onPrimary`
- `primaryContainer / onPrimaryContainer`
- `secondaryContainer`
- `tertiaryContainer`
- `surface`
- `surfaceContainerLow`
- `surfaceContainer`
- `surfaceContainerHigh`
- `surfaceContainerHighest`
- `outline / outlineVariant`
- `error / errorContainer`

推荐对应：

| 场景 | 颜色角色 |
|---|---|
| 首页背景 | `surface` |
| 大搜索条 | `surfaceContainer` |
| 快速入口卡片 | `secondaryContainer` 或 `surfaceContainerHigh` |
| 当前导航项 | `secondaryContainer` |
| 主 FAB | `primaryContainer` |
| Reader Control Island | `surfaceContainerHigh` / 半透明 tonal |
| 锁定状态徽章 | `tertiaryContainer` |
| 危险删除 | `errorContainer` |

---

## 2. Shape System

全部圆角来自 `AsterShapes`，禁止页面硬编码。

| Token | 建议 | 用途 |
|---|---:|---|
| `shapeXs` | 6dp | 极小标签、封面内状态 |
| `shapeSm` | 10dp | 封面、窄按钮 |
| `shapeMd` | 16dp | 普通卡片 |
| `shapeLg` | 24dp | Settings group、空状态卡 |
| `shapeXl` | 28–32dp | SearchBar、Bottom Sheet 顶部 |
| `shapePill` | 50% | Chips、Reader Control Island、导航选中态 |

M3 Expressive 组件支持动态 shape 时，优先使用组件官方 shape API；自定义 shape morph 必须复用统一 motion token。

---

## 3. Spacing / Layout Grid

使用 4dp 基础网格。

- 4dp：图标内部细间距
- 8dp：同组紧凑元素
- 12dp：chip / metadata
- 16dp：标准页面边距（Compact）
- 20dp：中等窗口页面边距
- 24dp：大卡片内部边距
- 32dp：大区块间距
- 40–48dp：Hero / 空状态呼吸空间

手机首页不允许内容贴边。平板不要简单把 16dp 拉到全宽；用内容最大宽度与 pane scaffold。

---

## 4. Typography

2026-11-07 用户校正：所有页面标题栏使用 `titleLarge`（18sp/24sp），最小高度 52dp，按钮触控区 48dp；禁止额外堆叠页头说明。设置行统一最小 52dp，200% 字体允许自然增高。设置组间距 12dp，页面左右 16dp，内容结束后 12dp；系统 Insets 与底栏实际占用单独处理，禁止用大块固定空白补偿。普通设置行不显示解释性副标题；当前值使用弱化文字，风险说明放入具体操作确认。独立设置功能使用导航页面，确认、简短输入和即时阅读工具可使用对话框或抽屉。

底部主导航保留原有 Apple 风格悬浮岛型 Tab，使用圆润独立胶囊容器和胶囊选中态，默认显示“书库 / 书架 / 搜索 / 统计”四项，统计可在设置隐藏，不得替换为全宽 Material NavigationBar。岛体最小高度 64dp，左右至少 16dp，距离系统导航安全区 8dp；大字体允许自然增高。系统导航 Insets 单独叠加，内容按岛体实际高度避让。设置与属性表单最大宽度 840dp，宽窗口居中，禁止将短文本与开关拉到屏幕两端。空状态按钮支持换行排列。详情阅读操作区参与页面测量，禁止覆盖最后一条内容。

2026-11-07 用户再次校正：移除 Reader 右侧放大、缩小、重置按钮，继续保留双击、双指缩放与 TalkBack 缩放动作。阅读浮层全部使用 Monet，漫画画布保持独立背景。右下状态使用 9sp 小字，距离右下安全区 2dp，不遮挡系统手势和切口。底部操作岛缩小间距，触控仍至少 48dp。末页按最后跨页或滚动实际末端判断完成，完成状态与当前位置分别保存，回翻不自动撤销读完。

并列双按钮默认等宽、同高、相同内边距，优先使用“语义图标＋文字”。主次层级通过填充与描边区分，不通过宽度区分。首页导入文件与添加目录分别使用文件和文件夹图标。可用宽度不足或大字体时整组改为纵向等宽排列，文字允许换行。例外需明确说明用途。

UI 使用系统字体与 Material 3 Typography；漫画页面保持原图。

2026-11-07 设置选择校正：2–3 项互斥选项采用官方 Material3 带图标分段按钮；4 项以上、宽度不足或大字体采用整行点选与右侧勾选。开关保留官方 Switch。设置分组采用动态 primaryContainer 与 surfaceContainerLow 派生的低饱和容器色，图标、当前值与选中态使用动态色角色。正文保持 onSurface 可读性。密码输入不保存至状态恢复或磁盘。主导航增加“统计”，设置可独立隐藏该 Tab，已有阅读记录不删除；隐藏当前统计 Tab 时回到书库。加密公开清单为独立设置页，不包含漫画文件和隐藏作品，不冒充 Vault。

### 4.1 UI 层级

- App/Screen Title：`headlineSmall` / `titleLarge`
- Section Title：`titleMedium`
- Card Primary：`titleMedium`
- Metadata：`bodyMedium`
- Secondary Metadata：`bodySmall`
- Chips：`labelLarge`

### 4.2 漫画阅读画布

漫画使用原始页面图像。页码和工具栏使用 UI Typography。漫画背景使用阅读专用色，不随壁纸随意变化，不提供正文排版设置。

---

## 5. Motion System

### 5.1 总原则

项目把“动画”作为交互语义，不是装饰。

优先使用 Material 3 的 `MotionScheme`：

```kotlin
MaterialTheme(
    colorScheme = colorScheme,
    typography = typography,
    shapes = shapes,
    motionScheme = MotionScheme.expressive(),
) { content() }
```

截至 2026-09-23，Material3 稳定版为 `1.4.0`，`MotionScheme` / `MotionScheme.expressive()` 位于 `1.5.0-alpha29`。因此项目采用 **稳定核心 + 单点实验封装** 策略：

- 业务 Feature 不直接依赖 alpha API；只通过 `:core:designsystem` 中的 `AsterMotion` / `AsterTheme` 使用。
- 开发通道可锁定 `1.5.0-alpha29` 以获得 Expressive Motion；升级必须单独 PR。
- 若 alpha 出现阻塞回归，Design System 可切回 Material3 `1.4.0`，Feature 无需改动。

### 5.2 动画分类

#### Effects Motion
不改变布局空间：颜色、alpha、阴影、icon 状态。

使用 `fastEffectsSpec / defaultEffectsSpec / slowEffectsSpec`。

#### Spatial Motion
改变位置、尺寸、容器边界、共享元素。

使用 `fastSpatialSpec / defaultSpatialSpec / slowSpatialSpec`。

### 5.3 统一动画语言

| 交互 | 设计 |
|---|---|
| 按压 | tonal/ripple + 轻微 shape/scale 响应，绝不整卡夸张弹跳 |
| 搜索展开 | 使用 M3 SearchBar 原生展开与 predictive back |
| 书封面 -> 详情 | `sharedElement/sharedBounds`，封面为主要共享元素 |
| FAB -> 添加来源 | FAB 与 BottomSheet/页面保持空间连续感 |
| 列表/网格重排 | animate item placement；不要逐项延迟造成“瀑布慢动作” |
| 详情 -> 阅读 | 内容淡出 + Reader surface 接管，不强行做复杂 3D 翻转 |
| Reader Controls | 下方 Control Island 轻微位移 + fade；顶部栏同步出现 |
| 设置开关 | 使用官方 M3 Switch 动画，不自定义轨道 |
| 解锁成功 | 锁屏轻微缩小/淡出，主界面淡入；认证前不参与 shared element |
| 加密迁移 | 进度卡片展开、成功后锁图标 shape/颜色状态变化 |

### 5.4 Predictive Back

必须完整支持 Android 预测性返回：

- 不在根 Activity 粗暴拦截 Back。
- Navigation 3 负责导航回退。
- SearchBar、ModalBottomSheet 等使用 Material3 自带 predictive back 支持。
- Detail -> Library 的共享元素应与 back gesture 进度保持一致。

### 5.5 Reduced Motion

提供：

```text
动效强度：跟随系统 / 减少动效 / 完整动效
```

`减少动效` 下：

- 禁止长距离 spring overshoot。
- shared element 可退化为短 fade/size。
- 无限动画全部停用。
- Reader 翻页允许设置为“无动画”。

测试必须覆盖 motion duration scale = 0。

---

## 6. Haptics

触觉反馈只用于“状态确认”，不是每点一下都震。

允许：

- 长按进入多选。
- 书签成功。
- 拖动进度条跨章节边界。
- 加密/解锁成功或失败。
- Reader 锁定/解锁。

禁止：

- 普通列表滚动。
- 每次翻页。
- 每次 chip 点击都强震。

设置里允许总开关。

---

## 7. 导航设计

### Compact phone

顶级目的地：

1. 首页 / Library
2. 书架 / Shelves

设置从 Overflow / Navigation Drawer / profile-less app menu 进入。

### Medium / Expanded

使用 `NavigationSuiteScaffold` / Material3 Adaptive：

- 手机：Bottom Navigation
- 平板/横屏：Navigation Rail
- 大屏：可扩展 Wide Rail / supporting pane

详情页优先 List-Detail 双栏：左侧书库，右侧详情；阅读器始终独占主要阅读区域。

---

## 8. 页面视觉规范

### 8.1 首页

沿用截图优点：

```text
System Bars
Large Search Capsule + filter badge
Scope title + overflow
Library grid/list
FAB
Bottom navigation
```

空状态允许插画，但最多 1 个主标题 + 1 个辅助说明 + 1 个主动作。

### 8.2 书架

顶部标题“书架”，右上 `+`。

固定 2×2 Quick Shelf：

- 全部
- 收藏
- 历史
- 书签

下方再显示用户书架。平板变为更宽的卡片网格，不无限拉伸。

### 8.3 搜索

搜索栏进入 expanded search state；筛选 chips 横向可滚动：

- 随机推荐
- 排序
- 阅读状态
- 来源/仓库
- 格式
- 标签

受保护内容未解锁时不得出现在搜索建议、最近搜索结果封面中。

### 8.4 详情

封面、标题、作者/系列、收藏、阅读状态、来源、目录、继续阅读。

长文件名永远不作为主标题；原始文件名放“文件信息”。

底部动作使用 Floating Toolbar / Control Island 思路，最多 5 个一级动作。

### 8.5 Reader

Reader 是一个“内容画布 + transient chrome”。

- 顶栏：返回 + 章节/标题。
- 中间：纯内容。
- 底部：进度 + Control Island。
- 漫画与文字书共享 Shell，但功能按钮随 capabilities 改变。

漫画常用：显示模式 / 目录 / 书签 / 设置 / 跳页。

### 8.6 Vault / 受保护内容

锁定卡片不显示真实缩略图。显示统一安全占位：

- 锁图标
- “受保护内容”
- 可选数量

解锁后再加载封面和元数据。

---

## 9. 安全相关 UI 设计

三个概念必须使用不同文案：

1. **应用锁**：进入 App 前认证。
2. **访问锁**：只在 App 内隐藏/要求认证，不改变原文件。
3. **加密并上锁**：文件迁移至 Vault，磁盘上保存为密文。

默认给用户的主动作应是 **“加密并上锁”**，因为它才满足“锁定后文件也加密”的语义。

不得把“仅 UI 隐藏”伪装成“加密”。

---

## 10. 可访问性

- 触控目标 >= 48dp。
- 支持 TalkBack 顺序与自定义动作。
- 不依赖颜色单独表达锁定/阅读状态。
- 200% 字体至少保持核心操作可达；推荐测试更高字号。
- 进度 Slider 有语义值。
- 漫画缩放与文本字号均提供显式按钮，不能只靠手势。
- 动画关闭后状态转换仍清晰。

---

## 11. 设计代码边界

唯一入口：

```text
:core:designsystem
  ├─ AsterTheme.kt
  ├─ Color.kt
  ├─ Typography.kt
  ├─ Shapes.kt
  ├─ Motion.kt
  ├─ Dimens.kt
  ├─ Haptics.kt
  ├─ Icons.kt
  └─ components/
```

Feature 模块禁止自己定义“另一套主题”。Reader 允许覆盖“内容主题”，但 Reader chrome 仍来自 AsterTheme。

---

## 12. 官方依据

- Material 3 in Compose: https://developer.android.com/develop/ui/compose/designsystems/material3
- Dynamic Color: https://developer.android.com/develop/ui/compose/designsystems/material3
- Material3 releases: https://developer.android.com/jetpack/androidx/releases/compose-material3
- MotionScheme API: https://developer.android.com/reference/kotlin/androidx/compose/material3/MotionScheme
- Shared elements: https://developer.android.com/develop/ui/compose/animation/shared-elements
- Predictive back: https://developer.android.com/develop/ui/compose/system/predictive-back-setup
- Adaptive navigation: https://developer.android.com/develop/adaptive-apps/guides/build-adaptive-navigation
- Edge-to-edge: https://developer.android.com/develop/ui/compose/system/setup-e2e


---

## 13. 完整页面设计实现手册（v4 UI Freeze 增补，2026-11-04）

**全页面唯一交互与视觉布局验收细则：**[`docs/36_COMPLETE_UI_UX_SPEC.md`](docs/36_COMPLETE_UI_UX_SPEC.md)。

两个文件分工明确：本 `design.md` 决定 Design Token（颜色、圆角、间距、Typography、Motion、无障碍等）；`36_COMPLETE_UI_UX_SPEC.md` 决定每一张屏幕的具体内容、层级、动作、各状态、转场、流程及验收。页面实施不得绕过 `design.md`；如两文件视觉规格不一致，先提交设计变更并同步修订。

开发顺序遵循 UI-W0～UI-W6（远程 UI-W7 为 1.x），对照 `planning/UI_SCREEN_TASK_MATRIX.csv` 和已有 S0–S28 开发计划。

## 漫画核心体验补充（2026-11-04）

R-01/R-02/R-03：保存真实页码、条漫页内比例、缩放与归一化平移。旋转或进程重建恢复；主动跳页重置缩放。加载与错误使用中性状态，锁定立即撤销内容。缩放优先以触点为中心，图像边缘允许父级翻页；系统返回边缘保留。

R-04：单本设置覆盖全局默认，提供恢复全局默认。双页阅读方向独立设置。支持将当前页设为封面，敏感作品拒绝生成普通缩略图。系列名称和卷序由用户确认，下一卷仅以显式按钮打开，不强制跳转。

I-03/I-05/I-06、P-01：仓库支持漫画文件与图片目录；扫描完成后标记失联，不自动移除阅读记录。权限失败和取消不据此认定文件删除。失联作品提供重新选择文件的入口；新文件解析成功后才替换来源，保留作品 ID、书签与阅读状态。重新扫描恢复可访问状态。

所有新增动作使用 MaterialTheme 令牌、标准点击与可访问标签。加载、空、错误、锁定均须核验；设备截图与 TalkBack 验收仍为独立门禁。

2026-11-07 平板适配：按实际窗口宽度切换。600dp 起使用可滚动侧栏，三个目的地与手机一致；960dp 起启用书库详情双栏，避免中等窗口内容被挤窄。书库与书架网格按单元最小宽度自适应列数，搜索输入、范围筛选和结果共同限制在 840dp 内并居中。主页面标题栏最小 48dp，可见操作图标 18dp，触控区仍为 48dp。浅色和深色均使用系统 Monet 颜色角色，设置选中项与主页导航统一使用 secondaryContainer/onSecondaryContainer；200% 字号允许按钮组纵向排列。

Monet 为全局固定策略，Theme 不暴露 dynamicColor 开关。设置中的明暗选择只决定动态浅色或动态深色，不切换固定品牌色。

系统状态栏与导航栏图标跟随应用当前明暗主题，不只跟随系统主题。深色页面使用浅色系统栏图标，避免黑字黑底产生整块顶部空白的视觉问题。保留一次真实状态栏安全距离，不叠加固定顶部占位。

2026-11-07 顶部密度再次校正：覆盖此前标题栏 48dp 规则。可见标题栏采用 40dp 最小高度，标题为 18sp/22sp，图标为 18dp；标题栏动作横向占位保持 48dp，垂直可见占位 40dp。仅在公共标题栏内取消 Material 为触控范围预留的额外布局空间，保留 Compose 输入层最小 48dp 触控扩展。不得降低系统最小触控配置。通过触控边界测试核验，不能只检查可见尺寸。大字体允许标题栏增长。正文紧接标题栏，状态栏仍只消费一次。

2026-11-07 阅读设置迁移：设置首页移除默认阅读和诊断入口。漫画阅读设置统一使用“本书 / 默认”选择作用范围。本书可恢复默认；更改默认不清除已有单本覆盖。亮度与音量键翻页沿用全局偏好，状态条沿用本次阅读状态。设置首页分组为常用、内容、隐私、数据；标题保持简短，不添加重复说明。关于页显示真实构建版本、格式、隐私边界及实际依赖许可。许可详情允许保留必要的版权和许可文字。

2026-11-07 阅读设置最终校正：手机和平板均打开独立全屏设置页，不使用上拉面板。返回只关闭设置，保持阅读位置。翻页四项在宽度允许时采用两列等宽选项，最小触控高度 48dp；大字体或窄窗口改为单列。缩放使用等宽分段按钮。全部使用统一 Monet 容器，只有选中项高亮，不做深浅交替。阅读状态条采用 42% 不透明度背景、8sp 字号、4dp/1dp 内边距，无边框和阴影；保持右下安全区域。其他面板的上拉交互不变。

2026-11-07 标题栏统一：以书架页为基准，所有页面标题栏最小高度 48dp，统一消费一次状态栏 Insets。书库标题独立于滚动网格，标题下正文间距 4dp；不得叠加网格顶部留白和标题行间距。搜索输入区同高 48dp，不附加上下 8dp 外边距。标题横向边距 16dp，二级页返回按钮保留 48dp 触控区。大字体允许标题栏增长，禁止裁切文字。

2026-11-07 身份验证：使用一次完整弹出的底部紧凑面板，不设半展开状态和拖动条，不作为可上拉的二级页。面板内容最大宽度 420dp，数字区最大宽度 320dp；按键最小高度 48dp、间距 8dp，使用 Monet 色调容器与系统点击反馈。取消和确认等宽带图标。不足 4 位、冷却及验证中禁用确认；PIN 不以明文进入无障碍语义。短窗口与大字体允许面板内滚动，不依赖上拉面板才能显示操作。
