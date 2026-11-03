package com.tac5studios.codextitlescrolls.config;

/** Every feature with its switch in features.toml. */
public enum Feature {
    TITLE_SCROLLS("title_scrolls", "Right-clicking a Title Scroll unlocks its title."),
    TITLE_MENU("title_menu", "/title opens the title menu. Off: /title is vanilla's on-screen text command again."),
    GIVE_COMMAND("give_command", "/titlescroll give."),
    SCROLL_TOOLTIP("scroll_tooltip", "Hovering a Title Scroll shows its title and rarity."),
    NAMETAG_TITLES("nametag_titles", "The active title shows on the player, above or beside the name (settings.toml)."),
    CHAT_BRIDGE("chat_bridge", "The {active_title} chat placeholder for NeoEssentials."),
    SCROLL_LOOT("scroll_loot", "Title Scrolls are added to loot (which loot, how often and which titles: [loot] in settings.toml).", false);

    public final String key;
    public final String comment;
    public final boolean onByDefault;

    Feature(String key, String comment) {
        this(key, comment, true);
    }

    Feature(String key, String comment, boolean onByDefault) {
        this.key = key;
        this.comment = comment;
        this.onByDefault = onByDefault;
    }

    public static Feature byKey(String key) {
        for (Feature f : values()) if (f.key.equals(key)) return f;
        return null;
    }
}
