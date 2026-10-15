# Beruf: Holzsäger

[← Berufe](README.md) · Station: **Holzsäge** (neu) · Phase: **umgesetzt**

Der Holzsäger verarbeitet Stämme und Bretter an der Holzsäge zu Brettern, Stufen, Treppen, Zäunen, Türen und anderen Holzteilen. Er holt das Holz aus den Eingangskisten und bringt die Teile in die Ausgangskisten.

## Workstation: Holzsäge

- Eigener Block, funktioniert für Spieler wie die Steinsäge: Stamm oder Brett hinein, Ergebnis anklicken. Ohne den Verschnitt der Werkbank, z. B. 1 Stamm → 4 Bretter, 4 Treppen, 8 Stufen oder 8 Stöcke; 1 Brett → 2 Stufen.
- Rezept: 1 Eisenbarren über 3 Brettern (beliebige Holzart), passend zur Steinsäge mit Eisen + Stein.
- Rezepte als eigener Rezepttyp `workingvillagers:woodcutting`, per Datapack erweiterbar (Holz aus anderen Mods). Was hineinpasst, legt der Item-Tag `workingvillagers:woodcuttable` fest: Stämme, Bretter, Bambusblöcke, Bambusmosaik.
- Der Knopf **i** im Menü öffnet die Stationsübersicht mit Arbeiter, Kisten und Auftrag (siehe [Steuerung](../grundlagen/steuerung.md#stationsübersicht)).
- Nur angeheuerte Dorfbewohner arbeiten hier. Vanilla-Dorfbewohner nehmen die Holzsäge nicht als Arbeitsplatz.

## Auftrag

Ein Auftrag ist ein **Erzeugnis** plus die **Holzsorten**, die er verarbeiten darf.

- Erzeugnisse: Bretter, Stufen, Treppen, Zäune, Zauntore, Türen, Falltüren, Druckplatten, Knöpfe, Schilder, Stöcke.
- Holzsorten: alle Vanilla-Hölzer einschließlich Karmesin, Wirr und Bambus, je an/aus, dazu „Alle“ und „Keine“. Gibt es ein Erzeugnis in einer Holzsorte nicht, zeigt der Tooltip das an.
- Der Säger nimmt nur Material der erlaubten Holzsorten, Stämme wie Bretter. Entrindete Stämme zählen wie normale.
- Ohne Auftrag oder ohne Holzsorte arbeitet er nicht.

## Arbeitsablauf

1. Lohn aus den Emeralds nehmen, die er bei sich trägt (siehe [Anheuern & Lohn](../wirtschaft/anheuern-und-lohn.md#stand-der-umsetzung)).
2. Passendes Material aus einer Eingangskiste holen, bis zu 16 Items auf einmal.
3. An der Holzsäge sägen: etwa 4 Items pro Sekunde, mindestens eine Sekunde je Schnitt. Späne fliegen, die Säge ist zu hören.
4. Die Teile in eine Ausgangskiste bringen. Material eines früheren Auftrags gibt er dort mit ab.

Beide Zahlen stehen in der Config (`sawyer.batchSize`, `sawyer.itemsPerSecond`) und gelten auch für den [Steinsäger](steinsaeger.md).

## Kisten

| Eingang | Ausgang |
| --- | --- |
| Stämme, Bretter, Bambusblöcke | Bretter, Stufen, Treppen, Zäune, Türen, Stöcke … |

Er braucht mindestens einen Eingang und einen Ausgang. Ein [Träger](traeger.md) bringt die Stämme vom [Holzfäller](holzfaeller.md) herüber; Stöcke, Bretter und Stufen gehen weiter zum [Tischler](tischler.md).

## Status

„Holt Material“, „Geht zur Säge“, „Sägt Treppen“, „Wartet auf Material“, „Wartet: kommt nicht an die Säge“, dazu die allgemeinen Meldungen (Lohn, volle Kisten, Feierabend).

## Stand der Umsetzung

- Umgesetzt wie oben beschrieben. Werkzeug braucht der Holzsäger nicht.
- Alle Arbeiter sind vorerst Novizen. Level und Master-Fähigkeit fehlen.

## Ideen (Konzept)

- **Master-Fähigkeit:** sägt zwei Erzeugnisse im Wechsel, z. B. Treppen und Stufen, ohne Umstellung.
- **Stückzahl:** „64 Zäune, dann fertig“, wie beim [Tischler](tischler.md#aufträge) geplant.

## Siehe auch

[Holzfäller](holzfaeller.md) · [Steinsäger](steinsaeger.md) · [Tischler](tischler.md) · [Logistik](../grundlagen/logistik.md)
