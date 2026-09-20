package com.philia093.neofactory.util.nbt;

/** A signed 32 bit integer, the usual type for a seed or a block coordinate. */
public final class NbtInt extends NbtTag {

    private final int value;

    /**
     * Creates an integer tag.
     *
     * @param name name of the tag
     * @param value value to store
     */
    public NbtInt(String name, int value) {
        super(name);
        this.value = value;
    }

    @Override
    public NbtType type() {
        return NbtType.INT;
    }

    /** Value of this tag. */
    public int value() {
        return value;
    }

    @Override
    public String toString() {
        return super.toString() + " = " + value;
    }
}
