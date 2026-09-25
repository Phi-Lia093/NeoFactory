package com.philia093.neofactory.machine;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Checks what a machine may ask its screen to show.
 * <p>
 * A machine describes its panel by counting: how many slots it takes, how many it makes and how many tanks
 * it holds. The counts the layout arranges are checked here without a world and without a window, together
 * with the shape a block of slots is drawn with - a machine may have none of a side at all, which is what a
 * boiler with no product and a tank of fluid that holds no item ask for.
 */
class MachineScreenTest {

    @Test
    void aMachineMayHoldNoSlotOfASide() {
        MachineScreen screen = new MachineScreen("Boiler", ProgressKind.GENERIC, List.of(),
                List.of(), 1, 1, false);

        assertEquals(0, screen.inputSlots());
        assertEquals(0, screen.outputSlots());
        assertEquals(0, MachineScreen.columns(0), "a side without a slot takes no column");
        assertEquals(0, MachineScreen.rows(0), "and no row");
    }

    @Test
    void theShapesOfTheLayoutAreTheOnesThatFit() {
        assertEquals(1, MachineScreen.columns(1));
        assertEquals(2, MachineScreen.columns(4));
        assertEquals(3, MachineScreen.columns(6));

        assertEquals(1, MachineScreen.rows(1));
        assertEquals(2, MachineScreen.rows(2));
        assertEquals(2, MachineScreen.rows(4));
        assertEquals(2, MachineScreen.rows(6));
    }

    @Test
    void aSideOfAShapeTheLayoutDoesNotKnowIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> new MachineScreen("Odd",
                ProgressKind.GENERIC,
                List.of(SlotKind.GENERIC, SlotKind.GENERIC, SlotKind.GENERIC),
                List.of(SlotKind.GENERIC), 0, 0, false));
        assertThrows(IllegalArgumentException.class, () -> MachineScreen.columns(3));
        assertThrows(IllegalArgumentException.class, () -> MachineScreen.rows(5));
    }

    @Test
    void aTankIsNoSlot() {
        assertThrows(IllegalArgumentException.class, () -> new MachineScreen("Mixed",
                ProgressKind.GENERIC, List.of(SlotKind.FLUID_INPUT), List.of(SlotKind.GENERIC),
                0, 0, false));
    }

    @Test
    void aBlankTitleIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> new MachineScreen(" ",
                ProgressKind.GENERIC, List.of(SlotKind.GENERIC), List.of(SlotKind.GENERIC),
                0, 0, false));
    }

    @Test
    void tooManyTanksForThePanelAreRefused() {
        assertThrows(IllegalArgumentException.class, () -> new MachineScreen("Three Tanks",
                ProgressKind.GENERIC, List.of(SlotKind.GENERIC), List.of(SlotKind.GENERIC),
                3, 0, false));
    }
}
