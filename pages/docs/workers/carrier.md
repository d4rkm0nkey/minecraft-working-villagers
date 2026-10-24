---
icon: wv/head-carrier
workers: [carrier]
---

# Carrier

Hired with a
**wicker basket**, needs no workstation.

A carrier moves items between chests along up to **three routes**. Use carriers to
connect your workers: logs from the lumberjack to the sawyer, planks from the sawyer to
the carpenter, products to your storage.

[[craft: sugar_cane stick sugar_cane / sugar_cane _ sugar_cane / sugar_cane sugar_cane sugar_cane -> wicker_basket]]

## Hiring

Right-click an unemployed adult villager with the basket. It takes the basket and
becomes a carrier. No work contract needed: the basket is contract and
workstation at once.

Like every worker the carrier needs [its wage](basics.md#wage-and-tools): right-click
it with emeralds. It needs no tool. A carrier that carries emeralds, e.g. the earnings of a
[shop assistant](shop-assistant.md) to a village chest, takes its day's wage out of its load
when it has none left and no village chest in reach has any.

## The carrier menu

Right-click the carrier with an **empty hand**. The menu has two tabs at the top:

- :mc-map: **Routes:** its routes (up to 3) with name, source and target, by the names of
  the containers (see [naming chests](basics.md#naming-chests)). Hover for the length. The
  route it is currently working on is green. A red dot on the tab: no route yet.
- :mc-wicker_basket: **Basket:** what the carrier is doing, the basket contents (which you
  can take out), its emeralds and your inventory. A carrier has no workstation and so no
  wage box: give it emeralds directly. A red dot on the tab: it has no wage.

### Adding a route

Click **+ New route** in the first free slot. The menu closes. Right-click the
**source** container, then the **target**. Right-clicking the source again cancels; an
unfinished pick runs out after two minutes.

- Source and target may be at most **32 blocks** apart.
- The sources of one carrier may be at most **40 blocks** from each other.

**✎** renames a route (up to 24 characters, Enter saves, Esc cancels), **×** deletes it.

### Planning routes before hiring

Right-click the air while holding a basket: the same menu opens for the routes stored
on the basket. You can set everything up first and then hand the basket over.

### Who may change the routes?

Only the employer, unless they switch the button at the bottom of the **Routes** tab to
**Edit routes: Everyone**.
Operators (and the owner of a singleplayer world) always can.

## How it works

During work hours the carrier goes through its routes in turn:

1. it fills the basket at a source that has items: up to **6 stacks**, but only as many
   as the target has room for,
2. carries them to the target,
3. brings back what did not fit.

When all sources are empty, it strolls around near its first source and looks again
every few seconds.

!!! info "No filters yet"
    A carrier moves **everything** in the source. Use a separate output chest for the
    things it should carry.

## Taking the basket back

**Sneak + right-click** the carrier with an empty hand. You get the basket back with its
routes and contents, the carrier's emeralds and anything else it carried, and the
villager is unemployed again. A carrier that dies drops its
basket.
