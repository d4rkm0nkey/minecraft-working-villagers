# Dorftruhen

[← Wiki-Start](../README.md) · Phase: **umgesetzt** (ohne Dorfkauf und Dorfkasse, siehe [Stand der Umsetzung](#stand-der-umsetzung))

Eine **Dorftruhe** ist ein gemeinsamer Vorrat für alle Arbeiter eines Dorfs. Der Spieler kann beliebig viele aufstellen; jeder Arbeiter in Reichweite holt sich daraus selbst, was ihm fehlt: **Lohn** (Emeralds), **Werkzeug** und **Verbrauchsmaterial** (Setzlinge, Saatgut, Leitern, Fackeln). Das direkte Geben per Rechtsklick oder Zuwerfen bleibt, die Dorftruhe ergänzt es nur.

## Stand der Umsetzung

Umgesetzt wie unten beschrieben, mit diesen Abweichungen:

- **Block:** eine Vanilla-Truhe (Deckel, Geräusche, Katzen und Blöcke obendrauf, Doppeltruhe wie bei der Truhe) mit eigener Textur: die Truhentextur in Fichte, Eisenbänder statt der dunklen Kanten, ein Smaragd als Schloss. Erzeugt von `tools/generate_textures.py`.
- **Unerreichbare Truhe:** Kein eigener Status „Kommt nicht an die Dorftruhe“. Eine Truhe, die er nicht erreicht oder aus der er nichts bekommt, lässt er für den Rest des Tages aus und sucht die nächste; sonst wartet er mit dem bekannten Status.
- **Erneut nachsehen:** alle 100 Ticks, auch beim Lohn (nicht einmal pro Minute).
- **Werkzeugwahl:** ein einziges Kriterium, die wenigsten verbleibenden Nutzungen. Das bevorzugt abgenutzte und billige Werkzeuge zugleich. Werkzeug mit höchstens 10 % Haltbarkeit nimmt er nur, wenn er gar keins hat (aus der [Werkzeugrückgabe](werkzeugrueckgabe.md#abgenutztes-werkzeug-wird-nicht-wieder-genommen) vorgezogen).
- **Stationsmenü:** Der Tooltip der Versorgungszeile sagt nur, aus wie vielen Dorftruhen der Arbeiter sich bedient, nicht was darin liegt.
- **Dorftruhen-Menü:** Liste mit ✔ oder ✘ und dem, was fehlt und in keiner Dorftruhe liegt („Braucht eine Axt!“). Einen gelben Zustand „knapp“ gibt es nicht. Ab 11 Arbeitern „… und N weitere“.
- **Mehrspieler:** ohne Besitzer, wie unten beschrieben.
- **Nicht umgesetzt:** alles, was den [Dorfkauf](dorf-und-kauf.md) braucht (Zugehörigkeit zum Dorf, Dorfkasse, Dorfbuch).
- **Tests:** Ein Holzfäller ohne alles holt sich Lohn für 3 Tage, die abgenutztere von zwei Äxten und 16 Setzlinge und fällt den Baum; eine umbenannte Axt bleibt liegen. Weil die Gametests nebeneinander laufen, setzen sie die Reichweite auf 8.

## Warum

- **Weniger Laufarbeit für den Spieler.** Heute muss man jedem Arbeiter einzeln Emeralds und Werkzeug geben. Mit fünf Arbeitern ist das eine tägliche Runde durchs Dorf. Eine gefüllte Dorftruhe hält alle für Tage am Laufen.
- **Geschlossene Kreisläufe.** Was ein Arbeiter herstellt, kann ein anderer verbrauchen: Der [Tischler](../berufe/tischler.md) baut Leitern, ein [Träger](../berufe/traeger.md) bringt sie in die Dorftruhe, der [Grubenarbeiter](../berufe/grubenarbeiter.md) holt sie sich. Setzlinge aus der Holzfäller-Kiste landen über einen Träger wieder beim [Holzfäller](../berufe/holzfaeller.md). Das passt zum Grundsatz „Arbeiter sprechen nur über Kisten“ ([Logistik](../grundlagen/logistik.md)).
- **Vorstufe zur Dorfkasse.** Die geplante [Dorfkasse](dorf-und-kauf.md#dorfkasse) braucht den Dorfkauf. Dorftruhen gehen schon vorher und werden später zur Kasse (siehe [Übergang zum Dorfkauf](#übergang-zum-dorfkauf)).

## Der Block

- **Dorftruhe** (`workingvillagers:village_chest`): sieht aus wie eine Truhe mit Eisenbeschlägen und einem Emerald im Schloss. 27 Plätze, öffnet wie eine Truhe.
- **Doppeltruhe** wie bei der Vanilla-Truhe: Zwei Dorftruhen nebeneinander werden eine große mit 54 Plätzen (Schleichen beim Setzen hält sie getrennt). Arbeiter sehen beide Hälften als eine Truhe.
- **Rezept:** formlos aus einer Truhe und einem Emerald. Bewusst billig, die Dorftruhe soll kein Fortschrittshindernis sein.
- Trichter und Träger können sie befüllen und leeren wie jede Kiste. Sie lässt sich [benennen](../grundlagen/steuerung.md#kisten-benennen).
- Sie ist ein **POI** (`workingvillagers:village_chest`, ohne Beruf), damit Arbeiter sie über den `PoiManager` finden, ohne Blöcke abzusuchen. Vanilla-Dorfbewohner beachten sie nicht.

## Reichweite: Wer bedient sich?

Ohne [Dorfkauf](dorf-und-kauf.md) gibt es noch kein Dorf-Objekt. Bis dahin gilt eine einfache Regel:

> Ein Arbeiter bedient sich aus jeder Dorftruhe, die höchstens **48 Blöcke** (Config `villageChests.radius`) von seinem Arbeitsplatz entfernt ist.

- **Arbeitsplatz** ist die Station, beim Träger die Quelle seines ersten Wegs (wie heute `WorkerSupplies.anchor`).
- Gemessen wird waagerecht; senkrecht gilt ±24 Blöcke, damit der [Grubenarbeiter](../berufe/grubenarbeiter.md) unten in der Grube die Truhe oben am Rand noch nutzt.
- Liegen mehrere Dorftruhen in Reichweite, geht er zur nächsten, die das Gesuchte enthält.
- Mit dem Dorfkauf wird die Reichweite durch die Zugehörigkeit ersetzt: Ein Arbeiter nutzt alle Dorftruhen **seines** Dorfs, und nur diese.

## Was sich ein Arbeiter holt

Die Höchstmengen sind dieselben wie beim direkten Geben (`WorkerSupplies.accepts`). Die Dorftruhe ändert nur, *wer* nachfüllt, nicht wie viel ein Arbeiter trägt.

| Vorrat | Holt sich, wenn … | Nimmt bis zu | Beispiel |
| --- | --- | --- | --- |
| Lohn (Emeralds) | er den Tageslohn nicht mehr hat | Lohn für **3 Tage** (Config `villageChests.wageDays`) | Lohn 1 → nimmt 3 Emeralds |
| Werkzeug | er keins mehr hat, oder nur noch eins mit < 10 % Haltbarkeit | 1 Stück (das Ersatzwerkzeug) | Holzfäller: Axt |
| Verbrauchsmaterial | Bestand unter einem Viertel der Höchstmenge | auf die Höchstmenge (`keepSaplings`, `keepSeeds`, …) | Grubenarbeiter: Leitern auf 64 |

- **Wenige Emeralds bei sich**, nicht 64: Stirbt ein Arbeiter, fällt nur wenig zu Boden. Das Geld bleibt sicher in der Truhe. Wer ihm direkt mehr gibt, darf das weiter tun.
- **Werkzeugwahl:** zuerst das am stärksten abgenutzte passende Werkzeug, damit angebrochene Werkzeuge aufgebraucht werden. Bei gleichem Verschleiß das billigste Material (Holz vor Stein vor Eisen …). Werkzeug, das fast kaputt ist, nimmt er nicht (siehe [Werkzeugrückgabe](werkzeugrueckgabe.md#abgenutztes-werkzeug-wird-nicht-wieder-genommen)).
- **Verzauberte oder umbenannte Werkzeuge nimmt er nicht** (Config `villageChests.takeEnchanted`, Standard `false`). Sonst verschwindet die Glücksspitzhacke des Spielers im Grubenarbeiter. Ein verzaubertes Werkzeug kann man ihm weiter direkt geben.
- **Saatgut/Setzlinge:** Er nimmt nur Sorten, die er schon kennt oder für seine Arbeit braucht. Der Bauer nimmt jedes Saatgut (wie beim Geben), der Holzfäller jede Setzlingsart, die auf Erde wächst.
- Er legt **nichts** in die Dorftruhe zurück. Ertrag geht wie bisher in die Zielkisten der Station. Die Dorftruhe ist reiner Vorrat. Einzige Ausnahme ist die [Werkzeugrückgabe](werkzeugrueckgabe.md): Fast kaputtes Werkzeug legt er hinein, statt es zu zerbrechen.

## Ablauf

1. **Morgens vor der Arbeit** prüft der Arbeiter seine Vorräte (Erweiterung von `WorkerSupplies.busy`). Fehlt etwas oder ist etwas knapp (siehe Tabelle), sucht er eine Dorftruhe in Reichweite, die es enthält.
2. Er geht hin, öffnet sie (Truhen-Animation und -Geräusch), nimmt das Benötigte und geht an die Arbeit. Status im Menü: **„Holt Vorräte aus der Dorftruhe“**.
3. **Während der Arbeit** geht er nur, wenn er sonst nicht weiterarbeiten kann: Werkzeug zerbrochen, Leitern alle. Knappe Vorräte füllt er erst am nächsten Morgen auf, damit er nicht dauernd hin- und herläuft.
4. Hat keine Dorftruhe das Gesuchte, bleibt alles wie heute: Er wartet an der Station mit dem bekannten Status („Wartet auf Lohn“, „Hat keine Axt“). Nach 100 Ticks schaut er erneut nach, beim Lohn nur einmal pro Minute, damit er nicht pendelt.
5. Kommt er nicht hin (kein Weg), merkt er sich die Truhe für den Rest des Tages als unerreichbar. Status: **„Kommt nicht an die Dorftruhe“**.

**Gleichzeitiger Zugriff:** Keine Reservierung. Wer zuerst ankommt, nimmt. Ist das Gesuchte weg, geht der andere zur nächsten Dorftruhe oder wartet. Bei den kleinen Mengen reicht das.

## Anzeige

**Stationsmenü** (Versorgungszeile, `SuppliesRow`): Neben dem, was der Arbeiter trägt, steht ein kleines Truhen-Symbol, wenn eine Dorftruhe in Reichweite ist. Der Tooltip zeigt, was dort für ihn bereitliegt: „Dorftruhe ‚Lager‘, 14 Blöcke: 23 Emeralds, 2 Äxte, 40 Setzlinge“.

**Dorftruhen-Menü:** das normale Truhen-Inventar, dazu rechts ein Seitenpanel (`GuiPanels`) mit den Arbeitern, die sie bedient:

```
┌────────────────────────────┐ ┌──────────────────────────────┐
│ Dorftruhe „Lager“          │ │ Versorgt 4 Arbeiter          │
│ [27 Plätze]                │ │ Holzfäller   ✔               │
│                            │ │ Bauer        ✘ keine Hacke   │
│                            │ │ Grubenarb.   ! Leitern knapp │
│                            │ │ Träger       ✔               │
│                            │ │ Lohn reicht für ~6 Tage      │
└────────────────────────────┘ └──────────────────────────────┘
```

„Lohn reicht für“ = Emeralds in allen Dorftruhen dieser Arbeiter ÷ (Lohn × Arbeiter). Rot, wenn ein Arbeiter etwas braucht, das in keiner Dorftruhe liegt.

## Übergang zum Dorfkauf

- Mit dem [Dorfkauf](dorf-und-kauf.md) gehört jede Dorftruhe zu dem Dorf, in dessen Radius sie steht. Außerhalb eines eigenen Dorfs bleibt die Reichweitenregel.
- **Vorschlag: Die Dorfkasse sind die Emeralds in den Dorftruhen.** Statt eines eigenen Barrels an der Glocke zählt der Kassenstand alle Emeralds in allen Dorftruhen des Dorfs. Einnahmen ([Handelsangestellter](../berufe/handelsangestellter.md), Steuern) legen ihre Emeralds in eine Dorftruhe. Das spart einen zweiten Block mit fast derselben Aufgabe.
- Das [Dorfbuch](dorfbuch.md) zeigt im Reiter **Versorgung** den Bestand aller Dorftruhen und was welchem Arbeiter fehlt.
- Wochenlohn und Lohnstufen ändern nichts am Prinzip; der Arbeiter nimmt dann den Lohn seiner Stufe.

### Dorftruhen als Dorfkasse: Vor- und Nachteile

| Vorteile | Nachteile |
| --- | --- |
| Ein Block und ein Begriff statt zwei. Spieler lernen nur „Dorftruhe“. | Emeralds zum Handeln und Lohngeld liegen zusammen. Wer seine Handels-Emeralds in eine Dorftruhe legt, bezahlt damit Löhne. |
| Arbeiter holen ihren Lohn schon dort. Die Kasse braucht keinen eigenen Zahlweg. | Trichter und Träger können Emeralds unbemerkt herausziehen. Die Kasse „leckt“. |
| Kein Umbau später: Was der Spieler vor dem Dorfkauf eingelagert hat, ist danach einfach der Kassenstand. | Der Kassenstand ist eine Summe über viele Truhen, auch in nicht geladenen Chunks. Ohne Zwischenspeicher ist er unbekannt. |
| Verteilt: Jeder Ortsteil (Mine, Holzplatz) hat seine Kasse in der Nähe, kurze Wege. | Chunk-Loading hängt am Kassenstand („leer → Laden endet“). Eine falsch gezählte Kasse schaltet ein Dorf ab. |
| Einnahmen landen dort, wo sie gebraucht werden. | Auf Servern mehrere Stellen, an denen Geld gestohlen werden kann, statt einer. |

**Gegenmittel:** Jede Dorftruhe meldet bei jeder Änderung ihren Emerald-Bestand an einen Dorf-Speicher (`SavedData`). Der Kassenstand ist dann die Summe der zuletzt bekannten Bestände, auch für entladene Truhen. Gegen die Vermischung hilft eine eigene Truhe für Handels-Emeralds; das ist Sache des Spielers.

**Empfehlung:** ersetzen, mit zwischengespeichertem Bestand. Die Nachteile sind lösbar oder Spielerentscheidung, die Vorteile (ein Konzept, kein Umbau) überwiegen.

## Mehrspieler

- **Vorerst ohne Besitzer.** Arbeiter haben keinen Arbeitgeber und Dorftruhen keinen Eigentümer. Jeder Arbeiter bedient sich aus jeder Dorftruhe in Reichweite, egal wer sie aufgestellt hat, und jeder Spieler kann sie öffnen wie eine normale Truhe.
- Auf Servern heißt das: Wer neben dem Dorf eines anderen baut, teilt sich unter Umständen dessen Dorftruhen. Das wird bewusst hingenommen.
- Später, mit Dorfkauf und Rollen ([Multiplayer](../technik/multiplayer.md)), dürfen nur Besitzer und Verwalter eine Dorftruhe öffnen, und die Arbeiter nutzen nur die ihres Dorfs.

## Config

| Schlüssel | Standard | Bedeutung |
| --- | --- | --- |
| `villageChests.enabled` | `true` | Arbeiter nutzen Dorftruhen. Aus: Block bleibt, wirkt wie eine normale Truhe. |
| `villageChests.radius` | 48 | Waagerechte Reichweite vom Arbeitsplatz (8–128) |
| `villageChests.wageDays` | 3 | Für wie viele Tage er Lohn mitnimmt (1–16) |
| `villageChests.takeEnchanted` | `false` | Auch verzauberte und umbenannte Werkzeuge nehmen |

## Technik

- Neuer Block mit `BlockEntity` (Unterklasse von `ChestBlockEntity` oder eigener `RandomizableContainerBlockEntity` mit Truhenmodell), eigener `PoiType` mit 0 Arbeitsplätzen (`platform.registerPoi("village_chest", …, 0, 1)`).
- Suche: `PoiManager.findAll(…, pos, radius, PoiManager.Occupancy.ANY)`, gefiltert auf „enthält Gesuchtes“ (`Chests.contains`) und sortiert nach Entfernung. Ergebnis bis zum nächsten Morgen zwischenspeichern, damit nicht jeder Tick sucht.
- Herausnehmen über das vorhandene `Chests.take` mit Filter und Menge aus der Tabelle oben.
- Das Holen ist ein Zwischenschritt in `WorkerSupplies.busy`: Statt sofort `waitFor` aufzurufen, wird erst ein Gang zur Dorftruhe versucht. So bekommen alle Berufe es auf einmal, ohne dass jedes Arbeitsverhalten (`ChopTrees`, `FarmFields`, …) einzeln angepasst wird.
- Die Dorftruhen-Übersicht im Menü rechnet der Server und schickt sie mit dem Menü (wie `SuppliesInfo`).

## Tests (beim Umsetzen)

- Ein Arbeiter ohne Emeralds geht zu einer Dorftruhe in Reichweite, nimmt 3 Emeralds (Lohn 1) und arbeitet.
- Eine Dorftruhe außerhalb der Reichweite wird ignoriert; der Arbeiter wartet mit „Wartet auf Lohn“.
- Eine verzauberte Axt bleibt liegen, eine unverzauberte daneben wird genommen.
- Der Holzfäller nimmt die abgenutzte Axt vor der neuen.
- Zwei Arbeiter, eine Truhe mit einem Emerald: einer arbeitet, der andere wartet, nichts hängt.
- Direktes Geben funktioniert weiter; wer genug trägt, geht nicht zur Truhe.
- Ein Grubenarbeiter mit 10 von 64 Leitern füllt morgens auf 64 auf, während der Arbeit nicht.

## Offene Fragen

- **Eigener Block oder Markierung?** Statt eines neuen Blocks könnte man jede Truhe per Item (z. B. „Dorfsiegel“) zur Dorftruhe machen. Spart Texturen, ist aber schwerer zu erkennen. Vorschlag: eigener Block, weil man ihn auf einen Blick sieht.
- **Kasse:** Sollen die Dorftruhen die Dorfkasse ersetzen? Siehe [Vor- und Nachteile](#dorftruhen-als-dorfkasse-vor--und-nachteile).
- **Überschüsse:** Soll der Bauer überzähliges Saatgut, der Holzfäller überzählige Setzlinge selbst in die Dorftruhe bringen? Bequem, nimmt aber dem Träger eine Aufgabe. Vorerst nein.
- **Nahrung:** Wenn [Bedürfnisse](../wirtschaft/beduerfnisse.md) kommen, ist die Dorftruhe der naheliegende Ort für Essen. Oder bleibt das bei eigenen Vorratskammern?
- **Reichweite 48** genug? Ein großes Dorf mit Mine und Holzplatz am Rand könnte zwei Dorftruhen brauchen. Das ist gewollt, aber sollte im Playtest geprüft werden.

## Siehe auch

[Werkzeugrückgabe](werkzeugrueckgabe.md) · [Anheuern & Lohn](../wirtschaft/anheuern-und-lohn.md) · [Dorf & Dorfkauf](dorf-und-kauf.md) · [Dorfbuch](dorfbuch.md) · [Logistik](../grundlagen/logistik.md) · [Roadmap](../planung/roadmap.md)
