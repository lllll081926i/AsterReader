# 阅读设置单行布局

日期：2026-11-07。对应 R-04。按用户要求将翻页选择改为“滚动 / RTL / LTR / 双页”，四项等宽、同一行，保留图标与 Monet 选中态。

## 修改范围

- `:core:designsystem`：新增可选择的单行选择布局与紧凑尺寸 token。正常宽度均分可用宽度；大字体、窄窗口保留最小可读宽度并横向滚动。每项触控高度至少 48dp。其他设置选择保留原布局。
- `:app`：阅读模式只缩短显示名称，原有模式标识及保存逻辑不变。RTL/LTR 保留完整方向的无障碍描述。“恢复默认”移至作用范围右侧，使用可访问的小图标按钮；修改默认时禁用恢复本书。组内间距从 8dp 改为 4dp；亮度标签、滑杆、百分比合并为一行。
- 保留独立全屏阅读设置、系统 Insets、滚动内容、返回阅读及本书/默认保存范围。没有新增动画或改变 Reduced Motion。无数据库、密文格式、权限或安全状态变更。

## 核验方式

复用 `SettingsConsistencyTest` 与 API31 子类的布局交互测试，检查四项等宽同高、48dp 触控高度、完整方向语义、默认值不覆盖本书、返回阅读，以及深色横屏和窄屏 200% 字体下选项可达性。截图由测试中的隔离样本生成，不关闭用户应用的防截屏保护。

普通未加密作品的设置交互由真实 ReaderScreen 测试覆盖。空库/失败/锁定的既有页面回归由同一测试类覆盖；本次不改变作品加载或认证逻辑。API31/API35 自动化截图不替代 API31/API37 真机 TalkBack 与完整状态手工验收。

## 验收证据

- API31 与 API35 分别运行 `SettingsApi31ConsistencyTest`、`SettingsConsistencyTest`；每类 32 个测试，共 64 项。最终构建记录为 `build/reader-compact-row-verified.log`。
- 正常阅读设置截图：`app/build/ui-review/reader-settings-monet.png`、`reader-settings-monet-api31.png`。已检查四项单行、等宽、48dp 高度、方向语义和返回阅读。
- 深色横屏与 200% 字体：`app/build/ui-review/reading-landscape-font-200.png` 及 API31 同名后缀版本。
- 窄屏 320dp 与 200% 字体：`app/build/ui-review/reader-single-row-narrow-font-200.png` 及 API31 版本。末项可横向滚动到达并选中，选项不换行。
- 本轮编译、debug 打包、Lint 检查与覆盖安装；设备为 API35 模拟器，保留原有数据。API37 仅完成编译，API31/API37 未完成设备手工验收。
- 不涉及数据库迁移、文件加密或认证逻辑，不更改用户阅读模式的持久化标识。
