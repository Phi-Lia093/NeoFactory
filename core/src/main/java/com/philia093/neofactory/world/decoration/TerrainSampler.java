package com.philia093.neofactory.world.decoration;

import com.philia093.neofactory.block.Block;
import com.philia093.neofactory.world.Biome;

/**
 * Read only view on the generated terrain.
 * <p>
 * Decorators must not load chunks while they decide where to plant something,
 * otherwise a decoration at the border of a chunk would trigger the generation of
 * its neighbours and could recurse endlessly. Every method of this interface is
 * therefore a pure function of the world seed and the block coordinates.
 */
public interface TerrainSampler {

    /**
     * Biome of a cell.
     *
     * @param x block coordinate along the first horizontal axis
     * @param y block coordinate along the second horizontal axis
     * @return the biome covering that cell, never {@code null}
     */
    Biome biomeAt(int x, int y);

    /**
     * Block the generator puts into the floor layer of a cell.
     * <p>
     * The value is computed on the fly and does not require the chunk to be
     * generated.
     *
     * @param x block coordinate along the first horizontal axis
     * @param y block coordinate along the second horizontal axis
     * @return the floor block of that cell, never {@code null}
     */
    Block floorAt(int x, int y);

    /**
     * {@code true} when a pool of lava lies on that cell.
     * <p>
     * A pool is a field of the terrain rather than a decoration: the ground under it is scorched, see
     * {@link #floorAt(int, int)}, and the lava itself is placed while the column is filled, so that a
     * tree can never be planted in it and a body of water and a pool never fight over a cell.
     *
     * @param x block coordinate along the first horizontal axis
     * @param y block coordinate along the second horizontal axis
     * @return {@code true} when the cell carries the lava of a pool
     */
    boolean isLavaPoolAt(int x, int y);

    /**
     * Height of the cell above the ground of a column, where a body stands.
     * <p>
     * This is the height a decoration plants at and the height a spawn looks for: the ground itself is
     * one below it, and the sea, a river and a lake have their water above the ground of their own cells.
     * The value is computed on the fly, like everything else here, and does not require the chunk to be
     * generated.
     *
     * @param x block coordinate along the first horizontal axis
     * @param y block coordinate along the second horizontal axis
     * @return the block Y coordinate of the cell a body stands in
     */
    int surfaceY(int x, int y);
}
