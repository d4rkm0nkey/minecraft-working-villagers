---
icon: wv/emerald
---

# Hiring and wages

These rules apply to every worker.

!!! info "Numbers"
    All numbers in this wiki (wage, distances, speeds, limits) are the defaults. They can
    be changed in the [configuration](../configuration.md).

## Hiring

- **Lumberjack, sawyer, stone sawyer, carpenter, farmer, smith:** right-click an unemployed adult
  villager with a [work contract](../blocks-and-items.md#work-contract). Then it looks for
  a free workstation by itself.
- **Carrier:** right-click an unemployed adult villager with a
  [wicker basket](../blocks-and-items.md#wicker-basket). The basket is contract and
  workstation at once, no work contract needed.

Only villagers without a profession can be hired: no nitwits, no babies, no villagers
that already have a job. Hired villagers get no name, just like vanilla villagers. Use a
name tag to tell them apart. (The config option `randomNames` gives them a random name
like Anton or Berta.)

Hired villagers **never take a vanilla job** (lectern, smithing table, ...). They do take
part in normal village life: they sleep in beds, meet at the village bell and flee from
zombies and raids.

## Finding a workstation

A hired villager without a job looks for a **free workstation of this mod** within
**48 blocks** that it can walk to: a chopping block, woodcutter, stonecutter, carpenter's
bench, composter or blast furnace. It walks there and takes the job (green particles),
just like vanilla villagers claim their job sites.

At a stonecutter, composter or blast furnace a hired villager becomes the stone sawyer,
farmer or smith of this mod. Unhired villagers still become vanilla masons, farmers and
armorers there.

## Assigning and removing

To pick a specific worker yourself, open the workstation menu (at the woodcutter,
stonecutter, carpenter's bench and blast furnace: the **i** button at the top right of
its screen; at the composter: right-click it with an empty hand) and click **Assign**. The menu closes; right-click a hired villager. It becomes the
worker of this station. A villager that already works somewhere else moves to the new
station.

**Remove** takes the worker away again. It stays hired and keeps its name. It does not
go back to that same station by itself for one day, but it may take another free one.

Every workstation has a small **paper note** on its front with the name of its worker,
or *Hired villager* if it has no name. An empty note means the station is free. A
vanilla stonecutter or blast furnace only gets a note once it is set up or has a hired
worker. The composter has no front and gets no note.

## Linking chests

Workers take their material from chests and put their products into chests. Chests and
barrels can be linked, and every other block a hopper can put items into (also from
other mods):

- up to **8 chests** per station
- within **16 blocks** of the station

Click **+ Chest** (lumberjack and farmer) or **+ Input** / **+ Output** (the others). The menu
closes; right-click the chests you want to link. Right-click a linked chest again to
unlink it. Right-click the workstation to finish; its menu opens again.

The menu lists the linked chests with a bar showing how full each one is. The **×**
unlinks a chest. At the stations with input and output chests, one click on *Input* /
*Output* switches a chest's role.

## Wage and tools

Give a worker its wage and tools directly, like you give a villager food:
**right-click the worker** with emeralds, its tool or what it plants, or **throw** the
items next to it. It walks over, picks them up and keeps them in its own inventory.
Happy villager particles show that it took them. If it already has enough, it shakes
its head and takes nothing.

The limits in the table count for both ways: a worker only picks up what it still wants
and leaves the rest on the ground. Throwing needs the `mob_griefing` game rule (like
vanilla mobs picking up items); right-clicking always works.

| Worker | Needs | Carries up to |
| --- | --- | --- |
| every worker | :mc-emerald: emeralds | 64 |
| lumberjack | :mc-any_axe: an axe, optionally :mc-oak_sapling: saplings | 2 axes, 16 saplings of each kind |
| farmer | :mc-any_hoe: a hoe, :mc-wheat_seeds: seeds to start with | 2 hoes, 16 seeds of each kind |

- **Wage:** before work each day the worker takes **1 emerald** from what it carries.
  64 emeralds pay for 64 work days. Without an emerald it does not work. It waits next
  to its workstation ("Waiting for wages") and checks again every few seconds, so you
  can pay it late.
- **Tools:** the lumberjack needs an axe, the farmer a hoe. The sawyer, stone sawyer,
  carpenter, smith and carrier only need emeralds.
- Workers never hand in their emeralds or their tool to the output chests.
- **A hired worker that dies drops everything it carries:** emeralds, tools and
  material.

!!! note
    With `dailyWage` set to 0 in the [configuration](../configuration.md) workers work
    for free. The lumberjack still needs an axe and the farmer a hoe.

The station menu (and the carrier menu) shows a row of **slots** with what the worker
carries: its emeralds, its tool with the durability bar and its saplings or seeds. Empty
slots are faded, a missing wage or tool is tinted red. Next to them it says *Paid for 5
more days*, *Works for free* or what is missing, e.g. *Needs an emerald!*. The tooltip
shows the details, e.g. whether today's wage has been paid.

## Complaining

A worker that cannot work (no wage, no tool, no seeds, chests full, a chest or the
station out of reach, no material, no ore or fuel, no chest linked, stuck) **shakes its head** with
the "no" sound now and then while a player is within 8 blocks. The menu tells you why;
see [Worker does not work?](../guides/troubleshooting.md).

## Work hours

Workers work during the normal villager work hours, from the morning to the
afternoon. The rest of the day they live like other villagers. In the station menu you
can always see what the worker is doing right now:

- during work: the current step, e.g. *Looking for a tree*, *Chopping a tree (40 %)*,
  *Cutting Oak Stairs*, *Carrying items: Mill*, *Waiting: chests are full*,
  *Done for today*
- otherwise: *Sleeping*, *Going to bed*, *Chatting at the village square*, *Free time*,
  *Fleeing!*, *Hiding (raid)*

If a worker does not work, the status tells you why. See
[Worker does not work?](../guides/troubleshooting.md).

## How workers move items

Workers walk to the front of a chest, open it (you see and hear the lid), move the
items and close it again. At the woodcutter and stonecutter they stand in front of the
station. So make sure the front of every linked chest can be reached.

## Experience

For now all workers are **novices**. Levels that make workers faster or let them carry
more are planned.
