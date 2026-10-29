---
title: Working Villagers
hide:
  - navigation
  - toc
---

<div class="wv-hero">
<span class="wv-hero__sun" aria-hidden="true"></span>
<span class="wv-hero__cloud" style="top: 3.4rem; left: 7%; width: 4.8rem" aria-hidden="true"></span>
<span class="wv-hero__cloud" style="top: 6.6rem; left: 70%; width: 3.6rem" aria-hidden="true"></span>
<img class="wv-hero__logo" src="assets/img/icon.png" alt="">
<h1 class="wv-hero__title">Working Villagers</h1>
<p class="wv-hero__tagline">Hire villagers, pay them a wage and let them do the work.</p>
<div class="wv-hero__actions">
<a class="md-button md-button--primary" href="installation/">Download</a>
<a class="md-button" href="getting-started/">Your first worker</a>
</div>
<div class="wv-hero__village" aria-hidden="true">
<span class="wv-villager" style="--delay: 0s"><img src="assets/mc/heads/lumberjack.png" alt=""><img src="assets/mc/blocks/chopping_block.png" alt=""></span>
<span class="wv-villager" style="--delay: .4s"><img src="assets/mc/heads/woodcutter.png" alt=""><img src="assets/mc/blocks/woodcutter.png" alt=""></span>
<span class="wv-villager" style="--delay: .9s"><img src="assets/mc/heads/carpenter.png" alt=""><img src="assets/mc/blocks/carpenter_bench.png" alt=""></span>
<span class="wv-villager" style="--delay: .2s"><img src="assets/mc/heads/farmer.png" alt=""><img src="assets/mc/blocks/composter.png" alt=""></span>
<span class="wv-villager" style="--delay: .7s"><img src="assets/mc/heads/smith.png" alt=""><img src="assets/mc/blocks/blast_furnace.png" alt=""></span>
<span class="wv-villager" style="--delay: 1.1s"><img src="assets/mc/heads/stonecutter.png" alt=""><img src="assets/mc/blocks/stonecutter.png" alt=""></span>
<span class="wv-villager" style="--delay: .5s"><img src="assets/mc/heads/carrier.png" alt=""><img src="assets/mc/blocks/chest.png" alt=""></span>
<span class="wv-villager" style="--delay: .3s"><img src="assets/mc/heads/pit_digger.png" alt=""><img src="assets/mc/blocks/pit_stake.png" alt=""></span>
<span class="wv-villager" style="--delay: .8s"><img src="assets/mc/heads/kiln_operator.png" alt=""><img src="assets/mc/blocks/kiln.png" alt=""></span>
<span class="wv-villager" style="--delay: .6s"><img src="assets/mc/heads/shop_assistant.png" alt=""><img src="assets/mc/blocks/trading_counter.png" alt=""></span>
<span class="wv-villager" style="--delay: 1s"><img src="assets/mc/heads/miner.png" alt=""><img src="assets/mc/blocks/mining_bench.png" alt=""></span>
<span class="wv-villager" style="--delay: .45s"><img src="assets/mc/heads/shepherd.png" alt=""><img src="assets/mc/blocks/loom.png" alt=""></span>
<span class="wv-villager" style="--delay: .85s"><img src="assets/mc/heads/butcher.png" alt=""><img src="assets/mc/blocks/smoker.png" alt=""></span>
<span class="wv-villager" style="--delay: .15s"><img src="assets/mc/heads/fisher.png" alt=""><img src="assets/mc/blocks/barrel.png" alt=""></span>
<span class="wv-villager" style="--delay: .7s"><img src="assets/mc/heads/hauler.png" alt=""><img src="assets/mc/blocks/jetty.png" alt=""></span>
<span class="wv-villager" style="--delay: .65s"><img src="assets/mc/heads/fletcher.png" alt=""><img src="assets/mc/blocks/fletching_table.png" alt=""></span>
<span class="wv-villager" style="--delay: .35s"><img src="assets/mc/heads/guard.png" alt=""><img src="assets/mc/blocks/target.png" alt=""></span>
<span class="wv-villager" style="--delay: .95s"><img src="assets/mc/heads/apothecary.png" alt=""><img src="assets/mc/blocks/brewing_stand.png" alt=""></span>
<span class="wv-villager" style="--delay: .55s"><img src="assets/mc/heads/tanner.png" alt=""><img src="assets/mc/blocks/cauldron.png" alt=""></span>
<span class="wv-villager" style="--delay: .25s"><img src="assets/mc/heads/weaponsmith.png" alt=""><img src="assets/mc/blocks/grindstone.png" alt=""></span>
<span class="wv-villager" style="--delay: .75s"><img src="assets/mc/heads/librarian.png" alt=""><img src="assets/mc/blocks/lectern.png" alt=""></span>
</div>
</div>

Working Villagers turns villagers into paid employees. You hand them a work
contract, give them a workstation and a few chests, and they start working: felling
trees, sawing planks, crafting chests, farming fields, smelting ore, digging pits and tunnels, firing
glass and bricks, selling goods to the villagers and carrying items from one place to
another.
There are no machines: everything is done by villagers walking around, opening chests
and working at their stations.

!!! warning "Beta"
    This is an early beta for testing. Back up your worlds before trying it, and expect
    that saves may not carry over to later versions. See [Beta notes](beta.md).

## The workers

[[workers]]

Every worker needs emeralds :mc-emerald: as its wage. Right-click it with them; it takes
one emerald each day, but only on a day it gets something done. No wage, no work. Hired late in the afternoon (in the last third of
the work hours), a worker works the rest of that first day for free.

## How it works in short

<ol class="wv-steps">
<li><span class="mc-slot">:mc-work_contract:</span><strong>Hire</strong> Craft a work contract and right-click a villager with it.</li>
<li><span class="mc-slot">:mc-chopping_block:</span><strong>Workstation</strong> Place a workstation. The villager walks there and takes the job.</li>
<li><span class="mc-slot">:mc-chest:</span><strong>Chests</strong> Link chests: where the material comes from and where the products go.</li>
<li><span class="mc-slot">:mc-emerald:</span><strong>Wage</strong> Right-click the worker with emeralds (and its tool, if it needs one).</li>
<li><span class="mc-slot">:mc-clock:</span><strong>Work</strong> During the day the villager works on its own. Its menu shows what it is doing.</li>
</ol>

New here? Start with [Installation](installation.md) and then
[Your first worker](getting-started.md), which walks you through your first lumberjack.

## Features at a glance

- Seven professions that work together: a lumberjack feeds a sawyer, a carrier brings the
  planks to a carpenter.
- Workers really walk to chests, open them and work at their stations, with sounds and
  particles.
- Hired villagers find a free workstation by themselves, never take vanilla jobs and
  never leave their job by themselves.
- The menus show what each worker is doing right now: chopping 40 %, waiting for wages,
  done for today, sleeping, ...
- Lumberjacks only touch natural trees, so your log houses are safe.
- New: the **woodcutter** :mc-woodcutter:, a stonecutter for wood, and the
  **carpenter's bench** :mc-carpenter_bench:, a crafting table for wooden things. Both are
  useful for players too.
- Woodcutter recipes and carpenter products are data driven, so data packs and other
  mods can add their wood. The sawyer's orders only know the vanilla wood types.
- Wage, speeds, distances and limits can be set in a [config file](configuration.md).
- Works on Fabric and NeoForge, in singleplayer and on servers.
