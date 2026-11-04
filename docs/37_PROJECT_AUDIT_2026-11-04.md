# Aster Reader 全项目审查记录

审查日期：2026-11-04。源码基准：`66bfd6d`。本次只新增审查记录，不修复业务代码。

结论：当前工程能够构建 Debug APK，但核心业务仍包含大量演示实现。真实文件阅读、安全认证、Vault、仓库扫描和备份均未达到项目 Alpha Gate。现有测试通过不能作为上述功能可用的证据。

优先级：P1 表示应在提供给用户处理真实文件前修复；P2 表示需要安排修复的功能或工程问题。本次确认 11 项 P1、12 项 P2。问题以源码证据为依据；操作步骤为从代码推导的复现路径，未经设备实测。

## 审查范围与方法

- 覆盖 `:app`、`:core:model`、`:core:engine-api`、`:core:security-api`、`:core:database`、`:core:designsystem`。
- 检查生产源码、测试、Manifest、资源、Gradle 配置、备份规则、版本管理状态，以及 `starter-contracts` 与实际实现的接入情况。
- 对照架构冻结决策、DoD、设计规范、UI 页面矩阵、阅读引擎、SAF 和安全验证计划。
- 对 UI 检查事件处理、状态、授权、导航、阅读进度、动效与无障碍代码。没有执行逐屏截图、布局或手势验收。
- 不把文档中的待开发功能全部计为缺陷；下列问题重点记录已经暴露的操作入口、错误行为和冻结规范偏差。
- 未执行联网漏洞库扫描、第三方许可证逐项核验或恶意格式模糊测试。没有据依赖版本新旧推断已知漏洞。

## P1：发布阻断问题

### P1-01 加密操作只改数据库标记，并覆盖真实源 URI

位置：`app/src/main/java/com/example/ui/AsterViewModel.kt:502`、`:581`、`:380`。

`toggleBookVaultProtection()` 和批量加密只设置 `isVaultProtected/isVaultEncrypted`，把 `filePath` 改成拼接的 `vault://` 地址。代码没有读取源文件、生成密文、调用 Keystore 或验证密文。解除保护又把路径改成假定的 `/storage/emulated/0/Books/...`。完整性校验仅统计数据库行数，随后无条件显示“校验通过”。

影响：外部原文件仍是明文，用户得到错误的安全保证；原始 SAF URI 被覆盖，后续真实阅读功能将失去源文件引用。

修复：接入真实 Vault 实现。按读取、加密、认证校验、提交顺序执行；提交前保留原 URI。失败时不得改变保护状态或宣称完成。完整性校验必须读取并认证密文。

验收：使用真实文件验证密文、篡改检测、中断恢复、失败回滚和源 URI 保留。

### P1-02 指纹图标、空 PIN 和固定 PIN 可以绕过认证

位置：`app/src/main/java/com/example/ui/screens/AuthGateAndOnboardingScreen.kt:131`、`:188`、`:268`；`app/src/main/java/com/example/ui/AsterViewModel.kt:327`。

点击应用内指纹图标后，代码等待 620ms 即调用 `onAuthenticated()`，不依赖系统认证结果。切换密码验证后，空输入被替换为 `1234`；`verifyPin()` 即使在修改 PIN 后也永久接受 `1234`。系统凭据界面取消后仍可走这些路径。

影响：获得设备前台操作能力的人可以直接解锁受保护内容。

修复：认证成功必须来自受信任的系统认证回调或真实的应用密码校验。移除延时成功、默认通行 PIN 和空输入替换；实现失败节流与安全密码验证材料存储。

验收：取消系统认证、点击图标、空 PIN、旧 PIN、默认 PIN、连续错误输入均不得解锁。

### P1-03 应用锁与后台自动锁定没有生效

位置：`app/src/main/java/com/example/ui/AsterViewModel.kt:95`、`:278`、`:349`；`app/src/main/java/com/example/MainActivity.kt:70`。

应用锁默认开启，但 `isAppLockedNow` 默认是 false，所有写入也保持 false。根界面没有用该状态阻止主内容构建。立即锁定只重置 Vault 布尔值。没有接入进程前后台、超时或息屏事件，`AppLockManager` 仅有接口。

影响：开启应用锁后冷启动仍直接进入书库；Vault 会话在进程仍存活时不会按后台或息屏策略失效。

修复：实现独立的应用锁状态机和会话有效期。启动先判定授权，再加载敏感内容；后台、息屏及显式锁定时同步撤销会话并遮盖页面。

### P1-04 截图保护和最近任务保护开关没有对应保护

位置：`app/src/main/java/com/example/MainActivity.kt:79`；`app/src/main/java/com/example/ui/screens/ShelvesAndVaultScreen.kt:1651`。

Activity 启动后明确调用 `window.clearFlags(FLAG_SECURE)`。安全设置仍显示防截屏与多任务遮罩开关，开关只修改内存状态；没有执行最近任务预览保护。

影响：用户开启保护后，受保护阅读内容仍可被截图或进入最近任务预览。

修复：把窗口安全策略绑定到实际页面与授权状态；预览环境的特殊需求必须隔离到独立构建配置，不得覆盖生产安全策略。

### P1-05 系统备份没有排除受保护数据

位置：`app/src/main/AndroidManifest.xml:9`；`app/src/main/res/xml/data_extraction_rules.xml:8`；`app/src/main/res/xml/backup_rules.xml:9`。

应用启用 `allowBackup`，两份备份规则仍是模板。云备份没有任何排除项，设备迁移也没有定义受保护数据排除规则。当前 Room 内直接保存标题、来源、章节正文和批注。

影响：项目要求排除的隐私数据库内容没有得到备份边界保护。实际是否执行云备份取决于设备及账户条件；本次没有实测备份。

修复：定义数据分区及 cloud-backup/device-transfer 排除规则，并使用真实备份与恢复流程核验。后续新增密钥包装材料、Vault 索引时同步排除。

### P1-06 锁定状态仍可在继续阅读和平板详情显示真实元数据

位置：`app/src/main/java/com/example/ui/screens/LibraryAndSearchScreen.kt:159`、`:164`、`:651`、`:1379`；`app/src/main/java/com/example/ui/screens/PublicationDetailScreen.kt:110`；`app/src/main/java/com/example/ui/screens/ReaderScreen.kt:163`。

继续阅读优先固定选择 `dungeon_meshi_12`，没有检查该书当前是否已受保护。平板详情接收 `isVaultUnlocked` 却没有使用，直接加载封面、标题、作者、简介和章节。默认选中书籍、刚加密的选中书籍、解锁后再次锁定的书籍均可能留在面板。已进入的详情和 Reader 也没有会话失效后停止展示内容的判断。

影响：只隐藏列表中的卡片不能保护其他显示路径；真实标题和内容仍可在锁定后出现。

修复：统一输出经过授权裁剪的 UI 模型；授权失效时立即清除敏感 UI 状态和数据订阅。继续阅读必须采用同一策略。

### P1-07 私密书架不向成员作品传播保护策略

位置：`app/src/main/java/com/example/ui/AsterViewModel.kt:566`、`:689`；`app/src/main/java/com/example/ui/screens/ShelvesAndVaultScreen.kt:429`、`:799`。

新建私密书架只设置 `ShelfEntity.isProtected`。向该书架添加作品只修改 CSV 关系。作品在普通资料库及搜索中仍由自己的 `isVaultProtected` 决定访问权限，而书架界面将受保护书架标为“加密”。

影响：用户把作品加入标为加密的书架后，仍可从资料库或搜索直接打开作品，作品没有加密。

修复：明确区分仅锁定书架入口与加密成员作品。对后者实现统一策略计算与真实批量迁移，所有入口使用相同授权规则。

### P1-08 删除受保护书籍后，遗留批注绕过搜索隐私判断

位置：`core/database/src/main/java/com/asterreader/core/database/AsterDao.kt:34`；`core/database/src/main/java/com/asterreader/core/database/AsterEntities.kt:59`；`app/src/main/java/com/example/ui/screens/LibraryAndSearchScreen.kt:1743`。

批量移除只删除 books，批注没有外键或联动清理。搜索仅在 `parentBook?.isVaultProtected == true` 时隐藏批注。父书籍不存在时判断放行，仍按原文、笔记和章节标题匹配。

复现路径：给受保护书籍添加批注 → 从资料库移除该书索引 → 锁定 → 搜索批注中的词。

影响：本应隐藏的批注原文进入普通搜索。

修复：明确批注保留策略；删除书籍时事务清理或保留独立保护信息。找不到父对象时必须默认隐藏，不能默认公开。

### P1-09 导入没有读取真实文档，Reader 显示演示内容

位置：`app/src/main/java/com/example/ui/AsterViewModel.kt:772`；`app/src/main/java/com/example/ui/screens/ReaderScreen.kt:171`、`:261`、`:274`、`:341`；`app/src/main/java/com/example/MainActivity.kt:118`。

导入函数只根据 URI 末段猜扩展名，写入固定 140 页、24.6 MB 和两章演示正文。未读取文档、查询真实显示名或保存持久 URI 授权。TXT 被改标 EPUB，未知格式也被当作 EPUB。Reader 仅把 CBZ 视为漫画并循环网络样图，其余格式包括 PDF/CBR 都显示文本画布。选择器取消或抛异常时还会导入一条虚构书籍。

影响：用户导入成功后无法阅读自己选择的文件；取消操作也改变资料库。真实源访问恢复与格式错误处理均未完成。

修复：取消时不写数据；完成 SAF 持久授权、元数据读取、格式探测及真实 Engine/DocumentSource 接入。只在解析和索引提交成功后显示导入完成。

验收：真实 EPUB/TXT/CBZ/CBR/PDF 内容与源文件一致；取消、权限撤销、未知格式、损坏文件与进程重启均有明确结果。

### P1-10 创建仓库与扫描直接生成成功结果

位置：`app/src/main/java/com/example/ui/AsterViewModel.kt:834`、`:854`。

创建仓库根据用户输入的名称拼接 `content://`，没有目录授权，直接写入 12 本、1.8 GB 与扫描完成。重新扫描仅显示成功消息，没有枚举或更新文件。

影响：仓库列表会显示不存在或未授权的目录；用户无法索引自己的目录，离线和权限丢失也被当作健康。

修复：接入 OpenDocumentTree、持久授权、扫描任务与真实扫描结果。保存 URI 与扫描统计必须来自授权提供者和实际扫描。

### P1-11 备份和恢复密钥为固定展示，没有可恢复产物

位置：`app/src/main/java/com/example/ui/AsterViewModel.kt:933`；`app/src/main/java/com/example/ui/screens/ShelvesAndVaultScreen.kt:1751`、`:1993`、`:2280`。

备份只更新内存时间和成功消息，弹窗固定声称生成 `aster_backup_2026.zip`，没有写出文件。恢复密钥是硬编码字符串，没有生成、包装或恢复流程。

影响：用户可能相信已有备份或已保存恢复凭据，随后卸载、换机或清理数据，造成不可恢复的数据丢失。

修复：实现输出目标选择、可验证备份格式、恢复校验与恢复演练；恢复材料必须与真实密钥槽绑定。实现前不得报告备份成功或展示假恢复凭据。

## P2：功能和工程问题

### P2-01 设置、PIN 和导航状态在进程重建后丢失

位置：`app/src/main/java/com/example/ui/AsterViewModel.kt:158`、`:336`、`:905`。

所有设置只保存在默认 `MutableStateFlow(AsterUiState())`。没有 DataStore 设置读写或 SavedStateHandle。修改 PIN、阅读配置、主题与布局后重启会恢复默认；导航栈和搜索状态也无法恢复。会话授权应在重启后失效，但不能因此清除用户的安全配置。

修复：分离持久设置、可恢复导航状态和短期授权状态；分别使用持久存储、SavedStateHandle 与安全会话管理。

### P2-02 清空资料库后重启会重新插入演示书籍

位置：`app/src/main/java/com/example/ui/AsterViewModel.kt:163`、`:175`。

每次 ViewModel 创建，只要 books 数量为零就重新写入全部 SeedData。因此主动清空资料库后重启会出现旧演示内容。初始 Flow 值也是 SeedData，数据库加载前会短暂显示与真实库不符的数据。多表初始化没有事务，中断后可能只剩部分种子数据。

修复：演示数据隔离到 Preview/测试或明确的演示构建；生产使用真实空状态和独立初始化标识。多表首次初始化用事务。

### P2-03 横向翻页模式跳页后，页码和实际页面不同步

位置：`app/src/main/java/com/example/ui/screens/ReaderScreen.kt:764`、`:1286`、`:1607`。

滑块和目录修改外层 `currentPage`，Pager 只在初次创建时读取它。没有在后续跳页时调用 `scrollToPage/animateScrollToPage`。滑块跳转后显示旧页，数据库却保存新页；下一次滑动又写回 Pager 页码。

修复：让 seek 命令驱动 Pager，并从稳定的页面状态提交阅读进度，避免双向事件形成循环。

### P2-04 默认条漫模式只显示八页，滚动不更新进度

位置：`app/src/main/java/com/example/ui/screens/ReaderScreen.kt:1514`、`:1537`。

LazyColumn 固定 `items(count = 8)`，没有滚动位置到页码的回传，也没有按保存的页内位置恢复滚动。实际阅读后仍保存进入时的页码，八页后直接显示章节结束。

修复：根据真实页面索引创建列表，使用可见页和偏移更新 Locator；打开或跳页时恢复相应位置。

### P2-05 书签图标与数据库不是同一真源，取消不会删除书签

位置：`app/src/main/java/com/example/ui/screens/ReaderScreen.kt:206`、`:478`。

`bookmarkedPages` 在进入时被初始化为包含当前页，而不是从批注读取。取消只修改内存集合，没有删除数据库记录；再次添加会重复插入。重新进入也不能正确恢复其他页书签状态。

修复：从 BOOKMARK 批注派生图标状态，按作品和 Locator 幂等增删，移除独立的可写内存集合。

### P2-06 搜索排序使用错误字段，书签快捷分类没有筛选书签

位置：`app/src/main/java/com/example/ui/screens/LibraryAndSearchScreen.kt:1733`；`app/src/main/java/com/example/ui/screens/ShelvesAndVaultScreen.kt:246`、`:294`。

“文件大小”实际按 `totalPages` 排序，“最近添加”按混合语义的字符串 id 排序，模型没有对应的真实文件字节数和添加时间。“书签札记”快捷分类落入默认分支，显示普通书籍而非书签。

修复：保存真实 sizeBytes/addedAt 并按对应字段排序；书签分类由批注关联作品生成，或进入专门的批注结果页。

### P2-07 底部导航的自定义手势没有可访问点击动作

位置：`core/designsystem/src/main/java/com/asterreader/core/designsystem/AsterMotion.kt:105`；`app/src/main/java/com/example/ui/components/AsterComponents.kt:148`。

`asterBounceClick()` 通过 pointerInput 手动调用回调，没有 clickable/selectable 或 OnClick semantics。底部导航仅使用该手势；图标虽有说明，TalkBack 和键盘没有对应的标准激活动作，也没有选中状态语义。

修复：使用 selectable/clickable 提供语义、焦点和键盘行为；按压动效监听 InteractionSource，不替代标准点击。

### P2-08 减少动态效果没有覆盖导航以外的主要动效

位置：`core/designsystem/src/main/java/com/asterreader/core/designsystem/AsterMotion.kt:105`；`app/src/main/java/com/example/ui/components/AsterIllustrations.kt:60`；`app/src/main/java/com/example/ui/components/AsterComponents.kt:127`；`app/src/main/java/com/example/MainActivity.kt:320`。

reduceMotion 只传入根页面切换。按压弹簧、导航色彩弹簧、插图无限漂浮和底部导航滑入滑出均不读取该选项。

修复：在设计系统统一提供动效偏好，所有组件遵守同一降级策略；补充系统动画关闭和应用减少动态效果两种场景的验收。

### P2-09 SDK 与冻结平台基线不一致

位置：`app/build.gradle.kts:13`、`:17`；`core/database/build.gradle.kts:8`，其余 core 模块同类配置。

实际为 minSdk 24、targetSdk 36、compileSdk 36.1，冻结要求为 31/37/37。当前构建没有验证 API37 的编译约束与目标行为，并意外声明了 Android 7 起的支持范围。

修复：统一平台约定并在环境支持时执行 API31/API37 测试。若需要改变冻结决策，应先记录 ADR 和影响，不应仅修改文档掩盖偏差。

### P2-10 设计系统存在双份实现，业务仍绕过令牌与实验 API 边界

位置：`app/src/main/java/com/example/ui/theme/Theme.kt:88`；`core/designsystem/src/main/java/com/asterreader/core/designsystem/Theme.kt:96`；`app/src/main/java/com/example/ui/screens/ReaderScreen.kt:143`；`app/src/main/java/com/example/ui/screens/AuthGateAndOnboardingScreen.kt:88`。

实际根主题使用 app 内版本，默认参数关闭动态取色；core 版本默认开启但未作为全局主题入口。当前 Activity 显式传 true，因此当前正常启动仍开启 Monet。设置页允许用户切换到品牌色，违反“fallback 仅异常兜底”的冻结要求。业务页大量硬编码圆角、颜色和动画，且直接 OptIn 实验 Material3 API。

修复：合并到唯一的 core 主题与语义令牌；去除正常用户入口的 fallback 色板切换；实验组件只从设计系统暴露。

### P2-11 TTS 不会自动推进段落，播放状态无法反映引擎结果

位置：`app/src/main/java/com/example/ui/screens/ReaderScreen.kt:213`、`:461`、`:636`。

播放只 speak 当前段落；`ttsCurrentParagraphIndex` 始终为零，没有完成回调或队列推进。`isTtsPlaying` 由按钮自行修改，没有处理初始化失败、语言不可用、speak 失败或朗读完成。

影响：点击朗读只读第一段，完成后界面可能仍显示播放中。

修复：通过独立状态持有者管理 TTS 初始化、错误、UtteranceProgressListener、段落队列、暂停恢复及生命周期。

### P2-12 Git 跟踪了 463 个 core 构建生成文件

位置：`core/database/build/`、`core/designsystem/build/`、`core/engine-api/build/`、`core/model/build/`、`core/security-api/build/`。

`git ls-files` 确认 463 个生成文件已进入版本管理。当前 `.gitignore` 的 `build/` 能阻止新文件进入，但不能移除已跟踪项。正常重跑测试会修改 KSP 缓存、路径映射、测试报告和二进制结果文件，产生与业务无关的差异。

修复：在独立变更中将这些目录从 Git 索引移除，保留本地文件；继续忽略构建产物。不得通过提交每次生成的缓存维持表面干净。

## 架构与验收缺口

以下是实现成熟度和门禁缺口，不额外重复计入上述 23 个问题。

| 范围 | 当前状态 | 缺口 |
|---|---|---|
| 分层 | AsterViewModel 直接依赖 Room DAO，Screens 接收实体和大型 ViewModel | Repository、SourceProvider、按功能划分的状态模型未接入 |
| 安全 | security-api 提供接口 | 没有 VaultManager/VaultKeyRepository/AppLockManager 的生产实现与接线 |
| Reader | engine-api 提供接口，UI 自行渲染演示内容 | 没有真实 PublicationEngine/ReaderSession 实现与 Registry；Locator 没有成为进度真源 |
| 导航和自适应 | 自建 List<SubScreen>、700dp 分支 | 未接入冻结的 Navigation 3、Material3 Adaptive 和 Hilt |
| 数据迁移 | Room version=1，未发现 destructive migration | exportSchema=false，无 schema 快照与迁移测试基础；本次没有执行 schema 变更，也不声称当前已有迁移丢失 |
| 数据一致性 | 多处读实体后 copy 并整行 Update，多表写入缺少事务 | 后续需验证并发进度/收藏/保护更新与中断恢复，避免旧字段覆盖 |
| 错误状态 | 直接 launch DAO 写入，数据 Flow 无统一错误 UiState | 数据库失败、权限失效、空间不足、解析失败的可恢复状态不足 |
| 缓存 | 清理只修改 coverCacheMb/pageDecodeCacheMb 常量 | 没有真实缓存计量和清理；真实受保护封面缓存策略尚未建立 |
| 阅读控制 | 部分开关仅更新字段 | 音量键翻页没有键事件接线；真实缩放平移、裁边、fit policy 和 PDF 能力尚未验证 |
| UI 规范 | 已有多屏 UI，任务矩阵全部“未开始” | 缺少逐屏真实状态、截图、TalkBack、字体缩放、Insets、返回和多窗口验收证据 |
| 性能与发布 | Release 配置 isMinifyEnabled=false | 无 Baseline Profile、Macrobenchmark、R8 性能证据和正式发布门禁记录 |
| 文档状态 | README/VALIDATION_REPORT 为工程档案验证 | 不能将旧的文档 PASS 当作 Android 功能验收；应明确演示功能与真实实现边界 |

## 本次验证结果

| 检查 | 结果 | 证据 |
|---|---|---|
| `gradlew.bat testDebugUnitTest lintDebug assembleDebug --console=plain` | 通过；首轮测试任务复用已有结果 | Gradle BUILD SUCCESSFUL，Debug APK 可打包 |
| `gradlew.bat :app:testDebugUnitTest :core:database:testDebugUnitTest --rerun-tasks --console=plain` | 强制重跑通过；共 23 个测试，0 失败、0 错误、0 跳过 | `build/audit-unit-tests-2026-11-04.log`；app 21 项，database 2 项 |
| Lint | app：0 errors、57 warnings；其余五个 core 模块无 Lint 问题 | `app/build/reports/lint-results-debug.html` 及各模块报告 |
| `:app:assembleRelease` | Release 编译、Dex 和 Lint Vital 通过；packageRelease 因缺少配置的 `my-upload-key.jks` 失败，没有完成 Release APK 打包 | `build/audit-release-2026-11-04.log` |
| Android 设备连接 | 无设备或模拟器在线 | `adb devices -l` 为空 |
| API31/API37 手工验收 | 未执行 | 无运行设备；SDK 配置也未达到冻结 API37 基线 |
| 真实格式、安全故障注入及备份恢复 | 未执行 | 对应生产实现尚未接入，不能用演示状态替代验证 |

测试覆盖说明：现有测试包含状态切换、枚举、数据库 CRUD、Activity 创建和基本示例。安全测试明确把 `verifyPin("1234")` 成功作为预期，没有验证取消、失败、默认 PIN 失效、真实密文、篡改或后台锁定。engine-api、security-api、model、designsystem 没有单元测试源码。设备测试仅检查包名。

证据留存：本次新生成的单元测试 XML 与 Lint 报告复制到 `build/audit-evidence-2026-11-04/`。工作开始时 Git 工作区干净；验证后只恢复本次构建修改的已跟踪生成文件，保留日志和证据副本。`build/` 被忽略，因此这些证据不会自动随报告进入 Git。

## 建议实施顺序

1. 移除伪认证与成功提示。明确标识未实现能力，防止用户对加密、备份和恢复形成错误预期。
2. 实现 SAF、Repository、真实导入和数据错误状态。保留原始 URI，补充真实文档测试样本。
3. 按冻结顺序实现应用锁、密钥体系和 Vault；补齐元数据、缓存、备份及窗口保护边界。
4. 接入真实 Reader Engine/Session，以 Locator 统一跳页、阅读进度、书签和内容恢复。
5. 修复设置持久化、排序、导航无障碍、动效降级和设计系统接线。
6. 在 API31/API37 设备完成安全与 UI 验收，再评估 Alpha Gate。依赖升级、SDK 调整和生成文件索引清理各自独立提交。

## 交付影响

- 实施结果：完成源码与配置审查、Debug 构建、Lint、单元测试强制重跑；新增此报告。
- 修改模块：没有修改业务模块、依赖、数据库 schema 或安全配置。
- 安全与迁移影响：本次没有执行真实加密、解密、原文件删除或数据库迁移；发现的问题仍未修复。
- 已知限制：没有设备手工验证、截图、性能基准、外部依赖漏洞库扫描和生产签名发布验证。源码审查不构成不存在其他问题的保证。
