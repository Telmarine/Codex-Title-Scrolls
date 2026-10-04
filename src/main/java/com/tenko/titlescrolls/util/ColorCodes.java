package com.tenko.titlescrolls.util;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

public final class ColorCodes {

    private ColorCodes() {}

    public static MutableComponent translate(String legacyText) {
        MutableComponent result = Component.empty();
        MutableComponent current = Component.empty();
        Style currentStyle = Style.EMPTY;

        char[] chars = legacyText.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            if (c == '&' && i + 1 < chars.length) {
                char code = Character.toLowerCase(chars[i + 1]);
                ChatFormatting formatting = ChatFormatting.getByCode(code);
                if (formatting != null) {
                    if (!current.getString().isEmpty()) {
                        result = result.append(current);
                    }
                    current = Component.empty();
                    if (formatting == ChatFormatting.RESET) {
                        currentStyle = Style.EMPTY;
                    } else if (formatting.isColor()) {
                        currentStyle = Style.EMPTY.withColor(formatting);
                    } else {
                        currentStyle = applyFormatting(currentStyle, formatting);
                    }
                    i++;
                    continue;
                }
            }
            current = current.append(Component.literal(String.valueOf(c)).setStyle(currentStyle));
        }
        result = result.append(current);
        return result;
    }

    private static Style applyFormatting(Style style, ChatFormatting formatting) {
        return switch (formatting) {
            case BOLD -> style.withBold(true);
            case ITALIC -> style.withItalic(true);
            case UNDERLINE -> style.withUnderlined(true);
            case STRIKETHROUGH -> style.withStrikethrough(true);
            case OBFUSCATED -> style.withObfuscated(true);
            default -> style;
        };
    }
}