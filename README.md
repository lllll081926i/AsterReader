# Aster Reader — 漫画阅读器

> 2026-11-04：产品仅做漫画阅读。当前实际支持 CBZ 与 PDF 漫画。应用仍在开发。

本轮审查、修复、性能措施与验收限制见 `docs/38_AUDIT_FIXES_AND_PERFORMANCE.md`。开发库从 `aster_comics.db` schema v1 新建，不迁移旧开发数据。Vault、加密备份及恢复密钥尚未实现，入口拒绝执行并保留原文件。

## 1. 已冻结的核心决策

- **最低 Android 12 / API 31**；`targetSdk=37`、`compileSdk=37`。
- Kotlin + Jetpack Compose，单 Activity。
- Material 3；全局默认 Monet Dynamic Color。
- Material 3 Expressive 只通过 Design System 单点封装，避免 alpha API 污染业务层。
- Navigation 3 + Material3 Adaptive。
- 本地文件通过 SAF，不依赖真实路径、不把“所有文件访问”当常规前提。
- 统一 `Publication` 模型，漫画共用 Library、Detail、Progress、Security。
- `ReaderShell` 与实际 Reader Engine 分离。
- App Lock + 单本/文件夹/书架/仓库保护。
- **Access Lock ≠ Encrypt & Lock**：后者必须把持久文件变成 Vault 密文。
- Android Keystore + Vault Master Secret + per-object DEK + AES-256-GCM chunk container。
- 受保护 metadata、cover、FTS、recents、screenshot 均纳入隐私模型。
- Room 数据库不允许正式版 destructive migration。
- Baseline Profile + Macrobenchmark 是发布要求，不是“有时间再做”。

完整冻结清单：`docs/27_FINAL_ARCHITECTURE_DECISIONS.md`。

## 2. 第一次阅读顺序

如果你是 Android 新手，按以下顺序，不要跳：

1. `docs/00_START_HERE.md`
2. `docs/27_FINAL_ARCHITECTURE_DECISIONS.md`
3. `design.md`
4. `docs/01_PRODUCT_PRD.md`
5. `docs/20_ANDROID_ENGINEERING_STANDARD.md`
6. `docs/05_SYSTEM_ARCHITECTURE.md`
7. `docs/19_SECURITY_VAULT_AND_APP_LOCK.md`
8. `docs/06_READER_ENGINE.md`
9. `docs/22_IMPLEMENTATION_COOKBOOK.md`
10. `docs/28_FINAL_DEVELOPMENT_PLAN.md`
11. `docs/29_DEFINITION_OF_DONE_AND_GATES.md`
12. `docs/35_FINAL_PRE_DEVELOPMENT_CHECKLIST.md`

如果准备让 Codex/Claude Code 等开始写代码，还必须给它根目录 `AGENTS.md`。

## 3. 文档地图

### Product / UX
- `docs/01_PRODUCT_PRD.md`
- `docs/02_COMPETITOR_SYNTHESIS.md`
- `docs/03_UX_INFORMATION_ARCHITECTURE.md`
- `docs/18_SCREENSHOT_AUDIT.md`

### Design / Motion
- `design.md` — **唯一视觉/动效/交互真源**
- `docs/04_MD3E_DESIGN_SYSTEM.md`
- `docs/21_MOTION_ANIMATION_SPEC.md`
- `docs/25_PRIVACY_LOCK_UX_SPEC.md`
- `docs/33_ACCESSIBILITY_AND_LOCALIZATION.md`

### Architecture / Data / Reader
- `docs/05_SYSTEM_ARCHITECTURE.md`
- `docs/06_READER_ENGINE.md`
- `docs/07_STORAGE_LIBRARY_SYNC.md`
- `docs/08_DATA_MODEL.md`
- `docs/09_FORMAT_MATRIX.md`
- `docs/10_PERFORMANCE.md`
- `docs/27_FINAL_ARCHITECTURE_DECISIONS.md`
- `docs/34_DIAGNOSTICS_AND_OBSERVABILITY.md`

### Security
- `docs/11_SECURITY_PRIVACY.md`
- `docs/19_SECURITY_VAULT_AND_APP_LOCK.md`
- `docs/23_THREAT_MODEL_AND_RECOVERY.md`
- `docs/32_SECURITY_VERIFICATION_PLAN.md`

### Engineering / Execution
- `docs/12_TESTING_QA.md`
- `docs/13_DEVELOPMENT_PLAN.md` — 快速索引
- `docs/14_LICENSE_AND_RISKS.md`
- `docs/15_PROJECT_STRUCTURE.md`
- `docs/16_ACCEPTANCE_CRITERIA.md`
- `docs/20_ANDROID_ENGINEERING_STANDARD.md`
- `docs/22_IMPLEMENTATION_COOKBOOK.md`
- `docs/24_BUILD_DEPENDENCY_BASELINE.md`
- `docs/26_NEWBIE_BUILD_GUIDE.md`
- `docs/28_FINAL_DEVELOPMENT_PLAN.md` — **正式开发主计划**
- `docs/29_DEFINITION_OF_DONE_AND_GATES.md`
- `docs/30_RELEASE_MIGRATION_MAINTENANCE.md`
- `docs/31_AGENT_IMPLEMENTATION_PLAYBOOK.md`
- `docs/35_FINAL_PRE_DEVELOPMENT_CHECKLIST.md`

### Planning / Agent
- `ROADMAP.md`
- `AGENTS.md`
- `planning/backlog.csv`
- `planning/development_tasks.csv`

### Reusable skeletons
- `starter-contracts/core/`
- `starter-contracts/security/`
- `starter-contracts/design/`
- `diagrams/`
- `references/screenshots/`

## 4. 工程基线

```text
minSdk     31 / Android 12
targetSdk  37 / Android 17
compileSdk 37
Kotlin     2.4.20 baseline
AGP        9.4.x baseline
Gradle     9.6.x baseline
JDK        17
```

Jetpack/Reader 具体锁定版本见 `docs/24_BUILD_DEPENDENCY_BASELINE.md`。

## 5. 正式实施顺序

```text
Foundation
  ↓
Library + SAF + Room
  ↓
App Lock + Keystore + Vault
  ↓
Comic Reader
  ↓
PDF
  ↓
Backup + Recovery + Performance + Accessibility
  ↓
WebDAV（可延后）
  ↓
1.0 Hardening / RC
```

不要在 Reader 做完以后再“补安全”；不要在 1.0 RC 期间继续塞大功能。

## 6. 两条最重要的工程纪律

### 安全纪律

“加锁”如果宣称文件已加密，就必须真的把明文迁入 Vault。UI 模糊、隐藏目录、数据库 flag 都不能叫文件加密。

### 设计纪律

任何页面如果需要一个新颜色、新圆角、新 motion duration、新导航模式，先改 `design.md` / Design System，再写 Feature；不允许每个页面长成自己的风格。

## 7. 开始开发

先执行 `docs/35_FINAL_PRE_DEVELOPMENT_CHECKLIST.md` 中的前 10 个 Issue。完成 BOOT/DESIGN/NAV/DATA/FILE 的基础骨架后，再进入 Scanner 与 Vault。
