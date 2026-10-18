---
icon: wv/head-guard
workers: [guard]
---

# Guard

Hired with a work contract, the guard works at a **target**. At night it stands at its post
and shoots the monsters around it with bow and arrows. Every shot costs an arrow, so the
village needs a steady supply: that is what the [fletcher](fletcher.md) is for.

## Setting it up

1. Place a target where the guard should stand watch. Room above it? Then the guard stands
   **on top** of it: build a tower and put the target at the top.
2. **Right-click the target with an empty hand** to open its overview.
3. Give the guard its wage, a **bow** and **arrows** (it carries up to 64). Optionally link an
   **input chest** with arrows (and spare bows) and an **output chest** for the loot.
4. Tab :mc-bow: **Settings**: how far around the target it guards, 8 to 32 blocks (16 by
   default).

A [village chest](basics.md#village-chests) works too: the guard takes arrows and bows from
there when it runs low. Let the fletcher bring its arrows into the village chest.

## The night shift

Guards work **at night** and sleep in the morning, the other way round from everyone else:

| Time | The guard |
| --- | --- |
| Dusk to morning | stands at its post and shoots monsters |
| Early morning | picks up its arrows that missed and what the monsters dropped, brings the loot to its output chest |
| Morning | sleeps |
| Afternoon | free time |

At its post it shoots every monster it can see within its radius and at most **24 blocks**
away (zombies, skeletons, spiders, creepers, witches, pillagers, ...), the closest first,
about one arrow every 1.5 seconds, like a skeleton. It does not attack neutral mobs
(endermen, zombified piglins, wolves, golems). Guards **do not panic**.

**Its arrows hit only monsters:** they fly through villagers, players, iron golems and
animals.

## Supplies

- **Bow:** wears out one use per shot. Shortly before it breaks the guard gives it back (see
  [Worn tools](basics.md#worn-tools)); keep spare bows in its input chest or a village chest.
  **Infinity** does not help: a guard always uses up an arrow.
- **Arrows:** below 16 it fetches more from its input chest before the night. Without arrows
  it does not stand watch ("needs arrows").

The menu shows how many monsters it killed last night.
