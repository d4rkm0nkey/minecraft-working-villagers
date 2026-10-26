---
icon: wv/work_contract
---

# Getting started: your first lumberjack

This walk-through takes about one in-game day. You need:

- :mc-emerald: 3 emeralds, :mc-paper: 1 paper and :mc-ink_sac: 1 ink sac for the work contract
- :mc-any_axe: 1 axe and :mc-any_log: 1 log for the chopping block
- :mc-any_axe: 1 more axe for the lumberjack to work with
- :mc-chest: 1 chest
- :mc-emerald: a few more emeralds for the wage
- :mc-head_villager: an unemployed villager (no profession, not a nitwit, not a baby)
- :mc-oak_sapling: some natural trees nearby

## 1. Hire the villager

Craft a **work contract** (paper, ink sac and 3 emeralds, shapeless) and right-click an
unemployed adult villager with it. The contract is used up, the villager is now hired.
It has no name; give it one with a name tag :mc-name_tag: if you want to tell your
workers apart.

[[craft shapeless: paper ink_sac emerald emerald emerald -> work_contract]]

A hired villager never takes a vanilla job, so it will not run off to the next
lectern.

## 2. Place a chopping block

Craft a **chopping block** (an axe above a log) and place it close to the trees. The
lumberjack looks for trees within 24 blocks of it.

[[craft: _ any_axe _ / _ any_log _ -> chopping_block]]

## 3. Let it find the chopping block

A hired villager without a job looks for a free workstation within 48 blocks. It walks
to the chopping block and takes the job (green particles). The chopping block's menu now
shows it as the worker.

!!! tip
    To pick a specific villager, right-click the chopping block to open its menu and
    click **Assign**. The menu closes. Now right-click your hired villager: it becomes the
    lumberjack of this chopping block.

## 4. Link a chest for the logs

Place a chest within 16 blocks of the chopping block. In the menu click **+ Chest**,
then right-click the chest. Right-click the chopping block again to finish and get back
to the menu. Without a chest the lumberjack does not work.

## 5. Pay your worker

Every worker carries its own wage and tools. **Right-click the lumberjack** with them
(or throw them next to it, it picks them up if the `mob_griefing` game rule is on):

- **emeralds**: one per working day, up to 64
- **an axe**: the lumberjack wears it out like a player would
- optionally **saplings**: the lumberjack also keeps the saplings from the leaves of
  the trees it fells

The chopping block menu shows what it carries and for how many days it is paid.

## 6. Watch it work

During the villagers' work hours (morning to afternoon) the lumberjack takes its wage,
walks to a tree, chops it down, replants it and brings the logs to the
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
- Keep your workers' tools going with a smith and a village chest: see
  [Tools that never break](guides/tool-repair.md).
