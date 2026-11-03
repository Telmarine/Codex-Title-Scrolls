# Codex: Title Scrolls changelog

## 1.0.3

- A player's active title now shows on them in multiplayer: on its own line above the name tag, or beside the name. Position, text around the title and size are set in `settings.toml`. Each player can hide other players' titles in `titlescrolls-client.toml`.
- New `features.toml` with an on/off switch for every feature: scrolls, the `/title` menu, `/titlescroll give`, the scroll tooltip, titles on players and the chat placeholder.
- New overlap guard: titles on players turn off by themselves when another installed mod already shows titles on players. Mods can also be listed by hand, and `force_on` keeps a feature on.
- Titles moved from datapacks to `config/titlescrolls/titles/`, one file per title. Datapack titles are copied over on the first start, keeping their ids. `/titlescroll import datapacks` copies them again.
- New `/titlescroll reload`: re-reads `settings.toml` and the title files. Vanilla `/reload` no longer reloads titles.
- New `/title wear <title>` and `/title off`.
- Title Scrolls can be added to loot: `scroll_loot` in `features.toml` (off by default), with loot tables, chance, rarity weights and excluded titles in `settings.toml`.
- Rarities are set in `settings.toml`: add your own rarity with its own border color, change the built-in colors, and set the order titles are sorted in the menu.
- New `remove_missing_titles` setting (off by default): takes titles whose file was removed off players.
- New `/titlescroll migrate scan` and `apply`: brings titles and players' titles over from another title mod through `config/titlescrolls/migration/migrate.json`.
- Permission nodes `titlescrolls.menu` (everyone), `titlescrolls.give` (OP 2), `titlescrolls.reload` (OP 3) and `titlescrolls.import` (OP 3), registered at start, so permission mods can manage them.
- Server and client must both run 1.0.3.

## 1.0.2

- Title Scrolls is now part of the Codex family: renamed to Codex: Title Scrolls, and the jar is now `codex_title_scrolls-<version>.jar`.
- Java package moved to `com.tac5studios.codextitlescrolls`. The mod id is still `titlescrolls`, so existing scrolls, titles and title datapacks keep working.
- Chat mods that read titles by class name need to look in the new package (Elements: Nexus does from 1.0.2 on).

## 1.0.1

- New `/titlescroll give <player> <title> [amount]` (OP level 2). Title ids tab-complete, and scrolls that don't fit are dropped at the player's feet.
- Titles can have an optional `chat_color` (e.g. `"&b"` or `"&#FFD700"`): the color of the player's chat messages while they wear the title. Chat mods read it (Elements: Nexus 1.0.1 does). It stays on the server and is never sent to the /title menu.
- Title Scrolls now show their title and rarity when you hover over them in an inventory (scrolls from loot too).
- Minecraft and NeoForge version ranges narrowed to 1.21.1 / 21.1.x so mod sites list the right versions.
