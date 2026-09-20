package com.philia093.neofactory.util.nbt;

/** A 32 bit floating point number, used for positions and angles. */
public final class NbtFloat extends NbtTag {

    private final float value;

    /**
     * Creates a float tag.
     *
     * @param name name of the tag
     * @param value value to store
     */
    public NbtFloat(String name, float value) {
        super(name);
        this.value = value;
    }

    @Override
    public NbtType type() {
        return NbtType.FLOAT;
    }

    /** Value of this tag. */
    public float value() {
        return value;
    }

    @Override
    public String toString() {
        return super.toString() + " = " + value;
    }
}
