package com.philia093.neofactory.world.decoration;

import com.philia093.neofactory.block.Block;
import com.philia093.neofactory.block.Blocks;
import com.philia093.neofactory.world.Biome;
import com.philia093.neofactory.world.Noise;
import com.philia093.neofactory.world.World;

import java.util.Random;

/**
 * An oak tree, seen from above.
 * <p>
 * A tree is a cross of five blocks: one trunk in the middle showing its growth
 * rings and four oak leaves above, below, left and right of it.
 * <p>
 * The position of every tree is a pure function of the seed, so trees stay in
 * place no matter in which order the chunks are generated. Two trees can never
 * touch because a cell only becomes a tree when it carries the smallest hash of
 * its 5 by 5 neighbourhood, which spreads the trees out like a blue noise
 * pattern and rules out a trunk landing inside the canopy of a neighbour.
 */
public class TreeDecoration extends Decoration {

    /** Chebyshev radius of the neighbourhood a tree has to win to be planted. */
    private static final int SPACING_RADIUS = 2;

    /** Candidate chance of a plains cell, one in fifty cells tries to grow a tree. */
    private static final float PLAINS_CHANCE = 0.02f;

    /** Candidate chance of a forest cell, the densest woodland. */
    private static final float FOREST_CHANCE = 0.5f;

    /** Candidate chance of a snowy cell, cold air keeps the woods thin. */
    private static final float SNOWY_CHANCE = 0.015f;

    private final int seed;

    /**
     * Creates the decorator.
     *
     * @param seed world seed, hashed into the candidate field
     */
    public TreeDecoration(int seed) {
        super("tree_oak");
        this.seed = seed;
    }

    @Override
    public boolean shouldPlaceAt(TerrainSampler terrain, int x, int y) {
        if (!isSoil(terrain.floorAt(x, y))) {
            return false;
        }
        if (!isCandidate(terrain, x, y)) {
            return false;
        }
        return winsNeighbourhood(terrain, x, y);
    }

    @Override
    public void place(World world, TerrainSampler terrain, int x, int y, Random random) {
        // The trunk takes the center cell only when it is still free. A decorator that came before
        // owns the cells it filled - the water of a river, the lava of a pool - and a tree has no
        // business growing out of either of them. Tall grass is planted after the trees, see
        // WorldGen, so the trunk never has to push a plant aside.
        world.placeObjectIfAir(x, y, Blocks.LOG_OAK);
        world.placeObjectIfAir(x + 1, y, Blocks.LEAVES_OAK);
        world.placeObjectIfAir(x - 1, y, Blocks.LEAVES_OAK);
        world.placeObjectIfAir(x, y + 1, Blocks.LEAVES_OAK);
        world.placeObjectIfAir(x, y - 1, Blocks.LEAVES_OAK);
    }

    /** {@code true} when the block can carry a tree. */
    private static boolean isSoil(Block floor) {
        return floor == Blocks.GRASS || floor == Blocks.SNOW;
    }

    /**
     * {@code true} when the hash of the cell falls inside the candidate window of
     * its biome.
     */
    private boolean isCandidate(TerrainSampler terrain, int x, int y) {
        float chance = candidateChance(terrain.biomeAt(x, y));
        if (chance <= 0.0f) {
            return false;
        }
        return Noise.hash(x, y, seed) < chance;
    }

    /**
     * {@code true} when no other candidate of the neighbourhood is stronger.
     * <p>
     * Comparing hashes instead of picking the first candidate makes the decision
     * independent of the chunk generation order. Equal hashes are broken by the
     * coordinates so that exactly one of two tied cells survives.
     */
    private boolean winsNeighbourhood(TerrainSampler terrain, int x, int y) {
        float ownHash = Noise.hash(x, y, seed);
        for (int dy = -SPACING_RADIUS; dy <= SPACING_RADIUS; dy++) {
            for (int dx = -SPACING_RADIUS; dx <= SPACING_RADIUS; dx++) {
                if (dx == 0 && dy == 0) {
                    continue;
                }
                int neighbourX = x + dx;
                int neighbourY = y + dy;
                if (!isSoil(terrain.floorAt(neighbourX, neighbourY))) {
                    continue;
                }
                if (!isCandidate(terrain, neighbourX, neighbourY)) {
                    continue;
                }
                float neighbourHash = Noise.hash(neighbourX, neighbourY, seed);
                if (neighbourHash < ownHash) {
                    return false;
                }
                if (neighbourHash == ownHash && isBefore(neighbourX, neighbourY, x, y)) {
                    return false;
                }
            }
        }
        return true;
    }

    /** Lexicographic comparison used as a deterministic tie break. */
    private static boolean isBefore(int ax, int ay, int bx, int by) {
        return ax == bx ? ay < by : ax < bx;
    }

    /** Candidate chance of a biome, zero means trees never grow there. */
    private static float candidateChance(Biome biome) {
        switch (biome) {
            case FOREST:
                return FOREST_CHANCE;
            case PLAINS:
                return PLAINS_CHANCE;
            case SNOWY:
                return SNOWY_CHANCE;
            case DESERT:
            case ROCKY:
            default:
                return 0.0f;
        }
    }
}
