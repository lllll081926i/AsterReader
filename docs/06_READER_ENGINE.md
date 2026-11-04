# 06 阅读引擎设计

## 总体策略：两类渲染内核，一个统一 Session

### A. Reflow/Text Engine

适合 EPUB、FB2、MOBI/KF8、TXT、MD、HTML。

能力：

- 章节/目录
- 分页与连续滚动
- CSS/字体/段落排版
- 文本选择
- 全文搜索
- 高亮/笔记
- TTS

### B. Fixed/Image Engine

适合图片目录、CBZ/CBR/CB7/CBT、扫描 PDF、固定版式 EPUB。

能力：

- 逐页图像
- 横翻/竖翻/条漫
- RTL
- 双页
- Zoom/Pan
- 裁边与 fit policy
- 预取

## EPUB

建议使用 Readium Kotlin Toolkit 作为兼容性底座。原因：EPUB 2/3、固定版式、RTL、搜索、TTS、偏好等已有成熟实现，且许可证宽松。

注意：Readium 的 Compose-based web navigator 仍可能处于演进状态，因此业务层必须通过 `engine:readium` 适配，不让 feature 依赖其具体 Navigator 类型。

## FB2 / MOBI / TXT / Markdown

可走自有文本 pipeline，参考 FrogReader 的能力边界：

`bytes -> encoding -> parser -> semantic blocks -> style tree -> layout -> page cache`

MOBI/KF8 推荐两条路线：

1. 审查并复用 MIT 许可实现思路/代码，保留 NOTICE；
2. 独立实现 PalmDoc/PDB/MOBI/KF8 解析。

不建议把一个 LGPL native 库直接定为首发硬依赖，除非团队明确接受对应合规成本。

## PDF

PDF 必须区分两种 UX：

- **Document mode**：保留 PDF 页，支持目录、文本选择能力（视引擎能力）。
- **Comic mode**：按图像页优化，强调预读、双页、RTL、裁边。

底层尽量共用 PDFium/Readium adapter，避免打包两个完整 PDF engine。

## 漫画压缩包

### ZIP / CBZ

- 首选随机访问 ZipFile。
- 读取中央目录并自然排序 entry。
- 不全量解压。

### RAR / CBR、7Z / CB7、TAR / CBT

- 通过 libarchive adapter。
- 建立 entry index。
- 对不适合随机访问的格式采用“窗口缓存”：当前位置前 2 页、后 6–12 页。
- 对 solid archive 明确提示首次跳页可能较慢。

## 图片目录

支持 JPG/JPEG/PNG/WEBP/BMP/HEIC/GIF。动画 GIF 首发按静态首帧显示；后续可选原动图。

排序规则默认 natural sort：`1, 2, 10` 而不是 `1, 10, 2`；支持用户覆盖。

## Locator 设计

统一 Locator 不能只存“页码”。建议：

```text
Locator
- publicationId
- resourceId/chapterId
- progression: 0..1
- position: optional integer
- textAnchor: optional (cfi/css selector/text quote)
- imagePageId: optional stable entry id
- timestamp
```

这样 EPUB 重排后仍可尽量恢复，漫画重命名后也能通过 stable entry id 匹配。

## 分页缓存

文字分页缓存 key 至少包含：

`publication hash + chapter + viewport + font + size + line height + margins + theme-affecting-layout flags`

主题颜色变化不应让分页缓存失效；字号变化必须失效。

## Reader 预取

漫画：

- 当前页高优先级。
- 后 2 页立即预解码缩略/适配尺寸。
- 后 6–12 页做 I/O 预取。
- 反向 2 页保留。
- 内存压力时先丢全分辨率 bitmap，再丢缩略。

文字：预解析下一章，分页只对邻近页做 warmup。

## 大图策略

禁止默认按原始分辨率整图解码。根据 viewport 计算目标采样尺寸；放大时再触发 tile/region decode。PDF 采用 page tile/cache，而不是一次 render 超大 bitmap。

## 阅读进度可靠性

写入时机：

- 翻页/滚动 debounce 1–2 秒
- Activity `onStop`
- Session close
- 切换章节
- 进入后台

数据库写入使用单独 coroutine dispatcher；UI 不等待落盘。

## v2.0 SecureDocumentSource

Reader Engine 不知道“这是明文文件还是 Vault 文件”。统一接收 `PublicationInput/DocumentSource` 抽象。

受保护内容链路：

```text
ReaderSession
 -> Engine
 -> SecureDocumentSource
 -> DecryptingSeekableSource
 -> encrypted ASV1 chunks
```

硬规则：

- Engine 不拿 raw key。
- Engine 不要求永久明文 path。
- PDF/Archive adapter 优先支持 seekable/random read。
- 若第三方 engine 强制 real plaintext file，必须先做 security architecture review；默认不允许用于 Vault 内容。
- Reader close 时关闭 SecureDocumentSource，并释放明文 page/text caches。
