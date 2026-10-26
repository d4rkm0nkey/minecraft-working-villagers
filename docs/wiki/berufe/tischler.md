# Beruf: Tischler

[← Berufe](README.md) · Station: **Hobelbank** (neu) · Phase: **umgesetzt (MVP)**

Der Tischler stellt aus Planks, Sticks und Slabs alle Holzgegenstände her, die nicht aus der [Holzsäge](holzsaeger.md) kommen: Kisten, Fässer, Leitern, Boote, Möbel und die Workstations der anderen Berufe.

## Workstation: Hobelbank

- Eigener Block statt der Vanilla-Werkbank. An einer Werkbank würde der Spieler erwarten, dass der Arbeiter *alles* craftet. Die Hobelbank sagt dagegen schon durch ihre Optik: Hier wird nur Holz verarbeitet.
- Rezept: drei Holz-Slabs oben, Eisenbarren in der Mitte, zwei Stämme als Beine. Das passt zur Holzsäge (Eisen + Holz).
- Optik: Tisch mit dicker Platte, vier Beinen und Querstreben, an der rechten Seite ein eiserner Schraubstock mit Knebel.
- Auch der Spieler kann an ihr arbeiten: Ein Menü listet alle Tischler-Rezepte. Was er mangels Zutaten im Inventar nicht bauen kann, ist ausgegraut; der Tooltip nennt die Zutaten. Die Hobelbank ist damit eine Werkbank nur für Holz, aber mit Rezeptliste statt 3×3-Raster.
- Das Menü funktioniert wie bei der Holzsäge: Auftrag setzen, **i** öffnet die Übersicht mit Arbeiter, Input- und Output-Kisten.

## Was er herstellt

| Stufe | Produkte | Zusätzliche Zutaten |
| --- | --- | --- |
| 1 – nur Holz | Kiste, Fass, Werkbank, Leiter, Schüssel, Boot/Floß, Composter, Shelf, Holzwerkzeuge | – |
| 2 – Holz + Zusatz | Truhenboot, Bücherregal, Hängeschild, Bett, Item Frame, Armor Stand, Angel, Schild, Fackel, Tripwire Hook | Bücher, Ketten, Wolle, Leder, Stein-Slab, Schnur, Eisen, Kohle |
| 3 – Workstations | Lectern, Loom, Fletching Table, Cartography Table, Smithing Table, Note Block, Jukebox | Bücherregal, Schnur, Feuerstein, Papier, Eisen, Redstone, Diamant |

Mit Stufe 3 baut der Tischler die Arbeitsplätze für die anderen Berufe und wird so zum Ausbauberuf des Dorfs. Die Stufen hängen am [Level](../grundlagen/level.md#was-das-level-freischaltet): Neuling kann Stufe 1, ab Lehrling Stufe 2, ab Geselle Stufe 3 (Startwerte für Playtests).

### Rezepte

- Die Rezepte werden nicht selbst gepflegt, sondern aus den Vanilla-Crafting-Rezepten (`minecraft:crafting`) gelesen.
- Welche davon der Tischler kann, legt ein Item-Tag fest: `workingvillagers:joinery` (bzw. ein Tag je Stufe). Holz und Möbel aus anderen Mods kommen per Datapack dazu, wie bei `workingvillagers:woodcutting`.
- Rezepte mit Rückgabe (Eimer o. Ä.) oder Komponenten-Daten bleiben außen vor.

## Aufträge

Ein Auftrag ist ein Produkt plus eine Mengenregel.

- **Holzartgebundene Produkte** (Boot, Hängeschild, Shelf, Holzwerkzeuge mit Holzart): wie beim [Holzsäger](holzsaeger.md#auftrag), also Produkt plus Holzarten an/aus.
- **Holzartneutrale Produkte** (Kiste, Werkbank, Leiter, Fass): Vanilla erlaubt gemischte Planks. Der Tischler nimmt die Holzart, von der am meisten da ist. Einzelne Holzarten lassen sich sperren („keine Kirsche verbrauchen“).
- **Mengenregel:**
  - *Dauerauftrag*: so lange Material da ist.
  - *Stückzahl*: „12 Fässer, dann fertig“. Danach ist die Station frei und zeigt „Auftrag erledigt“.
  - *Vorrat halten*: „immer 8 Kisten in den Output-Kisten“. Er arbeitet nur, wenn der Bestand darunter fällt.

## Arbeitsablauf

1. Lohn aus den Emeralds nehmen, die er bei sich trägt (siehe [Anheuern & Lohn](../wirtschaft/anheuern-und-lohn.md#stand-der-umsetzung)), wie bei allen Arbeitern.
2. Prüfen, ob der Auftrag noch etwas zu tun hat (Stückzahl erreicht, Vorrat voll?).
3. Bis zu 32 Zutaten aus den Input-Kisten holen (Config `carpenter.ingredientsPerTrip`), mindestens die für ein Werkstück. Fehlt eine Zutat, wartet er mit Status „wartet auf Sticks“.
4. An der Hobelbank arbeiten: Armschwung, Hobelspäne, Holz-Sounds. Die Dauer richtet sich nach der Zahl der Zutaten, etwa eine halbe Sekunde je Zutat und mindestens eine Sekunde je Werkstück (Leiter schnell, Lectern langsam).
5. Werkstücke in eine Output-Kiste bringen, übrige Zutaten zurück in die Input-Kiste.

## Quellkiste / Zielkiste

| Quellkiste | Zielkiste |
| --- | --- |
| Planks, Sticks, Slabs, Zusatzzutaten (Bücher, Wolle, Eisen …) | Fertige Holzgegenstände |

### Zwischenprodukte

Der Tischler nimmt nur, was in den Input-Kisten liegt. Sticks und Slabs kommen vom [Holzsäger](holzsaeger.md), der [Träger](traeger.md) verbindet die Kisten. So lohnt sich die Kette Holzfäller → Holzsäger → Träger → Tischler.

## Status

Neue Zeilen für das Menü: „baut Fässer (3/12)“, „wartet auf Sticks“, „Vorrat voll (8/8)“, „Auftrag erledigt“.

## Master-Fähigkeit

Stellt Zwischenprodukte selbst her: Fehlen Sticks oder Slabs, macht er sie aus Planks, langsamer als der Holzsäger.

## Sonderfälle & offene Fragen

- **Werkzeug:** Braucht der Tischler eine Axt, die man ihm gibt und die je Werkstück abnutzt? Das wäre konsistent mit dem Holzfäller, ist aber vielleicht unnötige Pflege. Vorschlag: zunächst ohne Werkzeug.
- **Mehrere Aufträge:** siehe [Mehrere Produkte](#mehrere-produkte-umgesetzt-in-050). Eine Auftragsliste nacheinander („erst 200 Leitern, dann 50 Kisten“) bleibt eine Idee für später.
- **Shulker-Kisten, Lectern mit Buch usw.:** Alles mit Komponenten oder Nicht-Holz-Kern bleibt draußen. Im Zweifel entscheidet der Tag.
- **Abgrenzung zum Crafter-Bediener:** Der Tischler ist früh verfügbar, schnell und billig, kann aber nur Holz. Der [Crafter-Bediener](crafter-bediener.md) kann später jedes Rezept, ist dafür langsamer und braucht Redstone-Technik.

## MVP (umgesetzt)

Hobelbank mit Spieler-Menü, Stufe 1 und 2 über den Tag `workingvillagers:joinery`, Mengenregeln *Dauerauftrag* und *Vorrat halten*, Zwischenprodukte nur aus den Input-Kisten. Menüs, Kistenverwaltung und Lohn kommen vom Holzsäger.

Stand der Umsetzung:

- Stufe 2 kann er ab Lehrling (Tag `workingvillagers:unlocks/carpenter/apprentice`, siehe [Level und Aufstieg](../grundlagen/level.md)). Der Tag `joinery` enthält Stufe 1 und 2, Stufe 3 (Workstations) fehlt noch.
- Betten fehlen bewusst (16 Farben, Wolle). Sie lassen sich per Datapack über den Tag ergänzen.
- Pro Gang holt der Tischler höchstens 32 Zutaten (nicht 32 Werkstücke), mindestens aber die Zutaten für ein Werkstück. Die Holzart wählt er nicht nach der größten Menge, er nimmt einfach, was erlaubt ist (gemischte Planks sind in Vanilla erlaubt).
- Der Vorrat lässt sich in Stufen einstellen: ohne Grenze, 1, 2, 4, 8, 12, 16, 24, 32, 48, 64, 96, 128, 192, 256.
- Statt einer Axt als Werkzeug braucht er vorerst nichts.

Später: Stufe 3, *Stückzahl*, Auftragsliste, Master-Fähigkeit.

## Mehrere Produkte (umgesetzt in 0.5.0)

Ein Auftrag hat bis zu **8 Produkte** (`Workstations.MAX_ORDERS`), und der Tischler stellt sie **zu gleichen Teilen** her. Dasselbe gilt für [Holzsäger und Steinsäger](holzsaeger.md), [Brenner](brenner.md), [Pfeilmacher](pfeilmacher.md), [Gerber](gerber.md) und [Waffenschmied](waffenschmied.md). Nur der [Apotheker](apotheker.md) braut weiter einen Trank nach dem anderen (seine Brauwege laufen über Zwischentränke am Braustand).

### Bedienung

- Im Reiter **Auftrag** schaltet ein Klick ein Produkt an oder aus. Über dem Raster steht „Erzeugnisse (2/8)“. Ist der Auftrag voll, sagt der Tooltip das, und ein Klick meldet „Der Auftrag ist voll“.
- **Holzarten** gelten für alle Produkte gemeinsam. Ein holzartgebundenes Produkt (Treppe an der Holzsäge, Boot an der Hobelbank) ist **ein** Produkt in allen erlaubten Holzarten: Bestand und Zähler zählen alle Holzarten zusammen, wie bisher beim einzelnen Auftrag. (Abweichung vom Konzept, das jede Holzart einzeln verteilen wollte: so bleibt das Verhalten alter Stationen gleich.)
- Das Auftragsfeld an Hobelbank und Säge zeigt die Produkte nacheinander (eine Sekunde je Produkt) mit „×3“ in der Ecke, der Tooltip listet alle.
- Der **Vorrat** gilt **je Produkt**.

### Verteilung

`Station.ordersByNeed` sortiert die Produkte nach Bedarf:

1. **Mit Vorrat:** wenigster Bestand in den Ausgangskisten zuerst; volle fallen heraus.
2. **Ohne Vorrat:** die Station zählt je Produkt, was gemacht wurde (`made`, gespeichert). Es gewinnt der kleinste Zähler. Ein neu dazugenommenes Produkt startet beim kleinsten Zähler der anderen. Gespeichert werden nur die Abstände (der kleinste Zähler wird immer auf 0 gezogen).
3. **Gleichstand:** Reihenfolge der Auswahl.

Jeder Beruf nimmt davon das erste Produkt, für das er Material hat, und macht **einen Satz** davon; danach plant er neu. Fehlt für ein Produkt Material, kommt das nächste dran. Die Säger holen bei mehreren Produkten nur ihren Anteil am Satz (`sawyer.batchSize` geteilt durch die Zahl der Produkte), sonst würde das erste Produkt das ganze Holz verbrauchen. Der Brenner wählt das Produkt beim Füllen eines leeren Ofens.

Ist ein Produkt Material eines anderen (Kisten für Truhenboote, Steinziegel für Steinziegeltreppen), liefert der Arbeiter es trotzdem ab, statt es weiterzuverarbeiten.

### Daten

Das Feld `order` der Station ist jetzt eine Liste; ein einzelnes Item aus 0.4.0 wird beim Laden zu einer Liste mit einem Eintrag. Zurück zu 0.4.0 geht nicht.

### Tests

Kisten + Fässer mit Vorrat 2; Kisten + Leitern ohne Sticks (die Kisten kommen trotzdem); Bretter + Treppen an der Holzsäge; Glas + Stein im Brennofen; Pfeile + Bögen; Eisenaxt + Eisenspitzhacke; Bücher + Bündel.

### Offen

- **Gewichte** („doppelt so viele Kisten wie Fässer“).
- Mehrere Tränke beim Apotheker.
- Eine Auftragsliste nacheinander („erst 200 Leitern, dann 50 Kisten“).

## Siehe auch

[Holzfäller](holzfaeller.md) · [Holzsäger](holzsaeger.md) · [Träger](traeger.md) · [Crafter-Bediener](crafter-bediener.md) · [Logistik](../grundlagen/logistik.md)

## Bestellungen (umgesetzt)

Der Spieler sagt „ich brauche 64 Kisten“, der Arbeiter holt sich das Material und meldet, was fehlt. Gilt für alle Stationen mit Auftrag: Holzsäge, Steinsäge, Hobelbank, Brennofen, Bognertisch, Kessel, Schleifstein, Braustand.

### Bedienung

- Ein Klick auf ein Produkt im Auftrag **wählt es aus** (weißer Rahmen), ein weiterer Klick nimmt es heraus. Ein neu dazugenommenes Produkt ist gleich ausgewählt.
- Die Zeile unter dem Raster zeigt das ausgewählte Produkt, links von − / + schaltet ein Knopf je Produkt zwischen
  - **Bestellung** (Papier): eine feste Menge einmal, Stufen 1 bis 1024. Im Raster steht in der Ecke, wie viele noch fehlen.
  - **Vorrat** (Kiste): wie bisher, jetzt **je Produkt** einstellbar. Der alte gemeinsame Vorrat gilt für Produkte ohne eigenen weiter (alte Stationen laden unverändert).
- Wechsel zur Bestellung startet mit dem bisherigen Vorrat oder einem Stapel (1 bei nicht stapelbaren). Mehr bestellen erhöht den Rest, weniger zieht ab; was schon gemacht ist, zählt.

### Ablauf

- **Reihenfolge:** Bestellungen vor Vorrat vor „ohne Grenze“ (`Station.ordersByNeed`).
- **Material** kommt aus den Input-Kisten und den **Dorftruhen** in Reichweite (`Orders.sources`); eine Station braucht dann keine eigene Input-Kiste mehr. Der Brenner ohne Auftrag („alles brennen“) nimmt weiter nur aus seinen Input-Kisten.
- Er holt nur so viel, wie die Bestellung noch braucht (Säger: 10 Bretter → 3 Stämme).
- **Erledigt:** gezählt wird beim Herstellen (`addMade`), erledigt ist eine Bestellung, wenn er nichts mehr davon trägt, also alles abgeliefert ist (`Orders.finish`). Dann verschwindet das Produkt aus dem Auftrag, der Arbeiter zeigt ein grünes ✔ und Glückspartikel. Der Brenner behält seine letzte erledigte Bestellung (markiert als abgeliefert), sonst würde er alles brennen.
- **Fehlt Material**, macht er, was geht, und zeigt dann ein rotes **!** über dem Kopf (`WorkerNotice`, synchronisiert über die Entity-Daten). Der Tischler nennt die Menge für die ganze Restbestellung („Es fehlen 18× Eisenbarren“).
- **Zwischenprodukte:** Fehlt eine Zutat, die die eigene Station herstellt, macht er sie zuerst, bis zu zwei Stufen zurück (Tischler: Kiste und Boot für das Truhenboot; Pfeilmacher: Pfeile für Spektralpfeile). Brenner, Gerber und Apotheker konnten das schon (Stein → glatter Stein, Leder aus Fleisch, Brauwege). Das geht ab Novize, nicht erst als Master-Fähigkeit wie oben geplant.

### Tests

Drei Kisten bestellt (genau drei, Rest bleibt liegen); Kisten aus der Dorftruhe ohne Input-Kiste; vier bestellt, Material für zwei (Status und „!“); Truhenboot nur aus Planks; zehn Bretter an der Säge (drei Stämme); zwei Glas im Brennofen (Bestellung bleibt); Spektralpfeile ohne Pfeile.
