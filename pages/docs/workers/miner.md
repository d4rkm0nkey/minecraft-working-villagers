---
icon: wv/head-miner
workers: [miner]
---

# Miner

Hired with a work contract, the miner works at the **mining bench**. It digs a tunnel
into a hill from where you click: 3 blocks wide and 3 high, with wooden supports like a
mineshaft, side tunnels if you want them, or stairs down. It digs out the ores along the
way, seals off water and lava, and brings everything to its chest.

## Setting it up

1. Place a [mining bench](../blocks-and-items.md#mining-bench) near the hill and link at
   least one **chest** (**+ Chest** in the **Chests** tab above
   the mill).
2. Click **Pick tunnel**. The menu closes; right-click the **side** of the block where the
   tunnel starts. The tunnel goes straight into that block, away from the side you
   clicked; the block is the middle of its bottom row. It has to be within **16 blocks**
   of the bench.
3. Set the **length** with **-** and **+** (1 to 32 blocks), and the **side tunnels**
   (none, 4, 8, ... blocks).
4. A hired villager without a job finds the bench by itself; **Assign** picks a specific
   one.
5. Right-click the miner with its wage (emeralds) and a **pickaxe**. **Fences** and
   **planks** for the supports and **torches** are optional, and so is some
   **cobblestone** to start with (it keeps its own afterwards).

**Without a chest or a pickaxe the miner does not work.** It takes what it needs from a
[village chest](basics.md#village-chests) nearby, too.

## The tunnel

```
   seen from the side            seen from above (side tunnels 4)

   ┌───────────────────┐               │       │
   │ ▓ ▓ ▓ = ▓ ▓ ▓ = ▓ │         ──────┼───────┼──────
   │ ▓ ▓ ▓ | ▓ ▓ ▓ | ▓ │          ═════╪═══════╪═════   tunnel 3 wide
   │ ▓ ▓ ▓ | ▓ ▓ ▓ | ▓ │         ──────┼───────┼──────
   └───────────────────┘               │       │
     = planks, | fences                side tunnels 1 wide, 2 high
```

- **Tunnel:** 3 wide, 3 high. Every **4th block** a support: two fences at the walls and
  three planks under the ceiling, like in a mineshaft. Every **8th block** a torch on the
  wall.
- **Side tunnels:** every 4 blocks, left and right, 1 wide and 2 high: branch mining. They
  leave 3 blocks of stone between them, so you see most ores without digging everything.
- **Stairs:** 1 wide, 3 high, one block down with every block forward. From the bottom you
  can pick a new tunnel. Switch between tunnel and stairs with the button next to the
  length.

## How it works

During work hours the miner:

1. walks to the end of the tunnel,
2. seals **water and lava** around the next block of the tunnel with cobblestone,
3. digs the next block of the tunnel from the top down. It takes about **8 times** as
   long as you with the same pickaxe, at least 1.5 seconds per block,
4. closes **holes in the floor** with cobblestone (a cave, a ravine), so the tunnel never
   ends in a drop,
5. digs the **ores** in the walls, ceiling and floor, and the ores behind them, as far as
   it reaches from the tunnel,
6. puts up the **supports** and **torches** where they are missing,
7. brings everything to its chest when its pockets are full, after **256 blocks** a day
   and when the tunnel is done.

Without fences, planks or torches it digs on and puts them up later, when it has some
again. Make the tunnel longer with **+** and it goes on where it stopped.

## What it digs and what not

- It digs **natural blocks**: stone and its kinds, deepslate, dirt, gravel, sand, clay,
  ores, amethyst and cave plants. Data packs can change that with the block tag
  `workingvillagers:mine_diggable`, the ores it follows with `workingvillagers:miner_ores`.
- **Builds block the tunnel**: planks, rails and chests of a mineshaft, bricks, spawners.
  The miner stops and says where (*the tunnel is blocked at x y z*). Take the block away
  and it goes on.
- A block its pickaxe is **too weak** for (diamond ore with a stone pickaxe) stops it,
  too, until it has a better one.
- An ore next to water or lava stays, so no liquid gets into the tunnel.
- It keeps up to **16 cobblestone** for sealing and delivers the rest with everything
  else. Without cobblestone it waits when there is water or lava to seal.
- The pickaxe wears out like yours. Shortly before it breaks, the miner gives it back
  ([Worn tools](basics.md#worn-tools)) and uses its spare; without one it waits for a new
  one.

## The mill

For you, the mining bench is a **mill**, used like a stonecutter:

| In | Out |
| --- | --- |
| :mc-cobblestone: Cobblestone, cobbled deepslate, stone, andesite, diorite, granite, tuff | :mc-gravel: Gravel |
| :mc-gravel: Gravel | :mc-sand: Sand or :mc-flint: flint |
| Sandstone (all kinds) | 4 :mc-sand: sand |
| Red sandstone (all kinds) | 4 red sand |

So the stone from the mine becomes sand for the [kiln operator's](kiln-operator.md)
glass. Data packs can add recipes of the type `workingvillagers:milling`.
