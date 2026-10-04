package com.tenko.titlescrolls.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.CommandNode;
import com.tenko.titlescrolls.TitleScrolls;
import com.tenko.titlescrolls.data.PlayerTitleData;
import com.tenko.titlescrolls.network.OpenTitleScreenPayload;
import com.tenko.titlescrolls.registry.ModAttachments;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * /title opens the title menu for every player.
 *
 * Vanilla already has a /title command (on-screen text, OP level 2). Brigadier would merge
 * ours into it and keep the vanilla permission, so players could never open the menu.
 * Instead the root is rebuilt: /title alone is open to everyone, and the vanilla
 * sub-commands (/title <targets> ...) keep their OP level 2 requirement.
 */
public class TitleCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        CommandNode<CommandSourceStack> vanilla = dispatcher.getRoot().getChild("title");

        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("title")
                .executes(ctx -> {
                    if (!(ctx.getSource().getEntity() instanceof ServerPlayer player)) {
                        ctx.getSource().sendFailure(Component.literal("Only players can use /title."));
                        return 0;
                    }
                    openFor(player);
                    return 1;
                });

        if (vanilla != null) {
            for (CommandNode<CommandSourceStack> child : vanilla.getChildren()) {
                var copy = child.createBuilder(); // copies the command and requirement, not the children
                if (child.getRedirect() == null) {
                    for (CommandNode<CommandSourceStack> grandchild : child.getChildren()) copy.then(grandchild);
                }
                copy.requires(s -> s.hasPermission(2) && child.getRequirement().test(s));
                root.then(copy);
            }
            removeRoot(dispatcher, "title");
        }
        dispatcher.register(root);
    }

    /** Remove a root command so it can be registered again with a different requirement. */
    private static void removeRoot(CommandDispatcher<CommandSourceStack> dispatcher, String name) {
        for (String field : new String[]{"children", "literals", "arguments"}) {
            try {
                Field f = CommandNode.class.getDeclaredField(field);
                f.setAccessible(true);
                ((Map<?, ?>) f.get(dispatcher.getRoot())).remove(name);
            } catch (ReflectiveOperationException ignored) {
            }
        }
    }

    private static void openFor(ServerPlayer player) {
        PlayerTitleData data = player.getData(ModAttachments.PLAYER_TITLES.get());

        // Locked titles are sent with no name, rarity or flavor text, so a modified client
        // can't read them. The menu only needs their id to draw a "?" slot.
        List<OpenTitleScreenPayload.TitleEntry> entries = new ArrayList<>();
        TitleScrolls.TITLE_REGISTRY.getAll().forEach((id, def) -> entries.add(data.has(id)
                ? new OpenTitleScreenPayload.TitleEntry(id, def.display(), def.rarity(), def.flavorText())
                : new OpenTitleScreenPayload.TitleEntry(id, "", "", "")));

        PacketDistributor.sendToPlayer(player, new OpenTitleScreenPayload(
                entries,
                new ArrayList<>(data.unlockedTitles()),
                data.activeTitle().orElse("")
        ));
    }

    private TitleCommand() {}
}
