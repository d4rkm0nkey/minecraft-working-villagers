# Installation

## Requirements

| | |
| --- | --- |
| Minecraft | **26.3** |
| Java | **25** (the Minecraft launcher brings the right version) |
| Mod loader | **Fabric** (with Fabric API) **or NeoForge** |

The mod has to be installed on **both** the client and the server. In singleplayer
that is just your game.

## Fabric

1. Install the [Fabric Loader](https://fabricmc.net/use/installer/) for Minecraft 26.3
   (version 0.19 or later).
2. Download [Fabric API](https://modrinth.com/mod/fabric-api) for Minecraft 26.3.
3. Download `working-villagers-fabric-<version>.jar`.
4. Put both jars into the `mods` folder of your Minecraft directory:
    - Windows: `%appdata%\.minecraft\mods`
    - macOS: `~/Library/Application Support/minecraft/mods`
    - Linux: `~/.minecraft/mods`
5. Start Minecraft with the *Fabric Loader* profile.

## NeoForge

1. Install [NeoForge](https://neoforged.net/) for Minecraft 26.3.
2. Download `working-villagers-neoforge-<version>.jar`.
3. Put it into the `mods` folder (see above).
4. Start Minecraft with the *NeoForge* profile.

!!! tip "Launchers"
    With Prism Launcher, the Modrinth App or the CurseForge app, create an instance for
    Minecraft 26.3 with Fabric or NeoForge and add the mod there. Remember to add Fabric API
    on Fabric.

## Dedicated server

Install the same loader on the server, put the same jar (and Fabric API on Fabric) into
the server's `mods` folder and restart. Every player who joins needs the mod too.

The server settings are in `config/workingvillagers.json`, see
[Configuration](configuration.md).

## Updating

Replace the old jar with the new one. During the beta, **back up your world first**:
the way workers and stations are saved may still change.

## Uninstalling

Removing the mod removes its blocks and items from the world, and workers lose their
jobs. This is not well tested yet, so back up your world before you try it.
