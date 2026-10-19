# Beruf: Steinsäger

[← Berufe](README.md) · Station: **Steinsäge** (Vanilla) · Phase: **umgesetzt**

Der Steinsäger verarbeitet Stein an der Vanilla-Steinsäge zu Ziegeln, Stufen, Treppen, Mauern und anderen Steinteilen. Er ist das Gegenstück zum [Holzsäger](holzsaeger.md) und die umgesetzte Vorstufe des [Maurers](maurer.md).

## Workstation: Steinsäge

- Die normale Vanilla-Steinsäge, kein neuer Block. Für Spieler funktioniert sie wie immer; ihr Menü zeigt zusätzlich den Auftrag, und die Reiter über dem Menü führen in die Stationsübersicht. Solange niemand sie eingerichtet hat, stehen dort nur die Reiter Block und Arbeiter.
- Steinmetze aus dem Dorf arbeiten weiter an Steinsägen. Arbeitet dort schon einer, zeigt die Station „besetzt“.
- Ein angeheuerter Dorfbewohner, der hier anfängt, wird Steinsäger, nicht Steinmetz.

## Auftrag

- Ein Auftrag ist **ein bestimmtes Erzeugnis**, z. B. Steinziegeltreppen.
- In der Übersicht zeigt die Auftragsspalte erst die **Materialien** (Steinfamilien). Ein Klick öffnet eine Familie mit allen ihren Erzeugnissen, ein Klick auf ein Erzeugnis macht es zum Auftrag, ein zweiter Klick löscht ihn.
- Eine Familie ist alles, was über Steinsägen-Rezepte zusammenhängt, z. B. Tiefenschiefer mit seinen Ziegeln, Fliesen, Stufen, Treppen und Mauern. Die Familien kommen aus den Rezepten, also auch aus Datapacks.
- Als Material nimmt er alles, woraus die Steinsäge das Erzeugnis machen kann (für Steinziegeltreppen also Stein oder Steinziegel).

## Arbeitsablauf

Wie beim [Holzsäger](holzsaeger.md#arbeitsablauf): Lohn nehmen, bis zu 16 Items Material aus einer Eingangskiste holen, an der Steinsäge etwa 4 Items pro Sekunde machen (mindestens eine Sekunde je Schnitt), Ergebnis in eine Ausgangskiste bringen. Config: `sawyer.batchSize`, `sawyer.itemsPerSecond`.

## Kisten

| Eingang | Ausgang |
| --- | --- |
| Stein, Bruchstein, Tiefenschiefer, Sandstein … | Ziegel, Stufen, Treppen, Mauern … |

## Stand der Umsetzung

- Umgesetzt wie oben beschrieben, ohne Werkzeug.
- Stein im Ofen zu Glattstein brennen kann er nicht. Das übernimmt der [Brenner](brenner.md).
- Der [Maurer](maurer.md) bleibt als Konzept für später (z. B. mit Master-Fähigkeit).

## Siehe auch

[Holzsäger](holzsaeger.md) · [Maurer](maurer.md) · [Minenarbeiter](minenarbeiter.md) · [Logistik](../grundlagen/logistik.md)
