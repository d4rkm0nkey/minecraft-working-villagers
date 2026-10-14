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

## Workers

| Option | Default | Range | What it does |
| --- | --- | --- | --- |
| `dailyWage` | 1 | 0–64 | Emeralds a worker takes from its personal chest each work day. **0**: workers work for free, and workers that need no tools (everyone but the lumberjack) then need no personal chest. |
| `maxChests` | 8 | 1–8 | Chests that can be linked to one workstation. |
| `chestDistance` | 16 | 4–32 | How far a linked chest may be from its workstation. |
| `personalChestDistance` | 32 | 4–64 | How far a personal chest may be from the workstation (carrier: from its first route's source). |
| `stuckSeconds` | 60 | 0–600 | After this many seconds of trying to get somewhere without getting anywhere, a worker is brought back to its workstation (carrier: its first route's source). **0**: never; the menu only shows that it is stuck. |

## Lumberjack

| Option | Default | Range | What it does |
| --- | --- | --- | --- |
| `requireMobGriefing` | true | | Lumberjacks only work while the game rule `mob_griefing` is on. Set to `false` to let them work anyway. |
| `searchRadius` | 16 | 4–32 | How far from the chopping block it looks for trees. |
| `dailyLogs` | 64 | 1–4096 | Logs per day, then it stops for the day. |
| `chopSlowdown` | 8.0 | 1.0–32.0 | How many times as long as a player with the same axe it needs for a tree. `1.0` is as fast as you. |
| `restSeconds` | 30 | 0–600 | Break after each delivery. |
| `keepSaplings` | 16 | 1–64 | Saplings of each kind it carries for replanting. |

## Sawyer and stone sawyer

| Option | Default | Range | What it does |
| --- | --- | --- | --- |
| `itemsPerSecond` | 4 | 1–64 | Items made per second, at least one second per cut. |
| `batchSize` | 16 | 1–64 | Items of material fetched from the input chest at once. |

## Carpenter

| Option | Default | Range | What it does |
| --- | --- | --- | --- |
| `secondsPerIngredient` | 0.5 | 0.05–10.0 | Time per ingredient of a piece, at least one second per piece. A chest (8 planks) takes 4 seconds. |
| `ingredientsPerTrip` | 32 | 1–64 | Ingredients fetched from the input chests at once (always at least one piece's worth). |

## Carrier

| Option | Default | Range | What it does |
| --- | --- | --- | --- |
| `maxRouteLength` | 32 | 4–64 | How far apart source and target of a route may be. |
| `maxSourceSpread` | 40 | 4–96 | How far apart the sources of one carrier may be. |
| `stacksPerTrip` | 6 | 1–6 | Stacks carried at once (the basket holds 6). |

!!! note
    Lowering a distance does not unlink chests or routes that are already set up; it only
    applies to new ones.

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
