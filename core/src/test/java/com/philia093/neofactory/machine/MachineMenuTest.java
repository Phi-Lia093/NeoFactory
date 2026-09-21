package com.philia093.neofactory.machine;

import com.philia093.neofactory.gui.container.ContainerLayout;
import com.philia093.neofactory.gui.container.Slot;
import com.philia093.neofactory.gui.panel.PanelTextures;
import com.philia093.neofactory.item.ItemStack;
import com.philia093.neofactory.item.Items;
import com.philia093.neofactory.item.PlayerInventory;
import com.philia093.neofactory.recipe.RecipeLoader;
import com.philia093.neofactory.recipe.RecipeRegistry;
import com.philia093.neofactory.recipe.RecipeType;
import com.philia093.neofactory.support.TestRegistries;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks where the screen of a machine puts its slots.
 * <p>
 * The panel of a machine is empty in its upper half and carries the slots of the player
 * inventory in its lower half, see
 * {@link com.philia093.neofactory.gui.panel.MachineTextures}. The slots a machine works with
 * therefore have to land above the inventory, and the inventory has to land exactly on the
 * rows its panel carries - otherwise the bevel of the picture and the click of the player
 * would drift apart.
 * <p>
 * The shapes are checked here as well: a block of one, two, four or six slots has a shape of
 * its own, see {@link MachineScreen#columns(int)}, and a machine that declares a screen its
 * inventory does not fill is refused while its screen is opened.
 */
class MachineMenuTest {

    @BeforeAll
    static void registerGameData() {
        TestRegistries.ensure();
    }

    @BeforeEach
    void setUp() {
        // A furnace only burns what it can smelt, so the recipes of the game are needed here.
        RecipeRegistry.clear();
        RecipeRegistry.register(RecipeLoader.parse(RecipeType.SMELTING, "iron_ingot",
                "{ \"ingredient\": \"iron_ore\", \"result\": { \"item\": \"iron_ingot\" } }"));
    }

    @Test
    void theSlotsOfTheFurnaceSitAboveTheInventory() {
        MachineMenu menu = new MachineMenu(new SmeltingMachine(), new PlayerInventory());
        ContainerLayout layout = menu.container().layout();
        Slot input = layout.slots().get(SmeltingMachine.INPUT);
        Slot fuel = layout.slots().get(SmeltingMachine.FUEL);
        Slot output = layout.slots().get(SmeltingMachine.OUTPUT);

        assertEquals(MachineMenu.INPUT_RIGHT, input.x());
        assertEquals(MachineMenu.MACHINE_TOP, input.y());
        assertEquals(input.x(), fuel.x(), "fuel stands in the column of the input");
        assertEquals(input.y() + ContainerLayout.SLOT_PITCH, fuel.y(), "fuel stands below it");
        assertEquals(MachineMenu.OUTPUT_LEFT, output.x(), "the product stands beside them");
        assertEquals(MachineMenu.MACHINE_TOP + ContainerLayout.SLOT_PITCH / 2, output.y(),
                "a block of one slot is centred on the two rows of the machine area");
        assertTrue(output.isOutput(), "the product can only be taken out");
        assertFalse(input.isOutput());
        assertFalse(fuel.isOutput(), "fuel is put in by the player");
    }

    @Test
    void theInventoryLandsOnTheRowsItsPanelCarries() {
        MachineMenu menu = new MachineMenu(new SmeltingMachine(), new PlayerInventory());
        ContainerLayout layout = menu.container().layout();
        List<Slot> slots = layout.slots();
        int firstPlayerSlot = 3;

        assertEquals(MachineMenu.WIDTH, layout.panelWidth(), "the panel of the sheet");
        assertEquals(MachineMenu.HEIGHT, layout.panelHeight(), "the panel of the sheet");
        assertEquals(3 + PlayerInventory.SLOT_COUNT, slots.size(), "the machine and the player");

        Slot storage = slots.get(firstPlayerSlot);
        assertEquals(MachineMenu.PLAYER_LEFT, storage.x());
        assertEquals(MachineMenu.PLAYER_STORAGE_TOP, storage.y());
        assertEquals(PlayerInventory.HOTBAR_SLOTS, storage.index(), "the storage comes first");

        Slot hotbar = slots.get(firstPlayerSlot + PlayerInventory.MAIN_SLOTS);
        assertEquals(MachineMenu.PLAYER_LEFT, hotbar.x());
        assertEquals(MachineMenu.PLAYER_HOTBAR_TOP, hotbar.y());
        assertEquals(0, hotbar.index(), "the hotbar comes last and starts at slot zero");
    }

    @Test
    void oneSlotStandsAlone() {
        MachineMenu menu = menu(1, 1, 0, 0, false);
        List<Slot> slots = menu.container().layout().slots();

        // Centred on the two rows of machine slots the empty half of the panel offers.
        assertEquals(MachineMenu.INPUT_RIGHT, slots.get(0).x());
        assertEquals(MachineMenu.MACHINE_TOP + ContainerLayout.SLOT_PITCH / 2, slots.get(0).y());
        assertEquals(MachineMenu.OUTPUT_LEFT, slots.get(1).x());
        assertEquals(slots.get(0).y(), slots.get(1).y(), "both stand on the same height");
    }

    @Test
    void twoSlotsStandAboveEachOther() {
        MachineMenu menu = menu(2, 2, 0, 0, false);
        List<Slot> slots = menu.container().layout().slots();

        assertEquals(MachineMenu.INPUT_RIGHT, slots.get(0).x());
        assertEquals(MachineMenu.MACHINE_TOP, slots.get(0).y());
        assertEquals(slots.get(0).x(), slots.get(1).x(), "one column");
        assertEquals(MachineMenu.MACHINE_TOP + ContainerLayout.SLOT_PITCH, slots.get(1).y());

        assertEquals(MachineMenu.OUTPUT_LEFT, slots.get(2).x());
        assertEquals(MachineMenu.OUTPUT_LEFT, slots.get(3).x());
        assertEquals(MachineMenu.MACHINE_TOP + ContainerLayout.SLOT_PITCH, slots.get(3).y());
    }

    @Test
    void fourSlotsFormASquare() {
        MachineMenu menu = menu(4, 4, 0, 0, false);
        List<Slot> slots = menu.container().layout().slots();
        int left = MachineMenu.INPUT_RIGHT - ContainerLayout.SLOT_PITCH;

        assertEquals(left, slots.get(0).x());
        assertEquals(MachineMenu.INPUT_RIGHT, slots.get(1).x(), "the first row is filled first");
        assertEquals(MachineMenu.MACHINE_TOP, slots.get(1).y());
        assertEquals(left, slots.get(2).x(), "the second row starts at the left again");
        assertEquals(MachineMenu.MACHINE_TOP + ContainerLayout.SLOT_PITCH, slots.get(2).y());
        assertEquals(MachineMenu.INPUT_RIGHT, slots.get(3).x());
    }

    @Test
    void sixSlotsFillTwoRowsOfThree() {
        MachineMenu menu = menu(6, 6, 0, 0, false);
        List<Slot> slots = menu.container().layout().slots();
        int left = MachineMenu.INPUT_RIGHT - 2 * ContainerLayout.SLOT_PITCH;
        int secondRow = MachineMenu.MACHINE_TOP + ContainerLayout.SLOT_PITCH;

        assertEquals(left, slots.get(0).x());
        assertEquals(left + ContainerLayout.SLOT_PITCH, slots.get(1).x());
        assertEquals(MachineMenu.INPUT_RIGHT, slots.get(2).x());
        assertEquals(left, slots.get(3).x(), "the fourth slot opens the second row");
        assertEquals(secondRow, slots.get(3).y());
        assertEquals(MachineMenu.OUTPUT_LEFT, slots.get(6).x(), "the products follow the inputs");
        assertEquals(MachineMenu.OUTPUT_LEFT + 2 * ContainerLayout.SLOT_PITCH, slots.get(8).x(),
                "the third column of the products");
        assertEquals(secondRow, slots.get(9).y());
    }

    @Test
    void theTanksStandAtTheFootAndTheConfigureSlotInTheCorner() {
        MachineMenu menu = menu(2, 1, 2, 2, true);
        List<MachineMenu.FluidSlot> tanks = menu.fluidSlots();

        assertEquals(4, tanks.size(), "two tanks in, two tanks out");
        assertEquals(MachineMenu.FOOT_LEFT, tanks.get(0).x(), "the input tanks start at the left");
        assertEquals(MachineMenu.FOOT_TOP, tanks.get(0).y());
        assertTrue(tanks.get(0).input());
        assertEquals(MachineMenu.FOOT_LEFT + ContainerLayout.SLOT_PITCH, tanks.get(1).x());
        assertEquals(MachineMenu.FOOT_LEFT + 2 * ContainerLayout.SLOT_PITCH + MachineMenu.TANK_GAP,
                tanks.get(2).x(), "the output tanks follow with a gap");
        assertFalse(tanks.get(2).input());
        assertEquals(2, tanks.get(2).tank(), "the first tank of the output side, behind both inputs");
        assertEquals(3, tanks.get(3).tank());

        assertTrue(menu.hasConfigureSlot(), "the corner was asked for");
        Slot configure = menu.container().layout().slots().get(3);
        assertEquals(MachineMenu.CONFIGURE_LEFT, configure.x());
        assertEquals(MachineMenu.FOOT_TOP, configure.y());
    }

    @Test
    void aMachineWithoutATankShowsNone() {
        MachineMenu menu = menu(1, 1, 0, 0, false);

        assertTrue(menu.fluidSlots().isEmpty());
        assertFalse(menu.hasConfigureSlot());
    }

    @Test
    void theTitleAndTheStatusAreNamedByTheMachine() {
        MachineMenu furnace = new MachineMenu(new SmeltingMachine(), new PlayerInventory());

        assertEquals("Furnace", furnace.title());
        assertEquals(ProgressKind.GENERIC, furnace.progressKind());
        assertEquals("Remain fuel: 0s", furnace.statusText(), "an empty furnace reports zero");
        assertEquals(MachineError.NONE, furnace.error());
    }

    @Test
    void theFuelLineReportsWhatIsLeftToBurn() {
        SmeltingMachine furnace = new SmeltingMachine();
        MachineMenu menu = new MachineMenu(furnace, new PlayerInventory());

        assertEquals(0.0f, menu.fuelSeconds(), 0.001f, "an empty furnace burns nothing");

        furnace.inventory().set(SmeltingMachine.INPUT, ItemStack.of(Items.IRON_ORE, 1));
        furnace.inventory().set(SmeltingMachine.FUEL, ItemStack.of(Items.COAL, 1));
        furnace.tick(1.0f);

        assertTrue(menu.fuelSeconds() > 0.0f, "the coal is burning now");
        assertEquals(79.0f, menu.fuelSeconds(), 0.001f, "one second of the coal is gone");
        assertEquals("Remain fuel: 79s", menu.statusText());
        assertTrue(menu.isRunning(), "a furnace that burns is running");
    }

    @Test
    void aScreenThatDoesNotDescribeItsMachineIsRefused() {
        // Two slots in the inventory, four declared by the screen.
        TestMachine machine = new TestMachine(
                new MachineScreen("Broken", ProgressKind.GENERIC,
                        List.of(SlotKind.GENERIC, SlotKind.GENERIC, SlotKind.GENERIC,
                                SlotKind.GENERIC),
                        List.of(SlotKind.GENERIC), 0, 0, false),
                new MachineInventory(MachineInventory.Role.INPUT, MachineInventory.Role.INPUT,
                        MachineInventory.Role.OUTPUT));

        assertThrows(IllegalArgumentException.class,
                () -> new MachineMenu(machine, new PlayerInventory()));
    }

    /** A machine of a shape, used to check how the layout arranges its slots. */
    private static MachineMenu menu(int inputs, int outputs, int fluidInputs, int fluidOutputs,
            boolean configure) {
        return menu(inputs, outputs, 0, fluidInputs, fluidOutputs, configure);
    }

    /**
     * A machine of a shape, used to check how the layout arranges its slots.
     *
     * @param inputs amount of input slots
     * @param outputs amount of output slots
     * @param upgrades amount of upgrade slots in the column at the right edge
     * @param fluidInputs amount of tanks a recipe drains
     * @param fluidOutputs amount of tanks the machine fills
     * @param configure {@code true} to ask for the configure slot
     * @return the menu of that machine
     */
    private static MachineMenu menu(int inputs, int outputs, int upgrades, int fluidInputs,
            int fluidOutputs, boolean configure) {
        List<MachineInventory.Role> roles = new ArrayList<>();
        List<SlotKind> inKinds = new ArrayList<>();
        List<SlotKind> outKinds = new ArrayList<>();
        List<MachineTank> tanks = new ArrayList<>();
        for (int slot = 0; slot < inputs; slot++) {
            roles.add(MachineInventory.Role.INPUT);
            inKinds.add(SlotKind.SMELTING);
        }
        for (int slot = 0; slot < outputs; slot++) {
            roles.add(MachineInventory.Role.OUTPUT);
            outKinds.add(SlotKind.GENERIC);
        }
        for (int slot = 0; slot < upgrades; slot++) {
            roles.add(MachineInventory.Role.UPGRADE);
        }
        if (configure) {
            roles.add(MachineInventory.Role.CONFIGURE);
        }
        for (int tank = 0; tank < fluidInputs; tank++) {
            tanks.add(MachineTank.of(1000, MachineTank.Role.INPUT));
        }
        for (int tank = 0; tank < fluidOutputs; tank++) {
            tanks.add(MachineTank.of(1000, MachineTank.Role.OUTPUT));
        }
        MachineScreen screen = new MachineScreen("Test", ProgressKind.GENERIC, inKinds, outKinds,
                fluidInputs, fluidOutputs, configure);
        TestMachine machine = new TestMachine(screen,
                new MachineInventory(roles.toArray(new MachineInventory.Role[0])),
                tanks.toArray(new MachineTank[0]));
        return new MachineMenu(machine, new PlayerInventory());
    }

    @Test
    void theUpgradesFillTheColumnAtTheRightEdgeFromBelow() {
        MachineMenu one = menu(1, 1, 1, 0, 0, false);
        Slot only = one.container().layout().slots().get(2);

        assertEquals(MachineMenu.UPGRADE_LEFT, only.x(), "the column at the right edge");
        assertEquals(MachineMenu.UPGRADE_BOTTOM, only.y(), "a single upgrade keeps the corner");
        assertFalse(only.isOutput(), "the player puts an upgrade in and takes it out");

        MachineMenu four = menu(1, 1, MachineMenu.MAX_UPGRADES, 0, 0, false);
        List<Slot> upgrades = four.container().layout().slots()
                .subList(2, 2 + MachineMenu.MAX_UPGRADES);
        for (int index = 0; index < MachineMenu.MAX_UPGRADES; index++) {
            assertEquals(MachineMenu.UPGRADE_LEFT, upgrades.get(index).x());
            assertEquals(MachineMenu.UPGRADE_BOTTOM - index * ContainerLayout.SLOT_PITCH,
                    upgrades.get(index).y(), "upgrade " + index + " of the column");
        }
        assertEquals(MachineMenu.UPGRADE_LEFT - MachineMenu.TEXT_GAP, four.statusRight(),
                "the status line stops beside the column");
    }

    @Test
    void moreUpgradesThanTheColumnHoldsAreRefused() {
        assertThrows(IllegalArgumentException.class,
                () -> menu(1, 1, MachineMenu.MAX_UPGRADES + 1, 0, 0, false));
    }

    @Test
    void aMachineWithEverythingItCanHoldHasNoOverlap() {
        MachineMenu menu = menu(6, 6, MachineMenu.MAX_UPGRADES, 2, 2, true);
        List<int[]> boxes = new ArrayList<>();
        for (Slot slot : menu.container().layout().slots()) {
            boxes.add(box(slot.x(), slot.y()));
        }
        for (MachineMenu.FluidSlot tank : menu.fluidSlots()) {
            boxes.add(box(tank.x(), tank.y()));
        }

        for (int a = 0; a < boxes.size(); a++) {
            for (int b = a + 1; b < boxes.size(); b++) {
                assertFalse(overlaps(boxes.get(a), boxes.get(b)),
                        "the boxes " + Arrays.toString(boxes.get(a)) + " and "
                                + Arrays.toString(boxes.get(b)) + " overlap");
            }
        }
        assertTrue(menu.statusRight() + MachineMenu.TEXT_GAP <= MachineMenu.UPGRADE_LEFT,
                "the status line runs under the column of the upgrades");
    }

    /** The box the picture of a cell covers, its bevel included. */
    private static int[] box(int x, int y) {
        int bevel = PanelTextures.SLOT_BEVEL;
        int size = ContainerLayout.SLOT_SIZE + 2 * bevel;
        return new int[] {x - bevel, y - bevel, size, size};
    }

    /** {@code true} when two boxes share a pixel. */
    private static boolean overlaps(int[] a, int[] b) {
        return a[0] < b[0] + b[2] && b[0] < a[0] + a[2] && a[1] < b[1] + b[3]
                && b[1] < a[1] + a[3];
    }

    /** A machine that does nothing but describe a screen. */
    private static final class TestMachine extends Machine {

        private TestMachine(MachineScreen screen, MachineInventory inventory, MachineTank... tanks) {
            super(screen, inventory, new SimpleEnergyStorage(0), List.of(), tanks);
        }

        @Override
        protected void update(float delta) {
            // A machine that does nothing has no work of its own.
        }
    }
}
