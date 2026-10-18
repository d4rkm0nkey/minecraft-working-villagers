# Anheuern & Lohn

[← Wiki-Start](../README.md)

Lohn ist die zentrale Bremse: Ein Angestellter muss mehr Wert erzeugen, als er kostet, aber nicht so viel, dass Handarbeit sinnlos wird. Alle Werte sind Startwerte für Playtests und per Config änderbar.

## Wer kann angeheuert werden?

Konzept (Stand der Umsetzung siehe [unten](#stand-der-umsetzung)):

- Nur Dorfbewohner eines [gekauften Dorfes](../dorf/dorf-und-kauf.md).
- Arbeitslose Dorfbewohner können angeheuert werden. Idee: Nitwits nur als [Träger](../berufe/traeger.md) (einfachste Arbeit, halber Lohn) – endlich ein Nutzen für sie. Noch nicht umgesetzt, Nitwits lassen sich bisher gar nicht anheuern.
- Berufstätige Villager mit Handelsangeboten können nicht angeheuert werden, damit Handel nicht kaputtgeht. Alternative: Anheuern löscht die Trades (Config).
- Das Level eines angeheuerten Villagers steigt durch Arbeit statt durch Handel (Novice → Master).

## Lohnstufen

Planwerte; im Spiel gibt es noch keine Lohnstufen. Derzeit zahlt man jedem Arbeiter **1 Emerald pro Arbeitstag** (Config `workers.dailyWage`, 0–64), siehe [Stand der Umsetzung](#stand-der-umsetzung).

| Level | Lohn (Emerald/Woche) | Effekt |
| --- | --- | --- |
| Novice | 16 | Grundtempo, kleiner Bereich (z. B. 9×9) |
| Apprentice | 24 | +15 % Tempo |
| Journeyman | 32 | Größerer Bereich (z. B. 15×15) |
| Expert | 48 | Werkzeug hält länger |
| Master | 64 | Zusatzfähigkeit je Beruf (siehe [Berufe](../berufe/README.md)) |

Die ursprünglich genannten 100 Emeralds pro Woche sind für Early Game sehr viel. Vorschlag: mit 16–64 Emeralds starten und nach Playtests justieren. Erfüllte [Bedürfnisse](beduerfnisse.md) senken den Lohn um bis zu 50 %.

## Dorfkasse & Bezahlung

- Alle Löhne kommen aus der [Dorfkasse](../dorf/dorf-und-kauf.md#dorfkasse) (Barrel neben der Glocke). Am Zahltag nimmt sich der Villager seinen Lohn selbst.
- Alternativ zahlt der Spieler per Rechtsklick mit Emeralds im Voraus (Guthaben in Wochen).

### Stand der Umsetzung

**Anheuern:** Den Arbeitsvertrag craftet man formlos aus Papier, Tintenbeutel und 3 Emeralds. Rechtsklick damit auf einen arbeitslosen, erwachsenen Dorfbewohner stellt ihn ein, in jedem Dorf und auch außerhalb; einen Dorfkauf gibt es noch nicht. Berufstätige Dorfbewohner und Nitwits lassen sich nicht anheuern. Eine Anheuergebühr gibt es nicht, der Vertrag ist der Preis. Den [Träger](../berufe/traeger.md) stellt man statt mit Vertrag mit dem Weidenkorb ein. Geplant: [ein Vertrag je Beruf](arbeitsvertraege.md), nur in Truhen von Strukturen zu finden statt craftbar.

**Arbeitszeit:** die normale Vanilla-Arbeitszeit der Dorfbewohner; nachts schlafen sie.

**Lohn:** Eine Dorfkasse gibt es noch nicht. Jede Station hat eine **Lohnkasse** für bis zu 64 Emeralds (Wunsch vom 18.10.2026): Reiter **Lohn** der Stationsübersicht mit einem Slot und dem Spielerinventar wie ein Container; außerdem legt ein Rechtsklick mit Emeralds auf die Station sie hinein, und Emeralds, die man dem Arbeiter gibt oder zuwirft, legt er ebenfalls hinein. Aus Dorftruhen geholter Lohn (für 3 Tage) landet auch dort. Vor der Arbeit nimmt sich der Arbeiter den Tageslohn (Standard 1 Emerald) aus der Kasse, danach aus der Tasche (Emeralds aus älteren Versionen). Ohne Emerald wartet er an seiner Station.

- Die Emeralds gehören der Station, nicht dem Arbeiter: Stirbt er, wird er zum Zombie oder wechselt er die Station, geht nichts verloren; der nächste Arbeiter dort wird aus derselben Kasse bezahlt. Wird die Station abgebaut, gesprengt oder ersetzt, fallen die Emeralds heraus wie aus einer Truhe (über die POI-Aktualisierung von Vanilla, `ServerLevelMixin`). Gespeichert in den Stationsdaten (`wage`), nicht im Block.
- Der Träger hat keine Station und trägt seine Emeralds weiter selbst (bis zu 64).
- Werkzeug und Saatgut trägt der Arbeiter weiter selbst, der Spieler gibt sie direkt, wie Futter: Rechtsklick auf den Arbeiter oder die Items vor ihm fallen lassen.
- Holzfäller: bis zu 2 Äxte und 16 Setzlinge je Sorte. Farmer: bis zu 2 Hacken und 16 Saatgut je Sorte zum Start, danach pflanzt er aus der Ernte nach.
- Hat er genug, schüttelt er den Kopf. Zugeworfene Items nimmt er nur bis zu denselben Höchstmengen auf. Stirbt er, lässt er alles fallen.
- Der Lohn ist für alle gleich (Config `workers.dailyWage`, 0 = umsonst). Lohnstufen, Level, Moral und Kündigung gibt es noch nicht; alle Arbeiter sind Novizen.
- Kann er nicht arbeiten, schüttelt er ab und zu den Kopf mit dem „Nein“-Sound, wenn ein Spieler in der Nähe ist.

Ergänzend gibt es [Dorftruhen](../dorf/dorftruhen.md): Jeder Arbeiter in der Nähe holt sich daraus selbst, was ihm ausgeht (Lohn für 3 Tage, Ersatzwerkzeug, Setzlinge, Saatgut, Leitern, Fackeln).

## Moral statt Strafe

- Moral 0–100. Steigt durch pünktlichen Lohn, eigenes Bett, Nachtruhe, Glocke in Reichweite, erfüllte [Bedürfnisse](beduerfnisse.md).
- Sinkt durch ausgefallenen Lohn, Verletzungen, fehlendes Werkzeug.
- Unter 30: langsamer. Unter 10 oder 2 Wochen ohne Lohn: Kündigung, Villager wird wieder arbeitslos.

## Kosten neben dem Lohn

- Werkzeug nutzt sich ab und muss nachgegeben werden (derzeit direkt an den Arbeiter, siehe [Stand der Umsetzung](#stand-der-umsetzung)).
- Verbrauchsmaterial: Setzlinge, Fackeln, Stützblöcke.
- Unterhalt für geladene Chunks (siehe [Chunk-Loading](../dorf/chunk-loading.md)).
- Nahrung und Unterkunft sind keine Pflichtkosten, sondern senken den Lohn (siehe [Bedürfnisse](beduerfnisse.md)).

Wie das Geld hereinkommt: [Einnahmen](einnahmen.md).
