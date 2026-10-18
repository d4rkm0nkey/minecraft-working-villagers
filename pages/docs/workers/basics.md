---
icon: wv/emerald
---

# Hiring and wages

These rules apply to every worker.

!!! info "Numbers"
    All numbers in this wiki (wage, distances, speeds, limits) are the defaults. They can
    be changed in the [configuration](../configuration.md).

## Hiring

- **Lumberjack, sawyer, stone sawyer, carpenter, farmer, smith, pit digger, kiln operator, shop assistant:** right-click an unemployed adult
  villager with a [work contract](../blocks-and-items.md#work-contract). Then it looks for
  a free workstation by itself.
- **Carrier:** right-click an unemployed adult villager with a
  [wicker basket](../blocks-and-items.md#wicker-basket). The basket is contract and
  workstation at once, no work contract needed.

Only villagers without a profession can be hired: no nitwits, no babies, no villagers
that already have a job. Hired villagers get no name, just like vanilla villagers. Use a
name tag to tell them apart. (The config option `randomNames` gives them a random name
like Anton or Berta.)

Hired villagers **never take a vanilla job** (lectern, cartography table, ...). They do take
part in normal village life: they sleep in beds, meet at the village bell and flee from
zombies and raids.

## Finding a workstation

A hired villager without a job looks for a **free workstation of this mod** within
**48 blocks** that it can walk to: a chopping block, woodcutter, stonecutter, carpenter's
bench, composter, blast furnace, smithing table, pit stake, kiln, trading counter or mining bench. It walks there and takes the job (green particles),
just like vanilla villagers claim their job sites.

At a stonecutter, composter, blast furnace or smithing table a hired villager becomes the
stone sawyer, farmer or smith of this mod. Unhired villagers still become vanilla masons,
farmers, armorers and toolsmiths there, but not at one you have set up (linked chests or set an order): that one
is kept for a hired worker. **Assign** at a station where an ordinary villager already
works gives it to your worker, as long as that villager has never traded; one that has
traded keeps its job.

## Assigning and removing

To pick a specific worker yourself, open the workstation menu (at the woodcutter,
stonecutter, carpenter's bench, blast furnace, smithing table and kiln: the button with the worker's face at the top right of
its screen; at the composter: right-click it with an empty hand) and click **Assign**. The menu closes; right-click a hired villager. It becomes the
worker of this station. A villager that already works somewhere else moves to the new
station.

**Remove** takes the worker away again. It stays hired and keeps its name. It does not
go back to that same station by itself for one day, but it may take another free one.

The menu of every workstation shows its worker, or *Free* if nobody works there.

### The overview

The overview of a workstation has tabs at the top, like the creative inventory:

- The block itself, if it has a screen of its own (woodcutter, furnace, ...): back there.
- The worker's face: who works here, **Assign** / **Remove**, what it is doing, what it
  did today and what it carries.
- :mc-emerald: **Wage**: the station's [wage box](#the-wage-box) and your inventory.
- :mc-chest: **Chests**: the linked chests with **+ Input** / **+ Output** (or **+ Chest**).
- :mc-paper: **Order** (woodcutter, stonecutter, carpenter's bench, kiln) or **Settings**
  (chopping block, pit stake, mining bench).

A red dot on a tab means something is missing there: no worker, no wage or tool, no
chest, no order.

## Linking chests

Workers take their material from chests and put their products into chests. Chests and
barrels can be linked, and every other block a hopper can put items into (also from
other mods):

- up to **8 chests** per station
- within **16 blocks** of the station

Click **+ Chest** (lumberjack, farmer and pit digger) or **+ Input** / **+ Output** (the others). The menu
closes; right-click the chests you want to link. Right-click a linked chest again to
unlink it. Right-click the workstation to finish; its menu opens again.

The menu lists the linked chests with a bar showing how full each one is. The **×**
unlinks a chest. At the stations with input and output chests, one click on *Input* /
*Output* switches a chest's role.

### Naming chests

The menus list chests by name. To name one, open the chest (or barrel), click **✎** at the
top right, type the name (up to 24 characters) and press Enter. The name is the same as one
given with an anvil: it also shows as the chest's title. A chest without a name is listed
by its kind (*Chest*, *Barrel*) in gray.

## The wage box

Every workstation has a **wage box** for up to **64 emeralds**. The worker takes its wage
from there before work each day. Fill it in three ways:

- open the workstation, tab :mc-emerald: **Wage**, and put emeralds into its slot (or
  shift-click them in), like into a chest,
- **right-click the workstation** with emeralds in your hand,
- give them to the worker (right-click it with emeralds, or throw them to it): it puts
  them into the box.

The emeralds belong to the station, not to the worker. **Nothing is lost** when the worker
dies, turns into a zombie villager or moves to another station; the next worker there
is paid from the same box. When the workstation is broken (or blown up), the emeralds
drop like the contents of a chest. Take them out again in the **Wage** tab.

A [carrier](carrier.md) has no workstation: it carries its emeralds itself.

## Wage and tools

Give a worker its tools directly, like you give a villager food:
**right-click the worker** with emeralds, its tool or what it plants, or **throw** the
items next to it. It walks over, picks them up and keeps them in its own inventory.
Happy villager particles show that it took them. If it already has enough, it shakes
its head and takes nothing.

The limits in the table count for both ways: a worker only picks up what it still wants
and leaves the rest on the ground. Throwing needs the `mob_griefing` game rule (like
vanilla mobs picking up items); right-clicking always works.

| Worker | Needs | Carries up to |
| --- | --- | --- |
| every worker | :mc-emerald: emeralds | 64, in the wage box of its workstation |
| lumberjack | :mc-any_axe: an axe, optionally :mc-oak_sapling: saplings | 2 axes, 16 saplings of each kind |
| farmer | :mc-any_hoe: a hoe, :mc-wheat_seeds: seeds to start with | 2 hoes, 16 seeds of each kind |
| pit digger | :mc-iron_shovel: a shovel, :mc-iron_pickaxe: a pickaxe, :mc-ladder: ladders, optionally :mc-torch: torches | 2 of each tool, 64 ladders, 16 torches |

- **Wage:** before work each day the worker takes **1 emerald** from the wage box (then
  from its pockets, if it still carries emeralds from an older version of the mod).
  64 emeralds pay for 64 work days. Without an emerald it does not work. It waits next
  to its workstation ("Waiting for wages") and checks again every few seconds, so you
  can pay it late.
- **Tools:** the lumberjack needs an axe, the farmer a hoe, the pit digger a shovel, a
  pickaxe and ladders. The sawyer, stone sawyer, carpenter, smith, kiln operator, shop
  assistant and carrier only need emeralds.
- Workers never hand in their emeralds or their tool to the output chests.
- **A hired worker that dies drops everything it carries:** tools and material. Its
  wage stays in the wage box.

!!! note
    With `dailyWage` set to 0 in the [configuration](../configuration.md) workers work
    for free. They still need their tools.

The station menu (and the carrier menu) shows a row of **slots** with what the worker
carries: its emeralds, its tools with the durability bar and its saplings, seeds,
ladders or torches. Empty
slots are faded, a missing wage or tool is tinted red. Next to them it says *Paid for 5
more days*, *Works for free* or what is missing, e.g. *Needs an emerald!*. The tooltip
shows the details, e.g. whether today's wage has been paid, and how many village chests
the worker has nearby.

## Village chests

Instead of giving every worker its wage and tools, put them into a
:mc-village_chest: **[village chest](../blocks-and-items.md#village-chest)**. Every worker
whose workstation is within **48 blocks** of it (and 24 blocks up or down) takes what it
needs from there by itself. A carrier counts from the source chest of its first route.
Place as many village chests as you like; a worker walks to the closest one that has
what it wants ("Fetching supplies from a village chest").

- **Before its first work of the day** it fetches whatever runs low. **During the day**
  it only goes when it cannot go on otherwise: no wage, its tool is worn, no ladders left.
- **Emeralds:** once its wage box holds less than a day's wage, it takes the wage for
  **3 days** and puts it into the wage box.
- **Tools:** once it has none, or only one that is about to break, it takes one. It takes
  the **most worn** one first, so half used tools get used up and the new ones wait. It
  never takes one that is [worn](#worn-tools).
- **Saplings, seeds, ladders, torches:** once it has less than a quarter of what it
  keeps, it fills up to that amount, kinds it already carries first.
- It never takes **enchanted or renamed tools**: give those directly.
- Workers put nothing into a village chest except their [worn tools](#worn-tools). Hoppers
  and [carriers](carrier.md) can fill it like any chest, e.g. with the saplings from the
  lumberjack's chest.
- If no village chest has what it needs, it waits as before.

Right-click the village chest to open it. Next to its slots it lists the **workers it
serves**, marks the ones that lack something no village chest has (*Needs an axe!*), and
says for **how many days** the emeralds (in their village chests and pockets) pay all their
wages. If workers gave back worn tools, it says how many lie in the chest.

## Worn tools

Workers do not break their tools. Once a tool has only **10 %** of its durability left
(at least 2 uses), the worker stops using it and, before its next piece of work, gives it
back ("Giving back a worn tool"):

1. into the closest **village chest** with a free slot, taking a new tool from it on the
   same walk if it needs one,
2. otherwise into an **output chest** of its workstation,
3. otherwise it keeps the tool and tries again a minute later.

Then it works on with its spare tool, or waits for a new one ("needs an axe"). A
[smith at a smithing table](smith.md#repairing-tools) repairs the worn tools in the
village chests; or repair them yourself at an anvil and put them back. Workers
never take a worn tool, and you cannot give them one.

**Wooden and stone tools** are cheap: they are used until they break, unless they are
enchanted or renamed. The tooltip of the worker's supplies says when it carries a worn tool
to give back. Both limits can be changed in the [config](../configuration.md#tools-tools).

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
