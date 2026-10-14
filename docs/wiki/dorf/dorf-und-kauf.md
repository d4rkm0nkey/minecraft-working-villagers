# Dorf & Dorfkauf

[← Wiki-Start](../README.md)

Jeder Arbeiter gehört zu genau einem Dorf. Anheuern ist nur in eigenen Dörfern möglich. Wer ein Dorf kauft, erhält das [Dorfbuch](dorfbuch.md) und die Dorfkasse.

## Was ist ein Dorf?

- Vanilla kennt kein Dorf-Objekt, nur POIs (Betten, Glocke, Workstations). Der Mod legt beim Generieren einer Dorfstruktur einen Dorfeintrag an: ID, generierter Name, Biom, Glockenposition, Radius (z. B. 64 Blöcke).
- Dabei werden Betten, Workstations und Villager gezählt. Daraus ergibt sich der feste Kaufpreis.
- Eigene Dörfer: Spieler platziert eine Glocke mit mindestens 3 Betten im Umkreis und zahlt eine Gründungsgebühr (z. B. 50 Emeralds).

## Kaufpreis (gemessen bei Generierung)

Preis = 20 + 8 × Betten + 5 × Workstations + 3 × Villager (Emeralds). Startwerte, per Config änderbar.

| Dorfgröße | Betten | Workstations | Villager | Preis (Emeralds) |
| --- | --- | --- | --- | --- |
| Klein | 5 | 3 | 5 | 90 |
| Mittel | 10 | 7 | 10 | 165 |
| Groß | 20 | 14 | 20 | 310 |

Der Preis ist bewusst fest: Ein Dorf, das der Spieler später ausbaut, wird nicht teurer. Gekauft wird an der Glocke per Rechtsklick mit Emeralds; der Spieler erhält Kaufurkunde und Dorfbuch.

## Was der Kauf freischaltet

- [Anheuern](../wirtschaft/anheuern-und-lohn.md) von Dorfbewohnern dieses Dorfes.
- Das [Dorfbuch](dorfbuch.md), als Item oder auf einem Lectern neben der Glocke („Rathaus“, für Mitspieler lesbar).
- Die Dorfkasse (siehe unten).
- [Chunk-Loading](chunk-loading.md) für das Dorf und seine Arbeitsbereiche.
- Multiplayer: ein Besitzer, beliebig viele Verwalter mit Leserechten oder Vollzugriff (siehe [Multiplayer](../technik/multiplayer.md)).

## Dorfkasse

- Ein Barrel neben der Glocke, aus dem alle Löhne und der Chunk-Unterhalt bezahlt werden.
- [Einnahmen](../wirtschaft/einnahmen.md) (Handel, Steuern, Aufträge) fließen hier hinein.
- Der Kassenstand steht groß im Kopf des [Dorfbuchs](dorfbuch.md), zusammen mit den Lohnkosten pro Woche und wie lange die Kasse noch reicht.
- Ist die Kasse leer: Status „Unbezahlt“, Moral sinkt, nach einem Tag Kulanz wird das Chunk-Loading beendet.
