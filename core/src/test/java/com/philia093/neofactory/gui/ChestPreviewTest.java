package com.philia093.neofactory.gui;

import com.badlogic.gdx.graphics.Color;
import com.philia093.neofactory.blockentity.ChestBlockEntity;
import com.philia093.neofactory.gui.container.ContainerLayout;
import com.philia093.neofactory.gui.container.Slot;
import com.philia093.neofactory.gui.panel.NineSlice;
import com.philia093.neofactory.gui.panel.PanelTextures;
import com.philia093.neofactory.item.Inventory;
import com.philia093.neofactory.item.ItemStack;
import com.philia093.neofactory.item.Items;
import com.philia093.neofactory.item.PlayerInventory;
import com.philia093.neofactory.material.Materials;
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
 * Paints the screen of a chest the way the game draws it and writes the result to a picture.
 * <p>
 * The test builds the very layout the screen uses, {@link ChestLayout#of}, and paints it with the very
 * pictures the game uses: the panel of {@code gui/inventory_icons.png} stretched in nine cells, the
 * bevel of every slot, the small cube a block item is folded into and the bar of a worn tool. The name
 * of the container is written into the band the layout keeps free at the top and is drawn by the
 * screen, not by this test; the band itself is part of the picture here.
 * <p>
 * The result lies in {@code core/build/reports/chest-preview.png} and is meant to be looked at: it is how
 * the layout of a container screen is reviewed without starting the game. The test also pins down that
 * the slots of the player land on the rows the layout names, so the drawn panel and the click of the
 * player stay the same place.
 */
class ChestPreviewTest {

    /** Picture the preview is written to. */
    private static final Path PREVIEW = Path.of("build", "reports", "chest-preview.png");

    /** Colour the preview is filled with, a dark grey that shows the frame. */
    private static final int BACKDROP = 0xFF202020;

    /** Space around the panel. */
    private static final int MARGIN = 8;

    /** X coordinate of the slot bevel inside {@link PanelTextures#SHEET}. */
    private static final int SLOT_X = 176;

    /** Y coordinate of the slot bevel inside {@link PanelTextures#SHEET}. */
    private static final int SLOT_Y = 0;

    @BeforeAll
    static void registerGameData() {
        TestRegistries.ensure();
    }

    @Test
    void theScreenOfAChestIsPaintedAsTheGameDrawsIt() throws IOException {
        Inventory contents = new Inventory(ChestBlockEntity.SLOTS);
        PlayerInventory player = new PlayerInventory();
        ContainerLayout layout = ChestLayout.of(contents, player);

        assertEquals(ChestBlockEntity.SLOTS + PlayerInventory.SLOT_COUNT, layout.size());
        assertEquals(PanelTextures.PANEL_WIDTH, layout.panelWidth());
        assertEquals(ChestLayout.HOTBAR_Y + Slot.size() + ContainerLayout.PADDING,
                layout.panelHeight(), "the panel ends below the hotbar row");
        assertEquals(ChestLayout.STORAGE_Y, layout.slots().get(ChestBlockEntity.SLOTS).y(),
                "the storage of the player lands on the row the layout names");
        assertEquals(ChestLayout.HOTBAR_Y,
                layout.slots().get(layout.size() - PlayerInventory.HOTBAR_SLOTS).y(),
                "the hotbar of the player lands on the row the layout names");

        fillChest(contents);
        fillPlayer(player);

        Path sheetPath = Path.of("..", "assets").resolve(PanelTextures.SHEET + ".png");
        BufferedImage sheet = ImageIO.read(sheetPath.toFile());
        BufferedImage picture = new BufferedImage(MARGIN + layout.panelWidth() + MARGIN,
                MARGIN + layout.panelHeight() + MARGIN, BufferedImage.TYPE_INT_ARGB);
        fill(picture, BACKDROP);

        drawPanel(sheet, picture, MARGIN, MARGIN, layout.panelWidth(), layout.panelHeight());
        for (Slot slot : layout.slots()) {
            drawSlot(sheet, picture, MARGIN + slot.x(), MARGIN + slot.y());
            drawItem(picture, MARGIN + slot.x(), MARGIN + slot.y(), slot.stack());
            drawDurabilityBar(picture, MARGIN + slot.x(), MARGIN + slot.y(), slot.stack());
        }

        Files.createDirectories(PREVIEW.getParent());
        ImageIO.write(picture, "png", PREVIEW.toFile());
        System.out.println("chest preview written to " + PREVIEW.toAbsolutePath()
                + ": panel " + layout.panelWidth() + " x " + layout.panelHeight() + ", "
                + layout.size() + " slots");

        assertTrue(Files.isRegularFile(PREVIEW), "the preview exists");
    }

    /** Puts a small kit into the chest, so the picture shows what a player keeps in it. */
    private static void fillChest(Inventory contents) {
        contents.set(0, ItemStack.of(Items.STONE, 64));
        contents.set(1, ItemStack.of(Items.COBBLESTONE, 32));
        contents.set(2, ItemStack.of(Items.COAL_ORE, 12));
        contents.set(3, ItemStack.of(Materials.IRON.ingot(), 9));
        contents.set(4, ItemStack.of(Items.PLANKS_OAK, 48));
        contents.set(5, ItemStack.of(Items.GLASS, 16));
        contents.set(9, ItemStack.of(Items.FURNACE, 2));
        contents.set(10, ItemStack.of(Items.CRAFTING_TABLE, 1));
        contents.set(13, ItemStack.of(Items.TORCH, 24));
        contents.set(18, ItemStack.of(Materials.COPPER.ingot(), 5));
    }

    /** Puts a small kit into the inventory of the player, a worn tool included. */
    private static void fillPlayer(PlayerInventory player) {
        ItemStack pickaxe = ItemStack.of(Items.DIAMOND_PICKAXE, 1);
        pickaxe.setDamage(Items.DIAMOND_TOOL_DURABILITY / 3);
        player.set(0, pickaxe);
        player.set(1, ItemStack.of(Items.SAND, 21));
        player.set(2, ItemStack.of(Items.STICK, 32));
        player.set(9, ItemStack.of(Items.DIRT, 64));
        player.set(11, ItemStack.of(Items.GRAVEL, 64));
        player.set(13, ItemStack.of(Materials.BRONZE.ingot(), 11));
        player.set(20, ItemStack.of(Items.CLAY_BALL, 13));
        player.set(29, ItemStack.of(Items.LADDER, 6));
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
