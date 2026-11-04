# AGENTS.md — Aster Reader Coding Rules

This file is mandatory context for any coding agent.

## Frozen decisions

- Android native Kotlin + Jetpack Compose.
- `minSdk=31`, `targetSdk=37`, `compileSdk=37`.
- Monet Dynamic Color is the global default.
- `design.md` is the UI/Motion source of truth.
- Navigation 3 + Material3 Adaptive.
- UI → ViewModel → UseCase(optional) → Repository → DataSource.
- Composables NEVER access DAO/ContentResolver/network/archive/crypto directly.
- Reader Shell is separate from Publication Engines.
- SAF is the standard local repository entry.
- “Access Lock” != “Encrypt & Lock”. Encrypt & Lock must store ciphertext in Vault.
- Vault: Android Keystore + master secret + per-object DEK + AES-256-GCM chunks.
- Protected metadata/cover/search must stay hidden while locked.
- No destructive database migration.
- Stable dependencies by default; experimental Material3 APIs are wrapped only in `:core:designsystem`.

## Before coding

Read:
1. `docs/27_FINAL_ARCHITECTURE_DECISIONS.md`
2. relevant feature docs
3. `docs/29_DEFINITION_OF_DONE_AND_GATES.md`
4. `design.md` for any UI task

State the modules/files you plan to touch and the tests you will run.

## Never do

- No giant MainActivity.
- No hardcoded UI colors/radii/motion durations in Feature modules.
- No raw file paths assumption for SAF documents.
- No plaintext protected-content temp files unless explicitly required and securely lifecycle-managed.
- No passwords/keys/decrypted text/protected paths in logs.
- No deleting source files before encrypted output is verified and committed.
- No dependency upgrades inside unrelated feature PRs.
- No disabling tests to make CI green.

## Required finish report

- implemented
- changed modules
- tests run
- API31/API37 manual status
- security/data migration impact
- known limitations



## UI 工作流强制规则（v4）

创建、修改任何 Screen 或可复用组件前，**先阅读** `design.md` 与 `docs/36_COMPLETE_UI_UX_SPEC.md` 中对应 Screen ID。每个 UI PR 需覆盖至少正常/空/错误/锁定（如适用）状态，支持 API 31/新系统、Monet 浅/深、Insets、返回、Reduced Motion、TalkBack，并附截图/录屏及验收证据。UI 不得直接调用 DAO、ContentResolver、Vault 密钥；受保护页面必须先授权才加载敏感内容。逐屏实施顺序与状态由 `planning/UI_SCREEN_TASK_MATRIX.csv` 管理。
