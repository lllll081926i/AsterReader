# 28 Final Development Plan — 正式开发执行计划

> 本文件是正式开工后的主计划。`13_DEVELOPMENT_PLAN.md` 保留为早期摘要；**实际执行以本文件为准**。

## 0. 开发方式

项目按“可运行纵切（vertical slice）”推进，而不是先把所有底层写完再做 UI。每个阶段都必须产生一个可以安装、可以演示、可以测试的 App。

推荐节奏：

- 每个 Sprint：1 周。
- 每周最多 1 个“大风险核心目标” + 若干小任务。
- 每个 Sprint 结束必须打 internal build。
- 不以“代码写完”作为完成，以对应验收测试通过作为完成。

单人第一次做大型 Android 项目，不要求机械追日期。下面周数是**推荐顺序和体量**，真正进度由 Exit Gate 决定。

---

## Phase 0 — Project Foundation（S0–S2）

### 目标

建立一个以后不需要推倒重来的工程骨架。

### Sprint 0：工具链与仓库

任务：

- 创建 Git 仓库，默认分支 `main`。
- Android Studio 当前稳定版。
- JDK/Gradle/AGP/Kotlin 版本锁定。
- `compileSdk 37 / targetSdk 37 / minSdk 31`。
- Gradle Version Catalog。
- CI：assemble + unit test + lint。
- Dependabot/Renovate 二选一，只提升级 PR，不自动合并。
- 建立 `AGENTS.md`、PR 模板、Issue 模板。

Exit Gate：

- clean clone 后一条命令可构建。
- API 31 emulator 和 API 37 emulator 均能启动空 App。
- CI 绿。

### Sprint 1：模块、Theme、Navigation

落地最小模块：

```text
:app
:core:model
:core:common
:core:designsystem
:core:database
:core:datastore
:core:files
:core:reader-api
:core:security-api
:feature:library
:feature:shelves
:feature:settings
:benchmark
```

实现：

- AsterTheme。
- Monet light/dark。
- emergency fallback。
- edge-to-edge。
- Navigation 3 根导航。
- adaptive navigation bar/rail。
- Library/Shelves/Settings 空页面。

Exit Gate：

- 与 `design.md` token 对齐。
- 旋转/分屏不丢导航状态。
- Android 12 真机能看到 Monet。

### Sprint 2：UI 基础组件与质量底座

实现：

- Search surface。
- Publication card。
- Empty state。
- Settings group。
- Reader control island prototype。
- AsterMotion semantic wrapper。
- Reduced Motion。
- screenshot tests / Preview catalog。
- detekt/ktlint 或等价静态检查。

Exit Gate：Design System 可以支撑后续页面，不需要 Feature 自己发明颜色/圆角/动画。

---

## Phase 1 — Library / SAF / Data（S3–S6）

### Sprint 3：Room 数据模型

- Publication / Source / Repository / Shelf / Tag / ReadingProgress / Bookmark。
- FTS 普通索引。
- migration test 框架。
- fake repository + test fixtures。

### Sprint 4：SAF Repository

- `ACTION_OPEN_DOCUMENT_TREE`。
- persistable URI permission。
- capability probe。
- DocumentSource 抽象。
- 目录授权移除/失效处理。

### Sprint 5：Scanner

Scanner 流程：

```text
Enumerate
→ Classify
→ Fingerprint
→ Parse light metadata
→ Stage
→ Diff
→ Commit transaction
→ Cover job
```

必须支持：

- 取消。
- 崩溃后重扫。
- 文件移动/删除/修改。
- 大目录分批处理。

### Sprint 6：Library 真页面

- grid/list。
- search/filter/sort。
- shelves/tags/favorites/history。
- 详情页第一版。
- 1,000 publication fixture。

Exit Gate：不要求全格式都能读，但 Library 已经是一个真正可用的本地资料管理器。

---

## Phase 2 — Security Foundation / Vault（S7–S10）

> 这是 v1.0 最重要的技术风险阶段之一。不要为了赶 UI 跳过测试。

### Sprint 7：App Lock

- SecurityBootstrap。
- BiometricPrompt。
- BIOMETRIC_STRONG / DEVICE_CREDENTIAL capability matrix。
- App PIN/Password。
- lock timeout。
- background/foreground state machine。
- lock screen UI。

### Sprint 8：Key hierarchy

- Android Keystore KEK。
- Vault Master Secret。
- per-object DEK。
- recovery key slot。
- key versioning。
- key unavailable / invalidated 状态。

### Sprint 9：ASV1 encrypted container

实现并测试：

- header/version。
- chunk table。
- AES-256-GCM chunk encryption。
- random-access decrypt。
- authenticated metadata。
- corruption detection。
- fuzz/property tests。

**禁止直接对用户重要文件做第一次实验。先对生成 fixture 测试。**

### Sprint 10：Encrypt & Lock UX

- 单文件迁移。
- atomic copy → encrypt → verify → commit。
- 原文件删除必须二次确认并在验证成功后执行。
- protected metadata/cover placeholder。
- FTS exclusion。
- FLAG_SECURE / recents policy。

Exit Gate 必测：

1. 明文 CBZ 加密。
2. 文件系统只能看到密文容器。
3. 未认证无法读出有效明文。
4. 解锁后 ReaderSource 可随机读。
5. 强杀进程后无遗留明文 temp。
6. 任意篡改 chunk 后认证失败，不能静默返回损坏明文。

---

## Phase 3 — Comic Reader（S11–S14）

### Sprint 11：Image / CBZ

- image directory。
- ZIP/CBZ。
- page index。
- natural sort。
- cover detect。

### Sprint 12：Reader Shell

- immersive/edge-to-edge reader。
- chrome hide/show。
- progress。
- page jump。
- bookmark。
- orientation policy。
- reader settings sheet。
- control island。

### Sprint 13：Comic modes

- horizontal paging。
- vertical paging。
- continuous webtoon。
- RTL。
- dual page。
- crop/fit modes。

### Sprint 14：Archive + performance

- RAR/CBR。
- 7Z/CB7。
- TAR/CBT。
- prefetch scheduler。
- decode sampling。
- region/tile strategy。
- memory pressure handling。

Exit Gate：

- 大图不会整图无脑解码导致 OOM。
- 500 页漫画连续阅读稳定。
- Vault 内 CBZ 与普通 CBZ 使用同一个 Reader，不出现两套逻辑。

---

## Phase 5 — PDF（S20–S21）

### Sprint 20–21：PDF 漫画

- PdfRenderer 按需渲染。
- 页码、书签、单页、条漫、双页与阅读方向。
- 密码或损坏容器显示明确错误，保留原件。
- 后续完善区域解码和大文档缓存。

Exit Gate：PDF 与 CBZ 使用同一漫画阅读外壳，不创建独立文档模式。

---

## Phase 6 — Backup / Recovery / Performance / Accessibility（S22–S25）

### Sprint 22：Backup

- metadata-only backup。
- full encrypted app backup。
- schema/version header。
- import dry-run。
- conflict policy。

### Sprint 23：Vault Recovery

- recovery password setup。
- export/import recovery material。
- lost-key UX。
- corruption report。
- restore test vectors。

### Sprint 24：Performance

Baseline Profile CUJ：

1. cold start → Library。
2. search。
3. open Detail。
4. open CBZ Reader。
5. 打开 PDF 漫画 Reader。
6. unlock protected publication。

同时做：

- Macrobenchmark。
- jank inspection。
- startup trace。
- memory profiling。
- 1k/10k Library test。

### Sprint 25：Accessibility / Adaptive hardening

- TalkBack。
- 200%+ font scale。
- keyboard/D-pad basic navigation。
- contrast。
- reduced motion。
- compact/medium/expanded。
- fold/unfold continuity。

Exit Gate：通过 `29_DEFINITION_OF_DONE_AND_GATES.md` 中 Beta Gate。

---

## Phase 7 — WebDAV（S26–S28，可延后到 1.1）

如果本地 1.0 质量尚未达标，**本阶段允许整体延期**。

- credential storage。
- DAV4JVM/HTTP adapter。
- ETag/Last-Modified。
- incremental refresh。
- cache/offline。
- Range capability probe。
- 不支持 random access 时明确 fallback 到下载后阅读。

原则：远程功能不能破坏 Local-first 的稳定性。

---

## Phase 8 — 1.0 Hardening（至少 2 个 Sprint）

只做：

- bug。
- compatibility。
- performance regression。
- migration。
- security review。
- copywriting/localization。
- store/release assets。

**禁止 1.0 候选期继续塞大 Feature。**

### RC Gate

- 0 个 P0/P1 已知 blocker。
- API 31 / 33 / 35 / 37 核心路径通过。
- Room migrations 从最早公开测试 schema 全链升级通过。
- 加密/恢复 roundtrip 通过。
- 删除/批量加密有断电/强杀恢复测试。
- 1,000 本资料库无明显交互卡顿。
- 关键 CUJ Macrobenchmark 无显著回归。
- 受保护内容 screenshot/recents/FTS/notification 泄露测试通过。

---

# 里程碑定义

## M0 — Skeleton

能装、能切页面、Monet 正常。

## M1 — Library Alpha

能管理本地资料库。

## M2 — Secure Alpha

App Lock + 单文件 Vault 完成。

## M3 — Comic Alpha

可真正拿来读漫画。

## M5 — Feature Complete Beta

PDF/Backup/Recovery/Adaptive/Performance 到位。

## M6 — 1.0 RC

冻结 Feature，只解决质量。

## M7 — 1.0

正式发布。

---

# 每个 Sprint 的固定仪式

开始前：

- 确定 1 个主要目标。
- 明确受影响模块。
- 列出 acceptance tests。
- 如果改架构先写 ADR。

提交中：

- 小 PR。
- 每个 PR 只解决一个主题。
- 新代码有 test 或说明为什么不适合自动化。
- 不顺手升级无关依赖。

结束时：

- 构建 internal APK/AAB。
- 跑 lint/unit/instrumentation relevant tests。
- 真机走一次主路径。
- 更新 backlog/status。
- 记录技术债，不在最后一天偷偷扩大 Scope。
