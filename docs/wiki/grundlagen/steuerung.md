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

Jede Station hat eine Übersicht in Vanilla-Größe (176×166, wie eine Kiste) mit Reitern darüber, wie im Kreativinventar. Man öffnet sie so:

| Station | Öffnen |
| --- | --- |
| Hackklotz, Grubenpflock, Handelstresen | Rechtsklick |
| Bognertisch, Zielscheibe, Kessel | Rechtsklick mit leerer Hand (sie haben in Vanilla kein Menü) |
| Komposter | Rechtsklick mit leerer Hand |
| Holzsäge, Steinsäge, Mühle, Hobelbank, Brennofen, Schmelzofen, Schmiedetisch, Webstuhl, Räucherofen, Fass, Braustand, Schleifstein | Rechtsklick öffnet das Menü des Blocks; über ihm stehen dieselben Reiter wie über der Übersicht |

**Reiter über den Block-Menüs:** Der Reiter des Blocks ist dort der offene; ein Klick auf einen anderen öffnet die Übersicht gleich auf diesem Reiter, der Reiter des Blocks führt zurück. Beide Fenster stehen an derselben Stelle (normal zentriert, die Reiter ragen in den Rand darüber; bei 854×480 bleiben 37 px, die Hobelbank mit 184 px Höhe passt genau). Die roten Punkte stehen schon am Block-Menü. An einem **Vanilla-Block, den niemand eingerichtet hat** (Steinsäge, Schmelzofen, Schmiedetisch, Webstuhl, Räucherofen, Fass, Braustand ohne Arbeiter und Kisten), stehen nur die Reiter Block und Arbeiter, ohne rote Punkte: Spieler, die die Mod dort nicht nutzen, sehen wenig davon. Der Knopf mit dem Gesicht oben rechts entfällt; das Gesicht steht im Reiter Arbeiter. Wechselt man den Reiter, öffnet der Server das andere Menü; ein Item an der Maus wandert dabei ins Inventar.

Was die Reiter zeigen (Arbeiter da, eingerichtet, rote Punkte), berechnet der Server an einer Stelle (`StationTabFlags`) und schickt es der Übersicht mit ihren Infos, den Block-Menüs in einem Datenslot, einmal pro Sekunde neu.

Die Reiter (ein roter Punkt am Reiter zeigt, dass dort etwas fehlt):

- **Block** (nur Stationen mit eigenem Menü): das Menü des Blocks; ersetzt den früheren Knopf „Zurück“.
- **Arbeiter** (Symbol: Gesicht des Berufs, abgedunkelt ohne Arbeiter): Name, **„Zuweisen“** bzw. **„Abziehen“** als großer Knopf, der Status auf bis zu drei Zeilen umgebrochen, darunter das Tagespensum oder der Auftrag, die Versorgung als Slots („Lohn und Werkzeug“, Details im Tooltip) und unten, wie man dem Arbeiter etwas gibt. Roter Punkt: kein Arbeiter, oder ihm fehlt Lohn oder Werkzeug.
- **Lohn** (Smaragd): die [Lohnkasse](../wirtschaft/anheuern-und-lohn.md#stand-der-umsetzung) der Station mit einem Slot und darunter das Spielerinventar, wie ein Container; Shift-Klick schiebt Emeralds hin und her. Roter Punkt: der Arbeiter hat heute keinen Lohn.
- **Kisten**: höchstens 8, sechs sichtbar, dann scrollen (Mausrad). Pro Zeile Name, die Rolle als Knopf (**Eingang**/**Ausgang**, Klick schaltet um), Füllstand und ×; Art, Füllstand und Entfernung im Tooltip. Unten **„+ Eingang“** und **„+ Ausgang“** (an Stationen nur mit Ausgang **„+ Kiste“**). Roter Punkt: keine Kiste.
- **Auftrag** (Holzsäge, Steinsäge, Hobelbank, Brennofen, Bognertisch, Braustand, Kessel, Schleifstein; am Braustand mit allen Tränken im Raster, am Kessel mit der Farbe für Lederrüstung, am Schleifstein mit der Wahl, ob er auch zusammenschleift und entzaubert): Erzeugnis, Holzsorten, Vorrat. **Einstellungen** an Hackklotz („Stämme entrinden“), Webstuhl und Räucherofen (Herdengröße), Fass (Bootsfahrt an/aus), Grubenpflock (Grube, Tiefe, Leiter) und Grubenwerkbank (Stollen, Länge, Art, Abzweige), jeweils mit Erklärung darunter. Roter Punkt: kein Auftrag, keine Grube, kein Stollen.

Komposter, Schmelzofen, Handelstresen und Schmiedetisch haben keinen dritten Reiter. Alle Knöpfe sind 16 bis 20 px hoch, wie Vanilla-Knöpfe.

Zuweisen ist meist gar nicht nötig: Ein angeheuerter Dorfbewohner ohne Arbeit sucht sich selbst die nächste freie Station dieses Mods, die er im Umkreis von 48 Blöcken erreichen kann, wie Vanilla-Dorfbewohner ihren Arbeitsplatz.

### Konzept (noch nicht umgesetzt)

- **[Menüs überarbeiten](../planung/menues.md):** Stationsübersicht, [Träger](../berufe/traeger.md#träger-menü) und [Dorftruhe](../dorf/dorftruhen.md) haben ihre Reiter; offen sind Reiter auch über den Block-Menüs und die Figur des Arbeiters.

- Reiter „Auftrag“ für den Modus, z. B. Abbaumodus der Mine.
- Reiter „Bereich“: Größe, Position und Visualisierung an/aus. Arbeitsbereiche gibt es noch nicht; Holzfäller und Bauer arbeiten im festen Umkreis ihrer Station.
- Reiter „Personal“: Lohn ändern, Entlassen.

## Grenzen

- Kisten müssen innerhalb von **16 Blöcken** um die Workstation liegen (Config `workers.chestDistance`, 4 bis höchstens 32). Höchstens **8 Kisten** pro Station (Config `workers.maxChests`). Für spätere Arbeitsbereiche gilt dieselbe Reichweite. Ausnahme Mine (Konzept): Die Reichweite gilt ab dem letzten Depot des [Grubenlogistikers](../berufe/grubenlogistiker.md).
- Eine Workstation = ein Arbeiter (wie Vanilla-POI). Mehr Arbeiter brauchen mehr Stationen.
- Idee: Schilder als Wegmarken/Filter, z. B. ein Schild „Nur Eiche“ an der Zielkiste.

Siehe auch: [Kernloop](kernloop.md) · [Multiplayer-Rechte](../technik/multiplayer.md)
