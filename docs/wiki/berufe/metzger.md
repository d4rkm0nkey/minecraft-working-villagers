# Beruf: Metzger

[← Berufe](README.md) · Station: **Räucherofen** (Vanilla) · Phase: **umgesetzt** (Züchten, Schlachten, Räuchern; ohne Master-Fähigkeit)

Der Metzger hält die Tiere rund um seinen Räucherofen auf einer eingestellten Herdengröße: Darunter züchtet er, darüber schlachtet er. Das Fleisch räuchert er im Räucherofen und liefert es für die geplante [Vorratskammer](../wirtschaft/beduerfnisse.md#vorratskammer).

## Workstation: Räucherofen

- Der normale Vanilla-Räucherofen; über seinem Menü stehen die Reiter der Station (siehe [Steuerung](../grundlagen/steuerung.md#stationsübersicht)). Spieler können weiter selbst darin räuchern; was fertig ist, nimmt der Metzger mit heraus.
- Vanilla-Fleischer arbeiten weiter an Räucheröfen, außer an eingerichteten. Ein Fleischer, mit dem noch niemand gehandelt hat, lässt sich direkt anheuern und bleibt als Metzger an seinem Räucherofen (siehe [Anheuern & Lohn](../wirtschaft/anheuern-und-lohn.md#stand-der-umsetzung)).
- Kisten: mindestens eine **Ausgangskiste**; **Eingangskisten** sind freiwillig und halten Futter und Kohle für ihn.

## Stand der Umsetzung

- **Tiere:** Kühe, Pilzkühe, Schweine, Hühner und Kaninchen im Umkreis von 12 Blöcken (seitlich, Config `butcher.pastureRadius`, 4–24), bis 4 Blöcke höher oder tiefer. Schafe gehören dem [Hirten](hirte.md). Jede Art zählt für sich, Jungtiere zählen mit.
- **Herdengröße:** Reiter **Einstellungen**, − und +, 0 bis 32, Standard 8 **je Art** (dasselbe Feld `herd` der Station wie beim Webstuhl).
- **Schlachten:** Sind es mehr Tiere einer Art als eingestellt, schlachtet er ein ausgewachsenes, das nächste zuerst. Nie unter **zwei ausgewachsene** einer Art, nie **benannte**, **angeleinte** oder verliebte Tiere. Etwa 1,5 Sekunden pro Tier (Config `butcher.secondsPerAnimal`). Die Beute (Fleisch, Leder, Federn, …) sammelt er direkt ein. Die Waffe nutzt sich je Tier einmal ab.
- **Züchten:** Sind es weniger, füttert er zwei ausgewachsene Tiere mit ihrem Futter (Item-Tags `cow_food`, `pig_food`, `chicken_food`, `rabbit_food`), wie beim [Hirten](hirte.md): ein neues Paar nur, solange die kommenden Jungen noch passen.
- **Über den Zaun:** Reichweite 3,5 Blöcke wie beim Hirten; ein Gehege braucht kein Tor.
- **Räuchern:** Hat er Kohle oder Holzkohle (oder brennt der Ofen), legt er rohes Fleisch und Kohle in den Räucherofen und nimmt Fertiges heraus. Ohne Feuer, oder wenn der Ofen mit etwas anderem belegt ist, liefert er das rohe Fleisch ab.
- **Versorgung:** Lohn und eine **Waffe** (Schwert oder Axt, bis zu 2). Futter (bis 16 je Sorte, Config `butcher.keepFeed`) und Kohle (bis 16, Config `butcher.keepFuel`) gibt man ihm, er holt sie aus einer [Dorftruhe](../dorf/dorftruhen.md) oder aus seinen Eingangskisten. Findet er dort nichts, versucht er es erst eine Minute später wieder.
- **Abliefern:** alles außer Waffe, Futter, Kohle, Lohn und dem Fleisch, das er gleich räuchert, in eine Ausgangskiste mit Platz.
- **Status im Menü:** „Sieht nach den Tieren“, „Schlachtet ein Tier“, „Füttert ein Tier“, „Räuchert Fleisch“, „Holt Futter oder Kohle“, „Wartet, bis die Herde wächst“, „Wartet: keine Tiere am Räucherofen“, „Wartet: kein Futter zum Züchten“, „Wartet: braucht ein Schwert oder eine Axt“, dazu „Heute geschlachtet: x“.
- **Kleidung:** weiße Mütze, blau-weiß gestreifte Schürze über einem dunkelroten Hemd (`tools/generate_textures.py`).
- **Tests:** Vier Kühe bei Herdengröße 2: zwei werden über den Zaun geschlachtet, rohes Rindfleisch landet in der Kiste. Zwei Schweine bekommen nach dem Füttern mit Karotten ein Ferkel. Drei Hühner bei Herdengröße 2 mit Kohle: Gebratenes Hühnchen landet in der Kiste.
- **Nicht umgesetzt:** Eier und Federn vom Boden aufsammeln, Master-Fähigkeit.

## Hinweise

- Leder und Federn gehen an [Gerber](gerber.md) bzw. [Pfeilmacher](pfeilmacher.md).

## Master-Fähigkeit

Mehr Fleisch pro Tier (wie Looting I).

## Siehe auch

[Hirte](hirte.md) · [Farmer](farmer.md)
