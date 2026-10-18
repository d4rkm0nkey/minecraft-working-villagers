#!/usr/bin/env python3
"""Generates the textures of the mod (pixel art drawn in code, needs Pillow).

Usage: python3 tools/generate_textures.py <minecraft client or merged jar>

The woodcutter textures are the vanilla stonecutter textures with the stone replaced by
oak planks, the kiln textures the furnace textures with bricks instead of cobblestone, the
village chest the chest texture in spruce with iron bands and an emerald lock, so they are
read from the Minecraft jar. The chopping block uses the vanilla
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


def hat_rim(p, color, reach, front=0):
    """Paints the hat rim of the villager model, a 16x16 plate around the head, as a brim
    `reach` pixels wide with `front` more pixels at the front. The model tips the plate
    forward, so the bottom rows of its texture are at the front; it is painted on both
    faces so it shows from above and below."""
    for x0 in (31, 48):
        for v in range(16):
            for u in range(16):
                side = max(4 - u, u - 11)
                if 4 <= u < 12 and 4 <= v < 12:
                    continue  # inside the head
                if side <= reach and 4 - v <= reach and v - 11 <= reach + front:
                    p[x0 + u, 48 + v] = color(u, v)


def wood_sawyer():
    """Villager profession overlay: an olive flat cap with a short peak (the front of the
    hat rim); an olive work shirt with sawdust on it, leather braces, brown trousers and
    boots."""
    image = blank(64)
    p = image.load()
    olive, olive_dark, olive_light = (120, 130, 70, 255), (95, 104, 52, 255), (140, 150, 88, 255)
    cap, cap_dark, cap_fleck = (78, 86, 45, 255), (58, 64, 32, 255), (105, 112, 70, 255)
    sawdust, braces, buckle = (230, 205, 150, 255), (100, 62, 34, 255), (200, 170, 80, 255)
    trousers, trousers_dark, boots = (105, 75, 48, 255), (85, 60, 38, 255), (45, 34, 26, 255)

    def fill(x0, y0, x1, y1, color):
        for y in range(y0, y1):
            for x in range(x0, x1):
                p[x, y] = color(x, y)

    shirt = lambda x, y: sawdust if (x * 7 + y * 13) % 17 == 0 else olive_dark if y % 4 == 3 else olive
    trouser = lambda x, y: sawdust if (x * 5 + y * 3) % 19 == 0 else trousers_dark if x % 3 == 0 else trousers
    fill(16, 20, 44, 38, shirt)  # body
    fill(6, 38, 14, 44, shirt)  # jacket top
    fill(0, 44, 28, 54, shirt)  # jacket, upper part
    fill(0, 54, 28, 64, trouser)  # jacket, lower part: trousers
    fill(0, 54, 28, 55, lambda x, y: braces)  # waistband
    for y in range(38, 54):
        for x in ((7, 12) if y < 44 else (7, 12, 21, 26)):
            p[x, y] = braces  # braces over the shoulders, down the front and back
    for y in range(20, 38):
        p[23, y] = p[28, y] = p[38, y] = p[41, y] = braces  # braces on the body
    p[7, 52] = p[12, 52] = buckle
    fill(44, 22, 60, 34, shirt)  # arms
    fill(40, 38, 64, 46, shirt)  # crossed arms, middle part
    fill(0, 22, 16, 34, trouser)  # legs
    fill(0, 34, 16, 38, lambda x, y: boots)
    tweed = lambda x, y: cap_fleck if (x * 3 + y * 5) % 7 == 0 else cap
    fill(40, 0, 48, 8, tweed)  # cap top
    fill(32, 8, 64, 10, tweed)  # cap sides
    fill(32, 10, 64, 11, lambda x, y: cap_dark)
    fill(40, 8, 48, 9, lambda x, y: cap_dark)  # seam where the crown is pulled over the peak
    hat_rim(p, lambda u, v: cap_dark if v == 13 else cap, 0, front=2)  # peak
    return image


def stone_sawyer():
    """Villager profession overlay: a ribbed grey wool cap with safety goggles on it and a
    dust mask over the nose; a light grey smock covered in stone dust with a belt, dark
    grey trousers and boots."""
    image = blank(64)
    p = image.load()
    smock, smock_dark, dust = (150, 150, 155, 255), (125, 125, 130, 255), (215, 215, 212, 255)
    wool, wool_dark = (95, 98, 105, 255), (72, 75, 82, 255)
    glass, glass_light, frame = (110, 170, 195, 255), (175, 220, 235, 255), (40, 40, 45, 255)
    mask, mask_dark = (228, 226, 218, 255), (195, 193, 185, 255)
    belt, buckle = (60, 50, 45, 255), (170, 170, 175, 255)
    trousers, boots = (75, 75, 82, 255), (40, 38, 38, 255)

    def fill(x0, y0, x1, y1, color):
        for y in range(y0, y1):
            for x in range(x0, x1):
                p[x, y] = color(x, y)

    work = lambda x, y: dust if (x * x + y * 3) % 7 == 0 else smock_dark if y % 4 == 3 else smock
    fill(16, 20, 44, 38, work)  # body
    fill(6, 38, 14, 44, work)  # jacket top
    fill(0, 44, 28, 60, work)  # jacket: the smock
    fill(0, 60, 28, 64, lambda x, y: trousers)  # jacket, lower part
    fill(0, 52, 28, 53, lambda x, y: belt)  # belt
    p[9, 52] = p[10, 52] = buckle
    fill(44, 22, 60, 34, work)  # arms
    fill(40, 38, 64, 46, work)  # crossed arms, middle part
    fill(0, 22, 16, 34, lambda x, y: trousers)  # legs
    fill(0, 34, 16, 38, lambda x, y: boots)
    fill(40, 0, 48, 8, lambda x, y: wool_dark if x % 2 else wool)  # wool cap top
    fill(32, 8, 64, 12, lambda x, y: wool_dark if x % 2 else wool)  # ribbed sides
    fill(32, 11, 64, 12, lambda x, y: wool_dark)  # turned up edge
    fill(32, 10, 64, 11, lambda x, y: frame)  # goggle strap
    fill(41, 9, 47, 11, lambda x, y: frame)  # goggles pushed up on the cap
    p[42, 9] = p[45, 9] = glass_light
    p[42, 10] = p[43, 10] = p[45, 10] = p[46, 10] = glass
    p[43, 9] = p[46, 9] = glass
    fill(24, 0, 32, 6, lambda x, y: mask_dark if y == 5 else mask)  # dust mask over the nose
    fill(32, 15, 40, 16, lambda x, y: mask_dark)  # its strings around the head
    fill(48, 15, 64, 16, lambda x, y: mask_dark)
    return image


def carpenter():
    """Villager profession overlay: a folded paper hat with a pencil behind the ear; a sand
    coloured work smock with wood shavings on it, a short leather tool apron at the waist
    with a yellow folding rule in its pocket, brown corduroy trousers and boots."""
    image = blank(64)
    p = image.load()
    smock, smock_dark, shavings = (215, 195, 150, 255), (185, 165, 122, 255), (240, 225, 185, 255)
    leather, leather_dark = (125, 82, 48, 255), (92, 58, 32, 255)
    rule, rule_mark = (235, 195, 45, 255), (60, 50, 30, 255)
    cord, cord_dark, boots = (100, 72, 48, 255), (80, 56, 36, 255), (45, 35, 28, 255)
    paper, paper_fold, print_ = (238, 236, 228, 255), (200, 198, 190, 255), (150, 150, 150, 255)
    wood, graphite = (225, 185, 60, 255), (50, 50, 55, 255)

    def fill(x0, y0, x1, y1, color):
        for y in range(y0, y1):
            for x in range(x0, x1):
                p[x, y] = color(x, y)

    work = lambda x, y: shavings if (x * 7 + y * 11) % 13 == 0 else smock_dark if y % 4 == 3 else smock
    fill(16, 20, 44, 38, work)  # body
    fill(6, 38, 14, 44, work)  # jacket top
    fill(0, 44, 28, 56, work)  # jacket, upper part
    fill(0, 56, 28, 64, lambda x, y: cord_dark if x % 2 else cord)  # jacket, lower part: trousers
    fill(0, 52, 28, 53, lambda x, y: leather_dark)  # belt of the tool apron
    fill(5, 53, 15, 59, lambda x, y: leather)  # tool apron on the front of the robe
    fill(5, 58, 15, 59, lambda x, y: leather_dark)  # hem
    fill(6, 55, 14, 56, lambda x, y: leather_dark)  # pocket seam
    p[9, 55] = p[10, 55] = leather  # the pockets are split in the middle
    for y in range(50, 56):
        p[12, y] = rule_mark if y % 2 else rule  # folding rule standing in the pocket
    p[7, 54] = p[7, 55] = graphite  # a pencil in the other pocket
    p[7, 53] = wood
    fill(44, 22, 60, 34, work)  # arms
    fill(40, 38, 64, 46, work)  # crossed arms, middle part
    fill(0, 22, 16, 34, lambda x, y: cord_dark if x % 2 else cord)  # legs
    fill(0, 34, 16, 38, lambda x, y: boots)
    fill(40, 0, 48, 8, lambda x, y: paper_fold if x == 44 else print_ if y in (2, 5) and x in (41, 42, 46) else paper)  # hat top, a folded newspaper
    fill(32, 8, 64, 11, lambda x, y: paper)  # hat sides
    fill(32, 11, 64, 12, lambda x, y: paper_fold)  # folded edge
    for x0 in (40, 56):  # the fold forms a point on the front and the back
        for i in range(3):
            p[x0 + 3 - i, 8 + i] = p[x0 + 4 + i, 8 + i] = paper_fold
    for x in range(34, 38):
        p[x, 13] = wood  # pencil behind the ear
    p[33, 13] = graphite
    return image


def smith():
    """Villager profession overlay: no hat, only a leather headband; a heavy black leather
    apron down to the knees with ember burns over a linen shirt with rolled up sleeves,
    leather gloves, dark trousers and boots."""
    image = blank(64)
    p = image.load()
    leather, leather_dark, leather_light = (42, 38, 36, 255), (28, 25, 24, 255), (66, 60, 54, 255)
    linen, linen_dark = (160, 145, 118, 255), (132, 118, 92, 255)
    glove, glove_dark = (110, 72, 42, 255), (82, 52, 30, 255)
    trousers, boots, strap = (58, 54, 60, 255), (32, 27, 25, 255), (95, 60, 30, 255)
    ember, ember_bright = (225, 105, 35, 255), (250, 190, 80, 255)

    def fill(x0, y0, x1, y1, color):
        for y in range(y0, y1):
            for x in range(x0, x1):
                p[x, y] = color(x, y)

    shirt = lambda x, y: linen_dark if (x * 3 + y * 5) % 9 == 0 else linen
    apron = lambda x, y: leather_light if (x * 7 + y * 3) % 10 == 0 else leather_dark if y % 5 == 4 else leather
    fill(16, 20, 44, 38, shirt)  # body
    fill(22, 27, 30, 38, apron)  # bib on the front of the body
    fill(6, 38, 14, 44, shirt)  # jacket top
    fill(0, 44, 28, 52, shirt)  # jacket, upper part
    fill(0, 52, 28, 64, lambda x, y: trousers)  # jacket, lower part
    fill(5, 44, 15, 62, apron)  # apron on the front of the robe, wrapping around the edges
    for x in range(0, 28):
        if not 5 <= x < 15:
            p[x, 52] = strap  # apron string around the waist
    for y in range(20, 27):
        p[23, y] = p[28, y] = strap  # neck straps over the shoulders
    for i in range(8):
        p[36 + i, 26 + i] = p[43 - i, 26 + i] = strap  # straps crossed on the back
    for x, y, color in [(7, 48, ember), (12, 51, ember_bright), (9, 55, ember), (13, 58, ember),
                        (6, 59, ember_bright), (10, 60, ember), (25, 31, ember), (27, 35, ember_bright)]:
        p[x, y] = color  # burns from flying sparks
    fill(44, 22, 60, 31, shirt)  # arms: rolled up sleeves
    fill(44, 30, 60, 31, lambda x, y: linen_dark)  # rolled up cuff
    fill(44, 31, 60, 34, lambda x, y: glove)  # gloves
    fill(52, 22, 56, 26, lambda x, y: glove)  # end of the arm
    fill(40, 38, 64, 46, lambda x, y: glove_dark if (x + y) % 4 == 0 else glove)  # crossed arms, middle part
    fill(0, 22, 16, 34, lambda x, y: trousers)  # legs
    fill(0, 34, 16, 38, lambda x, y: boots)
    fill(32, 9, 64, 10, lambda x, y: strap)  # headband
    fill(32, 10, 64, 11, lambda x, y: glove_dark)
    p[60, 11] = p[61, 12] = glove_dark  # knot ends on the back of the head
    return image


def farmer():
    """Villager profession overlay: denim overalls with straps over a green shirt, and a
    straw hat with a wide brim (the hat rim of the villager model) and a red band."""
    image = blank(64)
    p = image.load()
    denim, denim_dark, stitch = (60, 90, 140, 255), (45, 70, 110, 255), (200, 170, 80, 255)
    straw, straw_dark, band = (220, 190, 110, 255), (180, 150, 80, 255), (170, 40, 35, 255)

    def fill(x0, y0, x1, y1, color):
        for y in range(y0, y1):
            for x in range(x0, x1):
                p[x, y] = color(x, y)

    overalls = lambda x, y: denim_dark if y % 4 == 3 else denim
    fill(0, 46, 28, 58, overalls)  # trousers part of the robe
    fill(22, 30, 30, 38, overalls)  # bib on the front of the body
    for y in range(20, 30):
        p[23, y] = p[28, y] = denim_dark  # straps
    p[23, 30] = p[28, 30] = stitch  # buttons
    hat = lambda x, y: straw_dark if (x + 2 * y) % 5 == 0 else straw
    fill(40, 0, 48, 8, hat)  # crown top
    fill(32, 8, 64, 11, hat)  # crown sides
    fill(32, 11, 64, 12, lambda x, y: band)
    fill(31, 48, 47, 64, hat)  # brim
    return image


def kiln_operator():
    """Villager profession overlay: a brick red head scarf knotted at the back; a terracotta shirt with the sleeves rolled up, a grey leather waist
    apron with clay and ash stains, soot on the forearms, dark trousers and boots."""
    image = blank(64)
    p = image.load()
    shirt_, shirt_dark = (185, 95, 58, 255), (155, 76, 45, 255)
    scarf, scarf_dark = (140, 50, 38, 255), (110, 38, 28, 255)
    leather, leather_dark, clay, ash = (95, 88, 82, 255), (70, 64, 60, 255), (190, 130, 90, 255), (190, 186, 180, 255)
    skin, skin_dark, soot = (190, 136, 108, 255), (170, 118, 92, 255), (70, 62, 58, 255)
    trousers, boots = (70, 52, 40, 255), (40, 32, 26, 255)

    def fill(x0, y0, x1, y1, color):
        for y in range(y0, y1):
            for x in range(x0, x1):
                p[x, y] = color(x, y)

    shirt = lambda x, y: shirt_dark if y % 4 == 3 else shirt_
    apron = lambda x, y: clay if (x * 5 + y * 7) % 11 == 0 else ash if (x * 3 + y * 5) % 13 == 0 else leather
    fill(16, 20, 44, 38, shirt)  # body
    fill(6, 38, 14, 44, shirt)  # jacket top
    fill(0, 44, 28, 56, shirt)  # jacket, upper part
    fill(0, 56, 28, 64, lambda x, y: trousers)  # jacket, lower part
    fill(0, 50, 28, 51, lambda x, y: leather_dark)  # apron string around the waist
    fill(5, 51, 15, 63, apron)  # waist apron on the front of the robe
    fill(5, 62, 15, 63, lambda x, y: leather_dark)  # hem
    fill(44, 22, 60, 28, shirt)  # arms: sleeves
    fill(44, 27, 60, 28, lambda x, y: shirt_dark)  # rolled up cuff
    fill(44, 28, 60, 34, lambda x, y: skin_dark if y == 28 else skin)  # bare forearms
    fill(52, 22, 56, 26, lambda x, y: skin)  # end of the arm
    fill(40, 38, 64, 46, lambda x, y: skin_dark if (x + y) % 5 == 0 else skin)  # crossed arms, middle part
    p[46, 43] = p[49, 44] = p[58, 42] = soot
    fill(0, 22, 16, 34, lambda x, y: trousers)  # legs
    fill(0, 34, 16, 38, lambda x, y: boots)
    scarf_cloth = lambda x, y: scarf_dark if (x + 2 * y) % 5 == 0 and y % 2 == 0 else scarf
    fill(40, 0, 48, 8, scarf_cloth)  # head scarf top
    fill(32, 8, 64, 11, scarf_cloth)  # head scarf sides
    fill(32, 11, 64, 12, lambda x, y: scarf_dark)  # folded edge
    fill(59, 12, 61, 13, lambda x, y: scarf_dark)  # knot on the back of the head
    p[59, 13] = p[60, 14] = scarf  # its ends
    return image


def kiln(jar):
    """The furnace textures with bricks instead of cobblestone: grey (stone) pixels take the
    bricks pixel at the same position, keeping the brightness of the stone pixel so the
    shading stays. The dark opening and the fire are kept."""
    bricks = read_vanilla(jar, "block/bricks")
    result = {}
    for part, source in (("front", "furnace_front"), ("front_on", "furnace_front_on"), ("side", "furnace_side"), ("top", "furnace_top")):
        image = read_vanilla(jar, f"block/{source}")
        if image.height > image.width:
            image = image.crop((0, 0, image.width, image.width))  # first frame of an animation
        stone = [image.getpixel((x, y)) for x in range(16) for y in range(16)]
        grey = [c for c in stone if c[3] and max(c[:3]) - min(c[:3]) < 24]
        stone_brightness = sum(sum(c[:3]) for c in grey) / max(len(grey), 1) / 3
        p = image.load()
        for y in range(16):
            for x in range(16):
                r, g, b, a = p[x, y]
                if not a or max(r, g, b) - min(r, g, b) >= 24:
                    continue  # transparent or part of the fire
                factor = ((r + g + b) / 3) / stone_brightness
                if part.startswith("front") and factor < 0.55:
                    continue  # the opening stays dark
                br, bg, bb, _ = bricks.getpixel((x, y))
                p[x, y] = tuple(min(255, int(c * factor)) for c in (br, bg, bb)) + (255,)
        result[part] = image
    return result


# The lock of the chest textures (at 0, 0; 2x4x1, or 1x4x1 for a half of a double chest).
CHEST_LOCK = (0, 0, 6, 5)


def box_faces(u, v, width, height, depth):
    """The faces of an entity model box in its texture, unwrapped like every box, as
    (x, y, width, height)."""
    return [
        (u + depth, v, width, depth), (u + depth + width, v, width, depth),
        (u, v + depth, depth, height), (u + depth, v + depth, width, height),
        (u + depth + width, v + depth, depth, height), (u + 2 * depth + width, v + depth, width, height),
    ]


def village_chest(jar, half=""):
    """The vanilla chest (or a half of the double chest: half "_left" or "_right") in spruce,
    with iron bands instead of the dark edges of its faces and an emerald as lock. Each pixel
    keeps its brightness relative to the oak, so the grain and the shading stay. Only dark
    edge pixels become iron, so where the halves of a double chest meet stays wood."""
    image = read_vanilla(jar, f"entity/chest/normal{half}")
    oak = average(read_vanilla(jar, "block/oak_planks"))
    spruce = average(read_vanilla(jar, "block/spruce_planks"))
    p = image.load()
    width = 15 if half else 14
    edges = set()
    for fx, fy, fw, fh in box_faces(0, 0, width, 5, 14) + box_faces(0, 19, width, 10, 14):
        for x in range(fx, fx + fw):
            edges.add((x, fy))
            edges.add((x, fy + fh - 1))
        for y in range(fy, fy + fh):
            edges.add((fx, y))
            edges.add((fx + fw - 1, y))
    lx, ly, lw, lh = CHEST_LOCK
    for y in range(image.height):
        for x in range(image.width):
            r, g, b, a = p[x, y]
            if not a or max(r, g, b) < 12:
                continue  # transparent, or the black inside of the chest
            light = (r + g + b) / 3
            if lx <= x < lx + lw and ly <= y < ly + lh:
                # Emerald: dark green to light green by the brightness of the iron lock
                t = min(1.0, light / 200)
                p[x, y] = (int(10 + 60 * t), int(90 + 140 * t), int(40 + 70 * t), a)
            elif (x, y) in edges and light < 70:
                grey = int(min(170, 70 + light * 1.2))
                p[x, y] = (grey, grey, min(255, grey + 6), a)
            else:
                p[x, y] = tuple(min(255, int(c * s / o)) for c, s, o in zip((r, g, b), spruce, oak)) + (a,)
    return image


def average(image):
    pixels = [c for c in image.getdata() if c[3]]
    return tuple(sum(c[i] for c in pixels) / len(pixels) for i in range(3))


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
    """Villager profession overlay: a brown felt travelling hat with a brim (the hat rim) and
    a teal band; a teal travelling tunic with a belt, the leather straps of the basket over
    the shoulders, brown trousers and tall boots."""
    image = blank(64)
    p = image.load()
    teal, teal_dark, teal_light = (45, 125, 125, 255), (32, 98, 100, 255), (70, 150, 148, 255)
    felt, felt_dark = (110, 80, 55, 255), (85, 60, 40, 255)
    strap, buckle = (90, 60, 35, 255), (200, 170, 80, 255)
    trousers, boots, boots_dark = (95, 75, 55, 255), (70, 45, 28, 255), (50, 32, 20, 255)

    def fill(x0, y0, x1, y1, color):
        for y in range(y0, y1):
            for x in range(x0, x1):
                p[x, y] = color(x, y)

    tunic = lambda x, y: teal_light if (x * 3 + y * 7) % 13 == 0 else teal_dark if y % 4 == 3 else teal
    fill(16, 20, 44, 38, tunic)  # body
    fill(6, 38, 14, 44, tunic)  # jacket top
    fill(0, 44, 28, 58, tunic)  # jacket: the tunic
    fill(0, 58, 28, 64, lambda x, y: trousers)  # jacket, lower part
    fill(0, 54, 28, 55, lambda x, y: strap)  # belt
    for y in range(38, 44):
        p[7, y] = p[12, y] = strap  # basket straps on the robe top (shoulders)
    for y in range(44, 53):
        p[7, y] = p[12, y] = strap  # robe front
    for y in range(44, 55):
        p[21, y] = p[26, y] = strap  # robe back, down to the basket
    p[7, 50] = p[12, 50] = buckle
    fill(44, 22, 60, 34, tunic)  # arms
    fill(40, 38, 64, 46, tunic)  # crossed arms, middle part
    fill(0, 22, 16, 28, lambda x, y: trousers)  # legs
    fill(0, 28, 16, 38, lambda x, y: boots_dark if y in (28, 37) else boots)  # tall boots
    felt_cloth = lambda x, y: felt_dark if (x * 5 + y * 3) % 11 == 0 else felt
    fill(40, 0, 48, 8, felt_cloth)  # hat top
    fill(32, 8, 64, 10, felt_cloth)  # hat sides
    fill(32, 10, 64, 11, lambda x, y: teal_dark)  # hat band
    hat_rim(p, lambda u, v: felt_dark if max(4 - u, u - 11, 4 - v, v - 11) == 2 else felt, 2)  # brim
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
        "entity/villager/profession/farmer.png": farmer(),
        "entity/zombie_villager/profession/farmer.png": farmer(),
        "entity/villager/profession/smith.png": smith(),
        "entity/zombie_villager/profession/smith.png": smith(),
        "entity/villager/profession/kiln_operator.png": kiln_operator(),
        "entity/zombie_villager/profession/kiln_operator.png": kiln_operator(),
    }
    for part, image in woodcutter(jar).items():
        outputs[f"block/woodcutter_{part}.png"] = image
    for part, image in kiln(jar).items():
        outputs[f"block/kiln_{part}.png"] = image
    for half in ("", "_left", "_right"):
        outputs[f"entity/chest/village{half}.png"] = village_chest(jar, half)
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
