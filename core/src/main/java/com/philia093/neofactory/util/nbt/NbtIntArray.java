package com.philia093.neofactory.util.nbt;

import java.util.Arrays;

/** An array of 32 bit integers, useful for compact tables of block ids. */
public final class NbtIntArray extends NbtTag {

    private final int[] value;

    /**
     * Creates an integer array tag.
     *
     * @param name name of the tag
     * @param value numbers to store
     */
    public NbtIntArray(String name, int[] value) {
        super(name);
        this.value = value == null ? new int[0] : Arrays.copyOf(value, value.length);
    }

    @Override
    public NbtType type() {
        return NbtType.INT_ARRAY;
    }

    /** Amount of numbers stored by this tag. */
    public int length() {
        return value.length;
    }

    /**
     * Returns a number of the array.
     *
     * @param index index inside the array
     * @return the stored number
     */
    public int get(int index) {
        return value[index];
    }

    /** Detached copy of the stored numbers. */
    public int[] toArray() {
        return Arrays.copyOf(value, value.length);
    }

    @Override
    public String toString() {
        return super.toString() + " = " + value.length + " ints";
    }
}
