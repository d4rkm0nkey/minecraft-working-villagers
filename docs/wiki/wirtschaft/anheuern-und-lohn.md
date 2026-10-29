# Anheuern & Lohn

[← Wiki-Start](../README.md)

Lohn ist die zentrale Bremse: Ein Angestellter muss mehr Wert erzeugen, als er kostet, aber nicht so viel, dass Handarbeit sinnlos wird. Alle Werte sind Startwerte für Playtests und per Config änderbar.

## Wer kann angeheuert werden?

Konzept (Stand der Umsetzung siehe [unten](#stand-der-umsetzung)):

- Nur Dorfbewohner eines [gekauften Dorfes](../dorf/dorf-und-kauf.md).
- Arbeitslose Dorfbewohner können angeheuert werden. Idee: Nitwits nur als [Träger](../berufe/traeger.md) (einfachste Arbeit, halber Lohn) – endlich ein Nutzen für sie. Noch nicht umgesetzt, Nitwits lassen sich bisher gar nicht anheuern.
- Berufstätige Villager behalten beim Anheuern ihre Angebote, damit Handel nicht kaputtgeht (umgesetzt, siehe unten).
- Das Level eines angeheuerten Villagers steigt durch Arbeit statt durch Handel (Novice → Master). Derzeit steigen Arbeiter mit Vanilla-Gegenstück durch Handel auf wie in Vanilla; die Stufe durch Arbeit soll dazukommen.

## Lohnstufen

Planwerte für einen späteren Wochenlohn. Im Spiel zahlt man einen Tageslohn, der mit dem [Level](../grundlagen/level.md#lohn) wächst: 1 Emerald für Neuling und Lehrling, 2 für Geselle und Experte, 3 für den Meister (Config `workers.dailyWage`, `workers.masterDailyWage`), siehe [Stand der Umsetzung](#stand-der-umsetzung).

| Level | Lohn (Emerald/Woche) |
| --- | --- |
| Novice | 16 |
| Apprentice | 24 |
| Journeyman | 32 |
| Expert | 48 |
| Master | 64 |

Was ein höheres Level bringt (Tempo, Werkzeugabnutzung, Arbeitsbereich, Freischaltungen) und wie Arbeiter aufsteigen, steht unter [Level und Aufstieg](../grundlagen/level.md).

Die ursprünglich genannten 100 Emeralds pro Woche sind für Early Game sehr viel. Vorschlag: mit 16–64 Emeralds starten und nach Playtests justieren. Erfüllte [Bedürfnisse](beduerfnisse.md) senken den Lohn um bis zu 50 %.

## Dorfkasse & Bezahlung

- Alle Löhne kommen aus der [Dorfkasse](../dorf/dorf-und-kauf.md#dorfkasse) (Barrel neben der Glocke). Am Zahltag nimmt sich der Villager seinen Lohn selbst.
- Alternativ zahlt der Spieler per Rechtsklick mit Emeralds im Voraus (Guthaben in Wochen).

### Stand der Umsetzung

**Anheuern:** Den Arbeitsvertrag craftet man formlos aus Papier, Tintenbeutel und 3 Emeralds. Rechtsklick damit auf einen erwachsenen Dorfbewohner stellt ihn ein, in jedem Dorf und auch außerhalb; einen Dorfkauf gibt es noch nicht. Ein arbeitsloser sucht sich danach selbst eine Station. Einer **mit Beruf bleibt an seinem Block** und wird dort Arbeiter dieser Mod: der Bauer am Komposter, der Steinmetz als Steinsäger an der Steinsäge, der Schäfer als Hirte am Webstuhl, Panzermacher und Werkzeugschmied als Schmied, der Fleischer als Metzger am Räucherofen, der Fischer am Fass. Nicht angeheuert werden Nitwits, Babys und Dorfbewohner mit einem Beruf ohne Arbeit in dieser Mod (Bibliothekar, Kartograf, …).

**Handel:** Arbeiter mit Vanilla-Gegenstück handeln wie dieses (`TradeFamilies`): Bauer wie Bauer, Steinsäger wie Steinmetz, Hirte wie Schäfer, Metzger wie Fleischer, Fischer wie Fischer, Schmied wie Panzermacher (am Schmelzofen) bzw. Werkzeugschmied (am Schmiedetisch). Ihre Berufe verweisen dafür auf die Vanilla-Angebotslisten. Wer schon gehandelt hat, **behält beim Anheuern Angebote und Stufe**: Vanilla würde die Angebote beim Berufswechsel löschen, ein Mixin hebt sie innerhalb derselben Familie auf (auch über eine Zeit ohne Arbeit hinweg, gespeichert am Dorfbewohner). Aufstieg geschieht wie in Vanilla durch Handel; die Angebote füllt der Arbeiter während der Arbeitszeit an seiner Station auf (höchstens zweimal am Tag, wie Vanilla). Gehandelt wird mit leerer Hand oder mit Schleichen + Rechtsklick; mit Smaragden oder Werkzeug in der Hand bekommt er sie geschenkt. Die übrigen Arbeiter (Holzfäller, Holzsäger, Tischler, …) handeln nicht. Eine Anheuergebühr gibt es nicht, der Vertrag ist der Preis. Den [Träger](../berufe/traeger.md) stellt man statt mit Vertrag mit dem Weidenkorb ein. Geplant: [ein Vertrag je Beruf](arbeitsvertraege.md), nur in Truhen von Strukturen zu finden statt craftbar.

**Arbeitszeit:** die normale Vanilla-Arbeitszeit der Dorfbewohner; nachts schlafen sie.

**Lohn:** Eine Dorfkasse gibt es noch nicht. Jede Station hat eine **Lohnkasse** für bis zu 64 Emeralds (Wunsch vom 18.10.2026): Reiter **Lohn** der Stationsübersicht mit einem Slot und dem Spielerinventar wie ein Container; außerdem legt ein Rechtsklick mit Emeralds auf die Station sie hinein, und Emeralds, die man dem Arbeiter gibt oder zuwirft, legt er ebenfalls hinein. Aus Dorftruhen geholter Lohn (für 3 Tage) landet auch dort. Mit dem ersten Arbeitsstück des Tages (erster Stamm, erstes hergestelltes Teil, erste Ladung des Trägers; der Wächter, sobald er seinen Posten bezieht) nimmt sich der Arbeiter den Tageslohn (Standard 1 Emerald) aus der Kasse, danach aus der Tasche (Emeralds aus älteren Versionen). Schafft er den ganzen Tag nichts, etwa weil Material fehlt, kostet der Tag nichts (Wunsch vom 26.10.2026). Der Lohn muss aber vor der Arbeit da sein: Ohne Emerald wartet er an seiner Station.

- Die Emeralds gehören der Station, nicht dem Arbeiter: Stirbt er, wird er zum Zombie oder wechselt er die Station, geht nichts verloren; der nächste Arbeiter dort wird aus derselben Kasse bezahlt. Wird die Station abgebaut, gesprengt oder ersetzt, fallen die Emeralds heraus wie aus einer Truhe (über die POI-Aktualisierung von Vanilla, `ServerLevelMixin`). Gespeichert in den Stationsdaten (`wage`), nicht im Block.
- Der Träger hat keine Station und trägt seine Emeralds weiter selbst (bis zu 64).
- Werkzeug und Saatgut trägt der Arbeiter weiter selbst, der Spieler gibt sie direkt, wie Futter: Rechtsklick auf den Arbeiter oder die Items vor ihm fallen lassen.
- Holzfäller: bis zu 2 Äxte und 16 Setzlinge je Sorte. Farmer: bis zu 2 Hacken und 16 Saatgut je Sorte zum Start, danach pflanzt er aus der Ernte nach.
- Hat er genug, schüttelt er den Kopf. Zugeworfene Items nimmt er nur bis zu denselben Höchstmengen auf. Stirbt er, lässt er alles fallen.
- Der Lohn wächst mit dem [Level](../grundlagen/level.md): `workers.dailyWage` für den Neuling (0 = umsonst), `workers.masterDailyWage` für den Meister, dazwischen abgerundet (1, 1, 2, 2, 3). Wochenlöhne, Moral und Kündigung gibt es noch nicht.
- Kann er nicht arbeiten, schüttelt er ab und zu den Kopf mit dem „Nein“-Sound, wenn ein Spieler in der Nähe ist.

Ergänzend gibt es [Dorftruhen](../dorf/dorftruhen.md): Jeder Arbeiter in der Nähe holt sich daraus selbst, was ihm ausgeht (Lohn für 3 Tage, Ersatzwerkzeug, Setzlinge, Saatgut, Leitern, Fackeln).

## Moral statt Strafe

- Moral 0–100. Steigt durch pünktlichen Lohn, eigenes Bett, Nachtruhe, Glocke in Reichweite, erfüllte [Bedürfnisse](beduerfnisse.md).
- Sinkt durch ausgefallenen Lohn, Verletzungen, fehlendes Werkzeug.
- Unter 30: langsamer. Unter 10 oder 2 Wochen ohne Lohn: Kündigung, Villager wird wieder arbeitslos.

## Kosten neben dem Lohn

- Werkzeug nutzt sich ab und muss nachgegeben werden (derzeit direkt an den Arbeiter, siehe [Stand der Umsetzung](#stand-der-umsetzung)).
- Verbrauchsmaterial: Setzlinge, Fackeln, Stützblöcke.
- Aufschlag für Arbeiter, die ohne Spieler arbeiten: `chunkLoading.dailyFee` (Standard 1 Smaragd) pro Arbeitstag, mit dem Lohn genommen (siehe [Chunk-Loading](../dorf/chunk-loading.md#kosten)).
- Nahrung und Unterkunft sind keine Pflichtkosten, sondern senken den Lohn (siehe [Bedürfnisse](beduerfnisse.md)).

Wie das Geld hereinkommt: [Einnahmen](einnahmen.md).
