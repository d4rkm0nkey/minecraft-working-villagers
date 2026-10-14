# Die Mine: Aufbau, Etagen & Förderung

[← Wiki-Start](../README.md)

Eine Mine wächst nach unten statt in die Länge: ein zentraler Schacht, mehrere Etagen mit Gängen im Stil der Vanilla-Minenschächte und ein Aufzug, der das Erz nach oben bringt. So bleibt die Mine in wenigen Chunks und ist billig dauerhaft zu laden.

Beteiligte Berufe: [Minenarbeiter](../berufe/minenarbeiter.md) (gräbt und stützt), [Grubenlogistiker](../berufe/grubenlogistiker.md) (Schienen, Depots, Aufzug), [Träger](../berufe/traeger.md) (Lücken überbrücken).

## Warum Etagen?

| | Langer Stollen | Etagenmine |
| --- | --- | --- |
| Grundfläche | 1 Gang, wächst in die Länge | fester Bereich: 3×3 bis 7×7 Chunks je nach Ausbau |
| Geladene Chunks | wächst mit der Länge (256 Blöcke ≈ 16 Chunks) | fest pro Ausbaustufe (9, 25 … 49), egal wie viele Etagen |
| Unterhalt pro Woche | steigt laufend | steigt nur mit bewusstem Ausbau |
| Freigelegtes Gestein pro Chunk | gering | hoch, jede Etage nutzt die ganze Fläche |
| Tiefenerze (Diamant, Redstone) | nur mit Schacht-Modus | jede neue Etage geht tiefer |

Chunks reichen in Minecraft über die ganze Höhe der Welt. Zusätzliche Etagen kosten deshalb keine zusätzlichen Chunk-Tickets. Der lange Stollen bleibt als einfacher Einstieg erhalten.

## Aufbau

**Seitenansicht**

```
  Oberfläche   [Starttruhe]◄─┐  Förderhaspel
 ──────────────────────────── │ ─────────────────────────────────
                              │
  Etage 1  ═╤═══╤═══╤═══╤═════╡ Füllort 1 [Depot]═══╤═══╤═══╤═
            ┴   ┴   ┴   ┴     │  (Schacht)          ┴   ┴   ┴
                              │
  Etage 2  ═╤═══╤═══╤═══╤═════╡ Füllort 2 [Depot]═══╤═══╤═══╤═
            ┴   ┴   ┴   ┴     │                     ┴   ┴   ┴
                              │
  Etage 3  ═╤═══╤═══╤═════════╡ Füllort 3 [Depot]   (im Bau)
            ┴   ┴   ┴         ▼

  ═ Hauptgang mit Schiene   ╤/┴ Holzstütze   │ Aufzug im Schacht
```

**Draufsicht einer Etage**

```
        │   │   │       │   │   │
     ───┼───┼───┼───────┼───┼───┼───   ← Abbaustrecken (1×2), alle 3 Blöcke
        │   │   │       │   │   │
 ═══════════════╦═[F]═╦═══════════════  ← Hauptgang (3×3) mit Stützen und Schiene
        │   │   │  ║   │   │   │
     ───┼───┼───┼──║───┼───┼───┼───
        │   │   │  ║   │   │   │
                   ║
              Hauptgang Nord/Süd     [F] = Füllort mit Depot und Aufzug
```

- **Schacht:** 3×3 im Zentrum der Mine, Leitern an einer Wand, daneben der Aufzug.
- **Füllort:** Auf jeder Etage ein Platz am Schacht mit Depot-Truhe und Ladestelle des Aufzugs.
- **Hauptgänge:** 3 breit und 3 hoch, in alle vier Richtungen bis zum Rand der Grundfläche. Hier liegen die Lorenrouten.
- **Abbaustrecken:** 1 breit und 2 hoch, im Abstand von 3 Blöcken (klassisches Branch Mining: legt fast alle Erze frei, ohne alles abzubauen).
- **Etagenabstand:** Gang 3 hoch + 3 Blöcke Gestein dazwischen, also alle 6 Blöcke eine Etage (Startwert).

## Optik: wie ein Vanilla-Minenschacht

Die Minenarbeiter bauen ihre Gänge so, dass sie wie die generierten Minenschächte aussehen:

- **Stützen** im Hauptgang alle 4 Blöcke: zwei Zaunpfosten links und rechts, ein Brett-Balken darüber (wie im Vanilla-Minenschacht).
- **Holzart** nach Biom: Eiche als Standard, Schwarzeiche in den Badlands wie bei den Vanilla-Minenschächten der Tafelberge. Andere Holzarten aus der Quellkiste sind erlaubt.
- **Licht:** Fackeln an den Stützen oder Laternen an Ketten von der Decke.
- **Schienen** mittig im Hauptgang, Weichen an Kreuzungen.
- **Abbaustrecken** bleiben schlicht ohne Stützen, damit der Holzbedarf überschaubar bleibt.

Das Holz für Stützen kommt aus der Quellkiste. Hier zahlt sich der [Holzfäller](../berufe/holzfaeller.md) aus: Bretter und Zäune aus dem Woodcutter gehen direkt in die Mine. Wer die Optik nicht will, kann Stützen in der Config abschalten.

## Größe der Mine: Ausbau mit dem Logistiker

Die Grundfläche wird in Chunks um den Schacht-Chunk gemessen (Chunk mit Förderhaspel und Schacht in der Mitte). Sie wächst mit dem Ausbau durch den [Grubenlogistiker](../berufe/grubenlogistiker.md):

| Ausbau | Grundfläche | Geladene Chunks | Rand ab Schacht |
| --- | --- | --- | --- |
| Nur Minenarbeiter | 3×3 | 9 | ca. 24 Blöcke |
| Mit Grubenlogistiker (Depots + Träger) | 5×5 | 25 | ca. 40 Blöcke |
| Pro Lorenroute | +1 Chunk-Reihe in dieser Richtung | +5 bis +7 | ca. 56 Blöcke in dieser Richtung |
| Alle 4 Lorenrouten (N, O, S, W) | 7×7 | 49 | ca. 56 Blöcke |

```
 . . . . . . .     S  Schacht-Chunk
 . o o o o o .     #  3×3: nur Minenarbeiter
 . o # # # o .     o  5×5: mit Grubenlogistiker
 . o # S # o .     .  +1 Reihe je Lorenroute
 . o # # # o .        (z. B. nur Route Ost → 6×5)
 . o o o o o .
 . . . . . . .
```

- **Nur Minenarbeiter:** Er läuft jede Ladung selbst zum Füllort. Weiter als 3×3 lohnt sich das nicht.
- **Mit Grubenlogistiker:** Er setzt Depots an die Hauptgänge, [Träger](../berufe/traeger.md) bringen den Ertrag zum Füllort. Die Minenarbeiter legen ab in das nächste Depot an ihrem Gang (wie im [Stollen](../berufe/grubenlogistiker.md#stufe-1-depots)).
- **Lorenroute:** Der Logistiker verlegt im Hauptgang einer Richtung eine Schiene vom Füllort bis zum Rand und setzt dort ein Depot mit Ladestelle. Jede Route schiebt den Rand in ihrer Richtung um einen Chunk (16 Blöcke) hinaus. Routen lassen sich einzeln bauen; eine Mine mit nur einer Route nach Osten ist 6×5 groß.
- Die Grundfläche gilt für die ganze Mine, also für alle Etagen. Gegraben wird in der erweiterten Reihe aber nur auf Etagen, auf denen die Lorenroute dieser Richtung liegt.
- Jede Stufe kostet mehr [Chunk-Unterhalt](../dorf/chunk-loading.md) (9 → 25 → bis zu 49 Chunks). Größer bauen ist damit eine bewusste Entscheidung.

## Aufzug zwischen den Etagen

Der Aufzug bringt alles von den Füllorten zur Starttruhe an der Oberfläche und auf dem Rückweg Nachschub (Fackeln, Werkzeug, Holz für Stützen) nach unten.

| Variante | Aufbau | Material | Durchsatz | Wann |
| --- | --- | --- | --- | --- |
| Leiter + Träger | [Träger](../berufe/traeger.md) klettert mit einem Stack | Leitern | niedrig | sofort |
| **Förderkorb** (empfohlen) | Förderhaspel oben, Ketten im Schacht, ein Korb fährt sichtbar zwischen den Füllorten | Ketten, Eisen, Bretter | mittel | M4 |
| Blasensäule | Wassersäule auf Seelensand, Items steigen nach oben, Hopper oben in die Starttruhe | Seelensand (Nether), Wassereimer | hoch | später |

- **Förderkorb:** Eine kleine Entity mit Inventar, die an der Kette im Schacht hoch und runter fährt und an jedem Füllort hält. Er gehört zur [Förderhaspel](../berufe/grubenlogistiker.md#workstation-förderhaspel) und passt optisch zu Ketten und Holz. Der Durchsatz steigt mit dem Level des Logistikers.
- **Blasensäule:** Eine reine Vanilla-Mechanik (im Prototyp prüfen, wie zuverlässig Items darin steigen). Sie ist schnell, braucht aber Material aus dem Nether. Gut als Upgrade.
- **Eigene Aufzüge des Spielers:** Baut der Spieler selbst einen Aufzug, verknüpft er nur Ein- und Ausgangstruhe. Weil [Kisten die Schnittstelle sind](vision.md#designprinzipien), funktioniert das ohne Sonderlogik.

## Wachstum der Mine

1. Grubenwerkbank und Förderhaspel an der Oberfläche platzieren, die Mine startet mit 3×3 Chunks.
2. Minenarbeiter gräbt den Schacht bis zur ersten Etage und den Füllort.
3. Hauptgänge mit Stützen, dann Abbaustrecken im Radius ohne Schiene.
4. Mit Logistiker: Depots und Träger, die Mine wächst auf 5×5. Jede Lorenroute erweitert sie um eine Chunk-Reihe in ihrer Richtung.
5. Ist eine Etage abgebaut, gräbt der Minenarbeiter den Schacht weiter zur nächsten. Der Aufzug wird verlängert.

Wie viele Etagen gleichzeitig aktiv sind, hängt von der Zahl der Minenarbeiter ab: einer pro aktiver Etage.

## Chunk-Loading

- Geladen werden nur die Chunks der aktuellen Grundfläche (3×3, 5×5, bis 7×7); Grubenwerkbank und Förderhaspel stehen im Schacht-Chunk. Die Kosten ändern sich nur beim Ausbau (siehe [Chunk-Loading](../dorf/chunk-loading.md)).
- Der Spieler kann die Grundfläche kleiner halten als möglich, um Unterhalt zu sparen. Die Server-Config begrenzt die maximale Größe.

## Offene Fragen

- Etagenabstand: 6 Blöcke (wenig Verlust) oder mehr Gestein zwischen den Etagen?
- Darf der Minenarbeiter vorhandene Höhlen und Vanilla-Minenschächte in seine Etage einbeziehen, oder umgeht er sie?
- Förderkorb als Entity oder abstrakt mit Ketten-Animation?
- Unterhalt bei 1 Emerald pro Chunk: Eine 7×7-Mine kostet 49 Emeralds pro Woche, so viel wie ein Expert-Lohn. Bekommen Minen-Chunks einen Rabatt?

## Siehe auch

[Minenarbeiter](../berufe/minenarbeiter.md) · [Grubenlogistiker](../berufe/grubenlogistiker.md) · [Logistik](logistik.md) · [Chunk-Loading](../dorf/chunk-loading.md)
