package com.tac5studios.codextitlescrolls.compat;

import com.tac5studios.codextitlescrolls.TitleScrolls;
import net.neoforged.fml.ModList;
import net.neoforged.neoforgespi.language.IModInfo;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Finds installed mods that show titles on players, without a list of known mod ids.
 *
 * <p>A mod counts when both are true:
 * <ul>
 *   <li>its id, name or description is about titles ("title", "epithet", "honorific"),
 *       not counting "title screen" mods, and</li>
 *   <li>it hooks name tag rendering (a NeoForge RenderNameTagEvent listener, or a mixin into the
 *       player/entity renderers), or its description talks about name tags or text over players' heads.</li>
 * </ul>
 * Mods such as on-screen biome titles don't hook name tags, so they don't count.
 */
public final class TitleModDetector {

    private static final Pattern TITLE_SCREEN = Pattern.compile("title[ _-]?screens?|main[ _-]?menu");
    private static final Pattern TITLE = Pattern.compile("\\b(titles?|epithets?|honorifics?)\\b|titlesystem|playertitle");
    private static final Pattern OVERHEAD = Pattern.compile(
            "name[ _-]?tags?|name[ _-]?plates?|above (the )?(player'?s'? |players'? |their |your )?heads?|"
                    + "over(head| (the |their |your )?heads?)|below (the |their )?names?|next to (the |their )?names?");

    private static final Set<String> ALWAYS_SKIP = Set.of(TitleScrolls.MOD_ID, "minecraft", "neoforge");

    private TitleModDetector() {}

    /** Mod id to a short reason, for every mod that shows titles on players. */
    public static Map<String, String> find(List<? extends String> ignore) {
        Map<String, String> found = new LinkedHashMap<>();
        Set<String> hooks = modsHookingNameTags();
        for (IModInfo mod : ModList.get().getMods()) {
            String id = mod.getModId();
            if (ALWAYS_SKIP.contains(id) || ignore.contains(id)) continue;
            String text = (id + " " + mod.getDisplayName() + " " + mod.getDescription()).toLowerCase(Locale.ROOT);
            text = TITLE_SCREEN.matcher(text).replaceAll(" ");
            if (!TITLE.matcher(text).find()) continue;
            if (hooks.contains(id)) {
                found.put(id, mod.getDisplayName() + " (titles, draws name tags)");
            } else if (OVERHEAD.matcher(text).find()) {
                found.put(id, mod.getDisplayName() + " (titles shown on players)");
            }
        }
        return found;
    }

    /**
     * Mods whose classes listen to RenderNameTagEvent or mix into the player/entity renderers, read from
     * the annotation data NeoForge collects at start. Read by reflection, so a change in that data only
     * turns this part off (the description check still runs).
     */
    private static Set<String> modsHookingNameTags() {
        Set<String> out = new HashSet<>();
        try {
            Object all = ModList.get().getClass().getMethod("getAllScanData").invoke(ModList.get());
            for (Object scan : (List<?>) all) {
                if (!hooksNameTags(scan)) continue;
                for (Object fileInfo : (List<?>) call(scan, "getIModInfoData")) {
                    for (Object mod : (List<?>) call(fileInfo, "getMods")) out.add(String.valueOf(call(mod, "getModId")));
                }
            }
        } catch (Throwable t) {
            TitleScrolls.LOGGER.debug("[TitleScrolls] Name tag hook scan skipped: {}", t.toString());
        }
        return out;
    }

    private static boolean hooksNameTags(Object scan) throws ReflectiveOperationException {
        for (Object a : (Iterable<?>) call(scan, "getAnnotations")) {
            String type = String.valueOf(call(a, "annotationType"));
            if (type.contains("SubscribeEvent")) {
                if (String.valueOf(call(a, "memberName")).contains("RenderNameTagEvent")) return true;
            } else if (type.contains("spongepowered/asm/mixin/Mixin")) {
                String data = String.valueOf(call(a, "annotationData"));
                if (data.contains("PlayerRenderer") || data.contains("/EntityRenderer") || data.contains("LivingEntityRenderer")) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Calls a no-argument method by name, or its getX() form. */
    private static Object call(Object target, String name) throws ReflectiveOperationException {
        Class<?> c = target.getClass();
        Method m;
        try {
            m = c.getMethod(name);
        } catch (NoSuchMethodException e) {
            m = c.getMethod("get" + Character.toUpperCase(name.charAt(0)) + name.substring(1));
        }
        m.setAccessible(true);
        return m.invoke(target);
    }
}
