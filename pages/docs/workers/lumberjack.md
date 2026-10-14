# Lumberjack

<img class="mc" src="../../assets/img/work_contract.png" alt=""> Hired with a work
contract, works at the **chopping block**.

The lumberjack fells natural trees around its chopping block, replants them and brings
the logs to a chest.

<div class="recipe" markdown>
<div class="craft"><span></span><span>Any axe</span><span></span><span></span><span>Any log</span><span></span><span></span><span></span><span></span></div>
<span class="arrow">→</span>
<span class="craft-result">Chopping Block</span>
</div>

## Setup

1. Place the chopping block near the trees you want to harvest.
2. Assign a hired villager (see [Hiring and wages](basics.md#assigning-and-removing)).
3. Link at least one chest with **+ Chest**. **Without a chest the lumberjack does not
   work.**
4. Pick a personal chest and put emeralds, an axe and, if you like, saplings into it.

## How it works

During work hours the lumberjack:

1. takes its wage and an axe from the personal chest,
2. looks for the nearest natural tree within **16 blocks** of the chopping block,
3. takes up to 16 saplings of that kind from the personal chest,
4. walks to the trunk and hacks at it, then fells the whole tree,
5. replants a sapling,
6. brings the wood to the nearest linked chest,
7. rests for 30 seconds and starts again.

It stops for the day after **64 logs**. The chopping block menu shows how many it has
felled today.

### Which trees?

Only **natural** trees: trees that grow on dirt (or grass, podzol, ...) and have
leaves that were not placed by a player. Log houses, log fences and other builds are
left alone.

### Speed

A tree takes the lumberjack **eight times as long** as a player with the same axe. A
better axe (or Efficiency) makes it faster.

### The axe

The axe wears out like yours: one use per log, Unbreaking counts. When it breaks, it is
gone and the lumberjack takes the next axe from its personal chest. Without an axe it
waits ("needs an axe"). The menu tooltip shows how much of the current axe is left.

### Saplings

The lumberjack only replants with saplings it carries. It keeps up to 16 of each kind it
picks up below the trees and hands in the rest with the logs. Without saplings it still
fells trees but does not replant.

## Stripping logs

The switch **Strip logs** in the chopping block menu (off by default) makes the
lumberjack strip every log while felling, so you get stripped logs in the chest. This
uses the stripping rules of the axe, so modded wood works too. Each log takes a bit
longer and wears the axe once more, so the axe lasts half as long.

## Notes

- The `mob_griefing` game rule must be on (unless `requireMobGriefing` is turned off in
  the [configuration](../configuration.md)).
- Keep the area around the chopping block walkable: lumberjacks cannot build their way
  up to a tree. A tree it cannot reach is skipped for a while.
