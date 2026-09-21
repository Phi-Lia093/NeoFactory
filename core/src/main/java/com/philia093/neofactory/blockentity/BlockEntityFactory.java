package com.philia093.neofactory.blockentity;

/**
 * Creates an empty block entity of a type.
 * <p>
 * The type is passed in, so a type may build entities that know which type they are
 * without the static table having to be complete while it is initialized.
 */
@FunctionalInterface
public interface BlockEntityFactory {

    /**
     * Creates an empty block entity.
     *
     * @param type type the entity belongs to
     * @return the new entity, without a position
     */
    BlockEntity create(BlockEntityType type);
}
