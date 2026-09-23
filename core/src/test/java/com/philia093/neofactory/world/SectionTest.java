package com.philia093.neofactory.world;

import com.philia093.neofactory.block.Block;
import com.philia093.neofactory.block.BlockRegistry;
import com.philia093.neofactory.block.Blocks;
import com.philia093.neofactory.support.TestRegistries;
import com.philia093.neofactory.util.Constants;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the cube a chunk is stored and drawn in.
 * <p>
 * Two things here outlive any single world: the order of the cells, which a stored chunk and a
 * meshed section both read in, and the promise that a section of nothing but air costs nothing. The
 * first is checked by naming the index of the corners of the cube, the second by watching that a
 * section which is never given a block never gains a light map either.
 */
class SectionTest {

    private static final int LOWEST = 0;

    @BeforeAll
    static void registerGameData() {
        TestRegistries.ensure();
    }

    @Test
    void aSectionIsSixteenBlocksOnEverySide() {
        assertEquals(Constants.SECTION_SIZE, Section.SIZE);
        assertEquals(16, Section.SIZE);
        assertEquals(4096, Section.VOLUME);
        assertEquals(2048, Section.LIGHT_BYTES);
        assertEquals(15, Section.MAX_LIGHT);
    }

    @Test
    void aSectionKnowsWhereItSitsInTheWorld() {
        Section lowest = new Section(LOWEST);
        assertEquals(LOWEST, lowest.sectionY());
        assertEquals(Constants.MIN_Y, lowest.originY());

        Section second = new Section(1);
        assertEquals(Constants.MIN_Y + 16, second.originY());

        Section highest = new Section(Constants.SECTION_COUNT - 1);
        assertEquals(Constants.MIN_Y + (Constants.SECTION_COUNT - 1) * 16, highest.originY());
    }

    @Test
    void aSectionOutsideTheWorldIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> new Section(-1));
        assertThrows(IllegalArgumentException.class, () -> new Section(Constants.SECTION_COUNT));
    }

    @Test
    void theCellsAreLaidOutAsHeightThenDepthThenWidth() {
        assertEquals(0, Section.index(0, 0, 0));
        assertEquals(1, Section.index(1, 0, 0));
        assertEquals(15, Section.index(15, 0, 0));
        assertEquals(16, Section.index(0, 0, 1));
        assertEquals(240, Section.index(0, 0, 15));
        assertEquals(256, Section.index(0, 1, 0));
        assertEquals(4095, Section.index(15, 15, 15));

        for (int y = 0; y < Section.SIZE; y++) {
            for (int z = 0; z < Section.SIZE; z++) {
                for (int x = 0; x < Section.SIZE; x++) {
                    assertEquals((y << 8) | (z << 4) | x, Section.index(x, y, z),
                            "the cell " + x + "," + y + "," + z + " moved");
                }
            }
        }
    }

    @Test
    void aCellOutsideTheSectionIsRefused() {
        assertThrows(IndexOutOfBoundsException.class, () -> Section.index(-1, 0, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> Section.index(0, -1, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> Section.index(0, 0, 16));
        assertThrows(IndexOutOfBoundsException.class, () -> Section.index(16, 16, 16));

        assertTrue(Section.contains(0));
        assertTrue(Section.contains(15));
        assertFalse(Section.contains(-1));
        assertFalse(Section.contains(16));
    }

    @Test
    void anUntouchedSectionIsEmptyAndHoldsNoArray() {
        Section section = new Section(LOWEST);

        assertTrue(section.isEmpty());
        assertEquals(0, section.blockCount());
        assertEquals(Block.AIR_ID, section.rawId(3, 4, 5));
        assertEquals(0, section.state(3, 4, 5));
        assertFalse(section.hasLight());

        // Writing air into a cell that already holds air is not a change, so it neither counts nor
        // allocates.
        section.setRawId(3, 4, 5, Block.AIR_ID);
        assertTrue(section.isEmpty());
        assertFalse(section.isDirty());
    }

    @Test
    void aBlockIsStoredAndCounted() {
        Section section = new Section(LOWEST);

        section.setRawId(1, 2, 3, Blocks.STONE.id());
        assertEquals(Blocks.STONE.id(), section.rawId(1, 2, 3));
        assertEquals(1, section.blockCount());
        assertFalse(section.isEmpty());
        assertTrue(section.isDirty());

        section.setRawId(1, 2, 3, Blocks.GRASS.id());
        assertEquals(Blocks.GRASS.id(), section.rawId(1, 2, 3));
        assertEquals(1, section.blockCount(), "replacing a block does not add one");

        section.setRawId(1, 2, 3, Block.AIR_ID);
        assertEquals(Block.AIR_ID, section.rawId(1, 2, 3));
        assertEquals(0, section.blockCount());
        assertTrue(section.isEmpty());

        section.clearDirty();
        assertFalse(section.isDirty());
    }

    @Test
    void aBlockOutsideTheGameIsRefused() {
        Section section = new Section(LOWEST);

        assertThrows(IllegalArgumentException.class, () -> section.setRawId(0, 0, 0, -1));
        assertThrows(IllegalArgumentException.class,
                () -> section.setRawId(0, 0, 0, BlockRegistry.MAX_BLOCKS));
    }

    @Test
    void aStateBelongsToTheBlockThatIsThere() {
        Section section = new Section(LOWEST);
        section.setRawId(4, 4, 4, Blocks.FURNACE.id());

        section.setState(4, 4, 4, 7);
        assertEquals(7, section.state(4, 4, 4));

        // Writing the same block again is not a change, so the state survives it.
        section.setRawId(4, 4, 4, Blocks.FURNACE.id());
        assertEquals(7, section.state(4, 4, 4));

        // Another block cannot inherit the state of the one before it.
        section.setRawId(4, 4, 4, Blocks.STONE.id());
        assertEquals(0, section.state(4, 4, 4));

        section.setState(4, 4, 4, 200);
        assertEquals(200, section.state(4, 4, 4), "a state is one byte, up to 255");
    }

    @Test
    void aStateOfZeroCostsNothing() {
        Section section = new Section(LOWEST);

        section.setState(0, 0, 0, 0);
        assertEquals(0, section.state(0, 0, 0));
    }

    @Test
    void twoLightLevelsShareOneByteWithoutTouchingEachOther() {
        Section section = new Section(LOWEST);

        assertFalse(section.hasLight());
        section.setSkyLight(0, 0, 0, 5);
        assertTrue(section.hasLight());
        assertEquals(5, section.skyLight(0, 0, 0));
        assertEquals(0, section.skyLight(1, 0, 0), "the neighbour of a lit cell stays dark");

        section.setSkyLight(1, 0, 0, 9);
        assertEquals(5, section.skyLight(0, 0, 0), "the level below it survived the neighbour");
        assertEquals(9, section.skyLight(1, 0, 0));

        section.setSkyLight(2, 0, 0, 15);
        assertEquals(15, section.skyLight(2, 0, 0));
        assertEquals(0, section.skyLight(3, 0, 0));
    }

    @Test
    void theTwoLightMapsAreKeptApart() {
        Section section = new Section(LOWEST);

        section.setSkyLight(2, 2, 2, 12);
        section.setBlockLight(2, 2, 2, 4);

        assertEquals(12, section.skyLight(2, 2, 2));
        assertEquals(4, section.blockLight(2, 2, 2));

        section.clearLight();
        assertFalse(section.hasLight());
        assertEquals(0, section.skyLight(2, 2, 2));
        assertEquals(0, section.blockLight(2, 2, 2));
    }

    @Test
    void aLightLevelIsKeptBetweenZeroAndFifteen() {
        Section section = new Section(LOWEST);

        section.setBlockLight(0, 0, 0, 20);
        assertEquals(Section.MAX_LIGHT, section.blockLight(0, 0, 0));

        section.setBlockLight(0, 0, 0, -3);
        assertEquals(0, section.blockLight(0, 0, 0));
    }

    @Test
    void aStateIsWideEnoughForTheFluidOfACell() {
        Section section = new Section(LOWEST);
        section.setRawId(2, 3, 4, Blocks.WATER.id());

        // A source packs its level and its flag into nine bits, which is what a byte could not hold.
        section.setState(2, 3, 4, 256);
        assertEquals(256, section.state(2, 3, 4), "the source flag survived the storage");

        section.setState(2, 3, 4, Section.MAX_STATE);
        assertEquals(Section.MAX_STATE, section.state(2, 3, 4));
    }

    @Test
    void aStateThatDoesNotFitIsRefusedWhereItIsWritten() {
        Section section = new Section(LOWEST);

        assertThrows(IllegalArgumentException.class, () -> section.setState(0, 0, 0, -1));
        assertThrows(IllegalArgumentException.class,
                () -> section.setState(0, 0, 0, Section.MAX_STATE + 1));
    }

    @Test
    void aSectionDescribesItself() {
        Section section = new Section(3);
        section.setRawId(0, 0, 0, Blocks.STONE.id());
        section.setRawId(1, 0, 0, Blocks.DIRT.id());

        String text = section.toString();
        assertTrue(text.contains("y=3"), text);
        assertTrue(text.contains("2 blocks"), text);
    }
}
