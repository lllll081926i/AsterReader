# 阅读解码门锁按页隔离

## 根因与实施

DocumentRenderSession 原先使用 32 把按来源指纹和页码取模分配的 Mutex。同一来源中，相差 32 页的请求必然共用一把门锁。后台旧页尚未完成时，远处目标页可能在进入解码并发控制之前就被该门锁阻塞。即使 ZIP 或散图目录支持并发读取、两个解码名额尚有空位，目标页也不能开始。

现改为按来源指纹和完整页码获取门锁。同页的整页、高清和区域请求仍共享门锁，避免相同页面重复解码；不同页面不因取模发生串行等待。门锁表以引用计数记录在途和等待请求，所有请求完成或取消后立即移除该页门锁，避免阅读超长作品后积累每页 Mutex。注册与释放均在短同步段内完成，不在同步段内等待协程门锁或执行文件读取。

全局两个解码名额、PDF/RAR/7z 的来源串行门锁、前台优先策略、内存预算、缓存准入与来源租约均保持原值。不会通过增加并发或降低清晰度掩盖问题。串行来源仍须等待自身读取完成；本改动只移除无关页面共享门锁产生的等待。

## 回归证据

新增万页目录回归，阻塞第 1 页预加载并保持输入未释放，同时请求第 33 页两次。修复前 API31/35 均在等待目标页时超时，日志为 build/reader-page-gate-reproduction.log。修复后 API31/35 均在旧页仍阻塞时完成第 33 页两次请求，两次返回同一位图，实际只打开一次目标页流。测试的超时用于检测阻塞，不作为真机加载时间基准。

另新增同页取消回归：一个请求正在读取时，取消排队的同页请求，再提交另一个同页请求。剩余请求必须复用同一解码，打开流次数仍为一次。保留现有前台优先、同页不同清晰度共享、关闭等待在途解码、跨页预取、取消流关闭与缓存预算回归。

DocumentRenderSessionTest 与 ReaderPrefetchQueueTest 共 42 项定向回归全部通过，日志为 build/reader-page-gate-targeted.log。完整执行 `testDebugUnitTest :app:lintRelease :app:compileDebugAndroidTestKotlin :app:assembleRelease -Paster.splitApks=true --console=plain` 成功：409 项测试全部通过，失败、错误和跳过均为 0；Release Lint 为 56 条警告、0 错误。完整日志为 build/reader-page-gate-final.log。

通用版、arm64-v8a、armeabi-v7a 三个 Release APK 均启用 R8，均通过签名校验，证书 SHA256 与原正式版一致：EB1F0D147AD3CA031A8E0E63245865D4AD4377C16061F3693A4FB45CA02B9B2A。试用包位于 build/reader-performance-20261114-page-gates/。版本保持 1.0.1（code 4）、minSdk 31、targetSdk 37。本轮未发布、未连接手机、未启动应用或模拟器。

## 范围与限制

修改 app 的 DocumentRenderSession 和对应测试，不修改 Screen、导航或手势。无数据库 schema、依赖、版本号或加密格式变化。API31/API37 本轮真机未验收，不以自动化通过宣称用户漫画已达到指定帧率。
