# Dorfbuch

[← Wiki-Start](../README.md)

Das Dorfbuch ist die zentrale Übersicht eines [gekauften Dorfes](dorf-und-kauf.md): Einwohnerregister, Geburten und Todesfälle mit Ursache, Live-Status aller Arbeiter, Versorgung und Kasse.

## Aufbau

Reiter: **Arbeiter** · **Register** · **Versorgung** · **Kasse** · **Dorf**

Im Kopf jeder Seite: Dorfname, Einwohner, Arbeiter, Chunk-Status und groß hervorgehoben der Kassenstand mit Lohnkosten pro Woche und Reichweite.

## Mock-up: Reiter Arbeiter

```
┌──────────────────────────────────────────────────────────────────────┐
│ Dorf Birkenau                                  ┌──────────────────┐  │
│ 14 Einwohner · 5 Arbeiter · Chunks geladen     │ Dorfkasse        │  │
│                                                │ 212 Emeralds     │  │
│ [Arbeiter] [Register] [Versorgung] [Kasse] [Dorf]  96/Woche · 2 Wo. │  │
│                                                └──────────────────┘  │
│ Name    Beruf             Status                    Wochenlohn       │
│ ──────────────────────────────────────────────────────────────────── │
│ Otto    Träger            (rot)  Blockiert: kein Weg     8 Emeralds  │
│ Greta   Minenarbeiter I   (gelb) Braucht: Spitzhacke    16 Emeralds  │
│ Ilse    Schmied III       (gelb) Wartet: Zielkiste voll 32 Emeralds  │
│ Bruno   Holzfäller II     (grün) Arbeitet: fällt Birke  24 Emeralds  │
│ Karl    Holzfäller I      (grau) Pause                  16 Emeralds  │
│ ──────────────────────────────────────────────────────────────────── │
│ Zuletzt verstorben: Hans, Minenarbeiter II · Tag 41 · Zombie, Stollen │
└──────────────────────────────────────────────────────────────────────┘
```

Probleme stehen immer oben; ein Klick auf einen Arbeiter markiert ihn in der Welt (Leuchten wie Spectral-Effekt, nur für den Besitzer).

## Status der Arbeiter

| Status | Bedeutung | Anzeige |
| --- | --- | --- |
| Arbeitet | führt seine Aufgabe aus, mit Detail („fällt Birke“) | grün |
| Unterwegs | auf dem Weg zu Kiste, Bereich oder Bett | grau |
| Pause / Schläft | außerhalb der Arbeitszeit | grau |
| Wartet | Zielkiste voll, nichts zu tun (Baum wächst noch) | gelb |
| Braucht | Werkzeug, Setzlinge, Fackeln oder Nahrung fehlen | gelb |
| Hungrig / Kein Bett | [Bedürfnis](../wirtschaft/beduerfnisse.md) nicht erfüllt | gelb |
| Blockiert | kein Weg, Bereich ungültig, Station zerstört | rot |
| Unbezahlt | Dorfkasse leer, Moral sinkt | rot |
| In Gefahr | flieht vor Mob, verletzt | rot |

Weitere Spalten: Wochenlohn (umschaltbar auf Tageslohn), Ertrag heute und diese Woche, Moral. Ist ein Arbeiter länger als z. B. einen halben Ingame-Tag rot, erscheint ein Toast beim Besitzer.

## Register

| Register | Eintrag | Daten |
| --- | --- | --- |
| Einwohner | jeder Villager im Dorf | Name, Beruf, Level, angestellt ja/nein, Bett |
| Geburten | Villager-Baby geboren | Ingame-Tag, Name, Eltern |
| Zu- und Wegzug | Villager kommt hinzu oder wandert ab | Tag, Grund (kein Bett, neues Dorf) |
| Personal | Einstellung, Beförderung, Kündigung | Tag, Beruf, Grund der Kündigung |
| Sterbefälle | Villager stirbt | Tag, Ursache, Ort, Beruf, letzte Aufgabe, verlorenes Werkzeug |
| Verwandlungen | Zombifizierung, Blitz → Hexe | Tag, Ursache; Zombie-Villager heilbar, Eintrag bleibt offen |
| Ereignisse | Überfall auf Karawane, Raid | Tag, Ort, Verluste |

Die Todesursache kommt direkt aus der `DamageSource`: z. B. Zombie, Creeper-Explosion, Sturz, Lava, Ertrinken, Ersticken (Kies/Sand), Spieler. Stirbt ein Angestellter, bekommt der Besitzer eine Chatmeldung wie bei benannten Haustieren in Vanilla.

## Weitere Reiter

- **Versorgung:** Bedarf, Bestand und Reichweite der Vorratskammern (siehe [Bedürfnisse](../wirtschaft/beduerfnisse.md)).
- **Kasse:** Einnahmen und Ausgaben pro Woche, Steuersatz, Handelsrouten (siehe [Einnahmen](../wirtschaft/einnahmen.md)).
- **Dorf:** Wohlstandsstufe, geladene Chunks mit Kosten und zugehörigem Arbeiter (siehe [Chunk-Loading](chunk-loading.md)), Rollen der Mitspieler.
