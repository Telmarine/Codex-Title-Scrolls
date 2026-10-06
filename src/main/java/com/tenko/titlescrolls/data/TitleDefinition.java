package com.tenko.titlescrolls.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * One title, as defined by a server owner's own datapack JSON file at
 * data/<namespace>/titles/<id>.json. The mod ships with ZERO of these baked
 * in — every field here is content, not code. See docs/example_title.json.txt
 * for a fully-commented example of the file format.
 *
 * Fields:
 *   display     - the exact text shown in chat/GUI, color codes allowed with
 *                 "&" (e.g. "&6Kitsune"). Required.
 *   rarity      - freeform label ("common"/"uncommon"/"rare"/whatever a
 *                 server wants) - purely cosmetic/informational right now,
 *                 shown once a title is unlocked. Defaults to "common".
 *   flavor_text - a short line shown under the title once unlocked. Optional,
 *                 defaults to blank.
 *   chat_color  - color of the player's chat messages while this title is worn,
 *                 e.g. "&b" or "&#FFD700". Optional, defaults to blank (no change).
 *                 Read by chat mods (Elements: Nexus reads it as chatColor()).
 *                 Server side only: never sent to the /title menu.
 */
public record TitleDefinition(String display, String rarity, String flavorText, String chatColor) {

    public static final Codec<TitleDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("display").forGetter(TitleDefinition::display),
            Codec.STRING.optionalFieldOf("rarity", "common").forGetter(TitleDefinition::rarity),
            Codec.STRING.optionalFieldOf("flavor_text", "").forGetter(TitleDefinition::flavorText),
            Codec.STRING.optionalFieldOf("chat_color", "").forGetter(TitleDefinition::chatColor)
    ).apply(instance, TitleDefinition::new));
}
