# 24 构建与依赖基线（2026-09-29）

> 依赖必须统一放 Version Catalog。这里记录“已调研的工程基线”，真正升级时单独 PR，不在功能 PR 顺手升级一堆库。

## 平台

- Android 17 SDK / API 37
- `compileSdk = 37`
- `targetSdk = 37`
- `minSdk = 31`

官方： https://developer.android.com/about/versions/17/setup-sdk

## 平台冻结决策 ADR-001

**最低支持版本固定为 Android 12 / API 31。** 这是 Final v3.0 的架构冻结项，不在普通功能 PR 中修改。理由：

- Android 12 是系统 Monet / Dynamic Color 的原生起点。
- 项目从第一天就以 Material 3 / Material You 为统一视觉基线，不维护 Android 11 及以下的第二套主题与系统行为分支。
- App Lock、Android Keystore、SAF、Room、WorkManager、Compose 等核心能力在 API 31 上均可完整实现。
- 高版本功能使用渐进增强；Android 12 用户必须拥有完整核心功能，不是阉割版。

平台矩阵：

```text
minSdk     = 31  (Android 12)
targetSdk  = 37  (Android 17)
compileSdk = 37
```

任何依赖升级都必须先验证 `minSdk <= 31`、API 31 instrumentation smoke test、API 37 行为测试。

## Toolchain

建议首个正式工程锁定以下 2026-09-29 基线：

- Android Gradle Plugin：`9.4.x`（官方 9.4 支持最高 API 37；初始工程优先使用当前稳定 patch）。
- Gradle：`9.6.x`（AGP 9.4 官方默认/最低基线）。
- JDK：`17`。
- Kotlin：`2.4.20`。

官方：

- https://developer.android.com/build/releases/agp-9-4-0-release-notes
- https://developer.android.com/build/releases/about-agp
- https://kotlinlang.org/docs/whatsnew2420.html

注意：真正创建工程当天，若 Android Studio Stable 已带更高的 **patch** 版本（例如 AGP 9.4.x），可用最新 patch；不得未经验证跨 minor/major。

## Compose

截至 2026-09-23：

- Compose UI/Foundation/Animation stable：`1.12.1`
- Material3 stable：`1.4.0`
- Material3 alpha：`1.5.0-alpha29`

`MotionScheme.expressive()` 在 `1.5.0-alpha29` 提供，因此项目如要使用最新 Expressive Motion，需要接受 alpha 风险。

官方：

- https://developer.android.com/jetpack/androidx/releases/compose
- https://developer.android.com/jetpack/androidx/releases/compose-material3

## Navigation 3

截至 2026-09-23 stable：`1.2.0`。

https://developer.android.com/jetpack/androidx/releases/navigation3

## Material3 Adaptive

截至 2026-08-12 stable：`1.3.0`。
1.4 系列仍为 alpha。

https://developer.android.com/jetpack/androidx/releases/compose-material3-adaptive

## Room

截至 2026-09-09 stable：`2.8.5`。

https://developer.android.com/jetpack/androidx/releases/room

## DataStore

截至 2026-09-09 stable：`1.2.1`。

https://developer.android.com/jetpack/androidx/releases/datastore

## WorkManager

2026-09 稳定通道：`2.12.0`。

https://developer.android.com/jetpack/androidx/versions/stable-channel

## Biometric

稳定版仍为 `1.1.0`；存在更新 alpha。若仅使用成熟 `BiometricPrompt` + DEVICE_CREDENTIAL 能力，可优先 stable；需要新版 API 再评估 alpha。

https://developer.android.com/jetpack/androidx/releases/biometric

## Benchmark

稳定通道 2026-09：Benchmark `1.5.0`。

用于 Macrobenchmark / Baseline Profile。

## 已确认的核心第三方基线

- Dagger/Hilt：Dagger `2.60.1` 为当前最新 release；工程初始化时锁定对应 Hilt 依赖并跑 KSP/AGP 兼容构建。
- Coil：`3.6.3`（2026-09-18，包含针对 AGP 9.4/R8 的构建修复）。

来源：

- https://dagger.dev/
- https://coil-kt.github.io/coil/changelog/

仍建议在第一次 `./gradlew build` 成功后，把最终解析版本写入 Version Catalog 和 dependency lockfile；文档中的版本是“初始化候选”，不是无条件永久冻结。

## 版本策略

### 稳定优先

Room / DataStore / WorkManager / Navigation 3 / Adaptive 默认用 stable。

### 唯一允许主动使用 alpha 的核心候选

Material3 `1.5.0-alpha29`：因为项目明确追求最新 M3 Expressive `MotionScheme`。

条件：

- 版本锁死。
- 设计系统把 experimental API 包装起来。
- 每次升级单独 PR。
- UI screenshot test + predictive back + bottom sheet + search 回归。

### 不写死的库

Hilt、Coil、dav4jvm、libarchive 等在真正初始化工程时根据各自官方 release 再锁版本，并记录 commit/tag/license。
