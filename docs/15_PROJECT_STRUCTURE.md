# 15 工程目录与编码规范

## 推荐目录

```text
aster-reader/
├─ app/
├─ core/
│  ├─ common/
│  ├─ model/
│  ├─ database/
│  ├─ datastore/
│  ├─ designsystem/
│  ├─ engine-api/
│  ├─ source-api/
│  └─ reader-session/
├─ engine/
│  ├─ comic/
│  └─ pdf/
├─ source/
│  ├─ local/
│  └─ webdav/
├─ feature/
│  ├─ library/
│  ├─ shelves/
│  ├─ search/
│  ├─ detail/
│  ├─ reader/
│  ├─ metadata/
│  ├─ settings/
│  └─ backup/
├─ benchmark/
└─ build-logic/
```

## Kotlin 规则

- UI model 与 DB entity 分离。
- `Result`/sealed error 明确错误类型，不用 `Exception.message` 当业务状态。
- I/O API 全部 suspend。
- Engine 生命周期必须 Closeable/AutoCloseable 语义。
- 禁止 GlobalScope。
- Bitmap/ParcelFileDescriptor/native handle 必须有明确 owner。

## Compose 规则

- Screen composable 无业务 I/O。
- ViewModel 只暴露不可变 UiState + event function。
- 每个 feature 提供 Preview fixtures。
- 组件优先 stateless；state hoisting。
- Reader canvas/gesture 高频部分可单独使用低层 pointer input，不要让整个 screen 重组。

## Git/PR

分支：`feat/`, `fix/`, `perf/`, `refactor/`, `docs/`。

PR 必须包含：

- 变更原因
- 测试
- UI 变更截图（如有）
- 性能影响（Reader/Scanner 相关必须说明）
- 许可证新增依赖（如有）

## CI

每个 PR：

- ktlint/detekt
- unit tests
- assembleDebug
- license check

主分支/RC：

- instrumentation tests
- screenshot tests
- migration tests
- benchmark subset
- dependency vulnerability scan

## v2.0 新增目录

```text
core/
  security-api/
  security-keystore/
  crypto/
  vault/
feature/
  auth/
  vault/
```

设计系统唯一规范根文件：`design.md`。

### 安全编码额外规则

- `Cipher` 只允许出现在 `core:crypto` / `core:security-keystore`。
- feature 层不得接触 raw SecretKey。
- 日志禁止 protected title/path。
- 任何“解密到 temp file”的代码必须 security review，默认不允许。
- 任何删除原文件操作必须在“加密副本校验成功”之后。
