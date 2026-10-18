---
icon: wv/head-kiln_operator
workers: [kiln_operator]
---

# Kiln operator

Hired with a work contract, the kiln operator works at the **kiln**, a brick furnace for
building materials. It fires sand into glass, cobblestone into stone, clay into bricks
and terracotta, logs into charcoal and more, from its input chests into its output
chests.

## The kiln

The [kiln](../blocks-and-items.md#kiln) works like a furnace, with the furnace's recipe
book and at the furnace's speed, but it fires **no food** (that is the smoker's job) and
**nothing the blast furnace can smelt** (ores, raw metal, iron and gold tools). So each
of the three furnaces has its own things:

| Furnace | Makes | Worker |
| --- | --- | --- |
| :mc-blast_furnace: Blast furnace | Ingots from ore, raw metal, iron and gold tools | [Smith](smith.md) |
| :mc-kiln: Kiln | :mc-glass: glass, :mc-stone: stone, :mc-smooth_stone: smooth stone, :mc-brick: bricks, :mc-terracotta: terracotta, :mc-charcoal: charcoal, :mc-green_dye: dyes, ... | Kiln operator |

As it comes from the furnace recipes, things from other mods and data packs are fired too.
Anything else put into the kiln just stays in its input slot.

## Setting it up

1. Place a kiln.
2. Open it and click the **Chests** tab above its screen to open its overview there. (The
   first tab leads to the kiln again.)
3. Link chests with **+ Input** and **+ Output**. A hired villager without a job finds
   the kiln by itself; **Assign** picks a specific one.
4. Right-click the kiln operator with emeralds for its wage.

**Without an input chest and an output chest the kiln operator does not work.**

## The order

The overview lists everything the kiln makes.

- **No product picked** (the start): the kiln operator fires **everything** the kiln can
  fire that it finds in the input chests, once. Cobblestone becomes stone.
- **A product picked**: it only fires what makes that product, e.g. only sand for
  :mc-glass: glass. Everything else stays in the input chests, like the sand you keep for
  concrete or the cobblestone you build with. Click the product again to go back to
  everything.
- Some products take **two firings**: for :mc-smooth_stone: smooth stone it fires
  cobblestone into stone and that into smooth stone. The stone in between stays with the
  kiln operator and does not end up in the output chest.
- **Stock**: with a product picked, **-** and **+** set how many of it to keep in the
  output chests. With that many there, it stops, and it goes on once some are taken out.

## How it works

During work hours the kiln operator:

1. fetches up to **16** items to fire and up to **8** fuel from the input chests,
2. puts them into the kiln,
3. takes out what the kiln made,
4. brings it to an output chest.

Fuel is everything that burns (coal, charcoal, logs, lava buckets, ...), but nothing it
should fire: when you order **charcoal**, logs are the material and only other fuel is
burnt. Charcoal it made itself goes to the output chest, not into the fire.

## Good to know

- The **experience** stays in the kiln. You get it the next time you take something out
  of it yourself.
- An empty bucket left over from a lava bucket is taken out and handed in.
- The kiln operator only fills the kiln with the same item it is already firing, so a
  stack you put in by hand is finished first.
- Players can use the kiln like a furnace at any time.
