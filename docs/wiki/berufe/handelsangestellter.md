# Beruf: Handelsangestellter

[← Berufe](README.md) · Station: **Handelstresen** (neu) · Phase: **umgesetzt** (nur Verkauf, ohne Dorfkasse und Master-Fähigkeit)

Der Handelsangestellte verkauft Waren aus der Verkaufskiste automatisch über die Vanilla-Trades der Dorfbewohner. Er ist die einfachste [Einnahmequelle](../wirtschaft/einnahmen.md) und die Brücke vom Handel per Hand zur Automatisierung.

## Stand der Umsetzung

- **Handelstresen:** neuer Block (Theke mit Smaragden und Kontobuch), Rezept 3 Holzstufen über Brett, Smaragd, Brett und 3 Brettern. Rechtsklick öffnet die Stationsübersicht mit Eingabe- und Ausgabekisten. Entschieden: eigener Block statt Lectern, weil der Lectern dem Bibliothekar gehört und normale Dorfbewohner ihn sonst übernehmen.
- **Kunden:** normale (nicht angeheuerte) erwachsene Dorfbewohner mit Beruf im Umkreis von `shopAssistant.tradeRadius` (24) Blöcken um den Tresen; keine Nitwits, keine Kinder, keiner, der gerade schläft oder mit einem Spieler handelt.
- **Trades:** nur Angebote, bei denen der Dorfbewohner etwas **gegen Smaragde ankauft** (ohne zweite Zutat) und die nicht ausverkauft sind, zum aktuellen Preis inklusive Nachfrage. Jeder Handel läuft über `notifyTrade` wie beim Spieler: Die Nutzungen steigen, der Dorfbewohner bekommt Erfahrung, steigt im Level auf, lässt Erfahrungskugeln fallen und muss an seiner Workstation nachfüllen (Restock). Damit ist der Handel natürlich gedeckelt.
- **Ablauf:** nächster Kunde mit passendem Angebot → so viel Ware holen, wie seine Angebote nehmen (höchstens 64) → hingehen, alle 2 Sekunden ein Handel (`shopAssistant.secondsPerTrade`) → Smaragde in die Ausgabekiste (spätestens ab 32), Unverkauftes zurück in die Eingabekiste.
- Verdiente Smaragde zählt er getrennt vom Lohn (nicht gespeichert: nach einem Neustart unterwegs gelten sie als Lohn). Die Übersicht zeigt „Heute verdient: x Smaragde“.
- **Status:** „Holt Ware“, „Geht zu …“, „Handelt mit …“, „Bringt unverkaufte Ware zurück“, „Wartet auf Ware zum Verkaufen“, „Wartet: niemand in der Nähe kauft diese Ware“, „Wartet: die Käufer sind ausverkauft, bis sie nachfüllen“.
- Profession `workingvillagers:shop_assistant` mit dunkelgrüner Schürze und schwarzer Mütze.
- **Nicht umgesetzt:** Einkaufen, Dorfkasse (die Smaragde gehen in die Ausgabekisten), Master-Fähigkeit.
- **Nebenbei:** Normale Dorfbewohner übernehmen keine eingerichteten Vanilla-Stationen (Steinsäge, Komposter, Schmelzofen mit Kisten, Auftrag oder Arbeiter) mehr, und „Zuweisen“ verdrängt dort einen normalen Dorfbewohner, der noch nie gehandelt hat.

## Arbeitsablauf

1. Verkaufskiste prüfen.
2. Dorfbewohner mit passenden Trades aufsuchen (z. B. Farmer kauft Weizen, Fletcher kauft Stöcke).
3. Im Auftrag des Spielers handeln; Emeralds landen in der [Dorfkasse](../dorf/dorf-und-kauf.md#dorfkasse).
4. Warten, bis die Trades wieder aufgefüllt sind (Restock).

## Quellkiste / Zielkiste

| Quellkiste | Zielkiste |
| --- | --- |
| Verkaufskiste | Dorfkasse (Emeralds), optional Kiste für eingekaufte Waren |

## Regeln

- Nutzt die echten Vanilla-Trades inklusive Preisänderungen durch Nachfrage und Hero of the Village.
- Skaliert automatisch mit Anzahl und Level der Villager, ist aber durch Restocks gedeckelt.
- Bonus: Er levelt die Dorfbewohner beim Handeln, das Dorf wird mit der Zeit wertvoller.

## Master-Fähigkeit

Bessere Preise (wie ein dauerhafter kleiner Hero-of-the-Village-Rabatt).

## Offene Fragen

- Darf er auch einkaufen (z. B. Brot für die [Vorratskammer](../wirtschaft/beduerfnisse.md))?
- Eigener Block oder Lectern? Der Lectern ist bereits POI des Bibliothekars.

## Siehe auch

[Händler](haendler.md) · [Einnahmen](../wirtschaft/einnahmen.md)
