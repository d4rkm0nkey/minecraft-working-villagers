# Working Villagers

Hire unemployed villagers with a work contract and give them real jobs.
This demo adds the **lumberjack**, the **woodcutter** block, two processing
professions: the **sawyer** (woodcutter) and the **stone sawyer** (stonecutter), and the
**carpenter** at the **carpenter's bench**.

Minecraft 26.3, Fabric (needs Fabric API) and NeoForge.

## Lumberjack

1. **Craft a work contract** (paper + ink sac + 3 emeralds) and right-click an
   unemployed adult villager with it. The villager is now under contract and gets a
   name (unless it already has one).
2. **Place a chopping block** (an axe above a log) and right-click it to open its menu.
   **Assign** (next to the worker line) closes the menu; then right-click a hired
   villager and it becomes the lumberjack of this chopping block. A worker that already
   works somewhere else moves here. **Remove** takes the worker away again: it is
   unemployed but stays hired. Hired villagers never take a job by themselves, and no
   vanilla jobs either.
3. **Chests:** **+ Chest** below the chest list closes the menu; then right-click chests
   (up to 8, within 16 blocks) to link them, or a linked one to unlink it.
   Right-clicking the chopping block ends this and opens the menu again. The menu shows
   the lumberjack, what it is doing, the logs felled today, and the chests with how full
   they are; the × next to a chest unlinks it.
   **Without a chest the lumberjack does not work.**
4. **Personal chest:** every worker needs one (see below). The lumberjack takes its
   wage, an axe and saplings from it.
5. During work hours (like other professions) the lumberjack looks for natural trees
   within 16 blocks of the chopping block, walks up to the trunk and hacks at it, fells it, replants a sapling and brings the wood to the
   nearest chest. A tree takes eight times as long as for a player with the same axe (for now
   all workers are novices; a master would need five times as long). After each delivery it rests for 30 seconds, and it stops for the day
   after 64 logs. Trees count as natural when they grow on dirt, grass, podzol, mud or moss and have non-persistent
   leaves, so builds made of logs are left alone. Needs the `mob_griefing` game rule.
6. **Strip logs** (switch in the chopping block menu, off by default): the lumberjack
   strips every log while felling, so the chest gets stripped logs. It uses the stripping
   rules of its axe (data driven since 26.3), so wood from data packs and mods works too;
   on NeoForge also the stripping rules mods add through the data map. Each log takes a
   bit longer and wears the axe once more, so the axe lasts half as long.

## Workers and workstations

- Workers walk to the front of a chest (or barrel), open it like a player, move the
  items and close it again. At the woodcutter and stonecutter they stand in front of
  the station.
- Every workstation has a paper note on its front with the name of its worker; an
  empty note means the station is free. (A vanilla stonecutter only gets a note once it
  is set up or has a hired worker.) The menu of the woodcutter and stonecutter also
  shows the worker.
- Hired villagers never take vanilla jobs, not even right next to a stonecutter.

## Personal chest and wage

Every worker (lumberjack, sawyer, stone sawyer, carpenter, carrier) has a personal chest. Pick it
with **Pick chest** in the station overview or the carrier menu, then right-click the
chest (within 32 blocks of the workstation, or of the carrier's first route). Clicking
the current chest again takes it away. The menus show how many emeralds are left; the
tooltip shows whether today's wage was paid and, for the lumberjack, its axe and
saplings.

- **Wage:** before work each day the worker takes 1 emerald out of its chest. Without
  a chest or without an emerald it does not work and waits next to the chest
  ("Waiting for wages"), checking every few seconds.
- **Lumberjack tools:** it needs an axe from the chest. The axe wears out like a
  player's (one use per log, Unbreaking counts) and is gone when it breaks; then the
  lumberjack takes the next one, or waits ("needs an axe"). Chopping speed depends on
  the axe. It only replants with saplings it carries: before a tree it takes up to 16
  saplings of that kind from the chest, and it keeps up to 16 of each kind it picks up
  under trees, handing in the rest. Without saplings it still fells, but does not
  replant.

## Worker status

The menus of the chopping block, the woodcutter and stonecutter overview and the carrier
show what the worker is doing right now: during work hours the step of its work (looking
for a tree, chopping 40 %, cutting stairs, carrying items for a route, waiting because the
chests are full, done for today, ...), otherwise what its day brings (sleeping, going to
bed, chatting with another villager or at the village square, free time, fleeing, hiding
from a raid).

## Woodcutter

The stonecutter for wood (iron ingot above three planks): logs, stems and bamboo blocks
become planks, stairs, slabs, fences, doors, signs and so on, planks become stairs,
slabs, buttons and sticks, without the waste of the crafting table. The recipes are
data driven (`workingvillagers:woodcutting`, same format as `minecraft:stonecutting`), so data
packs can add wood from other mods.

## Sawyer and stone sawyer

A hired villager at a woodcutter becomes a **sawyer**, at a vanilla stonecutter a
**stone sawyer**. (Ordinary villagers still become masons at stonecutters.) Both work
the same way:

1. **Set the order** in the overview: the **i** button at the top right of the station's
   menu (or a click on the order box below the result slot, which shows the current
   order) opens it.
   At the woodcutter an order is a **product shape and a set of wood types**: the shape
   (planks, slabs, stairs, fences, fence gates, doors, trapdoors, pressure plates,
   buttons, signs, sticks) is picked and wood types are switched on and off (All / None).
   Wood types without that product are marked red. Material of other wood types stays in
   the input chest.
   At the stonecutter the overview shows the **stone families** (stone, granite,
   deepslate, copper, ...: everything connected by stonecutting recipes, data packs
   included). A click opens a family and shows its products; a click on a product orders
   it, another click clears the order. The family of the current order is marked green.
   Any material that has a recipe for the ordered item is used.
2. **Worker and chests** are set up in the overview (**i** button, back with *Back*),
   like at the chopping block: **Assign** / **Remove** for the worker, **+ Input** and
   **+ Output** to link chests by right-clicking them (*input*: material is taken from
   here, *output*: products go here). The list shows how full each chest is; a chest's
   role is switched with one click and the × unlinks it.
3. During work hours the worker fetches up to 16 matching items from an input chest,
   cuts them at the station and brings the products to an output chest. It makes 4
   items per second (one log into four planks, slabs take twice as long), at least one
   second per cut, with wood or stone chips flying off the saw.
   **Without an order, an input chest and an output chest the worker does not work.**

The settings of all workstations are stored per dimension
(`data/workingvillagers/workstations.dat`), not in block entities, because the vanilla
stonecutter has none. To show the order and lead to the overview, the stonecutter opens
the cutter menu of this mod (it looks and works like the vanilla one).

## Carpenter's bench and carpenter

The carpenter's bench (three wooden slabs, an iron ingot in the middle, two logs as legs)
is a crafting table for wooden things only: chests, barrels, crafting tables, ladders,
bowls, composters, bookshelves, wooden tools, boats and boats with chest, shelves,
hanging signs, item frames, paintings, armor stands, bows, crossbows, fishing rods,
shields, torches, tripwire hooks, trapped chests and campfires. Its menu lists all of them;
the ones the player lacks the ingredients for are greyed out, the tooltip shows what they
need. Pick one and take the result, it is made of the ingredients in the inventory
(shift-click makes as many as possible). The recipes are the vanilla crafting recipes
whose result is in the item tag `workingvillagers:joinery`, so data packs can add more.

A hired villager at the bench becomes a **carpenter**. It is set up like the sawyer:

1. **Order:** pick the product in the overview (**i**, or a click on the order box below
   the result slot in the bench menu). Boats, boats with chest, hanging
   signs and shelves are ordered as a shape with wood types, like at the woodcutter. Any
   other product can be made of any wood (vanilla allows mixed planks); the wood types in
   the overview then say which wood the carpenter may use up.
2. **Stock:** in the overview, **-** / **+** set how many products the carpenter keeps in
   the output chests (no limit, 1, 2, 4, ... 256). It stops at that many and goes on as
   soon as some are taken out.
3. During work hours it fetches the ingredients for up to 32 items' worth from the input
   chests (they may be spread over several chests; it only sets off when all of them
   together are enough for one piece), makes the products at the bench (half a second
   per ingredient, at least a second per piece, with shavings flying) and brings them to
   an output chest. Material it cannot use goes back to an input chest. Sticks and slabs
   are not made by the carpenter: they come from the sawyer, e.g. via a carrier.

## Carrier

A carrier moves items between chests. It has no workstation: its **wicker basket**
(sugar cane around a stick handle) is workstation and contract at once.

1. **Hand over the basket:** right-click an unemployed adult villager with the basket.
   It takes the basket, gets a name and becomes a carrier (no work contract needed).
2. **Routes** (up to 3) are managed in the carrier menu: right-click the carrier with an
   empty hand. Each route shows its name, source and target (hover for the containers
   and the length); the one it is carrying items for is green.
   - **New route:** click *+ New route* in the first free slot. The menu closes; right-click
     the source container, then the target (right-clicking the source again cancels, an
     unfinished pick runs out after two minutes). Source and target
     may be at most 32 blocks apart, and the sources of one carrier at most 40 blocks from
     each other.
   - **✎** renames a route (up to 24 characters, Enter saves, Esc cancels), **×** deletes it.
   - Below are the basket contents, which can be taken out.
   - Only the employer can change routes, unless they switch the button at the top right
     to *Everyone*; operators (and the owner of a singleplayer world) always can.
   - Right-clicking the air with a basket opens the same menu for the routes stored on the
     basket, so they can be set up before handing it over.
3. During work hours the carrier goes through its routes in turn: it fills the basket at
   a source that has items (up to 6 stacks, as many as the target has room for), carries
   them to the target and brings back what did not fit. No filters yet: everything is
   carried. When all sources are empty, it strolls around near its first source and
   looks again every few seconds.
4. **Taking the basket back:** sneak + right-click the carrier with an empty hand. The
   basket comes back with its routes and contents, the villager is unemployed again. A
   carrier that dies drops its basket.

All carriers are novices for now; later the capacity is meant to grow with the level.

## Configuration

`config/workingvillagers.json` (created on the first start, with comments) sets the wage, the
distances and limits of chests and routes, and the speeds and daily limits of the workers.
Changes take effect after a restart; on a server the server's settings count, and the
menus show those. See `pages/docs/configuration.md` for all options.

## Player wiki

`pages/` is the player wiki (installation, workers, guides), built with MkDocs Material
and published to GitHub Pages by `.github/workflows/pages.yml` on every push to `main`
that touches it. Local preview:

```
docker run --rm -it -p 8000:8000 -v "$PWD/pages:/docs" squidfunk/mkdocs-material:9.6.14
```

## Building

```
./gradlew build
```

The jars end up in `fabric/build/libs` and `neoforge/build/libs`.
`./gradlew :fabric:runClient` / `./gradlew :neoforge:runClient` start a dev client.

## Game tests

`./gradlew build` also runs the game tests on a test server of each loader
(`./gradlew :fabric:runGameTest`, `./gradlew :neoforge:runGameTestServer` on their own).
They hire villagers and set up every workstation like a player would, then check that the
workers take their wage and deliver: the lumberjack fells a tree, replants it and spares
builds, nobody works without wage, the sawyer only cuts allowed wood, the stone sawyer
cuts bricks, the carpenter stops at its stock, the carrier empties its source.

- Tests: `common/src/gametest/java`, one function per test, registered by the small test
  mods in `fabric/src/gametest` and `neoforge/src/gametest`.
- Test instances: `common/src/gametest/resources/data/workingvillagers/test_instance`
  (structure, time limit, environment). `test_environment/work_hours.json` sets the clock
  to the villagers' work hours.
- The structures are empty grass platforms written by `tools/generate_test_structures.py`.
- A failure message names the worker's status, e.g. "Waiting for wages", which usually
  tells what went wrong.

## Layout

- `common`: all game logic, compiled into both loader jars (mixins work on both loaders)
- `fabric`, `neoforge`: entrypoints, registration and mod metadata
- `tools`: generators for the textures and the chopping block model (the carpenter's bench
  model and its vise texture are not generated but edited by hand, e.g. in Blockbench)
  (`generate_textures.py <minecraft jar>`) and the woodcutter recipes
  (`generate_woodcutting_recipes.py`)
