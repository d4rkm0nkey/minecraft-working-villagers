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
   unless its job has no work in this mod (librarian, ...). These workers trade like their
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
   within 16 blocks of the chopping block, walks up to the trunk and hacks at it, fells it, replants a sapling and brings the wood to the
   nearest chest. A tree takes eight times as long as for a player with the same axe (for now
   all workers are novices; a master would need five times as long). After each delivery it rests for 30 seconds, and it stops for the day
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
  stonecutter, mill, carpenter's bench, kiln, blast furnace, smithing table, loom) have the
  same tabs above them, like the creative inventory: the block, the worker, the wage, the
  chests and the order or settings. A red dot marks the tab where something is missing. A
  vanilla block nobody has set up shows only its own tab and the worker tab.
- The menu of every workstation shows its worker ("Hired villager" in the player's
  language if it has no name), or "Free". (Earlier versions put a paper note with the
  name on the front of the stations; notes left in a world remove themselves when they are
  loaded.)
- Hired villagers never take vanilla jobs (lectern etc.). At a stonecutter, composter,
  loom, smoker, barrel, blast furnace or smithing table a hired villager becomes the stone
  sawyer, farmer, shepherd, butcher, fisher or smith of this mod; unhired villagers still
  become masons, farmers, shepherds, butchers, fishermen, armorers and toolsmiths, except at one that is set
  up (chests, order or worker): vanilla job acquisition skips it. *Assign* there makes an
  ordinary villager that works there and has never traded (villager XP 0) unemployed;
  one that has traded keeps its job.

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
chests. A hired worker that dies drops everything it carries (tools, material); its wage
stays in the wage box.

- **Wage:** before work each day the worker takes `dailyWage` (default 1) emerald from the
  wage box, then from its pockets (emeralds carried from older versions). Without an
  emerald it does not work and waits next to its workstation ("Waiting for wages"),
  checking every few seconds.
- **Lumberjack:** needs an axe (carries up to 2), optionally saplings (up to 16 of each
  kind, `lumberjack.keepSaplings`). The axe wears out like a player's (one use per log,
  Unbreaking counts); shortly before it breaks it is given back (see *Worn tools* below);
  without one it waits ("needs an axe").
  Chopping speed depends on the axe. It only replants with saplings it carries (given,
  or picked up under the trees) and hands in the rest; it never fetches saplings from
  a chest. Without saplings it still fells, but does not replant.
- **Farmer:** needs a hoe (carries up to 2), which wears out one use per harvested crop;
  without one it waits ("needs a hoe"). It needs seeds to start with (up to 16 of each
  kind, `farmer.keepSeeds`); afterwards it replants from its harvest.
- **Pit digger:** needs a shovel and a pickaxe (up to 2 of each) and ladders (up to 64,
  `pitDigger.keepLadders`), optionally torches (up to 16, `pitDigger.keepTorches`). The
  tools wear out one use per block.
- **Miner:** needs a pickaxe (up to 2); optionally fences and planks for the supports (up
  to 16 each, `miner.keepFences`, `miner.keepPlanks`) and torches (up to 16,
  `miner.keepTorches`). It keeps up to 16 cobblestone of what it digs
  (`miner.keepSealBlocks`) for sealing water and lava and closing holes.
- **Fisher:** needs a fishing rod (carries up to 2), one use per catch; rods are used until
  they break. Optionally a boat (keeps 1) to row out.
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
  workstation) once it has less than a day's wage; a spare tool once it has none or only one with a tenth of its durability left (the
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
piece of work, give it back: into the closest village chest with a free slot (taking a new
tool on the same walk), else into an output chest of their workstation, else they keep it
and try again a minute later. So tools never break. Wooden and stone tools are used up
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
2. **Worker and chests** are set up in the overview (the tabs for the block, the worker,
   the wage, the chests and the order stand above the station's menu and above the
   overview),
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
bowls, composters, bookshelves, chiseled bookshelves, wooden tools, boats and boats with chest, shelves,
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
blocks (`fisher.seaRadius`): it puts the boat on the water at the bank, rows out, fishes
there, also treasure, rows back, gets out and takes the boat with it. Once in the boat it
keeps going until it is back on land, also after work. Boat trips can be switched off at
each barrel (tab *Settings*). The fisher trades like a vanilla fisherman. The menu shows the
catches of today.

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
the durability per material like an anvil, without experience costs (enchantments and
names stay), and puts them back where they came from.

## Pit digger

A hired villager at the **pit stake** (string, stick and planks) becomes a **pit digger**.
The player lays **string** on the ground as a closed ring (strings connect straight only,
each on solid ground, up to `pitDigger.maxUnevenness` 4 blocks up and down), clicks *Pick
pit* in the stake's overview and right-clicks a string of the ring. Everything inside the
ring (seen from above, any shape, 2 to `pitDigger.maxArea` 64 blocks, the whole ring within
the chest distance of the stake) is dug out down to the depth set with *-* / *+* (1 to
`pitDigger.maxDepth` 16), counted from the highest string; the height is stored when the
pit is picked. The blocks under the strings stay as the rim; while a string is missing the
pit digger waits.

It digs top down, layer by layer from the ladder outwards, with the shovel or the pickaxe,
whichever is faster (`pitDigger.digSlowdown`: 8 times a player's time, at least
`pitDigger.minSecondsPerBlock` 2 seconds per block). As soon as the
ladder column (inside the ring next to the string closest to the stake, or where *Ladder*
puts it) gets deeper it puts a ladder against the wall, and it only gets in and out of the
pit by that ladder: vanilla villagers do not path over ladders, so climbing is moved by
hand. It only digs the block tag `workingvillagers:pit_diggable` (natural ground, stone,
ores, leaves, plants); builds stay, and so does a column under one, blocks its pickaxe is
too weak for and blocks next to water or lava. With torches it lights the floor about every
six blocks. Everything it digs goes to its chest: when the drops of the next block would not
fit into its inventory any more, after `pitDigger.dailyBlocks` 256 blocks a day and when
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
the material, and closes new holes in the floor. Digging takes `miner.digSlowdown` 8 times a
player's time with the same pickaxe, at least `miner.minSecondsPerBlock` 1.5 seconds. It
delivers like the pit digger (when the next drops do not fit, after `miner.dailyBlocks` 256
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
and in the overview a product can be picked from everything the kiln makes: then it only
fires what makes that product, fires it again until it is the product (cobblestone, stone,
smooth stone; the stone in between stays with it), keeps a stock like the carpenter and does
not burn its material as fuel (logs for charcoal). Without a product it fires everything
once. What it took out of the kiln is handed in, even if it could burn.

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
fisher fishes at a pond and rows out on a lake with its boat and takes it back, the
butcher slaughters cows down to the herd size, breeds a piglet and smokes chicken, a
vanilla farmer is hired and stays at its composter and gets farmer trades, one that has
traded keeps its trades and experience, a librarian is not hired, hired villagers do not trample farmland, the smith smelts raw iron, the kiln operator
fires glass but leaves meat and raw iron alone and fires cobblestone twice for smooth stone,
the pit digger digs a pit with a ladder down to its bottom without touching the ring, the
shop assistant sells wheat to a farmer until its trade is sold out, ordinary villagers do
not take a set-up composter and *Assign* makes room at one, a boxed-in worker is brought
back, a lumberjack takes its wage, the more worn axe and saplings from a village chest but
leaves a renamed axe there, a lumberjack gives back a worn axe into the village chest (or
the output chest) instead of breaking it while a stone axe is used up, the smith repairs a
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
