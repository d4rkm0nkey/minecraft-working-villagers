#!/usr/bin/env python3
"""Prints a report of a village simulation (./gradlew :fabric:runSimulation).

    python3 tools/simulation_report.py [fabric/run/simulation/world/simulation]

Reads summary.json (and the CSV files next to it) and answers: how long the ticks take,
which workers could not work and why, whether the shops earn the wages, and how full the
storage gets.
"""
import json
import sys
from collections import defaultdict
from pathlib import Path

DEFAULT = Path(__file__).resolve().parent.parent / "fabric/run/simulation/world/simulation"

# Statuses that mean the worker does what it should (or rests as planned)
WORKING_HINTS = ("chopping", "cutting", "crafting", "carrying", "smelting", "firing", "digging", "harvesting", "planting", "clearing",
                 "selling", "trading", "fishing", "shearing", "brewing", "tanning", "enchanting", "forging", "fletching",
                 "repairing", "walking", "going", "fetching", "delivering", "bringing", "looking", "guarding", "feeding",
                 "slaughtering", "smoking", "filling", "taking", "putting", "rowing", "mining", "sealing", "climbing")


def pct(part, whole):
    return f"{100.0 * part / whole:.0f} %" if whole else "-"


def main():
    folder = Path(sys.argv[1]) if len(sys.argv) > 1 else DEFAULT
    summary = json.loads((folder / "summary.json").read_text())
    days = summary["days"]
    hours = summary["hours"]

    print(f"# Village simulation: {summary['wings']} wings, {summary['workers']} workers, {summary['traders']} customers\n")
    print("Jobs: " + ", ".join(f"{job} {n}" for job, n in summary["jobs"].items()) + "\n")

    print("## Performance (per in-game hour)\n")
    print("| day | hour | mean ms | p95 ms | p99 ms | max ms | ticks > 50 ms | entities | villagers | monsters | items |")
    print("|---|---|---|---|---|---|---|---|---|---|---|")
    for h in hours[::3]:
        print(f"| {int(h['day'])} | {(int(h['timeOfDay']) // 1000 + 6) % 24:02d}:00 | {h['mean']:.2f} | {h['p95']:.2f} | {h['p99']:.2f} | {h['max']:.1f} "
              f"| {int(h['over50'])} | {int(h['entities'])} | {int(h['villagers'])} | {int(h['monsters'])} | {int(h['items'])} |")
    if hours:
        means = [h["mean"] for h in hours]
        work = [h for h in hours if 2000 <= h["timeOfDay"] < 9000]
        print(f"\nOverall mean {sum(means) / len(means):.2f} ms per tick (budget 50 ms), "
              f"work hours {sum(h['mean'] for h in work) / max(1, len(work)):.2f} ms, "
              f"worst hour p95 {max(h['p95'] for h in hours):.2f} ms, longest tick {max(h['max'] for h in hours):.0f} ms, "
              f"{int(sum(h['over50'] for h in hours))} of {int(sum(h['ticks'] for h in hours))} ticks over 50 ms.\n")

    print("## Economy (per day)\n")
    print("| day | workers | paid | unpaid | shop earned | emeralds total | in wage boxes | in village chests | tills | transit | deaths | storage slots |")
    print("|---|---|---|---|---|---|---|---|---|---|---|---|")
    for d in days:
        print(f"| {d['day']} | {d['alive']} | {d['paid']} | {d['unpaid']} | {d['shopEarned']} | {d['total']} | {d['wageBoxes']} | {d['villageChests']} "
              f"| {d['tills']} | {d['transit']} | {d['deaths']} | {d['storageSlotsUsed']}/{d['storageSlots']} |")
    if len(days) >= 2:
        change = (days[-1]["total"] - days[0]["total"]) / (len(days) - 1)
        earned = sum(d["shopEarned"] for d in days[1:]) / (len(days) - 1)
        print(f"\nEmeralds change {change:+.1f} per day; the shops earn {earned:.1f} per day against wages of about {days[-1]['alive']} per day.\n")

    print("## Customers of the shops\n")
    for d in days:
        if "tradersAlive" in d:
            levels = defaultdict(int)
            for key, n in d["traderLevels"].items():
                levels[key.rsplit(" ", 1)[1]] += n
            print(f"- day {d['day']}: {d['tradersAlive']} alive, {d['tradersWithJob']} with a job, levels "
                  + ", ".join(f"{lvl} {n}" for lvl, n in sorted(levels.items())) + f"; villagers died so far {d.get('villagersDied', '-')}")
    print()

    print("## What the workers did during work hours (share of samples)\n")
    totals = defaultdict(lambda: defaultdict(int))
    for d in days:
        for job, statuses in d["statuses"].items():
            for status, n in statuses.items():
                totals[job][status] += n
    for job in sorted(totals):
        statuses = totals[job]
        whole = sum(statuses.values())
        top = sorted(statuses.items(), key=lambda kv: -kv[1])[:6]
        print(f"- **{job}**: " + "; ".join(f"{status} {pct(n, whole)}" for status, n in top))
    print()

    print("## Production (work done per day, summed per job)\n")
    jobs = sorted({job for d in days for job in d["production"]})
    print("| job | " + " | ".join(f"day {d['day']}" for d in days) + " |")
    print("|---|" + "---|" * len(days))
    for job in jobs:
        print(f"| {job} | " + " | ".join(str(d["production"].get(job, 0)) for d in days) + " |")
    print()

    print("## Into the storage per day (top items)\n")
    for d in days:
        added = sorted(d["storageAdded"].items(), key=lambda kv: -kv[1])[:12]
        print(f"- day {d['day']}: " + ", ".join(f"{item} {n:+d}" for item, n in added))
    print()

    if summary["events"]:
        print("## Events\n")
        for event in summary["events"][:60]:
            print(f"- {event}")
        print()
    if summary["warnings"]:
        print("## Layout warnings\n")
        for warning in summary["warnings"][:60]:
            print(f"- {warning}")


if __name__ == "__main__":
    main()
