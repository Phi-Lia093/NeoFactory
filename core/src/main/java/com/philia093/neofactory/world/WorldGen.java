package com.philia093.neofactory.world;

import com.badlogic.gdx.math.MathUtils;
import com.philia093.neofactory.block.Block;
import com.philia093.neofactory.block.Blocks;
import com.philia093.neofactory.util.Constants;
import com.philia093.neofactory.world.decoration.Decoration;
import com.philia093.neofactory.world.decoration.GrassDecoration;
import com.philia093.neofactory.world.decoration.TerrainSampler;
import com.philia093.neofactory.world.decoration.TreeDecoration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static com.philia093.neofactory.util.Constants.CHUNK_SIZE;
import static com.philia093.neofactory.util.Constants.SEA_LEVEL;

/**
 * Turns a seed and a pair of block coordinates into terrain.
 * <p>
 * The generator works in two clearly separated phases:
 * <ol>
 *     <li><b>Floor generation</b> - {@link #generateCell} and
 *         {@link #generateFloor} write the {@link Chunk#LAYER_FLOOR ground} of a
 *         cell. Both only evaluate noise, they never look at another chunk.</li>
 *     <li><b>Decoration</b> - {@link #decorate} plants trees and plants on top of
 *         a finished floor. Decorators decide through {@link TerrainSampler},
 *         which is also noise based, and write into the
 *         {@link Chunk#LAYER_OBJECT object layer} only.</li>
 * </ol>
 * Keeping those phases apart is what makes on demand chunk generation safe: no
 * step of generation ever reads a chunk that could still be empty, so generation
 * cannot recurse into itself.
 * <p>
 * The generator builds one of two lands, see {@link WorldType}: the landscape described above, or the
 * table of blocks a flat world is, where every column is the same height and nothing is planted on it.
 * The type travels with the world, because a chunk is generated exactly once and would not match a
 * world that was created as the other type. Only the few places that depend on the land ask for it -
 * the height of a column, the block on top of it, the bodies of water and the decorations - and
 * nothing else does: what stands above the ground is the work of the same code in both worlds.
 */
public final class WorldGen implements TerrainSampler {

    /** Octaves of the biome field, three give large regions with wavy borders. */
    private static final int BIOME_OCTAVES = 3;

    /** Frequency of the biome field, about one unit per two hundred blocks. */
    private static final float BIOME_FREQUENCY = 1.0f / 220.0f;

    /** Amplitude factor between two biome octaves. */
    private static final float BIOME_PERSISTENCE = 0.5f;

    /** Amount of octaves of the patch field that breaks up uniform ground. */
    private static final int PATCH_OCTAVES = 2;

    // ------------------------------------------------------------------
    // The height of the ground
    // ------------------------------------------------------------------

    /** Octaves of the field that draws the shape of the land, three give hills and valleys. */
    private static final int HEIGHT_OCTAVES = 3;

    /** Frequency of that field, about one unit per hundred and eighty blocks. */
    private static final float HEIGHT_FREQUENCY = 1.0f / 180.0f;

    /** Amplitude factor between two octaves of the shape of the land. */
    private static final float HEIGHT_PERSISTENCE = 0.5f;

    /** Amplitude factor between the two octaves of the small detail on top of it. */
    private static final float DETAIL_PERSISTENCE = 0.5f;

    /**
     * How far the land rises and falls around the sea, in blocks.
     * <p>
     * The field behind it clusters around its middle, so the shape of the land is mostly gentle hills
     * with a few slopes that reach the extremes - which is what a world wants: it must be worth walking
     * through, and every step of it must still be walkable.
     */
    private static final float HEIGHT_RELIEF = 20.0f;

    /**
     * How far above the sea the land stands on average, in blocks.
     * <p>
     * Without it the middle of the shape of the land lies exactly at the level of the sea, which makes
     * half of the world an ocean. The land is raised by this much instead, so that a walk through a world
     * runs into water often enough to be worth exploring and rarely enough to stay a walk.
     */
    private static final float LAND_ABOVE_SEA = 11.0f;

    /** Amplitude of the small bumps that make flat ground uneven, in blocks. */
    private static final float DETAIL_RELIEF = 4.0f;

    /** Octaves of the field that draws those small bumps. */
    private static final int DETAIL_OCTAVES = 2;

    /** Frequency of the field that draws those small bumps, about one unit per forty blocks. */
    private static final float DETAIL_FREQUENCY = 1.0f / 40.0f;

    /** Highest a column of the world is ever drawn, in blocks above the sea. */
    private static final int HIGHEST_GROUND = SEA_LEVEL + 34;

    /** Lowest a column of the world is ever drawn, in blocks above the bottom of the world. */
    private static final int LOWEST_GROUND = SEA_LEVEL - 30;

    /** Depth of the bed of a river and of a lake below the level their water stands at, in blocks. */
    private static final int RIVER_DEPTH = 2;

    /** Depth of the soil below the surface block, in blocks; under it the world is stone. */
    private static final int SOIL_DEPTH = 3;

    /**
     * Depth below the sea at which the ore turns from coal into iron.
     * <p>
     * Both kinds exist everywhere in the stone, but the deeper one is worth mining, so the shallow
     * layers carry the cheap ore and the deep ones the other: a player who digs down finds what they
     * were digging for.
     */
    private static final int IRON_DEPTH = SEA_LEVEL - 24;

    /** Frequency of the patch field, about one unit per eighteen blocks. */
    private static final float PATCH_FREQUENCY = 1.0f / 18.0f;

    /** Amplitude factor between two patch octaves. */
    private static final float PATCH_PERSISTENCE = 0.5f;

    /**
     * Above this patch value the accent block of the biome is used.
     * <p>
     * The value was picked from the distribution of the patch field so that about
     * fifteen percent of a biome shows its accent block, which reads as variation
     * instead of noise.
     */
    private static final float PATCH_THRESHOLD = 0.82f;

    /** Side length in blocks of a single ore cluster cell. */
    private static final int ORE_CELL_SIZE = 5;

    /** Chance that a cluster cell of the rocky biome holds ore at all. */
    private static final float ORE_CLUSTER_CHANCE = 0.12f;

    /** Chance that a block inside an ore cluster really is ore. */
    private static final float ORE_BLOCK_CHANCE = 0.40f;

    // ------------------------------------------------------------------
    // Rivers, lakes and the fields that drop them
    // ------------------------------------------------------------------

    /** Octaves of the fields that outline a body of water. */
    private static final int BODY_OCTAVES = 3;

    /** Amplitude factor between two octaves of a body field. */
    private static final float BODY_PERSISTENCE = 0.5f;

    /** Frequency of the river field, about one unit per ninety blocks. */
    private static final float RIVER_FREQUENCY = 1.0f / 90.0f;

    /**
     * Distance from the middle of the river field that still counts as river.
     * <p>
     * A river is the contour line of its field, the line where the field crosses {@code 0.5}, and
     * a band around that line is what the water covers. Taking a contour line instead of a slice
     * of the field is what makes a river wind: the line follows the field, the field rolls over
     * the land, and the band around it is as wide as the slope of the field allows. A steep piece
     * of the field gives a narrow place, a flat one a wide pond, which reads like a real river
     * instead of like a drawn ribbon.
     */
    private static final float RIVER_HALF_WIDTH = 0.020f;

    /** Frequency of the lake field, about one unit per one hundred and eighty blocks. */
    private static final float LAKE_FREQUENCY = 1.0f / 180.0f;

    /**
     * Above this value of the lake field the terrain lies under water.
     * <p>
     * A lake is a lobe of its field rather than a contour line, so its border is a piece of the
     * field itself: ragged, with bays and peninsulas, and never a circle. The value is picked from
     * the distribution of the field so that a walk of a few hundred blocks runs into a lake.
     */
    private static final float LAKE_THRESHOLD = 0.75f;

    /** Frequency of the field that offsets a body before it is read, which bends it. */
    private static final float WARP_FREQUENCY = 1.0f / 140.0f;

    /** How far that offset reaches, in blocks. */
    private static final float WARP_BLOCKS = 26.0f;

    /**
     * Above this patch value the bed of a body of water is gravel.
     * <p>
     * The patch field already breaks up the uniform ground of a biome, see
     * {@link #PATCH_THRESHOLD}, and the bed of a river wants the same kind of variation at the
     * same scale: patches of gravel and clay in sand, instead of a floor of one material.
     */
    private static final float BED_GRAVEL_THRESHOLD = 0.86f;

    /** Above this patch value, but below {@link #BED_GRAVEL_THRESHOLD}, the bed is clay. */
    private static final float BED_CLAY_THRESHOLD = 0.72f;

    /**
     * Cumulative distribution of {@link #biomeNoiseAt}, sampled at 26 evenly
     * spaced values of the noise range.
     * <p>
     * Layered noise clusters around the middle of its range, so slicing the raw
     * value directly would hand the middle biomes far too much ground: a naive
     * slice turned a thirty percent forest into a sixty percent forest. The table
     * turns the raw value into a share with a flat distribution, which makes
     * {@link Biome#weight()} control the area of a biome directly.
     * <p>
     * The values were measured with the terrain dump tool. Re-measure them when
     * the biome noise parameters change.
     */
    private static final float[] FIELD_DISTRIBUTION = {
        0.0000f, 0.0198f, 0.0330f, 0.0579f, 0.0903f, 0.1222f, 0.1589f, 0.2014f,
        0.2560f, 0.3212f, 0.3963f, 0.4738f, 0.5511f, 0.6244f, 0.6853f, 0.7440f,
        0.7921f, 0.8348f, 0.8754f, 0.9125f, 0.9417f, 0.9694f, 0.9869f, 0.9921f,
        0.9960f, 1.0000f};

    /** Amount of intervals between the samples of {@link #FIELD_DISTRIBUTION}. */
    private static final int FIELD_DISTRIBUTION_STEPS = FIELD_DISTRIBUTION.length - 1;

    /** Radius in blocks searched for a spawn cell inside a grassy biome. */
    private static final int SPAWN_BIOME_SEARCH_RADIUS = 320;

    /** Distance between two probed points of the spawn search, in blocks. */
    private static final int SPAWN_BIOME_SEARCH_STEP = 4;

    // ------------------------------------------------------------------
    // The land of a flat world
    // ------------------------------------------------------------------

    /** Height of the bedrock of a flat world, the block the world ends at. */
    private static final int FLAT_BEDROCK_Y = Constants.MIN_Y;

    /** Height of the soil of a flat world, one block above the bedrock. */
    private static final int FLAT_SOIL_Y = Constants.MIN_Y + 1;

    /**
     * Height of the grass of a flat world, the block a body walks on.
     * <p>
     * The table stands at the very bottom of the world, which is what keeps a flat world at one
     * section per chunk: the land costs a chunk almost nothing, and a hole dug into it is one step
     * below the surface, so a mechanic can be tried out without walking anywhere.
     */
    private static final int FLAT_GROUND_Y = Constants.MIN_Y + 2;

    private final int seed;

    /** Land this generator builds, see {@link WorldType}. */
    private final WorldType type;

    private final Noise biomeNoise;
    private final Noise patchNoise;
    private final Noise riverNoise;
    private final Noise lakeNoise;
    private final Noise warpNoise;
    private final Noise heightNoise;
    private final Noise detailNoise;
    private final int oreCellSeed;
    private final int oreBlockSeed;
    private final List<Decoration> decorations;

    /**
     * Creates a generator of the landscape the seed describes.
     *
     * @param seed world seed, every value produces a different world
     */
    public WorldGen(int seed) {
        this(seed, WorldType.NORMAL);
    }

    /**
     * Creates a generator of one kind of land.
     *
     * @param seed world seed, every value produces a different world
     * @param type land to build, {@code null} builds the landscape the seed describes
     */
    public WorldGen(int seed, WorldType type) {
        this.seed = seed;
        this.type = type == null ? WorldType.NORMAL : type;
        // Derived seeds keep the noise fields uncorrelated while staying
        // reproducible for a given world seed.
        this.biomeNoise = new Noise(seed * 31 + 1);
        this.patchNoise = new Noise(seed * 37 + 2);
        this.riverNoise = new Noise(seed * 41 + 3);
        this.lakeNoise = new Noise(seed * 43 + 7);
        this.warpNoise = new Noise(seed * 71 + 8);
        this.heightNoise = new Noise(seed * 73 + 13);
        this.detailNoise = new Noise(seed * 79 + 14);
        this.oreCellSeed = seed * 53 + 4;
        this.oreBlockSeed = seed * 59 + 5;

        List<Decoration> built = new ArrayList<>();
        // A flat world is left bare: a decorator that asks the land for a height finds the same one in
        // every column, so the only thing it could plant is a forest of trees side by side.
        if (!isFlat()) {
            // The ground, the water of a river and the lava of a pool all belong to the terrain itself, see
            // fillColumn: what is left for a decorator is what grows on the ground.
            built.add(new TreeDecoration(seed));
            built.add(new GrassDecoration(seed));
        }
        this.decorations = Collections.unmodifiableList(built);
    }

    /** Seed the whole world was derived from. */
    public int seed() {
        return seed;
    }

    /** Land this generator builds, see {@link WorldType}. */
    public WorldType type() {
        return type;
    }

    /**
     * {@code true} when this generator builds the table of blocks of a flat world.
     * <p>
     * Every question about the land answers with the one column such a world is made of: no field of
     * noise draws it, no river or lake is carved into it and no biome covers it. The few methods below
     * that do ask are the only places the two lands are told apart.
     */
    private boolean isFlat() {
        return type == WorldType.FLAT;
    }

    /** Decorators planted by this generator, in placement order. */
    public List<Decoration> decorations() {
        return decorations;
    }

    /**
     * Raw noise value of the biome field at a cell.
     * <p>
     * Clusters around {@code 0.5}, which is why {@link #biomeShareAt} exists. The
     * value changes smoothly with the coordinates and is reproducible.
     *
     * @param x block coordinate along the first horizontal axis
     * @param y block coordinate along the second horizontal axis
     * @return the raw noise value of the biome field, in the range {@code [0, 1]}
     */
    public float biomeNoiseAt(int x, int y) {
        return biomeNoise.fbm2(x, y, BIOME_OCTAVES, BIOME_FREQUENCY, BIOME_PERSISTENCE);
    }

    /**
     * Share of the world that owns a value smoother than this cell.
     * <p>
     * The result is evenly distributed over {@code [0, 1]}, which is what makes
     * the biome weights describe real area shares. See
     * {@link #FIELD_DISTRIBUTION}.
     *
     * @param x block coordinate along the first horizontal axis
     * @param y block coordinate along the second horizontal axis
     * @return a flat distributed value in the range {@code [0, 1]}
     */
    public float biomeShareAt(int x, int y) {
        return flatten(biomeNoiseAt(x, y));
    }

    @Override
    public Biome biomeAt(int x, int y) {
        if (isFlat()) {
            // The whole table is covered by the one green biome, which is also the biome a spawn looks
            // for, so a flat world starts wherever it was asked to start.
            return Biome.PLAINS;
        }
        // The water of a river and of a lake covers every other biome: a river runs through a
        // forest as well as through a desert, and the field that draws it does not care where it
        // is. That is why both answer before the biome field is asked at all.
        if (isRiverValleyAt(x, y)) {
            return Biome.RIVER_VALLEY;
        }
        if (isLakeValleyAt(x, y)) {
            return Biome.LAKE_VALLEY;
        }
        return Biome.fromNormalized(biomeShareAt(x, y));
    }

    /**
     * {@code true} when a river runs through a cell.
     * <p>
     * The river is the band around the contour line of a field of its own, see
     * {@link #RIVER_HALF_WIDTH}: it winds because the line does, and it is as wide as the slope of
     * the field allows.
     *
     * @param x block coordinate along the first horizontal axis
     * @param y block coordinate along the second horizontal axis
     * @return {@code true} when the cell lies under the water of a river
     */
    public boolean isRiverValleyAt(int x, int y) {
        return !isFlat()
                && Math.abs(bodyAt(riverNoise, RIVER_FREQUENCY, x, y) - 0.5f) <= RIVER_HALF_WIDTH;
    }

    /**
     * {@code true} when a lake covers a cell.
     * <p>
     * The lake is a lobe of a field of its own above {@link #LAKE_THRESHOLD}. Its border is a piece
     * of the field, so it has bays, islands and peninsulas and is never the circle a hand drawn
     * shape would be.
     *
     * @param x block coordinate along the first horizontal axis
     * @param y block coordinate along the second horizontal axis
     * @return {@code true} when the cell lies under the water of a lake
     */
    public boolean isLakeValleyAt(int x, int y) {
        return !isFlat() && bodyAt(lakeNoise, LAKE_FREQUENCY, x, y) >= LAKE_THRESHOLD;
    }

    /**
     * Reads a body field at a cell, with the sampling position bent by another field.
     * <p>
     * The offset is what makes a river meander and a lake ragged instead of smooth. Without it the
     * contour line of the river field would be a clean curve and the lobe of the lake field an
     * even blob; pushing the sampling position around with a second curve folds both of them into
     * the shapes a landscape has. The two offsets are read from the same field at two places far
     * apart, which makes them independent without a third field.
     *
     * @param field body field to read
     * @param frequency frequency of that field
     * @param x block coordinate along the first horizontal axis
     * @param y block coordinate along the second horizontal axis
     * @return the value of the field, in the range {@code [0, 1]}
     */
    private float bodyAt(Noise field, float frequency, int x, int y) {
        float offsetX = (warpNoise.fbm2(x, y, BODY_OCTAVES, WARP_FREQUENCY, BODY_PERSISTENCE)
                - 0.5f) * WARP_BLOCKS;
        float offsetY = (warpNoise.fbm2(x + 1000.0f, y + 1000.0f, BODY_OCTAVES, WARP_FREQUENCY,
                BODY_PERSISTENCE) - 0.5f) * WARP_BLOCKS;
        return field.fbm2(x + offsetX, y + offsetY, BODY_OCTAVES, frequency, BODY_PERSISTENCE);
    }

    /**
     * Maps a raw noise value onto the share of the world it is below.
     * <p>
     * This is the forward evaluation of {@link #FIELD_DISTRIBUTION}: a value that
     * only ten percent of the world reaches maps to {@code 0.10}, no matter how
     * the noise is shaped in between.
     *
     * @param value raw noise value, clamped to the range {@code [0, 1]}
     * @return the share of the world below that value
     */
    private static float flatten(float value) {
        float scaled = MathUtils.clamp(value, 0.0f, 1.0f) * FIELD_DISTRIBUTION_STEPS;
        int index = (int) scaled;
        if (index >= FIELD_DISTRIBUTION_STEPS) {
            return 1.0f;
        }
        float lower = FIELD_DISTRIBUTION[index];
        float upper = FIELD_DISTRIBUTION[index + 1];
        return MathUtils.clamp(lower + (upper - lower) * (scaled - index), 0.0f, 1.0f);
    }

    @Override
    public Block floorAt(int x, int y) {
        if (isFlat()) {
            // Grass from edge to edge: no bank of a body of water, no scorched ground of a pool and no
            // patch of a second material anywhere.
            return Blocks.GRASS;
        }
        Biome biome = biomeAt(x, y);
        if (biome == Biome.RIVER_VALLEY || biome == Biome.LAKE_VALLEY || isBankOf(x, y)) {
            // The bed of a valley and the bank around it are made of the same three materials - sand,
            // clay and gravel - and the game has no fluid block, so what is left of a river is the shape
            // it carved. Handling the bank here and not in a decoration is what keeps the shore solid
            // without a second pass over the world, see generateCell.
            return bedAt(x, y);
        }
        if (patchNoise.fbm2(x, y, PATCH_OCTAVES, PATCH_FREQUENCY, PATCH_PERSISTENCE)
                >= PATCH_THRESHOLD) {
            return biome.accentBlock();
        }
        return biome.floorBlock();
    }

    /** {@code true} when a river or a lake touches the cell, which makes it a bank. */
    private boolean isBankOf(int x, int y) {
        return isValleyAt(x + 1, y) || isValleyAt(x - 1, y)
                || isValleyAt(x, y + 1) || isValleyAt(x, y - 1);
    }

    /** {@code true} when the water of a river or of a lake covers a cell. */
    private boolean isValleyAt(int x, int y) {
        Biome biome = biomeAt(x, y);
        return biome == Biome.RIVER_VALLEY || biome == Biome.LAKE_VALLEY;
    }

    /**
     * Material of a river bed or a lake bed.
     * <p>
     * Sand is the ground of the bed, with patches of clay and gravel in it, drawn from the patch
     * field the rest of the world uses for its ground details. The three materials are what a bank
     * of a river is made of, and they read through the water above them, which is drawn with a
     * colour that lets the ground shine through.
     *
     * @param x block coordinate along the first horizontal axis
     * @param y block coordinate along the second horizontal axis
     * @return sand, clay or gravel
     */
    private Block bedAt(int x, int y) {
        float patch = patchNoise.fbm2(x, y, PATCH_OCTAVES, PATCH_FREQUENCY, PATCH_PERSISTENCE);
        if (patch >= BED_GRAVEL_THRESHOLD) {
            return Blocks.GRAVEL;
        }
        if (patch >= BED_CLAY_THRESHOLD) {
            return Blocks.CLAY;
        }
        return Blocks.SAND;
    }

    /**
     * Returns the ore of a cell of stone.
     * <p>
     * Ores are placed with hashes instead of smooth noise. A rare cluster cell decides whether the area
     * holds ore at all, a second hash decides which of its blocks are ore, and the depth decides which
     * kind it is: the cheap ore lies above {@link #IRON_DEPTH} and the one worth digging for below it.
     * That produces small, clearly bounded veins whose density is easy to tune through
     * {@link #ORE_CLUSTER_CHANCE} and {@link #ORE_BLOCK_CHANCE}.
     *
     * @param x block X coordinate of the cell
     * @param z block Z coordinate of the cell
     * @param height block Y coordinate of the cell
     * @return the ore block or {@code null} when the cell is plain rock
     */
    private Block oreAt(int x, int z, int height) {
        int cellX = Math.floorDiv(x, ORE_CELL_SIZE);
        int cellZ = Math.floorDiv(z, ORE_CELL_SIZE);
        if (Noise.hash(cellX, cellZ, oreCellSeed) >= ORE_CLUSTER_CHANCE) {
            return null;
        }
        if (Noise.hash(x * 31 + z, height, oreBlockSeed) >= ORE_BLOCK_CHANCE) {
            return null;
        }
        return height <= IRON_DEPTH ? Blocks.IRON_ORE : Blocks.COAL_ORE;
    }

    /**
     * Searches for a spawn cell inside a biome that carries grass.
     * <p>
     * Biomes cover large areas, so a fixed spawn point can end up in a desert or
     * on bare rock. Walking outwards in growing rings and taking the first grassy
     * cell makes the game always start in the classic green landscape without
     * hard coding a starting area into the generator.
     *
     * @param startX block X coordinate to start the search from
     * @param startY block Y coordinate to start the search from
     * @return the first block coordinate pair with a grassy biome, or the start
     *         coordinates when nothing was found
     */
    public int[] findSpawnBiome(int startX, int startY) {
        if (isFlat()) {
            // Every cell of the table is grass, so the requested point is already a place to start.
            return new int[] {startX, startY};
        }
        for (int radius = 0; radius <= SPAWN_BIOME_SEARCH_RADIUS;
                radius += SPAWN_BIOME_SEARCH_STEP) {
            int steps = radius == 0
                    ? 1
                    : Math.max(8, (int) (2.0 * Math.PI * radius / SPAWN_BIOME_SEARCH_STEP));
            for (int step = 0; step < steps; step++) {
                double angle = 2.0 * Math.PI * step / steps;
                int x = startX + (int) Math.round(radius * Math.cos(angle));
                int y = startY + (int) Math.round(radius * Math.sin(angle));
                if (isGrassy(biomeAt(x, y))) {
                    return new int[] {x, y};
                }
            }
        }
        return new int[] {startX, startY};
    }

    /** {@code true} for the biomes whose floor is grass. */
    private static boolean isGrassy(Biome biome) {
        return biome == Biome.PLAINS || biome == Biome.FOREST;
    }

    /**
     * Height of the ground of a column, the block a body stands on.
     * <p>
     * The shape of the land is one field of noise around the sea; a second, finer field makes the ground
     * uneven so that a walk through a plain is not a walk over a table. A world is only as high and as low
     * as {@link #HIGHEST_GROUND} and {@link #LOWEST_GROUND} allow, which keeps every column inside the
     * world and every step of a slope walkable.
     * <p>
     * A river and a lake are carved into the land rather than drawn over it: the ground of their cells is
     * pulled below the sea, so a river runs through a hill as a valley with water in it instead of being a
     * band of sand painted over a slope.
     *
     * @param x block coordinate along the first horizontal axis
     * @param y block coordinate along the second horizontal axis
     * @return the block Y coordinate of the highest block of that column
     */
    public int groundY(int x, int y) {
        if (isFlat()) {
            return FLAT_GROUND_Y;
        }
        float shape = heightNoise.fbm2(x, y, HEIGHT_OCTAVES, HEIGHT_FREQUENCY, HEIGHT_PERSISTENCE);
        float detail = detailNoise.fbm2(x, y, DETAIL_OCTAVES, DETAIL_FREQUENCY, DETAIL_PERSISTENCE);
        int height = MathUtils.clamp(Math.round(SEA_LEVEL + LAND_ABOVE_SEA
                        + (shape - 0.5f) * 2.0f * HEIGHT_RELIEF
                        + (detail - 0.5f) * 2.0f * DETAIL_RELIEF),
                LOWEST_GROUND, HIGHEST_GROUND);
        if (isRiverValleyAt(x, y) || isLakeValleyAt(x, y)) {
            return Math.min(height, SEA_LEVEL - RIVER_DEPTH);
        }
        return height;
    }

    @Override
    public int surfaceY(int x, int y) {
        return groundY(x, y) + 1;
    }

    /**
     * Fills the floor layer of a single cell.
     * <p>
     * Does nothing when the cell was already generated, which makes the call
     * idempotent and cheap enough to run before every block read.
     *
     * @param chunk chunk owning the cell
     * @param localX local X coordinate inside the chunk
     * @param localY local Y coordinate inside the chunk
     */
    public void generateCell(Chunk chunk, int localX, int localY) {
        if (chunk.isCellGenerated(localX, localY)) {
            return;
        }
        fillColumn(chunk, localX, localY, chunk.originX() + localX, chunk.originZ() + localY);
        chunk.markCellGenerated(localX, localY);
    }

    /**
     * Fills one column of the world from the bottom up.
     * <p>
     * This is where a world of cubes is made: bedrock at the very bottom, stone above it, a layer of soil
     * and the block of the biome on top. Nothing is poured over it - the game has no fluid block, see
     * {@link com.philia093.neofactory.fluid.Fluid} - so the valley a river or a lake carves stays dry and
     * a body of water reaches the game as an item or in a tank.
     *
     * @param chunk chunk owning the column
     * @param localX local X coordinate inside the chunk
     * @param localZ local Z coordinate inside the chunk
     * @param x block X coordinate of the column
     * @param z block Z coordinate of the column
     */
    private void fillColumn(Chunk chunk, int localX, int localZ, int x, int z) {
        if (isFlat()) {
            fillFlatColumn(chunk, localX, localZ);
            return;
        }
        int ground = groundY(x, z);
        Block surface = floorAt(x, z);
        Block soil = subsoilOf(surface);
        chunk.setRawId(localX, Constants.MIN_Y, localZ, Blocks.BEDROCK.id());
        for (int y = Constants.MIN_Y + 1; y <= ground; y++) {
            int depth = ground - y;
            Block block;
            if (depth >= SOIL_DEPTH) {
                Block ore = oreAt(x, z, y);
                block = ore == null ? Blocks.STONE : ore;
            } else if (depth > 0) {
                block = soil;
            } else {
                block = surface;
            }
            chunk.setRawId(localX, y, localZ, block.id());
        }
    }

    /**
     * Fills one column of a flat world.
     * <p>
     * Three blocks and nothing else: the bedrock the world ends at, one layer of soil over it and one
     * layer of grass on top. No stone, no ore, no water and no lava, because the point of such a world
     * is that a player always knows what is under their feet: a hole dug into the ground shows dirt and
     * then bedrock, no matter where it is dug.
     *
     * @param chunk chunk owning the column
     * @param localX local X coordinate inside the chunk
     * @param localZ local Z coordinate inside the chunk
     */
    private static void fillFlatColumn(Chunk chunk, int localX, int localZ) {
        chunk.setRawId(localX, FLAT_BEDROCK_Y, localZ, Blocks.BEDROCK.id());
        chunk.setRawId(localX, FLAT_SOIL_Y, localZ, Blocks.DIRT.id());
        chunk.setRawId(localX, FLAT_GROUND_Y, localZ, Blocks.GRASS.id());
    }

    /** Material of the soil under the surface block of a column. */
    private static Block subsoilOf(Block surface) {
        if (surface == Blocks.STONE || surface == Blocks.SANDSTONE) {
            return Blocks.STONE;
        }
        if (surface == Blocks.SAND || surface == Blocks.GRAVEL || surface == Blocks.CLAY) {
            return Blocks.SAND;
        }
        return Blocks.DIRT;
    }

    /**
     * Fills the floor layer of every cell of a chunk.
     * <p>
     * The method is pure noise evaluation: it never reads a block and therefore
     * can never trigger the generation of another chunk.
     *
     * @param chunk chunk to fill
     */
    public void generateFloor(Chunk chunk) {
        for (int localY = 0; localY < CHUNK_SIZE; localY++) {
            for (int localX = 0; localX < CHUNK_SIZE; localX++) {
                generateCell(chunk, localX, localY);
            }
        }
    }

    /**
     * Plants every decoration that belongs to a chunk.
     * <p>
     * Decorations may spill into the object layer of a neighbouring chunk, which
     * is why the blocks are written through the helpers of {@link World}. Those
     * allocate a missing chunk but never generate it, so the floor of the
     * neighbour is still produced later without losing the leaves.
     *
     * @param world world receiving the blocks
     * @param chunk chunk whose floor was just completed
     */
    public void decorate(World world, Chunk chunk) {
        for (int localY = 0; localY < CHUNK_SIZE; localY++) {
            for (int localX = 0; localX < CHUNK_SIZE; localX++) {
                int x = chunk.originX() + localX;
                int y = chunk.originZ() + localY;
                for (Decoration decoration : decorations) {
                    if (decoration.shouldPlaceAt(this, x, y)) {
                        decoration.place(world, this, x, y, randomFor(x, y));
                    }
                }
            }
        }
    }

    /**
     * Deterministic random source of a cell.
     * <p>
     * Decorators that need more than one random value use this instead of
     * {@link java.util.Random} created from the world seed alone, so that a cell
     * always gets the same values no matter when it is generated.
     *
     * @param x block coordinate along the first horizontal axis
     * @param y block coordinate along the second horizontal axis
     * @return a random source seeded from that cell
     */
    public Random randomFor(int x, int y) {
        int cellSeed = seed;
        cellSeed = cellSeed * 31 + x;
        cellSeed = cellSeed * 31 + y;
        return new Random(cellSeed);
    }
}

