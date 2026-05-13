#!/usr/bin/env python3
"""
Convert any PNG/JPEG/WEBP image to a Supreme Crafting recipe whose pattern is
made of vanilla wool blocks (16 colors). Each pixel is mapped to the nearest
wool color by RGB Euclidean distance.

Used for "image-as-recipe" content like the Mona Lisa → portal recipe.

Usage:
    python scripts/recipe_from_png.py <input> <recipe_id> <result_id> [options]

Examples:
    # Mona Lisa → 10 obsidian, 27x27 grid (~730 wool):
    python scripts/recipe_from_png.py "C:/.../Mona-Lisa.webp" mona_lisa minecraft:obsidian --count 10 --size 27

    # Bigger 51x51 grid for a more detailed image:
    python scripts/recipe_from_png.py "C:/.../picture.png" my_recipe minecraft:nether_star --size 51

The output JSON is written to
    common/src/main/resources/data/supreme_crafting/recipe/<recipe_id>.json

(relative to the project root, which is assumed to be the cwd).

The script enforces SupremeShapedPattern's MAX_SIZE = 81. The script also
prints how many wool blocks of each color the recipe ends up costing — useful
for sanity-checking grind before committing.
"""
from __future__ import annotations

import argparse
import collections
import json
import os
import sys
from typing import Dict, Tuple

from PIL import Image

# Approximate sRGB color of each vanilla wool block.
# Sourced from the Minecraft wiki + cross-checked with vanilla textures.
WOOL_PALETTE: Dict[str, Tuple[int, int, int]] = {
    "white":      (0xF9, 0xFF, 0xFE),
    "orange":     (0xF9, 0x80, 0x1D),
    "magenta":    (0xC7, 0x4E, 0xBD),
    "light_blue": (0x3A, 0xB3, 0xDA),
    "yellow":     (0xFE, 0xD8, 0x3D),
    "lime":       (0x80, 0xC7, 0x1F),
    "pink":       (0xF3, 0x8B, 0xAA),
    "gray":       (0x47, 0x4F, 0x52),
    "light_gray": (0x9D, 0x9D, 0x97),
    "cyan":       (0x16, 0x9C, 0x9C),
    "purple":     (0x89, 0x32, 0xB8),
    "blue":       (0x3C, 0x44, 0xAA),
    "brown":      (0x83, 0x54, 0x32),
    "green":      (0x5E, 0x7C, 0x16),
    "red":        (0xB0, 0x2E, 0x26),
    "black":      (0x1D, 0x1D, 0x21),
}

# Single-character keys for the recipe pattern. Reserve ' ' and '.' (those are
# "empty cell" sentinels in SupremeShapedPattern). 16 wool colors fit easily in
# A-P; remaining chars unused.
PATTERN_CHARS = "ABCDEFGHIJKLMNOP"
MAX_SIZE = 81  # mirrors SupremeShapedPattern.MAX_SIZE


def nearest_wool(rgb: Tuple[int, int, int]) -> str:
    best_name = ""
    best_dist = float("inf")
    for name, color in WOOL_PALETTE.items():
        d = (rgb[0] - color[0]) ** 2 + (rgb[1] - color[1]) ** 2 + (rgb[2] - color[2]) ** 2
        if d < best_dist:
            best_dist = d
            best_name = name
    return best_name


def main() -> int:
    parser = argparse.ArgumentParser(
        description="PNG/JPEG/WEBP → Supreme Crafting wool-pixel recipe.")
    parser.add_argument("input", help="Path to input image (PNG/JPEG/WEBP/...).")
    parser.add_argument("recipe_id", help="Recipe id; used as the output filename.")
    parser.add_argument("result_id",
                        help="Result item id, e.g. minecraft:obsidian, minecraft:end_portal_frame.")
    parser.add_argument("--size", type=int, default=27,
                        help=f"Side length of the square grid (max {MAX_SIZE}). Default 27.")
    parser.add_argument("--count", type=int, default=1, help="Result item count. Default 1.")
    parser.add_argument(
        "--out-dir",
        default="common/src/main/resources/data/supreme_crafting/recipe",
        help="Output directory (relative to cwd).")
    args = parser.parse_args()

    if not (1 <= args.size <= MAX_SIZE):
        print(f"--size must be between 1 and {MAX_SIZE}", file=sys.stderr)
        return 2

    img = Image.open(args.input).convert("RGB")
    # "Cover" fit: scale so the shorter side reaches target, then center-crop
    # excess off the longer side. Preserves aspect ratio + fills the square
    # without distortion. For Mona Lisa (portrait) this trims top + bottom
    # equally; for landscape sources it trims left + right.
    src_w, src_h = img.size
    scale = max(args.size / src_w, args.size / src_h)
    scaled_w, scaled_h = round(src_w * scale), round(src_h * scale)
    img = img.resize((scaled_w, scaled_h), Image.LANCZOS)
    left = (scaled_w - args.size) // 2
    top = (scaled_h - args.size) // 2
    img = img.crop((left, top, left + args.size, top + args.size))
    pixels = img.load()

    # Build pattern. Assign chars to colors lazily so unused colors stay out of the key.
    color_to_char: Dict[str, str] = {}
    color_counts: collections.Counter[str] = collections.Counter()
    rows = []
    for y in range(args.size):
        row_chars = []
        for x in range(args.size):
            rgb = pixels[x, y]
            color = nearest_wool(rgb)
            color_counts[color] += 1
            if color not in color_to_char:
                if len(color_to_char) >= len(PATTERN_CHARS):
                    raise RuntimeError(
                        f"Image used more than {len(PATTERN_CHARS)} wool colors; impossible.")
                color_to_char[color] = PATTERN_CHARS[len(color_to_char)]
            row_chars.append(color_to_char[color])
        rows.append("".join(row_chars))

    key = {char: {"item": f"minecraft:{color}_wool"}
           for color, char in color_to_char.items()}

    recipe = {
        "type": "supreme_crafting:supreme_shaped",
        "pattern": rows,
        "key": key,
        "result": {"id": args.result_id, "count": args.count},
    }

    os.makedirs(args.out_dir, exist_ok=True)
    out_path = os.path.join(args.out_dir, f"{args.recipe_id}.json")
    with open(out_path, "w") as f:
        json.dump(recipe, f, indent=2)

    print(f"Wrote {out_path}  ({args.size}×{args.size} = {args.size * args.size} cells)")
    print(f"Used {len(color_to_char)} of 16 wool colors. Per-color counts:")
    for color, count in color_counts.most_common():
        # Stacks of 64; surface ceil(count/64) for grind estimate.
        stacks = (count + 63) // 64
        print(f"  {color:<11s} : {count:5d}  ({stacks} stacks)")
    return 0


if __name__ == "__main__":
    sys.exit(main())
