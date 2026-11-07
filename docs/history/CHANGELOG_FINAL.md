# FINAL v3.0 变更摘要

日期：2026-09-29

相较 v2，本次是正式开发前的“冻结与消歧”版本：

- 最低版本正式冻结为 **Android 12 / API 31**，清除 Android 8–11 兼容叙述。
- target/compile 固定 API 37。
- 增补 AGP/Kotlin/Gradle/JDK 工具链基线。
- 正式建立 20 项架构 ADR。
- 把开发计划重写为 S0–S28 + RC 的可执行 Sprint/Exit Gate。
- 新增 Definition of Done、Release/Migration、Security Verification、Accessibility、Diagnostics。
- 新增根目录 `AGENTS.md`，可直接作为 Coding Agent 项目约束。
- 新增 `ROADMAP.md` 与 `planning/development_tasks.csv`。
- 统一 Security 前置的 Phase 顺序，修正旧 backlog 中 Comic/Text/PDF 阶段编号。
- `design.md` 删除旧系统 Monet fallback 语义，fallback 只保留异常兜底。
- 正式规定 stable-first + experimental UI 单点封装策略。

从本版本开始，普通开发任务不再修改核心架构决定；需要变化时新增 ADR。
