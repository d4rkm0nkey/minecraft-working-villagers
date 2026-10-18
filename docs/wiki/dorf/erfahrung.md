# Jagd und Erfahrung (Konzept)

[← Wiki-Start](../README.md) · Phase: **Konzept** (Stand 18.10.2026) · baut auf [Wache](../berufe/wache.md), [Pfeilmacher](../berufe/pfeilmacher.md) und [Schmied](../berufe/schmied.md) auf

Wachen jagen nachts aktiv Monster. Die Erfahrung der erlegten Monster füllen sie in **Erfahrungsfläschchen**. Der Schmied braucht diese Fläschchen, um **verzauberte** Ausrüstung zu reparieren und Dinge, für die es kein Reparaturmaterial gibt (Bögen, Angeln, Scheren). So schließt sich ein zweiter Kreislauf:

```
Pfeilmacher ──Pfeile──► Wachen jagen ──Erfahrung──► Fläschchen ──► Schmied repariert ──► Bögen, verzauberte Werkzeuge
     ▲                                                                                        │
     └────────────────────────────────── zurück an die Arbeiter ◄─────────────────────────────┘
```

## Jagen statt Bewachen

An der Zielscheibe stellt man ein, was die Wache nachts tut:

| Modus | Was | Risiko | Ertrag |
| --- | --- | --- | --- |
| **Bewachen** (Standard) | Bleibt am Posten, schießt auf Monster im Wachradius | gering | wenig Erfahrung, das Dorf ist sicher |
| **Jagen** | Zieht im Jagdradius (Standard 32 Blöcke) umher, sucht Monster im Dunkeln, kehrt zum Morgen zurück | hoch: sie kann sterben, Rüstung zählt | viel Erfahrung, Beute, mehr Pfeilverbrauch |

Eine jagende Wache nimmt eine Fackel mit und meidet Wasser und Abgründe wie jeder Arbeiter. Hat sie nur noch 8 Pfeile oder weniger als die Hälfte ihrer Gesundheit, kehrt sie um.

## Erfahrung sammeln

- In Vanilla geben Monster nur Erfahrung, wenn ein **Spieler** sie tötet. Für Wachen weichen wir davon ab: Die Erfahrung, die das Monster einem Spieler gäbe (Zombie 5, Blaze 10, …), bekommt die Wache gutgeschrieben. Kugeln fallen keine.
- Die Wache sammelt die Erfahrung in sich (sichtbar im Menü: „Erfahrung: 23“). Morgens füllt sie sie an ihrem Posten in **leere Glasflaschen**: je 7 Punkte ein Erfahrungsfläschchen. Das entspricht dem Durchschnitt, den ein geworfenes Fläschchen in Vanilla gibt (3–11), es entsteht also keine Erfahrung aus dem Nichts.
- Glasflaschen holt sie aus ihren Eingangskisten oder einer Dorftruhe. Glas brennt der [Brenner](../berufe/brenner.md); die Flaschen fertigt vorerst der Spieler (Idee: der Brenner bekommt „Glasflaschen“ als Erzeugnis).
- Die Fläschchen bringt sie in ihre Ausgangskiste. Spieler können sie selbst nutzen (werfen gibt Erfahrung) oder sie dem Schmied lassen.

## Der Schmied repariert mit Erfahrung

Heute repariert der Schmied mit Material (Eisen, Diamant, Bretter …), auch Verzaubertes, ohne Erfahrungskosten. Mit den Fläschchen kommt dazu:

1. **Was kein Reparaturmaterial hat** (Bogen, Armbrust, Angel, Schere, Dreizack): 1 Erfahrungsfläschchen repariert 10 % der Haltbarkeit. So werden die Bögen der Wachen und die verzauberten Angeln des Fischers wieder brauchbar, die heute als „abgenutzt“ in der Dorftruhe liegen bleiben.
2. **Verzauberte Ausrüstung** kostet zusätzlich zum Material 1 Fläschchen je Reparaturschritt, ähnlich wie der Amboss Erfahrungsstufen kostet. Ohne Fläschchen repariert er Verzaubertes nicht (Status „Wartet auf Erfahrungsfläschchen“).
3. Anders als der Amboss kennt der Schmied kein „Zu teuer!“ und keine steigenden Kosten. Das ist der Lohn für den Aufwand der Kette.

Die Fläschchen nimmt er aus seinen Eingangskisten oder den Dorftruhen in Reichweite, wie heute das abgenutzte Werkzeug.

## Ergibt das Sinn?

**Ja**, aus drei Gründen:

- **Pfeile bekommen einen zweiten Zweck:** nicht nur Schutz, sondern Ertrag. Wer jagen lässt, verbraucht mehr Pfeile und braucht einen stärkeren Pfeilmacher und Metzger.
- **Verzauberte Ausrüstung für Arbeiter lohnt sich:** Heute ist eine verzauberte Axt beim Holzfäller nach dem Abnutzen ein Fall für den Spieler am Amboss. Mit Fläschchen erledigt das das Dorf selbst.
- **Erfahrung bleibt knapp:** Sie kommt nur aus echten Monstern, die Wachen erlegen, und kostet Pfeile, Rüstung und manchmal eine Wache.

**Risiken:**

- Ein Dorf mit vielen jagenden Wachen ist eine Erfahrungsfarm ohne Spieler. Gegenmittel: Monster spawnen nur im Dunkeln und nicht beliebig viele; der Pfeilverbrauch begrenzt; optional eine Obergrenze an Fläschchen pro Wache und Nacht (Config).
- Mehr Eingriff in Vanilla (Erfahrung ohne Spieler). Darum als eigener Schritt nach der Wache, abschaltbar per Config.

## Umsetzung in Schritten

1. Pfeilmacher und Wache (Bewachen), wie beschrieben.
2. Erfahrung der Wache gutschreiben und abfüllen.
3. Modus Jagen.
4. Schmied: Reparatur mit Fläschchen (zuerst Dinge ohne Material, dann Verzaubertes).

## Siehe auch

[Wache](../berufe/wache.md) · [Schmied](../berufe/schmied.md) · [Werkzeugrückgabe](werkzeugrueckgabe.md) · [Pfeilmacher](../berufe/pfeilmacher.md)
