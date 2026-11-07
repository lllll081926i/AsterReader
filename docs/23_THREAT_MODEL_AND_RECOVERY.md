# 23 威胁模型与恢复设计

## 1. 我们要防什么

### 主要威胁

- 手机借给他人时被直接打开 App。
- 最近任务缩略图泄露封面/标题。
- 受保护文件从存储中被复制后直接查看。
- App backup 泄露敏感元数据/密钥。
- WebDAV 凭据明文泄露。
- App 崩溃留下明文临时文件。

### 能降低但不能承诺完全防御

- Root 后的恶意系统级攻击。
- 已解锁设备上拥有高权限的恶意软件。
- 用户在允许截图模式下主动截屏。
- 屏幕被摄像头拍摄。
- 运行时内存取证。

文案不能宣称“绝对安全”。

---

## 2. 风险矩阵

| 场景 | 防护 | 剩余风险 |
|---|---|---|
| 他人拿到已锁屏手机 | AppLock + system credential | 系统锁本身强度 |
| 他人拿到已解锁手机 | AppLock timeout | timeout 窗口 |
| 文件被拷走 | Vault AES-GCM | 弱恢复密码可被猜测 |
| App 数据目录被备份 | backup exclusions | root/forensic |
| 最近任务 | no recents screenshot | OEM 特殊行为需测试 |
| 截图/投屏 | FLAG_SECURE | OEM/系统边界 |
| 短 PIN | UI gate + Keystore | 不等价高熵密码 |
| 卸载 App | recovery password slot | 没恢复槽则不可恢复 |

---

## 3. 密钥丢失场景

### App 被卸载

Android Keystore key 通常随 app data 消失。若 vault 只有 Device Slot：**数据永久不可解密**。

因此首次启用 Vault 时明确给两种选择：

1. 设备绑定（最简单；卸载即失去访问）
2. 设置恢复密码（推荐；可导入新设备）

### 用户改变锁屏/生物识别

部分认证绑定 key 可能失效。实现必须捕获 key invalidation，并尝试 recovery slot。

### 忘记 App PIN

如果系统认证仍允许：可认证后重置 PIN。  
如果策略是 PIN-only 且没有 recovery：无法绕过。

---

## 4. 数据损坏恢复

Vault 每文件独立，因此一文件损坏不影响整个 Vault。

提供：

- 验证 Vault
- 重新扫描 vault manifest
- 标记损坏 chunk
- 导出诊断（不含明文/key）
- 从 backup 恢复单文件/metadata

---

## 5. 加密迁移中断

`.staging` 文件必须与 committed 文件区分。

启动清理：

```text
find stale staging
-> if DB transaction committed? reconcile
-> else delete ciphertext staging only
```

绝不因发现 staging 自动删除用户原文件。

---

## 6. 安全 UX

高风险操作必须显示后果：

- “删除原文件后，其他阅读器将无法打开它。”
- “如果不设置恢复密码，卸载应用后保险库无法恢复。”
- “4 位 PIN 仅用于本机快捷访问，不是强恢复密码。”

不使用吓人的文案，但后果必须准确。

## 7. WebDAV 读取边界（2026-11-07）

- 连接凭据与地址采用独立 Keystore AES-256-GCM 密钥加密，存储于 noBackupFilesDir；认证失败或密钥缺失不覆盖原密文。凭据不随书库清单导出。
- WebDAV 设置页启用安全窗口；密码不进入 SavedState。切换服务商清除旧凭据。HTTPS 默认启用系统证书校验；HTTP 必须逐连接显式授权。
- 仅发送 PROPFIND 与 GET。拒绝自动重定向、跨源 href 和配置根目录之外的路径，防止向其他服务器转发认证。
- XML 响应与文件下载具有字节预算；XML 还限制条目、深度和字段长度。下载先写可再生缓存，校验完整后提交，取消不保留可打开的半文件。
- 普通下载缓存为应用私有明文，受 Android 沙箱保护，不能称为保险库密文。连接移除和缓存清理删除可再生下载副本，保留远端原件与阅读记录。
- 保险库导入成功后清理该书的 WebDAV 下载缓存，但不能删除远端原件。使用者需单独管理云端文件的访问权限。
- 设备已解锁且应用被攻破、root 或服务器自身泄露，不属于本地 Keystore 凭据加密可以消除的风险。真实服务认证、限流与额度需要独立验收。
