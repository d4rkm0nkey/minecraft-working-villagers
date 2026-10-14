# Beruf: Tischler

[← Berufe](README.md) · Station: **Hobelbank** (neu) · Phase: **umgesetzt (MVP)**

Der Tischler stellt aus Planks, Sticks und Slabs alle Holzgegenstände her, die nicht aus dem Woodcutter kommen: Kisten, Fässer, Leitern, Boote, Möbel und die Workstations der anderen Berufe.

## Workstation: Hobelbank

- Eigener Block statt der Vanilla-Werkbank. An einer Werkbank würde der Spieler erwarten, dass der Arbeiter *alles* craftet. Die Hobelbank sagt dagegen schon durch ihre Optik: Hier wird nur Holz verarbeitet.
- Rezept: drei Holz-Slabs oben, Eisenbarren in der Mitte, zwei Stämme als Beine. Das passt zum Woodcutter (Eisen + Holz).
- Optik: Tisch mit dicker Platte, vier Beinen und Querstreben, an der rechten Seite ein eiserner Schraubstock mit Knebel. Vorne hängt wie bei allen Stationen die Notiz mit dem Namen des Arbeiters.
- Auch der Spieler kann an ihr arbeiten: Ein Menü listet alle Tischler-Rezepte. Was er mangels Zutaten im Inventar nicht bauen kann, ist ausgegraut; der Tooltip nennt die Zutaten. Die Hobelbank ist damit eine Werkbank nur für Holz, aber mit Rezeptliste statt 3×3-Raster.
- Das Menü funktioniert wie beim Woodcutter: Auftrag setzen, **i** öffnet die Übersicht mit Arbeiter, Input- und Output-Kisten.

## Was er herstellt

| Stufe | Produkte | Zusätzliche Zutaten |
| --- | --- | --- |
| 1 – nur Holz | Kiste, Fass, Werkbank, Leiter, Schüssel, Boot/Floß, Composter, Shelf, Holzwerkzeuge | – |
| 2 – Holz + Zusatz | Truhenboot, Bücherregal, Hängeschild, Bett, Item Frame, Armor Stand, Bogen, Angel, Schild, Fackel, Tripwire Hook | Bücher, Ketten, Wolle, Leder, Stein-Slab, Schnur, Eisen, Kohle |
| 3 – Workstations | Lectern, Loom, Fletching Table, Cartography Table, Smithing Table, Note Block, Jukebox | Bücherregal, Schnur, Feuerstein, Papier, Eisen, Redstone, Diamant |

Mit Stufe 3 baut der Tischler die Arbeitsplätze für die anderen Berufe und wird so zum Ausbauberuf des Dorfs. Die Stufen hängen an den [Lohnstufen](../wirtschaft/anheuern-und-lohn.md#lohnstufen): Novice kann Stufe 1, ab Journeyman Stufe 2, Master Stufe 3 (Startwerte für Playtests).

### Rezepte

- Die Rezepte werden nicht selbst gepflegt, sondern aus den Vanilla-Crafting-Rezepten (`minecraft:crafting`) gelesen.
- Welche davon der Tischler kann, legt ein Item-Tag fest: `workingvillagers:joinery` (bzw. ein Tag je Stufe). Holz und Möbel aus anderen Mods kommen per Datapack dazu, wie bei `workingvillagers:woodcutting`.
- Rezepte mit Rückgabe (Eimer o. Ä.) oder Komponenten-Daten bleiben außen vor.

## Aufträge

Ein Auftrag ist ein Produkt plus eine Mengenregel.

- **Holzartgebundene Produkte** (Boot, Hängeschild, Shelf, Holzwerkzeuge mit Holzart): wie beim Sawyer, also Produkt plus Holzarten an/aus.
- **Holzartneutrale Produkte** (Kiste, Werkbank, Leiter, Fass): Vanilla erlaubt gemischte Planks. Der Tischler nimmt die Holzart, von der am meisten da ist. Einzelne Holzarten lassen sich sperren („keine Kirsche verbrauchen“).
- **Mengenregel:**
  - *Dauerauftrag*: so lange Material da ist.
  - *Stückzahl*: „12 Fässer, dann fertig“. Danach ist die Station frei und zeigt „Auftrag erledigt“.
  - *Vorrat halten*: „immer 8 Kisten in den Output-Kisten“. Er arbeitet nur, wenn der Bestand darunter fällt.

## Arbeitsablauf

1. Lohn aus der [persönlichen Kiste](../wirtschaft/anheuern-und-lohn.md) nehmen, wie bei allen Arbeitern.
2. Prüfen, ob der Auftrag noch etwas zu tun hat (Stückzahl erreicht, Vorrat voll?).
3. Zutaten für bis zu einen Stapel Werkstücke aus den Input-Kisten holen. Fehlt eine Zutat, wartet er mit Status „wartet auf Sticks“.
4. An der Hobelbank arbeiten: Armschwung, Hobelspäne, Holz-Sounds. Die Dauer richtet sich nach der Zahl der Zutaten, etwa eine halbe Sekunde je Zutat und mindestens eine Sekunde je Werkstück (Leiter schnell, Lectern langsam).
5. Werkstücke in eine Output-Kiste bringen, übrige Zutaten zurück in die Input-Kiste.

## Quellkiste / Zielkiste

| Quellkiste | Zielkiste |
| --- | --- |
| Planks, Sticks, Slabs, Zusatzzutaten (Bücher, Wolle, Eisen …) | Fertige Holzgegenstände |

### Zwischenprodukte

Der Tischler nimmt nur, was in den Input-Kisten liegt. Sticks und Slabs kommen vom [Sawyer](holzfaeller.md), der [Träger](traeger.md) verbindet die Kisten. So lohnt sich die Kette Holzfäller → Sawyer → Träger → Tischler.

## Status

Neue Zeilen für das Menü: „baut Fässer (3/12)“, „wartet auf Sticks“, „Vorrat voll (8/8)“, „Auftrag erledigt“.

## Master-Fähigkeit

Stellt Zwischenprodukte selbst her: Fehlen Sticks oder Slabs, macht er sie aus Planks, langsamer als der Sawyer. Außerdem schaltet er Stufe 3 (Workstations) frei.

## Sonderfälle & offene Fragen

- **Werkzeug:** Braucht der Tischler eine Axt aus der persönlichen Kiste, die je Werkstück abnutzt? Das wäre konsistent mit dem Holzfäller, ist aber vielleicht unnötige Pflege. Vorschlag: zunächst ohne Werkzeug.
- **Mehrere Aufträge:** Eine Auftragsliste („erst 200 Leitern, dann 50 Kisten“) wäre praktisch, kommt aber erst nach dem MVP.
- **Shulker-Kisten, Lectern mit Buch usw.:** Alles mit Komponenten oder Nicht-Holz-Kern bleibt draußen. Im Zweifel entscheidet der Tag.
- **Abgrenzung zum Crafter-Bediener:** Der Tischler ist früh verfügbar, schnell und billig, kann aber nur Holz. Der [Crafter-Bediener](crafter-bediener.md) kann später jedes Rezept, ist dafür langsamer und braucht Redstone-Technik.

## MVP (umgesetzt)

Hobelbank mit Spieler-Menü, Stufe 1 und 2 über den Tag `workingvillagers:joinery`, Mengenregeln *Dauerauftrag* und *Vorrat halten*, Zwischenprodukte nur aus den Input-Kisten. Menüs, Kistenverwaltung, persönliche Kiste und Lohn kommen vom Sawyer.

Stand der Umsetzung:

- Die Stufen hängen noch nicht an den Lohnstufen, weil alle Arbeiter vorerst Novizen sind. Der Tag enthält Stufe 1 und 2, Stufe 3 (Workstations) fehlt noch.
- Betten fehlen bewusst (16 Farben, Wolle). Sie lassen sich per Datapack über den Tag ergänzen.
- Pro Gang holt der Tischler Zutaten für höchstens 32 Items. Die Holzart wählt er nicht nach der größten Menge, er nimmt einfach, was erlaubt ist (gemischte Planks sind in Vanilla erlaubt).
- Der Vorrat lässt sich in Stufen einstellen: ohne Grenze, 1, 2, 4, 8, 12, 16, 24, 32, 48, 64, 96, 128, 192, 256.
- Statt einer Axt als Werkzeug braucht er vorerst nichts.

Später: Stufe 3, *Stückzahl*, Auftragsliste, Master-Fähigkeit.

## Siehe auch

[Holzfäller & Sawyer](holzfaeller.md) · [Träger](traeger.md) · [Crafter-Bediener](crafter-bediener.md) · [Logistik](../grundlagen/logistik.md)
