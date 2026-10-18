---
icon: wv/head-shop_assistant
workers: [shop_assistant]
---

# Shop assistant

Hired with a work contract, the shop assistant works at the **trading counter**. It sells
the goods from its input chests to the villagers around, through their own trades, and
brings the emeralds to its output chests. It is the worker that earns the wages of the
others.

## Setting it up

1. Place a [trading counter](../blocks-and-items.md#trading-counter) near villagers with
   jobs: farmers, fletchers, librarians, masons, ...
2. Right-click it to open its overview.
3. Link chests with **+ Input** (the goods to sell) and **+ Output** (the emeralds). A
   hired villager without a job finds the counter by itself; **Assign** picks a specific
   one.
4. Right-click the shop assistant with emeralds for its wage.

**Without an input chest and an output chest the shop assistant does not work.**

## How it works

During work hours the shop assistant:

1. looks for a villager within **24 blocks** of the counter that buys something from the
   input chests for emeralds, the nearest first,
2. fetches as much of the goods as that villager's trades take,
3. walks over and trades, one trade every **2 seconds**,
4. brings the emeralds to an output chest, and what it could not sell back into an input
   chest.

The overview shows the emeralds it earned today.

## The trades

It uses the villagers' **own trades**, the same ones you see when you trade with them:

- Only trades where a villager **buys** something **for emeralds**: a farmer buying wheat,
  a fletcher buying sticks, a librarian buying paper, a mason buying clay. It buys nothing.
- The price is the villager's current price, demand included. Trades that have been used a
  lot get more expensive, like for you.
- A trade is **sold out** after its uses. The villager restocks at its workstation during
  the day, then the shop assistant sells again. So more villagers with more trades earn
  more.
- The villagers earn **experience** from these trades and rise in level, like when you
  trade with them. Their experience orbs pop out next to them.
- It only trades with ordinary villagers with a job, not with hired workers, nitwits,
  children or a villager you are trading with.

## Good to know

- Put only what you want to sell into the input chests: it sells everything a villager
  around buys.
- What nobody buys stays in the input chests.
- It keeps its wage apart: only the emeralds it earned go to the output chest.
