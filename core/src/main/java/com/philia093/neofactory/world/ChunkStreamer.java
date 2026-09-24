package com.philia093.neofactory.world;

import com.philia093.neofactory.util.Constants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static com.philia093.neofactory.util.Constants.CHUNK_SIZE;

/**
 * Keeps the chunks around the player loaded and drops the ones behind.
 * <p>
 * Without unloading, a world grows with every step the player takes: the chunk
 * table only ever gained entries, so walking for an hour meant holding the terrain
 * of an hour in memory. The streamer closes that loop, which is what makes the
 * memory use of a world follow the view distance instead of the distance walked.
 * <p>
 * Two numbers are involved, and they are not the same:
 * <ul>
 *     <li>the <b>load radius</b> is what has to be in memory. It is at least the
 *         {@link #viewDistance() view distance} and grows with the zoom, because a
 *         chunk outside memory is a hole in the picture.</li>
 *     <li>the <b>keep radius</b> is the load radius plus
 *         {@link Constants#CHUNK_UNLOAD_MARGIN}, and it is where chunks are
 *         dropped. The difference between the two is a buffer: the player can walk
 *         back and forth across a chunk border without anything being loaded
 *         twice.</li>
 * </ul>
 * Loading is limited by {@link Constants#CHUNK_LOADS_PER_FRAME} so that walking
 * into new terrain never costs one long frame, while unloading is free of that
 * limit: it only writes a chunk that the player changed, which the store keeps.
 */
public final class ChunkStreamer {

    private static final Logger LOGGER = LogManager.getLogger();

    /**
     * Time one frame may spend revealing new terrain, in nanoseconds.
     * <p>
     * A chunk costs a few milliseconds to make, and a player who walks into new terrain asks for one after
     * the other. The budget is what keeps such a frame short: revealing slower means the terrain appears a
     * little later, which no player notices, while a frame that runs three times as long is felt at once.
     */
    private static final long REVEAL_BUDGET_NANOS = 3_000_000L;

    /** Minimum chunks kept in memory, follows the view distance. */
    private int viewDistance = Constants.CHUNK_VIEW_DISTANCE;

    /** Chunks loaded by the most recent {@link #update} call. */
    private int lastLoaded;

    /** Chunks dropped by the most recent {@link #update} call. */
    private int lastUnloaded;

    /** Chunks the player keeps around, see {@link Constants#CHUNK_VIEW_DISTANCE}. */
    public int viewDistance() {
        return viewDistance;
    }

    /**
     * Sets how many chunks the player keeps around.
     * <p>
     * The value is clamped, and shrinking it takes effect on the next update, which
     * then drops whatever fell out of the kept area.
     *
     * @param viewDistance requested amount of chunks, in every direction
     */
    public void setViewDistance(int viewDistance) {
        int clamped = Math.max(Constants.CHUNK_VIEW_DISTANCE_MIN,
                Math.min(Constants.CHUNK_VIEW_DISTANCE_MAX, viewDistance));
        if (clamped != this.viewDistance) {
            LOGGER.info("View distance changed from {} to {} chunks", this.viewDistance, clamped);
        }
        this.viewDistance = clamped;
    }

    /**
     * Chunks that have to be in memory for the visible area.
     * <p>
     * The zoom decides how much terrain fits on screen, so a zoomed out camera asks
     * for more chunks than the view distance alone. One chunk is added on top of the
     * visible area: terrain and decorations are planted when a chunk is finished, so
     * the margin keeps new trees from appearing out of nothing inside the view.
     *
     * @param halfVisibleBlocks half the amount of blocks that fit on screen
     * @return the radius in chunks
     */
    public int loadRadius(float halfVisibleBlocks) {
        int visible = (int) Math.ceil(halfVisibleBlocks / CHUNK_SIZE) + 1;
        return Math.max(viewDistance, visible);
    }

    /**
     * Fills the visible area without a budget.
     * <p>
     * Used when a world is opened and when the window size changes: the player has
     * nothing to see yet, so a frame in which a few hundred chunks are produced is
     * the right trade. Everything that happens while the world runs goes through
     * {@link #update(World, float, float, float)} instead, which spreads the work.
     *
     * @param world world to fill
     * @param playerBlockX block X coordinate of the player
     * @param playerBlockY block Y coordinate of the player
     * @param halfVisibleBlocks half the amount of blocks that fit on screen
     */
    public void fill(World world, float playerBlockX, float playerBlockY,
            float halfVisibleBlocks) {
        int loadRadius = loadRadius(halfVisibleBlocks);
        lastLoaded = world.loadChunksAround(playerBlockX, playerBlockY, loadRadius,
                Integer.MAX_VALUE);
        lastUnloaded = 0;
    }

    /**
     * Loads and drops chunks around the player.
     * <p>
     * Dropping happens first and over a larger square than loading, see
     * {@link Constants#CHUNK_UNLOAD_MARGIN}, so the terrain the player is about to
     * see is never generated twice in one frame.
     *
     * @param world world to keep up to date
     * @param playerBlockX block X coordinate of the player
     * @param playerBlockY block Y coordinate of the player
     * @param halfVisibleBlocks half the amount of blocks that fit on screen
     */
    public void update(World world, float playerBlockX, float playerBlockY,
            float halfVisibleBlocks) {
        int loadRadius = loadRadius(halfVisibleBlocks);
        int centerChunkX = Chunk.chunkOf((int) Math.floor(playerBlockX));
        int centerChunkY = Chunk.chunkOf((int) Math.floor(playerBlockY));

        lastUnloaded = world.unloadChunksOutside(centerChunkX, centerChunkY,
                loadRadius + Constants.CHUNK_UNLOAD_MARGIN);
        // Making a chunk costs a few milliseconds, so a frame may not reveal as many as it could: the
        // reveal runs to a budget of its own and stops there, which keeps a frame that walks into new
        // terrain as short as one that stands still. The count is still the ceiling, so a fast machine
        // reveals terrain at the pace the constant allows.
        lastLoaded = 0;
        long deadline = System.nanoTime() + REVEAL_BUDGET_NANOS;
        while (lastLoaded < Constants.CHUNK_LOADS_PER_FRAME) {
            int loaded = world.loadChunksAround(playerBlockX, playerBlockY, loadRadius, 1);
            if (loaded == 0) {
                break;
            }
            lastLoaded += loaded;
            if (System.nanoTime() >= deadline) {
                break;
            }
        }
    }

    /** Chunks loaded by the most recent update. */
    public int lastLoaded() {
        return lastLoaded;
    }

    /** Chunks dropped by the most recent update. */
    public int lastUnloaded() {
        return lastUnloaded;
    }

    @Override
    public String toString() {
        return "ChunkStreamer(view " + viewDistance + ", loaded " + lastLoaded + ", unloaded "
                + lastUnloaded + ")";
    }
}
