# Beruf: Minenarbeiter

[← Berufe](README.md) · Station: **Grubenwerkbank** (neu) · Phase: **umgesetzt** (Stollen, Abzweige, Treppe; ohne Level, Etagenmine und Logistiker)

Der Minenarbeiter baut Stein und Erze in einem festgelegten Muster ab und bringt sie zur Zielkiste. Er gräbt nur, was ihm zugewiesen ist, und sichert seinen Stollen selbst.

## Stand der Umsetzung

Umgesetzt nach dem [Umsetzungsplan](#umsetzungsplan), Schritte 0 bis 3, am 15.10.2026.

- **Grubenwerkbank:** Steinbank mit Mühlstein und Kurbel, Rezept Glatter Stein · Spitzhacke · Glatter Stein / Bruchstein · Eisenbarren · Bruchstein / 3 Bruchstein. Für Spieler ist sie eine **Gesteinsmühle** (Menü wie die Steinsäge, Rezepttyp `workingvillagers:milling`): Bruchstein, Bruchtiefenschiefer, Stein, Andesit, Diorit, Granit, Tuff → Kies; Kies → Sand oder Feuerstein; Sandstein (alle Sorten) → 4 Sand, roter Sandstein → 4 roter Sand. Der Knopf **i** öffnet die Stationsübersicht.
- **Stollen wählen:** Knopf in der Übersicht, dann Rechtsklick auf die Seite eines Blocks (höchstens Kistenreichweite, 16, von der Werkbank). Oben oder unten anklicken gibt eine Meldung. Neu wählen behält Länge, Abzweige und Art.
- **Übersicht:** „Stollen nach Süden“, **Länge − / +** (1 bis `miner.maxTunnelLength`, 32), Umschalter **Stollen / Treppe**, **Abzweige − / +** (0, 4, 8, … bis `miner.maxBranchLength`, 16), „Heute gegraben x / 256“.
- **Stollen:** 3 breit, 3 hoch. Stütze jeden 4. Block (Abschnitt 4, 8, …): Zäune an beiden Wänden unten und Mitte, drei Bretter unter der Decke. Fackel an der Wand jeden 8. Block (Abschnitt 7, 15, …). **Abzweige** 1 breit, 2 hoch an den Abschnitten 2, 6, 10, … links und rechts, dazwischen bleiben 3 Blöcke Gestein; erst beide Abzweige, dann geht es geradeaus weiter. **Treppe** 1 breit, 3 hoch, je Block einen tiefer.
- **Ablauf** (`DigTunnel`): Abschnitt für Abschnitt. Erst Wasser und Lava in Hülle, Zellen und Boden mit Bruchstein abdichten, dann die Zellen von oben nach unten graben, dann Löcher im Boden schließen. Hinter der Front: Löcher im Boden, Erze, fehlende Stützen und Fackeln. Er steht nur in fertigen Abschnitten oder vor dem Stollen.
- **Erze:** alles aus `workingvillagers:miner_ores` (`#minecraft:ores`, `#c:ores`), das an fertige Zellen grenzt, und die Erze dahinter (höchstens 64 auf einmal vorgemerkt), soweit er sie aus dem Stollen erreicht (4,5 Blöcke ab Augenhöhe). Erz neben Flüssigkeit bleibt.
- **Blockiert:** Blöcke außerhalb von `workingvillagers:mine_diggable` (Bretter, Schienen, Truhen, Ziegel …) mit „Stollen blockiert bei x y z“; zu schwache Spitzhacke mit „braucht eine bessere Spitzhacke“; kein Bruchstein zum Abdichten mit „braucht Bruchstein“. Fackeln im Weg nimmt er mit.
- **Vorräte:** Spitzhacke (Pflicht, bis 2), Zäune und Bretter (je bis 16), Fackeln (bis 16), Bruchstein (behält 16 vom Aushub). Alles auch aus Dorftruhen. Eine Schaufel nutzt er nicht.
- **Zeit:** `miner.digSlowdown` 8-mal Spielerzeit mit derselben Spitzhacke, mindestens `miner.minSecondsPerBlock` 1,5 Sekunden; Tagespensum `miner.dailyBlocks` 256.
- **Abliefern** wie beim Grubenarbeiter; Bruchstein über 16 geht in die Kiste.
- Beruf `workingvillagers:miner`, Kleidung: graublaue Arbeitsjacke, Lederhelm mit Grubenlampe.
- **Tests:** 3×3-Stollen mit Stützen und Fackel, Abzweige, Treppe, Eisenerz in der Wand und dahinter, Lava wird abgedichtet, Bretter blockieren, Mühlenrezepte. Schritt 0 (gemeinsamer Helfer `Digging`) ohne Verhaltensänderung: die Grubenarbeiter-Tests sind unverändert grün.
- **Nicht umgesetzt:** Level und Master-Fähigkeit, Etagenmine, Depots und Logistiker, „Wartet: Weg zu weit“ (die Länge ist begrenzt). Ungetestet im Spiel: Optik der Werkbank, Menüs, Wegfindung in langen Abzweigen.

## Umsetzungsplan

Stand 15.10.2026. Der Minenarbeiter kommt in Schritten. Jeder Schritt ist für sich spielbar und wird mit Gametests abgesichert. Die Etagenmine bleibt das Ziel, braucht aber [Dorfkauf](../dorf/dorf-und-kauf.md), [Chunk-Loading](../dorf/chunk-loading.md) und den [Grubenlogistiker](grubenlogistiker.md) und kommt deshalb zuletzt.

### Entscheidungen für die erste Version

| Frage | Entscheidung | Warum |
| --- | --- | --- |
| Erster Modus | **Stollen** (gerader Gang), kein Steinbruch | Den Steinbruch deckt der [Grubenarbeiter](grubenarbeiter.md) schon ab. Der Stollen bringt Erz, die Grube nur Aushub. |
| Station | eigene **Grubenwerkbank** (neuer Block, wie Grubenpflock und Hackklotz nur für Angeheuerte) | Grindstone und Schmiedetisch gehören Vanilla-Berufen. Eine Spielerfunktion ist für V1 nicht nötig. |
| Wo der Stollen anfängt | Im Menü **Stollen wählen**, dann Rechtsklick auf die Seite eines Blocks (Hang, Grubenwand). Der angeklickte Block ist die Mitte der untersten Reihe des ersten Abschnitts, der Stollen geht von dort waagerecht in den Block hinein. | Entschieden am 15.10.2026. Wie Grube und Kisten wählen: Ort und Richtung mit einem Klick. Der Startblock muss in Kistenreichweite der Station liegen. |
| Querschnitt | **Hauptgang 3 breit, 3 hoch, mit Stützen** wie im Vanilla-Minenschacht; Abzweige (Schritt 2) 1 breit, 2 hoch | Entschieden am 15.10.2026: gleich die richtige Optik. Im breiten Gang laufen Dorfbewohner auch sicherer als im 1×2-Gang. |
| Länge | 1 bis `miner.maxTunnelLength` (32), im Menü einstellbar wie die Tiefe der Grube | Er läuft jede Ladung selbst zur Zielkiste. Längere Stollen erst mit Depots (Logistiker). |
| Erze | Erze in Wand, Decke und Boden des Stollens baut er mit ab, auch die daran hängenden der Ader, soweit er sie vom Stollen aus erreicht | Bringt den eigentlichen Wert. Löcher tief in die Wand gräbt er nicht. |
| Werkzeug | Spitzhacke Pflicht, Schaufel optional (Erde, Kies), Fackeln optional | Wie beim Grubenarbeiter, dieselben Versorgungsregeln und [Dorftruhen](../dorf/dorftruhen.md). |
| Holz für Stützen | Vorräte **Zäune** (bis 16) und **Bretter** (bis 16), aus dem Inventar oder Dorftruhen. Ohne Holz gräbt er weiter und setzt die Stützen nach, wie der Grubenarbeiter seine Leitern. | Stützen sind Optik, sie sollen den Abbau nicht blockieren. Config `miner.supports` schaltet sie ab. |
| Abdichten | Bruchstein oder Bruchtiefenschiefer aus dem eigenen Aushub, er behält bis zu 16 (`miner.keepSealBlocks`) | Kein extra Nachschub nötig, er hat ihn immer dabei. |

### Schritt 0: Gemeinsames Graben herauslösen

Der [Grubenarbeiter](grubenarbeiter.md) kann schon fast alles, was der Minenarbeiter braucht. Aus `DigPit` wird ein Helfer `job/Digging` mit:

- Grabzeit (`digTicks`, Verlangsamung je Level), Werkzeug wählen (`findTool`), Verschleiß (`wearTool`)
- Drops wie beim Spieler (`drops`), passt die Ausbeute ins Inventar (`fits`), Abliefern in die Zielkisten
- Fackel setzen (Abstand, Wand oder Boden), Regeln „neben Flüssigkeit nicht graben“ und „Werkzeugstufe zu niedrig“

Der Grubenarbeiter nutzt danach den Helfer; seine Gametests müssen unverändert grün bleiben. Kein neues Verhalten in diesem Schritt.

### Schritt 1: Grubenwerkbank und Stollen (spielbar)

**Block und Beruf**
- Grubenwerkbank: Werkbank mit Spitzhacke und Eisenbeschlägen. Rezept: Spitzhacke (beliebig) über Eisenbarren über zwei Bruchstein-Stufen, Vorschlag zum Testen. POI mit einem Platz, Beruf `workingvillagers:miner` mit eigener Kleidung (dunkle Jacke, Helm mit Grubenlampe, über `generate_textures.py`).
- `StationType.MINING_BENCH`, Rechtsklick öffnet die Übersicht wie beim Grubenpflock: Status, Lohn und Werkzeug, Zielkisten, **Stollen wählen**, **Länge − / +**, „Stollen: 14 von 32“, „Heute abgebaut x / 256“.
- Der Stollen wird bei der Station gespeichert (Startblock, Richtung, Länge), wie die Grube in `Workstations`.

**Arbeitsablauf (`job/DigTunnel`)**
1. Lohn und Werkzeug wie alle Arbeiter (`WorkerSupplies.busy` mit `PICKAXE`, holt auch aus Dorftruhen).
2. Zum Stollenende laufen. Ein Abschnitt ist eine 3×3-Scheibe quer zum Gang. Er gräbt sie von oben nach unten und von der Mitte nach außen, damit Kies und Sand nicht auf ihn fallen. Er steht immer im fertigen Teil, nie im Leeren.
3. Vor dem Graben prüfen:
   - Grenzt der Block oder ein Nachbar des neuen Abschnitts an Wasser oder Lava, setzt er erst Bruchstein auf die Flüssigkeit, dann gräbt er. Ohne Bruchstein: „Wartet: Flüssigkeit im Stollen“.
   - Fehlt unter dem neuen Abschnitt der Boden (Höhle, Schlucht), setzt er einen Bruchstein als Boden, höchstens einen pro Abschnitt. Ist die Lücke tiefer als 1 oder größer, endet der Stollen hier: „Stollen endet an einer Höhle“.
   - Block nicht in `workingvillagers:mine_diggable` (Bretter, Schienen eines Vanilla-Minenschachts, Truhen, Spawner): „Wartet: Stollen blockiert bei x y z“.
4. Nach jedem Abschnitt: Erze sammeln. Alle Erzblöcke (`workingvillagers:miner_ores`: die Vanilla-Erz-Tags und `#c:ores`), die von einem Standplatz im Stollen aus in Reichweite (4,5 Blöcke) liegen und an Luft grenzen, und was nach ihrem Abbau neu freiliegt, bis `miner.maxVein` (24) Blöcke.
5. Alle 4 Abschnitte eine Stütze: zwei Zaunpfosten an den Wänden, darüber drei Bretter unter der Decke (wie im Vanilla-Minenschacht), eine Fackel an jeden zweiten Pfosten. Fehlt Holz, setzt er sie später nach. Mit `miner.supports = false` hängt er die Fackeln alle 8 Blöcke direkt an die Wand.
6. Nachrutschender Kies und Sand: Er gräbt ihn wieder weg, bevor er weitermacht.
7. Abliefern wie der Grubenarbeiter: wenn die nächste Ausbeute nicht mehr passt, nach `miner.dailyBlocks` (256) oder wenn der Stollen fertig ist. Er behält Lohn, Werkzeug, Fackeln und bis zu 16 Bruchstein.
8. Stollen fertig: „Stollen fertig“. Mit **Länge +** geht es weiter.

**Was er gräbt:** `workingvillagers:mine_diggable` = Stein und Sorten (auch Tiefenschiefer, Tuff, Kalzit), Erde, Kies, Sand, Ton, Erze, Höhlenpflanzen. Per Datapack änderbar, wie `pit_diggable`.

**Gametests**
- Stollen 10 Blöcke in einem Steinblock: der Gang ist 3×3, zwei Stützen mit Fackeln, Bruchstein in der Zielkiste.
- Ohne Bretter und Zäune: der Gang wird trotzdem fertig, die Stützen fehlen; mit Holz aus einer Dorftruhe setzt er sie nach.
- Eisenerz in der Wand und eins dahinter: beide abgebaut, Raw Iron in der Kiste.
- Lava neben dem Stollen: abgedichtet, kein Feuer, Arbeiter lebt.
- Bretter im Weg: Stollen blockiert, Bretter stehen noch.
- Ohne Spitzhacke kein Abbau; mit Dorftruhe holt er sich eine.

**Doku:** Spieler-Wiki (neue Seite `workers/miner.md`, Blöcke, Config), README, Status in `minenarbeiter.md`.

### Schritt 2: Abzweige (Branch Mining)

- Im Menü **Abzweige: aus / alle 3 Blöcke**, **Abzweiglänge** (4 bis 16).
- Nach jedem dritten Abschnitt des Hauptstollens gräbt er links und rechts je einen 1×2-Abzweig der eingestellten Länge (ohne Stützen, Fackeln alle 8 Blöcke), dann weiter geradeaus. Gleiche Regeln wie im Hauptgang.
- Das ist das klassische Branch Mining. Es legt fast alle Erze frei, ohne alles abzubauen.
- Gametest: Hauptstollen 9, Abzweige 4. Alle Gänge an der erwarteten Stelle.

### Schritt 3: Treppenschacht (Tiefe)

- Modus **Schacht**: eine Treppe aus dem Gestein (1 breit, je Block einer tiefer) bis zu einer Ziel-Höhe im Menü, unten ein kleiner Raum. Von dort lässt sich ein neuer Stollen wählen.
- Damit erreicht er Tiefenerze ohne Leitern. Laufen kann der Dorfbewohner auf Stufen von selbst.

### Später, mit anderen Meilensteinen

| Ausbau | Braucht |
| --- | --- |
| Längere Stollen über Depots und Träger | [Grubenlogistiker](grubenlogistiker.md) Stufe 1 |
| Schiene mit Kistenlore | Grubenlogistiker Stufe 2 |
| Etagenmine mit Füllorten und Förderkorb | Grubenlogistiker Stufe 3, [Chunk-Loading](../dorf/chunk-loading.md) |
| Level, Master-Fähigkeit (Fortune I) | Level-System aus M2 |
| Stollen weit weg als Außenposten mit Bett | Chunk-Loading, Dorfkauf |

### Risiken

- **Wege im Stollen:** Im 3×3-Hauptgang kein Problem. Dorfbewohner sind 1,95 hoch und passen auch in die 2 hohen Abzweige, in deren Ecken bleiben sie aber eher hängen. Die bestehende Rückholung (`workers.stuckSeconds`) fängt das auf, sollte aber selten nötig sein. Früh im Spiel testen.
- **Ungeladene Chunks:** Ein Stollen von 32 Blöcken reicht über 2 bis 3 Chunks. Ist der Spieler weit weg, steht die Arbeit. Das ist wie bei allen Stationen, bis Chunk-Loading kommt.
- **Mobs im Stollen:** Fackeln alle 8 Blöcke halten den Gang hell. Höhlen, die er anschneidet, bleiben dunkel. Ein Zombie im Stollen ist ein echtes Risiko für den Arbeiter. Das ist gewollt und passt zu „Wache als Begleitschutz“.
- **Erzwert:** Mit Erzadern und Branch Mining könnte ein Minenarbeiter zu viel Diamant bringen. Tagespensum, Grabzeit (×8) und Spitzhackenverschleiß bremsen. Im Playtest prüfen.

### Offene Fragen

- Soll ein zweiter Arbeiter die Mühle mit einem Auftrag bedienen, wie den Brennofen?

## Workstation: Grubenwerkbank

- Der Grindstone ist bereits POI des Weaponsmith. Eine eigene Station verhindert Konflikte: **Grubenwerkbank** (Mining Bench), z. B. Spitzhacke + Eisen + Bruchstein.
- Funktion für Spieler (entschieden am 15.10.2026): **Gesteinsmühle**. Sie mahlt Bruchstein zu Kies, Kies zu Sand und Feuerstein. So wird der Aushub der Mine zu Sand für das Glas des [Brenners](brenner.md). Verworfen: Probebohrung (nur Info), Erzwäsche (erzeugt Erz aus dem Nichts), Stützbalken, Werkzeug schärfen (Sache des Schmieds).
- Alternative: Vanilla-Blast-Furnace oder Smithing Table wären thematisch nah, sind aber belegt (offene Frage, siehe [Risiken & Fragen](../planung/risiken-und-fragen.md)).

## Abbau-Modi (im GUI wählbar)

| Modus | Was passiert | Ertrag | Freigeschaltet ab |
| --- | --- | --- | --- |
| Steinbruch | Markierten Quader schichtweise von oben abtragen | Stein, Erde, Kies, zufällige Erze | Novice |
| Stollen | Gerader 2×1- oder 3×3-Tunnel in eine Richtung, Abzweige alle n Blöcke | Stein, Erze an den Wänden | Apprentice |
| Erzader folgen | Bei Erzfund die Ader komplett abbauen | Mehr Erz | Journeyman |
| Schacht | Treppenschacht bis auf eine Ziel-Y-Höhe | Tiefenerze, Deepslate | Expert |
| **Etagenmine** | Zentralschacht mit mehreren Etagen, Hauptgänge mit Stützen, Abbaustrecken im Branch-Mining-Muster (siehe [Die Mine](../grundlagen/mine.md)) | Viel Erz auf wenig Chunks | Journeyman |

Die Etagenmine ist das Ziel für große Dörfer: Sie bleibt in einer festen Grundfläche und braucht deshalb immer gleich viele geladene Chunks. Allein schafft der Minenarbeiter 3×3 Chunks; mit [Grubenlogistiker](grubenlogistiker.md) wächst die Mine auf 5×5 und mit Lorenrouten bis 7×7 (siehe [Größe der Mine](../grundlagen/mine.md#größe-der-mine-ausbau-mit-dem-logistiker)).

## Quellkiste / Zielkiste

| Quellkiste | Zielkiste |
| --- | --- |
| Spitzhacke, Schaufel, Fackeln oder Laternen, Bruchstein zum Abdichten, Bretter und Zäune für Stützen | Stein, Erde, Kies, Erze, Kohle |

Mit [Grubenlogistiker](grubenlogistiker.md) ist Quell- und Zielkiste immer das neueste Depot am Stollenende.

## Optik

Hauptgänge sehen aus wie Vanilla-Minenschächte: Stützen aus Zaunpfosten und Brettern alle 4 Blöcke, Fackeln an den Stützen, Schienen in der Mitte, Holzart nach Biom (Schwarzeiche in den Badlands). Details: [Die Mine](../grundlagen/mine.md#optik-wie-ein-vanilla-minenschacht).

## Sicherheit & Welt

- Setzt Fackeln aus der Quellkiste in festen Abständen (verhindert Mob-Spawns im Stollen).
- Lava und Wasser werden mit Bruchstein verschlossen, nie durchbrochen.
- Kies und Sand: wartet, bis sie gefallen sind, und baut sie dann ab.
- Mobs: flieht wie Vanilla-Villager. Optional Begleitschutz durch eine [Wache](wache.md).
- Verletzt sich der Arbeiter, sinkt die Moral; stirbt er, lässt er wie alle Arbeiter alles fallen, was er bei sich trägt (Emeralds, Werkzeug, Material; so ist es schon umgesetzt). Geplant: Der Tod landet mit Ursache im [Sterberegister](../dorf/dorfbuch.md#register).
- Minen weit vom Dorf sind **Außenposten** mit eigenem Bett (siehe [Chunk-Loading](../dorf/chunk-loading.md)).

## Lange Stollen

Je tiefer der Stollen, desto weiter der Weg zur Starttruhe. Ohne Hilfe endet ein Stollen bei der Reichweite der Station (z. B. 32–48 Blöcke), danach meldet der Minenarbeiter „Wartet: Weg zu weit“. Der [Grubenlogistiker](grubenlogistiker.md) löst das: Er baut neue Depot-Truhen am Stollenende, die Minenarbeiter nutzen dann immer das neueste Depot statt der alten Truhe, [Träger](traeger.md) bringen alles nach draußen, und später fährt eine Lore auf Schienen.

## Balancing

- Kein Silk Touch, kein Fortune in V1.
- Seltene Erze (Diamant, Ancient Debris) werden nur gefunden, nicht „erzeugt“. Der Ertrag hängt also von der echten Welt ab.
- Spitzhacken-Verschleiß ist der Haupt-Kostenfaktor neben dem Lohn.
- In Bergbiomen kann Smaragderz eine direkte [Einnahmequelle](../wirtschaft/einnahmen.md) sein.

## Master-Fähigkeit

Abbau mit Fortune I.

## Siehe auch

[Die Mine](../grundlagen/mine.md) · [Grubenlogistiker](grubenlogistiker.md) · [Grubenarbeiter](grubenarbeiter.md) (einfache Grube per Schnur) · [Schmied](schmied.md) (verarbeitet das Erz) · [Logistik](../grundlagen/logistik.md)
