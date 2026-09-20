package com.philia093.neofactory.util.nbt;

/** A 64 bit floating point number, kept for values that need full precision. */
public final class NbtDouble extends NbtTag {

    private final double value;

    /**
     * Creates a double tag.
     *
     * @param name name of the tag
     * @param value value to store
     */
    public NbtDouble(String name, double value) {
        super(name);
        this.value = value;
    }

    @Override
    public NbtType type() {
        return NbtType.DOUBLE;
    }

    /** Value of this tag. */
    public double value() {
        return value;
    }

    @Override
    public String toString() {
        return super.toString() + " = " + value;
    }
}
