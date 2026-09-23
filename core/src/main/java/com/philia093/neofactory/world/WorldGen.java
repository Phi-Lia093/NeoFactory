package com.philia093.neofactory.world;

import com.badlogic.gdx.math.MathUtils;
import com.philia093.neofactory.block.Block;
import com.philia093.neofactory.block.Blocks;
import com.philia093.neofactory.world.decoration.Decoration;
import com.philia093.neofactory.world.decoration.GrassDecoration;
import com.philia093.neofactory.world.decoration.LavaLakeDecoration;
import com.philia093.neofactory.world.decoration.TerrainSampler;
import com.philia093.neofactory.world.decoration.TreeDecoration;
import com.philia093.neofactory.world.decoration.WaterBodyDecoration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static com.philia093.neofactory.util.Constants.CHUNK_SIZE;

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

    /** Share of the clusters that turn out to be coal, the rest becomes iron. */
    private static final float ORE_COAL_SHARE = 0.55f;

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

    // ------------------------------------------------------------------
    // Pools of lava
    // ------------------------------------------------------------------

    /** Frequency of the field that drops the pools of lava, about one unit per seventy blocks. */
    private static final float POOL_FREQUENCY = 1.0f / 70.0f;

    /**
     * Above this value of the pool field the ground carries lava.
     * <p>
     * Picked from the distribution of the field so that roughly one percent of the world is lava:
     * often enough to run into a pool while exploring, rare enough that a walk through the world
     * does not turn into a minefield.
     */
    private static final float POOL_THRESHOLD = 0.88f;

    /** Share of the ground under a pool that is gravel, the rest of it is stone. */
    private static final float SCORCHED_GRAVEL_SHARE = 0.25f;

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

    private final int seed;
    private final Noise biomeNoise;
    private final Noise patchNoise;
    private final Noise riverNoise;
    private final Noise lakeNoise;
    private final Noise lavaNoise;
    private final Noise warpNoise;
    private final int scorchSeed;
    private final int oreCellSeed;
    private final int oreBlockSeed;
    private final int oreKindSeed;
    private final List<Decoration> decorations;

    /**
     * Creates a generator.
     *
     * @param seed world seed, every value produces a different world
     */
    public WorldGen(int seed) {
        this.seed = seed;
        // Derived seeds keep the noise fields uncorrelated while staying
        // reproducible for a given world seed.
        this.biomeNoise = new Noise(seed * 31 + 1);
        this.patchNoise = new Noise(seed * 37 + 2);
        this.riverNoise = new Noise(seed * 41 + 3);
        this.lakeNoise = new Noise(seed * 43 + 7);
        this.lavaNoise = new Noise(seed * 47 + 9);
        this.warpNoise = new Noise(seed * 71 + 8);
        this.scorchSeed = seed * 67 + 12;
        this.oreCellSeed = seed * 53 + 4;
        this.oreBlockSeed = seed * 59 + 5;
        this.oreKindSeed = seed * 61 + 6;

        List<Decoration> built = new ArrayList<>();
        // The water comes first: a river and a lake own their cells, and everything planted later
        // only adds to what it finds.
        built.add(new WaterBodyDecoration(seed));
        built.add(new LavaLakeDecoration());
        built.add(new TreeDecoration(seed));
        built.add(new GrassDecoration(seed));
        this.decorations = Collections.unmodifiableList(built);
    }

    /** Seed the whole world was derived from. */
    public int seed() {
        return seed;
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
        // The water of a river and of a lake covers every other biome: a river runs through a
        // forest as well as through a desert, and the field that draws it does not care where it
        // is. That is why both answer before the biome field is asked at all.
        if (isRiverAt(x, y)) {
            return Biome.RIVER;
        }
        if (isLakeAt(x, y)) {
            return Biome.LAKE;
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
    public boolean isRiverAt(int x, int y) {
        return Math.abs(bodyAt(riverNoise, RIVER_FREQUENCY, x, y) - 0.5f) <= RIVER_HALF_WIDTH;
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
    public boolean isLakeAt(int x, int y) {
        return bodyAt(lakeNoise, LAKE_FREQUENCY, x, y) >= LAKE_THRESHOLD;
    }

    /**
     * {@code true} when a pool of lava lies on a cell.
     * <p>
     * The lava itself is poured by {@code LavaLakeDecoration}, which asks this method, but the field
     * belongs here for the same reason the fields of the river and the lake do: the ground of a cell
     * has to answer where it is and what it is made of, see {@link #floorAt(int, int)}. A tree that
     * asked the ground without knowing about the lava was planted in the middle of a pool.
     *
     * @param x block coordinate along the first horizontal axis
     * @param y block coordinate along the second horizontal axis
     * @return {@code true} when the cell lies under a pool of lava
     */
    @Override
    public boolean isLavaPoolAt(int x, int y) {
        return bodyAt(lavaNoise, POOL_FREQUENCY, x, y) >= POOL_THRESHOLD;
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
        Biome biome = biomeAt(x, y);
        if (biome == Biome.RIVER || biome == Biome.LAKE) {
            // The bed of a body of water lies here, in the ground layer, while the water itself
            // stands above it in the layer the player walks in, see WaterBodyDecoration. Neither
            // ore nor the accent block of the biome reaches a bed: a patch of gravel in the middle
            // of a lake would stick out of the water.
            return bedAt(x, y);
        }
        if (isLavaPoolAt(x, y)) {
            // The ground a pool lies on is burnt into stone and gravel. It is burnt here and not
            // left to the decorator that pours the lava, because this method is what everything
            // that plants on the ground reads: without it a tree grew out of the lava.
            return scorchedAt(x, y);
        }
        if (biome == Biome.ROCKY) {
            Block ore = oreAt(x, y);
            if (ore != null) {
                return ore;
            }
        }
        if (patchNoise.fbm2(x, y, PATCH_OCTAVES, PATCH_FREQUENCY, PATCH_PERSISTENCE)
                >= PATCH_THRESHOLD) {
            return biome.accentBlock();
        }
        return biome.floorBlock();
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
     * Material of the ground under a pool of lava.
     * <p>
     * Stone with patches of gravel in it, drawn from a hash of the cell: the burn has no bed of its
     * own the way a river has, it simply scorches what was there.
     *
     * @param x block coordinate along the first horizontal axis
     * @param y block coordinate along the second horizontal axis
     * @return stone or gravel
     */
    private Block scorchedAt(int x, int y) {
        return Noise.hash(x, y, scorchSeed) < SCORCHED_GRAVEL_SHARE
                ? Blocks.GRAVEL : Blocks.STONE;
    }

    /**
     * Returns the ore of a rocky cell.
     * <p>
     * Ores are placed with hashes instead of smooth noise. A rare cluster cell
     * decides whether the area holds ore at all, a second hash decides which of
     * its blocks are ore and a third one whether the cluster is coal or iron.
     * That produces small, clearly bounded veins whose density is easy to tune
     * through {@link #ORE_CLUSTER_CHANCE} and {@link #ORE_BLOCK_CHANCE}.
     *
     * @param x block coordinate along the first horizontal axis
     * @param y block coordinate along the second horizontal axis
     * @return the ore block or {@code null} when the cell is plain rock
     */
    private Block oreAt(int x, int y) {
        int cellX = Math.floorDiv(x, ORE_CELL_SIZE);
        int cellY = Math.floorDiv(y, ORE_CELL_SIZE);
        if (Noise.hash(cellX, cellY, oreCellSeed) >= ORE_CLUSTER_CHANCE) {
            return null;
        }
        if (Noise.hash(x, y, oreBlockSeed) >= ORE_BLOCK_CHANCE) {
            return null;
        }
        return Noise.hash(cellX, cellY, oreKindSeed) < ORE_COAL_SHARE
                ? Blocks.COAL_ORE
                : Blocks.IRON_ORE;
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
        int x = chunk.originX() + localX;
        int y = chunk.originZ() + localY;
        chunk.setRawId(localX, localY, Chunk.LAYER_FLOOR, floorAt(x, y).id());
        chunk.markCellGenerated(localX, localY);
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

