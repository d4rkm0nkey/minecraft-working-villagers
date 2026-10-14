# Multiplayer

[← Wiki-Start](../README.md)

Das Konzept ist gut multiplayerfähig, wenn es von Anfang an eingeplant wird: Die Arbeit läuft komplett auf dem Server, und Dorf, Arbeiter und Rechte hängen an Spieler-UUIDs. Schwierig sind nicht die Mechaniken, sondern Eigentumskonflikte, Griefing und Serverlast.

| Thema | Problem | Lösung |
| --- | --- | --- |
| Eigentum | Zwei Spieler wollen dasselbe Dorf | Wer zuerst kauft, besitzt es. Limit Dörfer pro Spieler (Config). Verkauf/Übertragung an andere Spieler per Urkunde. |
| Rollen | Freunde sollen mitverwalten | Besitzer, Verwalter (alles außer Verkauf), Leser (nur Dorfbuch). Optional Kopplung an Team-Mods (z. B. FTB Teams, Open Parties and Claims). |
| Fremdzugriff | Andere Spieler entlassen Arbeiter oder verstellen Stationen | Anheuern, Entlassen und Workstation-GUI nur mit Rolle. |
| Kisten | Vanilla schützt keine Kisten; Fremde können Ertrag stehlen | Kein eigener Kistenschutz; stattdessen Claim-Mods unterstützen. Optional: Dorfkasse nur für Rollen öffenbar. |
| Griefing | Spieler töten fremde Arbeiter | [Sterberegister](../dorf/dorfbuch.md#register) nennt den Spieler als Ursache. Config: Angestellte gegen fremde Spieler unverwundbar. |
| Inaktive Spieler | Dörfer bleiben ewig gesperrt und geladen | Nach X Tagen Inaktivität (Config, z. B. 30 Echtzeit-Tage) Chunks entladen, später Dorf wieder freigeben. |
| Offline-Besitzer | Soll das Dorf arbeiten, wenn der Besitzer offline ist? | Config: immer, nur wenn ein Teammitglied online ist, oder nur wenn der Besitzer online ist (siehe [Chunk-Loading](../dorf/chunk-loading.md)). |
| Serverlast | Viele Spieler × viele Arbeiter × geladene Chunks | Globales Chunk-Limit, Tick-Budget pro Arbeiter, Admin-Befehl zur Auflistung aller Dörfer und ihrer Last. |
| Netzwerk | Dorfbuch mit vielen Arbeitern | Daten nur beim Öffnen senden, Live-Updates nur für offene GUIs. |
| Spielerhandel | Spieler wollen untereinander handeln | Handelsrouten zwischen Spielerdörfern über den [Händler](../berufe/haendler.md) (Idee). |

## Technische Pflicht ab M1

- Mod muss auf Client und Server installiert sein (neue Blöcke, GUIs). Vanilla-Clients können nicht beitreten.
- Keine Client-Klassen in gemeinsamem Code; jede Version auch auf einem Dedicated Server testen.
- Alle Aktionen serverseitig prüfen (Rolle, Reichweite), nie dem Client vertrauen.
- Dorf- und Arbeiterdaten als `SavedData`, nie im Spieler-Inventar oder auf dem Client.

Siehe auch: [Technische Architektur](architektur.md)
