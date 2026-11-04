# 31 AI / Coding Agent Implementation Playbook

> 适用于 Codex、Claude Code、Gemini CLI 或其他 Coding Agent。根目录 `AGENTS.md` 是短版强制规则；本文件解释完整工作方式。

## 1. Agent 开始任何任务前

必须按顺序读取：

1. `AGENTS.md`
2. `design.md`（如果涉及 UI）
3. `27_FINAL_ARCHITECTURE_DECISIONS.md`
4. 当前 Feature 对应文档
5. `29_DEFINITION_OF_DONE_AND_GATES.md`

然后先输出/记录：

- 任务理解。
- 受影响模块。
- 是否涉及 schema/security/public API。
- 实现步骤。
- 测试步骤。

## 2. Agent 禁止行为

- 不得为“快”把所有逻辑塞进 `MainActivity`。
- 不得让 Composable 直接调 DAO / ContentResolver / HTTP。
- 不得新建第二套 Theme token。
- 不得为了通过编译删除 test。
- 不得使用 destructive Room migration。
- 不得降低 Vault 算法或跳过认证校验。
- 不得自动删除用户原文件。
- 不得把明文受保护内容写入 `/cache` 后忘记清理。
- 不得把路径、密码、key 放到 log。
- 不得把 experimental API 泄漏到 Feature。
- 不得“顺便”重构无关区域。
- 不得无说明升级依赖。

## 3. 每次改动的最小闭环

```text
Read docs
→ Inspect current implementation
→ Make smallest coherent change
→ Unit test
→ Feature/instrumentation test
→ Static analysis
→ Build API31/API37 smoke
→ Summarize changed files + risks
```

## 4. UI 任务

Agent 必须检查：

- Monet semantic colors。
- design.md spacing/shape/typography。
- loading/empty/error。
- compact/expanded。
- font scale。
- TalkBack semantics。
- reduced motion。
- predictive back。

截图参考用于理解信息层级，不允许逐像素复制第三方 App。

## 5. Security 任务

在写代码前先明确：

- protected asset 是什么？
- attacker 能看到哪些持久数据？
- key 在哪里？
- crash 时会留下什么？
- auth 失败时是否 fail closed？
- backup 会不会把 secret 带走？

任何不确定的密码学设计必须停下并请求 review，而不是“猜一个实现”。

## 6. Reader 任务

- Source 是 seekable 还是 stream-only？
- 是否支持 cancellation？
- 是否需要 copy 到 app cache？如果需要，为什么？
- 最大单页/单资源大小？
- prefetch 上限？
- low-memory 如何释放？
- locator 如何恢复？

## 7. 完成报告模板

```markdown
## Implemented
- ...

## Changed modules
- ...

## Tests
- unit: ...
- instrumentation: ...
- manual: API 31 / API 37 ...

## Security / data migration impact
- none / details

## Known limitations
- ...

## Next task
- ...
```
