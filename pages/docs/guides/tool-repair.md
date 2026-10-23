---
icon: wv/filled_map
---

# Tools that never break

Workers do not break their tools: shortly before a tool would break, they give it back. This
guide sets up a corner of your village where those tools are repaired and go back to work
by themselves, and where the experience your guards collect pays for the repairs.

<div class="wv-flow" role="img" aria-label="Workers give back worn tools into a village chest, the smith and the weaponsmith repair them there, and the workers take them again">
<div class="wv-flow__step">:mc-head_lumberjack:Workers</div>
<div class="wv-flow__node">:mc-iron_axe:Worn tools</div>
<div class="wv-flow__node">:mc-village_chest:Village chest</div>
<div class="wv-flow__step">:mc-head_smith:Smith</div>
<div class="wv-flow__step">:mc-head_weaponsmith:Weaponsmith</div>
<div class="wv-flow__node">:mc-iron_axe:Repaired</div>
<div class="wv-flow__step">:mc-head_lumberjack:Workers</div>
</div>

<div class="wv-flow" role="img" aria-label="The apothecary makes glass bottles, the guard fills them with experience, the smith repairs with the bottles">
<div class="wv-flow__step">:mc-head_apothecary:Apothecary</div>
<div class="wv-flow__node">:mc-glass_bottle:Glass bottles</div>
<div class="wv-flow__step">:mc-head_guard:Guard</div>
<div class="wv-flow__node">:mc-experience_bottle:Bottles o' enchanting</div>
<div class="wv-flow__step">:mc-head_smith:Smith</div>
</div>

## How it works

- **Worn tools.** Once a tool has only **10 %** of its durability left (at least 2 uses),
  the worker stops using it and gives it back: into a **village chest** where a smith or
  weaponsmith repairs it, otherwise into an output chest of its workstation, so you see that
  it needs a new one. Wooden and stone tools, shears and fishing rods are used up instead,
  unless they are enchanted or renamed. See [Worn tools](../workers/basics.md#worn-tools).
- **New tools.** A worker without a usable tool takes one from a village chest in reach,
  the most worn one first. It never takes a worn one, and it leaves enchanted and renamed
  tools alone: give those to it yourself.
- **Repairing.** The [smith](../workers/smith.md#repairing-tools) at a **smithing table**
  repairs the worn tools in the village chests with material from its input chest, like an
  anvil: iron ingots for iron tools, diamonds for diamond tools. The
  [weaponsmith](../workers/weaponsmith.md#grinding-together) at a **grindstone** grinds two
  worn tools of the same kind into one, without material. Both put the result back into the
  village chest, where the workers take it again.
- **Experience.** Bows, shears and fishing rods have no repair material, and enchanted
  items cost experience at an anvil. For both the smith uses **bottles o' enchanting**:
  [guards](../workers/guard.md#experience) get the experience of the monsters they shoot and
  fill it into glass bottles, 7 per bottle.

## What you need

- a [village chest](../blocks-and-items.md#village-chest) (or several) within **48 blocks**
  of the workers' workstations (and 24 up or down)
- a **smithing table** and a smith, with an input chest
- optional: a **grindstone** and a weaponsmith
- for the experience: a **target** and a guard, glass bottles from an
  [apothecary](../workers/apothecary.md) or yourself
- a few spare tools for the workers to start with

## 1. Village chest

Place the village chest in the middle of your workplaces. Right-click it: the tab of the
workers it serves lists everyone in reach. Put a few spare tools in, iron ones are worth
repairing. A worker whose tool is worn gives it back here and takes a spare on the same
walk.

## 2. Smith → repairs

Place a smithing table within 48 blocks of the village chest and link a chest next to it as
**input**: put iron ingots (and diamonds, planks, ... for other tools) in. Assign a smith
and give it its wage. It takes the worn tools from the village chest, up to 4 at a time,
repairs them with the material from its input chest and puts them back.

## 3. Guard → bottles o' enchanting

Place a target where monsters come at night, assign a guard and give it a bow, arrows and
glass bottles. In the morning it fills the experience of its kills into bottles and brings
them to its **output chest**, together with what the monsters dropped.

**Link the guard's output chest as an input chest of the smith**, too: one chest for both.
The smith takes the bottles from there; the rest of the loot (rotten flesh, bones, arrows)
stays and you empty it now and then. Do not make the village chest the guard's output
chest: the loot would fill it up.

The chest has to be within 16 blocks of the target and of the smithing table, or a
[carrier](../workers/carrier.md) brings the bottles over.

## 4. Weaponsmith → two worn tools into one (optional)

Place a grindstone within 48 blocks of the village chest and assign a weaponsmith. It looks
for two worn tools of the same kind in the village chests (two iron axes) and grinds them
into one, with the durability of both plus 5 %, which saves the smith's ingots. Enchanted
and renamed tools it leaves to the smith. With an order it also forges new tools; with the
village chest as its output chest the workers take them from there.

## 5. Pay everybody

Each worker takes one emerald a day. Put emeralds into the village chest as well: the
workers in reach take their wage for a few days from there.

## Ideas to extend it

- An **apothecary** makes the glass bottles for the guard from glass, and a kiln operator
  fires the glass from sand.
- The **weaponsmith** disenchants the loot of the guards and the fisher's treasure and fills
  that experience into bottles too; the [librarian](../workers/librarian.md) enchants books
  and tools with the bottles.
- A **miner** and a smith at a blast furnace make the iron ingots for the repairs.
