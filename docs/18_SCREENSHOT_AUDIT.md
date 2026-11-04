# 18 用户提供的 MH-ARK 截图审计

本节只分析你提供的界面行为和视觉，不推断其内部实现。

## 01 首页

观察：

- 顶部大 Search Bar 占据视觉主导，右侧筛选带 Badge。
- “全部”作为当前范围标题。
- Empty State 插画位置居中偏上，底部留白充分。
- 右下 `+` 作为导入主动作。
- 底部只保留“首页 / 书架”两个一级入口。

我们保留：大搜索、两 Tab、右下 Add FAB、低噪声空状态。

我们调整：底部不额外塞高权重深色圆形按钮；快速阅读/随机阅读改为 FAB speed dial 或 overflow。

## 02 书架

观察：全部/收藏/历史/书签 2×2 大块卡片，认知成本非常低。

我们保留并扩展：下方增加用户自定义书架；平板改为 3–4 列。

## 03 设置

观察：分组标题使用绿色 tone；同组内大圆角容器；左侧统一线性图标。

我们保留：这种 grouped settings 比 Android 传统 Preference 更现代。

我们调整：同组内部减少每行独立圆角，改为共享 Surface + divider，让层级更整洁。

## 04 软件设置

观察：开关尺寸与 M3 视觉一致；“同步时间线路”有二级文本。

我们保留：一级项 + supporting text。

我们增加：每项可展示当前值，而不是必须点进去才能知道。

## 05 开源许可列表

截图中能看到 AndroidX、Coil、Moshi、tabler-icons、lottie、libarchive-android、Libmobi、pdfium、dav4jvm、libzip、OpenCC、MMKV 等依赖。它说明 MH-ARK 对漫画格式/网络/图片/本地存储用了成熟库组合。

我们的策略不是逐个照抄依赖，而是：

- Coil 保留候选。
- libarchive-android 保留候选。
- pdfium/Readium 统一评估，避免重复 PDF 引擎。
- dav4jvm 可作为 WebDAV 实现候选。
- MMKV 不必引入，DataStore + Room 已能覆盖需求。
- Moshi 可避免，若全项目以 kotlinx.serialization 统一更一致。

## 06 搜索

观察：搜索框顶部固定；下面是水平 filter chips；“排序/阅读状态/仓库”直接暴露。

我们保留此模式，并增加格式/标签 chip；筛选状态支持保存为 Smart Shelf（P1）。

## 07 本地仓库文档

截图显示支持：图片目录、ZIP/CBZ、7Z/CB7、RAR/CBR、TAR/CBT、EPUB、MOBI、AZW3、PDF。这正是统一漫画引擎应覆盖的格式边界。

我们的改进：不要依赖固定文件夹命名规则，扫描前提供 Profile 和预览。

## 08 详情页

观察：封面+大标题、收藏按钮、更新时间/阅读状态、目录大卡片、底部 action island。

问题：原始文件名/哈希直接作为标题时过长。

我们的改进：导入阶段做 title sanitizer；详情页标题优先元数据，原文件名下沉到“文件信息”。

## 09 阅读器

观察：内容全屏、顶部标题、侧面锁、底部进度与工具岛，符合漫画阅读场景。

我们保留壳层，并让 tool island 根据 engine capabilities 动态变化：

- 漫画：页面模式 / 目录 / 书签 / 显示设置。
- 文字：排版 / 目录 / 书签 / 搜索 / TTS。

Reader Shell 应统一，避免每个格式各写一套手势和 overlay。
