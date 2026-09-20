package com.philia093.neofactory.util.nbt;

/**
 * A single signed byte.
 * <p>
 * The format stores small numbers as bytes, for example a flag or a block id, and
 * this tag is what they are read as.
 */
public final class NbtByte extends NbtTag {

    private final byte value;

    /**
     * Creates a byte tag.
     *
     * @param name name of the tag
     * @param value value to store
     */
    public NbtByte(String name, byte value) {
        super(name);
        this.value = value;
    }

    /**
     * Creates a byte tag from an integer.
     *
     * @param name name of the tag
     * @param value value to store, reduced to eight bits
     */
    public static NbtByte of(String name, int value) {
        return new NbtByte(name, (byte) value);
    }

    @Override
    public NbtType type() {
        return NbtType.BYTE;
    }

    /** Value of this tag. */
    public byte value() {
        return value;
    }

    /** Value of this tag as an unsigned integer, {@code 0} to {@code 255}. */
    public int unsignedValue() {
        return value & 0xFF;
    }

    @Override
    public String toString() {
        return super.toString() + " = " + value;
    }
}
