package com.philia093.neofactory.blockentity;

/**
 * The table of the workshop: nine cells a player lays a pattern in.
 * <p>
 * The first container whose content is a work in progress: what lies on the table is the field a recipe
 * is looked for in, and the product of that recipe appears beside it, see
 * {@link com.philia093.neofactory.recipe.CraftingField}. The cells themselves are a plain bag of items
 * like those of a chest, so the pattern is stored with the chunk and the table that a player walks away
 * from still holds what was lying on it when they come back.
 * <p>
 * The result is not stored: it is worked out from the cells every time the field changes, so a stored
 * table can never hand out a product its own cells do not make.
 */
public final class CraftingTableBlockEntity extends ContainerBlockEntity {

    /** Amount of cells of a table, three rows of three. */
    public static final int SLOTS = 9;

    /**
     * Creates an empty table.
     *
     * @param type type of this block entity, the one the block names
     */
    public CraftingTableBlockEntity(BlockEntityType type) {
        super(type, SLOTS);
    }
}
