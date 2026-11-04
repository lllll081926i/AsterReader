# 32 Security Verification Plan

## 1. 安全目标

Aster Reader 不承诺对已经 root 且完全控制运行时的攻击者提供 DRM 级保护。目标是可靠保护：

- 静态存储中的受保护书籍/漫画。
- App 未认证状态下的内容访问。
- 最近任务/截图等常见 UI 泄露。
- 意外备份泄露。
- 文件篡改与损坏的可检测性。

## 2. 必测攻击/故障场景

### Auth

- 生物识别成功/失败/取消。
- device credential 成功/取消。
- 没有注册生物识别。
- 没有系统锁屏。
- App PIN 错误、暴力尝试节流。
- background timeout。
- process kill 后重启。
- configuration change 正在认证。

### Keystore

- key alias 不存在。
- key invalidated。
- user changes lock-screen credential。
- secure hardware unavailable。
- restore 到另一设备时 device slot 不可用。

### Vault

- header 1 byte corruption。
- chunk ciphertext corruption。
- GCM tag corruption。
- truncated file。
- reordered chunk。
- wrong DEK。
- wrong object ID/AAD。
- migration worker kill at 0/25/50/99%。
- storage full。
- source permission revoked mid migration。

### Privacy

锁定时检查：

- Library card。
- Search/FTS。
- Recent history。
- Android recents。
- screenshot。
- notification。
- accessibility tree。
- logs。
- crash report。

## 3. Test vectors

仓库保存固定测试向量：

- plaintext fixture。
- deterministic metadata fixture。
- expected container structural assertions。
- corrupted variants。

**不要固定生产 key/nonce。** 测试可以注入 deterministic RNG，但 production implementation 必须 SecureRandom。

## 4. Fail-closed 原则

以下状态一律拒绝解密而不是“尝试继续”：

- tag invalid。
- unsupported container version。
- key unavailable。
- metadata authentication failed。
- chunk index invalid/out of range。

UI 给出可理解错误，但不把低层密钥细节显示给普通用户。

## 5. 原文件删除安全

Encrypt & Lock 默认：

```text
read source
→ write encrypted temp/container.new
→ fsync/close
→ verify header + sampled/full integrity according to policy
→ atomically publish Vault record
→ mark migration committed
→ ONLY THEN offer/delete source
```

如果 SAF provider 不保证 atomic rename，则以数据库 transaction + immutable final object + recovery journal 保证可恢复，而不是假设 POSIX 原子语义。
