# Berufe

[← Wiki-Start](../README.md)

Fast jeder Vanilla-Beruf hat schon eine Workstation, die sich als Arbeitsauftrag deuten lässt. Damit wächst der Mod ohne neue Blöcke. Neu sind nur Hackklotz, Holzsäge (Woodcutter), Hobelbank, Grubenwerkbank, Förderhaspel, Grubenpflock, Brennofen und für den Handel Handelstresen bzw. Handelskontor. Umgesetzt sind davon Hackklotz, Holzsäge, Hobelbank, Grubenpflock, Brennofen und Handelstresen; der [Träger](traeger.md) braucht statt eines Blocks den Weidenkorb.

Alle Berufe folgen dem [Kernloop](../grundlagen/kernloop.md) und den [Lohnstufen](../wirtschaft/anheuern-und-lohn.md#lohnstufen) (Lohnstufen sind noch Konzept, siehe [Stand der Umsetzung](../wirtschaft/anheuern-und-lohn.md#stand-der-umsetzung)). Jede Berufsseite beschreibt nur, was den Beruf besonders macht, inklusive seiner Master-Fähigkeit. Im Spiel erklärt jeden Beruf sein [Berufsbuch](../grundlagen/berufsbuecher.md).

| Beruf | Workstation | Arbeit als Angestellter | Neuer Block? | Phase |
| --- | --- | --- | --- | --- |
| [Holzfäller](holzfaeller.md) | Hackklotz | Bäume im Umkreis fällen und nachpflanzen | Hackklotz | umgesetzt (ohne Bereich/Raster) |
| [Holzsäger](holzsaeger.md) | Holzsäge (Woodcutter) | Stämme zu Brettern, Stufen, Treppen … | Holzsäge | umgesetzt |
| [Steinsäger](steinsaeger.md) | Steinsäge | Stein zu Ziegeln, Stufen, Treppen, Mauern | nein | umgesetzt |
| [Tischler](tischler.md) | Hobelbank | Holzgegenstände aus Planks, Sticks, Slabs | Hobelbank | umgesetzt |
| [Minenarbeiter](minenarbeiter.md) | Grubenwerkbank | Stollen 3×3 mit Stützen, Abzweige, Treppe; folgt Erzen; Werkbank ist Gesteinsmühle | Grubenwerkbank | umgesetzt (ohne Etagenmine und Level) |
| [Handelsangestellter](handelsangestellter.md) | Handelstresen | Verkauf über Vanilla-Trades | Handelstresen | umgesetzt (nur Verkauf) |
| [Träger](traeger.md) | keine (Weidenkorb) | Items von Kiste A nach Kiste B, bis zu 3 Wege | nein (Item Weidenkorb) | umgesetzt (ohne Filter) |
| [Crafter-Bediener](crafter-bediener.md) | Crafter | Crafter befüllen und auslösen | nein | M4 |
| [Grubenlogistiker](grubenlogistiker.md) | Förderhaspel | Depots, Trägerkette, Schienen mit Lore, Aufzug zwischen Etagen | Förderhaspel | M4 |
| [Händler](haendler.md) | Handelskontor | Karawanen zu Nachbardörfern | Handelskontor | M4 |
| [Farmer](farmer.md) (im Spiel: Bauer) | Komposter | Vorhandene Felder ernten und neu säen | nein | umgesetzt (ohne Pflügen) |
| [Schmied](schmied.md) | Schmelzofen / Schmiedetisch | Erze schmelzen, Werkzeug reparieren | nein | umgesetzt |
| [Grubenarbeiter](grubenarbeiter.md) | Grubenpflock | Mit Schnur umrandete Grube ausheben, Leiter als Ausgang | Grubenpflock | umgesetzt |
| [Brenner](brenner.md) | Brennofen | Glas, Stein, Ziegel, Holzkohle brennen (Ofen ohne Essen) | Brennofen | umgesetzt |
| [Fischer](fischer.md) | Barrel | Angeln an markiertem Wasser | nein | später |
| [Hirte](hirte.md) | Webstuhl (Loom) | Schafe scheren und züchten, Wolle abliefern | nein | umgesetzt (ohne Wolle sortieren) |
| [Metzger](metzger.md) | Räucherofen (Smoker) | Tiere züchten, schlachten, räuchern | nein | umgesetzt |
| [Gerber](gerber.md) | Cauldron | Leder verarbeiten | nein | später |
| [Maurer](maurer.md) | Steinsäge | Ausbau des Steinsägers (Glattstein, Master) | nein | später |
| [Pfeilmacher](pfeilmacher.md) | Fletching Table | Pfeile herstellen | nein | später |
| [Kartograf](kartograf.md) | Cartography Table | Gebiete und Dörfer erkunden | nein | später |
| [Kleriker](kleriker.md) | Brewing Stand | Tränke brauen | nein | später |
| [Bibliothekar](bibliothekar.md) | Lectern | Lagerverwaltung, Inventarliste | nein | später |
| [Wache](wache.md) | – (Idee) | Schutz von Minen und Karawanen | offen | Idee |

Umgesetzt sind: Holzfäller, Holzsäger, Steinsäger, Tischler, Bauer, Hirte, Metzger, Schmied (Schmelzen und Reparieren), Grubenarbeiter, Minenarbeiter, Brenner, Handelsangestellter (Verkauf) und Träger. Damit läuft die Holzkette Holzfäller → Holzsäger → Tischler, verbunden durch Träger (siehe [Logistik](../grundlagen/logistik.md)), und Erz kommt aus der Mine zum Schmied.

Als Nächstes stehen auf der [Roadmap](../planung/roadmap.md): Wirtschaft und Dorf (M2) und der [Crafter-Bediener](crafter-bediener.md) (M4). Mit ihm ist die Kette Erz → Barren → Crafter geschlossen.

## Vorlage für neue Berufsseiten

```markdown
# Beruf: <Name>
[← Berufe](README.md) · Station: … · Phase: …
<Ein Satz: was er tut.>
## Workstation
## Arbeitsablauf
## Kisten (Eingang / Ausgang)
## Stand der Umsetzung
## Master-Fähigkeit
## Sonderfälle & offene Fragen
## Berufsbuch (welcher Vanilla-Beruf verkauft es)
## Siehe auch
```
