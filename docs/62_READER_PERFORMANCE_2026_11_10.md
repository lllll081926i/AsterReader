# 封面刷新、阅读解码与导航岛优化

## 根因与实现

扫描遵循“只登记来源”的约定，但缺少独立封面队列。此前阅读首页还同步执行封面缩放、JPEG 压缩和写库；生成封面仅 300×450，重建时复用路径也可能命中旧图片缓存。

app 的 AsterViewModel 现在订阅作品来源变化，串行补全公开本地作品封面。扫描、详情预读、阅读和应用锁定时暂停派发。清理缓存后主动触发补全。封面更新通过 Room Flow 回到书库，不需要打开详情；普通封面最高 1080×1620，JPEG 质量 92，不放大低分辨率原图。历史低清封面自动重建一次，使用新缓存路径使 Coil 重新加载。扫描仍不生成封面或解码阅读页。

LibraryRepository 从阅读请求移除封面压缩和写库。生成 ZIP/CBZ 封面优先使用可寻址来源读取条目，提供者不支持随机读取时保留兼容路径。封面解析尺寸和像素尽量复用同一条目流。后台等待结束后复核来源、隐藏状态和封面选择；core:database 使用事务比对来源及当前封面，拒绝过期任务覆盖新选择。

DocumentRenderSession 原先为每个放大区域重新解压图片、读入 Java 字节数组并创建解码器。现在同一页不同区域复用一个 BitmapRegionDecoder，换页或关闭会话时释放。取消普通图片 32MiB 和区域图像 48MiB 的编码大小限制，保留像素预算、有限解码并发和 LRU 内存预算。预取取得解码槽后再次检查前台请求，避免排队预取优先于新出现的可见页。

app 与 core:designsystem 的悬浮导航岛改为按标签内容测量，最大宽度 480dp，单项至少 48dp。隐藏统计标签后岛体自动缩窄。窄窗口约束总宽度，大字体允许标签换行。

## 回归覆盖

- 入库后不打开详情或阅读，ViewModel 自动生成封面，作品仍未建立阅读索引。
- 高清源生成 1080×1620 封面；重复补全跳过有效封面；主动重建使用新路径。
- 阅读渲染不再同步生成封面，随后独立补全成功。
- 用户换封面或隐藏作品后，旧后台结果不能写入；等待期间隐藏作品不再打开来源。
- 同页两个不同区域只打开一次图片流；重复区域命中缓存；会话关闭后重新打开来源。
- 三标签岛体小于四标签岛体，保持居中和最小触控尺寸；320dp 窄屏、200% 字体仍可切换标签。

Robolectric 的 ShadowBitmapRegionDecoder 只创建指定尺寸的空位图，不能验证区域像素。区域像素与输入流关闭后的真实解码验证已加入 LocalDirectReadDeviceTest，设备测试尚未执行。不能把本地流打开次数断言当作真机加载耗时。

截图已检查：app/build/ui-review/library-empty.png、floating-island-content-width.png、floating-island-font-200.png。全量回归也生成对应 API31 截图。这些是 Robolectric Native Graphics 截图。

最终验证通过：365 项测试，0 失败、0 错误、0 跳过。覆盖 Robolectric API31/35。Release Lint 为 56 条警告、0 错误，警告包含 KTX 写法建议；未关闭规则。Debug APK 构建和设备测试代码编译通过。

验证命令：`gradlew.bat testDebugUnitTest :app:lintRelease :app:assembleDebug :app:compileDebugAndroidTestKotlin --console=plain`。日志：build/reader-performance-verification.log；定向测试日志：build/reader-performance-tests.log。安装包：app/build/outputs/apk/debug/app-debug.apk。本轮未构建或测量 Release 安装包，不能用 Debug 包判断正式包性能。

## 影响与待验收范围

修改 app、core:database、core:designsystem。无数据库 schema、迁移、依赖、加密格式或密钥变更。后台队列跳过隐藏、加密、失联和远程来源，不创建加密内容的公开封面。

没有连接手机，没有启动模拟器或自动启动应用。API31/API37 真机、TalkBack、实际系统 Insets，以及用户原始大 ZIP/PDF 的 Release 耗时和峰值内存仍待设备验证。已开始的原生解码不能立即抢占；后台只在每本之间暂停。新封面使用不同路径，旧缓存由清理缓存操作或系统缓存策略回收。

本轮不修改正式版本号、tag 或 Release。
