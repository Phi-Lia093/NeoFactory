package com.philia093.neofactory.world.light;

import com.badlogic.gdx.utils.IntArray;
import com.philia093.neofactory.block.Block;
import com.philia093.neofactory.block.BlockRegistry;
import com.philia093.neofactory.block.Blocks;
import com.philia093.neofactory.util.Constants;
import com.philia093.neofactory.world.Chunk;
import com.philia093.neofactory.world.Section;
import com.philia093.neofactory.world.World;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * The light of the cells of the loaded world.
 * <p>
 * A cell carries two levels: the light of the sky that falls into it and the light of the sources of the
 * industry - a torch, a glowing rock - that reach it. Both travel the same way: <b>one step costs one
 * level</b>, they spread through everything that is not opaque and they never add up - a cell is drawn with
 * the brighter of the two, see {@code SectionMesher} and {@code BlockShader}.
 * <p>
 * <b>The sky is the one source that does not fade on its way down.</b> A shaft of open sky is as bright at
 * the bottom of a cliff as at its top, which is why a column is filled from the top in one step and only
 * the cells that stand next to something darker are offered to the spread: the light that seeps into a cave
 * mouth and the light that falls into a hole a player dug.
 * <p>
 * <b>Nothing of this is stored.</b> A chunk arrives dark - the save format leaves the two light maps out on
 * purpose, see {@code ChunkCodec} - so a chunk is lit once it is complete, whether the generator made it or
 * the save game brought it back, and a cell that changed its block is lit again on the spot. A cell whose
 * light really changed marks its section, and a marked section is meshed again, see {@code Section}.
 * <p>
 * The engine is the only writer of the light maps, and it writes through
 * {@link World#peekSkyLight(int, int, int)} and {@link World#setSkyLight(int, int, int, int)}: it never
 * generates terrain, and a cell of a chunk that is not loaded waits for the moment that chunk arrives.
 */
public final class LightEngine {

    private static final Logger LOGGER = LogManager.getLogger();

    /** First horizontal direction of every neighbour, the two vertical ones being the first of them. */
    private static final int[] NEIGHBOUR_X = {0, 0, -1, 1, 0, 0};

    /** Height of every neighbour, the first two being the cell below and the cell above. */
    private static final int[] NEIGHBOUR_Y = {-1, 1, 0, 0, 0, 0};

    /** Second horizontal direction of every neighbour. */
    private static final int[] NEIGHBOUR_Z = {0, 0, 0, 0, -1, 1};

    /** Cells the spread of the sky light has to visit, one queue per level, see {@link #spread}. */
    private final IntArray[] skyBuckets = buckets();

    /** Cells the spread of the block light has to visit, one queue per level, see {@link #spread}. */
    private final IntArray[] blockBuckets = buckets();

    /** Cells the removal of the sky light has to visit. */
    private final IntArray skyRemoval = new IntArray();

    /** Cells the removal of the block light has to visit. */
    private final IntArray blockRemoval = new IntArray();

    /** Cells whose light was really changed since the engine was created, for the log and for tests. */
    private int changed;

    /** {@code true} when a block holds the light back. */
    public static boolean opaque(Block block) {
        return block.isSolid() && !block.isTransparent();
    }

    /**
     * Lights a chunk that just became complete.
     * <p>
     * Three things are offered to the spread: the sky of every column of the chunk, every block of the chunk
     * that gives light away, and the light of the cells just outside the chunk - so two chunks that meet
     * share the light across their border, whichever of them was lit first.
     *
     * @param world world the chunk lies in
     * @param chunk chunk to light
     * @return amount of cells whose light changed
     */
    public int lightChunk(World world, Chunk chunk) {
        int before = changed;
        fillSkyColumns(world, chunk);
        seedSources(world, chunk);
        seedBorder(world, chunk);
        spread(world, true);
        spread(world, false);
        return changed - before;
    }

    /**
     * Lights the cells around one that changed its block.
     * <p>
     * Called by the world for every write of a block: what the cell had is taken away with the block that
     * stood there, a block that gives light becomes a source of its own, and the cells around offer their
     * light to the cell again - which is what lights the hole a player just dug.
     *
     * @param world world the cell lies in
     * @param x block X coordinate
     * @param y block Y coordinate, the height
     * @param z block Z coordinate
     * @return amount of cells whose light changed
     */
    public int relight(World world, int x, int y, int z) {
        if (World.outsideTheWorld(y)) {
            return 0;
        }
        int before = changed;
        takeAway(world, true, x, y, z);
        takeAway(world, false, x, y, z);
        Block block = world.peekBlock(x, y, z);
        if (block.emitsLight()) {
            setLight(world, false, x, y, z, block.lightEmission());
            offer(false, x, y, z, block.lightEmission());
        }
        for (int face = 0; face < NEIGHBOUR_X.length; face++) {
            int nx = x + NEIGHBOUR_X[face];
            int ny = y + NEIGHBOUR_Y[face];
            int nz = z + NEIGHBOUR_Z[face];
            if (!World.outsideTheWorld(ny) && loaded(world, nx, nz)) {
                offerLightOf(world, nx, ny, nz);
            }
        }
        spread(world, true);
        spread(world, false);
        return changed - before;
    }

    /** Cells whose light is waiting to be spread or to be taken away, both channels together. */
    public int pending() {
        int waiting = skyRemoval.size + blockRemoval.size;
        for (int level = 0; level <= Block.MAX_LIGHT; level++) {
            waiting += skyBuckets[level].size + blockBuckets[level].size;
        }
        return waiting / 3;
    }

    /** Cells whose light was really changed since this engine was created. */
    public int changedCells() {
        return changed;
    }

    /** Fills the sky light of every column of a chunk, from the top of the world downwards. */
    private void fillSkyColumns(World world, Chunk chunk) {
        int originX = chunk.originX();
        int originZ = chunk.originZ();
        for (int localZ = 0; localZ < Constants.CHUNK_SIZE; localZ++) {
            for (int localX = 0; localX < Constants.CHUNK_SIZE; localX++) {
                int x = originX + localX;
                int z = originZ + localZ;
                // Above the tallest block around this column every neighbour of a cell is a cell of the sky
                // as well, so only the cells below that line have to be asked whether they stand next to
                // something darker, see #offersSkyToSomething.
                int cap = tallestAround(chunk, localX, localZ) + 1;
                for (int y = Constants.MAX_Y; y >= Constants.MIN_Y; y--) {
                    int id = chunk.rawId(localX, y, localZ);
                    if (id != Blocks.AIR_ID && opaque(BlockRegistry.byId(id))) {
                        // Below the first block that hides the sky the sky does not reach: a cell of a
                        // chunk that arrives dark keeps the dark it has, see ChunkCodec.
                        break;
                    }
                    Section section = chunk.sectionForLight(y / Section.SIZE);
                    int localY = Chunk.localOf(y);
                    if (section.skyLight(localX, localY, localZ) != Block.MAX_LIGHT) {
                        // The sky does not fade on its way down the open air of a column.
                        section.setSkyLight(localX, localY, localZ, Block.MAX_LIGHT);
                        changed++;
                    }
                    if (y <= cap && offersSkyToSomething(world, chunk, localX, y, localZ)) {
                        offer(true, x, y, z, Block.MAX_LIGHT);
                    }
                }
            }
        }
    }

    /** Height of the tallest block of the column of a chunk and of the four columns around it. */
    private static int tallestAround(Chunk chunk, int localX, int localZ) {
        int tallest = chunk.highestBlockY(localX, localZ);
        tallest = Math.max(tallest,
                chunk.highestBlockY(Math.max(localX - 1, 0), localZ));
        tallest = Math.max(tallest,
                chunk.highestBlockY(Math.min(localX + 1, Constants.CHUNK_SIZE - 1), localZ));
        tallest = Math.max(tallest,
                chunk.highestBlockY(localX, Math.max(localZ - 1, 0)));
        tallest = Math.max(tallest,
                chunk.highestBlockY(localX, Math.min(localZ + 1, Constants.CHUNK_SIZE - 1)));
        return tallest;
    }

    /** Offers every glowing block of a chunk to the spread of the block light. */
    private void seedSources(World world, Chunk chunk) {
        int originX = chunk.originX();
        int originZ = chunk.originZ();
        for (int sectionY = 0; sectionY < Constants.SECTION_COUNT; sectionY++) {
            if (chunk.isEmptySection(sectionY)) {
                continue;
            }
            int originY = Constants.MIN_Y + sectionY * Constants.SECTION_SIZE;
            for (int localZ = 0; localZ < Constants.CHUNK_SIZE; localZ++) {
                for (int localX = 0; localX < Constants.CHUNK_SIZE; localX++) {
                    for (int localY = 0; localY < Constants.SECTION_SIZE; localY++) {
                        Block block = chunk.getBlock(localX, originY + localY, localZ);
                        if (!block.emitsLight()) {
                            continue;
                        }
                        int x = originX + localX;
                        int z = originZ + localZ;
                        setLight(world, false, x, originY + localY, z, block.lightEmission());
                        offer(false, x, originY + localY, z, block.lightEmission());
                    }
                }
            }
        }
    }

    /** Offers the light of the cells just outside a chunk, so a chunk lights the one beside it. */
    private void seedBorder(World world, Chunk chunk) {
        int originX = chunk.originX();
        int originZ = chunk.originZ();
        int lowX = originX - 1;
        int highX = originX + Constants.CHUNK_SIZE;
        int lowZ = originZ - 1;
        int highZ = originZ + Constants.CHUNK_SIZE;
        for (int y = Constants.MIN_Y; y <= Constants.MAX_Y; y++) {
            for (int offset = 0; offset < Constants.CHUNK_SIZE; offset++) {
                offerLightOf(world, lowX, y, originZ + offset);
                offerLightOf(world, highX, y, originZ + offset);
                offerLightOf(world, originX + offset, y, lowZ);
                offerLightOf(world, originX + offset, y, highZ);
            }
        }
    }

    /** Offers the light one cell holds, so the spread may carry it on. */
    private void offerLightOf(World world, int x, int y, int z) {
        if (!loaded(world, x, z)) {
            return;
        }
        int sky = world.peekSkyLight(x, y, z);
        if (sky > 0) {
            offer(true, x, y, z, sky);
        }
        int block = world.peekBlockLight(x, y, z);
        if (block > 0) {
            offer(false, x, y, z, block);
        }
    }

    /**
     * {@code true} when a cell of the sky stands next to a cell that is darker than it.
     * <p>
     * A cell of the chunk that is being lit is read out of the chunk itself and a cell just outside it out
     * of the world, because this question is asked for every cell of the sky and a map lookup per neighbour
     * would cost more than the whole rest of the lighting.
     */
    private boolean offersSkyToSomething(World world, Chunk chunk, int localX, int y, int localZ) {
        for (int face = 0; face < NEIGHBOUR_X.length; face++) {
            int ny = y + NEIGHBOUR_Y[face];
            if (World.outsideTheWorld(ny)) {
                continue;
            }
            int nx = localX + NEIGHBOUR_X[face];
            int nz = localZ + NEIGHBOUR_Z[face];
            int level;
            if (nx >= 0 && nx < Constants.CHUNK_SIZE && nz >= 0 && nz < Constants.CHUNK_SIZE) {
                // A section that holds nothing but air carries no light map either, and there the dark is.
                Section section = chunk.section(ny / Section.SIZE);
                level = section == null ? 0 : section.skyLight(nx, Chunk.localOf(ny), nz);
            } else {
                level = world.peekSkyLight(chunk.originX() + nx, ny, chunk.originZ() + nz);
            }
            if (level != Block.MAX_LIGHT) {
                return true;
            }
        }
        return false;
    }

    /** {@code true} when the chunk a cell lies in is loaded, which is where the light may be written. */
    private static boolean loaded(World world, int x, int z) {
        return world.chunkIfLoaded(Chunk.chunkOf(x), Chunk.chunkOf(z)) != null;
    }

    /** The queues of a spread: one per level of the light map, see {@link #spread}. */
    private static IntArray[] buckets() {
        IntArray[] buckets = new IntArray[Block.MAX_LIGHT + 1];
        for (int level = 0; level <= Block.MAX_LIGHT; level++) {
            buckets[level] = new IntArray();
        }
        return buckets;
    }

    /** Offers a cell to the spread of one of the two channels, at the level it spreads from. */
    private void offer(boolean sky, int x, int y, int z, int level) {
        if (level <= 0) {
            return;
        }
        (sky ? skyBuckets : blockBuckets)[Math.min(level, Block.MAX_LIGHT)].add(x, y, z);
    }

    /**
     * Carries the light of the cells that are waiting on, one level per step.
     * <p>
     * <b>The queues are walked from the brightest level down</b>, and that is what makes the spread cheap:
     * the light of the sky does not fade on its way down, so a cell that is lit from below can be as bright
     * as the cell that lit it, and a spread that walked its cells in the order they were found would come
     * back to the same cell again and again with a brighter light. Taken from the brightest level down, no
     * cell is ever lit twice.
     */
    private void spread(World world, boolean sky) {
        IntArray[] buckets = sky ? skyBuckets : blockBuckets;
        for (int level = Block.MAX_LIGHT; level > 0; level--) {
            IntArray bucket = buckets[level];
            while (bucket.size > 0) {
                int z = bucket.pop();
                int y = bucket.pop();
                int x = bucket.pop();
                if (lightAt(world, sky, x, y, z) != level) {
                    // It was lit brighter or taken away since it was offered, so there is nothing to do.
                    continue;
                }
                for (int face = 0; face < NEIGHBOUR_X.length; face++) {
                    int nx = x + NEIGHBOUR_X[face];
                    int ny = y + NEIGHBOUR_Y[face];
                    int nz = z + NEIGHBOUR_Z[face];
                    if (World.outsideTheWorld(ny) || !loaded(world, nx, nz)
                            || opaque(world.peekBlock(nx, ny, nz))) {
                        continue;
                    }
                    // The sky keeps its level on its way down, every other step costs one.
                    int next = sky && NEIGHBOUR_Y[face] < 0 ? level : level - 1;
                    if (next <= lightAt(world, sky, nx, ny, nz)) {
                        continue;
                    }
                    setLight(world, sky, nx, ny, nz, next);
                    offer(sky, nx, ny, nz, next);
                }
            }
        }
    }

    /**
     * Takes the light of a cell away and carries the removal on.
     * <p>
     * A cell that was lit by the one that is losing its light loses its own as well, and everything that
     * shines on its own is offered to the spread again - which is what keeps a cave lit by a second torch
     * when the first one is taken down.
     *
     * @param world world the cell lies in
     * @param sky {@code true} for the light of the sky, {@code false} for the light of the sources
     * @param x block X coordinate
     * @param y block Y coordinate, the height
     * @param z block Z coordinate
     */
    private void takeAway(World world, boolean sky, int x, int y, int z) {
        int level = lightAt(world, sky, x, y, z);
        if (level == 0) {
            return;
        }
        setLight(world, sky, x, y, z, 0);
        IntArray queue = sky ? skyRemoval : blockRemoval;
        IntArray levels = new IntArray();
        queue.add(x, y, z);
        levels.add(level);
        while (queue.size > 0) {
            int currentZ = queue.pop();
            int currentY = queue.pop();
            int currentX = queue.pop();
            int current = levels.pop();
            for (int face = 0; face < NEIGHBOUR_X.length; face++) {
                int nx = currentX + NEIGHBOUR_X[face];
                int ny = currentY + NEIGHBOUR_Y[face];
                int nz = currentZ + NEIGHBOUR_Z[face];
                if (World.outsideTheWorld(ny) || !loaded(world, nx, nz)) {
                    continue;
                }
                int neighbour = lightAt(world, sky, nx, ny, nz);
                if (neighbour == 0) {
                    continue;
                }
                // The sky is the one light that keeps its level on its way down, so a cell below that is
                // just as bright came from here; every other cell keeps its light if it is not dimmer.
                boolean fromHere = neighbour < current
                        || (sky && NEIGHBOUR_Y[face] < 0 && neighbour == current);
                if (fromHere) {
                    setLight(world, sky, nx, ny, nz, 0);
                    queue.add(nx, ny, nz);
                    levels.add(neighbour);
                } else {
                    // It shines on its own, so it is offered to the spread and its light is carried back.
                    offer(sky, nx, ny, nz, neighbour);
                }
            }
        }
        spread(world, sky);
    }

    /** Writes a level of one of the two light maps, counting the cell when the level really changed. */
    private void setLight(World world, boolean sky, int x, int y, int z, int level) {
        if (lightAt(world, sky, x, y, z) == level) {
            return;
        }
        if (sky) {
            world.setSkyLight(x, y, z, level);
        } else {
            world.setBlockLight(x, y, z, level);
        }
        changed++;
    }

    /** Reads a level of one of the two light maps. */
    private static int lightAt(World world, boolean sky, int x, int y, int z) {
        return sky ? world.peekSkyLight(x, y, z) : world.peekBlockLight(x, y, z);
    }

    // MARKER
}
