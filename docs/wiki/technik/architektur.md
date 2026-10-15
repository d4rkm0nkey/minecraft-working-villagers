# Technische Architektur

[← Wiki-Start](../README.md)

Villager werden erweitert statt durch eine eigene Entity ersetzt. So bleiben Handel, Schlaf, Gossip und Kompatibilität mit anderen Villager-Mods erhalten.

## Stand der Umsetzung

- **Loader:** Minecraft 26.3, Fabric und NeoForge parallel im Multiloader-Setup. Spiellogik in `common`, die Loader-Module registrieren nur.
- **Daten am Villager:** zusätzliche Felder per Mixin (`VillagerMixin`, Schnittstelle `HiredVillager`), gespeichert mit dem Villager: Arbeitgeber, Trägerdaten (Wege, Korb), Tag des letzten Lohns, Tagesleistung. Keine Attachments/Components.
- **Berufe:** eigene `VillagerProfession`s. Holzfäller, Holzsäger und Tischler haben eigene POIs (Hackklotz, Holzsäge, Hobelbank); Steinsäger, Bauer und Schmied nutzen die Vanilla-POIs von Steinsäge, Komposter und Schmelzofen, bekommen aber den Beruf des Mods. Der Träger hat keinen POI.
- **Verhalten:** Brain-System. Statt einer eigenen Activity wird für die Berufe des Mods das Paket der Vanilla-Activity `WORK` ersetzt (`VillagerGoalPackagesMixin`); die Arbeitszeit ist also die von Vanilla. Jede Arbeit ist ein `Behavior` mit kleiner Zustandsmaschine (z. B. `ChopTrees`, `CutAtStation`, `CraftAtBench`, `FarmFields`, `SmeltAtFurnace`, `CarryItems`).
- **Stationen:** keine BlockEntity. Kisten, Auftrag und Arbeiter jeder Station liegen in `SavedData` pro Dimension (`Workstations`), weil auch Vanilla-Blöcke wie die Steinsäge Stationen sind.
- **Kisten:** über den Vanilla-`Container` (wie ein Trichter sie findet), nicht über Transfer API oder `IItemHandler`. Kisten anderer Mods gehen, soweit sie ein Vanilla-Container sind.
- **Blockabbau:** Der Holzfäller nimmt die normalen Drops des Blocks (`Block.getDrops`) und nutzt die Axt pro Stamm ab, ohne FakePlayer. Claim-/Protection-Mods werden noch nicht geprüft; stattdessen gilt die Spielregel `mob_griefing`.
- **Config:** `config/workingvillagers.json`, die Werte des Servers gelten.

Der Rest der Seite ist das ursprüngliche Konzept.

## Loader & Version

- Das Repository nutzt ein Multiloader-Setup (`common`, `fabric`, `neoforge`). Spiellogik gehört nach `common`, loaderspezifischer Code (Registrierung, Chunk-Tickets, Inventar-APIs) in die Loader-Module.
- Fabric: schlank, schnelle Updates. NeoForge: mächtigere Capabilities/Attachments für Inventare.
- Voraussetzung: eine Version mit Vanilla-Crafter (1.21+).

## Villager-Erweiterung (Konzept)

- Daten am Villager (Attachment): Arbeitgeber-UUID, Dorf-ID, Moral, Level, zugewiesene Station, Bedürfnisstatus.
- Neue Berufe als `VillagerProfession` + `PoiType` registrieren (Hackklotz, Holzsäge, Hobelbank, Grubenwerkbank, ggf. Handelstresen und Handelskontor). Vanilla-Berufe für den Ausbau wiederverwenden.
- Verhalten über das Brain-System (`Behavior`/`Activity`), nicht über alte `Goal`s: eigene Activity `EMPLOYED_WORK` ersetzt während der Arbeitszeit `WORK`.
- Jede Arbeit als kleine Zustandsmaschine (Task-Queue): HOLE_MATERIAL → GEHE_ZU → BEARBEITE_BLOCK → SAMMLE_DROPS → LIEFERE_AB.

## Workstation-BlockEntity (Konzept, umgesetzt als SavedData)

- Speichert Bereich (BlockBox), Quell-/Zielkisten (BlockPos-Liste), Modus, zugewiesenen Arbeiter.
- Liefert das GUI (Menu/Screen) und verwaltet die Job-Planung (welcher Block als Nächstes).
- Kisten werden über die Standard-Inventar-API angesprochen (Fabric Transfer API bzw. NeoForge `IItemHandler`), damit Mod-Kisten funktionieren.

## Blockabbau & Platzierung (Konzept)

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
