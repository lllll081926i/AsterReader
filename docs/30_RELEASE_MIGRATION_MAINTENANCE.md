# 30 Release, Migration & Maintenance Strategy

## 1. 版本规则

使用 SemVer 风格：

- `0.x`：公开测试，schema/API 可变但必须迁移用户数据。
- `1.0`：第一版正式稳定承诺。
- `1.x`：兼容升级。
- `2.0`：只有真正需要大迁移时才使用。

Android `versionCode` 单调递增，不从 Git commit 数临时生成正式版。

## 2. 分支

推荐简单 trunk-based：

- `main`：随时应可构建。
- 短命 `feature/*` / `fix/*`。
- 发布时打 `vX.Y.Z` tag。

不维护长期 `develop` 分支，避免第一次项目被 merge 管理拖垮。

## 3. 发布通道

- Internal：每 Sprint。
- Alpha：功能未完整但数据安全可控。
- Beta：Feature complete，专注兼容与质量。
- RC：冻结 Feature。
- Stable。

每个通道使用不同 applicationIdSuffix 或明确 build flavor，禁止测试版覆盖唯一稳定数据而无备份。

## 4. Database migration

原则：用户 Library、进度、笔记、书签、Vault metadata 是资产。

维护：

```text
schemas/
  1.json
  2.json
  ...
```

每个 schema 版本至少一个 migration test，从最早公开版本一路升级到 current。

## 5. Vault container migration

ASV 格式必须带版本，例如：

```text
magic = ASTERVLT
version = 1
headerLength
flags
objectId
chunkSize
wrappedDek...
```

以后 ASV2 出现时：

- Reader 保持 V1 read support 至少一个 major release 周期。
- 迁移通过 background worker 做 copy/verify/commit。
- 不原地重写唯一密文。

## 6. Backup format migration

Backup 文件必须含：

- format version
- app version
- schema version
- creation time
- capabilities
- manifest hash

导入流程先 dry-run，展示：将新增/覆盖/冲突/跳过什么。

## 7. Dependency policy

每月最多安排一个“依赖维护窗口”，除严重 security fix 外不在功能 Sprint 中大面积升级。

升级优先级：

1. Android/Jetpack security/compatibility 修复。
2. Reader engine correctness。
3. Stable library bugfix。
4. Alpha 新功能。

## 8. Android API 升级

新的 targetSdk 不在发布前一天升级。流程：

1. 阅读 behavior changes。
2. 建独立升级分支。
3. Android Studio SDK Upgrade Assistant。
4. storage/background/notifications/edge-to-edge/back/backup/security 回归。
5. API 31 边界保持不变，除非单独 ADR 决定提高 minSdk。

## 9. Crash/diagnostics privacy

任何 crash/telemetry 若未来加入：

- 默认不上传完整文件路径。
- 不上传书名/作者/笔记正文/搜索词/解密内容。
- 不上传密钥、nonce、wrapped secret。
- publication 使用随机诊断 ID。
- 用户可以关闭。

## 10. 长期维护预算

每个稳定版本周期至少安排：

- 依赖升级。
- Android preview 兼容测试。
- corrupted fixture 回归。
- performance benchmark。
- security threat model review。
- license review。

