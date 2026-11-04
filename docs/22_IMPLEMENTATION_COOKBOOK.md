# 22 Implementation Cookbook：从空工程直接开做

> 本文件按“新手真的要动手”组织。先照顺序建立骨架，再逐块填实现。

## 1. 建项目

Android Studio：Empty Activity（Compose）。

建议：

```text
name: AsterReader
package: com.example.asterreader   # 正式开发前改成自己的永久包名
minSdk: 31
compileSdk: 37
targetSdk: 37
```

项目一开始就 Git init，不要等代码写多再版本控制。

---

## 2. 创建模块

第一批：

```text
:app
:core:model
:core:common
:core:designsystem
:core:database
:core:datastore
:core:source-api
:core:engine-api
:core:reader-session
:core:security-api
:core:security-keystore
:core:crypto
:core:vault
:feature:auth
:feature:library
:feature:shelves
:feature:detail
:feature:reader
:feature:settings
:feature:vault
:source:local
:engine:comic
:benchmark
```

其他格式后加，别第一天创建 30 个空模块。

---

## 3. Version Catalog

所有依赖集中 `gradle/libs.versions.toml`。

禁止 feature 模块写不同版本号。

推荐基线详见 `24_BUILD_DEPENDENCY_BASELINE.md`。

---

## 4. App 根结构

```kotlin
@Composable
fun AsterApp() {
    AsterTheme {
        val bootstrapState = ...
        when (bootstrapState) {
            Loading -> BootstrapScreen()
            Locked -> AuthGate()
            Ready -> AsterNavigation()
        }
    }
}
```

**不要**：

```kotlin
AsterNavigation()
LockOverlayIfNeeded()
```

因为后者可能先绘制敏感书库。

---

## 5. Dynamic Color + Expressive Motion

示意：

```kotlin
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AsterTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colors = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && darkTheme ->
            dynamicDarkColorScheme(context)
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            dynamicLightColorScheme(context)
        darkTheme -> AsterDarkFallback
        else -> AsterLightFallback
    }

    MaterialTheme(
        colorScheme = colors,
        typography = AsterTypography,
        shapes = AsterShapes,
        motionScheme = MotionScheme.expressive(),
        content = content,
    )
}
```

若 API 名随 alpha 变化，只允许在 `:core:designsystem` 一处修。

---

## 6. Edge-to-edge

`MainActivity`：

```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    enableEdgeToEdge()
    super.onCreate(savedInstanceState)
    setContent { AsterApp() }
}
```

页面使用 `Scaffold` / Insets，不手写状态栏高度。

---

## 7. App Lock API

先定义接口再实现：

```kotlin
interface AppLockManager {
    val state: StateFlow<AppLockState>
    suspend fun requireUnlock(reason: UnlockReason): UnlockResult
    fun onAppBackgrounded()
    fun onAppForegrounded()
    fun lockNow()
}
```

认证 UI 在 `feature:auth`，BiometricPrompt 适配在 platform 实现层。

---

## 8. BiometricPrompt

允许系统屏锁回退：

```kotlin
val authenticators =
    BIOMETRIC_STRONG or DEVICE_CREDENTIAL

val promptInfo = BiometricPrompt.PromptInfo.Builder()
    .setTitle("解锁 Aster Reader")
    .setAllowedAuthenticators(authenticators)
    .build()
```

注意：设置 `DEVICE_CREDENTIAL` 时不要同时设置 negative button text。

---

## 9. Vault API

```kotlin
interface VaultManager {
    suspend fun encryptAndImport(
        source: DocumentSource,
        request: VaultImportRequest,
    ): VaultItem

    suspend fun open(itemId: VaultItemId): SecureDocumentSource

    suspend fun removeProtection(itemId: VaultItemId, exportTarget: Uri?): Result<Unit>
}
```

`open()` 返回可随机读取的 secure source，不返回临时明文 path。

---

## 10. 加密伪代码

```kotlin
suspend fun encryptStream(
    input: Source,
    output: Sink,
    fileKey: SecretKeyMaterial,
) {
    var index = 0L
    while (true) {
        val plain = input.readChunk(CHUNK_SIZE) ?: break
        val nonce = nonceFor(index)
        val aad = aadFor(index, plain.size)
        val encrypted = aesGcmEncrypt(fileKey, nonce, aad, plain)
        output.writeChunk(encrypted)
        plain.zeroIfPossible()
        index++
    }
}
```

真实实现必须使用标准 JCA/Keystore Cipher，不自己实现 AES。

---

## 11. Protected Publication 数据读取

```text
Library DB query
  -> sees isProtected
  -> locked?
       yes: return ProtectedPlaceholder model
       no : decrypt metadata -> normal UI model
```

不要查询后再把 title 替换成“***”；那时真实 title 已经进入内存/日志/Compose state。

---

## 12. 本地 Source

SAF 流程：

```text
Add repository
-> ACTION_OPEN_DOCUMENT_TREE
-> takePersistableUriPermission
-> save SourceEntity(rootUri)
-> enqueue scan
```

永远围绕 `Uri` / `DocumentFile` / provider API 工作，不依赖 `/storage/emulated/0/...`。

---

## 13. Scanner

两阶段：

```text
SCAN/STAGE
  -> discover files
  -> fingerprints
  -> proposed publications

COMMIT
  -> Room transaction
  -> enqueue covers/index
```

这样中途失败不会把书库写成半成品。

---

## 14. Reader Session

```kotlin
interface ReaderSession : AutoCloseable {
    val state: StateFlow<ReaderSessionState>
    val capabilities: ReaderCapabilities
    suspend fun seek(locator: Locator)
    suspend fun next()
    suspend fun previous()
}
```

Reader ViewModel 保存 locator，不保存“session 可永久复活”的假设。

---

## 15. Room 数据库第一版

先做：

```text
SourceEntity
PublicationEntity
ReadingProgressEntity
ShelfEntity
ShelfPublicationCrossRef
BookmarkEntity
ProtectionEntity
VaultItemEntity
```

FTS 等核心 CRUD 稳定后再加。

---

## 16. Backup Rules

必须显式定义：

- vault files 默认排除系统云备份
- device-bound wrapped keys 排除
- cache 排除
- 用户普通 settings 可备份

项目 `minSdk = 31`，统一使用 Android 12+ 的 `android:dataExtractionRules`。无需为 Android 11 及以下维护运行时兼容逻辑；如构建工具或第三方测试需要保留 `fullBackupContent`，仅作为显式空/排除规则，不承诺旧系统支持。

---

## 17. Recents / Screenshot

受保护页面：

```kotlin
window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
```

Android 13+：

```kotlin
setRecentsScreenshotEnabled(false)
```

离开受保护页面后按策略恢复截图能力。

---

## 18. Navigation 3

顶级：Library / Shelves。
详情、Settings、Vault 为二级 destination。
Reader 可作为全屏 destination，但保持单 Activity。

不要第一版用多个 Activity 分功能，否则 predictive back、共享元素、状态管理都更复杂。

---

## 19. 第一个可交付里程碑

只做下面这些就提交一个可运行 build：

- Dynamic Color
- Expressive/统一 Motion
- Library 空状态
- Shelves 空页面
- Settings
- App Lock 设置与 AuthGate
- Add local repository SAF
- Room 写入 Source

此阶段只实现已明确的漫画范围。

---

## 20. 第二个里程碑

- 扫描图片目录/CBZ
- 首页出现书籍
- 详情页
- Comic Reader
- Progress
- Encrypt & Lock 单本漫画

做到这里，App 已经是真正可用的本地漫画阅读器。

---

## 21. 常见错误排查顺序

构建错误：

1. 看第一条 `Caused by`，不是最后 100 行。
2. 检查 Gradle/AGP/JDK 兼容。
3. 检查 alpha Material API 是否改名。
4. `./gradlew :app:assembleDebug --stacktrace`

运行崩溃：

1. Logcat 找当前 package。
2. 先看 exception type + first app-owned stack frame。
3. 不要“清缓存重装”掩盖 migration bug。

SAF 问题：

1. URI permission 是否 persisted。
2. provider 是否支持 seek/write/delete。
3. 不要转换 real path。

Vault 问题：

1. 先验证 ciphertext 完整性。
2. 不要在失败时输出 key/plaintext 到日志。
3. 不要自动删除源文件。
