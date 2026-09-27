# Title Scrolls — Starter Title Pack

34 example titles across all five rarity tiers (common, rare, epic, legendary, unique), each with flavor text. Use this to get titles working on day one, or as a reference for writing your own.

## Installation

1. Copy this whole `starter-pack` folder into your world's `datapacks` folder — the result should look like `world/datapacks/starter-pack/pack.mcmeta` and `world/datapacks/starter-pack/data/...`.
2. If the server is already running, type `/reload` in-game (as an op) or in console. Otherwise just start the server normally.
3. Check the server log for a line like `Loaded 34 title definition(s)` to confirm it picked them up.
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

Edit, delete, or add new title files freely — `/reload` picks up changes instantly, no recompiling needed. `display` supports `&`-color codes, `rarity` is just a label your own loot tables can key off of, and `flavor_text` is optional.