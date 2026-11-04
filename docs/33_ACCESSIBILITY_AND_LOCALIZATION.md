# 33 Accessibility & Localization

## 1. Accessibility 是 P0，不是发布前补丁

必须支持：

- TalkBack。
- system font scaling。
- display size scaling。
- reduced motion。
- high contrast / distinguishable state。
- keyboard/DPAD 基础操作（对平板/ChromeOS 也有价值）。

## 2. Compose semantics

- 纯装饰图标不重复朗读。
- IconButton 必须可理解 label。
- Publication card 合并为合理语义节点，避免封面/标题/作者每个都碎片朗读。
- Slider/reader progress 暴露范围与当前值。
- lock/protection status 有文本语义，不只靠锁图标颜色。

## 3. 字体缩放

至少测试：

- 100%
- 130%
- 200%
- 最大常用 accessibility scale

规则：

- 设置页不得裁切开关 label。
- dialog/bottom sheet 允许滚动。
- 顶栏长标题可以 ellipsize，但必须可在详情看到完整值。
- Reader UI 控件不可因为 font scale 覆盖正文关键交互。

## 4. Color

Monet 负责 UI 色；状态不能只用颜色表达：

- error = icon + text + color。
- encrypted = lock icon + label。
- selected = shape/container/icon 三者至少两种差异。

## 5. Motion

跟随系统 animator scale；应用内还提供“减少动态效果”。

- 无 motion 时操作结果仍然可理解。
- shared element 不是导航正确性的前提。

## 6. Localization

代码从第一天：

- 所有用户可见字符串进 resources。
- 禁止字符串拼接构造语法句。
- plural 使用 plurals。
- 日期/数字/文件大小 locale-aware。
- UI 不假设中文长度。

首发语言建议：简体中文 + English。后续扩展繁中/日文时无需改布局架构。

## 7. RTL

漫画 reading direction 与 UI RTL 是两个概念：

- UI 根据 locale RTL。
- Reader reading direction 根据 Publication/user setting。
- 不把两者直接绑定。

