#!/usr/bin/env python3
"""Draw the Stålvingar icon (a blue propeller fighter in front of a sunset) as pixel art.
icon.py OUT.png [SIZE]   or   icon.py --android RESDIR"""
import os
import sys

from PIL import Image, ImageDraw

N = 32   # pixel-art grid, scaled up with nearest neighbour
img = Image.new("RGBA", (N, N), (0, 0, 0, 0))
d = ImageDraw.Draw(img)
sky = [(43, 63, 122), (111, 128, 194), (233, 160, 122), (255, 213, 154)]
for y in range(20):
    t = y / 19 * 3
    i = min(2, int(t)); f = t - i
    d.line([(0, y), (N, y)], fill=tuple(round(a + (b - a) * f) for a, b in zip(sky[i], sky[i + 1])) + (255,))
d.ellipse((17, 9, 29, 21), fill=(255, 242, 192, 255))
for y in range(20, N):
    t = (y - 20) / 11
    d.line([(0, y), (N, y)], fill=(round(74 - 45 * t), round(120 - 57 * t), round(176 - 61 * t), 255))
for x, y, w in [(20, 22, 6), (19, 24, 8), (21, 27, 5), (4, 25, 4)]:
    d.line([(x, y), (x + w, y)], fill=(255, 240, 200, 255))
K, B, L, D, Y, C = (16, 19, 28), (61, 111, 196), (162, 200, 248), (34, 66, 122), (242, 200, 75), (127, 230, 255)
plane = [  # facing right
    "...KK..................",
    "...KBK.................",
    "...KBBK.......KKK......",
    "...KBBBKKKKKKKCCCKK....",
    "..KBBBBBBBBBBBBBBBBBKK.",
    ".KLLLLLLLLLLLLLLLLLBBYK",
    ".KBBBBBBBBBBBBBBBBBBBYK",
    "..KKKKKKKLLLLLLLLKKKKK.",
    "........KDDDDDDDK......",
    ".........KDDDDDK.......",
    "..........KKKKK........",
]
pal = {"K": K, "B": B, "L": L, "D": D, "Y": Y, "C": C}
ox, oy = 4, 10
for j, row in enumerate(plane):
    for i, ch in enumerate(row):
        if ch in pal:
            img.putpixel((ox + i, oy + j), pal[ch] + (255,))
for y in range(oy + 2, oy + 9):
    img.putpixel((ox + len(plane[0]), y), (230, 230, 230, 200))
mask = Image.new("L", (N, N), 0)
ImageDraw.Draw(mask).rounded_rectangle((0, 0, N - 1, N - 1), radius=6, fill=255)
img.putalpha(mask)


def save(path, size):
    img.resize((size, size), Image.NEAREST).save(path)


if sys.argv[1] == "--android":
    for name, px in {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}.items():
        os.makedirs(os.path.join(sys.argv[2], "mipmap-" + name), exist_ok=True)
        save(os.path.join(sys.argv[2], "mipmap-" + name, "ic_launcher.png"), px)
else:
    save(sys.argv[1], int(sys.argv[2]) if len(sys.argv) > 2 else 256)
