# Title Scrolls

![NeoForge 1.21.1](https://img.shields.io/badge/NeoForge-1.21.1-orange) ![Environment: Client & Server](https://img.shields.io/badge/Environment-Client%20%26%20Server-lightgrey) ![License: Custom](https://img.shields.io/badge/License-Custom-blue)

Players find a Title Scroll in loot, right-click it to unlock a title, then open `/title` to see their collection and pick the title they want to wear. Titles they haven't found yet show as "?" with no name, rarity or flavor text.

No titles are built in. Every title is a small JSON file in a datapack, so each server writes its own.

## Features

- One item for every title. Which title a scroll gives is stored on the stack, set by a loot table entry or `/give`.
- Unlocked titles and the active title are saved per player and survive relogs and deaths.
- `/title` opens a grid menu, 54 titles per page. Click a title to wear it, click it again to take it off.
- Rarity shows as the slot border: green common, blue rare, purple epic, gold legendary, red unique.
- Locked titles are never sent to the client, so nothing can be read ahead of time.
- A scroll for a title you already own is not used up, so it can be traded.
- Titles reload with vanilla `/reload`.

## Adding titles

Add a file at `data/<namespace>/titles/<id>.json`:

```json
{
  "display": "&6Kitsune",
  "rarity": "unique",
  "flavor_text": "Outfoxed the impossible.",
  "chat_color": "&6"
}
```

- `display` (required): the title text. `&` color codes work.
- `rarity` (optional, default `common`): `common`, `rare`, `epic`, `legendary` or `unique`.
- `flavor_text` (optional): a line shown in the menu once unlocked.
- `chat_color` (optional): the color of the player's chat messages while they wear the title, e.g. `&b` or `&#FFD700`. Needs a chat mod that reads it (Elements: Nexus does).

A title's id is its file name (`kitsune`). If two datapacks use the same file name, the second one is used as `namespace:id` and a warning is logged.

See `docs/example_title.json.txt` for more.

## Giving a title

```
/titlescroll give <player> <title> [amount]
```

Title ids tab-complete. Needs OP level 2. The long vanilla way still works too:

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

## Showing titles in chat

- **Elements: Nexus:** use `{title}` in Nexus' chat format. Detected automatically.
- **NeoEssentials:** Title Scrolls registers an `{active_title}` placeholder when NeoEssentials is installed.

Title Scrolls works without either.

## Building

Targets NeoForge 1.21.1 and Java 21. Build it in IntelliJ. The jar is written to `build/libs/`.

## License

Free to use in any modpack, paid or free, with credit to **Telmarine**. Not for resale or repackaging as a paid product on its own. See [LICENSE](LICENSE).
