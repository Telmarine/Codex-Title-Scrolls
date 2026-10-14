# Codex: Title Scrolls changelog

## 1.0.2

- Title Scrolls is now part of the Codex family: renamed to Codex: Title Scrolls, and the jar is now `codex_title_scrolls-<version>.jar`.
- Java package moved to `com.tac5studios.codextitlescrolls`. The mod id is still `titlescrolls`, so existing scrolls, titles and title datapacks keep working.
- Chat mods that read titles by class name need to look in the new package (Elements: Nexus does from 1.0.2 on).

## 1.0.1

- New `/titlescroll give <player> <title> [amount]` (OP level 2). Title ids tab-complete, and scrolls that don't fit are dropped at the player's feet.
- Titles can have an optional `chat_color` (e.g. `"&b"` or `"&#FFD700"`): the color of the player's chat messages while they wear the title. Chat mods read it (Elements: Nexus 1.0.1 does). It stays on the server and is never sent to the /title menu.
- Title Scrolls now show their title and rarity when you hover over them in an inventory (scrolls from loot too).
- Minecraft and NeoForge version ranges narrowed to 1.21.1 / 21.1.x so mod sites list the right versions.
