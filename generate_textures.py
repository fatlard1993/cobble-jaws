#!/usr/bin/env python3
"""
The shark's skin, adult and pup, and its spawn egg, recoloured from the dolphin's own textures.

Drawn from vanilla so every pixel lands where the dolphin model expects it, and the shading the
dolphin already has survives: each pixel keeps where it sat between the dolphin's darkest and
lightest blue, and only the ramp it sits on changes.

Which ramp is decided by the model, not by colour. A dolphin is nearly one blue all over, so its
belly cannot be found by looking for the pale pixels; it is found by knowing where each box's
underside and lower flanks are laid out on the sheet. That is what turns a grey dolphin into a
shark: a dark back over a white belly, with the line between them running down the flanks.

The boxes below are copied from DolphinModel and BabyDolphinModel. If Mojang moves a box, its belly
lands somewhere else and the shark comes out patchy, which is visible the first time anyone looks.
"""

import zipfile
from pathlib import Path

from PIL import Image

CLIENT_JAR = Path.home() / ".gradle/caches/fabric-loom/26.3/minecraft-client-only.jar"
SOURCE = "assets/minecraft/textures/entity/dolphin/{}.png"
OUT = Path(__file__).parent / "src/main/resources/assets/cobble-jaws-justfatlard/textures/entity"

# Darkest and lightest of each ramp.
BACK = ((46, 54, 62), (98, 108, 117))
BELLY = ((196, 200, 199), (238, 240, 236))
FIN = ((34, 40, 47), (72, 80, 88))

# The dolphin's own range, so its shading maps onto the whole of each ramp.
DOLPHIN_DARK = 125
DOLPHIN_LIGHT = 215

# Below this a pixel is an eye, not skin, and is left exactly as it is.
FEATURE = 90

# How far down a flank the belly comes up, as a fraction of the box's height from the bottom.
BELLY_LINE = 0.43

# (u, v, width, height, depth, fin). Width runs across the animal, height up and down it, depth
# nose to tail.
ADULT = [
    (0, 0, 8, 7, 6, False),     # head
    (0, 13, 2, 2, 4, False),    # nose
    (22, 0, 8, 7, 13, False),   # body
    (0, 19, 4, 5, 11, False),   # tail
    (19, 20, 10, 1, 6, False),  # tail fin
    (51, 0, 1, 4, 5, True),     # back fin
    (48, 20, 1, 4, 7, True),    # flippers
]
PUP = [
    (0, 0, 6, 5, 4, False),
    (0, 9, 2, 2, 2, False),
    (20, 0, 6, 5, 8, False),
    (0, 13, 4, 3, 7, False),
    (22, 13, 8, 1, 4, False),
    (42, 0, 1, 3, 4, True),
    (34, 18, 1, 3, 6, True),
    (48, 18, 1, 3, 6, True),
]


def regions(boxes):
    """Every pixel each box owns on the sheet, and which ramp it takes."""
    ramp = {}
    for u, v, w, h, d, fin in boxes:
        if fin:
            for x in range(u, u + 2 * (d + w)):
                for y in range(v, v + d + h):
                    ramp[(x, y)] = FIN
            continue
        # The two caps: the top is back, the underside is belly.
        for x in range(u + d, u + d + w):
            for y in range(v, v + d):
                ramp[(x, y)] = BACK
        for x in range(u + d + w, u + d + 2 * w):
            for y in range(v, v + d):
                ramp[(x, y)] = BELLY
        # The four sides, top rows first: belly comes up from the bottom to the line.
        line = v + d + h - max(1, round(h * BELLY_LINE))
        for x in range(u, u + 2 * (d + w)):
            for y in range(v + d, v + d + h):
                ramp[(x, y)] = BELLY if y >= line else BACK
    return ramp


def recolour(source: Image.Image, boxes) -> Image.Image:
    ramp = regions(boxes)
    out = Image.new("RGBA", source.size)
    src = source.convert("RGBA").load()
    dst = out.load()
    for y in range(source.height):
        for x in range(source.width):
            r, g, b, a = src[x, y]
            if a == 0:
                dst[x, y] = (0, 0, 0, 0)
                continue
            luma = (r * 299 + g * 587 + b * 114) // 1000
            if luma < FEATURE or (r, g, b) == (255, 255, 255):
                dst[x, y] = (r, g, b, a)
                continue
            dark, light = ramp.get((x, y), BACK)
            t = min(1.0, max(0.0, (luma - DOLPHIN_DARK) / (DOLPHIN_LIGHT - DOLPHIN_DARK)))
            dst[x, y] = tuple(round(dark[i] + (light[i] - dark[i]) * t) for i in range(3)) + (a,)
    return out


EGG_SOURCE = "assets/minecraft/textures/item/dolphin_spawn_egg.png"
EGG_OUT = Path(__file__).parent / "src/main/resources/assets/cobble-jaws-justfatlard/textures/item"
# Above this a pixel of the egg is a spot, and the spots are belly white.
EGG_SPOT = 200


def recolour_egg(source: Image.Image) -> Image.Image:
    out = Image.new("RGBA", source.size)
    src = source.convert("RGBA").load()
    dst = out.load()
    for y in range(source.height):
        for x in range(source.width):
            r, g, b, a = src[x, y]
            if a == 0:
                dst[x, y] = (0, 0, 0, 0)
                continue
            luma = (r * 299 + g * 587 + b * 114) // 1000
            dark, light = BELLY if luma >= EGG_SPOT else BACK
            t = min(1.0, max(0.0, luma / 255))
            dst[x, y] = tuple(round(dark[i] + (light[i] - dark[i]) * t) for i in range(3)) + (a,)
    return out


def main() -> None:
    OUT.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(CLIENT_JAR) as jar:
        for source, boxes, name in (("dolphin", ADULT, "shark"), ("dolphin_baby", PUP, "shark_pup")):
            with jar.open(SOURCE.format(source)) as handle:
                dolphin = Image.open(handle).convert("RGBA")
                dolphin.load()
            path = OUT / f"{name}.png"
            recolour(dolphin, boxes).save(path)
            print(f"{path.relative_to(Path(__file__).parent)}  {dolphin.width}x{dolphin.height}")

        # The egg has no boxes: its pale spots are the belly and the rest is back.
        with jar.open(EGG_SOURCE) as handle:
            egg = Image.open(handle).convert("RGBA")
            egg.load()
        EGG_OUT.mkdir(parents=True, exist_ok=True)
        path = EGG_OUT / "shark_spawn_egg.png"
        recolour_egg(egg).save(path)
        print(f"{path.relative_to(Path(__file__).parent)}  {egg.width}x{egg.height}")


if __name__ == "__main__":
    main()
