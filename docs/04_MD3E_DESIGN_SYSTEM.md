# 04 Material 3 Expressive 设计系统

> **规范真源已迁移至根目录 `design.md`。** 任何 UI、颜色、形状、动效、Reader chrome、平板适配都以 `design.md` 为准；本文件保留为工程索引与实现约束。

## 核心结论

- Android 12+：全局默认 Monet / Dynamic Color。
- 项目最低版本即 Android 12；Aster fallback scheme 仅作异常兜底和测试，不用于旧系统兼容。
- UI Theme 与 Reader content theme 分离。
- 优先 Material 3 Expressive。
- 最新 Motion 使用 `MaterialTheme.motionScheme` + `MotionScheme.expressive()`；如 alpha API 不稳定，设计系统层统一提供 fallback，业务页面不得直接感知。
- 页面默认 edge-to-edge。
- 手机/平板/折叠屏统一走 Material3 Adaptive。
- Shared Element、Predictive Back、SearchBar、BottomSheet 动效遵循 `21_MOTION_ANIMATION_SPEC.md`。

## 代码位置

```text
:core:designsystem
  AsterTheme.kt
  Color.kt
  Typography.kt
  Shapes.kt
  Dimens.kt
  Motion.kt
  Haptics.kt
  Icons.kt
  components/
```

## 强制规则

1. feature 模块禁止直接硬编码品牌主色。
2. feature 模块禁止散落自定义动画 duration/easing。
3. 受保护内容认证前禁止绘制真实封面/标题。
4. 所有触控目标 >= 48dp。
5. Reader 可以覆盖书页颜色，但工具栏/控制岛仍使用 Aster Theme。
6. 任何新组件先确认 M3/M3E 是否已有官方实现。

## 必读

- `../design.md`
- `21_MOTION_ANIMATION_SPEC.md`
- `20_ANDROID_ENGINEERING_STANDARD.md`

## 官方参考

- https://developer.android.com/develop/ui/compose/designsystems/material3
- https://developer.android.com/jetpack/androidx/releases/compose-material3
- https://developer.android.com/reference/kotlin/androidx/compose/material3/MotionScheme
