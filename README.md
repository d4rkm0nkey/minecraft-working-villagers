# Working Villagers

Hire villagers with a work contract and give them real jobs.
This demo adds the **lumberjack**, the **woodcutter** block, two processing
professions: the **sawyer** (woodcutter) and the **stone sawyer** (stonecutter), the
**carpenter** at the **carpenter's bench**, the **farmer** (composter) and the **smith**
(blast furnace, smithing table), the **pit digger** at the **pit stake**, the **kiln operator** at the
**kiln**, the **shop assistant** at the **trading counter**, the **miner** at the
**mining bench**, and the **carrier**, hired
with a **wicker basket**, which carries items between chests.

Minecraft 26.3, Fabric (needs Fabric API) and NeoForge.

📖 **[Player wiki](https://d4rkm0nkey.github.io/minecraft-working-villagers/)**: installation,
[getting started](https://d4rkm0nkey.github.io/minecraft-working-villagers/getting-started/),
every worker, guides and all [configuration options](https://d4rkm0nkey.github.io/minecraft-working-villagers/configuration/).

## Lumberjack

1. **Craft a work contract** (paper + ink sac + 3 emeralds) and right-click an
   adult villager with it. The villager is now under contract. One that already has a
   vanilla job stays at its block and becomes the worker of this mod there (a farmer at
   its composter, a mason as stone sawyer at its stonecutter, a shepherd at its loom, a
   butcher at its smoker, a fisherman at its barrel, an armorer or toolsmith as smith), keeping its trades and level,
   unless its job has no work in this mod (the cartographer). These workers trade like their
   vanilla counterparts (the smith like an armorer at a blast furnace, like a toolsmith at a
   smithing table), level up by trading and restock at their workstation; right-click with
   an empty hand, or sneak, to trade. It gets no
   name (like vanilla villagers; a name tag names it), unless `workers.randomNames` is on.
2. **Place a chopping block** (an axe above a log). A hired villager without a job looks
   for a free workstation of this mod within 48 blocks that it can walk to (chopping
   block, woodcutter, stonecutter, carpenter's bench, composter, loom, smoker, barrel, blast furnace, smithing table, ...), walks
   there and takes the job (green particles), like vanilla villagers claim job sites.
   Right-click the block to open its menu: **Assign** (next to the worker line) closes
   the menu; then right-click a hired villager to pick a specific worker. A worker that
   already works somewhere else moves here. **Remove** takes the worker away again: it is
   unemployed but stays hired, and it does not go back to that same station by itself for
   one day or until the server restarts (another free one it may take). Hired villagers never take vanilla jobs.
3. **Chests:** **+ Chest** below the chest list closes the menu; then right-click chests
   (up to 8, within 16 blocks) to link them, or a linked one to unlink it.
   Right-clicking the chopping block ends this and opens the menu again. The menu shows
   the lumberjack, what it is doing, the logs felled today, and the chests with how full
   they are; the × next to a chest unlinks it.
   **Without a chest the lumberjack does not work.**
4. **Wage and tools:** put emeralds into the chopping block's wage box (or right-click the
   block or the lumberjack with them), and right-click the lumberjack with an axe and
   optionally saplings (see below).
5. During work hours (like other professions) the lumberjack looks for natural trees
   within 24 blocks of the chopping block, walks up to the trunk and hacks at it, fells it, clears the leaves left without a log (keeping their saplings, sticks and apples), replants a sapling and brings the wood to the
   nearest chest. A tree takes eight times as long as for a player with the same axe (for now
   all workers are novices; a master would need five times as long). After each delivery it rests for 15 seconds, and it stops for the day
   after 64 logs. Trees count as natural when they grow on dirt, grass, podzol, mycelium, mud or moss (`#minecraft:substrate_overworld`) and have non-persistent
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
- The overview of a workstation and the screen of the block itself (woodcutter,
  stonecutter, mill, carpenter's bench, kiln, blast furnace, smithing table, loom, smoker,
  barrel, brewing stand, grindstone) have the
  same tabs above them, like the creative inventory: the block, the worker, the wage, the
  chests and the order or settings. A red dot marks the tab where something is missing. A
  vanilla block nobody has set up shows only its own tab and the worker tab.
- The menu of every workstation shows its worker ("Hired villager" in the player's
  language if it has no name), or "Free".
- Hired villagers never take vanilla jobs (lectern etc.). At a stonecutter, composter,
  loom, smoker, barrel, fletching table, brewing stand, cauldron, grindstone, lectern, blast
  furnace or smithing table a hired villager becomes the stone sawyer, farmer, shepherd,
  butcher, fisher, fletcher, apothecary, tanner, weaponsmith, librarian or smith of this
  mod; unhired villagers still become masons, farmers, shepherds, butchers, fishermen,
  fletchers, clerics, leatherworkers, weaponsmiths, librarians, armorers and toolsmiths,
  except at one that is set
  up (chests, order or worker): vanilla job acquisition skips it. *Assign* there makes an
  ordinary villager that works there and has never traded (villager XP 0) unemployed;
  one that has traded keeps its job.
- These workers wear the clothes of the vanilla job (the fisher those of the fisherman,
  the stone sawyer the mason's, the tanner the leatherworker's, the apothecary the
  cleric's, the smith the armorer's), so a hired villager keeps its looks; the profession
  layer maps the texture (`WorkerSkins`). Only the jobs that are new in this mod
  (lumberjack, sawyer, carpenter, guard, pit digger, miner, kiln operator, shop assistant,
  carrier) have clothes of their own.

## Wage and tools

Every workstation has a **wage box** for up to 64 emeralds (the **Wage** tab of its
overview, with a slot and the player's inventory). The worker takes its wage of the day
from there. Right-clicking the workstation with emeralds puts them in too, and so does
giving them to the worker. The emeralds belong to the station: nothing is lost when the
worker dies, becomes a zombie villager or changes its job; a broken or blown-up station
drops them like a chest. A carrier, without a workstation, carries its emeralds itself.

Workers carry their tools in their own inventory. The player gives them
directly, like food to a villager: **right-click the worker** holding emeralds, its tool
or what it plants, or **throw** the items next to it (it walks over and picks them up).
Throwing needs the `mob_griefing` game rule (like vanilla mobs picking up items);
right-clicking always works. The limits below count for both: a worker only takes what it
still wants and leaves the rest on the ground. Giving shows happy villager particles; a
worker that already has enough shakes its head and takes nothing. Workers never hand in their emeralds or their tool to the output
chests. A hired worker that dies drops everything it carries (tools, material, its food
and purse); its wage stays in the wage box.

- **Wage:** each day the worker takes `dailyWage` (default 1) emerald from the wage box,
  then from its pockets, with its first piece of work (a log, a crafted item, a repaired
  tool, a carried load; a guard when it takes up its post). A day it gets nothing done, for
  want of material say, costs nothing. The wage has to be there before it starts, though. A worker from 0.3.0 puts the emeralds it carried
  into the wage box once, as many as fit (saved per worker as
  `WorkingVillagersWageStored`). Without an
  emerald it does not work and waits next to its workstation ("Waiting for wages"),
  checking every few seconds.
- **Meals** (`meals.enabled`): of each wage, `meals.upkeep` (default 50) percent is the
  worker's upkeep and gone; the rest goes into its purse (at most the food money of 3
  days; with a full purse the rest of the wage stays in the wage box). At work it gets
  hungry by `meals.dailyFood` (default 10) food points per work day (2000 to 9000) and eats
  from its lunch bag (4 stacks, apart from its pockets) once it misses half of that. With
  less than a day's food in the bag it buys food at a village chest for two days, as far
  as its purse pays (a day's food money buys `meals.foodBudget`, default 150, percent of a
  day's food), and puts the emeralds into the chest. Missing a whole day's food with an
  empty bag, it eats out: an emerald of its purse buys a day's food money's worth of food
  points (100 %) and is gone; the station menu, its status and the village chest say so,
  and the village chest's wage forecast counts the food money of workers fed there as
  coming back. Hunger never slows work. Food the player gives or throws goes into the bag
  for free. Variety: food of a kind (item tags `workingvillagers:meals/field`,
  `meals/meat`, `meals/fish`, `meals/fruit`; a meal out is a kind of its own) it had in one
  of its last 2 meals counts 25 % less per such meal; it buys the kinds it has least of,
  never food that counts less for its emeralds than eating out, and eats what counts most.
  Saved per worker as `WorkingVillagersMeals`.
- **Lumberjack:** needs an axe (carries up to 2), optionally saplings (up to 16 of each
  kind, `lumberjack.keepSaplings`). The axe wears out like a player's (one use per log,
  Unbreaking counts); shortly before it breaks it is given back (see *Worn tools* below);
  without one it waits ("needs an axe").
  Chopping speed depends on the axe. It only replants with saplings it carries (given,
  or from the leaves of the felled trees) and hands in the rest; it never fetches saplings from
  a chest. Without saplings it still fells, but remembers the place (saved per worker as
  `WorkingVillagersPlantings`, at most 64) and plants there once it has the saplings,
  before the next tree; after planting, or when the place is taken, it forgets it.
- **Farmer:** needs a hoe (carries up to 2), which wears out one use per harvested crop;
  without one it waits ("needs a hoe"). It needs seeds to start with (up to 16 of each
  kind, `farmer.keepSeeds`); afterwards it replants from its harvest.
- **Pit digger:** needs a shovel and a pickaxe (up to 2 of each) and ladders (up to 64,
  `pitDigger.keepLadders`), optionally torches (up to 16, `pitDigger.keepTorches`); for a
  pit with storeys also planks (up to 64, `pitDigger.keepPlanks`) and logs (up to 16,
  `pitDigger.keepLogs`), which it only takes while its pit has floors. The tools wear out
  one use per block.
- **Miner:** needs a pickaxe (up to 2); optionally fences and planks for the supports (up
  to 16 each, `miner.keepFences`, `miner.keepPlanks`) and torches (up to 16,
  `miner.keepTorches`). It keeps up to 16 cobblestone of what it digs
  (`miner.keepSealBlocks`) for sealing water and lava and closing holes.
- **Fisher:** needs a fishing rod (carries up to 2), one use per catch; plain rods are used
  until they break, enchanted or renamed ones are given back before. Optionally a boat (keeps 1) to row out.
- **Butcher:** needs a sword or an axe (carries up to 2), one use per animal. Feed (up to
  16 of each kind, `butcher.keepFeed`) only for breeding, coal or charcoal (up to 16,
  `butcher.keepFuel`) only for smoking.
- **Shepherd:** needs shears (carries up to 2), one use per sheep; shears are used until
  they break. Wheat (up to 16, `shepherd.keepWheat`) only for breeding.
- **Sawyer, stone sawyer, carpenter, smith, kiln operator, shop assistant, carrier:** only
  emeralds.

### Village chests

A **village chest** (shapeless: chest + emerald) is a chest every worker nearby takes its
supplies from by itself: every worker whose workstation (a carrier: the source of its
first route) is within 48 blocks horizontally and 24 up or down
(`villageChests.radius`). Place as many as you like; a worker walks to the closest one
that has what it wants. Giving directly still works.

- **When:** before its first work of the day it fetches whatever runs low; during the day
  only what it cannot work without (no wage, its tool is worn, no ladders). If no village
  chest has it, it waits as before.
- **What:** emeralds for 3 days (`villageChests.wageDays`, into the wage box of its
  workstation) once it has less than a day's wage, but no more than its share of the
  emeralds in the chest (divided among the workers it serves) and at least today's wage; a spare tool once it has none or only one with a tenth of its durability left (the
  most worn one first, so tools get used up; never a worn one, see below);
  saplings, seeds, ladders and torches up to the amount it keeps once it has less than a
  quarter of it (kinds it already has first).
- **Not:** enchanted or renamed tools (`villageChests.takeEnchanted`). Workers put nothing
  into a village chest but their worn tools.
- Two village chests side by side form a double chest, like vanilla chests.
- The village chest's screen has a second tab (left of the chest) with the workers it
  serves, what one of them lacks that no village chest has, and for how many days the
  emeralds pay all their wages. Hoppers and
  carriers can fill it like any chest.
- With `dailyWage` 0 they work for free; they still need their tools.

**Worn tools:** once a tool has 10 % of its durability left (`tools.returnBelow`, at least
2 uses), the lumberjack, farmer, pit digger and miner stop using it and, before their next
piece of work, give it back: into the closest village chest with a free slot where a hired
smith or weaponsmith repairs it (taking a new tool on the same walk), else into an output
chest of their workstation so the player sees it, else into any village chest with room,
else they keep it and try again a minute later. So tools never break. Wooden and stone tools are used up
unless enchanted or renamed (`tools.returnCheap`). Workers never take a worn tool and
refuse one from a player. A smith at a smithing table repairs them (see *Smith*).

The station overview and the carrier menu show a row of slots: the emeralds the worker
carries (with count), its tool with the durability bar (lumberjack and farmer) and its
saplings or seeds. Empty slots are faded, a missing wage or tool is tinted red. Next to
them: "Paid for N more days", "Works for free" or what is missing ("Needs an emerald!",
"Needs an axe!", "Needs a hoe!"). The tooltip shows the details (e.g. whether today's
wage was paid).

A worker that cannot work (no wage, tool or seeds, chests full, chest or station out of
reach, no material, no ore or fuel, no chest linked, stuck) shakes its head with the
"no" sound now and then while a player is within 8 blocks.

## Worker status

The overview of every workstation and the carrier menu show what the worker is doing right now: during work hours the step of its work (looking
for a tree, chopping 40 %, cutting stairs, carrying items for a route, waiting because the
chests are full, done for today, ...), otherwise what its day brings (sleeping, going to
bed, chatting with another villager or at the village square, free time, fleeing, hiding
from a raid).

A worker that cannot reach where it wants to go and does not get anywhere (in a hole, boxed
in, behind a fence) shows *Stuck* after 20 seconds. After a minute (`stuckSeconds`) it is
moved to a free spot next to its workstation (a carrier: to the source of its first route),
unless it could walk there by itself or there is no room there.

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

1. **Set the order** in the overview: the tab *Order* above the station's menu (or a
   click on the order box below the result slot, which shows the current order) opens it.
   At the woodcutter an order is **product shapes and a set of wood types**: a click on a
   shape (planks, slabs, stairs, fences, fence gates, doors, trapdoors, pressure plates,
   buttons, signs, sticks) adds it to the order, another click takes it out, and wood
   types are switched on and off (All / None). Wood types without any of the shapes are
   marked red. Material of other wood types stays in the input chest.
   At the stonecutter the overview shows the **stone families** (stone, granite,
   deepslate, copper, ...: everything connected by stonecutting recipes, data packs
   included). A click opens a family and shows its products; a click on a product adds it
   to the order, another click takes it out. Families with ordered products are marked
   green. Any material that has a recipe for an ordered item is used.
   An order has up to **8 products** (see [Several products](#several-products)).
2. **Worker and chests** are set up in the overview (the tabs for the block, the worker,
   the wage, the chests and the order stand above the station's menu and above the
   overview),
   like at the chopping block: **Assign** / **Remove** for the worker, **+ Input** and
   **+ Output** to link chests by right-clicking them (*input*: material is taken from
   here, *output*: products go here). The list shows how full each chest is; a chest's
   role is switched with one click and the × unlinks it. A double chest is linked as a
   whole, whichever half is clicked.
3. During work hours the worker fetches up to 16 matching items from an input chest
   (with several products a share of that for one of them),
   cuts them at the station and brings the products to an output chest. It makes 4
   items per second (one log into four planks, slabs take twice as long), at least one
   second per cut, with wood or stone chips flying off the saw.
   **Without an order, an input chest (or a village chest) and an output chest the worker does not work.**

### Several products

The sawyers, the carpenter, the kiln operator, the fletcher, the tanner and the weaponsmith
take up to 8 products in one order: a click in the order grid adds a product, a click on a
product of the order selects it, another click takes it out. For each product the row below
the grid sets how many:

- **Order**: a fixed amount once ("64 chests"). When all are in the output chests, the order
  is done: the worker shows a green ✔ above its head and the product leaves the order (the
  kiln keeps its last one, as without an order it fires everything).
- **Stock**: keep that many in the output chests ("keep 8" means 8 chests *and* 8 barrels),
  or without a limit as long as there is material.

Ordered products come first, then the stocks (emptiest first), then those without a limit
(the one made the fewest of first), one batch at a time, always the most needed one there
is material for. A product without material does not hold up the others.

The workers take their material from the input chests **and the village chests** in reach.
When material is missing they make what they can and then show a red **!** above their
head; the carpenter's status says how many are missing ("Missing 18× Oak Planks"). An
ingredient their own station makes they make first (the carpenter the chest and the boat
of a boat with chest, the fletcher arrows for spectral arrows).

A product made of one wood type (stairs at the woodcutter, boats at the bench) counts as
one product in all its wood types. The order box below the result slot of the bench and
the cutters shows the products one after another. The apothecary still brews one potion
at a time. Stations saved before 0.5.0 load with their one product.

The settings of all workstations are stored per dimension
(`data/workingvillagers/workstations.dat`), not in block entities, because the vanilla
stonecutter has none. To show the order and lead to the overview, the stonecutter opens
the cutter menu of this mod (it looks and works like the vanilla one).

## Carpenter's bench and carpenter

The carpenter's bench (three wooden slabs, an iron ingot in the middle, two logs as legs)
is a crafting table for wooden things only: chests, barrels, crafting tables, ladders,
bowls, composters, bookshelves, chiseled bookshelves, wooden tools, boats and boats with chest, shelves,
hanging signs, item frames, paintings, armor stands, fishing rods,
shields, torches, tripwire hooks, trapped chests and campfires. Its menu lists all of them;
the ones the player lacks the ingredients for are greyed out, the tooltip shows what they
need. Pick one and take the result, it is made of the ingredients in the inventory
(shift-click makes as many as possible). The recipes are the vanilla crafting recipes
whose result is in the item tag `workingvillagers:joinery`, so data packs can add more.

A hired villager at the bench becomes a **carpenter**. It is set up like the sawyer:

1. **Order:** pick up to 8 products in the overview (**i**, or a click on the order box below
   the result slot in the bench menu), see [Several products](#several-products). Boats, boats with chest, hanging
   signs and shelves are ordered as a shape with wood types, like at the woodcutter. Any
   other product can be made of any wood (vanilla allows mixed planks); the wood types in
   the overview then say which wood the carpenter may use up.
2. **Stock:** in the overview, **-** / **+** set how many of each product the carpenter keeps in
   the output chests (no limit, 1, 2, 4, ... 256). It stops at that many and goes on as
   soon as some are taken out.
3. During work hours it fetches up to 32 ingredients (`ingredientsPerTrip`, e.g. 4
   chests' worth) from the input chests (they may be spread over several chests; it only sets off when all of them
   together are enough for one piece), makes the products at the bench (half a second
   per ingredient, at least a second per piece, with shavings flying) and brings them to
   an output chest. Material it cannot use goes back to an input chest. Sticks and slabs
   are not made by the carpenter: they come from the sawyer, e.g. via a carrier.

## Farmer

A hired villager at a vanilla **composter** becomes a **farmer** (ordinary villagers still
become vanilla farmers there). Right-click the composter with an empty hand to open its
overview (when it is full, the bone meal comes out first; with an item it composts as usual): assign the worker and link chests, like at the
chopping block. **Without a chest the farmer does not work.**

During work hours it harvests ripe crops (any `CropBlock` at full age: wheat, carrots,
potatoes, beetroots, ...) on farmland within 8 blocks of the composter (3 lower, 2 higher),
1.5 seconds per crop, and plants the same crop again. Empty farmland gets the crop of its
neighbours if it has seeds for it, otherwise the seeds it has most of. It keeps up to 16 of
every kind of seed (`villager_plantable_seeds`), replanting from its harvest, and brings the rest of the harvest to the nearest chest once there is nothing left
to do. It needs a hoe it carries (one use per harvested crop). It does not
till: it only works farmland that is already there. When it waits, it stands next to the
composter, and hired villagers never trample farmland. The menu shows the crops harvested
today.

## Shepherd

A hired villager at a vanilla **loom** becomes a **shepherd** (ordinary villagers still
become vanilla shepherds there). The tabs above the loom screen open the overview:
assign the worker and link chests. **Without a chest the
shepherd does not work.**

During work hours it shears every sheep with wool within 12 blocks of the loom
(`shepherd.pastureRadius`, 4 up or down), 1.5 seconds per sheep, and picks up the wool. It
reaches 3.5 blocks, also over a fence, so a pen needs no gate. While there are fewer sheep
(lambs included) than the **herd size** set at the loom (tab *Settings*, 0 to 32, default
8), it feeds wheat to two grown sheep, which then get a lamb like after a player fed them;
it only starts a new pair while the lambs to come still fit. It brings the wool to the
nearest chest once there is nothing left to do. The menu shows the wool sheared today and
the sheep around the loom.

## Guard

A hired villager at a vanilla **target** becomes a **guard** (the target becomes a point of
interest of this mod; no vanilla villager takes it). Right-click the target with an empty
hand to open the overview. Guards have their own schedule: they **work at night** (from dusk
to the early morning), sleep in the morning and have free time in the afternoon, and they do
not panic. At night the guard stands at its target (on top of it if there is room) and
shoots every monster (`Enemy`, not neutral mobs) it can see within its radius (tab
*Settings*, 8 to 32 blocks, default 16) and at most 24 blocks away (`guard.range`), like a
skeleton, every 1.5 seconds (`guard.secondsPerShot`). Every shot uses an arrow and a use of
the bow; Infinity does not help. Its arrows hit only monsters. In the early morning it picks
up its arrows that missed and what the monsters it killed dropped, and brings the loot to
its output chest. It carries up to 64 arrows (`guard.keepArrows`) and fetches more from its
input chests or a village chest. The menu shows the monsters of last night.

At the target the guard can be set to **hunt**: at night it roams up to 32 blocks around
(`guard.huntRadius`) looking for monsters and goes back to its post with 8 arrows or less or
below half health. **Raids:** guards do not hide or flee; during a raid they work also in
the daytime, guard at least 24 blocks, shoot raiders first and ring the closest bell (within
48 blocks) when they see one, so the raiders glow. **Experience:** a guard gets the
experience of the monsters its arrows kill (players only in vanilla, `guard.experience`) and
fills it into bottles o' enchanting in the morning, 7 per bottle
(`guard.experiencePerBottle`), using its glass bottles (it keeps 16).

At a smithing table the smith repairs with these **bottles o' enchanting** what has no repair
material (bows, fishing rods, shears: a tenth per bottle, `smith.experienceRepairPercent`),
and enchanted items cost a bottle per step on top of the material
(`smith.enchantedNeedsExperience`).

## Fletcher

A hired villager at a vanilla **fletching table** becomes a **fletcher** (ordinary villagers
still become vanilla fletchers there). The fletching table has no screen in vanilla: a
right-click with an empty hand opens the overview. The fletcher makes the ordered products
(arrows, spectral arrows, bow, crossbow) from its input chests at one piece per second and
keeps a **stock** of each in its output chests, like the carpenter. An arrow needs a head, a
stick and a feather and makes 4; the head is flint, a copper nugget or an iron nugget, in
that order. It knaps flint from gravel (1:1) and cuts ingots into nuggets (1 ingot, 9
heads) itself. What the order does not need it puts back into the input chest. It trades
like a vanilla fletcher.

## Apothecary

A hired villager at a vanilla **brewing stand** becomes an **apothecary** (ordinary
villagers still become vanilla clerics there). The tabs above the brewing stand screen open
the overview. The apothecary makes the ordered product and keeps a **stock** of it in its
output chests: **glass bottles** (3 glass, 3 bottles), **water bottles** (it fills glass
bottles at a water source within 8 blocks, `apothecary.waterRadius`) or any **potion** or
splash potion the brewing recipes (`minecraft:brewing`, data packs included) lead to from a
water bottle. A potion it brews at the real brewing stand, three bottles at a time, step by
step along the shortest way (water, awkward potion, potion of healing, ...); the reagents
and blaze powder come from its input chests, glass bottles from glass if needed. It heals
**hired workers** within 32 blocks (`apothecary.healRadius`) that miss two hearts or more
with a potion of healing from its inventory or a chest of the station, and harvests and
replants **nether wart** on soul sand within 6 blocks (`apothecary.wartRadius`), planting
what it carries beyond one. It trades like a vanilla cleric.

## Tanner

A hired villager at a vanilla **cauldron** (empty or with water, lava or powder snow: the
same station) becomes a **tanner** (ordinary villagers still become vanilla leatherworkers
there). The cauldron has no screen: a right-click with an empty hand opens the overview. The
tanner makes the ordered products and keeps a **stock** of each in its output chests: leather,
books (3 paper, 1 leather), leather armor and horse armor, bundles (string, leather) and
saddles (3 leather, an iron ingot). Leather it takes from its input chests, makes of rabbit
hide (4, 1) or **tans** of rotten flesh: up to 16 go into the water of the cauldron (one
level) and are leather after 300 seconds (`tanner.tanningSeconds`), 4 for one
(`tanner.fleshPerLeather`, off with `tanner.tanRottenFlesh`); what is in the cauldron drops
when it is broken. It fills the cauldron with its bucket at water within 16 blocks
(`tanner.waterRadius`), without using up the water. At the cauldron a **color** is set:
leather armor it makes it dyes in that color, and leather armor of another color from its
input chests it washes out in the cauldron (one level) and dyes again, also without an
order. It trades like a vanilla leatherworker.

**Guards wear armor** from a village chest or given to them, one piece per slot; it
protects them and wears out like a player's (`LivingEntity.hurtArmor` does nothing for mobs
in vanilla), and a piece about to break they give back like a worn tool. Villagers show no
armor, but a guard in a leather chestplate wears its tabard in the chestplate's color (a
render layer over the guard's clothes): the uniform of the village.

## Weaponsmith

A hired villager at a vanilla **grindstone** becomes a **weaponsmith** (ordinary villagers
still become vanilla weaponsmiths there). The tabs above the grindstone screen open the
overview. It **forges** the ordered tools and weapons from its input chests and keeps a stock
of each: the crafting recipes whose result is in the item tag `workingvillagers:forgeable`
(axes, pickaxes, shovels, hoes, swords, spears, shears, the mace), except wooden ones, up
to 4 per trip, one every two seconds. Before its order it **grinds together** two damaged
tools of a kind, like a grindstone (both durabilities plus 5%; not enchanted or renamed
ones): worn ones from the village chests in reach and damaged ones from its input chests,
and brings the result back to the chest it came from. And it **disenchants** everything
enchanted in its input chests (not curses, not renamed things), with the experience a
grindstone gives, which it fills into bottles o' enchanting (`guard.experiencePerBottle`)
with its glass bottles. At the grindstone one picks both, one of them or only the order.
It trades like a vanilla weaponsmith.

## Librarian

A hired villager at a vanilla **lectern** becomes a **librarian** (ordinary villagers still
become vanilla librarians there) and enchants at the closest **enchanting table** within 6
blocks of the lectern (`librarian.tableRadius`). A right-click with an empty hand on a
lectern without a book opens the overview. It enchants everything enchantable in its input
chests (tools, weapons, armor, books), up to 3 per trip, one every three seconds, like a
player taking the best offer of the table (`EnchantmentHelper.enchantItem` with the
enchantments of `in_enchanting_table`): the level is two per bookshelf counted like for a
player, at least 8, at most 30, or lower as set at the lectern. It pays one bottle o'
enchanting per 3 levels (`librarian.levelsPerBottle`, 10 at level 30) and 1 to 3 lapis
lazuli, from its input chests or a village chest in reach. It trades like a vanilla
librarian.

## Fisher

A hired villager at a vanilla **barrel** becomes a **fisher** (ordinary villagers still
become vanilla fishermen there). The tabs above the barrel screen open the overview; the
barrel stays a barrel for the player. **Without a chest the fisher does not work.**

From the shore it fishes at the closest bank of water within 16 blocks of the barrel
(`fisher.waterRadius`, a small pond is enough): a bite after 5 to 30 seconds
(`fisher.minBiteSeconds`, `fisher.maxBiteSeconds`, shortened by Lure), then a catch from the
vanilla fishing loot (fish 85, junk 10, treasure 5 only on open water; Luck of the Sea
helps). After 12 catches (`fisher.catchesPerTrip`) it brings them to the chest. With a
**boat** it rows out to open water (7 × 7 blocks, two deep, only water on the way) within 32
blocks (`fisher.seaRadius`): it puts the boat on the water at a bank of that water (a pond
closer to the barrel does not keep it there), rows out, fishes
there, also treasure, rows back, gets out and takes the boat with it. Once in the boat it
keeps going until it is back on land, also after work. Boat trips can be switched off at
each barrel (tab *Settings*). The fisher trades like a vanilla fisherman. The menu shows the
catches of today. Its bobber (`fisher_bobber`, an entity only for the looks, not saved)
floats on the water with the line to the rod and dips at a bite; the catch flies to the
fisher like to a player and goes into its inventory when it arrives.

## Butcher

A hired villager at a vanilla **smoker** becomes a **butcher** (ordinary villagers still
become vanilla butchers there). The tabs above the smoker screen open the overview: assign
the worker and link chests. **Without an output chest the butcher does not work**; input
chests are optional and hold feed and coal for it.

During work hours it looks after the cows, mooshrooms, pigs, chickens and rabbits within 12
blocks of the smoker (`butcher.pastureRadius`, 4 up or down); sheep are the shepherd's. For
each kind on its own it keeps the **herd size** set at the smoker (tab *Settings*, 0 to 32,
default 8): with fewer it feeds two grown animals the food of their kind (wheat, carrots,
seeds, ...) so they get a young one; with more it slaughters grown animals, never below two
of a kind and never named, leashed or animals in love. It reaches 3.5 blocks, over a fence
too, 1.5 seconds per animal. What the animals drop it picks up. If it has coal or charcoal
it smokes the meat in the smoker and takes the smoked meat out; without, the raw meat goes
to the chest with the leather and feathers. The menu shows the animals slaughtered today.

## Smith

A hired villager at a vanilla **blast furnace** becomes a **smith** (ordinary villagers still
become armorers). The tabs above the blast furnace screen open its overview (the
furnace's own tab leads back); there you link input and output chests. There is no order: the smith fetches up to 16 items the blast furnace can
smelt and up to 8 fuel (anything with the `cooking_fuel` component) from the input chests,
loads the blast furnace, takes out what it made and brings that to an output chest. The
blast furnace smelts at its usual speed; the experience stays in it for the next player who
takes something out. An empty bucket in the fuel slot is taken out and handed in.

At a vanilla **smithing table** (ordinary villagers still become toolsmiths there) the
smith **repairs** instead; the tabs above the smithing screen open its overview. It
takes up to 4 items at a time: the worn tools the workers gave back into the village
chests in reach of the table, and anything damaged in its input chests. It repairs them
with the material of their `repairable` component from the input chests, a quarter of
the durability per material like an anvil, never "too expensive" (enchantments and
names stay; enchanted items cost a bottle o' enchanting per step, see the guard), and puts
them back where they came from.

## Pit digger

A hired villager at the **pit stake** (string, stick and planks) becomes a **pit digger**.
The player lays **string** on the ground as a closed ring that touches the stake (strings
connect straight only, each on solid ground, up to `pitDigger.maxUnevenness` 4 blocks up
and down); the stake takes the ring as its pit by itself. Everything inside the ring (seen
from above, any shape, at least 2 blocks) is dug out down to the depth set with *-* / *+*,
counted from the highest string. How big and deep grows with the pit digger's level, from
`pitDigger.noviceArea` 64 blocks and `noviceDepth` 8 to `masterArea` 256 and `masterDepth`
16, the side of the pit growing evenly. The blocks under the strings stay as the rim; while
a string is missing the pit digger waits. A player holding string sees the pit at every
stake within 24 blocks as an outline (`PitPreviews`, drawn on the client with gizmos), or
the strings at fault in red; the overview and the status say what is wrong with a ring.

Below the novice depth the pit gets **storeys**: every 4 blocks a floor of planks across the
pit (the ladder column stays open), laid from below once the three layers under it are dug,
on pillars of logs in a grid every 4 blocks, which grow down with the pit from the bottom up
to the first floor (`Pit.Storeys`).

It digs top down, layer by layer from the ladder outwards, with the shovel or the pickaxe,
whichever is faster (`pitDigger.digSlowdown`: 6 times a player's time, at least
`pitDigger.minSecondsPerBlock` 1.5 seconds per block). As soon as the
ladder column (inside the ring next to the string closest to the stake, or where *Ladder*
puts it) gets deeper it puts a ladder against the wall, and it only gets in and out of the
pit by that ladder: vanilla villagers do not path over ladders, so climbing is moved by
hand. It only digs the block tag `workingvillagers:pit_diggable` (natural ground, stone,
ores, leaves, plants); builds stay, and so does a column under one, blocks its pickaxe is
too weak for and blocks next to water or lava. With torches it lights the floor about every
six blocks. Everything it digs goes to its chest: when the drops of the next block would not
fit into its inventory any more, after `pitDigger.dailyBlocks` 160 blocks a day and when
the pit is done it climbs out and delivers; at the end of the work day it climbs out. The menu shows the size of the pit, the depth and the
blocks dug today.

## Miner

A hired villager at the **mining bench** (smooth stone, any pickaxe, iron ingot and
cobblestone) becomes a **miner**. *Pick tunnel* in the overview (the *i* button of the
mill), then a right-click on the side of a block within the chest distance picks the
tunnel: it goes horizontally into that block, away from the clicked side, the block being
the middle of its bottom row (`Tunnel`, stored at the station). *-* / *+* set the length (1
to `miner.maxTunnelLength` 32), a button switches between a **tunnel** (3 wide, 3 high) and
**stairs** (1 wide, 3 high, one block down per block), and the side tunnels (1 wide, 2 high,
every 4 blocks left and right, 0 to `miner.maxBranchLength` 16 in steps of 4).

The tunnel is a list of sections (a slice of the tunnel, a block of a side tunnel, a step),
each with its cells, floor, shell and fixtures. `DigTunnel` finishes them in order: first it
seals water and lava in the shell, the cells and the floor with cobblestone, then digs the
cells top down (`workingvillagers:mine_diggable`; anything else, or a block its pickaxe is
too weak for, blocks the tunnel with the position in the status), then closes holes in the
floor. It stands only in finished sections or in front of the tunnel. Behind the front it
digs the ores (`workingvillagers:miner_ores`) next to the finished cells and the ores that
hang on them (not next to liquids), puts up the supports (every 4th slice: fences at the
sides, planks under the ceiling; `miner.supports`) and torches (every 8th block) when it has
the material, and closes new holes in the floor. Digging takes `miner.digSlowdown` 6 times a
player's time with the same pickaxe, at least `miner.minSecondsPerBlock` 1.5 seconds. It
delivers like the pit digger (when the next drops do not fit, after `miner.dailyBlocks` 96
blocks a day, when the tunnel is done) and keeps 16 cobblestone.

For players the bench is a **mill** (`CutterMenu` with recipe type
`workingvillagers:milling`, items in `workingvillagers:millable`): cobblestone, cobbled
deepslate, stone, andesite, diorite, granite and tuff to gravel, gravel to sand or flint,
sandstone to 4 sand.

## Kiln operator

The **kiln** (a furnace with four bricks) works like a furnace for players, with the
furnace recipe book, but only fires the furnace recipes that are neither food (recipe book
category "food") nor smeltable in a blast furnace, minus the item tag
`workingvillagers:kiln_excluded`: glass, stone, smooth stone, bricks, terracotta, charcoal,
dyes, ... Anything else stays in its input slot. A hired villager there becomes a **kiln
operator**; the tabs above the kiln screen open the overview. It works like the smith,
and in the overview products can be picked from everything the kiln makes: then it only
fires what makes these products, fires it again until it is the product (cobblestone, stone,
smooth stone; the stone in between stays with it), keeps a stock of each like the carpenter and does
not burn its material as fuel (logs for charcoal). With several products it loads the kiln
for the most needed one. Without a product it fires everything once. What it took out of the kiln is handed in, even if it could burn.

## Shop assistant

A hired villager at the **trading counter** (three wooden slabs over planks with an emerald)
becomes a **shop assistant**. It sells the goods of its input chests to ordinary villagers
with a job within `shopAssistant.tradeRadius` 24 blocks of the counter, through their own
trades: only trades that buy an item for emeralds (no second cost), not sold out, at the
villager's current price (demand included). For the nearest such customer it fetches what
the customer's trades take (up to 64 items), walks over and trades every
`shopAssistant.secondsPerTrade` 2 seconds; each trade uses the offer
(`MerchantOffer.increaseUses` via `notifyTrade`), so the villager earns experience, levels up
and has to restock at its workstation like after trading with a player. The emeralds it
earns are counted apart from its wage and brought to an output chest (at the latest once it
has 32); unsold goods go back into an input chest. The overview shows the emeralds earned
today. Statuses: "Waiting for goods to sell", "nobody nearby buys these goods", "the buyers
are sold out until they restock".

## Carrier

A carrier moves items between chests. It has no workstation: its **wicker basket**
(sugar cane around a stick handle) is workstation and contract at once.

1. **Hand over the basket:** right-click an unemployed adult villager with the basket.
   It takes the basket and becomes a carrier (no work contract needed).
2. **Routes** (up to 3) are managed in the carrier menu: right-click the carrier with an
   empty hand, tab *Routes*. Each route shows its name, source and target (hover for the
   containers and the length); the one it is carrying items for is green.
   - **New route:** click *+ New route* in the first free slot. The menu closes; right-click
     the source container, then the target (right-clicking the source again cancels, an
     unfinished pick runs out after two minutes). Source and target
     may be at most 32 blocks apart, and the sources of one carrier at most 40 blocks from
     each other.
   - **✎** renames a route (up to 24 characters, Enter saves, Esc cancels), **×** deletes it.
   - The tab *Basket* shows what the carrier is doing, the basket contents (which can be
     taken out) and its emeralds.
   - Only the employer can change routes, unless they switch the button at the bottom
     to *Everyone*; operators (and the owner of a singleplayer world) always can.
   - Right-clicking the air with a basket opens the same menu for the routes stored on the
     basket, so they can be set up before handing it over.
3. During work hours the carrier goes through its routes in turn: it fills the basket at
   a source that has items (up to 6 stacks, as many as the target has room for), carries
   them to the target and brings back what did not fit. No filters yet: everything is
   carried. When all sources are empty, it strolls around near its first source and
   looks again every few seconds.
4. **Taking the basket back:** sneak + right-click the carrier with an empty hand. The
   basket comes back with its routes and contents, and so do the carrier's emeralds and
   anything else it carried; the villager is unemployed again. A
   carrier that dies drops its basket.

All carriers are novices for now; later the capacity is meant to grow with the level.

## Configuration

`config/workingvillagers.json` (created on the first start, with comments) sets the wage, the
distances and limits of chests and routes, and the speeds and daily limits of the workers.
Changes take effect after a restart; on a server the server's settings count, and the
menus show those. See [Configuration](https://d4rkm0nkey.github.io/minecraft-working-villagers/configuration/) in the wiki for all options.

## Player wiki

`pages/` is the [player wiki](https://d4rkm0nkey.github.io/minecraft-working-villagers/) (installation, workers, guides), built with MkDocs Material
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
workers take their wage and deliver: a contract hires an unemployed villager but not an
employed one, a hired villager finds a free stonecutter by itself, a worker picks up thrown
emeralds, a station put where another kind stood starts fresh, the lumberjack fells a tree
(also on grass and mud), replants it and spares builds, nobody works without wage, the
sawyer only cuts allowed wood, the stone sawyer cuts bricks, the carpenter stops at its
stock, the carrier empties its source, the farmer harvests and replants (also a raised
bed), the shepherd shears sheep over the fence of a pen and breeds a lamb with wheat, the
guard shoots a husk at night without hitting the cow next to it, gets its experience,
picks up its own arrows in the morning and bottles 20 experience into 2 bottles, a hunting
guard kills a husk far outside its radius, a guard works during a raid in the daytime, the
smith repairs a bow with bottles and waits for bottles for an enchanted pickaxe, the
fletcher makes 24 arrows from gravel and 36 from an iron ingot and stops at its stock, the
apothecary makes glass bottles, fills water bottles at a puddle, brews three potions of
healing from glass bottles (water, nether wart, glistering melon, blaze powder), heals an
injured worker with a potion from its output chest and harvests and replants ripe nether
wart, the tanner makes books, leather from rabbit hide, tans rotten flesh in the cauldron,
fills an empty cauldron with its bucket, makes a red leather chestplate and dyes blue boots
red, a guard puts on a chestplate it is given and boots from a village chest, the
librarian enchants a book at a bare table and an iron pickaxe at level 30 for 10 bottles,
the weaponsmith forges two iron axes, grinds two worn iron pickaxes from a village chest into
one and disenchants a sword into bottles o' enchanting, the
fisher fishes at a pond and rows out on a lake with its boat and takes it back, also past
a pond closer to the barrel, uses up a
plain rod but gives back an enchanted one, the
butcher slaughters cows down to the herd size, breeds a piglet and smokes chicken, a
vanilla farmer is hired and stays at its composter and gets farmer trades, one that has
traded keeps its trades and experience, a cartographer is not hired, hired villagers do not trample farmland, the smith smelts raw iron, the kiln operator
fires glass but leaves meat and raw iron alone and fires cobblestone twice for smooth stone,
the pit digger digs a pit with a ladder down to its bottom without touching the ring and
lays floors on log pillars in pits 10 and 14 deep, the
shop assistant sells wheat to a farmer until its trade is sold out, ordinary villagers do
not take a set-up composter and *Assign* makes room at one, a boxed-in worker is brought
back, a lumberjack takes its wage, the more worn axe and saplings from a village chest but
leaves a renamed axe there, a lumberjack gives back a worn axe into the output chest
(without a smith at the village chest) instead of breaking it while a stone axe is used up, the smith repairs a
worn axe from a village chest and an enchanted pickaxe from its input chest, the miner digs a 3×3 tunnel with supports and a torch, side
tunnels and stairs, digs iron ore in the wall and the one behind it, seals lava and stops
at planks in its way, and the mill turns cobblestone into gravel and gravel into sand and
flint.

- Tests: `common/src/gametest/java`, one function per test, registered by the small test
  mods in `fabric/src/gametest` and `neoforge/src/gametest`.
- Test instances: `common/src/gametest/resources/data/workingvillagers/test_instance`
  (structure, time limit, environment). `test_environment/work_hours.json` sets the clock
  to the villagers' work hours.
- The structures are empty grass platforms written by `tools/generate_test_structures.py`.
- The tests run side by side, so they set `villageChests.radius` to 8: the village chest
  of one test does not reach the workers of the test next to it. The miner digs at a
  player's speed in the tests (`miner.digSlowdown` 1). Each test sets these when it
  starts: on Fabric the mod may load its config file after the tests are registered.
- In the tests the floor of the platform is at y 0 (the air layer above the grass).
- A failure message names the worker's status, e.g. "Waiting for wages", which usually
  tells what went wrong.

## Layout

- `common`: all game logic, compiled into both loader jars (mixins work on both loaders)
- `fabric`, `neoforge`: entrypoints, registration and mod metadata
- `tools`: generators for the textures and the chopping block model (the carpenter's bench
  model and its vise texture are not generated but edited by hand, e.g. in Blockbench)
  (`generate_textures.py <minecraft jar>`), the woodcutter recipes
  (`generate_woodcutting_recipes.py`), the game test structures
  (`generate_test_structures.py`) and the pictures and icons of the wiki
  (`generate_wiki_assets.py <minecraft jar>`, needs Pillow and NumPy)
