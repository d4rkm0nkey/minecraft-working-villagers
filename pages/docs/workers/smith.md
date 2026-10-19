---
icon: wv/head-smith
workers: [smith]
---

# Smith

Hired with a work contract, the smith works at a vanilla **blast furnace** or at a
vanilla **smithing table**. At the blast furnace it loads the furnace with ore and fuel
from its input chests and brings what comes out to its output chests. At the smithing
table it [repairs tools](#repairing-tools). Which of the two it does depends on where it
works; for both, hire two smiths.

!!! note
    Ordinary (not hired) villagers still become armorers at blast furnaces and toolsmiths
    at smithing tables. Only hired villagers become smiths. A station you have set up (linked chests) is
    kept for hired workers, and **Assign** makes room there.

## Setting it up

1. Place a blast furnace.
2. Open it and click the **Chests** tab above the furnace screen to open its overview
   there. (The first tab leads to the furnace again.)
3. Link chests with **+ Input** and **+ Output**. A hired villager without a job finds
   the blast furnace by itself; **Assign** picks a specific one.
4. Right-click the smith with emeralds for its wage.

**Without an input chest and an output chest the smith does not work.**

## How it works

There is no order: the smith smelts **everything the blast furnace can smelt** that it
finds in the input chests (raw iron, raw gold, raw copper, ores, iron and gold tools and
armor, ...). Everything that burns (coal, charcoal, blocks of coal, ...) is used as
fuel. Put both into the input chests.

During work hours the smith:

1. fetches up to **16** items to smelt and up to **8** fuel from the input chests,
2. puts them into the blast furnace,
3. takes out what the blast furnace made,
4. brings it to an output chest.

The blast furnace does the smelting itself, at its usual speed (twice as fast as a
furnace). You can watch and use it at any time.

## Good to know

- The **experience** stays in the blast furnace. You get it the next time you take
  something out of it yourself.
- An empty bucket left over from a lava bucket is taken out and handed in.
- The smith only fills the blast furnace with the same item it is already smelting, so
  a stack you put in by hand is finished first.
- Glass, stone, bricks and charcoal are not smelted in the blast furnace; that is the job
  of the [kiln operator](kiln-operator.md).

## Repairing tools

At a **smithing table** the smith repairs the tools of the other workers, and yours.

1. Place a smithing table and open it: the tabs above its screen lead to its overview.
2. Link at least one **input chest** and put the repair material in: iron ingots for iron
   tools, diamonds for diamond tools, planks for wooden ones, the same as at an anvil.
3. Assign a hired villager (or let one find the table) and give it its wage.

It repairs:

- the **worn tools** the workers gave back into the
  [village chests](basics.md#worn-tools) within 48 blocks of the smithing table, and
- **everything damaged** you put into its input chests: tools, weapons and armor.

It takes up to **4** of them at a time, only those whose material is in an input chest,
repairs them at the smithing table and puts them back into the chest they came from. One
material repairs a quarter of the durability, like at an anvil, and it never gets "too
expensive". Enchantments and names stay. Repaired tools in a village chest are taken by
the workers again.

### Bottles o' enchanting

With :mc-experience_bottle: **bottles o' enchanting** (the [guards](guard.md#experience) and the [weaponsmith](weaponsmith.md#disenchanting)
fill them, the [librarian](librarian.md) enchants with them) the smith repairs more:

- **Things without a repair material** (bows, crossbows, fishing rods, shears): one bottle
  repairs a **tenth** of the durability. So the guards' bows and the fisher's enchanted rods
  come back.
- **Enchanted items** cost a bottle per step on top of the material, as an anvil costs
  experience. Without bottles it does not repair them ("Waiting for bottles o' enchanting").

It takes the bottles from its input chests and the village chests within 48 blocks.

Without material it waits ("Waiting for Iron Ingot to repair with"). An output chest is
not needed; if the chest a tool came from is full, it puts it into another village chest
or one of its own chests.
