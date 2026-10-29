---
icon: wv/barrier
---

# Worker does not work?

First open the worker's menu (the workstation, or the carrier with an empty hand) and
read its **status line**. It usually says what is missing. A worker that cannot work
also shakes its head with the "no" sound now and then when you are within 8 blocks. If
the status does not help, go through this list.

## What the status means

| Status | What to do |
| --- | --- |
| *Waiting for wages (give them an emerald)* | Right-click the worker with an emerald (one per work day). |
| *Cannot work (chest or order missing)* | Link the chests (lumberjack and farmer: one chest; others: an input and an output) and, at a cutter or the bench, set an order. A lumberjack also shows this when the game rule `mob_griefing` is off. |
| *Waiting: cannot reach the chest* | Clear the front of the chest, or move it to where the worker can walk. |
| *Waiting: cannot reach the saw* / *the bench* / *the blast furnace* / *the smithing table* / *the kiln* | Clear the front of the workstation. |
| *Cannot get to the post* / *the fletching table* / *the brewing stand* / *the cauldron* / *the grindstone* / *the enchanting table* / *the smoker* | The worker finds no way to its workstation. Clear a path and a free spot next to it. |
| *Waiting: chests are full* | Empty the output chests or link another one. |
| *Waiting: needs an axe (give them one)* | Right-click the lumberjack with an axe. |
| *Waiting: no tree found* | No natural tree within 24 blocks of the chopping block, or none it can reach. |
| *Done for today* | The lumberjack has felled 64 logs today. It goes on tomorrow. |
| *Waiting for material* | The input chests have no material for the order, or not of the selected wood types. |
| *Waiting for material: Oak Planks* | The carpenter's input chests lack this ingredient (or not enough of it) for one piece. |
| *Waiting: no recipe for the order* | The carpenter's bench has no recipe for the ordered product (e.g. a data pack removed it). Pick another order. |
| *Missing 18× Oak Planks* | The carpenter has made what it could of an ordered product (or a stock); for the rest this much is missing in its input chests and the village chests in reach. It shows a red **!** above its head until there is more. |
| *Stock is full (16/16)* | The carpenter has as many products in its output chests as set. Take some out or raise the stock. |
| *Order done* | Everything ordered is in the output chests. Order more in the **Order** tab. |
| *Waiting: cannot carry more* | The carpenter's inventory is full. Make room in the input chests so it can put back what it cannot use. |
| *Waiting for the crops to grow* | No ripe crops and no empty farmland within 8 blocks of the composter. |
| *Waiting: no seeds for the empty farmland (give them some)* | Right-click the farmer with seeds (or carrots, potatoes). |
| *Waiting: needs a hoe (give them one)* | Right-click the farmer with a hoe. |
| *Going to bed* (a guard in the morning) | Guards work at night and sleep in the morning. |
| *Waiting: no arrows (give them some)* | Give the guard arrows, or put arrows into its input chest or a village chest. |
| *Waiting: needs a bow (give them one)* | Give the guard a bow. |
| *Waiting: no order* | Pick a product at the fletching table (right-click it with an empty hand, tab **Order**). |
| *Waiting for Feather* (or another material) | The fletcher's input chests lack it. Arrows need a head (flint, gravel, copper or iron), a stick and a feather. |
| *No enchanting table near the lectern* | Place an enchanting table within 6 blocks of the librarian's lectern. |
| *Waiting for bottles o' enchanting (10 per piece)* | The librarian pays with bottles o' enchanting: put some into its input chest or a village chest, or set a lower level at the lectern. |
| *Waiting for lapis lazuli (3 per piece)* | Put lapis lazuli into its input chest or a village chest. |
| *Waiting for something to enchant* | Its input chests hold nothing enchantable (enchanted things it leaves alone). |
| *Waiting for Iron Ingot* (or Stick, ...) | The weaponsmith's input chests lack material for one piece of its order. |
| *Waiting: no recipe for the order* (weaponsmith) | No crafting recipe makes the ordered tool (a data pack removed it). Pick another order. |
| *Waiting for Leather* (or Paper, Red Dye, ...) | The tanner's input chests lack it. Leather can also be rabbit hide or rotten flesh; dyed armor needs the dye of the color set at the cauldron. |
| *No water in the cauldron (needs a bucket and water nearby)* | Give the tanner a bucket, with water within 16 blocks of the cauldron, or fill the cauldron yourself. |
| *Tanning, done in 3 min* | Rotten flesh is tanning in the cauldron and nothing else is left to do. |
| *Waiting for Glass Bottle* (or another material) | The apothecary's input chests lack it: glass bottles or glass, or an ingredient of the next brewing step. |
| *No brewing recipe leads to the order* | No chain of brewing steps from a water bottle makes the ordered potion (with data packs or other mods, some potions only come from other sources). Order another potion. |
| *Waiting for blaze powder* | The brewing stand has no fuel left. Put blaze powder into the apothecary's input chest. |
| *No water near the brewing stand* | No water within 8 blocks of the brewing stand, or no bank next to it to stand on. |
| *Waiting: the brewing stand is in use* | Someone else brews at the brewing stand, or it holds items the apothecary cannot carry. |
| *Waiting: no water near the barrel* | No water (at least a small pond) within 16 blocks of the barrel, or no bank next to it to stand on. |
| *Cannot get to the water* | The fisher cannot walk to the bank it picked. Make a path, or move the barrel. |
| *Waiting: needs a fishing rod (give them one)* | Right-click the fisher with a fishing rod. |
| *Waiting for the herd to grow* | The butcher's herds are as big as set, or the young ones still have to grow up. |
| *Waiting: no animals near the smoker* | No cows, pigs, chickens or rabbits within 12 blocks of the smoker. |
| *Waiting: no feed to breed the animals (give them some)* | A kind has fewer animals than the herd size. Give the butcher their food (wheat, carrots, seeds, ...) or put it into an input chest. |
| *Waiting: needs a sword or an axe (give them one)* | Right-click the butcher with a sword or an axe. |
| *Waiting for the wool to grow back* | Every sheep around the loom is sheared, and the herd is as big as set. Sheep need grass to grow their wool back. |
| *Waiting: no sheep near the loom* | No sheep within 12 blocks of the loom. Bring two and the shepherd breeds more. |
| *Waiting: no wheat to breed the sheep (give them some)* | There are fewer sheep than the herd size. Right-click the shepherd with wheat, or set the herd size lower. |
| *Waiting: needs shears (give them some)* | Right-click the shepherd with shears. |
| *Waiting for something to smelt* | The smith's input chests have nothing the blast furnace can smelt. |
| *Waiting for fuel* | Put coal, charcoal or other fuel into the smith's input chests. |
| *Waiting for tools to repair* | The smith at the smithing table finds no worn tool in the village chests near it and nothing damaged in its input chests. Nothing to do. |
| *Waiting for Iron Ingot to repair with* | Put the material into the input chests of the smithing table (iron ingots, diamonds, planks, ...). |
| *Waiting for something to fire* | The kiln operator's input chests have nothing the kiln can fire, or nothing for the picked product. |
| *Stock is full (16/16)* at a kiln | The output chests hold as many of the product as set. Take some out or raise the stock. |
| *Waiting: no ring of string at the stake* | Lay a closed ring of string that touches the pit stake. The stake's overview says what is wrong with the ring. |
| *Waiting: The pit is too big (…)* and other ring problems | The ring at the stake is no pit yet: too big, not closed or too uneven. Hold string to see the strings that are wrong in red. Make the ring smaller or mend it; how big a pit may be grows with the pit digger's [level](../workers/pit-digger.md#size-and-level). |
| *Waiting: the string ring is broken* | A string of the ring is gone. Put it back where it was, or lay a new closed ring at the stake. |
| *Waiting: needs a shovel* / *a pickaxe* / *ladders (give them ...)* | Right-click the pit digger with it. It needs ladders once the pit is deeper than one block. |
| *Waiting: needs planks for the floor* / *logs for the pillars (give them some)* | The pit is deeper than 8 blocks and gets [storeys](../workers/pit-digger.md#storeys). Give the pit digger planks or logs, or put them into a village chest. |
| *Waiting: needs a better pickaxe* | Only blocks are left that its pickaxe cannot mine (e.g. diamond ore with a stone pickaxe). Give it a better one. |
| *Waiting: water or lava in the pit* | Only blocks next to water or lava are left. Remove the liquid, then it digs on. |
| *Pit finished, some pillars stay* | Blocks it does not dig (builds, logs) and everything under them stay. |
| *Waiting: cannot reach the pit at x y z* | It found no way to the block at these coordinates, e.g. because of pillars or steps it cannot climb. It tries again after a while. |
| *Waiting: the ladder is blocked* | Something is in the ladder column or on the string above it, so the pit digger cannot climb. Clear the ladder column from the exit down to the bottom. |
| *Waiting: the pit is not loaded* | Part of the ring lies in chunks that are not loaded, because no player is near enough. Stay closer to the pit, or let the pit digger [work without a player](../workers/basics.md#working-without-a-player-nearby). |
| *Waiting: no tunnel picked* | Click **Pick tunnel** at the mining bench, then right-click the side of the block where the tunnel starts. |
| *Waiting: the tunnel is too far from players (not loaded)* | The end of the tunnel lies in chunks that are not loaded. Go closer, the miner goes on by itself; or let it [work without a player](../workers/basics.md#working-without-a-player-nearby). |
| *Waiting: the tunnel is blocked at x y z* | A block the miner does not dig (planks, rails, a chest of a mineshaft, bricks) is in the way. Take it away. |
| *Waiting: needs cobblestone to seal off water or lava* | Give the miner some cobblestone. Afterwards it keeps 16 of what it digs. |
| *Tunnel finished* | Make the tunnel longer with **+**, or pick a new one. |
| *Waiting for goods to sell* | The shop assistant's input chests are empty. |
| *Waiting: nobody nearby buys these goods* | No villager with a job within 24 blocks of the trading counter buys anything from the input chests for emeralds. Check their trades, or put other goods in. |
| *Waiting: the buyers are sold out until they restock* | The buyers' trades are used up. Villagers restock at their workstation during the day. |
| *Nothing to carry, strolling around* | All sources of the carrier are empty, or the targets are full. |
| *Waiting: carries on "…" only as …* | The route is too long for the carrier's level (a novice: half of `carrier.maxRouteLength`). Shorten it or put a chest in between; it learns only by carrying. |
| *Waiting: carries on "…" only as …, before that it does not work that many routes* | A novice works only its first route, an apprentice two. Give the route to another carrier, or wait until this one rises. |
| *Waiting: "…" is longer than … blocks* | The route is longer than any carrier goes (the server lowered `carrier.maxRouteLength`). Shorten it. |
| *Stuck: cannot get anywhere* | The worker is in a hole, boxed in or behind a fence. After a minute it is brought back to its workstation (a carrier: to the source of its first route). If it stays stuck, clear a free spot next to the workstation or dig it out. |
| *Sleeping*, *Free time*, *Chatting ...* | Not work time. Workers work from the morning to the afternoon. |

## Checklist

### Every worker

- [ ] **Is it work time?** Workers only work from the morning to the afternoon. At other
  times the status shows *sleeping*, *free time*, ...
- [ ] **Does it carry an emerald?** The menu shows its emeralds next to the status. If
  not, right-click the worker with emeralds.
- [ ] **Can it reach the chests?** Workers walk to the **front** of a chest. Blocked
  fronts, fences, water or big height differences stop them.
- [ ] **Thrown items stay on the ground?** Workers only pick up thrown items while the
  game rule `mob_griefing` is on, and only what they still want. Right-click the worker
  with them instead.
- [ ] **Still assigned?** The menu of the workstation shows its worker; *Free* means
  nobody works there. A free station within 48
  blocks is taken by the next hired villager without a job; use **Assign** to pick one.

### Lumberjack

- [ ] At least one chest linked.
- [ ] It carries an axe (right-click it with one).
- [ ] Natural trees within 24 blocks of the chopping block. Trees with player-placed
  leaves or on stone are ignored.
- [ ] Game rule `mob_griefing` is on.
- [ ] Not already at 64 logs today.

### Sawyer, stone sawyer, carpenter

- [ ] An **order** is set (the order box below the result slot is not empty).
- [ ] At least one **input** and one **output** chest.
- [ ] The input chest has material for the order **of the selected wood types**.
- [ ] Carpenter: the stock limit is not reached yet. For a carpenter the material of
  all input chests together must be enough for at least one piece.

### Farmer

- [ ] At least one chest linked (right-click the composter with an empty hand).
- [ ] It carries an emerald and a hoe.
- [ ] Farmland with crops within 8 blocks of the composter. The farmer does not till.
- [ ] Seeds for empty farmland (right-click it with some).

### Fisher

- [ ] A chest linked (the **Chests** tab above the barrel screen).
- [ ] It carries an emerald and a fishing rod.
- [ ] Water within 16 blocks of the barrel, with a bank it can stand on.
- [ ] For boat trips: a boat, open water (7 × 7, two deep) within 32 blocks, and **Row out in
  a boat** switched on.

### Butcher

- [ ] An output chest linked (the **Chests** tab above the smoker screen).
- [ ] It carries an emerald and a sword or an axe.
- [ ] Animals within 12 blocks of the smoker, close enough to the fence to reach them (3 blocks).
- [ ] Feed, if it should breed them; coal or charcoal, if it should smoke the meat.

### Shepherd

- [ ] At least one chest linked (the **Chests** tab above the loom screen).
- [ ] It carries an emerald and shears.
- [ ] Sheep within 12 blocks of the loom, close enough to the fence to reach them (3 blocks).
- [ ] Wheat, if it should breed them.

### Smith

- [ ] An input and an output chest linked (the **Chests** tab above the blast furnace screen).
- [ ] Something to smelt **and** fuel in the input chests.
- [ ] At a smithing table: an input chest linked, with the repair material in it.

### Pit digger

- [ ] The overview shows the size of the pit: the ring of string is closed and touches the
  stake. Hold string to see the pit's outline, or the strings that are wrong in red.
- [ ] At least one chest linked.
- [ ] It carries a shovel, a pickaxe and ladders; for a pit deeper than 8 blocks also planks
  and logs.
- [ ] Not already at 160 blocks today.

### Kiln operator

- [ ] An input and an output chest linked (the **Chests** tab above the kiln screen).
- [ ] Something the kiln fires **and** fuel in the input chests. With a product picked,
  only what makes that product counts.

### Shop assistant

- [ ] An input and an output chest linked.
- [ ] Villagers with jobs within 24 blocks of the trading counter whose trades buy the goods
  for emeralds (not hired workers, not nitwits).

### Carrier

- [ ] At least one route.
- [ ] Something is in a source, and the target has room for it.
- [ ] The route is short enough for its level (a novice: 16 blocks), and a novice has it as its first route.
- [ ] The carrier still has its basket (it shows on the villager).

## Still stuck?

Please report it with a screenshot of the menu and your setup, see
[Beta notes](../beta.md#reporting-bugs).
