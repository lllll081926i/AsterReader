# Aster Reader

Aster Reader 是一款专为 Android 平台打造的本地现代化漫画与电子书阅读器。基于 Android 原生纯 Kotlin 与 Jetpack Compose 全面构建，深度结合 Material 3 Expressive 动效、系统原生 Monet 动态取色、本地硬件级隐私保险库与自适应多端流体布局，提供通透、沉浸、纯粹的阅读体验。

---

## 核心特性

### 1. 沉浸式漫画阅读体验
- **无缝条漫排版**：纵向滚动零缝隙贴合，支持超长条漫与高清大图分块渲染，杜绝边缘拉伸与白边。
- **触点中心缩放与手势体系**：支持双击以触点为中心智能放大、多指自由捏合平移，并在屏幕左右边缘留出安全冗余，完美避让 Android 系统返回手势。
- **双页对开与连贯续读**：在宽屏或平板模式下自动支持双页跨页对开；自动识别漫画系列与分卷卷序（如 `Vol.01`、`卷11` 等），读完当卷自动顺畅衔接下一卷。
- **沉浸式真全屏**：进入阅读器时自动隐藏系统状态栏与导航栏，右下角提供精致居中的微型页码胶囊，退出时平滑恢复。

### 2. 全格式漫画与归档支持
- 支持所有主流漫画包与电子书容器格式：
  - **漫画归档**：`CBZ`, `ZIP`, `CBR`, `RAR`, `CB7`, `7Z`, `CBT`, `TAR`
  - **文档图书**：`PDF`, `EPUB`, `MOBI`
- 导入或挂载目录后，系统自动从漫画首张有效页面生成本地高清缩略图缓存，滑动书库流畅无白块。

### 3. 本地存储与 SAF 文件树管理
- **系统原生目录选取**：通过 Android Storage Access Framework (SAF) 直接调用系统文件管理器选取书库目录，杜绝繁琐的手动输入路径。
- **递归智能扫描**：自动遍历深层子文件夹树，智能识别包含独立话次的子目录与分卷。
- **一致性保护**：当存储介质挂载点变化时，完整保留书签、批注与阅读历史，提供一键重新定位与关联来源能力。

### 4. 纯净极简界面与 Material 3 动效
- **极致去冗余**：彻底移除多余的说教式说明、无意义的二次确认与大块空白占位；没有简介、书签时自动收敛，保持界面干净清爽。
- **紧凑断点续读行**：在主书库顶部以一小行紧凑胶囊条呈现最近阅读进度，点击一秒续读。
- **流体转场曲线**：采用 Material 3 Expressive 柔顺缓动与轻量级层级位移，底栏 Tab 切换方向自适应滑动，告别生硬卡顿。
- **多端与横竖屏严格适配**：仅在横屏展开态且屏幕宽大于高时激活平板双栏检视器，杜绝竖屏大手机或折叠屏竖屏误触分栏。

### 5. 独立应用锁与后台隐私防窥
- **自建应用级 PIN 码**：支持设置 4~8 位独立应用 PIN 码，无需强制依赖系统锁屏密码。
- **生物识别协同**：提供一键调用系统生物凭据（指纹/人脸识别）快速解锁通道。
- **后台自动模糊与防截屏**：支持 `FLAG_SECURE` 防截屏录屏；切入多任务后台时自动阻断 Recent 任务快照泄露，全方位保护个人藏书隐私。

---

## 架构设计

Aster Reader 采用现代 Android 模块化解耦架构，严格遵循单向数据流（UDF）与分层隔离原则：

```text
AsterReader/
├── core/
│   ├── model/           # 核心数据模型、统一 Locator、阅读视口 ReaderViewport 与格式定义
│   ├── engine-api/      # PublicationEngine、ReaderSession、DocumentSource 契约
│   ├── security-api/    # VaultManager、AppLockManager、ProtectionPolicy 契约
│   ├── database/        # Room 独立持久化层：AsterDatabase、DAO、实体与无损迁移
│   └── designsystem/    # AsterTheme、Monet 动态取色、AsterMotion 动效系统与字体资产
└── app/                 # 业务装配层：MainActivity、ViewModel、各业务 Screen 与渲染管线
```

---

## 技术栈与平台规范

- **开发语言**：100% Kotlin
- **UI 框架**：Jetpack Compose + Material 3 (Monet Dynamic Color 默认开启)
- **目标 SDK**：`minSdk = 31` (Android 12+), `compileSdk = 37`, `targetSdk = 37`
- **本地数据库**：Room 2.6.1 + KSP
- **图片与异步渲染**：Coil 2.7.0 + Coroutines + Flow
- **构建工具**：Gradle 9.3.1 + Android Gradle Plugin 9.1.1 + JDK 17

---

## 快速构建与运行

### 1. 环境准备
- Android SDK 包含 `platforms;android-37.0` 与 `build-tools;37.0.0`
- JDK 17 (Temurin 或 OpenJDK)

### 2. 编译与测试
```bash
# 执行全量单元测试与特征回归套件
./gradlew testDebugUnitTest

# 编译生成可直接安装的 Debug APK
./gradlew assembleDebug
```
产物路径：`app/build/outputs/apk/debug/app-debug.apk`

### 3. 推送至设备或模拟器安装
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## 开源协议

本项目采用 [MIT License](LICENSE) 授权许可。

## 发布构建

release 使用 `signingConfigs.release`：默认从环境变量读取密钥，**本地开发用的
`my-upload-key.jks` 已生成并加入 `.gitignore`，正式发布请务必换成自己的上传密钥**。

```bash
KEYSTORE_PATH=/path/to/upload.jks STORE_PASSWORD=xxx KEY_PASSWORD=xxx \
  ./gradlew :app:assembleRelease
```

`versionCode` 每次发版必须递增；`proguard-rules.pro` 里的 Room / 归档库 keep 规则
仅在 release 生效，改动依赖后请重新跑一次 `assembleRelease` + 冒烟测试。

## 数据备份

应用关闭了系统备份（`allowBackup=false`）以避免书库与 PIN 进入云端。
换机或重装请在「设置 → 本地书库」使用 **导出书库清单 / 恢复书库清单**：
导出的 JSON 只含元数据（进度、标签、书架、批注、阅读状态），不含漫画文件与 PIN；
恢复后旧设备的 `content://` 授权失效，作品会标记为 MISSING，用详情页的
「重新定位来源」重新选择文件即可继续阅读。
