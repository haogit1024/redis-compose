#!/usr/bin/env python3
"""Generate the Redis Compose app icon (PNG/ICNS/ICO) from the brand mark.

The mark mirrors `design/icon/BrandMark.kt`: a diagonal iris→mint tile with three
stacked white bars. Run from the repository root:

    python3 scripts/generate-app-icon.py

Outputs:
  desktopApp/icons/app-icon.png      1024 px master / Linux icon
  desktopApp/icons/app-icon.icns     macOS
  desktopApp/icons/app-icon.ico      Windows
"""
from pathlib import Path
from PIL import Image, ImageDraw, ImageFilter

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "desktopApp" / "icons"
OUT.mkdir(parents=True, exist_ok=True)

MASTER = 1024
TILE = 832
MARGIN = (MASTER - TILE) // 2
RADIUS = int(TILE * 0.30)
TOP = (0xA4, 0x9B, 0xFF)
MID = (0x6C, 0x67, 0xF2)
BOTTOM = (0x27, 0xD3, 0xB4)


def mix(a, b, t):
    return tuple(round(a[i] + (b[i] - a[i]) * t) for i in range(3))


def gradient_tile():
    tile = Image.new("RGB", (TILE, TILE))
    pixels = tile.load()
    denom = (TILE - 1) * 2
    for y in range(TILE):
        for x in range(TILE):
            t = (x + y) / denom
            color = mix(TOP, MID, t * 2) if t <= 0.5 else mix(MID, BOTTOM, (t - 0.5) * 2)
            pixels[x, y] = color
    return tile


def rounded_mask(size, radius):
    mask = Image.new("L", (size, size), 0)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, size - 1, size - 1), radius=radius, fill=255)
    return mask


def main():
    canvas = Image.new("RGBA", (MASTER, MASTER), (0, 0, 0, 0))

    # Soft shadow so the tile reads well on light and dark desktops.
    shadow_mask = Image.new("L", (MASTER, MASTER), 0)
    ImageDraw.Draw(shadow_mask).rounded_rectangle(
        (MARGIN, MARGIN + 18, MARGIN + TILE - 1, MARGIN + TILE + 17),
        radius=RADIUS,
        fill=90,
    )
    shadow = Image.new("RGBA", (MASTER, MASTER), (0, 0, 0, 0))
    shadow.putalpha(shadow_mask.filter(ImageFilter.GaussianBlur(26)))
    canvas.alpha_composite(shadow)

    tile = gradient_tile().convert("RGBA")
    tile.putalpha(rounded_mask(TILE, RADIUS))
    canvas.alpha_composite(tile, (MARGIN, MARGIN))

    # Three outlined bars, matching BrandMark's proportions.
    bars = Image.new("RGBA", (MASTER, MASTER), (0, 0, 0, 0))
    draw = ImageDraw.Draw(bars)
    for index, width_ratio in enumerate((0.56, 0.44, 0.32)):
        bar_width = TILE * width_ratio
        bar_height = TILE * 0.115
        left = MARGIN + (TILE - bar_width) / 2
        top = MARGIN + TILE * (0.30 + index * 0.18)
        alpha = int(255 * (0.92 - index * 0.22))
        draw.rounded_rectangle(
            (left, top, left + bar_width, top + bar_height),
            radius=TILE * 0.05,
            outline=(255, 255, 255, alpha),
            width=max(2, round(TILE * 0.075)),
        )
    canvas.alpha_composite(bars)

    master = canvas
    master.save(OUT / "app-icon.png")

    # macOS .iconset → .icns
    iconset = OUT / "app.iconset"
    iconset.mkdir(exist_ok=True)
    for size in (16, 32, 128, 256, 512):
        master.resize((size, size), Image.LANCZOS).save(iconset / f"icon_{size}x{size}.png")
        master.resize((size * 2, size * 2), Image.LANCZOS).save(
            iconset / f"icon_{size}x{size}@2x.png"
        )
    import subprocess

    subprocess.run(
        ["iconutil", "-c", "icns", str(iconset), "-o", str(OUT / "app-icon.icns")],
        check=True,
    )

    # Windows .ico with the standard size set.
    master.save(
        OUT / "app-icon.ico",
        sizes=[(16, 16), (24, 24), (32, 32), (48, 48), (64, 64), (128, 128), (256, 256)],
    )

    # Linux package icon (512 px is accepted by most desktop environments).
    master.resize((512, 512), Image.LANCZOS).save(OUT / "app-icon-linux.png")

    print("wrote:")
    for path in sorted(OUT.glob("app-icon.*")):
        print(" ", path.relative_to(ROOT))


if __name__ == "__main__":
    main()
