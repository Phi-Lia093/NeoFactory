package com.philia093.neofactory.entity;

import java.util.Objects;

/**
 * A kind of entity.
 * <p>
 * The id and the name are what a save game stores, so both have to stay stable
 * once a world exists: changing a name makes every entity of that type vanish from
 * older worlds. The factory is what turns a stored name back into a live entity,
 * see {@link EntityRegistry}.
 *
 * @param id numeric id, unique and stable
 * @param name name stored in a save game, unique and stable
 * @param factory creates an empty entity of this type
 */
public record EntityType(int id, String name, EntityFactory factory) {

    /** Checks the fields, so a broken registration fails at startup and not later. */
    public EntityType {
        if (id < 0) {
            throw new IllegalArgumentException("Entity id must not be negative: " + id);
        }
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(factory, "factory");
        if (name.isBlank()) {
            throw new IllegalArgumentException("Entity name must not be blank");
        }
    }

    /**
     * Creates an empty entity of this type.
     *
     * @return the new entity, its type is this one
     */
    public Entity create() {
        return factory.create();
    }

    @Override
    public String toString() {
        return "EntityType(" + name + ", id " + id + ")";
    }
}
