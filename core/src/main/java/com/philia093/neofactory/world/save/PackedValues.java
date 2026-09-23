package com.philia093.neofactory.world.save;

import com.philia093.neofactory.util.nbt.NbtByteArray;
import com.philia093.neofactory.util.nbt.NbtCompound;
import com.philia093.neofactory.util.nbt.NbtIntArray;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * One array of a chunk section, stored as a palette and packed indices.
 * <p>
 * A section is sixteen blocks on every side, which is four thousand and ninety six cells. Writing the
 * number of every one of them as a full integer would spend sixteen kilobytes on an array that almost
 * always repeats a handful of values: a piece of terrain holds stone, dirt, grass and air, and a
 * chunk of a world that is filled from the bottom up carries nine such sections, each with a block
 * and a state array.
 * <p>
 * The array is therefore stored the way the original game stores it:
 * <ul>
 *     <li>{@code Palette} lists the values that occur, in the order the cells were walked, so the file
 *         of a chunk built from the same data holds the same bytes every time</li>
 *     <li>{@code Data} holds one index per cell, {@code Bits} bits each, packed one after the other
 *         into the low bits of every byte</li>
 *     <li>{@code Bits} of zero means the whole section holds the one value of the palette, which is
 *         how a section of solid stone and the untouched air of a section nothing reached cost
 *         nothing but the palette</li>
 * </ul>
 * <p>
 * <b>The width travels with the file</b> instead of being fixed at the amount of ids the game has
 * today: a palette that outgrows its width simply gets a wider one, from the single bit of two values
 * up to {@link #MAX_WIDTH} bits, which covers every value a block id can hold. A palette that does
 * not fit into that many bits is refused where it is written rather than cut down, so a system that
 * outgrows the storage hears about it in its own code.
 * <p>
 * <b>A file that cannot describe the array is refused.</b> A palette of the wrong size, a data array
 * of the wrong length and an index that names no entry of the palette all throw a
 * {@link SaveException}, because a chunk that reads halfway would put blocks where none were.
 */
final class PackedValues {

    /** Name of the entry holding the width of one index in bits. */
    static final String TAG_BITS = "Bits";

    /** Name of the entry holding the values of the array. */
    static final String TAG_PALETTE = "Palette";

    /** Name of the entry holding the packed indices. */
    static final String TAG_DATA = "Data";

    /**
     * Highest width one index may be stored with.
     * <p>
     * Sixteen bits hold sixty five thousand five hundred and thirty six values, which is exactly the
     * range a block id has, see {@link com.philia093.neofactory.block.BlockRegistry#MAX_BLOCKS}, and
     * more than the sixteen bits a state is stored in.
     */
    static final int MAX_WIDTH = 16;

    private PackedValues() {
        // Utility class: never instantiated.
    }

    /**
     * Packs an array of values into a compound.
     *
     * @param name name the compound and the entries inside it are stored under
     * @param values values to store, one per cell of a section
     * @return the compound describing that array
     * @throws IllegalArgumentException when the array holds more distinct values than the storage can
     *         address
     */
    static NbtCompound write(String name, int[] values) {
        Map<Integer, Integer> indices = new LinkedHashMap<>();
        int[] packed = new int[values.length];
        for (int cell = 0; cell < values.length; cell++) {
            int value = values[cell];
            Integer index = indices.get(value);
            if (index == null) {
                index = indices.size();
                if (index >= (1 << MAX_WIDTH)) {
                    throw new IllegalArgumentException("The array '" + name + "' holds more than "
                            + (1 << MAX_WIDTH) + " distinct values");
                }
                indices.put(value, index);
            }
            packed[cell] = index;
        }

        int width = width(indices.size());
        int[] palette = new int[indices.size()];
        for (Map.Entry<Integer, Integer> entry : indices.entrySet()) {
            palette[entry.getValue()] = entry.getKey();
        }

        NbtCompound compound = new NbtCompound(name);
        compound.putInt(TAG_BITS, width);
        compound.put(new NbtIntArray(TAG_PALETTE, palette));
        if (width > 0) {
            compound.put(new NbtByteArray(TAG_DATA, pack(packed, width)));
        }
        return compound;
    }

    /**
     * Reads an array written by {@link #write(String, int[])}.
     *
     * @param compound compound holding the array, {@code null} when the entry is missing
     * @param count amount of cells the array has to hold
     * @return the values, one per cell
     * @throws SaveException when the compound does not describe an array of that many cells
     */
    static int[] read(NbtCompound compound, int count) {
        if (compound == null) {
            throw new SaveException("A section holds no array where one is expected");
        }
        int width = compound.getInt(TAG_BITS, -1);
        NbtIntArray paletteTag = compound.getIntArray(TAG_PALETTE);
        if (paletteTag == null) {
            throw new SaveException("A section holds no palette where one is expected");
        }
        int[] palette = paletteTag.toArray();
        if (width < 0 || width > MAX_WIDTH) {
            throw new SaveException("A section stores " + width + " bits per cell, the storage"
                    + " carries 0 to " + MAX_WIDTH);
        }
        if (palette.length == 0) {
            throw new SaveException("A section holds an empty palette");
        }
        if (width == 0) {
            if (palette.length != 1) {
                throw new SaveException("A section stores no index per cell but its palette holds "
                        + palette.length + " values");
            }
            int[] values = new int[count];
            Arrays.fill(values, palette[0]);
            return values;
        }
        if (palette.length > (1 << width)) {
            throw new SaveException("A section stores " + width + " bits per cell, which addresses "
                    + (1 << width) + " values, but its palette holds " + palette.length);
        }

        NbtByteArray data = compound.getByteArray(TAG_DATA);
        if (data == null || data.length() != bytesFor(count, width)) {
            throw new SaveException("A section holds " + (data == null ? "no" : data.length())
                    + " bytes of packed cells instead of " + bytesFor(count, width));
        }

        int[] indices = unpack(data.toArray(), width, count);
        int[] values = new int[count];
        for (int cell = 0; cell < count; cell++) {
            int index = indices[cell];
            if (index >= palette.length) {
                throw new SaveException("A section names palette entry " + index + " of "
                        + palette.length);
            }
            values[cell] = palette[index];
        }
        return values;
    }

    /**
     * Width one index needs for a palette.
     *
     * @param paletteSize amount of values in the palette
     * @return {@code 0} for a single value, otherwise the bits that address the whole palette
     */
    static int width(int paletteSize) {
        if (paletteSize <= 1) {
            return 0;
        }
        int width = 1;
        while ((1 << width) < paletteSize) {
            width++;
        }
        return width;
    }

    /**
     * Amount of bytes a packed array of a given width needs.
     *
     * @param count amount of cells
     * @param width width of one index in bits, at least one
     * @return the length in bytes, rounded up to the next whole byte
     */
    static int bytesFor(int count, int width) {
        return (count * width + 7) / 8;
    }

    /**
     * Packs indices into bytes.
     * <p>
     * The values are written one after the other, low bits first, and an index is allowed to start in
     * the middle of a byte: the accumulator collects bits until a whole byte is there and pushes it
     * out, which keeps the array as short as the width allows without a rule about where a cell may
     * begin.
     *
     * @param indices indices to pack, each one below {@code 1 << width}
     * @param width width of one index in bits
     * @return the packed bytes
     */
    static byte[] pack(int[] indices, int width) {
        byte[] data = new byte[bytesFor(indices.length, width)];
        long accumulator = 0L;
        int buffered = 0;
        int out = 0;
        for (int index : indices) {
            accumulator |= ((long) index) << buffered;
            buffered += width;
            while (buffered >= 8) {
                data[out++] = (byte) accumulator;
                accumulator >>>= 8;
                buffered -= 8;
            }
        }
        if (buffered > 0) {
            data[out] = (byte) accumulator;
        }
        return data;
    }

    /**
     * Reads indices packed by {@link #pack(int[], int)}.
     *
     * @param data packed bytes
     * @param width width of one index in bits
     * @param count amount of indices to read
     * @return the indices, one per cell
     */
    static int[] unpack(byte[] data, int width, int count) {
        int[] indices = new int[count];
        int mask = (1 << width) - 1;
        long accumulator = 0L;
        int buffered = 0;
        int in = 0;
        for (int cell = 0; cell < count; cell++) {
            while (buffered < width) {
                accumulator |= ((long) (data[in++] & 0xFF)) << buffered;
                buffered += 8;
            }
            indices[cell] = (int) (accumulator & mask);
            accumulator >>>= width;
            buffered -= width;
        }
        return indices;
    }
}
