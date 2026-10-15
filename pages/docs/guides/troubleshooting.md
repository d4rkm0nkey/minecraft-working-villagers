# Worker does not work?

First open the worker's menu (the workstation, or the carrier with an empty hand) and
read its **status line**. It usually says what is missing. If it does not, go through
this list.

## What the status means

| Status | What to do |
| --- | --- |
| *Waiting: has no personal chest* | Click **Pick chest** in the menu and right-click a chest within 32 blocks. |
| *Waiting for wages (emerald in their chest)* | Put emeralds into the personal chest. One per day. |
| *Cannot work (chest or order missing)* | Link the chests (lumberjack: one chest; others: an input and an output) and, at a cutter or the bench, set an order. |
| *Waiting: cannot reach the chest* | Clear the front of the chest, or move it to where the worker can walk. |
| *Waiting: cannot reach the saw* / *the bench* / *the blast furnace* | Clear the front of the workstation. |
| *Waiting: chests are full* | Empty the output chests or link another one. |
| *Waiting: needs an axe in their chest* | Put an axe into the lumberjack's personal chest. |
| *Waiting: no tree found* | No natural tree within 16 blocks of the chopping block, or none it can reach. |
| *Done for today* | The lumberjack has felled 64 logs today. It goes on tomorrow. |
| *Waiting for material* | The input chests have no material for the order, or not of the selected wood types. |
| *Stock is full (16/16)* | The carpenter has as many products in its output chests as set. Take some out or raise the stock. |
| *Waiting: cannot carry more* | The carpenter's inventory is full. Make room in the input chests so it can put back what it cannot use. |
| *Waiting for the crops to grow* | No ripe crops and no empty farmland within 8 blocks of the composter. |
| *Waiting: no seeds for the empty farmland* | Put seeds (or carrots, potatoes) into the farmer's personal chest. |
| *Waiting: needs a hoe in their chest* | Put a hoe into the farmer's personal chest. |
| *Waiting for something to smelt* | The smith's input chests have nothing the blast furnace can smelt. |
| *Waiting for fuel* | Put coal, charcoal or other fuel into the smith's input chests. |
| *Nothing to carry, strolling around* | All sources of the carrier are empty, or the targets are full. |
| *Stuck: cannot get anywhere* | The worker is in a hole, boxed in or behind a fence. After a minute it is brought back to its workstation (a carrier: to the source of its first route). If it stays stuck, clear a free spot next to the workstation or dig it out. |
| *Sleeping*, *Free time*, *Chatting ...* | Not work time. Workers work from the morning to the afternoon. |

## Checklist

### Every worker

- [ ] **Is it work time?** Workers only work from the morning to the afternoon. At other
  times the status shows *sleeping*, *free time*, ...
- [ ] **Personal chest set, with an emerald in it?**
- [ ] **Can it reach the chests?** Workers walk to the **front** of a chest. Blocked
  fronts, fences, water or big height differences stop them.
- [ ] **Still assigned?** The note on the workstation shows the worker's name. An empty
  note means nobody works there.

### Lumberjack

- [ ] At least one chest linked.
- [ ] An axe in the personal chest.
- [ ] Natural trees within 16 blocks of the chopping block. Trees with player-placed
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
- [ ] A personal chest with an emerald and a hoe.
- [ ] Farmland with crops within 8 blocks of the composter. The farmer does not till.
- [ ] Seeds in its inventory or its personal chest for empty farmland.

### Smith

- [ ] An input and an output chest linked (the **i** button in the blast furnace screen).
- [ ] Something to smelt **and** fuel in the input chests.

### Carrier

- [ ] At least one route.
- [ ] Something is in a source, and the target has room for it.
- [ ] The carrier still has its basket (it shows on the villager).

## Still stuck?

Please report it with a screenshot of the menu and your setup, see
[Beta notes](../beta.md#reporting-bugs).
