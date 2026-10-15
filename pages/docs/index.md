# Working Villagers

**Hire villagers, pay them a wage and let them do the work.**

Working Villagers turns unemployed villagers into paid employees. You hand them a work
contract, give them a workstation and a few chests, and they start working: felling
trees, sawing planks, crafting chests and carrying items from one place to another.
There are no machines: everything is done by villagers walking around, opening chests
and working at their stations.

!!! warning "Beta"
    This is an early beta for testing. Back up your worlds before trying it, and expect
    that saves may not carry over to later versions. See [Beta notes](beta.md).

## The workers

| Worker | Workstation | What it does |
| --- | --- | --- |
| [Lumberjack](workers/lumberjack.md) | Chopping block | Fells natural trees around its block, replants them and brings the logs to a chest |
| [Sawyer](workers/sawyer.md) | Woodcutter | Cuts logs and planks into planks, slabs, stairs, fences, doors, ... |
| [Stone sawyer](workers/sawyer.md) | Stonecutter | Cuts stone into stairs, slabs, walls, bricks, ... |
| [Carpenter](workers/carpenter.md) | Carpenter's bench | Crafts chests, barrels, ladders, tools, boats and other wooden goods |
| [Carrier](workers/carrier.md) | Wicker basket | Carries items between chests along up to three routes |
| [Farmer](workers/farmer.md) | Composter | Harvests and replants the fields around its composter |
| [Smith](workers/smith.md) | Blast furnace | Smelts ore from its input chests in the blast furnace |

Every worker needs a **personal chest** with emeralds in it: it takes one emerald each
day as its wage. No wage, no work.

## How it works in short

1. Craft a **work contract** and right-click an unemployed villager with it.
2. Place a **workstation**, open it and assign the villager.
3. Link **chests**: where the material comes from and where the products go.
4. Pick a **personal chest** and put emeralds in it.
5. During the day the villager works on its own. Its menu shows what it is doing.

New here? Start with [Installation](installation.md) and then
[Getting started](getting-started.md), which walks you through your first lumberjack.

## Features at a glance

- Seven professions that work together: a lumberjack feeds a sawyer, a carrier brings the
  planks to a carpenter.
- Workers really walk to chests, open them and work at their stations, with sounds and
  particles.
- Hired villagers never take vanilla jobs and never leave their job by themselves.
- Each workstation shows the name of its worker on a paper note.
- The menus show what each worker is doing right now: chopping 40 %, waiting for wages,
  done for today, sleeping, ...
- Lumberjacks only touch natural trees, so your log houses are safe.
- New: the **woodcutter**, a stonecutter for wood, and the **carpenter's bench**, a
  crafting table for wooden things. Both are useful for players too.
- Recipes and wood types are data driven, so data packs and other mods can add their
  wood.
- Wage, speeds, distances and limits can be set in a [config file](configuration.md).
- Works on Fabric and NeoForge, in singleplayer and on servers.
