package com.philia093.neofactory.cable;

import com.philia093.neofactory.block.Block;
import com.philia093.neofactory.block.BlockFace;
import com.philia093.neofactory.block.Blocks;
import com.philia093.neofactory.item.ItemRegistry;
import com.philia093.neofactory.item.Items;
import com.philia093.neofactory.support.TestRegistries;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the table of the cables: the blocks and the items of every material, their ids and their masks.
 * <p>
 * A cable is a block and an item like a pipe, so what is checked here is the same three things: the run of
 * numbers the cables are given is tight and sits where the game says it does, every cable knows the block
 * and the item that carry it, and the mask of a state is the six sides of the cable written as bits.
 */
class CableTableTest {

    @BeforeAll
    static void registerGameData() {
        TestRegistries.ensure();
    }

    @Test
    void theTableHoldsOneCableOfEveryMaterial() {
        assertEquals(45, CableMaterials.all().size());
        assertEquals(1, CableSize.values().length, "the single line is the cable of the table");
        assertEquals(45, Cables.all().size());
        assertEquals(45, Cables.COUNT);
        for (CableMaterial material : CableMaterials.all()) {
            for (CableSize size : CableSize.values()) {
                Cables.Cable cable = Cables.of(material, size);
                assertNotNull(cable, material + " " + size);
                assertEquals(material, cable.material());
                assertEquals(size, cable.size());
                assertEquals(material.voltage(), cable.voltage());
                assertEquals(material.amperage() * size.factor(), cable.amperage());
                assertEquals(material.loss(), cable.loss());
                assertEquals(material.throughput(), cable.throughput());
                assertEquals(material.name() + "_cable_" + size.fileName(), cable.name());
                assertEquals(material.displayName() + " " + size.displayName() + " Cable",
                        cable.displayName());
                assertEquals(cable, Cables.of(cable.block()), "the block leads back to its cable");
                assertEquals(cable.block(), cable.item().block(), "the item places its block");
            }
        }
        assertNull(Cables.of(Blocks.STONE), "a stone is no cable");
        assertNull(Cables.of((Block) null));
        assertNull(Cables.of(CableMaterials.COPPER, null));
    }

    @Test
    void everyCableTakesANumberOfItsOwn() {
        Set<Integer> blockIds = new HashSet<>();
        Set<Integer> itemIds = new HashSet<>();
        int block = Cables.FIRST_BLOCK_ID;
        int item = Cables.FIRST_ITEM_ID;
        for (Cables.Cable cable : Cables.all()) {
            assertEquals(block++, cable.block().id(), "the run of the blocks of the cables");
            assertEquals(item++, cable.item().id(), "the run of the items of the cables");
            assertTrue(blockIds.add(cable.block().id()));
            assertTrue(itemIds.add(cable.item().id()));
            assertEquals(cable, Cables.of(cable.block()));
            assertEquals(cable.item(), ItemRegistry.byId(cable.item().id()));
            assertEquals(cable.item(), ItemRegistry.byName(cable.name()));
        }
        assertEquals(Cables.FIRST_BLOCK_ID + Cables.COUNT, block, "the run of the blocks stays tight");
        assertEquals(Cables.FIRST_ITEM_ID + Cables.COUNT, item, "the run of the items stays tight");
        assertEquals(Blocks.NEXT_FREE_ID - Cables.COUNT, Cables.FIRST_BLOCK_ID,
                "the cables take the numbers the blocks of the game left free");
        assertEquals(Items.NEXT_FREE_ID - Cables.COUNT, Cables.FIRST_ITEM_ID,
                "and the items the ones the materials of the game used to start at");
    }

    @Test
    void theMaskOfACableIsWrittenInTheBitsOfItsDirections() {
        assertEquals(32, Cables.bit(BlockFace.NORTH));
        assertEquals(16, Cables.bit(BlockFace.EAST));
        assertEquals(8, Cables.bit(BlockFace.SOUTH));
        assertEquals(4, Cables.bit(BlockFace.WEST));
        assertEquals(2, Cables.bit(BlockFace.TOP));
        assertEquals(1, Cables.bit(BlockFace.BOTTOM));
        assertEquals(63, Cables.ALL_MASK);
        assertEquals(Cables.ALL_MASK, Cables.stateOf(Cables.ALL_MASK));
        assertEquals(Cables.STRAIGHT_MASK, Cables.maskOf(Cables.stateOf(Cables.STRAIGHT_MASK)));
        assertTrue(Cables.isConnected(Cables.STRAIGHT_MASK, BlockFace.NORTH));
        assertFalse(Cables.isConnected(Cables.STRAIGHT_MASK, BlockFace.EAST));
        assertEquals(Cables.STRAIGHT_MASK, Cables.mask(BlockFace.NORTH, BlockFace.SOUTH));
        assertEquals(0, Cables.toggled(Cables.STRAIGHT_MASK, BlockFace.NORTH) & Cables.bit(BlockFace.NORTH),
                "the wrench closes a side that was joined");
        assertEquals(Cables.STRAIGHT_MASK, Cables.joined(Cables.bit(BlockFace.NORTH),
                BlockFace.SOUTH), "and opens one that was not");
    }

    @Test
    void aTurnOfACableIsTheSameCable() {
        assertEquals(Cables.mask(BlockFace.WEST, BlockFace.EAST), Cables.turned(Cables.STRAIGHT_MASK, 1));
        assertEquals(Cables.STRAIGHT_MASK, Cables.turned(Cables.STRAIGHT_MASK, 2));
        assertEquals(Cables.ALL_MASK, Cables.turned(Cables.ALL_MASK, 1),
                "a cable that joins on every side is the same after a turn");
        Set<Integer> canonicals = new HashSet<>();
        for (int mask = 0; mask <= Cables.ALL_MASK; mask++) {
            assertEquals(mask, Cables.turned(mask, 4), "four turns come back to where they started");
            assertEquals(mask, Cables.turned(Cables.canonical(mask), Cables.turnsToDraw(mask)),
                    "the turn of a state has to show the mask it stands for");
            assertTrue(Cables.canonical(mask) <= mask, "the canonical mask is the smallest of the four");
            assertEquals(Cables.canonical(mask), Cables.canonical(Cables.turned(mask, 1)),
                    "every turn of a cable is the same cable");
            canonicals.add(Cables.canonical(mask));
        }
        assertEquals(24, canonicals.size(), "the number of models one cable needs");
        assertEquals("pipe_metal_tiny_00", Cables.modelName(0), "the art of a thin tube");
        assertEquals("pipe_metal_tiny_63", Cables.modelName(Cables.ALL_MASK),
                "the model of a cable that joins on all six sides");
    }
}
