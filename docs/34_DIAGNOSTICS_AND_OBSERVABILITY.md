# 34 Diagnostics & Observability

## 1. 为什么本地 App 也需要可观测性

复杂阅读器最难排查的是“某个特定文件打不开/扫描卡住/某设备解码失败”。因此需要本地诊断能力，但必须优先保护隐私。

## 2. 结构化日志

日志分类：

- APP
- SOURCE
- SCAN
- DB
- READER
- ARCHIVE
- PDF
- EPUB
- VAULT
- AUTH
- BACKUP

Release 默认只保留必要 warning/error，敏感值使用 redaction。

禁止日志：

- password/PIN。
- key material。
- decrypted text。
- note content。
- protected title/path。
- WebDAV Authorization header。

## 3. Diagnostic bundle

用户主动点击“导出诊断信息”时生成 ZIP，包含：

- app/version/device API。
- dependency summary。
- database schema version（不含内容）。
- repository capabilities。
- anonymized error log。
- failed format signature/size（可选）。

默认不包含原书文件。若用户主动附加测试文件，必须二次确认。

## 4. Reader error taxonomy

不要 everywhere `Exception("open failed")`。定义稳定 error：

```text
UnsupportedFormat
CorruptedPublication
PasswordRequired
SourcePermissionLost
SourceUnavailable
RandomAccessUnsupported
VaultLocked
VaultIntegrityFailure
OutOfStorage
OutOfMemoryPrevented
ParserFailure
RendererFailure
```

UI 根据类型给出可操作建议。

## 5. 性能指标（本地）

开发/benchmark 记录：

- cold/warm start。
- library query latency。
- scan files/sec。
- cover decode latency。
- reader first-page latency。
- page-turn frame time/jank。
- peak PSS。
- Vault random read overhead。

不需要上线 telemetry 才能做好性能。

