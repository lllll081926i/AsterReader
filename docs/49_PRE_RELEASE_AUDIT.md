# 上线前全量审查

日期：2026-11-07。审查基线 `066a2d5`。结论：**暂不放行正式上线**。代码审查、桌面截图、模拟器实测分别记录，不能互相替代。本次同时覆盖一级页面、二级页面、弹窗、确认面板和用户可见文案。

## 已修复

| 编号 | 问题与影响 | 实施位置 |
|---|---|---|
| F01 | 隐藏或加密作品编辑草稿进入 SavedInstanceState；改为仅公开作品可恢复草稿 | `PublicationPropertiesScreen.kt:25`，新增 API31/35 共 6 项恢复回归 |
| F02 | 异步恢复旧导航可能覆盖用户刚选择的 Tab；用操作代次阻止过期恢复 | `AsterViewModel.kt:221,342,363,374`；首轮全量测试确实发现导航测试失败，修复后通过 |
| F03 | 加密作品到达末页即标完成，回翻又撤销完成；与普通作品语义不同 | `LibraryRepository.kt:170`；明确完成事件负责标记，回翻保留完成；扩展保险库回归 |
| F04 | 统计的两个入口分别按当前位置和百分比计数，结果不一致 | `MainActivity.kt:517,584`；统一按已保存的完成状态计数 |
| F05 | 普通 7Z 没有保险库路径已有的解码内存上限 | `ComicArchive.kt:201`；统一限制为 64 MiB |
| F06 | 设置中的长口令表单没有键盘 Insets；输入可能遮挡后续操作 | `SettingsScreen.kt` 增加 imePadding；仍需设备软键盘验收 |
| F07 | 修改保险库口令时取消没有清空当前口令，也没有重置生成口令显示状态 | `FileVaultContent.kt` 完整清空本次表单，运行时禁止切换取消表单 |
| F08 | 引导宣称隐私仅锁入口，关于页遗漏完整密文导出且混淆清单备份 | 引导、关于、设置菜单明确隐藏/加密/清单；明确 RAR4、图片型 EPUB；同步 README |
| F09 | 阅读统计柱状图没有逐日读屏内容 | `ReadingStatsScreen.kt` 为每根柱添加日期与分钟数语义；仍需真实 TalkBack 验收 |

## 必须关闭的发布问题

| 编号 | 等级 | 证据、触发与影响 | 关闭条件 |
|---|---|---|---|
| B01 | P1 | `gradle/libs.versions.toml:45` 的 junrar 7.5.5 落入上游 RAR VM 无限循环漏洞影响范围；普通与保险库 RAR 都进入该库。小型恶意 RAR 可能持续占用 CPU。另两项公告涉及扩展时间头越界和流式提取吞掉完整性失败，实际使用 `Archive.getInputStream` | 单独维护升级至覆盖上述修复的稳定版并回归普通/加密 RAR、损坏头、错误 CRC、取消及 API31；不可用协程超时假装终止库内部 CPU 循环 |
| B02 | P1 | `ComicArchive.kt:149–160` 直接将 EPUB XML 解压流交给 DOM；已关闭外部实体，但没有独立展开字节数、节点与深度预算。512 MB 原始文件限制不能限制解压后 XML | 添加有界读取与解析预算，补压缩膨胀、超深和大量节点样本；当前为源码确认的资源预算缺口，未在用户设备执行耗尽攻击 |
| B03 | 发布门禁 | `app/build.gradle.kts:218` 生成运行库条目的 `notice` 为空，字体有全文，其余主要靠外链；不是完整离线许可证交付证据 | 核对实际打入包的依赖，收集上游要求保留的许可和 NOTICE 并验证最终包；目录里的未使用依赖不能算作打入包的风险 |
| B04 | 发布门禁 | 审查时 APK/AAB 正式签名均失败；APK 明确缺少 storePassword，AAB 签名任务失败 | 本地和 CI 使用同一长期签名，验证 APK 证书、AAB 签名与后续覆盖升级；签名专项实施结果见 docs/50 |
| B05 | 发布门禁 | docs/29 要求的 API31/33/35/37 设备矩阵、7 天 soak、万本压力、真实设备耗电温升、密钥丢失/磁盘写满/Provider 中断演练没有完整证据；无 Macrobenchmark/Baseline Profile 模块 | 补设备验收与可复现测量，不能把单次启动耗时或合成读取测试外推为电池改善比例 |

上游依据：[RAR VM 无限循环公告](https://github.com/junrar/junrar/security/advisories/GHSA-h9h9-2rmf-rvhp)、[扩展时间头公告](https://github.com/junrar/junrar/security/advisories/GHSA-h76x-7cgm-p442)、[流式完整性公告](https://github.com/junrar/junrar/security/advisories/GHSA-frq4-6xmx-hm4g)。上游分别标记修复版本 >=7.6.1、8.1.1、8.1.2；不能只升到第一个版本就宣称全部关闭。Commons Compress 1.27.1 高于其官方页面所列相关历史修复版本；不据此承诺不存在其他漏洞。

## 界面、交互与文字待修清单

以下均为具体源码发现，不将它们标成设备已复现。P2 需要修复或明确发布说明与替代操作。

| 编号 | 等级 | 页面与定位 | 问题及验收要求 |
|---|---|---|---|
| U01 | P2 | `LibraryAndSearchScreen.kt:545,1258,2330,2500` | 多处筛选仍固定 34dp，文字限制单行；200% 字体可能截断，自定义 clickable 没有统一 48dp 触控和选中语义。统一可增高布局并检查 TalkBack selected |
| U02 | P2 | `SettingsScreen.kt:186`、`PublicationPropertiesScreen.kt:93`、`HiddenBooksScreen.kt:184`、`ShelvesAndVaultScreen.kt:570` | 多个确认框仍使用分开的 confirm/dismiss 按钮，宽度随文字变化，缺少图标。与用户并列按钮等宽规则不一致；统一成可换行的等宽操作组 |
| U03 | P2 | `ShelvesAndVaultScreen.kt:509` | 书架排序和删除仍挤在弹窗，单行同时放上移/下移/删除。与设置书架页重复；合并至独立管理页，统一删除确认和返回 |
| U04 | P2 | `ShelvesAndVaultScreen.kt:619–629`、`AsterViewModel.kt:1386` | 新建书架在异步保存成功前清空输入并关闭；重名或写入失败只能收到通用错误。等待成功再关闭，失败保留表单并定位错误；空名称时禁用创建 |
| U05 | P2 | `LibraryAndSearchScreen.kt:853–884` | 批量加入书架用不可滚动 Column；大量书架可越出视口，空书架列表只有取消。使用 LazyColumn，空状态提供新建或明确说明 |
| U06 | P2 | `ReaderScreen.kt:448–501` | “上一话/下一话”在无章节或边界时实际跳 10 页。按钮文字与动作不一致；应分别显示前/后 10 页，或正确禁用章节操作 |
| U07 | P2 | `ReaderScreen.kt:708`、`PublicationDetailScreen.kt:750,805` | 目录列表固定 340dp，部分抽屉标题/操作未预留大字号宽度。横屏/软键盘/200% 字体需分别验证；按可用高度约束内容并允许标题换行 |
| U08 | P2 | `MainActivity.kt` 不可用作品回退 | 按钮写“返回资料库”，实际只 pop 一层，可能回到搜索或详情。改准确的“返回”，或明确导航到书库 |
| U09 | P2 | `PublicationDetailScreen.kt` 批注面板 | 输入笔记后按钮仍叫“添加书签”，实际创建 NOTE；应随输入显示“保存笔记/添加书签”，并反馈异步结果 |
| U10 | P2 | `SettingsScreen.kt:140–152` | 第一项书架仍有上移，最后一项仍有下移；用户点击无可见排序变化。边界操作禁用，排序立即反馈 |
| U11 | P2 | `PublicationPropertiesScreen.kt` 文件属性 | 密文作品仍复用普通 URI/仓库展示，缺少密文状态与最近校验信息。避免把内部 vault URI 当成用户可重新授权的来源 |
| U12 | P2 | 文档、版本与架构 | 旧 UI 规格正文仍保留两个主 Tab、诊断入口等早期规划，与后补用户决定冲突；任务矩阵有过时“未开始”。Nav3/Adaptive/Hilt 仍是目标，当前为手动栈和大 ViewModel。不得宣称已完成冻结架构；需按实现逐项对齐，而非审查中整体重写 |

## 逐页面覆盖与边界

| 页面族 | 本轮覆盖 | 仍需补充 |
|---|---|---|
| 启动、引导、应用锁、紧凑认证面板 | 路由/失败回退/文案/锁定逻辑；已有 API31/35 截图回归 | 真正生物识别、冷却计时、系统返回、进程死亡手工 |
| 书库、继续阅读、批量、仓库创建与扫描 | 标题 Insets、动作绑定、筛选、空态、扫描与来源恢复单测 | 万本、权限撤销、多 Provider 和大量书架 |
| 书架、书架详情、隐藏、系列 | 管理/新建/移除路径，授权与空态源码 | 长名称、长列表、删除误触、读屏 |
| 搜索、历史、结果、筛选面板 | 独立 Tab、结果订阅生命周期、筛选状态与尺寸源码 | 所有筛选项大字号、选中语义、键盘焦点 |
| 作品详情、文件属性、元数据、封面与系列编辑 | 返回/未保存确认/保存回调、草稿状态恢复、密文入口 | 输入长度上限、保存中连续点击、错误焦点、完整属性 |
| 漫画/PDF、单行阅读设置、目录、书签笔记 | 阅读完成与回翻、工具栏、独立设置、亮暗/横屏截图及数据测试 | 章节边界文案、超大 PDF、真实触屏手势、全部抽屉横屏 |
| 统计、设置各分类、关于、许可详情 | 完成计数、柱图语义、设置层级、许可加载与格式/隐私文案 | 真实 TalkBack、完整许可正文、软键盘不遮挡 |
| 保险库建立/解锁/加密/导出/恢复/改口令/校验 | 取消与内存口令、认证失败、实际 Keystore 与归档/PDF 解密设备测试 | 磁盘写满、断电、设备密钥失效与跨真实设备恢复 |

保护策略保持开启；没有为截图而解除用户应用防截屏。截图测试的正常/空/错误/锁定仅覆盖相应 fixture，不代表每个 Screen 的四种状态均完成手工验收。当前没有发现重复增加系统状态栏 Insets 的统一根因，主页面已有相同高度和上沿断言；仍存在的局部固定高度问题见 U01/U07，不能继续靠全局负 padding 压缩。

## 核验结果

- `build/pre-release-final-verification.log`：`BUILD SUCCESSFUL in 3m`；app 195、database 10、vault 9，共 214 项，0 失败；Debug Lint 0 错误/52 警告，Release Lint 0 错误/38 警告；Debug、AndroidTest 与 Release R8 通过。没有屏蔽警告。
- API35 模拟器 `127.0.0.1:7555`：覆盖安装 Debug 和测试包成功；`build/pre-release-device-check.log` 为 `OK (2 tests)`，7.385 秒。真实 Keystore、归档/PDF 代理读取、导出恢复、锁定撤销与合成读取通过。应用启动 `Status: ok`。没有重置用户数据。
- API31：Robolectric 回归；API37：编译检查。API31/API37 未完成设备手工验收。平板/横屏/大字体为桌面截图证据，不能替代真实折叠设备和 TalkBack。
- Debug APK 含 4 个 `libandroidx.graphics.path.so`；arm64/x86_64 的 LOAD 段对齐为 16384，`zipalign -c -P 16 4` 通过，证据 `build/pre-release-zipalign.log`。当前模拟器为 4096 页大小，没有 16 KB 设备实测或最终签名 Release 的完整 ELF/RELRO 验收。依据：[Android 16 KB 页支持说明](https://developer.android.com/guide/practices/page-sizes)。
- Debug APK SHA-256：`BABCF9F567AD2FD5AD9E2772C22A2382393D64F84004F05E96F92864382F260C`。后续重新构建产物应重新计算，不沿用该值。
- 安全与迁移：无数据库版本、容器格式、KDF 或密钥更换；不删除用户原文件；草稿不落系统恢复状态。既有保险库不迁移。

后续签名实施会解决 B04，不自动关闭 B01/B02/B03/B05，也不代表用户已批准正式公开发布。
