package com.tac5studios.codextitlescrolls.loot;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.tac5studios.codextitlescrolls.TitleScrolls;
import com.tac5studios.codextitlescrolls.config.Feature;
import com.tac5studios.codextitlescrolls.config.Features;
import com.tac5studios.codextitlescrolls.config.Settings;
import com.tac5studios.codextitlescrolls.data.TitleDefinition;
import com.tac5studios.codextitlescrolls.item.TitleScrollItem;
import com.tac5studios.codextitlescrolls.registry.ModDataComponents;
import com.tac5studios.codextitlescrolls.registry.ModItems;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Adds one Title Scroll to matching loot ([loot] in settings.toml), when scroll_loot is on in features.toml.
 * Settings are read each time loot is rolled, so /titlescroll reload changes them right away.
 */
public class ScrollLootModifier extends LootModifier {

    public static final MapCodec<ScrollLootModifier> CODEC =
            RecordCodecBuilder.mapCodec(inst -> codecStart(inst).apply(inst, ScrollLootModifier::new));

    public ScrollLootModifier(LootItemCondition[] conditions) {
        super(conditions);
    }

    @Override
    @Nonnull
    protected ObjectArrayList<ItemStack> doApply(@Nonnull ObjectArrayList<ItemStack> loot, @Nonnull LootContext context) {
        if (!Features.on(Feature.SCROLL_LOOT) || !Settings.SPEC.isLoaded()) return loot;
        ResourceLocation table = context.getQueriedLootTableId();
        if (!matches(table.toString(), Settings.LOOT_TABLES.get())
                || matches(table.toString(), Settings.LOOT_EXCLUDE_TABLES.get())) return loot;
        RandomSource random = context.getRandom();
        if (random.nextDouble() >= Settings.LOOT_CHANCE.get()) return loot;
        String id = pick(random);
        if (id == null) return loot;
        ItemStack scroll = new ItemStack(ModItems.TITLE_SCROLL.get());
        scroll.set(ModDataComponents.GRANTS_TITLE.get(), id);
        scroll.set(DataComponents.LORE, TitleScrollItem.lore(id));
        loot.add(scroll);
        return loot;
    }

    /** A title picked by rarity weight, or null when no title can drop. */
    private static String pick(RandomSource random) {
        Map<String, Integer> weights = new HashMap<>();
        for (String entry : Settings.RARITY_WEIGHTS.get()) {
            int eq = entry.indexOf('=');
            weights.put(entry.substring(0, eq).trim().toLowerCase(Locale.ROOT), Integer.parseInt(entry.substring(eq + 1).trim()));
        }
        List<? extends String> excluded = Settings.LOOT_EXCLUDE_TITLES.get();
        long total = 0;
        for (Map.Entry<String, TitleDefinition> t : TitleScrolls.TITLE_REGISTRY.getAll().entrySet()) {
            if (!excluded.contains(t.getKey())) total += weightOf(t.getValue(), weights);
        }
        if (total <= 0) return null;
        long roll = (long) (random.nextDouble() * total);
        for (Map.Entry<String, TitleDefinition> t : TitleScrolls.TITLE_REGISTRY.getAll().entrySet()) {
            if (excluded.contains(t.getKey())) continue;
            roll -= weightOf(t.getValue(), weights);
            if (roll < 0) return t.getKey();
        }
        return null;
    }

    private static int weightOf(TitleDefinition def, Map<String, Integer> weights) {
        return Math.max(0, weights.getOrDefault(def.rarity().toLowerCase(Locale.ROOT), 0));
    }

    private static boolean matches(String table, List<? extends String> patterns) {
        for (String p : patterns) {
            String regex = Pattern.quote(p.trim()).replace("*", "\\E.*\\Q");
            if (table.matches(regex)) return true;
        }
        return false;
    }

    @Override
    @Nonnull
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}
