# 大 ZIP 直接读取与设置全局尺寸

日期：2026-11-08。改动模块：`app`、`:core:designsystem`。本文更新 docs/56 中的 ZIP 读取与设置尺寸决策。

## 实现

普通 ZIP/CBZ 的可寻址 SAF 来源使用文件描述符的 FileChannel 直接随机读取，不再依赖 `/proc/self/fd` 的路径检查。原路径检查失败会回退到整包复制，大文件可能长时间停留在加载状态。仅不支持随机读取的提供者保留流式副本路径。

ZIP 使用项目已有的 Commons Compress，不升级依赖。首次索引只读取中央目录，关闭预先逐条访问本地文件头；打开单页时才读取对应本地文件头和图片数据。支持 ZIP64，保留自然排序、中文编码兼容及同会话并发读取。异常与会话关闭均释放通道及 SAF 描述符。加密或不支持的压缩方式由读取层明确报错。

设置、隐私与阅读设置取消局部 1.5 倍密度。文字、图标、控件、间距和容器恢复全局尺寸。系统字体缩放、Insets、滚动及原有交互保持生效。

## 核验

- 全量 `testDebugUnitTest :app:lintDebug :app:assembleDebugAndroidTest` 通过：327 项，0 失败、0 跳过。Lint 0 错误、75 项现有警告。日志：`build/large-zip-final.log`。
- ZIP64 回归构造有效归档头和超过 5 GiB 的稀疏通道偏移，验证中央目录解析与目标图片读取，累计读取少于 256 KiB。该测试不分配 5 GiB 内存，不代表读取了用户原件。
- 回归覆盖 GB18030 文件名、自然排序、Deflate 图片、损坏归档释放通道；API31/API35 Repository 回归检查直接读取零整包副本、页面复用及关闭后重新打开。
- API31/API35 设置回归覆盖全局行高、Monet 浅深色、大字体、窄屏及阅读设置。截图：`docs/evidence/zip-direct-global-settings/`。
- 补齐仅支持流式读取的提供者兼容回退后，再运行 Repository 与 ZIP 专项共 50 项及 Lint，全部通过。日志：`build/large-zip-provider-check.log`。

## 影响与限制

不修改数据库 schema、Vault 格式、用户原件或阅读记录。受保护内容继续走原授权与加密读取链路。

API31 未做手工设备验收；API37 仅覆盖安装检查，不自动启动应用。未启动电脑模拟器。未获取用户实际卡住的 3.6 GB ZIP，尚未确认该原件的真机读取结果。流式提供者无法随机读取时仍需兼容副本。

## 安装

正式签名 Release 构建通过，版本保持 1.0.0 / code 3，不发布。APK：`build/user-fixes-install/AsterReader-1.0.0-fixes.apk`。SHA256：`C334A4947AEEFAE740C69D086AE5EECED3A6BF89D11F6B61193754C2D24F7C23`。

证书 SHA256：`EB1F0D147AD3CA031A8E0E63245865D4AD4377C16061F3693A4FB45CA02B9B2A`。API37 Pixel 6 Pro 无线 ADB 覆盖安装成功，保留数据；安装后检查无应用进程。
