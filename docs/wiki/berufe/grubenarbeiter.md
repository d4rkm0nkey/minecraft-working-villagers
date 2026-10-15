# Beruf: Grubenarbeiter

[← Berufe](README.md) · Station: **Grubenpflock** (neu) · Phase: **Idee**

Der Grubenarbeiter hebt eine Grube aus, die der Spieler mit Schnur umrandet: Er gräbt alles innerhalb des Rings Schicht für Schicht bis zur eingestellten Tiefe ab, setzt dabei eine Leiter als Ausgang und bringt den Aushub in die Zielkiste. Gut für Keller, Fundamente, Sand- und Tongruben und als einfacher Steinbruch vor der [Mine](../grundlagen/mine.md).

## Workstation: Grubenpflock

- Ein Holzpflock mit aufgewickelter Schnur, z. B. aus Stöcken, Brettern und Schnur. Er steht am Rand der Grube.
- Kein Vanilla-Block ist frei: Der Grindstone gehört dem Weaponsmith, die [Grubenwerkbank](minenarbeiter.md) dem Minenarbeiter. Nur angeheuerte Dorfbewohner arbeiten am Grubenpflock, wie an Hackklotz und Holzsäge.
- Für Spieler hat er keine eigene Funktion. Ein Rechtsklick öffnet die Stationsübersicht.

## Die Grube markieren

1. Der Spieler legt **Schnur** auf den Boden und schließt sie zu einem Ring. Vanilla macht daraus Stolperdraht-Blöcke, Haken sind nicht nötig.
2. Im Menü des Grubenpflocks auf **Grube wählen** klicken, dann eine Schnur des Rings mit Rechtsklick anklicken (wie beim [Kisten verknüpfen](../grundlagen/steuerung.md#auswahl-in-der-welt)).
3. Der Mod sucht den Ring ab dieser Schnur und bestimmt das Innere. Ist der Ring nicht geschlossen, zu groß oder außerhalb der Reichweite, gibt es eine Meldung statt einer Grube.

Regeln für den Ring:

- Es zählen nur Schnüre, die sich gerade berühren (Norden, Süden, Osten, Westen). Diagonal ist eine Lücke, so wie Vanilla-Stolperdraht auch nur gerade verbindet.
- Jede Form ist erlaubt, nicht nur Rechtecke. Innen ist alles, was vom Rand aus nicht erreichbar ist, ohne eine Schnur zu kreuzen.
- Die Schnüre dürfen dem Gelände folgen, also auf unterschiedlicher Höhe liegen, aber höchstens **4 Blöcke** auseinander (Startwert, Config). Sonst kommt „Ring zu uneben“.
- Jede Schnur muss auf einem festen Block liegen. Eine Schnur über einem Loch zählt nicht.
- Der Block unter jeder Schnur bleibt stehen. Er ist der Rand der Grube, die Schnur bleibt so liegen und die Leiter hat eine Wand.
- Größe: höchstens **64 Blöcke** Grundfläche innen, z. B. 8×8 oder 4×16 (Startwert, Config). Ist der Ring größer, kommt „Grube zu groß (x von 64)“. Der ganze Ring muss in der Reichweite der Station liegen (wie bei den Kisten: Standard 16 Blöcke, per Config höchstens 32).
- Wird eine Schnur entfernt, pausiert der Grubenarbeiter mit „Wartet: Schnur unterbrochen“. Neu wählen ist nicht nötig, sobald der Ring wieder geschlossen ist und an derselben Stelle liegt.

## Tiefe

- Im Menü stellt der Spieler die **Tiefe** ein: 1 bis **x** Blöcke. Das Maximum steht in der Config, Startwert **16**.
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

Er gräbt nie den Block, auf dem er steht, und arbeitet in jeder Schicht vom Leiterplatz weg. Ein Tagespensum begrenzt den Aushub: **256 Blöcke** pro Arbeitstag (Startwert, Config, wie die 64 Stämme beim [Holzfäller](holzfaeller.md)).

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
- Optional: Umriss und Leiterplatz kurz mit Partikeln anzeigen.

## Master-Fähigkeit

Gräbt behutsam: Gras, Myzel und Podsol landen als Block in der Zielkiste statt als Erde (wie Behutsamkeit). Damit lässt sich Gelände umsetzen, ohne dass es kahl wird.

## Technik

- Vanilla-Dorfbewohner planen keine Wege über Leitern. Das Klettern läuft deshalb als eigener Schritt: zum Leiterplatz laufen, dann senkrecht an der Leiter hoch oder runter bewegen. Bleibt er hängen, greift die bestehende Rückholung zur Station.
- Der Ring wird nur beim Wählen und dann alle paar Sekunden geprüft, nicht jeden Tick.
- Die Grube wird bei der Station gespeichert (Ring, Leiterplatz, Tiefe), wie die Kisten in `Workstations`.

## Abgrenzung zum Minenarbeiter

Der [Minenarbeiter](minenarbeiter.md) hat einen Steinbruch-Modus mit markiertem Quader. Der Grubenarbeiter ist die einfache, frühe Variante davon: freie Form per Schnur, keine Stützen, keine Etagen, nur nach unten. Kommt der Minenarbeiter, kann sein Steinbruch-Modus diese Logik übernehmen.

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
