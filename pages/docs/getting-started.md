# Getting started: your first lumberjack

This walk-through takes about one in-game day. You need:

- 3 emeralds, 1 paper and 1 ink sac for the work contract
- 1 axe and 1 log for the chopping block
- 1 more axe for the lumberjack to work with
- 2 chests
- a few more emeralds for the wage
- an unemployed villager (no profession, not a nitwit, not a baby)
- some natural trees nearby

## 1. Hire the villager

Craft a **work contract** (paper, ink sac and 3 emeralds, shapeless) and right-click an
unemployed adult villager with it. The contract is used up, the villager is now hired
and gets a name, so you can tell your workers apart.

<div class="recipe" markdown>
<div class="craft"><span>Paper</span><span>Ink Sac</span><span>Emerald</span><span>Emerald</span><span>Emerald</span><span></span><span></span><span></span><span></span></div>
<span class="arrow">→</span>
<span class="craft-result">Work Contract</span>
</div>

A hired villager never takes a vanilla job, so it will not run off to the next
composter or lectern.

## 2. Place a chopping block

Craft a **chopping block** (an axe above a log) and place it close to the trees. The
lumberjack looks for trees within 16 blocks of it.

<div class="recipe" markdown>
<div class="craft"><span></span><span>Axe</span><span></span><span></span><span>Log</span><span></span><span></span><span></span><span></span></div>
<span class="arrow">→</span>
<span class="craft-result">Chopping Block</span>
</div>

## 3. Assign the worker

Right-click the chopping block to open its menu and click **Assign**. The menu closes.
Now right-click your hired villager: it becomes the lumberjack of this chopping block.
The block now carries a paper note with the worker's name.

## 4. Link a chest for the logs

Place a chest within 16 blocks of the chopping block. In the menu click **+ Chest**,
then right-click the chest. Right-click the chopping block again to finish and get back
to the menu. Without a chest the lumberjack does not work.

## 5. Set up the personal chest

Every worker needs its own chest for its wage and tools. Place a second chest (within 32
blocks of the chopping block), click **Pick chest** in the menu and right-click it.
Put into it:

- **emeralds**: one per working day
- **an axe**: the lumberjack wears it out like a player would
- optionally **saplings**: the lumberjack also keeps the saplings that fall from the
  trees it fells

## 6. Watch it work

During the villagers' work hours (morning to afternoon) the lumberjack takes its wage,
grabs the axe, walks to a tree, chops it down, replants it and brings the logs to the
chest. The chopping block menu shows what it is doing and how many logs it has felled
today. After 64 logs it calls it a day.

!!! note "Game rule"
    Lumberjacks need the `mob_griefing` game rule to be on (it is by default). This can
    be turned off in the [configuration](configuration.md).

## Next steps

- Read about [hiring and wages](workers/basics.md) in more detail.
- Add a [sawyer](workers/sawyer.md) to turn the logs into planks.
- Let a [carrier](workers/carrier.md) bring the planks to a
  [carpenter](workers/carpenter.md): see [A wood workshop](guides/wood-workshop.md).
