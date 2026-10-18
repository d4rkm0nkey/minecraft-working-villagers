# Risiken & offene Fragen

[← Wiki-Start](../README.md)

Das größte Risiko ist nicht die Idee, sondern die Villager-KI: Pathfinding, Stuck-Situationen und Weltschäden entscheiden, ob der Mod sich gut anfühlt.

## Risiken

| Risiko | Auswirkung | Gegenmaßnahme |
| --- | --- | --- |
| Villager bleiben hängen (Löcher, Wasser, Bäume) | Arbeit stoppt unbemerkt | Umgesetzt: Stuck-Erkennung, nach 60 s zurück zur Station (Config `workers.stuckSeconds`), Status „Steckt fest“ in der Stationsübersicht. Später auch im [Dorfbuch](../dorf/dorfbuch.md) |
| Unbeabsichtigte Zerstörung | Spielerbauten beschädigt | Strikte Bereichsgrenzen, nur natürliche Blöcke, Block-Blacklist per Tag |
| Performance bei vielen Arbeitern | Server-Lag | Inkrementelle Scans, Tick-Budget pro Arbeiter, Config-Limit |
| Zu stark (Ressourcen ohne Aufwand) | Balancing kaputt | Lohn, Verschleiß, Tempo unter Spielertempo |
| Zu schwach (lohnt nicht) | Niemand nutzt es | Playtests mit Ertrag/Woche vs. Lohn |
| Konflikt mit anderen Villager-Mods | Crashes, doppelte Berufe | Eigene Berufe; Daten als zusätzliche Felder per Mixin am Villager, Vanilla-Logik bleibt sonst unangetastet (siehe [Architektur](../technik/architektur.md)) |
| Entladene Chunks | Arbeit steht, wenn Spieler weg ist | [Dorf-Chunk-Loading](../dorf/chunk-loading.md) mit Unterhalt und Limits; Aufholberechnung als Fallback |
| Loren bleiben hängen (Mobs, fehlende Schiene, entladener Chunk) | Erz staut sich im Depot | Status „Strecke unterbrochen“, Fackeln entlang der Strecke, Chunk-Loading der Strecke, Abstraktion als Fallback |
| Mikromanagement durch Bedürfnisse | Spieler fühlen sich gegängelt | [Bedürfnisse](../wirtschaft/beduerfnisse.md) nur als Rabatt, abschaltbar |

## Offene Fragen

- [ ] Wie hoch ist der Lohn wirklich? 100 Emeralds/Woche vs. 16–64 gestaffelt. (Derzeit 1 Emerald pro Arbeitstag, per Config.)
- [x] Werden Villager mit Trades anheuerbar (und verlieren sie ihre Trades)? Entschieden: Dorfbewohner mit Beruf lassen sich anheuern, bleiben an ihrem Block und behalten Angebote und Stufe; Arbeiter mit Vanilla-Gegenstück handeln wie dieses.
- [ ] Eigene Grubenwerkbank oder Vanilla-Block als Minen-Station?
- [ ] Wie viele Arbeiter pro Spieler (Limit)?
- [x] Was passiert beim Tod eines Arbeiters? Er lässt alles fallen, was er trägt (Emeralds, Werkzeug, Material); ein Träger zusätzlich seinen Korb samt Inhalt.
- [x] Fabric oder NeoForge? Beide parallel über das Multiloader-Setup, Spiellogik in `common`.
- [ ] Kaufformel für Dörfer: reichen Betten, Workstations und Villager, oder zählt auch das Biom?
- [ ] Handelsangestellter und Bibliothekar: beide am Lectern, oder eigener Handelstresen?
- [ ] Wache: Golem, kämpfender Villager oder nur Abschreckung?
- [ ] Grubenlogistiker: eigener Block Förderhaspel oder zweiter Arbeiter an der Grubenwerkbank?
- [ ] Etagenmine: Etagenabstand, Unterhalt großer Minen, Förderkorb als Entity oder abstrakt (siehe [Die Mine](../grundlagen/mine.md#offene-fragen)).
