package com.philia093.neofactory.util.nbt;

/**
 * Kind of a {@link NbtTag}, which is also the type byte of the binary format.
 * <p>
 * The numbers are part of the file format and must never change: a stored world
 * names the type of every tag with the value of this enum, so swapping two
 * numbers would make existing save games unreadable.
 */
public enum NbtType {

    /** A single signed byte. */
    BYTE(1),

    /** A signed 16 bit integer. */
    SHORT(2),

    /** A signed 32 bit integer. */
    INT(3),

    /** A signed 64 bit integer. */
    LONG(4),

    /** A 32 bit floating point number. */
    FLOAT(5),

    /** A 64 bit floating point number. */
    DOUBLE(6),

    /** An array of bytes, used for uncompressed block data. */
    BYTE_ARRAY(7),

    /** A UTF-8 string. */
    STRING(8),

    /** An ordered list of tags that all share one type. */
    LIST(9),

    /** A named collection of tags, the structure a save game is built from. */
    COMPOUND(10),

    /** An array of 32 bit integers. */
    INT_ARRAY(11);

    private final int id;

    NbtType(int id) {
        this.id = id;
    }

    /** Type byte used by the binary format. */
    public int id() {
        return id;
    }

    /** Name of the type, used to write and to read the file. */
    public String typeName() {
        return name();
    }

    /**
     * Returns the type of a type byte.
     *
     * @param id type byte read from a file
     * @return the matching type
     * @throws NbtException when the byte does not belong to any type
     */
    public static NbtType byId(int id) {
        for (NbtType type : values()) {
            if (type.id == id) {
                return type;
            }
        }
        throw new NbtException("Unknown tag type: " + id);
    }
}
