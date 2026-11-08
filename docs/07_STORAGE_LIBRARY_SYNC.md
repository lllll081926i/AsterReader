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
选择目录 → 枚举文件信息 → 按扩展名或 MIME 分类 → 分作品事务入库 → 扫描完成
```

扫描不打开文件内容，不复制原件，不建立 ZIP/PDF 索引，不解码或批量补封面。文件体积使用 Long，不设本地容器大小门槛。

新登记容器的 `totalPages=0` 表示页数尚未解析；保留真实来源 URI，界面不显示 0/0。详情页预读或首次阅读时建立索引，再复用该来源解码目标页。ZIP 使用中央目录与条目随机读取，不解压整本。损坏或非漫画内容在首次打开时反馈，可重试；扫描入库不等于内容校验通过。

散图目录只枚举文件信息和建立页序，不读取图片内容。无后缀或错误后缀的来源可通过单文件导入执行文件头识别；目录扫描按名称和 MIME 分类。

### 增量扫描

对每个资源保存：relative path、size、mtime、quick fingerprint。

扫描使用提供者返回的 size、mtime 和远程 ETag；不为增量扫描读取首尾数据。同一来源未变化时保留页数、目录和封面。来源变化时将页数标记为待解析，并清除旧目录与封面引用；收藏、隐藏、书名、阅读位置、进度和书签保持原值。

延迟索引提交前必须核对来源版本；旧解析结果不能覆盖新扫描结果。数据库结构与版本保持不变。

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
