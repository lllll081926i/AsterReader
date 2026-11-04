# 21 Motion & Animation 统一规范

## 1. 目标

Aster Reader 的动效要做到：

- 看起来是 Android 2026 年的原生 App，而不是静态卡片堆叠。
- 有空间连续性，但不喧宾夺主。
- Reader 操作快速，不因动画拖慢翻页。
- 所有动画可中断、可预测、可降级。
- 跟随 Material 3 Expressive，而不是每个功能自行设计时长/曲线。

---

## 2. 技术基线

优先：

- Material3 `MotionScheme.expressive()`
- Compose Animation
- `SharedTransitionLayout`
- `sharedElement()` / `sharedBounds()`
- Navigation 3 transitions
- Predictive Back
- Material3 SearchBar / ModalBottomSheet predictive animations
- Material3 Adaptive AnimatedPane / pane navigation

截至 2026-09-23，MotionScheme 在 Material3 1.5.0-alpha29 中提供。使用前必须固定版本，并把 API 升级作为独立 PR。

---

## 3. Motion Token

业务层不直接拿 duration。

```text
AsterMotion.effectsFast
AsterMotion.effectsDefault
AsterMotion.effectsSlow
AsterMotion.spatialFast
AsterMotion.spatialDefault
AsterMotion.spatialSlow
```

在 M3 1.5+ 时直接映射 `MaterialTheme.motionScheme`。

---

## 4. 页面级动效

### 启动

System Splash -> AuthGate/Main：

- 只做短 fade / content reveal。
- 不做 logo 长动画。
- 若需要应用锁，主界面数据绝不在锁页下方预绘制。

### Library -> Detail

共享：

- 封面 `sharedElement`
- 封面容器 `sharedBounds`

可选：标题轻微淡入，不推荐同时把所有 metadata 做共享元素。

### Detail -> Reader

Reader 是沉浸模式，因此不强制封面飞入正文。

推荐：

- Detail chrome fade/scale out。
- Reader content 在目标 background 上快速出现。
- Bottom control island 之后进入。

### Search

使用官方 expanded SearchBar；返回手势可跟随 predictive back 收拢。

### Settings

列表导航采用标准 spatial transition；Switch 使用组件内建 motion。

### Vault

加密进度：

- 行项目 height 变化 `animateContentSize`
- progress 变化平滑
- 完成态 icon/tonal color 变化
- 失败态不 shake 整屏，可使用局部 haptic + error container

---

## 5. Reader Motion

### UI overlay

单击中央显示/隐藏：

- Top bar fade + small slide
- Bottom island fade + small slide
- scrim 非必要，漫画模式可轻 tonal scrim

### 漫画翻页

提供：

- Instant
- Slide
- Page-like（后期）

默认 Slide 追求响应，手指 drag 与页面 position 1:1 跟随，松手使用 spring settle。

### Webtoon/Continuous

滚动本身就是 motion，不额外加 item enter animation。

## 6. Predictive Back 规则

- 所有能返回上一级的页面都要允许系统 back gesture 驱动动画。
- 不要用全屏水平拖动手势抢占最左/最右系统边缘。
- 漫画 RTL/LTR 手势区要预留系统返回 edge slop。
- Drawer/Sheet 使用官方 predictive back 支持。

---

## 7. Shared Element 限制

Compose shared element 不是万能：

- ContentScale 变化不会自动平滑处理所有情况。
- shape clipping 需要 `sharedBounds` + 自定义 enter/exit。
- Compose 与 `AndroidView` 混合有互操作限制。

因此 Reader 如果底层引擎包含 AndroidView，不强求共享元素穿透 Reader。

---

## 8. 性能规范

- 动画尽量改变 `graphicsLayer` 属性而非触发重测量。
- 大图片共享元素使用已经缓存的 thumbnail，不在过渡时解码原图。
- Lazy grid 重排不做每项长延迟。
- Reader 翻页不得因 toolbar 动画丢帧。
- 120Hz 设备 benchmark。

---

## 9. Reduced Motion

设置：跟随系统 / 减少 / 完整。

减少模式：

- shared transitions -> fade/short spatial
- spring overshoot -> critically damped / snap-like
- indefinite shimmer -> 禁用
- decorative parallax -> 禁用

测试 motion duration scale = 0，确保 UI 最终状态正确。

---

## 10. 动效验收

- [ ] 所有页面过渡可中断。
- [ ] back gesture 取消时能回到原状态。
- [ ] 120Hz 快速滚动无显著卡顿。
- [ ] animation scale 0 不造成逻辑死锁。
- [ ] 认证前无敏感 shared content。
- [ ] 大字号/屏幕旋转时动画状态不崩。

---

## 官方参考

- MotionScheme: https://developer.android.com/reference/kotlin/androidx/compose/material3/MotionScheme
- Compose animation: https://developer.android.com/develop/ui/compose/animation/quick-guide
- Shared elements: https://developer.android.com/develop/ui/compose/animation/shared-elements
- Predictive Back: https://developer.android.com/develop/ui/compose/system/predictive-back-setup
