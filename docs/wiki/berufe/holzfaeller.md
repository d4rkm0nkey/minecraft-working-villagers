# Beruf: Holzfäller

[← Berufe](README.md) · Station: **Hackklotz** (neu) · Phase: **umgesetzt** (M1, mit Abweichungen)

Der Holzfäller fällt Bäume rund um seinen Hackklotz, pflanzt Setzlinge nach und bringt die Stämme in die Zielkiste.

Die Holzsäge (Woodcutter) gehört nicht zu ihm, sondern zum [Holzsäger](holzsaeger.md).

## Workstation: Hackklotz

- Eigener Block: ein Holzklotz mit einer Axt darin. Rezept: eine beliebige Axt über einem beliebigen Stamm.
- Rechtsklick öffnet direkt die Stationsübersicht: Arbeiter, Zielkisten, Schalter „Stämme entrinden“, die Zeile „Lohn und Werkzeug“ und „Heute gefällt: x / 64 Stämme“.
- Kisten verknüpft man mit **„+ Kiste“**. Der Holzfäller holt nichts, er liefert nur ab, deshalb gibt es hier keinen Eingang.
- Nur angeheuerte Dorfbewohner arbeiten am Hackklotz.

## Stand der Umsetzung

- **Keine Quellkiste:** Axt und Setzlinge gibt der Spieler ihm direkt, per Rechtsklick auf den Holzfäller oder indem er sie vor ihm fallen lässt (wie Lohn, siehe [Anheuern & Lohn](../wirtschaft/anheuern-und-lohn.md#stand-der-umsetzung)). Er trägt bis zu 2 Äxte und 16 Setzlinge je Sorte.
- **Bäume:** Er fällt natürliche Bäume im Umkreis von 16 Blöcken um den Hackklotz (Config `lumberjack.searchRadius`, 4–32). Natürlich heißt: Stämme auf Erde, Gras, Podsol, Schlamm o. Ä., verbunden mit Blättern, die nicht vom Spieler gesetzt sind. Häuser aus Stämmen bleiben stehen. Auch 2×2-Bäume fällt er; mehrere eng stehende Bäume trennt er sauber.
- **Fällen:** Er hackt erst eine Weile am Stamm (etwa 8-mal so lange wie ein Spieler mit derselben Axt), dann fällt der ganze Baum Stamm für Stamm. Die Stämme landen direkt in seinem Inventar, die Blätter verfallen von selbst. Die Axt nutzt sich je Stamm einmal ab; ohne Axt wartet er.
- **Nachpflanzen:** Hat er passende Setzlinge dabei, pflanzt er sie dort, wo der Stamm stand (4 für einen 2×2-Baum). Herabfallende Setzlinge sammelt er auf, bis er 16 je Sorte hat.
- **Abliefern:** Alles außer Axt, Essen, Emeralds und den behaltenen Setzlingen bringt er in die nächste Zielkiste mit Platz. Danach macht er 30 Sekunden Pause (Config `lumberjack.restSeconds`).
- **Tagespensum:** Nach 64 Stämmen am Tag hat er Feierabend (Config `lumberjack.dailyLogs`).
- **Ohne Zielkiste** fängt er gar nicht erst an.
- **Spielregel:** Er arbeitet nur, wenn die Spielregel `mob_griefing` an ist (abschaltbar mit Config `lumberjack.requireMobGriefing`).
- **Status im Menü:** „Sucht einen Baum“, „Geht zum Baum“, „Hackt einen Baum (x %)“, „Fällt den Baum“ bzw. „Fällt und entrindet den Baum“, „Bringt alles zur Kiste“, „Macht Pause“, „Wartet: findet keinen Baum“, „Hat sein Tagespensum geschafft“, „Wartet: braucht eine Axt (gib ihm eine)“, dazu die allgemeinen Meldungen (Lohn, volle Kisten, steckt fest).

## Entrinden (umgesetzt)

- Schalter „Stämme entrinden“ im Menü des Hackklotzes, Standard aus. Ist er an, kommen entrindete Stämme in die Zielkiste.
- Der Holzfäller entrindet beim Fällen: Für jeden Stamm wertet er die Entrinden-Regeln seiner Axt aus (seit 26.3 datengetrieben über die Komponente `minecraft:block_transformer`, Datei `block_transformer/axe.json`). Holz aus Datapacks und Mods funktioniert damit ohne eigene Liste; auf NeoForge zählen auch die Regeln, die Mods per Data Map anhängen.
- Der Block in der Welt wird dabei nicht entrindet, er fällt direkt als entrindeter Stamm. Ein halb gefällter Baum bleibt so ein normaler Baum, und das Nachpflanzen funktioniert weiter.
- Kosten: 10 statt 2 Ticks je Stamm, und die Axt nutzt sich je Stamm einmal mehr ab (bei Vanilla doppelt so schnell). Das Tagespensum (64 Stämme) bleibt gleich.
- Der [Holzsäger](holzsaeger.md) nimmt entrindete Stämme wie normale an, die Tags `#<holz>_logs` enthalten sie.
- Später denkbar: Stämme aus Input-Kisten am Hackklotz entrinden. Schwieriger, weil die Regeln einen Block in der Welt prüfen und kein Item.

## Konzept: Baumfarm (noch nicht umgesetzt)

Die ursprüngliche Idee geht weiter als der jetzige Stand:

- **Arbeitsbereich** statt Umkreis: Der Spieler markiert die Fläche, nur dort wird gefällt und gepflanzt.
- **Pflanzraster:** Setzlinge in festem Abstand (2–3 Blöcke, je Baumart; 2×2 für Schwarzeiche, Fichte, Tropenbaum) neu anlegen, nicht nur dort nachpflanzen, wo ein Baum stand.
- **Baumart wählen** im Menü (oder „was er dabeihat“).
- **Knochenmehl** zum schnelleren Wachsen.
- **Direkt verarbeiten:** Bretter oder Stufen statt Stämme abliefern. Dafür gibt es inzwischen den [Holzsäger](holzsaeger.md).

## Master-Fähigkeit (Konzept)

Ganzer Baum in einem Schlag (Treecapitator-Stil) oder doppelter Setzlingsertrag.

## Sonderfälle

- Riesenbäume, Pilze, Mangroven, Nether-Stämme: Ob die Baumerkennung sie zuverlässig fällt, ist noch nicht durchgetestet. Nether-Stämme stehen nicht auf Erde und werden deshalb nicht gefällt.
- Setzlinge wachsen ohne Spieler in der Nähe nur, wenn der Holzfäller [ohne Spieler arbeitet](../dorf/chunk-loading.md); dann bleibt der Umkreis `lumberjack.searchRadius` geladen.

## Definition of Done (M1)

Ein Holzfäller läuft drei Ingame-Tage ohne Eingriff, hängt nicht fest, beschädigt nichts außer natürlichen Bäumen und liefert messbar Holz.

## Siehe auch

[Holzsäger](holzsaeger.md) · [Tischler](tischler.md) · [Logistik](../grundlagen/logistik.md) · [Steuerung](../grundlagen/steuerung.md) · [Roadmap](../planung/roadmap.md)
