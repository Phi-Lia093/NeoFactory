package com.philia093.neofactory.gui;

import com.badlogic.gdx.graphics.Color;
import com.philia093.neofactory.gui.container.ContainerLayout;
import com.philia093.neofactory.material.Materials;
import com.philia093.neofactory.gui.panel.NineSlice;
import com.philia093.neofactory.gui.panel.PanelTextures;
import com.philia093.neofactory.item.Inventory;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Paints the inventory screen the way the game draws it and writes the result to a
 * picture.
 * <p>
 * The test builds the very layout the screen uses, {@link InventoryLayout#of}, and
 * paints it with the very pictures and the very folder for the icons the game uses:
 * the panel is stretched in nine cells, every slot gets its bevel, the crafting arrow
 * is drawn between the field and its result, and a block item shows the small cube of
 * {@link BlockIconFactory}.
 * <p>
 * The result lies in {@code core/build/reports/inventory-preview.png} and is meant to be
 * looked at: it is how the layout of the screen is reviewed without starting the game.
 */
class InventoryPreviewTest {

    /** Assets of the project, the tests run inside the core module. */
    private static final Path ASSETS = Path.of("..", "assets");

    /** The sheet holding the panel, the slots and the arrow. */
    private static final Path SHEET = ASSETS.resolve(PanelTextures.SHEET + ".png");

    /** Picture the preview is written to. */
    private static final Path PREVIEW = Path.of("build", "reports", "inventory-preview.png");

    /** Colour the preview is filled with, a dark grey that shows the frame. */
    private static final int BACKDROP = 0xFF202020;

    /** Space around the panel. */
    private static final int MARGIN = 8;

    private static final int SLOT_X = 176;
    private static final int SLOT_Y = 0;
    private static final int ARROW_X = 177;

    /** The dark arrow: the crafting arrow of the screen stays empty. */
    private static final int ARROW_Y = 53;

    @BeforeAll
    static void registerGameData() {
        TestRegistries.ensure();
    }

    @Test
    void theScreenIsPaintedAsTheGameDrawsIt() throws IOException {
        PlayerInventory player = new PlayerInventory();
        Inventory crafting = new Inventory(4);
        Inventory result = new Inventory(1);
        ContainerLayout layout = InventoryLayout.of(player, crafting, result);

        assertEquals(4 + 1 + PlayerInventory.SLOT_COUNT, layout.size(), "the slots of the screen");
        assertEquals(PanelTextures.PANEL_WIDTH, layout.panelWidth(), "the panel is as wide as the art");
        assertTrue(layout.panelHeight() > 0);

        fillTestInventory(player);
        // Four clay balls in one slot make a block of clay, the recipe of the game.
        crafting.set(0, ItemStack.of(Items.CLAY_BALL, 4));
        result.set(0, ItemStack.of(Items.CLAY, 1));

        BufferedImage sheet = ImageIO.read(SHEET.toFile());
        BufferedImage picture = new BufferedImage(MARGIN + layout.panelWidth() + MARGIN,
                MARGIN + layout.panelHeight() + MARGIN, BufferedImage.TYPE_INT_ARGB);
        fill(picture, BACKDROP);

        drawPanel(sheet, picture, MARGIN, MARGIN, layout.panelWidth(), layout.panelHeight());
        drawArrow(sheet, picture, MARGIN, MARGIN, layout.panelHeight());
        for (var slot : layout.slots()) {
            drawSlot(sheet, picture, MARGIN + slot.x(), MARGIN + slot.y());
            drawItem(picture, MARGIN + slot.x(), MARGIN + slot.y(), slot.stack());
            drawDurabilityBar(picture, MARGIN + slot.x(), MARGIN + slot.y(), slot.stack());
        }

        Files.createDirectories(PREVIEW.getParent());
        ImageIO.write(picture, "png", PREVIEW.toFile());
        System.out.println("inventory preview written to " + PREVIEW.toAbsolutePath()
                + ": panel " + layout.panelWidth() + " x " + layout.panelHeight() + ", "
                + layout.size() + " slots");

        assertTrue(Files.isRegularFile(PREVIEW), "the preview exists");
    }

    /** Puts a small kit into the player inventory, so the picture shows something. */
    private static void fillTestInventory(PlayerInventory player) {
        // The pickaxe has already dug, which is what makes the preview show the bar of a worn piece.
        ItemStack pickaxe = ItemStack.of(Items.DIAMOND_PICKAXE, 1);
        pickaxe.setDamage(Items.DIAMOND_TOOL_DURABILITY / 3);
        player.set(0, pickaxe);
        player.set(1, ItemStack.of(Items.GRASS, 64));
        player.set(2, ItemStack.of(Items.DIRT, 64));
        player.set(3, ItemStack.of(Items.STONE, 64));
        player.set(4, ItemStack.of(Items.PLANKS_OAK, 40));
        player.set(5, ItemStack.of(Items.LOG_OAK, 12));
        player.set(6, ItemStack.of(Items.STICK, 32));
        player.set(7, ItemStack.of(Items.COAL, 30));
        player.set(8, ItemStack.of(Materials.IRON.ingot(), 24));
        player.set(9, ItemStack.of(Items.SAND, 21));
        player.set(10, ItemStack.of(Items.SANDSTONE, 7));
        player.set(11, ItemStack.of(Items.COAL_ORE, 5));
        player.set(12, ItemStack.of(Items.IRON_ORE, 5));
        player.set(13, ItemStack.of(Items.COBBLESTONE, 9));
        player.set(14, ItemStack.of(Items.CLAY, 13));
        player.set(15, ItemStack.of(Items.GRAVEL, 64));
        player.set(16, ItemStack.of(Items.LEAVES_OAK, 8));
        player.set(17, ItemStack.of(Items.GLASS, 16));
    }

    /** Stretches the nine cells of the panel picture into a target size. */
    private static void drawPanel(BufferedImage sheet, BufferedImage target, int x, int y,
            int width, int height) {
        NineSlice slice = new NineSlice(width, height, PanelTextures.BORDER);
        int[] columnStart = {0, PanelTextures.BORDER,
                PanelTextures.PANEL_WIDTH - PanelTextures.BORDER};
        int[] columnSize = {PanelTextures.BORDER,
                PanelTextures.PANEL_WIDTH - 2 * PanelTextures.BORDER, PanelTextures.BORDER};
        int[] rowStart = {0, PanelTextures.BORDER,
                PanelTextures.PANEL_HEIGHT - PanelTextures.BORDER};
        int[] rowSize = {PanelTextures.BORDER,
                PanelTextures.PANEL_HEIGHT - 2 * PanelTextures.BORDER, PanelTextures.BORDER};

        for (int column = 0; column < NineSlice.COUNT; column++) {
            for (int row = 0; row < NineSlice.COUNT; row++) {
                stretch(sheet, target, columnStart[column], rowStart[row], columnSize[column],
                        rowSize[row], x + slice.x(column), y + slice.y(row), slice.width(column),
                        slice.height(row));
            }
        }
    }

    /** Draws the bevel of one slot, the picture reaches one pixel beyond the cell. */
    private static void drawSlot(BufferedImage sheet, BufferedImage target, int x, int y) {
        stretch(sheet, target, SLOT_X, SLOT_Y, PanelTextures.SLOT_SIZE, PanelTextures.SLOT_SIZE,
                x - PanelTextures.SLOT_BEVEL, y - PanelTextures.SLOT_BEVEL,
                PanelTextures.SLOT_SIZE, PanelTextures.SLOT_SIZE);
    }

    /** Draws the crafting arrow between the field and its result. */
    private static void drawArrow(BufferedImage sheet, BufferedImage target, int panelX, int panelY,
            int panelHeight) {
        stretch(sheet, target, ARROW_X, ARROW_Y, PanelTextures.ARROW_WIDTH,
                PanelTextures.ARROW_HEIGHT, panelX + InventoryLayout.ARROW_X,
                panelY + InventoryLayout.ARROW_Y, PanelTextures.ARROW_WIDTH,
                PanelTextures.ARROW_HEIGHT);
    }

    /** Draws the icon of a stack, a block as a small cube, everything else as it is. */
    private static void drawItem(BufferedImage target, int x, int y, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        int size = Constants.ITEM_ICON_SIZE;
        int[] icon = TestIcons.icon(stack.item());
        for (int row = 0; row < size; row++) {
            for (int column = 0; column < size; column++) {
                int colour = icon[row * size + column];
                if ((colour & 0xFF) == 0) {
                    continue;
                }
                int targetX = x + column;
                int targetY = y + row;
                if (targetX < target.getWidth() && targetY < target.getHeight()) {
                    target.setRGB(targetX, targetY, toArgb(colour));
                }
            }
        }
    }

    /**
     * Draws the bar of a piece that wears out, with the geometry and the colours of the game.
     * <p>
     * The picture counts downwards, the way the art of the project is stored, so the row of the bar is
     * mirrored: what the interface reaches upwards from the lower edge of an icon is a row above the
     * lower edge of that icon here, see {@link DurabilityBar}.
     */
    private static void drawDurabilityBar(BufferedImage target, int x, int y, ItemStack stack) {
        if (!stack.isDamageable() || stack.damage() <= 0) {
            return;
        }
        int barX = x + DurabilityBar.OFFSET_X;
        int barY = y + Constants.ITEM_ICON_SIZE - DurabilityBar.OFFSET_Y - DurabilityBar.HEIGHT;
        fillRectangle(target, barX, barY, DurabilityBar.WIDTH, DurabilityBar.BACKGROUND_HEIGHT,
                toArgb(DurabilityBar.BACKGROUND));
        fillRectangle(target, barX, barY, DurabilityBar.widthOf(stack), DurabilityBar.HEIGHT,
                toArgb(DurabilityBar.colorOf(stack)));
    }

    /** Fills a rectangle of a picture with one colour, ignoring what reaches beyond it. */
    private static void fillRectangle(BufferedImage target, int x, int y, int width, int height,
            int argb) {
        for (int row = 0; row < height; row++) {
            for (int column = 0; column < width; column++) {
                int targetX = x + column;
                int targetY = y + row;
                if (targetX < 0 || targetY < 0 || targetX >= target.getWidth()
                        || targetY >= target.getHeight()) {
                    continue;
                }
                target.setRGB(targetX, targetY, argb);
            }
        }
    }

    /** ARGB of a colour of the game, rounded into the eight bit channels a picture uses. */
    private static int toArgb(Color colour) {
        int alpha = Math.round(colour.a * 255.0f);
        int red = Math.round(colour.r * 255.0f);
        int green = Math.round(colour.g * 255.0f);
        int blue = Math.round(colour.b * 255.0f);
        return (alpha << 24) | (red << 16) | (green << 8) | blue;
    }

    /** Fills a whole picture with one colour. */
    private static void fill(BufferedImage image, int argb) {
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                image.setRGB(x, y, argb);
            }
        }
    }

    /** Copies a rectangle of a source into a target, sampling the nearest pixel. */
    private static void stretch(BufferedImage source, BufferedImage target, int sourceX,
            int sourceY, int sourceWidth, int sourceHeight, int targetX, int targetY,
            int targetWidth, int targetHeight) {
        for (int y = 0; y < targetHeight; y++) {
            int row = sourceY + Math.min(sourceHeight - 1, y * sourceHeight / targetHeight);
            for (int x = 0; x < targetWidth; x++) {
                int column = sourceX + Math.min(sourceWidth - 1, x * sourceWidth / targetWidth);
                int targetColumn = targetX + x;
                int targetRow = targetY + y;
                if (targetColumn < 0 || targetRow < 0 || targetColumn >= target.getWidth()
                        || targetRow >= target.getHeight()) {
                    continue;
                }
                int colour = source.getRGB(column, row);
                if (((colour >>> 24) & 0xFF) == 0) {
                    continue;
                }
                target.setRGB(targetColumn, targetRow, colour);
            }
        }
    }

    /** RGBA8888 of the game to the ARGB8888 the picture writer expects. */
    private static int toArgb(int colour) {
        return ((colour & 0xFF) << 24) | ((colour >>> 8) & 0xFFFFFF);
    }
}
