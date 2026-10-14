#!/usr/bin/env python3
"""Generates the woodcutter recipes (workingvillagers:woodcutting) for all vanilla wood types.

Usage: python3 tools/generate_woodcutting_recipes.py
Writes common/src/main/resources/data/workingvillagers/recipe/woodcutting/*.json (the folder is replaced).

Yields follow the stonecutter idea: no waste compared to the crafting table, one log is
worth four planks.
"""
import json
import pathlib
import shutil

OUT = pathlib.Path(__file__).resolve().parent.parent / "common/src/main/resources/data/workingvillagers/recipe/woodcutting"

# wood type -> tag of the log-like blocks (logs, wood, stripped variants)
WOODS = {
    "oak": "#minecraft:oak_logs",
    "spruce": "#minecraft:spruce_logs",
    "birch": "#minecraft:birch_logs",
    "jungle": "#minecraft:jungle_logs",
    "acacia": "#minecraft:acacia_logs",
    "dark_oak": "#minecraft:dark_oak_logs",
    "pale_oak": "#minecraft:pale_oak_logs",
    "mangrove": "#minecraft:mangrove_logs",
    "cherry": "#minecraft:cherry_logs",
    "poplar": "#minecraft:poplar_logs",
    "crimson": "#minecraft:crimson_stems",
    "warped": "#minecraft:warped_stems",
}

# result suffix -> count from one log (4 planks)
FROM_LOG = {
    "planks": 4, "slab": 8, "stairs": 4, "fence": 2, "fence_gate": 1,
    "door": 2, "trapdoor": 1, "pressure_plate": 2, "button": 4, "sign": 1,
}
# result suffix -> count from one plank
FROM_PLANKS = {"slab": 2, "stairs": 1, "button": 1}
STICKS_FROM_LOG = 8
STICKS_FROM_PLANKS = 2


def recipe(ingredient, result, count):
    return {"type": "workingvillagers:woodcutting", "ingredient": ingredient, "result": {"id": result, "count": count}}


def main():
    recipes = {}
    for wood, logs in WOODS.items():
        for suffix, count in FROM_LOG.items():
            recipes[f"{wood}_{suffix}_from_logs"] = recipe(logs, f"minecraft:{wood}_{suffix}", count)
        recipes[f"stick_from_{wood}_logs"] = recipe(logs, "minecraft:stick", STICKS_FROM_LOG)
        planks = f"minecraft:{wood}_planks"
        for suffix, count in FROM_PLANKS.items():
            recipes[f"{wood}_{suffix}_from_planks"] = recipe(planks, f"minecraft:{wood}_{suffix}", count)

    # Bamboo: one block is worth two planks, bamboo mosaic is cut from planks
    for suffix, count in {"planks": 2, "slab": 4, "stairs": 2, "fence": 1, "door": 1, "pressure_plate": 1, "button": 2}.items():
        recipes[f"bamboo_{suffix}_from_block"] = recipe("#minecraft:bamboo_blocks", f"minecraft:bamboo_{suffix}", count)
    recipes["stick_from_bamboo_blocks"] = recipe("#minecraft:bamboo_blocks", "minecraft:stick", 4)
    for result, count in {"bamboo_slab": 2, "bamboo_stairs": 1, "bamboo_button": 1, "bamboo_mosaic": 1,
                          "bamboo_mosaic_slab": 2, "bamboo_mosaic_stairs": 1}.items():
        recipes[f"{result}_from_planks"] = recipe("minecraft:bamboo_planks", f"minecraft:{result}", count)
    for result, count in {"bamboo_mosaic_slab": 2, "bamboo_mosaic_stairs": 1}.items():
        recipes[f"{result}_from_mosaic"] = recipe("minecraft:bamboo_mosaic", f"minecraft:{result}", count)

    # Planks to sticks, for every wood type at once
    recipes["stick_from_planks"] = recipe("#minecraft:planks", "minecraft:stick", STICKS_FROM_PLANKS)

    shutil.rmtree(OUT, ignore_errors=True)
    OUT.mkdir(parents=True)
    for name, content in sorted(recipes.items()):
        (OUT / f"{name}.json").write_text(json.dumps(content, indent=2) + "\n")
    print(f"wrote {len(recipes)} recipes to {OUT}")


if __name__ == "__main__":
    main()
