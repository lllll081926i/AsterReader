# 14 许可证、合规与风险

## 推荐许可证策略

默认建议 **Apache-2.0**（或 MIT）发布本项目自有代码，前提是采用 clean-room 架构，不直接复制 GPL-3.0 项目源码。

### Book's Story

仓库公开为 GPL-3.0。若直接复制/修改其源码形成派生作品，需要遵守 GPL-3.0 的传播条件。因此本档案默认只参考它的公开行为、功能和设计，不以其源码为基础。

### FrogReader

公开为 MIT。若复用具体代码，保留原版权与 MIT 许可声明，并在 THIRD_PARTY_NOTICES 记录来源与 commit。

### MH-ARK

从公开商店/官网信息与截图可观察其产品行为，但不能假定其应用代码开源。仅参考通用交互与公开功能，禁止反编译复制实现。

## 第三方库候选

- Coil：Apache-2.0
- libarchive-android：Apache-2.0
- AndroidX / Compose：Apache-2.0 体系
- dav4jvm：MPL-2.0，若采用需保留对应文件级许可义务

每次引入 native library 必须额外检查：ABI、16KB page size、维护状态、CVE、许可证。

## 主要工程风险

### 1. 格式长尾

漫画容器兼容性必须用真实损坏样本验证。解决：fixture corpus + Engine 隔离 + 失败降级。

### 2. 超大漫画/PDF 内存

解决：采样、tile、窗口缓存、bitmap budget、低内存回调。

### 3. SAF 性能与 Provider 差异

不同 DocumentsProvider 的 list/open 性能差异很大。解决：扫描限流、缓存 metadata、避免频繁 `DocumentFile` 层层遍历，必要时封装更底层 DocumentsContract 查询。

### 4. WebDAV 随机访问假设错误

并非所有服务器支持 Range，且压缩格式本身可能不适合随机读。解决：运行时能力探测 + fallback 完整缓存。

### 5. 过度模块化

早期拆 30 个模块会拖慢构建和开发。建议核心边界先定，feature 逐步拆。

### 6. UI 追求“精美”导致功能层混乱

M3E 是组件和 motion 语言，不等于每个元素都加大圆角和动画。阅读器本身必须让内容优先。
