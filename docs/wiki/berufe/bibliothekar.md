# Beruf: Bibliothekar

[← Berufe](README.md) · Station: **Lesepult** (Vanilla), verzaubert an einem **Zaubertisch** daneben · Phase: **umgesetzt** (Verzaubern; ohne Lagerverwaltung)

Der Bibliothekar verzaubert. Er verbraucht die Erfahrung, die das Dorf sammelt: Die Erfahrungsfläschchen der [Wachen](wache.md) und des [Waffenschmieds](waffenschmied.md) werden verzauberte Werkzeuge, Waffen, Rüstung und Bücher. Damit schließt sich der Erfahrungskreislauf: Bisher ging die Erfahrung nur in Reparaturen beim [Schmied](schmied.md).

```
Wachen (Jagd) ──────────┐
Waffenschmied (Entzaubern) ┼──► Erfahrungsfläschchen ──► Schmied (Reparatur)
                        │                        └──► Bibliothekar (Verzaubern) ──► Werkzeug der Arbeiter
Minenarbeiter ── Lapislazuli ───────────────────────────┘
Gerber ── Bücher ───────────────────────────────────────┘
```

## Stand der Umsetzung

- **Station:** Vanilla-Lesepult (POI `librarian`), dazu ein **Zaubertisch** bis 6 Blöcke entfernt (Config `librarian.tableRadius`); er nimmt den nächsten. Rechtsklick mit leerer Hand auf ein Lesepult **ohne Buch** öffnet die Übersicht; mit Buch bleibt es Vanilla. Vanilla-Bibliothekare lassen sich jetzt anheuern, behalten ihre Angebote (verzauberte Bücher) und handeln wie Bibliothekare (Handelsfamilie `librarian`). Der Test „wird nicht angeheuert“ nimmt dafür jetzt einen Kartografen.
- **Was:** alles Verzauberbare aus den **Eingangskisten** (`ItemStack.isEnchantable`: unverzauberte Werkzeuge, Waffen, Rüstung, Bücher), bis zu 3 auf einmal, eins alle 3 Sekunden am Zaubertisch, Ergebnis in die Ausgangskiste.
- **Wie der Spieler beim besten Angebot:** `EnchantmentHelper.enchantItem` mit den Verzauberungen des Tags `in_enchanting_table`, also auch mehrere auf einmal. Die **Stufe** ist zwei je Bücherregal (gezählt wie beim Spieler, `EnchantingTableBlock.BOOKSHELF_OFFSETS`, höchstens 15), mindestens 8 wie das dritte Angebot eines nackten Tischs, höchstens 30. Am Lesepult lässt sie sich niedriger einstellen (Reiter **Einstellungen**, `-` `+`, „höchste“ oder 1 bis 30; Feld `enchant_level` der Station).
- **Preis** statt der Level des Spielers: ein Erfahrungsfläschchen je 3 Stufen (Config `librarian.levelsPerBottle`; Stufe 30: 10 Fläschchen, 70 Erfahrung) und 1 bis 3 Lapislazuli wie die drei Angebote (ab Stufe 10: 2, ab 20: 3). Beides aus den Eingangskisten oder einer Dorftruhe in Reichweite; die Dinge zum Verzaubern nur aus den Eingangskisten.
- **Anzeige:** Der Reiter Einstellungen zeigt die Bücherregale, die Stufe und den Preis pro Stück; roter Punkt, wenn kein Zaubertisch da ist. Im Menü „Heute verzaubert: x“.
- **Status:** „Sucht etwas zum Verzaubern“, „Holt etwas zum Verzaubern“, „Verzaubert“, „Wartet auf etwas zum Verzaubern“, „Wartet auf Erfahrungsfläschchen (x pro Stück)“, „Wartet auf Lapislazuli (x pro Stück)“, „Kein Zaubertisch beim Lesepult“, „Kommt nicht an den Zaubertisch“.
- **Kleidung:** dunkelvioletter Gelehrtenmantel mit Goldsaum und kleinen goldenen Sternen, lapisblaue Schärpe, runde Brille, violette Kappe mit Goldband.
- **Tests:** an einem nackten Tisch wird ein Buch mit 3 Fläschchen und einem Lapis ein verzaubertes Buch; mit 15 Bücherregalen wird eine Eisenspitzhacke auf Stufe 30 verzaubert und kostet 10 Fläschchen.
- **Nicht umgesetzt:** Lagerverwaltung (Bestandsbuch, Mangelliste, Bestellungen; siehe unten), Werkzeug der Arbeiter aus den Dorftruhen gezielt verzaubern, Meisterfähigkeit.

## Später: Lagerverwalter

Die frühere Idee bleibt als zweite Arbeit am selben Beruf, mit einer Einstellung wie beim Waffenschmied:

- **Bestandsbuch:** jeden Morgen ein Buch mit dem Inhalt aller Dorftruhen im Lesepult.
- **Mangelliste:** worauf die Arbeiter des Dorfes warten, an einer Stelle.
- **Bestellungen:** Wunschliste als Buch ins Lesepult, er sammelt die Sachen aus den Dorftruhen in eine Abholkiste.
- Führt das Rathaus-Exemplar des [Dorfbuchs](../dorf/dorfbuch.md).

## Master-Fähigkeit

Ein Fläschchen weniger pro Stück.

## Siehe auch

[Jagd und Erfahrung](../dorf/erfahrung.md) · [Waffenschmied](waffenschmied.md) · [Schmied](schmied.md) · [Gerber](gerber.md) · [Berufe](README.md)
