package com.philia093.neofactory.machine;

import com.philia093.neofactory.block.BlockFace;
import com.philia093.neofactory.fluid.Fluids;
import com.philia093.neofactory.gui.container.ContainerLayout;
import com.philia093.neofactory.item.PlayerInventory;
import com.philia093.neofactory.support.TestRegistries;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks what the screen of a machine says about the sides of its block, and how it walks them.
 * <p>
 * A machine is reached through one side of its block, and which side that is is read from the front of the
 * machine and not from the world: a player who turned a machine reads the flanks of the machine they are
 * looking at, see {@link MachineSides}. The screen is the second place those sides are set, the wrench in the
 * world being the first: the box of a tank names the side the tank is reached through while the modifier key
 * is held, and the wheel walks that side on, see
 * {@link MachineMenu#tankTooltip(MachineMenu.FluidSlot, boolean)} and
 * {@link MachineMenu#cycleTank(int, int, int)}.
 * <p>
 * Everything here needs no window: the menu is the whole logic of the screen, and the screen on top of it
 * only translates the mouse into a point of the panel, see {@code MachineGui#scrolled}.
 */
class MachineFaceInterfaceTest {

    /** Capacity of the tank of the machine that stands in for a machine of the age of power. */
    private static final int TANK_CAPACITY = 4000;

    /** Capacity of its buffer, which is what makes the tooltip of the cell of energy say something. */
    private static final int BUFFER_CAPACITY = 1000;

    @BeforeAll
    static void registerGameData() {
        TestRegistries.ensure();
    }

    @Test
    void theBoxOfATankNamesTheSideItIsReachedThrough() {
        SteamBoilerMachine boiler = new SteamBoilerMachine();
        MachineMenu menu = new MachineMenu(boiler, new PlayerInventory());

        // A player who holds no key reads what is in the tank, exactly as before.
        assertEquals(List.of(MachineMenu.EMPTY_TANK,
                        "0 / " + SteamBoilerMachine.WATER_CAPACITY + " " + MachineMenu.FLUID_UNIT),
                menu.tankTooltip(menu.fluidSlots().get(0)));

        // With the modifier key the side follows: the water lies on the right flank of a machine that looks
        // north and the steam on the left one, see FaceConfig#defaultSides.
        assertEquals(MachineMenu.FACING_PREFIX + MachineSides.RIGHT, sideOf(menu, 0));
        assertEquals(MachineMenu.FACING_PREFIX + MachineSides.LEFT, sideOf(menu, 1));

        boiler.water().fill(Fluids.WATER, 3200, false);
        assertEquals(List.of("Water",
                        "3200 / " + SteamBoilerMachine.WATER_CAPACITY + " " + MachineMenu.FLUID_UNIT,
                        MachineMenu.FACING_PREFIX + MachineSides.RIGHT),
                menu.tankTooltip(menu.fluidSlots().get(0), true));
    }

    @Test
    void aMachineThatWasTurnedNamesTheSidesOfItsNewFront() {
        SteamBoilerMachine boiler = new SteamBoilerMachine();
        // The wrench of the world turns the sides of the machine with the machine, see FaceConfig#turned.
        boiler.faces().turned(BlockFace.EAST);
        MachineMenu menu = new MachineMenu(boiler, new PlayerInventory(), BlockFace.EAST);

        // The water stood on the west, which lies behind a machine that looks east.
        assertEquals(MachineMenu.FACING_PREFIX + MachineSides.BACK, sideOf(menu, 0));
        // The steam stood on the east, which is the front a player turned towards: the front carries nothing,
        // so the tank is reached from nowhere until the player gives it another side.
        assertEquals(MachineMenu.FACING_PREFIX + MachineSides.NONE, sideOf(menu, 1));
    }

    @Test
    void theWheelWalksATankOfSteamPastItsVent() {
        // A machine of steam takes its steam in on the right flank and blows it out of the back, see
        // SteamMachine: the wheel of the tank of steam walks the free sides and steps over the vent.
        MachineMenu menu = new MachineMenu(new CompressorMachine(), new PlayerInventory());
        int x = middleX(menu, 0);
        int y = middleY(menu, 0);

        assertTrue(menu.cycleTank(x, y, 1));
        assertEquals(MachineMenu.FACING_PREFIX + MachineSides.NONE, sideOf(menu, 0),
                "a tank with no side is where the wheel starts");
        assertTrue(menu.cycleTank(x, y, 1));
        assertEquals(MachineMenu.FACING_PREFIX + MachineSides.UP, sideOf(menu, 0),
                "and the ceiling is the first side that is free, the vent of the back being taken");
        assertTrue(menu.cycleTank(x, y, 1));
        assertEquals(MachineMenu.FACING_PREFIX + MachineSides.DOWN, sideOf(menu, 0));
        assertTrue(menu.cycleTank(x, y, 1));
        assertEquals(MachineMenu.FACING_PREFIX + MachineSides.LEFT, sideOf(menu, 0),
                "the left flank carries nothing on a machine of steam");
        assertTrue(menu.cycleTank(x, y, 1));
        assertEquals(MachineMenu.FACING_PREFIX + MachineSides.RIGHT, sideOf(menu, 0),
                "until the wheel comes back to where it started");
        assertEquals(BlockFace.WEST, menu.machine().faces().faceOfTank(0));

        assertTrue(menu.cycleTank(x, y, -1), "and it walks the other way as well");
        assertEquals(MachineMenu.FACING_PREFIX + MachineSides.LEFT, sideOf(menu, 0));
        assertEquals(BlockFace.SOUTH, menu.machine().faces().exhaust(),
                "the vent of the steam still blows out of the back");
    }

    @Test
    void theWheelOfABoilerStepsOverTheFlankItsSteamLiesOn() {
        // A boiler makes steam and does not breathe it, so the back of the machine carries nothing and the
        // water may reach it; the flank the tank of steam stands on is what the wheel steps over.
        MachineMenu menu = boiler();
        int x = middleX(menu, 0);
        int y = middleY(menu, 0);

        assertTrue(menu.cycleTank(x, y, 1));
        assertEquals(MachineMenu.FACING_PREFIX + MachineSides.NONE, sideOf(menu, 0));
        assertTrue(menu.cycleTank(x, y, 1));
        assertEquals(MachineMenu.FACING_PREFIX + MachineSides.BACK, sideOf(menu, 0),
                "the back of a boiler carries nothing at all");
        assertTrue(menu.cycleTank(x, y, 1));
        assertEquals(MachineMenu.FACING_PREFIX + MachineSides.UP, sideOf(menu, 0));
        assertTrue(menu.cycleTank(x, y, 1));
        assertEquals(MachineMenu.FACING_PREFIX + MachineSides.DOWN, sideOf(menu, 0));
        assertTrue(menu.cycleTank(x, y, 1));
        assertEquals(MachineMenu.FACING_PREFIX + MachineSides.RIGHT, sideOf(menu, 0),
                "and the flank the steam lies on is stepped over");

        assertEquals(BlockFace.EAST, menu.machine().faces().faceOfTank(1),
                "the side of the steam was never walked over");
    }

    @Test
    void theWheelOfATankStepsOverThePlugsOfThePower() {
        // A machine that takes power in and gives power out, with the two plugs on the back and on the left
        // flank of a machine that looks north, see FaceConfig#defaultSides.
        MachineMenu menu = new MachineMenu(machine(BUFFER_CAPACITY, BUFFER_CAPACITY), new PlayerInventory());
        int x = middleX(menu, 0);
        int y = middleY(menu, 0);

        for (String expected : new String[] {MachineSides.NONE, MachineSides.UP, MachineSides.DOWN,
                MachineSides.RIGHT}) {
            assertTrue(menu.cycleTank(x, y, 1));
            assertEquals(MachineMenu.FACING_PREFIX + expected, sideOf(menu, 0),
                    "the plugs of the power are stepped over");
        }
        assertEquals(BlockFace.SOUTH, menu.machine().faces().energyIn(), "and they stay where they were");
        assertEquals(BlockFace.EAST, menu.machine().faces().energyOut());
    }

    @Test
    void theWheelOfThePanelBelongsToTanks() {
        MachineMenu menu = boiler();
        int x = middleX(menu, 0);
        int y = middleY(menu, 0);

        assertFalse(menu.cycleTank(x, y, 0), "a wheel that did not turn changes nothing");
        assertFalse(menu.cycleTank(MachineMenu.ENERGY_X, MachineMenu.FOOT_TOP, 1),
                "the cell of energy is no tank");
        assertFalse(menu.cycleTank(MachineMenu.PLAYER_LEFT, MachineMenu.PLAYER_STORAGE_TOP, 1),
                "and neither is an inventory slot");
        assertFalse(menu.cycleTank(x + ContainerLayout.SLOT_SIZE, y, 1), "beside a tank is not on it");
        assertEquals(BlockFace.WEST, menu.machine().faces().faceOfTank(0), "nothing was walked");
    }

    @Test
    void theBoxOfTheCellOfEnergyNamesThePlugsOfThePower() {
        MachineMenu menu = new MachineMenu(machine(BUFFER_CAPACITY, BUFFER_CAPACITY), new PlayerInventory());

        assertEquals(List.of(MachineMenu.ENERGY, "0 / " + BUFFER_CAPACITY + " " + MachineMenu.ENERGY_UNIT),
                menu.energyTooltip(false), "a player who holds no key reads what is stored");
        assertEquals(List.of(MachineMenu.ENERGY, "0 / " + BUFFER_CAPACITY + " " + MachineMenu.ENERGY_UNIT,
                        MachineMenu.POWER_IN_PREFIX + MachineSides.BACK,
                        MachineMenu.POWER_OUT_PREFIX + MachineSides.LEFT),
                menu.energyTooltip(true));
    }

    @Test
    void aMachineThatHoldsNoBufferSaysNothingAboutOne() {
        MachineMenu boiler = boiler();

        // A machine of the age of steam has no buffer, and a box about a plug it does not have would lie.
        assertTrue(boiler.energyTooltip(false).isEmpty());
        assertTrue(boiler.energyTooltip(true).isEmpty());
    }

    @Test
    void theSidesOfAMachineAreReadFromTheSideItLooksIn() {
        // A machine that does not look along the horizon has no flanks, so no name for its sides either.
        assertThrows(IllegalArgumentException.class,
                () -> new MachineMenu(new SteamBoilerMachine(), new PlayerInventory(), BlockFace.TOP));

        MachineMenu menu = boiler();
        assertEquals(BlockFace.NORTH, menu.facing(), "a machine nobody turned looks north");
        assertThrows(IllegalArgumentException.class, () -> menu.sideName(7),
                "and a tank the machine does not hold cannot be named");
    }

    /** The menu of a boiler that nobody turned. */
    private static MachineMenu boiler() {
        return new MachineMenu(new SteamBoilerMachine(), new PlayerInventory());
    }

    /** A machine that holds one tank a recipe drains and a buffer of energy with both of its plugs. */
    private static TestMachine machine(int receive, int extract) {
        return new TestMachine(new SimpleEnergyStorage(BUFFER_CAPACITY, receive, extract));
    }

    /** The third line of the box of a tank, which is the side it is reached through. */
    private static String sideOf(MachineMenu menu, int tank) {
        return menu.tankTooltip(menu.fluidSlots().get(tank), true).get(2);
    }

    /** X coordinate of the middle of a tank of the panel. */
    private static int middleX(MachineMenu menu, int tank) {
        return menu.fluidSlots().get(tank).x() + ContainerLayout.SLOT_SIZE / 2;
    }

    /** Y coordinate of the middle of a tank of the panel. */
    private static int middleY(MachineMenu menu, int tank) {
        return menu.fluidSlots().get(tank).y() + ContainerLayout.SLOT_SIZE / 2;
    }

    /** A machine that holds one tank and a buffer of energy, and does no work of its own. */
    private static final class TestMachine extends Machine {

        private TestMachine(EnergyStorage energy) {
            super(new MachineScreen("Test", ProgressKind.GENERIC, List.of(SlotKind.SMELTING),
                            List.of(), 1, 0, false),
                    new MachineInventory(MachineInventory.Role.INPUT), energy, List.of(),
                    MachineTank.of(TANK_CAPACITY, MachineTank.Role.INPUT));
        }

        @Override
        protected void update(float delta) {
            // A machine that does nothing has no work of its own.
        }
    }
}
