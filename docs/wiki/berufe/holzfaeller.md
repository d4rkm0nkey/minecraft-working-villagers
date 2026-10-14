# Beruf: Holzfäller

[← Berufe](README.md) · Station: **Woodcutter** (neu) · Phase: **M1 (MVP)**

Der Holzfäller betreibt eine Baumfarm im markierten Bereich: pflanzen, warten, fällen, aufsammeln, abliefern.

## Workstation: Woodcutter

- Funktioniert für Spieler wie der Stonecutter: 1 Stamm → 4 Bretter (statt Crafting), 1 Stamm → Treppen, Stufen, Zäune, Türen usw. ohne Verschnitt.
- Rezept: 1 Eisenbarren + 3 Bretter (analog zum Stonecutter mit Eisen + Stein).
- Ist der POI für den Beruf. Es gibt bereits Woodcutter-Mods, das Konzept ist also akzeptiert; eine eigene Implementierung vermeidet Abhängigkeiten.
- Rezepte als eigener Recipe-Type `woodcutting`, per Datapack erweiterbar (wichtig für Holz aus anderen Mods).

## Arbeitsablauf

1. Setzlinge und Axt aus der Quellkiste holen.
2. Pflanzraster im Bereich anlegen (Abstand 2–3 Blöcke, je Baumart; 2×2 für Dark Oak, Fichte, Dschungel).
3. Optional Knochenmehl aus der Quellkiste nutzen.
4. Ausgewachsenen Baum erkennen und fällen: Stamm von unten nach oben abbauen, Blätter verfallen natürlich oder werden mit Schere/Hacke entfernt.
5. Drops einsammeln: Stämme, Setzlinge, Äpfel, Stöcke. Setzlinge gehen zurück in den eigenen Vorrat.
6. Ertrag in die Zielkiste legen.

## Quellkiste / Zielkiste

| Quellkiste | Zielkiste |
| --- | --- |
| Axt, Setzlinge, optional Knochenmehl, Schere | Stämme (oder Bretter/Stufen), Äpfel, Stöcke, überzählige Setzlinge |

## Modus im GUI

- Baumart wählen (oder „was in der Quellkiste ist“).
- Optional: direkt im Woodcutter verarbeiten. Dann landen Bretter oder Stufen in der Zielkiste statt Stämme (langsamer, da Zwischenschritt).

## Entrinden (umgesetzt)

- Schalter „Stämme entrinden“ im Menü des Hackklotzes, Standard aus. Ist er an, kommen entrindete Stämme in die Zielkiste.
- Der Holzfäller entrindet beim Fällen: Für jeden Stamm wertet er die Entrinden-Regeln seiner Axt aus (seit 26.3 datengetrieben über die Komponente `minecraft:block_transformer`, Datei `block_transformer/axe.json`). Holz aus Datapacks und Mods funktioniert damit ohne eigene Liste; auf NeoForge zählen auch die Regeln, die Mods per Data Map anhängen.
- Der Block in der Welt wird dabei nicht entrindet, er fällt direkt als entrindeter Stamm. Ein halb gefällter Baum bleibt so ein normaler Baum, und das Nachpflanzen funktioniert weiter.
- Kosten: 10 statt 2 Ticks je Stamm, und die Axt nutzt sich je Stamm einmal mehr ab (bei Vanilla doppelt so schnell). Das Tagespensum (64 Stämme) bleibt gleich.
- Der Sawyer nimmt entrindete Stämme wie normale an, die Tags `#<holz>_logs` enthalten sie.
- Später denkbar: Stämme aus Input-Kisten am Hackklotz entrinden. Schwieriger, weil die Regeln einen Block in der Welt prüfen und kein Item.

## Master-Fähigkeit

Ganzer Baum in einem Schlag (Treecapitator-Stil) oder doppelter Setzlingsertrag.

## Sonderfälle

- Riesenbäume, Pilze, Mangroven, Kirsche, Nether-Stämme: in V1 nur 1×1- und 2×2-Bäume, Rest später.
- Keine Bäume außerhalb des Bereichs fällen, auch nicht, wenn Blätter hineinragen. Spieler-Häuser bleiben tabu.
- Erkennung „Baum vs. Bauwerk“: nur Stämme, die er selbst gepflanzt hat oder die natürliche Blätter (`persistent=false`) tragen.
- Axt ist leer → Arbeit stoppt, Status „Braucht: Axt“ im [Dorfbuch](../dorf/dorfbuch.md).
- Setzlinge wachsen ohne Spieler in der Nähe nur durch eigene Wachstums-Ticks (siehe [Chunk-Loading](../dorf/chunk-loading.md#wichtiger-haken-random-ticks)).

## Definition of Done (M1)

Ein Holzfäller läuft drei Ingame-Tage ohne Eingriff, hängt nicht fest, beschädigt nichts außerhalb seines Bereichs und liefert messbar Holz.

## Siehe auch

[Logistik](../grundlagen/logistik.md) · [Steuerung](../grundlagen/steuerung.md) · [Roadmap](../planung/roadmap.md)
