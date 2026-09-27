package com.tenko.titlescrolls.command;

import com.mojang.brigadier.CommandDispatcher;
import com.tenko.titlescrolls.TitleScrolls;
import com.tenko.titlescrolls.data.PlayerTitleData;
import com.tenko.titlescrolls.network.OpenTitleScreenPayload;
import com.tenko.titlescrolls.registry.ModAttachments;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/**
 * /title — opens the GUI. No arguments, no sub-command required to pick a
 * title (that's what the user explicitly didn't want — see the mod plan).
 */
public class TitleCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("title")
                .executes(ctx -> {
                    if (!(ctx.getSource().getEntity() instanceof ServerPlayer player)) {
                        ctx.getSource().sendFailure(Component.literal("Only players can use /title."));
                        return 0;
                    }
                    openFor(player);
                    return 1;
                }));
    }

    private static void openFor(ServerPlayer player) {
        PlayerTitleData data = player.getData(ModAttachments.PLAYER_TITLES.get());

        List<OpenTitleScreenPayload.TitleEntry> entries = new ArrayList<>();
        TitleScrolls.TITLE_REGISTRY.getAll().forEach((id, def) ->
                entries.add(new OpenTitleScreenPayload.TitleEntry(id, def.display(), def.rarity(), def.flavorText())));

        PacketDistributor.sendToPlayer(player, new OpenTitleScreenPayload(
                entries,
                new ArrayList<>(data.unlockedTitles()),
                data.activeTitle().orElse("")
        ));
    }

    private TitleCommand() {}
}
