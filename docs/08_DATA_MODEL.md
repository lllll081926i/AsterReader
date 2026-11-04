# 08 数据模型

## 核心实体

### SourceEntity

- id
- type
- name
- rootUri
- capabilitiesJson
- credentialAlias
- scanProfileId
- lastScanAt
- status

### PublicationEntity

- id (UUID)
- sourceId
- relativePath
- mediaType
- format
- title
- sortTitle
- authorText
- seriesName
- seriesIndex
- description
- language
- publisher
- publishedYear
- isbn
- coverRef
- contentFingerprint
- fileSize
- modifiedAt
- addedAt
- lastOpenedAt
- readingState

### ResourceEntity

代表章节/内部文件/页组：

- id
- publicationId
- href/path
- title
- orderIndex
- mediaType
- pageCount optional

### ReadingProgressEntity

- publicationId PK
- locatorJson
- progression
- chapterProgression
- updatedAt
- readingSeconds

### BookmarkEntity

- id
- publicationId
- locatorJson
- label
- createdAt

### AnnotationEntity

- id
- publicationId
- locatorJson
- note
- colorRole
- createdAt/updatedAt

### ShelfEntity / ShelfPublicationCrossRef

多对多，另有 sortIndex。

### TagEntity / PublicationTagCrossRef

支持自定义颜色/图标但不强制。

### ScanEntryEntity

记录上次扫描 fingerprint、mtime、size、status，帮助增量扫描。

## FTS

元数据索引规划：

`PublicationFts`：title, authors, series, description, tags。漫画不建立正文索引。

元数据索引是可重建派生数据，不进入关键备份也可以；备份时只保留用户数据。

## 删除语义

必须区分：

- `RemoveFromLibrary`：保留原文件。
- `DeleteOriginal`：需要 Source WRITE/DELETE 能力 + 二次确认。
- `Missing`：扫描发现文件消失，但先保留用户进度一段时间，允许重新关联。

## 数据迁移

每个 schema migration 都需要：

- instrumentation migration test
- 从至少前 2 个稳定版本升级
- 不允许 destructive fallback 进入 release

## v2.0 保护与保险库实体

### ProtectionEntity

- targetType: PUBLICATION / SHELF / SOURCE / PATH
- targetId
- mode: ACCESS_LOCK / VAULT_ENCRYPTED
- authPolicyId
- hideMetadata
- screenshotPolicy
- createdAt

### VaultItemEntity

- id
- publicationId nullable
- vaultRelativePath
- containerVersion
- encryptedSize
- plaintextLogicalSize
- wrappedDekRef
- state: STAGING / READY / CORRUPT / MIGRATING_OUT
- createdAt / verifiedAt

### KeySlotEntity

只保存 **包装后的** secret / metadata，不保存明文 key：

- slotId
- type: DEVICE_KEYSTORE / RECOVERY_PASSWORD
- wrappedMasterSecret
- kdfParamsRef（recovery slot only）
- createdAt

### AppLockPolicy

放 DataStore（不含 secret）：

- enabled
- unlockMode
- timeout
- lockOnScreenOff
- secureRecents
- secureProtectedScreens

### Protected metadata

对 `isProtected=true` 的 publication，普通 `PublicationEntity` 只保留非敏感 shell；真实 title/author/cover/private notes 使用 encrypted payload。保护内容不得进入普通 `PublicationFts`。
