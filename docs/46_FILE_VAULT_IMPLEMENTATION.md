# 文件加密与导出实施规范

日期：2026-11-07。以下记录本次实施契约、实际能力及核验边界。

## 威胁与边界

保护被复制的应用数据与导出文件、未授权应用访问、密文篡改、部分写入和会话撤销。不承诺抵抗已控制 Android 内核、运行中进程、输入法或屏幕摄像的攻击者，不承诺闪存物理擦除、云端历史副本清除或离线整库回滚检测。

短 PIN 只用于应用访问门禁，不用于文件加密。文件保险库使用独立长口令。设备槽先使用 Argon2id 包装随机 Master Secret，再由 Android Keystore 设备密钥加密保存；两层均必须成功，设备密钥不导出。设备密钥明确不配置系统认证绑定，应用门禁不冒充硬件认证；保险库另需独立口令。锁定、进程重启后清除内存密钥，不能仅靠成功布尔值解封。设备密钥丢失时不得自动创建新密钥覆盖旧保险库。

Argon2id 使用 Bouncy Castle 1.86 的现成实现，版本 0x13，内存 64 MiB、3 遍、4 lanes、随机 16 字节盐、32 字节输出。导入仅接受这组已知参数，在执行 KDF 前检查；通过互斥锁限制并发，避免恶意成本和并发内存耗尽。创建要求 12–256 字符并拒绝明显重复及常见弱口令，提供密码学随机生成口令；不声称长度检查能证明熵。口令原样匹配，不 trim 或归一化。

导出文件可在新设备通过导出口令恢复，不依赖原设备 Keystore。每次导出重新生成随机 Master Secret、盐和 nonce。修改本机口令不撤销已流出的导出文件。没有有效导出文件时，卸载、清除数据或设备密钥丢失会失去本机保险库，必须在界面明确提示。

## 容器与提交

每个不可变对象使用独立随机 DEK、对象 UUID、nonce 前缀和 AES-256-GCM 分块。块大小 1 MiB，每块独立 128 位 tag。头部长度、块大小、UUID、算法版本、DEK 包装和总字节数均受认证；拒绝头损坏、换块、截断、尾随数据、长度溢出。空文件也必须验证头部 tag。正文块号不得使用头认证的保留 nonce。

数据先写密文 staging，fsync，完整重读全部 tag 并比对源内容摘要，然后原子发布不可变数据。加密元数据最后原子发布，构成提交标志。提交前失败保留源文件；没有元数据的孤立密文不自动销毁。外部源文件保留，界面明确外部明文仍存在，避免文档 Provider 缺乏稳定版本时误删。应用私有 imports 目录内的导入副本必须再次比对完整明密文摘要，才可清理；路径检查使用 canonicalFile，导入外来密文时清除 originalUri。原文档缓存和封面在成功提交后清理，不能承诺历史闪存零残留。

标题、来源、封面、章节、批注和阅读状态写入加密元数据。普通数据库中不保留加密作品的真实记录；解锁后在内存组合书库。导入必须完整验证后提交，不允许先更新普通数据库再验 tag。

阅读通过内存随机解密与只读代理文件描述符，不写明文临时文件。锁定先遮盖 UI，再撤销会话代次，关闭安全来源与缓存；旧任务不能重新发布明文。导出先在私有目录完成密文及校验，再写 SAF 目标并重读密文校验；失败不得报告成功，不能自动删除用户选择的文件。

## 实施范围与核验

修改 `core:vault`（容器、KDF、限速）、`app` 数据层（Keystore、事务与读取）、ViewModel（授权、撤销、操作状态）、详情和设置页（加密、导出、恢复）及相应规范。现有清单备份保持独立语义。新增端到端及故障测试：空文件、跨块读取、错误口令、篡改、换块、整块截断、超限头、取消、提交中断、锁定后旧回调、导出恢复及无明文临时文件。执行全量单测、数据库测试、Lint 和 debug 构建，再覆盖安装模拟器。API31/API37 手工测试独立记录。

依据：[RFC 9106](https://www.rfc-editor.org/rfc/rfc9106.html)、[Android Keystore](https://developer.android.com/privacy-and-security/keystore)、[只读代理描述符](https://developer.android.com/reference/android/os/storage/StorageManager#openProxyFileDescriptor(int,android.os.ProxyFileDescriptorCallback,android.os.Handler))、[Bouncy Castle 1.86](https://www.bouncycastle.org/resources/new-release-bouncy-castle-java-1-86/)、[OWASP 密码存储](https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html)。

## 二进制协议 v2

所有整数使用大端。对象头前 44 字节依次为 magic `ASTERV02`（8）、UUID（16）、原文长度（8）、块大小（4）、nonce 前缀（8）。后接包装 DEK（nonce 12 + 密钥密文 32 + tag 16），以 `dek-v2` 和前 44 字节为 AAD。104 字节头后接空正文的 GCM tag 16；对象头总长 120。

头认证 nonce 为前缀加块号 -1；正文块号从 0 开始，以前缀加块号组成 12 字节 nonce。正文 AAD 为 104 字节头、块号和该块明文长度。正文最多 16 GiB；准确密文大小为 `120 + 原文长度 + 16 × 块数`。

口令槽为 96 字节：magic `ASTERX02`（8）、Argon2 内存/遍数/lanes（各 4）、盐（16）、nonce（12）、Master 密文（32）和 tag（16）。前 48 字节为 AAD。本机 Keystore 再包装该槽，总长 124 字节；AAD 为 `aster-device-slot-v2`。导出为 96 字节口令槽加密文对象，对象解密后为 4 字节元数据长度、JSON 元数据和原始文件字节；图片文件夹使用已认证长度表定位每页。

## 访问与防猜测

第 5 次尝试后进入 30 秒等待，随后指数增加至 1 小时。尝试次数在派生口令前原子持久化，进程重启不清零；同次开机同时检查单调时钟和墙钟。成功认证后重置。应用 PIN 验证串行执行，并保存递增限速等级。该限速不能阻止攻击者对复制出的导出文件离线猜测；必须依靠随机长口令和 Argon2id 增加攻击成本。

保险库口令不写入保存状态、导航、日志或剪贴板。创建和输入口令页强制禁止截图；解锁保险库期间禁止截图及最近任务预览。屏幕关闭撤销会话；后台按应用策略锁定。系统文件选择器最多保留 120 秒授权窗口，超时或锁屏即撤销。旧读取、旧渲染、旧封面请求必须检查会话代次。PDF 使用只读代理描述符；归档使用随机解密通道，不将代理 FD 重新按文件路径打开。

## 当前边界

本次交付逐个作品加密、独立口令密文导出、跨设备导入恢复、口令修改和完整性验证。现有清单备份不是漫画正文备份。尚未交付整库快照、文件夹自动加密策略、批量迁移、独立恢复密钥槽或解除加密后导出明文；删除加密作品暂时阻止，避免误删唯一密文。RAR5 仍受既有解析器限制，需要转换为 ZIP/CBZ。设备密钥失效后可在新安装或新设备通过已有导出恢复，本机原保险库不能自动重建。没有第三方密码学审计，不作绝对防破解承诺。

## 使用入口

设置 → 安全 → 文件加密：建立或解锁保险库、恢复密文文件、修改口令、验证完整性、锁定。作品详情菜单 → 加密文件：迁移正文、封面、章节、批注及阅读状态。作品加密后，详情菜单改为加密导出；文件扩展名为 `.astervault`，导出口令独立于本机保险库口令。

成功加密后清空搜索记录；保险库打开期间不保存搜索词与敏感导航。外部原文件仍存在，旧备份、原文件及设备外副本不由本次加密撤销。

## 设备核验

2026-11-07，模拟器 `127.0.0.1:7555`，API35。最终 debug APK 已覆盖安装，未执行清除数据。独立 instrumentation 使用随机目录和专用 Keystore 别名，不读取用户保险库或数据库。

`build/vault-device-acceptance.log`：`OK (1 test)`，8.66 秒。覆盖真实 Android Keystore、原生目录 fsync、ZIP/TAR/7Z/RAR 随机解密读取、PDF 代理描述符与实际渲染、独立导出恢复、锁定撤销、错误口令限速，以及测试目录未出现明文标题。正文不落明文缓存的路径由实现检查与单测共同覆盖。

界面证据位于 `app/build/ui-review/file-vault-create.png`、`file-vault-create-api31.png`、`file-vault-dark-font-200.png`、`file-vault-dark-font-200-api31.png`。API31/35 的 Robolectric 界面回归与设备实测分别记录；API31 和 API37 尚未完成真实设备手工验收，API37 已编译。TalkBack、真实文档 Provider 中断/断电、磁盘写满和 Keystore 失效仍需专项设备验收，不能用现有通过结果替代。

本次没有数据库 schema 版本升级。加密作品通过事务清除普通记录、章节、批注和阅读状态，再写去敏占位；执行 WAL checkpoint 和 VACUUM。密文元数据先提交、数据库去敏未完成时，后续解锁会重放去敏。保留孤立密文用于故障恢复，不作自动清理或物理擦除承诺。

## 最终回归与产物

`build/vault-acceptance.log`：`BUILD SUCCESSFUL`，3 分 22 秒。执行 `:app:testDebugUnitTest :core:database:testDebugUnitTest :core:vault:testDebugUnitTest :app:assembleDebugAndroidTest :app:assembleDebug :app:lintDebug --offline`。app 179 项、database 10 项、vault 7 项，共 196 项通过，0 失败、0 跳过。Lint 0 错误、52 警告，警告未屏蔽。

加密核心 7 项覆盖 NIST AES-GCM 向量、空文件、跨块读取、撤销、错误密钥、头篡改、尾随数据、尾块截断、换块、取消、源长度变化、弱口令及恶意 KDF 参数。保险库与仓库 7 项覆盖跨独立设备密钥的导出恢复、篡改不提交、取消保留来源、持久限速、口令轮换、首次建立后继续加密，以及普通数据库去敏与私密阅读状态持久化。API31/35 设置回归覆盖创建确认、恢复条件确认、深色及双倍字号；其余既有 UI/扫描/阅读回归保持通过。

debug APK：`app/build/outputs/apk/debug/app-debug.apk`，23,730,960 字节。SHA-256：`74770190F89860BD770131B620A87448483FF379F6AA7093FEB3F5A92078A29B`。已通过 `adb install -r` 安装至上述模拟器，并启动 `com.asterreader.app/com.example.MainActivity`。用户数据未重置。
