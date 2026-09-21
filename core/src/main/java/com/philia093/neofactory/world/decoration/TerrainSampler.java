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
     * A pool is dropped by a decorator rather than drawn as a biome, the way the original game drops
     * one into a hollow - but the field behind it belongs to the terrain all the same, for the same
     * reason {@link #floorAt(int, int)} does: everything that plants on the ground reads the ground,
     * and a tree that did not know about the lava was planted in it. A decorator that pours a pool
     * asks this so that the ground it covers is already scorched, see
     * {@link #floorAt(int, int)}, and so that a body of water and a pool never fight over a cell.
     *
     * @param x block coordinate along the first horizontal axis
     * @param y block coordinate along the second horizontal axis
     * @return {@code true} when the cell lies under a pool of lava
     */
    boolean isLavaPoolAt(int x, int y);
}
