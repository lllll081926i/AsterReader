# Aster Reader 当前工作区全量审查

审查日期：2026-11-07。对象为当前磁盘上的实现，包含已有未提交修改和新增文件。旧报告 `37`、`38` 仅作为历史背景，不作为当前实现通过验收的证据。本轮未修改业务代码、已有测试、依赖、数据库或用户文件。

结论：项目已有真实的漫画导入、渲染、扫描和数据库实现，不能笼统称为纯空架子。但当前存在核心功能故障、隐私绕过和恢复链路缺陷。Lint 未通过，不能进入发布验收。普通书库清单不等于加密备份，隐藏作品不等于 Vault。

本报告列出 50 项问题：9 项 P1、37 项 P2、4 项 P3。P1 为核心功能、安全或数据完整性阻断；P2 为功能、交互或工程缺陷；P3 为较低优先级的一致性问题。每项标注证据类别：**探针复现**、**工具确认**或**源码确认**。源码确认表示控制流可确定，不代表已在设备上实测。并发与真实设备风险单列，不混入已复现结果。

## 审查覆盖与边界

- 覆盖 `:app`、`:core:model`、`:core:database`、`:core:designsystem` 的生产实现、测试、Manifest、Gradle、资源、schema、设计契约和页面矩阵。
- 沿导入→扫描→详情→阅读→进度→后台→认证→备份→恢复→重新定位检查调用链。
- UI 检查布局约束、事件、状态、触控语义、Insets、导航、字体放大风险、Reduced Motion，以及入口对应的真实能力。
- 对照架构冻结决策、质量门禁、`design.md`、全页面规格、漫画改进提案。ADR-021 和改进提案已允许开发库隔离，因此本轮不把“不迁移旧开发库”简单判作违规。
- 未联网扫描 CVE，未逐项完成许可证法律核验，未做长期压力测试、恶意格式模糊测试或设备功耗测量。依赖升级提示不等于依赖漏洞。
- `adb devices -l` 无连接设备。没有 API 31/API 37 设备截图、TalkBack 录屏或真实手势验收。无法保证已发现全部细小问题；本报告给出可核实的问题和剩余验证范围。

## 已确认问题

### F01 · P1 · 缺少生物识别权限，Lint 阻断

证据：**工具确认**。[SystemBiometrics.kt:29](/D:/Code/AsterReader/app/src/main/java/com/example/data/local/SystemBiometrics.kt:29)、[SystemBiometrics.kt:103](/D:/Code/AsterReader/app/src/main/java/com/example/data/local/SystemBiometrics.kt:103)、[Manifest](/D:/Code/AsterReader/app/src/main/AndroidManifest.xml)。合并后的 Debug/Release Manifest 也没有 `USE_BIOMETRIC`。

`canAuthenticate()` 和 `authenticate()` 被 Lint 报为 2 条 `MissingPermission` 错误。运行时异常又被 `runCatching`/catch 转成“不可用”，系统认证可能直接被禁用或回落到其他路径。修复：声明权限，再验证真实生物识别、设备凭据、取消和失败路径。

### F02 · P1 · 图片封面生成在尺寸探测后无条件提前结束

证据：**探针复现**。[LibraryRepository.kt:745](/D:/Code/AsterReader/app/src/main/java/com/example/data/local/LibraryRepository.kt:745)。`inJustDecodeBounds=true` 时 `decodeStream()` 返回 null；`open()?.use { decodeStream(...) } ?: return null` 将该返回值当成打开流失败。

有效 PNG 导入成功、页数为 1，但 `coverUrl` 为空。CBZ/其他图片归档、单图、图片目录都共用此函数，重建封面同样无效。修复：独立检查流是否存在，再读取 `outWidth/outHeight`；增加真实封面文件可解码断言。

### F03 · P1 · 重新定位新来源失败，还会新增重复作品

证据：**探针复现**。[LibraryRepository.kt:509](/D:/Code/AsterReader/app/src/main/java/com/example/data/local/LibraryRepository.kt:509)、[AsterDao.kt:270](/D:/Code/AsterReader/core/database/src/main/java/com/asterreader/core/database/AsterDao.kt:270)。先 `importDocument()` 插入新作品，再调用 `replaceSource()`；后者发现同 URI 已属于新作品，拒绝替换。

探针得到返回 false、作品数从 1 变 2、旧作品仍为 MISSING。图片目录替换也采用同样流程。修复：先解析为未提交的来源描述，再在一个事务内替换旧作品；不得先建立临时作品。

### F04 · P1 · 重新选择同一个失联 URI 会假报修复成功

证据：**探针复现**。[LibraryRepository.kt:139](/D:/Code/AsterReader/app/src/main/java/com/example/data/local/LibraryRepository.kt:139)、[LibraryRepository.kt:520](/D:/Code/AsterReader/app/src/main/java/com/example/data/local/LibraryRepository.kt:520)。已有 URI 在读取/解析前直接返回，随后被标记 AVAILABLE。

探针使用不存在的 ContentProvider，仍得到重新定位成功。修复：修复来源必须实际打开并解析，成功后才更新状态。该项与 F03 分别覆盖“新 URI”和“相同 URI”。

### F05 · P1 · 多卷系列页使用负 padding，触发布局异常

证据：**探针复现**。[SeriesScreen.kt:147](/D:/Code/AsterReader/app/src/main/java/com/example/ui/screens/SeriesScreen.kt:147)。除第三张封面外均使用 `.padding(end = (-18).dp)`，Compose padding 拒绝负值。

探针确认该 Modifier 构造抛出 `IllegalArgumentException`。有系列封面时即可执行到这里。修复：用 `offset`、重叠 Box 或自定义布局实现叠放，验证 1/2/3 张封面和字体放大。

### F06 · P1 · 普通备份无需隐藏内容认证就导出敏感元数据

证据：**探针复现**。[LibraryBackup.kt:43](/D:/Code/AsterReader/app/src/main/java/com/example/data/local/LibraryBackup.kt:43)、[LibraryBackup.kt:89](/D:/Code/AsterReader/app/src/main/java/com/example/data/local/LibraryBackup.kt:89)、[设置导出入口:1604](/D:/Code/AsterReader/app/src/main/java/com/example/ui/screens/ShelvesAndVaultScreen.kt:1604)。导出查询所有作品、书架、笔记和来源 URI，没有按认证状态过滤。

探针确认隐藏标题、隐藏笔记以明文出现在 JSON 中。仅“不导出 PIN”无法保护这些信息。修复：默认排除隐藏内容；包含敏感内容时先认证并采用加密备份，明确告知导出范围。

### F07 · P1 · 隐藏作品认证可被默认 PIN 或无验证重设 PIN 绕过

证据：**源码确认**。[AsterViewModel.kt:548](/D:/Code/AsterReader/app/src/main/java/com/example/ui/AsterViewModel.kt:548)、[AsterViewModel.kt:553](/D:/Code/AsterReader/app/src/main/java/com/example/ui/AsterViewModel.kt:553)、[PrivacySecuritySettingsScreen.kt:313](/D:/Code/AsterReader/app/src/main/java/com/example/ui/screens/PrivacySecuritySettingsScreen.kt:313)。隐藏作品不要求先建立自定义凭据；无摘要时接受公开的 `1234`。

应用锁关闭、仅启用隐藏作品时，任何可进入设置的人都能清除或设置新 PIN，再解锁隐藏作品。应用锁首次启用要求设 PIN 的措施没有覆盖独立的隐藏内容认证。修复：首次隐藏先建立凭据；已有凭据的修改/清除必须验证旧凭据或系统身份。

### F08 · P1 · 恢复不是事务，失败后留下部分写入

证据：**探针复现**。[LibraryBackup.kt:133](/D:/Code/AsterReader/app/src/main/java/com/example/data/local/LibraryBackup.kt:133)。作品、书架、批注、视口、章节和仓库分段提交。

探针在 readerStates 外键失败前成功插入作品；整个恢复返回失败，但作品仍留在库里。修复：完整解析与校验后事务提交；失败时数据库不发生可见变化。

### F09 · P2 · 同一备份重复恢复会重复添加批注

证据：**探针复现**。[LibraryBackup.kt:219](/D:/Code/AsterReader/app/src/main/java/com/example/data/local/LibraryBackup.kt:219)。恢复批注统一使用 `id=0`，每次生成新主键，无稳定备份 ID 或去重规则。

探针中原有 1 条笔记，恢复同一文件两次后变为 3 条。修复：引入稳定批注 ID 或确定的合并键；同一备份重复恢复应幂等。

### F10 · P2 · 恢复直接覆盖本机进度，没有冲突策略或预览

证据：**探针复现**。[LibraryBackup.kt:148](/D:/Code/AsterReader/app/src/main/java/com/example/data/local/LibraryBackup.kt:148)、[恢复入口:1611](/D:/Code/AsterReader/app/src/main/java/com/example/ui/screens/ShelvesAndVaultScreen.kt:1611)。探针中本机已读到第 50 页，导入旧备份后退回第 1 页。

恢复旧状态可以是用户意图，但当前 UI 没有说明覆盖后果，也没有按时间合并、覆盖选择或恢复预览。已有测试注释声称“不应回退进度”，却没有断言进度。修复：定义恢复契约并让用户选择冲突处理；补充对应断言。

### F11 · P1 · 图片目录备份丢失页索引，重扫还有提前返回障碍

证据：**探针复现 + 源码确认**。[LibraryBackup.kt:99](/D:/Code/AsterReader/app/src/main/java/com/example/data/local/LibraryBackup.kt:99)、[LibraryRepository.kt:230](/D:/Code/AsterReader/app/src/main/java/com/example/data/local/LibraryRepository.kt:230)、[LibraryRepository.kt:384](/D:/Code/AsterReader/app/src/main/java/com/example/data/local/LibraryRepository.kt:384)。没有导出/恢复 comic_pages。

探针恢复后作品仍在，页索引为空。相同设备重新授权并扫描，若图片签名与备份中相同，导入在重建索引前返回；渲染却依赖该索引。修复：恢复时清除需重建的签名/建立待重建状态；重扫必须检查页索引完整性。

### F12 · P2 · 备份遗漏单本设置字段

证据：**源码确认**。[LibraryBackup.kt:99](/D:/Code/AsterReader/app/src/main/java/com/example/data/local/LibraryBackup.kt:99)、[AsterEntities.kt:96](/D:/Code/AsterReader/core/database/src/main/java/com/asterreader/core/database/AsterEntities.kt:96)。readerStates 中未导出或恢复 `coverSingle`、`rightToLeft`。

恢复会丢失这些值；对已有 readerState 的 REPLACE 也会把未带入字段变成 null。修复：维护完整、带版本的备份模型，逐字段做往返验证。

### F13 · P2 · 备份导入缺少输入大小和字段范围限制

证据：**源码确认**。[LibraryBackup.kt:134](/D:/Code/AsterReader/app/src/main/java/com/example/data/local/LibraryBackup.kt:134)。`readBytes()` 无上限并整份构建 JSON；仅检查 version 不大于 1，缺失、零或负版本也可接受。页码、进度和浮点视口未统一校验。

大文件会造成高内存占用，坏数据可以进入数据库，恢复又缺乏事务保护。修复：限制字节数/记录数/字符串长度；严格验证版本和引用关系；对页码、进度、有限浮点数进行校验后再提交。

### F14 · P2 · 扫描把解析失败的现存作品标记为失联

证据：**源码确认**。[LibraryRepository.kt:317](/D:/Code/AsterReader/app/src/main/java/com/example/data/local/LibraryRepository.kt:317)、[LibraryRepository.kt:365](/D:/Code/AsterReader/app/src/main/java/com/example/data/local/LibraryRepository.kt:365)。`seen` 只在导入成功后添加；解析失败、文件级权限失败只增加 failed，末尾仍执行 reconcileSources。

文件明明已枚举到，却可能被标记 MISSING。修复：将“枚举存在”和“可成功解析”分成独立状态；权限或解析错误不得作为文件消失的证据。

### F15 · P2 · 仓库数量只计新增和更新，正常增量扫描会显示 0

证据：**源码确认**。[LibraryRepository.kt:323](/D:/Code/AsterReader/app/src/main/java/com/example/data/local/LibraryRepository.kt:323)、[LibraryRepository.kt:366](/D:/Code/AsterReader/app/src/main/java/com/example/data/local/LibraryRepository.kt:366)。不变作品既不增加 imported 也不增加 updated，但 bookCount 最终写入二者之和。

再次扫描健康的已有库可把仓库书数改成 0；进度 processed 也不包含跳过的已有文件，不能准确表示处理量。修复：分别统计发现、处理、跳过、新增、更新、失败和库内总数。

### F16 · P2 · 扫描上限检查发生在整目录枚举之后

证据：**源码确认**。[LibraryRepository.kt:311](/D:/Code/AsterReader/app/src/main/java/com/example/data/local/LibraryRepository.kt:311)、[LibraryRepository.kt:615](/D:/Code/AsterReader/app/src/main/java/com/example/data/local/LibraryRepository.kt:615)。queryChildren 先把全部 Cursor 行装入 ArrayList，返回后才检查 10000 项上限；循环内也没有协程取消检查。

一个超大目录可在上限生效前占用大量内存，取消响应取决于整次查询。修复：枚举时检查剩余预算与取消状态；对慢 Provider 使用可取消查询。

### F17 · P2 · 重扫修改过的文件会重置实际封面，但保留旧封面页字段

证据：**源码确认**。[LibraryRepository.kt:131](/D:/Code/AsterReader/app/src/main/java/com/example/data/local/LibraryRepository.kt:131)、[LibraryRepository.kt:321](/D:/Code/AsterReader/app/src/main/java/com/example/data/local/LibraryRepository.kt:321)、[AsterDao.kt:85](/D:/Code/AsterReader/core/database/src/main/java/com/asterreader/core/database/AsterDao.kt:85)。扫描调用默认 `coverPage=1`，图片目录也始终取首图；importComic 却保留 current.coverPage。

修复 F02 后，用户选第 5 页作封面，源文件变化后重扫可能显示第一页但字段仍写第 5 页。修复：扫描读取现有封面页并在页数变化时合理钳制，保证封面与字段一致。

### F18 · P2 · 本地副本键缺少完整身份，来源更新可能复用旧内容

证据：**源码确认**。[LibraryRepository.kt:821](/D:/Code/AsterReader/app/src/main/java/com/example/data/local/LibraryRepository.kt:821)。磁盘键仅为 `id.hashCode()`、sizeBytes 和 sourceModifiedAt；没有 URI。32 位 hash 也不是唯一标识。

Provider 不提供修改时间/大小时，内容变化不会使键失效；更换 URI 且大小时间相同时也会命中旧副本。修复：使用完整作品身份和来源版本，来源替换显式失效；未知版本采用重新验证策略。

### F19 · P2 · 清理缓存遗漏最大的阅读副本，还立即重建封面

证据：**源码确认**。[AsterViewModel.kt:1414](/D:/Code/AsterReader/app/src/main/java/com/example/ui/AsterViewModel.kt:1414)、[LibraryRepository.kt:822](/D:/Code/AsterReader/app/src/main/java/com/example/data/local/LibraryRepository.kt:822)。清理只处理内存、Coil 和 covers，未清除最多约 1 GiB 的 read 副本；随后又调用 refreshMissingCovers。

用户选择清理缓存后，大量磁盘占用仍存在，封面还会被重建。修复：分别显示各缓存占用，关闭在用会话后清理可重建副本；按需重建封面。

### F20 · P2 · 临时 URI 授权失败被忽略，导入成功不代表重启后可读

证据：**源码确认**。[MainActivity.kt:159](/D:/Code/AsterReader/app/src/main/java/com/example/MainActivity.kt:159)、[LibraryRepository.kt:134](/D:/Code/AsterReader/app/src/main/java/com/example/data/local/LibraryRepository.kt:134)。持久授权失败直接忽略，作品仍登记为 AVAILABLE。外部分享常只能提供短期访问权限。

当前缓存不能作为可靠来源；缓存淘汰后也未必能重新读取。修复：区分持久来源与临时打开；短期来源需明确导入到持久位置或提示重新授权，并记录权限状态。

### F21 · P2 · 后台处理无条件清空导航，未启用应用锁也会丢页面

证据：**源码确认**。[AsterViewModel.kt:538](/D:/Code/AsterReader/app/src/main/java/com/example/ui/AsterViewModel.kt:538)、[AsterViewModel.kt:697](/D:/Code/AsterReader/app/src/main/java/com/example/ui/AsterViewModel.kt:697)。默认立即策略在 onStop 调用 lockAppImmediately；该方法无条件清空子页栈、选择和查询，只对锁屏布尔值判断应用锁开关。

关闭应用锁时，切到其他 App 返回也可能从阅读/设置退回首页。系统文件选择器同样可能使 Activity 进入 onStop，影响来源修复和备份回调宿主。修复：分离安全撤权、资源回收和导航保留；补充系统选择器往返测试。

### F22 · P2 · 退出后恢复 Reader 会再次采用旧 initialPage

证据：**源码确认**。[AsterViewModel.kt:339](/D:/Code/AsterReader/app/src/main/java/com/example/ui/AsterViewModel.kt:339)、[ReaderScreen.kt:177](/D:/Code/AsterReader/app/src/main/java/com/example/ui/screens/ReaderScreen.kt:177)。路由永久保存最初的显式跳页参数；恢复时 initialPage 优先于已保存的实际页码。

从第 10 页书签进入，之后读到第 30 页，进程重建仍可能回第 10 页。修复：消费一次性跳转参数，或保存当前 locator 作为恢复参数。

### F23 · P2 · 单本阅读设置只写不读，全局设置覆盖单本设置

证据：**源码确认**。[ReaderScreen.kt:180](/D:/Code/AsterReader/app/src/main/java/com/example/ui/screens/ReaderScreen.kt:180)、[ReaderScreen.kt:207](/D:/Code/AsterReader/app/src/main/java/com/example/ui/screens/ReaderScreen.kt:207)、[ReaderScreen.kt:242](/D:/Code/AsterReader/app/src/main/java/com/example/ui/screens/ReaderScreen.kt:242)。restored 只用于页码/缩放，渲染仍全部使用 uiState 全局配置；进入后还立即把全局配置写回单本记录。

没有真正的“单本覆盖全局”和“恢复全局默认”。平板打开作品还会直接把全局模式改成双页。修复：建立全局默认与单本覆盖的合成状态，加载完成前禁止覆盖写回。

### F24 · P2 · 阅读视口从未上报真实平移量

证据：**源码确认**。[PublicationPageCanvas.kt:148](/D:/Code/AsterReader/app/src/main/java/com/example/ui/components/PublicationPageCanvas.kt:148)、[PublicationPageCanvas.kt:196](/D:/Code/AsterReader/app/src/main/java/com/example/ui/components/PublicationPageCanvas.kt:196)。两条 onViewportChanged 路径均传 `panX=0f, panY=0f`；DocumentPage 仅回调缩放，不上报 translation。

缩放后平移到边缘，恢复时回到中心。UI 最大缩放 4.5，模型保存上限为 4，也不一致。修复：从单页回传完整归一化视口，统一范围，并测试实际手势到持久化的闭环。

### F25 · P2 · 跳页后继续使用旧缩放，旧页组件可影响全局手势锁

证据：**源码确认**。[PublicationPageCanvas.kt:97](/D:/Code/AsterReader/app/src/main/java/com/example/ui/components/PublicationPageCanvas.kt:97)、[PublicationPageCanvas.kt:269](/D:/Code/AsterReader/app/src/main/java/com/example/ui/components/PublicationPageCanvas.kt:269)。zoomScale 仅以 publicationId 记忆；每次换页单页又以 startScale 初始化，而 startScale 来源仍是最初 restored。

主动跳页没有可靠重置缩放；全局 userScrollEnabled 可能与当前页的实际 scale 不一致，相邻页也共用缩放回调。修复：以当前页视口为单一状态源，显式跳转重置，预加载页不得改变当前手势状态。

### F26 · P2 · 双页模式翻页方向没有独立实现

证据：**源码确认**。[PublicationPageCanvas.kt:182](/D:/Code/AsterReader/app/src/main/java/com/example/ui/components/PublicationPageCanvas.kt:182)、[PublicationPageCanvas.kt:202](/D:/Code/AsterReader/app/src/main/java/com/example/ui/components/PublicationPageCanvas.kt:202)。双页与 RTL 被合并为互斥的 ReaderLayoutMode；reverseLayout 仅在 RTL_PAGER 生效，双页内部始终按普通 Row 页序。

数据库 rightToLeft 没有进入该链路，日漫对开无法独立选择从右到左。修复：阅读布局和阅读方向分成两个属性，同时调整页序、点击区域与音量键语义。

### F27 · P2 · “原始比例”使用采样位图尺寸，缩放升级后画面尺寸会变化

证据：**源码确认**。[PublicationPageCanvas.kt:275](/D:/Code/AsterReader/app/src/main/java/com/example/ui/components/PublicationPageCanvas.kt:275)、[PublicationPageCanvas.kt:549](/D:/Code/AsterReader/app/src/main/java/com/example/ui/components/PublicationPageCanvas.kt:549)。natural 分支用 decoded bitmap 的 width/height 当原始尺寸；这些尺寸受像素预算影响。

达到缩放阈值重新解码后，采样尺寸变化，1:1 绘制大小也变化。修复：返回原始页面尺寸和采样比例，让显示坐标与解码分辨率分离。

### F28 · P2 · PDF 放大仍限制在原始 point 尺寸，高清预算未实际用于超采样

证据：**源码确认**。[DocumentRenderSession.kt:219](/D:/Code/AsterReader/app/src/main/java/com/example/data/local/DocumentRenderSession.kt:219)、[DocumentRenderSession.kt:256](/D:/Code/AsterReader/app/src/main/java/com/example/data/local/DocumentRenderSession.kt:256)。普通和区域 PDF 渲染的 scale 在预算足够时固定为 1，不会随屏幕或缩放提高。

常见低 point 尺寸 PDF 在高密度屏幕放大时仍被放大已有位图，增加 maxPixels 不提升清晰度。修复：按目标视口像素与缩放计算采样，再受总预算约束。

### F29 · P2 · 后台仍保留页面位图，区域解码未绑定前台生命周期

证据：**源码确认**。[PublicationPageCanvas.kt:332](/D:/Code/AsterReader/app/src/main/java/com/example/ui/components/PublicationPageCanvas.kt:332)、[PublicationPageCanvas.kt:351](/D:/Code/AsterReader/app/src/main/java/com/example/ui/components/PublicationPageCanvas.kt:351)、[PublicationPageCanvas.kt:528](/D:/Code/AsterReader/app/src/main/java/com/example/ui/components/PublicationPageCanvas.kt:528)。result 在停止时清空，但 shown 持续保留；rememberRegion 的 produceState 未使用 repeatOnLifecycle。

采用延迟锁定、Reader 仍留在组合中时，后台仍持有位图，区域请求也可能继续或重新打开会话。与旧修复说明“后台清空 Compose 图片结果”不一致。修复：明确前台令牌，停止时同步清理基础图与区域图并取消全部请求。

### F30 · P2 · Reduced Motion 没有覆盖阅读翻页和统计页

证据：**源码确认**。[PublicationPageCanvas.kt:127](/D:/Code/AsterReader/app/src/main/java/com/example/ui/components/PublicationPageCanvas.kt:127)、[PublicationPageCanvas.kt:218](/D:/Code/AsterReader/app/src/main/java/com/example/ui/components/PublicationPageCanvas.kt:218)、[PublicationPageCanvas.kt:362](/D:/Code/AsterReader/app/src/main/java/com/example/ui/components/PublicationPageCanvas.kt:362)、[ReadingStatsScreen.kt:263](/D:/Code/AsterReader/app/src/main/java/com/example/ui/screens/ReadingStatsScreen.kt:263)。仍无条件 animateScrollToItem/animateScrollToPage、tween 淡入和交错柱图动画。

应用“减少动效”不等于所有这些 API 的系统动画比例为零。修复：统一读取 AsterMotion/LocalAsterReduceMotion，降级为立即滚动与无入场动画。

### F31 · P2 · Reader 无障碍操作不完整

证据：**源码确认**。[PublicationPageCanvas.kt:385](/D:/Code/AsterReader/app/src/main/java/com/example/ui/components/PublicationPageCanvas.kt:385)、[PublicationPageCanvas.kt:463](/D:/Code/AsterReader/app/src/main/java/com/example/ui/components/PublicationPageCanvas.kt:463)。翻页、显示工具栏和缩放主要依赖 pointerInput；Canvas 仅给出页码描述，没有相应可访问动作；未提供显式放大/缩小按钮。

TalkBack 用户不能依赖普通单击/双击手势完成这些操作。修复：增加 semantics 自定义动作和显式控件；保留手势作为补充。真实 TalkBack 操作仍需设备验证。

### F32 · P2 · Reader 顶栏没有避让刘海和显示切口

证据：**源码确认**。[ReaderScreen.kt:143](/D:/Code/AsterReader/app/src/main/java/com/example/ui/screens/ReaderScreen.kt:143)、[ReaderScreen.kt:298](/D:/Code/AsterReader/app/src/main/java/com/example/ui/screens/ReaderScreen.kt:298)。隐藏 systemBars 后顶栏直接贴顶部，固定 48dp，没有 displayCutout/safeDrawing 避让。

沉浸式内容可以延伸到切口，但返回和书签等操作必须避让。修复：只对 chrome 使用安全 Insets，分别验收横屏左右切口和竖屏打孔。

### F33 · P2 · 阅读统计会把后台停留算作阅读时间

证据：**源码确认**。[ReaderScreen.kt:138](/D:/Code/AsterReader/app/src/main/java/com/example/ui/screens/ReaderScreen.kt:138)、[AsterViewModel.kt:829](/D:/Code/AsterReader/app/src/main/java/com/example/ui/AsterViewModel.kt:829)、[AsterViewModel.kt:697](/D:/Code/AsterReader/app/src/main/java/com/example/ui/AsterViewModel.kt:697)。只在组合进入/销毁时开始/结束计时，onBackground 未暂停心跳。

延迟锁定或仅息屏策略下，后台 Reader 可继续每分钟记时。修复：按前台生命周期暂停并结算，恢复时重新计时；跨日期分摊也需按实际时间区间处理。

### F34 · P2 · 阅读进度失败前先移除待写数据，无法可靠重试

证据：**源码确认**。[AsterViewModel.kt:873](/D:/Code/AsterReader/app/src/main/java/com/example/ui/AsterViewModel.kt:873)。进度和视口均在数据库成功前从 pending Map 移除；saveViewport 失败还被 runCatching 吞掉。

磁盘/数据库瞬时失败后，错误发生的最后位置不再待写。修复：成功后按版本移除，失败保留并执行有界重试；页码和视口在同一事务提交，避免双真源。

### F35 · P2 · 元数据、入架和删除书架存在整行覆盖与非事务更新

证据：**源码确认**。[AsterViewModel.kt:929](/D:/Code/AsterReader/app/src/main/java/com/example/ui/AsterViewModel.kt:929)、[AsterViewModel.kt:999](/D:/Code/AsterReader/app/src/main/java/com/example/ui/AsterViewModel.kt:999)、[AsterViewModel.kt:1112](/D:/Code/AsterReader/app/src/main/java/com/example/ui/AsterViewModel.kt:1112)。读取整行后 copy/updateBook，会覆盖读取之后的收藏、进度或扫描更新；删除书架逐书处理后再删除书架。

删除还依赖 allBooks 这个“当前可见集合”，未解锁隐藏作品的 shelfIds 不会被清理。修复：使用字段级 SQL、关系表和事务，在数据层处理全部成员。

### F36 · P2 · 删除索引/书架没有确认或撤销，部分失败无明细

证据：**源码确认**。[LibraryAndSearchScreen.kt:244](/D:/Code/AsterReader/app/src/main/java/com/example/ui/screens/LibraryAndSearchScreen.kt:244)、[ShelvesAndVaultScreen.kt:545](/D:/Code/AsterReader/app/src/main/java/com/example/ui/screens/ShelvesAndVaultScreen.kt:545)。图标点击直接执行移除；删除作品会同步删除批注和阅读状态。

保留外部源文件不等于能恢复用户笔记。修复：在动作前说明删除的数据，提供确认或可恢复撤销；批量动作返回逐项失败结果。

### F37 · P2 · 书架“管理”入口仅能新建，标签“管理”入口只是跳搜索

证据：**源码确认**。[ShelvesAndVaultScreen.kt:1825](/D:/Code/AsterReader/app/src/main/java/com/example/ui/screens/ShelvesAndVaultScreen.kt:1825)、[ShelvesAndVaultScreen.kt:1856](/D:/Code/AsterReader/app/src/main/java/com/example/ui/screens/ShelvesAndVaultScreen.kt:1856)。前者仅创建固定名“新书架”，没有现有书架的编辑；后者没有标签重命名、合并或删除。

这些入口有响应，但没有提供名称承诺的管理能力。修复：实现实际管理列表与操作，或把入口准确改为“新建书架”“按标签搜索”，同步页面矩阵。

### F38 · P2 · 系列识别未经确认，续卷可能选到同卷并自动跳转

证据：**源码确认**。[LibraryRepository.kt:108](/D:/Code/AsterReader/app/src/main/java/com/example/data/local/LibraryRepository.kt:108)、[LibraryRepository.kt:177](/D:/Code/AsterReader/app/src/main/java/com/example/data/local/LibraryRepository.kt:177)、[AsterViewModel.kt:958](/D:/Code/AsterReader/app/src/main/java/com/example/ui/AsterViewModel.kt:958)、[ReaderScreen.kt:259](/D:/Code/AsterReader/app/src/main/java/com/example/ui/screens/ReaderScreen.kt:259)。所有散本也被赋予非空系列；续卷筛选使用 `>=`，可能在相同卷序作品间往返；末页继续操作直接换作品。

违反设计补充要求的“用户确认系列”和“显式下一卷按钮”。`第 02 卷` 等带空格模式也需补样例。修复：自动识别仅为建议；下一卷严格取后续卷，并过滤不可访问来源、提供明确入口。

### F39 · P2 · 大屏适配由横屏布尔量决定，没有接入冻结的 Adaptive 导航

证据：**源码确认**。[MainActivity.kt:322](/D:/Code/AsterReader/app/src/main/java/com/example/MainActivity.kt:322)、[LibraryAndSearchScreen.kt:708](/D:/Code/AsterReader/app/src/main/java/com/example/ui/screens/LibraryAndSearchScreen.kt:708)、[app/build.gradle.kts](/D:/Code/AsterReader/app/build.gradle.kts)。宽度达到 720dp 还必须宽大于高，竖屏平板/展开折叠屏仍强制单栏；支持面板固定 360dp；导航始终是底部 Dock。

修复：使用实际窗口尺寸与折叠信息，接入 NavigationSuite/Adaptive canonical layout；验证紧凑横屏、竖屏大屏和自由窗口。

### F40 · P2 · 自定义返回栈没有预测性返回进度

证据：**源码确认**。[MainActivity.kt:304](/D:/Code/AsterReader/app/src/main/java/com/example/MainActivity.kt:304)、[AsterViewModel.kt:439](/D:/Code/AsterReader/app/src/main/java/com/example/ui/AsterViewModel.kt:439)。BackHandler 直接弹列表，AnimatedContent 另做定时转场，没有 Navigation 3 或返回进度状态。

返回动作可执行不代表预测性返回已实现。修复：按冻结架构接入 Navigation 3，并测试返回手势中途取消、弹层优先级和减少动效。

### F41 · P2 · 搜索把加载态当成零结果，书签空页没有空状态

证据：**源码确认**。[LibraryAndSearchScreen.kt:1737](/D:/Code/AsterReader/app/src/main/java/com/example/ui/screens/LibraryAndSearchScreen.kt:1737)、[LibraryAndSearchScreen.kt:2116](/D:/Code/AsterReader/app/src/main/java/com/example/ui/screens/LibraryAndSearchScreen.kt:2116)、[LibraryAndSearchScreen.kt:2149](/D:/Code/AsterReader/app/src/main/java/com/example/ui/screens/LibraryAndSearchScreen.kt:2149)。防抖和计算期间 null 结果变为空列表，立即显示“未找到”；书签范围结果为空时没有对应分支。

修复：区分加载、成功零项和失败；保留匹配请求的结果，书签范围提供可操作空状态。

### F42 · P2 · 标签和书架成员用子串判断，产生误匹配

证据：**源码确认**。[LibrarySearch.kt:34](/D:/Code/AsterReader/app/src/main/java/com/example/ui/LibrarySearch.kt:34)、[ShelvesAndVaultScreen.kt:1238](/D:/Code/AsterReader/app/src/main/java/com/example/ui/screens/ShelvesAndVaultScreen.kt:1238)。标签筛选 `tagsCsv.contains(tag)`；详情 `shelfIdsCsv.contains(shelfId)`。

标签“科幻”会匹配“非科幻”，短书架 ID 可匹配其他 ID。修复：按统一分隔与规范化规则精确匹配，长期使用关系表。

### F43 · P2 · 触控尺寸和大字号布局缺少统一保障

证据：**源码确认**。[AsterUiKit.kt:166](/D:/Code/AsterReader/app/src/main/java/com/example/ui/components/AsterUiKit.kt:166)、[LibraryAndSearchScreen.kt:2053](/D:/Code/AsterReader/app/src/main/java/com/example/ui/screens/LibraryAndSearchScreen.kt:2053)、[AsterUiKit.kt:100](/D:/Code/AsterReader/app/src/main/java/com/example/ui/components/AsterUiKit.kt:100)、[ReaderScreen.kt:301](/D:/Code/AsterReader/app/src/main/java/com/example/ui/screens/ReaderScreen.kt:301)。存在 20dp 自定义点击框、32/36/44dp 图标按钮约束；空态双按钮固定同一 Row；Reader 顶栏固定 48dp 且标题两行。

Material 触控扩展不能自动证明拥挤布局下满足验收，也不覆盖所有自定义点击区域。修复：保证实际可点击区域至少 48dp，窄屏/200% 字体允许换行或纵向操作。裁切与重叠程度需截图验证。

### F44 · P2 · Snackbar 不适配无障碍，错误内容可能被截断

证据：**源码确认**。[MainActivity.kt:314](/D:/Code/AsterReader/app/src/main/java/com/example/MainActivity.kt:314)、[MainActivity.kt:568](/D:/Code/AsterReader/app/src/main/java/com/example/MainActivity.kt:568)。自定义提示固定 2.6/3.4 秒消失，最多两行，点击语义仅为“关闭提示”；未使用 liveRegion 或无障碍推荐超时。

修复：使用标准 SnackbarHost 或补齐播报、推荐超时、错误详情与可重试动作；避免靠文案里是否含“已”推断成功状态。

### F45 · P2 · 自实现 TAR 解析缺少必要的格式与资源校验

证据：**源码确认**。[ComicArchive.kt:273](/D:/Code/AsterReader/app/src/main/java/com/example/data/local/ComicArchive.kt:273)。没有校验 header checksum、size 非负、偏移算术溢出、dataStart+size 是否超过文件；GNU 长文件名直接 `size.toInt()` 分配；PAX 扩展被直接忽略。

坏 TAR 可造成异常分配/越界读取，合法 PAX 长路径也可能被错识别。修复：优先复用已有 Commons Compress 的成熟 TAR 解析器；限制解包尺寸、条目数和索引预算。当前未完成模糊测试，不能据此断言具体可利用性。

### F46 · P2 · PIN KDF 在主线程执行，并带未标版本的自创回退

证据：**源码确认**。[AsterViewModel.kt:548](/D:/Code/AsterReader/app/src/main/java/com/example/ui/AsterViewModel.kt:548)、[AsterViewModel.kt:566](/D:/Code/AsterReader/app/src/main/java/com/example/ui/AsterViewModel.kt:566)、[ReaderPreferences.kt:48](/D:/Code/AsterReader/app/src/main/java/com/example/data/local/ReaderPreferences.kt:48)。同步 UI 操作直接执行 60000 次 PBKDF2。失败时自行重复 SHA-256，但仍存相同 `v2` 格式。

低性能设备有输入卡顿风险；回退算法与正常算法无版本区分，环境变化后可能无法验证。修复：在计算线程执行，使用明确版本的标准 KDF；平台不支持时明确失败，不自行替代密码算法。PIN 失败次数和冷却还仅在进程内，重启可重置。

### F47 · P3 · 隐藏内容认证抽屉不显示真实冷却状态

证据：**源码确认**。[AppLockScreen.kt:412](/D:/Code/AsterReader/app/src/main/java/com/example/ui/screens/AppLockScreen.kt:412)。键盘永远 enabled，失败始终显示“PIN 码不正确”；未像全屏锁一样订阅倒计时。

实际因冷却被拒绝也显示密码错误，提示在根认证分支中不一定可见。修复：两个认证界面共享状态模型、倒计时和错误原因。

### F48 · P3 · “累计”阅读时间实际是最近约 60 天

证据：**源码确认**。[ReadingStatsStore.kt:41](/D:/Code/AsterReader/app/src/main/java/com/example/data/local/ReadingStatsStore.kt:41)、[ReadingStatsStore.kt:68](/D:/Code/AsterReader/app/src/main/java/com/example/data/local/ReadingStatsStore.kt:68)。total 是现存日记录求和，prune 会移除 60 天前记录；UI 却显示“累计”。

修复：单独维护全周期累计，或准确标为“近 60 天”。柱图仅有星期标签，也缺少每天分钟数的可访问语义。

### F49 · P3 · 系列“读到第几卷”按页码是否大于零推断

证据：**源码确认**。[SeriesScreen.kt:126](/D:/Code/AsterReader/app/src/main/java/com/example/ui/screens/SeriesScreen.kt:126)。`volumes.lastOrNull { it.currentPage > 0 }` 会包含所有默认 currentPage=1 的未读作品。

未开始阅读也可显示读到最后一卷。修复：以 lastReadTimestamp/progressPercent 判定实际阅读，再按最后阅读时间选择。

### F50 · P3 · 设计令牌、资源与说明未与实现保持一致

证据：**源码确认**。[ReadingStatsScreen.kt:263](/D:/Code/AsterReader/app/src/main/java/com/example/ui/screens/ReadingStatsScreen.kt:263)、[HiddenBooksScreen.kt:146](/D:/Code/AsterReader/app/src/main/java/com/example/ui/screens/HiddenBooksScreen.kt:146)、[strings.xml](/D:/Code/AsterReader/app/src/main/res/values/strings.xml)、[README.md:10](/D:/Code/AsterReader/README.md:10)、[页面矩阵](/D:/Code/AsterReader/planning/UI_SCREEN_TASK_MATRIX.csv)。Feature 仍散落圆角/动效常量；strings 仅有 app_name；README 使用“全格式”“完美避让”等尚未验收的表述，模块树还列已移除的契约模块。

修复：统一设计令牌和可本地化字符串；按实际支持范围写格式限制，删除无证据的绝对性能/体验保证；页面矩阵按实现与验收分别记录。

## 尚未实现或只有局部接入的能力

以下是范围与完成度差距，不伪装成已复现的运行时 bug：

| 能力 | 当前事实 | 缺少的闭环 |
|---|---|---|
| Vault / Encrypt & Lock | 当前源集没有 Keystore、主秘密、DEK、AES-GCM 分块实现；加密作品被拒绝阅读 | 建库、加密、校验、恢复、导出和中断恢复；冻结架构仍要求 |
| 加密备份 | 只有 JSON 元数据清单 | 密码、认证加密、完整性验证、密钥恢复 |
| Reader Shell / Engine 边界 | 归档接口在 app 内部，core:engine-api 已移出构建 | 独立来源/引擎契约与 registry，SAF/Vault 来源统一 |
| Navigation 3 / Hilt / Adaptive | 没有实际依赖与装配，ViewModel 自建 Repository | 与冻结决策一致的边界；或先提交新的 ADR |
| 首次引导 | 有 OnboardingScreen 和路由，未找到正常入口创建该路由或首次完成标记 | 首次进入、跳过、完成和不重复展示 |
| 导入确认与扫描预览 | 选文件即导入，选目录即扫描 | 规则选择、分类预览、冲突处理、失败文件清单 |
| 仓库权限修复 | 展示仓库和删除，主要依赖全局重扫 | 单仓库重新授权、路径重关联、明确离线/权限状态 |
| 书架编辑 | 有创建、删除和相邻排序 | 重命名、编辑备注、成员移除、重复名策略；时间戳 ID 与数量型 sortOrder 也需治理 |
| Reader 内笔记 | 阅读器能切换书签；笔记主要从详情页追加 | 阅读当前页直接写/改笔记，书签与笔记明确区分 |
| 开源许可与诊断 | 关于弹窗仅版本说明 | 依赖/字体许可证正文、来源、NOTICE，脱敏诊断和帮助 |
| 性能验收 | 有 LRU、像素预算、R8 等措施 | Baseline Profile、Macrobenchmark、真实 10k 库、长图/500页、堆与功耗证据 |

WebDAV 等已声明 1.x 的功能不列为首版实现故障。RAR5 明确不支持，不应继续用“全格式”掩盖边界。EPUB 目前为图片页面抽取，不是通用重排式电子书引擎。

## 仍需专项验证的源码风险

1. **会话切换与关闭并发**：[DocumentRenderSession.kt:131](/D:/Code/AsterReader/app/src/main/java/com/example/data/local/DocumentRenderSession.kt:131) 切换 source 时 closeLocked 不等待已有 inFlight；取到 OpenSource 与登记 inFlight 之间也有窗口。close 没有 finally 保证恢复 closing。需用可控挂起点验证两书切换、预取、区域解码、低内存关闭并发；不要把正常关闭测试当作竞态已排除。
2. **缓存预算不等于峰值内存上限**：LRU 不包含 Compose 持有的 shown、在途并发位图、解压器字典与区域读取的 ByteArrayOutputStream/toByteArray 双份字节。应在低内存设备测峰值和 OOM，限制并发与解压资源。
3. **区域条带边界**：[PublicationPageCanvas.kt:522](/D:/Code/AsterReader/app/src/main/java/com/example/ui/components/PublicationPageCanvas.kt:522) 用 top+0.01/0.025 作为 coerceIn 下限，极端边界必须验证上下界合法；条漫可见区计算只使用整 item 高度，没有显式包含 LazyColumn 屏幕裁切偏移。需用超长图实际滚动、缩放、上下边缘和平移截图核对。
4. **条漫位置恢复时机**：按首次 item 布局高度恢复，但加载后 aspectRatio 改变高度；应测试慢来源、未缓存图像、进程重建，核对最终像素位置，而非只核对数据库字段。
5. **系统认证生命周期**：取消旧 prompt 与新 prompt 回调、系统凭据 fallback、onStop、配置变化和连续点击需要设备联调，不能用“已开始尝试”的布尔值代替认证结果的关联校验。
6. **持久权限数量与泄漏**：删除仓库/作品未看到 releasePersistableUriPermission。需验证长期重复添加删除、共享授权引用计数和重新添加后的可读性。
7. **恶意归档与 Provider**：条目数、解压后资源、7z 字典、EPUB XML 大小、长期阻塞 Cursor/流、路径与重复条目均需负向样本；已通过的 XXE 测试不覆盖这些风险。

## UI 设备验收清单

| 场景 | 必测组合 | 当前状态 |
|---|---|---|
| 首页/搜索/书架/详情 | 正常、空、加载、错误、隐藏；返回后滚动位置 | 源码已审，设备未验收 |
| Reader | LTR/RTL/双页/条漫；慢加载、损坏页、末页、缩放、边缘翻页 | 部分 Compose 单测，设备未验收 |
| 窗口和系统栏 | API31/API37；手势/三键；横屏切口；分屏/自由窗口/折叠 | 未验收 |
| 字体和主题 | 320dp 窄屏、200% 字体；Monet 浅/深、多种壁纸 | 未验收 |
| 无障碍 | TalkBack 顺序、可访问动作、状态播报、48dp 触控 | 未验收 |
| 动效 | 应用减少动效、系统比例0、快速重复操作、预测返回取消 | 未验收 |
| 安全 | 后台、息屏、取消认证、进程重启、外部 Intent、普通备份 | 探针/源码局部验证，设备未验收 |
| 系统选择器 | 导入/导出/恢复/重新定位时离开与返回，含应用锁开/关 | 未验收 |

## 自动核验结果与证据

| 检查 | 结果 |
|---|---|
| 原有 `testDebugUnitTest` | 74 项通过：app 65，core database 9；失败/错误/跳过均 0 |
| 独立审查探针 | 8 项通过；“通过”表示成功断言当前缺陷存在，不能作为修复后验收测试 |
| `lintDebug` | **失败**，app 2 errors / 63 warnings；2 errors 均为 USE_BIOMETRIC 缺失 |
| `assembleDebug` | 通过 |
| `:app:minifyReleaseWithR8` | 通过 |
| `assembleRelease` | **失败**于 packageRelease：release SigningConfig 缺少 storePassword；不擅自改签名 |
| `git diff --check` | 通过（执行时的工作区） |
| API31 / API37 手工 | 均未执行，adb 无设备 |

执行命令：

```powershell
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug :app:minifyReleaseWithR8 --console=plain
.\gradlew.bat testDebugUnitTest assembleDebug :app:minifyReleaseWithR8 assembleRelease --continue --console=plain
.\gradlew.bat -I build/audit-probes/audit.init.gradle :app:testDebugUnitTest --tests com.example.AuditEvidenceTest --console=plain
adb devices -l
git diff --check
```

证据文件：

- [全量检查日志](/D:/Code/AsterReader/build/audit-2026-11-07.log)。
- [单元测试与构建日志](/D:/Code/AsterReader/build/audit-build-2026-11-07.log)。
- [审查探针源码](/D:/Code/AsterReader/build/audit-probes/src/com/example/AuditEvidenceTest.kt)、[临时 Gradle 接入](/D:/Code/AsterReader/build/audit-probes/audit.init.gradle)、[探针日志](/D:/Code/AsterReader/build/audit-probes/run.log)。探针位于忽略的 build 目录，不进入项目正常源集。
- [8 项探针结果快照](/D:/Code/AsterReader/build/audit-probes/evidence.xml)、[撤去临时接入后的原有单测复核日志](/D:/Code/AsterReader/build/audit-unit-final-2026-11-07.log)。
- [Lint 文本报告](/D:/Code/AsterReader/app/build/intermediates/lint_intermediate_text_report/debug/lintReportDebug/lint-results-debug.txt)。

现有测试的主要缺口：封面仅验证导入/页数而不验证产物；恢复测试没有断言注释承诺的进度策略；迁移测试只是最小表执行 SQL，不是完整 v1 schema 经 Room 升级验证；应用锁 instrumentation 仅断言 Compose 根节点存在；阅读测试核对页码节点，没有验证手势、平移保存、慢加载恢复和最终画面。

## 修复顺序与交付影响

1. 修复 F01 权限与 F05 系列页异常，恢复基础质量检查和页面可进入性。
2. 修复 F02 封面、F03/F04 来源修复、F08/F11 恢复链路，并补真实文件验收。
3. 修复 F06/F07 隐私绕过；建立明确的普通清单与敏感备份契约。
4. 修复阅读状态、后台生命周期、扫描状态、数据并发，再做 UI 设备矩阵。
5. 对 Vault、架构冻结项与未实现页面明确继续实施或提交 ADR 调整范围；最后完成签名和发布门禁。

本轮实际交付：新增本报告和 build 下审查探针/日志。业务模块无本轮修改。安全与数据库迁移行为无本轮改动。报告中的问题尚未修复；设备验收和并发专项仍待执行。不要把本轮审查结果标为项目已完成或已满足 Release Gate。
