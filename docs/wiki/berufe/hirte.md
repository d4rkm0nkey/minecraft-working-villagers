# Beruf: Hirte

[← Berufe](README.md) · Station: **Webstuhl** (Vanilla) · Phase: **umgesetzt** (Scheren und Züchten, ohne Wolle sortieren und Master-Fähigkeit)

Der Hirte kümmert sich um die Schafe rund um seinen Webstuhl: scheren, züchten und die Wolle in die Zielkiste bringen.

## Workstation: Webstuhl

- Der normale Vanilla-Webstuhl. Der Knopf mit dem Gesicht des Hirten oben rechts im Webstuhl-Menü öffnet die Stationsübersicht, wie beim Schmiedetisch. Er überdeckt den oberen Rand der Banner-Vorschau; der Webstuhl selbst funktioniert wie in Vanilla.
- Vanilla-Schäfer aus dem Dorf arbeiten weiter an Webstühlen. Ein angeheuerter Dorfbewohner wird dort Hirte dieses Mods. Einen eingerichteten Webstuhl (Kisten verknüpft) nehmen gewöhnliche Dorfbewohner nicht.
- Kisten verknüpft man mit **„+ Kiste“**. Er braucht nur Zielkisten, keinen Eingang.

## Stand der Umsetzung

- **Weide:** alle Schafe im Umkreis von 12 Blöcken um den Webstuhl (seitlich, Config `shepherd.pastureRadius`, 4–24), bis 4 Blöcke höher oder tiefer. Ein Gehege muss man nicht markieren.
- **Scheren:** Jedes Schaf mit Wolle schert er wie ein Spieler (Vanilla-Beute, Wolle in der Farbe des Schafs) und sammelt die Wolle direkt ein. Etwa 1,5 Sekunden pro Schaf (Config `shepherd.secondsPerSheep`). Die Schere nutzt sich je Schaf einmal ab.
- **Über den Zaun:** Er erreicht Schafe bis 3,5 Blöcke weit, also auch über einen Zaun. Ein Gehege braucht kein Tor; Zauntore öffnen Dorfbewohner ohnehin nicht. Ein Schaf, an das er nicht herankommt, lässt er eine halbe Minute in Ruhe.
- **Züchten:** Sind weniger Schafe (Lämmer mitgezählt) da als die **Herdengröße** am Webstuhl, füttert er zwei ausgewachsene Schafe mit je einem Weizen. Sie verlieben sich wie nach dem Füttern durch einen Spieler und bekommen ein Lamm. Ein neues Paar beginnt er nur, solange die kommenden Lämmer noch in die Herde passen; ein einzelnes verliebtes Schaf bekommt immer einen Partner.
- **Herdengröße:** Reiter **Einstellungen** der Übersicht, mit − und +, 0 (nicht züchten) bis 32, Standard 8. Darunter steht, wie viele Schafe gerade am Webstuhl sind. Gespeichert wird sie an der Station (`herd`).
- **Versorgung:** Lohn und eine **Schere** (bis zu 2). Weizen (bis zu 16, Config `shepherd.keepWheat`) braucht er nur zum Züchten; ohne Weizen schert er weiter. Scheren gelten wie Holz- und Steinwerkzeug als billig: Er benutzt sie, bis sie zerbrechen, statt sie zurückzugeben (siehe [Werkzeugrückgabe](../dorf/werkzeugrueckgabe.md)). Beides holt er sich auch aus einer [Dorftruhe](../dorf/dorftruhen.md).
- **Abliefern:** alles außer Schere, Weizen und Lohn in eine Zielkiste mit Platz. Ohne Zielkiste fängt er nicht an.
- **Status im Menü:** „Sucht Schafe“, „Schert ein Schaf“, „Füttert ein Schaf mit Weizen“, „Wartet, bis die Wolle nachwächst“, „Wartet: keine Schafe am Webstuhl“, „Wartet: kein Weizen zum Züchten“, „Wartet: braucht eine Schere“, dazu „Wolle heute: x“.
- **Kleidung:** ein Kittel aus ungefärbter Wolle mit Ledergürtel, moosgrüne Hose und eine moosgrüne Filzkappe mit Wollrand (`tools/generate_textures.py`).
- **Tests:** Zwei Schafe in einem Gehege ohne Tor werden über den Zaun geschoren, die weiße Wolle landet in der Kiste. Zwei geschorene Schafe bekommen nach dem Füttern ein Lamm.
- **Nicht umgesetzt:** Wolle nach Farbe sortieren (das machen Trichter, Redstone oder ein [Träger](traeger.md)), Farbstoffe, Teppiche und Banner am Webstuhl, Master-Fähigkeit.

## Konzept (noch nicht umgesetzt)

| Quellkiste | Zielkiste |
| --- | --- |
| Schere, Weizen, Farbstoffe | Wolle (nach Farbe), optional Teppiche/Banner am Webstuhl |

## Master-Fähigkeit

Färbt Schafe gezielt und produziert Banner nach Vorlage.

## Ideen

Wolle als Material für Betten: mehr Betten → mehr [Einwohner und Steuern](../wirtschaft/einnahmen.md#steuern-miete--wohlstand).

## Siehe auch

[Metzger](metzger.md) · [Gerber](gerber.md) · [Farmer](farmer.md)
