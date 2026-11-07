# 正式 LOGO 资源

原图由项目所有者提供。保留原始字节，不进行生成式重绘、拉伸或裁切。

| 位置 | 内容与用途 |
|---|---|
| `source/logo-original.png` | 1254×1254 RGB 原图，后续处理的唯一输入 |
| `exports/logo-rounded-{尺寸}.png` | 48、64、96、128、192、256、512、1024px；透明圆角，半径为边长 22% |
| `exports/logo-transparent-1024.png` | 去除外部白底的主体，内部白色保留 |
| `exports/logo-square-512.png` | 512px 方形白底，供需要自行裁切的分发工具使用 |
| `previews/overview.png` | 圆角、圆形、圆角矩形、单色图标核验图，不进入 APK |
| `manifest.json` | 原图与生成文件的 SHA-256、尺寸、相对路径 |
| `app/src/main/res/drawable-nodpi/aster_logo.png` | 192px 圆角展示图，关于页使用 |
| `app/src/main/res/drawable-xxxhdpi/aster_launcher_*.png` | 432px 自适应主体与独立单色层，对应 108dp |
| `app/src/main/res/mipmap-{密度}/ic_launcher*.png` | mdpi 48、hdpi 72、xhdpi 96、xxhdpi 144、xxxhdpi 192px；方圆两套兼容资源 |
| `app/src/main/res/mipmap-anydpi-v26/` | 正常桌面、自适应形状与主题图标入口 |

展示图提前处理圆角；Android 自适应图层不提前加圆角，外形由系统遮罩处理。主体所有像素位于中央安全圆内，预留抗锯齿余量。单色层按明暗提取轮廓、眼睛和书本，不使用不透明白底，也不继续使用旧书本标志。LOGO 保留品牌原色，页面组件继续使用 Monet。

生成命令（Python 3，Pillow 12 或更高）：

```powershell
python scripts/Generate-Branding.py
```

生成器只写本清单对应的资源，不清空目录。更换原图后重新生成，再检查预览、关于页和模拟器桌面。尺寸不会超过原图，不作无收益放大。

Android 规格依据：[Adaptive icons](https://developer.android.com/develop/ui/compose/system/icon_design_adaptive)。
