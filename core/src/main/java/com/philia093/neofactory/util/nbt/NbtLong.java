package com.philia093.neofactory.util.nbt;

/** A signed 64 bit integer, used for timestamps and for byte counts. */
public final class NbtLong extends NbtTag {

    private final long value;

    /**
     * Creates a long tag.
     *
     * @param name name of the tag
     * @param value value to store
     */
    public NbtLong(String name, long value) {
        super(name);
        this.value = value;
    }

    @Override
    public NbtType type() {
        return NbtType.LONG;
    }

    /** Value of this tag. */
    public long value() {
        return value;
    }

    @Override
    public String toString() {
        return super.toString() + " = " + value;
    }
}
