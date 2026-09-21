package com.philia093.neofactory.entity;

/**
 * Creates an empty entity of one type.
 * <p>
 * Reading a save game needs entities that do not exist yet: the type is looked up
 * by name and its factory is asked for a fresh instance, which is then filled with
 * the stored fields. An empty constructor per type is therefore not enough, because
 * the position of an entity is not its identity.
 */
@FunctionalInterface
public interface EntityFactory {

    /**
     * Creates an entity without any stored state.
     *
     * @return a new entity of one type, never {@code null}
     */
    Entity create();
}
