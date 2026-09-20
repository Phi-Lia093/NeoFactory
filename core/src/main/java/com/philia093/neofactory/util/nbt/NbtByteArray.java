package com.philia093.neofactory.util.nbt;

import java.util.Arrays;

/**
 * An array of bytes.
 * <p>
 * This is the type the block data of a saved chunk is stored in: two layers of a
 * chunk are a few hundred bytes, which the compression of the file shrinks a lot
 * further.
 */
public final class NbtByteArray extends NbtTag {

    private final byte[] value;

    /**
     * Creates a byte array tag.
     *
     * @param name name of the tag
     * @param value bytes to store
     */
    public NbtByteArray(String name, byte[] value) {
        super(name);
        this.value = value == null ? new byte[0] : Arrays.copyOf(value, value.length);
    }

    @Override
    public NbtType type() {
        return NbtType.BYTE_ARRAY;
    }

    /** Length of the array in bytes. */
    public int length() {
        return value.length;
    }

    /**
     * Returns the byte at an index.
     *
     * @param index index inside the array
     * @return the byte, interpreted as unsigned by the caller when needed
     */
    public byte get(int index) {
        return value[index];
    }

    /** Detached copy of the stored bytes. */
    public byte[] toArray() {
        return Arrays.copyOf(value, value.length);
    }

    @Override
    public String toString() {
        return super.toString() + " = " + value.length + " bytes";
    }
}
