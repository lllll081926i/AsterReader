# 26 新手开发路线：从 0 到可用 APK

## 第 0 天：只准备环境

安装：

- Android Studio
- Android 17 SDK / Build Tools 37
- Git
- JDK 使用 Android Studio 推荐版本

创建 Pixel Emulator + 至少一台真实 Android 12+ 手机测试 Dynamic Color。

不要先下载十几个第三方库。

---

## 第 1 阶段：只做 UI 骨架

目标：App 能开、能导航、能看到截图风格。

完成：

- `AsterTheme`
- Monet Dynamic Color
- `design.md` tokens
- 首页
- 书架
- 设置
- 搜索
- 详情 mock
- Reader mock
- Adaptive navigation
- Predictive back

所有页面先用假数据。

---

## 第 2 阶段：数据层

完成：

- Room
- DataStore
- Source/Publication model
- local SAF repository
- scanner

不要同时做 EPUB parser。

---

## 第 3 阶段：安全层

完成：

- App Lock
- BiometricPrompt
- SecurityBootstrap
- Keystore
- Vault single-file encryption
- Protected metadata placeholder
- Recents/screenshot protection

用测试文件验证“磁盘上看不到明文”。

---

## 第 4 阶段：先把漫画做完整

原因：图片目录 + CBZ 比 EPUB 分页/排版简单，可最快得到真正可用 App。

完成：

- Image directory
- CBZ
- Page index
- Comic Reader
- Zoom
- Horizontal/vertical
- Progress/bookmark
- encrypted comic read path

---

## 第 5 阶段：电子书

顺序：

1. TXT/MD
2. EPUB
3. FB2
4. MOBI/KF8
5. PDF

每加格式只实现 `PublicationEngine`，不要修改 Library UI 业务逻辑。

---

## 第 6 阶段：性能

不要等“全部功能做完”才测试。

准备 fixture：

- 1000 books
- 10000 metadata records
- 50MP image
- 2GB comic archive
- long EPUB
- large PDF

跑 Macrobenchmark。

---

## 第 7 阶段：发布前

- Room migration
- backup/restore
- vault recovery
- forgotten password paths
- TalkBack
- font scaling
- tablet/foldable
- animation scale 0
- airplane mode
- permission revoked
- app killed during scan/encrypt

---

## 每天的开发节奏

建议每个任务按：

```text
需求 -> interface -> unit test -> implementation -> UI -> instrumentation -> docs
```

而不是：

```text
先堆 UI -> 到处直接调用文件/数据库 -> 后面再重构
```

---

## 遇到不会的 API

优先级：

1. Android Developers 官方文档
2. AndroidX release notes / API reference
3. 官方 sample
4. 库的 GitHub README / examples
5. 社区文章

不要用 5 年前的博客指导 Android 17 项目。
