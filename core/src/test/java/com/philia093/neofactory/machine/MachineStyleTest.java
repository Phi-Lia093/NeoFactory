package com.philia093.neofactory.machine;

import com.philia093.neofactory.gui.container.Slot;
import com.philia093.neofactory.gui.panel.MachineTextures;
import com.philia093.neofactory.item.PlayerInventory;
import com.philia093.neofactory.support.TestRegistries;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the two ages of the machine screens: the grey panel a machine of the electrical age is drawn in and
 * the bronze one of a machine that runs on steam.
 * <p>
 * What makes a machine of the bronze age is nothing but the pictures it names - the panel of its style, the
 * cells of its own column of slots and its own bars - so that is what is pinned down here. The last test opens
 * the very sheet of the game and checks that every cell the kinds name is drawn on it, which is what keeps a
 * coordinate that drifted from shipping as an invisible slot.
 */
class MachineStyleTest {

    /** The sheet holding the two panels, the slots, the bars and the alarms of a machine screen. */
    private static final Path SHEET = TestRegistries.ASSETS.resolve(MachineTextures.SHEET + ".png");

    @BeforeAll
    static void registerGameData() {
        TestRegistries.ensure();
    }

    @Test
    void aMachineThatNamesNoStyleIsDrawnWithTheGreyPanel() {
        MachineScreen screen = new MachineScreen("Test", ProgressKind.GENERIC,
                List.of(SlotKind.GENERIC), List.of(SlotKind.GENERIC), 0, 0, false);

        assertEquals(MachineStyle.NORMAL, screen.style(), "the age of electricity");
        assertEquals(0, MachineStyle.NORMAL.panelY(), "the grey panel is the upper one of the sheet");
        assertEquals(MachineTextures.HEIGHT, MachineStyle.BRONZE.panelY(),
                "the bronze panel lies right below it");
        assertEquals("normal", MachineStyle.NORMAL.toString());
        assertEquals("bronze", MachineStyle.BRONZE.toString());
    }

    @Test
    void everyMachineOfTheAgeOfSteamIsDrawnWithItsOwnPanelAndBar() {
        // The six machines hold the tank of steam they spend, and the boiler beside it the water it boils.
        assertScreenOfTheAgeOfSteam(new SteamFurnaceMachine(), ProgressKind.BRONZE, 1);
        assertScreenOfTheAgeOfSteam(new AlloyFurnaceMachine(), ProgressKind.BRONZE, 1);
        assertScreenOfTheAgeOfSteam(new GrinderMachine(), ProgressKind.BRONZE_GRINDER, 1);
        assertScreenOfTheAgeOfSteam(new CompressorMachine(), ProgressKind.BRONZE_COMPRESSOR, 1);
        assertScreenOfTheAgeOfSteam(new ExtractorMachine(), ProgressKind.BRONZE_EXTRACTOR, 1);
        assertScreenOfTheAgeOfSteam(new ForgeHammerMachine(), ProgressKind.BRONZE_HAMMER, 1);
        // The boiler is the machine the six of them are built around, and it carries the plain bronze bar.
        assertScreenOfTheAgeOfSteam(new SteamBoilerMachine(), ProgressKind.BRONZE, 2);
    }

    /** Checks the panel, the bar and the tanks of one machine of the age of steam. */
    private static void assertScreenOfTheAgeOfSteam(Machine machine, ProgressKind progress, int tanks) {
        MachineScreen screen = machine.screen();

        assertEquals(MachineStyle.BRONZE, screen.style(), machine.name() + " is a machine of steam");
        assertEquals(progress, screen.progress(), machine.name() + " draws the bar of its own work");
        assertEquals(1, screen.fluidInputs(), machine.name() + " shows the tank it is filled by");
        assertEquals(tanks, machine.tankCount(), machine.name() + " holds the tanks of its own work");
        for (int index = 0; index < machine.tankCount(); index++) {
            assertEquals(SteamMachine.STEAM_CAPACITY, machine.tank(index).storage().capacity(),
                    machine.name() + " holds tanks of sixteen thousand millibuckets");
        }
        // The layout refuses a screen that does not describe the slots of its machine, so building the menu
        // is what checks that the six of them agree about what they hold.
        new MachineMenu(machine, new PlayerInventory());
    }

    @Test
    void theSlotsOfTheAgeOfSteamAreDrawnFromItsOwnColumnOfTheSheet() {
        // The flame is the one kind the two ages share: a furnace of the bronze age burns with its own flame.
        assertEquals(0, SlotKind.SMELTING.column(MachineStyle.NORMAL));
        assertEquals(1, SlotKind.SMELTING.row(MachineStyle.NORMAL));
        assertEquals(5, SlotKind.SMELTING.column(MachineStyle.BRONZE));
        assertEquals(3, SlotKind.SMELTING.row(MachineStyle.BRONZE));

        for (SlotKind kind : List.of(SlotKind.GRINDER_INPUT, SlotKind.GRINDER_OUTPUT,
                SlotKind.HAMMER_INPUT, SlotKind.EXTRACTOR_INPUT, SlotKind.COMPRESSOR_INPUT)) {
            assertTrue(kind.isBronze(), kind + " is a slot the age of steam brought");
            assertEquals(5, kind.column(MachineStyle.BRONZE), kind + " stands in the bronze column");
            assertEquals(kind.column(), kind.column(MachineStyle.NORMAL),
                    kind + " is drawn from the same cell in either age");
        }
    }

    @Test
    void theTanksOfAMachineDoNotChangeWithItsStyle() {
        // The steam of a bronze machine stands in the very cell the water of an electrical one stands in.
        for (SlotKind kind : List.of(SlotKind.FLUID_INPUT, SlotKind.FLUID_OUTPUT)) {
            assertEquals(kind.column(), kind.column(MachineStyle.BRONZE), kind + " is the same cell");
            assertEquals(kind.row(), kind.row(MachineStyle.BRONZE), kind + " is the same cell");
            assertFalse(kind.isBronze(), kind + " belongs to both ages");
        }
    }

    @Test
    void thePlainSlotOfAMachineIsTheSlotOfItsOwnPanel() {
        MachineMenu boiler = new MachineMenu(new SteamBoilerMachine(), new PlayerInventory());

        Slot output = slotOfRole(boiler, Slot.Rule.OUTPUT);
        assertEquals(Slot.DEFAULT_ICON, output.iconColumn(),
                "a plain slot names no picture, so the screen draws the slot of the bronze panel");
        assertEquals(Slot.DEFAULT_ICON, output.iconRow());
        Slot fuel = slotOfRole(boiler, Slot.Rule.NORMAL);
        assertEquals(SlotKind.SMELTING.column(MachineStyle.BRONZE), fuel.iconColumn(),
                "the fuel of the boiler is the flame of the bronze age");
        assertEquals(SlotKind.SMELTING.row(MachineStyle.BRONZE), fuel.iconRow());
        // The slot of a panel is one cell of the icons and stands inside the panel it belongs to.
        assertTrue(MachineStyle.BRONZE.slotX() < MachineTextures.WIDTH);
        assertTrue(MachineStyle.BRONZE.slotY() > MachineStyle.BRONZE.panelY(),
                "the slot of the bronze panel lies inside the bronze panel");
        assertTrue(MachineStyle.BRONZE.slotY() + MachineTextures.ICON_CELL
                <= MachineStyle.BRONZE.panelY() + MachineTextures.HEIGHT);
    }

    @Test
    void theBarOfTheForgeHammerStandsOnItsSide() {
        assertTrue(ProgressKind.BRONZE_HAMMER.vertical(), "the hammer falls, so its bar grows upwards");
        for (ProgressKind kind : ProgressKind.values()) {
            if (kind != ProgressKind.BRONZE_HAMMER) {
                assertFalse(kind.vertical(), kind + " grows to the right like every other bar");
            }
        }
        assertEquals(8, ProgressKind.BRONZE_HAMMER.fullColumn(), "the bright part of the bar");
        assertEquals(1, ProgressKind.BRONZE_HAMMER.fullRow());
    }

    @Test
    void theAlarmOfASteamMachineIsTheOneOfItsOwnColumn() {
        assertEquals(6, MachineError.NO_STEAM.column(), "the alarm of the age of steam");
        assertEquals(0, MachineError.NO_STEAM.row());
        assertEquals(MachineError.NO_STEAM.column(), MachineError.NO_EXHAUST.column(),
                "a machine that cannot vent reports the very same alarm");
        assertEquals(MachineError.NO_STEAM.row(), MachineError.NO_EXHAUST.row());
    }

    @Test
    void everyCellAScreenNamesIsDrawnOnTheSheet() throws IOException {
        assertTrue(Files.exists(SHEET), "the sheet of the machine screens: " + SHEET);
        BufferedImage sheet = ImageIO.read(SHEET.toFile());

        for (MachineStyle style : MachineStyle.values()) {
            assertPainted(sheet, 0, style.panelY(), MachineTextures.WIDTH, MachineTextures.HEIGHT,
                    "the panel of " + style);
            assertPainted(sheet, style.slotX(), style.slotY(), MachineTextures.ICON_CELL,
                    MachineTextures.ICON_CELL, "the plain slot of " + style);
        }
        for (SlotKind kind : SlotKind.values()) {
            if (kind == SlotKind.GENERIC) {
                // The plain kind is no cell of the grid but the slot of the panel, checked above.
                continue;
            }
            for (MachineStyle style : MachineStyle.values()) {
                assertPainted(sheet, MachineTextures.iconX(kind.column(style)),
                        MachineTextures.iconY(kind.row(style)), MachineTextures.ICON_CELL,
                        MachineTextures.ICON_CELL, kind + " as " + style + " draws it");
            }
        }
        for (ProgressKind kind : ProgressKind.values()) {
            assertPainted(sheet, MachineTextures.iconX(kind.fullColumn()),
                    MachineTextures.iconY(kind.fullRow()), MachineTextures.ICON_CELL,
                    MachineTextures.ICON_CELL, "the bright part of " + kind);
            if (!kind.vertical()) {
                assertPainted(sheet, MachineTextures.iconX(kind.emptyColumn()),
                        MachineTextures.iconY(kind.emptyRow()), MachineTextures.ICON_CELL,
                        MachineTextures.ICON_CELL, "the track of " + kind);
            }
        }
        for (MachineError error : MachineError.values()) {
            if (!error.isError() || error == MachineError.NO_WATER) {
                // The alarm of a boiler that ran dry is the one cell of the alarms of the sheet without a
                // picture, so a screen shows nothing there; it is left out of this check on purpose, see
                // MachineError#NO_WATER.
                continue;
            }
            assertPainted(sheet, MachineTextures.iconX(error.column()),
                    MachineTextures.iconY(error.row()), MachineTextures.ICON_CELL,
                    MachineTextures.ICON_CELL, "the alarm " + error);
        }
        // The heavy track of the tall bar of the hammer stands beyond the grid, see MachineTextures.
        assertPainted(sheet, MachineTextures.iconX(MachineTextures.ICON_COLUMNS),
                MachineTextures.iconY(0), 20, 24, "the heavy track of the hammer");
    }

    /** The one slot of a menu that carries a rule, which is where a machine takes and leaves its items. */
    private static Slot slotOfRole(MachineMenu menu, Slot.Rule rule) {
        for (Slot slot : menu.container().layout().slots()) {
            if (slot.rule() == rule) {
                return slot;
            }
        }
        throw new AssertionError("The menu of " + menu.machine().name() + " holds no slot of " + rule);
    }

    /** Checks that a rectangle of the sheet holds at least one pixel that is not transparent. */
    private static void assertPainted(BufferedImage sheet, int x, int y, int width, int height,
            String what) {
        assertTrue(x >= 0 && y >= 0 && x + width <= sheet.getWidth() && y + height <= sheet.getHeight(),
                what + " lies on the sheet");
        boolean painted = false;
        for (int row = y; row < y + height && !painted; row++) {
            for (int column = x; column < x + width; column++) {
                if ((sheet.getRGB(column, row) >>> 24) != 0) {
                    painted = true;
                    break;
                }
            }
        }
        assertTrue(painted, what + " is drawn at " + x + "," + y);
    }
}
