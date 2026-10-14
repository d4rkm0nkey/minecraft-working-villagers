# Anheuern & Lohn

[← Wiki-Start](../README.md)

Lohn ist die zentrale Bremse: Ein Angestellter muss mehr Wert erzeugen, als er kostet, aber nicht so viel, dass Handarbeit sinnlos wird. Alle Werte sind Startwerte für Playtests und per Config änderbar.

## Wer kann angeheuert werden?

- Nur Dorfbewohner eines [gekauften Dorfes](../dorf/dorf-und-kauf.md).
- Arbeitslose Dorfbewohner können angeheuert werden. Nitwits nur als [Träger](../berufe/traeger.md) (einfachste Arbeit, halber Lohn) – endlich ein Nutzen für sie.
- Berufstätige Villager mit Handelsangeboten können nicht angeheuert werden, damit Handel nicht kaputtgeht. Alternative: Anheuern löscht die Trades (Config).
- Das Level eines angeheuerten Villagers steigt durch Arbeit statt durch Handel (Novice → Master).

## Lohnstufen

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

## Moral statt Strafe

- Moral 0–100. Steigt durch pünktlichen Lohn, eigenes Bett, Nachtruhe, Glocke in Reichweite, erfüllte [Bedürfnisse](beduerfnisse.md).
- Sinkt durch ausgefallenen Lohn, Verletzungen, fehlendes Werkzeug.
- Unter 30: langsamer. Unter 10 oder 2 Wochen ohne Lohn: Kündigung, Villager wird wieder arbeitslos.

## Kosten neben dem Lohn

- Werkzeug nutzt sich ab und muss in der Quellkiste nachgefüllt werden.
- Verbrauchsmaterial: Setzlinge, Fackeln, Stützblöcke.
- Unterhalt für geladene Chunks (siehe [Chunk-Loading](../dorf/chunk-loading.md)).
- Nahrung und Unterkunft sind keine Pflichtkosten, sondern senken den Lohn (siehe [Bedürfnisse](beduerfnisse.md)).

Wie das Geld hereinkommt: [Einnahmen](einnahmen.md).
