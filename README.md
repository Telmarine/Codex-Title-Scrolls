# Title Scrolls

A small NeoForge 1.21.1 mod: players find a consumable **Title Scroll** item
in loot, right-click it to unlock a collectible title, then run **/title**
to open a real GUI showing titles they've collected (click one to make it
active) and titles they haven't found yet (shown as "???" — no name, rarity,
or flavor text revealed until you actually unlock it).

Built for Tenko MC, but the mod itself has **zero titles hardcoded** — every
title is defined by a small JSON file a server owner writes themselves. See
`docs/example_title.json.txt` for the full schema (three fields: `display`,
`rarity`, `flavor_text`) and how to add your own.

## Status

Source-complete, **not yet compiled or run against a real NeoForge/Minecraft
jar**. See "Building" below for why, and what's needed to actually produce a
working .jar.

## Features

- One reusable item (no new item per title — which title a scroll grants is
  a small data tag on that specific stack, set via a loot table entry or
  `/give`).
- Per-player persistent unlock storage (survives logout/relogin) and one
  active-title slot.
- `/title` opens a real scrollable GUI screen — not a command argument you
  have to already know the name of, not a chat menu.
- Undiscovered titles are a full mystery: no name, no rarity hint, nothing,
  until the player actually finds and unlocks that title.
- Unlock feedback goes to chat (not the actionbar) and stays in scrollback.
- A duplicate scroll (a title you already own) is **not consumed** — it
  stays in your inventory so you can hand it to someone who doesn't have it.
- Optional NeoEssentials integration: registers a `{tenko_title}` chat
  placeholder if NeoEssentials is detected, so a server already running it
  can slot the active title into its own chat-format config next to the
  existing rank prefix. Entirely skipped, no error, if NeoEssentials isn't
  installed — this mod has no required dependencies.

## Adding your own titles

See `docs/example_title.json.txt` for the fully-annotated schema. Short
version: drop a file at `data/<your_namespace>/titles/<title_id>.json` with
`display` (required), `rarity` (optional, defaults to `"common"`), and
`flavor_text` (optional). Run vanilla's own `/reload` and it's live — no
mod-specific reload command, no recompiling.

## Giving out a title

A title is granted by an item stack's `titlescrolls:grants_title` data
component being set to that title's id. Two ways to get one into the world:

```
/give @p titlescrolls:title_scroll[titlescrolls:grants_title="kitsune"]
```

or as a Lootr (or vanilla) loot table entry — same item, same component, as
one entry among your existing loot pools:

```json
{
  "type": "minecraft:item",
  "name": "titlescrolls:title_scroll",
  "functions": [
    {
      "function": "minecraft:set_components",
      "components": {
        "titlescrolls:grants_title": "kitsune"
      }
    }
  ]
}
```

## Building

This project targets **NeoForge 21.1.250 / Minecraft 1.21.1 / Java 21** (see
`gradle.properties` — change `neo_version` if you're building against a
different server's loader version).

**This source tree was written and organized in a sandboxed environment
without outbound network access to Mojang's, NeoForge's, or Maven Central's
servers** — so the Gradle build was never actually run against real
Minecraft/NeoForge jars here. Every file is written to match the real 1.21.1
NeoForge API as closely as I could get from documentation and known
conventions, but it has NOT been proven to compile. To actually build it:

1. Open this folder in IntelliJ IDEA (or your preferred IDE) with a JDK 21
   available, or just run Gradle from a terminal — either way needs real
   internet access to `maven.neoforged.net`, `libraries.minecraft.net`, and
   Maven Central to download the NeoForge userdev artifacts, Minecraft
   libraries, and mappings (a few hundred MB to a couple GB the first time).
2. **Optional:** if you want the NeoEssentials chat bridge included, drop
   the exact NeoEssentials jar you run on your server into `libs/` before
   building. If you don't want it, delete
   `src/main/java/com/tenko/titlescrolls/integration/NeoEssentialsIntegration.java`
   and the one guarded call to it in `TitleScrolls.java`'s `commonSetup()`.
3. Run `gradle build` (or `./gradlew build` if you generate a wrapper first
   with `gradle wrapper`). The finished jar lands in `build/libs/`.
4. Fix any compile errors that come up. The single most likely spot,
   flagged in its own file, is `client/TitleScreen.java` —
   `ObjectSelectionList`'s exact constructor signature moves around between
   Minecraft versions more than anything else here; everything else
   (item, data components, attachments, networking, commands) is written
   against APIs that have been stable for several NeoForge releases in a
   row and are far less likely to need changes.
5. Test with a real player per the standing project rule: a clean server
   boot alone doesn't confirm anything works. Have a real player
   right-click a test scroll, confirm the unlock message and that a
   duplicate doesn't consume the item, run `/title` and confirm the GUI
   opens with undiscovered titles as pure "???", select a title, and (if
   the chat bridge is in use) confirm the chat line actually shows the
   title next to the existing rank prefix. Also confirm the active title
   survives a relog.

## License

MIT — see `LICENSE`. No required dependencies; NeoEssentials is optional
and detected at runtime, never bundled.
