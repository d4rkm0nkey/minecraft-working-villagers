# Werkzeugrückgabe

[← Wiki-Start](../README.md) · Phase: **umgesetzt**, mit Reparieren durch den Schmied (siehe [Stand der Umsetzung](#stand-der-umsetzung)) · Wunsch vom 15.10.2026 · baut auf [Dorftruhen](dorftruhen.md) auf

Ohne Werkzeugrückgabe nutzt ein Arbeiter sein Werkzeug, bis es zerbricht. Dann ist es weg. Mit der Werkzeugrückgabe tauscht er es **kurz vorher** aus und legt das abgenutzte Werkzeug in eine [Dorftruhe](dorftruhen.md). Von dort kann der Spieler es reparieren, oder später der [Schmied](../berufe/schmied.md).

## Stand der Umsetzung

Umgesetzt für Holzfäller, Bauer, Hirte, Metzger, Fischer, Grubenarbeiter und Minenarbeiter, mit diesen Abweichungen:

- **Zeitpunkt:** Er gibt das Werkzeug vor seinem nächsten Arbeitsschritt zurück (nächster Baum, nächster Block), auch wenn er ein Ersatzwerkzeug dabeihat; nicht erst in der Pause. Die Grubenarbeit unterbricht er dafür wie für Lohn und Werkzeug: erst aus der Grube heraus.
- **Wohin:** in die nächste Dorftruhe in Reichweite mit einem **freien Platz**, sonst in die nächste Zielkiste der Station mit freiem Platz. Geht er zu einer Dorftruhe, nimmt er im selben Gang mit, was ihm fehlt. Findet er keinen Platz (oder kommt nicht hin), behält er es und versucht es nach einer Minute wieder.
- **Kein eigener Status „kein Platz zum Abgeben“:** Hat er kein brauchbares Werkzeug mehr, zeigt er wie bisher „Braucht eine Axt“. Der Tooltip der Versorgungszeile sagt „Gibt N abgenutzte Werkzeuge zurück“; Status unterwegs: „Gibt ein abgenutztes Werkzeug zurück“.
- **Grenze:** `tools.returnBelow` Prozent der Haltbarkeit, mindestens 2 Nutzungen. Bei der Grenze von 10 % ist ein Werkzeug zugleich „fast aufgebraucht“ im Sinne der [Dorftruhen](dorftruhen.md#was-sich-ein-arbeiter-holt): Er holt sich dann Ersatz.
- **Holz und Stein:** erkannt an der `repairable`-Komponente (repariert mit Brettern oder Bruchstein), so zählen auch Werkzeuge anderer Mods. Verzaubert oder umbenannt kommen sie immer zurück. **Scheren** und **Angeln** zählen ebenfalls als billig: Sie kosten wenig, und der Schmied kann sie nicht reparieren.
- **Geben:** Ein abgenutztes Werkzeug nimmt ein Arbeiter auch vom Spieler nicht an.
- **Anzeige:** Statt des gelben Rahmens nur die Tooltip-Zeile. Das Dorftruhen-Panel zeigt „N abgenutzte Werkzeuge zum Reparieren“.
- **Reparieren durch den Schmied:** umgesetzt, siehe [Schmied, Stand der Umsetzung](../berufe/schmied.md#stand-der-umsetzung). Abweichend repariert er auch alles Beschädigte aus den Eingangskisten seines Schmiedetischs. Das Dorftruhen-Panel zeigt nicht an, ob ein Schmied repariert.
- **Nicht umgesetzt:** die Meldung „Hat die ‚Glücksschaufel‘ in … gelegt“.
- **Tests:** Ein Holzfäller mit abgenutzter Axt und Ersatz fällt mit dem Ersatz und legt die alte in die Dorftruhe; ohne Ersatz tauscht er sie in einem Gang gegen eine neue; ohne Dorftruhe landet sie in der Zielkiste und er wartet, statt sie aufzubrauchen; eine Steinaxt wird aufgebraucht.

## Warum

- **Werkzeug geht nicht verloren.** Eine Eisenaxt ist im frühen Spiel teuer. Wird sie zurückgegeben statt zerbrochen, kostet sie mit Reparatur nur einen Teil der Barren.
- **Verzauberte Werkzeuge werden nutzbar.** Wer einem Grubenarbeiter seine Effizienz-Schaufel gibt, bekommt sie heute nie zurück. Mit der Rückgabe ist das kein Risiko mehr.
- **Arbeit für den Schmied.** Das geplante Reparieren am Schmiedetisch bekommt einen klaren Kreislauf: Arbeiter → Dorftruhe → Schmied → Dorftruhe → Arbeiter.

## Wann gibt er zurück?

- Sobald die Haltbarkeit seines Werkzeugs auf **10 % oder weniger** sinkt (Config `tools.returnBelow`, 0–50; 0 schaltet die Rückgabe ab). Mindestens bleiben aber 2 Haltbarkeitspunkte, damit auch sehr kurzlebige Werkzeuge vorher zurückkommen.
- Er arbeitet den laufenden Schritt noch fertig (den Baum, das Feld, die Etage) und hört dann mit diesem Werkzeug auf.
- Hat er ein Ersatzwerkzeug dabei, arbeitet er damit sofort weiter und bringt das alte bei der nächsten Gelegenheit weg: in der Pause, zum Feierabend oder wenn er ohnehin zur Dorftruhe geht.
- Hat er keinen Ersatz, geht er gleich los: altes Werkzeug abgeben, neues aus einer Dorftruhe holen (siehe [Dorftruhen, Ablauf](dorftruhen.md#ablauf)). Ein Gang für beides.

## Wohin?

Der Reihe nach:

1. **In die nächste Dorftruhe** in Reichweite, die Platz hat.
2. Ohne passende Dorftruhe: in die **Zielkiste seiner Station**, so wie seinen Ertrag. Der Träger hat keine Zielkiste der Station; er legt es in die Zielkiste seines ersten Wegs.
3. Ist auch dort kein Platz, **behält er es** und benutzt es nicht weiter. Status: „Axt fast kaputt, kein Platz zum Abgeben“. Er versucht es beim nächsten Gang erneut.

**Kein Ersatz in Sicht:** Gibt es in keiner Dorftruhe ein brauchbares Werkzeug, arbeitet er heute schon nicht ohne. Er wartet an der Station mit „Hat keine Axt“, wie bisher. Das abgenutzte Werkzeug opfert er also nie, um weiterzuarbeiten.

## Abgenutztes Werkzeug wird nicht wieder genommen

Ein Arbeiter nimmt aus einer Dorftruhe **kein Werkzeug mit 10 % Haltbarkeit oder weniger** (dieselbe Grenze `tools.returnBelow`). Sonst würde der nächste Arbeiter die eben abgegebene Axt sofort wieder holen. Eine eigene Markierung am Item ist dafür nicht nötig; die Haltbarkeit reicht.

Das ändert die Werkzeugwahl der [Dorftruhen](dorftruhen.md#was-sich-ein-arbeiter-holt): „das am stärksten abgenutzte zuerst“ gilt nur noch unter den Werkzeugen **über** der Grenze.

## Verzauberte und umbenannte Werkzeuge

- Arbeiter nehmen sie weiterhin nicht selbst aus Dorftruhen (Config `villageChests.takeEnchanted`).
- Gibt der Spieler sie direkt, gelten dieselben Regeln: Rückgabe bei 10 %. Sie landen in der Dorftruhe oder in der Zielkiste und bleiben dort, bis der Spieler sie holt oder repariert.
- Damit der Spieler sie findet, steht im Stationsmenü kurz: „Hat die ‚Glücksschaufel‘ in die Dorftruhe ‚Lager‘ gelegt“.

## Reparieren

**Ohne Schmied** repariert der Spieler selbst: am Amboss mit Material oder durch Kombinieren zweier Werkzeuge in der Werkbank. Die Dorftruhe ist dafür nur der Sammelplatz.

**Mit Schmied** (Modus „Reparieren“ am Schmiedetisch, siehe [Schmied](../berufe/schmied.md#modi)):

- Er sucht in den Dorftruhen in Reichweite **seines Schmiedetischs** nach Werkzeugen unter der Grenze.
- Er holt bis zu 4 auf einmal, repariert sie am Schmiedetisch und legt sie in die Dorftruhe zurück, aus der er sie genommen hat (ist sie voll: die nächste Dorftruhe).
- **Material** nach Vanilla-Regeln (`repairable`-Komponente): Eisenbarren für Eisen, Diamant für Diamant, Bruchstein für Stein, Bretter für Holz. Er nimmt es aus der Eingangskiste des Schmiedetischs.
- **Menge:** ein Material repariert 25 % der Haltbarkeit, wie am Amboss. Er repariert bis voll, sofern Material reicht.
- **Keine Erfahrungskosten** und keine „Zu teuer!“-Grenze. Verzauberungen bleiben erhalten.
- Status: „Holt Werkzeug zum Reparieren“, „Repariert eine Axt“, „Wartet auf Eisenbarren“.

## Holz und Stein

Holz- und Steinwerkzeug ist billig; eine Rückgabe ist hier eher lästig, weil sich in der Dorftruhe Bruch sammelt. Vorschlag: Config `tools.returnCheap` (Standard `false`). Aus heißt: Holz- und Steinwerkzeug wird wie heute aufgebraucht, Eisen und besser sowie alles Verzauberte oder Umbenannte geht zurück.

## Anzeige

- **Stationsmenü:** In der Versorgungszeile bekommt ein Werkzeug unter der Grenze einen gelben Rahmen und den Tooltip „Wird bald zurückgegeben“.
- **Dorftruhen-Panel:** eine Zeile „3 Werkzeuge zum Reparieren“. Mit Schmied: „Schmied repariert“.

## Config

| Schlüssel | Standard | Bedeutung |
| --- | --- | --- |
| `tools.returnBelow` | 10 | Rückgabe ab dieser Haltbarkeit in Prozent (0 = aus, bis 50) |
| `tools.returnCheap` | `false` | Auch Holz- und Steinwerkzeug zurückgeben |

## Technik

- Die Haltbarkeit prüft jede Arbeit schon beim Abnutzen (`hurtAndBreak` in `ChopTrees`, `FarmFields`, `DigPit`). Dort kommt eine gemeinsame Prüfung hinzu: `WorkerSupplies.isWorn(stack)`. Ein abgenutztes Werkzeug zählt bei `busy` nicht mehr als vorhanden.
- Das Abgeben ist ein weiterer Gang in `WorkerSupplies`, wie das Holen aus der Dorftruhe. Beides wird zu einem Gang zusammengelegt, wenn dieselbe Truhe passt.
- Der Filter, mit dem eine Arbeit ihren Ertrag abliefert, muss abgenutzte Werkzeuge durchlassen, damit sie im Fallback in der Zielkiste landen. Heute behält der Arbeiter sein Werkzeug immer.
- Beim Tod lässt er wie bisher alles fallen, auch abgenutzte Werkzeuge.

## Tests (beim Umsetzen)

- Ein Holzfäller mit einer Eisenaxt bei 10 % und einer Ersatzaxt arbeitet mit der neuen weiter und legt die alte in der Pause in die Dorftruhe.
- Ohne Ersatz holt er im selben Gang eine neue Axt und gibt die alte ab.
- Ein anderer Arbeiter nimmt die abgegebene Axt nicht.
- Ohne Dorftruhe landet die Axt in der Zielkiste der Station.
- Eine verzauberte Schaufel, direkt gegeben, kommt bei 10 % zurück und zerbricht nie.
- Mit `returnCheap = false` wird eine Steinaxt aufgebraucht.
- Mit `returnBelow = 0` verhält sich alles wie heute.

## Offene Fragen

- **Grenze für alle gleich?** Bei einer Diamantaxt sind 10 % noch über 150 Fällschläge. Vielleicht besser eine feste Restzahl (z. B. 20 Schläge) statt Prozent?
- **Wer bezahlt die Reparatur?** Nur Material, oder nimmt der Schmied zusätzlich Emeralds aus der Dorftruhe?
- **Zu viel Bruch:** Wenn niemand repariert, füllt sich die Dorftruhe mit kaputtem Werkzeug. Reicht ein Hinweis im Panel, oder soll ab einer Menge doch aufgebraucht werden?

## Siehe auch

[Dorftruhen](dorftruhen.md) · [Schmied](../berufe/schmied.md) · [Anheuern & Lohn](../wirtschaft/anheuern-und-lohn.md)
