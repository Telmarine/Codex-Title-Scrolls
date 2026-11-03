package com.tac5studios.codextitlescrolls.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.CommandNode;
import com.tac5studios.codextitlescrolls.TitleScrolls;
import com.tac5studios.codextitlescrolls.config.Settings;
import com.tac5studios.codextitlescrolls.data.PlayerTitleData;
import com.tac5studios.codextitlescrolls.data.TitleDefinition;
import com.tac5studios.codextitlescrolls.server.TitleSync;
import com.tac5studios.codextitlescrolls.util.ColorCodes;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.SharedSuggestionProvider;
import com.tac5studios.codextitlescrolls.network.OpenTitleScreenPayload;
import com.tac5studios.codextitlescrolls.perms.Perm;
import com.tac5studios.codextitlescrolls.registry.ModAttachments;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * /title opens the title menu for every player.
 * /title wear <title> puts on a title the player owns, /title off takes it off.
 *
 * <p>Needs titlescrolls.menu (everyone by default). Only registered when title_menu is on in features.toml.
 *
 * <p>Vanilla already has a /title command (on-screen text, OP level 2). Brigadier would merge
 * ours into it and keep the vanilla permission, so players could never open the menu.
 * Instead the root is rebuilt: /title alone is open to everyone, and the vanilla
 * sub-commands (/title <targets> ...) keep their OP level 2 requirement.
 */
public class TitleCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        CommandNode<CommandSourceStack> vanilla = dispatcher.getRoot().getChild("title");

        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("title")
                .requires(s -> s.hasPermission(2) || Perm.has(s, Perm.MENU))
                .executes(ctx -> {
                    if (!(ctx.getSource().getEntity() instanceof ServerPlayer player)) {
                        ctx.getSource().sendFailure(Component.literal("Only players can use /title."));
                        return 0;
                    }
                    openFor(player);
                    return 1;
                });

        root.then(Commands.literal("wear")
                .then(Commands.argument("title", StringArgumentType.string())
                        .suggests((c, b) -> c.getSource().getPlayer() == null ? b.buildFuture()
                                : SharedSuggestionProvider.suggest(owned(c.getSource().getPlayer()).stream()
                                        .map(id -> id.contains(":") ? "\"" + id + "\"" : id), b))
                        .executes(c -> wear(c.getSource(), StringArgumentType.getString(c, "title")))));
        root.then(Commands.literal("off").executes(c -> off(c.getSource())));

        if (vanilla != null) {
            for (CommandNode<CommandSourceStack> child : vanilla.getChildren()) {
                var copy = child.createBuilder(); // copies the command and requirement, not the children
                if (child.getRedirect() == null) {
                    for (CommandNode<CommandSourceStack> grandchild : child.getChildren()) copy.then(grandchild);
                }
                copy.requires(s -> s.hasPermission(2) && child.getRequirement().test(s));
                root.then(copy);
            }
            removeTitleRoot(dispatcher);
        }
        dispatcher.register(root);
    }

    /** Remove the vanilla /title root so it can be registered again with a different requirement. */
    private static void removeTitleRoot(CommandDispatcher<CommandSourceStack> dispatcher) {
        for (String field : new String[]{"children", "literals", "arguments"}) {
            try {
                Field f = CommandNode.class.getDeclaredField(field);
                f.setAccessible(true);
                ((Map<?, ?>) f.get(dispatcher.getRoot())).remove("title");
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
                data.activeTitle().orElse(""),
                new ArrayList<>(Settings.RARITIES.get())
        ));
    }

    /** Titles the player owns that still exist. */
    private static List<String> owned(ServerPlayer player) {
        return player.getData(ModAttachments.PLAYER_TITLES.get()).unlockedTitles().stream()
                .filter(id -> TitleScrolls.TITLE_REGISTRY.get(id).isPresent()).toList();
    }

    private static int wear(CommandSourceStack source, String id) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("Only players can wear a title."));
            return 0;
        }
        PlayerTitleData data = player.getData(ModAttachments.PLAYER_TITLES.get());
        Optional<TitleDefinition> def = TitleScrolls.TITLE_REGISTRY.get(id);
        if (def.isEmpty() || !data.has(id)) {
            source.sendFailure(Component.literal("You don't have a title called " + id + ".").withStyle(ChatFormatting.RED));
            return 0;
        }
        player.setData(ModAttachments.PLAYER_TITLES.get(), data.withActive(id));
        TitleSync.titleChanged(player);
        source.sendSuccess(() -> Component.literal("Now wearing ").withStyle(ChatFormatting.GRAY)
                .append(ColorCodes.translate(def.get().display())), false);
        return 1;
    }

    private static int off(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("Only players can take off a title."));
            return 0;
        }
        PlayerTitleData data = player.getData(ModAttachments.PLAYER_TITLES.get());
        if (data.activeTitle().isEmpty()) {
            source.sendFailure(Component.literal("You aren't wearing a title.").withStyle(ChatFormatting.RED));
            return 0;
        }
        player.setData(ModAttachments.PLAYER_TITLES.get(), data.withActive(null));
        TitleSync.titleChanged(player);
        source.sendSuccess(() -> Component.literal("Title taken off.").withStyle(ChatFormatting.GRAY), false);
        return 1;
    }

    private TitleCommand() {}
}
