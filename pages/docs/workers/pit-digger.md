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

1. Lay **string** on the ground in a **closed ring** around the area to dig. Strings only
   connect straight (north, south, east, west), not diagonally, and each one has to lie
   on solid ground. The ring may follow the terrain, up and down by up to 4 blocks.
2. Place a [pit stake](../blocks-and-items.md#pit-stake) near the ring and right-click
   it to open its overview.
3. Click **Pick pit**. The menu closes; right-click a string of the ring (or the block
   under it). The menu shows the size of the pit, e.g. *Pit: 8×8, 64 blocks*.
4. Set the **depth** with **-** and **+** (1 to 16 blocks).
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
- At most **64 blocks** of ground inside, e.g. 8×8 or 4×16, and at least 2.
- The whole ring has to be within **16 blocks** of the stake.
- If a string is taken away, the pit digger waits (*the string ring is broken*) until it
  is back in the same place. You do not have to pick the pit again.

If the ring is not right, a message says why: not closed, too big, too uneven or too far.

## Depth

The depth counts from the **highest string** of the ring: with a depth of 4 the pit goes
4 blocks down from the ground under that string. Higher ground inside the ring (a hill)
is dug away too. The height is fixed when you pick the pit, so moving strings around does
not make the pit deeper; only a higher depth in the menu does.

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
