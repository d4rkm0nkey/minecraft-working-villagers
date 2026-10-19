# Beruf: Waffenschmied

[← Berufe](README.md) · Station: **Schleifstein** (Vanilla, Waffenschmied) · Phase: **umgesetzt** (Schmieden, Zusammenschleifen, Entzaubern; ohne Nahkampfwachen und Meisterfähigkeit)

Der Waffenschmied macht neues Werkzeug und neue Waffen und holt aus Altem heraus, was noch drinsteckt. Der [Schmied](schmied.md) schmilzt Erz und repariert mit Material. Neues Werkzeug macht bisher niemand: Jede Axt, jede Hacke, jede Spitzhacke kommt vom Spieler. Der Waffenschmied schließt diese Lücke und hat dazu zwei Arbeiten, die nur der Schleifstein kann.

## Stand der Umsetzung

- **Mehrere Produkte** (0.5.0): bis zu 8 Produkte je Auftrag, zu gleichen Teilen, Vorrat je Produkt; siehe [Tischler](tischler.md#mehrere-produkte-umgesetzt-in-050).
- **Station:** Vanilla-Schleifstein (POI `weaponsmith`); die Reiter über dem Schleifstein-Menü führen zur Übersicht (Mixins auf `GrindstoneMenu` und `GrindstoneScreen`). Vanilla-Waffenschmiede lassen sich anheuern, behalten ihre Angebote und handeln wie Waffenschmiede (Handelsfamilie `weaponsmith`).
- **Rezepte** (`ForgeRecipes`): die Crafting-Rezepte, deren Ergebnis im Item-Tag `workingvillagers:forgeable` steht (Äxte, Spitzhacken, Schaufeln, Hacken, Schwerter, Speere, Schere, Streitkolben), ohne die mit Brettern (Holzwerkzeug macht der Tischler). Statt der Komponenten `tool`/`weapon` ein Tag wie beim Tischler (`joinery`): Datapacks können ergänzen. Gefertigt wird mit den Helfern des Tischlers (`CarpenterRecipes.account/pick/craft`), also mit Tag-Zutaten wie am Werkbank.
- **Auftrag** im Raster mit Seiten, **Vorrat**, darunter die Wahl, was er außerdem macht (`<` `>`): zusammenschleifen und entzaubern, nur eins davon, nur der Auftrag (Feld `grind` der Station). Bis zu 4 Stück pro Gang, ein Stück alle 2 Sekunden, Funken und Schleifstein-Geräusch.
- **Zusammenschleifen:** zwei beschädigte Teile derselben Art (unverzaubert, nicht umbenannt, ohne eigene Daten) aus einer Kiste: aus Dorftruhen in Reichweite nur abgenutzte (`tools.returnBelow`), aus den Eingangskisten jedes beschädigte. Ergebnis: Haltbarkeit beider plus 5 % wie in Vanilla, zurück in dieselbe Kiste (sonst Dorftruhe, dann Ausgangskiste). Bis zur Rückgabe trägt das Teil eine Markierung in `custom_data`.
- **Entzaubern:** Verzaubertes aus den Eingangskisten (nicht umbenannt), bis zu 4 auf einmal, mit Glasflaschen. Wie der Schleifstein: Flüche bleiben, verzauberte Bücher ohne Rest werden Bücher, Reparaturkosten neu. Erfahrung wie der Schleifstein (die Hälfte der Mindestkosten der Verzauberungen, aufgerundet, plus Zufall bis zur selben Menge), gesammelt wie bei den Wachen und in Fläschchen zu 7 (`guard.experiencePerBottle`) gefüllt. Glasflaschen hält er wie die Wachen vorrätig (bis 16, Dorftruhe).
- **Reihenfolge:** Zusammengeschliffenes zurückbringen, Paare schleifen, Verzaubertes entzaubern, abliefern, neue Paare holen, Verzaubertes holen, dann der Auftrag.
- **Status:** „Sieht nach dem Auftrag“, „Holt Material“, „Schmiedet“, „Schleift zwei Werkzeuge zusammen“, „Entzaubert“, „Bringt geschliffenes Werkzeug zurück“, „Wartet auf %s“, „Wartet: kein Auftrag“, „Wartet: kein Rezept für den Auftrag“, „Vorrat ist voll“; „Auftrag: x, heute y“ zählt Geschmiedetes und Entzaubertes.
- **Kleidung:** rußige Lederschürze über rostrotem Hemd, Lederhandschuhe, Schutzbrille auf der Stirn.
- **Tests:** 6 Eisenbarren und 4 Stöcke werden 2 Eisenäxte; zwei abgenutzte Eisenspitzhacken aus der Dorftruhe werden eine und liegen wieder dort; ein Schwert mit Schärfe III wird entzaubert, dazu mindestens ein Erfahrungsfläschchen.
- **Nicht umgesetzt:** Nahkampfwachen, „Geschliffen“, Meisterfähigkeit, Anzeige der nicht abgefüllten Erfahrung im Menü.

## Der Kreislauf

```
Minenarbeiter ── Erz ──► Schmied ── Barren ──┐
Holzsäger ────────────── Stöcke ─────────────┼──► Waffenschmied ── neues Werkzeug, Waffen ──► Dorftruhe ──► Arbeiter
                                             │         ▲   │
Arbeiter ── abgenutztes Werkzeug ─► Dorftruhe ─────────┘   └── Erfahrungsfläschchen ──► Schmied (Reparatur)
Wachen, Fischer ── verzauberte Beute ──► Eingangskiste ──┘
```

- **Werkzeug-Nachschub:** Holzfäller, Bauer, Metzger, Gruben- und Minenarbeiter brauchen ständig Werkzeug. Mit Schmied und Waffenschmied läuft das Dorf ohne Werkzeug vom Spieler: Erz → Barren → Werkzeug → Dorftruhe.
- **Zweite Erfahrungsquelle** neben der Jagd der [Wachen](wache.md): Entzaubern gibt Erfahrung, der Schmied repariert damit Verzaubertes.

## Station: Schleifstein

- Der Vanilla-**Schleifstein** ist der Arbeitsplatz des Waffenschmieds. Er hat ein Menü; darüber stehen die Reiter der Station wie beim Webstuhl.
- Vanilla-Waffenschmiede lassen sich anheuern, behalten ihre Angebote und handeln weiter wie Waffenschmiede.
- Reiter **Auftrag**: Erzeugnis im Raster (mit Seiten, wie beim Brennofen) und Vorrat. Reiter **Einstellungen**: Zusammenschleifen an/aus, Entzaubern an/aus.

## 1. Schmieden auf Auftrag

Wie beim [Pfeilmacher](pfeilmacher.md): Erzeugnis und Vorrat wählen, er fertigt aus den Eingangskisten, bis so viele in den Ausgangskisten liegen.

| Erzeugnis | Zutaten (Vanilla-Rezept) |
| --- | --- |
| Axt, Spitzhacke | 3 Material, 2 Stöcke |
| Schaufel | 1 Material, 2 Stöcke |
| Hacke | 2 Material, 2 Stöcke |
| Schwert | 2 Material, 1 Stock |
| Speer | wie im Vanilla-Rezept |
| Schere | 2 Eisenbarren |

Material ist Bruchstein (Steinwerkzeug), Kupfer-, Eisen- oder Goldbarren oder Diamant; Holzwerkzeug macht der [Tischler](tischler.md). Netherit-Aufwertung bleibt beim Schmiedetisch und beim Spieler. Die Rezepte kommen aus den Crafting-Rezepten des Spiels, nicht aus einer festen Liste; Werkzeug und Waffen aus Datapacks zählen also mit (Filter: Ergebnis hat die Komponente `tool` oder `weapon`).

Ein Stück dauert 2 Sekunden. Am Schleifstein „schleift“ er die Klinge: Funken, das Vanilla-Geräusch des Schleifsteins.

## 2. Zusammenschleifen

Was ein Schleifstein in Vanilla kann: zwei beschädigte Teile derselben Art werden eines, mit der Haltbarkeit beider plus 5 %.

- Er holt **abgenutzte Werkzeuge** aus den Dorftruhen (die die Arbeiter dort zurückgeben, siehe [Werkzeugrückgabe](../dorf/werkzeugrueckgabe.md)) und aus seinen Eingangskisten, immer zwei derselben Art (zwei Eisenäxte).
- Das Ergebnis legt er dorthin zurück, woher er sie hatte.
- **Spart Barren:** Der Schmied repariert eine Eisenaxt mit Eisenbarren, der Waffenschmied macht aus zwei fast kaputten Äxten eine fast heile, ohne Material.
- Nie verzauberte oder umbenannte Teile: Der Schleifstein würde die Verzauberung entfernen. Die bleiben beim Schmied.
- Was übrig bleibt (eine einzelne Axt), repariert weiter der Schmied. Arbeiten beide im Dorf, nimmt der Waffenschmied nur Paare, der Schmied den Rest.

## 3. Entzaubern

- Verzaubertes aus seinen **Eingangskisten** (Beute der Wachen und des Fischers, Rüstung von Zombies, alte Bücher) entzaubert er am Schleifstein wie der Spieler: Alle Verzauberungen außer Flüchen gehen ab, verzauberte Bücher werden normale Bücher.
- Die **Erfahrung**, die der Schleifstein dafür gibt, füllt er in Erfahrungsfläschchen, 7 pro Flasche wie die Wachen (Config `guard.experiencePerBottle`). Glasflaschen holt er aus den Eingangskisten oder einer Dorftruhe (vom [Apotheker](apotheker.md)).
- Nur aus den Eingangskisten, nie aus Dorftruhen: Was entzaubert wird, entscheidet der Spieler, indem er es hineinlegt. Umbenannte Teile lässt er liegen.
- Die entzauberten Teile bringt er in die Ausgangskiste. Werkzeug davon geht über die Dorftruhe an die Arbeiter.

## Versorgung

| Was | Pflicht | Woher |
| --- | --- | --- |
| Barren, Bruchstein, Diamant | für den Auftrag | [Schmied](schmied.md), [Minenarbeiter](minenarbeiter.md) |
| Stöcke | für den Auftrag | [Holzsäger](holzsaeger.md) |
| Glasflaschen | fürs Entzaubern | [Apotheker](apotheker.md), Dorftruhe |

Werkzeug braucht er selbst keines.

## Arbeitsablauf

1. Sind abgenutzte Werkzeuge paarweise da und ist Zusammenschleifen an: holen, schleifen, zurückbringen.
2. Liegt Verzaubertes in der Eingangskiste: holen, entzaubern, Fläschchen füllen, abliefern.
3. Sonst den Auftrag: Material für einen Satz holen (4 Stück), fertigen, abliefern, bis der Vorrat voll ist.

## Status

„Sieht nach dem Auftrag“, „Holt Material“, „Schmiedet“, „Schleift zwei Äxte zusammen“, „Entzaubert“, „Füllt Erfahrung ab“, „Wartet auf Eisenbarren“, „Wartet: kein Auftrag“, „Vorrat ist voll“; im Menü „Auftrag: x, heute y“ und „Erfahrung: z (nicht abgefüllt)“.

## Kleidung

Rußgeschwärzte Lederschürze über einem rostroten Hemd, Lederhandschuhe, eine Schutzbrille auf der Stirn.

## Stufe 2 (später)

- **Nahkampfwachen:** Wachen mit Schwert oder Speer und Schild, die Monster an der Mauer aufhalten, während die Bogenschützen schießen. Der Waffenschmied versorgt sie, der [Tischler](tischler.md) macht die Schilde, der [Gerber](gerber.md) die Uniform.
- **Geschliffen:** Ein frisch geschliffenes Werkzeug arbeitet einen Tag lang schneller (kein Vanilla, nur eine Idee).

## Master-Fähigkeit

Zusammenschleifen ergibt 10 % statt 5 % Bonus.

## Offene Fragen

- **Auch Werkzeug oder nur Waffen?** Vanilla-Waffenschmiede verkaufen Schwerter und Äxte. Empfehlung: Werkzeug und Waffen. Nur Schwerter wären zu wenig Arbeit, und der Werkzeug-Nachschub fehlt dem Dorf am meisten.
- **Überschneidung mit dem Schmied?** Empfehlung: Der Schmied schmilzt und repariert mit Material, der Waffenschmied macht Neues und schleift. Beide nutzen die Rückgabe über die Dorftruhen; Paare gehen an den Schleifstein.
- **Entzaubern abschaltbar?** Ja, in den Einstellungen der Station, und nur aus den Eingangskisten.

## Siehe auch

[Schmied](schmied.md) · [Werkzeugrückgabe](../dorf/werkzeugrueckgabe.md) · [Jagd und Erfahrung](../dorf/erfahrung.md) · [Berufe](README.md)
