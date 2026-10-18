# Beruf: Schmied

[← Berufe](README.md) · Station: **Schmelzofen** (Vanilla, Panzermacher) / **Schmiedetisch** (Werkzeugschmied) · Phase: **Schmelzen und Reparieren umgesetzt**

Der Schmied schmilzt Erz zu Barren und repariert die Werkzeuge der anderen Angestellten. Er schließt die Kette Mine → Barren → [Crafter](crafter-bediener.md).

## Stand der Umsetzung

- **Zwei Stationen:** Am Schmelzofen schmilzt er, am Schmiedetisch repariert er; was er tut, hängt allein davon ab, wo er arbeitet. Der Knopf **i** in beiden Menüs öffnet die Stationsübersicht. Panzermacher und Werkzeugschmiede aus dem Dorf arbeiten weiter an diesen Blöcken, außer an eingerichteten.
- **Kein Auftrag:** Er schmilzt alles aus den Eingangskisten, was der Schmelzofen annimmt (rohe Erze, Erzblöcke, Eisen- und Goldwerkzeug …). Was der Ofen nicht kann, lässt er liegen.
- **Brennstoff** ist alles, was brennt (Kohle, Holzkohle, Kohleblöcke, Lavaeimer …). Er legt ihn aus denselben Eingangskisten nach; den leeren Eimer bringt er mit weg.
- **Mengen:** Pro Gang holt er bis zu 16 Items zum Schmelzen (Config `smith.batchSize`) und bis zu 8 Brennstoff (Config `smith.fuelPerTrip`). Das Schmelzen selbst macht der Ofen in seinem normalen Tempo.
- **Abliefern:** Er nimmt die Ergebnisse aus dem Ofen und bringt sie in eine Ausgangskiste. Die Erfahrung bleibt im Ofen; der Spieler bekommt sie, wenn er selbst etwas herausnimmt.
- Am Schmelzofen arbeitet er nicht ohne Eingangs- und Ausgangskiste. Werkzeug braucht er nicht.
- **Status im Menü:** „Holt Erz und Brennstoff“, „Befüllt den Schmelzofen“, „Schmilzt“, „Wartet auf etwas zum Schmelzen“, „Wartet auf Brennstoff“, „Wartet: kommt nicht an den Schmelzofen“.
- **Reparieren** (Schmiedetisch), wie in der [Werkzeugrückgabe](../dorf/werkzeugrueckgabe.md#reparieren) beschrieben, mit diesen Abweichungen:
  - **Quellen:** abgenutzte Werkzeuge (`tools.returnBelow`) aus den Dorftruhen in Reichweite des Schmiedetischs **und alles Beschädigte** mit `repairable`-Komponente aus seinen Eingangskisten (die „Reparaturkiste“: Werkzeug, Waffen, Rüstung des Spielers).
  - Er nimmt nur, wofür Material in den Eingangskisten liegt, bis zu 4 Stück aus **einer** Truhe, und bringt alles in dieselbe zurück. Ist sie voll: nächste Dorftruhe, dann Ausgangs-, dann Eingangskiste.
  - Eine Ausgangskiste braucht er nicht, nur eine Eingangskiste. Pro Material 2 Sekunden am Tisch; Material holt er höchstens 16 auf einmal.
  - Keine Emeralds als Bezahlung, kein Erfahrungs- oder Kostenzuwachs; Verzauberungen und Namen bleiben.
  - Status: „Holt Werkzeug zum Reparieren“, „Holt Material zum Reparieren“, „Repariert Werkzeug“, „Bringt repariertes Werkzeug zurück“, „Wartet auf Werkzeug zum Reparieren“, „Wartet auf Eisenbarren zum Reparieren“, „Wartet: kommt nicht an den Schmiedetisch“.
  - Tests: eine abgenutzte Eisenaxt aus der Dorftruhe wird mit 4 Eisenbarren repariert und zurückgelegt; eine verzauberte, beschädigte Diamantspitzhacke aus der Eingangskiste mit 2 Diamanten, Stein bleibt liegen.
- **Nicht umgesetzt:** Master-Fähigkeit.

## Modi

| Modus | Station | Was passiert |
| --- | --- | --- |
| Schmelzen (umgesetzt) | Schmelzofen | Erz aus der Eingangskiste schmelzen, Brennstoff nachlegen, Barren abliefern |
| Reparieren (umgesetzt) | Schmiedetisch | Abgenutzte Werkzeuge aus den Dorftruhen und Beschädigtes aus der Eingangskiste mit Material reparieren und zurücklegen |

## Kisten

| Station | Eingang | Ausgang |
| --- | --- | --- |
| Schmelzofen | Rohe Erze, Kohle/Holzkohle | Barren |
| Schmiedetisch | Reparaturmaterial (Barren, Diamanten, Bretter …), beschädigte Werkzeuge und Rüstung | nicht nötig; Reserve, wenn die Herkunftstruhe voll ist |

## Hinweise

- Reparatur senkt die Werkzeugkosten aller Berufe deutlich, kostet aber Barren.
- Brennstoff kann vom [Holzfäller](holzfaeller.md) (Holzkohle über einen Ofen) oder [Minenarbeiter](minenarbeiter.md) (Kohle) kommen.

## Master-Fähigkeit (Konzept)

Doppelte Ausbeute bei einem Teil der Erze (wie ein kleiner Fortune-Bonus beim Schmelzen) oder kostenlose Reparatur ab und zu.

## Siehe auch

[Logistik](../grundlagen/logistik.md) · [Minenarbeiter](minenarbeiter.md) · [Brenner](brenner.md) (Brennofen für Baustoffe, ohne Erz und Essen)
