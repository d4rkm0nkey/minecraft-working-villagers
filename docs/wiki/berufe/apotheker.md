# Beruf: Apotheker

[← Berufe](README.md) · Station: **Braustand** (Vanilla) · Phase: **umgesetzt** (Flaschen, Wasser, Brauen, Heilen, Netherwarzen; ohne Zombie-Dorfbewohner heilen und Tränke für Wachen)

Der Apotheker (Vanilla: Kleriker) braut Tränke am Braustand. Allein wäre „Tränke brauen“ langweilig; spannend wird er, weil er das Dorf versorgt (Flaschen, Heilung) und seine Zutaten aus vielen Betrieben kommen.

## Stand der Umsetzung

- **Station:** Vanilla-Braustand (Kleriker-POI). Die Reiter über dem Braustand-Menü führen zur Übersicht (Mixins auf `BrewingStandMenu` und `BrewingStandScreen`). Vanilla-Kleriker lassen sich anheuern, behalten ihre Angebote und handeln wie Kleriker (Handelsfamilie `cleric`).
- **Auftrag** im Raster wie beim Brennofen: Glasflaschen, Wasserflaschen und jeder Trank und Wurftrank, zu dem die Braurezepte (`minecraft:brewing`, auch aus Datapacks) von einer Wasserflasche aus führen (seltsamer, gewöhnlicher und dickflüssiger Trank ausgenommen); dazu ein **Vorrat**. Der Auftrag ist ein Item plus Trank; der Trank steht im Feld `woods` der Station, das der Braustand sonst nicht braucht.
- **Rezeptweg** (`BrewingPaths`): Breitensuche über die Braurezepte ab der Wasserflasche, der erste gefundene Weg ist der kürzeste. Heiltrank: Wasser → Netherwarze (seltsamer Trank) → glitzernde Melone. Wurftrank der Heilung: zusätzlich Schwarzpulver.
- **Glasflaschen:** 3 Glas → 3 Flaschen am Braustand, eine Runde pro Sekunde.
- **Wasser:** Glasflaschen füllt er an einer Wasserquelle im Umkreis von 8 Blöcken (Config `apothecary.waterRadius`), an einem Ufer, auf dem er stehen kann. Ein Wasserkessel zählt nicht.
- **Brauen am echten Braustand:** bis zu 3 Flaschen der weitesten Stufe, das Reagenz des nächsten Schritts, und nur wenn der Stand leer ist, ein Lohenstaub (ein Pulver reicht für 20 Brauvorgänge). Dann wartet er die 20 Sekunden ab (Brauzeit und Brennstoff liest er über einen Accessor) und nimmt die Flaschen wieder heraus. Braut gerade jemand anders, wartet er. Was nach einem Abbruch (Feierabend) im Stand liegt, holt er beim nächsten Mal heraus.
- **Holen:** Flaschen für einen Satz (weiteste Stufe zuerst, dann Glasflaschen, dann Glas), ein Reagenz je ausstehendem Schritt, Lohenstaub. Was der Auftrag nicht braucht, legt er zurück; Tränke eines früheren Auftrags liefert er ab. Das Inventar hat 8 Plätze, Tränke stapeln nicht: deshalb immer nur 3 Flaschen.
- **Heilen:** Angeheuerte Dorfbewohner im Umkreis von 32 Blöcken (Config `apothecary.healRadius`), denen mindestens zwei Herzen fehlen, bekommen einen Heiltrank aus seinem Inventar oder aus einer Kiste der Station (Ein- oder Ausgang). Ein Trank zum Trinken lässt eine Glasflasche zurück, die er wieder benutzt. Wer nicht erreichbar ist, bleibt eine Minute außen vor.
- **Netherwarzen:** Reife Warzen auf Seelensand im Umkreis von 6 Blöcken (Config `apothecary.wartRadius`) erntet er und pflanzt neu; freien Seelensand bepflanzt er, wenn er mehr als eine Warze trägt (eine behält er zum Brauen).
- **Status:** „Sieht nach dem Auftrag“, „Holt Material“, „Stellt Glasflaschen her“, „Füllt Wasser ab“, „Braut“, „Heilt einen Arbeiter“, „Pflegt die Netherwarzen“, „Wartet auf %s“, „Wartet auf Lohenstaub“, „Kein Wasser beim Braustand“, „Wartet: Braustand ist belegt“, „Kein Braurezept führt zum Auftrag“, „Vorrat ist voll“; dazu „Auftrag: x, heute y“.
- **Kleidung:** cremefarbener langer Kittel mit flaschengrüner Schürze, Ledergürtel mit drei Fläschchen (rot, blau, grün), runde grüne Kappe.
- **Tests:** 6 Glas → 6 Flaschen; 3 Glasflaschen an einer Pfütze → 3 Wasserflaschen; aus Glasflaschen, Netherwarze, glitzernder Melone und Lohenstaub 3 Heiltränke; ein verletzter Arbeiter wird mit dem Heiltrank aus der Ausgangskiste geheilt; reife Netherwarze wird geerntet und neu gepflanzt.
- **Nicht umgesetzt:** Zombie-Dorfbewohner heilen, Tränke für Wachen vor der Schicht, getränkte Pfeile (Verweiltränke brauchen Drachenatem), Wasser aus dem Kessel, Meisterfähigkeit.

## Woher kommt was?

| Zutat | Herkunft |
| --- | --- |
| Glas | [Brenner](brenner.md) (Sand vom [Grubenarbeiter](grubenarbeiter.md)) |
| Netherwarze | einmal aus dem Nether, dann baut er sie selbst an |
| Zucker, glitzernde Melone | [Farmer](farmer.md); Goldklumpen vom [Schmied](schmied.md) |
| Spinnenauge, Schwarzpulver | Beute der [Wachen](wache.md#beute) |
| Lohenstaub | Spieler (Nether); der Grund, in den Nether zu gehen |

## Wofür das Dorf ihn braucht

1. **Glasflaschen** für die Erfahrungsfläschchen der [Wachen](wache.md) und damit für die Reparaturen des [Schmieds](schmied.md) (siehe [Jagd und Erfahrung](../dorf/erfahrung.md)).
2. **Heilung:** verletzte Arbeiter, vor allem Wachen nach einer harten Nacht.
3. Tränke für den Spieler und zum Handeln.

## Stufe 2 (später)

- **Zombie-Dorfbewohner heilen:** Wurftrank der Schwäche plus goldener Apfel, das Vanilla-Verfahren. So kommen nach Überfällen neue Dorfbewohner ins Dorf, und offene Einträge im [Register](../dorf/dorfbuch.md#register) schließen sich.
- **Wachen stärken:** Vor der Schicht trinken Wachen Nachtsicht oder Stärke, wenn an der Station eingestellt.
- **Getränkte Pfeile** für den [Pfeilmacher](pfeilmacher.md) (Verweiltränke, also Drachenatem: Endgame).

## Master-Fähigkeit

Verlängerte oder verstärkte Tränke ohne Zusatzzutat.

## Siehe auch

[Jagd und Erfahrung](../dorf/erfahrung.md) · [Wache](wache.md) · [Brenner](brenner.md) · [Dorfbuch](../dorf/dorfbuch.md) · [Berufe](README.md)
