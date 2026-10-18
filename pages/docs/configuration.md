---
icon: wv/clock
---

# Configuration

The settings are in **`config/workingvillagers.json`** in your Minecraft directory (or the
server directory). The file is created on the first start with every option, its
default and its allowed range, so you can edit it with any text editor.

- Changes take effect after **restarting** the game or server.
- On a server, **the server's settings count**. Players do not need to change anything;
  their menus show the server's values.
- Values outside the allowed range are clamped. An option with an invalid value
  (e.g. text instead of a number) keeps its default, and the log says so.
- If the file cannot be read at all (e.g. a missing bracket), the mod uses the defaults
  and leaves the file alone, so you can fix it.
- Lines starting with `//` are comments.

## Workers (`workers`)

| Option | Default | Range | What it does |
| --- | --- | --- | --- |
| `dailyWage` | 1 | 0–64 | Emeralds a worker takes each work day from the wage box of its workstation. **0**: workers work for free; they still need their tools. |
| `maxChests` | 8 | 1–8 | Chests that can be linked to one workstation. |
| `chestDistance` | 16 | 4–32 | How far a linked chest may be from its workstation. |
| `randomNames` | false | | Give hired villagers a random name (like Anton or Berta). Otherwise only a name tag names them. |
| `stuckSeconds` | 60 | 0–600 | After this many seconds of trying to get somewhere without getting anywhere, a worker is brought back to its workstation (carrier: its first route's source). **0**: never; the menu only shows that it is stuck. |

## Lumberjack (`lumberjack`)

| Option | Default | Range | What it does |
| --- | --- | --- | --- |
| `requireMobGriefing` | true | | Lumberjacks only work while the game rule `mob_griefing` is on. Set to `false` to let them work anyway. |
| `searchRadius` | 16 | 4–32 | How far from the chopping block it looks for trees. |
| `dailyLogs` | 64 | 1–4096 | Logs per day, then it stops for the day. |
| `chopSlowdown` | 8.0 | 1.0–32.0 | How many times as long as a player with the same axe it needs for a tree. `1.0` is as fast as you. |
| `restSeconds` | 30 | 0–600 | Break after each delivery. |
| `keepSaplings` | 16 | 1–64 | Saplings of each kind it carries for replanting (and takes when you give them). |

## Sawyer and stone sawyer (`sawyer`)

| Option | Default | Range | What it does |
| --- | --- | --- | --- |
| `itemsPerSecond` | 4 | 1–64 | Items made per second, at least one second per cut. |
| `batchSize` | 16 | 1–64 | Items of material fetched from the input chest at once. |

## Carpenter (`carpenter`)

| Option | Default | Range | What it does |
| --- | --- | --- | --- |
| `secondsPerIngredient` | 0.5 | 0.05–10.0 | Time per ingredient of a piece, at least one second per piece. A chest (8 planks) takes 4 seconds. |
| `ingredientsPerTrip` | 32 | 1–64 | Ingredients fetched from the input chests at once (always at least one piece's worth). |

## Farmer (`farmer`)

| Option | Default | Range | What it does |
| --- | --- | --- | --- |
| `fieldRadius` | 8 | 2–16 | How far from the composter (sideways) the farmer works the farmland. |
| `secondsPerCrop` | 1.5 | 0.1–20.0 | Time to harvest or plant one crop. |
| `keepSeeds` | 16 | 1–64 | Seeds of each kind it keeps for planting (wheat seeds, carrots, potatoes, ...) and takes when you give them. |

## Butcher (`butcher`)

| Option | Default | Range | What it does |
| --- | --- | --- | --- |
| `pastureRadius` | 12 | 4–24 | How far from the smoker (sideways) the butcher looks after animals. |
| `secondsPerAnimal` | 1.5 | 0.1–20.0 | Time to feed or slaughter one animal. |
| `keepFeed` | 16 | 2–64 | Feed of each kind it keeps for breeding and takes when you give it. |
| `keepFuel` | 16 | 1–64 | Coal or charcoal it keeps for the smoker. |

The herd size is no config option: set it at each smoker (tab **Settings**).

## Shepherd (`shepherd`)

| Option | Default | Range | What it does |
| --- | --- | --- | --- |
| `pastureRadius` | 12 | 4–24 | How far from the loom (sideways) the shepherd looks after sheep. |
| `secondsPerSheep` | 1.5 | 0.1–20.0 | Time to shear or feed one sheep. |
| `keepWheat` | 16 | 2–64 | Wheat it keeps for breeding and takes when you give it. |

The herd size is no config option: set it at each loom (tab **Settings**).

## Smith (`smith`)

| Option | Default | Range | What it does |
| --- | --- | --- | --- |
| `batchSize` | 16 | 1–64 | Items to smelt fetched from an input chest at once. |
| `fuelPerTrip` | 8 | 1–64 | Fuel fetched at once. |

## Kiln operator (`kilnOperator`)

| Option | Default | Range | What it does |
| --- | --- | --- | --- |
| `batchSize` | 16 | 1–64 | Items to fire fetched from an input chest at once. |
| `fuelPerTrip` | 8 | 1–64 | Fuel fetched at once. |

## Shop assistant (`shopAssistant`)

| Option | Default | Range | What it does |
| --- | --- | --- | --- |
| `tradeRadius` | 24 | 4–48 | How far from the trading counter it looks for villagers to sell to. |
| `secondsPerTrade` | 2.0 | 0.25–20.0 | How long one trade takes. |

## Pit digger (`pitDigger`)

| Option | Default | Range | What it does |
| --- | --- | --- | --- |
| `maxArea` | 64 | 4–256 | Blocks of ground a pit may have at most (inside the ring). |
| `maxDepth` | 16 | 1–64 | The deepest depth that can be set at the pit stake. |
| `maxUnevenness` | 4 | 0–8 | How far apart in height the strings of a ring may lie. |
| `dailyBlocks` | 256 | 1–4096 | Blocks per day, then it stops for the day. |
| `digSlowdown` | 8.0 | 1.0–32.0 | How many times as long as a player with the same tool it needs for a block, but at least `minSecondsPerBlock`. `1.0` is as fast as you. |
| `minSecondsPerBlock` | 2.0 | 0.25–20.0 | How long it needs at least for a block, however good its tool (dirt with an iron shovel). Plants take a quarter of it. |
| `keepLadders` | 64 | 1–64 | Ladders it carries at most (and takes when you give them). |
| `keepTorches` | 16 | 0–64 | Torches it carries at most. **0**: it takes none and leaves the pit dark. |

## Miner (`miner`)

| Option | Default | Range | What it does |
| --- | --- | --- | --- |
| `maxTunnelLength` | 32 | 4–64 | How long a tunnel may be at most. |
| `maxBranchLength` | 16 | 0–32 | How long the side tunnels may be at most. **0**: no side tunnels. |
| `dailyBlocks` | 256 | 1–4096 | Blocks per day, then it stops for the day. |
| `digSlowdown` | 8.0 | 1.0–32.0 | How many times as long as a player with the same pickaxe it needs for a block, but at least `minSecondsPerBlock`. |
| `minSecondsPerBlock` | 1.5 | 0.25–20.0 | How long it needs at least for a block, however good its pickaxe. |
| `supports` | true | | Wooden supports every 4 blocks. `false`: none, torches straight on the wall. |
| `keepFences` | 16 | 0–64 | Fences it carries at most. |
| `keepPlanks` | 16 | 0–64 | Planks it carries at most. |
| `keepSealBlocks` | 16 | 1–64 | Cobblestone it keeps of what it digs, for sealing water and lava and closing holes. |
| `keepTorches` | 16 | 0–64 | Torches it carries at most. **0**: none, the tunnel stays dark. |

## Carrier (`carrier`)

| Option | Default | Range | What it does |
| --- | --- | --- | --- |
| `maxRouteLength` | 32 | 4–64 | How far apart source and target of a route may be. |
| `maxSourceSpread` | 40 | 4–96 | How far apart the sources of one carrier may be. |
| `stacksPerTrip` | 6 | 1–6 | Stacks carried at once (the basket holds 6). |

!!! note
    Lowering a distance does not unlink chests or routes that are already set up; it only
    applies to new ones.

## Village chests (`villageChests`)

| Option | Default | Range | What it does |
| --- | --- | --- | --- |
| `enabled` | true | | Workers take their supplies from village chests. `false`: village chests are ordinary chests. |
| `radius` | 48 | 8–128 | How far (horizontally) a village chest may be from a worker's workstation. Up or down it is always 24 blocks. |
| `wageDays` | 3 | 1–16 | For how many days a worker takes its wage at once. |
| `takeEnchanted` | false | | Workers also take enchanted and renamed tools. |

## Tools (`tools`)

| Option | Default | Range | What it does |
| --- | --- | --- | --- |
| `returnBelow` | 10 | 0–50 | Once this many percent of a tool's durability are left (at least 2 uses), the worker stops using it and gives it back, see [Worn tools](workers/basics.md#worn-tools). `0`: tools are used until they break. |
| `returnCheap` | false | | Wooden and stone tools are given back, too. `false`: they are used up, unless they are enchanted or renamed. |

## Example: a relaxed survival setup

```json
{
  "workers": {
    "dailyWage": 0
  },
  "lumberjack": {
    "dailyLogs": 128,
    "chopSlowdown": 4.0
  }
}
```

Options you leave out keep their defaults; the mod adds them to the file on the next
start.
