# WebDAV 读取实施与核验

日期：2026-11-07。用户要求完善从云端获取漫画。遵循 ADR-004/007，以内部只读 DocumentsProvider 接入现有 Source 读取边界，UI 通过 ViewModel → Repository 访问网络。

## 实施范围

追加需求：多连接与目录书架映射见 [启动稳定性与云端书架](52_STARTUP_AND_CLOUD_SHELVES.md)。目录按钮更新为「加入书架」；扫描所选目录及子目录后复用稳定书架。

- 设置 → 仓库 → WebDAV：添加、编辑、移除连接，测试连接，逐级浏览目录，导入单文件或扫描当前目录。
- 支持现有漫画格式与散图目录。复用解析器、封面生成、阅读记录、书签与保险库导入；不复制阅读器。
- HTTPS 默认；HTTP 需用户显式允许，提示账号和内容明文传输。不忽略证书错误，不跨来源转发认证，不自动跟随重定向。
- PROPFIND Depth:1，目录 XML 有字节、层级、条数上限，禁用外部实体。限制 href 为同源且在配置根目录内。
- 凭据及连接配置采用 Android Keystore AES-GCM 加密，放在 noBackupFilesDir；不进入清单导出、日志或系统备份。
- 下载使用有界文件流、超时、取消与原子提交；缓存有总量上限，失败不提交半文件。缓存可清理，远端原文件不删除。
- 首版完整下载后随机读取，不宣称 Range 流式读取。没有后台轮询、自动上传或双向进度同步。

## 核验计划

协议 fixture 覆盖认证失败、多命名空间、相对/绝对 href、中文文件名、恶意跨源与越界路径、XML 实体与资源预算、取消、下载长度不符、缓存重用。回归本地导入、漫画解析和 Vault；运行单测、Lint、Debug 构建。API31/API37 和用户真实服务器结果单独记录。

### 本轮已完成核验

- `build/webdav-final-verification.log`：全量单测、Debug/Release Lint、Debug 主包与设备测试包构建成功。
- WebDAV 协议 8 项、仓库 14 项、界面 8 项，共 30 项均通过。仓库与界面覆盖 Robolectric API31/35。
- API35 模拟器：真实 Android Keystore、HTTP Basic、本机协议服务器、DocumentsProvider 随机读取漫画及缓存复用，设备测试 1 项通过（`build/webdav-device-check.log`）。未读写用户连接或漫画。
- 已检查 `app/build/ui-review/webdav-nutstore-light-api35.png` 与 `webdav-editor-dark-font-200-api31.png`；错误页、空页与表单截图同目录。真实服务账号、API31/API37 设备、TalkBack 与平板仍需独立验收。

## 数据与回滚

连接预设包含坚果云、Nextcloud、群晖 NAS 和自定义。坚果云自动填入 `https://dav.jianguoyun.com/dav/`；Nextcloud 和群晖部署地址由用户提供，不预填虚构域名。服务商切换清除旧账号、密码与 HTTP 许可。预设不代表各服务商 OAuth 集成。

官方配置依据：[坚果云第三方应用管理](https://help.jianguoyun.com/?cat=4)、[Nextcloud WebDAV](https://docs.nextcloud.com/server/stable/developer_manual/client_apis/WebDAV/basic.html)、[群晖 WebDAV 端口](https://kb.synology.com/en-global/DSM/tutorial/What_network_ports_are_used_by_Synology_services)。

## 安全边界与限制

- 凭据配置使用独立随机 IV、AES-GCM 和原子文件替换；损坏或密钥缺失时拒绝读取，不覆盖原配置。连接编辑页启用安全窗口，不保留密码到 SavedState。
- 内部 DocumentsProvider 由系统 MANAGE_DOCUMENTS 权限保护，不暴露系统选择器根目录，仅支持读取。
- 支持 Basic 认证和匿名访问；未实现 Digest、OAuth、客户端证书或忽略自签名证书错误。
- 单文件上限 512 MiB，WebDAV 下载缓存预算 768 MiB；现有阅读器还会生成独立的可清理读取缓存。缓存可能被系统或预算淘汰，不作为永久离线副本。断网只允许回退到完整下载的缓存。
- 目录响应上限 4 MiB、5000 条、XML 深度 32、单字段 16 KiB。超限报错，不把截断目录当作完整扫描结果。
- 不开启后台轮询。HTTP 仅在用户对当前连接明确允许时启用；网络安全配置为此允许明文连接，但客户端默认仍拒绝 HTTP。
- 加入保险库后清理该书对应下载缓存；远端原文件仍保留，用户需要自行管理服务商上的原件。

不改 Room schema、数据库版本或 Vault 容器格式。来源使用内部 content URI，凭据引用仅用随机连接 ID。移除连接保留书库阅读记录，不删除云端文件。回滚代码不会删除连接密文或漫画原文件；旧版不能打开新增内部来源。

参考：[RFC 4918](https://www.rfc-editor.org/rfc/rfc4918) 的 PROPFIND 与 multistatus。
