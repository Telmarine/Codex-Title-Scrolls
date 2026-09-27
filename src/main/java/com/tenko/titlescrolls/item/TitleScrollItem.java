package com.tenko.titlescrolls.item;

import com.tenko.titlescrolls.TitleScrolls;
import com.tenko.titlescrolls.data.PlayerTitleData;
import com.tenko.titlescrolls.data.TitleDefinition;
import com.tenko.titlescrolls.registry.ModAttachments;
import com.tenko.titlescrolls.registry.ModDataComponents;
import net.minecraft.ChatFormatting;
import com.tenko.titlescrolls.util.ColorCodes;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.Optional;

/**
 * One reusable item for every title in the game. Which specific title a given
 * stack unlocks is decided entirely by the GRANTS_TITLE data component set on
 * that stack (see a loot table entry, or /give ... [titlescrolls:grants_title="kitsune"]).
 *
 * Behavior (settled with the user 27 Sep 2026):
 *  - New unlock -> added to the player's collection, scroll is consumed,
 *    feedback goes to CHAT (not actionbar).
 *  - Duplicate of a title already owned -> scroll is NOT consumed, so it can
 *    be traded/handed to someone who doesn't have it yet.
 */
public class TitleScrollItem extends Item {

    public TitleScrollItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide) {
            return InteractionResultHolder.success(stack);
        }

        String titleId = stack.getOrDefault(ModDataComponents.GRANTS_TITLE.get(), "");
        if (titleId.isEmpty()) {
            player.displayClientMessage(
                    Component.literal("This scroll doesn't have a title assigned — tell an admin.")
                            .withStyle(ChatFormatting.RED),
                    false);
            return InteractionResultHolder.fail(stack);
        }

        Optional<TitleDefinition> definition = TitleScrolls.TITLE_REGISTRY.get(titleId);
        if (definition.isEmpty()) {
            player.displayClientMessage(
                    Component.literal("This scroll references an unknown title id: " + titleId)
                            .withStyle(ChatFormatting.RED),
                    false);
            return InteractionResultHolder.fail(stack);
        }

        PlayerTitleData data = player.getData(ModAttachments.PLAYER_TITLES.get());

        if (data.has(titleId)) {
            player.displayClientMessage(
                    Component.literal("You already have this title — this scroll can be given to someone else.")
                            .withStyle(ChatFormatting.GRAY),
                    false);
            // Not consumed — duplicate scrolls stay in inventory (user's call, 27 Sep 2026).
            return InteractionResultHolder.pass(stack);
        }

        player.setData(ModAttachments.PLAYER_TITLES.get(), data.withUnlocked(titleId));

        player.displayClientMessage(
                Component.literal("Title unlocked: ").withStyle(ChatFormatting.GOLD)
                        .append(ColorCodes.translate(definition.get().display()))
                        .append(Component.literal("  (use /title to select it)").withStyle(ChatFormatting.DARK_GRAY)),
                false);

        stack.shrink(1);
        return InteractionResultHolder.success(stack);
    }
}
