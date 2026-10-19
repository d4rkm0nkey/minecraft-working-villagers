# Beruf: Pfeilmacher

[← Berufe](README.md) · Station: **Bognertisch** (Vanilla) · Phase: **umgesetzt** (Pfeile, Spektralpfeile, Bögen, Armbrüste; ohne getränkte Pfeile und eigenes Bognertisch-Menü)

Der Pfeilmacher versorgt die [Wachen](wache.md) des Dorfes mit Pfeilen und Bögen. Allein wäre „Pfeile craften“ langweilig; spannend wird er erst als Mittelstück eines Kreislaufs: Wachen **verbrauchen** jede Nacht Pfeile, und die Zutaten kommen aus drei anderen Betrieben. Fällt einer aus, gehen den Wachen nach ein paar Nächten die Pfeile aus.

## Stand der Umsetzung

- **Mehrere Produkte** (0.5.0): bis zu 8 Produkte je Auftrag, zu gleichen Teilen, Vorrat je Produkt; siehe [Tischler](tischler.md#mehrere-produkte-umgesetzt-in-050).
- **Station:** Bognertisch, Rechtsklick mit leerer Hand öffnet die Übersicht (der Bognertisch hat in Vanilla kein Menü). Eingangs- und Ausgangskisten. Vanilla-Pfeilmacher lassen sich anheuern, behalten ihre Angebote; der Pfeilmacher handelt wie ein Vanilla-Pfeilmacher.
- **Auftrag** im Raster wie beim Brennofen: Pfeile, Spektralpfeile, Bogen, Armbrust; dazu ein **Vorrat** (zählt das Produkt in den Ausgangskisten).
- **Spitzen:** Feuerstein, Kupfer- oder Eisennugget, in dieser Reihenfolge. Fehlen sie, schlägt er Feuerstein aus Kies (1:1) oder zerlegt einen Barren (1 → 9 Nuggets, der Rest bleibt im Inventar). Alles ergibt 4 Pfeile pro Spitze wie im Vanilla-Rezept.
- **Ablauf:** Material für einen Satz holen (16× Pfeile, 8× Spektralpfeile, 4 Bögen oder Armbrüste), am Bognertisch ein Stück pro Sekunde fertigen, abliefern. Was der Auftrag nicht braucht, legt er beim nächsten Holen in die Eingangskiste zurück; Erzeugnisse eines früheren Auftrags liefert er ab. Findet er in den Eingangskisten nichts, versucht er es erst nach 30 Sekunden wieder.
- **Status:** „Sieht nach dem Auftrag“, „Holt Material“, „Fertigt Pfeile und Bögen“, „Wartet: kein Auftrag“, „Wartet auf Feder“ (das fehlende Material), „Vorrat ist voll (x/y)“, „Kommt nicht an den Bognertisch“, dazu „Heute: x“ (gefertigte Stücke).
- **Kleidung:** waldgrüner Kittel mit Köcherriemen, brauner Filzhut mit roter Feder.
- **Tests:** 6 Kies, Stöcke und Federn ergeben 24 Pfeile; ein Eisenbarren ergibt 36 Pfeile; bei einem Vorrat von 8 hört er bei 8 Pfeilen auf.
- **Nicht umgesetzt:** getränkte Pfeile (braucht Verweiltränke vom [Apotheker](apotheker.md)), ein eigenes Bognertisch-Menü für Spieler, Wahl der Spitzen im Auftrag (er nimmt sie in fester Reihenfolge).

## Der Kreislauf

```
Gruben-/Minenarbeiter ──Kies──┐
Holzsäger / Tischler ──Stöcke─┼──► Pfeilmacher ──Pfeile, Bögen──► Dorftruhe ──► Wachen ──► verschossen
Metzger (Hühner) ────Federn───┘         ▲                                          │
                                        └──── aufgesammelte Fehlschüsse ◄──────────┘
```

- **Verbrauch statt Lager:** Was eine Wache trifft, ist weg; Fehlschüsse sammelt sie morgens zum Teil wieder ein. So entsteht eine dauernde Nachfrage, ohne dass etwas künstlich verschwindet.
- **Engpass sind die Federn**, nicht der Feuerstein (siehe [Rechnung](#reicht-das-eine-rechnung)). Das macht den [Metzger](metzger.md) mit einer Hühnerherde wichtig.

## Woher kommt der Feuerstein?

Vanilla gibt Feuerstein nur zu 10 % beim Abbau von Kies. Der Pfeilmacher **schlägt ihn selbst aus Kies**, 1 Kies → 1 Feuerstein, wie die Mühle der [Grubenwerkbank](minenarbeiter.md). Kies gibt es genug:

- Der [Grubenarbeiter](grubenarbeiter.md) gräbt eine Kiesgrube, der [Minenarbeiter](minenarbeiter.md) bringt Kies aus dem Stollen.
- Bruchstein (fällt in der Mine massenhaft an) wird in der Mühle zu Kies; das macht vorerst der Spieler.
- Kies an Stränden und Flüssen.

### Andere Spitzen: Metall-Nuggets

Statt Feuerstein nimmt er auch **Eisen- oder Kupfernuggets** als Spitze: 1 Nugget + Stock + Feder → 4 Pfeile, genau wie mit Feuerstein. Ein Eisenbarren (9 Nuggets) reicht für 36 Pfeile. Er zerlegt Barren dafür selbst in Nuggets. Kupfer ist reichlich da und sonst wenig gebraucht; damit hat Kupfer einen Nutzen.

Die Pfeile bleiben **normale Vanilla-Pfeile**; nur die Spitze, aus der er sie macht, ist frei wählbar. Was er nimmt, stellt man im Auftrag ein (Feuerstein zuerst, dann Kupfer, dann Eisen – oder nur eins davon).

**Später (Stufe 2, offen):** eigene Pfeile mit Wirkung, z. B. Eisenpfeile mit mehr Schaden, Kupferpfeile, die Blitze anziehen. Das braucht eigene Items und Geschosse; erst, wenn der Kreislauf trägt.

## Was er macht

Wie beim [Tischler](tischler.md) wählt man im Reiter **Auftrag** Produkte und einen **Vorrat**: Er arbeitet, bis so viele in den Ausgangskisten (oder der Dorftruhe) liegen, und macht dann Pause.

| Produkt | Zutaten | Wofür |
| --- | --- | --- |
| Pfeile | Feuerstein oder Nugget, Stock, Feder (→ 4) | Wachen, Spieler, Handel |
| Bogen | 3 Stöcke, 3 Fäden | Wachen (Bögen nutzen sich ab) |
| Armbrust | 3 Stöcke, 2 Fäden, Eisenbarren, Stolperdrahthaken | Wachen mit Armbrust (später) |
| Spektralpfeile | 4 Glowstone-Staub, 1 Pfeil (→ 2) | Leuchtende Treffer, Wachen sehen Mobs im Dunkeln |
| Getränkte Pfeile | 8 Pfeile, 1 Verweiltrank (→ 8) | Verweiltränke braucht der [Apotheker](apotheker.md) (Drachenatem) |

Faden kommt von Spinnen: Wachen, die nachts Spinnen erlegen, bringen ihn mit (siehe [Wache](wache.md#beute)).

## Station: Bognertisch

- In Vanilla hat der Bognertisch keine Funktion. Rechtsklick öffnet daher direkt die Stationsübersicht (wie beim Hackklotz).
- Vanilla-Pfeilmacher arbeiten weiter an Bognertischen, außer an eingerichteten; angeheuert handelt er wie ein Vanilla-Pfeilmacher (siehe [Anheuern](../wirtschaft/anheuern-und-lohn.md#stand-der-umsetzung)).
- Eingangskisten: Kies, Nuggets, Barren, Stöcke, Federn, Fäden, Glowstone. Ausgangskisten: am besten eine [Dorftruhe](../dorf/dorftruhen.md), aus der sich die Wachen bedienen.
- Idee: Für Spieler bekommt der Bognertisch ein eigenes Menü, in dem man Pfeile aus Nuggets und getränkte Pfeile herstellt. Dann hätte der Block auch ohne Arbeiter einen Sinn.

## Reicht das? Eine Rechnung

Annahmen: Ein gut beleuchtetes Dorf, 2 Wachen, pro Nacht etwa 10 Monster in Reichweite. Ein Zombie braucht 3–4 Treffer, dazu jeder zweite Schuss ein Fehlschuss → rund 6 Pfeile pro Monster, also **60 Pfeile pro Nacht**. Die Hälfte der Fehlschüsse wird morgens eingesammelt → etwa **45 Pfeile Verbrauch**.

| Zutat | pro Nacht | Herkunft |
| --- | --- | --- |
| Federn | 12 | 1 Huhn ≈ 1 Feder; der [Metzger](metzger.md) braucht eine Hühnerherde von ~16 |
| Feuerstein/Nuggets | 12 | 12 Kies – eine halbe Grubenschicht |
| Stöcke | 12 | 6 Bretter, nebenbei vom [Holzsäger](holzsaeger.md) |

Weniger Licht im Dorf → mehr Monster → mehr Pfeile. Das ist gewollt: Beleuchten spart Pfeile.

## Arbeitsablauf

1. Auftrag und Vorrat prüfen. Ist der Vorrat voll, wartet er.
2. Zutaten für einen Satz (z. B. 16 Pfeile) aus den Eingangskisten holen; Kies zuerst zu Feuerstein schlagen, Barren zu Nuggets.
3. Am Bognertisch fertigen (etwa 1 Sekunde pro 4 Pfeile).
4. In die Ausgangskiste oder Dorftruhe bringen.

## Offene Fragen

- **Nur Vanilla-Pfeile in Stufe 1?** Empfehlung: ja; eigene Pfeilarten erst in Stufe 2.
- **Nugget-Ausbeute:** 4 Pfeile pro Nugget (wie Feuerstein) ist großzügig: ein Barren gibt 36 Pfeile. Alternative: 2 pro Nugget.
- **Federn ersetzen?** Vanilla kennt keine Pfeile ohne Federn. Empfehlung: dabei bleiben; der Engpass macht den Metzger wertvoll.

## Siehe auch

[Wache](wache.md) · [Metzger](metzger.md) · [Grubenarbeiter](grubenarbeiter.md) · [Dorftruhen](../dorf/dorftruhen.md)
