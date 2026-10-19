# Beruf: Imker

[← Berufe](README.md) · Station: **Lagerfeuer** (Vanilla) · Phase: **Konzept**

Der Imker pflegt die Bienenstöcke und Bienennester um seine Station: Er erntet Honigwaben und Honigflaschen, ohne die Bienen zu reizen, und hält die Völker am Leben. Er ist ein ruhiger Ausbauberuf, der vorhandene Ketten verbindet: Flaschen vom [Apotheker](apotheker.md), Scheren vom [Waffenschmied](waffenschmied.md), Blumen vom [Bauer](farmer.md), und seine Waben gehen an den Tischler, die Kerzen und an das Wachsen von Kupfer.

## Workstation: Lagerfeuer

- Vanilla kennt keinen Imker, also auch keine Station. Das **Lagerfeuer** passt: Sein Rauch beruhigt in Vanilla die Bienen, wenn man unter dem Stock erntet. Ein Imker an einem Lagerfeuer erklärt sich damit fast von selbst.
- Wie bei der [Zielscheibe](wache.md) wird das Lagerfeuer erst durch einen zugewiesenen Arbeiter zur Station. Gewöhnliche Dorfbewohner beanspruchen es nicht, Deko-Lagerfeuer bleiben Deko.
- Rechtsklick mit leerer Hand auf ein *eingerichtetes* Lagerfeuer öffnet die Übersicht; sonst bleibt das Lagerfeuer, wie es ist (Essen braten, löschen mit Schaufel).
- **Arbeitsbereich:** alle Bienenstöcke und -nester in 12 Blöcken um das Lagerfeuer (Config `beekeeper.radius`), wie beim Holzfäller.
- Alternative: ein eigener Block „Bienenstand“ (Tisch mit Rähmchen und Smoker). Mehr Arbeit (Modell, Textur, Rezept) und gegen das Prinzip „Vanilla-Station wo möglich“. Empfehlung: Lagerfeuer.

## Arbeitsablauf

1. Lohn nehmen wie alle Arbeiter.
2. Die Stöcke im Bereich ablaufen. Ein Stock mit **Honigstand 5** (voll) ist erntereif.
3. Ernten nach Auftrag (siehe unten):
   - **Schere** → 3 Honigwaben, die Schere nutzt sich ab.
   - **Glasflasche** → 1 Honigflasche.
4. Der Imker reizt die Bienen dabei nie, auch ohne Lagerfeuer unter dem Stock. Das ist sein Können; ein Spieler muss weiter räuchern.
5. Abliefern in die Ausgangskiste, leere Flaschen und Scheren aus der Eingangskiste nachholen.
6. Nachts und bei Regen sind die Bienen im Stock; er erntet trotzdem (die Bienen bleiben drin), aber nur bei Tag, wie alle Arbeiter.

### Auftrag

Ein Raster wie beim [Pfeilmacher](pfeilmacher.md):

| Auftrag | Was er tut |
| --- | --- |
| Honigwaben | erntet nur mit der Schere |
| Honigflaschen | erntet nur mit Flaschen; ohne Flaschen wartet er |
| Beides | Flaschen, solange welche da sind, sonst Schere |

Dazu ein **Vorrat** wie bei den anderen Berufen: Ist er voll, lässt er die Stöcke voll (sie verlieren nichts).

### Stufe 2: Weiterverarbeiten am Lagerfeuer

Wenn nichts zu ernten ist, verarbeitet er, was in der Eingangskiste liegt (an/aus im Auftrag):

| Produkt | Zutaten | Wofür |
| --- | --- | --- |
| Honigblock | 4 Honigflaschen (gibt 4 leere Flaschen zurück) | Bauen, Redstone |
| Honigwabenblock | 4 Honigwaben | Bauen |
| Kerze | 1 Faden, 1 Honigwabe | Licht; Faden bringen die [Wachen](wache.md) von Spinnen |
| Zucker | 1 Honigflasche → 3 Zucker | Kuchen, Tränke des Apothekers |

## Völker pflegen (Master-Fähigkeit)

- **Bienen vermehren:** Hat ein Stock Platz (weniger als 3 Bienen) und liegen Blumen in der Eingangskiste, füttert er zwei Bienen draußen mit Blumen → eine junge Biene. So füllt er neue Stöcke, die der Spieler aufstellt.
- **Nester umsetzen** (später): ein wildes Bienennest mit Behutsamkeit nehmen und als Stock am Lagerfeuer aufstellen. Braucht ein verzaubertes Werkzeug, eher Stufe 3.
- **Blumen pflanzen** (später): Blumen aus der Eingangskiste auf Gras im Bereich setzen, damit die Bienen kurze Wege haben.

## Kisten (Eingang / Ausgang)

| Eingangskiste | Ausgangskiste |
| --- | --- |
| Glasflaschen, Schere(n), Blumen, Faden, Honig/Waben zum Weiterverarbeiten | Honigwaben, Honigflaschen, Honig- und Wabenblöcke, Kerzen, Zucker |

Die Schere kann auch aus der [Dorftruhe](../dorf/dorftruhen.md) kommen und geht abgenutzt dorthin zurück ([Werkzeugrückgabe](../dorf/werkzeugrueckgabe.md)).

## Wofür das Dorf Honig braucht

- **Honigwaben:** Kupfer wachsen (Kupfertruhen, Kupferblöcke bleiben blank), Kerzen, **Bienenstöcke** (Planks + Waben; der [Tischler](tischler.md) kann sie über den Tag `workingvillagers:joinery` bauen). Damit wächst die Imkerei aus sich selbst.
- **Honigflaschen:** Essen (später für [Kost & Logis](../wirtschaft/beduerfnisse.md)), heilt Vergiftung, Zucker.
- **Bestäubung:** Bienen lassen Weizen, Karotten usw. auf dem Rückweg wachsen. Ein Imker neben den Feldern des [Bauern](farmer.md) macht die Felder schneller, ohne dass der Mod etwas dafür tun muss.

## Status

„Erntet Honig“, „Sieht nach den Stöcken (2/5 voll)“, „Wartet: keine Glasflaschen“, „Wartet: keine Schere“, „Keine Bienenstöcke in der Nähe“, „Vorrat ist voll (x/y)“, „Heute: x Waben, y Flaschen“.

## Kleidung

Heller, sandfarbener Kittel, breiter Hut mit Schleier (Netz vor dem Gesicht), Handschuhe.

## Technik

- Eigener Beruf (`VillagerProfession`) wie der Tischler, POI auf dem Lagerfeuer (wie die Wache auf der Zielscheibe). Beide Lagerfeuer (normal und Seelenfeuer) zählen.
- Ernte über den Vanilla-Weg des Stocks (Honigstand auf 0 setzen, Waben/Flasche ausgeben), aber **ohne** die Bienen freizulassen oder wütend zu machen.
- Bienen greifen nur an, wenn sie gereizt sind. Wird der Imker trotzdem gestochen (z. B. weil ein Spieler die Bienen reizt), flieht er wie bei einem Monster und arbeitet danach weiter.
- Vanilla kann das schon mit einem Werfer (Schere oder Flasche auf den Stock). Der Unterschied: Der Imker kümmert sich um viele Stöcke, holt Nachschub, liefert ab und vermehrt die Bienen.

## Tests

- Ein voller Stock, Schere in der Eingangskiste → 3 Waben in der Ausgangskiste, Stock leer, keine wütende Biene.
- Auftrag Flaschen, 2 Flaschen, 3 volle Stöcke → 2 Honigflaschen, ein Stock bleibt voll, Status „keine Glasflaschen“.
- Vorrat 3 Waben → er erntet nur einen Stock.

## Sonderfälle & offene Fragen

- **Wie schnell füllt sich ein Stock?** Das hängt an Blumen in der Nähe und der Zahl der Bienen. Im Playtest messen, bevor man über Lohn und Vorrat entscheidet; ein Imker sollte sich ab etwa 4 Stöcken lohnen.
- **Lagerfeuer als Station:** Ein Lagerfeuer, das jemand zum Kochen nutzt, ist nicht betroffen, solange niemand dort angestellt ist. Was, wenn der Spieler das Feuer löscht? Vorschlag: Der Imker arbeitet auch an einem gelöschten Lagerfeuer.
- **Berufsbuch:** Wer verkauft es? Vorschlag: der Bauer (Blumen, Felder).
- **Trades:** Ein angeheuerter Imker verkauft Honigflaschen, Waben und Kerzen und kauft Glasflaschen.

## Siehe auch

[Bauer](farmer.md) · [Apotheker](apotheker.md) · [Tischler](tischler.md) · [Wache](wache.md) · [Dorftruhen](../dorf/dorftruhen.md)
