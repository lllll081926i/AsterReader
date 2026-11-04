# 29 Definition of Done & Quality Gates

> “能跑”不等于完成。本文件定义每一项工作在什么条件下才允许从 In Progress 变成 Done。

## 1. Feature Definition of Done

任何 Feature 必须同时满足：

- [ ] 需求和边界明确。
- [ ] UI 遵循 `design.md`，没有私自新增色值/圆角/动画常量。
- [ ] UI state 由 ViewModel/StateHolder 驱动，Composable 不直接访问数据库/文件/network。
- [ ] loading / empty / error / success / permission-lost 状态完整。
- [ ] 进程重建/旋转不会损坏持久状态。
- [ ] Compact 和至少一种 Medium/Expanded 布局检查。
- [ ] TalkBack content description / semantics 检查。
- [ ] 触控目标 >= 48dp（特殊 Reader 手势区除外）。
- [ ] Reduced Motion 可退化。
- [ ] 单元测试或 instrumentation test 覆盖关键逻辑。
- [ ] 没有 Main thread disk/network/archive/crypto 重活。
- [ ] 没有日志输出密钥、密码、受保护文件路径、明文片段。
- [ ] lint/static analysis 通过。
- [ ] 对应 acceptance criteria 更新。

## 2. Security Feature DoD

涉及 Vault/App Lock 时额外要求：

- [ ] threat model 已更新。
- [ ] failure path 比 happy path 更先测试。
- [ ] 认证取消不会误解锁。
- [ ] 进程死亡后默认回到安全状态。
- [ ] 受保护 metadata 不会在锁定状态通过 UI/FTS/notification/recents 泄露。
- [ ] 密钥材料没有写入普通 DataStore/Room/log。
- [ ] 加密写入使用独立 nonce/IV，不复用。
- [ ] 密文认证失败必须 fail closed。
- [ ] 原文件删除只发生在密文校验成功后。
- [ ] crash/kill 中断不会同时失去明文与密文。

## 3. Reader Engine DoD

- [ ] `open()` / `close()` 生命周期清晰。
- [ ] locator 可序列化并恢复。
- [ ] 同一 Publication 不产生两个互相冲突的 progress 真源。
- [ ] cancellation 可传递。
- [ ] 大文件不整体读入内存。
- [ ] corrupted/unsupported/password-required 有明确错误类型。
- [ ] source 可来自普通 SAF 或 Vault，不把 File path 写死进 engine。
- [ ] cache 有上限和淘汰策略。

## 4. Database Change Gate

每次 schema change：

- [ ] version +1。
- [ ] migration 明确。
- [ ] migration test fixture。
- [ ] 旧数据升级后关键字段验证。
- [ ] 不使用 destructive migration 逃避问题。

## 5. Dependency Upgrade Gate

- [ ] 单独 PR。
- [ ] release notes 已读。
- [ ] minSdk 仍 <= 31。
- [ ] compile/target SDK 不被意外提升。
- [ ] API 31 smoke test。
- [ ] UI/Reader/security regression tests。
- [ ] license 变化检查。

对于 Material3 alpha：额外跑 SearchBar / BottomSheet / Shared Element / Predictive Back / Motion screenshot 回归。

## 6. Alpha Gate

进入公开 Alpha 前：

- Library、App Lock、Vault、Comic Reader 主链可用。
- 无已知“可能丢用户原文件”的 bug。
- 加密中断恢复通过。
- crash reporting 若启用必须默认不包含书名/文件名等敏感数据。
- 有导出诊断信息但默认脱敏。

## 7. Beta Gate

- 电子书与 PDF 主链完成。
- 备份/恢复完成。
- API 31/33/35/37 测试矩阵完成。
- 迁移测试完整。
- Baseline Profile 有基准验证。
- TalkBack/字体放大/横屏/平板/折叠屏完成一轮系统审计。

## 8. Release Candidate Gate

- Feature Freeze。
- P0/P1 = 0。
- P2 有明确 release note / workaround 才可放行。
- 7 天以上日常自用 soak test。
- 10k library 压力测试。
- 典型超大漫画/超大 PDF/复杂 EPUB 测试。
- 密钥丢失/损坏/恢复路径演练。
- Release build + R8 + baseline profile 真机性能通过。

## 9. 1.0 Release Gate

发布负责人签字式检查：

```text
[ ] Source code tagged
[ ] Changelog complete
[ ] VersionCode/VersionName correct
[ ] Reproducible clean build attempted
[ ] Licenses/NOTICE updated
[ ] Privacy policy matches actual behavior
[ ] Backup rules verified
[ ] Vault keys excluded from cloud backup
[ ] No debug logging / test endpoints
[ ] R8 mapping stored
[ ] Database migration tests green
[ ] API 31 minimum-device smoke test green
[ ] API 37 target behavior smoke test green
[ ] Release artifact hash recorded
```

