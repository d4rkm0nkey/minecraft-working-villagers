---
icon: wv/head-pit_digger
workers: [pit_digger]
---

# Pit digger

Hired with a work contract, the pit digger works at the **pit stake**. It digs out a pit
that you mark with **string** on the ground: layer by layer, down to the depth you set,
with a ladder as the way in and out. Everything it digs goes to its chest. Good for
basements, foundations, sand and clay pits and as a simple quarry.

## Setting it up

1. Place a [pit stake](../blocks-and-items.md#pit-stake) at the edge of the area to dig.
2. Lay **string** on the ground in a **closed ring** around the area that **touches the
   stake**: the stake may stand in the ring like a piece of string, or right next to it.
   The string connects to the stake, and the stake takes the ring as its pit by itself.
   **While you hold string**, every pit stake within 24 blocks shows its pit as a white
   outline, down to the floor; a ring that is no pit yet shows its strings, the ones that
   are the problem in red (the open end, or the whole ring if it is too big). Green
   particles show a ring that just became a pit, angry ones where the problem is. Strings only connect straight (north, south, east,
   west), not diagonally, at most one block up or down, and each one has to lie on
   ground (dirt paths, farmland and slabs count too). The ring may follow the terrain,
   up and down by up to 4 blocks.
3. Right-click the stake to open its overview. It shows the size of the pit, e.g.
   *Pit: 8×8, 64 blocks*, or what is wrong with the ring, and how big and deep the pit
   digger may dig (*At most 64 blocks, 8 deep*).
4. Set the **depth** with **-** and **+** (1 to 8 blocks for a novice).
5. Link at least one **chest** (**+ Chest**). A hired villager without a job finds the
   stake by itself; **Assign** picks a specific one.
6. Right-click the pit digger with its wage (emeralds), a **shovel**, a **pickaxe** and
   **ladders**, one per block of depth. **Torches** are optional.

**Without a chest, a shovel, a pickaxe or ladders the pit digger does not work.**

## The ring

- Everything **inside** the ring is dug, seen from above. Any shape works, not only
  rectangles.
- The blocks **under the strings stay**: they are the rim of the pit, and the strings
  stay where they are.
- At most **64 blocks** of ground inside for a novice, e.g. 8×8 or 4×16, and at least 2.
  See [Size and level](#size-and-level).
- Make the ring bigger or change its shape whenever you like: the stake takes the new
  ring the next time you open its overview or the pit digger looks at it.
- If a string is taken away, the pit digger waits (*the string ring is broken*) until it
  is back in the same place.

If the ring is not right, the overview and the pit digger's status say why: not closed
(with the place where it ends), too big or too uneven.

## Size and level

How big and how deep a pit may be grows with the pit digger's level:

| Level | Ground inside | Depth |
| --- | --- | --- |
| Novice | 64 (8×8) | 8 |
| Apprentice | 100 (10×10) | 10 |
| Journeyman | 144 (12×12) | 12 |
| Expert | 196 (14×14) | 14 |
| Master | 256 (16×16) | 16 |

The pit digger does not trade, so for now it stays a novice; levels for work are
[planned](../planned.md). Without a pit digger at the stake the novice's limits apply.
A depth set deeper than the pit digger may dig is kept; it digs as deep as it may.

## Depth

The depth counts from the **highest string** of the ring: with a depth of 4 the pit goes
4 blocks down from the ground under that string. Higher ground inside the ring (a hill)
is dug away too.

## How it works

During work hours the pit digger:

1. takes the top layer of the pit, starting next to the ladder,
2. digs every block of the layer with the shovel or the pickaxe, whichever is faster. It
   takes about **8 times** as long as you with the same tool, at least 2 seconds per block,
3. puts a **ladder** against the wall as soon as the ladder column gets deeper, so the
   ladder always reaches the bottom,
4. goes down the next layer, and so on until the depth is reached,
5. climbs out and brings everything to its chest when its pockets are full, after
   **256 blocks** a day and when the pit is done. At the end of the work day it climbs
   out too.

It only goes in and out **by the ladder**. The ladder goes into the column inside the ring
closest to the stake; **Ladder** in the menu moves it: click it, then right-click the
string where the ladder should go. The ladders stay in the finished pit, so you can climb
out too.

With torches it lights the **floor** of the pit, about every six blocks, so nothing
spawns in it at night.

## What it digs and what not

- It digs **natural blocks**: dirt, grass, sand, gravel, clay, mud, snow, stone and its
  kinds (granite, diorite, andesite, tuff, deepslate, ...), ores and plants. Data packs can
  add more, see [Data packs](../data-packs.md).
- **Builds stay**: planks, bricks, glass, chests, logs and the like. A column under such a
  block stays as a pillar, so nothing is left hanging in the air.
- A block its pickaxe is **too weak** for (a diamond ore with a stone pickaxe) stays as a
  pillar. With a better pickaxe it digs it next time.
- It never digs a block next to **water or lava**, so the pit does not flood. The status
  says *water or lava in the pit*.
- Sand and gravel that fall down are dug in the next round.
- **Everything** it digs goes to the chest, nothing is thrown away. If the chest is full,
  it waits.
- The shovel and the pickaxe wear out like yours. Shortly before one breaks, it gives it
  back ([Worn tools](basics.md#worn-tools)) and uses its spare; without one it waits for a
  new one.
