package com.philia093.neofactory.world.decoration;

import com.philia093.neofactory.world.World;

import java.util.Random;

/**
 * A feature that is planted on top of the generated floor.
 * <p>
 * Decorations fill the {@link World#LAYER_OBJECT object layer}: trees, plants,
 * boulders, chests and everything else that is not part of the ground. Adding a
 * new kind of scenery therefore means writing one subclass and registering it in
 * {@link com.philia093.neofactory.world.WorldGen}, no other code has to change.
 * <p>
 * Two rules keep decorations compatible with on demand chunk generation:
 * <ul>
 *     <li>{@link #shouldPlaceAt} must be a pure function of the seed and the
 *         coordinates, it may only read the terrain through the given
 *         {@link TerrainSampler} and must never touch a chunk</li>
 *     <li>{@link #place} writes blocks, but it may only write into the object
 *         layer and must tolerate that a neighbouring chunk is still empty</li>
 * </ul>
 */
public abstract class Decoration {

    private final String name;

    /**
     * Creates a decoration.
     *
     * @param name identifier used in logs and debug output
     */
    protected Decoration(String name) {
        this.name = name;
    }

    /** Identifier used in logs and debug output. */
    public final String name() {
        return name;
    }

    /**
     * Decides whether this feature belongs at a cell.
     * <p>
     * The method is called for every cell of every generated chunk, so it has to
     * be cheap and it has to return the same answer for the same coordinates no
     * matter in which order the chunks are generated.
     *
     * @param terrain read only view on the generated terrain
     * @param x block coordinate along the first horizontal axis
     * @param y block coordinate along the second horizontal axis
     * @return {@code true} when {@link #place} should be called for that cell
     */
    public abstract boolean shouldPlaceAt(TerrainSampler terrain, int x, int y);

    /**
     * Plants the feature.
     * <p>
     * Called only when {@link #shouldPlaceAt} returned {@code true}. The method
     * writes into the object layer through the helpers of {@link World}, which
     * allocate neighbouring chunks on demand but never generate them.
     *
     * @param world world receiving the blocks
     * @param terrain read only view on the generated terrain
     * @param x block coordinate along the first horizontal axis
     * @param y block coordinate along the second horizontal axis
     * @param random deterministic random source of that cell
     */
    public abstract void place(World world, TerrainSampler terrain, int x, int y, Random random);

    @Override
    public String toString() {
        return "Decoration(" + name + ")";
    }
}
