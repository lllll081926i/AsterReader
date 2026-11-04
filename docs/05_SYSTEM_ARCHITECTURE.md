# 05 系统架构

## 架构目标

- UI 不知道具体文件格式。
- 阅读器不直接操作 Room。
- Source 不知道 Book Card 如何展示。
- 新增格式只需实现 Engine；新增远程位置只需实现 SourceProvider。

## 推荐模块

```text
:app
:core:common
:core:model
:core:database
:core:datastore
:core:designsystem
:core:ui
:core:engine-api
:core:source-api
:core:reader-session

:engine:readium
:engine:text
:engine:fb2
:engine:mobi
:engine:comic
:engine:pdf

:source:local
:source:webdav

:feature:library
:feature:shelves
:feature:search
:feature:detail
:feature:reader
:feature:metadata
:feature:settings
:feature:backup

:benchmark
```

初期如果团队只有 1–2 人，可合并 engine 小模块，但接口边界不要省。

## 依赖方向

```text
feature:* ───> core:model / core:*-api / core:ui
engine:*  ───> core:engine-api / core:model
source:*  ───> core:source-api / core:model
core:database ─> core:model
app ───────> feature:* + DI wiring
```

`engine:*` 不依赖 `feature:*`。这条是硬规则。

## 核心接口

### SourceProvider

职责：列目录、打开随机/顺序读取流、查询元数据、检测能力。

能力位：

- LIST
- READ
- RANDOM_ACCESS
- RANGE_READ
- WRITE
- DELETE
- WATCH

本地 SAF Source 通常有 READ/WRITE；WebDAV 是否支持 RANGE_READ 需要探测。

### PublicationEngine

职责：判断格式、解析 metadata/TOC、创建 ReaderSession。

```kotlin
interface PublicationEngine {
    val id: String
    suspend fun probe(input: PublicationInput): ProbeResult
    suspend fun parse(input: PublicationInput): ParsedPublication
    suspend fun open(input: PublicationInput, locator: Locator?): ReaderSession
}
```

### ReaderSession

统一暴露：

- 当前 Locator
- 目录
- 进度
- next/previous
- seek(locator)
- search(query)（能力可选）
- bookmark target
- lifecycle close/flush

UI 根据 `capabilities` 决定显示哪些按钮。

## 状态管理

Feature 使用 `ViewModel + StateFlow`；复杂 Reader 状态建议 Reducer/MVI 风格，但不引入重型框架。

`ReaderUiState` 分为：

- session state（位置、章节、页数）
- transient UI（overlay visible、bottom sheet）
- settings snapshot
- engine capabilities

避免把 Bitmap、PdfDocument 等大对象塞进 Compose State。

## 数据持久化

- Room：Publication、Source、Shelf、Tag、Bookmark、Annotation、Progress、ScanRecord。
- Room FTS：标题/作者/描述/章节文本索引。
- DataStore：主题、全局阅读设置、手势、缓存上限。
- 文件缓存：app cache + 可选用户指定目录。
- KeyStore：远程仓库凭据加密。

## 后台任务

WorkManager：

- RepositoryScanWorker
- MetadataIndexWorker
- FullTextIndexWorker
- ThumbnailWorker
- BackupWorker
- RemoteRefreshWorker

任务均可重入、可取消、可恢复。

## 架构决策：为什么不是“一个超大 ReaderActivity”

阅读器的复杂度最终来自格式和来源组合。若 EPUB、PDF、CBZ、WebDAV 的逻辑都写进 Activity/ViewModel，后期任何修复都会触碰所有格式。使用 Source/Engine 双抽象后，组合是 `Source × Engine`，但代码仍是线性扩展。

## v2.0 Security / Vault 模块

新增：

```text
:core:security-api
:core:security-keystore
:core:crypto
:core:vault
:feature:auth
:feature:vault
```

依赖：

```text
feature:auth   -> core:security-api
feature:vault  -> core:security-api + core:vault + core:model
core:vault     -> core:crypto + core:security-api + core:source-api
core:security-keystore -> core:security-api
engine:*       -> 只能看到 SecureDocumentSource 抽象，不直接碰 Keystore
```

根启动结构：

```text
MainActivity
  -> AsterTheme
    -> BootstrapCoordinator
      -> SecurityBootstrap
        -> AuthGate
        -> Navigation3
```

### SecureDocumentSource

对于受保护文件，Reader Engine 仍然只处理统一的可读接口；差别由 Source 层完成：

```text
Plain Local -> SafDocumentSource
Vault       -> SecureDocumentSource -> DecryptingSeekableSource
Remote      -> RemoteDocumentSource
```

因此 EPUB/PDF/Comic engine 不需要知道密钥来自 Keystore 还是恢复密码。
