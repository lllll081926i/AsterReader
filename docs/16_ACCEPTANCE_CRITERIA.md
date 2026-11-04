# 16 验收标准

## 资料库

- [ ] 用户可添加多个本地仓库。
- [ ] 重新启动后 URI 权限仍有效；失效时能引导重新授权。
- [ ] 增量扫描不会重复创建未变化书籍。
- [ ] 文件重命名后尽可能保留阅读进度。
- [ ] 10,000 项列表可搜索/筛选，不一次性加载全部实体到内存。
- [ ] “从库移除”和“删除原文件”文案完全不同。

## 漫画

- [ ] 图片目录/CBZ/CBR/CB7/CBT 可打开。
- [ ] 横翻、竖滚、RTL、Webtoon、双页可独立配置。
- [ ] 50MP 图片不会默认整图 OOM。
- [ ] 快速跳页时旧解码任务被取消。
- [ ] 强杀 App 后恢复到最近可靠页。

## 文字书

- [ ] 字号/行距/边距变化实时生效且不会错页。
- [ ] 目录跳转准确。
- [ ] 全文搜索结果能跳回目标位置。
- [ ] 高亮/笔记重启后保持。
- [ ] 主题切换不错误改变分页缓存。

## PDF

- [ ] 超大 PDF 不整本载入内存。
- [ ] 不同页尺寸正确适配。
- [ ] 单页损坏不导致整本崩溃。
- [ ] 支持分页/连续两种阅读模式。

## UI

- [ ] Android 12+ 动态色正常。
- [ ] Android 12 / API 31 为最低支持边界；无 Android 11 及以下兼容分支。
- [ ] Dynamic Color 异常时 emergency fallback 可用，但不会覆盖正常 Monet。
- [ ] Phone/Tablet/Foldable 布局无明显拉伸。
- [ ] 200% 字体缩放可操作。
- [ ] Reader overlay 不遮住系统手势区域。

## 数据安全

- [ ] 备份/恢复 roundtrip 完整。
- [ ] 数据库升级不 destructive migrate。
- [ ] 清缓存不删除原文件。
- [ ] WebDAV 密码不进入明文备份。

## v2.0 应用锁与加密

- [ ] App Lock 开启后冷启动必须先认证，首页敏感数据不闪现。
- [ ] 支持 `BIOMETRIC_STRONG | DEVICE_CREDENTIAL`。
- [ ] 屏幕关闭后按策略重新锁定。
- [ ] 单本书“加密并上锁”后，Vault 文件不是可直接打开的原格式。
- [ ] 加密失败时绝不删除原文件。
- [ ] 阅读受保护内容不生成持久明文 temp file。
- [ ] 修改任意 ciphertext chunk 会触发完整性错误。
- [ ] 未解锁时真实标题/封面不进入 UI state。
- [ ] protected publication 不进入普通 FTS。
- [ ] Protected Reader 默认可阻止 screenshot / recents preview。
- [ ] 忘记 PIN / recovery password / device-key invalidation 流程符合策略。
- [ ] 未设置 recovery password 时明确提示卸载/清数据风险。

## v2.0 Design / Motion

- [ ] Android 12+ 默认 Dynamic Color。
- [ ] Material3 MotionScheme 或统一 fallback token 覆盖主要组件。
- [ ] Library -> Detail 封面共享元素正常。
- [ ] Predictive Back 可取消/恢复状态。
- [ ] animation scale 0 时无逻辑卡死。
- [ ] Reduced Motion 可用。
- [ ] edge-to-edge/insets 在手势和三键导航均正确。
