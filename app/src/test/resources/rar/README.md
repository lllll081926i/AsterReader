这些 RAR 回归样本来自 junrar v8.1.2，保留原始二进制；`stored.rar` 原名为 `unicode/rar3-ansi-name.rar`，`vm-trigger.rar` 原名为 `as-vm-1-trigger.rar`，`extended-time.rar` 原名为 `rar4-ext_time.rar`。

来源：https://github.com/junrar/junrar/tree/v8.1.2/src/test/resources/com/github/junrar

用途：验证正常/提前关闭/重复读取、CRC 失败传播、恶意 VM 终止及扩展时间头。VM 样本只用于已升级解析库的测试，不作为应用内置内容。

上游许可：https://github.com/junrar/junrar/blob/v8.1.2/LICENSE 。许可全文随应用运行库许可一并保留在 `app/src/main/licenses/runtime/`。
