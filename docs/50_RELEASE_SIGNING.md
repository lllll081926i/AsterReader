# 本地签名与 GitHub 发包

日期：2026-11-07。用户授权自行分发签名，并把同一签名注入 GitHub 发包流程。不依赖应用商店或购买证书。签名用于证明同一发布者和允许后续覆盖升级，不证明程序没有漏洞。上线审查尚未关闭的问题仍见 docs/49。

## 密钥与本地构建

- 密钥位于 `%LOCALAPPDATA%/AsterReader/Signing/aster-reader-release.p12`，在仓库之外；原来的 `my-upload-key.jks` 保留，未覆盖或删除。
- RSA 4096、SHA256withRSA、自签名证书有效期 10000 天、别名 `upload`。随机口令不写入源码、文档或日志。
- 同目录 `release-signing.json` 的口令由 Windows 当前账户 DPAPI 加密。目录 ACL 仅当前账户和 SYSTEM。`release-certificate.cer` 是可公开证书，不含私钥。
- 证书 SHA-256：`EB1F0D147AD3CA031A8E0E63245865D4AD4377C16061F3693A4FB45CA02B9B2A`。

在 PowerShell 7、JDK 17 环境中执行：

```powershell
./scripts/Release-Signing.ps1 -Action Info
./scripts/Release-Signing.ps1 -Action Build
./scripts/Release-Signing.ps1 -Action SyncGitHub
```

`Initialize` 只用于首次建立；已有密钥或配置时拒绝覆盖。`Build` 临时注入现有 Gradle 要求的 `KEYSTORE_PATH/STORE_PASSWORD/KEY_PASSWORD`，退出后恢复环境。签名构建关闭配置缓存和常驻 daemon，避免把签名配置留在新的配置缓存里。没有修改原来的 signingConfigs、debug 密钥或 SDK 版本。

**长期保留同一把密钥。** DPAPI 依赖当前 Windows 用户凭据；只复制 JSON 到另一台机器不能解密。重装 Windows 或删除该账户前，需要把密钥及其实际口令存入独立安全备份或密码管理器；GitHub Secrets 不支持读回口令，不能作为可取回的备份。此脚本不自动导出明文口令，也没有把私钥推入 Git。不要重新 Initialize 代替恢复旧密钥。

Debug 与 Release 证书不同，不能直接覆盖安装。切换签名需要另一应用身份、卸载或正式签名轮换方案；卸载会删除本地数据。此次只验证正式包，不卸载模拟器现有 Debug，不重置用户保险库。

## GitHub 配置

仓库：`lllll081926i/AsterReader`。已注入以下 repository Secrets：

- `ANDROID_RELEASE_KEYSTORE_BASE64`
- `ANDROID_RELEASE_STORE_PASSWORD`
- `ANDROID_RELEASE_KEY_PASSWORD`

公开变量 `ANDROID_RELEASE_CERT_SHA256` 保存固定证书指纹，构建后检查 APK 证书是否相符。Secrets 通过 `gh secret set` 标准输入注入，不放入进程命令行参数。GitHub 私有仓库的维护者和可修改工作流的人仍处于签名信任边界内；保护账户、限制写权限，不运行来源不明的工作流。

## 工作流

`.github/workflows/release.yml`：

1. 手动运行只测试、构建与保存产物，不创建公开 Release。
2. 推送 `v*` 标签时，标签必须等于 `v` 加当前 versionName。全部检查成功后只创建**草稿 Release**。
3. 固定 JDK 17、SDK/Build Tools 37；执行 app/database/vault 单测和 Release Lint。
4. 临时私有目录解码密钥，签名构建 APK 与 AAB；离开步骤时清理本步骤的临时密钥文件，不上传目录或配置缓存。
5. 验证 APK 签名、预期证书、AAB 签名、16 KB ZIP 对齐，计算 SHA-256。
6. 保存 APK、AAB、证书信息、校验值及 R8 mapping。mapping 仅进入构建 artifact，不附加到分发 Release。
7. 已公开的同名 Release 禁止覆盖；同名草稿可以重新上传已校验文件。

Actions 固定为实际 commit SHA；默认只读仓库权限，仅草稿发布 job 获得 contents:write。没有 pull_request_target，不向 PR 运行或第三方步骤暴露签名环境。Gradle 9.3.1 分发校验值来自其官方 `.sha256` 文件，没有升级 Gradle。

正式公开发包需要先关闭 docs/49 中的发布阻断并检查草稿附件、版本与发布说明。本次不创建版本标签，不自动公开 Release。
