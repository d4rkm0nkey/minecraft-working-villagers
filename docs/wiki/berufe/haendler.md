# Beruf: Händler

[← Berufe](README.md) · Station: **Handelskontor** (neu) oder Barrel mit Schild · Phase: **M4**

Der Händler bringt Waren per Karawane in Nachbardörfer und verkauft sie dort nach deren Nachfrage. Er ist das Endgame der [Wirtschaft](../wirtschaft/einnahmen.md).

## Handelsrouten

- Im [Dorfbuch](../dorf/dorfbuch.md) (Reiter Kasse): Ziel-Dorf, Ware, Menge, Mindestpreis.
- Nur Dörfer, die der Spieler oder ein [Kartograf](kartograf.md) entdeckt hat.
- Rückfracht: Der Händler kann Waren einkaufen, die das eigene Dorf nicht produziert.

## Nachfrage

- Jedes Dorf hat Nachfrage nach Biom und Berufen. Beispiele: Wüstendorf will Holz und Kohle, Schneedorf will Nahrung, Ebenendorf will Erz und Werkzeug.
- Verkauf senkt den Preis im Zieldorf; die Nachfrage erholt sich über einige Tage. Dadurch lohnt sich die Verteilung auf mehrere Routen.

## Reise

- Die Karawane (Händler + Lamas oder Esel) startet sichtbar.
- Sobald sie geladene Chunks verlässt, wird die Reise abstrahiert: Reisezeit nach Entfernung, Ankunft mit Erlös. Keine Chunk-Ladung der Strecke nötig.
- Risiko: Pillager-Überfall mit Wahrscheinlichkeit je nach Entfernung und Begleitschutz ([Wache](wache.md), Golem). Ereignis landet im [Register](../dorf/dorfbuch.md#register).

## Aufträge

Erfüllt Bestellungen vom Auftragsbrett (siehe [Aufträge](../wirtschaft/einnahmen.md#aufträge-schedule-1-stil)).

## Master-Fähigkeit

Zwei Routen pro Reise oder deutlich geringeres Überfallrisiko.

## Siehe auch

[Handelsangestellter](handelsangestellter.md) · [Multiplayer: Spielerhandel](../technik/multiplayer.md)
