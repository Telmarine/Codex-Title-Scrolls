package com.tac5studios.codextitlescrolls.perms;

import com.tac5studios.codextitlescrolls.TitleScrolls;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.server.permission.PermissionAPI;
import net.neoforged.neoforge.server.permission.events.PermissionGatherEvent;
import net.neoforged.neoforge.server.permission.nodes.PermissionNode;
import net.neoforged.neoforge.server.permission.nodes.PermissionTypes;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Permission nodes "titlescrolls.<path>", registered at start through the NeoForge permission API,
 * so they work with any permission mod or plain OP levels.
 */
@EventBusSubscriber(modid = TitleScrolls.MOD_ID)
public final class Perm {

    private record Entry(PermissionNode<Boolean> node, int opLevel) {}

    private static final Map<String, Entry> NODES = new LinkedHashMap<>();

    /** /title menu */
    public static final String MENU = node("menu", 0);
    /** /titlescroll give */
    public static final String GIVE = node("give", 2);
    /** /titlescroll reload */
    public static final String RELOAD = node("reload", 3);
    /** /titlescroll import datapacks, /titlescroll migrate */
    public static final String IMPORT = node("import", 3);

    private Perm() {}

    private static String node(String path, int opLevel) {
        PermissionNode<Boolean> n = new PermissionNode<>(TitleScrolls.MOD_ID, path, PermissionTypes.BOOLEAN,
                (player, uuid, ctx) -> opLevel == 0 || (player != null && player.hasPermissions(opLevel)));
        n.setInformation(Component.literal("titlescrolls." + path),
                Component.literal("Default: " + (opLevel == 0 ? "everyone" : "OP level " + opLevel + "+")));
        NODES.put(n.getNodeName(), new Entry(n, opLevel));
        return n.getNodeName();
    }

    @SubscribeEvent
    @SuppressWarnings("unused") // called by the event bus
    public static void onGatherNodes(PermissionGatherEvent.Nodes event) {
        event.addNodes(NODES.values().stream().map(Entry::node).toArray(PermissionNode<?>[]::new));
        TitleScrolls.LOGGER.info("[TitleScrolls] {} permission nodes registered.", NODES.size());
    }

    public static boolean has(CommandSourceStack source, String node) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            Entry e = NODES.get(node);
            return source.hasPermission(Math.max(e == null ? 3 : e.opLevel(), 2));
        }
        return has(player, node);
    }

    public static boolean has(ServerPlayer player, String node) {
        Entry e = NODES.get(node);
        if (e == null) return false;
        try {
            return PermissionAPI.getPermission(player, e.node());
        } catch (RuntimeException ex) {
            return e.opLevel() == 0 || player.hasPermissions(e.opLevel());
        }
    }
}
