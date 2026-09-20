package com.philia093.neofactory.util.nbt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * An ordered list whose entries all share one type.
 * <p>
 * The format has no tag that describes a list of mixed content: the type byte in
 * front of a list names the type of every entry, and an empty list counts as a
 * list of {@link NbtType#BYTE}. Adding a tag of a different type is therefore
 * refused instead of silently producing a file that cannot be read again.
 */
public final class NbtList extends NbtTag {

    /** Type of the entries, {@link NbtType#BYTE} while the list is empty. */
    private NbtType elementType = NbtType.BYTE;

    private final List<NbtTag> entries = new ArrayList<>();

    /**
     * Creates an empty list tag.
     *
     * @param name name of the tag
     */
    public NbtList(String name) {
        super(name);
    }

    @Override
    public NbtType type() {
        return NbtType.LIST;
    }

    /** Type every entry of this list has. */
    public NbtType elementType() {
        return elementType;
    }

    /** Amount of entries. */
    public int size() {
        return entries.size();
    }

    /** {@code true} when the list holds no entry. */
    public boolean isEmpty() {
        return entries.isEmpty();
    }

    /** Entries of this list, unmodifiable. */
    public List<NbtTag> entries() {
        return Collections.unmodifiableList(entries);
    }

    /**
     * Appends a tag.
     * <p>
     * The first tag decides the type of the list.
     *
     * @param tag tag to append, its name is ignored by the format
     * @return this list, so several entries can be added in one statement
     * @throws NbtException when the tag does not match the type of the list
     */
    public NbtList add(NbtTag tag) {
        if (tag == null) {
            throw new NbtException("Cannot add a null tag to list '" + name() + "'");
        }
        if (entries.isEmpty()) {
            elementType = tag.type();
        } else if (tag.type() != elementType) {
            throw new NbtException("List '" + name() + "' holds " + elementType.typeName()
                    + " entries, cannot add " + tag.type().typeName());
        }
        entries.add(tag);
        return this;
    }

    /**
     * Returns an entry.
     *
     * @param index index inside the list
     * @return the entry
     */
    public NbtTag get(int index) {
        return entries.get(index);
    }

    /**
     * Returns an entry as a compound.
     *
     * @param index index inside the list
     * @return the entry
     * @throws NbtException when the entry is not a compound
     */
    public NbtCompound getCompound(int index) {
        NbtTag tag = get(index);
        if (!(tag instanceof NbtCompound)) {
            throw new NbtException("Entry " + index + " of list '" + name() + "' is a "
                    + tag.type().typeName() + ", not a compound");
        }
        return (NbtCompound) tag;
    }

    @Override
    public String toString() {
        return super.toString() + " = " + entries.size() + " x " + elementType.typeName();
    }
}
