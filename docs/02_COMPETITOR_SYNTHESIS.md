# 02 竞品融合与取舍

## 结论先行

三者不是简单取并集，而应形成以下分工：

| 来源 | 最值得吸收 | 不建议照搬 |
|---|---|---|
| Book's Story | Compose/Material You、简洁本地书库、多文本格式、阅读自定义 | GPL 源码直接复用；单体项目结构直接照搬 |
| FrogReader | 自定义解析思路、全文搜索、元数据编辑、分页缓存、备份、数据安全 | Alpha 阶段尚未稳定的 UI/内部 API 直接依赖 |
| MH-ARK | 仓库模型、压缩包/图片/PDF 漫画管理、WebDAV 流式、本地优先、详情页与阅读控制岛、MD3E 风格 | 不确认开源许可的代码；把漫画目录规则写死 |

## Book's Story 给我们的启发

公开信息显示它是 Kotlin + Jetpack Compose 的 Material You 本地电子书阅读器，支持 PDF/TXT/EPUB/FB2/HTML/MD，并使用 Storage Access Framework。它最大的价值不是某个独家功能，而是证明了“原生 Compose + 本地文件 + 干净 UI”在阅读器上是成立的。

我们的使用方式：

- 借鉴产品行为和信息结构。
- 视觉上保留 Material You 的轻量感，但升级为更统一的 M3 Expressive 组件体系。
- **不把其 GPL-3.0 源码作为默认代码基础**，否则整个派生项目的许可证策略会被锁定。

## FrogReader 给我们的启发

FrogReader 的价值更偏“工程内核”：

- 自定义 EPUB/FB2/MOBI/TXT/Markdown 解析管线。
- 分页/连续模式。
- 全文搜索、目录、选择、引用、笔记。
- 元数据编辑采用安全的 copy-on-write。
- 数据与原文件分离，支持 data-only/full backup。
- WorkManager 定期备份与快照轮转。

我们的使用方式：

- 其 MIT 许可更适合按模块审查后复用思路或代码。
- 重点参考它对“用户数据”和“原书文件”的分离方式。
- 将解析层放到 `engine-*`，不要与 UI 绑定。

## MH-ARK 截图给我们的启发

### 仓库而不是“导入文件夹”

截图里的“本地仓库管理 / 远程仓库管理”非常适合大规模本地收藏。仓库是长期存在的 Source，有名字、有扫描策略、有缓存状态、有独立刷新，而不是一次性导入。

### 主页信息密度控制

截图主页：顶部搜索胶囊 + 筛选按钮 + 当前范围标题 + 内容区 + 右下 FAB + 底部导航。空库时用插画而不是堆说明文字。这个信息密度很舒服，值得保留。

### 书架页的“快速入口卡片”

全部 / 收藏 / 历史 / 书签做成 2×2 大卡片，比藏在筛选器里直观。下面再放用户自定义书架。

### 设置页分组卡片

仓库管理、漫画管理、其他，用圆角 Surface 分组而不是传统 Preference 列表，适合 MD3E，也非常适合平板双栏。

### 详情页底部控制岛

封面 + 标题 + 更新时间/状态 + 目录卡片 + 底部浮动工具岛。操作层级清楚。我们的改进：工具岛根据内容类型自适应，不固定 5 个图标。

### 阅读器 Overlay

顶部标题栏、侧面锁定按钮、底部进度条+控制岛的方式非常适合漫画。文字书也可复用壳层，但工具项换成排版/TTS/目录/书签。

## 最终融合后的产品结构

```
统一资料库
├─ 本地仓库 LocalSource
├─ WebDAV 仓库 WebDavSource
├─ 书架 / 标签 / 收藏 / 历史
└─ Publication
   ├─ Text/Reflow Engine
   │  ├─ EPUB
   │  ├─ FB2
   │  ├─ MOBI/KF8
   │  ├─ TXT/MD/HTML
   │  └─ PDF(Text mode, optional)
   └─ Fixed/Image Engine
      ├─ Image Folder
      ├─ CBZ/ZIP
      ├─ CBR/RAR
      ├─ CB7/7Z
      ├─ CBT/TAR
      └─ PDF(Comic mode)
```

## 我们要比三者再多做的一层

真正拉开差距的是“**统一 Locator 和 Source/Engine 接口**”：无论内容来自本地文件夹还是 WebDAV，无论用文字引擎还是漫画引擎，书签、进度、详情页、历史记录都只认统一的 `PublicationId + Locator`，这样以后新增格式不会重写整个 App。
