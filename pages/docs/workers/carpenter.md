---
icon: wv/head-carpenter
workers: [carpenter]
---

# Carpenter

Hired with a work contract, works at the **carpenter's bench**.

## The carpenter's bench

The carpenter's bench is a crafting table for wooden things only. You can use it
yourself: its menu lists everything it can make, items you lack the ingredients for are
greyed out, and the tooltip shows what they need. Pick one and take the result; it is
made from the ingredients in your inventory. Shift-click makes as many as possible.

[[craft: any_wooden_slab any_wooden_slab any_wooden_slab / _ iron_ingot _ / any_log _ any_log -> carpenter_bench]]

It makes: chests, trapped chests, barrels, crafting tables, ladders, bowls, composters,
bookshelves, chiseled bookshelves, wooden tools, boats and boats with chest, shelves, hanging signs, item
frames, paintings, armor stands, fishing rods, shields, torches,
tripwire hooks and campfires.
Bows and crossbows come from the [fletcher](fletcher.md).

## Setting the order

Pick the products in the overview (**i**, or click the order box below the result slot):
up to 8, made in [equal parts](basics.md#several-products).

- **Boats, boats with chest, hanging signs and shelves** are ordered as a shape with
  wood types, like at the woodcutter.
- **Everything else** can be made of any wood (vanilla allows mixed planks). The wood
  types in the overview then say which wood the carpenter may use up.

## Stock

With **-** / **+** in the overview you set how many of each product the carpenter keeps in its
output chests: no limit, 1, 2, 4, ... up to 256. It stops at that many and goes on as
soon as some are taken out. Handy for "always keep 16 chests and 16 barrels ready".

## How it works

During work hours the carpenter:

1. fetches up to **32 ingredients** from the input chests, e.g. enough for 4 chests (they
   may be spread over several chests; it only sets off when all of them together are
   enough for at least one piece),
2. makes the products at the bench (half a second per ingredient, at least one second
   per piece, with shavings flying),
3. brings them to an output chest.

Material it cannot use goes back to an input chest.

!!! tip "Sticks and slabs"
    The carpenter does not make sticks and slabs. They come from a
    [sawyer](sawyer.md); a [carrier](carrier.md) can bring them over. See
    [A wood workshop](../guides/wood-workshop.md).
