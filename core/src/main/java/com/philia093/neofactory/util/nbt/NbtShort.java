package com.philia093.neofactory.util.nbt;

/** A signed 16 bit integer, used for coordinates that stay inside a chunk. */
public final class NbtShort extends NbtTag {

    private final short value;

    /**
     * Creates a short tag.
     *
     * @param name name of the tag
     * @param value value to store
     */
    public NbtShort(String name, short value) {
        super(name);
        this.value = value;
    }

    /**
     * Creates a short tag from an integer.
     *
     * @param name name of the tag
     * @param value value to store, reduced to sixteen bits
     */
    public static NbtShort of(String name, int value) {
        return new NbtShort(name, (short) value);
    }

    @Override
    public NbtType type() {
        return NbtType.SHORT;
    }

    /** Value of this tag. */
    public short value() {
        return value;
    }

    @Override
    public String toString() {
        return super.toString() + " = " + value;
    }
}
