# 大图集连续滚动与导航岛间距

## 实施

阅读会话缓存以最近前台取页位置为中心。后台预加载只淘汰更远的缓存页；空间不足时停止本轮预加载，避免远页挤出当前页与近页。维持现有位图预算、解码并发与上下各0～16页的配置上限。预加载不是承诺将16张大图同时驻留内存。

解码线程在位图交给界面前调用 Bitmap.prepareToDraw，以便系统提前准备绘制资源。该调用不能保证所有设备都消除首次绘制开销。

图片与PDF的原始尺寸保留在当前阅读会话中。位图缓存淘汰后，阅读画布通过 ViewModel 与 Repository 的授权检查查询内存尺寸，继续使用真实比例测量页面；不读取文件或数据库。来源切换、会话关闭后不再返回旧尺寸。已加载页面不再启动延迟加载指示器的计时任务。

底部导航岛保留内容自适应与居中。标签最小宽度56dp，标签间距12dp，岛内水平边距12dp；尺寸统一放在 core:designsystem。四标签常规宽度约284dp，隐藏统计标签后继续缩窄。标签仍保留选中语义、点击操作和大字体换行。

## 核验

新增缓存回归使用有限容量与一万页目录，检查远页预加载不会淘汰当前页和近页、填满后停止读取、移动到新位置后可替换远页。尺寸回归检查位图淘汰后尺寸仍可用，来源切换和关闭后旧尺寸不可用。导航回归检查岛体宽度、按钮间距与窄屏两倍字体。

最终执行 `gradlew.bat testDebugUnitTest :app:lintRelease :app:assembleDebug :app:compileDebugAndroidTestKotlin --console=plain` 成功。全量379项测试通过，0失败、0错误、0跳过。Release Lint为56条警告、0错误。日志为 build/reader-cache-dock-verification.log。首次定向测试使用旧PNG常量时遇到原生解码器CRC校验失败；新增缓存测试改为实际生成有效PNG后通过，未修改生产解码器的校验逻辑。

已查看 app/build/ui-review/floating-island-content-width.png、floating-island-font-200.png 和 floating-island-font-200-api31.png：内容宽度随标签数量收缩；320dp窄屏两倍字体下四个标签仍可点击，文字换行，无越界。自动化使用Robolectric API31/35。新增页面高度回归在第一张位图尚未返回时，通过第二页的位置验证第一张仍按已知4:1比例测量。

## 边界

修改模块为 app 与 core:designsystem。不改数据库、加密格式、依赖或版本号，不发布。尺寸信息仅在授权会话内使用。API31/API37 真机滚动帧耗时、TalkBack 与系统Insets仍待设备验收。不能用自动化测试或Debug构建宣称实际帧率提升。
