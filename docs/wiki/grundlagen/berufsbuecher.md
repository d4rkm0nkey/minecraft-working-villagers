# Berufsbücher

[← Wiki-Start](../README.md) · Phase: **Idee**

Jeder Beruf hat ein eigenes Buch, das ihn dem Spieler im Spiel erklärt: Station, Einrichten, Kisten, Ablauf und was die Statusmeldungen bedeuten. Die Bücher kauft man manchmal bei Dorfbewohnern. So lernt man den Mod im Spiel kennen, ohne die Webseite zu öffnen, und Handeln mit Dorfbewohnern lohnt sich auch für den Mod.

## Grundsätze

- **Hilfe, keine Sperre.** Jeder Beruf funktioniert auch ohne sein Buch. Das Buch erklärt nur.
- **Ein Buch pro Beruf**, dazu ein Grundlagenbuch für das, was alle Berufe gemeinsam haben.
- **Seltener Fund:** Nicht jeder Dorfbewohner bietet ein Buch an. Es ist eine kleine Belohnung fürs Handeln und Erkunden.
- **Immer aktuell:** Der Text steht nicht im Item, sondern in den Sprachdateien. Ändert sich ein Beruf mit einem Update, ändern sich auch schon gekaufte Bücher, und sie erscheinen in der Sprache des Spielers.

## Die Bücher

| Buch | Inhalt | Verkauft von |
| --- | --- | --- |
| **Der Arbeitgeber** (Grundlagen) | Arbeitsvertrag, Anheuern, Station finden und zuweisen, Lohn und Werkzeug geben, Arbeitszeiten, Kisten verknüpfen | Bibliothekar |
| [Holzfäller](../berufe/holzfaeller.md) | Hackklotz, Baumfarm, Axt und Setzlinge, Entrinden | Fletcher, Bibliothekar |
| [Holzsäger](../berufe/holzsaeger.md) | Holzsäge (Woodcutter), Auftrag und Holzarten | Fletcher, Bibliothekar |
| [Tischler](../berufe/tischler.md) | Hobelbank, Produkte, Vorrat | Fletcher, Bibliothekar |
| [Steinsäger](../berufe/steinsaeger.md) | Steinsäge (Stonecutter), Steinfamilien | Mason, Bibliothekar |
| [Bauer](../berufe/farmer.md) | Komposter, Felder, Saatgut | Farmer, Bibliothekar |
| [Schmied](../berufe/schmied.md) | Schmelzofen, Erz und Brennstoff | Armorer, Toolsmith, Bibliothekar |
| [Träger](../berufe/traeger.md) | Weidenkorb, Transportwege | Cartographer, Bibliothekar |
| [Grubenarbeiter](../berufe/grubenarbeiter.md) | Grubenpflock, Ring aus Schnur, Tiefe, Leiter | Mason, Bibliothekar |
| [Brenner](../berufe/brenner.md) | Brennofen, Auftrag, was er brennt und was nicht | Mason, Bibliothekar |

Jeder neue Beruf bekommt sein Buch mit der Umsetzung. Er wird dem Vanilla-Beruf zugeordnet, der thematisch am nächsten liegt (Metzger → Butcher, Fischer → Fisherman, Hirte → Shepherd, Gerber → Leatherworker usw.). Der Bibliothekar kann jedes Buch anbieten.

## Kaufen

- **Wann:** Erreicht ein Dorfbewohner ein neues Level (ab Apprentice), bekommt er mit **25 %** Chance zusätzlich ein Angebot für ein Berufsbuch aus seiner Liste. Höchstens ein Buch-Angebot pro Dorfbewohner. Der Bibliothekar bietet das Grundlagenbuch schon als Novice mit 50 % Chance an.
- **Zusätzlich, nicht statt:** Das Buch kommt als weiteres Angebot dazu. Die Vanilla-Angebote bleiben genau so, wie sie wären, damit Handel nicht kaputtgeht.
- **Fahrender Händler:** Mit 30 % Chance hat er ein zufälliges Berufsbuch dabei.
- **Preis:** 4 Emeralds + 1 Buch (Startwert, Config), das Grundlagenbuch 2 Emeralds + 1 Buch. Das Angebot erneuert sich wie andere Angebote.
- Angeheuerte Dorfbewohner handeln nicht, sie verkaufen also auch keine Bücher.
- Alle Chancen und Preise stehen in der Config. Wer die Bücher nicht will, stellt die Chance auf 0.

## Aufbau eines Buchs

Jedes Berufsbuch hat dieselbe Gliederung, damit man sich schnell zurechtfindet. Etwa 6–8 Seiten:

1. **Titelseite:** Name des Berufs, ein Satz, was er tut.
2. **Station:** welcher Block, wie man ihn bekommt (Rezept in Worten) und wie man die Übersicht öffnet; bei Vanilla-Stationen anders als gewohnt: Komposter mit leerer Hand rechtsklicken, beim Schmelzofen und bei der Steinsäge der Knopf **i** im Menü.
3. **Einrichten:** die Schritte von der Station bis zum ersten Arbeitstag.
4. **Lohn, Werkzeug und Kisten:** was man dem Arbeiter gibt und was in die Eingangs- und die Ausgangskiste gehört.
5. **Ablauf:** was der Arbeiter über den Tag tut, Tagespensum.
6. **Statusmeldungen:** die häufigsten „Wartet: …“ und was zu tun ist.
7. **Tipps:** woher die Rohstoffe kommen, wohin die Produkte gehen (Kette zu anderen Berufen).

Beispiel, Seite 3 im Buch „Holzfäller“:

> **Einrichten**
>
> 1. Hackklotz hinstellen.
> 2. Ein angeheuerter Dorfbewohner ohne Arbeit findet ihn selbst. Oder: Rechtsklick, „Zuweisen“, dann den Dorfbewohner anklicken.
> 3. „+ Kiste“ und eine Kiste anklicken.
> 4. Den Holzfäller mit Emeralds, einer Axt und Setzlingen rechtsklicken.

## Umsetzung

- **Item:** ein Vanilla-Buch mit Text (`minecraft:written_book`). Es braucht keinen neuen Item-Typ und funktioniert auf dem Pult und im Bücherregal mit Fächern.
- **Text:** Die Seiten bestehen aus übersetzbaren Texten (`book.workingvillagers.<beruf>.page.<n>` in `en_us.json` und `de_de.json`). Das Item speichert nur die Schlüssel, deshalb bleiben gekaufte Bücher aktuell.
- **Titel und Autor:** z. B. „Der Holzfäller“, Autor „Dorfgilde“.
- **Angebote:** werden nach den Vanilla-Angeboten angehängt (Plattform-API für Handel bzw. Mixin), nicht in die Vanilla-Auswahl gemischt. Sonst würde ein Buch-Angebot ein Vanilla-Angebot verdrängen.
- **Webseite:** Die Bücher sind eine Kurzfassung der Berufsseiten unter `pages/docs/workers`. Beide sollten zusammen gepflegt werden.

## Erweiterungen (später)

- Advancement „Bücherwurm“: alle Berufsbücher gesammelt.
- Ein Buch auf einem Pult neben der Station zeigt dort zusätzlich die aktuelle Statusmeldung des Arbeiters.
- Mit [Gewerbeschein](../planung/ideensammlung.md#fortschritt) als Config-Option: Ein Beruf lässt sich erst zuweisen, wenn man sein Buch besitzt. Standard aus, siehe Grundsatz „Hilfe, keine Sperre“.

## Offene Fragen

- Reichen 25 % Chance, oder findet man so manche Bücher zu selten? Alternativ steigt die Chance mit jedem Level.
- Sollen die Bücher auch in Truhen von Dörfern und Außenposten liegen können?
- Bilder (Rezepte, Aufbau) gehen in Vanilla-Büchern nicht. Reicht Text, oder braucht es später ein eigenes Buch-Menü mit Bildern?

## Siehe auch

[Steuerung](steuerung.md) · [Berufe](../berufe/README.md) · [Anheuern & Lohn](../wirtschaft/anheuern-und-lohn.md) · [Bibliothekar](../berufe/bibliothekar.md)
