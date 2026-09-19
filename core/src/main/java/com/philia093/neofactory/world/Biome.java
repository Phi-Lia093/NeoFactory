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
    SNOWY("snowy", 4);

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
                return Blocks.GRAVEL;
            case ROCKY:
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
            accumulated += biome.weight;
            if (scaled < accumulated) {
                return biome;
            }
        }
        return PLAINS;
    }
}
