package com.philia093.neofactory.util.nbt;

import java.nio.charset.StandardCharsets;

/**
 * A string tag, stored as UTF-8.
 * <p>
 * The format writes the length in bytes and not in characters, which is what keeps
 * a name with Chinese characters intact: the same convention is used while writing
 * and while reading.
 */
public final class NbtString extends NbtTag {

    private final String value;

    /**
     * Creates a string tag.
     *
     * @param name name of the tag
     * @param value text to store, {@code null} counts as empty
     */
    public NbtString(String name, String value) {
        super(name);
        this.value = value == null ? "" : value;
    }

    @Override
    public NbtType type() {
        return NbtType.STRING;
    }

    /** Text of this tag. */
    public String value() {
        return value;
    }

    /** Length of the text in bytes once it is encoded as UTF-8. */
    public int byteLength() {
        return value.getBytes(StandardCharsets.UTF_8).length;
    }

    @Override
    public String toString() {
        return super.toString() + " = \"" + value + "\"";
    }
}
