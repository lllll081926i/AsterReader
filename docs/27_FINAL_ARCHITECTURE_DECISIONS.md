# 27 Final Architecture Decisions — 架构冻结清单

> 状态：**FINAL / 开发前冻结** · 2026-09-29
> 目的：把后续最容易反复争论、最容易造成返工的决策提前固定。普通 Feature PR 不得修改这些决策；确需修改时必须新建 ADR、写迁移影响和回滚方案。

## ADR-001 平台基线

- `minSdk = 31` — Android 12。
- `targetSdk = 37` — Android 17。
- `compileSdk = 37`。
- Android 12 必须具备完整核心体验；Android 13–17 只做渐进增强。
- 不为 Android 11 及以下维护主题、权限、存储、返回手势或备份兼容分支。

原因：Android 12 是 Monet/Dynamic Color 的原生起点，正好与本项目的 Material You 设计目标重合；减少旧系统兼容分支能显著降低首个大型 Android 项目的复杂度。

## ADR-002 Android 原生技术栈

- Kotlin。
- 单 Activity + Jetpack Compose。
- Material 3；Material 3 Expressive API 只能通过 `:core:designsystem` 封装。
- Navigation 3。
- Material3 Adaptive / adaptive-navigation3。
- Coroutines + Flow。
- Hilt 作为 DI。
- KSP 用于 Room/Hilt 等代码生成。

禁止：主业务 WebView 化、Flutter/React Native 混入、Feature 自建 Service Locator。

## ADR-003 官方推荐分层 + 强边界

遵循 Android 官方推荐的分层架构：

```text
Compose UI
  ↓ events / ↑ UiState
ViewModel / StateHolder
  ↓
UseCase（只在复杂或复用业务存在时）
  ↓
Repository
  ↓
DataSource / SourceProvider / PublicationEngine / Vault
```

规则：

- UI 不直接访问 Room、ContentResolver、WebDAV、libarchive 或 PdfRenderer。
- Feature 不直接引用具体 Reader engine 实现。
- Repository 是数据层公开入口。
- State 使用 UDF；持久状态必须有 Single Source of Truth。
- Activity 只承载窗口、系统集成和根 Compose，不保存业务状态。

官方参考：
- https://developer.android.com/topic/architecture
- https://developer.android.com/topic/architecture/recommendations

## ADR-004 Local-first

- 本地数据是默认真源。
- 网络来源（WebDAV/未来 OPDS 等）是 SourceProvider，不改变业务模型。
- 核心阅读功能离线完整可用。
- 不把云账户作为启动前置条件。

## ADR-005 统一 Publication 模型

漫画使用统一 `Publication` 模型：

```text
Publication
 ├─ metadata
 ├─ source
 ├─ protectionPolicy
 ├─ readingProgress
 ├─ locators
 └─ engineDescriptor
```

实际打开时由 `PublicationEngineRegistry` 选择：

- Fixed/Image：image folder/CBZ/CBR/CB7/CBT/PDF comic。

## ADR-006 Reader Shell 与 Reader Engine 分离

`ReaderShell` 负责：

- system bars
- top chrome
- bottom control island
- reader settings sheet
- accessibility
- secure-window policy
- progress/navigation UI

Engine 负责：

- 内容解析
- 页面/资源读取
- locator
- search
- decorations/highlights
- 渲染 surface

这样新增格式不会复制整套 UI。

## ADR-007 Storage Access Framework 为本地仓库标准入口

- 文件/目录授权走 `ACTION_OPEN_DOCUMENT` / `ACTION_OPEN_DOCUMENT_TREE`。
- 保存 persistable URI permission。
- 不依赖 `/storage/emulated/0/...` 真实路径。
- 不以 `MANAGE_EXTERNAL_STORAGE` 作为正常工作前提。
- 所有 DocumentProvider 都按“可能很慢、可能远程、可能不支持随机访问”处理。

官方参考：
- https://developer.android.com/training/data-storage/shared/documents-files

## ADR-008 真加密 Vault，不做“假隐私”

访问锁和加密锁是两个概念：

- Access Lock：只做应用内门禁。
- Encrypt & Lock：明文迁入 Vault，持久介质保存密文。

Vault V1：

- AES-256-GCM。
- 每个对象独立随机 DEK。
- Vault Master Secret 包装 DEK。
- Keystore Device KEK 保护 Master Secret 的设备槽。
- 可选恢复密码槽用于迁移/恢复。
- chunked authenticated encryption，支持按块随机读取。
- 解密内容默认只存在于短生命周期内存 buffer，不生成长期明文临时文件。

官方参考：
- https://developer.android.com/privacy-and-security/keystore
- https://developer.android.com/privacy-and-security/cryptography

## ADR-009 App Lock 认证

- 首选 `BiometricPrompt`。
- 支持强生物识别 + DEVICE_CREDENTIAL。
- App PIN/密码为应用层独立策略，不冒充系统凭据。
- `AppLockManager` 维护 `Locked / Authenticating / UnlockedUntil / PermanentlyUnavailable` 状态。
- 后台超时、进程重启、设备锁定等场景必须重新评估认证状态。

## ADR-010 受保护内容不泄露元数据

锁定状态：

- 不加载真实封面。
- 不在普通 FTS 返回标题/作者/文件名。
- 最近阅读卡片显示统一占位。
- App switcher / screenshot 根据策略启用 `FLAG_SECURE` / Recents 保护。
- 通知不得暴露受保护标题。

## ADR-011 Monet 是默认全局主题

- Android 12+ 正常路径始终 `dynamicLightColorScheme()` / `dynamicDarkColorScheme()`。
- 不提供强品牌 seed 默认覆盖壁纸色。
- fallback palette 只用于异常兜底和测试。
- Reader 内容主题独立，不把 Monet 强行灌进漫画背景。

官方参考：
- https://developer.android.com/develop/ui/compose/designsystems/material3

## ADR-012 Motion 统一入口

- 所有 Feature 使用 `AsterMotion` semantic token。
- Feature 禁止自行散落 `tween(300)` / `spring(...)`。
- M3 Expressive alpha API 只能留在 Design System。
- Predictive back、shared element、pane transition 都必须能被 Reduced Motion 降级。

## ADR-013 Adaptive 按“窗口”而不是“设备类型”

- 不写 `isTablet`。
- 使用实际 window size class / adaptive info。
- Library/Detail 使用 List-Detail canonical layout。
- Reader settings / metadata 使用 Supporting Pane 或 Sheet。
- 分屏、自由窗口、折叠态都必须动态响应。

官方参考：
- https://developer.android.com/develop/adaptive-apps/guides/canonical-layouts
- https://developer.android.com/develop/adaptive-apps/guides/support-different-display-sizes

## ADR-014 稳定依赖优先，实验能力单点封装

稳定依赖默认使用 stable。唯一允许主动采用 alpha 的核心场景是明确需要 M3 Expressive 新能力，且必须：

1. 锁版本。
2. 封装在 design system。
3. 有稳定回退。
4. 独立升级 PR。
5. 跑 screenshot/motion/predictive-back 回归。

## ADR-015 数据库可迁移，不允许 destructive migration 进入正式版

- Room schema 每次发布写 migration test。
- 正式版禁止 `fallbackToDestructiveMigration()`。
- Reader locator、Vault metadata、书架关系属于用户数据，升级必须保留。

## ADR-016 性能是 Feature Definition of Done 的一部分

- Release + R8 才能做性能结论。
- Baseline Profile 覆盖启动、Library、Detail、Reader。
- Macrobenchmark 使用真实设备。
- 解析/解码/扫描禁止在 Main dispatcher 做阻塞 IO。

官方参考：
- https://developer.android.com/develop/ui/compose/performance
- https://developer.android.com/develop/ui/compose/performance/baseline-profiles

## ADR-017 隐私数据默认不进入系统 Auto Backup

Vault 密钥、密文内部索引、App PIN 派生材料等必须显式排除系统云备份。应用自己的“加密备份”是一条独立功能链路。

因为项目 minSdk 31，统一使用 `android:dataExtractionRules`。

官方参考：
- https://developer.android.com/identity/data/autobackup
- https://developer.android.com/privacy-and-security/risks/backup-best-practices

## ADR-018 不自创密码学

可以设计自己的**容器格式**，但不得发明密码算法、MAC、KDF 或随机数方案。所有密码学使用平台/JCA/经审计实现；格式必须有版本号和测试向量。

## ADR-019 开发顺序安全前置

正式顺序冻结为：

```text
Foundation
→ Library/SAF/Data
→ Security/Vault
→ Comic Reader
→ PDF
→ Backup/Recovery/Performance
→ Remote/WebDAV
→ 1.0 hardening
```

原因：Vault 必须进入 DocumentSource/ReaderSession 的 IO 抽象，不能在 Reader 完成后再补。

## ADR-020 v1.0 Scope Freeze

v1.0 必须做到“本地强大且可靠”，不追求无限功能：

P0：
- 本地 Library/Repository
- 漫画主流压缩格式
- PDF
- 阅读进度/书签/高亮/笔记（格式支持时）
- App Lock + Encrypt & Lock
- 备份/恢复
- 手机/平板/折叠屏
- Monet/M3E/Motion

P1（可以 1.0 后）：
- WebDAV 深度流式读取优化
- OPDS/Komga/Calibre 等连接器
- 跨设备同步
- 插件系统

## ADR-021 漫画专用与开发数据库（2026-11-04）

用户明确要求产品只做漫画，并取消开发数据迁移。代码、UI、设置、契约、任务表仅保留漫画范围。当前只开放 CBZ 与 PDF 漫画；其他容器不得显示为已支持。

开发库改用 `aster_comics.db`，从 schema v1 新建，不注册旧开发库迁移，不读取或搬运旧开发数据。不调用 destructive migration，不删除用户源文件。正式发布后的数据兼容规则仍须单独设计。

回滚以 Git 提交为边界。旧开发库保留在原位置，回滚代码可重新使用旧库；当前库和旧库互不覆盖。
