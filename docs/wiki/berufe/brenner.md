# Beruf: Brenner

[← Berufe](README.md) · Station: **Brennofen** (neu) · Phase: **umgesetzt** (ohne Master-Fähigkeit)

Der Brenner bedient den Brennofen: Er brennt Baustoffe wie Glas, Stein, Terrakotta und Ziegel, macht Holzkohle und Farbstoffe. Er ist der dritte Ofen-Beruf neben dem [Schmied](schmied.md) am Blast Furnace und dem [Metzger](metzger.md) am Smoker.

## Stand der Umsetzung

- **Brennofen:** eigener Block mit Block-Entity, Rezept 1 Ofen + 4 Ziegel (Kreuz). Für Spieler wie ein Ofen: Ofen-Rezeptbuch (ohne Essen-Reiter), Brennstoff, Erfahrung, Hopper. Normales Ofentempo, Licht beim Brennen, Rauch auch oben aus dem „Kamin“. Texturen: die Ofen-Texturen mit Ziegeln statt Bruchstein (`tools/generate_textures.py`).
- **Was er brennt** steht in `KilnRecipes`: alle Zutaten von `minecraft:smelting`-Rezepten, außer solchen mit einem Essen-Rezept (Buch-Kategorie `food`) oder einem `minecraft:blasting`-Rezept und außer dem Item-Tag `workingvillagers:kiln_excluded` (leer). Liegt etwas anderes im Eingangsslot, tickt der Ofen wie ohne Zutat (Brennstoff brennt weiter herunter), es wird nicht gebrannt. Hopper und Shift-Klick legen nur Brennbares hinein.
- Der Knopf **i** oben rechts im Ofen-Fenster öffnet die Stationsübersicht, **Zurück** führt wieder zum Ofen.
- **Auftrag:** Die Übersicht zeigt alle Produkte des Brennofens (5 Reihen, bei mehr mit Seiten). Kein Produkt gewählt = **Alles** (jede Zutat einmal brennen). Ein Klick wählt ein Produkt, ein zweiter Klick hebt die Wahl wieder auf.
- **Mehrstufig:** Mit Auftrag nimmt er nur Zutaten, die (bis zu vier Brennvorgänge) zum Produkt führen. Zwischenstufen (Stein für Glatten Stein) behält er und legt sie wieder ein, sie landen nicht in der Zielkiste.
- **Vorrat** wie an der Hobelbank (nur mit gewähltem Produkt): ist er erreicht, holt er nichts mehr nach und wartet mit „Vorrat voll“.
- **Brennstoff** ist alles mit `cooking_fuel`, was er nicht brennen soll: Bei Auftrag „Holzkohle“ sind Stämme Zutat, bei anderen Aufträgen Brennstoff. Was er selbst aus dem Ofen genommen hat (Holzkohle, Stein bei „Alles“), merkt er sich und liefert es ab, statt es zu verfeuern oder noch einmal zu brennen (nicht gespeichert: nach einem Neustart zählt es wieder als Material).
- Der Ablauf ist der des Schmieds (`SmeltAtFurnace`, Art `KILN`): bis zu 16 Zutaten (Config `kilnOperator.batchSize`) und 8 Brennstoff (`kilnOperator.fuelPerTrip`) pro Gang.
- **Status:** „Holt Material und Brennstoff“, „Befüllt den Brennofen“, „Brennt“, „Wartet auf etwas zum Brennen“, „Wartet auf Brennstoff“, „Wartet: kommt nicht an den Brennofen“, „Vorrat voll (x/y)“.
- Profession `workingvillagers:kiln_operator` mit eigener Kleidung (terrakottafarbene Schürze, ziegelrote Mütze).
- **Nicht umgesetzt:** Master-Fähigkeit (es gibt noch keine Stufen).
- **Offene Fragen entschieden:** Der Brennofen brennt im normalen Ofentempo. Im Auftrag „Alles“ ist alles dabei, was er brennen kann, auch Kaktus und Schwamm.

## Workstation: Brennofen

- Ein neuer Ofen, der wie der Vanilla-Ofen funktioniert, nur **ohne Lebensmittel und ohne Erze**. Optik: gemauerter Ofen aus Ziegeln, damit er neben Ofen, Blast Furnace und Smoker erkennbar ist.
- Rezept: 1 Ofen + 4 Ziegel (wie der Blast Furnace einen Ofen braucht).
- Spieler können ihn wie einen Ofen benutzen, mit Rezeptbuch, Brennstoff und Erfahrung.
- Ist der POI für den Brenner. Nur angeheuerte Dorfbewohner arbeiten daran, wie an Hackklotz und Holzsäge.

### Was der Brennofen kann

Die drei Öfen teilen sich die Vanilla-Rezepte:

| Ofen | Kann | Tempo | Beruf |
| --- | --- | --- | --- |
| Blast Furnace | Erze, Eisen- und Goldwerkzeug | doppelt | [Schmied](schmied.md) |
| Smoker | Lebensmittel | doppelt | [Metzger](metzger.md) |
| **Brennofen** | alles, was ein Ofen kann, außer Lebensmitteln und dem, was der Blast Furnace kann | normal | **Brenner** |

- Grundlage sind die normalen Ofen-Rezepte (`minecraft:smelting`). Ausgeschlossen sind alle Rezepte der Kategorie **Essen** (`food` im Rezeptbuch) und alle Zutaten, für die es ein Blast-Furnace-Rezept gibt (`minecraft:blasting`: Erze, Rohmetall, Eisen- und Goldwerkzeug). So hat jeder der drei Öfen eigene Rezepte. Weil beides aus den Rezepten selbst kommt, sind Mods und Datapacks automatisch dabei, ohne eigene Liste.
- Zusätzlich gibt es ein Item-Tag `workingvillagers:kiln_excluded`, um einzelne Zutaten von Hand auszuschließen.
- Beispiele, die gehen: Sand → Glas, Bruchstein → Stein → Glatter Stein, Ton → Ziegel, Tonblock → Terrakotta, Terrakotta → Glasierte Terrakotta, Stämme → Holzkohle, Kaktus → Grüner Farbstoff, Nasser Schwamm → Schwamm, Netherrack → Netherziegel.
- Beispiele, die nicht gehen: rohes Fleisch, Fisch, Kartoffeln, Seetang (Smoker); Erze, Rohes Eisen, Eisenwerkzeug (Blast Furnace).

## Arbeitsablauf

Wie beim [Schmied](schmied.md), dessen Ablauf schon umgesetzt ist:

1. Bis zu 16 Zutaten und 8 Brennstoff aus den Quellkisten holen.
2. Den Brennofen befüllen. Er legt nur dieselbe Zutat nach, die gerade gebrannt wird, damit ein Stapel, den der Spieler eingelegt hat, zuerst fertig wird.
3. Fertiges herausnehmen.
4. In eine Zielkiste bringen.

Der Brennofen brennt selbst, im normalen Ofentempo. Die Erfahrung bleibt im Ofen, der Spieler bekommt sie beim nächsten Herausnehmen.

## Auftrag

Anders als beim Schmied gibt es einen Auftrag, weil in den Quellkisten oft Dinge liegen, die nicht gebrannt werden sollen (Sand für Beton, Bruchstein zum Bauen):

- **Alles** (Standard): brennt alles, was der Brennofen kann.
- **Ein Produkt**: z. B. nur Glas oder nur Glatten Stein. Die Liste zeigt die Ergebnisse der Brennofen-Rezepte, wie die Produktliste der Hobelbank.
- **Vorrat:** Wie an der [Hobelbank](tischler.md) lässt sich einstellen, wie viele Produkte in den Zielkisten liegen sollen. Ist der Vorrat erreicht, macht er Pause.
- Mehrstufig: Bei „Glatter Stein“ brennt er Bruchstein zuerst zu Stein und den dann weiter. Die Zwischenstufe bleibt im Ofen-Kreislauf und landet nicht in der Zielkiste.

## Quellkiste / Zielkiste

| Quellkiste | Zielkiste |
| --- | --- |
| Sand, Bruchstein, Ton, Stämme, Kaktus, …; Brennstoff (Kohle, Holzkohle, Stämme, Lava-Eimer) | Glas, Stein, Ziegel, Terrakotta, Holzkohle, Farbstoffe, leere Eimer |

- Ohne Quell- und Zielkiste arbeitet er nicht, wie der Schmied.
- Brennstoff ist alles, was brennt. Ein leerer Eimer vom Lava-Eimer wird herausgenommen und abgeliefert.
- Stämme sind Zutat (für Holzkohle) und Brennstoff zugleich. Bei Auftrag „Holzkohle“ nimmt er Stämme nur als Zutat und verbrennt Holzkohle als Brennstoff, sonst würde er seine eigene Zutat verfeuern.

## Kette

```
 Grubenarbeiter ──Sand, Ton, Bruchstein──► Brenner ──Glas, Stein, Ziegel──► Steinsäge / Spieler
 Holzfäller ─────Stämme────────────────────►   └──Holzkohle──► Schmied (Brennstoff)
```

- Der [Grubenarbeiter](grubenarbeiter.md) liefert die Rohstoffe, der [Träger](traeger.md) bringt sie zum Brennofen.
- Stein und Glatter Stein gehen weiter an die Steinsäge ([Maurer](maurer.md)).
- Holzkohle kann der Schmied als Brennstoff nutzen.

## Master-Fähigkeit

Sparsames Feuern: Ein Brennstoff reicht für 25 % mehr Items.

## Sonderfälle & offene Fragen

- Soll ein Brennofen schneller brennen als ein Ofen, oder bleibt der Vorteil allein, dass er bedient wird?
- Nasser Schwamm, Kaktus und andere Kleinigkeiten: im Auftrag „Alles“ dabei, oder nur Baustoffe?

## Fertig, wenn

Ein Brenner brennt Sand aus der Quellkiste zu Glas und bringt es in die Zielkiste. Rohes Fleisch und Rohes Eisen in derselben Quellkiste lässt er liegen.

## Siehe auch

[Schmied](schmied.md) · [Metzger](metzger.md) · [Grubenarbeiter](grubenarbeiter.md) · [Maurer](maurer.md) · [Logistik](../grundlagen/logistik.md)
