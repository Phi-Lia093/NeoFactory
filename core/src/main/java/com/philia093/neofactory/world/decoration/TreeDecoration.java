package com.philia093.neofactory.world.decoration;

import com.philia093.neofactory.block.Block;
import com.philia093.neofactory.block.Blocks;
import com.philia093.neofactory.util.Constants;
import com.philia093.neofactory.world.Biome;
import com.philia093.neofactory.world.Noise;
import com.philia093.neofactory.world.World;

import java.util.Random;

/**
 * An oak tree: a trunk with a canopy on top of it.
 * <p>
 * The trunk is four to six blocks tall and stands on the ground of its column, and the canopy is two wide
 * layers around its top with a narrow cap above them and the corners cut off - the shape an oak has in the
 * original game, and the reason a canopy is not simply a cube: a cube reads as a shrub.
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

    /** Shortest trunk of an oak, in blocks. */
    private static final int SHORTEST_TRUNK = 4;

    /** How many heights a trunk may take above the shortest one. */
    private static final int TRUNK_SPREAD = 3;

    /** Radius of the wide layers of the canopy, in blocks. */
    private static final int CANOPY_RADIUS = 2;

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
        if (terrain.surfaceY(x, y) <= Constants.SEA_LEVEL) {
            // A tree planted below the water line would stand in the water of a lake, or be buried by it.
            return false;
        }
        if (!isCandidate(terrain, x, y)) {
            return false;
        }
        return winsNeighbourhood(terrain, x, y);
    }

    @Override
    public void place(World world, TerrainSampler terrain, int x, int y, Random random) {
        int base = terrain.surfaceY(x, y);
        int trunk = SHORTEST_TRUNK + random.nextInt(TRUNK_SPREAD);
        int top = base + trunk - 1;
        // The trunk goes first: a canopy that came first would have taken the cells the trunk needs, and
        // the two never cover each other because a cell that is filled is owned by whoever filled it.
        for (int height = base; height <= top; height++) {
            world.placeObjectIfAirAt(x, height, y, Blocks.LOG_OAK);
        }
        placeCanopy(world, x, y, top);
    }

    /**
     * Plants the canopy of a tree around the top of its trunk.
     *
     * @param world world receiving the leaves
     * @param x block X coordinate of the trunk
     * @param z block Z coordinate of the trunk
     * @param top block Y coordinate of the highest block of the trunk
     */
    private static void placeCanopy(World world, int x, int z, int top) {
        for (int height = top - 1; height <= top; height++) {
            for (int dx = -CANOPY_RADIUS; dx <= CANOPY_RADIUS; dx++) {
                for (int dz = -CANOPY_RADIUS; dz <= CANOPY_RADIUS; dz++) {
                    if (Math.abs(dx) == CANOPY_RADIUS && Math.abs(dz) == CANOPY_RADIUS) {
                        // The corners stay open, which is what makes the canopy read as a round crown.
                        continue;
                    }
                    world.placeObjectIfAirAt(x + dx, height, z + dz, Blocks.LEAVES_OAK);
                }
            }
        }
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (Math.abs(dx) == 1 && Math.abs(dz) == 1) {
                    continue;
                }
                world.placeObjectIfAirAt(x + dx, top + 1, z + dz, Blocks.LEAVES_OAK);
            }
        }
        world.placeObjectIfAirAt(x, top + 2, z, Blocks.LEAVES_OAK);
    }

    /** {@code true} when the block can carry a tree. */
    private static boolean isSoil(Block floor) {
        // Gravel carries the trees of a cold highland: snow lost its picture, so the snowy biome is
        // built on gravel, see Biome.
        return floor == Blocks.GRASS || floor == Blocks.GRAVEL;
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
