#!/usr/bin/env python3
"""Генерирует растровые launcher-иконки YourFriend из тех же форм, что и adaptive icon.

Запуск из корня проекта:
    python tools/generate_launcher_icons.py

Нужны Pillow и numpy. Скрипт создаёт legacy WebP-иконки для всех плотностей
и превью docs/icon.png. Adaptive-иконка на API 26+ берётся из vector drawable.
"""

from __future__ import annotations

from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

ROOT = Path(__file__).resolve().parent.parent
RES = ROOT / "app" / "src" / "main" / "res"
DOCS = ROOT / "docs"

# Рисуем в 108-координатной сетке, как у vector drawable.
CANVAS = 108.0
RENDER = 2048
SCALE = RENDER / CANVAS

# Цвета из AvatarOrbView и colors.xml.
SPACE_TOP = (27, 19, 60)
SPACE_MID = (11, 8, 34)
SPACE_BOTTOM = (5, 3, 15)
PINK = (255, 111, 174)
VIOLET = (155, 108, 255)
CYAN = (107, 232, 255)

# На legacy-иконке контент крупнее: launcher сам «зумит» adaptive foreground.
LEGACY_FOREGROUND_SCALE = 1.5


def lerp_color(stops: list[tuple[float, tuple[int, int, int]]], t: np.ndarray) -> np.ndarray:
    positions = np.array([stop[0] for stop in stops], dtype=np.float32)
    colors = np.array([stop[1] for stop in stops], dtype=np.float32)
    out = np.empty(t.shape + (3,), dtype=np.float32)
    for channel in range(3):
        out[..., channel] = np.interp(t, positions, colors[:, channel])
    return out


def linear_background(size: int) -> Image.Image:
    yy, xx = np.mgrid[0:size, 0:size].astype(np.float32)
    t = (xx + yy) / (2.0 * (size - 1))
    rgb = lerp_color(
        [(0.0, SPACE_TOP), (0.55, SPACE_MID), (1.0, SPACE_BOTTOM)],
        t,
    )
    return Image.fromarray(np.dstack([rgb, np.full((size, size, 1), 255.0)]).astype(np.uint8), "RGBA")


def radial_layer(
    size: int,
    center: tuple[float, float],
    radius: float,
    stops: list[tuple[float, tuple[int, int, int, int]]],
) -> Image.Image:
    yy, xx = np.mgrid[0:size, 0:size].astype(np.float32)
    cx, cy = center
    dist = np.sqrt((xx - cx) ** 2 + (yy - cy) ** 2) / radius
    t = np.clip(dist, 0.0, 1.0)
    positions = np.array([stop[0] for stop in stops], dtype=np.float32)
    rgba = np.empty((size, size, 4), dtype=np.float32)
    for channel in range(4):
        values = np.array([stop[1][channel] for stop in stops], dtype=np.float32)
        rgba[..., channel] = np.interp(t, positions, values)
    return Image.fromarray(np.clip(rgba, 0, 255).astype(np.uint8), "RGBA")


def circle_mask(size: int, center: tuple[float, float], radius: float) -> Image.Image:
    mask = Image.new("L", (size, size), 0)
    draw = ImageDraw.Draw(mask)
    cx, cy = center
    draw.ellipse((cx - radius, cy - radius, cx + radius, cy + radius), fill=255)
    return mask


def bezier(points: list[tuple[float, float]], steps: int = 80) -> list[tuple[float, float]]:
    (x0, y0), (x1, y1), (x2, y2), (x3, y3) = points
    result = []
    for index in range(steps + 1):
        t = index / steps
        mt = 1.0 - t
        x = mt**3 * x0 + 3 * mt**2 * t * x1 + 3 * mt * t**2 * x2 + t**3 * x3
        y = mt**3 * y0 + 3 * mt**2 * t * y1 + 3 * mt * t**2 * y2 + t**3 * y3
        result.append((x, y))
    return result


def scale_point(x: float, y: float, factor: float) -> tuple[float, float]:
    """Масштабирует точку вокруг центра иконки (54, 54)."""
    return (
        (x - 54.0) * factor + 54.0,
        (y - 54.0) * factor + 54.0,
    )


def heart_polygon(scale: float, center: tuple[float, float] = (54.0, 54.0)) -> list[tuple[float, float]]:
    """Контур сердца из ic_launcher_foreground.xml, отмасштабированный вокруг центра."""
    source = [
        ("M", (54.0, 65.0)),
        ("C", (50.8, 62.1, 41.0, 55.2, 41.0, 48.2)),
        ("C", (41.0, 43.5, 44.5, 40.3, 48.7, 40.3)),
        ("C", (51.2, 40.3, 53.0, 41.4, 54.0, 43.2)),
        ("C", (55.0, 41.4, 56.8, 40.3, 59.3, 40.3)),
        ("C", (63.5, 40.3, 67.0, 43.5, 67.0, 48.2)),
        ("C", (67.0, 55.2, 57.2, 62.1, 54.0, 65.0)),
    ]
    points: list[tuple[float, float]] = []
    current = source[0][1]
    for command, values in source[1:]:
        assert command == "C"
        curve = bezier([current, values[0:2], values[2:4], values[4:6]])
        points.extend(curve)
        current = values[4:6]

    cx, cy = center
    return [
        ((x - 54.0) * scale + cx, (y - 54.0) * scale + cy)
        for x, y in points
    ]


def draw_arc(
    layer: Image.Image,
    center: tuple[float, float],
    radius: float,
    start: float,
    end: float,
    width: float,
    color: tuple[int, int, int, int],
    scale: float,
) -> None:
    draw = ImageDraw.Draw(layer)
    cx, cy = center
    box = (
        (cx - radius) * scale,
        (cy - radius) * scale,
        (cx + radius) * scale,
        (cy + radius) * scale,
    )
    draw.arc(box, start=start, end=end, fill=color, width=max(1, round(width * scale)))
    # Круглые торцы, как у Paint.Cap.ROUND в AvatarOrbView.
    for angle in (start, end):
        radians = np.deg2rad(angle)
        x = cx + radius * np.cos(radians)
        y = cy + radius * np.sin(radians)
        cap = width * scale / 2.0
        draw.ellipse((x * scale - cap, y * scale - cap, x * scale + cap, y * scale + cap), fill=color)


def star(
    draw: ImageDraw.ImageDraw,
    x: float,
    y: float,
    size: float,
    color: tuple[int, int, int, int],
    scale: float,
) -> None:
    points = [
        (x, y - size),
        (x + size * 0.32, y - size * 0.32),
        (x + size, y),
        (x + size * 0.32, y + size * 0.32),
        (x, y + size),
        (x - size * 0.32, y + size * 0.32),
        (x - size, y),
        (x - size * 0.32, y - size * 0.32),
    ]
    draw.polygon([(px * scale, py * scale) for px, py in points], fill=color)


def render_icon(shape: str, foreground_scale: float) -> Image.Image:
    size = RENDER
    center = (54.0 * SCALE, 54.0 * SCALE)
    scale = SCALE

    background = linear_background(size)
    background_glow = radial_layer(
        size,
        center,
        48.0 * scale,
        [
            (0.0, (255, 122, 182, 46)),
            (0.55, (155, 108, 255, 18)),
            (1.0, (107, 232, 255, 0)),
        ],
    )
    background = Image.alpha_composite(background, background_glow)

    # Маска самой иконки: скруглённый квадрат или круг.
    mask = Image.new("L", (size, size), 0)
    mask_draw = ImageDraw.Draw(mask)
    if shape == "round":
        mask_draw.ellipse((0, 0, size - 1, size - 1), fill=255)
    else:
        radius = 0.22 * size
        mask_draw.rounded_rectangle((0, 0, size - 1, size - 1), radius=radius, fill=255)
    background.putalpha(mask)

    # Звёзды фона.
    stars = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    star_draw = ImageDraw.Draw(stars)
    for x, y, r, alpha in [
        (17.6, 19.4, 0.9, 102),
        (89.2, 15.8, 0.7, 77),
        (14.4, 87.6, 1.1, 89),
        (93.2, 91.4, 0.8, 64),
        (88.6, 57.2, 0.6, 56),
    ]:
        star_draw.ellipse(
            (
                (x - r) * scale,
                (y - r) * scale,
                (x + r) * scale,
                (y + r) * scale,
            ),
            fill=(255, 255, 255, alpha),
        )
    stars.putalpha(Image.composite(stars.getchannel("A"), Image.new("L", (size, size), 0), mask))
    background = Image.alpha_composite(background, stars)

    fg = Image.new("RGBA", (size, size), (0, 0, 0, 0))

    # Сияние.
    glow = radial_layer(
        size,
        center,
        33.0 * foreground_scale * scale,
        [
            (0.0, (255, 122, 182, 89)),
            (0.58, (155, 108, 255, 38)),
            (1.0, (107, 232, 255, 0)),
        ],
    )
    fg = Image.alpha_composite(fg, glow)

    # Дуги.
    draw_arc(
        fg,
        (54.0, 54.0),
        28.06 * foreground_scale,
        20.0,
        90.0,
        1.5 * foreground_scale,
        (255, 122, 182, 179),
        scale,
    )
    draw_arc(
        fg,
        (54.0, 54.0),
        24.84 * foreground_scale,
        205.0,
        323.0,
        1.7 * foreground_scale,
        (107, 232, 255, 217),
        scale,
    )

    # Орб.
    orb_radius = 23.0 * foreground_scale * scale
    orb_center = scale_point(46.64, 45.26, foreground_scale)
    orb = radial_layer(
        size,
        (orb_center[0] * scale, orb_center[1] * scale),
        orb_radius * 1.55,
        [
            (0.0, (255, 180, 213, 255)),
            (0.32, (255, 111, 174, 255)),
            (0.72, (155, 108, 255, 255)),
            (1.0, (101, 70, 166, 255)),
        ],
    )
    orb.putalpha(circle_mask(size, center, orb_radius))
    fg = Image.alpha_composite(fg, orb)

    # Блик.
    highlight_center = scale_point(47.56, 46.64, foreground_scale)
    highlight = radial_layer(
        size,
        (highlight_center[0] * scale, highlight_center[1] * scale),
        7.82 * foreground_scale * scale,
        [
            (0.0, (255, 255, 255, 64)),
            (0.7, (255, 255, 255, 34)),
            (1.0, (255, 255, 255, 0)),
        ],
    )
    fg = Image.alpha_composite(fg, highlight)

    # Сердце: мягкая тень + белый контур.
    heart = heart_polygon(foreground_scale, (54.0, 54.0))
    heart_px = [(x * scale, y * scale) for x, y in heart]
    shadow = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    shadow_draw = ImageDraw.Draw(shadow)
    offset = 0.008 * size
    shadow_draw.polygon(
        [(x, y + offset) for x, y in heart_px],
        fill=(42, 12, 40, 80),
    )
    shadow = shadow.filter(ImageFilter.GaussianBlur(radius=0.012 * size))
    fg = Image.alpha_composite(fg, shadow)

    heart_layer = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    ImageDraw.Draw(heart_layer).polygon(heart_px, fill=(255, 255, 255, 255))
    fg = Image.alpha_composite(fg, heart_layer)

    # Искры.
    sparkle_layer = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    sparkle_draw = ImageDraw.Draw(sparkle_layer)
    for x, y, s, color in [
        (83.8, 62.6, 2.1, (107, 232, 255, 230)),
        (43.2, 84.2, 1.8, (255, 255, 255, 204)),
        (24.2, 43.2, 1.7, (255, 155, 200, 179)),
        (64.8, 24.2, 1.5, (255, 255, 255, 153)),
    ]:
        star(sparkle_draw, x, y, s * foreground_scale, color, scale)
    fg = Image.alpha_composite(fg, sparkle_layer)

    icon = Image.alpha_composite(background, fg)
    return icon.resize((1024, 1024), Image.LANCZOS)


def main() -> None:
    DOCS.mkdir(parents=True, exist_ok=True)
    densities = {
        "mdpi": 48,
        "hdpi": 72,
        "xhdpi": 96,
        "xxhdpi": 144,
        "xxxhdpi": 192,
    }

    square = render_icon("square", LEGACY_FOREGROUND_SCALE)
    round_icon = render_icon("round", LEGACY_FOREGROUND_SCALE)

    square.resize((512, 512), Image.LANCZOS).save(DOCS / "icon.png", optimize=True)

    for density, px in densities.items():
        folder = RES / f"mipmap-{density}"
        folder.mkdir(parents=True, exist_ok=True)
        square.resize((px, px), Image.LANCZOS).save(
            folder / "ic_launcher.webp", "WEBP", quality=95, method=6
        )
        round_icon.resize((px, px), Image.LANCZOS).save(
            folder / "ic_launcher_round.webp", "WEBP", quality=95, method=6
        )
        print(f"{density}: {px}x{px}")

    print("docs/icon.png готов")


if __name__ == "__main__":
    main()
