package com.tac5studios.codextitlescrolls.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.tac5studios.codextitlescrolls.TitleScrolls;
import com.tac5studios.codextitlescrolls.config.Feature;
import com.tac5studios.codextitlescrolls.config.Features;
import com.tac5studios.codextitlescrolls.config.Settings;
import com.tac5studios.codextitlescrolls.perms.Perm;
import com.tac5studios.codextitlescrolls.data.DatapackImport;
import com.tac5studios.codextitlescrolls.data.TitleDefinition;
import com.tac5studios.codextitlescrolls.migrate.Migration;
import com.tac5studios.codextitlescrolls.registry.ModDataComponents;
import com.tac5studios.codextitlescrolls.registry.ModItems;
import com.tac5studios.codextitlescrolls.util.ColorCodes;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.Collection;
import java.util.Optional;

/**
 * /titlescroll reload  (titlescrolls.reload, OP level 3): re-reads settings.toml and the title files.
 * /titlescroll import datapacks  (titlescrolls.import, OP level 3): copies datapack titles into the config folder again.
 * /titlescroll migrate scan|apply  (titlescrolls.import, OP level 3): moves titles over from another title mod.
 * /titlescroll give <players> <title> [amount]  (titlescrolls.give, OP level 2)
 * A short way to hand out scrolls instead of
 * /give @p titlescrolls:title_scroll[titlescrolls:grants_title="kitsune"].
 * Title ids tab-complete. Scrolls that don't fit in the inventory are dropped at the player's feet.
 */
public final class GiveCommand {

    private GiveCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("titlescroll")
                .requires(s -> Perm.has(s, Perm.RELOAD) || Perm.has(s, Perm.IMPORT)
                        || (Features.on(Feature.GIVE_COMMAND) && Perm.has(s, Perm.GIVE)))
                .then(Commands.literal("reload")
                        .requires(s -> Perm.has(s, Perm.RELOAD))
                        .executes(GiveCommand::reload))
                .then(Commands.literal("import")
                        .requires(s -> Perm.has(s, Perm.IMPORT))
                        .then(Commands.literal("datapacks").executes(GiveCommand::importDatapacks)))
                .then(Commands.literal("migrate")
                        .requires(s -> Perm.has(s, Perm.IMPORT))
                        .then(Commands.literal("scan").executes(GiveCommand::migrateScan))
                        .then(Commands.literal("apply").executes(GiveCommand::migrateApply)))
                .then(Commands.literal("give")
                        .requires(s -> Features.on(Feature.GIVE_COMMAND) && Perm.has(s, Perm.GIVE))
                        .then(Commands.argument("players", EntityArgument.players())
                                .then(Commands.argument("title", StringArgumentType.string())
                                        .suggests((c, b) -> SharedSuggestionProvider.suggest(
                                                TitleScrolls.TITLE_REGISTRY.getAll().keySet().stream()
                                                        .map(id -> id.contains(":") ? "\"" + id + "\"" : id), b))
                                        .executes(c -> give(c, 1))
                                        .then(Commands.argument("amount", IntegerArgumentType.integer(1, 64))
                                                .executes(c -> give(c, IntegerArgumentType.getInteger(c, "amount"))))))));
    }

    /** Re-reads settings.toml and the title files, then sends titles on players to clients again. */
    private static int reload(CommandContext<CommandSourceStack> c) {
        Settings.reload();
        TitleScrolls.TITLE_REGISTRY.load(c.getSource().getServer());
        int titles = TitleScrolls.TITLE_REGISTRY.getAll().size();
        c.getSource().sendSuccess(() -> Component.literal("Title Scrolls reloaded: settings.toml and " + titles + " title(s).")
                .withStyle(ChatFormatting.GREEN), true);
        return titles;
    }

    private static int importDatapacks(CommandContext<CommandSourceStack> c) {
        DatapackImport.Result r = DatapackImport.run(c.getSource().getServer());
        TitleScrolls.TITLE_REGISTRY.load(c.getSource().getServer());
        c.getSource().sendSuccess(() -> Component.literal("Datapack titles: " + r.imported() + " imported, " + r.skipped()
                + " already in config/titlescrolls/titles, " + r.failed() + " failed (see the log).").withStyle(ChatFormatting.GREEN), true);
        return r.imported();
    }

    private static int migrateScan(CommandContext<CommandSourceStack> c) {
        try {
            Migration.ScanResult r = Migration.scan(c.getSource().getServer());
            c.getSource().sendSuccess(() -> Component.literal("Migration scan read " + r.files() + " file(s): " + r.titles()
                    + " title(s) and " + r.players() + " player(s) added to migration/migrate.json. "
                    + "Check it and scan-report.txt, then run /titlescroll migrate apply.").withStyle(ChatFormatting.GREEN), true);
            return r.titles();
        } catch (Exception e) {
            c.getSource().sendFailure(Component.literal("Migration scan failed: " + e.getMessage()).withStyle(ChatFormatting.RED));
            return 0;
        }
    }

    private static int migrateApply(CommandContext<CommandSourceStack> c) {
        try {
            Migration.ApplyResult r = Migration.apply(c.getSource().getServer());
            c.getSource().sendSuccess(() -> Component.literal("Migration applied: " + r.titlesWritten() + " title(s) written ("
                    + r.titlesSkipped() + " already there), " + r.playersNow() + " online player(s) given their titles, "
                    + r.playersLater() + " get theirs at next login.").withStyle(ChatFormatting.GREEN), true);
            if (!r.problems().isEmpty()) {
                c.getSource().sendFailure(Component.literal(r.problems().size() + " problem(s), listed in migration/apply-problems.txt.")
                        .withStyle(ChatFormatting.YELLOW));
            }
            return r.titlesWritten();
        } catch (Exception e) {
            c.getSource().sendFailure(Component.literal("Migration failed: " + e.getMessage()).withStyle(ChatFormatting.RED));
            return 0;
        }
    }

    private static int give(CommandContext<CommandSourceStack> c, int amount) throws CommandSyntaxException {
        Collection<ServerPlayer> players = EntityArgument.getPlayers(c, "players");
        String id = StringArgumentType.getString(c, "title");
        Optional<TitleDefinition> def = TitleScrolls.TITLE_REGISTRY.get(id);
        if (def.isEmpty()) {
            c.getSource().sendFailure(Component.literal("No title called " + id + ".").withStyle(ChatFormatting.RED));
            return 0;
        }
        for (ServerPlayer p : players) {
            // Scrolls don't stack, so each one is its own stack.
            for (int i = 0; i < amount; i++) {
                ItemStack scroll = new ItemStack(ModItems.TITLE_SCROLL.get());
                scroll.set(ModDataComponents.GRANTS_TITLE.get(), id);
                scroll.set(net.minecraft.core.component.DataComponents.LORE, com.tac5studios.codextitlescrolls.item.TitleScrollItem.lore(id));
                if (!p.getInventory().add(scroll)) p.drop(scroll, false);
            }
            p.containerMenu.broadcastChanges(); // send the new items to the player's screen (same as vanilla /give)
        }
        Component title = ColorCodes.translate(def.get().display());
        String who = players.size() == 1 ? players.iterator().next().getGameProfile().getName() : players.size() + " players";
        c.getSource().sendSuccess(() -> Component.literal("Gave " + amount + " ")
                .append(title).append(Component.literal(" scroll" + (amount == 1 ? "" : "s") + " to " + who + ".")), true);
        return players.size();
    }
}
