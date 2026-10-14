# Hiring and wages

These rules apply to every worker.

!!! info "Numbers"
    All numbers in this wiki (wage, distances, speeds, limits) are the defaults. They can
    be changed in the [configuration](../configuration.md).

## Hiring

- **Lumberjack, sawyer, stone sawyer, carpenter:** right-click an unemployed adult
  villager with a [work contract](../blocks-and-items.md#work-contract). Then assign it at
  a workstation.
- **Carrier:** right-click an unemployed adult villager with a
  [wicker basket](../blocks-and-items.md#wicker-basket). The basket is contract and
  workstation at once, no work contract needed.

Only villagers without a profession can be hired: no nitwits, no babies, no villagers
that already have a job. A hired villager gets a random name unless it already has one.

Hired villagers **never take a vanilla job** by themselves, not even when they stand
right next to a stonecutter or a composter. They do take part in normal village life:
they sleep in beds, meet at the village bell and flee from zombies and raids.

## Assigning and removing

Open the workstation menu (at the woodcutter, stonecutter and carpenter's bench: the
**i** button at the top right of the crafting menu) and click **Assign**. The menu
closes; right-click a hired villager. It becomes the worker of this station. A villager
that already works somewhere else moves to the new station.

**Remove** takes the worker away again. It stays hired and keeps its name, so you can
assign it somewhere else without a new contract.

Every workstation has a small **paper note** on its front with the name of its worker.
An empty note means the station is free.

## Linking chests

Workers take their material from chests and put their products into chests. Chests and
barrels can be linked, and every other block a hopper can put items into (also from
other mods):

- up to **8 chests** per station
- within **16 blocks** of the station

Click **+ Chest** (lumberjack) or **+ Input** / **+ Output** (the others). The menu
closes; right-click the chests you want to link. Right-click a linked chest again to
unlink it. Right-click the workstation to finish; its menu opens again.

The menu lists the linked chests with a bar showing how full each one is. One click on
*Input* / *Output* switches a chest's role, the **×** unlinks it.

## Personal chest and wage

Every worker has its own **personal chest**: click **Pick chest** in the station menu
(or in the carrier menu) and right-click a chest within **32 blocks** of the workstation
(for a carrier: of its first route). Click the current chest again to take it away.

- **Wage:** before work each day the worker takes **1 emerald** from its personal chest.
  Without a chest, or without an emerald in it, it does not work. It waits next to the
  chest ("Waiting for wages") and checks again every few seconds, so you can pay it
  late.
- **Tools:** the lumberjack also takes its axe and saplings from this chest.

The menu shows how many emeralds are left, i.e. for how many days the worker is paid.
The tooltip tells you whether today's wage has been paid.

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
