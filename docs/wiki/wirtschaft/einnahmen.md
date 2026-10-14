# Einnahmen: Wie sich ein Dorf finanziert

[← Wiki-Start](../README.md)

Ein Dorf mit 20 Arbeitern kostet schnell 500+ Emeralds pro Woche. Handarbeit am Villager-Tresen skaliert dafür nicht. Die Einnahmen müssen deshalb selbst automatisierbar sein, aber jede Quelle braucht eine natürliche Grenze, sonst wird das Dorf zur Gelddruckmaschine.

## Übersicht der Ideen

| Idee | Mechanik | Skaliert über | Natürliche Bremse | Phase |
| --- | --- | --- | --- | --- |
| [Handelsangestellter](../berufe/handelsangestellter.md) | Nutzt die Vanilla-Trades der Dorfbewohner automatisch aus einer Verkaufskiste | Anzahl und Level der Villager im Dorf | Restock-Limits der Vanilla-Trades | früh (M2) |
| Steuern & Miete | Einwohner zahlen wöchentlich, Höhe nach Wohlstandsstufe | Einwohnerzahl × Wohlstand | Zufriedenheit; zu hohe Steuern → Wegzug | M2 |
| [Händler](../berufe/haendler.md) mit Nachbardörfern | Karawane bringt Waren in bekannte Dörfer, verkauft nach deren Nachfrage | Anzahl Routen und Dörfer | Sättigung der Nachfrage, Reisezeit, Überfälle | M4 |
| Aufträge | Nachbardörfer stellen Bestellungen mit Frist und Bonus | Ruf des Dorfes | Begrenzte Anzahl Aufträge pro Woche | M4 |
| Marktplatz | Verkäufer bedient Besucher aus anderen Dörfern und wandernde Händler | Wohlstand, Lage, Angebot | Besucherzahl pro Tag | später |
| Großhändler | Wandernder Händler kommt regelmäßig und kauft große Mengen zum Festpreis | Kontor-Ausbau | Fester Termin, niedriger Preis | M4 |
| Gasthaus | Reisende übernachten gegen Gebühr | Betten im Gasthaus, Wohlstand | Reisende pro Nacht | später |
| Smaragd-Abbau | [Minenarbeiter](../berufe/minenarbeiter.md) baut Smaragderz in Bergbiomen ab | Anzahl Minen | Seltenheit, nur Bergbiome | M3 |

## Handelsangestellter (Einstieg)

Eigene Seite: [Handelsangestellter](../berufe/handelsangestellter.md). Kurz: Er handelt automatisch über die Vanilla-Trades der Dorfbewohner und ist durch Restocks gedeckelt.

## Steuern, Miete & Wohlstand

Das Dorf hat eine Wohlstandsstufe. Je besser es ausgebaut ist, desto mehr Einwohner ziehen zu und desto mehr Steuern zahlen sie. So lohnt es sich, das Dorf zu verbessern, statt nur Fabriken zu bauen.

| Stufe | Voraussetzungen (Beispiele) | Steuer pro Einwohner und Woche |
| --- | --- | --- |
| Weiler | Glocke, 3 Betten | 0 |
| Dorf | 10 Betten, Beleuchtung, 1 Iron Golem | 1 Emerald |
| Marktflecken | 20 Betten, Marktplatz, Wege, Nahrungsversorgung | 2 Emeralds |
| Stadt | 40 Betten, Mauer, Rathaus (Lectern), Gasthaus | 4 Emeralds |

- Wohlstand wird aus messbaren Dingen berechnet: Betten, Lichtlevel im Dorfradius, Golems, Arbeitsquote, Nahrung im Dorf ([Bedürfnisse](beduerfnisse.md)), Deko-Blöcke (Blumen, Laternen), keine Raids in letzter Zeit.
- Steuersatz im [Dorfbuch](../dorf/dorfbuch.md) einstellbar (niedrig/normal/hoch). Hohe Steuern senken die Zufriedenheit, Einwohner ziehen weg.
- Angestellte zahlen Miete für ihr Bett, ein Teil des Lohns fließt also zurück („Werkssiedlung“).
- Zuzug: Ab der Stufe „Dorf“ erscheinen regelmäßig neue arbeitslose Villager, wenn freie Betten da sind. Das löst nebenbei das Problem, woher neue Arbeiter kommen.

## Händler mit Nachbardörfern

Eigene Seite: [Händler](../berufe/haendler.md).

## Aufträge (Schedule-1-Stil)

- Ein **Auftragsbrett** (z. B. Lectern mit Buch oder neues Schwarzes Brett) zeigt Bestellungen aus Nachbardörfern: „64 Eichenstämme bis Tag 60 – 40 Emeralds“.
- Pünktlich erfüllte Aufträge steigern den Ruf, größere und bessere Aufträge folgen. Verpasste Fristen kosten Ruf.
- Gibt dem Spieler Ziele, was er als Nächstes automatisieren soll.
- Ausgeliefert wird durch den [Händler](../berufe/haendler.md).

## Weitere Ideen

- **Großhändler:** Erweiterter Wandering Trader, der mit Kontor jede Woche fest kommt und Großmengen zu niedrigem, aber garantiertem Preis kauft.
- **Marktplatz:** Villager aus Nachbardörfern besuchen an Markttagen das Dorf. Ein Verkäufer bedient sie aus der Verkaufskiste.
- **Gasthaus:** Reisende zahlen für ein Bett. Mehr Wohlstand zieht mehr Reisende an.
- **Spielerhandel (Multiplayer):** Handelsrouten zwischen Spielerdörfern, Preise frei verhandelt (siehe [Multiplayer](../technik/multiplayer.md)).
- **Kredit:** Startkapital beim Kauf des Dorfes gegen Rückzahlung mit Zinsen aus der Dorfkasse.
- **Veredelung lohnt sich:** Verarbeitete Waren (Bretter → Kisten, Erz → Werkzeug) bringen deutlich mehr als Rohstoffe. Das belohnt längere [Produktionsketten](../grundlagen/logistik.md).

## Empfohlene Reihenfolge

1. [Handelsangestellter](../berufe/handelsangestellter.md): einfach, Vanilla-nah, sofort nützlich.
2. Steuern und Wohlstand: belohnen den Ausbau des Dorfes und liefern neue Arbeiter.
3. [Händler](../berufe/haendler.md) mit Nachbardörfern und Aufträge: das eigentliche Endgame der Wirtschaft.

Ziel fürs Balancing: Ein gut geführtes Dorf trägt sich selbst und erwirtschaftet etwa 20–30 % Überschuss.
