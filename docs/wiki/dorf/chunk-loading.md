# Betrieb ohne Spieler (Chunk-Loading)

[← Wiki-Start](../README.md)

**Stand der Umsetzung:** Umgesetzt, pro Arbeiter schaltbar (Schalter „Ohne Spieler arbeiten“). Die Kosten nimmt der Arbeiter mit seinem Lohn. Offen sind die Anbindung an [Dorfkasse](dorf-und-kauf.md#dorfkasse) und [Dorfbuch](dorfbuch.md) und eine Aufholberechnung für Server, die kein Chunk-Loading erlauben (siehe [Später](#später)).

Arbeiter arbeiten nur in Chunks, die geladen sind und ticken, also normalerweise nur in der Nähe eines Spielers. Schaltet der Arbeitgeber „Ohne Spieler arbeiten“ ein, hält der Arbeiter seine Chunks selbst geladen, wie eine geworfene Enderperle. So arbeitet ein Holzfällerlager weiter, während der Spieler in der Hauptbasis ist.

## Einschalten

- **Stationsübersicht**, Reiter „Arbeiter“: der Knopf mit der Enderperle links neben „Abziehen“.
- **Träger:** der Knopf mit der Enderperle rechts neben dem Namen im Träger-Menü (beide Reiter).
- Umschalten darf nur der **Arbeitgeber**, dazu Operatoren und der Besitzer einer Einzelspielerwelt. Alle anderen sehen den Knopf ausgegraut mit dem Zustand im Tooltip.
- Die Markierung oben rechts am Knopf zeigt den Zustand:

| Markierung | Bedeutung |
| --- | --- |
| keine | aus: Der Arbeiter arbeitet nur in Spielernähe. |
| grün | an, die Chunks sind geladen. Der Tooltip nennt die Zahl („Hält 13 Chunks geladen“). |
| gelb | an, aber pausiert: Der Arbeitgeber ist nicht online (`onlyWhileEmployerOnline`), oder der Server hat es abgeschaltet. |

- Beim Ein- und Ausschalten kommt eine kurze Meldung, beim Einschalten mit den Kosten pro Tag.
- Höchstens `maxWorkersPerPlayer` Arbeiter eines Arbeitgebers (Standard 16) dürfen gleichzeitig ohne Spieler arbeiten.

## Was geladen bleibt

### Um den Arbeiter: das mitlaufende Ticket

Der Arbeiter trägt ein Chunk-Ticket bei sich und erneuert es jede Sekunde an dem Chunk, in dem er gerade steht. Die **3×3 Chunks um ihn** ticken, ein Ring darum ist geladen, aber still (genug für die Wegfindung). Wohin er auch geht, die Chunks gehen mit: ins Bett, zur Dorftruhe, den Stollen entlang, die Wege eines Trägers ab. Stirbt er oder wird er entlassen, läuft das Ticket nach 5 Sekunden von selbst ab. Es bleiben keine Tickets übrig.

### Der Arbeitsplatz

Mit `loadWorkplace` (Standard an) bleibt auch der Arbeitsplatz geladen, während der Arbeiter woanders ist, etwa nachts im Bett. Sonst stünden Öfen still, und Setzlinge wüchsen nur, wenn er gerade daneben steht.

| Arbeiter | Was zum Arbeitsplatz gehört |
| --- | --- |
| alle mit Station | der Chunk der Station und die Chunks der verknüpften Kisten |
| [Holzfäller](../berufe/holzfaeller.md) | dazu der Umkreis `lumberjack.searchRadius` |
| [Bauer](../berufe/farmer.md) | dazu der Umkreis `farmer.fieldRadius` |
| [Hirte](../berufe/hirte.md), [Metzger](../berufe/metzger.md) | dazu die Weide (`shepherd.pastureRadius`, `butcher.pastureRadius`) |
| [Grubenarbeiter](../berufe/grubenarbeiter.md) | dazu die Grube mit dem ganzen Schnur-Ring (er gräbt nur, solange der Ring geladen ist) |
| [Minenarbeiter](../berufe/minenarbeiter.md) | dazu der Stollen mit den Abzweigen bis zum Ende |
| [Träger](../berufe/traeger.md) | die Quell- und Zielbehälter aller Wege |

Der Arbeitsplatz zählt höchstens 25 Chunks, die nächsten an der Station zuerst. Jeder davon tickt einzeln.

### Was nicht geladen wird

- **Gewöhnliche Dorfbewohner** haben kein Ticket. Kunden des [Handelsangestellten](../berufe/handelsangestellter.md) stehen still, solange kein Spieler in der Nähe ist. Der Verkauf läuft dann nur mit Kunden, die im geladenen Bereich wohnen.
- **Mobs spawnen nur in Spielernähe** (Vanilla prüft dafür 128 Blöcke Abstand zu einem Spieler). Ohne Spieler sind Arbeiter also sicherer als mit ihm, und die [Wache](../berufe/wache.md) hat nachts nichts zu tun. Das ist akzeptabel.

## Random Ticks: kein Haken mehr

Das alte Konzept nahm an, dass Setzlinge und Felder in zwangsgeladenen Chunks ohne Spieler nicht wachsen, und plante dafür eigene Wachstums-Ticks. In der Zielversion (26.3) stimmt das nicht: Vanilla gibt jedem tickenden Chunk seine Random Ticks, mit oder ohne Spieler (`ChunkMap.forEachBlockTickingChunk`). Eigene Wachstums-Ticks sind deshalb nicht nötig. Nur das Mob-Spawning hängt an Spielern (siehe oben).

## Kosten

- **`dailyFee` Smaragde pro Arbeitstag** (Standard 1) zusätzlich zum Lohn. Der Aufschlag ist Teil des Lohns: Die Lohnkasse zeigt ihn in „pro Arbeitstag“, und aus [Dorftruhen](dorftruhen.md) nimmt der Arbeiter entsprechend mehr.
- Wie der Lohn wird er nur an Tagen fällig, an denen der Arbeiter arbeitet.
- **Fehlt der Lohn länger als einen Tag**, schaltet sich „Ohne Spieler arbeiten“ ab, und der Arbeitgeber bekommt eine Nachricht. So lädt kein unbezahlter Arbeiter dauerhaft Chunks.

## Neustart und Arbeitgeber offline

Vanilla speichert die Tickets nicht (gewollt, so bleiben nach einem Absturz keine Tickets ohne Arbeiter übrig). Der Mod merkt sich für jeden Arbeiter, der ohne Spieler arbeitet: Arbeitgeber, Dimension, letzter Chunk und Name.

- Nach dem Serverstart, und wenn der Arbeitgeber wieder online kommt, lädt der Mod den letzten Chunk des Arbeiters. Ist der Arbeiter geladen, erneuert er sein Ticket wieder selbst.
- Taucht er innerhalb einer Minute nicht auf (getötet, als niemand hinsah; per Befehl versetzt), wird er vergessen. Das Server-Log nennt ihn.
- Mit `onlyWhileEmployerOnline` (Standard an) ruht alles, solange der Arbeitgeber offline ist. Der Knopf ist dann gelb.

## Server-Einstellungen

Abschnitt `chunkLoading` in `config/workingvillagers.json`:

| Option | Standard | Bereich | Bedeutung |
| --- | --- | --- | --- |
| `enabled` | true | | Aus: Der Schalter verschwindet, kein Arbeiter lädt Chunks. Eingeschaltete Arbeiter bleiben gespeichert und laden wieder, sobald es an ist. |
| `dailyFee` | 1 | 0–16 | Smaragde pro Arbeitstag zusätzlich zum Lohn |
| `maxWorkersPerPlayer` | 16 | 0–256 | wie viele Arbeiter eines Arbeitgebers gleichzeitig ohne Spieler arbeiten dürfen |
| `onlyWhileEmployerOnline` | true | | Chunks nur laden, solange der Arbeitgeber irgendwo in der Welt online ist |
| `loadWorkplace` | true | | auch den Arbeitsplatz laden, nicht nur die Chunks um den Arbeiter |

- **Server ganz ohne Spieler:** Ein dedizierter Server pausiert von selbst, wenn niemand online ist (`pause-when-empty-seconds` in `server.properties`, Standard 60). Wer will, dass Arbeiter auch dann weiterarbeiten, setzt den Wert auf 0 und `onlyWhileEmployerOnline` auf `false`.
- **Einzelspieler:** Die Welt läuft nur, solange das Spiel offen ist. Der Schalter hilft dort gegen die Entfernung, nicht gegen das Ausloggen.

## Befehl für Operatoren

- `/workingvillagers chunks` listet jeden Arbeiter, der ohne Spieler arbeitet: Name, Arbeitgeber, Dimension, Position, ob geladen und wie viele Chunks. Oben steht die Summe.
- `[aus]` hinter jedem Eintrag schreibt `/workingvillagers chunks off <UUID>` in die Eingabezeile. Der Befehl schaltet es für diesen Arbeiter ab, auch wenn er gerade nicht geladen ist.

## Technik

- Ein eigener Ticket-Typ `workingvillagers:worker`: lädt, simuliert und hält die Dimension aktiv (wie das der Enderperle), läuft nach 100 Ticks ohne Erneuerung ab und wird von Vanilla nicht gespeichert. Um den Arbeiter Radius 3 (3×3 tickend), je Chunk des Arbeitsplatzes Radius 2 (nur dieser Chunk tickt).
- Erneuert wird es in der Prüfung jedes Arbeiters (`WorkerJobs`, jede Sekunde). Der Level-Tick (`ServerLevelMixin`) lädt Arbeiter nach, die ihr Ticket länger als 3 Sekunden nicht erneuert haben.
- Die Daten (`WorkerChunks`) liegen bei der Oberwelt, für alle Dimensionen.
- Derselbe Code auf Fabric und NeoForge, nur über Vanilla-Tickets. Der `TicketController` von NeoForge ist nicht nötig.
- **Simulation:** `./gradlew :fabric:runSimulation -Pfar=true` baut das Testdorf, schaltet alle Arbeiter auf „Ohne Spieler arbeiten“, nimmt den Forceload weg und schickt den Platzhalter-Spieler 3000 Blöcke weit weg.

### Messung (27.10.2026, 2 Flügel, 88 Arbeiter, 3 Tage)

| | normal (Forceload, Spieler über dem Dorf) | `far` (nur Arbeiter-Tickets, Spieler 3000 Blöcke weg) |
| --- | --- | --- |
| Tick im Mittel / zur Arbeitszeit | 8,4 ms / 9,6 ms | 6,6 ms / 8,3 ms |
| Arbeiter am Ende | 81 (7 von Monstern getötet) | 88 (keine Monster) |
| Holzfäller, Stämme pro Tag | 154 / 115 / 126 | 144 / 113 / 113 |
| Holzsäger | 812 / 500 / 480 | 772 / 472 / 428 |
| Bauer | 77 / 109 / 158 | 69 / 173 / 204 |
| Grubenarbeiter Kies | 160 / 78 / 160 | 320 / 157 / 320 |

Die Arbeit läuft ohne Spieler im selben Umfang: Setzlinge und Felder wachsen, Öfen brennen. Ohne Spieler spawnen keine Monster, deshalb überleben mehr Arbeiter, und der Server ist schneller, weil weniger geladen ist. In beiden Läufen hatten die Träger nichts zu tragen. Das lag nicht am Chunk-Loading, sondern daran, dass die Simulation ihre Wege für Meister plante, ihre Träger aber Novizen waren. Inzwischen bekommt jeder Träger der Simulation das Level, das seine Wege brauchen.

## Später

- **Dorfkasse:** Die Kosten kommen aus der [Dorfkasse](dorf-und-kauf.md#dorfkasse) statt mit dem Lohn, das [Dorfbuch](dorfbuch.md) zeigt im Reiter „Dorf“ alle geladenen Chunks, ihre Kosten und welcher Arbeiter sie braucht.
- **Aufholberechnung** für Server, die Chunk-Loading verbieten (`enabled: false`): Beim Neuladen wird der Ertrag der verpassten Zeit grob berechnet (gedeckelt auf z. B. einen Ingame-Tag, mit Werkzeugverschleiß).
- **Inaktive Spieler:** Nach X Tagen ohne Login die Chunks ihrer Arbeiter nicht mehr laden (siehe [Multiplayer](../technik/multiplayer.md)).
- **Etagenmine und Schienen:** Die [Etagenmine](../grundlagen/mine.md#chunk-loading) braucht eine feste Grundfläche (bis 7×7), mehr als die 25 Chunks eines Arbeitsplatzes. Eine Lore auf einer langen Strecke hat kein mitlaufendes Ticket; dafür ein eigenes Ticket der Lore wie beim [Fuhrmann](../berufe/fuhrmann.md).

Siehe auch: [Technische Architektur](../technik/architektur.md#performance--chunks) · [Fuhrmann](../berufe/fuhrmann.md)
