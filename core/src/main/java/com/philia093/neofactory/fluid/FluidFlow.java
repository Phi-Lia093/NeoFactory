package com.philia093.neofactory.fluid;

import com.philia093.neofactory.block.Block;
import com.philia093.neofactory.block.Blocks;
import com.philia093.neofactory.util.Constants;
import com.philia093.neofactory.world.BlockPos;
import com.philia093.neofactory.world.Chunk;
import com.philia093.neofactory.world.World;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Lets the fluids of a world run.
 * <p>
 * The world hands every change to this class, see
 * {@link World#setBlock(int, int, int, Block)}, and once a tick it is asked to advance the
 * fluids that were touched. A fluid is only ever looked at where something happened - a bucket
 * poured water, a source was taken away, a wall was built through a lake - so a world with a
 * hundred lakes costs nothing while nobody touches them.
 * <p>
 * <b>How a spill advances:</b> the cells a fluid may stand in are collected, the level of every
 * cell is worked out by {@link FluidSpread} and then applied ring by ring. A cell is only filled
 * once a cell of the ring before it already carries the fluid, which is what makes the water run
 * instead of appearing at once; {@link Fluid#tickInterval()} decides how many ticks lie between
 * two rings.
 * <p>
 * <b>How a spill disappears:</b> the levels are worked out from the sources every time, so a
 * source that was taken away simply does not feed anything any more. What lived from it is then
 * cleared ring by ring as well, outermost first, so a lake runs out the way it ran in and no
 * water is left behind by a bucket that took it back.
 * <p>
 * <b>Which layer a fluid runs in:</b> a fluid may stand in either layer and the two never mix.
 * Water in a hole the player dug lies on the ground layer, water over the grass lies in the layer
 * the player stands in, and a spill reaches through neither. The layer of the player also needs
 * ground below it - a fluid over a hole would float in the air - which is why a spill stops at
 * the rim of a dug out cell instead of running over it.
 * <p>
 * <b>What this class never does:</b> it does not generate terrain. A fluid runs inside the
 * chunks that are loaded, because pulling a chunk out of the seed because a lake reached its
 * border would let the view distance depend on the water. A chunk that is loaded again tells the
 * fluid where it stands, see {@link #noticeLoaded(World, Chunk)}.
 */
public final class FluidFlow {

    /** The layer of the ground, see {@link Chunk#LAYER_FLOOR}. */
    public static final int FLOOR_LAYER = Chunk.LAYER_FLOOR;

    /** The layer the player stands in, see {@link Chunk#LAYER_OBJECT}. */
    public static final int OBJECT_LAYER = Chunk.LAYER_OBJECT;

    /** The two layers of the world, the ground first, read only. */
    private static final int[] LAYERS = {FLOOR_LAYER, OBJECT_LAYER};

    /**
     * A fluid inside one layer, the thing this class advances.
     * <p>
     * A fluid may stand in either layer and the two never mix: water in a hole the player dug is
     * water of the ground, water over the grass is water of the layer above it, and a spill of one
     * of them reaches through neither. Keeping the layer beside the fluid is what makes that
     * simple - a look at the water of the ground never touches the water above it.
     *
     * @param fluid fluid that runs
     * @param layer layer it runs in, see {@link #FLOOR_LAYER} and {@link #OBJECT_LAYER}
     */
    private record Runner(Fluid fluid, int layer) {
    }

    /** Cells that wait for a look, one set per fluid and layer, in the order they were touched. */
    private final Map<Runner, Set<BlockPos>> waiting = new LinkedHashMap<>();

    /**
     * {@code true} while this class writes into the world itself.
     * <p>
     * Its own writes come back as changes, see {@link World#setBlock(int, int, int, Block)}, and
     * remembering them would make every lake wait for a look in every tick forever.
     */
    private boolean writing;

    /** Cells looked at by the last {@link #tick(World, long)} call, for the log. */
    private int settledCells;

    /**
     * Remembers that a cell changed and has to be looked at again.
     * <p>
     * Only a cell that carries a fluid or touches one is remembered, so building a house in the
     * middle of a desert costs nothing.
     *
     * @param world world that changed
     * @param x block coordinate along the first horizontal axis
     * @param y block coordinate along the second horizontal axis
     */
    public void mark(World world, int x, int y) {
        if (writing) {
            return;
        }
        for (Fluid fluid : Fluids.all()) {
            for (int layer : LAYERS) {
                if (touches(world, fluid, layer, x, y)) {
                    waiting.computeIfAbsent(new Runner(fluid, layer),
                            key -> new LinkedHashSet<>()).add(BlockPos.of(x, y));
                }
            }
        }
    }

    /**
     * Looks at the fluids of a chunk that was just read.
     * <p>
     * A chunk that was unloaded stopped running, because the world only works on what is in
     * memory. Its fluids are therefore looked at once it returns, which lets a lake that was left
     * half spilled finish running.
     *
     * @param world world the chunk belongs to
     * @param chunk chunk that was loaded
     */
    public void noticeLoaded(World world, Chunk chunk) {
        for (int localY = 0; localY < Constants.CHUNK_SIZE; localY++) {
            for (int localX = 0; localX < Constants.CHUNK_SIZE; localX++) {
                for (int layer : LAYERS) {
                    if (Fluids.byBlock(chunk.getBlock(localX, Chunk.flatY(layer), localY)) != null) {
                        mark(world, chunk.originX() + localX, chunk.originZ() + localY);
                        return;
                    }
                }
            }
        }
    }

    /**
     * Advances every fluid that waits for a look.
     * <p>
     * A fluid is only looked at in the ticks of its own {@link Fluid#tickInterval()}, so water
     * runs and lava crawls without either of them asking how fast the other one is.
     *
     * @param world world to change
     * @param tickCount tick the world is in, decides whose turn it is
     * @return amount of cells that changed
     */
    public int tick(World world, long tickCount) {
        int changed = 0;
        settledCells = 0;
        for (Map.Entry<Runner, Set<BlockPos>> entry : waiting.entrySet()) {
            Runner runner = entry.getKey();
            Set<BlockPos> marks = entry.getValue();
            if (marks.isEmpty()) {
                continue;
            }
            if (tickCount % runner.fluid().tickInterval() != 0) {
                continue;
            }
            changed += advance(world, runner, marks);
        }
        return changed;
    }

    /** Cells the last {@link #tick(World, long)} call looked at. */
    public int settledCellCount() {
        return settledCells;
    }

    /** Amount of cells that wait for a look, over every fluid. */
    public int pendingCellCount() {
        int total = 0;
        for (Set<BlockPos> marks : waiting.values()) {
            total += marks.size();
        }
        return total;
    }

    /**
     * Brings one fluid one step closer to where it belongs.
     * <p>
     * The cells that live from a source are collected first, then the level every cell should
     * have is worked out and applied. A source of the first ring is written right away, every
     * other cell has to wait until the ring inside it carries the fluid, which spreads a spill
     * over several ticks and makes it run.
     * <p>
     * A cell that should hold nothing - because no source feeds it any more - is cleared in the
     * same step. The fluid stops waiting for a look once nothing changed and nothing is on its way,
     * see {@link #regionOf(World, Runner, Set)} for how much of a body of fluid one look covers.
     *
     * @param world world to change
     * @param fluid fluid to advance
     * @param marks cells that changed since the last look
     * @return amount of cells that changed
     */
    private int advance(World world, Runner runner, Set<BlockPos> marks) {
        Set<BlockPos> region = regionOf(world, runner, marks);
        settledCells += region.size();
        Map<BlockPos, Integer> wanted = FluidSpread.settle(sourcesOf(world, runner, region),
                runner.fluid(), (x, y) -> canHold(world, runner, x, y));

        // The levels of the walk arrive ring by ring, so a cell of the first ring is written
        // before a cell of the second one and a source before both of them. Two sets tell what was
        // there when the look began: the water of the window, which decides how a spill dries up,
        // and the part of the spill that already sits where it belongs, which is the ground the
        // next ring stands on.
        Set<BlockPos> carried = new LinkedHashSet<>(region);
        Set<BlockPos> before = settledIn(world, runner, wanted);
        int changed = 0;
        boolean waiting = false;
        for (Map.Entry<BlockPos, Integer> entry : wanted.entrySet()) {
            BlockPos cell = entry.getKey();
            int level = entry.getValue();
            if (before.contains(cell)) {
                continue;
            }
            if (level > 0 && !reached(cell, level, wanted, before)) {
                // The ring inside this cell is not full yet, so it waits for the next look - which
                // needs no flag here: the ring it grows from is being written by this very look, so
                // the next one has something to do and the fluid cannot fall asleep before its
                // outermost ring sits. Only a cell that is taken away has to say so, see below.
                continue;
            }
            if (write(world, cell, runner, level == 0 ? FluidState.SOURCE
                    : FluidState.flowing(level))) {
                changed++;
            }
        }

        // A cell that lived from a source that is gone carries water that nothing feeds. It is
        // taken away ring by ring, so a lake runs out instead of vanishing in a single step.
        for (BlockPos cell : region) {
            if (wanted.containsKey(cell)) {
                continue;
            }
            if (isEdge(cell, carried) && clear(world, cell, runner)) {
                changed++;
                // The cells around the one that went are the rim of the next look: without that,
                // the rest of the lake would lose its connection to the break and be forgotten.
                marks.addAll(FluidSpread.neighbours(cell));
            } else {
                // It has to go, but it does not lie at the rim of the spill yet, so the fluid has to
                // keep looking: the ring outside it goes first, and this cell follows on a later
                // look.
                waiting = true;
            }
        }

        if (changed == 0 && !waiting) {
            // Nothing was written and nothing is on its way: the fluid is where it belongs.
            marks.clear();
        }
        return changed;
    }

    /**
     * Cells of a spill that already hold what they should.
     * <p>
     * This is the set {@link #reached(BlockPos, int, Map, Set)} and {@link #isEdge(BlockPos, Set)}
     * ask: a cell is only written once the ring inside it was there when the look began, and a cell
     * is only taken away when a neighbour was not.
     * <p>
     * It is read from the spill itself and not from the window around the marks, and that is what
     * lets a spill grow past that window: the ring written by the look before may well lie outside
     * it, and it still counts as the ground the next ring stands on. Reading only the window left
     * the outer rings of a spill waiting for a neighbour that could never turn up.
     *
     * @param world world to read
     * @param runner fluid and layer the spill belongs to
     * @param wanted level every cell of the spill should have
     * @return the cells that already hold their level
     */
    private static Set<BlockPos> settledIn(World world, Runner runner,
            Map<BlockPos, Integer> wanted) {
        Set<BlockPos> settled = new LinkedHashSet<>();
        for (Map.Entry<BlockPos, Integer> entry : wanted.entrySet()) {
            int level = entry.getValue();
            FluidState current = state(world, entry.getKey(), runner);
            if (current != null && current.level() == level && current.isSource() == (level == 0)) {
                settled.add(entry.getKey());
            }
        }
        return settled;
    }

    /**
     * {@code true} when a cell that is about to disappear lies at the rim of the spill.
     * <p>
     * Only one ring is taken away per look, which lets a lake dry up ring by ring instead of
     * blinking out: a cell whose neighbour did not carry the fluid when the look began lies at an
     * edge of the spill and is the one the fluid leaves first. The set of cells of the beginning of
     * the look is what is asked - reading the world again would let a whole lake run out in a
     * single pass, because every cell becomes an edge as soon as the one beside it is gone.
     * <p>
     * A spill has two edges while it dries up, the one at the break where the source was taken and
     * the one at its own border, and both give way: the water closes in from both sides and meets
     * in the middle. A source is never part of this - it only disappears when the player takes it,
     * and that is what started the look.
     *
     * @param cell cell that carries the fluid but should not
     * @param before cells that carried the fluid when the look began
     * @return {@code true} when the cell may be emptied now
     */
    private static boolean isEdge(BlockPos cell, Set<BlockPos> before) {
        for (BlockPos neighbour : FluidSpread.neighbours(cell)) {
            if (!before.contains(neighbour)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Stores a block with its fluid state in the layer of a runner.
     *
     * @param world world to change
     * @param cell cell to write
     * @param runner fluid and layer that is written
     * @param state state that belongs to the fluid
     * @return {@code true} when the cell was written
     */
    private boolean write(World world, BlockPos cell, Runner runner, FluidState state) {
        writing = true;
        try {
            world.setFlatBlock(cell.x(), cell.y(), runner.layer(), runner.fluid().block());
            world.setFlatState(cell.x(), cell.y(), runner.layer(), state.pack());
        } finally {
            writing = false;
        }
        return true;
    }

    /**
     * Empties a cell of the layer of a runner.
     *
     * @param world world to change
     * @param cell cell to empty
     * @param runner fluid and layer that is emptied
     * @return {@code true} when the cell was emptied
     */
    private boolean clear(World world, BlockPos cell, Runner runner) {
        writing = true;
        try {
            world.setFlatBlock(cell.x(), cell.y(), runner.layer(), Blocks.AIR);
            world.setFlatState(cell.x(), cell.y(), runner.layer(), 0);
        } finally {
            writing = false;
        }
        return true;
    }

    /**
     * Cells of the fluid that lie around the marks.
     * <p>
     * A mark may already be empty - the source a bucket was just taken out of - so a window around
     * every mark is looked at: the water that lived from that source is what has to be found. The
     * window is as wide as the fluid reaches, {@code range + 1} cells to each side, because nothing
     * farther away can be moved by what happened at the mark - a spread stops at its own range.
     * <p>
     * <b>Why a window and not the body of the fluid:</b> an earlier version walked along the fluid
     * itself, which sounds tidier until a lake is large. The walk then had to be cut off by a
     * guard, because its cost grows with the amount of water, and the cut ran straight through the
     * middle of the lake: the cells at the cut never found the ring they were growing from, the
     * fluid never settled, and every tick was spent on the same lake again. A window is bounded by
     * the fluid instead of by a guard, so one look is cheap and complete at once - and a lake is
     * worked on around whatever the player touched, which is the only part that can have changed.
     *
     * @param world world to read
     * @param runner fluid and layer to gather
     * @param marks cells that changed since the last look
     * @return the cells that currently carry the fluid, empty when there are none
     */
    private Set<BlockPos> regionOf(World world, Runner runner, Set<BlockPos> marks) {
        int reach = runner.fluid().range() + 1;
        Set<BlockPos> region = new LinkedHashSet<>();
        for (BlockPos mark : marks) {
            for (int offsetY = -reach; offsetY <= reach; offsetY++) {
                for (int offsetX = -reach; offsetX <= reach; offsetX++) {
                    BlockPos cell = mark.offset(offsetX, offsetY);
                    if (region.contains(cell) || state(world, cell, runner) == null) {
                        continue;
                    }
                    region.add(cell);
                }
            }
        }
        return region;
    }

    /**
     * Sources that feed the fluid of a region.
     * <p>
     * Every cell of the region is asked whether it is a source, which ties this to
     * {@link #regionOf(World, Runner, Set)}: the region is exactly the fluid the marks belong to,
     * and the sources inside it are the ones that keep it alive. A source outside the region cannot
     * reach a cell of it either, because the spread of a source stops at its own range and the
     * cells in between carry no fluid.
     * <p>
     * <b>Why not the neighbourhood of a mark:</b> reading only the sources around the mark used to
     * leave the rest of the region without one, and every cell of the fluid that no source of the
     * answer reached was then taken away - a lake lost its banks in a ring, a few cells away from
     * wherever the player had dug. Asking the region itself is what keeps a lake whole.
     *
     * @param world world to read
     * @param runner fluid and layer to look for
     * @param region cells of the fluid that were found around the marks
     * @return the sources that were found, empty when the spill has none
     */
    private Set<BlockPos> sourcesOf(World world, Runner runner, Set<BlockPos> region) {
        Set<BlockPos> sources = new LinkedHashSet<>();
        for (BlockPos cell : region) {
            FluidState state = state(world, cell, runner);
            if (state != null && state.isSource()) {
                sources.add(cell);
            }
        }
        return sources;
    }

    /**
     * {@code true} when a fluid may stand in a cell of the layer of a runner.
     * <p>
     * An empty cell takes the fluid and a cell that already carries it keeps it. In the layer the
     * player stands in, a cell that holds something soft - tall grass, a plant the player may walk
     * through - is flooded, while a tree trunk, a wall, a machine and every other solid block hold
     * the fluid back; a cell of another fluid refuses it as well, so a lake of water never covers
     * lava.
     * <p>
     * The layer of the ground is stricter: it only takes a fluid where it is empty, because a
     * fluid that lay there would replace the very ground it stands on. A fluid of that layer
     * therefore lives in the holes the player dug.
     * <p>
     * The layer of the player needs ground below it - a fluid over a hole would float in the air -
     * and a cell whose chunk is not loaded refuses the fluid as well, see {@link FluidFlow}.
     *
     * @param world world to read
     * @param runner fluid and layer that ask
     * @param x block coordinate along the first horizontal axis
     * @param y block coordinate along the second horizontal axis
     * @return {@code true} when the fluid may stand there
     */
    private static boolean canHold(World world, Runner runner, int x, int y) {
        if (world.chunkIfLoaded(Chunk.chunkOf(x), Chunk.chunkOf(y)) == null) {
            return false;
        }
        int layer = runner.layer();
        Block block = world.peekFlatBlock(x, y, layer);
        boolean room = block.isAir() || block == runner.fluid().block();
        if (!room && layer == OBJECT_LAYER) {
            room = !block.isSolid() && Fluids.byBlock(block) == null;
        }
        if (!room) {
            return false;
        }
        return layer != OBJECT_LAYER || world.hasFlatGround(x, y);
    }

    /**
     * {@code true} when a cell of a layer carries the fluid or touches one.
     *
     * @param world world to read
     * @param fluid fluid to look for
     * @param layer layer to look in
     * @param x block coordinate along the first horizontal axis
     * @param y block coordinate along the second horizontal axis
     * @return {@code true} when a change at that cell may move the fluid
     */
    private static boolean touches(World world, Fluid fluid, int layer, int x, int y) {
        for (int offsetY = -1; offsetY <= 1; offsetY++) {
            for (int offsetX = -1; offsetX <= 1; offsetX++) {
                if (Fluids.byBlock(world.peekFlatBlock(x + offsetX, y + offsetY, layer)) == fluid) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * State of a cell, read without generating anything.
     *
     * @param world world to read
     * @param cell cell to read
     * @param runner fluid and layer the cell has to carry
     * @return the state, or {@code null} when the cell carries something else
     */
    private static FluidState state(World world, BlockPos cell, Runner runner) {
        if (world.peekFlatBlock(cell.x(), cell.y(), runner.layer()) != runner.fluid().block()) {
            return null;
        }
        return FluidState.unpack(world.peekFlatState(cell.x(), cell.y(), runner.layer()));
    }

    /**
     * {@code true} when the ring inside a cell already carries the fluid.
     * <p>
     * This is the rule that turns adding water into running water: a cell of level {@code n} is
     * only written once a neighbour of level {@code n - 1} held the fluid before this look, so a
     * spill grows one ring per look instead of appearing at once. The rings of a look are then
     * written in the order of the walk, which is the order of their level.
     *
     * @param cell cell that is about to be written
     * @param level level the cell should have
     * @param wanted level of every cell of the spill
     * @param before cells that carried the fluid when the look began
     * @return {@code true} when the cell may be written now
     */
    private static boolean reached(BlockPos cell, int level, Map<BlockPos, Integer> wanted,
            Set<BlockPos> before) {
        for (BlockPos neighbour : FluidSpread.neighbours(cell)) {
            Integer behind = wanted.get(neighbour);
            if (behind != null && behind == level - 1 && before.contains(neighbour)) {
                return true;
            }
        }
        return false;
    }

    /**
     * {@code true} when a fluid may stand in a cell of a layer.
     * <p>
     * This is the rule the world itself follows, see {@code canHold}: an empty cell takes the
     * fluid, a cell that already carries it keeps it, a cell of the ground layer takes nothing
     * else, and a cell of the layer of the player is flooded when it holds something soft. A test
     * asks it to show that the ring the generator lays around a lake really holds the water back.
     *
     * @param world world to read
     * @param fluid fluid that asks
     * @param x block coordinate along the first horizontal axis
     * @param y block coordinate along the second horizontal axis
     * @param layer layer to look in, see {@link #FLOOR_LAYER} and {@link #OBJECT_LAYER}
     * @return {@code true} when the cell takes that fluid
     */
    public static boolean mayStand(World world, Fluid fluid, int x, int y, int layer) {
        return canHold(world, new Runner(fluid, layer), x, y);
    }
}