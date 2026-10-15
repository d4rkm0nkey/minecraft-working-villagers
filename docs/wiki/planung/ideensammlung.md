# Ideensammlung: Richtungen & Erweiterungen

[← Wiki-Start](../README.md)

Der Kern (Anheuern → Zuweisen → Kisten) trägt mehrere Richtungen. Sie schließen sich nicht aus, aber jede setzt einen anderen Schwerpunkt. Eine davon sollte nach V1 bewusst als Leitlinie gewählt werden ([Roadmap M6](roadmap.md)).

## Mögliche Richtungen

| Richtung | Kernidee | Spielgefühl | Nähe zu Vanilla |
| --- | --- | --- | --- |
| **Unternehmer (Schedule-1-Stil)** | Gewinn maximieren: Ware produzieren, verkaufen, Löhne zahlen | Wirtschaftssimulation, Zahlen im Blick | hoch |
| **Dorfverwalter** | Das eigene Dorf wächst: mehr Angestellte ziehen zu, wenn es Betten, Arbeit und Sicherheit gibt | Siedlungsaufbau light | mittel |
| **Logistik-Puzzle** | Fokus auf Träger, Lager, Filter, Routen; Produktionsketten optimieren | Factory-Spiel ohne Maschinen | mittel |
| **Abenteuer-Begleiter** | Angestellte begleiten den Spieler: Minenarbeiter gräbt mit, Wache kämpft, Kartograf erkundet | Party-System | mittel |
| **Multiplayer-Wirtschaft** | Spieler stellen Villager anderer Spieler ein, handeln Arbeitskraft, Markt mit Angebot/Nachfrage | Server-Wirtschaft | niedriger |
| **Rein Utility** | Nur Holz, Stein, Farm; minimales UI, maximal leise | Quality-of-Life | sehr hoch |

Empfehlung: Start als **Rein Utility** (V1), Wachstum in Richtung **Unternehmer** + **Logistik-Puzzle**. Das passt am besten zur Schedule-1-Inspiration und bleibt Vanilla-nah. Mit [Dorfkauf](../dorf/dorf-und-kauf.md), [Bedürfnissen](../wirtschaft/beduerfnisse.md) und [Steuern](../wirtschaft/einnahmen.md) gibt es inzwischen auch Anteile von **Dorfverwalter**.

## Feature-Ideen

### Personal & Mitarbeiter

- Eigenschaften (Traits) pro Villager: fleißig, faul, ungeschickt (mehr Werkzeugverschleiß), Nachteule (arbeitet nachts).
- Namen und Lebenslauf; Erfahrung bleibt beim Berufswechsel teilweise erhalten.
- Vorarbeiter (Master-Level): beschleunigt Arbeiter im Umkreis um 10 %.
- Ausbildung: Novice lernt schneller neben einem Master.
- Urlaub/Krankheit als leichter Zufallsfaktor (abschaltbar).
- Uniformen: Arbeitskleidung sichtbar am Villager, je Beruf und Level.

### Wirtschaft

- Verkaufsstand/Marktplatz, Preisschwankungen, Buchhaltungsbuch, leistungsbezogener Lohn, Kredit. Ausgearbeitet in [Einnahmen](../wirtschaft/einnahmen.md).

### Logistik

- [Träger](../berufe/traeger.md) mit Routen und Item-Frame-Filtern.
- Lagerhaus mit [Bibliothekar](../berufe/bibliothekar.md), Bestellsystem per Buch.
- Lasttiere (Esel/Lama) und Chest-Minecart-Strecken.

### Produktion & Crafting

- [Crafter-Bediener](../berufe/crafter-bediener.md) mit Rezeptbüchern und Auftragslisten.
- Qualitätsstufen nur kosmetisch (z. B. benannte Items vom Master), keine neuen Stats.

### Welt & Atmosphäre

- Glocke läutet Schichtbeginn und Feierabend.
- Mittagspause: Villager essen gemeinsam (siehe [Vorratskammer](../wirtschaft/beduerfnisse.md#vorratskammer)).
- Sprechblasen-Symbole (Werkzeug fehlt, Kiste voll, kein Lohn).
- Unfälle: Creeper im Steinbruch, Arbeiter ruft um Hilfe.
- Saisonale Effekte bei Mods wie Serene Seasons ([Farmer](../berufe/farmer.md)).

### Fortschritt

- [Berufsbücher](../grundlagen/berufsbuecher.md): ein Buch pro Beruf, manchmal bei Dorfbewohnern zu kaufen.
- Advancements: „Erster Angestellter“, „10 Angestellte“, „1000 Stämme gefällt“, „Pleite“.
- Lizenzen/Gewerbeschein: neue Berufe gegen einmalige Gebühr freischalten.
- Ruf im Dorf: gute Arbeitgeber bekommen bessere Bewerber.

### Integration & Technik

- Redstone: Workstation gibt Signal bei aktivem Arbeiter oder voller Zielkiste; Signal an der Station pausiert die Arbeit.
- Kompatibilität mit Lager-Mods (Sophisticated Storage, AE2) über Standard-Inventar-API.
- Datapacks: neue Berufe, Bereiche und Rezepte ohne Java-Code.
- Jade/WTHIT-Tooltip, JEI/EMI-Integration für Woodcutter-Rezepte.

### Exotischere Ideen (später)

- Pillager überfallen reiche Betriebe, [Wachen](../berufe/wache.md) werden wichtig.
- Streik bei schlechter Moral in mehreren Betrieben gleichzeitig.
- Expeditionen: Arbeiter für einige Tage wegschicken, kommt mit Loot zurück.
- Nether- und End-Betriebe.
