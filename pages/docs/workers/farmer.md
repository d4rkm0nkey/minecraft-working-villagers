# Farmer

Hired with a work contract, the farmer works at a vanilla **composter**. It harvests the
ripe crops on the farmland around the composter, plants them again and brings the
harvest to a chest.

!!! note
    Ordinary (not hired) villagers still become vanilla farmers at composters. Only hired
    villagers become farmers of this mod.

## Setting it up

1. Place a composter next to your fields.
2. **Right-click** the composter with an **empty hand** to open its overview. (With an
   item in your hand it composts as usual; when it is full, the bone meal comes out
   first.)
3. **Assign** a hired villager, link at least one **chest** (**+ Chest**) and pick its
   **personal chest**.
4. Put its wage (emeralds), a **hoe** and some **seeds** into the personal chest.

**Without a chest or a hoe the farmer does not work.**

## How it works

During work hours the farmer:

1. looks for **ripe crops** (wheat, carrots, potatoes, beetroots, and crops from mods
   that work like them) on farmland within **8 blocks** of the composter, up to 3 blocks
   lower and 2 blocks higher,
2. walks over and harvests them (**1.5 seconds** per crop), then plants the same crop
   again in the same spot,
3. plants **empty farmland** too: with the crop of the neighbouring spots if it has
   seeds for it, otherwise with the seeds it has most of,
4. brings the harvest to the nearest chest once there is nothing left to do (or its
   inventory is full).

It keeps up to **16** of every kind of seed (wheat seeds, beetroot seeds, carrots,
potatoes, ...) for planting and hands in the rest. When it has no seeds left, it takes
some from its personal chest.

The hoe wears out like a player's, one use per harvested crop, and is gone when it
breaks. Then the farmer takes the next one from its personal chest, or waits ("needs a
hoe"). The tooltip of the personal chest in the menu shows how much of the hoe is left and
how many seeds there are.

The menu shows how many crops it harvested today.

## Good to know

- The farmer **does not till**. It only works farmland that is already there, so it
  never changes anything but crops. Hoe the field yourself and keep water nearby, or the
  farmland dries out.
- Hired villagers **never trample farmland**, not even when they jump onto it.
- When it waits, the farmer stands **next to** the composter, not on it.
- Raised beds and sunken fields work too, as long as the farmer can walk next to them.
- Crops only grow while a player is nearby (vanilla random ticks).
