# 跨页时复用在途预加载

## 问题

原实现每次当前页变化都取消整个预加载任务。对于较大的图片，预加载第 2 页期间从第 1 页滚到第 2 页，会取消该页已经完成的一部分读取与解码；前台随后重新打开同一个条目。提前读取无法交接，反而产生重复工作。

## 实施

app 新增 ReaderPrefetchQueue 与 ReaderPrefetchPlan。队列在主线程接收作品、当前页和邻页窗口；计划以同步方法向解码线程提供最新窗口。计划不持有位图或文件资源。

- 在途页仍属于新窗口或已成为当前页时，继续当前解码。前台复用 DocumentRenderSession 现有同页门锁与位图缓存。
- 每页完成后从最新窗口选择下一页，不再继续旧窗口的远页。
- 跳到远处或换书时取消过期任务。退出阅读仍通过原资源释放流程取消队列，并等待会话租约释放。
- 批次结束时检查窗口修订，避免最后一次窗口更新恰好发生在返回边界而未处理。
- 每批次保留原有四分之三缓存预算、近页优先准入和前台请求优先规则。未修改解码清晰度、内存上限或上下各 0～16 页设置。

修改范围为 app 的阅读页调用、ViewModel、Repository 与解码会话。未修改视觉组件、工具栏交互、手势或导航动画。

## 验证

新增队列回归验证：跨到在途页不取消、按最新窗口重排、远距离跳页取消、换书取消、显式释放取消，以及批次返回边界不丢更新。

新增 Native Graphics 集成回归：暂停第 2 页条目打开，模拟滚到第 2 页，并将邻页窗口由 [2,3,4] 改为 [5,3]。前台与预加载共享第 2 页位图，实际打开顺序为 [2,5,3]，第 2 页只打开一次，第 4 页未打开。该回归在 Robolectric API31/35 执行。

定向日志为 build/reader-prefetch-targeted.log。完整验证日志为 build/reader-prefetch-verification.log。阅读画布与缩放回归继续执行，检查缓存首帧、页内恢复、直接跳页、中心点击和缩放。

完整执行 `gradlew.bat testDebugUnitTest :app:lintRelease :app:assembleDebug :app:compileDebugAndroidTestKotlin --console=plain` 成功。395 项测试通过，0 失败、0 错误、0 跳过；Release Lint 为 56 条警告、0 错误。Debug APK 为 app/build/outputs/apk/debug/app-debug.apk。

已检查本次自动化生成的 app/build/ui-review/cached-short-page-api31.png 与 cached-short-page-api35.png：测试短图均按真实比例显示于顶部，无加载占位高度。该截图使用纯色测试图片，仅证明缓存首帧测量结果，不代替实际漫画的视觉和流畅度验收。

## 影响与限制

无数据库迁移、加密格式、扫描流程、依赖和版本号变化。Repository 继续执行原有来源准备与保险库授权；本轮不发布。

API31/API37 真机尚未验收。自动化证明减少重复打开与过期读取，不能证明实际帧率或所有格式均无卡顿。原生解码已开始的计算仍只能在输入读取边界或返回后响应取消。
