package com.tenko.titlescrolls.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.tenko.titlescrolls.TitleScrolls;
import com.tenko.titlescrolls.data.TitleDefinition;
import com.tenko.titlescrolls.registry.ModDataComponents;
import com.tenko.titlescrolls.registry.ModItems;
import com.tenko.titlescrolls.util.ColorCodes;
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
 * /titlescroll give <players> <title> [amount]  (OP level 2)
 * A short way to hand out scrolls instead of
 * /give @p titlescrolls:title_scroll[titlescrolls:grants_title="kitsune"].
 * Title ids tab-complete. Scrolls that don't fit in the inventory are dropped at the player's feet.
 */
public final class GiveCommand {

    private GiveCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("titlescroll")
                .requires(s -> s.hasPermission(2))
                .then(Commands.literal("give")
                        .then(Commands.argument("players", EntityArgument.players())
                                .then(Commands.argument("title", StringArgumentType.string())
                                        .suggests((c, b) -> SharedSuggestionProvider.suggest(
                                                TitleScrolls.TITLE_REGISTRY.getAll().keySet().stream()
                                                        .map(id -> id.contains(":") ? "\"" + id + "\"" : id), b))
                                        .executes(c -> give(c, 1))
                                        .then(Commands.argument("amount", IntegerArgumentType.integer(1, 64))
                                                .executes(c -> give(c, IntegerArgumentType.getInteger(c, "amount"))))))));
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
                scroll.set(net.minecraft.core.component.DataComponents.LORE, com.tenko.titlescrolls.item.TitleScrollItem.lore(id));
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
