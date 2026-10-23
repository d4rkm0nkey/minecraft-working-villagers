# Beruf: Grubenarbeiter

[← Berufe](README.md) · Station: **Grubenpflock** (neu) · Phase: **umgesetzt** (ohne Master-Fähigkeit), mit [Verständlichkeit](#verständlichkeit) und [Grenzen nach Level](#grenzen-nach-level); in Planung: [Ausbau tiefer Gruben](#ausbau-tiefer-gruben-geplant)

Der Grubenarbeiter hebt eine Grube aus, die der Spieler mit Schnur umrandet: Er gräbt alles innerhalb des Rings Schicht für Schicht bis zur eingestellten Tiefe ab, setzt dabei eine Leiter als Ausgang und bringt den Aushub in die Zielkiste. Gut für Keller, Fundamente, Sand- und Tongruben und als einfacher Steinbruch vor der [Mine](../grundlagen/mine.md).

## Stand der Umsetzung

- **Grubenpflock:** Pfosten mit aufgewickelter Schnur, Rezept Schnur über Stock über Brett. Rechtsklick öffnet die Übersicht mit Größe der Grube (oder was am Ring nicht stimmt), **Tiefe − / +**, **Leiter** und „Heute gegraben x / 256“. Schnur verbindet sich mit dem Pflock wie mit einem Haken (`TripWireBlockMixin`).
- **Ring** (`Pit.fromStake`, `Pit.attach`): Der Ring, der den Pflock berührt, ist die Grube; der Pflock darf im Ring stehen (zählt dann wie eine Schnur, gehört aber nicht zum Ring) oder direkt daneben. Übernommen wird er beim Öffnen der Übersicht (alle 10 Ticks, solange sie offen ist; bis 0.6.1 lief diese Prüfung wegen eines Überlaufs nie, die Übersicht zeigte dann nur „Noch keine Grube“) und alle 40 Ticks durch den Grubenarbeiter; eine verschobene Leiter bleibt, solange ihre Schnur zum Ring gehört. Ohne geschlossenen Ring bleibt die alte Grube, damit der Grubenarbeiter auf die Reparatur wartet. Schnüre verbinden sich gerade (N/S/O/W), auch einen Block höher oder tiefer, und zählen nur auf Boden mit Kollision (auch Trampelpfad, Ackerboden, Stufen). Innen ist, was vom Rand nicht erreichbar ist; der Rand darf dabei auch schräg gehen, eine fehlende Ecke ist also eine Lücke. Zum Ring gehören nur die Schnüre, die ans Innere grenzen. Prüfungen mit Meldung: keine Schnur am Pflock, nicht geschlossen (mit der Stelle, an der der Ring endet), zu groß (nach [Level](#grenzen-nach-level), Anfänger 64), zu klein (unter 2), zu uneben (`pitDigger.maxUnevenness`, 4). Zu jedem Problem merkt sich `Pit.Problem` die Schnüre, an denen es liegt (offenes Ende; bei zu groß/zu klein der ganze Ring; bei zu uneben höchste und tiefste Schnur), für Vorschau und Partikel. Gespeichert werden Ring, Innenfläche, Leiterplatz und die Höhe der höchsten Schnur, bei der Station in `Workstations`.
- **Tiefe** 1 bis `pitDigger.maxDepth` (16), Standard 4, ab der höchsten Schnur; nie tiefer als 5 Blöcke über der Welt-Untergrenze.
- **Graben** (`DigPit`): unter dem Rand streng Schicht für Schicht. Er gräbt nie tiefer, solange in der aktuellen Schicht noch ein Block offen ist (auch der, auf dem er steht – dafür wechselt er den Platz – oder einer, an den er gerade nicht herankommt – dann versucht er erst andere Standplätze und wartet nur, wenn keiner geht, mit „Wartet: kommt in der Grube nicht an x y z heran“). In jeder Schicht zuerst die Leiterspalte, dann vom Leiterplatz weg. Die Leiterspalte gräbt er ab der zweiten Schicht von oben im Schacht: Er steht auf dem Block, gräbt ihn unter sich weg, setzt die Leiter und rutscht nach. Kommt er von seinem Platz aus ohne Klettern an den Block der Leiterspalte heran (er steht unten im Loch daneben), gräbt er ihn von der Seite. Muss er aus der Grube und die Leiterspalte endet über ihm, gräbt er sie erst bis zu sich herunter, auch nach Feierabend. So entsteht nie ein Loch, aus dem die Leiter nicht herausführt. Hügel bis 8 Blöcke über der höchsten Schnur werden vorher von oben abgetragen; was er dort nicht erreicht, lässt er aus. Als Hügel zählt nur, was ohne Lücke auf dem Boden steht: Eine Baumkrone über der Grube bleibt hängen, darunter gräbt er weiter. Gräbt nur `workingvillagers:pit_diggable` (Erde, Gras, Sand, Kies, Ton, Schlamm, Schnee, Stein und Sorten, Erze, Laub, Pflanzen). Ein anderer Block sperrt die Spalte darunter nur, wenn er direkt aufliegt (eine Brücke mit Luft darunter nicht). Nicht gegraben: Blöcke neben Wasser/Lava, Blöcke, für die die Spitzhacke zu schwach ist, der Block, auf dem er steht (außer in der Leiterspalte, dort hält ihn die Leiter). Zeit: Spielerzeit mit dem Werkzeug × `pitDigger.digSlowdown` (8 für Anfänger, wie beim Holzfäller bis 5 für Meister), mindestens `pitDigger.minSecondsPerBlock` (2 Sekunden) pro Block, Pflanzen ein Viertel davon. Werkzeug: Schaufel oder Spitzhacke, was schneller ist; eine Nutzung pro Block. Drops wie beim Spieler mit diesem Werkzeug (Verzauberungen zählen).
- **Leiter:** Sobald ein Block der Leiterspalte gegraben ist, setzt er dort eine Leiter an die Wand; fehlende Leitern holt er nach. Die Leiterspalte ist die Innenspalte neben der Schnur, die der Station am nächsten liegt, oder die mit **Leiter** gewählte. Ab 2 Blöcken unter der Ausstiegsschnur ist die Grube „tief“: Dann steigt er nur über die Leiter hinein und heraus. Das Klettern bewegt ihn direkt (hinüber und senkrecht), weil Vanilla keine Wege über Leitern plant. Ab dieser Tiefe sind Leitern Pflicht.
- **Abliefern:** wenn die Ausbeute des nächsten Blocks nicht mehr ins Inventar passt (statt fest „6 von 8 Plätzen“: Lohn, Werkzeuge, Leitern und Fackeln belegen schon bis zu 7 Plätze), nach `pitDigger.dailyBlocks` (256) Blöcken, wenn nichts mehr zu tun ist. Alles außer Lohn, Werkzeug, Leitern und Fackeln kommt in die Zielkiste. Nach Feierabend steigt er erst aus der Grube, bevor er sein Tagwerk beendet.
- **Fackeln** (optional, `pitDigger.keepTorches`): auf der Sohle, wenn im Umkreis von 5 Blöcken keine steht. Fackeln einer früheren Sohle hebt er beim Tiefergraben wieder auf.
- **Status:** u. a. „Gräbt Schicht x von y“, „Ebnet den Boden“, „Steigt die Leiter hinunter/hinauf“, „Wartet: Schnur unterbrochen“, „Wartet: Flüssigkeit in der Grube“, „Wartet: braucht eine bessere Spitzhacke“, „Grube fertig“, „Grube fertig, einige Säulen bleiben stehen“, „Feierabend für heute“, „Wartet: braucht eine Schaufel / Spitzhacke / Leitern“.
- Profession `workingvillagers:pit_digger` mit eigener Kleidung (braune Arbeitshose mit Latz, gelber Helm).
- **Nicht umgesetzt:** Master-Fähigkeit (behutsames Graben).
- **Offene Fragen entschieden:** Gruben stapeln ist erlaubt. Kann die Wand keine Leiter halten (Luft, Sand), setzt er dort keine; das Klettern funktioniert trotzdem, für den Spieler fehlt dann eine Sprosse.

## Workstation: Grubenpflock

- Ein Holzpflock mit aufgewickelter Schnur, z. B. aus Stöcken, Brettern und Schnur. Er steht am Rand der Grube.
- Kein Vanilla-Block ist frei: Der Grindstone gehört dem Weaponsmith, die [Grubenwerkbank](minenarbeiter.md) dem Minenarbeiter. Nur angeheuerte Dorfbewohner arbeiten am Grubenpflock, wie an Hackklotz und Holzsäge.
- Für Spieler hat er keine eigene Funktion. Ein Rechtsklick öffnet die Stationsübersicht.

## Die Grube markieren

1. Der Spieler legt **Schnur** auf den Boden und schließt sie zu einem Ring. Vanilla macht daraus Stolperdraht-Blöcke, Haken sind nicht nötig.
2. Der Ring muss den Grubenpflock berühren: Der Pflock steht im Ring oder direkt daneben, die Schnur verbindet sich sichtbar mit ihm.
3. Der Mod sucht den Ring ab dem Pflock und bestimmt das Innere; grüne Partikel zeigen den übernommenen Ring. Ist der Ring nicht geschlossen, zu groß oder außerhalb der Reichweite, gibt es eine Meldung statt einer Grube.

Regeln für den Ring:

- Es zählen nur Schnüre, die sich gerade berühren (Norden, Süden, Osten, Westen). Diagonal ist eine Lücke, so wie Vanilla-Stolperdraht auch nur gerade verbindet.
- Jede Form ist erlaubt, nicht nur Rechtecke. Innen ist alles, was vom Rand aus nicht erreichbar ist, ohne eine Schnur zu kreuzen.
- Die Schnüre dürfen dem Gelände folgen, also auf unterschiedlicher Höhe liegen, aber höchstens **4 Blöcke** auseinander (Startwert, Config). Sonst kommt „Ring zu uneben“.
- Jede Schnur muss auf einem festen Block liegen. Eine Schnur über einem Loch zählt nicht.
- Der Block unter jeder Schnur bleibt stehen. Er ist der Rand der Grube, die Schnur bleibt so liegen und die Leiter hat eine Wand.
- Größe: höchstens **64 Blöcke** Grundfläche innen für einen Anfänger, z. B. 8×8 oder 4×16; mehr mit höherem Level, siehe [Grenzen nach Level](#grenzen-nach-level-geplant). Ist der Ring größer, kommt „Grube zu groß (x von 64)“.
- Keine eigene Reichweite mehr. Bisher musste jede Schnur näher als die Kistenreichweite (16) am Pflock liegen; ein 16×12-Ring direkt am Pflock scheiterte so mit 17,9 Blöcken an der fernen Ecke, ohne dass man es verstand. Der Ring muss den Pflock ohnehin berühren, und die Fläche begrenzt seine Größe.
- Wird eine Schnur entfernt, pausiert der Grubenarbeiter mit „Wartet: Schnur unterbrochen“. Sobald der Ring wieder geschlossen ist, geht es weiter; ein geänderter Ring wird automatisch übernommen.

## Tiefe

- Im Menü stellt der Spieler die **Tiefe** ein: 1 bis **x** Blöcke. Bisher Config, Startwert **16**; *geplant:* **8** für einen Anfänger, mehr mit höherem Level (siehe [Grenzen nach Level](#grenzen-nach-level-geplant)).
- Gezählt wird ab der **höchsten** Schnur des Rings: Die Sohle der Grube liegt *Tiefe* Blöcke unter dem Boden dieser Schnur. Was innerhalb des Rings höher liegt (ein Hügel), wird auch abgetragen.
- Die Sohle wird beim Wählen der Grube als feste Höhe gespeichert. Nur eine höhere Tiefe im Menü (bis zum Maximum) senkt sie, nie eine Änderung am Ring.

### Schutz gegen künstlich tiefe Gruben

Die Tiefe soll nicht davon abhängen, wo der Spieler die Schnur hinlegt. Sonst könnte er an einer Stelle des Rings ein tiefes Loch graben, unten eine Schnur hinlegen und so die Sohle beliebig weit nach unten ziehen. Bezug auf die höchste Schnur, höchstens 4 Blöcke Höhenunterschied im Ring und die beim Wählen gespeicherte Sohle verhindern das:

| Versuch | Was passiert |
| --- | --- |
| Eine Schnur in ein selbst gegrabenes Loch legen | Bezug ist die höchste Schnur, eine tiefere Schnur ändert die Sohle nicht. Ist das Loch tiefer als 4 Blöcke, ist der Ring „zu uneben“ und wird nicht angenommen. |
| Eine Schnur auf einen Turm legen | Die höchste Schnur liegt höher, die Sohle damit auch: Die Grube wird flacher, nicht tiefer. Auch hier gilt die Grenze von 4 Blöcken. |
| Den Ring nach dem Wählen tiefer legen | Die Sohle ist beim Wählen gespeichert. Liegt der Ring nicht mehr an derselben Stelle, wartet der Grubenarbeiter, bis die Grube neu gewählt wird; dann gelten die Regeln oben wieder. |
| Innerhalb des Rings vorgraben | Ändert nichts, die Sohle ist eine feste Höhe. Der Grubenarbeiter hat nur weniger zu tun. |

Die tiefste mögliche Sohle liegt damit höchstens *Maximum* Blöcke unter dem Boden an der höchsten Stelle des Rings.
- Nie tiefer als 5 Blöcke über der Welt-Untergrenze. Bedrock und andere unzerstörbare Blöcke bleiben immer stehen.

```
  Seitenansicht, Tiefe 4

   S                 S      S  Schnur auf dem Rand
  ███▓▓▓▓▓▓▓▓▓▓▓▓▓▓███      ▓  wird abgegraben
  ███▓▓▓▓▓▓▓▓▓▓▓▓▓▓███      █  bleibt stehen
  ██H▓▓▓▓▓▓▓▓▓▓▓▓▓▓███      H  Leiter an der Wand
  ██H▓▓▓▓▓▓▓▓▓▓▓▓▓▓███
  ██H▓▓▓▓▓▓▓▓▓▓▓▓▓▓███
  ████████████████████  ← Sohle
```

## Leiter als Ausgang

- Der Grubenarbeiter kommt nur über eine Leiter hinein und heraus. Er springt nicht hinunter und baut keine Treppen.
- **Leiterplatz:** Standard ist das innere Feld am Rand, das der Station am nächsten liegt. Im Menü lässt er sich mit **Leiterplatz wählen** ändern (Rechtsklick auf eine Schnur des Rings, die Leiter kommt an die Wand darunter).
- Nach jeder Schicht setzt er eine Leiter an die Wand, bevor er tiefer gräbt. Die Leiter reicht also immer bis zur aktuellen Sohle.
- Er braucht eine Leiter pro Block Tiefe. Die Leitern bleiben in der fertigen Grube, sie sind auch der Ausgang für den Spieler.

## Arbeitsablauf

1. Lohn nehmen und prüfen, ob er Schaufel, Spitzhacke und Leitern dabeihat. Der Spieler gibt sie ihm direkt, per Rechtsklick oder zuwerfen, wie heute Axt und Hacke (siehe [Anheuern & Lohn](../wirtschaft/anheuern-und-lohn.md#stand-der-umsetzung)).
2. Über die Leiter in die Grube steigen (am Anfang: zum Leiterplatz laufen).
3. Die oberste noch nicht fertige Schicht abgraben. Er nimmt immer das passende Werkzeug: Schaufel für Erde, Sand, Kies, Ton; Spitzhacke für Stein und Erze.
4. Ist die Schicht fertig: eine Leiter an den Leiterplatz setzen und eine Schicht tiefer weitermachen.
5. Ist das Inventar fast voll (6 von 8 Plätzen) oder Feierabend, steigt er hinaus und liefert ab.
6. Ist die Sohle erreicht: Status „Grube fertig“. Er hört auf, bis die Tiefe erhöht oder eine neue Grube gewählt wird.

Er gräbt nie den Block, auf dem er steht (außer in der Leiterspalte: Dort gräbt er sich nach unten, die Leiter hält ihn), und arbeitet in jeder Schicht vom Leiterplatz weg. Ein Tagespensum begrenzt den Aushub: **256 Blöcke** pro Arbeitstag (Startwert, Config, wie die 64 Stämme beim [Holzfäller](holzfaeller.md)).

## Was er dabeihat / Zielkiste

| Bekommt er direkt vom Spieler | Zielkiste |
| --- | --- |
| Emeralds, Schaufel, Spitzhacke, Leitern, optional Fackeln | Erde, Sand, Kies, Ton, Bruchstein, Erze, Feuerstein |

- Schaufel, Spitzhacke und Leitern sind Pflicht. Fehlt etwas, wartet er mit „Wartet: braucht eine Schaufel“ bzw. „... eine Spitzhacke“ oder „... Leitern“.
- Eine Quellkiste braucht er nicht, er liefert nur ab (wie der Holzfäller am Hackklotz).
- **Der ganze Aushub kommt in die Zielkiste**, er wirft nichts weg. Darum geht es: Erde, Sand, Kies und Stein als Rohstoff. Ist die Zielkiste voll, wartet er mit „Zielkiste voll“.
- Werkzeuge nutzen sich ab wie beim Spieler. Er trägt wie der Holzfäller ein Ersatzwerkzeug je Sorte; ist alles kaputt, wartet er, bis man ihm neues gibt.

## Was er gräbt und was nicht

- Nur **natürliche Blöcke** aus dem Block-Tag `workingvillagers:pit_diggable`: Erde, Gras, Sand, Kies, Ton, Schlamm, Schnee, Stein und seine Varianten (Granit, Diorit, Andesit, Tuff, Deepslate, …), Erze und Pflanzen wie Gras und Blumen. Per Datapack erweiterbar.
- **Nicht** gegraben werden Bauten und alles mit Inventar: Bretter, Ziegel, Glas, Truhen, Öfen, Schienen usw., auch keine Stämme. Unter so einem Block gräbt er auch nicht weiter, damit nichts in der Luft hängt. Die Spalte bleibt als Säule stehen.
- **Werkzeugstufe:** Braucht ein Block eine bessere Spitzhacke (z. B. Diamanterz mit Steinspitzhacke), bleibt er als Säule stehen und der Status meldet es. Mit besserer Spitzhacke gräbt er sie beim nächsten Durchgang ab.
- **Wasser und Lava:** Er gräbt keinen Block weg, der an eine Flüssigkeit grenzt, weder an der Wand noch am Boden. Die Grube läuft also nie voll. Der Status meldet „Flüssigkeit in der Grube“.
- **Kies und Sand** über ihm fallen nach; das ist gewollt, er gräbt sie in der nächsten Runde.
- **Licht:** Hat er Fackeln dabei, setzt er auf der Sohle etwa alle 6 Blöcke eine, damit nachts in der Grube nichts spawnt.

## Menü

- Status, Arbeiter, Zeile „Lohn und Werkzeug“ (Emeralds, Schaufel, Spitzhacke, Leitern, Fackeln) und Zielkisten wie bei den anderen Stationen.
- **Grube wählen**, **Leiterplatz wählen**, **Tiefe** (− / +).
- Anzeige: Größe der Grube, aktuelle Schicht von *Tiefe*, Aushub heute.
- Umriss als Vorschau, solange man Schnur in der Hand hält, siehe [Verständlichkeit](#verständlichkeit-geplant).

## Verständlichkeit

Anlass: Ein Ring mit 125 Blöcken Fläche, dessen ferne Ecke 17,9 Blöcke vom Pflock lag, blieb rot. Der Grund stand nur als laufende Zeile im Einstellungs-Tab, der Status sagte „kein Schnurring am Pflock“ (seit 0.6.1 steht dort der Grund). Keine Actionbar-Meldungen, sie wirken nicht wie Vanilla.

Umgesetzt (`PitPreviews`, `PitPreviewPayload`, `PitPreviewRenderer`, Screenshots per `PitPreviewScreenshots` im Client-Gametest):

- **Umriss-Vorschau:** Hält der Spieler Schnur in der Hand, zeigt der Client an jedem Grubenpflock im Umkreis von 24 Blöcken den Ring als Linien (`Gizmos`, wie der Umriss des Konstruktionsblocks). Gültige Grube: weißer Umriss des Aushubs oben am Ring, Sohle und Ecken blasser durch den Boden hindurch (Tiefe des Arbeiters). Ungültiger Ring: Rahmen um jede Schnur, die Schnüre mit dem Problem rot (offenes Ende, zu große Grube: der ganze Ring, zu uneben: höchste und tiefste Schnur). Der Server prüft alle 10 Ticks die Ringe an den Pflöcken in Reichweite (POI-Suche, `ServerPlayerMixin`), eine Quelle der Wahrheit ohne Config auf dem Client, und schickt die Vorschau nur, wenn sie sich ändert; nur an Clients mit dem Mod.
- **Partikel:** Ändert sich der Ring, zeigen Partikel das Ergebnis an der Stelle, an der der Spieler die Schnur legt: grüne auf dem übernommenen Ring (wie bisher), verärgerte Dorfbewohner-Partikel auf den Schnüren mit dem Problem.
- **Menü:** Der Einstellungs-Tab zeigt die Meldung umbrochen (bis 6 Zeilen) statt als laufende Zeile, bei zu großem Ring mit Fläche und Grenze („zu groß (125 von 64 Blöcken)“), darunter grau die Grenzen des Arbeiters („Höchstens 64 Blöcke, 8 tief“). Der Status des Grubenarbeiters nennt das Problem ebenfalls (seit 0.6.1).

## Grenzen nach Level

Fläche und Tiefe wachsen mit dem Level des Grubenarbeiters, die Seitenlänge um 2 je Level, die Tiefe um 2:

| Level | Fläche (Seite) | Tiefe |
| --- | --- | --- |
| Novice | 64 (8×8) | 8 |
| Apprentice | 100 (10×10) | 10 |
| Journeyman | 144 (12×12) | 12 |
| Expert | 196 (14×14) | 14 |
| Master | 256 (16×16) | 16 |

- Config: Anfänger- und Meisterwert je für Fläche und Tiefe (`pitDigger.noviceArea` 64, `masterArea` 256, `noviceDepth` 8, `masterDepth` 16); dazwischen wächst die Seitenlänge gleichmäßig. Neue Schlüssel, damit vorhandene Config-Dateien mit `maxDepth: 16` nicht den Anfänger auf 16 setzen.
- Ohne Arbeiter am Pflock (oder wenn er nicht geladen ist) gelten die Anfängerwerte (`PitLimits`). Die eingestellte Tiefe bleibt gespeichert (bis zum Meisterwert); gegraben und angezeigt wird höchstens die Tiefe des Arbeiters, und **+** geht nur bis dahin.
- Der Grubenarbeiter handelt nicht und bleibt deshalb Anfänger, bis Arbeiter durch Arbeit aufsteigen ([Anheuern & Lohn](../wirtschaft/anheuern-und-lohn.md)). Die Grenzen hängen trotzdem schon jetzt am Level, so wie das Grabtempo.

## Ausbau tiefer Gruben (geplant)

Tiefe Gruben bekommen **Etagen**: Unterhalb der Anfängertiefe (`pitDigger.noviceDepth`, 8) zieht der Grubenarbeiter alle 4 Blöcke eine Decke aus **Brettern** ein, getragen von Säulen aus **Stämmen**. Die Anfängergrube bleibt so, wie sie ist; eine tiefe Grube wird zum Keller mit mehreren Geschossen und kostet Holz aus der Kette [Holzfäller](holzfaeller.md) → [Holzsäger](holzsaeger.md). Kein Wandverbau.

```
  Seitenansicht, Tiefe 16 (Meister)

   S                S      S  Schnur
  ██H░░░░░░░░░░░░███      ░  ausgehoben
  ██H░░░░░░░░░░░░███      █  Erde/Stein, bleibt
  ██H░░░░░░░░░░░░███      H  Leiter (Leiterspalte bleibt offen)
  ██H==============███  ← Decke bei 8      =  Bretter
  ██H░║░░░░░░░░║░███      ║  Säule aus Stämmen
  ██H░║░░░░░░░░║░███
  ██H░║░░░░░░░░║░███
  ██H==============███  ← Decke bei 12
  ██H░║░░░░░░░░║░███
  ██H░║░░░░░░░░║░███
  ██H░║░░░░░░░░║░███
  ███████████████████  ← Sohle bei 16
```

- **Decken:** bei 8, 12, … Blöcken Tiefe (ab `noviceDepth`, dann alle 4), nicht auf der Sohle. Eine Decke ist die ganze Grubenfläche aus Brettern (jede Holzart), nur die **Leiterspalte bleibt offen**. Jedes Geschoss ist damit nutzbar (Lager, Farmen), und Spieler stehen nie über einem tiefen Loch.
- **Säulen:** aus Stämmen (jede Holzart), im Raster etwa **alle 4 Blöcke** in beide Richtungen, mindestens 4 je Geschoss, nie in der Leiterspalte. Sie stehen auf der nächsten Decke oder der Sohle und reichen bis unter die Decke darüber. Bei 8×8 sind es 4, bei 16×16 etwa 9 bis 16. Bei freien Formen gilt dasselbe Raster, Säulen nur auf Feldern innerhalb des Rings.
- **Ablauf:** Er gräbt ein Geschoss (die 4 Schichten unter einer künftigen Decke) aus, stellt von unten die Säulen auf und legt dann die Decke darüber; die Schicht der Decke wird also erst ausgehoben und dann mit Brettern geschlossen. Danach steigt er durch die Leiterspalte und gräbt das nächste Geschoss. Fehlen Bretter oder Stämme, bleibt er an der Decke stehen und wartet („Wartet: braucht Bretter“ / „... Stämme“); bis zur Anfängertiefe braucht er nie welche.
- **Mitnahme** wie Leitern: Bretter und Stämme gibt man ihm direkt oder er holt sie aus einer Dorfkiste (neue Vorräte wie `PLANKS` beim [Minenarbeiter](minenarbeiter.md)).
- **Bedarf** (größte Grube je Level, Raster alle 4 Blöcke):

| Level | Grube | Decken | Bretter | Stämme |
| --- | --- | --- | --- | --- |
| Novice | 8×8, 8 tief | – | – | – |
| Apprentice | 10×10, 10 tief | 1 (bei 8) | ~99 | ~8 (4 Säulen × 2) |
| Journeyman | 12×12, 12 tief | 1 (bei 8) | ~143 | ~36 (9 Säulen × 4) |
| Expert | 14×14, 14 tief | 2 (bei 8 und 12) | ~390 | ~45 (9 Säulen × (3 + 2)) |
| Master | 16×16, 16 tief | 2 (bei 8 und 12) | ~510 | ~112 (16 Säulen × (3 + 4)) |

  Werte zum Abschätzen, genau rechnen bei der Umsetzung. Eine Decke gibt es nur, wenn die Grube tiefer reicht (bei 12 also ab 13 Tiefe). Die Säulen sind so hoch wie das Geschoss darunter: zwischen zwei Decken 3, im untersten Geschoss bis zur Sohle 1 bis 4.
- **Übersicht:** Im Einstellungs-Tab steht unter der Tiefe, wie viele Decken die Grube bekommt und was sie kostet („2 Decken: 510 Bretter, 96 Stämme“), die Vorschau mit Schnur in der Hand zeigt die Decken als Linien.
- **Abbau:** Decken, Säulen und Leitern bleiben in der fertigen Grube.

Entschieden: Etagen statt Wandverbau, ganze Decke (nicht nur ein Rundgang am Rand), Säulen im Raster alle 4 Blöcke, Stämme und Bretter, erst das Geschoss graben, dann bauen, ab der Anfängertiefe aus der Config.

Offene Fragen:

- Darf der Spieler die Decken abschalten (Einstellung am Pflock), z. B. für einen offenen Steinbruch? Dann wäre die Grube unter 8 nicht tiefer erlaubt, oder doch?
- Säulen im Raster stehen mitten im Raum. Sollen sie bei kleinen Gruben (bis 6 breit) an den Rand rücken?

## Master-Fähigkeit

Gräbt behutsam: Gras, Myzel und Podsol landen als Block in der Zielkiste statt als Erde (wie Behutsamkeit). Damit lässt sich Gelände umsetzen, ohne dass es kahl wird.

## Technik

- Vanilla-Dorfbewohner planen keine Wege über Leitern. Das Klettern läuft deshalb als eigener Schritt: zum Leiterplatz laufen, dann senkrecht an der Leiter hoch oder runter bewegen. Bleibt er hängen, greift die bestehende Rückholung zur Station.
- Der Ring wird nur beim Wählen und dann alle paar Sekunden geprüft, nicht jeden Tick.
- Die Grube wird bei der Station gespeichert (Ring, Leiterplatz, Tiefe), wie die Kisten in `Workstations`.

## Abgrenzung zum Minenarbeiter

Der [Minenarbeiter](minenarbeiter.md) hat einen Steinbruch-Modus mit markiertem Quader. Der Grubenarbeiter ist die einfache, frühe Variante davon: freie Form per Schnur, nur nach unten; Etagen und Säulen nur unter den Decken tiefer Gruben ([Ausbau](#ausbau-tiefer-gruben-geplant)). Kommt der Minenarbeiter, kann sein Steinbruch-Modus diese Logik übernehmen.

## Sonderfälle & offene Fragen

- Name: „Grubenarbeiter“ klingt ähnlich wie Minenarbeiter und Grubenlogistiker. Alternative: „Erdarbeiter“ oder „Gräber“.
- Soll die Grube auch als Terraforming taugen, also nur bis auf eine feste Höhe abtragen statt eine bestimmte Tiefe?
- **Gruben stapeln:** Ein Ring auf der Sohle einer fertigen Grube ergibt die nächste Grube darunter. Das ist mühsam (Leitern, neuer Ring, alles in Reichweite der Station), aber erlaubt. Soll es verboten werden, z. B. indem der Grubenpflock auf derselben Höhe wie der Ring stehen muss (höchstens 4 Blöcke Unterschied)? Dann müsste für jede Stufe auch die Station mit nach unten.
- Darf die Grube größer als 64 Blöcke werden, wenn mehrere Grubenarbeiter an derselben Grube arbeiten (je einer pro Pflock)?
- Leiter an einer Seite, die aus Sand besteht: Leitern brauchen eine feste Wand. Prüfen, ob der Leiterplatz dann automatisch wechselt.

## Fertig, wenn

Ein Grubenarbeiter hebt eine 8×8 große und 8 tiefe Grube in Erde und Stein aus, setzt die Leiter durchgehend bis zur Sohle, beschädigt nichts außerhalb des Rings und liefert den Aushub in die Zielkiste.

## Siehe auch

[Minenarbeiter](minenarbeiter.md) · [Brenner](brenner.md) (verarbeitet Sand, Ton und Bruchstein) · [Steuerung](../grundlagen/steuerung.md) · [Holzfäller](holzfaeller.md)
