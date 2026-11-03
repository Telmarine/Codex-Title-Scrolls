package com.tac5studios.codextitlescrolls.registry;

import com.mojang.serialization.MapCodec;
import com.tac5studios.codextitlescrolls.TitleScrolls;
import com.tac5studios.codextitlescrolls.loot.ScrollLootModifier;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/** The loot modifier that adds Title Scrolls to loot (data/titlescrolls/loot_modifiers/title_scrolls.json). */
public final class ModLoot {
    public static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> REGISTER =
            DeferredRegister.create(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, TitleScrolls.MOD_ID);

    public static final DeferredHolder<MapCodec<? extends IGlobalLootModifier>, MapCodec<ScrollLootModifier>> TITLE_SCROLLS =
            REGISTER.register("title_scrolls", () -> ScrollLootModifier.CODEC);

    private ModLoot() {}
}
