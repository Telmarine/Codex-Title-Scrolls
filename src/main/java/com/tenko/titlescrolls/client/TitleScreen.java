package com.tenko.titlescrolls.client;

import com.tenko.titlescrolls.network.OpenTitleScreenPayload;
import com.tenko.titlescrolls.network.SetActiveTitlePayload;
import com.tenko.titlescrolls.util.ColorCodes;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The real GUI behind /title — rendered as a fixed grid of inventory-style
 * slots (like a chest screen), not a scrolling text list (user's call,
 * 27 Sep 2026 — "don't want a scrolling window for the gui"). Every title
 * fits on one screen at once, no paging/scrolling needed for up to 54
 * entries (9 columns wide).
 *
 * Every owned title shares the same icon (a Name Tag — fits the "title"
 * theme); rarity is conveyed purely by the slot's border color instead of
 * different item icons (user's call, 27 Sep 2026): green = common,
 * blue = rare, purple = epic, gold = legendary, red = unique/S-class.
 * The active title additionally gets a white outer ring around its rarity
 * border so it stays distinguishable even on a gold (legendary) slot.
 *
 * The catalog is sorted by rarity tier (common block, then rare, epic,
 * legendary, unique, in that order) client-side in the constructor, so
 * slots always group by rarity rather than whatever order the server's
 * datapack loader happened to produce (user's call, 27 Sep 2026). Sort is
 * stable, so titles within the same tier keep their original relative
 * order from the server.
 *
 * Hover info is shown in a FIXED strip below the grid rather than a
 * tooltip box that follows the cursor — GuiGraphics.renderItem() draws
 * through a separate render buffer that doesn't reliably flush in
 * code-call order relative to plain fill() rectangles, so a tooltip near
 * another slot's icon could get drawn underneath it regardless of draw
 * order in code. Anchoring the info text to a fixed spot outside the grid
 * entirely sidesteps that instead of fighting it.
 *
 * Deliberately built with only the most stable, version-independent
 * GuiGraphics primitives (fill, drawString, renderItem) instead of
 * blitting real vanilla container textures — blit()'s exact signature has
 * moved around between Minecraft versions more than almost anything else
 * in the GUI code, so this avoids that risk entirely rather than guessing.
 */
public class TitleScreen extends Screen {

    private static final int SLOT_SIZE = 18;
    private static final int COLS = 9;
    /** Six rows per page, like a double chest. */
    private static final int PAGE_SIZE = COLS * 6;
    private static final ItemStack ICON = new ItemStack(Items.NAME_TAG);

    private final List<OpenTitleScreenPayload.TitleEntry> catalog;
    private final Set<String> unlocked;
    private String activeTitle;

    private int gridLeft;
    private int gridTop;
    private int hoveredIndex = -1;
    private int page = 0;

    public TitleScreen(OpenTitleScreenPayload payload) {
        super(Component.literal("Titles"));
        List<OpenTitleScreenPayload.TitleEntry> sorted = new ArrayList<>(payload.titles());
        sorted.sort(Comparator.comparingInt(e -> rarityRank(e.rarity())));
        this.catalog = sorted;
        this.unlocked = new HashSet<>(payload.unlocked());
        this.activeTitle = payload.activeTitle();
    }

    private int pages() {
        return Math.max(1, (int) Math.ceil(catalog.size() / (double) PAGE_SIZE));
    }

    /** Number of titles on the current page. */
    private int pageCount() {
        return Math.min(PAGE_SIZE, catalog.size() - page * PAGE_SIZE);
    }

    @Override
    protected void init() {
        int rows = Math.max(1, (int) Math.ceil(Math.min(PAGE_SIZE, catalog.size()) / (double) COLS));
        int gridWidth = COLS * SLOT_SIZE;
        int gridHeight = rows * SLOT_SIZE;
        this.gridLeft = (this.width - gridWidth) / 2;
        this.gridTop = Math.max(40, (this.height - gridHeight) / 2);
        if (pages() > 1) {
            int y = gridTop + gridHeight + 52;
            addRenderableWidget(net.minecraft.client.gui.components.Button.builder(Component.literal("<"), b -> {
                if (page > 0) page--;
            }).bounds(this.width / 2 - 60, y, 20, 20).build());
            addRenderableWidget(net.minecraft.client.gui.components.Button.builder(Component.literal(">"), b -> {
                if (page < pages() - 1) page++;
            }).bounds(this.width / 2 + 40, y, 20, 20).build());
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        int rows = Math.max(1, (int) Math.ceil(Math.min(PAGE_SIZE, catalog.size()) / (double) COLS));
        int gridWidth = COLS * SLOT_SIZE;
        int gridHeight = rows * SLOT_SIZE;

        graphics.fill(gridLeft - 8, gridTop - 8, gridLeft + gridWidth + 8, gridTop + gridHeight + 8, 0xF0C6C6C6);
        graphics.fill(gridLeft - 6, gridTop - 6, gridLeft + gridWidth + 6, gridTop + gridHeight + 6, 0xF0373737);

        int collectedCount = unlocked.size();
        graphics.drawCenteredString(this.font, "Titles — " + collectedCount + "/" + catalog.size() + " collected",
                this.width / 2, gridTop - 22, 0xFFFFFF);

        this.hoveredIndex = -1;

        if (pages() > 1) {
            graphics.drawCenteredString(this.font, "Page " + (page + 1) + "/" + pages(), this.width / 2, gridTop + gridHeight + 58, 0xFFFFFF);
        }

        for (int slot = 0; slot < pageCount(); slot++) {
            int i = page * PAGE_SIZE + slot;
            int col = slot % COLS;
            int row = slot / COLS;
            int x = gridLeft + col * SLOT_SIZE;
            int y = gridTop + row * SLOT_SIZE;

            OpenTitleScreenPayload.TitleEntry entry = catalog.get(i);
            boolean owned = unlocked.contains(entry.id());
            boolean active = entry.id().equals(activeTitle);
            boolean hovered = mouseX >= x && mouseX < x + SLOT_SIZE && mouseY >= y && mouseY < y + SLOT_SIZE;
            if (hovered) {
                this.hoveredIndex = i;
            }

            int slotColor = hovered ? 0xFF8B8B8B : 0xFF6B6B6B;
            graphics.fill(x + 1, y + 1, x + SLOT_SIZE - 1, y + SLOT_SIZE - 1, slotColor);

            if (owned) {
                graphics.renderItem(ICON, x + 1, y + 1);
                drawBorder(graphics, x, y, rarityColor(entry.rarity()));
                if (active) {
                    drawOuterRing(graphics, x, y, 0xFFFFFFFF);
                }
            } else {
                graphics.drawCenteredString(this.font, "?", x + SLOT_SIZE / 2, y + 5, 0xAAAAAA);
            }
        }

        int infoY = gridTop + gridHeight + 14;
        if (hoveredIndex >= 0) {
            OpenTitleScreenPayload.TitleEntry entry = catalog.get(hoveredIndex);
            boolean owned = unlocked.contains(entry.id());
            if (owned) {
                MutableComponent name = ColorCodes.translate(entry.display());
                graphics.drawCenteredString(this.font, name, this.width / 2, infoY, 0xFFFFFF);
                if (!entry.flavorText().isEmpty()) {
                    graphics.drawCenteredString(this.font, entry.flavorText(), this.width / 2, infoY + 11, 0xAAAAAA);
                }
                if (entry.id().equals(activeTitle)) {
                    graphics.drawCenteredString(this.font, "Click again to take this title off.", this.width / 2, infoY + 22, 0x777777);
                }
            } else {
                graphics.drawCenteredString(this.font, "???", this.width / 2, infoY, 0xAAAAAA);
            }
        }
    }

    private void drawBorder(GuiGraphics graphics, int x, int y, int color) {
        graphics.fill(x, y, x + SLOT_SIZE, y + 1, color);
        graphics.fill(x, y + SLOT_SIZE - 1, x + SLOT_SIZE, y + SLOT_SIZE, color);
        graphics.fill(x, y, x + 1, y + SLOT_SIZE, color);
        graphics.fill(x + SLOT_SIZE - 1, y, x + SLOT_SIZE, y + SLOT_SIZE, color);
    }

    private void drawOuterRing(GuiGraphics graphics, int x, int y, int color) {
        int x0 = x - 1;
        int y0 = y - 1;
        int x1 = x + SLOT_SIZE + 1;
        int y1 = y + SLOT_SIZE + 1;
        graphics.fill(x0, y0, x1, y0 + 1, color);
        graphics.fill(x0, y1 - 1, x1, y1, color);
        graphics.fill(x0, y0, x0 + 1, y1, color);
        graphics.fill(x1 - 1, y0, x1, y1, color);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (hoveredIndex >= 0) {
            OpenTitleScreenPayload.TitleEntry entry = catalog.get(hoveredIndex);
            if (unlocked.contains(entry.id())) {
                // Clicking the active title takes it off.
                selectTitle(entry.id().equals(activeTitle) ? "" : entry.id());
                return true;
            }
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void selectTitle(String id) {
        this.activeTitle = id;
        PacketDistributor.sendToServer(new SetActiveTitlePayload(id));
        this.onClose();
    }

    private static int rarityRank(String rarity) {
        return switch (rarity) {
            case "common" -> 0;
            case "rare" -> 1;
            case "epic" -> 2;
            case "legendary" -> 3;
            case "unique" -> 4;
            default -> 5;
        };
    }

    private static int rarityColor(String rarity) {
        return switch (rarity) {
            case "rare" -> 0xFF5555FF;
            case "epic" -> 0xFFAA00AA;
            case "legendary" -> 0xFFFFAA00;
            case "unique" -> 0xFFFF5555;
            default -> 0xFF55FF55;
        };
    }
}