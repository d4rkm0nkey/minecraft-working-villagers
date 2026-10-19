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

Mit Stufe 3 baut der Tischler die Arbeitsplätze für die anderen Berufe und wird so zum Ausbauberuf des Dorfs. Die Stufen hängen an den [Lohnstufen](../wirtschaft/anheuern-und-lohn.md#lohnstufen): Novice kann Stufe 1, ab Journeyman Stufe 2, Master Stufe 3 (Startwerte für Playtests).

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

Stellt Zwischenprodukte selbst her: Fehlen Sticks oder Slabs, macht er sie aus Planks, langsamer als der Holzsäger. Außerdem schaltet er Stufe 3 (Workstations) frei.

## Sonderfälle & offene Fragen

- **Werkzeug:** Braucht der Tischler eine Axt, die man ihm gibt und die je Werkstück abnutzt? Das wäre konsistent mit dem Holzfäller, ist aber vielleicht unnötige Pflege. Vorschlag: zunächst ohne Werkzeug.
- **Mehrere Aufträge:** siehe [Konzept: Mehrere Produkte](#konzept-mehrere-produkte). Eine Auftragsliste nacheinander („erst 200 Leitern, dann 50 Kisten“) bleibt eine Idee für später.
- **Shulker-Kisten, Lectern mit Buch usw.:** Alles mit Komponenten oder Nicht-Holz-Kern bleibt draußen. Im Zweifel entscheidet der Tag.
- **Abgrenzung zum Crafter-Bediener:** Der Tischler ist früh verfügbar, schnell und billig, kann aber nur Holz. Der [Crafter-Bediener](crafter-bediener.md) kann später jedes Rezept, ist dafür langsamer und braucht Redstone-Technik.

## MVP (umgesetzt)

Hobelbank mit Spieler-Menü, Stufe 1 und 2 über den Tag `workingvillagers:joinery`, Mengenregeln *Dauerauftrag* und *Vorrat halten*, Zwischenprodukte nur aus den Input-Kisten. Menüs, Kistenverwaltung und Lohn kommen vom Holzsäger.

Stand der Umsetzung:

- Die Stufen hängen noch nicht an den Lohnstufen, weil alle Arbeiter vorerst Novizen sind. Der Tag enthält Stufe 1 und 2, Stufe 3 (Workstations) fehlt noch.
- Betten fehlen bewusst (16 Farben, Wolle). Sie lassen sich per Datapack über den Tag ergänzen.
- Pro Gang holt der Tischler höchstens 32 Zutaten (nicht 32 Werkstücke), mindestens aber die Zutaten für ein Werkstück. Die Holzart wählt er nicht nach der größten Menge, er nimmt einfach, was erlaubt ist (gemischte Planks sind in Vanilla erlaubt).
- Der Vorrat lässt sich in Stufen einstellen: ohne Grenze, 1, 2, 4, 8, 12, 16, 24, 32, 48, 64, 96, 128, 192, 256.
- Statt einer Axt als Werkzeug braucht er vorerst nichts.

Später: Stufe 3, *Stückzahl*, Auftragsliste, Master-Fähigkeit.

## Konzept: Mehrere Produkte

Bisher hat die Hobelbank genau ein Produkt. Wer Kisten, Fässer und Leitern braucht, braucht drei Tischler oder stellt den Auftrag ständig um. Künftig wählt man **mehrere Produkte**, und der Tischler stellt sie **möglichst zu gleichen Teilen** her.

### Bedienung

- Im Reiter **Auftrag** schaltet ein Klick auf ein Produkt im Raster es **an oder aus**, statt das bisherige zu ersetzen. Gewählte Produkte haben einen hellen Rahmen und einen Haken.
- Höchstens **8 Produkte** (Config `carpenter.maxProducts`). Beim neunten kommt die Meldung „Höchstens 8 Produkte“.
- **Holzarten** gelten für alle Produkte gemeinsam, wie heute. Ein holzartgebundenes Produkt (Boot, Hängeschild, Shelf) zählt im Raster als ein Eintrag; jede erlaubte Holzart davon ist beim Verteilen aber ein eigenes Produkt (Eichenboot und Fichtenboot wechseln sich also auch ab).
- Das Auftragsfeld in der Hobelbank zeigt die gewählten Produkte nacheinander (Wechsel jede Sekunde) mit einer kleinen Zahl „×3“. Der Tooltip listet alle mit ihrem Bestand.
- Der **Vorrat** gilt **je Produkt**: „Vorrat 8“ heißt 8 Kisten *und* 8 Fässer *und* 8 Leitern. Das ist die naheliegende Lesart und macht gleiche Teile einfach.

### Was „gleiche Teile“ heißt

Gezählt werden **Stück**, nicht Arbeitsgänge, weil der Spieler Stück in der Kiste sieht (eine Leiter-Runde ergibt 3 Leitern, eine Kiste-Runde 1 Kiste).

1. **Mit Vorrat:** Der Tischler zählt jedes Produkt in den Ausgangskisten. Er macht das Produkt, von dem **am wenigsten da ist** (im Verhältnis zum Vorrat), aber nur, wenn er Material dafür hat. Sind alle voll: „Vorrat voll“.
2. **Ohne Vorrat (Dauerauftrag):** Den Bestand in der Kiste kann man nicht nehmen, weil Träger ihn wegbringen. Stattdessen führt die Station einen **Zähler je Produkt** („gemacht seit Auftragsänderung“). Er macht das Produkt mit dem kleinsten Zähler. Ändert sich die Auswahl, starten alle Zähler bei 0; ein neu dazugenommenes Produkt startet beim kleinsten Zähler der anderen, damit es nicht alles andere aufholt.
3. **Gleichstand:** Reihenfolge im Raster.

### Material und Gänge

- **Ein Produkt ohne Material blockiert die anderen nicht.** Es wird übersprungen, bis wieder Material da ist. Erst wenn für *kein* Produkt Material da ist, wartet er („wartet auf Sticks“). Der Tooltip zeigt pro Produkt, was fehlt.
- Damit der Tischler nicht für jedes Stück zur Kiste läuft, bleibt es bei **einem Satz pro Gang** (bis zu 32 Zutaten). Der Satz wird aber auf den **Rückstand** des Produkts begrenzt: Fehlen nur 2 Kisten bis zum Gleichstand, holt er Material für 2 Kisten und plant danach neu.
- Teilen sich Produkte ein Material (Kisten und Fässer brauchen beide Planks), regelt das der Zähler von selbst: Er wechselt ab, statt alle Planks in Kisten zu stecken.
- Material, das kein gewähltes Produkt mehr braucht, bringt er wie heute zurück in die Eingangskiste.

### Status

- „Baut Fässer · Kiste 4 · Fass 3 · Leiter 3“ (Zähler bzw. Bestand/Vorrat).
- „Vorrat voll (8/8 je Produkt)“, „Wartet auf Sticks (Leiter, Fass)“.

### Daten und Umstieg

- `order` (ein Item) wird zu `orders` (Liste). Alte Welten: ein vorhandenes `order` wird beim Laden zu einer Liste mit einem Eintrag.
- Die Zähler liegen in der Station (gespeichert), nicht im Arbeiter: Ein neuer Tischler macht dort weiter.
- Die Verteilung (`nextProduct(station, verfügbar)`) wird allgemein gebaut, so dass [Brenner](brenner.md), [Pfeilmacher](pfeilmacher.md) und Holz-/Steinsäger sie später übernehmen können.

### Tests

- Kisten + Fässer, Vorrat 2, genug Planks → je 2 in der Ausgangskiste.
- Kisten + Leitern, keine Sticks → er macht nur Kisten und meldet nicht „wartet“.
- Dauerauftrag Kisten + Fässer mit 64 Planks → Unterschied der beiden Zähler höchstens ein Satz.
- Alte Station mit `order` lädt als Auftrag mit einem Produkt.

### Offene Fragen

- **Gewichte** („doppelt so viele Kisten wie Fässer“)? Empfehlung: nicht im ersten Schritt; ein Vorrat je Produkt wäre die spätere Erweiterung.
- **8 als Grenze?** Mehr ist technisch kein Problem, aber die Status-Zeile und der Tooltip werden unübersichtlich.

## Siehe auch

[Holzfäller](holzfaeller.md) · [Holzsäger](holzsaeger.md) · [Träger](traeger.md) · [Crafter-Bediener](crafter-bediener.md) · [Logistik](../grundlagen/logistik.md)
