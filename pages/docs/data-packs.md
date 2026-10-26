---
icon: wv/writable_book
---

# Data packs and other mods

Most of what the workers can make is data driven, so data packs (and mods) can add their
own wood.

## Woodcutter recipes

Recipe type `workingvillagers:woodcutting`, same format as `minecraft:stonecutting`:

```json title="data/<namespace>/recipe/my_planks_from_logs.json"
{
  "type": "workingvillagers:woodcutting",
  "ingredient": "#minecraft:acacia_logs",
  "result": {
    "id": "minecraft:acacia_planks",
    "count": 4
  }
}
```

Only items in the item tag `workingvillagers:woodcuttable` can be put into the woodcutter. By
default that is `#minecraft:logs`, `#minecraft:planks`, `#minecraft:bamboo_blocks` and
bamboo mosaic. Add your items to the tag if they are not in one of these.

## Carpenter's bench

The bench offers every **crafting recipe** (shaped or shapeless) whose result is
in the item tag `workingvillagers:joinery`. To add a product, add its item to the tag:

```json title="data/workingvillagers/tags/item/joinery.json"
{
  "replace": false,
  "values": ["mymod:wooden_crate"]
}
```

## Stripping logs

The lumberjack strips logs with the stripping rules of its axe, so modded logs that can
be stripped by hand can be stripped by the lumberjack too. On NeoForge this includes the
stripping rules mods add through the data map.

## Pit digger

The pit digger only digs blocks in the block tag `workingvillagers:pit_diggable`: dirt,
grass, sand, gravel, clay, mud, snow, stone and its kinds, ores, leaves and plants. Add
your own natural blocks to it:

```json title="data/workingvillagers/tags/block/pit_diggable.json"
{
  "replace": false,
  "values": ["mymod:limestone"]
}
```

## Miner and mill

The miner only digs blocks in the block tag `workingvillagers:mine_diggable` (stone and its
kinds, deepslate, dirt, gravel, sand, clay, ores, amethyst, cave plants); anything else
blocks its tunnel. The ores it follows from the tunnel are the block tag
`workingvillagers:miner_ores` (`#minecraft:ores` and `#c:ores`).

The mill of the mining bench uses the recipe type `workingvillagers:milling`, same format
as `minecraft:stonecutting`:

```json title="data/<namespace>/recipe/sand_from_limestone.json"
{
  "type": "workingvillagers:milling",
  "ingredient": "mymod:limestone",
  "result": {
    "id": "minecraft:sand",
    "count": 2
  }
}
```

Only items in the item tag `workingvillagers:millable` can be put into the mill by
shift-clicking.

## Kiln

The kiln fires every furnace recipe (`minecraft:smelting`) except food (recipes of the
recipe book category "food") and everything the blast furnace can smelt
(`minecraft:blasting`). Smelting recipes of data packs and mods are fired too. To keep an
item out of the kiln although a furnace smelts it, add it to the item tag
`workingvillagers:kiln_excluded`.

## Trees

A tree counts as natural when its logs stand on the ground trees grow on (anything in
`#minecraft:substrate_overworld`: dirt, grass, podzol, mycelium, mud, moss) and it has leaves that are not marked as player placed. Modded trees
that follow these rules are felled too.

## Meals

Workers eat the items in the item tag `workingvillagers:meals` that are food. For the
[variety](workers/basics.md#meals) each belongs to one of four kinds, one tag each:
`workingvillagers:meals/field`, `meals/meat`, `meals/fish` and `meals/fruit`; `meals` holds
these four. Add modded food to one of them:

```json title="data/workingvillagers/tags/item/meals/field.json"
{
  "replace": false,
  "values": ["mymod:rice_bowl"]
}
```

Food you add to `meals` directly, without a kind, counts as a kind of its own.
