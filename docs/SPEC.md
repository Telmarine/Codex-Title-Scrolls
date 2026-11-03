# Codex: Title Scrolls - Spec

## 1.0.3 workup

Status: built. Mod id stays `titlescrolls`. Server and client must both run 1.0.3 (network version 2).

### Titles on players
Each player's active title shows on them in multiplayer, drawn by the client.

- **Position** (server setting): `above` (its own line over the name tag, default) or `beside` (added after the name on the same line).
- **Format** (server setting): text around the title, `{title}` is replaced. Default `"{title}"`. Example: `"✦ {title} ✦"`. The title keeps its own `&` colors.
- **Scale** (server setting, `above` only): size of the title line compared to the name. Default 1.0.
- **Distance**: same as the vanilla name tag.
- Hides whenever the name tag hides: sneaking (fades like the name), invisible, spectator, past name tag distance. Same see-through rule as the name.
- Not shown on yourself, same as vanilla names.
- The name tag itself isn't replaced, so rank prefixes from Elements: Nexus or other mods still show. No scoreboard or team changes.
- **Client setting**: hide other players' titles (`show_titles = true`, client config).

How it works:
- Server sends a player's active title text (the display text only) to clients tracking that player: on title change, on login, and when a player comes into tracking range.
- Locked titles are never sent, as now.
- Client draws it in NeoForge's `RenderNameTagEvent` for players: a second line above the name, or appended to the name text.

### Master Control File
`config/titlescrolls/features.toml`, needs a restart. One switch per feature, a switched-off feature doesn't load.

| Key | Default | What it does |
| --- | --- | --- |
| `title_scrolls` | true | Right-clicking a Title Scroll unlocks its title |
| `title_menu` | true | `/title` opens the title menu |
| `give_command` | true | `/titlescroll give` |
| `scroll_tooltip` | true | Hovering a scroll shows its title and rarity |
| `nametag_titles` | true | Active title shows on the player |
| `chat_bridge` | true | `{active_title}` placeholder for NeoEssentials. Elements: Nexus reads titles directly and isn't affected |

Plus `[overlap_guard]`: `enabled`, `auto_detect`, `mods` (`"feature=modid"`), `ignore_mods` (default `["elements_nexus"]`) and `force_on`.

### Overlap guard
Turns a feature off when an installed mod already does the job. `force_on` keeps it on.

- `nametag_titles` is guarded by **auto-detection** at start, no list of mod ids needed. A mod counts when both are true:
  - its id, name or description is about titles ("title", "epithet", "honorific"), not counting "title screen" or main menu mods
  - it hooks name tag rendering (a `RenderNameTagEvent` listener or a mixin into the player/entity renderers, read from NeoForge's start-up annotation data), or its description talks about name tags or text over players' heads
- On-screen title mods (biome or area titles) don't hook name tags, so they don't count.
- Every mod found is named in the server log. `ignore_mods` keeps a mod out; `mods` adds one by hand.
- Runs on both server and client: the server stops sending titles, and a client with such a mod doesn't draw them.

### Settings
`config/titlescrolls/settings.toml`, reloads with `/titlescroll reload`.

```
[nametag]
position = "above"   # "above" or "beside"
format = "{title}"
scale = 1.0
```

### Title files
- Titles move from datapacks to `config/titlescrolls/titles/`, one JSON file per title, same fields as before (`display`, `rarity`, `flavor_text`, `chat_color`).
- The id is the file name. A file one folder down (`titles/starter/knight.json`) is `starter:knight`, which is how 1.0.2 named the second of two datapack titles with the same name. Deeper folders are skipped with a log line.
- Read at server start (before anyone can join) and on `/titlescroll reload`. Vanilla `/reload` no longer touches titles.
- `TitleScrolls.TITLE_REGISTRY` keeps `get(id)` and `getAll()`, so chat mods that read titles by name keep working.

### Datapack import
- First start with 1.0.3: every datapack title (`data/<namespace>/titles/<id>.json`) is copied into `titles/` with the id it had in 1.0.2, so unlocked titles and scrolls already handed out keep working. A marker file (`config/titlescrolls/.datapack_import_done`) stops it running again.
- A title already in the folder is never overwritten. Invalid titles are skipped and logged.
- `/titlescroll import datapacks` runs it again by hand.
- After the import, datapack titles aren't read; the log says the datapack can be removed.

### Migration from another title mod
`config/titlescrolls/migration/`, made at first start with an example `migrate.json`.

- `/titlescroll migrate scan`: reads other mods' title data without needing to know their layout, and adds what it finds to `migrate.json` (nothing is applied yet).
  - Files read: JSON in `config/` with "title" or "epithet" in the path (not title screen or main menu mods, not Title Scrolls' own folder), `world/data/*.dat` with "title" or "epithet" in the name, and every `world/playerdata/*.dat` (only attachments and capabilities with "title" or "epithet" in their name).
  - A title is an object with a text field (`display`, `name`, `title`, `text`, ...) under a "title" key, or with title fields (`rarity`, `description`, `color`, `conditions`, ...). Colors given as a name, `#hex` or number become `&` codes, `§` becomes `&`.
  - A player is a UUID (key, field or file name) with a list of titles and maybe a worn one.
  - A player title that matches no title found becomes a title of its own from its text (mods where any text is a title). `#newbie` becomes `newcomer` ("&aNewcomer"), `#none` is dropped.
  - `scan-report.txt` lists every file read and what came out of it.
- The owner checks or edits `migrate.json`, or fills it in by hand for a mod the scan can't read. Players can be a UUID or a name that has joined before.
- `/titlescroll migrate apply`: writes each title to `titles/` (existing ids skipped), reloads titles, gives online players their titles now and everyone else at next login (`pending_players.json`). The worn title is only set if the player wears none. Then `migrate.json` is renamed `migrate-applied-<date>.json`; problems go in `apply-problems.txt`.

### Reload
- New `/titlescroll reload` reloads `settings.toml` and every file in `titles/`, then resends active titles to clients.
- Only `features.toml` switches need a restart.

### Permissions
Registered at startup through the NeoForge permission API, so they work with Elements: Nexus ranks, any permission mod, or OP levels.

| Node | Default |
| --- | --- |
| `titlescrolls.menu` | everyone |
| `titlescrolls.give` | OP 2 |
| `titlescrolls.reload` | OP 3 |
| `titlescrolls.import` | OP 3 (`/titlescroll import datapacks`, `/titlescroll migrate`) |

### Unchanged
- The title file fields, the Title Scroll item, `/title` menu and saved player data all work as in 1.0.2.

### /title wear and /title off
- `/title wear <title>` puts on a title the player owns (tab-completes their titles), `/title off` takes it off. Same permission as `/title`.

### Scrolls in loot
- `scroll_loot` in `features.toml`, off by default.
- `settings.toml` `[loot]`: `tables` (loot table ids, `*` wildcard, default `minecraft:chests/*`), `exclude_tables` (default the spawn bonus chest), `chance` (0.05), `rarity_weights` (`common=50`, `rare=25`, `epic=15`, `legendary=8`, `unique=2`; unlisted rarities never drop), `exclude_titles`.
- Done with a NeoForge global loot modifier that reads the settings each roll, so `/titlescroll reload` applies changes right away. One scroll at most per table roll.

### Rarities
- `settings.toml` `[rarities] list`: `"name=#RRGGBB"`, defaults the five built-in rarities with their old colors. Reloads with `/titlescroll reload`.
- The color is the menu slot border and the rarity text on a scroll. The list order is the menu sort order. Unlisted rarities are gray and sort last; locked titles sort last as before.
- Sent to the client with the menu, so a server's own rarities need nothing on the client.

### Removed titles
- `settings.toml` `[cleanup] remove_missing_titles`, off by default: takes titles with no file off players at login and on `/titlescroll reload`.
- Skipped while the titles folder is empty or any title file failed to load.

### Decided against
- Features from other title mods (grant/revoke commands, starter title, progress unlocks, announcements, hints, combat lock, own title, inventory button, preview, built-in chat prefix, unique titles, stat buffs, tab list).
