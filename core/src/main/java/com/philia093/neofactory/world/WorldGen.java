package com.philia093.neofactory.world;

import com.badlogic.gdx.math.MathUtils;
import com.philia093.neofactory.block.Block;
import com.philia093.neofactory.block.Blocks;
import com.philia093.neofactory.world.decoration.Decoration;
import com.philia093.neofactory.world.decoration.GrassDecoration;
import com.philia093.neofactory.world.decoration.TerrainSampler;
import com.philia093.neofactory.world.decoration.TreeDecoration;

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
        this.oreCellSeed = seed * 53 + 4;
        this.oreBlockSeed = seed * 59 + 5;
        this.oreKindSeed = seed * 61 + 6;

        List<Decoration> built = new ArrayList<>();
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
        return Biome.fromNormalized(biomeShareAt(x, y));
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
        int y = chunk.originY() + localY;
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
                int y = chunk.originY() + localY;
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

