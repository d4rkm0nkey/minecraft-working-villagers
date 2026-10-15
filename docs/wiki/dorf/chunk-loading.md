# Betrieb ohne Spieler (Chunk-Loading)

[← Wiki-Start](../README.md)

**Stand der Umsetzung:** Noch nichts davon ist umgesetzt. Arbeiter arbeiten nur, solange ihre Chunks geladen sind, also meist nur in der Nähe eines Spielers.

Konzept: Gekaufte Dörfer halten ihre Chunks selbst geladen, solange die [Dorfkasse](dorf-und-kauf.md#dorfkasse) Löhne und Unterhalt deckt. Ein eigener Chunkloader-Block ist nicht nötig: Glocke und besetzte Workstations sind die Anker.

## Welche Chunks geladen bleiben

- **Dorfkern:** die Chunks um die Glocke (z. B. 3×3), damit Betten, Dorfkasse und Arbeiter auf dem Heimweg aktiv bleiben.
- **Pro besetzter Workstation:** der Chunk der Station, alle Chunks des Arbeitsbereichs und der verknüpften Kisten. Kisten liegen standardmäßig höchstens 16 Blöcke von der Station entfernt (Config, höchstens 32), deshalb sind das meist 2–6 Chunks.
- **Außenposten:** Stationen dürfen außerhalb des Dorfes stehen (z. B. eine Mine bis 256 Blöcke von der Glocke). Der Arbeiter braucht dann ein Bett am Außenposten; der Weg dazwischen wird nicht geladen.
- **Etagenmine:** nur die Chunks ihrer Grundfläche (3×3, mit Grubenlogistiker 5×5, mit allen Lorenrouten 7×7), egal wie viele Etagen; die günstigste Art, viel Erz zu fördern (siehe [Die Mine](../grundlagen/mine.md#chunk-loading)).
- **Stollen mit Schiene:** alle Chunks entlang der registrierten Strecke und der Depots, damit die Lore nicht in einem entladenen Chunk stehen bleibt (siehe [Grubenlogistiker](../berufe/grubenlogistiker.md#chunk-loading)).
- Unbesetzte Stationen laden nichts. Entlassen oder Tod gibt die Chunks sofort frei.

## Kosten & Grenzen

- Unterhalt pro geladenem Chunk, z. B. 1 Emerald pro Woche, aus der Dorfkasse. Macht Chunk-Loading zu einer bewussten Entscheidung.
- Ist die Kasse leer, bleibt das Dorf noch einen Ingame-Tag geladen, dann wird entladen und der Besitzer benachrichtigt.
- Server-Config: maximale Chunks pro Dorf und pro Spieler, Unterhalt an/aus, Modus „nur wenn Besitzer online“ (siehe [Multiplayer](../technik/multiplayer.md)).
- Das [Dorfbuch](dorfbuch.md) zeigt im Reiter „Dorf“ alle geladenen Chunks, ihre Kosten und welcher Arbeiter sie braucht.

## Wichtiger Haken: Random Ticks

In Vanilla laufen Random Ticks (Setzlinge, Feldfrüchte) und Mob-Spawning nur in der Nähe eines Spielers, auch in zwangsgeladenen Chunks (Verhalten im Prototyp auf der Zielversion prüfen). Ohne Gegenmaßnahme würden Bäume nicht wachsen, während der Spieler weg ist.

- Lösung: Der Mod gibt registrierten Setzlingen und Feldern im Arbeitsbereich selbst Wachstums-Ticks, mit derselben Rate wie Vanilla. Betrifft vor allem [Holzfäller](../berufe/holzfaeller.md) und [Farmer](../berufe/farmer.md).
- Nebeneffekt: Ohne Spieler in der Nähe spawnen keine Mobs. Arbeiter sind dann sicherer als im Beisein des Spielers; das ist akzeptabel.

## Technik

- NeoForge: eigener `TicketController` für erzwungene Chunks; Fabric: eigener `ChunkTicketType` bzw. `ServerWorld#setChunkForced`.
- Tickets werden beim Serverstart aus den Dorfdaten wiederhergestellt.
- Alternative für Server, die Chunk-Loading verbieten: **Aufholberechnung**. Beim Neuladen wird der Ertrag der verpassten Zeit grob berechnet (gedeckelt auf z. B. einen Ingame-Tag, mit Werkzeugverschleiß).

Siehe auch: [Technische Architektur](../technik/architektur.md#performance--chunks)
