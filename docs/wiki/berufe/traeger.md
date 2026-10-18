# Beruf: Träger

[← Berufe](README.md) · Station: **keine – der Weidenkorb ist die Workstation** · Phase: **umgesetzt** (ohne Filter und Level)

Der Träger bewegt Items zwischen Kisten. Er hat keinen Arbeitsblock: Sein Weidenkorb auf dem Rücken ist Workstation und Arbeitsvertrag zugleich.

## Anstellen

- Weidenkorb craften (Zuckerrohr um einen Stock als Henkel).
- Einen arbeitslosen, erwachsenen Dorfbewohner mit dem Korb in der Hand antippen: Er nimmt den Korb und wird Träger. Ein Arbeitsvertrag ist nicht nötig.
- Nitwits können vorerst keine Träger werden.
- **Lohn:** Wie alle Arbeiter nimmt er sich vor der Arbeit seinen Tageslohn (Standard 1 Emerald) aus den Emeralds, die er bei sich trägt. Man gibt sie ihm per Rechtsklick oder wirft sie ihm zu. Ohne Emerald wartet er mit „Wartet auf Lohn“ an der Quelle seines ersten Wegs (siehe [Anheuern & Lohn](../wirtschaft/anheuern-und-lohn.md#stand-der-umsetzung)).
- Zurücknehmen: Schleichen + Rechtsklick mit leerer Hand. Der Korb kommt mit Wegen und Inhalt zurück, dazu alles, was er sonst bei sich trägt (übrige Emeralds). Der Dorfbewohner ist wieder arbeitslos und nicht mehr unter Vertrag. Stirbt der Träger, fällt der Korb samt Inhalt zu Boden, ebenso seine Emeralds.

## Transportwege

- Bis zu **3 Wege**, jeder aus Quellkiste und Zielkiste.
- Quelle und Ziel liegen höchstens **32 Blöcke** auseinander.
- Die Quellen eines Trägers liegen höchstens **40 Blöcke** voneinander entfernt.
- Anlegen im Träger-Menü: **„+ Neuer Weg“** im ersten freien Platz anklicken. Das Menü schließt sich, dann mit Rechtsklick erst die Quelle, dann das Ziel anklicken. Nochmal auf die Quelle bricht ab, nach zwei Minuten verfällt die Auswahl. Einen Weg, den es schon gibt, lehnt das Spiel ab.
- Jeder Weg kann einen **Namen** haben (bis 24 Zeichen), sonst heißt er „Weg 1“ usw.
- Die Wege lassen sich auch vorab auf dem Korb anlegen (Korb in der Hand, Rechtsklick in die Luft öffnet das Menü). Sie wandern mit dem Korb zum Träger.

### Träger-Menü

Rechtsklick mit leerer Hand auf den Träger öffnet sein Menü:

- Die drei Wegplätze mit Name, Von- und Nach-Behälter (deren [Namen](../grundlagen/steuerung.md#kisten-benennen)). Der Tooltip zeigt die Länge. Der erste freie Platz ist der Knopf „+ Neuer Weg“.
- **✎** benennt einen Weg um (Enter speichert, Esc bricht ab).
- Der Weg, für den er gerade Items im Korb hat, ist grün hinterlegt.
- **×** löscht einen Weg gezielt. Hat er gerade Items dieses Wegs im Korb, bringt er sie zum Ziel des ersten Wegs.
- Wer Wege ändern darf (anlegen, umbenennen, löschen), stellt der Arbeitgeber oben rechts ein: **Nur Arbeitgeber** oder **Alle dürfen**. Operatoren und der Besitzer einer Einzelspielerwelt dürfen immer.
- Darunter der Korbinhalt: Items lassen sich herausnehmen, aber nicht hineinlegen.
- Die Zeile **„Lohn und Werkzeug“** zeigt seine Emeralds und wie viele Tage sie noch reichen.
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

Farm-Zielkiste → [Vorratskammer](../wirtschaft/beduerfnisse.md#vorratskammer), Stämme vom [Holzfäller](holzfaeller.md) → Eingang des [Holzsägers](holzsaeger.md), Barren → Crafter-Kiste, Fertigware → Verkaufskiste.

## In der Mine (Konzept)

Der [Grubenlogistiker](grubenlogistiker.md) trägt neue Depots automatisch in die Wege der Träger seiner Mine ein. Jeder Träger bedient einen Abschnitt (Depot n → Depot n−1) wie eine Eimerkette, bis eine Schienenstrecke den Abschnitt übernimmt.

## Master-Fähigkeit (Konzept)

Nutzt ein Lasttier (Esel/Lama) für größere Distanzen oder Chest-Minecart-Strecken.

## Sonderfälle

- Zielkiste voll: Er wartet und versucht es später erneut.
- Lange Wege kosten Zeit. Hopper und Loren sind für kurze, feste Verbindungen oft besser. Der Träger lohnt sich bei wechselnden Zielen.

## Siehe auch

[Logistik](../grundlagen/logistik.md) · [Grubenlogistiker](grubenlogistiker.md) · [Crafter-Bediener](crafter-bediener.md) · [Bibliothekar](bibliothekar.md)
