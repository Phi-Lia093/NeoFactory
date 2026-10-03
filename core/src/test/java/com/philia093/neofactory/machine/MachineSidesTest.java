package com.philia093.neofactory.machine;

import com.philia093.neofactory.block.BlockFace;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the names a player reads for the sides of a machine.
 * <p>
 * The interface of a machine writes those names and the wheel of a tank walks them, so they are arithmetic
 * and nothing else: which side of a machine is its left flank follows from the side it looks in, and it is
 * checked here for every one of them.
 */
class MachineSidesTest {

    @Test
    void theSidesOfAMachineAreNamedFromItsFront() {
        assertEquals(MachineSides.FRONT, MachineSides.nameOf(BlockFace.NORTH, BlockFace.NORTH));
        assertEquals(MachineSides.BACK, MachineSides.nameOf(BlockFace.NORTH, BlockFace.SOUTH));
        assertEquals(MachineSides.UP, MachineSides.nameOf(BlockFace.NORTH, BlockFace.TOP));
        assertEquals(MachineSides.DOWN, MachineSides.nameOf(BlockFace.NORTH, BlockFace.BOTTOM));
        assertEquals(MachineSides.LEFT, MachineSides.nameOf(BlockFace.NORTH, BlockFace.EAST));
        assertEquals(MachineSides.RIGHT, MachineSides.nameOf(BlockFace.NORTH, BlockFace.WEST));
        assertEquals(MachineSides.NONE, MachineSides.nameOf(BlockFace.NORTH, null),
                "a side that carries nothing is named as such and not as a side of the machine");
    }

    @Test
    void theLeftFlankOfAMachineDependsOnTheWayItLooks() {
        // A player who stands in front of a machine that looks north looks south themselves, so the east lies
        // to their left - and a machine that looks east has its left flank to the south.
        assertEquals(BlockFace.EAST, MachineSides.leftOf(BlockFace.NORTH));
        assertEquals(BlockFace.SOUTH, MachineSides.leftOf(BlockFace.EAST));
        assertEquals(BlockFace.WEST, MachineSides.leftOf(BlockFace.SOUTH));
        assertEquals(BlockFace.NORTH, MachineSides.leftOf(BlockFace.WEST));
        assertEquals(BlockFace.WEST, MachineSides.rightOf(BlockFace.NORTH));
        assertEquals(BlockFace.NORTH, MachineSides.rightOf(BlockFace.EAST));
        assertEquals(BlockFace.EAST, MachineSides.rightOf(BlockFace.SOUTH));
        assertEquals(BlockFace.SOUTH, MachineSides.rightOf(BlockFace.WEST));
    }

    @Test
    void everySideOfAMachineHasANameOfItsOwn() {
        for (BlockFace facing : BlockFace.SIDES) {
            List<String> names = new ArrayList<>();
            for (BlockFace side : BlockFace.ALL) {
                String name = MachineSides.nameOf(facing, side);
                assertFalse(names.contains(name), name + " names two sides of a machine that looks " + facing);
                names.add(name);
                assertEquals(side, MachineSides.sideOf(facing, name),
                        "the name " + name + " stands for the side it was made from");
            }
            assertEquals(BlockFace.ALL.length, names.size(), "every side of a machine has a name");
        }
    }

    @Test
    void theSidesOfAMachineAreWalkedInTheOrderAPlayerReadsThem() {
        assertEquals(List.of(BlockFace.NORTH, BlockFace.SOUTH, BlockFace.TOP, BlockFace.BOTTOM,
                BlockFace.EAST, BlockFace.WEST), MachineSides.order(BlockFace.NORTH),
                "the front comes first and the four flanks follow it");
        assertEquals(List.of(BlockFace.WEST, BlockFace.EAST, BlockFace.TOP, BlockFace.BOTTOM,
                BlockFace.NORTH, BlockFace.SOUTH), MachineSides.order(BlockFace.WEST),
                "and the order turns with the machine");
    }

    @Test
    void aMachineLooksAlongTheHorizon() {
        for (BlockFace facing : BlockFace.SIDES) {
            assertTrue(MachineSides.isHorizontal(facing), facing + " lies on the horizon");
        }
        assertFalse(MachineSides.isHorizontal(BlockFace.TOP), "a machine never looks at the ceiling");
        assertFalse(MachineSides.isHorizontal(BlockFace.BOTTOM), "nor at the floor");
        assertFalse(MachineSides.isHorizontal(null));
    }
}
