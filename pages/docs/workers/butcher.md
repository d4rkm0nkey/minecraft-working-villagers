---
icon: wv/head-butcher
workers: [butcher]
---

# Butcher

Hired with a work contract, the butcher works at a vanilla **smoker**. It keeps the cows,
pigs, chickens and rabbits around the smoker at the herd size you set: below it, it breeds
them; above it, it slaughters. With coal it smokes the meat.

!!! note
    Ordinary (not hired) villagers still become vanilla butchers at smokers. A vanilla
    butcher can be hired directly: it stays at its smoker, keeps its trades and becomes the
    butcher of this mod. The butcher trades like a vanilla butcher.

## Setting it up

1. Place a smoker next to your animal pens.
2. Open it: the [tabs](basics.md#the-overview) above its screen lead to the overview.
3. Link at least one **output chest** (**+ Output**). Optionally link an **input chest**
   with feed and coal for it.
4. Right-click the butcher with its wage (emeralds) and a **sword or an axe**; for breeding
   the food of the animals, for smoking coal or charcoal.

**Without an output chest or a weapon the butcher does not work.**

## How it works

During work hours the butcher looks after the animals within **12 blocks** of the smoker
(4 blocks up or down): cows, mooshrooms, pigs, chickens and rabbits. Each kind counts on its
own; young ones count too.

1. **Too many** of a kind: it slaughters a grown one, **1.5 seconds** per animal. Never
   below **two grown animals** of a kind, and never one that is **named**, **on a lead** or
   in love.
2. **Too few** of a kind: it feeds two grown ones their food (cows: wheat; pigs: carrots,
   potatoes, beetroots; chickens: seeds; rabbits: carrots, dandelions) and they get a young
   one, like after a player fed them.
3. It picks up what the animals drop. **With coal or charcoal** it puts the raw meat into
   the smoker and takes the smoked meat out; without, the raw meat goes to the chest.
4. It brings meat, leather, feathers and the rest to the nearest output chest.

Feed and coal it takes from its input chests when it runs out, or from a
[village chest](basics.md#village-chests).

It reaches **over a fence** like the [shepherd](shepherd.md): a pen needs no gate. Sheep
are left to the shepherd.

The weapon wears out like a player's, one use per animal. Shortly before it breaks, the
butcher gives it back (see [Worn tools](basics.md#worn-tools)); wooden and stone ones are
used up.

The menu shows how many animals it slaughtered today.

## The herd size

Tab :mc-porkchop: **Settings** in the overview: **−** and **+** set how many animals **of
each kind** the butcher keeps, from **0** (no breeding; it slaughters down to two) up to
**32**. The default is **8**. Below it the menu shows how many animals there are around the
smoker now.

## Good to know

- Keep the kinds in separate pens, or at least give each kind room: the butcher walks to
  each animal.
- Name an animal with a name tag, or put it on a lead, to keep it safe.
- You can still use the smoker yourself; the butcher takes out whatever is done.
