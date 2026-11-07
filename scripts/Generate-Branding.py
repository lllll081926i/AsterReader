"""从正式原图生成圆角展示图、Android 图标和可核验清单。需要 Pillow。"""

from hashlib import sha256
import json
from pathlib import Path

from PIL import Image, ImageChops, ImageDraw, ImageFont, ImageOps


ROOT = Path(__file__).resolve().parent.parent
BRAND = ROOT / "assets/branding"
RES = ROOT / "app/src/main/res"
SOURCE = BRAND / "source/logo-original.png"
SIZES = (48, 64, 96, 128, 192, 256, 512, 1024)
DENSITIES = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}
LANCZOS = Image.Resampling.LANCZOS
generated = []


def save(image, path):
    path.parent.mkdir(parents=True, exist_ok=True)
    image.save(path, optimize=True)
    generated.append(path)


def rounded(image, fraction=0.22):
    # 在四倍尺寸绘制遮罩，缩小后保留圆角抗锯齿。
    size = image.width
    mask = Image.new("L", (size * 4, size * 4))
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, size * 4 - 1, size * 4 - 1),
                                          radius=size * 4 * fraction, fill=255)
    result = image.convert("RGBA")
    result.putalpha(mask.resize(image.size, LANCZOS))
    return result


def extract_art(image):
    # 只移除与画布边缘连通的近白背景，保留头发、脸和书页内部的白色。
    red, green, blue = image.split()
    minimum = ImageChops.darker(ImageChops.darker(red, green), blue)
    candidates = minimum.point(lambda value: 255 if value >= 238 else 0)
    for seed in ((0, 0), (image.width - 1, 0), (0, image.height - 1),
                 (image.width - 1, image.height - 1)):
        ImageDraw.floodfill(candidates, seed, 128, thresh=0)
    alpha = candidates.point(lambda value: 0 if value == 128 else 255)
    result = image.convert("RGBA")
    result.putalpha(alpha)
    return result


def adaptive(art):
    # 所有主体像素落在中央 66dp 直径圆内，兼容圆形与圆角矩形遮罩。
    alpha = art.getchannel("A")
    center = (art.width / 2, art.height / 2)
    max_radius = max(((x - center[0]) ** 2 + (y - center[1]) ** 2) ** 0.5
                     for y in range(art.height) for x in range(art.width)
                     if alpha.getpixel((x, y)) > 0)
    factor = 31.5 * 4 / max_radius
    size = round(art.width * factor)
    result = Image.new("RGBA", (432, 432))
    result.alpha_composite(art.resize((size, size), LANCZOS), ((432 - size) // 2,) * 2)
    return result


def monochrome(foreground):
    # 明亮区域留空，保留眼睛、书本和轮廓，避免整张白底被系统染成色块。
    ink = ImageOps.grayscale(foreground).point(lambda value: max(0, min(255, (235 - value) * 3)))
    result = Image.new("RGBA", foreground.size, (0, 0, 0, 0))
    result.putalpha(ImageChops.multiply(ink, foreground.getchannel("A")))
    return result


def main():
    image = ImageOps.exif_transpose(Image.open(SOURCE)).convert("RGB")
    if image.width != image.height:
        raise ValueError("正式原图必须为正方形；禁止拉伸或静默裁切")
    art = extract_art(image)
    save(art.resize((1024, 1024), LANCZOS), BRAND / "exports/logo-transparent-1024.png")
    for size in SIZES:
        save(rounded(image.resize((size, size), LANCZOS)), BRAND / f"exports/logo-rounded-{size}.png")
    save(image.resize((512, 512), LANCZOS), BRAND / "exports/logo-square-512.png")
    foreground = adaptive(art)
    mono = monochrome(foreground)
    save(foreground, RES / "drawable-xxxhdpi/aster_launcher_foreground.png")
    save(mono, RES / "drawable-xxxhdpi/aster_launcher_monochrome.png")
    save(rounded(image.resize((192, 192), LANCZOS)), RES / "drawable-nodpi/aster_logo.png")
    # 兼容静态图标读取方；API 31+ 桌面优先使用 adaptive-icon XML。
    for density, size in DENSITIES.items():
        save(rounded(image.resize((size, size), LANCZOS)), RES / f"mipmap-{density}/ic_launcher.png")
        circle = image.resize((size, size), LANCZOS).convert("RGBA")
        mask = Image.new("L", (size * 4, size * 4))
        ImageDraw.Draw(mask).ellipse((0, 0, size * 4 - 1, size * 4 - 1), fill=255)
        circle.putalpha(mask.resize((size, size), LANCZOS))
        save(circle, RES / f"mipmap-{density}/ic_launcher_round.png")

    # 用于人工核验圆角、系统裁切与莫奈单色，不打包进 APK。
    preview = Image.new("RGB", (1000, 320), "#e9ecf1")
    draw = ImageDraw.Draw(preview)
    labels = ("Rounded", "Adaptive circle", "Adaptive squircle", "Monochrome")
    for index, label in enumerate(labels):
        if index == 0:
            icon = rounded(image.resize((200, 200), LANCZOS))
        else:
            background = Image.new("RGBA", (432, 432), "white" if index < 3 else "#d8e4fc")
            layer = foreground if index < 3 else Image.new("RGBA", (432, 432), "#29477d")
            if index == 3:
                layer.putalpha(mono.getchannel("A"))
            background.alpha_composite(layer)
            # 桌面可见窗口为中央 72dp，外侧留给视差动效。
            icon = background.crop((72, 72, 360, 360)).resize((200, 200), LANCZOS)
            icon = rounded(icon, 0.5 if index == 1 else 0.22)
        preview.paste(icon, (index * 250 + 25, 35), icon)
        draw.text((index * 250 + 25, 255), label, fill="#253046", font=ImageFont.load_default(size=18))
    save(preview, BRAND / "previews/overview.png")
    manifest = {"source": str(SOURCE.relative_to(ROOT)).replace("\\", "/"),
                "source_sha256": sha256(SOURCE.read_bytes()).hexdigest(),
                "source_size": list(image.size), "corner_radius_ratio": 0.22,
                "files": [{"path": str(path.relative_to(ROOT)).replace("\\", "/"),
                           "size": list(Image.open(path).size),
                           "sha256": sha256(path.read_bytes()).hexdigest()} for path in generated]}
    (BRAND / "manifest.json").write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"已生成 {len(generated)} 个资源；原图保持不变。")


if __name__ == "__main__":
    main()
