package com.philia093.neofactory.machine;

import com.philia093.neofactory.fluid.Fluids;
import com.philia093.neofactory.fluid.SimpleFluidStorage;
import com.philia093.neofactory.gui.container.ContainerLayout;
import com.philia093.neofactory.gui.container.Slot;
import com.philia093.neofactory.item.FluidContainer;
import com.philia093.neofactory.item.ItemStack;
import com.philia093.neofactory.item.Items;
import com.philia093.neofactory.item.PlayerInventory;
import com.philia093.neofactory.support.TestRegistries;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Checks that a machine trades the cells of its fluid slot with its tanks.
 * <p>
 * The trade runs while the machine is ticked, see {@link Machine#tick(float)}, so a machine only has to
 * declare a slot of {@link MachineInventory.Role#CELL} to be fed by hand: a full cell is poured into the
 * tank the machine takes fluid from and an empty one is filled from the tank it makes fluid in. The slot
 * is placed by the screen of the machine like any other input, which is checked here as well.
 */
class MachineCellSlotTest {

    private TestMachine machine;

    @BeforeAll
    static void registerGameData() {
        TestRegistries.ensure();
    }

    @BeforeEach
    void setUp() {
        machine = new TestMachine();
    }

    @Test
    void aFullCellIsPouredIntoTheTankTheMachineTakesFluidFrom() {
        machine.inventory().set(TestMachine.CELL, ItemStack.of(Items.WATER_CELL, 1));

        machine.tick(1.0f);

        assertEquals(Fluids.WATER, machine.water().fluid());
        assertEquals(FluidContainer.CAPACITY, machine.water().amount());
        assertEquals(Items.FLUID_CELL, machine.inventory().get(TestMachine.CELL).item(),
                "and the slot holds the empty cell now");
    }

    @Test
    void anEmptyCellIsFilledFromTheTankTheMachineMade() {
        machine.steam().fill(Fluids.WATER, FluidContainer.CAPACITY, false);
        machine.inventory().set(TestMachine.CELL, ItemStack.of(Items.FLUID_CELL, 1));

        machine.tick(1.0f);

        assertEquals(Items.WATER_CELL, machine.inventory().get(TestMachine.CELL).item());
        assertEquals(0, machine.steam().amount(), "and the tank gave its fluid away");
    }

    @Test
    void anEmptyCellIsLeftAloneWhileNoTankHasFluid() {
        machine.inventory().set(TestMachine.CELL, ItemStack.of(Items.FLUID_CELL, 1));

        machine.tick(1.0f);

        assertEquals(Items.FLUID_CELL, machine.inventory().get(TestMachine.CELL).item());
        assertEquals(0, machine.water().amount());
        assertEquals(0, machine.steam().amount());
    }

    @Test
    void theCellSlotIsShownBelowTheInputSlot() {
        MachineMenu menu = new MachineMenu(machine, new PlayerInventory());
        List<Slot> slots = menu.container().layout().slots();

        Slot input = slots.get(TestMachine.INPUT);
        Slot cell = slots.get(TestMachine.CELL);

        assertEquals(MachineMenu.INPUT_RIGHT, cell.x(), "the cells stand in the column of the inputs");
        assertEquals(input.y() + ContainerLayout.SLOT_PITCH, cell.y(), "right below the input");
        assertFalse(cell.isOutput(), "a player puts a cell in and takes one out");
    }

    /** A machine of the test: one tank it takes fluid from, one it makes fluid in and a slot for a cell. */
    private static final class TestMachine extends Machine {

        static final int INPUT = 0;
        static final int CELL = 1;
        static final int OUTPUT = 2;

        /** Tank the machine takes fluid from. */
        private static final int WATER_CAPACITY = 4000;

        /** Tank the machine makes fluid in. */
        private static final int STEAM_CAPACITY = 4000;

        private static final MachineScreen SCREEN = new MachineScreen("Test Machine",
                ProgressKind.GENERIC, List.of(SlotKind.SMELTING, SlotKind.CELL),
                List.of(SlotKind.GENERIC), 1, 1, false);

        private final SimpleFluidStorage water;
        private final SimpleFluidStorage steam;

        TestMachine() {
            super(SCREEN, new MachineInventory(MachineInventory.Role.INPUT,
                    MachineInventory.Role.CELL, MachineInventory.Role.OUTPUT),
                    new SimpleEnergyStorage(0), List.of(),
                    new MachineTank(new SimpleFluidStorage(WATER_CAPACITY), MachineTank.Role.INPUT),
                    new MachineTank(new SimpleFluidStorage(STEAM_CAPACITY),
                            MachineTank.Role.OUTPUT));
            // The machine hands its tanks back, so the test looks at the very storages it was built with.
            this.water = (SimpleFluidStorage) tank(0).storage();
            this.steam = (SimpleFluidStorage) tank(1).storage();
        }

        SimpleFluidStorage water() {
            return water;
        }

        SimpleFluidStorage steam() {
            return steam;
        }

        @Override
        protected void update(float delta) {
            // The machine of the test does no work of its own.
        }
    }
}
