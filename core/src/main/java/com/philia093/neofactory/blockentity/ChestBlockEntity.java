package com.philia093.neofactory.blockentity;

/**
 * A chest: three rows of nine slots and no work of its own.
 * <p>
 * The first container of the game and the shape every later one is measured against: what a
 * player puts in travels with the chunk, what the chest holds is handed over when the block is
 * broken, and the screen that shows it is built from the very pieces the inventory of the player
 * is built from, see {@link com.philia093.neofactory.gui.ChestLayout} and
 * {@link com.philia093.neofactory.gui.ContainerGui}.
 * <p>
 * The amount of slots is written down here because a stored chunk does not name it: a chest of
 * another size would be another class and another type, which is what keeps the slots of an
 * opened world exactly where the player left them.
 */
public final class ChestBlockEntity extends ContainerBlockEntity {

    /** Amount of slots a chest holds, three rows of nine. */
    public static final int SLOTS = 27;

    /**
     * Creates an empty chest.
     *
     * @param type type of this block entity, the one the block names
     */
    public ChestBlockEntity(BlockEntityType type) {
        super(type, SLOTS);
    }
}
