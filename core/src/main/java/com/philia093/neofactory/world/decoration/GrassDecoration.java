package com.philia093.neofactory.world.decoration;

import com.philia093.neofactory.block.Blocks;
import com.philia093.neofactory.world.Biome;
import com.philia093.neofactory.world.Noise;
import com.philia093.neofactory.world.World;

import java.util.Random;

/**
 * Scattered tufts of tall grass.
 * <p>
 * The plant is purely cosmetic: it is stored in the object layer, is not solid
 * and the player can walk straight through it. Like every other decoration its
 * position is a pure function of the seed, which keeps on demand chunk
 * generation reproducible.
 */
public class GrassDecoration extends Decoration {

    /** Chance that a plains cell carries a tuft of grass. */
    private static final float PLAINS_CHANCE = 0.18f;

    /** Chance that a forest cell carries a tuft of grass. */
    private static final float FOREST_CHANCE = 0.3f;

    /** Chance that a snowy cell carries a tuft of grass. */
    private static final float SNOWY_CHANCE = 0.03f;

    private final int seed;

    /**
     * Creates the decorator.
     *
     * @param seed world seed, hashed into the placement field
     */
    public GrassDecoration(int seed) {
        super("tall_grass");
        this.seed = seed;
    }

    @Override
    public boolean shouldPlaceAt(TerrainSampler terrain, int x, int y) {
        if (terrain.floorAt(x, y) != Blocks.GRASS) {
            return false;
        }
        float chance = chanceOf(terrain.biomeAt(x, y));
        if (chance <= 0.0f) {
            return false;
        }
        return Noise.hash(x, y, seed) < chance;
    }

    @Override
    public void place(World world, TerrainSampler terrain, int x, int y, Random random) {
        // A tree may already own this cell, the helper then leaves it untouched.
        world.placeObjectIfAir(x, y, Blocks.TALL_GRASS);
    }

    /** Chance of a biome, zero means the plant never grows there. */
    private static float chanceOf(Biome biome) {
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
