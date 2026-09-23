package com.philia093.neofactory.world.save;

import com.philia093.neofactory.util.nbt.NbtByteArray;
import com.philia093.neofactory.util.nbt.NbtCompound;
import com.philia093.neofactory.util.nbt.NbtIntArray;
import com.philia093.neofactory.world.Section;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Checks the palette and the packed indices one array of a chunk section is stored with.
 * <p>
 * Two things about the packing are easy to get wrong and are pinned down here: a width that does not
 * divide a byte, so an index starts in the middle of a byte and has to be read back from there, and a
 * section that holds one value, which spends no data at all. The width follows the palette, so the
 * numbers here are what a file of a real section looks like: three bits for the stone, dirt, grass
 * and air of a piece of terrain, sixteen for the whole range a block id can hold.
 */
class PackedValuesTest {

    @Test
    void anArrayOfOneValueCostsNoDataAtAll() {
        int[] values = new int[Section.VOLUME];
        Arrays.fill(values, 7);

        NbtCompound packed = PackedValues.write("Blocks", values);

        assertEquals(0, packed.getInt(PackedValues.TAG_BITS, -1));
        assertNull(packed.getByteArray(PackedValues.TAG_DATA), "a uniform array stored data");
        assertArrayEquals(values, PackedValues.read(packed, values.length));
    }

    @Test
    void theWidthFollowsThePalette() {
        assertEquals(0, PackedValues.width(1), "one value needs no index");
        assertEquals(1, PackedValues.width(2));
        assertEquals(2, PackedValues.width(3));
        assertEquals(2, PackedValues.width(4));
        assertEquals(3, PackedValues.width(5));
        assertEquals(16, PackedValues.width(65536), "a full palette needs sixteen bits");
    }

    @Test
    void everyCellKeepsItsValueAtAWidthThatDoesNotDivideAByte() {
        // Five values, so every index takes three bits and most of them start in the middle of a byte.
        int[] values = randomValues(1000, 5, 7);

        NbtCompound packed = PackedValues.write("Blocks", values);

        assertEquals(3, packed.getInt(PackedValues.TAG_BITS, -1));
        assertEquals(PackedValues.bytesFor(values.length, 3),
                packed.getByteArray(PackedValues.TAG_DATA).length());
        assertArrayEquals(values, PackedValues.read(packed, values.length));
    }

    @Test
    void valuesOfAFullSectionSurviveTheWholeRangeOfABlockId() {
        // Four values from both ends of the range a block id has, cycling over a whole section: the
        // palette keeps the numbers as they are, the packing only has to carry their indices.
        int[] range = {0, 1, 60000, 65535};
        int[] values = new int[Section.VOLUME];
        for (int cell = 0; cell < values.length; cell++) {
            values[cell] = range[cell & 3];
        }

        NbtCompound packed = PackedValues.write("Blocks", values);

        assertEquals(2, packed.getInt(PackedValues.TAG_BITS, -1));
        assertArrayEquals(values, PackedValues.read(packed, values.length));
    }

    @Test
    void aPaletteOfManyValuesSurvivesAWholeSection() {
        int[] values = randomValues(Section.VOLUME, 200, 1234);

        NbtCompound packed = PackedValues.write("States", values);

        assertEquals(8, packed.getInt(PackedValues.TAG_BITS, -1));
        assertArrayEquals(values, PackedValues.read(packed, values.length));
    }

    @Test
    void aPaletteThatDoesNotFitItsWidthIsRefused() {
        NbtCompound compound = compound(1, new int[] {10, 20, 30},
                PackedValues.pack(new int[4], 1));

        assertThrows(SaveException.class, () -> PackedValues.read(compound, 4));
    }

    @Test
    void aDataArrayOfTheWrongLengthIsRefused() {
        NbtCompound compound = compound(1, new int[] {10, 20}, new byte[] {0, 0});

        assertThrows(SaveException.class, () -> PackedValues.read(compound, 100));
    }

    @Test
    void anIndexThePaletteDoesNotHoldIsRefused() {
        // A palette of two values and an index of three, which names nothing.
        int[] indices = new int[4];
        indices[3] = 3;
        NbtCompound compound = compound(2, new int[] {10, 20}, PackedValues.pack(indices, 2));

        assertThrows(SaveException.class, () -> PackedValues.read(compound, 4));
    }

    @Test
    void anEmptyPaletteIsRefused() {
        NbtCompound compound = compound(0, new int[0], null);

        assertThrows(SaveException.class, () -> PackedValues.read(compound, 4));
    }

    @Test
    void aSectionWithoutItsArrayIsRefused() {
        assertThrows(SaveException.class, () -> PackedValues.read(null, 4));
    }

    /** A compound that looks like a stored array, whatever the caller puts into it. */
    private static NbtCompound compound(int bits, int[] palette, byte[] data) {
        NbtCompound compound = new NbtCompound("Blocks");
        compound.putInt(PackedValues.TAG_BITS, bits);
        compound.put(new NbtIntArray(PackedValues.TAG_PALETTE, palette));
        if (data != null) {
            compound.put(new NbtByteArray(PackedValues.TAG_DATA, data));
        }
        return compound;
    }

    /** Values below a limit, from a fixed seed so a failure can be repeated. */
    private static int[] randomValues(int count, int limit, int seed) {
        Random random = new Random(seed);
        int[] values = new int[count];
        for (int cell = 0; cell < count; cell++) {
            values[cell] = random.nextInt(limit);
        }
        return values;
    }
}
