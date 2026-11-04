# 11 安全与隐私总规范

> v2.0 起，本项目把安全从“凭据不明文”升级为完整的 **应用锁 + 局部访问锁 + 文件保险库加密 + 解密阅读 + 恢复模型**。

## 1. 安全原则

- 最小权限。
- 本地优先、无强制账号。
- 安全语义不欺骗：UI 锁 != 文件加密。
- “加密并上锁”必须真的把文件变为密文。
- 密钥不进入应用数据库、日志、备份。
- 解密内容优先只存在内存，不生成明文临时文件。
- 安全失败必须 fail closed：认证/完整性失败时不输出部分明文。

## 2. 应用锁

支持：

- Android 系统屏锁 / 强生物识别：`BiometricPrompt`
- 应用 PIN/密码
- 任一解锁 / 双重认证
- 自动锁定 timeout
- 屏幕关闭立即锁

Root UI 必须先做 `SecurityBootstrap -> AuthGate`，不能先绘制书库再盖锁屏。

## 3. 局部保护

保护粒度：

- 单本书
- 文件
- 文件夹
- 书架
- Repository

主安全动作叫 **“加密并上锁”**：迁移至 Vault，磁盘保存密文。
另有高级 **“访问锁”**：只在 App 内要求认证，不宣称文件加密。

## 4. 文件加密

- Android Keystore 保护 device KEK。
- Vault Master Secret 包装每文件 DEK。
- Per-file AES-256-GCM。
- 大文件分块认证加密。
- 每 chunk 唯一 nonce + AAD。
- 文件读取使用 `SecureDocumentSource / DecryptingSeekableSource` 按需解密。

详细：`19_SECURITY_VAULT_AND_APP_LOCK.md`。

## 5. 受保护元数据

保护内容不得把真实标题、封面、全文索引直接暴露在普通 Room/FTS 查询链中。

锁定时 UI 只拿 `ProtectedPlaceholder`。

## 6. 屏幕隐私

- AuthGate：隐藏 Recents snapshot。
- Protected Reader / Detail：默认 `FLAG_SECURE`。
- Android 13+ 可用 `setRecentsScreenshotEnabled(false)`。

## 7. 权限

- 文件访问优先 SAF。
- 不默认请求 all-files access。
- 只 persist 用户明确授权的 URI。
- 删除原文件必须 source 具有 DELETE 能力 + 用户明确确认。

## 9. 压缩包安全

- Zip Slip path normalization。
- entry count 上限。
- uncompressed size / compression ratio 检测。
- image pixel bomb 检测。
- 异常 archive 不后台无限展开。

## 10. Backup

- Vault files 默认不进系统 Auto Backup。
- Keystore key 不备份。
- 提供 Aster encrypted backup + optional recovery password。
- 没有 recovery slot 时必须警告：卸载/清数据后 Vault 可能永久不可恢复。

## 11. 隐私默认

- 无广告 SDK。
- 无行为追踪 SDK。
- 无默认云同步。
- 崩溃数据默认本地或明确 opt-in。
- 诊断导出主动触发，受保护标题/路径需脱敏。

## 12. 必读

- `19_SECURITY_VAULT_AND_APP_LOCK.md`
- `23_THREAT_MODEL_AND_RECOVERY.md`
- `25_PRIVACY_LOCK_UX_SPEC.md`
