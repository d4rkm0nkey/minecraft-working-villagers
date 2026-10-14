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

The bench offers every vanilla **crafting recipe** (shaped or shapeless) whose result is
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

## Trees

A tree counts as natural when its logs stand on the ground trees grow on (anything in
`#minecraft:substrate_overworld`: dirt, grass, podzol, mycelium, mud, moss) and it has leaves that are not marked as player placed. Modded trees
that follow these rules are felled too.
