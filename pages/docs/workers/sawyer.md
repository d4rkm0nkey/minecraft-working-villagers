---
icon: wv/head-woodcutter
workers: [sawyer, stone_sawyer]
---

# Sawyer and stone sawyer

Hired with a work
contract, they work at the **woodcutter** (sawyer) or at a vanilla **stonecutter**
(stone sawyer).

Both cut material from their input chests and bring the products to their output
chests.

## The woodcutter

The woodcutter is a stonecutter for wood. You can use it yourself, too: logs, stems and
bamboo blocks become planks, stairs, slabs, fences, doors, signs and so on; planks
become stairs, slabs, buttons and sticks. Unlike the crafting table, there is no waste and
no detour over planks: one log gives 8 slabs or 4 stairs directly.

[[craft: _ iron_ingot _ / any_planks any_planks any_planks -> woodcutter]]

## Setting the order

=== "Woodcutter"

    An order at the woodcutter is a **product shape** and a **set of wood types**.
    Open the **Order** tab above the screen (or click the order box below the result slot):

    - pick the shape: planks, slabs, stairs, fences, fence gates, doors, trapdoors,
      pressure plates, buttons, signs or sticks
    - switch wood types on and off (**All** / **None**). Wood types that do not have
      that product are marked red.

    The sawyer only uses wood of the selected types. Other wood stays in the input chest.

=== "Stonecutter"

    Open the **Order** tab above the screen (or click the order box below the result slot).
    It shows the stone families (stone, granite, deepslate,
    copper, ...):

    - pick a family to see its products
    - click a product to order it. Clicking the ordered product again clears the order.

    The stone sawyer uses any material that has a recipe for the ordered item.

!!! note
    Ordinary (not hired) villagers still become masons at stonecutters. Only hired
    villagers become stone sawyers. A station you have set up (linked chests) is
    kept for hired workers, and **Assign** makes room there.

## Worker and chests

In the overview (the tabs above the screen: **Worker**, **Chests** and **Order**):

- **Assign** / **Remove** the worker
- **+ Input**: chests the material is taken from
- **+ Output**: chests the products go to
- the emeralds the worker carries for its wage (right-click the worker to give it some)

**Without an order, an input chest and an output chest the worker does not work.**

## How it works

During work hours the worker:

1. fetches up to **16** matching items from an input chest,
2. cuts them at the station, with wood or stone chips flying off the saw,
3. brings the products to an output chest.

It makes **4 items per second** (one log into four planks takes one second; slabs take
twice as long), at least one second per cut. When all output chests are full, it waits.

## Tip

Point a lumberjack's chest and the sawyer's input at the same chest, and you have a
small sawmill: logs come in, planks come out.
