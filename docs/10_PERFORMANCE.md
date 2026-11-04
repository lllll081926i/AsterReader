# 10 性能、内存与缓存

## 性能目标

### 启动

- Warm start：目标 < 500ms（中高端设备）
- Cold TTFD：目标 < 1.5s；大库不阻塞首屏
- 所有扫描/索引延后到可交互后

### 列表

- 1,000 项滚动无明显 jank
- 10,000 项数据库查询分页/懒加载
- 封面只解码到显示尺寸，不使用原图 bitmap

### 阅读

- 普通翻页 P95 < 100ms 进入下一页占位/可视状态
- 缓存命中 P95 < 50ms
- 连续漫画滚动不累计保留离屏大图

## Compose 规则

- LazyGrid/LazyList 所有 item 使用稳定 key。
- `Immutable`/stable model；数据库 entity 不直接暴露给 UI。
- 高频滚动状态使用 `derivedStateOf`。
- 不在 composition 做文件 I/O、图片尺寸探测、hash。
- Debug 版卡顿不作为最终性能结论，Release + R8 才做基准。

## Baseline Profile

建立 `:benchmark` 模块，覆盖：

1. 冷启动到资料库显示。
2. 打开 1,000 本书的书库并滚动。
3. 打开 PDF 漫画到首个可读页。
4. 打开 CBZ 到首张图。
5. 打开大 PDF 到指定页。

每个 release 自动生成/验证 Baseline Profile。

## 内存预算

以 256–384MB 可用应用内存的中低端设备为保守目标：

- decoded bitmap memory cache：动态按 maxMemory 的 10–15%。
- 单张大图超过阈值使用采样/tile。
- PDF page render 只保留当前±N 页。
- 压缩包 entry 不一次性读进 ByteArray。

## I/O

- Scanner 使用 bounded concurrency（例如 2–4），避免同时打开几十个 archive。
- 小文件 metadata 可并行；大 PDF/7z 限流。
- 所有 hash 都分层：quick fingerprint -> 必要时 full hash。

## 缓存失效

CacheKey 必须包含内容 fingerprint，不仅是 path；否则文件原地替换后会显示旧封面/旧页。

## 观测

Release 中仅记录匿名本地性能日志（默认不上传）：

- scan duration
- parse duration
- first-page latency
- render error code
- OOM/low-memory events

用户可在“诊断”导出日志主动分享。

## v2.0 加密阅读性能

Vault 加密不能把 Reader 性能拖垮，因此单独 benchmark：

- sequential throughput
- random 4KiB / 64KiB / 1MiB reads
- CBZ page seek latency
- PDF random page latency
- encrypted vs plaintext overhead
- low-memory behavior

建议 1MiB chunk 起步，通过真实设备 benchmark 调整。DecryptingSeekableSource 只缓存当前/相邻 chunk，避免把整个文件解密进内存。

Performance trace 不得把 key/plaintext 写入 trace args。
