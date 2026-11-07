# 00 START HERE — Aster Reader FINAL v3.0

> **2026-11-07 补充**：现已提供公开书库清单的口令加密导出与认证后恢复。该功能不包含漫画文件、隐藏作品或安全凭据，不构成文件 Vault。PIN 凭据升级、敏感设置重新认证和后续 Vault 协议约束见 `43_SECURITY_PROTOCOL_CONSTRAINTS.md`。本轮界面及核验记录见 `44_SETTINGS_READER_DELIVERY_2026-11-07.md`。

> **实现状态（2026-11-05，务必先读）**
> 本目录 01–39 是开发前的规划文档，其中 `19_SECURITY_VAULT_AND_APP_LOCK.md`、
> `23_THREAT_MODEL_AND_RECOVERY.md`、`32_SECURITY_VERIFICATION_PLAN.md` 以及 PRD 里
> 「真加密 Vault / 受保护 metadata 加密 / ASTV1 容器」等章节**已明确搁置，未实现**。
> 当前安全模型只有两层：**应用锁**（自定义 PIN + 系统生物识别）与**隐藏作品**
> （纯可见性，文件字节不改动）。`docs/40_ENCRYPTION_VAULT_DESIGN.md` 已删除。
> 代码现状以 `app/src/main` 与 `core/database` 为准；界面文案目前仅简体中文硬编码，
> 尚未抽取多语言资源。数据库已注册 v1→v2 迁移，release 构建需环境变量提供上传密钥。

> 这是正式开发前的入口文档。你不需要先懂 Android 的所有知识；按文档规定的顺序做，每一阶段都先建立边界、再实现功能、最后过 Gate。

## 一句话目标

做一个 **Android 原生、Local-first、以本地资料库为核心的强大阅读器**：

- 漫画：图片目录、CBZ/ZIP、CBR/RAR、CB7/7Z、CBT/TAR、PDF Comic。
- Library：仓库、书架、标签、收藏、历史、搜索、筛选、元数据。
- Reader：统一 Shell + 不同 Engine。
- Privacy：App Lock、局部保护、真加密 Vault、受保护 metadata 隐藏。
- UI：Material 3 / M3 Expressive / Monet / Predictive Back / Shared Element。
- Adaptive：手机、横屏、平板、折叠屏、分屏。

## 平台边界已经定死

```text
Minimum Android = Android 12 / API 31
Target Android  = Android 17 / API 37
Compile SDK     = API 37
```

Android 12 是完整支持版本，不是阉割版；Android 13–17 只提供渐进增强。

## 最重要的四条原则

### 1. 不把所有东西塞进 Activity / Composable

```text
UI
→ ViewModel / StateHolder
→ UseCase（需要时）
→ Repository
→ DataSource / Source / Engine / Security
```

### 2. 不把“隐藏”叫“加密”

- Access Lock：App 内门禁，原文件仍是明文。
- Encrypt & Lock：迁移到 Vault，持久文件是密文。

### 3. 不让每个页面自己设计

根目录 `design.md` 是唯一视觉、交互、动画真源。

### 4. 不在 Reader 完成后才补安全

Security/Vault 在 Comic Reader 之前进入架构，以 `SecureDocumentSource` 方式成为普通 IO 路径的一部分。

---

# 新手阅读顺序

## 第一轮：先弄懂“我们到底要做什么”

1. `01_PRODUCT_PRD.md`
2. `02_COMPETITOR_SYNTHESIS.md`
3. `03_UX_INFORMATION_ARCHITECTURE.md`
4. `../design.md`
5. `27_FINAL_ARCHITECTURE_DECISIONS.md`

## 第二轮：弄懂 Android 工程边界

6. `20_ANDROID_ENGINEERING_STANDARD.md`
7. `05_SYSTEM_ARCHITECTURE.md`
8. `15_PROJECT_STRUCTURE.md`
9. `24_BUILD_DEPENDENCY_BASELINE.md`
10. `26_NEWBIE_BUILD_GUIDE.md`

## 第三轮：数据与 Reader

11. `07_STORAGE_LIBRARY_SYNC.md`
12. `08_DATA_MODEL.md`
13. `06_READER_ENGINE.md`
14. `09_FORMAT_MATRIX.md`
15. `10_PERFORMANCE.md`

## 第四轮：安全

16. `11_SECURITY_PRIVACY.md`
17. `19_SECURITY_VAULT_AND_APP_LOCK.md`
18. `23_THREAT_MODEL_AND_RECOVERY.md`
19. `25_PRIVACY_LOCK_UX_SPEC.md`
20. `32_SECURITY_VERIFICATION_PLAN.md`

## 第五轮：正式开工

21. `22_IMPLEMENTATION_COOKBOOK.md`
22. `28_FINAL_DEVELOPMENT_PLAN.md`
23. `29_DEFINITION_OF_DONE_AND_GATES.md`
24. `30_RELEASE_MIGRATION_MAINTENANCE.md`
25. `35_FINAL_PRE_DEVELOPMENT_CHECKLIST.md`

使用 Coding Agent 时再读：`../AGENTS.md` + `31_AGENT_IMPLEMENTATION_PLAYBOOK.md`。

---

# 开发顺序

```text
Phase 0  工程 / Design System / Monet / Navigation / Adaptive
Phase 1  Room / SAF / Scanner / Library
Phase 2  App Lock / Keystore / Vault
Phase 3  Comic Reader
Phase 5  PDF
Phase 6  Backup / Recovery / Performance / Accessibility
Phase 7  WebDAV（若 1.0 本地质量不足则延期）
Phase 8  1.0 Hardening / Release Candidate
```

正式的每周任务、依赖和 Exit Gate：`28_FINAL_DEVELOPMENT_PLAN.md`。

# 开发第一天不要做什么

- 不要先接 WebDAV。
- 不要一次创建 40 个空模块。
- 不要先做“全功能设置页”。
- 不要把第三方截图逐像素照搬。
- 不要先写密码学实现再补测试。

第一天做：Project → Version Catalog → CI → Theme → Navigation → 三个空顶级页面 → API31/API37 启动测试。

# 你可以直接复用

- `starter-contracts/`：核心接口草案。
- `diagrams/`：架构/Reader/Vault 状态图。
- `planning/`：Backlog 与正式开发任务表。
- `AGENTS.md`：直接交给 Coding Agent。

# 版本策略

稳定依赖优先。Material3 的最新 Expressive API 如果处于 alpha，只允许被 `:core:designsystem` 封装，业务 Feature 不直接 import experimental API。这样未来切回 stable 或升级时不会全项目爆炸。
