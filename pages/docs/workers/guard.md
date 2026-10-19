---
icon: wv/head-guard
workers: [guard]
---

# Guard

Hired with a work contract, the guard works at a **target**. At night it stands at its post,
or goes hunting, and shoots the monsters around it with bow and arrows. Every shot costs an
arrow, so the village needs a steady supply: that is what the [fletcher](fletcher.md) is
for. The experience of its kills it fills into bottles o' enchanting, which the
[smith](smith.md#bottles-o-enchanting) repairs with.

## Setting it up

1. Place a target where the guard should stand watch. Room above it? Then the guard stands
   **on top** of it: build a tower and put the target at the top.
2. **Right-click the target with an empty hand** to open its overview.
3. Give the guard its wage, a **bow** and **arrows** (it carries up to 64). Optionally link an
   **input chest** with arrows (and spare bows) and an **output chest** for the loot.
4. Tab :mc-bow: **Settings**: how far around the target it guards, 8 to 32 blocks (16 by
   default), and what it does at night: **Guard** or **Hunt**.
5. For the experience: **glass bottles** in its input chest or a village chest.

A [village chest](basics.md#village-chests) works too: the guard takes arrows and bows from
there when it runs low. Let the fletcher bring its arrows into the village chest.

## The night shift

Guards work **at night** and sleep in the morning, the other way round from everyone else:

| Time | The guard |
| --- | --- |
| Dusk to morning | stands at its post and shoots monsters |
| Early morning | picks up its arrows that missed and what the monsters dropped, fills its experience into bottles, brings the loot and the bottles to its output chest |
| Morning | sleeps |
| Afternoon | free time |

At its post it shoots every monster it can see within its radius and at most **24 blocks**
away (zombies, skeletons, spiders, creepers, witches, pillagers, ...), the closest first,
about one arrow every 1.5 seconds, like a skeleton. It does not attack neutral mobs
(endermen, zombified piglins, wolves, golems). Guards **do not panic**.

**Its arrows hit only monsters:** they fly through villagers, players, iron golems and
animals.

## Hunting

Set to **Hunt**, the guard does not wait at its post: at night it roams up to **32 blocks**
around the target and looks for monsters, shooting them up to 24 blocks away. More kills,
more experience and loot, but more arrows and more danger. With **8 arrows** or less, or
with less than half its health, it goes back to its post for the rest of the night.

## Raids

Guards **fight raids**, also in the daytime when they would sleep: they do not hide, and
they guard at least 24 blocks around their target. They shoot **raiders first**. When a
guard sees a raider it rings the closest **bell** (within 48 blocks of its target), at most
every half minute: as in vanilla the raiders glow and the villagers hide.

## Experience

In vanilla only players get experience from monsters. A guard gets the experience of every
monster its arrows kill (a zombie 5, a blaze 10, ...). In the morning it fills it into
**bottles o' enchanting**: 7 experience per bottle, as much as a thrown bottle gives on
average, so no experience comes out of nothing. It needs a glass bottle for each (it keeps
16) and brings the full bottles to its output chest. Throw them for experience yourself, or
leave them to the smith. The menu shows the experience it has not bottled yet.

## Supplies

- **Bow:** wears out one use per shot. Shortly before it breaks the guard gives it back (see
  [Worn tools](basics.md#worn-tools)); keep spare bows in its input chest or a village chest.
  **Infinity** does not help: a guard always uses up an arrow.
- **Arrows:** below 16 it fetches more from its input chest before the night. Without arrows
  it does not stand watch ("needs arrows").

The menu shows how many monsters it killed last night.
