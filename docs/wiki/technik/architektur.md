# Technische Architektur

[← Wiki-Start](../README.md)

Villager werden per Attachment/Component erweitert statt durch eine eigene Entity. So bleiben Handel, Schlaf, Gossip und Kompatibilität mit anderen Villager-Mods erhalten.

## Loader & Version

- Das Repository nutzt ein Multiloader-Setup (`common`, `fabric`, `neoforge`). Spiellogik gehört nach `common`, loaderspezifischer Code (Registrierung, Chunk-Tickets, Inventar-APIs) in die Loader-Module.
- Fabric: schlank, schnelle Updates. NeoForge: mächtigere Capabilities/Attachments für Inventare.
- Voraussetzung: eine Version mit Vanilla-Crafter (1.21+).

## Villager-Erweiterung

- Daten am Villager (Attachment): Arbeitgeber-UUID, Dorf-ID, Moral, Level, zugewiesene Station, Bedürfnisstatus.
- Neue Berufe als `VillagerProfession` + `PoiType` registrieren (Woodcutter, Grubenwerkbank, ggf. Handelstresen und Handelskontor). Vanilla-Berufe für den Ausbau wiederverwenden.
- Verhalten über das Brain-System (`Behavior`/`Activity`), nicht über alte `Goal`s: eigene Activity `EMPLOYED_WORK` ersetzt während der Arbeitszeit `WORK`.
- Jede Arbeit als kleine Zustandsmaschine (Task-Queue): HOLE_MATERIAL → GEHE_ZU → BEARBEITE_BLOCK → SAMMLE_DROPS → LIEFERE_AB.

## Workstation-BlockEntity

- Speichert Bereich (BlockBox), Quell-/Zielkisten (BlockPos-Liste), Modus, zugewiesenen Arbeiter.
- Liefert das GUI (Menu/Screen) und verwaltet die Job-Planung (welcher Block als Nächstes).
- Kisten werden über die Standard-Inventar-API angesprochen (Fabric Transfer API bzw. NeoForge `IItemHandler`), damit Mod-Kisten funktionieren.

## Blockabbau & Platzierung

- Abbau über eine FakePlayer-ähnliche Logik mit Werkzeug-Haltbarkeit und Loot-Tables, damit Drops und Enchants stimmen.
- Abbauzeit realistisch nach Härte und Werkzeug, mit Crack-Animation.
- Respektiert Claim-/Protection-Mods: Aktionen im Namen des Arbeitgebers prüfen.

## Performance & Chunks

- Pathfinding ist der teuerste Teil: Ziele bündeln, Pfade cachen, nur alle n Ticks neu planen.
- Bereichs-Scan inkrementell (z. B. 64 Blöcke pro Tick), nicht der ganze Bereich auf einmal.
- Chunk-Loading über gekaufte Dörfer und besetzte Workstations, inklusive eigener Wachstums-Ticks für Arbeitsbereiche (siehe [Chunk-Loading](../dorf/chunk-loading.md)).

## Datenhaltung & Netzwerk

- Dorf-, Register- und Arbeitgeberdaten in `SavedData` pro Welt.
- Client-Sync nur für GUIs (Dorfbuch, Workstation) und Bereichs-Visualisierung.
- Config (serverseitig): Löhne, Bereichsgrößen, Arbeitszeiten, Moral an/aus, Bedürfnisse an/aus, Chunk-Limits, Dorfpreis-Formel.

Siehe auch: [Multiplayer](multiplayer.md) · [Risiken](../planung/risiken-und-fragen.md)
