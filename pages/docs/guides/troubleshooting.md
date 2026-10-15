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
| *Waiting: cannot reach the saw* / *the bench* / *the blast furnace* | Clear the front of the workstation. |
| *Waiting: chests are full* | Empty the output chests or link another one. |
| *Waiting: needs an axe (give them one)* | Right-click the lumberjack with an axe. |
| *Waiting: no tree found* | No natural tree within 16 blocks of the chopping block, or none it can reach. |
| *Done for today* | The lumberjack has felled 64 logs today. It goes on tomorrow. |
| *Waiting for material* | The input chests have no material for the order, or not of the selected wood types. |
| *Waiting for material: Oak Planks* | The carpenter's input chests lack this ingredient (or not enough of it) for one piece. |
| *Waiting: no recipe for the order* | The carpenter's bench has no recipe for the ordered product (e.g. a data pack removed it). Pick another order. |
| *Stock is full (16/16)* | The carpenter has as many products in its output chests as set. Take some out or raise the stock. |
| *Waiting: cannot carry more* | The carpenter's inventory is full. Make room in the input chests so it can put back what it cannot use. |
| *Waiting for the crops to grow* | No ripe crops and no empty farmland within 8 blocks of the composter. |
| *Waiting: no seeds for the empty farmland (give them some)* | Right-click the farmer with seeds (or carrots, potatoes). |
| *Waiting: needs a hoe (give them one)* | Right-click the farmer with a hoe. |
| *Waiting for something to smelt* | The smith's input chests have nothing the blast furnace can smelt. |
| *Waiting for fuel* | Put coal, charcoal or other fuel into the smith's input chests. |
| *Nothing to carry, strolling around* | All sources of the carrier are empty, or the targets are full. |
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
- [ ] **Still assigned?** The note on the workstation shows the worker's name (or
  *Hired villager*; the composter has no note, open its menu instead). An empty note
  means nobody works there. A free station within 48
  blocks is taken by the next hired villager without a job; use **Assign** to pick one.

### Lumberjack

- [ ] At least one chest linked.
- [ ] It carries an axe (right-click it with one).
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
- [ ] It carries an emerald and a hoe.
- [ ] Farmland with crops within 8 blocks of the composter. The farmer does not till.
- [ ] Seeds for empty farmland (right-click it with some).

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
