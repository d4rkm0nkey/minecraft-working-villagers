# Beruf: Schmied

[← Berufe](README.md) · Station: **Schmelzofen** (Vanilla, Panzermacher) / **Schmiedetisch** (Werkzeugschmied) · Phase: **Schmelzen umgesetzt**, Reparieren Konzept

Der Schmied schmilzt Erz zu Barren und soll später Werkzeuge der anderen Angestellten reparieren. Er schließt die Kette Mine → Barren → [Crafter](crafter-bediener.md).

## Stand der Umsetzung

- **Nur Schmelzen am Schmelzofen.** Der Knopf **i** im Menü des Schmelzofens öffnet die Stationsübersicht. Panzermacher aus dem Dorf arbeiten weiter an Schmelzöfen.
- **Kein Auftrag:** Er schmilzt alles aus den Eingangskisten, was der Schmelzofen annimmt (rohe Erze, Erzblöcke, Eisen- und Goldwerkzeug …). Was der Ofen nicht kann, lässt er liegen.
- **Brennstoff** ist alles, was brennt (Kohle, Holzkohle, Kohleblöcke, Lavaeimer …). Er legt ihn aus denselben Eingangskisten nach; den leeren Eimer bringt er mit weg.
- **Mengen:** Pro Gang holt er bis zu 16 Items zum Schmelzen (Config `smith.batchSize`) und bis zu 8 Brennstoff (Config `smith.fuelPerTrip`). Das Schmelzen selbst macht der Ofen in seinem normalen Tempo.
- **Abliefern:** Er nimmt die Ergebnisse aus dem Ofen und bringt sie in eine Ausgangskiste. Die Erfahrung bleibt im Ofen; der Spieler bekommt sie, wenn er selbst etwas herausnimmt.
- Ohne Eingangs- und Ausgangskiste arbeitet er nicht. Werkzeug braucht er nicht.
- **Status im Menü:** „Holt Erz und Brennstoff“, „Befüllt den Schmelzofen“, „Schmilzt“, „Wartet auf etwas zum Schmelzen“, „Wartet auf Brennstoff“, „Wartet: kommt nicht an den Schmelzofen“.
- **Nicht umgesetzt:** Reparieren am Schmiedetisch, Master-Fähigkeit.

## Modi (Konzept)

| Modus | Station | Was passiert |
| --- | --- | --- |
| Schmelzen (umgesetzt) | Schmelzofen | Erz aus der Eingangskiste schmelzen, Brennstoff nachlegen, Barren abliefern |
| Reparieren | Schmiedetisch | Beschädigte Werkzeuge aus einer Reparaturkiste mit Barren reparieren und zurücklegen |

## Kisten

| Eingang | Ausgang |
| --- | --- |
| Rohe Erze, Kohle/Holzkohle, später beschädigte Werkzeuge | Barren, später reparierte Werkzeuge |

## Hinweise

- Reparatur senkt die Werkzeugkosten aller Berufe deutlich, kostet aber Barren.
- Brennstoff kann vom [Holzfäller](holzfaeller.md) (Holzkohle über einen Ofen) oder [Minenarbeiter](minenarbeiter.md) (Kohle) kommen.

## Master-Fähigkeit (Konzept)

Doppelte Ausbeute bei einem Teil der Erze (wie ein kleiner Fortune-Bonus beim Schmelzen) oder kostenlose Reparatur ab und zu.

## Siehe auch

[Logistik](../grundlagen/logistik.md) · [Minenarbeiter](minenarbeiter.md) · [Brenner](brenner.md) (Brennofen für Baustoffe, ohne Erz und Essen)
