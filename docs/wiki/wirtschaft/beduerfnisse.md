# Bedürfnisse: Kost & Logis

[← Wiki-Start](../README.md)

Angestellte haben Bedürfnisse. Jedes erfüllte Bedürfnis senkt ihren Lohn, weil ein Teil als Sachleistung bezahlt wird. Das schließt den Kreislauf: Was das Dorf produziert, wird teilweise im Dorf verbraucht, und Emeralds werden weniger knapp.

## Warum das gut passt

- [Farmer](../berufe/farmer.md), [Fischer](../berufe/fischer.md) und [Metzger](../berufe/metzger.md) bekommen einen direkten Zweck, nicht nur Verkauf.
- Vanilla-nah: Villager sammeln schon heute Brot, Karotten, Kartoffeln und Rote Bete und brauchen ein Bett.
- Ein neues Dorf ohne Einnahmen kann trotzdem starten, wenn es Nahrung hat.
- Es entsteht ein Anno-artiger Fortschritt: Höhere Level wollen mehr und Besseres, zahlen sich aber auch mehr aus.

## Bedürfnisse und Rabatt (Startwerte)

| Bedürfnis | Erfüllt durch | Verbrauch | Lohnrabatt | Nicht erfüllt |
| --- | --- | --- | --- | --- |
| Nahrung | Essen aus der Vorratskammer, z. B. Brot, Kartoffeln, Fisch | 1 Brot oder gleichwertig pro Arbeitstag | −25 % | Moral sinkt, ab 2 Tagen langsamer |
| Unterkunft | Eigenes Bett im Dorf, nicht geteilt | – | −15 % | Moral sinkt, schläft schlecht |
| Sicherheit | Licht im Dorf, Iron Golem, Mauer | – | −5 % | Moral sinkt nachts |
| Abwechslung (ab Journeyman) | 3 verschiedene Nahrungsmittel pro Woche | wie Nahrung | −10 % | nur kein Rabatt |
| Komfort (ab Expert) | Luxusgut pro Woche: Kuchen, Honig, Kürbiskuchen | 1 pro Woche | −10 % | nur kein Rabatt |

Der Rabatt ist auf **−50 %** gedeckelt, Emeralds bleiben also immer nötig. Beispiel: Ein Novice kostet 16 Emeralds; mit Nahrung und Bett nur noch 10. Für 7 Brote pro Woche braucht man 21 Weizen, das schafft ein [Farmer](../berufe/farmer.md) nebenbei.

## Vorratskammer

- Ein Barrel oder eine Kiste im Dorf, im [Dorfbuch](../dorf/dorfbuch.md) als Vorratskammer markiert. Mehrere sind möglich.
- Mittagspause: Angestellte gehen zur Kammer und essen. Ein [Träger](../berufe/traeger.md) kann sie aus der Farm-Zielkiste nachfüllen, das Ganze ist also automatisierbar.
- Der Nährwert kommt aus den Vanilla-Werten der Nahrung (Brot 5, Kartoffel gebacken 5, Karotte 3). So funktionieren auch Lebensmittel aus anderen Mods.

## Im Dorfbuch

- Reiter **Versorgung**: Bedarf pro Woche, Bestand in den Vorratskammern, Reichweite in Tagen und Abdeckung pro Bedürfnis in Prozent.
- Der Kasten der Dorfkasse zeigt die Lohnkosten nach Rabatt und daneben die Ersparnis.
- Status in der Arbeiterübersicht: „Hungrig“, „Kein Bett“.

## Zusammenspiel

- Gut versorgte Angestellte haben höhere [Moral](anheuern-und-lohn.md#moral-statt-strafe); das senkt Kündigungen und erhöht das Arbeitstempo.
- Die Versorgung fließt in den [Wohlstand](einnahmen.md#steuern-miete--wohlstand) des Dorfes ein, also auch in die Steuern.
- Auch nicht angestellte Einwohner können Bedürfnisse haben; erfüllte Bedürfnisse erhöhen dann die Steuern statt Lohn zu sparen.

## Risiko: Mikromanagement

Bedürfnisse dürfen nie Pflicht sein, nur Rabatt. Wer nur Emeralds zahlt, kann das tun. In M2 starten nur Nahrung und Unterkunft; Abwechslung und Komfort kommen später. Alles per Config abschaltbar.
