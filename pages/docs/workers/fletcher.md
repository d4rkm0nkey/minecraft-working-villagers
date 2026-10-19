---
icon: wv/head-fletcher
workers: [fletcher]
---

# Fletcher

Hired with a work contract, the fletcher works at a vanilla **fletching table**. It makes
arrows, spectral arrows, bows and crossbows from its input chests and keeps a stock of them
in its output chests, for you, for trading, and for the [guards](guard.md).

!!! note
    Ordinary (not hired) villagers still become vanilla fletchers at fletching tables. A
    vanilla fletcher can be hired directly: it stays at its table, keeps its trades and
    becomes the fletcher of this mod. The fletcher trades like a vanilla fletcher.

## Setting it up

1. Place a fletching table and **right-click it with an empty hand**: its overview opens
   (the fletching table has no screen of its own).
2. Link an **input chest** with the material and an **output chest** (**+ Input**, **+ Output**).
   A [village chest](basics.md#village-chests) makes a good output chest: the guards take
   their arrows from there.
3. Tab :mc-arrow: **Order**: click the product and set the **stock**.
4. Give it its wage.

## Products

| Product | Material | Makes |
| --- | --- | --- |
| :mc-arrow: Arrows | a head, a stick, a feather | 4 |
| :mc-spectral_arrow: Spectral arrows | 4 glowstone dust, 1 arrow | 2 |
| :mc-bow: Bow | 3 sticks, 3 string | 1 |
| Crossbow | 3 sticks, 2 string, an iron ingot, a tripwire hook | 1 |

**The head of an arrow** is :mc-flint: flint, a :mc-copper_nugget: copper nugget or an
:mc-iron_nugget: iron nugget. The fletcher makes them itself:

- it knaps **flint from gravel**, one for one, like the mill of the mining bench,
- it cuts **ingots into nuggets**: one copper or iron ingot gives 9 heads, so 36 arrows.

It uses flint first, then copper, then iron, and only knaps or cuts when it has no heads
left. Put gravel from the [pit digger](pit-digger.md), copper you have plenty of, or sticks from
the [sawyer](sawyer.md) and feathers from the [butcher](butcher.md)'s chickens into its input
chest.

## How it works

It fetches material for up to 16 sets of arrows (4 bows, 8 sets of spectral arrows) at
once, makes one piece per second at the fletching table and brings them to the output
chest. With a **stock** set it stops when that many lie in the output chests and goes on
when some are taken out. What the order does not need it puts back into the input chest.

The menu shows how many pieces it made today.
