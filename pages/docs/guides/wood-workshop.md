# A wood workshop

This guide connects four workers into a small production chain that turns trees into
chests, without you touching a single log.

```
 trees ──► Lumberjack ──► [Logs]
                             │  Sawyer (planks)
                             ▼
                          [Planks] ──► Carrier ──► [Workshop input]
                                                        │  Carpenter (chests)
                                                        ▼
                                                    [Chests]
```

## What you need

- 4 work contracts (12 emeralds, 4 paper, 4 ink sacs) and 1 wicker basket
- 4 unemployed villagers
- a chopping block, a woodcutter and a carpenter's bench
- 4 chests for the chain plus 4 personal chests
- an axe or two for the lumberjack, emeralds for everybody

## 1. Lumberjack → logs

Place the chopping block near a forest, assign a lumberjack and link the chest
**Logs** as its chest. See [Lumberjack](../workers/lumberjack.md).

## 2. Sawyer → planks

Place a woodcutter next to the **Logs** chest. Assign a sawyer and link:

- **Logs** as input
- a new chest **Planks** as output

Order: shape *planks*, all wood types.

## 3. Carrier → workshop

Hire a carrier and add one route: source **Planks**, target **Workshop input** (a chest
next to the carpenter's bench, at most 32 blocks away).

## 4. Carpenter → chests

Place the carpenter's bench, assign a carpenter and link:

- **Workshop input** as input
- a new chest **Chests** as output

Order: *chest*. Set the stock to, say, 16, so it does not use up all your planks.

## 5. Pay everybody

Each of the four workers needs a personal chest with emeralds (and the lumberjack an
axe). One emerald per worker and day: a week of work costs 28 emeralds.

## Ideas to extend it

- A second sawyer makes **slabs** or **sticks** from the planks; a second route of the
  carrier brings them to the workshop for products that need them (barrels, ladders,
  bookshelves, tools).
- A stone sawyer with a stonecutter makes stone bricks, stairs and walls from cobblestone.
