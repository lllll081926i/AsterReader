# 12 测试与 QA

## 测试金字塔

### Unit

- natural sort
- fingerprint
- TXT encoding
- EPUB/FB2/MOBI parser fixtures
- archive entry sanitizer
- locator serialization
- progress merge
- scan diff algorithm

### Integration

- Room migration
- SAF fake provider
- SourceProvider contract tests
- Engine contract tests
- backup -> restore roundtrip
- metadata copy-on-write

### UI / Screenshot

关键页面做 Compose screenshot tests：

- Phone / Foldable / Tablet / Desktop-size preview
- Light / Dark / Dynamic color off
- fontScale 1.0 / 1.5 / 2.0
- Chinese / English / long German text

### E2E

- 添加仓库 -> 扫描 -> 打开 -> 翻页 -> 强杀 -> 恢复位置
- 重命名文件 -> 刷新 -> 不丢进度
- 删除文件 -> Missing -> 重新关联
- WebDAV 断网/重连
- 备份 -> 清数据 -> 恢复

## 格式测试语料

建立合法可分发 fixture corpus：

- EPUB2/3 正常与不规范样本
- RTL EPUB
- 大字体/复杂 CSS
- FB2 含诗歌/图片
- MOBI PalmDoc/KF8
- UTF-8/UTF-16/GBK 测试 TXT（若支持 GBK 需专门声明）
- CBZ 多层目录
- RAR/7z 大包
- PDF 不同页尺寸、超长页、损坏页
- 50MP 超大图片

## 设备矩阵

最低：

- Android 12 / API 31 基线机：4GB RAM（最低支持边界）
- Android 12 动态色
- Android 16/17 旗舰机
- 8–11 英寸平板
- 折叠屏
- 120Hz 屏幕

## 性能 CI

Macrobenchmark 不要求每个 PR 都跑全套，但每个 release candidate 必跑。任何关键路径 P95 回退 > 15% 需要说明。

## Bug 分级

P0：数据丢失、删除错文件、无法恢复、严重安全问题。

P1：常用格式无法打开、频繁崩溃、进度丢失、OOM。

P2：布局错位、少数文件兼容问题、功能异常有绕路。

P3：文案/动画/非阻塞视觉问题。

## v2.0 Security / Motion 测试矩阵

### Security

- Biometric success/fail/cancel
- Device credential fallback
- background timeout
- screen off relock
- cold start no sensitive flash
- encrypt/migrate crash at every phase
- ciphertext tamper/truncate/chunk swap
- source delete only after verified commit
- recovery password restore
- no-recovery uninstall warning
- protected metadata absent from FTS
- screenshot/recents policy

### Motion

- predictive back complete/cancel
- shared element with portrait/landscape change
- animation duration scale 0
- reduced motion
- 60/90/120Hz reader transition traces
- font scale + animation
- tablet pane transitions
