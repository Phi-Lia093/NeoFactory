package com.philia093.neofactory.gui.creative;

import com.philia093.neofactory.item.Item;
import com.philia093.neofactory.item.ItemStack;
import com.philia093.neofactory.item.Items;
import com.philia093.neofactory.item.PlayerInventory;
import com.philia093.neofactory.render.BlockTextureCache;
import com.philia093.neofactory.support.TestIcons;
import com.philia093.neofactory.support.TestRegistries;
import com.philia093.neofactory.util.Constants;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the pictures of the creative inventory and writes a preview of the screen.
 * <p>
 * Two things are checked without a window: the pieces of the original art really sit
 * where {@link CreativeLayout} says they do - the grid of the panel is measured against
 * the slots the screen draws into it - and the slots, the tabs and the scroll bar stay
 * inside the panel.
 * <p>
 * The same code paints {@code core/build/reports/creative-preview.png}: the panel with
 * its tabs, the grid filled with the blocks of the game and the search panel below it,
 * so the look of the screen is reviewed without starting the game.
 */
class CreativePreviewTest {

    /** Assets of the project, the tests run inside the core module. */
    private static final Path ASSETS = Path.of("..", "assets");

    /** The sheet holding the panels of the creative inventory. */
    private static final Path SHEET = ASSETS.resolve(CreativeTextures.SHEET + ".png");

    /** Picture the preview is written to. */
    private static final Path PREVIEW = Path.of("build", "reports", "creative-preview.png");

    /** Colour the preview is filled with, a dark grey that shows the frame. */
    private static final int BACKDROP = 0xFF202020;

    /** Space between the two panels of the preview and around it. */
    private static final int GAP = 8;

    /** Zoom the preview is drawn with. */
    private static final int ZOOM = 2;

    /** Where the panel of the items lies inside the sheet. */
    private static final int ITEMS_X = 256;
    private static final int ITEMS_Y = 0;

    /** Where the tab of a chosen tab lies inside the sheet. */
    private static final int TAB_ACTIVE_X = 285;
    private static final int TAB_ACTIVE_Y = 280;

    /** Where the two thumbs of the scroll bar lie inside the sheet. */
    private static final int THUMB_ACTIVE_X = 256;
    private static final int THUMB_IDLE_X = 270;
    private static final int THUMB_ACTIVE_Y = 316;

    @BeforeAll
    static void registerGameData() {
        TestRegistries.ensure();
    }

    @Test
    void everySlotSitsInARecessOfThePanel() throws IOException {
        BufferedImage sheet = readSheet();

        for (int row = 0; row < CreativeInventory.ROWS; row++) {
            for (int column = 0; column < CreativeInventory.COLUMNS; column++) {
                int index = row * CreativeInventory.COLUMNS + column;
                int x = ITEMS_X + CreativeLayout.slotIconX(index);
                int y = ITEMS_Y + CreativeLayout.slotIconY(index);
                assertNotEquals(sheet.getRGB(x - 1, y + 2), sheet.getRGB(x + 2, y + 2),
                        "slot " + index + " has a drawn left edge");
                assertNotEquals(sheet.getRGB(x + 2, y - 1), sheet.getRGB(x + 2, y + 2),
                        "slot " + index + " has a drawn top edge");
            }
        }
        for (int slot = 0; slot < CreativeLayout.HOTBAR_COLUMNS; slot++) {
            int x = ITEMS_X + CreativeLayout.hotbarIconX(slot);
            int y = ITEMS_Y + CreativeLayout.hotbarIconY();
            assertNotEquals(sheet.getRGB(x - 1, y + 2), sheet.getRGB(x + 2, y + 2),
                    "hotbar slot " + slot + " has a drawn left edge");
            assertNotEquals(sheet.getRGB(x + 2, y - 1), sheet.getRGB(x + 2, y + 2),
                    "hotbar slot " + slot + " has a drawn top edge");
        }
    }

    @Test
    void theThumbWalksFromTheTopOfTheTrackToItsBottom() {
        int travel = CreativeLayout.SCROLL_HEIGHT - CreativeLayout.SCROLL_THUMB_HEIGHT;

        assertEquals(CreativeLayout.SCROLL_Y, CreativeLayout.scrollThumbY(0, 4),
                "the first row puts the thumb at the upper end of the track");
        assertEquals(CreativeLayout.SCROLL_Y + travel, CreativeLayout.scrollThumbY(4, 4),
                "the last row puts it at the lower end");
        assertEquals(CreativeLayout.SCROLL_Y + travel / 2, CreativeLayout.scrollThumbY(2, 4),
                "and half way down in between");
        assertEquals(CreativeLayout.SCROLL_Y, CreativeLayout.scrollThumbY(0, 0),
                "a list that fits keeps the thumb at the upper end");

        assertEquals(0, CreativeLayout.rowAtScrollY(CreativeLayout.SCROLL_Y, 4),
                "a click on the upper end of the track asks for the first row");
        assertEquals(4, CreativeLayout.rowAtScrollY(
                CreativeLayout.SCROLL_Y + travel + CreativeLayout.SCROLL_THUMB_HEIGHT, 4),
                "a click below the track asks for the last one");
    }

    @Test
    void everySlotTabAndThumbStaysInsideThePanel() {
        assertTrue(CreativeLayout.slotIconX(CreativeLayout.COLUMNS - 1) + CreativeLayout.SLOT_SIZE
                < CreativeLayout.SCROLL_X, "the last column stays left of the scroll bar");
        assertTrue(CreativeLayout.slotIconY((CreativeLayout.ROWS - 1) * CreativeLayout.COLUMNS)
                + CreativeLayout.SLOT_SIZE < CreativeLayout.HOTBAR_Y,
                "the last row stays above the hotbar");
        assertTrue(CreativeLayout.hotbarIconX(CreativeLayout.HOTBAR_COLUMNS - 1)
                + CreativeLayout.SLOT_SIZE <= CreativeLayout.PANEL_WIDTH,
                "the hotbar ends inside the panel");
        int tabs = CreativeRegistry.tabs().size();
        for (int index = 0; index < tabs; index++) {
            int tabX = CreativeLayout.tabX(index, tabs);
            assertTrue(tabX >= 0 && tabX + CreativeLayout.TAB_WIDTH <= CreativeLayout.PANEL_WIDTH,
                    "tab " + index + " stays within the width of the panel");
        }
        assertTrue(CreativeLayout.SCROLL_Y + CreativeLayout.SCROLL_HEIGHT
                <= CreativeLayout.PANEL_HEIGHT, "the track ends inside the panel");
        assertTrue(CreativeLayout.TAB_ICON_X + CreativeLayout.SLOT_SIZE <= CreativeLayout.TAB_WIDTH,
                "the icon of a tab fits into it");
        assertTrue(CreativeLayout.TAB_ICON_Y + CreativeLayout.SLOT_SIZE < CreativeLayout.TAB_HEIGHT,
                "and stays above the edge that reaches into the panel");
        assertTrue(CreativeLayout.SCROLL_WIDTH <= CreativeLayout.PANEL_WIDTH,
                "a thumb is never wider than the panel");
        assertTrue(CreativeLayout.SCROLL_Y + CreativeLayout.SCROLL_THUMB_HEIGHT
                <= CreativeLayout.PANEL_HEIGHT, "and never reaches below the panel");

        assertEquals(CreativeLayout.SCROLL_Y, CreativeLayout.scrollThumbY(0, 3),
                "the thumb starts at the top of the track");
        assertTrue(CreativeLayout.scrollThumbY(3, 3) + CreativeLayout.SCROLL_THUMB_HEIGHT
                <= CreativeLayout.SCROLL_Y + CreativeLayout.SCROLL_HEIGHT,
                "and ends before the track does");
        assertEquals(CreativeLayout.SCROLL_Y + CreativeLayout.SCROLL_HEIGHT
                - CreativeLayout.SCROLL_THUMB_HEIGHT, CreativeLayout.scrollThumbY(3, 3),
                "a full scroll puts the thumb on the last row");
    }

    @Test
    void theScreenIsPaintedAsTheGameDrawsIt() throws IOException {
        BufferedImage sheet = readSheet();
        CreativeInventory creative = new CreativeInventory();
        int width = 3 * CreativeLayout.PANEL_WIDTH + 4 * GAP;
        int height = 2 * CreativeLayout.TAB_HEIGHT + CreativeLayout.PANEL_HEIGHT + 3 * GAP;
        BufferedImage canvas = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        fill(canvas, BACKDROP);

        int panelTop = GAP + CreativeLayout.TAB_HEIGHT - CreativeLayout.TAB_INSET;
        drawScreen(sheet, canvas, creative, null, GAP, panelTop, 0);
        // The second screen searches for one letter, which matches more items than the
        // grid can show, so both shapes of the scroll bar appear in this preview.
        creative.setQuery("a");
        drawScreen(sheet, canvas, creative, null, GAP + CreativeLayout.PANEL_WIDTH + GAP, panelTop,
                creative.selectedIndex());
        // The third screen is the inventory of the player: the very same panel with the
        // slots of the player in it, which is what keeps the screen still while the tabs
        // are chosen.
        PlayerInventory player = new PlayerInventory();
        player.set(PlayerInventory.HOTBAR_SLOTS, ItemStack.of(Items.STONE, 64));
        player.set(PlayerInventory.HOTBAR_SLOTS + 2, ItemStack.of(Items.FURNACE, 1));
        player.set(0, ItemStack.of(Items.IRON_PICKAXE, 1));
        creative.setQuery("");
        creative.select(indexOfTab(CreativeTab.Kind.INVENTORY));
        drawScreen(sheet, canvas, creative, player,
                GAP + 2 * (CreativeLayout.PANEL_WIDTH + GAP), panelTop, creative.selectedIndex());

        BufferedImage zoomed = new BufferedImage(width * ZOOM, height * ZOOM,
                BufferedImage.TYPE_INT_ARGB);
        blit(canvas, zoomed, 0, 0, ZOOM);
        Files.createDirectories(PREVIEW.getParent());
        ImageIO.write(zoomed, "png", PREVIEW.toFile());
    }

    /** Index of the first tab of the given kind. */
    private static int indexOfTab(CreativeTab.Kind kind) {
        for (int index = 0; index < CreativeRegistry.tabs().size(); index++) {
            if (CreativeRegistry.tabs().get(index).kind() == kind) {
                return index;
            }
        }
        throw new AssertionError("the game has no tab of kind " + kind);
    }

    /**
     * Draws the whole screen of the creative inventory, the way the game draws it.
     *
     * @param player inventory shown instead of the grid of items, {@code null} for a tab
     *               that lists items
     */
    private static void drawScreen(BufferedImage sheet, BufferedImage canvas,
            CreativeInventory creative, PlayerInventory player, int panelX, int panelTop,
            int chosen) {
        int tabs = creative.tabs().size();
        for (int index = 0; index < tabs; index++) {
            boolean active = index == chosen;
            boolean below = CreativeLayout.isTabBelow(index, tabs);
            int tabX = panelX + CreativeLayout.tabX(index, tabs);
            int tabY = panelTop + CreativeLayout.tabY(index, tabs);
            int sourceX = active ? TAB_ACTIVE_X : 256;
            int sourceY = active ? TAB_ACTIVE_Y : 280;
            if (below) {
                // The row below the panel is painted with the very art of the upper one,
                // flipped upside down, see CreativeTextures#turned.
                blitUpsideDownFrom(sheet, canvas, sourceX, sourceY, tabX, tabY,
                        CreativeLayout.TAB_WIDTH, CreativeLayout.TAB_HEIGHT);
            } else {
                blitFrom(sheet, canvas, sourceX, sourceY, tabX, tabY, CreativeLayout.TAB_WIDTH,
                        CreativeLayout.TAB_HEIGHT);
            }
            CreativeTab tab = creative.tabs().get(index);
            if (tab.hasIcon()) {
                // The tabs of the art are empty shapes, the icon is an item drawn on the
                // very place the picture of a tab leaves for it.
                int iconX = tabX + CreativeLayout.TAB_ICON_X;
                int iconY = below
                        ? tabY + CreativeLayout.TAB_HEIGHT - CreativeLayout.TAB_ICON_Y
                                - CreativeLayout.SLOT_SIZE
                        : tabY + CreativeLayout.TAB_ICON_Y;
                drawItem(canvas, iconX, iconY, ItemStack.of(tab.icon(), 1));
            }
        }
        blitFrom(sheet, canvas, ITEMS_X, creative.isSearching() ? 136 : ITEMS_Y, panelX, panelTop,
                CreativeLayout.PANEL_WIDTH, CreativeLayout.PANEL_HEIGHT);

        if (creative.isSearching()) {
            // The text of the search box is drawn by the font of the game, which the
            // preview cannot set up, so its place is marked with the room it takes.
            fill(canvas, panelX + CreativeLayout.SEARCH_X + CreativeLayout.SEARCH_INSET,
                    panelTop + CreativeLayout.SEARCH_Y + CreativeLayout.SEARCH_INSET, 30, 6,
                    0xFFE0E0E0);
        }
        for (int index = 0; index < CreativeInventory.PAGE_SIZE; index++) {
            ItemStack stack = player == null
                    ? creative.stackAt(index)
                    : stackOf(player, index);
            if (!stack.isEmpty()) {
                drawItem(canvas, panelX + CreativeLayout.slotIconX(index),
                        panelTop + CreativeLayout.slotIconY(index), stack);
            }
        }
        for (int slot = 0; slot < CreativeLayout.HOTBAR_COLUMNS; slot++) {
            if (player == null) {
                continue;
            }
            ItemStack stack = player.get(slot);
            if (!stack.isEmpty()) {
                drawItem(canvas, panelX + CreativeLayout.hotbarIconX(slot),
                        panelTop + CreativeLayout.hotbarIconY(), stack);
            }
        }
        // The bright thumb is the one that can be dragged, the dark one says that
        // everything already fits into the grid.
        blitFrom(sheet, canvas, creative.maxRow() > 0 ? THUMB_ACTIVE_X : THUMB_IDLE_X,
                THUMB_ACTIVE_Y, panelX + CreativeLayout.SCROLL_X,
                panelTop + CreativeLayout.scrollThumbY(creative.firstRow(), creative.maxRow()),
                CreativeLayout.SCROLL_WIDTH, CreativeLayout.SCROLL_THUMB_HEIGHT);
    }

    /** Draws the icon of a stack, a block as a small cube, everything else as it is. */
    private static void drawItem(BufferedImage target, int x, int y, ItemStack stack) {
        int size = Constants.ITEM_ICON_SIZE;
        int[] icon = TestIcons.icon(stack.item());
        for (int row = 0; row < size; row++) {
            for (int column = 0; column < size; column++) {
                int colour = icon[row * size + column];
                if ((colour & 0xFF) == 0) {
                    continue;
                }
                target.setRGB(x + column, y + row, toArgb(colour));
            }
        }
    }

    /** The stack a cell of the panel holds when the inventory of the player is shown. */
    private static ItemStack stackOf(PlayerInventory player, int index) {
        int slot = PlayerInventory.HOTBAR_SLOTS + index;
        return slot < PlayerInventory.SLOT_COUNT ? player.get(slot) : ItemStack.EMPTY;
    }

    /** Copies a rectangle of the sheet into the preview. */
    private static void blitFrom(BufferedImage sheet, BufferedImage target, int sourceX,
            int sourceY, int targetX, int targetY, int width, int height) {
        for (int row = 0; row < height; row++) {
            for (int column = 0; column < width; column++) {
                target.setRGB(targetX + column, targetY + row,
                        sheet.getRGB(sourceX + column, sourceY + row));
            }
        }
    }

    /**
     * Copies a rectangle of the sheet into the preview, flipped upside down.
     * <p>
     * That is what the tabs below the panel are painted with: turned by half a circle and
     * mirrored back left to right, which leaves them flipped upside down, see
     * {@code CreativeTextures#turned}.
     */
    private static void blitUpsideDownFrom(BufferedImage sheet, BufferedImage target, int sourceX,
            int sourceY, int targetX, int targetY, int width, int height) {
        for (int row = 0; row < height; row++) {
            for (int column = 0; column < width; column++) {
                target.setRGB(targetX + column, targetY + row,
                        sheet.getRGB(sourceX + column, sourceY + height - 1 - row));
            }
        }
    }

    /** Copies a whole picture into a target, enlarged by a whole number. */
    private static void blit(BufferedImage source, BufferedImage target, int x, int y, int scale) {
        for (int row = 0; row < source.getHeight(); row++) {
            for (int column = 0; column < source.getWidth(); column++) {
                int colour = source.getRGB(column, row);
                for (int offsetY = 0; offsetY < scale; offsetY++) {
                    for (int offsetX = 0; offsetX < scale; offsetX++) {
                        target.setRGB(x + column * scale + offsetX, y + row * scale + offsetY,
                                colour);
                    }
                }
            }
        }
    }

    /** Fills a whole picture with one colour. */
    private static void fill(BufferedImage image, int argb) {
        fill(image, 0, 0, image.getWidth(), image.getHeight(), argb);
    }

    /** Fills a rectangle of a picture with one colour. */
    private static void fill(BufferedImage image, int x, int y, int width, int height, int argb) {
        for (int row = 0; row < height; row++) {
            for (int column = 0; column < width; column++) {
                image.setRGB(x + column, y + row, argb);
            }
        }
    }

    /** RGBA8888 of the game to the ARGB8888 the picture writer expects. */
    private static int toArgb(int colour) {
        return ((colour & 0xFF) << 24) | ((colour >>> 8) & 0xFFFFFF);
    }

    /** Reads the sheet the panels are cut from. */
    private static BufferedImage readSheet() throws IOException {
        BufferedImage sheet = ImageIO.read(SHEET.toFile());
        assertNotNull(sheet, "the sheet " + SHEET + " can be read");
        return sheet;
    }
}
