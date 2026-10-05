# Title Scrolls — Starter Title Pack

34 example titles across all five rarity tiers (common, rare, epic, legendary, unique), each with flavor text. Use this to get titles working on day one, or as a reference for writing your own.

## Installation

1. Put `titlescrolls-starter-pack-1.0.0.zip` in your world's `datapacks` folder (`world/datapacks/`). No need to unzip it.
2. If the server is already running, run `/reload`. Otherwise just start the server.
3. The server log should show `Loaded 34 title definition(s)`.
4. Test with `/give @p titlescrolls:title_scroll[titlescrolls:grants_title="rookie"]`, then right-click the scroll to unlock it.

## Editing

Every file under `data/starter/titles/` is a plain JSON file:

```json
{
  "display": "&7Rookie",
  "rarity": "common",
  "flavor_text": "Everyone starts somewhere."
}
```

Edit, delete, or add new title files freely. Run `/reload` to pick up changes. `display` supports `&`-color codes, `rarity` sets the slot border color in the `/title` menu (common, rare, epic, legendary or unique), and `flavor_text` is optional.