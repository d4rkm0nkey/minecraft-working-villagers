---
icon: wv/head-hauler
workers: [hauler]
---

# Hauler

Hired with a work contract, the hauler works at a **jetty**. It rows a **chest boat** to a
second jetty further along a river, a lake or the coast: there it unloads what it brought
and loads what waits there, then rows back. Use it for a camp far from your base: logs from a
lumberjack camp to the sawyer at home, emeralds, axes and saplings back to the camp.

[[craft: _ string _ / _ any_log _ / _ any_log _ -> jetty]]

## Setting it up

1. Place a **jetty** at the water where the trip starts, and a second one at the water
   where it ends.
2. Link chests to both jetties (tab :mc-chest: **Chests**). **Input chests**: what the
   hauler takes to the other jetty. **Output chests**: where it unloads what it brings.
   Each jetty can have both, so the boat carries something both ways.
3. Record the route with the **freight book** (see below).
4. Right-click the hauler with its wage (emeralds) and a **chest boat**. It also takes
   one from a [village chest](basics.md#village-chests).

**Without a route or a chest boat the hauler does not work.**

## Recording the route

Villagers cannot find their way along a river by themselves, so you row the route once:

1. Right-click the first jetty with the **freight book**. The book glints while it records.
2. Get into a boat right at that jetty (at most 16 blocks away) and row to the second
   jetty. The bar above your hotbar shows the length so far.
3. Get out next to the second jetty and right-click it with the book.

The book notes a **waypoint** about every 16 blocks and before every bend; the hauler later
rows straight from one waypoint to the next. Both jetties get the route, the second one the
other way round, so a hauler at either jetty can row it. A route is at most **512 blocks**
long. Right-clicking the first jetty again while recording cancels it. Row the route again to
change it.

[[craft shapeless: book compass -> freight_book]]

## A trip

1. At its jetty the hauler puts the chest boat on the water where you got into your boat,
   and loads it from the input chests (27 stacks fit).
2. It gets in and rows along the route, about as fast as a player.
3. At the other jetty it gets out, unloads into the output chests there and loads what lies
   in the input chests there.
4. It rows back, unloads at home and goes again as long as there is something to carry.

It only sets out if it is **back before dusk**, so on a long route it makes one trip a day.
Once it set out it does not stop at the end of the work hours: it comes home first. When
there is nothing more to carry it takes the boat out of the water again. If the output
chests at one end are full, it waits there until there is room; it never takes cargo back.

It also sets out with an empty boat if something waits at the other jetty.

## Good to know

- The overview of the jetty (tab :mc-freight_book: **Settings**) shows the route: where the
  other jetty is, its length and the waypoints.
- With [work without a player nearby](basics.md#working-without-a-player-nearby) the hauler
  carries its loaded chunks along while it rows; the other jetty and its chests stay
  loaded as well. Without it, the hauler only rows where a player keeps the chunks loaded,
  and waits on the water where the route is not loaded.
- If the hauler gets stuck on the way (you built a bridge, filled in the river), it says so
  in its status: record the route again.
- Every stack it loads counts as work for its [experience](basics.md#experience).
