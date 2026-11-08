# 字体许可来源

以下文件保持上游版权声明和 SIL Open Font License 1.1 全文，获取日期为 2026-11-07。版权声明与项目内对应字体的 name 表一致。构建时生成关于页资源，页面读取不依赖网络。

- `lora.OFL.txt`：[Google Fonts / Lora](https://github.com/google/fonts/blob/main/ofl/lora/OFL.txt)
- `roboto_mono.OFL.txt`：[Google Fonts / Roboto Mono](https://github.com/google/fonts/blob/main/ofl/robotomono/OFL.txt)

运行依赖清单和许可类型来自 Gradle 实际解析版本的 Maven POM，缺少直接声明时读取父 POM。此清单不替代发布前对第三方许可、NOTICE 和源代码提供义务的核验。

1.0.0 在 `runtime/index.json` 按实际解析坐标关联离线全文。文本来自上游许可、缓存 JAR/AAR 的 LICENSE/NOTICE 和 AndroidX 图形组件所用 Skia 声明；来源由 `scripts/Collect-Dependency-Notices.py` 固定记录。更新依赖时重新收集并人工核对；普通构建不联网获取许可，缺少坐标或空文本直接失败。应用“致谢”读取同一批全文，Release 另附 `THIRD_PARTY_NOTICES.txt`。
