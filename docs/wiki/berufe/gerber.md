# Beruf: Gerber

[← Berufe](README.md) · Station: **Kessel** (Vanilla, Lederer) · Phase: **umgesetzt** (Leder, Gerben, Lederwaren, Färben, Uniformen der Wachen; ohne Meisterfähigkeit)

Der Gerber macht aus den Resten anderer Betriebe Leder und aus Leder Waren. Allein wäre „Leder zu Rüstung craften“ zu wenig. Spannend wird er als Verwerter: Er nimmt, was sonst im Müll landet (Kaninchenfelle des [Metzgers](metzger.md), verrottetes Fleisch aus der Beute der [Wachen](wache.md)), und beliefert damit den [Bibliothekar](bibliothekar.md) mit Büchern, die Wachen mit Uniformen und den Spieler mit Sätteln und Bündeln.

## Stand der Umsetzung

- **Mehrere Produkte** (0.5.0): bis zu 8 Produkte je Auftrag, zu gleichen Teilen, Vorrat je Produkt; siehe [Tischler](tischler.md#mehrere-produkte-umgesetzt-in-050).
- **Station:** Jeder Kessel (`AbstractCauldronBlock`: leer, Wasser, Lava, Pulverschnee) ist dieselbe Station `CAULDRON`; der Vanilla-POI `leatherworker` umfasst alle vier. Rechtsklick mit leerer Hand öffnet die Übersicht, mit Eimer oder Rüstung in der Hand bleibt es Vanilla. Vanilla-Lederer lassen sich anheuern und handeln weiter wie Lederer (Handelsfamilie `leatherworker`).
- **Auftrag** im Raster wie beim Pfeilmacher: Leder, Buch, Lederkappe, -jacke, -hose, -stiefel, Lederpferderüstung, Bündel, Sattel; dazu ein **Vorrat**. Bücher, Bündel und Sättel in Sätzen von 4 bzw. 1, Rüstung einzeln (Tränke und Rüstung stapeln nicht, 8 Inventarplätze).
- **Leder:** zuerst aus den Eingangskisten, dann aus Kaninchenfell (4 → 1), dann gegerbt.
- **Gerben:** bis zu 16 verrottetes Fleisch in den Wasserkessel (eine Stufe Wasser), nach 300 Sekunden (Config `tanner.tanningSeconds`) 4 → 1 Leder (`tanner.fleshPerLeather`, abschaltbar mit `tanner.tanRottenFlesh`). Inhalt und Fertigzeit stehen in der Station (Felder `tanning`, `tanned_at`); Blasen über dem Kessel, solange der Gerber arbeitet. Wird der Kessel abgebaut, fällt Fleisch bzw. das fertige Leder heraus. Leeren und Füllen des Kessels wechselt den Block, die Station bleibt.
- **Wasser:** Mit einem Eimer (Vorrat 1, auch aus der Dorftruhe) holt er Wasser an einer Quelle bis 16 Blöcke entfernt (`tanner.waterRadius`) und füllt den Kessel. Die Quelle bleibt (wie eine endlose Quelle). Regen füllt wie in Vanilla.
- **Farbe:** im Reiter Auftrag unter dem Vorrat (`<` `>`, ungefärbt und die 16 Farben), gespeichert im Feld `dye`. Rüstung färbt er mit einem Farbstoff pro Teil (`DyedItemColor.applyDyes`, also derselbe Farbton wie beim Spieler).
- **Umfärben:** Lederrüstung (auch Pferde- und Wolfsrüstung) in anderer Farbe aus den Eingangskisten holt er, bis zu 3 auf einmal, wäscht sie im Kessel aus (eine Stufe) und färbt sie neu, auch ohne Auftrag.
- **Speicherformat:** Die Station hatte die 16 Felder des Record-Codecs voll; `herd`, `boat`, `dye`, `tanning` und `tanned_at` stehen jetzt in einem eingebetteten `MapCodec` (gleiche Feldnamen, alte Welten laden unverändert).
- **Status:** „Sieht nach dem Auftrag“, „Holt Material“, „Arbeitet am Kessel“, „Holt Wasser für den Kessel“, „Gerbt, fertig in x min“, „Wartet auf %s“, „Kein Wasser im Kessel (braucht Eimer und Wasser in der Nähe)“, „Vorrat ist voll“; im Menü „Im Kessel: 16 verrottetes Fleisch, fertig in x min“ oder „Auftrag: x, heute y“.
- **Kleidung:** dunkelbraune Lederschürze mit Farbflecken über einem Leinenhemd mit hochgekrempelten Ärmeln, braune Lederkappe.
- **Uniformen:** siehe [Wache](wache.md#stand-der-umsetzung). Die Rüstung zeigt sich nicht als Rüstung (Dorfbewohner haben keine Rüstungsebene), sondern färbt den Wappenrock: Ebene `UniformLayer` mit einer grauen Maske des Wappenrocks (`guard_tabard.png`), getönt mit der Farbe des Lederharnischs.
- **Tests:** Bücher aus Papier und Leder; Leder aus 8 Fellen; 8 Fleisch gerbt im Wasserkessel zu 2 Leder; leerer Kessel, Eimer, Pfütze: er holt Wasser und gerbt; roter Lederharnisch; blaue Stiefel werden ausgewaschen und rot; eine Wache zieht einen gegebenen Harnisch an und holt Stiefel aus der Dorftruhe.
- **Nicht umgesetzt:** Meisterfähigkeit (Farben mischen), Rüstung für andere Arbeiter, Eisenrüstung als eigene Uniform.

## Der Kreislauf

```
Metzger ── Leder (Kühe), Kaninchenfelle ──┐
Wachen ─── verrottetes Fleisch (Zombies) ─┼──► Gerber ──► Leder ──► Bücher, Rüstung, Sattel, Bündel
Farmer ─── Farbstoffe (Blumen, Rüben) ────┘     ▲ Wasser
                                                └── Kessel (füllt er selbst mit dem Eimer)
```

- **Mehr Leder ohne mehr Kühe:** Kühe geben 0–2 Leder; eine Herde von 8 reicht kaum für Bücher. Der Gerber holt das Leder aus Abfällen.
- **Verrottetes Fleisch** ist sonst wertlos, fällt aber jede Nacht bei den Wachen an. So bekommt die Beute der Wachen einen Abnehmer, wie der Faden beim Pfeilmacher.

## Station: Kessel

- Der Vanilla-**Kessel** ist der Arbeitsplatz des Lederers. Er hat kein Menü: Rechtsklick mit leerer Hand öffnet die Übersicht (wie Bognertisch und Zielscheibe). Mit Eimer, Flasche oder Lederrüstung in der Hand bleibt alles wie in Vanilla.
- Alle Kessel-Arten zählen als dieselbe Station (leer, mit Wasser, Lava, Pulverschnee), sonst ginge die Station verloren, sobald sich der Wasserstand ändert. Bei der Umsetzung prüfen, ob der Vanilla-POI `leatherworker` alle Kessel-Zustände umfasst.
- Vanilla-Lederer lassen sich anheuern, behalten ihre Angebote und handeln wie Lederer.

## Was er macht

Im Reiter **Auftrag** wählt man ein Erzeugnis und einen **Vorrat**, wie beim [Pfeilmacher](pfeilmacher.md):

| Erzeugnis | Zutaten | Wofür |
| --- | --- | --- |
| Leder | 4 Kaninchenfelle (Vanilla-Rezept) oder 4 verrottetes Fleisch (gegerbt, siehe unten) | alles Weitere |
| Buch | 3 Papier, 1 Leder | [Bibliothekar](bibliothekar.md), Bücherregale des [Tischlers](tischler.md), Spieler |
| Lederkappe, -jacke, -hose, -stiefel | 5 / 8 / 7 / 4 Leder | Uniformen der Wachen, Spieler |
| Bündel | 1 Faden, 1 Leder | Spieler |
| Sattel | 3 Leder, 1 Eisenbarren | Spieler |
| Lederpferderüstung | 7 Leder | Spieler |

Nimmt er Leder aus den Eingangskisten, verarbeitet er erst das; Kaninchenfelle und Fleisch gerbt er nur, wenn das Leder fehlt. Das Papier kommt vom Spieler (Zuckerrohr vom [Farmer](farmer.md)); einen Papiermacher gibt es nicht.

## Gerben: verrottetes Fleisch zu Leder

Kein Vanilla-Rezept, deshalb mit Zeit und Wasser, damit es kein Zaubertrick ist:

1. Er legt 16 verrottetes Fleisch in den **Wasserkessel** ein (eine Wasserstufe).
2. Nach einem halben Tag (6000 Ticks) ist es gegerbt: 4 Leder. In der Zwischenzeit macht er seine anderen Aufträge.
3. Den Kessel füllt er selbst mit einem **Eimer** an Wasser in der Nähe nach (wie der [Apotheker](apotheker.md) seine Flaschen). Regen füllt ihn wie in Vanilla.

- Was im Kessel liegt, merkt sich die Station (unsichtbar; Partikel wie Blasen zeigen, dass gegerbt wird). Wird der Kessel abgebaut, fällt das Fleisch heraus.
- Config `tanner.tanRottenFlesh` (an), `tanner.fleshPerLeather` (4) und `tanner.tanningTicks` (6000).

## Färben und Waschen

- **Färben:** Im Reiter **Einstellungen** wählt man eine Farbe (die 16 Farbstoffe) für die Lederrüstung. Den Farbstoff holt er aus der Eingangskiste; eine Farbe pro Teil, wie im Vanilla-Rezept. Ohne Farbe bleibt das Leder braun.
- **Waschen:** Gefärbte Lederrüstung aus der Eingangskiste wäscht er im Wasserkessel aus, wie der Spieler es kann (eine Wasserstufe je Teil), und färbt sie dann neu. So lassen sich alte Uniformen umfärben.

## Uniformen für die Wachen

Bisher tragen die [Wachen](wache.md) keine Rüstung („Rüstung tragen“ ist dort offen). Mit dem Gerber:

- Lederrüstung in der Dorftruhe ziehen Wachen an, wie sie sich dort Pfeile holen (neuer Vorrat „Rüstung“ in `WorkerSupplies`, kein Pflichtteil).
- Rüstung nutzt sich ab; der [Schmied](schmied.md) repariert sie mit Leder (Vanilla-Reparaturmaterial), sonst bringt sie der Gerber neu.
- Die Farbe ist die des Dorfes: Wachen eines Dorfes sehen gleich aus. Später auch Eisenrüstung vom Schmied.

## Arbeitsablauf

1. Auftrag und Vorrat prüfen; ist der Vorrat voll, wartet er.
2. Steht Gerbgut fertig im Kessel, nimmt er das Leder heraus.
3. Ist im Kessel zu wenig Wasser und Fleisch zum Gerben da, füllt er den Kessel und legt neues ein.
4. Material für einen Satz holen (z. B. 4 Bücher), am Kessel fertigen (ein Stück pro Sekunde), abliefern.

## Versorgung

| Was | Pflicht | Hält bis | Woher |
| --- | --- | --- | --- |
| Eimer | nein | 1 | Spieler, Schmied; ohne Eimer füllt nur Regen den Kessel |
| Leder, Felle, Fleisch | für den Auftrag | einen Satz | Metzger, Wachen, Dorftruhe |
| Farbstoff | nein | einen Satz | Farmer, Spieler |

## Status

„Sieht nach dem Auftrag“, „Holt Material“, „Fertigt Lederwaren“, „Gerbt (noch x min)“, „Füllt den Kessel“, „Wäscht Rüstung aus“, „Wartet auf Leder“, „Kein Wasser beim Kessel“, „Vorrat ist voll“; im Menü „Auftrag: x, heute y“ und „Im Kessel: 16 Fleisch, fertig in x min“.

## Kleidung

Dunkelbraune Lederschürze über einem hellen Hemd mit hochgekrempelten Ärmeln, Unterarme gefleckt (Gerbstoff), Lederkappe.

## Master-Fähigkeit

Färbt Lederrüstung nach Vorlage (mehrere Farbstoffe gemischt, wie in Vanilla) und gerbt schneller.

## Offene Fragen

- **Fleisch zu Leder?** Nicht Vanilla. Empfehlung: ja, mit Zeit und Wasser und abschaltbar; ohne das bliebe der Gerber ein Rezeptbuch.
- **Gerbgut sichtbar?** Vanilla-Kessel zeigen nur Flüssigkeit. Empfehlung: unsichtbar in der Station, mit Blasen-Partikeln; ein eigenes Kessel-Modell nicht.
- **Bücher hier oder beim Tischler?** Empfehlung: hier, Leder ist die knappe Zutat. Der Tischler macht weiter Bücherregale aus fertigen Büchern.
- **Rüstung für alle Arbeiter?** Empfehlung: erst nur Wachen; andere Arbeiter kämpfen nicht.

## Siehe auch

[Metzger](metzger.md) · [Wache](wache.md) · [Apotheker](apotheker.md) · [Bibliothekar](bibliothekar.md) · [Berufe](README.md)
