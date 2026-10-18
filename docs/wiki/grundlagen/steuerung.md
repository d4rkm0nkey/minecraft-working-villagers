# Steuerung

[← Wiki-Start](../README.md)

Alles wird über die Stationsübersicht, das Träger-Menü und später das [Dorfbuch](../dorf/dorfbuch.md) gesteuert. Keine Befehle. Die Config-Datei (`config/workingvillagers.json`) ist für Server-Betreiber, nicht für das tägliche Spiel.

## Auswahl in der Welt

Ein eigenes Werkzeug gibt es nicht. Wo etwas in der Welt ausgewählt werden muss, startet ein Button im Menü die Auswahl: Das Menü schließt sich, dann wählt man per Rechtsklick aus.

- Arbeiter zuweisen: **„Zuweisen“** in der Stationsübersicht, dann Rechtsklick auf einen Dorfbewohner mit Arbeitsvertrag.
- Kisten verknüpfen: **„+ Eingang“** bzw. **„+ Ausgang“** (am Hackklotz und am Komposter nur **„+ Kiste“**), dann Rechtsklick auf Kisten; nochmal klicken entfernt sie. Rechtsklick auf die Workstation beendet die Auswahl und öffnet das Menü wieder.
- Transportwege des [Trägers](../berufe/traeger.md): „+ Neuer Weg“, dann Quelle und Ziel anklicken.

## Kisten benennen

Im Menü einer Kiste oder eines Fasses steht oben rechts **✎**: Ein Klick macht den Titel zum Textfeld, Enter speichert (bis 24 Zeichen), Esc bricht ab. Gespeichert wird der Vanilla-Name (wie vom Amboss), bei Doppelkisten in beiden Hälften. Stationsübersicht und Träger-Menü zeigen diesen Namen; ohne Namen steht dort grau die Blockart.

## Stationsübersicht

### Stand der Umsetzung

Jede Station hat eine einzige Übersicht ohne Reiter. Man öffnet sie so:

| Station | Öffnen |
| --- | --- |
| Hackklotz | Rechtsklick |
| Holzsäge, Steinsäge, Hobelbank | Rechtsklick öffnet das Arbeitsmenü, der Knopf mit dem Gesicht des Arbeiters die Übersicht |
| Komposter | Rechtsklick mit leerer Hand |
| Schmelzofen | Knopf mit dem Gesicht des Arbeiters im Ofenmenü |

Der Knopf sitzt oben rechts im Menü des Blocks (Holzsäge, Steinsäge, Mühle, Hobelbank, Schmelzofen, Brennofen, Schmiedetisch), so groß wie der Rezeptbuch-Knopf. Er zeigt das Gesicht des Berufs, der dort arbeitet, gezeichnet aus der Villager-Textur und der Kleidung des Berufs; ohne angeheuerten Arbeiter ist es abgedunkelt. Der Tooltip nennt den Arbeiter. Die Titelzeile der Block-Menüs zeigt den Arbeiter nicht mehr.

Inhalt der Übersicht:

- **Arbeiter** mit Name und Status („Macht gerade: …“). Ist die Station frei, holt **„Zuweisen“** einen angeheuerten Dorfbewohner; **„Abziehen“** nimmt ihn wieder weg, er bleibt eingestellt.
- **Kisten** (höchstens 8) mit ihrem Namen; Art, Füllstand und Entfernung im Tooltip. Koordinaten zeigt kein Menü. An Stationen mit Eingang schaltet ein Klick die Rolle zwischen **Eingang** und **Ausgang** um; „Kiste entfernen“ löst die Verknüpfung.
- **Auftrag** (nur Holzsäge, Steinsäge, Hobelbank): Erzeugnis und Holzsorten, bei der Steinsäge Material und Erzeugnis, an der Hobelbank zusätzlich der Vorrat. Am Hackklotz steht hier der Schalter „Stämme entrinden“.
- Zeile **„Lohn und Werkzeug“**: was der Arbeiter bei sich trägt (Emeralds, Axt oder Hacke, Setzlinge oder Saatgut) und wie lange der Lohn reicht. Geben geht nur direkt am Arbeiter (Rechtsklick oder zuwerfen).

Zuweisen ist meist gar nicht nötig: Ein angeheuerter Dorfbewohner ohne Arbeit sucht sich selbst die nächste freie Station dieses Mods, die er im Umkreis von 48 Blöcken erreichen kann, wie Vanilla-Dorfbewohner ihren Arbeitsplatz.

### Konzept (noch nicht umgesetzt)

- **[Menüs überarbeiten](../planung/menues.md):** Reiter Block · Arbeit · Auftrag statt des Knopfs mit dem Gesicht und „Zurück“, jedes Menü in Vanilla-Breite und passend für 854×480.

- Reiter „Auftrag“ für den Modus, z. B. Abbaumodus der Mine.
- Reiter „Bereich“: Größe, Position und Visualisierung an/aus. Arbeitsbereiche gibt es noch nicht; Holzfäller und Bauer arbeiten im festen Umkreis ihrer Station.
- Reiter „Personal“: Lohn ändern, Entlassen.

## Grenzen

- Kisten müssen innerhalb von **16 Blöcken** um die Workstation liegen (Config `workers.chestDistance`, 4 bis höchstens 32). Höchstens **8 Kisten** pro Station (Config `workers.maxChests`). Für spätere Arbeitsbereiche gilt dieselbe Reichweite. Ausnahme Mine (Konzept): Die Reichweite gilt ab dem letzten Depot des [Grubenlogistikers](../berufe/grubenlogistiker.md).
- Eine Workstation = ein Arbeiter (wie Vanilla-POI). Mehr Arbeiter brauchen mehr Stationen.
- Idee: Schilder als Wegmarken/Filter, z. B. ein Schild „Nur Eiche“ an der Zielkiste.

Siehe auch: [Kernloop](kernloop.md) · [Multiplayer-Rechte](../technik/multiplayer.md)
