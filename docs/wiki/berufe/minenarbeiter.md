# Beruf: Minenarbeiter

[← Berufe](README.md) · Station: **Grubenwerkbank** (neu) · Phase: **M3**

Der Minenarbeiter baut Stein und Erze in einem festgelegten Muster ab und bringt sie zur Zielkiste. Er gräbt nur, was ihm zugewiesen ist, und sichert seinen Stollen selbst.

## Workstation: Grubenwerkbank

- Der Grindstone ist bereits POI des Weaponsmith. Eine eigene Station verhindert Konflikte: **Grubenwerkbank** (Mining Bench), z. B. Spitzhacke + Eisen + Bruchstein.
- Funktion für Spieler: Erzbrocken (Raw Ore) gegen Stein-Varianten/Kies umwandeln oder Erze sortieren. Kann auch rein dekorativ sein.
- Alternative: Vanilla-Blast-Furnace oder Smithing Table wären thematisch nah, sind aber belegt (offene Frage, siehe [Risiken & Fragen](../planung/risiken-und-fragen.md)).

## Abbau-Modi (im GUI wählbar)

| Modus | Was passiert | Ertrag | Freigeschaltet ab |
| --- | --- | --- | --- |
| Steinbruch | Markierten Quader schichtweise von oben abtragen | Stein, Erde, Kies, zufällige Erze | Novice |
| Stollen | Gerader 2×1- oder 3×3-Tunnel in eine Richtung, Abzweige alle n Blöcke | Stein, Erze an den Wänden | Apprentice |
| Erzader folgen | Bei Erzfund die Ader komplett abbauen | Mehr Erz | Journeyman |
| Schacht | Treppenschacht bis auf eine Ziel-Y-Höhe | Tiefenerze, Deepslate | Expert |
| **Etagenmine** | Zentralschacht mit mehreren Etagen, Hauptgänge mit Stützen, Abbaustrecken im Branch-Mining-Muster (siehe [Die Mine](../grundlagen/mine.md)) | Viel Erz auf wenig Chunks | Journeyman |

Die Etagenmine ist das Ziel für große Dörfer: Sie bleibt in einer festen Grundfläche und braucht deshalb immer gleich viele geladene Chunks. Allein schafft der Minenarbeiter 3×3 Chunks; mit [Grubenlogistiker](grubenlogistiker.md) wächst die Mine auf 5×5 und mit Lorenrouten bis 7×7 (siehe [Größe der Mine](../grundlagen/mine.md#größe-der-mine-ausbau-mit-dem-logistiker)).

## Quellkiste / Zielkiste

| Quellkiste | Zielkiste |
| --- | --- |
| Spitzhacke, Schaufel, Fackeln oder Laternen, Bruchstein zum Abdichten, Bretter und Zäune für Stützen | Stein, Erde, Kies, Erze, Kohle |

Mit [Grubenlogistiker](grubenlogistiker.md) ist Quell- und Zielkiste immer das neueste Depot am Stollenende.

## Optik

Hauptgänge sehen aus wie Vanilla-Minenschächte: Stützen aus Zaunpfosten und Brettern alle 4 Blöcke, Fackeln an den Stützen, Schienen in der Mitte, Holzart nach Biom (Schwarzeiche in den Badlands). Details: [Die Mine](../grundlagen/mine.md#optik-wie-ein-vanilla-minenschacht).

## Sicherheit & Welt

- Setzt Fackeln aus der Quellkiste in festen Abständen (verhindert Mob-Spawns im Stollen).
- Lava und Wasser werden mit Bruchstein verschlossen, nie durchbrochen.
- Kies und Sand: wartet, bis sie gefallen sind, und baut sie dann ab.
- Mobs: flieht wie Vanilla-Villager. Optional Begleitschutz durch eine [Wache](wache.md).
- Verletzt sich der Arbeiter, sinkt die Moral; stirbt er, landet der Tod mit Ursache im [Sterberegister](../dorf/dorfbuch.md#register), Werkzeug und Lohnguthaben sind verloren.
- Minen weit vom Dorf sind **Außenposten** mit eigenem Bett (siehe [Chunk-Loading](../dorf/chunk-loading.md)).

## Lange Stollen

Je tiefer der Stollen, desto weiter der Weg zur Starttruhe. Ohne Hilfe endet ein Stollen bei der Reichweite der Station (z. B. 32–48 Blöcke), danach meldet der Minenarbeiter „Wartet: Weg zu weit“. Der [Grubenlogistiker](grubenlogistiker.md) löst das: Er baut neue Depot-Truhen am Stollenende, die Minenarbeiter nutzen dann immer das neueste Depot statt der alten Truhe, [Träger](traeger.md) bringen alles nach draußen, und später fährt eine Lore auf Schienen.

## Balancing

- Kein Silk Touch, kein Fortune in V1.
- Seltene Erze (Diamant, Ancient Debris) werden nur gefunden, nicht „erzeugt“. Der Ertrag hängt also von der echten Welt ab.
- Spitzhacken-Verschleiß ist der Haupt-Kostenfaktor neben dem Lohn.
- In Bergbiomen kann Smaragderz eine direkte [Einnahmequelle](../wirtschaft/einnahmen.md) sein.

## Master-Fähigkeit

Abbau mit Fortune I.

## Siehe auch

[Die Mine](../grundlagen/mine.md) · [Grubenlogistiker](grubenlogistiker.md) · [Schmied](schmied.md) (verarbeitet das Erz) · [Logistik](../grundlagen/logistik.md)
