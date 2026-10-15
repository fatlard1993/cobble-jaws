#!/usr/bin/env python3
"""
The shark's body, its skin, and its spawn egg.

The body and the skin come from one table, BOXES, because they are one thing: every box's place on
the sheet is decided here, and the model file and the painting both read it. A box moved in one
and not the other is a skin smeared over the wrong faces, which the table makes impossible.

The model is the shark's whole body: Pandorical draws it from this file alone, and moves it by the
file's animation block - the body turns where the shark looks, and the animations in
assets/cobble-jaws-justfatlard/animations sway the tail side to side, lagging into the tail fin,
faster as it swims faster.

Sides are painted by vanilla's box layout: across the top, the top face then the underside; below
that, the four sides, starting with the animal's right flank (+x), then its front (-z), its left
flank, and its back. On the right flank the front is at the strip's right-hand end, and on the
left flank at its left-hand end, which is where the eyes and gills go.
"""

import json
import zipfile
from pathlib import Path

from PIL import Image

HERE = Path(__file__).parent
ASSETS = HERE / "src/main/resources/assets/cobble-jaws-justfatlard"
CLIENT_JAR = Path.home() / ".gradle/caches/fabric-loom/26.3/minecraft-client-only.jar"
SHEET = (64, 64)

# Each part: pivot and rotation (degrees) against its parent, its boxes, and its children. Each box:
# where it is cut from the sheet, its corner and size in model units (sixteen to a block, y down),
# what it is painted as, and whether its texture is mirrored.
SHARK = {
    "body": {
        "pivot": [0, 22, -5],
        "boxes": [((0, 0), (-4, -7, 0), (8, 7, 13), "flank")],
        "children": {
            "head": {
                "boxes": [((0, 20), (-3.5, -6.5, -6), (7, 6, 6), "head")],
                "children": {
                    "snout": {"boxes": [((26, 20), (-2.5, -6, -9), (5, 4, 3), "snout")]},
                },
            },
            # Raked forward edge, sheer trailing edge: the one part of a shark everyone can draw.
            "back_fin": {
                "boxes": [
                    ((42, 0), (-0.5, -10, 3), (1, 3, 5), "fin"),
                    ((54, 0), (-0.5, -12, 5), (1, 2, 3), "fin"),
                    ((54, 5), (-0.5, -13, 6), (1, 1, 2), "fin"),
                ],
            },
            # High on the flank and only a little dropped: lower and steeper, they stood like legs.
            "left_fin": {
                "pivot": [4, -2.5, 3],
                "rotation": [0, 0, 15],
                "boxes": [
                    ((0, 46), (0, -0.5, 0), (6, 1, 4), "fin"),
                    ((20, 46), (4, -0.5, 3), (2, 1, 3), "fin"),
                ],
            },
            "right_fin": {
                "pivot": [-4, -2.5, 3],
                "rotation": [0, 0, -15],
                "boxes": [
                    ((0, 46), (-6, -0.5, 0), (6, 1, 4), "fin", True),
                    ((20, 46), (-6, -0.5, 3), (2, 1, 3), "fin", True),
                ],
            },
            "tail": {
                "pivot": [0, -3.5, 13],
                "rotation": [-6, 0, 0],
                "boxes": [
                    ((0, 32), (-2, -2.5, 0), (4, 5, 9), "flank"),
                    ((26, 32), (-1.5, -2, 9), (3, 4, 3), "flank"),
                ],
                "children": {
                    # Upright and lopsided, the top lobe the longer: a shark's, not a dolphin's.
                    "tail_fin": {
                        "pivot": [0, 0, 11],
                        "boxes": [
                            ((42, 8), (-0.5, -3, 0), (1, 3, 3), "fin"),
                            ((50, 8), (-0.5, -6, 1.5), (1, 3, 3), "fin"),
                            ((58, 8), (-0.5, -9, 3), (1, 3, 2), "fin"),
                            # Both lobes sweep back; the lower one shorter.
                            ((42, 14), (-0.5, 0, 0.5), (1, 2, 3), "fin"),
                            ((50, 14), (-0.5, 2, 2), (1, 2, 2), "fin"),
                        ],
                    },
                },
            },
        },
    },
}

# How the shark moves. The body turns to look; at rest it idles, on the move it swims, by the
# distance it covers (pace) and taking over from the idle as it speeds up (strength); it bites.
ANIMATION = {
    "look": "body",
    "idle": "cobble-jaws-justfatlard:shark_idle",
    "moving": {"animation": "cobble-jaws-justfatlard:shark_swim", "pace": 2.5, "strength": 3},
    "attack": "cobble-jaws-justfatlard:shark_bite",
}

BACK_DARK, BACK_LIGHT = (44, 52, 60), (88, 98, 108)
BELLY_DARK, BELLY_LIGHT = (205, 209, 207), (240, 242, 238)
FIN_DARK, FIN_LIGHT = (32, 38, 45), (64, 72, 80)
EYE = (12, 12, 16)
GILL = (30, 34, 40)
MOUTH = (40, 20, 24)
TOOTH = (250, 250, 246)

# How far down a flank the belly reaches, from the bottom, as a share of the flank's height.
BELLY_LINE = 0.45


def boxes(parts):
    for part in parts.values():
        for box in part["boxes"]:
            yield box
        yield from boxes(part.get("children", {}))


def model_json(parts):
    out = {}
    for name, part in parts.items():
        entry = {}
        if "pivot" in part:
            entry["pivot"] = part["pivot"]
        if "rotation" in part:
            entry["rotation"] = part["rotation"]
        entry["cubes"] = [
            {"uv": list(box[0]), "origin": list(box[1]), "size": list(box[2]),
             **({"mirror": True} if len(box) > 4 and box[4] else {})}
            for box in part["boxes"]
        ]
        if part.get("children"):
            entry["children"] = model_json(part["children"])
        out[name] = entry
    return out


def ramp(dark, light, t):
    return tuple(round(dark[i] + (light[i] - dark[i]) * t) for i in range(3)) + (255,)


def shade(x, y):
    """A little grain, the same every run: flat colour reads as plastic at this size."""
    return ((x * 7 + y * 13) % 5) / 20


def paint(sheet, uv, size, kind):
    (u, v), (w, h, d) = uv, size
    px = sheet.load()

    def fill(x0, y0, width, height, colour):
        for y in range(y0, y0 + height):
            for x in range(x0, x0 + width):
                px[x, y] = colour(x, y, y - y0, height)

    fin = kind == "fin"
    top = (lambda x, y, row, rows: ramp(FIN_DARK, FIN_LIGHT, shade(x, y))) if fin \
        else (lambda x, y, row, rows: ramp(BACK_DARK, BACK_LIGHT, 0.2 + shade(x, y)))
    under = (lambda x, y, row, rows: ramp(FIN_DARK, FIN_LIGHT, 0.5 + shade(x, y))) if fin \
        else (lambda x, y, row, rows: ramp(BELLY_DARK, BELLY_LIGHT, 0.5 + shade(x, y)))

    def side(x, y, row, rows):
        if fin:
            return ramp(FIN_DARK, FIN_LIGHT, row / max(1, rows - 1) * 0.6 + shade(x, y))
        line = rows - max(1, round(rows * BELLY_LINE))
        if row >= line:
            return ramp(BELLY_DARK, BELLY_LIGHT, 0.4 + shade(x, y))
        return ramp(BACK_DARK, BACK_LIGHT, row / max(1, line) * 0.7 + shade(x, y))

    fill(u + d, v, w, d, top)
    fill(u + d + w, v, w, d, under)
    fill(u, v + d, 2 * (d + w), h, side)

    right, front, left = u, u + d, u + d + w
    if kind == "head":
        # Eyes high and forward, one on each flank, each at its own strip's front end.
        px[right + d - 2, v + d + 2] = EYE + (255,)
        px[left + 1, v + d + 2] = EYE + (255,)
        # The mouth, under the snout on the head's front face: a dark line and a row of teeth.
        for x in range(front, front + w):
            px[x, v + d + h - 2] = MOUTH + (255,)
            px[x, v + d + h - 1] = TOOTH + (255,) if (x - front) % 2 == 0 else MOUTH + (255,)
    if kind == "snout":
        # Nostrils on its front face, just above where the mouth begins.
        px[front + 1, v + d + 1] = GILL + (255,)
        px[front + w - 2, v + d + 1] = GILL + (255,)
    if kind == "flank" and d >= 13:
        # Gill slits just behind the head, three to a side, in the dark half of the flank.
        for slit in range(3):
            for row in range(1, 4):
                px[right + d - 2 - 2 * slit, v + d + row] = GILL + (255,)
                px[left + 1 + 2 * slit, v + d + row] = GILL + (255,)


def write_model():
    path = ASSETS / "pandorical/entity_models/shark.json"
    path.parent.mkdir(parents=True, exist_ok=True)
    model = {
        "texture": "cobble-jaws-justfatlard:textures/entity/shark.png",
        "texture_size": list(SHEET),
        "parts": model_json(SHARK),
        "animation": ANIMATION,
    }
    path.write_text(json.dumps(model, indent=2) + "\n")
    print(path.relative_to(HERE))


def write_skin():
    sheet = Image.new("RGBA", SHEET, (0, 0, 0, 0))
    seen = set()
    for box in boxes(SHARK):
        uv, _, size, kind = box[:4]
        if uv in seen:
            continue  # a mirrored pair shares its patch of the sheet
        seen.add(uv)
        paint(sheet, uv, size, kind)
    path = ASSETS / "textures/entity/shark.png"
    path.parent.mkdir(parents=True, exist_ok=True)
    sheet.save(path)
    print(path.relative_to(HERE))


EGG_SOURCE = "assets/minecraft/textures/item/dolphin_spawn_egg.png"
# Above this a pixel of the egg is a spot, and the spots are belly white.
EGG_SPOT = 200


def write_egg():
    with zipfile.ZipFile(CLIENT_JAR) as jar, jar.open(EGG_SOURCE) as handle:
        egg = Image.open(handle).convert("RGBA")
        egg.load()
    out = Image.new("RGBA", egg.size)
    src, dst = egg.load(), out.load()
    for y in range(egg.height):
        for x in range(egg.width):
            r, g, b, a = src[x, y]
            if a == 0:
                dst[x, y] = (0, 0, 0, 0)
                continue
            luma = (r * 299 + g * 587 + b * 114) // 1000
            dark, light = (BELLY_DARK, BELLY_LIGHT) if luma >= EGG_SPOT else (BACK_DARK, BACK_LIGHT)
            dst[x, y] = ramp(dark, light, min(1.0, luma / 255))
    path = ASSETS / "textures/item/shark_spawn_egg.png"
    out.save(path)
    print(path.relative_to(HERE))


if __name__ == "__main__":
    write_model()
    write_skin()
    write_egg()
