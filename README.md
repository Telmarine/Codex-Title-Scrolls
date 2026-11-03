# Codex: Title Scrolls

![Minecraft](https://img.shields.io/badge/Minecraft-1.21.1-62B47A?style=for-the-badge) ![NeoForge](https://img.shields.io/badge/NeoForge-21.1.x-E68A2E?style=for-the-badge) ![Side](https://img.shields.io/badge/Side-Client%20%26%20Server-5865F2?style=for-the-badge) ![Type](https://img.shields.io/badge/Type-Quality%20of%20Life-8A2BE2?style=for-the-badge) [![Source](https://img.shields.io/badge/Source-GitHub-181717?style=for-the-badge&logo=github)](https://github.com/Telmarine/Codex-Title-Scrolls)

Players find a Title Scroll in loot, right-click it to unlock a title, then open `/title` to see their collection and pick the title they want to wear. Titles they haven't found yet show as "?" with no name, rarity or flavor text.

No titles are built in. Every title is a small JSON file in `config/titlescrolls/titles/`, so each server writes its own.

## Features

- One item for every title. Which title a scroll gives is stored on the stack, set by a loot table entry or `/give`.
- Unlocked titles and the active title are saved per player and survive relogs and deaths.
- `/title` opens a grid menu, 54 titles per page. Click a title to wear it, click it again to take it off. `/title wear <title>` and `/title off` do the same from chat.
- Rarity shows as the slot border: green common, blue rare, purple epic, gold legendary, red unique. Owners can add their own rarities with their own colors (see Rarities).
- Locked titles are never sent to the client, so nothing can be read ahead of time.
- A scroll for a title you already own is not used up, so it can be traded.
- Titles reload with `/titlescroll reload`.
- Moving from another title mod: titles and players' titles can be brought over (see Migrating).

## Adding titles

Add one file per title in `config/titlescrolls/titles/`, e.g. `config/titlescrolls/titles/kitsune.json`, then run `/titlescroll reload`:

```json
{
  "display": "&6Kitsune",
  "rarity": "unique",
  "flavor_text": "Outfoxed the impossible.",
  "chat_color": "&6"
}
```

- `display` (required): the title text. `&` color codes work.
- `rarity` (optional, default `common`): any rarity from `[rarities]` in `settings.toml`. Built in: `common`, `rare`, `epic`, `legendary`, `unique`.
- `flavor_text` (optional): a line shown in the menu once unlocked.
- `chat_color` (optional): the color of the player's chat messages while they wear the title, e.g. `&b` or `&#FFD700`. Needs a chat mod that reads it (Elements: Nexus does).

A title's id is its file name (`kitsune`). Files can also go one folder down: `titles/starter/knight.json` is the title `starter:knight`. Vanilla `/reload` doesn't touch titles.

### Titles from datapacks (1.0.2 and older)

Titles used to live in datapacks (`data/<namespace>/titles/<id>.json`). The first time the server starts with 1.0.3, every datapack title is copied into `config/titlescrolls/titles/` with the same id, so unlocked titles and scrolls already handed out keep working. After that, titles in datapacks aren't read, and the datapack can be removed. `/titlescroll import datapacks` copies them over again (titles already in the folder are left alone).

See `docs/example_title.json.txt` for more.

## Rarities

Rarities and their border colors are set in `settings.toml`, and reload with `/titlescroll reload`. To add a rarity, add a line with its name and color, then use that name in a title's `rarity`:

```toml
[rarities]
list = ["common=#55FF55", "rare=#5555FF", "epic=#AA00AA", "legendary=#FFAA00", "unique=#FF5555", "mythic=#00FFFF"]
```

- The color is the slot border in `/title` and the rarity text on a scroll's hover.
- The order of the list is the order titles are sorted in the menu.
- Any color can be changed, including the built-in ones.
- A rarity that isn't in the list shows gray and sorts last.
- For a new rarity to drop from loot, add it to `rarity_weights` in `[loot]` too.

## Scrolls in loot

Off by default. Turn on `scroll_loot` in `features.toml` (restart), then set it up in `settings.toml` (reloads with `/titlescroll reload`):

```toml
[loot]
tables = ["minecraft:chests/*"]                  # loot tables that can hold a scroll, * matches anything
exclude_tables = ["minecraft:chests/spawn_bonus_chest"]
chance = 0.05                                    # chance a matching table gets one scroll
rarity_weights = ["common=50", "rare=25", "epic=15", "legendary=8", "unique=2"]
exclude_titles = []                              # title ids that never drop
```

The scroll's title is picked by rarity weight. A rarity that isn't listed never drops. Tables from other mods work too (`"somemod:chests/*"`). Your own loot tables with scroll entries keep working either way.

## Removed titles

When a title file is deleted, players who own or wear it keep it, and it comes back if the file is put back. To take removed titles off players instead (at login and on `/titlescroll reload`), set this in `settings.toml`:

```toml
[cleanup]
remove_missing_titles = false
```

It never runs while the titles folder is empty or a title file has a problem, so one bad file can't wipe titles.

## Titles on players

A player's active title shows on them in multiplayer, drawn by the client. It hides whenever the name tag does (sneaking, invisible, spectator, too far away), and you don't see your own. The name tag itself isn't changed, so rank prefixes from other mods still show.

`config/titlescrolls/settings.toml`:

```toml
[nametag]
position = "above"   # "above" = its own line over the name, "beside" = after the name
format = "{title}"   # text around the title, & colors work, e.g. "&7✦ {title} &7✦"
scale = 1.0          # size of the title line ("above" only)
```

Players can hide other players' titles with `show_titles = false` in `config/titlescrolls-client.toml`.

## Giving a title

```
/titlescroll give <player> <title> [amount]
```

Title ids tab-complete. Needs `titlescrolls.give` (OP level 2 by default). The long vanilla way still works too:

```
/give @p titlescrolls:title_scroll[titlescrolls:grants_title="kitsune"]
```

Or as a loot table entry:

```json
{
  "type": "minecraft:item",
  "name": "titlescrolls:title_scroll",
  "functions": [
    {
      "function": "minecraft:set_components",
      "components": { "titlescrolls:grants_title": "kitsune" }
    }
  ]
}
```

## Migrating from another title mod

1. Keep the other mod's config and the world as they are, and start the server with Title Scrolls installed.
2. Run `/titlescroll migrate scan`. It reads the other mod's title files in `config/` and its saved data in the world (players' owned and worn titles), and writes what it found to `config/titlescrolls/migration/migrate.json`. Every file it read is listed in `scan-report.txt`. A title that's only known by its text (no title list) becomes a title of its own.
3. Check `migrate.json`. Fix ids, colors or rarities, or fill it in by hand if the other mod keeps its data somewhere the scan can't read:

```json
{
  "titles": {
    "dragon_slayer": { "display": "&6Dragon Slayer", "rarity": "epic", "flavor_text": "Brought down the dragon." }
  },
  "players": {
    "069a79f4-44e9-4726-a5be-fca90e38aaf5": { "unlocked": ["dragon_slayer"], "active": "dragon_slayer" },
    "SomePlayer": { "unlocked": ["dragon_slayer"] }
  }
}
```

4. Run `/titlescroll migrate apply`. Each title is written to `titles/` (ids that already exist are skipped). Online players get their titles right away, everyone else at their next login. The worn title is only set if the player isn't wearing one already. `migrate.json` is renamed to `migrate-applied-<date>.json` so it can't run twice, and anything that couldn't be applied is listed in `apply-problems.txt`.

Players can be listed by UUID or by name (a name only works if that player has joined the server before).

## Commands and permissions

| Command | Permission | Default |
| --- | --- | --- |
| `/title` | `titlescrolls.menu` | everyone |
| `/title wear <title>` / `/title off` | `titlescrolls.menu` | everyone |
| `/titlescroll give <player> <title> [amount]` | `titlescrolls.give` | OP 2 |
| `/titlescroll reload` | `titlescrolls.reload` | OP 3 |
| `/titlescroll import datapacks` | `titlescrolls.import` | OP 3 |
| `/titlescroll migrate scan` / `apply` | `titlescrolls.import` | OP 3 |

Nodes go through the NeoForge permission API, so they work with any permission mod or plain OP levels. `/titlescroll reload` re-reads `settings.toml` and every file in `titles/`.

## Config

- `config/titlescrolls/features.toml`: an on/off switch for every feature, plus the overlap guard. Needs a restart.
- `config/titlescrolls/settings.toml`: how titles on players look, scrolls in loot, removed titles. Reloads with `/titlescroll reload`.
- `config/titlescrolls/titles/`: one file per title. Reloads with `/titlescroll reload`.
- `config/titlescrolls/migration/`: `migrate.json` for moving from another title mod.
- `config/titlescrolls-client.toml`: each player's own `show_titles`.

## Overlap guard

When another installed mod already shows titles on players, titles on players turn off by themselves and the server log says which mod was found. Title Scrolls finds those mods on its own at start: a mod counts when it's about titles and it draws name tags or shows text over players' heads. Mods can also be listed in `overlap_guard.mods` (`"nametag_titles=modid"`), kept out with `ignore_mods`, and `force_on = ["nametag_titles"]` keeps the feature on anyway.

## Showing titles in chat

- **Elements: Nexus:** use `{title}` in Nexus' chat format. Detected automatically.
- **NeoEssentials:** Title Scrolls registers an `{active_title}` placeholder when NeoEssentials is installed.

Title Scrolls works without either.

## Building

Targets NeoForge 1.21.1 and Java 21. Build it in IntelliJ. The jar is written to `build/libs/`.

## License

Free to use in any modpack, paid or free, with credit to **Telmarine**. Not for resale or repackaging as a paid product on its own. See [LICENSE](LICENSE).
