---
icon: wv/head-apothecary
workers: [apothecary]
---

# Apothecary

Hired with a work contract, the apothecary works at a vanilla **brewing stand**. It makes
glass bottles, fills them with water and brews the potion you order, at the brewing stand
itself, and keeps a stock in its output chests. On the side it **heals injured workers**
and grows **nether wart** next to its brewing stand.

!!! note
    Ordinary (not hired) villagers still become vanilla clerics at brewing stands. A
    vanilla cleric can be hired directly: it stays at its brewing stand, keeps its trades
    and becomes the apothecary of this mod. The apothecary trades like a vanilla cleric.

## Setting it up

1. Place a brewing stand, with **water** within 8 blocks (a small pond is enough).
2. Open the brewing stand: the [tabs](basics.md#the-overview) above its screen lead to the
   overview. Link an **input chest** with the material and an **output chest**.
3. Tab :mc-glass_bottle: **Order**: click the product and set the **stock**.
4. Give it its wage.

## Products

| Product | Material |
| --- | --- |
| :mc-glass_bottle: Glass bottles | 3 :mc-glass: glass make 3, like the vanilla recipe |
| Water bottles | glass bottles (or glass), filled at the water |
| Every potion and splash potion | glass bottles or glass, the ingredients of the brewing steps, :mc-blaze_powder: blaze powder |

For a potion it goes through the brewing steps by itself. A **potion of healing** for
example: water bottle, then :mc-nether_wart: nether wart (awkward potion), then a
:mc-glistering_melon_slice: glistering melon slice. A **splash** potion of healing takes
gunpowder on top. It brews three bottles at a time, 20 seconds per step, like you would.
It only takes the ingredients the order needs; the brewing recipes of data packs count too.

**Glass** comes from the [kiln operator](kiln-operator.md), **sugar** and **glistering
melons** from the [farmer](farmer.md), **spider eyes** and **gunpowder** from what the
[guards](guard.md) bring home. **Blaze powder** you have to fetch from the Nether: one fuels
20 brews.

## Healing

During work hours the apothecary looks for **hired workers** within 32 blocks that miss
at least two hearts. If it has a potion of healing (in its inventory, its input or its
output chests), it walks over and gives it to them. A drinkable potion leaves a glass
bottle, which it uses again. Keep a few potions of healing in stock and the
[guards](guard.md) start every night healed.

## Nether wart

Put **soul sand** next to the brewing stand (within 6 blocks) and give the apothecary a
few :mc-nether_wart: nether warts: it plants them on free soul sand (it keeps one for
brewing), harvests what has grown and plants again. So you only fetch nether wart from the
Nether once.

## Glass bottles for the village

Ordered **glass bottles**, it keeps a stock for the [guards](guard.md#experience), who
fill the experience of their kills into them. A [village chest](basics.md#village-chests)
as output chest brings them to the guards by itself.

The menu shows how many of the order it made today.
