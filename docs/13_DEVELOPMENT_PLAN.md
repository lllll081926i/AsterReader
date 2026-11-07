# 13 开发计划（索引版）

> Final v3.0 起，正式执行计划已经升级为 `28_FINAL_DEVELOPMENT_PLAN.md`。本文件保留为快速索引，避免旧链接失效；**阶段顺序、Sprint、Exit Gate 与发布门槛均以 28 号文档为准。**

## 冻结后的阶段顺序

```text
Phase 0  Project Foundation
Phase 1  Library / SAF / Data
Phase 2  Security Foundation / Vault
Phase 3  Comic Reader
Phase 5  PDF
Phase 6  Backup / Recovery / Performance / Accessibility
Phase 7  WebDAV（允许延后到 1.1）
Phase 8  1.0 Hardening / RC
```

为什么 Security 在 Reader 前：Vault 必须通过 `DocumentSource` / `ReaderSession` 进入 IO 层。若先把 Reader 完成后再补真加密，会导致 Source、缓存、临时文件、进度恢复和错误模型大规模返工。

## 正式开发必读

- `27_FINAL_ARCHITECTURE_DECISIONS.md` — 不得随便改变的架构决定。
- `28_FINAL_DEVELOPMENT_PLAN.md` — 每个 Sprint 的任务、依赖和退出条件。
- `29_DEFINITION_OF_DONE_AND_GATES.md` — 什么才叫完成。
- `35_FINAL_PRE_DEVELOPMENT_CHECKLIST.md` — 开工前检查和第一批 Issue。
- 根目录 `AGENTS.md` — 给 Coding Agent 的强制规则。

## 里程碑

- M0 Skeleton
- M1 Library Alpha
- M2 Secure Alpha
- M3 Comic Alpha
- M5 Feature Complete Beta
- M6 1.0 RC
- M7 1.0

详见 [实施路线](../planning/ROADMAP.md)。
