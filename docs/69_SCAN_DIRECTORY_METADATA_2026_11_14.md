# 目录扫描元数据复用与项数限制移除

## 问题与实施

目录扫描与散图目录枚举共用一万项硬限制。目录内文件数量较多时，扫描会直接失败，即使这些文件只需要读取元数据。现已移除本地 SAF 和 WebDAV 枚举的该项数限制，继续保留目录去重、取消检查、游标关闭与扫描失败状态。

散图扫描此前先查询目录并分类，再调用完整散图导入流程。该流程重复查询同一目录，并为每个文件读取文件头。现复用本次扫描取得的图片元数据，按自然顺序建立页面 URI 索引；不打开图片内容，不读取文件头，不解码封面。单独导入散图目录仍保留原来的文件头识别路径。

目录分类改为一次遍历，避免创建多份筛选后的中间列表。压缩包和 PDF 仍仅登记来源，详情预读与阅读才建立内容索引。扫描中断不执行来源缺失核对，避免将尚未访问的作品误标为缺失。散图刷新继续保留阅读进度和用户信息。

## 核验范围

LibraryRepositoryTest 新增超过一万项目录的扫描回归，验证忽略项计数、作品入库、零次文件内容打开和游标关闭。另新增散图扫描与刷新回归，检查每轮只查询一次目录、零次图片内容打开、1/2/3/10 的自然排序和阅读页码保留；新增页面后，百分比仍按新总页数重新计算。取消枚举回归检查游标关闭、仓库可重试状态与已有作品来源状态。

定向运行 LibraryRepositoryTest 和 WebDavRepositoryTest，79 项测试全部通过。完整执行 `testDebugUnitTest :app:lintRelease :app:compileDebugAndroidTestKotlin :app:assembleRelease -Paster.splitApks=true --console=plain`，构建成功，405 项测试全部通过，失败、错误和跳过均为 0；Release Lint 为 56 条警告、0 错误。完整日志为 build/reader-performance-final.log，定向日志为 build/scan-metadata-targeted.log。

通用版、arm64-v8a、armeabi-v7a 三个 APK 均通过 apksigner 校验，证书 SHA256 与原正式版一致：EB1F0D147AD3CA031A8E0E63245865D4AD4377C16061F3693A4FB45CA02B9B2A。启用 Release R8 和资源压缩，版本保持 1.0.1（code 4），minSdk 31、targetSdk 37。用户试用包位于 build/reader-performance-20261114/；本轮没有上云构建、发布、连接手机或启动应用。

## 影响与限制

修改模块为 app 的 LibraryRepository 与对应回归测试。无数据库 schema、加密格式、依赖或版本号变化，不发布。本地副本缓存总预算保持原值，该预算不是单文件读取上限。

本轮仍按目录保留元数据列表，WebDAV 列表接口也仍返回完整目录；没有将其描述为恒定内存的流式扫描。文件提供者的查询速度、实际内存和磁盘空间仍影响可处理规模。API31/API37 真机未进行本轮手工验收，用户使用实际漫画检查效果。
