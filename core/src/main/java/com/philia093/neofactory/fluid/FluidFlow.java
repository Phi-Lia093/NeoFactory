package com.philia093.neofactory.fluid;

import com.badlogic.gdx.utils.LongMap;
import com.badlogic.gdx.utils.LongSet;
import com.philia093.neofactory.block.Block;
import com.philia093.neofactory.block.BlockRegistry;
import com.philia093.neofactory.block.Blocks;
import com.philia093.neofactory.util.Constants;
import com.philia093.neofactory.world.Chunk;
import com.philia093.neofactory.world.Section;
import com.philia093.neofactory.world.World;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The fluids of a world, run one look at a time.
 * <p>
 * <b>A look is the unit of work.</b> The world tells this class where something happened - a block was
 * placed, a bucket was poured, a chunk came back into memory - and the class writes down those cells. In
 * every tick of a fluid it takes the cells that wait for it, works out where that fluid belongs with
 * {@link FluidSpread} and writes the answer into the world: one ring of a spill per look, so water runs
 * instead of appearing at once.
 * <p>
 * <b>What a look covers.</b> A mark may already be empty, so a window around every mark is looked at,
 * {@code range + 1} cells to each side, and the fluid inside it is what the look is about. A window is
 * bounded by the fluid and not by a guard, and a mark that an earlier window already covers is skipped,
 * so a lake costs one window and not one per cell of it.
 * <p>
 * <b>A cell that should hold nothing is emptied ring by ring</b>, which lets a lake dry out from its
 * edges instead of blinking away - and it stops as soon as nothing changed and nothing is on its way, so
 * a settled body of water costs nothing at all.
 * <p>
 * <b>What this class never does:</b> it does not generate terrain. A fluid runs inside the chunks that
 * are loaded, because pulling a chunk out of the seed because a lake reached its border would let the
 * view distance depend on the water. A chunk that is loaded again tells the fluid where it stands, see
 * {@link #noticeLoaded(World, Chunk)}.
 */
public final class FluidFlow {

    /**
     * Cells one window covers, counted per look.
     * <p>
     * A window is walked once per mark, and a mark that an earlier window already covers costs nothing.
     * The limit is what keeps one tick cheap when a body of water holds more marks than a look can walk:
     * the cells this look did cover are exactly what a source inside their window asks for, so the rest
     * of the body follows on later ticks instead of costing the whole of it at once.
     */
    private static final int WINDOWS_PER_LOOK = 48;

    /** Cells that wait for a look, one set per fluid, in the order they were touched. */
    private final Map<Fluid, LongSet> waiting = new LinkedHashMap<>();

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
     * Only a cell that carries a fluid or touches one is remembered, so building a house in the middle
     * of a desert costs nothing.
     *
     * @param world world that changed
     * @param x block X coordinate
     * @param y block Y coordinate, the height
     * @param z block Z coordinate
     */
    public void mark(World world, int x, int y, int z) {
        if (writing) {
            return;
        }
        for (Fluid fluid : Fluids.all()) {
            if (touches(world, fluid, x, y, z)) {
                waiting.computeIfAbsent(fluid, key -> new LongSet()).add(FluidSpread.key(x, y, z));
            }
        }
    }

    /**
     * Looks at the fluids of a chunk that was just read.
     * <p>
     * A chunk that was unloaded stopped running, because the world only works on what is in memory. Its
     * fluids are therefore looked at once it returns, which lets a lake that was left half spilled finish
     * running.
     * <p>
     * One cell per column is marked, and only sections that hold something are read: a mark is enough to
     * find the body of fluid it belongs to, see {@link #regionOf(World, Fluid, LongSet)}, while reading
     * every cell of every chunk would make loading one as expensive as running the whole world.
     *
     * @param world world the chunk belongs to
     * @param chunk chunk that was loaded
     */
    public void noticeLoaded(World world, Chunk chunk) {
        for (int sectionY = 0; sectionY < Constants.SECTION_COUNT; sectionY++) {
            if (chunk.isEmptySection(sectionY)) {
                continue;
            }
            Section section = chunk.section(sectionY);
            int originY = Constants.MIN_Y + sectionY * Section.SIZE;
            for (int localZ = 0; localZ < Section.SIZE; localZ++) {
                for (int localX = 0; localX < Section.SIZE; localX++) {
                    for (int localY = 0; localY < Section.SIZE; localY++) {
                        Block block = BlockRegistry.byId(section.rawId(localX, localY, localZ));
                        if (Fluids.byBlock(block) == null) {
                            continue;
                        }
                        mark(world, chunk.originX() + localX, originY + localY,
                                chunk.originZ() + localZ);
                        break;
                    }
                }
            }
        }
    }

    /**
     * Advances every fluid that waits for a look.
     * <p>
     * A fluid is only looked at in the ticks of its own {@link Fluid#tickInterval()}, so water runs and
     * lava crawls without either of them asking how fast the other one is.
     *
     * @param world world to change
     * @param tickCount tick the world is in, decides whose turn it is
     * @return amount of cells that changed
     */
    public int tick(World world, long tickCount) {
        int changed = 0;
        settledCells = 0;
        for (Map.Entry<Fluid, LongSet> entry : waiting.entrySet()) {
            LongSet marks = entry.getValue();
            if (marks.size == 0 || tickCount % entry.getKey().tickInterval() != 0) {
                continue;
            }
            changed += advance(world, entry.getKey(), marks);
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
        for (LongSet marks : waiting.values()) {
            total += marks.size;
        }
        return total;
    }

    /**
     * Brings one fluid one step closer to where it belongs.
     * <p>
     * The cells that live from a source are collected first, then the state every cell should have is
     * worked out and applied. A source and a cell the fluid fell into are written right away, every other
     * cell has to wait until the ring inside it carries the fluid, which spreads a spill over several
     * ticks and makes it run.
     * <p>
     * A cell that should hold nothing - because no source feeds it any more - is cleared in the same
     * step. The fluid stops waiting for a look once nothing changed and nothing is on its way, see
     * {@link #regionOf(World, Fluid, LongSet)} for how much of a body of fluid one look covers.
     *
     * @param world world to change
     * @param fluid fluid to advance
     * @param marks cells that changed since the last look
     * @return amount of cells that changed
     */
    private int advance(World world, Fluid fluid, LongSet marks) {
        LongSet region = regionOf(world, fluid, marks);
        settledCells += region.size;
        LongMap<FluidState> wanted = FluidSpread.settle(sourcesOf(world, fluid, region), fluid,
                (x, y, z) -> canHold(world, fluid, x, y, z));

        // Two sets tell what was there when the look began: the fluid of the window, which decides how a
        // spill dries up, and the part of it that already sits where it belongs, which is the ground the
        // next ring stands on.
        LongSet carried = new LongSet();
        for (LongSet.LongSetIterator iterator = region.iterator(); iterator.hasNext;) {
            carried.add(iterator.next());
        }
        LongSet before = settledIn(world, fluid, wanted);
        int changed = 0;
        boolean waiting = false;
        for (LongMap.Entry<FluidState> entry : wanted) {
            long cell = entry.key;
            if (entry.value.isFalling() && state(world, cell, fluid) != null) {
                // The walk reached a cell that already carries the fluid and wants it to be one it fell
                // into. That would turn a source of water into a cell that has to be fed from above, and a
                // body of water would churn forever - so a cell that is already there owns its state. A
                // cell that holds nothing is filled, which is what lets water run into a hole.
                continue;
            }
            if (before.contains(cell) || !reached(cell, entry.value, wanted, before)) {
                // The ring inside this cell is not full yet, so it waits for the next look - which needs
                // no flag here: the ring it grows from is being written by this very look, so the next
                // one has something to do and the fluid cannot fall asleep before its outermost ring
                // sits. Only a cell that is taken away has to say so, see below.
                continue;
            }
            if (write(world, cell, fluid, entry.value)) {
                changed++;
            }
        }

        // A cell that lived from a source that is gone carries fluid that nothing feeds. It is taken away
        // ring by ring, so a lake runs out instead of vanishing in a single step.
        for (LongSet.LongSetIterator iterator = region.iterator(); iterator.hasNext;) {
            long cell = iterator.next();
            if (wanted.containsKey(cell)) {
                continue;
            }
            if (isEdge(cell, carried) && clear(world, cell, fluid)) {
                changed++;
                // The cells around the one that went are the rim of the next look: without that, the rest
                // of the lake would lose its connection to the break and be forgotten.
                for (long neighbour : neighbours(cell)) {
                    marks.add(neighbour);
                }
            } else {
                // It has to go, but it does not lie at the rim of the spill yet, so the fluid has to keep
                // looking: the ring outside it goes first, and this cell follows on a later look.
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
     * Cells of the fluid that lie around the marks.
     * <p>
     * A mark may already be empty - the source a bucket was just taken out of - so a window around every
     * mark is looked at: the fluid that lived from that source is what has to be found. The window is as
     * wide as the fluid reaches sideways plus one, because nothing farther away can be moved by what
     * happened at the mark.
     * <p>
     * <b>Why a window and not the body of the fluid:</b> an earlier version walked along the fluid itself,
     * which sounds tidier until a lake is large. The walk then had to be cut off by a guard, because its
     * cost grows with the amount of water, and the cut ran straight through the middle of the lake: the
     * cells at the cut never found the ring they were growing from, the fluid never settled, and every
     * tick was spent on the same lake again. A window is bounded by the fluid instead of by a guard, so
     * one look is cheap and complete at once.
     * <p>
     * A mark that an earlier window already covers is skipped - it lies in the region then - which is what
     * keeps a lake from being walked once per cell it has: the window of one cell of a lake covers every
     * cell within {@code range + 1}, and the marks of its neighbours fall inside it.
     *
     * @param world world to read
     * @param fluid fluid to gather
     * @param marks cells that changed since the last look
     * @return the cells that currently carry the fluid, empty when there are none
     */
    private LongSet regionOf(World world, Fluid fluid, LongSet marks) {
        int reach = fluid.range() + 1;
        LongSet region = new LongSet();
        int windows = 0;
        for (LongSet.LongSetIterator iterator = marks.iterator(); iterator.hasNext;) {
            long mark = iterator.next();
            if (region.contains(mark)) {
                continue;
            }
            if (windows++ >= WINDOWS_PER_LOOK) {
                // A body of water may hold more marks than one look may walk, and a mark that waits is
                // not lost: the state of the cells this look did cover is exactly what a source inside
                // this window asks for, and the rest of the body follows on later ticks. Without the
                // limit a sea costs one window per cell it touches in a single tick.
                break;
            }
            int x = FluidSpread.xOf(mark);
            int y = FluidSpread.yOf(mark);
            int z = FluidSpread.zOf(mark);
            for (int offsetY = -reach; offsetY <= reach; offsetY++) {
                for (int offsetZ = -reach; offsetZ <= reach; offsetZ++) {
                    for (int offsetX = -reach; offsetX <= reach; offsetX++) {
                        long cell = FluidSpread.key(x + offsetX, y + offsetY, z + offsetZ);
                        if (region.contains(cell) || state(world, cell, fluid) == null) {
                            continue;
                        }
                        region.add(cell);
                    }
                }
            }
        }
        return region;
    }

    /**
     * Sources that feed the fluid of a region.
     * <p>
     * Every cell of the region is asked whether it is a source, which ties this to
     * {@link #regionOf(World, Fluid, LongSet)}: the region is exactly the fluid the marks belong to, and
     * the sources inside it are the ones that keep it alive. A source outside the region cannot reach a
     * cell of it either, because the spread of a source stops at its own range and the cells in between
     * carry no fluid.
     * <p>
     * A cell the fluid fell into is not a source, see {@link FluidState}: it is as strong as one, but
     * nothing feeds the fluid from it once the source above is gone.
     *
     * @param world world to read
     * @param fluid fluid to look for
     * @param region cells of the fluid
     * @return the cells of the region that are sources
     */
    private static LongSet sourcesOf(World world, Fluid fluid, LongSet region) {
        LongSet sources = new LongSet();
        for (LongSet.LongSetIterator iterator = region.iterator(); iterator.hasNext;) {
            long cell = iterator.next();
            FluidState state = state(world, cell, fluid);
            if (state != null && state.isSource()) {
                sources.add(cell);
            }
        }
        return sources;
    }

    /**
     * Cells of a spill that already hold what they should.
     * <p>
     * This is the set {@link #reached(long, FluidState, LongMap, LongSet)} and
     * {@link #isEdge(long, LongSet)} ask: a cell is only written once the ring inside it was there when
     * the look began, and a cell is only taken away when a neighbour was not.
     * <p>
     * It is read from the spill itself and not from the window around the marks, and that is what lets a
     * spill grow past that window: the ring written by the look before may well lie outside it, and it
     * still counts as the ground the next ring stands on.
     *
     * @param world world to read
     * @param fluid fluid to look for
     * @param wanted state every cell of the spill should have
     * @return the cells that already hold their state
     */
    private static LongSet settledIn(World world, Fluid fluid, LongMap<FluidState> wanted) {
        LongSet settled = new LongSet();
        for (LongMap.Entry<FluidState> entry : wanted) {
            FluidState current = state(world, entry.key, fluid);
            if (current != null && current.equals(entry.value)) {
                settled.add(entry.key);
            }
        }
        return settled;
    }

    /**
     * {@code true} when a cell that is about to disappear lies at the rim of the spill.
     * <p>
     * Only one ring is taken away per look, which lets a lake dry up ring by ring instead of blinking
     * out: a cell whose neighbour did not carry the fluid when the look began lies at an edge of the
     * spill and is the one the fluid leaves first. The set of cells of the beginning of the look is what
     * is asked - reading the world again would let a whole lake run out in a single pass, because every
     * cell becomes an edge as soon as the one beside it is gone.
     * <p>
     * A spill has two edges while it dries up, the one at the break where the source was taken and the one
     * at its own border, and both give way: the water closes in from both sides and meets in the middle. A
     * source is never part of this - it only disappears when the player takes it, and that is what started
     * the look.
     *
     * @param cell cell that carries the fluid but should not
     * @param before cells that carried the fluid when the look began
     * @return {@code true} when the cell may be emptied now
     */
    private static boolean isEdge(long cell, LongSet before) {
        long[] sides = FluidSpread.sides(cell);
        for (int index = 0; index < sides.length; index++) {
            if (!before.contains(sides[index])) {
                return true;
            }
        }
        return !before.contains(FluidSpread.above(cell)) || !before.contains(FluidSpread.below(cell));
    }

    /**
     * The six cells around a cell: the four sides and the two heights.
     * <p>
     * Both heights belong to it because a fluid reaches them under rules of its own - it falls and never
     * climbs - so the cell at the foot of a waterfall stands at the rim of the spill just like the one at
     * the edge of a lake.
     *
     * @param cell cell in the middle
     * @return the cells around it
     */
    private static long[] neighbours(long cell) {
        long[] sides = FluidSpread.sides(cell);
        return new long[] {sides[0], sides[1], sides[2], sides[3], FluidSpread.above(cell),
                FluidSpread.below(cell)};
    }

    /**
     * {@code true} when the ring inside a cell already carries the fluid.
     * <p>
     * This is the rule that turns adding water into running water: a cell is only written once what feeds
     * it was there before this look, so a spill grows one ring per look instead of appearing at once. A
     * cell the fluid fell into is fed from above instead of from the side, see {@link FluidState}.
     *
     * @param cell cell that is about to be written
     * @param state state the cell should have
     * @param wanted state of every cell of the spill
     * @param before cells that carried the fluid when the look began
     * @return {@code true} when the cell may be written now
     */
    private static boolean reached(long cell, FluidState state, LongMap<FluidState> wanted,
            LongSet before) {
        if (state.isSource()) {
            return true;
        }
        if (state.isFalling()) {
            long source = FluidSpread.above(cell);
            FluidState rest = wanted.get(source);
            return rest != null && rest.level() == state.level() && before.contains(source);
        }
        for (long side : FluidSpread.sides(cell)) {
            FluidState behind = wanted.get(side);
            if (behind != null && behind.level() == state.level() - 1 && before.contains(side)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Writes a block with its state into the world.
     * <p>
     * Writing is what the fluid does to the world and the world reports it back as a change; the flag
     * around it is what keeps the fluid from waking itself up. Without it a lake that is exactly where it
     * belongs would wait for a look in every tick, forever.
     *
     * @param world world to change
     * @param cell cell to write
     * @param fluid fluid that is written
     * @param state state that belongs to it
     * @return {@code true} when the cell was written
     */
    private boolean write(World world, long cell, Fluid fluid, FluidState state) {
        int x = FluidSpread.xOf(cell);
        int y = FluidSpread.yOf(cell);
        int z = FluidSpread.zOf(cell);
        writing = true;
        try {
            world.setBlock(x, y, z, fluid.block());
            world.setState(x, y, z, state.pack());
        } finally {
            writing = false;
        }
        return true;
    }

    /**
     * Empties a cell of a fluid.
     * <p>
     * A cell that no longer carries the fluid of this look is left alone: another fluid may have taken it
     * over, or the player may have put a block there, and neither belongs to this one.
     *
     * @param world world to change
     * @param cell cell to empty
     * @param fluid fluid that leaves
     * @return {@code true} when the cell was emptied
     */
    private boolean clear(World world, long cell, Fluid fluid) {
        if (state(world, cell, fluid) == null) {
            return false;
        }
        int x = FluidSpread.xOf(cell);
        int y = FluidSpread.yOf(cell);
        int z = FluidSpread.zOf(cell);
        writing = true;
        try {
            world.setBlock(x, y, z, Blocks.AIR);
            world.setState(x, y, z, 0);
        } finally {
            writing = false;
        }
        return true;
    }

    /**
     * {@code true} when a cell carries the fluid or touches one.
     * <p>
     * The cell itself is part of the question: a block that was placed on top of water takes the water
     * away, and the cell that changed is then empty and only its neighbours carry anything.
     *
     * @param world world to read
     * @param fluid fluid to look for
     * @param x block X coordinate
     * @param y block Y coordinate, the height
     * @param z block Z coordinate
     * @return {@code true} when a change at that cell may move the fluid
     */
    private static boolean touches(World world, Fluid fluid, int x, int y, int z) {
        for (int offsetY = -1; offsetY <= 1; offsetY++) {
            for (int offsetZ = -1; offsetZ <= 1; offsetZ++) {
                for (int offsetX = -1; offsetX <= 1; offsetX++) {
                    Block block = world.peekBlock(x + offsetX, y + offsetY, z + offsetZ);
                    if (Fluids.byBlock(block) == fluid) {
                        return true;
                    }
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
     * @param fluid fluid the cell has to carry
     * @return the state, or {@code null} when the cell carries something else
     */
    private static FluidState state(World world, long cell, Fluid fluid) {
        int x = FluidSpread.xOf(cell);
        int y = FluidSpread.yOf(cell);
        int z = FluidSpread.zOf(cell);
        if (world.peekBlock(x, y, z) != fluid.block()) {
            return null;
        }
        return FluidState.unpack(world.peekState(x, y, z));
    }

    /**
     * {@code true} when a fluid may stand in a cell.
     * <p>
     * This is the rule the world itself follows, see {@link #canHold(World, Fluid, int, int, int)}. A
     * test asks it to show that the ground the generator lays around a lake really holds the water back.
     *
     * @param world world to read
     * @param fluid fluid that asks
     * @param x block X coordinate
     * @param y block Y coordinate, the height
     * @param z block Z coordinate
     * @return {@code true} when the cell takes that fluid
     */
    public static boolean mayStand(World world, Fluid fluid, int x, int y, int z) {
        return canHold(world, fluid, x, y, z);
    }

    /**
     * {@code true} when a fluid may stand in a cell.
     * <p>
     * An empty cell takes the fluid and a cell that already carries it keeps it, which is what makes a
     * spill spread and then stay. Everything that is not solid and carries no fluid is flooded, such as
     * tall grass - which is how a plant drowns. Stone, wood, a machine and a different fluid hold it
     * back, and a cell of a chunk that is not loaded refuses it as well: the water of the world runs
     * inside the part of it that is in memory.
     *
     * @param world world to read
     * @param fluid fluid that asks
     * @param x block X coordinate
     * @param y block Y coordinate, the height
     * @param z block Z coordinate
     * @return {@code true} when the cell takes that fluid
     */
    private static boolean canHold(World world, Fluid fluid, int x, int y, int z) {
        if (world.chunkIfLoaded(Chunk.chunkOf(x), Chunk.chunkOf(z)) == null
                || World.outsideTheWorld(y)) {
            return false;
        }
        Block block = world.peekBlock(x, y, z);
        if (block.isAir() || block == fluid.block()) {
            return true;
        }
        return !block.isSolid() && Fluids.byBlock(block) == null;
    }
}

