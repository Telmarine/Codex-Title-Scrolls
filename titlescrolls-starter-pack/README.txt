# Title Scrolls — Starter Title Pack

34 example titles across all five rarity tiers (common, rare, epic, legendary, unique), each with flavor text. Use this to get titles working on day one, or as a reference for writing your own.

## Installation

1. Copy the files in `data/starter/titles/` into `config/titlescrolls/titles/`.
2. If the server is already running, run `/titlescroll reload`. Otherwise just start the server.
3. The server log should show `Loaded 34 title(s) from config/titlescrolls/titles`.
4. Test with `/give @p titlescrolls:title_scroll[titlescrolls:grants_title="rookie"]`, then right-click the scroll to unlock it.

## Editing

Every title is a plain JSON file:

```json
{
  "display": "&7Rookie",
  "rarity": "common",
  "flavor_text": "Everyone starts somewhere."
}
```

Edit, delete, or add new title files freely. Run `/titlescroll reload` to pick up changes. `display` supports `&`-color codes, `rarity` sets the slot border color in the `/title` menu (common, rare, epic, legendary, unique, or a rarity you add in `[rarities]` in `settings.toml`), and `flavor_text` is optional.

The pack still works as a datapack in `world/datapacks/` too: on the first start with Title Scrolls 1.0.3 its titles are copied into `config/titlescrolls/titles/` (or run `/titlescroll import datapacks`).
