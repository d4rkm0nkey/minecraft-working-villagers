---
icon: wv/head-lumberjack
workers: [lumberjack]
---

# Lumberjack

Hired with a work
contract, works at the **chopping block**.

The lumberjack fells natural trees around its chopping block, replants them and brings
the logs to a chest.

[[craft: _ any_axe _ / _ any_log _ -> chopping_block]]

## Setup

1. Place the chopping block near the trees you want to harvest.
2. A hired villager without a job walks there and takes it. Or click **Assign** to pick
   one (see [Hiring and wages](basics.md#finding-a-workstation)).
3. Link at least one chest with **+ Chest**. **Without a chest the lumberjack does not
   work.**
4. Right-click the lumberjack with emeralds, an axe and, if you like, saplings (see
   [Wage and tools](basics.md#wage-and-tools)).

## How it works

During work hours the lumberjack:

1. takes its wage from the emeralds it carries,
2. looks for the nearest natural tree within **16 blocks** of the chopping block,
3. walks to the trunk and hacks at it, then fells the whole tree,
4. replants a sapling, if it carries one of that kind,
5. brings the wood to the nearest linked chest,
6. rests for 30 seconds and starts again.

It stops for the day after **64 logs**. The chopping block menu shows how many it has
felled today.

### Which trees?

Only **natural** trees: trees that grow on the ground trees grow on
(`#minecraft:substrate_overworld`: dirt, grass, podzol, mycelium, mud, moss) and have
leaves that were not placed by a player. Log houses, log fences and other builds are
left alone.

### Speed

A tree takes the lumberjack **eight times as long** as a player with the same axe. A
better axe makes it faster; Efficiency does not.

### The axe

The axe wears out like yours: one use per log, Unbreaking counts. Shortly before it
breaks, the lumberjack gives it back (see [Worn tools](basics.md#worn-tools)) and uses its
second axe, if it carries one (it takes up to 2). Wooden and stone axes are used up.
Without an axe it waits ("needs an axe"). The menu shows the axe with its durability
bar; the tooltip shows how much of it is left.

### Saplings

The lumberjack only replants with saplings it carries: the ones you give it and the
ones it picks up below the trees. It keeps up to 16 of each kind and hands in the rest
with the logs. It never fetches saplings from a chest. Without saplings it still
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
