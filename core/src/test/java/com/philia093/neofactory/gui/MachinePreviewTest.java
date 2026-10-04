package com.philia093.neofactory.gui;

import com.philia093.neofactory.cable.Voltage;
import com.philia093.neofactory.fluid.Fluids;
import com.philia093.neofactory.gui.container.ContainerLayout;
import com.philia093.neofactory.material.Materials;
import com.philia093.neofactory.gui.container.Slot;
import com.philia093.neofactory.gui.panel.MachineTextures;
import com.philia093.neofactory.gui.panel.PanelTextures;
import com.philia093.neofactory.item.Batteries;
import com.philia093.neofactory.item.BatteryChemistry;
import com.philia093.neofactory.item.Item;
import com.philia093.neofactory.item.ItemRegistry;
import com.philia093.neofactory.item.ItemStack;
import com.philia093.neofactory.item.Items;
import com.philia093.neofactory.item.PlayerInventory;
import com.philia093.neofactory.machine.BatteryBoxMachine;
import com.philia093.neofactory.machine.ElectricMaceratorMachine;
import com.philia093.neofactory.machine.Machine;
import com.philia093.neofactory.machine.MachineError;
import com.philia093.neofactory.machine.MachineInventory;
import com.philia093.neofactory.machine.MachineMenu;
import com.philia093.neofactory.machine.MachineScreen;
import com.philia093.neofactory.machine.MachineTank;
import com.philia093.neofactory.machine.ProgressKind;
import com.philia093.neofactory.machine.SimpleEnergyStorage;
import com.philia093.neofactory.machine.SlotKind;
import com.philia093.neofactory.machine.SmeltingMachine;
import com.philia093.neofactory.machine.SteamTurbineMachine;
import com.philia093.neofactory.machine.TurbineTier;
import com.philia093.neofactory.recipe.RecipeLoader;
import com.philia093.neofactory.recipe.RecipeRegistry;
import com.philia093.neofactory.recipe.RecipeType;
import com.philia093.neofactory.render.BlockTextureCache;
import com.philia093.neofactory.render.PixelFont;
import com.philia093.neofactory.support.TestIcons;
import com.philia093.neofactory.support.TestRegistries;
import com.philia093.neofactory.util.Constants;
import com.philia093.neofactory.world.TickClock;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

    /** Picture of the panel of a generator, whose cell of energy wears the icon of a battery. */
    private static final Path GENERATOR_PREVIEW =
            Path.of("build", "reports", "machine-generator-preview.png");

    /** Picture of the panel of a machine of the line of the power, which has a bar and an alarm. */
    private static final Path LINE_PREVIEW =
            Path.of("build", "reports", "machine-line-preview.png");

    /** Picture of the four panels of the boxes of cells, one per size. */
    private static final Path BOX_PREVIEW =
            Path.of("build", "reports", "machine-box-preview.png");

    /** Colour the fill of the cell of energy is painted in, the red of the screen of a machine. */
    private static final int ENERGY_FILL = 0xFFB81F1F;

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
        drawEnergyCell(picture, machine, menu, layout);
        BufferedImage font = read(FONT_SHEET);
        drawInfo(picture);
        drawFlame(picture, menu);

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
        drawEnergyCell(picture, machine, menu, layout);
        drawArrow(picture, machine, CRAFT_PROGRESS);
        drawInfo(picture);
        drawFlame(picture, menu);

        ImageIO.write(picture, "png", FULL_PREVIEW.toFile());
        assertTrue(FULL_PREVIEW.toFile().isFile(), "the preview was not written");
    }

    /**
     * Paints the panel of a generator, the one machine whose cell of energy wears the icon of a battery.
     * <p>
     * A machine that makes power has no slot and no bar, so what a player reads its power on is the cell at the
     * foot of its panel: the test pins down that this cell wears the icon of
     * {@link SlotKind#BATTERY} - the very cell a battery is put into in the screens of the original game - and
     * writes the whole panel to {@code core/build/reports/machine-generator-preview.png}, which is the picture
     * to look at when the screen of a generator changes.
     */
    @Test
    void theCellOfAGeneratorIsPaintedAsTheGameDrawsIt() throws IOException {
        SteamTurbineMachine turbine = new SteamTurbineMachine(TurbineTier.LV);
        turbine.steam().fill(Fluids.STEAM, TurbineTier.LV.steamPerTick(), false);
        turbine.tick(TickClock.TICK_SECONDS);
        MachineMenu menu = new MachineMenu(turbine, new PlayerInventory());
        ContainerLayout layout = menu.container().layout();

        assertEquals(ProgressKind.NONE, menu.progressKind(), "a generator has no bar");
        assertEquals(SlotKind.BATTERY, menu.energySlotKind(),
                "so its power is read on the cell of the electricity");

        BufferedImage machine = read(MACHINE_SHEET);
        BufferedImage picture = new BufferedImage(
                MachineTextures.WIDTH + 2 * MARGIN, MachineTextures.HEIGHT + 2 * MARGIN,
                BufferedImage.TYPE_INT_ARGB);
        fill(picture, BACKDROP);
        copy(machine, picture, 0, 0, MachineTextures.WIDTH, MachineTextures.HEIGHT, MARGIN, MARGIN);
        drawSlots(picture, machine, layout);
        drawFluidSlots(picture, machine, menu, layout);
        drawEnergyCell(picture, machine, menu, layout);
        drawInfo(picture);
        drawFlame(picture, menu);

        ImageIO.write(picture, "png", GENERATOR_PREVIEW.toFile());
        assertTrue(GENERATOR_PREVIEW.toFile().isFile(), "the preview was not written");
    }

    /**
     * Paints the panel of a machine of the line of the power, the age a workshop is played in once the first
     * generator turns.
     * <p>
     * A machine of the line is a machine of recipes: it fills the bar of its craft, it reads its buffer on the
     * plain cell of energy at the foot of the panel, and a machine that has work and nothing to run it with
     * shows the red alarm of the fourth column of the icons. The test writes the whole panel to
     * {@code core/build/reports/machine-line-preview.png} - the picture to look at when the screen of a machine
     * of that age changes - and pins the three of them down, see {@code ElectricMachine} and
     * {@code MachineFamilies}.
     */
    @Test
    void thePanelOfAMachineOfTheLineIsPaintedAsTheGameDrawsIt() throws IOException {
        RecipeRegistry.register(RecipeLoader.parse(RecipeType.GRINDING, "preview_dust",
                "{ \"ingredient\": \"iron_ore\", \"result\": { \"item\": \"iron_dust\" },"
                        + " \"time\": 8.0, \"power\": 1, \"voltage\": 8 }"));
        ElectricMaceratorMachine macerator = new ElectricMaceratorMachine(Voltage.LOW);
        macerator.inventory().set(ElectricMaceratorMachine.INPUT, ItemStack.of(Items.IRON_ORE, 1));
        MachineMenu menu = new MachineMenu(macerator, new PlayerInventory());
        ContainerLayout layout = menu.container().layout();

        assertEquals(ProgressKind.GENERIC, menu.progressKind(), "a machine of the line fills the bar");
        assertEquals(SlotKind.BATTERY, menu.energySlotKind(),
                "and is fed through the cell of energy at the foot of its panel");
        assertTrue(menu.hasEnergySlot(), "which is a slot and not a picture of how full its buffer is");
        assertEquals(macerator.reagentSlot(), menu.energySlot(), "the cell the reagent is put into");
        // The cell of energy of a machine of the line is where the reagent goes, so the preview shows a
        // handful of redstone dust in it, see Reagents.
        ItemStack reagent = ItemStack.of(Items.REDSTONE_DUST, 4);
        macerator.inventory().set(macerator.reagentSlot(), reagent);
        assertEquals(MachineError.NO_POWER, menu.error(),
                "an input that waits for the line reports the alarm of a machine without power");

        BufferedImage machine = read(MACHINE_SHEET);
        BufferedImage picture = new BufferedImage(
                MachineTextures.WIDTH + 2 * MARGIN, MachineTextures.HEIGHT + 2 * MARGIN,
                BufferedImage.TYPE_INT_ARGB);
        fill(picture, BACKDROP);
        copy(machine, picture, 0, 0, MachineTextures.WIDTH, MachineTextures.HEIGHT, MARGIN, MARGIN);
        drawSlots(picture, machine, layout);
        drawContents(picture, layout);
        drawStack(picture, layout, macerator.reagentSlot(), reagent);
        drawArrow(picture, machine, CRAFT_PROGRESS);
        drawEnergyCell(picture, machine, menu, layout);
        drawErrorMark(picture, machine, menu, layout);
        drawInfo(picture);

        ImageIO.write(picture, "png", LINE_PREVIEW.toFile());
        assertTrue(LINE_PREVIEW.toFile().isFile(), "the preview was not written");
    }

    /**
     * Paints the four panels of the boxes of cells, the machines of the industry that hold energy and nothing
     * else.
     * <p>
     * A box works no recipe, so its panel carries no bar, no tank and no cell of energy: what is left is a grid
     * of plain slots - one, four, nine or sixteen of them, the same square a box is built for - and the mark of
     * the upper left corner, which names the machine and what it holds. The four panels stand side by side in
     * {@code core/build/reports/machine-box-preview.png}, each with a charged cell in its first slot, and the
     * test pins down what the screen of a box says: the grid is as large as the box, the cell of energy is gone
     * and the buffer is named by the mark, see {@link MachineMenu#gridTop(int)} and
     * {@link MachineMenu#infoTooltip()}.
     */
    @Test
    void thePanelsOfTheBoxesOfCellsArePaintedAsTheGameDrawsThem() throws IOException {
        BufferedImage sheet = read(MACHINE_SHEET);
        List<BufferedImage> panels = new ArrayList<>();
        for (int cells : MachineScreen.GRID_SHAPES) {
            panels.add(paintBoxPanel(sheet, cells));
        }

        int width = panels.size() * (MachineTextures.WIDTH + 2 * MARGIN);
        BufferedImage picture = new BufferedImage(width, MachineTextures.HEIGHT + 2 * MARGIN,
                BufferedImage.TYPE_INT_ARGB);
        fill(picture, BACKDROP);
        for (int index = 0; index < panels.size(); index++) {
            BufferedImage panel = panels.get(index);
            copy(panel, picture, 0, 0, panel.getWidth(), panel.getHeight(),
                    index * (MachineTextures.WIDTH + 2 * MARGIN), 0);
        }

        ImageIO.write(picture, "png", BOX_PREVIEW.toFile());
        assertTrue(BOX_PREVIEW.toFile().isFile(), "the preview was not written");
    }

    /** Paints one panel of a box of that many cells, the way a screen of the game draws it. */
    private static BufferedImage paintBoxPanel(BufferedImage machine, int cells) {
        BatteryBoxMachine box = new BatteryBoxMachine(Voltage.LOW, cells);
        ItemStack cell = ItemStack.of(
                ItemRegistry.byName(Batteries.itemNameOf(Batteries.of(BatteryChemistry.LITHIUM, Voltage.LOW))),
                1);
        box.inventory().set(0, cell);
        MachineMenu menu = new MachineMenu(box, new PlayerInventory());
        ContainerLayout layout = menu.container().layout();

        assertTrue(menu.hasGrid(), "the panel of a box is a grid of its cells");
        assertEquals(cells, menu.gridSlots());
        assertFalse(menu.hasEnergySlot(), "a box is fed by the cells a player puts in and not by dust");
        assertEquals(List.of(box.name(), MachineMenu.ENERGY,
                        box.bank().amount() + " / " + box.bank().capacity() + " " + MachineMenu.ENERGY_UNIT),
                menu.infoTooltip(), "and the mark of the upper left corner names what it holds");

        BufferedImage picture = new BufferedImage(MachineTextures.WIDTH + 2 * MARGIN,
                MachineTextures.HEIGHT + 2 * MARGIN, BufferedImage.TYPE_INT_ARGB);
        fill(picture, BACKDROP);
        copy(machine, picture, 0, 0, MachineTextures.WIDTH, MachineTextures.HEIGHT, MARGIN, MARGIN);
        drawSlots(picture, machine, layout);
        for (int index = 0; index < cells; index++) {
            // The cells of the picture are charged, so that a box of every size reads as what it holds: the
            // first one full, the second half spent and the rest spent to the last unit, see Battery.
            ItemStack shown = ItemStack.of(cell.item(), 1);
            int spent = index == 0 ? 0 : index == 1 ? cell.item().maxDamage() / 2 : cell.item().maxDamage();
            shown.setDamage(spent);
            drawStack(picture, layout, index, shown);
        }
        drawInfo(picture);
        return picture;
    }

    /**
     * Draws the cell of energy at the foot of the panel, the way {@code MachineGui#drawEnergy} does it.
     * <p>
     * <b>A machine of the line is fed through that cell, so it is a slot and the slots of the panel already
     * carry it</b> - what lies in it is drawn with the items of the machine, see {@link MachineMenu#energySlot}.
     * What is left here is the cell of a machine that may not be filled by hand: the icon
     * {@link MachineMenu#energySlotKind()} names, with the lower half filled with the colour of the energy, the
     * way a buffer that is half full is shown.
     */
    private static void drawEnergyCell(BufferedImage picture, BufferedImage machine, MachineMenu menu,
            ContainerLayout layout) {
        if (menu.hasEnergySlot()) {
            return;
        }
        SlotKind kind = menu.energySlotKind();
        int x = MARGIN + MachineMenu.ENERGY_X - PanelTextures.SLOT_BEVEL;
        int y = MARGIN + layout.panelHeight() - MachineMenu.FOOT_TOP - ContainerLayout.SLOT_SIZE
                - PanelTextures.SLOT_BEVEL;
        copy(machine, picture, MachineTextures.iconX(kind.column()), MachineTextures.iconY(kind.row()),
                MachineTextures.ICON_CELL, MachineTextures.ICON_CELL, x, y);
        int inside = ContainerLayout.SLOT_SIZE - 2;
        fillBox(picture, x + PanelTextures.SLOT_BEVEL + 1, y + PanelTextures.SLOT_BEVEL + 1, inside,
                inside / 2, ENERGY_FILL);
    }

    /**
     * Draws the alarm of the machine, the way {@code MachineGui#drawError} does it.
     * <p>
     * The mark is the cell {@link MachineMenu#error()} names of the column of errors of the icons - the red
     * alarm of a machine that has work and nothing to run it with - and it stands where the flame of a furnace
     * and of a boiler stands, see {@link MachineGui}.
     */
    private static void drawErrorMark(BufferedImage picture, BufferedImage machine, MachineMenu menu,
            ContainerLayout layout) {
        MachineError error = menu.error();
        if (!error.isError()) {
            return;
        }
        int x = MARGIN + MachineMenu.ERROR_LEFT;
        int y = MARGIN + layout.panelHeight() - MachineMenu.ERROR_TOP - MachineMenu.MARK_SIZE;
        copy(machine, picture, MachineTextures.iconX(error.column()),
                MachineTextures.iconY(error.row(menu.style())), MachineTextures.ICON_CELL,
                MachineTextures.ICON_CELL, x, y);
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
        // The panel of a machine carries two blocks of slots, two pairs of tanks and the cell of energy
        // between them: there is no column of upgrades and no corner to configure, see MachineMenu.
        MachineScreen screen = new MachineScreen("Full machine", ProgressKind.CHEMICAL, inKinds,
                outKinds, 2, 2, false);
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
        drawStack(picture, layout, SmeltingMachine.OUTPUT, ItemStack.of(Materials.IRON.ingot(), 4));
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

    /** Draws the mark of the pack in the upper left corner, which names the machine when it is asked. */
    private static void drawInfo(BufferedImage picture) {
        int fill = 0xFF8B8B8B;
        fillBox(picture, MARGIN + MachineMenu.INFO_LEFT, MARGIN + MachineMenu.INFO_TOP,
                MachineMenu.INFO_SIZE, MachineMenu.INFO_SIZE, fill);
    }

    /** Draws the flame under the slot a machine burns in, filled by the share the screen reports. */
    private static void drawFlame(BufferedImage picture, MachineMenu menu) {
        if (!menu.hasFlame()) {
            return;
        }
        Slot fuel = menu.fuelCell();
        int size = MachineTextures.ICON_CELL;
        int x = MARGIN + fuel.x() - MachineMenu.FLAME_LEFT_PIXELS;
        int y = MARGIN + fuel.y() + ContainerLayout.SLOT_SIZE + MachineMenu.FLAME_GAP
                - MachineMenu.FLAME_UP_PIXELS;
        int filled = Math.max(1, Math.round(size * menu.flameShare()));
        fillBox(picture, x, y + size - filled, size, filled, 0xFFD08020);
    }

    /** Paints one box of the picture, the band the label of a machine covers. */
    private static void fillBox(BufferedImage picture, int x, int y, int width, int height, int argb) {
        for (int row = 0; row < height; row++) {
            for (int column = 0; column < width; column++) {
                if (x + column >= 0 && x + column < picture.getWidth() && y + row >= 0
                        && y + row < picture.getHeight()) {
                    picture.setRGB(x + column, y + row, argb);
                }
            }
        }
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
