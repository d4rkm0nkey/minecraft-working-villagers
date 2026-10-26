# Beruf: Höhlenforscher

[← Berufe](README.md) · Station: **Leitstein** (Vanilla) · Phase: **Konzept**

Der Höhlenforscher macht natürliche Höhlen sicher: Er leuchtet sie Stück für Stück mit Fackeln aus, bis dort kein Monster mehr spawnen kann, und nimmt die Erze mit, die dabei frei an den Wänden liegen. Anders als der [Minenarbeiter](minenarbeiter.md) gräbt er keine Gänge, er nutzt, was die Welt schon hat.

## Funktioniert das?

Ja, besser als gedacht, weil die meisten Höhlenmonster Dorfbewohner gar nicht angreifen:

| Monster | Greift Dorfbewohner an? |
| --- | --- |
| Zombie, Husk, Drowned, Zombie-Dorfbewohner | **ja**, die einzige echte Kampfgefahr |
| Skelett, Bogged, Spinne, Höhlenspinne, Silberfisch, Schleim, Hexe | nein, nur Spieler (und Golems) |
| Creeper | nein, nur Spieler. **Außer** der Forscher greift ihn an: dann wehrt sich der Creeper |
| Warden | ja, er reagiert auf Vibrationen, auch auf die Schritte eines Dorfbewohners. Tödlich |

Gefährlich sind also vor allem Zombies und die Höhle selbst: Lava, Abgründe, Wasser, nachrutschender Kies und das Deep Dark.

## Was „sicher machen“ heißt

Seit 1.18 spawnen Monster nur bei Blocklicht 0. Der Forscher muss deshalb nicht alles töten:

1. **Ausleuchten ist die Hauptarbeit.** Er setzt Fackeln, bis kein begehbarer Block im Bereich mehr Licht 0 hat. Danach spawnt dort nichts mehr.
2. **Aufräumen macht Vanilla.** Monster ohne Spieler in der Nähe despawnen von selbst (zufällig ab 32 Blöcken Abstand, sofort ab 128). Eine ausgeleuchtete Höhle leert sich mit der Zeit von allein.
3. **Kämpfen nur, wenn nötig:** Zombies wehrt er immer ab, sie greifen ihn ja an. Creeper nur mit [Katze](#die-katze) dabei. Den Rest lässt er in Ruhe.

## Workstation: Leitstein

- Vanilla kennt keinen Höhlenforscher. Der **Leitstein** passt: Er ist der feste Punkt, zu dem ein Kompass zurückführt, und genau das ist die Station für den Forscher, der Ausgangspunkt, zu dem er immer zurückkehrt.
- Wie bei der [Zielscheibe](wache.md) und dem Lagerfeuer des [Imkers](imker.md) wird der Leitstein erst durch einen zugewiesenen Arbeiter zur Station. Ein Leitstein für Spieler-Kompasse bleibt, wie er ist.
- **Höhle wählen:** Knopf in der Übersicht, dann Rechtsklick auf den Boden am Höhleneingang (in Kistenreichweite der Station), wie beim Stollen des Minenarbeiters.
- **Radius:** im Menü 16, 32 oder 48 Blöcke um den Eingang (Config `caver.maxRadius`). Weiter nicht, solange es kein [Chunk-Loading](../dorf/chunk-loading.md) gibt.
- Alternative: eine Laterne als Station. Billiger, aber Laternen stehen überall als Deko herum. Empfehlung: Leitstein.

## Arbeitsablauf

1. Lohn nehmen wie alle Arbeiter. Fackeln, Werkzeug, Waffe, Essen und Tränke aus der Eingangskiste oder einer [Dorftruhe](../dorf/dorftruhen.md).
2. **Erkunden mit Lichtfront:** Ab dem Eingang sucht er per Flood-Fill alle begehbaren Blöcke im Radius. Er betritt nur Blöcke, die schon Licht haben, und setzt an der Grenze zum Dunkel die nächste Fackel. Hinter ihm kann so nie etwas spawnen; Gefahr kommt nur von vorn, und dahin schaut er.
3. **Erze:** Er baut nur freiliegende Erze ab (`workingvillagers:miner_ores`, an Luft grenzend), die er von seinem Weg aus erreicht, plus die Erze, die dahinter frei werden, bis `miner.maxVein`. Das ist dieselbe Erzsuche wie beim Minenarbeiter. Ganze Adern im Gestein gräbt er nicht frei.
4. **Abliefern:** wenn das Inventar voll ist, nach dem Tagespensum (`caver.dailyTorches`) und wenn die Höhle fertig ist. Er behält Lohn, Werkzeug, Fackeln, Essen und Tränke.
5. **Fertig:** Wenn kein dunkler, erreichbarer Block mehr im Radius liegt: „Höhle gesichert“. Danach geht er ab und zu eine Runde und setzt fehlende Fackeln nach (z. B. wenn ein Creeper eine weggesprengt hat).
6. Er arbeitet nur tagsüber, der Weg an der Oberfläche ist nachts gefährlich, und schläft nachts im Dorf.

## Wie er überlebt

Die Sicherheit kommt in Schichten:

1. **Vorbeugen:** die Lichtfront (siehe oben).
2. **Gefahren meiden:**
   - Lava und Wasser am Weg dichtet er mit Bruchstein ab, wie der Minenarbeiter.
   - An Abgründen über 3 Blöcken Fallhöhe hört er auf, statt zu springen. Der Bereich dahinter bleibt dunkel und wird gemeldet.
   - Sculk-Blöcke im Umkreis heißen Deep Dark: sofort umkehren, Bereich sperren, „Deep Dark bei x y z“. Den Warden weckt er so nie.
   - Spawner fasst er nicht an, er meldet sie nur („Spawner bei x y z“).
3. **Ausrüstung:** Rüstung, die sich wie bei der [Wache](wache.md) abnutzt (`Uniforms`), dazu Schwert oder Bogen. Optional ein Schild.
4. **Verhalten im Kampf:**
   - Er gerät nicht in Panik (derselbe Mixin wie bei der Wache).
   - Unter der Hälfte seiner Lebenspunkte zieht er sich ins Licht hinter seine Fackeln zurück.
   - Unter einem Drittel trinkt er einen Heiltrank aus dem Inventar. Hat er keinen, geht er nach Hause; dort heilt ihn der [Apotheker](apotheker.md).
5. **Katze** gegen Creeper (siehe unten).
6. **Begleitschutz:** später eine [Wache](wache.md), die ihm folgt (die Idee steht schon beim Minenarbeiter).

## Die Katze

Creeper fliehen in Vanilla vor Katzen und Ozelots und kommen nicht näher als etwa 6 Blöcke. Ohne Katze müsste der Forscher Creeper meiden, denn wer sie angreift, wird zum Ziel. Mit Katze kann er sie gefahrlos mit dem Bogen erledigen. Die Katze ist also ein Upgrade, keine Pflicht.

- **Übergeben:** Der Spieler gibt dem Forscher eine gezähmte Katze an der Leine oder per Rechtsklick. Die Katze gehört dann dem Arbeiter und folgt ihm.
- **Folgen:** wie bei zahmen Tieren; liegt sie mehr als 12 Blöcke zurück, teleportiert sie sich zu ihm.
- **Risiko:** Sie kämpft nicht, und Zombies beachten Katzen nicht. Gefährlich sind nur Explosionen, Lava und Stürze.
- Nachts bleibt die Katze bei ihm im Dorf. Wird er entlassen, sitzt sie beim Leitstein, bis der Spieler sie abholt.

## Kisten (Eingang / Ausgang)

| Eingangskiste | Ausgangskiste |
| --- | --- |
| Fackeln, Spitzhacke, Schwert oder Bogen und Pfeile, Rüstung, Essen, Heiltränke, Bruchstein zum Abdichten | Erze, Kohle, Beute der Monster (Knochen, Fäden, Schwarzpulver) |

**Kreislauf:** Fackeln baut er später selbst aus gefundener Kohle und Stöcken. Dann braucht er nur noch Stöcke vom [Tischler](tischler.md).

## Ausbaustufen

| Stufe | Was dazukommt |
| --- | --- |
| 1 | Ausleuchten und freiliegende Erze im Radius, nur auf Wegen, die Dorfbewohner von selbst laufen. Zombies wehrt er mit dem Schwert ab, sonst zieht er sich zurück. |
| 2 | Katze und Bogen: Er bekämpft Creeper. Heiltränke, Fackeln selbst bauen. |
| 3 | Kleine Stufen und Brücken aus Bruchstein, damit er auch über Lücken und Absätze kommt. Wache als Begleitschutz. |

## Status

„Leuchtet die Höhle aus (x Fackeln)“, „Baut Erz ab“, „Kämpft“, „Zieht sich zurück“, „Trinkt einen Heiltrank“, „Höhle gesichert“, „Kontrolliert die Fackeln“, „Wartet: keine Fackeln“, „Deep Dark bei x y z“, „Spawner bei x y z“, „Abgrund bei x y z“, „Keine Höhle gewählt“.

## Kleidung

Ein neuer Beruf, also ein eigenes Aussehen: erdbraune Jacke mit Lederflicken, Helm mit Grubenlampe wie der Minenarbeiter, aber heller, ein Seil über der Schulter.

## Technik

- Eigener Beruf (`VillagerProfession`) wie der Minenarbeiter, POI auf dem Leitstein (wie die Wache auf der Zielscheibe).
- **Erkunden:** Breitensuche über begehbare Blöcke (Luft mit festem Boden darunter, 2 hoch), begrenzt durch Radius und gesperrte Bereiche. Nächstes Ziel ist der dunkle Block mit dem kürzesten Weg über beleuchtete Blöcke. Ergebnis wird bei der Station gespeichert, damit er nach einem Neustart weitermacht.
- **Fackeln setzen:** dort, wo die meisten Blöcke mit Licht 0 hell werden. Am Boden oder an der Wand, wie `Digging` schon kann.
- **Kampf:** aus `GuardPost` (Bogen) übernehmen; Nahkampf mit Schwert ist neu. Ziel sind nur Zombies (immer) und Creeper (mit Katze).
- **Katze:** Mixin oder eigenes Goal auf `Cat`, das statt dem Spieler dem Arbeiter folgt. Besitzer-UUID bleibt beim Spieler, die Katze merkt sich zusätzlich den Arbeiter.
- **Erze:** Erzsuche aus `DigTunnel` in einen gemeinsamen Helfer ziehen.

## Tests

- Kleine Höhle aus Luft im Steinblock: danach hat kein begehbarer Block Licht 0, Fackeln im Inventar sind weniger geworden.
- Eisenerz an der Höhlenwand → Raw Iron in der Ausgangskiste; Eisenerz eine Reihe tief in der Wand bleibt.
- Lava am Weg: abgedichtet, Arbeiter lebt.
- Abgrund von 5 Blöcken: er springt nicht, Status „Abgrund bei …“.
- Sculk-Block im Bereich: der Bereich bleibt dunkel, Status „Deep Dark bei …“.
- Zombie in der Höhle: der Arbeiter lebt, der Zombie nicht.
- Creeper ohne Katze: der Arbeiter greift ihn nicht an. Mit Katze: der Creeper hält Abstand und stirbt durch Pfeile.

## Sonderfälle & offene Fragen

- **Wegfindung:** Dorfbewohner springen nicht über Lücken und bleiben an Kanten hängen. Wie viel Höhle erreicht er in Stufe 1 überhaupt? Früh in echten Höhlen testen. Die Rückholung (`workers.stuckSeconds`) fängt Hänger auf.
- **Ungeladene Chunks:** Ist der Spieler weit weg, bleibt der Forscher mitten in der Höhle stehen. Deshalb der kleine Radius, bis [Chunk-Loading](../dorf/chunk-loading.md) kommt.
- **Fackelverbrauch:** Große Höhlen brauchen hunderte Fackeln. Reicht ein Tagespensum, oder braucht es eine Grenze pro Höhle?
- **Erzwert:** Höhlen zeigen viele Erze. Bringt er zu viel Diamant? Im Playtest prüfen; Grabzeit und Spitzhackenverschleiß wie beim Minenarbeiter bremsen.
- **Tod:** Er lässt alles fallen, auch die Beute, wie jeder Arbeiter. Die Katze setzt sich an die Stelle, damit der Spieler die Sachen findet?
- **Was ist mit Spielern in der Höhle?** Dann greifen Skelette und Creeper den Spieler an, und der Forscher steht mitten drin. Hilft er dem Spieler?
- **Master-Fähigkeit:** Vorschlag: Er sieht Erze durch eine Blockschicht und gräbt sie frei.
- **Berufsbuch:** Wer verkauft es? Vorschlag: der Kartograf (Karten, Erkunden).

## Siehe auch

[Minenarbeiter](minenarbeiter.md) · [Wache](wache.md) · [Apotheker](apotheker.md) · [Kartograf](kartograf.md) · [Chunk-Loading](../dorf/chunk-loading.md)
