# 连续滚动卡顿与缓存页面空白过渡修复

## 问题与改动

用户反馈上下连续滚动卡顿，偶尔出现此前没有的短暂黑屏加载。本轮检查发现：

- PublicationPageCanvas 在 LaunchedEffect 的组合参数中读取 list.layoutInfo.totalItemsCount。布局状态随滚动更新，组合范围会因此失效，即使总条目数没有变化。
- 已预加载的正常清晰度图片仍经过异步 Repository 请求。LazyColumn 重新创建页面时先显示加载态，再显示缓存图片；短图还先使用 320dp 最小高度，随后改为真实高度。
- 页内滚动每次都调用 onPageChanged，重复发送相同页码通知。
- 上一轮加入的封面来源签名计算在主线程排序、整理作品列表。阅读进度写库也会触发作品列表更新。

阅读位置恢复现改用 snapshotFlow 等待目标页实际尺寸，不在组合阶段观察列表布局。用户已滚动或离开目标页时，放弃旧恢复任务。页码通知仅在页码变化时发送，页内视口仍持续更新并沿用原节流持久化逻辑。

DocumentRenderSession 增加只读内存缓存查询。Repository 按来源版本限定缓存；ViewModel 保留应用锁、隐藏内容与保险库授权检查；Composable 只使用 ViewModel 提供的内存回调，不访问 DAO、归档、文件或解密接口。DocumentPage 首次组合即可取得预加载图片并按真实比例测量，命中时跳过异步请求；未命中时继续使用原异步解码和错误重试。缓存清理、来源切换或会话关闭后不再返回旧缓存。

封面来源签名流移至 Dispatchers.Default。维持现有正常清晰度、0～16 页预加载设置、内存预算和解码并发，不增加第二份位图缓存，不生成低清预览。

## 验证范围

新增回归覆盖：

- 预加载后同步获得同一位图，不重复打开来源；不同来源、清晰度不足、内存清理或会话关闭时返回空值。
- 已缓存短图在异步加载函数永不返回时仍能显示，使用真实高度，异步加载调用数为 0。
- 同页滚动继续更新视口，但重复页码通知数为 0。
- 已缓存长页仍正确恢复 25% 页内滚动位置。

定向测试与 Debug 构建通过，日志为 build/reader-scroll-tests.log。最终全量验证记录见下方。

全量 373 项测试通过，0 失败、0 错误、0 跳过。Release Lint 为 56 条警告、0 错误。Debug APK 构建、设备测试代码编译通过。命令为 `gradlew.bat testDebugUnitTest :app:lintRelease :app:assembleDebug :app:compileDebugAndroidTestKotlin --console=plain`，日志为 build/reader-scroll-verification.log。

缓存短图回归启用 Native Graphics，并输出 app/build/ui-review/cached-short-page-api31.png、cached-short-page-api35.png。已查看两张截图：绿色测试位图按 4:1 比例绘制，无 320dp 最小高度或加载指示器。这是自动化图形验证，不是真机帧率测量。补充图形回归日志为 build/reader-scroll-visual-tests.log。

## 影响与边界

生产修改仅涉及 app 模块。无依赖升级、数据库 schema、数据迁移或加密格式变化，不修改版本号或发布。

测试使用 Robolectric API31/35；API31/API37 真机滚动、TalkBack、Insets 和实际帧耗时仍待设备验收。内存未命中的页面仍需真实读取与解码，不能据此宣称所有文件零等待。本轮没有连接手机、启动模拟器或自动启动应用。
