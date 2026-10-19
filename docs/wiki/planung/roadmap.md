# Roadmap

[← Wiki-Start](../README.md)

Das MVP ist ein einziger [Holzfäller](../berufe/holzfaeller.md), der zuverlässig Holz liefert. Alles andere baut darauf auf.

Status: **erledigt** · **teilweise** · **offen**. Was genau umgesetzt ist, steht auf den Seiten unter „Stand der Umsetzung“.

1. **M0 – Fundament** · *erledigt*: Multiloader-Setup (Fabric und NeoForge), Holzsäge mit Steinsägen-Menü und Rezepten, Berufe und POIs registriert.
2. **M1 – MVP Holzfäller** · *erledigt, mit Abweichungen*: Anheuern mit Vertrag, Zuweisung über die [Stationsübersicht](../grundlagen/steuerung.md#stationsübersicht), Zielkisten, Fällen und Nachpflanzen, Axt-Verschleiß, Lohn als einfache Pauschale (1 Emerald pro Tag).
   Abweichungen vom Plan:
   - Station ist der **Hackklotz**, nicht die Holzsäge.
   - **Kein Arbeitsbereich:** Er fällt natürliche Bäume im Umkreis der Station; kein Pflanzraster, keine Baumartwahl.
   - **Keine Quellkiste:** Axt, Setzlinge und Lohn gibt man ihm direkt.
   - Alle Baumarten, die auf Erde wachsen, nicht nur Eiche, Birke und Fichte.
   - Dazu gekommen: Entrinden, Tagespensum, Rückholung festgesteckter Arbeiter.
3. **M2 – Wirtschaft & Dorf** · *teilweise*: umgesetzt ist der [Handelsangestellte](../berufe/handelsangestellter.md) (Verkauf über die Vanilla-Trades, Smaragde in Kisten). Offen: [Dorfkauf, Dorfkasse](../dorf/dorf-und-kauf.md), [Dorfbuch](../dorf/dorfbuch.md) mit Registern und Arbeiterübersicht, [Chunk-Loading](../dorf/chunk-loading.md), Wochenrhythmus, Moral, Kündigung, Level-Aufstieg durch Arbeit, Steuern und Wohlstand, [Bedürfnisse](../wirtschaft/beduerfnisse.md) Nahrung und Unterkunft.
4. **M3 – Minenarbeiter** · *erledigt bis auf Steinbruch-Modus und Level* ([Stand](../berufe/minenarbeiter.md#stand-der-umsetzung); den Steinbruch macht der Grubenarbeiter): [Grubenwerkbank](../berufe/minenarbeiter.md), Modi Steinbruch und Stollen, Fackeln setzen, Lava/Wasser abdichten, Stützen in Vanilla-Minenschacht-Optik. Als einfache Vorstufe ist der [Grubenarbeiter](../berufe/grubenarbeiter.md) umgesetzt.
5. **M4 – Logistik & Handel** · *teilweise*: [Träger](../berufe/traeger.md) ist umgesetzt (bis zu 3 Wege; einen Filter gibt es vorerst bewusst nicht, sortiert wird mit Redstone). Offen: [Crafter-Bediener](../berufe/crafter-bediener.md), Redstone-Signale an Stationen, [Händler](../berufe/haendler.md) mit Nachbardörfern, [Grubenlogistiker](../berufe/grubenlogistiker.md) (erst Depots + Trägerkette, dann Schiene mit Lore), [Etagenmine](../grundlagen/mine.md) mit Förderkorb. Aufträge gibt es schon an Holzsäge, Steinsäge und Hobelbank.
6. **M5 – Ausbau** · *teilweise*: umgesetzt sind [Bauer](../berufe/farmer.md) (ohne Pflügen), [Hirte](../berufe/hirte.md) (Scheren und Züchten), [Metzger](../berufe/metzger.md) (Züchten, Schlachten, Räuchern), [Fischer](../berufe/fischer.md) (Ufer und Boot), [Pfeilmacher](../berufe/pfeilmacher.md), [Wache](../berufe/wache.md) (Nachtschicht, Jagd, Überfälle, Erfahrung), [Apotheker](../berufe/apotheker.md) (Flaschen, Brauen, Heilen), [Gerber](../berufe/gerber.md) (Leder, Gerben, Färben, Uniformen) und [Schmied](../berufe/schmied.md) (Schmelzen und Reparieren), außerdem weitere Baumarten. Offen: Erzader-Modus, Bedürfnisse Abwechslung und Komfort, Jade/JEI-Integration.
7. **M6 – Richtung wählen** · *offen*: Aus der [Ideensammlung](ideensammlung.md) eine Leitlinie festlegen (z. B. Unternehmer mit Verkaufsstand und Buchhaltung).

**Ohne eigenen Meilenstein dazugekommen (umgesetzt):** [Holzsäger](../berufe/holzsaeger.md) an der Holzsäge, [Steinsäger](../berufe/steinsaeger.md) an der Steinsäge, [Tischler](../berufe/tischler.md) an der Hobelbank.

**Ausgearbeitete Konzepte ohne Meilenstein:** [Berufsbücher](../grundlagen/berufsbuecher.md). Umgesetzt ohne Meilenstein: [Grubenarbeiter](../berufe/grubenarbeiter.md), [Brenner](../berufe/brenner.md).

**Definition of Done für M1:** Ein Holzfäller läuft drei Ingame-Tage ohne Eingriff, hängt nicht fest, beschädigt nichts außer natürlichen Bäumen und liefert messbar Holz.

## Weitere geplante Punkte

- **[Arbeitsverträge je Beruf](../wirtschaft/arbeitsvertraege.md):** für jeden Beruf ein eigener Vertrag, nicht craftbar, sondern als Fund in Truhen von Dörfern, Minenschächten, Außenposten und Anwesen. Der Vertrag legt den Beruf des Angeheuerten fest. (Wunsch vom 15.10.2026)
- **[Dorftruhen](../dorf/dorftruhen.md)** · *umgesetzt, ohne Dorfkasse*: ein eigenes Item/Block „Dorftruhe“. Der Spieler kann mehrere aufstellen, und alle Arbeiter des Dorfs bedienen sich aus allen Dorftruhen mit Lohn (Smaragde), Werkzeug und Saatgut/Setzlingen. Optional zusätzlich zum direkten Geben. (Wunsch vom 15.10.2026)
- **[Werkzeugrückgabe](../dorf/werkzeugrueckgabe.md)** · *umgesetzt*: Arbeiter legen fast kaputtes Werkzeug in eine Dorftruhe (sonst in die Zielkiste), statt es zu zerbrechen; der Schmied repariert es am Schmiedetisch. (Wunsch vom 15.10.2026)
- **[Menüs überarbeiten](menues.md)** · *Konzept*: Reiter (Block · Arbeit · Auftrag) statt Knopf „i“, Vanilla-Maße, jedes Menü passt in 854×480. (Wunsch vom 18.10.2026)
