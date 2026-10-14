# Beruf: Träger

[← Berufe](README.md) · Station: **keine – der Weidenkorb ist die Workstation** · Phase: **M4** (Demo umgesetzt)

Der Träger bewegt Items zwischen Kisten. Er hat keinen Arbeitsblock: Sein Weidenkorb auf dem Rücken ist Workstation und Arbeitsvertrag zugleich.

## Anstellen

- Weidenkorb craften (Zuckerrohr um einen Stock als Henkel).
- Einen arbeitslosen, erwachsenen Dorfbewohner mit dem Korb in der Hand antippen: Er nimmt den Korb und wird Träger. Ein Arbeitsvertrag ist nicht nötig.
- Nitwits können vorerst keine Träger werden.
- Zurücknehmen: Schleichen + Rechtsklick mit leerer Hand. Der Korb kommt mit Wegen und Inhalt zurück, der Dorfbewohner ist wieder arbeitslos. Stirbt der Träger, fällt der Korb samt Inhalt zu Boden.

## Transportwege

- Bis zu **3 Wege**, jeder aus Quellkiste und Zielkiste.
- Quelle und Ziel liegen höchstens **32 Blöcke** auseinander.
- Die Quellen eines Trägers liegen höchstens **40 Blöcke** voneinander entfernt.
- Anlegen im Träger-Menü: **„+ Neuer Weg“** im ersten freien Platz anklicken. Das Menü schließt sich, dann mit Rechtsklick erst die Quelle, dann das Ziel anklicken. Nochmal auf die Quelle bricht ab, nach zwei Minuten verfällt die Auswahl. Einen Weg, den es schon gibt, lehnt das Spiel ab.
- Jeder Weg kann einen **Namen** haben (bis 24 Zeichen), sonst heißt er „Weg 1“ usw.
- Die Wege lassen sich auch vorab auf dem Korb anlegen (Korb in der Hand, Rechtsklick in die Luft öffnet das Menü). Sie wandern mit dem Korb zum Träger.

### Träger-Menü

Rechtsklick mit leerer Hand auf den Träger öffnet sein Menü:

- Die drei Wegplätze mit Name, Von- und Nach-Koordinaten. Der Tooltip zeigt Behälterart und Länge. Der erste freie Platz ist der Knopf „+ Neuer Weg“.
- **✎** benennt einen Weg um (Enter speichert, Esc bricht ab).
- Der Weg, für den er gerade Items im Korb hat, ist grün hinterlegt.
- **×** löscht einen Weg gezielt. Hat er gerade Items dieses Wegs im Korb, bringt er sie zum Ziel des ersten Wegs.
- Wer Wege ändern darf (anlegen, umbenennen, löschen), stellt der Arbeitgeber oben rechts ein: **Nur Arbeitgeber** oder **Alle dürfen**. Operatoren und der Besitzer einer Einzelspielerwelt dürfen immer.
- Darunter der Korbinhalt: Items lassen sich herausnehmen, aber nicht hineinlegen.
- Rechtsklick mit dem Korb in die Luft öffnet dasselbe Menü für die Wege, die auf dem Korb gespeichert sind.

## Arbeitsablauf

1. Wege reihum prüfen: Hat die Quelle Items und das Ziel Platz?
2. An der Quelle den Korb füllen: höchstens so viele Stacks wie in den Korb passen und wie im Ziel Platz ist.
3. Zur Zielkiste tragen und abladen. Was nicht passt, bringt er zur Quelle zurück.
4. Nichts zu tun: In der Nähe der ersten Quelle herumschlendern, ohne Kisten offen zu halten, und regelmäßig neu prüfen.

Er öffnet Kisten wie alle Arbeiter von vorne, mit sichtbarem Deckel.

## Kapazität

- Novize: **6 Stacks** im Korb.
- Später soll die Kapazität mit dem Level wachsen. In der Demo sind alle Träger Novizen.

## Filter

Noch nicht umgesetzt: Der Träger nimmt alles aus der Quelle mit. Spätere Idee: Filter pro Weg, etwa per Item-Frame an der Kiste (Vanilla-Stil).

## Typische Wege

Farm-Zielkiste → [Vorratskammer](../wirtschaft/beduerfnisse.md#vorratskammer), Stämme → Holzsäge-Quelltruhe, Barren → Crafter-Kiste, Fertigware → Verkaufskiste.

## In der Mine

Der [Grubenlogistiker](grubenlogistiker.md) trägt neue Depots automatisch in die Wege der Träger seiner Mine ein. Jeder Träger bedient einen Abschnitt (Depot n → Depot n−1) wie eine Eimerkette, bis eine Schienenstrecke den Abschnitt übernimmt.

## Master-Fähigkeit

Nutzt ein Lasttier (Esel/Lama) für größere Distanzen oder Chest-Minecart-Strecken.

## Sonderfälle

- Zielkiste voll: Er wartet und versucht es später erneut.
- Lange Wege kosten Zeit. Hopper und Loren sind für kurze, feste Verbindungen oft besser. Der Träger lohnt sich bei wechselnden Zielen.

## Siehe auch

[Logistik](../grundlagen/logistik.md) · [Grubenlogistiker](grubenlogistiker.md) · [Crafter-Bediener](crafter-bediener.md) · [Bibliothekar](bibliothekar.md)
