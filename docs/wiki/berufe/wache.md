# Beruf: Wache

[← Berufe](README.md) · Station: **Zielscheibe** (Vanilla) · Phase: **umgesetzt** (Bewachen, Jagen, Überfälle, Erfahrung; ohne Rückzug vor Monstern und Pfeil-Anzeige in der Dorftruhe)

Die Wache schützt das Dorf nachts mit Pfeil und Bogen vor Monstern. Sie ist der Grund, warum das Dorf eine dauernde **Pfeilversorgung** braucht: Jeder Schuss kostet einen Pfeil, und ohne Pfeile ist die Wache wehrlos. Die Pfeile macht der [Pfeilmacher](pfeilmacher.md).

Vanilla-Dorfbewohner kämpfen nicht; die Wache ist eine bewusste Abweichung. Sie bleibt aber nah an Vanilla: Sie schießt wie ein Skelett mit echten Pfeilen und echtem Bogen, und Monster können sie verletzen.

## Stand der Umsetzung

- **Station:** Die Zielscheibe ist ein POI dieser Mod (`workingvillagers:guard`), kein Vanilla-Dorfbewohner nimmt sie. Rechtsklick mit leerer Hand öffnet die Übersicht; Eingangskisten für Pfeile und Bögen, Ausgangskisten für die Beute, beides freiwillig. Reiter **Einstellungen**: Wachradius 8–32 Blöcke, Standard 16 (gespeichert im Feld `herd` der Station als halber Radius).
- **Nachtschicht:** eigener Tagesplan statt des Vanilla-Plans (Mixin auf den Schritt `UpdateActivityFromSchedule`, der am Ende jedes Pakets steht): Arbeit von 12000 bis 1000, Schlaf bis 9000, danach Freizeit. Dadurch setzt der Plan die Wache auch nach Überfall oder Glocke wieder an die Arbeit. **Keine Panik** (Mixin auf `VillagerPanicTrigger`).
- **Schießen:** wie ein Skelett mit echtem Pfeil (`ProjectileUtil.getMobArrow`) aus dem Inventar und dem Bogen der Wache, alle 1,5 Sekunden (Config `guard.secondsPerShot`), bis 24 Blöcke weit (Config `guard.range`), auf das nächste sichtbare Monster (`Enemy`, aber nicht `NeutralMob`) im Wachradius. Jeder Schuss kostet einen Pfeil und eine Nutzung des Bogens; Unendlichkeit wirkt nicht.
- **Kein Eigenbeschuss:** Pfeile von Wachen treffen nur `Enemy` (Mixin auf `AbstractArrow.canHitEntity`).
- **Morgenrunde** (0 bis 1000): eigene Fehlschüsse im Radius + 8 aufsammeln, dann die Beute an den Stellen der erlegten Monster; die Beute (nicht Pfeile und Bögen, Pfeile über 64 schon) in die Ausgangskiste.
- **Versorgung:** Bogen (Werkzeug, bis 2, wird kurz vor dem Bruch zurückgegeben) und Pfeile (Pflicht, bis 64, Config `guard.keepArrows`). Unter 16 Pfeilen holt sie Pfeile (und fehlt der Bogen, einen Bogen) aus den Eingangskisten; die Dorftruhe füllt wie bei allen Arbeitern auf.
- **Menü:** „Monster letzte Nacht: x“ (gezählt pro Nacht von Dämmerung bis Dämmerung).
- **Kleidung:** Kettenhemd unter blauem Wappenrock, Eisenhelm mit Nasenschutz.
- **Tests:** Nachts erlegt die Wache einen Zombie, ohne die Kuh daneben zu treffen; morgens sammelt sie ihre drei Fehlschüsse ein, einen fremden Pfeil nicht.
- **Überfälle:** Wachen machen beim Vanilla-Überfallverhalten nicht mit (Mixins auf `SetRaidStatus` und `ReactToBell`): Sie verstecken sich nicht und fliehen nicht. Läuft ein Überfall, wo die Wache ist, arbeitet sie auch tagsüber, bewacht mindestens 24 Blöcke um ihren Posten und schießt zuerst auf Plünderer. Sieht sie einen, läutet sie die nächste Glocke im Umkreis von 48 Blöcken (Vanilla-`attemptToRing`), höchstens alle 30 Sekunden: Die Plünderer leuchten, die Dorfbewohner verstecken sich. Vorher gab es hier eine Lücke: Vanilla schaltete die Wache bei Überfällen auf „Überfall“ oder „Verstecken“, ihr Tagesplan nachts zurück auf „Arbeit“, tagsüber versteckte sie sich.
- **Jagen und Erfahrung:** siehe [Jagd und Erfahrung](../dorf/erfahrung.md#stand-der-umsetzung).
- **Tests unabhängig von der Uhr:** Die Gametests laufen nebeneinander, und Fabric startet Testgruppen mit verschiedenen Uhrzeiten teils gleichzeitig. Die Wachen-Tests geben der Wache deshalb eine eigene Uhrzeit vor (`GuardPost.setTimeForTests`) und nehmen Wüstenzombies, die im Tageslicht nicht brennen. Der Überfall-Test prüft nur, dass die Wache bei einem Überfall am Tag arbeitet, und beendet ihn sofort, damit sich die Dorfbewohner der anderen Tests nicht verstecken.
- **Nicht umgesetzt:** Zurückweichen vor nahen Monstern, „Pfeile reichen für ~N Nächte“ in der Dorftruhe, Rüstung tragen, Armbrust.

## Station: Zielscheibe

- Die Vanilla-**Zielscheibe** wird zum Wachposten. Kein Vanilla-Beruf nutzt sie, also keine Konflikte mit Dorfbewohnern.
- Wer will, baut einen Wachturm und setzt die Zielscheibe oben hin: Die Wache steht dann dort oben (erhöht trifft sie besser und wird seltener erreicht).
- Übersicht wie bei jeder Station: Reiter Arbeiter, Lohn, Kisten, **Einstellungen** (Wachradius 8–32 Blöcke, Standard 16).

## Nachtschicht

Wachen arbeiten **nachts** und schlafen tagsüber, umgekehrt wie alle anderen. Sie bekommen dafür einen eigenen Tagesplan (Schlafen vormittags, Freizeit am Nachmittag, Dienst von der Dämmerung bis zum Morgen).

**Im Dienst:**

1. Am Posten stehen und Ausschau halten.
2. Feindliche Mobs (Zombies, Skelette, Spinnen, Creeper, Hexen, Plünderer …) im Wachradius **beschießen**, die nächsten an anderen Dorfbewohnern zuerst. Creeper aus der Ferne, bevor sie nahe kommen.
3. Kommt ein Monster zu nah, weicht sie zurück, statt in Panik zu fliehen (Wachen geraten nicht in Panik).
4. Bei einem Überfall läutet sie die Glocke, wie ein Vanilla-Dorfbewohner, und kämpft weiter.

**Am Morgen:**

- Sie sammelt ihre Fehlschüsse im Radius ein (nur ihre eigenen; Skelettpfeile lassen sich ohnehin nicht aufheben).
- <a id="beute"></a>Sie sammelt die Beute der erlegten Monster ein und bringt sie in ihre Ausgangskiste: Fäden (für Bögen), Knochen, Schießpulver, Pfeile der Skelette. So kommt ein Teil des Materials zurück.

## Versorgung

| Was | Pflicht | Hält bis | Woher |
| --- | --- | --- | --- |
| Bogen | ja | 2 | Pfeilmacher, Dorftruhe; nutzt sich ab wie beim Spieler |
| Pfeile | ja | 64 | Pfeilmacher über die [Dorftruhe](../dorf/dorftruhen.md) |
| Rüstung | nein | angelegt | Spieler oder Schmied; sichtbar getragen |
| Lohn | ja | | Lohnkasse der Zielscheibe |

- **Pfeile werden knapp:** Unter 16 Pfeilen holt sie sich vor dem Dienst neue aus der Dorftruhe. Ohne Pfeile steht im Menü „Keine Pfeile!“, der Reiter bekommt den roten Punkt, und sie schüttelt den Kopf. Sie bleibt dann im Haus, statt ungeschützt draußen zu stehen.
- **Die Dorftruhe** zeigt, für wie viele Nächte die Pfeile reichen, wie heute beim Lohn („Pfeile reichen für ~3 Nächte“), gerechnet aus dem Verbrauch der letzten Nächte.
- Verzauberte Bögen (Stärke, Flamme, Unendlichkeit) wirken wie beim Spieler. **Unendlichkeit** würde den Kreislauf aushebeln. Empfehlung: Bei Wachen wirkt Unendlichkeit nicht, oder nur auf die Hälfte der Schüsse.

## Sicherheit

- **Kein Eigenbeschuss:** Pfeile von Wachen treffen keine Dorfbewohner, Spieler, Golems, Haustiere oder Tiere der Herden; sie fliegen hindurch.
- Wachen greifen keine neutralen Mobs an (Endermen, Wölfe, Eisengolems), nur feindliche.
- Eine Wache kann sterben. Dann fällt ihre Ausrüstung zu Boden, wie bei jedem Arbeiter.

## Zahlen im Menü

Reiter Arbeiter: „Letzte Nacht: 14 Pfeile verschossen, 6 eingesammelt, 4 Monster erlegt“. Das macht den Verbrauch sichtbar und zeigt, ob mehr Licht oder eine zweite Wache nötig ist.

## Stufe 2 (später)

- **Jagen und Erfahrung:** Wachen jagen nachts aktiv, füllen die Erfahrung der erlegten Monster in Erfahrungsfläschchen, und der Schmied repariert damit Bögen und verzauberte Ausrüstung. Eigenes Konzept: [Jagd und Erfahrung](../dorf/erfahrung.md).

- Armbrust statt Bogen (durchschlagende Pfeile, langsamer).
- Spektral- und getränkte Pfeile gezielt gegen bestimmte Mobs.
- Begleitschutz für [Minenarbeiter](minenarbeiter.md) und [Händler](haendler.md) (frühere Idee dieser Seite).
- Senkt das Überfallrisiko in der Abstraktion des Händlers und zählt zur [Sicherheit](../wirtschaft/beduerfnisse.md) des Dorfes.

## Offene Fragen

- **Nachtschicht oder Tag und Nacht?** Empfehlung: Nachtschicht, weil Monster nachts kommen und der Tagesplan der anderen Arbeiter unverändert bleibt.
- **Zielscheibe als Station?** Alternative: ein eigener Block „Wachposten“. Empfehlung: Zielscheibe, kein neuer Block nötig.
- **Wie stark?** Ein Vanilla-Skelett trifft mit etwa 2–5 Schaden. Empfehlung: gleich viel, damit Rüstung und Bogenverzauberung Unterschiede machen.

## Siehe auch

[Pfeilmacher](pfeilmacher.md) · [Metzger](metzger.md) · [Dorftruhen](../dorf/dorftruhen.md) · [Ideensammlung](../planung/ideensammlung.md)
