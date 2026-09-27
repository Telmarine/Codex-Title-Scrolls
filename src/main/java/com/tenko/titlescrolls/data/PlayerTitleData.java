package com.tenko.titlescrolls.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Immutable snapshot of one player's title progress. Attachments are get/set,
 * not mutated in place — every unlock or selection replaces the stored value
 * with a new instance via withUnlocked()/withActive().
 */
public record PlayerTitleData(List<String> unlockedTitles, Optional<String> activeTitle) {

    public static final Codec<PlayerTitleData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.listOf().fieldOf("unlocked").forGetter(PlayerTitleData::unlockedTitles),
            Codec.STRING.optionalFieldOf("active").forGetter(PlayerTitleData::activeTitle)
    ).apply(instance, PlayerTitleData::new));

    public static PlayerTitleData empty() {
        return new PlayerTitleData(new ArrayList<>(), Optional.empty());
    }

    public boolean has(String titleId) {
        return unlockedTitles.contains(titleId);
    }

    public PlayerTitleData withUnlocked(String titleId) {
        if (unlockedTitles.contains(titleId)) {
            return this;
        }
        List<String> copy = new ArrayList<>(unlockedTitles);
        copy.add(titleId);
        return new PlayerTitleData(copy, activeTitle);
    }

    public PlayerTitleData withActive(String titleId) {
        return new PlayerTitleData(unlockedTitles, Optional.ofNullable(titleId));
    }
}
