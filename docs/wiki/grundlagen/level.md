# Level und Aufstieg

[← Wiki-Start](../README.md) · Phase: **umgesetzt** (24.10.2026, ohne Meister-Fähigkeiten) · ersetzt die Effekte der [Lohnstufen](../wirtschaft/anheuern-und-lohn.md#lohnstufen)

Arbeiter steigen durch Arbeit auf, wie Vanilla-Dorfbewohner durch Handel: Neuling (Novice), Lehrling (Apprentice), Geselle (Journeyman), Experte (Expert), Meister (Master). Ein höheres Level macht einen Arbeiter **besser in dem, was er schon kann**, und **schaltet Neues frei**. Er kostet dafür mehr Lohn.

## Stand der Umsetzung

Umgesetzt wie unten beschrieben, mit diesen Einzelheiten und Abweichungen:

- **Erfahrung:** 1 Punkt je 20 Sekunden Arbeit (`levels.secondsPerExperience`), in die Vanilla-Erfahrung; Aufstieg über `Villager.increaseMerchantCareer` (neue Angebote, grüne Partikel, Jubel). Was nicht zählt, legt der Status fest (`WorkerStatus`): alle Probleme und die Wartezustände wie „Vorrat ist voll“, „Wartet, bis die Feldfrüchte wachsen“, „Gerbt …“, „Tagespensum geschafft“.
- **Lehrer:** Ein angeheuerter Meister desselben Berufs bis 16 Blöcke entfernt (`levels.mentorRadius`) lässt **alle unter Meister** 1,5-mal so schnell lernen (`levels.mentorFactor`), nicht nur den Neuling.
- **Berufswechsel:** Das Level gehört zum Beruf (gespeichert als `WorkingVillagersCareer`); wer einen Beruf einer anderen Handelsfamilie annimmt, fängt als Neuling an. Wer seine Station verliert, wird arbeitslos und behält sein Level (Vanilla täte das nur für Dorfbewohner ohne Erfahrung).
- **Tempo:** Dauer je Arbeitsschritt bis 62,5 % (`levels.masterTime`) bei allen Berufen: Hacken, Graben, Sägen, Bauen, Pfeile, Brauen, Gerben, Schleifen, Reparieren, Verzaubern, Ernten, Scheren, Schlachten, Handeln, Schießen. Schmied und Brenner holen bis zur doppelten Menge pro Gang.
- **Werkzeug:** bis 40 % Chance (`levels.masterToolSaving`), dass ein Einsatz keine Haltbarkeit kostet (Axt, Hacke, Schaufel, Spitzhacke, Schere, Waffe, Angel, Bogen).
- **Arbeitsbereich:** einfacher als unten vorgeschlagen: Die bisherigen Config-Werte sind die des Meisters, der Neuling bekommt einen Anteil davon (`levels.noviceArea`, 50 %), dazwischen gleichmäßig. Der Wachradius der Wache bleibt, wie er an der Zielscheibe eingestellt ist; es wächst der Jagdradius. Der Träger kann weiter drei Wege bis zur vollen Länge anlegen, bedient aber nur die, die sein Level schon erlaubt.
- **Freischaltungen:** Item-Tags `workingvillagers:unlocks/<beruf>/<level>` (Produkte und Materialien, z. B. `unlocks/woodcutter/journeyman` mit Schildern, `unlocks/weaponsmith/expert` mit Diamant) und feste Fähigkeiten (`Unlocks.Ability`). Ein Rezept ist gesperrt, wenn sein Ergebnis oder eine Zutat in einem Tag eines höheren Levels steht. Der Steinsäger richtet sich nach dem Material, der Apotheker nach den Reagenzien auf dem Rezeptweg. Nicht gesperrt: Färben beim Gerber (die Lederrüstung kommt ohnehin erst mit dem Lehrling), Stufe 3 des Tischlers (gibt es noch nicht), Pflügen und der Imker (noch nicht umgesetzt).
- **Lohn:** `workers.dailyWage` ist der Lohn des Neulings, `workers.masterDailyWage` (3) der des Meisters, dazwischen abgerundet: 1, 1, 2, 2, 3.
- **Menü:** Reiter Arbeiter: „Name · Geselle“, darunter ein grüner Erfahrungsbalken; der Tooltip zeigt Erfahrung, was die nächste Stufe bringt und wie man aufsteigt. In den Auftragsrastern sind Produkte, die der Arbeiter noch nicht kann, ausgegraut, mit „Ab Geselle“ im Tooltip (grün, sobald er es kann). Der Status lautet „Wartet: das kann er erst als Geselle“.
- **Abschaltbar:** `levels.unlocks` aus: alle können alles.
- **Meister-Fähigkeiten** stehen weiter auf den Berufsseiten und sind noch nicht umgesetzt.
- **Tests:** Aufstieg bei 10 und 70 Erfahrung; ein Säger bekommt beim Arbeiten Erfahrung, beim Warten keine; neben einem Meister lernt ein Neuling 1,5-mal so schnell; ein Neuling sägt keine Türen und sagt warum, als Lehrling schon; Lohn 1, 1, 2, 2, 3; ein Geselle, der Holzfäller wird, ist wieder Neuling. Die Tests mit Jagen, Boot, Räuchern, Züchten, Seitenstollen, Treppe, Erzadern, Entzaubern, verzauberter Reparatur, Lederrüstung, Bündeln und Stufe 30 setzen das nötige Level.

## Grundsätze

- **Zahlenwerte wachsen gleichmäßig.** Tempo, Werkzeugabnutzung und Arbeitsbereich wachsen mit jedem Level ein Stück, nicht sprunghaft auf einer Stufe. So rechnen heute schon Holzfäller und Grubenarbeiter.
- **Freischaltungen kommen in Stufen.** Rezepte, Arbeitsweisen und die Meister-Fähigkeit hängen an einem bestimmten Level.
- **Der Neuling hält das Dorf am Laufen.** Was die Kreisläufe brauchen (Bretter, Pfeile, Eisen reparieren, Heiltrank …), kann schon der Neuling. Ein höheres Level macht mehr möglich, verhindert aber nie die Grundversorgung.
- **Abschaltbar.** Config `levels.unlocks` (an): aus heißt, jeder Arbeiter kann alles, nur die Zahlenwerte wachsen noch.

## Aufstieg durch Arbeit

### Erfahrung

- Ein Arbeiter bekommt **1 Erfahrungspunkt je 20 Sekunden, in denen er arbeitet** (Config `levels.secondsPerExperience`).
- **Arbeiten** heißt: Er hat eine Arbeit und wartet nicht. Laufen zum Holen und Abliefern zählt, denn das gehört zur Arbeit. Kein Punkt gibt es für „Wartet auf …“, „Vorrat ist voll“, „Kein Auftrag“, „Kommt nicht an …“, für Pausen außerhalb der Arbeitszeit und ohne Lohn.
- Die Punkte landen in der Vanilla-Erfahrung des Dorfbewohners (`VillagerData`, dieselbe wie beim Handel). **Handel zählt also weiter dazu**, und Arbeiter mit Vanilla-Gegenstück bekommen beim Aufstieg ihre neuen Angebote wie in Vanilla.
- **Warum Zeit statt Stückzahl?** Ein gefällter Baum, ein Brett und ein Trank dauern sehr unterschiedlich lang. Mit Zeit braucht es keine Werte je Beruf, und kein Beruf steigt schneller auf als ein anderer. Ein schnellerer Meister bekommt auch nicht mehr Erfahrung als ein Neuling: Der Aufstieg schaukelt sich nicht auf.

### Schwellen

Die Vanilla-Schwellen bleiben:

| Level | Erfahrung | bei voller Arbeit (etwa 17 Punkte am Tag) |
| --- | --- | --- |
| Neuling | 0 | – |
| Lehrling | 10 | nach dem 1. Tag |
| Geselle | 70 | nach 4 bis 5 Tagen |
| Experte | 150 | nach 9 Tagen |
| Meister | 250 | nach 15 Tagen (etwa 5 Stunden Spielzeit) |

Die Arbeitszeit der Dorfbewohner ist etwa 350 Sekunden am Tag (Tick 2000 bis 9000). Wer viel wartet, braucht entsprechend länger.

### Wie der Spieler es sieht

- Beim Aufstieg wie in Vanilla: kurz grüne Partikel und der Aufstiegston.
- Reiter **Arbeiter** der Stationsübersicht: Level, Erfahrungsbalken und eine Zeile „Als Geselle: Türen, Falltüren, Zauntore“ mit dem, was die nächste Stufe bringt.
- Was ein Arbeiter noch nicht kann, steht in der Auftragsauswahl grau mit dem Tooltip „ab Geselle“. Wählt der Spieler es trotzdem, bleibt der Auftrag stehen, und der Arbeiter meldet „Wartet: kann er erst als Geselle“.

### Berufswechsel und Kündigung

- Das Level gehört zum Beruf. Wer seine Station verliert und eine andere des gleichen Berufs (bzw. der gleichen Handelsfamilie) bekommt, behält es. So wird es heute schon mit den Angeboten gemacht.
- Wechselt er den Beruf, fängt er als Neuling an. Offene Frage: einen Teil behalten (siehe [Ideensammlung](../planung/ideensammlung.md))?

## Was mit dem Level wächst

Jeder Wert hat in der Config einen Wert für den Neuling und einen für den Meister; dazwischen wird gleichmäßig gerechnet (Lehrling ein Viertel, Geselle die Hälfte, Experte drei Viertel des Wegs).

| Wert | Neuling → Meister | Anmerkung |
| --- | --- | --- |
| **Tempo** | Dauer je Arbeitsschritt 100 % → 62,5 % | So rechnen Holzfäller und Grubenarbeiter heute schon (`MASTER_SHARE`). An Öfen (Schmied, Brenner, Metzger) gibt der Ofen das Tempo vor; dort wächst stattdessen die Menge pro Gang (16 → 32). |
| **Werkzeugabnutzung** | 0 % → 40 % Chance, keine Haltbarkeit zu verlieren | Wirkt zusätzlich zu Haltbarkeit (Unbreaking). |
| **Arbeitsbereich** | siehe Tabelle unten | Das Level gibt die **Obergrenze** vor; der Spieler kann den Bereich kleiner einstellen, wie heute die Grube am Grubenpflock. |

### Arbeitsbereich je Beruf

Mit den Standardwerten (Meister: der Config-Wert, Neuling: die Hälfte):

| Beruf | Wert (Config) | Neuling | Meister |
| --- | --- | --- | --- |
| [Holzfäller](../berufe/holzfaeller.md) | Suchradius (`lumberjack.searchRadius`) | 12 | 24 |
| [Bauer](../berufe/farmer.md) | Feldradius (`farmer.fieldRadius`) | 4 | 8 |
| [Hirte](../berufe/hirte.md), [Metzger](../berufe/metzger.md) | Weideradius | 6 | 12 |
| [Fischer](../berufe/fischer.md) | Ufer- und Meeresradius | 8 / 16 | 16 / 32 |
| [Grubenarbeiter](../berufe/grubenarbeiter.md) | Fläche und Tiefe der Grube | 8×8 | 16×16 (eigene Werte) |
| [Minenarbeiter](../berufe/minenarbeiter.md) | Stollen- und Abzweiglänge | 16 / 8 | 32 / 16 |
| [Wache](../berufe/wache.md) | Jagdradius | 16 | 32 |
| [Apotheker](../berufe/apotheker.md) | Heilradius | 16 | 32 |
| [Träger](../berufe/traeger.md) | Weglänge | 16 | 32 |
| [Handelsangestellter](../berufe/handelsangestellter.md) | Handelsradius | 12 | 24 |

Fest bleiben die Abstände, die nur das Einrichten betreffen: zu Dorftruhen, Wasser oder dem Zaubertisch.

## Was das Level freischaltet

Neuling ist überall „was es heute schon gibt, soweit es die Grundversorgung braucht“. Die Meister-Fähigkeit steht wie bisher auf der jeweiligen Berufsseite.

| Beruf | Lehrling | Geselle | Experte | Meister |
| --- | --- | --- | --- | --- |
| [Holzfäller](../berufe/holzfaeller.md) | – | große Bäume (2×2 Fichte, Dschungel, Schwarzeiche) | – | Meister-Fähigkeit |
| [Holzsäger](../berufe/holzsaeger.md) | Zauntore, Türen, Falltüren | Druckplatten, Knöpfe, Schilder | – | zwei Erzeugnisse im Wechsel |
| [Steinsäger](../berufe/steinsaeger.md) | Tiefenschiefer, Tuffstein, Kupfer | Schwarzstein, Nether-Ziegel, Quarz | Prismarin, Purpur, Endstein | zwei Erzeugnisse im Wechsel |
| [Tischler](../berufe/tischler.md) | Stufe 2 (Holz mit Zusatz) | Stufe 3 (Workstations, sobald es sie gibt) | – | Zwischenprodukte selbst machen |
| [Waffenschmied](../berufe/waffenschmied.md) | Gold | Entzaubern | Diamant | 10 % Bonus beim Zusammenschleifen |
| [Schmied](../berufe/schmied.md) | – | Gold und Diamant reparieren, Verzaubertes mit Fläschchen | Netherit reparieren | Meister-Fähigkeit |
| [Pfeilmacher](../berufe/pfeilmacher.md) | Armbrust | Spektralpfeile | getränkte Pfeile (später) | – |
| [Apotheker](../berufe/apotheker.md) | – | Tränke mit Redstone, Glowstone, fermentiertem Spinnenauge | Wurftränke | verlängert/verstärkt ohne Zutat |
| [Bibliothekar](../berufe/bibliothekar.md) | höchste Stufe 15 | 20 | 25 | 30, ein Fläschchen weniger |
| [Gerber](../berufe/gerber.md) | Lederrüstung | Sattel, Bündel, Pferderüstung | – | Farben mischen |
| [Brenner](../berufe/brenner.md) | Terrakotta | glasierte Terrakotta, Farbstoffe | – | sparsames Feuern |
| [Bauer](../berufe/farmer.md) | – | Pflügen (später) | – | Knochenmehl, +1 Ertrag |
| [Hirte](../berufe/hirte.md) | Züchten | – | – | Schafe färben, Banner |
| [Metzger](../berufe/metzger.md) | – | Räuchern | – | mehr Fleisch |
| [Fischer](../berufe/fischer.md) | – | Boot | – | mehr Glück |
| [Minenarbeiter](../berufe/minenarbeiter.md) | Treppe | Abzweige | folgt Erzen | Fortune I |
| [Wache](../berufe/wache.md) | – | Jagen | – | – |
| [Träger](../berufe/traeger.md) | 2. Weg | 3. Weg | – | Lasttier |
| [Imker](../berufe/imker.md) | Kerzen, Honigblöcke | – | – | Bienen vermehren |
| [Grubenarbeiter](../berufe/grubenarbeiter.md), [Handelsangestellter](../berufe/handelsangestellter.md) | – | – | – | Meister-Fähigkeit |

Neuling bedeutet also zum Beispiel: Der Holzsäger macht Bretter, Stufen, Treppen, Zäune und Stöcke; der Waffenschmied Stein- und Eisenwerkzeug und schleift zusammen; der Schmied repariert Unverzaubertes aus Eisen, Stein, Holz und Leder; der Pfeilmacher macht Pfeile und Bögen; der Apotheker Glasflaschen, Wasser und einfache Tränke wie Heilung; die Wache bewacht.

## Lohn

Der Lohn steigt mit dem Level, damit ein Meister nicht einfach nur besser ist. Tageslohn: Neuling und Lehrling 1 Emerald, Geselle und Experte 2, Meister 3 (`workers.dailyWage`, `workers.masterDailyWage`). Die Wochenwerte der [Lohnstufen](../wirtschaft/anheuern-und-lohn.md#lohnstufen) gelten erst mit Wochenrhythmus und Dorfkasse.

## Technik

- Eine Hilfsklasse für alle: Fortschritt 0 (Neuling) bis 1 (Meister) aus `VillagerData.level()`, dazu „zwischen Neuling- und Meisterwert“ und „ab Level x“. `Digging`, `ChopTrees` und `PitLimits` rechnen das heute je für sich; sie ziehen um.
- Die Erfahrung zählt die gemeinsame Arbeitsschleife: Jede Sekunde ohne „Wartet“-Status zählt mit, alle 20 Sekunden ein Punkt. Aufstieg über den Vanilla-Weg (`increaseMerchantCareer`), damit Angebote und Partikel stimmen.
- Werkzeugabnutzung an einer Stelle: die Aufrufe von `hurtAndBreak` in den Jobs gehen über einen gemeinsamen Helfer.
- Freischaltungen je Beruf als Item-Tags je Level (`workingvillagers:unlocks/<beruf>/<level>`), damit Datapacks sie ändern können, wie `joinery` und `forgeable`.
- **Bestehende Welten:** Wer schon gehandelt hat, behält sein Level. Alle anderen sind heute Neulinge; nach dem Update fehlt ihnen, was erst ein höheres Level freischaltet (z. B. jagende Wachen, Fischer im Boot). Sie arbeiten weiter mit „Wartet: kann er erst als Geselle“, bis sie aufsteigen, oder der Spieler schaltet `levels.unlocks` ab.

## Offene Fragen

- Entschieden: 15 Tage bis zum Meister passen; neben einem Meister lernt man 1,5-mal so schnell.
- Soll ein Beruf mit Freischaltungen an fast jeder Stufe (Steinsäger, Minenarbeiter) welche verlieren, damit der Neuling nicht zu wenig kann?

## Siehe auch

[Anheuern & Lohn](../wirtschaft/anheuern-und-lohn.md) · [Berufe](../berufe/README.md) · [Steuerung](steuerung.md) · [Roadmap](../planung/roadmap.md)
