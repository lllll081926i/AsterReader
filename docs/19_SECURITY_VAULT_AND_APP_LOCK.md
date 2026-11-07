# 19 安全保险库、应用锁与局部加密架构

> 本文为整体规划。2026-11-07 文件 Vault 正在按 `46_FILE_VAULT_IMPLEMENTATION.md` 实施，最终能力与验收以该文为准；不能将本文中的批量策略、系统认证绑定和恢复槽等规划一并视为已交付。访问锁、文件 Vault 和公开清单备份保持独立语义。示意容器头不作为当前二进制协议。

> 这是 v2.0 新增的核心文档。目标不是“加一个密码页面”，而是让 **应用锁、单本书锁、文件夹锁、书架锁、仓库锁与真正的磁盘加密** 在同一套安全模型中工作。

## 1. 必须先明确的语义

用户说“上锁以后，文件就加密了，只有阅读时才解密”，工程上必须区分：

### A. Access Lock（访问锁）

只是 App 内要求认证，外部原文件仍是明文。

它适合：

- 用户不想改变原目录。
- 原文件还要给其他 App 使用。

**它不能宣传为“文件已加密”。**

### B. Encrypt & Lock（加密并上锁）

把内容迁移到 Aster Vault，源文件在迁移成功后可由用户选择删除。Vault 中只存密文；阅读时按需解密。

这是本项目默认推荐的“锁定”行为。

---

## 2. 保护粒度

支持以下范围：

- 整个 App
- 单个 Publication（一本漫画或 PDF 漫画）
- 单个文件
- 文件夹 / Repository path
- Shelf（书架）
- Source / Repository（整个本地仓库或远程仓库的访问）

### 2.1 Shelf 的特殊语义

书架本身是“虚拟集合”。若用户选择“加密书架”：

- 实际加密对象是该书架中的 Publications。
- 同一本书即使同时存在于普通书架，也仍然是加密文件。
- 书架只是触发批量策略，不创建第二份明文副本。

若用户只选择“锁定书架入口”：这是 Access Lock，不自动加密文件，UI 必须明确写明。

---

## 3. 认证方式

### 3.1 系统认证（推荐默认）

使用 AndroidX `BiometricPrompt`：

```text
BIOMETRIC_STRONG | DEVICE_CREDENTIAL
```

用户可以用：

- 指纹/面容等强生物识别（设备支持时）
- 系统屏幕锁 PIN / 密码 / 图案

不要自己模拟系统 PIN UI。

### 3.2 应用密码 / PIN

提供两类：

1. **快捷 PIN**：4–8 位，主要用于 App 内访问门禁。
2. **保险库恢复密码**：至少 12 个字符，建议较长随机口令；必须配套版本化 KDF、独立盐与成本参数，用于可恢复密钥包装。长度不构成抗猜测保证。

短 PIN 熵太低，不应单独承担“离线文件被拷走后仍抗暴力破解”的全部责任。

### 3.3 解锁策略

用户可选择：

- 系统认证
- 应用密码
- 任一即可
- 双重认证（高级）

建议默认：**系统认证即可**。

---

## 4. App Lock 状态机

```text
COLD_START
   |
   v
LOCK_REQUIRED? ----no----> APP_READY
   |
  yes
   v
AUTH_GATE
   | success
   v
UNLOCKED_SESSION
   |
   +-- screen off ----------> LOCKED
   +-- timeout -------------> LOCKED
   +-- explicit lock -------> LOCKED
   +-- process death -------> COLD_START
```

### 4.1 自动锁定

设置项：

- 立即
- 15 秒
- 30 秒
- 1 分钟
- 5 分钟
- 仅屏幕关闭时

安全默认建议：**30 秒 + 屏幕关闭立即锁定**。

### 4.2 进入后台

不能只依赖某个 Activity 的 `onPause()`。使用进程级前后台观察（如 `ProcessLifecycleOwner`）维护 session lock deadline。

---

## 5. 启动顺序：防止“先闪出书库再锁屏”

根 UI 必须是：

```text
System Splash
   -> AppBootstrap
      -> SecurityBootstrap
         -> AuthGate OR MainNavigation
```

在 `AuthGate` 通过之前：

- 不查询受保护书籍的真实 metadata。
- 不请求 Coil 加载保护封面。
- 不恢复 Reader 的真实页面。
- 不在 Recents 生成真实内容预览。

---

## 6. 密钥层级：Envelope Encryption

推荐密钥层级：

```text
Android Keystore
  └─ Device KEK (non-exportable; optional user-auth bound)
        └─ wraps Vault Master Secret

Recovery Password (optional)
  └─ KDF -> Recovery KEK
        └─ wraps the SAME Vault Master Secret

Vault Master Secret
  └─ wraps per-file DEK
        └─ AES-256-GCM encrypt file chunks
```

解释：

- **Device KEK**：Android Keystore 生成，尽量硬件支持，不导出。
- **Vault Master Secret**：随机 256-bit；不直接长期明文落盘。
- **Per-file DEK**：每个加密文件独立随机 256-bit，文件替换时重新生成。
- **Recovery Slot**：用户自愿设置恢复密码后，额外包装同一个 Vault Master Secret，便于卸载/换机后的可恢复导入。

### 6.1 为什么不直接用一个 Keystore AES Key 加密所有大文件

因为：

- 大文件需要随机访问与分块。
- 每文件独立 DEK 更容易重加密、迁移、撤销和并发读取。
- Master Secret 只负责包装 DEK，减少核心密钥直接处理大数据。

---

## 7. Android Keystore 规则

Device KEK 使用 Android Keystore：

- AES-256
- `AES/GCM/NoPadding`
- `PURPOSE_ENCRYPT | PURPOSE_DECRYPT`
- `setUserAuthenticationRequired(true)`（当采用系统认证绑定时）
- 允许 `BIOMETRIC_STRONG | DEVICE_CREDENTIAL`

若 StrongBox 可用可优先尝试，但 **StrongBox 不应成为硬依赖**，失败时回退到普通 hardware-backed / TEE Keystore。

明文秘密与 Device KEK 永不写入普通存储、日志或备份。经过认证加密包装的 Master Secret/DEK 可以写入受控槽位和用户主动加密备份，不等同于保存明文密钥。禁止明文写入：

- Room
- DataStore
- logcat
- crash report
- backup
- git

---

## 8. Vault 文件格式

建议自定义简单、可版本化的容器：`ASV1`。

### 8.1 Header（示意）

```text
magic: "ASTERVLT"
version: 1
fileId: UUID
chunkSize: 1048576
originalLength: UInt64
mimeType: encrypted metadata ref
wrappedDekLength
wrappedDek
noncePrefix
headerTag / authenticated metadata
```

### 8.2 数据分块

建议 chunk size：**1 MiB** 起步，后续按 benchmark 调整。

每个 chunk：

```text
plaintext chunk
  -> AES-256-GCM
  -> ciphertext + 128-bit tag
```

Nonce 必须唯一。可用：

```text
nonce = random 64-bit file prefix + UInt32 chunkIndex
```

前提：每个文件拥有全新的 DEK；文件重写时生成新 DEK。

AAD：

```text
fileId + containerVersion + chunkIndex + plaintextLength
```

这样可以防止 chunk 被调换或拼接。

---

## 9. “只有阅读时才解密”的实现

### 9.1 禁止方案

严禁：

```text
打开受保护 PDF
-> 解密为 /cache/book.pdf
-> 阅读器打开明文临时文件
```

原因：闪存上可能留下可恢复明文，而且异常退出可能来不及删除。

### 9.2 正确方案

创建 `DecryptingSeekableSource`：

```text
Reader Engine
    |
    v
SecureDocumentSource
    |
    v
DecryptingSeekableSource
    |
    +-> read(offset, length)
          -> 算出 chunk 范围
          -> 读取密文
          -> 内存解密
          -> 只返回请求的数据
```

明文生命周期只存在：

- 当前方法内存 buffer
- Reader 当前页的 bitmap/text layout
- 必需的短期内存缓存

Session 关闭后主动清除可控 byte arrays；Bitmap/系统图形内存交由生命周期及时释放。

### 9.3 PDF / ZIP 随机访问

选择底层引擎时必须优先选择可接受：

- Seekable byte source
- FileDescriptor proxy
- random read callback

的库。

若某个第三方库强制要求真实明文 path，默认判定为“不适合作为受保护文件引擎”；除非后续能通过 `ProxyFileDescriptorCallback` / 受控 seekable FD 适配。

---

## 10. 加密迁移流程

用户点击“加密并上锁”：

```text
1. Validate source readable
2. Create staging encrypted file
3. Generate per-file DEK
4. Stream plaintext -> encrypted chunks
5. fsync/close
6. Verify:
   - header
   - all chunk tags
   - plaintext logical length
   - source content digest when source deletion is requested
7. Atomically commit vault record
8. Update Publication protection state
9. Ask/execute delete original (only if explicitly requested)
10. Rescan source
```

**任何一步失败都不得先删除原文件。**

### 10.1 删除原文件

只有：

- Source provider 确认具有 DELETE 能力
- 加密副本完整校验通过
- 用户明确选择“迁移后删除原文件”

才允许删除。

---

## 11. 文件夹 / 仓库批量加密

对大量文件必须使用 WorkManager：

- 可暂停/取消
- 每本独立事务
- 显示成功/失败数量
- 断电后可恢复
- 不因一个损坏文件回滚 1000 本已经成功的书

任务状态：

```text
QUEUED -> COPYING -> VERIFYING -> COMMITTED -> SOURCE_DELETE(optional)
```

---

## 12. 元数据也可能泄露隐私

如果文件加密了，但 Room 仍明文保存：

- 标题
- 作者
- 封面路径
- 阅读历史
- 搜索全文

那么隐私仍可能泄漏。

因此保护 Publication 使用两层数据：

### Public shell
Room 可保留：

- publication UUID
- `isProtected=true`
- generic display label
- encrypted payload ref
- minimal state

### Encrypted private metadata
加密保存：

- title/author/series/description
- original path/name（可配置）
- cover thumbnail
- annotations
- sensitive progress labels

保护内容 **不得进入普通 FTS**。解锁后可在内存中临时搜索，或单独维护加密索引。

---

## 13. 截图、最近任务与投屏

提供安全策略：

### 默认

- 锁屏页：禁止 Recents screenshot。
- 受保护内容详情/Reader：默认开启 `FLAG_SECURE`。
- 可选“所有阅读页面禁止截图”。

Android 13+ 可额外：

- `Activity.setRecentsScreenshotEnabled(false)` 防止最近任务缩略图。

注意：这与 `FLAG_SECURE` 作用范围不同，两者可组合。

---

## 14. 备份与恢复

### 14.1 自动系统备份

Vault 密文和 Device KEK 绑定关系不应依赖 Android Auto Backup。

默认：

- Vault 大文件排除 Auto Backup。
- Keystore secret 不备份。
- 敏感配置放 `noBackupFilesDir` 或 backup rules 排除。

### 14.2 Aster Encrypted Backup

提供用户主动导出：

```text
.asterbackup
  manifest
  database user-data snapshot
  encrypted metadata
  encrypted vault files(optional)
  wrapped Vault Master Secret via recovery password
```

没有 Recovery Password 的 device-bound vault，卸载应用/清除数据后应明确提示 **无法恢复**。

---

## 15. 忘记密码 / 生物识别变化 / 锁屏变化

必须设计恢复流程：

- 系统认证可用 + 忘记 App PIN -> 允许重置 PIN（若策略为 OR）。
- `Both required` -> 缺一不可。
- Recovery Password 存在 -> 可在新设备恢复 Vault Master Secret。
- 没有 Recovery Slot -> 卸载/清数据后不可恢复。

任何设置“关闭锁”都需要先成功认证。

---

## 16. 安全模块划分

```text
:core:security-api
:core:security-keystore
:core:crypto
:core:vault
:feature:auth
:feature:vault
```

核心接口：

```kotlin
interface AppLockManager
interface VaultManager
interface VaultKeyRepository
interface SecureDocumentSource
interface ProtectionPolicyRepository
```

UI 不直接调用 Cipher；Reader 不直接操作 Keystore。

---

## 17. 安全日志规则

允许记录：

- vault operation id
- duration
- error enum
- encrypted file byte count

禁止记录：

- password/PIN
- raw key / wrapped key blob
- 原文件完整私密路径（release）
- 标题（受保护内容）
- 明文 chunk

---

## 18. 关键安全测试

必须有：

- 加密 -> 解密 roundtrip fixture。
- 任意修改一个 ciphertext byte -> GCM 验证失败。
- 交换两个 chunk -> 验证失败。
- 截断文件 -> 明确 corruption error。
- 错误密钥 -> 无明文输出。
- app background -> timeout 后重新认证。
- screen off -> 立即锁定。
- protected book 不进入 FTS。
- crash during encryption -> 原文件仍存在，staging 可清理。
- crash after commit before source delete -> 两份文件都安全，后续提示处理。
- app process killed while reading -> 不产生明文 temp 文件。
- forgotten PIN / recovery paths 测试。

---

## 19. 官方依据

- BiometricPrompt: https://developer.android.com/identity/sign-in/biometric-auth
- Android Keystore: https://developer.android.com/privacy-and-security/keystore
- Cryptography: https://developer.android.com/privacy-and-security/cryptography
- KeyGenParameterSpec: https://developer.android.com/reference/android/security/keystore/KeyGenParameterSpec.Builder
- FLAG_SECURE guidance: https://developer.android.com/security/fraud-prevention/activities
- Activity recents screenshot: https://developer.android.com/reference/android/app/Activity#setRecentsScreenshotEnabled(boolean)
- Auto Backup rules: https://developer.android.com/identity/data/autobackup
