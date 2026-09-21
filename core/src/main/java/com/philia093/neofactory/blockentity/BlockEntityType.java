package com.philia093.neofactory.blockentity;

import java.util.Objects;

/**
 * A kind of block entity.
 * <p>
 * The name is what a stored chunk writes, so it has to stay stable once a world exists:
 * renaming a type makes every entity of that kind vanish from older worlds. The home of
 * a type is the block that declares it, see
 * {@link com.philia093.neofactory.block.Block#blockEntityTypeName()}, and the factory is
 * what turns a stored name back into a live entity, see {@link BlockEntityRegistry}.
 */
public record BlockEntityType(String name, BlockEntityFactory factory) {

    /** Checks the fields, so a broken registration fails at startup and not later. */
    public BlockEntityType {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(factory, "factory");
        if (name.isBlank()) {
            throw new IllegalArgumentException("A block entity name must not be blank");
        }
    }

    /**
     * Creates an empty block entity of this type.
     *
     * @return the new entity, its type is this one and it has no position yet
     */
    public BlockEntity create() {
        return factory.create(this);
    }

    @Override
    public String toString() {
        return "BlockEntityType(" + name + ")";
    }
}
