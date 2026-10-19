---
icon: wv/oak_sapling
---

# Beta notes

Working Villagers is in an early beta. Thanks for testing!

## Before you start

- **Back up your world.** Saves may not carry over to the next version.
- Test in a separate world rather than in your main one.

## Known limitations

- All workers are **novices**. Levels are planned.
- Carriers have **no filters** yet: they carry everything.
- Workers only work while their chunks are loaded, i.e. while a player is nearby.
- A worker that gets stuck (in a hole, boxed in) is brought back to its workstation after
  a minute. If there is no room at the workstation either, it stays stuck until you free it.
- No integration with JEI/EMI/REI or Jade yet.
- Villagers show no armor. A [guard](workers/guard.md#armor) in a leather tunic wears its
  tabard in the tunic's color; other armor protects it but is not visible.
- Some things are not in vanilla: the [tanner](workers/tanner.md) tans rotten flesh into
  leather (can be switched off), the [fletcher](workers/fletcher.md) makes arrowheads of
  copper and iron nuggets, and the [guards](workers/guard.md#experience) get the experience
  of the monsters they kill.
- The cartographer has no job in this mod yet: cartographers cannot be hired.
- Saves from 0.4.0 should load, but this has not been tested in many worlds. **Back up
  first.** An order now holds several products; a station from 0.4.0 keeps its one product.
  A world saved with 0.5.0 cannot go back to 0.4.0: the workstations would lose their settings.
- The [apothecary](workers/apothecary.md) still brews one potion at a time; the other
  workers with an order take [several products](workers/basics.md#several-products).

What comes next is on [Planned features](planned.md).

## Reporting bugs

Please include:

- Minecraft version, loader (Fabric or NeoForge) and mod version
- what you did, what you expected and what happened
- a screenshot of the worker's menu and the setup around it
- the log file (`logs/latest.log`) or crash report, if there is one

Other mods can matter, especially mods that change villagers. Please list them.
