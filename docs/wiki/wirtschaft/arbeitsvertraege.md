# Arbeitsverträge je Beruf

[← Wiki-Start](../README.md) · Phase: **Idee** (Wunsch vom 15.10.2026)

Statt eines einzigen, craftbaren Arbeitsvertrags gibt es **für jeden Beruf einen eigenen Vertrag**. Verträge lassen sich **nicht craften**: Man findet sie in Truhen von Strukturen in der Welt. Wer einen Holzfäller will, braucht einen Holzfäller-Vertrag; wer einen Grubenarbeiter will, muss erst einen Grubenarbeiter-Vertrag finden.

## Warum

- **Erkunden lohnt sich.** Dörfer, Minenschächte und Außenposten werden zu Fundorten für Personal. Der Fortschritt im Mod hängt an Abenteuern, nicht nur an Emeralds.
- **Langsamer, bewusster Aufbau.** Heute kann man mit genug Emeralds beliebig viele Arbeiter auf einmal anheuern. Mit gefundenen Verträgen ist jeder Angestellte etwas Besonderes, und die Reihenfolge der Berufe ergibt sich aus dem, was man findet.
- **Klarere Zuordnung.** Der Vertrag sagt, wofür der Dorfbewohner angeheuert ist. Er sucht sich nur Stationen seines Berufs und landet nicht zufällig am falschen Block.

## Die Verträge

Ein Vertrag je Beruf, mit dem Beruf im Namen und im Tooltip, z. B. „Arbeitsvertrag: Holzfäller“.

| Vertrag | Beruf | Station |
| --- | --- | --- |
| Holzfäller | [Holzfäller](../berufe/holzfaeller.md) | Hackklotz |
| Holzsäger | [Holzsäger](../berufe/holzsaeger.md) | Holzsäge |
| Steinsäger | [Steinsäger](../berufe/steinsaeger.md) | Steinsäge |
| Tischler | [Tischler](../berufe/tischler.md) | Hobelbank |
| Bauer | [Bauer](../berufe/farmer.md) | Komposter |
| Schmied | [Schmied](../berufe/schmied.md) | Schmelzofen |
| Grubenarbeiter | [Grubenarbeiter](../berufe/grubenarbeiter.md) | Grubenpflock |
| Brenner | [Brenner](../berufe/brenner.md) | Brennofen |

Neue Berufe bringen ihren Vertrag mit. Der [Träger](../berufe/traeger.md) bleibt bei seinem Weidenkorb, der Vertrag und Workstation zugleich ist (offene Frage unten).

**Technisch** ein einziges Item `workingvillagers:work_contract` mit einer Datenkomponente für den Beruf (`workingvillagers:contract_profession`), nicht ein Item pro Beruf. Das hält Registrierung, Loot-Funktion und Texturen klein; das Siegel auf dem Vertrag bekommt die Farbe des Berufs (Item-Model mit Tint je Komponente). Die Creative-Tab zeigt je Beruf einen Vertrag.

## Anheuern mit einem Berufsvertrag

- Rechtsklick auf einen arbeitslosen, erwachsenen Dorfbewohner wie heute. Er ist angeheuert **für diesen Beruf**, der Vertrag ist verbraucht.
- Er sucht sich nur freie Stationen seines Berufs ([Station finden](anheuern-und-lohn.md#stand-der-umsetzung)). Gibt es keine, wartet er und sagt es im Status („Sucht einen Hackklotz“).
- **Zuweisen** an eine Station eines anderen Berufs geht nicht: „Dieser Dorfbewohner ist als Holzfäller angeheuert.“
- **Entfernen** von der Station: Er bleibt für seinen Beruf angeheuert und sucht sich die nächste passende Station.
- Der Beruf steht im Menü der Station und im späteren [Dorfbuch](../dorf/dorfbuch.md).
- Umschulen ist nicht vorgesehen: Ein anderer Beruf braucht einen anderen Dorfbewohner und einen neuen Vertrag.

## Fundorte

Verträge liegen als zusätzlicher Eintrag in den Truhen von Vanilla-Strukturen (Loot-Tabellen werden ergänzt, nicht ersetzt). Jede Struktur liefert die Berufe, die zu ihr passen. Chancen sind Startwerte für Playtests.

| Struktur (Loot-Tabelle) | Verträge | Chance je Truhe |
| --- | --- | --- |
| Dorf: Werkzeugschmied (`village_toolsmith`) | Holzfäller, Grubenarbeiter | 30 % |
| Dorf: Steinmetz (`village_mason`) | Steinsäger, Brenner | 30 % |
| Dorf: Rüstungs- und Waffenschmied (`village_armorer`, `village_weaponsmith`) | Schmied | 30 % |
| Dorf: Häuser (`village_plains_house`, `_taiga_`, `_savanna_`, `_desert_`, `_snowy_`) | Bauer, Tischler, Holzsäger | 15 % |
| Dorf: Pfeilmacher, Hirte, Gerberei, Kartograf, Metzger, Fischer | ein passender späterer Beruf, sonst Bauer | 10 % |
| Verlassener Minenschacht (`abandoned_mineshaft`) | Grubenarbeiter, Brenner | 10 % |
| Verlassenes Lager (`abandoned_camp_common_chest`) | Holzfäller, Holzsäger | 15 % |
| Plünderer-Außenposten (`pillager_outpost`) | jeder Beruf | 20 % |
| Waldanwesen (`woodland_mansion`) | jeder Beruf, bis zu 2 | 30 % |
| Schiffswrack, Vorräte (`shipwreck_supply`) | Träger-Weidenkorb (falls er nicht craftbar bleibt) | 10 % |

- Gewichtung innerhalb einer Zeile: gleich verteilt. Ein Datapack kann alles über Loot-Tabellen ändern.
- Höchstens ein Vertrag pro Truhe (Ausnahme Waldanwesen), damit eine Truhe nicht gleich ein ganzes Dorf liefert.
- **Startdorf-Problem:** Damit man nicht stundenlang suchen muss, liegt in Dörfern (Werkzeugschmied, Steinmetz, Häuser) die Grundausstattung für die frühen Berufe: Holzfäller, Bauer, Holzsäger. Seltene Berufe (Grubenarbeiter, Brenner, Schmied) eher in Minenschächten, Außenposten, Anwesen.
- Optional: Bonus-Truhe beim Weltstart (`spawn_bonus_chest`) enthält einen Holzfäller-Vertrag.

**Technisch:** Loot-Tabellen ergänzen geht nicht loader-übergreifend. Fabric: `LootTableEvents.MODIFY`; NeoForge: Global Loot Modifier (Datei je Fundort, damit Datapacks sie abschalten können). Gemeinsam ist eine Liste „Loot-Tabelle → Pool mit Verträgen und Chance“ im Common-Code, die die Plattform-Schicht ([`Platform`](../technik/architektur.md)) anwendet. Die Loot-Funktion setzt die Berufskomponente.

## Nicht craftbar

- Das Rezept für den Arbeitsvertrag entfällt.
- **Config** `workers.craftableContracts` (Standard `false`): Wer lieber craftet, schaltet das alte Rezept wieder ein, dann als Rezept je Beruf (Papier, Tintenbeutel, 3 Emeralds und ein Kennzeichen-Item, z. B. eine Axt für den Holzfäller, ein Kompost-Item für den Bauern).
- **Config** `workers.contractLootChance` (Faktor, Standard 1.0) skaliert alle Fundchancen; 0 schaltet die Funde ab (für Server, die Verträge nur über Shops oder Befehle verteilen).

## Weitere Wege an Verträge (optional)

- **Handel:** Der Bibliothekar (Meister) verkauft ab und zu einen zufälligen Vertrag für viele Emeralds. Passt zu den [Berufsbüchern](../grundlagen/berufsbuecher.md), die Dorfbewohner ebenfalls verkaufen.
- **Belohnung:** Ein Dorf, dessen [Dorfkasse](../dorf/dorf-und-kauf.md) gut gefüllt ist, legt nach einer Woche einen Vertrag in die Kasse.
- Befehl für Admins: `/workingvillagers contract <beruf>` (oder einfach `/give` mit der Komponente).

## Übergang von heute

- Bereits angeheuerte Dorfbewohner bleiben angeheuert. Wer gerade arbeitet, bekommt den Beruf seiner Station fest zugeordnet; wer ohne Station ist, bleibt „frei“ und darf wie heute jede Station nehmen, bis er eine hat.
- Vorhandene alte Verträge (ohne Beruf) bleiben gültig als **„Allgemeiner Arbeitsvertrag“**: Sie heuern wie heute für jeden Beruf an. Man bekommt sie nur nicht mehr neu.
- Wiki, Berufsbücher und „Erster Arbeiter“-Anleitung beschreiben dann das Finden statt des Craftens; der Einstieg zeigt, wo man den ersten Holzfäller-Vertrag am ehesten findet.

## Tests (beim Umsetzen)

- Ein Holzfäller-Vertrag heuert an, der Dorfbewohner nimmt einen freien Hackklotz und ignoriert eine freie Holzsäge daneben.
- Zuweisen an die falsche Station scheitert mit Meldung.
- Ein allgemeiner (alter) Vertrag heuert für jeden Beruf an.
- Die ergänzten Loot-Tabellen lassen sich laden und enthalten den Vertrag mit Berufskomponente (Loot-Tabelle auswerten, Komponente prüfen).
- Mit `craftableContracts = false` gibt es kein Rezept.

## Offene Fragen

- **Träger:** Bleibt der Weidenkorb craftbar, oder wird auch er zum Fund (Schiffswrack, Fischerhütte)? Er ist heute Vertrag und Werkzeug zugleich; ein verlorener Korb wäre ohne Rezept ärgerlich.
- **Wie selten?** Ein Dorf hat etwa 1–3 passende Truhen. Reicht das für den Start, oder braucht es den Vertrag in der Bonus-Truhe bzw. einen Händler?
- **Mehrspieler:** Truhen sind einmalig. Auf Servern mit vielen Spielern sind Dörfer in Spawn-Nähe schnell geplündert; dann helfen nur der Händler oder Server-Shops.
- **Kündigen:** Bekommt man den Vertrag zurück, wenn man einen Angestellten entlässt (heute gibt es kein Entlassen)? Sonst wäre ein Fehlgriff teuer.
- **Berufsbuch statt Vertrag?** Alternativ könnte man den Vertrag allgemein lassen und das passende [Berufsbuch](../grundlagen/berufsbuecher.md) als Voraussetzung nehmen. Verworfen, weil Bücher erklären sollen, nicht freischalten.

## Siehe auch

[Anheuern & Lohn](anheuern-und-lohn.md) · [Berufsbücher](../grundlagen/berufsbuecher.md) · [Roadmap](../planung/roadmap.md) · [Berufe](../berufe/README.md)
