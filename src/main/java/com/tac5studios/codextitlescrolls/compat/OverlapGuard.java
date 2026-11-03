package com.tac5studios.codextitlescrolls.compat;

import com.tac5studios.codextitlescrolls.TitleScrolls;
import com.tac5studios.codextitlescrolls.config.Feature;
import com.tac5studios.codextitlescrolls.config.Features;
import net.neoforged.fml.ModList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Turns a feature off when an installed mod already does the same job. Checked once at start
 * (features.toml changes need a restart). Mods come from [overlap_guard] mods and from auto-detection.
 */
public final class OverlapGuard {

    private static volatile Map<Feature, List<String>> blocked = Collections.emptyMap();

    private OverlapGuard() {}

    public static boolean blocks(Feature f) {
        return blocked.containsKey(f);
    }

    public static synchronized void check() {
        Map<Feature, List<String>> found = new EnumMap<>(Feature.class);
        try {
            if (!Features.GUARD_ENABLED.get()) {
                blocked = Collections.emptyMap();
                return;
            }
            for (String line : Features.GUARD_MODS.get()) {
                int eq = line.indexOf('=');
                Feature f = eq > 0 ? Feature.byKey(line.substring(0, eq).trim()) : null;
                String modId = eq > 0 ? line.substring(eq + 1).trim() : "";
                if (f == null || modId.isEmpty()) {
                    TitleScrolls.LOGGER.warn("[TitleScrolls] features.toml: can't read overlap_guard mod \"{}\" (use \"feature=modid\").", line);
                    continue;
                }
                if (ModList.get().isLoaded(modId)) {
                    found.computeIfAbsent(f, k -> new ArrayList<>()).add(name(modId));
                }
            }
            if (Features.AUTO_DETECT.get()) {
                TitleModDetector.find(Features.IGNORE_MODS.get()).forEach((id, reason) -> {
                    List<String> list = found.computeIfAbsent(Feature.NAMETAG_TITLES, k -> new ArrayList<>());
                    if (!list.contains(name(id))) list.add(name(id));
                    TitleScrolls.LOGGER.info("[TitleScrolls] Found {} [{}].", reason, id);
                });
            }
            for (String key : Features.FORCE_ON.get()) {
                Feature f = Feature.byKey(key.trim());
                if (f != null) found.remove(f);
            }
        } catch (IllegalStateException notLoaded) {
            TitleScrolls.LOGGER.warn("[TitleScrolls] features.toml wasn't loaded yet; overlap guard skipped.");
        }
        blocked = Collections.unmodifiableMap(found);
        found.forEach((f, mods) -> TitleScrolls.LOGGER.info(
                "[TitleScrolls] {} found: {} is turned off. Add \"{}\" to overlap_guard.force_on to keep it.",
                String.join(", ", mods), f.key, f.key));
    }

    private static String name(String modId) {
        return ModList.get().getModContainerById(modId).map(c -> c.getModInfo().getDisplayName()).orElse(modId);
    }
}
