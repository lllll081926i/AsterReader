# 全局设置与阅读设置 MD3 重做

## 范围与实现

按用户本轮确认重做全局设置 T-01 和阅读设置 R-04。取消原有 1.2 倍密度覆盖，直接沿用系统密度与字体缩放；既有嵌套设置入口不再重复改变密度。

core:designsystem 新增 AsterPreferences，集中提供设置标题栏、分组、列表项、开关行与滑杆。使用 Material3 TopAppBar、ListItem、Switch、Slider 和主题令牌。实验标题栏 API 仅在设计系统内封装。单行至少 56dp，图标 24dp，分组间距 24dp，宽窗口内容最大宽度 640dp。分组使用 surfaceContainerLow，无额外混色和投影。

全局设置保留常用、内容、隐私、数据四组和所有原入口。二级设置沿用同一组列表组件。输入法和导航栏避让置于滚动容器外侧。

阅读设置保留独立全屏页。顶部使用“本书 / 默认”页签和恢复默认按钮；默认作用范围禁用本书恢复按钮。翻页和缩放使用双行列表项显示当前值，打开带图标、单选语义的 Material3 对话框。上下预加载独立支持 0～16 页，Slider 提供操作名称和当前值语义。开关整行响应，子 Switch 不重复提交事件。所有保存范围和单本覆盖保持原行为。

截图复核发现不依赖 Surface 的深色宿主可能让恢复图标和滑杆标签使用黑色，已改为显式 onSurface/onSurfaceVariant 主题角色。

## 验证与证据

首轮设置页面定向测试及 Debug APK 构建通过。最终全量 354 项测试通过，0 失败、0 错误；Release Lint 为 55 条警告、0 错误，Debug APK 构建通过。设置回归覆盖 Robolectric API 31/35；新增测试确认嵌套设置不改变系统密度、200% 字体保留、深色页面可达和默认作用范围禁用本书恢复。

已人工查看自动化截图：浅色全局设置、完整阅读设置页、深色阅读设置、深色 200% 字体设置首页、横屏与窄屏大字体页面，未见控件挤压或裁切。窄屏单选对话框的选项可达性由自动测试验证。

日志：build/md3-settings-tests.log、build/md3-settings-verification.log。

Robolectric Native Graphics 截图位于 app/build/ui-review：

- settings-home-light.png：全局设置浅色。
- md3-settings-dark-font-200-top.png、md3-settings-dark-font-200-bottom.png：全局设置深色与 200% 字体。
- reader-settings-monet.png：完整阅读设置页。
- md3-reader-settings-dark.png：深色阅读设置。
- reading-landscape-font-200.png：横屏阅读设置与 200% 字体。
- reader-picker-narrow-font-200.png：320dp 窄屏、大字体完成选页方式后的页面。

API 31 截图使用 -api31 后缀。截图来自自动化环境，不作为真机截图。

## 影响与边界

修改 app 与 core:designsystem。没有依赖升级、数据库 schema 或数据迁移，没有加密格式和密钥变更。受保护页面仍沿用原授权门禁；相关锁定控件自动回归覆盖。未更改正式版本、tag 或发布附件。

API 31/API 37 真机视觉、实际系统 Insets、TalkBack 和全屏连续返回仍待人工验收。本轮未启动模拟器或手机应用。
