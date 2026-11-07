# 正式 LOGO 与根目录整理

日期：2026-11-07。范围：正式图标、关于页图标、根目录文件归类与备份整理。

## LOGO

原根目录 `logo.png` 移至 `assets/branding/source/logo-original.png`，原始 SHA-256 为 `7750795ed1a233a7181b70d5f40d2fea641c80cc12eb1422feface4e61ae4086`。生成器和完整尺寸说明见 [品牌资源](../assets/branding/README.md)。保留原图与透明主体，生成圆角展示图、五种 Android 密度、自适应图层和独立单色图层。关于页使用正式圆角资源；Manifest 继续使用既有资源名。

## 文件归类

| 原根目录文件 | 新位置 |
|---|---|
| `ROADMAP.md` | `planning/ROADMAP.md` |
| `CHANGELOG_V2.md`、`CHANGELOG_FINAL.md` | `docs/history/` |
| `VALIDATION_REPORT.md`、`UIUX_DOC_VALIDATION.md`、`CHECKSUMS.sha256` | `docs/history/reports/` |
| `MASTER_ENGINEERING_DOSSIER.md`、`UIUX_READABLE_OFFLINE.html` | `docs/archive/` |
| `metadata.json` | `docs/archive/ai-studio/` |
| `debug.keystore`、`my-upload-key.jks` | `.local/signing/`，已忽略 Git，Gradle 路径同步更新 |

根目录保留 README、AGENTS、design 和实际构建配置。README、路线与开发计划的活动引用同步更新。历史报告和校验清单保留原始内容，并标注历史用途。

## 备份与安全

现用正式签名及 DPAPI 配置仍在仓库外 `%LOCALAPPDATA%/AsterReader/Signing/`，GitHub Secrets 不变。两个旧密钥仅移动文件，不重新生成，保持 Debug 覆盖安装能力。

原 `.trash/scan-diagnostics-20261107/` 含数据库与 ZIP，归入 `.local/backups/scan-diagnostics-20261107/` 继续保留，不纳入 Git。旧界面临时副本 `.trash/aster-ui-review/`、`.trash/settings-about/` 无构建引用，移入 Windows 回收站，可恢复。其他会话的 `.workbuddy/` 不作处理。

本次不修改漫画文件、数据库结构、访问锁或保险库数据。

## 核验记录

- 24 个生成资源的 SHA-256、尺寸、圆角透明边缘和自适应主体安全圆检查通过；原图校验一致。
- 本次维护的 README、文档索引、路线和资源说明的本地链接检查通过。
- API 31/35 的 4 项关于页测试通过，包括深色 200% 字体；截图位于 `app/build/ui-review/about-*.png`。
- `:app:lintDebug`、`:app:assembleDebug` 通过，Lint 0 错误、52 项警告；日志 `build/branding-root-validation.log`。
- API 35 模拟器 Debug 覆盖安装成功，保留同一签名及用户数据；桌面截图 `build/branding-launcher.png` 已确认正式 LOGO。关于页正常显示，保留截图保护。
- API 31/37 设备手动验证及实际系统主题图标开关尚未验收；单色层已通过资源与预览检查。
- 密钥和数据库备份被 Git 忽略；两个旧界面备份通过 Windows 回收站接口处理，未执行硬删除。
