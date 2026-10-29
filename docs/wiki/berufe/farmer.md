# Beruf: Farmer

[← Berufe](README.md) · Station: **Komposter** (Vanilla) · Phase: **umgesetzt (ohne Pflügen)** · im Spiel: **Bauer**

Der Bauer erntet die Felder rund um seinen Komposter, sät neu und liefert die Ernte in die Zielkiste. Er ist die Grundlage für die geplante [Vorratskammer](../wirtschaft/beduerfnisse.md#vorratskammer).

## Workstation: Komposter

- Der normale Vanilla-Komposter. Rechtsklick **mit leerer Hand** öffnet die Stationsübersicht (ist der Komposter voll, kommt erst das Knochenmehl heraus). Kompostieren mit einem Item in der Hand bleibt wie in Vanilla.
- Vanilla-Bauern aus dem Dorf arbeiten weiter an Kompostern. Ein angeheuerter Dorfbewohner wird dort Bauer dieses Mods.
- Kisten verknüpft man mit **„+ Kiste“**. Er braucht nur Zielkisten, keinen Eingang.

## Stand der Umsetzung

- **Nur vorhandenes Ackerland:** Er bearbeitet Ackerland im Umkreis von 8 Blöcken um den Komposter (seitlich, Config `farmer.fieldRadius`, 2–16), bis 3 Blöcke tiefer und 2 höher. Er pflügt nicht und verändert nichts außer den Pflanzen. Angeheuerte Dorfbewohner zertrampeln kein Ackerland.
- **Ernten und säen:** Reife Pflanzen erntet er und setzt dieselbe Frucht neu. Leeres Ackerland bepflanzt er mit der Frucht der Nachbarfelder, sonst mit dem Saatgut, von dem er am meisten hat. Für eine Pflanze braucht er etwa 1,5 Sekunden (Config `farmer.secondsPerCrop`).
- **Saatgut:** alles, was Vanilla-Dorfbewohner pflanzen (Weizensamen, Karotten, Kartoffeln, Rote-Bete-Samen …). Er behält bis zu 16 je Sorte (Config `farmer.keepSeeds`) und sät aus der Ernte nach.
- **Keine Quellkiste:** Hacke und Saatgut zum Start gibt der Spieler ihm direkt, per Rechtsklick oder indem er sie vor ihm fallen lässt (siehe [Anheuern & Lohn](../wirtschaft/anheuern-und-lohn.md#stand-der-umsetzung)). Die Hacke nutzt sich je Ernte einmal ab; ohne Hacke wartet er.
- **Abliefern:** die ganze Ernte außer dem behaltenen Saatgut in eine Zielkiste mit Platz. Ohne Zielkiste fängt er nicht an.
- **Nicht umgesetzt:** Pflügen, Kompostieren zu Knochenmehl, Fruchtwahl im Menü, Master-Fähigkeit.
- **Status im Menü:** „Sucht reife Feldfrüchte“, „Arbeitet auf dem Feld“, „Wartet, bis die Feldfrüchte wachsen“, „Wartet: kein Saatgut für das leere Ackerland“, „Wartet: braucht eine Hacke“, dazu „Heute geerntet: x“.

## Konzept (noch nicht umgesetzt)

1. Erde im Bereich pflügen (nur Erde/Gras, keine anderen Blöcke), Wasser bleibt unangetastet.
2. Überschüssige Pflanzenreste kompostieren → Knochenmehl in die Zielkiste.
3. Fruchtwahl im Menü: feste Frucht oder „gemischt“ (gut für das Bedürfnis Abwechslung).

## Hinweise

- Ohne Spieler in der Nähe wachsen Felder nur, wenn der Bauer [ohne Spieler arbeitet](../dorf/chunk-loading.md); dann bleibt der Umkreis `farmer.fieldRadius` geladen.

## Master-Fähigkeit (Konzept)

Nutzt eigenes Knochenmehl automatisch, Ertrag +1 pro Ernte.

## Ideen

- Bäcker-Variante: Weizen direkt zu Brot verarbeiten.
- Saisonale Effekte mit Mods wie Serene Seasons.

## Siehe auch

[Bedürfnisse](../wirtschaft/beduerfnisse.md) · [Metzger](metzger.md) · [Fischer](fischer.md)
