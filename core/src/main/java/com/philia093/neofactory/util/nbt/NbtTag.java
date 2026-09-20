package com.philia093.neofactory.util.nbt;

import java.util.Objects;

/**
 * Base class of every tag of the named binary tag format.
 * <p>
 * A tag knows its {@link NbtType type}, carries an optional name and knows how to
 * write and read its own payload. Names are only meaningful for the members of a
 * {@link NbtCompound}: a list entry and the root tag never write one, which is why
 * {@link NbtIo} asks the container and not the tag whether a name belongs into the
 * file.
 * <p>
 * The format is the one the original game uses, so a stored world could be opened
 * by other tools as well: the type byte comes first, then the name, then the
 * payload. Everything in this package works on plain streams, it does not depend
 * on the engine, which is what makes stored data testable without a window.
 */
public abstract class NbtTag {

    private final String name;

    /**
     * Creates a tag.
     *
     * @param name name of the tag, an empty name for an unnamed tag
     */
    protected NbtTag(String name) {
        this.name = Objects.requireNonNull(name, "name");
    }

    /** Type of this tag, also its type byte inside a file. */
    public abstract NbtType type();

    /** Name of this tag, empty when it is unnamed. */
    public final String name() {
        return name;
    }

    @Override
    public String toString() {
        return type().typeName() + (name.isEmpty() ? "" : "(\"" + name + "\")");
    }
}
