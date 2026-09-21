package com.philia093.neofactory.util.nbt;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * A named collection of tags, the structure a stored world is built from.
 * <p>
 * Entries keep the order they were added in, so a file written twice from the same
 * data is byte for byte identical, which makes a save game easy to compare while
 * it is being developed.
 * <p>
 * Reading is forgiving on purpose: a tag that is missing or has an unexpected type
 * gives the default the caller passed instead of an exception. A save game written
 * by an older or a newer build therefore stays loadable, and only the parts that
 * really cannot be parsed make the whole file fail, see {@link NbtIo}.
 */
public final class NbtCompound extends NbtTag {

    private final Map<String, NbtTag> children = new LinkedHashMap<>();

    /**
     * Creates an empty compound.
     *
     * @param name name of the tag
     */
    public NbtCompound(String name) {
        super(name);
    }

    @Override
    public NbtType type() {
        return NbtType.COMPOUND;
    }

    /**
     * Stores a tag under a name, replacing a tag that uses the same name.
     *
     * @param tag tag to store, its name is used as the key
     * @return this compound, so several entries can be added in one statement
     */
    public NbtCompound put(NbtTag tag) {
        if (tag == null) {
            throw new NbtException("Cannot store a null tag in compound '" + name() + "'");
        }
        children.put(tag.name(), tag);
        return this;
    }

    /**
     * Stores an integer.
     *
     * @param key name of the entry
     * @param value value to store
     * @return this compound
     */
    public NbtCompound putInt(String key, int value) {
        return put(new NbtInt(key, value));
    }

    /**
     * Stores a long.
     *
     * @param key name of the entry
     * @param value value to store
     * @return this compound
     */
    public NbtCompound putLong(String key, long value) {
        return put(new NbtLong(key, value));
    }

    /**
     * Stores a float.
     *
     * @param key name of the entry
     * @param value value to store
     * @return this compound
     */
    public NbtCompound putFloat(String key, float value) {
        return put(new NbtFloat(key, value));
    }

    /**
     * Stores a string.
     *
     * @param key name of the entry
     * @param value text to store
     * @return this compound
     */
    public NbtCompound putString(String key, String value) {
        return put(new NbtString(key, value));
    }

    /**
     * Stores a flag.
     *
     * @param key name of the entry
     * @param value flag to store
     * @return this compound
     */
    public NbtCompound putBoolean(String key, boolean value) {
        return put(NbtByte.of(key, value ? 1 : 0));
    }

    /** Names of every entry, in insertion order. */
    public Set<String> keys() {
        return Collections.unmodifiableSet(children.keySet());
    }

    /** Amount of entries. */
    public int size() {
        return children.size();
    }

    /** {@code true} when no entry is stored. */
    public boolean isEmpty() {
        return children.isEmpty();
    }

    /**
     * Returns an entry.
     *
     * @param key name of the entry
     * @return the tag, or {@code null} when the name is unused
     */
    public NbtTag get(String key) {
        return children.get(key);
    }

    /**
     * {@code true} when a name holds an entry.
     *
     * @param key name of the entry
     */
    public boolean contains(String key) {
        return children.containsKey(key);
    }

    /**
     * Removes an entry.
     * <p>
     * Used while converting an older save game: a tag that moved somewhere else is
     * dropped from the file so that the next reader cannot mix the old and the new
     * location up.
     *
     * @param key name of the entry
     * @return the removed tag, or {@code null} when the name was unused
     */
    public NbtTag remove(String key) {
        return children.remove(key);
    }

    /**
     * Returns an entry of an expected type.
     *
     * @param key name of the entry
     * @param type type the entry must have
     * @return the tag, or {@code null} when it is missing or has another type
     */
    public NbtTag get(String key, NbtType type) {
        NbtTag tag = children.get(key);
        return tag != null && tag.type() == type ? tag : null;
    }

    /**
     * Returns an entry as a compound.
     *
     * @param key name of the entry
     * @return the compound, or {@code null} when the entry is missing or of
     *         another type
     */
    public NbtCompound getCompound(String key) {
        return (NbtCompound) get(key, NbtType.COMPOUND);
    }

    /**
     * Returns an entry as a list.
     *
     * @param key name of the entry
     * @return the list, or {@code null} when the entry is missing or of another
     *         type
     */
    public NbtList getList(String key) {
        return (NbtList) get(key, NbtType.LIST);
    }

    /**
     * Returns an entry as a byte array.
     *
     * @param key name of the entry
     * @return the array, or {@code null} when the entry is missing or of another
     *         type
     */
    public NbtByteArray getByteArray(String key) {
        return (NbtByteArray) get(key, NbtType.BYTE_ARRAY);
    }

    /**
     * Returns an integer entry.
     *
     * @param key name of the entry
     * @param defaultValue value used when the entry is missing
     * @return the stored number, or the default
     */
    public int getInt(String key, int defaultValue) {
        NbtTag tag = get(key, NbtType.INT);
        return tag == null ? defaultValue : ((NbtInt) tag).value();
    }

    /**
     * Returns a long entry.
     *
     * @param key name of the entry
     * @param defaultValue value used when the entry is missing
     * @return the stored number, or the default
     */
    public long getLong(String key, long defaultValue) {
        NbtTag tag = get(key, NbtType.LONG);
        return tag == null ? defaultValue : ((NbtLong) tag).value();
    }

    /**
     * Returns a float entry.
     *
     * @param key name of the entry
     * @param defaultValue value used when the entry is missing
     * @return the stored number, or the default
     */
    public float getFloat(String key, float defaultValue) {
        NbtTag tag = get(key, NbtType.FLOAT);
        return tag == null ? defaultValue : ((NbtFloat) tag).value();
    }

    /**
     * Returns a string entry.
     *
     * @param key name of the entry
     * @param defaultValue text used when the entry is missing
     * @return the stored text, or the default
     */
    public String getString(String key, String defaultValue) {
        NbtTag tag = get(key, NbtType.STRING);
        return tag == null ? defaultValue : ((NbtString) tag).value();
    }

    /**
     * Returns a flag.
     *
     * @param key name of the entry
     * @param defaultValue value used when the entry is missing
     * @return the stored flag, or the default
     */
    public boolean getBoolean(String key, boolean defaultValue) {
        NbtTag tag = get(key, NbtType.BYTE);
        return tag == null ? defaultValue : ((NbtByte) tag).value() != 0;
    }

    @Override
    public String toString() {
        return super.toString() + " = {" + String.join(", ", children.keySet()) + "}";
    }
}
