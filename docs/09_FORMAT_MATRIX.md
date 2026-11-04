# 09 格式支持矩阵

| 格式 | MVP | 解析/渲染 | 目录 | 全文搜索 | 元数据编辑 | 远程流式策略 |
|---|---:|---|---:|---:|---:|---|
| EPUB 2/3 | ✓ | Readium | ✓ | ✓ | P1 | Range/缓存 |
| EPUB FXL | ✓ | Readium/Fixed | ✓ | 部分 | P1 | Range/缓存 |
| FB2 | ✓ | 自有/可参考 MIT | ✓ | ✓ | ✓ | 下载/缓存 |
| MOBI/KF8 | ✓ | 自有/可参考 MIT | ✓ | ✓ | ✓ | 下载/缓存 |
| AZW3 | P1 | KF8 路线 | ✓ | ✓ | P1 | 下载/缓存 |
| TXT | ✓ | Text Engine | 自动章节 | ✓ | n/a | 流/缓存 |
| Markdown | ✓ | CommonMark | H1-H6 | ✓ | n/a | 流/缓存 |
| HTML/HTM | ✓ | sanitized HTML | 视文档 | ✓ | n/a | 流/缓存 |
| PDF | ✓ | PDFium/Readium | ✓ | 引擎允许时 | 否 | Range 优先 |
| Image Folder | ✓ | Comic Engine | 文件/目录 | 否 | n/a | provider dependent |
| ZIP/CBZ | ✓ | ZipFile | ✓ | 否 | n/a | 可做 Range |
| RAR/CBR | ✓ | libarchive | ✓ | 否 | n/a | 通常缓存 |
| 7Z/CB7 | ✓ | libarchive | ✓ | 否 | n/a | 通常完整缓存 |
| TAR/CBT | ✓ | libarchive | ✓ | 否 | n/a | 顺序缓存 |

## 图片编码

首发：JPEG/JPG、PNG、WEBP、BMP、HEIC（系统支持时）、GIF 静态首帧。

P1：AVIF；动图逐帧播放仅作为可选能力。

## MIME/扩展名策略

扩展名只能做快速筛选，最终 `probe()` 必须验证 magic bytes / container signature，避免用户把错误文件改后缀导致崩溃。

## 失败降级

- 不规范 EPUB：先正常解析，失败后启用兼容模式。
- PDF 某页解码失败：显示单页错误占位，不让整本退出。
- 压缩包某 entry 损坏：跳过并记录问题列表。
- 编码不明 TXT：BOM -> UTF-8 -> UTF-16 -> 用户选择编码；不要静默乱码。
