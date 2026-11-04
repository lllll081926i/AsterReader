# 07 仓库、扫描、缓存与同步

## 仓库模型

每个仓库 `Source` 是长期配置：

- `id`
- `type`：LOCAL_TREE / WEBDAV / future SMB/SFTP
- displayName
- rootUri
- capabilities
- scanPolicy
- credentialRef（仅远程）
- lastScanAt
- health/status

## 本地仓库

使用 Storage Access Framework `ACTION_OPEN_DOCUMENT_TREE` 获取 persistable URI permission。不要依赖真实 `/storage/emulated/0/...` 路径。

优势：

- Android 官方支持方式。
- 不需要泛化存储权限。
- 可访问 SD 卡、系统 DocumentsProvider 和部分云盘 Provider。

## 扫描流程

```text
选择目录
  ↓
建立 Source
  ↓
快速目录枚举
  ↓
候选文件 Probe（扩展名 + magic bytes）
  ↓
轻量 metadata/cover
  ↓
写入 staging tables
  ↓
事务 commit
  ↓
后台生成全文索引/缩略图
```

### 增量扫描

对每个资源保存：relative path、size、mtime、quick fingerprint。

快速指纹建议：

`hash(size + first64KiB + last64KiB)`

用于识别重命名/移动，避免重复创建 Publication。

## 目录规则

不要像某些漫画工具那样假设“一个父目录下所有压缩包一定是同一作品”。提供可配置 Profile：

- 每文件即作品
- 每目录即作品、子文件为章节
- 根目录单行本
- 自动推断（默认）

扫描预览必须告诉用户“将识别出 X 本 / Y 章节”，可在提交前修改规则。

## WebDAV

WebDAV 是 SourceProvider，不是特殊业务分支。

- PROPFIND 列表
- ETag/Last-Modified 做增量刷新
- HTTPS 优先
- 凭据存在 Keystore 加密后的本地数据库/Preferences 中
- ZIP/PDF 若服务器支持 HTTP Range，可尝试范围读取
- RAR/7Z 等随机访问差的格式允许转入下载缓存

每个远程文件应显示缓存状态：未缓存 / 部分 / 完整。

## 缓存层级

1. Memory decoded cache：小，跟随内存压力。
2. Disk render cache：页面缩略/文本分页。
3. Remote file cache：可配置上限。
4. Original files：永不由“清缓存”删除。

设置页必须显示每类缓存占用，清理时分项选择。

## 备份

两种模式：

### Data-only

包含：数据库、设置、书架、标签、进度、书签、笔记、自定义封面引用，不含原书。

### Full

Data-only + 用户选择的原文件。需要估算大小并提示。

备份使用 manifest 带 schemaVersion；恢复前先验证 hash，再在事务里导入。自动备份保留最近 5 份，可配置。

## 同步

首发不做复杂双向实时同步。1.x 可做“备份文件同步”：将 data-only backup 推送 WebDAV，再在另一设备恢复/合并。

若后续做真正进度同步，要为每条可变记录引入 `updatedAt + deviceId + logicalVersion`，否则冲突难以解决。

## v2.0 Vault Storage

Vault 是独立于普通 Repository 的安全存储层。

### 默认位置

建议默认 app-private storage；高级用户未来可选择 SAF Vault directory。

### 迁移语义

“加密并上锁”是 copy-encrypt-verify-commit，再根据用户选择删除原文件。绝不原地先覆盖原文件。

### 批量加密

目录/书架/仓库批量迁移由 WorkManager 执行，每文件独立事务，可取消、可恢复。

### 扫描

普通 Scanner 不直接把 `.asv` 密文当未知书籍解析；Vault 有自己的 manifest/index，并映射回 Publication。
