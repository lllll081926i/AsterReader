# 35 Final Pre-Development Checklist — 开工前最后检查

## 产品

- [x] Local-first 定位确定。
- [x] 漫画专用资料库确定。
- [x] v1.0 scope 与 P1 延后项确定。
- [x] App Lock / Encrypt & Lock 语义确定。

## 平台

- [x] minSdk 31 / Android 12。
- [x] target/compile SDK 37。
- [x] Android 原生 Kotlin + Compose。
- [x] Navigation 3 / Adaptive。

## Design

- [x] `design.md` 是唯一设计真源。
- [x] Monet 默认。
- [x] Material 3 Expressive 通过单点封装。
- [x] Motion / Reduced Motion / Predictive Back 规则确定。
- [x] Compact/Medium/Expanded 原则确定。

## Data / Storage

- [x] SAF 是本地目录标准入口。
- [x] Room schema / migration 原则确定。
- [x] Repository / SourceProvider 边界确定。

## Security

- [x] Biometric + device credential + App PIN/Password 策略确定。
- [x] Android Keystore key hierarchy 确定。
- [x] AES-256-GCM chunked Vault 确定。
- [x] metadata / cover / FTS 隐私确定。
- [x] recovery 与 lost-key 边界确定。
- [x] backup exclusion 确定。

## Reader

- [x] Reader Shell / Engine 分离。
- [x] DocumentSource 支持普通/远程/Vault。
- [x] locator/progress 单真源原则确定。
- [x] Comic / Text / PDF 独立 engine family 确定。

## Quality

- [x] Definition of Done。
- [x] API 31/33/35/37 测试矩阵。
- [x] Baseline Profile / Macrobenchmark。
- [x] Accessibility/i18n。
- [x] Diagnostics/redaction。

## 正式开工第一批 Issue

建议创建以下 Issue，按顺序执行：

1. `BOOT-001 Create Android project, minSdk31 targetSdk37`
2. `BOOT-002 Configure Version Catalog and CI`
3. `ARCH-001 Create core/feature module skeleton`
4. `DESIGN-001 Implement AsterTheme + Monet`
5. `DESIGN-002 Implement AsterShapes/Typography/Spacing/Motion wrappers`
6. `NAV-001 Navigation3 top-level destinations`
7. `ADAPT-001 NavigationSuite compact/expanded behavior`
8. `DATA-001 Room database v1 + migration test harness`
9. `FILE-001 SAF directory picker + persisted permissions`
10. `LIB-001 Library fake-data vertical slice`

**完成这 10 项后，再进入真实 Scanner 和 Vault。**
