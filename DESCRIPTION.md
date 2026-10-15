# Working Villagers

**Hire villagers, pay them a wage and let them do the work.**

Working Villagers turns unemployed villagers into paid employees. Hand one a work contract, give it a workstation and a few chests, and it gets to work: felling trees, sawing planks, crafting chests, farming fields, smelting ore and carrying goods from one place to another. There are no machines. Every job is done by a villager walking around, opening chests and working at its station.

---

## 👷 The workers

| Worker | Workstation | What it does |
|---|---|---|
| **Lumberjack** | Chopping block | Fells natural trees around its block, replants them and brings the logs to a chest |
| **Sawyer** | Woodcutter | Cuts logs and planks into planks, slabs, stairs, fences, doors, signs, ... |
| **Stone sawyer** | Stonecutter | Cuts stone into stairs, slabs, walls, bricks, ... |
| **Carpenter** | Carpenter's bench | Crafts chests, barrels, ladders, tools, boats and other wooden goods |
| **Carrier** | Wicker basket | Carries items between chests along up to three routes |
| **Farmer** | Composter | Harvests and replants the fields around its composter |
| **Smith** | Blast furnace | Smelts ore from its input chests in the blast furnace |

The workers fit together. A lumberjack supplies the sawyer, and a carrier takes the planks to the carpenter, so you get a whole wood workshop that runs by itself.

## 📜 Hiring and wages

1. Craft a **work contract** (paper, ink sac and 3 emeralds) and right-click an unemployed villager with it. It's hired and gets a name.
2. Place a **workstation**, open it and **assign** the villager.
3. **Link chests:** where the material comes from and where the products go.
4. Pick a **personal chest** and put emeralds in it. Every worker takes **one emerald a day** as its wage. No wage, no work.
5. During the day the villager works on its own.

Hired villagers never take vanilla jobs and never quit by themselves. Outside work hours they still live like normal villagers: they sleep in beds, chat at the village bell and run from zombies and raids.

## 🪓 Lumberjack

- Looks for **natural trees** near its chopping block, chops them down, replants a sapling and takes the logs to a chest.
- Only touches trees with natural leaves, so **your log houses are safe**.
- Uses a real axe from its personal chest. The axe wears out like yours, and better axes (and Efficiency) make it faster.
- Can **strip the logs** while felling if you switch it on.

## 🪚 Sawyer and stone sawyer

- The new **woodcutter** is a stonecutter for wood. One log gives 8 slabs or 4 stairs directly, without the crafting table's waste. You can use it yourself too.
- Give the sawyer an order (a product shape and the wood types it may use) and it cuts everything from its input chests.
- A hired villager at a vanilla **stonecutter** becomes a stone sawyer. Pick a product from any stone family and it does the rest. Ordinary villagers still become masons.

## 🔨 Carpenter

- The **carpenter's bench** is a crafting table for wooden things only: chests, barrels, ladders, bookshelves, wooden tools, boats, shields, bows, torches and more. Its menu lists everything and shows what you're missing.
- The carpenter crafts your order from its input chests and keeps a **stock** you choose, e.g. "always keep 16 chests ready".

## 🌾 Farmer and smith

- A hired villager at a vanilla **composter** becomes a farmer: it harvests the ripe crops around it, replants them and brings the harvest to a chest. It needs a hoe in its personal chest, never tills, never tramples the fields and touches nothing but crops.
- A hired villager at a vanilla **blast furnace** becomes a smith: it loads the furnace with ore and fuel from its input chests and brings the ingots to its output chests.
- Right-click the composter with an empty hand, or click the **i** button in the blast furnace screen, to set them up. Ordinary villagers still become vanilla farmers and armorers there.

## 🧺 Carrier

- Craft a **wicker basket** and hand it to a villager: it becomes a carrier, no contract needed.
- Set up to **three routes** from a source chest to a target chest. The carrier fills its basket, carries the items over and brings back what didn't fit.
- You can plan the routes on the basket before you hand it over.

## 🔍 Always know what's going on

- Every workstation has a **paper note** on its front with its worker's name.
- The menus show what each worker is doing right now: *Chopping a tree (40 %)*, *Cutting Oak Stairs*, *Waiting for wages*, *Done for today*, *Sleeping*, ...
- If a worker isn't working, the status tells you why.

## ⚙️ Data packs and configuration

- Woodcutter recipes, wood types and the carpenter's products are **data driven**, so data packs and other mods can add their own wood.
- `config/workingvillagers.json` sets the wage, the distances, the speeds and the daily limits.

---

## Requirements

- Minecraft **26.3**
- **Fabric** (with [Fabric API](https://modrinth.com/mod/fabric-api)) or **NeoForge**

Needed on both client and server. Works in singleplayer and multiplayer.

## ⚠️ Beta

This is an early beta. **Back up your worlds** before trying it, and expect that saves may not carry over to later versions. Known limitations:

- All workers are novices for now. Levels are planned.
- Carriers have no filters yet and carry everything.
- Workers only work while their chunks are loaded.
- No JEI/EMI/REI or Jade integration yet.

Please report bugs and ideas on [GitHub](https://github.com/d4rkm0nkey/minecraft-working-villagers/issues).

## License

Released under the **MIT License**, so feel free to include it in modpacks.
