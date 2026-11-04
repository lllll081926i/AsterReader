# 20 Android 工程规范（新手可直接照此执行）

> 目标：把 Android 官方推荐实践固化成项目规则，避免“功能能跑，但架构、生命周期、存储、安全、平板、返回手势全部返工”。

## 1. 平台基线

- `compileSdk = 37`
- `targetSdk = 37`
- `minSdk = 31`
- Kotlin 2.x
- Jetpack Compose
- 单 Activity
- Navigation 3
- Material 3 / Material 3 Expressive
- Material3 Adaptive

Android 17 对应 API 37。升级 targetSdk 时必须运行 Android SDK Upgrade Assistant，并做行为变更测试。

---

## 2. 架构原则

遵循 Android 官方 app architecture：

```text
UI Layer
  -> Domain Layer (only where useful)
    -> Data Layer
```

### 2.1 UI Layer

- Composable 只渲染 state、发 event。
- ViewModel 负责 screen state 与调用 use case/repository。
- UI 不能直接访问 Room DAO、File、Cipher、WebDAV client。

### 2.2 Data Layer

Repository 是业务数据访问唯一入口。

例如：

```text
LibraryRepository
PublicationRepository
ProgressRepository
VaultRepository
SourceRepository
```

### 2.3 UDF

单向数据流：

```text
State -> UI -> Event -> ViewModel -> Repository -> New State
```

避免 Composable 内维护业务真相。

---

## 3. 模块依赖规则

```text
feature:* -> core APIs + domain/data interfaces
engine:*  -> engine-api + model
source:*  -> source-api + model
security implementation -> security-api
app -> wiring only
```

硬规则：

- `feature:*` 不互相直接依赖实现。
- `engine:*` 不依赖 UI。
- `core:model` 不依赖 Android UI。
- `:core:crypto` 不依赖 feature。

---

## 4. Compose 编码规范

### Screen 分层

```kotlin
@Composable
fun LibraryRoute(viewModel: LibraryViewModel = hiltViewModel()) { ... }

@Composable
fun LibraryScreen(
    state: LibraryUiState,
    onAction: (LibraryAction) -> Unit,
) { ... }
```

`Route` 负责 ViewModel / navigation glue；`Screen` 可 Preview、可截图测试。

### State

- 业务 state 使用 immutable data class / sealed interface。
- 不把 Bitmap、FileDescriptor、Reader native handle 放进 `UiState`。
- 大列表用 Paging 或数据库 Flow，不一次性加载 10,000 个完整对象。

### Side effects

只在合适 API 使用：

- `LaunchedEffect`
- `DisposableEffect`
- `rememberUpdatedState`

禁止把网络/磁盘 I/O 直接写进 Composable body。

---

## 5. 生命周期与进程死亡

Android 可以随时杀掉后台进程。

因此：

- 阅读进度要周期性可靠落库，不只 `onDestroy` 保存。
- 扫描任务用 WorkManager，可恢复。
- ReaderSession 可以重建：数据库保存 Locator，不保存 engine object。
- `SavedStateHandle` 只保存轻量导航/表单状态。
- 安全 unlock session 不跨进程持久化为“已解锁”。

---

## 6. Storage 规范

### 用户文档

优先 SAF：

- `ACTION_OPEN_DOCUMENT`
- `ACTION_OPEN_DOCUMENT_TREE`
- 持久化 URI permission

不要假设真实 path 可用。

### 不申请

首发不申请 `MANAGE_EXTERNAL_STORAGE`，除非未来有无法由 SAF 满足且政策允许的明确场景。

### App private

- `filesDir`：长期 app 私有数据
- `cacheDir`：可随时删除
- `noBackupFilesDir`：不进入系统 Auto Backup 的设备相关 secret/状态

Vault 是否在 `filesDir` 或用户指定 SAF vault 由存储策略决定，但 backup 规则必须明确。

---

## 7. 后台任务

长任务统一 WorkManager：

- 扫描
- 元数据索引
- 全文索引
- 生成缩略图
- Vault 批量加密
- Backup
- Remote refresh

Worker 必须：

- idempotent
- 可取消
- 可重试
- 有进度
- 能识别 app/schema 版本

---

## 8. Edge-to-edge

Android 15+ target 35+ 强制 edge-to-edge，因此从第一天就正确做：

```kotlin
onCreate() {
    enableEdgeToEdge()
    setContent { ... }
}
```

Compose 使用 WindowInsets / Material3 Scaffold 正确处理：

- status bars
- navigation bars
- display cutout
- IME
- gesture region

不要靠固定 `padding(top = 24.dp)` 假装状态栏高度。

---

## 9. Predictive Back

- 使用 Navigation 3 / 官方 Back API。
- 不在 Activity 根部吞掉 back。
- ModalBottomSheet/SearchBar 使用兼容 predictive back 的 M3 组件。
- Reader 内自定义手势必须避免和系统边缘返回冲突。

---

## 10. Adaptive UI

布局依据 **当前 window size**，不是“手机型号”。

建议：

- Compact：单 pane + bottom nav
- Medium：单/双 pane + rail
- Expanded：list-detail/supporting pane
- Foldable：考虑 hinge/posture

使用 Material3 Adaptive：

- `NavigationSuiteScaffold`
- `ListDetailPaneScaffold`
- `SupportingPaneScaffold`
- adaptive-navigation3

---

## 11. 权限规范

权限请求原则：

1. 尽可能不用 runtime permission。
2. 只在用户触发功能时请求。
3. 先解释“为什么”。
4. 拒绝后核心本地功能仍能用。

文件通过 SAF 不需要传统广泛存储权限。

---

## 12. 安全规范

- 密钥 -> Android Keystore。
- AES -> GCM。
- 密码/PIN -> 永不明文保存。
- Remote credentials -> Keystore 包装。
- Protected content -> 不进入普通 FTS。
- release 日志不得含私密路径/书名。
- 备份必须有明确 include/exclude rules。
- WebView/HTML publication 默认禁止任意 JS 与外网资源。

详细见 `19_SECURITY_VAULT_AND_APP_LOCK.md`。

---

## 13. 网络规范

即使 WebDAV 后做，也先定规则：

- HTTPS 默认。
- 超时分层。
- 不在 UI thread 做请求。
- 远程 Range 能力先 probe。
- 证书错误默认失败，不提供“忽略所有 TLS 错误”按钮。
- 可配置 debug network logging，但 release 移除敏感 header。

---

## 14. 数据库规范

Room：

- migration 必须写测试。
- release 不使用 destructive migration。
- Entity 与 domain model 分离。
- 大文本索引与核心用户数据分离。
- FTS 是可重建缓存，不是唯一数据源。
- 每个写操作考虑事务边界。

---

## 15. DataStore 规范

DataStore 用于：

- theme prefs
- reader defaults
- motion/haptics preference
- app lock timeout preference
- scan/cache preferences

不要把大型结构、书库、密钥放 DataStore。

---

## 16. 错误模型

禁止把 `Throwable.message` 直接显示给用户。

统一：

```kotlin
sealed interface AsterError {
    data object PermissionLost : AsterError
    data object VaultLocked : AsterError
    data class CorruptPublication(val reason: CorruptionReason) : AsterError
    ...
}
```

日志保留技术原因，UI 映射成人能读懂的文案。

---

## 17. Accessibility

必须测试：

- TalkBack
- 200%+ 字体
- 横屏
- Switch Access/键盘基本可达
- 动画 scale 0
- 高对比下状态可分辨

阅读器必须提供显式缩放/字号入口，不能只有 pinch gesture。

---

## 18. 性能规则

- 不在 main thread 解压/解码/解析大文件。
- 不整本加载 PDF/漫画。
- 图片按屏幕尺寸 sample。
- 列表稳定 key。
- 避免 Reader 高频帧驱动整个 screen recomposition。
- 使用 Baseline Profile + Macrobenchmark。

关键 CUJ：

1. 冷启动 -> 首页
2. 首页滚动
3. 搜索
4. 详情打开
5. 详情 -> Reader
6. 漫画快速翻页
7. Protected book 解锁 -> Reader

---

## 19. 测试金字塔

### Unit

parser、locator、scanner、crypto、repository。

### Integration

Room migration、SAF fake/provider、vault roundtrip。

### UI

Compose test + screenshot/golden。

### Instrumentation

Biometric flow（可测试抽象）、system insets、large screen。

### Benchmark

Macrobenchmark + baseline profile。

---

## 20. CI 必须做

每 PR：

```text
format/lint
unit test
assembleDebug
license check
static analysis
```

主分支/RC：

```text
instrumentation
Room migration tests
crypto fixtures
screenshot tests
benchmark subset
R8 release assemble
```

---

## 21. Release 构建

- R8/资源压缩开启。
- signing key 不进仓库。
- `debuggable=false`。
- 禁止 release 网络明文调试器。
- 检查 backup rules。
- 检查 exported components。
- 检查所有 ContentProvider/Intent 输入。

---

## 22. 新手最重要的 12 条

1. 不要在 Composable 里直接读文件。
2. 不要把 Android `Context` 到处传。
3. 不要假设 URI 就有真实文件 path。
4. 不要用 `GlobalScope`。
5. 不要把 Reader 大对象塞 `UiState`。
6. 不要只在 `onDestroy()` 保存进度。
7. 不要自己发明密码学算法。
8. 不要把密码放 SharedPreferences。
9. 不要把所有代码写在 `MainActivity.kt`。
10. 不要为平板复制一套页面；做 adaptive layout。
11. 不要跳过 migration test。
12. 不要“先做完再优化性能”；Reader 性能必须从架构开始。

---

## 23. 官方依据

- App architecture: https://developer.android.com/topic/architecture
- Android 17 SDK: https://developer.android.com/about/versions/17/setup-sdk
- Edge-to-edge: https://developer.android.com/develop/ui/compose/system/setup-e2e
- Adaptive apps: https://developer.android.com/develop/adaptive-apps/guides/get-started-with-adaptive-apps
- Adaptive navigation: https://developer.android.com/develop/adaptive-apps/guides/build-adaptive-navigation
- Predictive back: https://developer.android.com/develop/ui/compose/system/predictive-back-setup
- SAF: https://developer.android.com/training/data-storage/shared/documents-files
- Baseline Profiles: https://developer.android.com/develop/ui/compose/performance/baseline-profiles
