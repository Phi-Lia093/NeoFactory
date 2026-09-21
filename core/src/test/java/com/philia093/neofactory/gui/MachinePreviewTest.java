package com.philia093.neofactory.gui;

import com.philia093.neofactory.gui.container.ContainerLayout;
import com.philia093.neofactory.gui.container.Slot;
import com.philia093.neofactory.gui.panel.MachineTextures;
import com.philia093.neofactory.gui.panel.PanelTextures;
import com.philia093.neofactory.item.Item;
import com.philia093.neofactory.item.ItemStack;
import com.philia093.neofactory.item.Items;
import com.philia093.neofactory.item.PlayerInventory;
import com.philia093.neofactory.machine.Machine;
import com.philia093.neofactory.machine.MachineInventory;
import com.philia093.neofactory.machine.MachineMenu;
import com.philia093.neofactory.machine.MachineScreen;
import com.philia093.neofactory.machine.MachineTank;
import com.philia093.neofactory.machine.ProgressKind;
import com.philia093.neofactory.machine.SimpleEnergyStorage;
import com.philia093.neofactory.machine.SlotKind;
import com.philia093.neofactory.machine.SmeltingMachine;
import com.philia093.neofactory.render.BlockTextureCache;
import com.philia093.neofactory.render.PixelFont;
import com.philia093.neofactory.support.TestIcons;
import com.philia093.neofactory.support.TestRegistries;
import com.philia093.neofactory.util.Constants;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Paints the screen of a machine the way the game draws it and writes the result to a
 * picture.
 * <p>
 * The test builds the very layout the screen uses, {@link MachineMenu}, and paints it with
 * the very pictures the game uses: the panel and the slots of {@code gui/machine_icons.png`,
 * the arrow of {@code gui/inventory_icons.png}, a block item folded into the cube of
 * {@link BlockIconFactory} and the line that reports the fuel.
 * <p>
 * The result lies in {@code core/build/reports/machine-preview.png} and is meant to be
 * looked at: it is how the layout of a machine screen is reviewed without starting the
 * game. The test also pins down that the slots of the player inventory land on the rows the
 * panel of a machine carries - if they drifted, the bevel of the picture and the click of
 * the player would no longer be the same place.
 */
class MachinePreviewTest {

    /** Assets of the project, the tests run inside the core module. */
    private static final Path ASSETS = Path.of("..", "assets");

    /** The sheet holding the panel of a machine, its slot, its arrows and its icons. */
    private static final Path MACHINE_SHEET = ASSETS.resolve(MachineTextures.SHEET + ".png");

    /** Picture the preview is written to. */
    private static final Path PREVIEW = Path.of("build", "reports", "machine-preview.png");

    /** Picture of a machine that holds everything the layout can carry. */
    private static final Path FULL_PREVIEW =
            Path.of("build", "reports", "machine-full-preview.png");

    /** Colour the preview is filled with, a dark grey that shows the frame. */
    private static final int BACKDROP = 0xFF202020;

    /** The sheet of the bitmap font the game draws with. */
    private static final Path FONT_SHEET = ASSETS.resolve(PixelFont.ASCII_PAGE + ".png");

    /** Side of one glyph of the font in pixels. */
    private static final int FONT_GLYPH = PixelFont.ASCII_CELL_SIZE;

    /** Amount of glyphs a row of the font sheet holds. */
    private static final int FONT_COLUMNS = 16;

    /** Pixels added behind a glyph, so two characters do not touch. */
    private static final int FONT_SPACING = 1;

    /** Advance of a space, the cell of a space holds no pixels. */
    private static final int FONT_SPACE_ADVANCE = 4;

    /** Pixels with a lower alpha count as invisible, like the font does it. */
    private static final int ALPHA_MIN = 8;

    /** Colour of the shadow behind a line of text, the dark edge that makes it read. */
    private static final int SHADOW = 0xFF404040;

    /** Space around the panel. */
    private static final int MARGIN = 8;

    /** Share of the arrow that is filled in the preview. */
    private static final float CRAFT_PROGRESS = 0.5f;

    /** Seconds of fuel the preview reports. */
    private static final int FUEL_SECONDS = 42;

    @BeforeAll
    static void registerGameData() {
        TestRegistries.ensure();
    }

    @Test
    void theMachineScreenIsPaintedAsTheGameDrawsIt() throws IOException {
        SmeltingMachine furnace = new SmeltingMachine();
        MachineMenu menu = new MachineMenu(furnace, new PlayerInventory());
        ContainerLayout layout = menu.container().layout();

        assertEquals(MachineTextures.WIDTH, layout.panelWidth(), "the panel of the art");
        assertEquals(MachineTextures.HEIGHT, layout.panelHeight(), "the panel of the art");

        BufferedImage machine = read(MACHINE_SHEET);
        BufferedImage picture = new BufferedImage(
                MachineTextures.WIDTH + 2 * MARGIN, MachineTextures.HEIGHT + 2 * MARGIN,
                BufferedImage.TYPE_INT_ARGB);
        fill(picture, BACKDROP);

        // The panel, exactly as large as the art, so nothing is stretched here.
        copy(machine, picture, 0, 0, MachineTextures.WIDTH, MachineTextures.HEIGHT, MARGIN, MARGIN);

        drawSlots(picture, machine, layout);
        drawContents(picture, layout);
        drawArrow(picture, machine, CRAFT_PROGRESS);
        BufferedImage font = read(FONT_SHEET);
        drawTitle(picture, font, menu.title());
        drawStatus(picture, font, "Remain fuel: " + FUEL_SECONDS + "s");

        // The slots of the player inventory sit on the rows the panel carries: the picture
        // and the sheet hold the same pixel where such a slot begins.
        assertEquals(machine.getRGB(MachineMenu.PLAYER_LEFT, MachineMenu.PLAYER_STORAGE_TOP),
                picture.getRGB(MARGIN + MachineMenu.PLAYER_LEFT,
                        MARGIN + MachineMenu.PLAYER_STORAGE_TOP),
                "the inventory of the player no longer lands on its panel");

        ImageIO.write(picture, "png", PREVIEW.toFile());
        assertTrue(PREVIEW.toFile().isFile(), "the preview was not written");
    }

    /**
     * Paints a machine that holds everything the layout can carry and checks that nothing
     * lands on top of anything else.
     * <p>
     * This is the picture to look at when the layout of a machine changes: six slots in, six
     * slots out, four upgrades, two tanks each way and the configure slot, all of it on one
     * panel. The test fails as soon as two cells share a pixel, so the picture and the rule
     * are checked together.
     */
    @Test
    void aMachineWithEverythingItCanHoldIsPaintedWithoutOverlap() throws IOException {
        MachineMenu menu = new MachineMenu(fullMachine(), new PlayerInventory());
        ContainerLayout layout = menu.container().layout();

        assertEquals(MachineTextures.WIDTH, layout.panelWidth());
        assertEquals(MachineTextures.HEIGHT, layout.panelHeight());

        BufferedImage machine = read(MACHINE_SHEET);
        BufferedImage font = read(FONT_SHEET);
        BufferedImage picture = new BufferedImage(
                MachineTextures.WIDTH + 2 * MARGIN, MachineTextures.HEIGHT + 2 * MARGIN,
                BufferedImage.TYPE_INT_ARGB);
        fill(picture, BACKDROP);
        copy(machine, picture, 0, 0, MachineTextures.WIDTH, MachineTextures.HEIGHT, MARGIN, MARGIN);
        drawSlots(picture, machine, layout);
        drawFluidSlots(picture, machine, menu, layout);
        drawArrow(picture, machine, CRAFT_PROGRESS);
        drawTitle(picture, font, menu.title());
        drawStatus(picture, font, "Remain fuel: " + FUEL_SECONDS + "s");

        ImageIO.write(picture, "png", FULL_PREVIEW.toFile());
        assertTrue(FULL_PREVIEW.toFile().isFile(), "the preview was not written");
    }

    /** Draws the tanks of the foot of the panel. */
    private static void drawFluidSlots(BufferedImage picture, BufferedImage machine,
            MachineMenu menu, ContainerLayout layout) {
        for (MachineMenu.FluidSlot tank : menu.fluidSlots()) {
            SlotKind kind = tank.input() ? SlotKind.FLUID_INPUT : SlotKind.FLUID_OUTPUT;
            copy(machine, picture, MachineTextures.iconX(kind.column()),
                    MachineTextures.iconY(kind.row()), MachineTextures.ICON_CELL,
                    MachineTextures.ICON_CELL, MARGIN + tank.x() - PanelTextures.SLOT_BEVEL,
                    MARGIN + tank.y() - PanelTextures.SLOT_BEVEL);
        }
    }

    /** A machine that holds everything the layout can carry. */
    private static Machine fullMachine() {
        List<MachineInventory.Role> roles = new ArrayList<>();
        List<SlotKind> inKinds = new ArrayList<>();
        List<SlotKind> outKinds = new ArrayList<>();
        for (int slot = 0; slot < 6; slot++) {
            roles.add(MachineInventory.Role.INPUT);
            inKinds.add(SlotKind.SMELTING);
            roles.add(MachineInventory.Role.OUTPUT);
            outKinds.add(SlotKind.GENERIC);
        }
        for (int slot = 0; slot < MachineMenu.MAX_UPGRADES; slot++) {
            roles.add(MachineInventory.Role.UPGRADE);
        }
        roles.add(MachineInventory.Role.CONFIGURE);
        MachineScreen screen = new MachineScreen("Full machine", ProgressKind.CHEMICAL, inKinds,
                outKinds, 2, 2, true);
        return new Machine(screen,
                new MachineInventory(roles.toArray(new MachineInventory.Role[0])),
                new SimpleEnergyStorage(1000), List.of(), MachineTank.of(1000, MachineTank.Role.INPUT),
                MachineTank.of(1000, MachineTank.Role.INPUT), MachineTank.of(1000, MachineTank.Role.OUTPUT),
                MachineTank.of(1000, MachineTank.Role.OUTPUT)) {
            @Override
            protected void update(float delta) {
                // A machine that does nothing has no work of its own.
            }
        };
    }

    /** Draws the bevel of every slot of the layout. */
    private static void drawSlots(BufferedImage picture, BufferedImage machine,
            ContainerLayout layout) {
        int slotX = MachineTextures.iconX(MachineTextures.SLOT_COLUMN);
        int slotY = MachineTextures.iconY(MachineTextures.SLOT_ROW);
        int size = MachineTextures.ICON_CELL;
        for (Slot slot : layout.slots()) {
            copy(machine, picture, slotX, slotY, size, size,
                    MARGIN + slot.x() - PanelTextures.SLOT_BEVEL,
                    MARGIN + slot.y() - PanelTextures.SLOT_BEVEL);
        }
    }

    /** Draws the items the preview shows in the slots of the machine and of the player. */
    private static void drawContents(BufferedImage picture, ContainerLayout layout) {
        drawStack(picture, layout, SmeltingMachine.INPUT, ItemStack.of(Items.IRON_ORE, 8));
        drawStack(picture, layout, SmeltingMachine.FUEL, ItemStack.of(Items.COAL, 12));
        drawStack(picture, layout, SmeltingMachine.OUTPUT, ItemStack.of(Items.IRON_INGOT, 4));
        drawStack(picture, layout, 3, ItemStack.of(Items.FURNACE, 1));
        drawStack(picture, layout, 4, ItemStack.of(Items.STONE, 64));
    }

    /** Draws one stack into a slot of the layout. */
    private static void drawStack(BufferedImage picture, ContainerLayout layout, int index,
            ItemStack stack) {
        Slot slot = layout.slots().get(index);
        drawItem(picture, MARGIN + slot.x(), MARGIN + slot.y(), stack);
    }

    /** Draws the arrow of the machine, filled with the progress of the craft. */
    private static void drawArrow(BufferedImage picture, BufferedImage machine, float progress) {
        ProgressKind kind = SmeltingMachine.SCREEN.progress();
        int cell = MachineTextures.ICON_CELL;
        int arrowX = MARGIN + MachineMenu.ARROW_X;
        int arrowY = MARGIN + MachineMenu.ARROW_Y;
        copy(machine, picture, MachineTextures.iconX(kind.emptyColumn()),
                MachineTextures.iconY(kind.emptyRow()), cell, cell, arrowX, arrowY);
        int filled = Math.round(progress * cell);
        copy(machine, picture, MachineTextures.iconX(kind.fullColumn()),
                MachineTextures.iconY(kind.fullRow()), filled, cell, arrowX, arrowY);
    }

    /** Writes the name of the machine in the upper left corner of the panel. */
    private static void drawTitle(BufferedImage picture, BufferedImage font, String text) {
        drawText(picture, font, text, MARGIN + MachineMenu.TEXT_LEFT, MARGIN + MachineMenu.TEXT_TOP);
    }

    /** Writes what the machine reports in the upper right corner of the panel. */
    private static void drawStatus(BufferedImage picture, BufferedImage font, String text) {
        int width = textWidth(font, text);
        drawText(picture, font, text, MARGIN + MachineMenu.TEXT_RIGHT - width,
                MARGIN + MachineMenu.TEXT_TOP);
    }

    /** Width a line takes in pixels of the interface. */
    private static int textWidth(BufferedImage font, String text) {
        int width = 0;
        for (char character : text.toCharArray()) {
            width += advance(font, character) + FONT_SPACING;
        }
        return width;
    }

    /**
     * Writes a line the way the screen does: the bitmap of the game, white with a dark shadow
     * behind it.
     * <p>
     * The shadow is the dark edge that makes the text read on the light panel, see the colour
     * of {@code MachineGui}. Drawing the very bitmap of the font here is what makes this
     * preview tell whether the text of the screen is legible.
     *
     * @param picture picture to draw into
     * @param font the sheet of the font, {@code font/ascii.png}
     * @param text line to write
     * @param x left edge of the line
     * @param y upper edge of the line
     */
    private static void drawText(BufferedImage picture, BufferedImage font, String text, int x,
            int y) {
        int pen = x;
        for (char character : text.toCharArray()) {
            int cell = character & 0xFF;
            int cellX = (cell % FONT_COLUMNS) * FONT_GLYPH;
            int cellY = (cell / FONT_COLUMNS) * FONT_GLYPH;
            blitGlyph(picture, font, cellX, cellY, pen + 1, y + 1, SHADOW);
            blitGlyph(picture, font, cellX, cellY, pen, y, 0xFFFFFFFF);
            pen += advance(font, character) + FONT_SPACING;
        }
    }

    /** Copies one glyph of the sheet into the picture in a single colour. */
    private static void blitGlyph(BufferedImage picture, BufferedImage font, int cellX, int cellY,
            int x, int y, int colour) {
        for (int row = 0; row < FONT_GLYPH; row++) {
            for (int column = 0; column < FONT_GLYPH; column++) {
                if (((font.getRGB(cellX + column, cellY + row) >>> 24) & 0xFF) < ALPHA_MIN) {
                    continue;
                }
                int targetX = x + column;
                int targetY = y + row;
                if (targetX < 0 || targetY < 0 || targetX >= picture.getWidth()
                        || targetY >= picture.getHeight()) {
                    continue;
                }
                picture.setRGB(targetX, targetY, colour);
            }
        }
    }

    /** Pixels a glyph of the sheet advances, measured by scanning it like the font does. */
    private static int advance(BufferedImage font, char character) {
        int cell = character & 0xFF;
        int cellX = (cell % FONT_COLUMNS) * FONT_GLYPH;
        int cellY = (cell / FONT_COLUMNS) * FONT_GLYPH;
        int right = 0;
        for (int row = 0; row < FONT_GLYPH; row++) {
            for (int column = 0; column < FONT_GLYPH; column++) {
                if (((font.getRGB(cellX + column, cellY + row) >>> 24) & 0xFF) > ALPHA_MIN) {
                    right = Math.max(right, column + 1);
                }
            }
        }
        return right == 0 ? FONT_SPACE_ADVANCE : right;
    }

    /** Reads a picture of the assets. */
    private static BufferedImage read(Path file) throws IOException {
        BufferedImage picture = ImageIO.read(file.toFile());
        if (picture == null) {
            throw new IllegalStateException("Unable to read " + file);
        }
        return picture;
    }

    /** Fills a whole picture with one colour. */
    private static void fill(BufferedImage image, int argb) {
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                image.setRGB(x, y, argb);
            }
        }
    }

    /** Copies a rectangle of a source into a target without changing its size. */
    private static void copy(BufferedImage source, BufferedImage target, int sourceX, int sourceY,
            int sourceWidth, int sourceHeight, int targetX, int targetY) {
        stretch(source, target, sourceX, sourceY, sourceWidth, sourceHeight, targetX, targetY,
                sourceWidth, sourceHeight);
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

    /** RGBA8888 of the game to the ARGB8888 the picture writer expects. */
    private static int toArgb(int colour) {
        return ((colour & 0xFF) << 24) | ((colour >>> 8) & 0xFFFFFF);
    }
}
