"""One-shot generator for the supreme elytra recipe from the vanilla 16x16 texture.

Each native pixel -> a 5x5 block of the same key in an 80x80 pattern.
Classification: alpha<128 -> space (empty); else luminance<96 -> 'D' (diamond, outline);
otherwise -> 'P' (phantom_membrane, fill).
"""
from pathlib import Path
from PIL import Image
import json

SRC = Path(r"D:\NotWork\MinecraftDebug\MinecraftSource-1.21.1\assets\minecraft\textures\item\elytra.png")
OUT = Path(r"D:\NotWork\MinecraftSupremeCrafting\common\src\main\resources\data\supreme_crafting\recipe\elytra.json")
SCALE = 2

def classify(rgba):
    r, g, b, a = rgba
    if a < 128:
        return ' '
    lum = 0.299 * r + 0.587 * g + 0.114 * b
    if lum < 96:
        return 'D'
    return 'P'

img = Image.open(SRC).convert("RGBA")
w, h = img.size
assert (w, h) == (16, 16), f"unexpected texture size {w}x{h}"

native = [[classify(img.getpixel((x, y))) for x in range(w)] for y in range(h)]

# 5x upscale
pattern = []
for row in native:
    expanded = ''.join(c * SCALE for c in row)
    for _ in range(SCALE):
        pattern.append(expanded)

recipe = {
    "type": "supreme_crafting:supreme_shaped",
    "pattern": pattern,
    "key": {
        "P": {"item": "minecraft:phantom_membrane"},
        "D": {"item": "minecraft:diamond"},
    },
    "result": {"id": "minecraft:elytra", "count": 1},
}

OUT.write_text(json.dumps(recipe, indent=2), encoding="utf-8")
print(f"wrote {OUT}")
print(f"pattern is {len(pattern)}x{len(pattern[0])}")
fills = sum(c == 'P' for row in pattern for c in row)
outlines = sum(c == 'D' for row in pattern for c in row)
print(f"phantom_membrane: {fills}, diamond: {outlines}")
