package com.philia093.neofactory.world;

import com.philia093.neofactory.block.Block;
import com.philia093.neofactory.block.Blocks;

/**
 * Climatic regions of the generated world.
 * <p>
 * A biome is picked for every cell from a low frequency noise field, exactly
 * like the overworld biomes of Minecraft. The biome decides which block forms
 * the {@link Chunk#LAYER_FLOOR floor} of that cell and which decorations may be
 * planted on it by the {@link com.philia093.neofactory.world.decoration.Decoration
 * decorators}.
 */
public enum Biome {

    /** Temperate grassland: grass floor, scattered flowers and a few trees. */
    PLAINS("plains", 2),

    /** Dry sand: sand floor, no plants, only the rare dry shrub. */
    DESERT("desert", 5),

    /** Dense woodland: grass floor with a high tree density. */
    FOREST("forest", 6),

    /** Bare rock: stone floor with patches of gravel. */
    ROCKY("rocky", 3),

    /** Cold highland: snow floor with sparse trees. */
    SNOWY("snowy", 4),

    /**
     * A river, running through every other biome of the world.
     * <p>
     * Its bed is gravel, clay and sand and the water stands above it in the layer the player
     * walks in, held by a rim of sand and gravel, see
     * {@link com.philia093.neofactory.world.decoration.WaterBodyDecoration}.
     * <p>
     * The weight is zero, which keeps this biome out of the slices of
     * {@link #fromNormalized(float)}: no band of the biome field produces a river. The terrain
     * decides where one runs instead - the river follows the contour line of a noise field of
     * its own, which is what makes it wind instead of run straight, see
     * {@code WorldGen#biomeAt(int, int)}.
     */
    RIVER("river", 0),

    /**
     * A lake, the wide cousin of the river.
     * <p>
     * The same bed and the same water as {@link #RIVER}, above a shape the noise of the terrain
     * drew: a lobe of a fractal field, never a circle. It is also the level a lake of lava is
     * laid out on, see {@code LavaLakeDecoration}.
     */
    LAKE("lake", 0);

    private final String name;
    private final int weight;

    Biome(String name, int weight) {
        this.name = name;
        this.weight = weight;
    }

    /** Identifier used in logs and debug output. */
    public String biomeName() {
        return name;
    }

    /**
     * Relative chance of this biome inside the biome noise bands.
     * <p>
     * The weights are normalized against {@link #totalWeight()} by
     * {@link #fromNormalized(float)}, so a higher value produces larger regions
     * of this biome.
     */
    public int weight() {
        return weight;
    }

    /** Block forming the floor of this biome. */
    public Block floorBlock() {
        switch (this) {
            case DESERT:
            case RIVER:
            case LAKE:
                return Blocks.SAND;
            case ROCKY:
                return Blocks.STONE;
            case SNOWY:
                return Blocks.SNOW;
            case FOREST:
            case PLAINS:
            default:
                return Blocks.GRASS;
        }
    }

    /**
     * Block filling the ground below the floor, used for the sub surface patches
     * such as dirt under grass or sandstone under sand.
     */
    public Block soilBlock() {
        switch (this) {
            case DESERT:
                return Blocks.SANDSTONE;
            case RIVER:
            case LAKE:
                // Clay is what lies under a bank, the material a river carries along.
                return Blocks.CLAY;
            case ROCKY:
                return Blocks.GRAVEL;
            case SNOWY:
                return Blocks.STONE;
            case FOREST:
            case PLAINS:
            default:
                return Blocks.DIRT;
        }
    }

    /**
     * Block that breaks up the otherwise uniform floor of this biome, used for
     * the patchy spots a player sees while walking around.
     */
    public Block accentBlock() {
        switch (this) {
            case DESERT:
            case ROCKY:
            case RIVER:
            case LAKE:
                return Blocks.GRAVEL;
            case SNOWY:
                return Blocks.STONE;
            case FOREST:
                return Blocks.DIRT;
            case PLAINS:
            default:
                return Blocks.STONE;
        }
    }

    /** Sum of every {@link #weight()}, cached because it is constant. */
    private static final int TOTAL_WEIGHT = computeTotalWeight();

    private static int computeTotalWeight() {
        int total = 0;
        for (Biome biome : values()) {
            total += biome.weight;
        }
        return total;
    }

    /** Sum of the weights of every biome. */
    public static int totalWeight() {
        return TOTAL_WEIGHT;
    }

    /**
     * Maps a normalized noise value onto a biome.
     * <p>
     * The range {@code [0, 1]} is cut into consecutive slices, one per biome,
     * whose sizes follow {@link #weight()}. The mapping is stable, a given noise
     * value always yields the same biome.
     * <p>
     * A biome with a weight of zero never owns a slice. That is how a river and a lake stay out of
     * the biome field: their cells come from fields of their own, which the generator asks before
     * this method is reached, see {@code WorldGen#biomeAt(int, int)}.
     *
     * @param normalized value in the range {@code [0, 1]}
     * @return the biome owning that slice of the noise range
     */
    public static Biome fromNormalized(float normalized) {
        float value = normalized;
        if (value < 0.0f) {
            value = 0.0f;
        } else if (value >= 1.0f) {
            // Keep the upper bound inside the last slice.
            value = 0.999999f;
        }

        int scaled = (int) (value * TOTAL_WEIGHT);
        int accumulated = 0;
        for (Biome biome : values()) {
            if (biome.weight <= 0) {
                // A river and a lake are drawn by the terrain and never by the biome field, so
                // they take no room from the biomes that are.
                continue;
            }
            accumulated += biome.weight;
            if (scaled < accumulated) {
                return biome;
            }
        }
        return PLAINS;
    }
}
