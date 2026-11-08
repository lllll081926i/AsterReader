# 1.0.0 最终审查与发布记录

日期：2026-11-08。审查基线 `1927a89`。用户明确要求完成交互、功能、性能审查后，在云端打包全部适用版本并发布首个正式版本 1.0.0；随后明确授权把仓库改为公开。

## 审查范围与结论边界

检查 `app` 与 `core/model`、`core/database`、`core/designsystem`、`core/vault` 的实现边界，重点覆盖启动导航、书库与搜索、书架管理、阅读与目录、受保护元数据、归档解析、WebDAV、备份恢复、数据迁移、离线许可和签名分发。结合自动化回归、API35 模拟器及正式 R8 构建，不把源码检查等同于逐设备手工验收。

本轮修复了确认的代码问题。此前 docs/49 的 B01/B02/B03 分别由 RAR 依赖升级、EPUB 解析预算和离线许可解决。B05 继续保留：没有完成 API31/API37 真机、7 天 soak、万本书库、耗电温升及完整故障恢复矩阵。用户指定正式发布渠道，不代表这些门禁已经通过。发布说明显式披露这些限制。

## 问题与修复

| 编号 | 类型 | 根因与最终行为 |
|---|---|---|
| F01 | 安全/性能 | junrar 7.5.5 位于已知 RAR VM 循环、扩展时间头与流式 CRC 失败漏洞范围。独立提交升级至 8.1.2；NOP 日志实现同步为库要求的 SLF4J 2.0.17。加入正常/提前关闭/重复读取、错误 CRC、VM 恶意样本及扩展头回归，另验证 API31 加密 RAR 随机通道与锁定撤销。 |
| F02 | 安全/性能 | EPUB 直接解析无界展开 XML。现在每文档最多 2 MiB、深度 64、20,000 节点；整本累计 16 MiB、100,000 节点。先校验再构造 DOM，拒绝实体声明。超过预算回退到 ZIP 图片目录，不继续解析 XML。构造失败关闭原 ZipFile，防止重复导入泄漏句柄。 |
| F03 | 功能 | 启动时完整替换 UiState 仍可能覆盖在途导航。加载的安全与偏好保持权威；若已有导航操作，则保留当前导航字段。新回归覆盖设置读取期间的导航和首个就绪状态。 |
| F04 | 功能/性能 | 自动页码目录被视为真实章节。章节按钮改为按真实章节判断，否则明确跳 10 页；页首/页末禁用相应按钮。页码目录改为按需对象列表，标题和当前序号直接由页码计算，避免长漫画启动时分配与每次翻页扫描整本目录。滚动到底部时回报最后一页，避免短页导致页码停在前一页；读取已保存位置前不写入初始进度，恢复不覆盖已经发生的跳页。 |
| F05 | 交互/数据 | 新建、重命名书架及保存笔记在数据库成功前关闭或清空输入。改为持久化成功回调；失败保留表单。新建名称限制 80 字，禁用空名与重名。 |
| F06 | 交互 | 排序删除弹窗与设置书架页重复。入口统一为独立管理页；禁用首项上移/末项下移；删除仍需确认且保留漫画文件。 |
| F07 | 交互/无障碍 | 批量书架选择使用无界 Column。改为 LazyColumn，补空状态；长书架名分配独立宽度。筛选和阅读快捷操作最小 48dp；筛选补充选中语义。新建书架表单可内部滚动。 |
| F08 | 交互 | 回退按钮声称回到资料库，实际只回退一层。改为“返回”。笔记按钮根据内容显示“保存笔记”；密文属性不展示内部 vault URI。阅读进度行分配按钮宽度并允许文字换行；目录不再使用固定 340dp 高度。 |
| F09 | 分发 | 运行库仅有许可证外链。收集上游全文、已解析 JAR/AAR 的 LICENSE/NOTICE、字体 OFL 和图形组件 Skia 声明，提交离线许可索引。构建遇到缺失坐标即失败，最终 APK 再验证许可条目均非空。 |
| F10 | 构建 | AGP 不允许同次构建拆分 APK 和 AAB。默认不开拆分，先构建 AAB，再以显式属性构建五个 APK。逐包验证签名、版本、架构、16 KB ZIP 对齐及 64 位 ELF LOAD 对齐，统一生成 SHA-256。 |

RAR 上游依据：[VM 循环](https://github.com/junrar/junrar/security/advisories/GHSA-h9h9-2rmf-rvhp)、[扩展时间头](https://github.com/junrar/junrar/security/advisories/GHSA-h76x-7cgm-p442)、[流式完整性](https://github.com/junrar/junrar/security/advisories/GHSA-frq4-6xmx-hm4g)。升级不等于承诺任意恶意归档均无风险；RAR5 不列为已验收能力。

## 数据、安全与性能边界

- 未变更数据库 schema、数据库名称、保险库容器格式、KDF、用户密钥或用户原文件。沿用已有 1→2 迁移，不使用破坏性迁移。
- 访问锁不等于加密；外部原件不会自动删除。受保护草稿不进入系统恢复状态，保护内容不写普通封面缓存，系统备份和设备迁移排除规则继续保持。
- WebDAV 默认 HTTPS、限制同源路径、拒绝自动重定向；完整下载、文件大小和缓存预算保持。协议设备测试使用本机 fixture，不等于所有服务商实际账户验收。
- 缓存预算、采样、解码并发、7Z 64 MiB 解码预算和主线程 IO 边界已经源码复核。合成密文吞吐和 Debug 冷启动不能用于宣称 Release 的真实耗电或 P95 改善。
- Navigation 3、Material3 Adaptive 和 Hilt 尚未完整接入。手动导航和集中 ViewModel 是已有架构欠项，不在发布审查中进行高风险整体重写。
- 部分确认框尚未统一等宽图标按钮。字体放大、TalkBack、真实系统返回及全部抽屉横屏验收继续列为已知限制。

## 公开仓库与发布内容

发布前扫描 Git 全部可达历史中的 1,022 个文件版本，所用规则未检出私钥 PEM、GitHub/常见云密钥特征；历史中无签名密钥文件或实际 `.env`。长期密钥留在仓库外，GitHub 仅保存 Actions Secrets。此扫描不是对任意形式凭据的绝对无泄漏保证。

仓库已按用户指令设为公开：[AsterReader](https://github.com/lllll081926i/AsterReader)。版本名 `1.0.0`、versionCode `3`；保留长期证书。提供通用及 arm64-v8a/armeabi-v7a/x86_64/x86 APK、AAB、SHA256SUMS、证书、第三方声明与隐私说明。R8 mapping 只进入 Actions 构建 artifact，不作为公开 Release 附件。

发布文案：[1.0.0 说明](releases/1.0.0.md)。隐私说明：[PRIVACY.md](PRIVACY.md)。最终构建、测试、截图与云端运行结果记录在本文末尾的核验记录中。

## 核验记录

- 全量 JVM/Robolectric：275 项，失败、错误、跳过均为 0。包含 API31/API35 的阅读跳页和系统字体 200% 新建书架回归。日志：`build/final-review-verification.log`。
- 同次 Debug/Release Lint 均通过：0 错误，分别有 71/51 条警告。警告保留在构建报告，不禁用检查。Debug 与 androidTest APK 构建成功。
- API35 模拟器：最新 Debug 覆盖安装成功；Vault 集成、密文读取性能、WebDAV 集成、应用锁 UI 共 4 项设备测试通过。日志：`build/final-review-device-final.log`。性能测试使用合成数据，不代表真实漫画全集或耗电结果。
- 本轮改动模块：`app`、`:core:designsystem`、签名构建与发布脚本、运行库离线许可及发布文档。`:core:database`、`:core:vault` 的生产实现与 schema 未更改。
- UI 截图来自公开测试 fixture，无用户漫画或受保护内容，保存在 [验收截图](evidence/1.0.0/)。包含 API31/API35 200% 字体书架表单、阅读设置、横屏大字体、书库空态与加密清单错误态。新建表单改由 AsterFormDialog 根据窗口约束布局，解决滚动正文与平台弹窗固有尺寸反复测量；阅读末页至少占满视口，避免解码后尺寸变化使定位回退。
- API31/API37 真机手工矩阵尚未完成；API31 自动化与 API35 设备测试分别记录，不互相替代。
- 本地正式构建：AAB 与拆分 APK 分两次 Gradle 调用成功，R8 开启。`Verify-Release.py` 对五个 APK 和一个 AAB 的证书、版本、架构、16 KB 对齐与离线许可全部校验成功；分发哈希位于 `build/release-delivery/SHA256SUMS.txt`。日志：`build/final-review-signed-build.log`、`build/final-review-release-verification.log`。
- API37 真机冒烟：按用户后续指令，先通过无线 ADB 配对并安装本地已校验的 ARM64 正式包。安装成功，设备包管理器确认 `versionName=1.0.0`、`versionCode=3`、`minSdk=31`、`targetSdk=37`；显式启动 `Status: ok`，启动后进程仍在。未卸载既有应用或清除数据。这项结果仅覆盖安装与启动，不替代完整 API37 手工矩阵。


## 2026-11-08 真机反馈修复（暂停发布）

- 按用户要求暂停正式发布，保留现有草稿与 tag，不上传本轮安装包。
- 本地扫描、单文件导入、临时授权持久副本和流式兼容读取移除 512 MiB 硬限制；WebDAV 下载与单页解码预算独立保留。
- 普通本地文件直接读取，可寻址 SAF 来源保持描述符并通过进程内 fd 读取。扫描、封面与阅读不再强制复制整个容器；不可寻址提供者仍有流式兼容路径，临时授权仍转换为持久来源。
- 纵向阅读按图片比例排布，末图取消整屏最小高度；尺寸记录随阅读画布保留，取消滚动加载淡入；首次跳页在解码后按实际尺寸再次定位。
- 阅读设置增加通用“向上预加载”“向下预加载”，各 0～16 页，默认 3/6 页。按移动方向优先预取，LRU 最高 96 MiB 且受设备预算约束；为当前页保留缓存空间。
- 主 Tab 使用先退出后进入的淡变，内容层使用不透明背景；应用内返回通过 PredictiveBackHandler 在完成时回退一次，按用户最新选择不播放返回转场，取消不提交导航。漫画保留透明系统导航区，避免首次手势只唤醒系统栏。保留未保存编辑、安全设置及 WebDAV 内部导航的局部拦截。
- 本轮改动模块为 app 与 core:designsystem；不变更数据库 schema、Vault 密文协议或用户漫画原件。手机仅覆盖安装，不清数据、不自动启动。
- 官方 API 参考：https://developer.android.com/develop/ui/compose/system/predictive-back ，https://developer.android.com/reference/android/os/ParcelFileDescriptor 。

- 阅读交互补充：进入默认隐藏工具栏，中央半宽画布抬手即切换；连点逐次响应且不延迟补切换。加载和短图留白也可点击，拖动、多指、长按与取消不触发开关。两侧双击保留缩放，RTL 点击方向与双页整幅画布热区已校正。
- 最终自动核验：`testDebugUnitTest :app:lintDebug :app:assembleDebugAndroidTest` 成功；app 282 项、core:database 10 项、core:vault 9 项，共 301 项，失败、错误、跳过均为 0。Lint 为 0 错误、73 警告。日志：`build/reader-interaction-final-check.log`。
- 新触摸测试在 API31/API35 运行，覆盖即时点击、连续点击、加载及短图留白、拖动、多指、长按、取消、双击缩放、RTL 和双页热区；返回测试验证取消不回退、完成只回退一次。返回实现已移除 SeekableTransitionState，进入页面和 Tab 切换继续采用设计系统动效。
- [本轮 UI 证据](evidence/1.0.0-feedback/)使用测试漫画，不含用户内容。API31/API37 真机手工矩阵、TalkBack 和设备快速滑动验收未执行；3 GiB SAF 设备测试已编译，未运行。覆盖安装不等同于这些手工验收。
- 最终正式签名构建成功：`build/user-fixes-install/AsterReader-1.0.0-fixes.apk`，版本 `1.0.0` / code `3`，签名证书与原安装一致，SHA-256 为 `9DD26748D0FA8AF6B25E594D6AE68182F49EF5BB16224D1B3C5E56ED1CB1E475`。日志：`build/user-fixes-release-build.log`。
- API37 手机无线 ADB 覆盖安装返回 `Success`，包管理器记录更新时间为 `2026-11-08 11:54:44`。未卸载、未清数据、未启动应用；安装后 `pidof com.asterreader.app` 无输出。本轮没有推送、上传附件或发布 Release。

## 2026-11-08 PDF、阅读设置与普通返回

- 按最新要求移除所有应用预测性返回处理和系统预测性返回动画，使用普通 BackHandler 与短淡入；减少动效时直接切换。只组合当前页面，避免旧页面残影和返回处理重叠。重复点击同一入口不重复压栈。
- 阅读设置重写为页面、预加载、屏幕、操作四组 Monet 列表。翻页与缩放通过单选弹窗选择，保留本书和默认作用范围、恢复默认、上下独立 0～16 页预加载。
- PDF 单文件导入复用同一个 PdfRenderer 获取页数和封面；目录扫描只索引页数，扫描完成后后台补封面，进入阅读取消补封面任务。重新扫描不再先等待封面生成。进度按作品计数。
- PDF 阅读先显示 60 万像素预览，再补高清页；高清失败保留预览。预取只生成预览，当前页请求优先于尚未开始的预取。预取等待不持有来源租约，避免换书互相等待；旧书异步清理按来源校验，避免关闭另一作品的会话。
- Android PdfRenderer 的原生调用可能占用全局锁，已参考 [AOSP Android 14 PdfRenderer](https://android.googlesource.com/platform/frameworks/base/+/android-14.0.0_r1/graphics/java/android/graphics/pdf/PdfRenderer.java)。没有通过增加 PDF 并发来争用原生锁，也没有把协程超时当作能中断原生渲染。
- 新增当前页优先、预取取消与换书、旧来源清理不关闭新来源、预览等待高清及高清失败、普通返回只回退一次和页面释放的回归。PDF 原生索引与封面测试放在 `PdfScanDeviceTest`；Windows Robolectric 无法运行真实 PdfRenderer，设备测试仅编译，未启动手机执行。
- [设置截图](evidence/pdf-reader-settings/)来自 API31/API35 测试夹具，包含浅色、深色横屏和窄屏 200% 字体。API31/API37 手工手势、TalkBack 和用户实际大 PDF 性能尚未验收。
- 本轮生产改动为 `app`；同步设计、引擎、格式和页面任务文档。数据库、Vault 协议和用户漫画原件均未变更。

- 最终核验：`testDebugUnitTest :app:lintDebug :app:assembleDebugAndroidTest` 成功；app 289 项、core:database 10 项、core:vault 9 项，共 308 项，失败、错误、跳过均为 0。Lint 为 0 错误、73 警告。日志：`build/pdf-settings-final-check.log`。
- 正式签名 Release 构建成功，版本 `1.0.0` / code `3`，通用 APK 四种 ABI，证书与原安装一致。安装包：`build/user-fixes-install/AsterReader-1.0.0-fixes.apk`；SHA-256：`843E67D284DEAD0B4A61352E7D7DE0A558BFA89749FEB64EBA6ECBC4BBE886D1`。
- API37 手机无线 ADB 覆盖安装返回 `Success`；更新时间 `2026-11-08 12:45:06`，安装后 `pidof com.asterreader.app` 无输出。未启动应用，未启动电脑模拟器，未清数据，未推送或发布。API31 自动化通过；API31/API37 手工矩阵与用户实际大 PDF 性能未执行。
