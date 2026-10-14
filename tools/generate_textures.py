#!/usr/bin/env python3
"""Generates the textures of the mod (pixel art drawn in code, needs Pillow).

Usage: python3 tools/generate_textures.py <minecraft client or merged jar>

The woodcutter textures are the vanilla stonecutter textures with the stone replaced by
oak planks, so they are read from the Minecraft jar. The chopping block uses the vanilla
iron axe texture (copied, block models cannot use the item atlas) and its model is built
from the axe pixels, so it also needs the jar. Everything else is drawn from scratch.

The carpenter's bench model and its vise texture are not generated here; they are edited
by hand (e.g. in Blockbench).
"""
import json
import pathlib
import sys
import zipfile
from io import BytesIO

from PIL import Image

ROOT = pathlib.Path(__file__).resolve().parent.parent
TEXTURES = ROOT / "common/src/main/resources/assets/workingvillagers/textures"


def blank(size):
    return Image.new("RGBA", (size, size), (0, 0, 0, 0))


def work_contract():
    """Parchment with text lines, an emerald in the corner and a red wax seal."""
    image = blank(16)
    p = image.load()
    paper, edge, ink = (236, 224, 186, 255), (186, 160, 112, 255), (70, 60, 50, 255)
    seal, seal_highlight, emerald = (178, 34, 34, 255), (220, 70, 60, 255), (23, 185, 80, 255)
    for y in range(1, 15):
        for x in range(3, 13):
            p[x, y] = edge if x in (3, 12) or y in (1, 14) else paper
    for y in (4, 6, 8):
        for x in range(5, 11):
            p[x, y] = ink
    for x in range(5, 8):
        p[x, 10] = ink
    for x, y in [(9, 11), (10, 11), (11, 11), (9, 12), (10, 12), (11, 12), (10, 13)]:
        p[x, y] = seal
    p[9, 11] = seal_highlight
    p[5, 2] = p[6, 2] = emerald
    return image


def lumberjack():
    """Villager profession overlay (64x64, vanilla villager UV layout): red flannel shirt,
    suspenders, belt and a red beanie."""
    image = blank(64)
    p = image.load()
    red, dark, black = (170, 40, 35, 255), (90, 20, 20, 255), (40, 30, 30, 255)
    brown, buckle, cuff = (95, 60, 30, 255), (200, 170, 80, 255), (130, 30, 28, 255)

    def flannel(x, y):
        a, b = (x // 2) % 2, (y // 2) % 2
        return black if a and b else dark if a or b else red

    def fill(x0, y0, x1, y1, color):
        for y in range(y0, y1):
            for x in range(x0, x1):
                p[x, y] = color(x, y)

    fill(16, 20, 44, 38, flannel)  # body
    fill(44, 22, 60, 34, flannel)  # crossed arms
    fill(40, 38, 64, 46, flannel)  # crossed arms, middle part
    fill(6, 38, 14, 44, flannel)  # jacket top
    fill(0, 44, 28, 56, flannel)  # jacket, upper part of the sides
    for y in range(44, 56):
        for x in (7, 12, 21, 26):
            p[x, y] = brown  # suspenders on the jacket
    for y in range(26, 38):
        for x in (18, 23, 30, 35):
            p[x, y] = brown  # suspenders on the body
    for x in range(0, 28):
        p[x, 56] = brown  # belt
    p[9, 56] = p[10, 56] = buckle
    fill(40, 0, 48, 8, lambda x, y: red)  # beanie top
    fill(32, 8, 64, 12, lambda x, y: red if (x + y) % 3 else cuff)
    fill(32, 12, 64, 13, lambda x, y: cuff)
    return image


def apron_worker(cloth, cloth_dark, dust, cap, cap_dark, goggles=False):
    """Villager profession overlay (64x64, vanilla villager UV layout): a work apron with
    dust on it and a cap; optionally safety goggles on the cap."""
    image = blank(64)
    p = image.load()
    strap = (70, 50, 35, 255)

    def apron(x, y):
        if (x * 7 + y * 13) % 11 == 0:
            return dust
        return cloth_dark if y % 4 == 3 else cloth

    for y in range(46, 60):
        for x in range(6, 14):
            p[x, y] = apron(x, y)  # apron on the front of the robe
    for y in range(28, 38):
        for x in range(23, 29):
            p[x, y] = apron(x, y)  # bib on the front of the body
    for y in range(20, 28):
        p[23, y] = p[28, y] = strap  # neck straps
    for x in range(16, 44):
        p[x, 36] = strap  # apron string around the waist
    for x in range(0, 28):
        p[x, 45] = strap
    for y in range(0, 8):
        for x in range(40, 48):
            p[x, y] = cap if (x + y) % 5 else cap_dark  # cap top
    for y in range(8, 11):
        for x in range(32, 64):
            p[x, y] = cap if y < 10 else cap_dark  # cap band
    if goggles:
        glass, frame = (120, 180, 200, 255), (40, 40, 45, 255)
        for x in range(40, 48):
            p[x, 11] = frame
        for x in (41, 42, 45, 46):
            p[x, 11] = glass
        for x in list(range(32, 40)) + list(range(48, 64)):
            p[x, 11] = frame  # strap around the head
    return image


def wood_sawyer():
    """Leather apron covered in sawdust and a green cap."""
    return apron_worker(
        (120, 80, 45, 255), (100, 65, 35, 255), (225, 200, 150, 255), (60, 110, 60, 255), (40, 80, 40, 255)
    )


def stone_sawyer():
    """Grey canvas apron covered in stone dust, a dark cap and safety goggles."""
    return apron_worker(
        (110, 110, 115, 255), (90, 90, 95, 255), (200, 200, 200, 255), (70, 70, 80, 255), (50, 50, 60, 255), goggles=True
    )


def carpenter():
    """Blue canvas apron covered in wood shavings and a red cap."""
    return apron_worker(
        (70, 95, 140, 255), (55, 75, 115, 255), (235, 210, 150, 255), (150, 45, 40, 255), (110, 32, 30, 255)
    )


WICKER_LIGHT, WICKER_DARK, WICKER_SHADOW = (205, 165, 105, 255), (160, 118, 68, 255), (110, 78, 42, 255)


def wicker(x, y):
    """Woven pattern: two pixel wide strands, alternating every row."""
    return WICKER_LIGHT if (x // 2 + y) % 2 else WICKER_DARK


def wicker_basket_item():
    """A basket with a woven body, a darker rim and two carrying straps."""
    image = blank(16)
    p = image.load()
    strap = (90, 60, 35, 255)
    for y in range(5, 15):
        inset = (y - 5) // 4  # slightly narrower towards the bottom
        for x in range(2 + inset, 14 - inset):
            p[x, y] = WICKER_SHADOW if x in (2 + inset, 13 - inset) or y == 14 else wicker(x, y)
    for x in range(1, 15):
        p[x, 4] = WICKER_SHADOW
        p[x, 5] = WICKER_DARK
    for y in range(1, 4):
        p[4, y] = p[11, y] = strap
    for x in range(4, 12):
        p[x, 1] = strap
    return image


def wicker_basket_entity():
    """Texture of the basket model on the carrier's back (32x32, box UV layout): the body
    box 7x9x4 at (0, 0), the rim 8x1x5 at (0, 14)."""
    image = blank(32)
    p = image.load()
    for y in range(0, 13):
        for x in range(0, 22):
            p[x, y] = wicker(x, y)
    for y in range(0, 4):
        for x in range(4, 11):
            p[x, y] = WICKER_SHADOW  # inside, seen from above
    for y in range(14, 20):
        for x in range(0, 26):
            p[x, y] = WICKER_DARK if (x + y) % 3 else WICKER_SHADOW
    return image


def carrier():
    """Villager profession overlay: leather straps of the basket over the shoulders, on the
    front, top and back of the robe, and a straw hat band."""
    image = blank(64)
    p = image.load()
    strap, buckle = (90, 60, 35, 255), (200, 170, 80, 255)
    for y in range(38, 44):
        p[7, y] = p[12, y] = strap  # robe top (shoulders)
    for y in range(44, 53):
        p[7, y] = p[12, y] = strap  # robe front
    for y in range(44, 55):
        p[21, y] = p[26, y] = strap  # robe back, down to the basket
    p[7, 50] = p[12, 50] = buckle
    for x in range(32, 64):
        p[x, 9] = (215, 190, 120, 255)  # hat band
    return image


def woodcutter(jar):
    """The stonecutter textures with oak planks instead of stone. Grey (stone) pixels take
    the planks pixel at the same position, keeping the brightness of the stone pixel so
    the shading stays. The wooden frame and the saw slot on top are kept as they are."""
    def read(name):
        with zipfile.ZipFile(jar) as z:
            return Image.open(BytesIO(z.read(f"assets/minecraft/textures/block/{name}.png"))).convert("RGBA")

    planks = read("oak_planks")
    result = {}
    for part in ("side", "top", "bottom"):
        image = read(f"stonecutter_{part}")
        stone = [image.getpixel((x, y)) for x in range(16) for y in range(16)]
        grey = [c for c in stone if c[3] and max(c[:3]) - min(c[:3]) < 24]
        stone_brightness = sum(sum(c[:3]) for c in grey) / max(len(grey), 1) / 3
        p = image.load()
        for y in range(16):
            for x in range(16):
                r, g, b, a = p[x, y]
                if not a or max(r, g, b) - min(r, g, b) >= 24:
                    continue  # transparent or part of the wooden frame
                factor = ((r + g + b) / 3) / stone_brightness
                if part == "top" and factor < 0.8:
                    continue  # the saw slot stays a dark slot
                pr, pg, pb, _ = planks.getpixel((x, y))
                p[x, y] = tuple(min(255, int(c * factor)) for c in (pr, pg, pb)) + (255,)
        result[part] = image
    return result


def read_vanilla(jar, path):
    with zipfile.ZipFile(jar) as z:
        return Image.open(BytesIO(z.read(f"assets/minecraft/textures/{path}.png"))).convert("RGBA")


def chopping_block_model(axe):
    """A stump with the iron axe stuck in it, blade down. Every row of axe pixels becomes a
    1 pixel thick box, like the extruded item model. The axe texture is turned by 180
    degrees: the head ends up at the bottom with its edge sunk into the stump and the
    handle sticks out diagonally."""
    dx, dy = 2, 6  # moves the head over the middle of the stump, the edge 2 pixels deep
    elements = [{
        "name": "stump",
        "from": [2, 0, 2], "to": [14, 9, 14],
        "faces": {
            **{side: {"uv": [2, 7, 14, 16], "texture": "#bark"} for side in ("north", "east", "south", "west")},
            "up": {"uv": [2, 2, 14, 14], "texture": "#rings"},
            "down": {"uv": [2, 2, 14, 14], "texture": "#rings", "cullface": "down"},
        },
    }]
    for v in range(16):
        u = 0
        while u < 16:
            if not axe.getpixel((u, v))[3]:
                u += 1
                continue
            start = u
            while u < 16 and axe.getpixel((u, v))[3]:
                u += 1
            ua, ub = start, u - 1  # run of opaque pixels in this row
            x0, x1 = 15 - ub + dx, 16 - ua + dx
            y0 = v + dy
            elements.append({
                "from": [x0, y0, 7.5], "to": [x1, y0 + 1, 8.5],
                "faces": {
                    "south": {"uv": [ub + 1, v + 1, ua, v], "texture": "#axe"},
                    "north": {"uv": [ua, v + 1, ub + 1, v], "texture": "#axe"},
                    "up": {"uv": [ub + 1, v, ua, v + 1], "texture": "#axe"},
                    "down": {"uv": [ub + 1, v, ua, v + 1], "texture": "#axe"},
                    "east": {"uv": [ua, v, ua + 1, v + 1], "texture": "#axe"},
                    "west": {"uv": [ub, v, ub + 1, v + 1], "texture": "#axe"},
                },
            })
    return {
        "parent": "minecraft:block/block",
        "textures": {
            "particle": "minecraft:block/oak_log",
            "bark": "minecraft:block/oak_log",
            "rings": "minecraft:block/oak_log_top",
            "axe": "workingvillagers:block/chopping_block_axe",
        },
        "elements": elements,
    }


def main():
    if len(sys.argv) != 2:
        sys.exit(__doc__)
    jar = sys.argv[1]
    outputs = {
        "item/work_contract.png": work_contract(),
        "entity/villager/profession/lumberjack.png": lumberjack(),
        "entity/zombie_villager/profession/lumberjack.png": lumberjack(),
        "item/wicker_basket.png": wicker_basket_item(),
        "entity/wicker_basket.png": wicker_basket_entity(),
        "entity/villager/profession/carrier.png": carrier(),
        "entity/zombie_villager/profession/carrier.png": carrier(),
        "entity/villager/profession/woodcutter.png": wood_sawyer(),
        "entity/zombie_villager/profession/woodcutter.png": wood_sawyer(),
        "entity/villager/profession/stonecutter.png": stone_sawyer(),
        "entity/zombie_villager/profession/stonecutter.png": stone_sawyer(),
        "entity/villager/profession/carpenter.png": carpenter(),
        "entity/zombie_villager/profession/carpenter.png": carpenter(),
    }
    for part, image in woodcutter(jar).items():
        outputs[f"block/woodcutter_{part}.png"] = image
    axe = read_vanilla(jar, "item/iron_axe")
    outputs["block/chopping_block_axe.png"] = axe
    model = ROOT / "common/src/main/resources/assets/workingvillagers/models/block/chopping_block.json"
    model.write_text(json.dumps(chopping_block_model(axe), indent=2) + "\n")
    print("wrote", model.relative_to(ROOT))
    for path, image in outputs.items():
        target = TEXTURES / path
        target.parent.mkdir(parents=True, exist_ok=True)
        image.save(target)
        print("wrote", target.relative_to(ROOT))


if __name__ == "__main__":
    main()
