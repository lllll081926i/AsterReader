# Aster Reader

Aster Reader 是 Android 本地漫画阅读器。当前实现使用 Kotlin、Jetpack Compose、Material 3 和系统 Monet 动态色。功能完成度以实现、测试与设备验收记录为准。

## 当前能力

- 通过 SAF 导入文件和目录，递归扫描仓库；失联来源可重新定位，并保留作品身份、进度和批注。
- 读取 CBZ/ZIP、CBR/RAR、CB7/7Z、CBT/TAR、PDF、单图和图片目录。RAR5、加密归档与 DRM 不在当前支持范围。
- EPUB 仅提取图片用于漫画阅读；未实现 EPUB 重排正文或 MOBI 阅读。
- 支持纵向滚动、分页、双页、独立翻页方向、缩放和平移；单本配置可以覆盖全局默认值。
- 具有书架、收藏、标签、搜索、书签、笔记和本机阅读时长统计。
- 设置按分类和独立页面组织，统一紧凑标题栏、行高、边距与返回方向；支持减少动效。
- 应用 PIN 与系统生物识别用于访问锁。隐藏作品仍是原文件，**访问锁不等于文件加密**。

## 当前边界

- WebDAV 入口：设置 → 仓库 → WebDAV。内置坚果云、Nextcloud、群晖 NAS 与自定义预设，支持多个账户。提供连接测试、目录浏览、点选漫画阅读、目录加入书架与自动封面。凭据由 Keystore 加密保存；默认 HTTPS，HTTP 需明确允许。完整下载后读取，单文件 512 MB，远程缓存上限 768 MB；不包含范围流式、自动上传或跨设备进度同步。详见 [WebDAV 读取](docs/51_WEBDAV_SOURCE.md)。

- 单文件导入上限为 512 MB；损坏或不支持的文档会报告失败。
- 系列自动识别仅采用明确卷号标记；读完后由用户主动打开下一卷。
- Vault 已实现 Keystore 设备密钥、独立长口令、分块密文读取、逐作品加密导出与跨设备恢复。尚无整库快照、独立应急恢复槽或解除加密导出明文；没有已验证的导出文件时，设备密钥丢失不可恢复。
- Navigation 3、Material3 Adaptive 与 Hilt 是冻结架构目标，目前尚未完整接入。
- API 31/API 37 真机、TalkBack、系统返回手势与性能验收尚未完成。桌面截图测试不能替代设备验收。

## 模块

```text
core/model          数据模型与阅读定位契约
core/database       Room 实体、DAO 与事务
core/designsystem   Monet、字体、尺寸与动效令牌
core/vault          认证密文容器、口令派生与防猜测
app                 系统集成、页面、ViewModel、仓库与渲染实现
```

旧契约文件保存在 `starter-contracts/`，不属于当前运行模块。

## 构建

平台固定为 `minSdk=31`、`compileSdk=37`、`targetSdk=37`。使用 JDK 17、Gradle 9.3.1、AGP 9.1.1、Room 2.7.0。依赖版本以 `gradle/libs.versions.toml` 为准。

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat lintDebug
.\gradlew.bat assembleDebug
```

Debug APK 位于 `app/build/outputs/apk/debug/app-debug.apk`。

Release 构建读取 `KEYSTORE_PATH`、`STORE_PASSWORD` 和 `KEY_PASSWORD`，别名为 `upload`。缺少有效签名配置时不能生成正式发布包。

本机已配置仓库外的长期签名。使用 PowerShell 7 执行 `./scripts/Release-Signing.ps1 -Action Build`，无需手工填写口令。GitHub 发包工作流使用同一签名，手动运行保存构建产物，版本标签仅创建草稿 Release。密钥保管、证书指纹与操作说明见 [本地签名与 GitHub 发包](docs/50_RELEASE_SIGNING.md)。

## 书库清单备份

系统备份关闭。通过“设置 → 备份”导出或合并恢复 JSON 清单，也可使用口令加密清单。清单包含公开作品的元数据、进度、书架、批注、页索引和单本配置，不包含漫画文件、隐藏作品或安全凭据。逐作品完整密文导出位于作品详情菜单，与清单导出分开。

恢复先验证文件，再事务提交。重复批注去重，较旧进度不覆盖本机较新进度。新恢复作品的 SAF 授权不能跨设备转移，需要重新扫描或定位来源。此 JSON 清单不是加密备份。

## 审查与验收

- 原始问题清单：`docs/40_FULL_PROJECT_AUDIT_2026-11-07.md`
- 修复与当前验证：`docs/41_FIX_PROGRESS_2026-11-07.md`
- 页面实施矩阵：`planning/UI_SCREEN_TASK_MATRIX.csv`

本项目使用 [MIT License](LICENSE)。
