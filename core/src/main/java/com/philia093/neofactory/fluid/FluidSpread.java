package com.philia093.neofactory.fluid;

import com.badlogic.gdx.utils.LongMap;
import com.badlogic.gdx.utils.LongSet;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Works out where a fluid stands once it has run.
 * <p>
 * The class owns no world and keeps no state: it is handed the cells the fluid grows from and a
 * question - may the fluid stand here? - and answers with the state of every cell it reaches. A
 * level is the amount of sideward steps to the nearest source, so two sources that stand close to
 * each other share the water between them instead of arguing.
 * <p>
 * <b>A fluid falls for free and never climbs.</b> The walk visits the four sides of a cell and the
 * cell below it; stepping down costs nothing while stepping sideways costs one. Water that runs over
 * an edge therefore arrives at the foot of the cliff with the strength it had at the top and spreads
 * from there, and water that meets a wall stays where it is - which is the way a spill behaves in the
 * original game. A cell the fluid fell into is written with {@link FluidState#fallen(int)} instead of
 * a source, because it is as strong as a source but must run dry when the source above is taken away.
 * <p>
 * A cell that can fall does not spread sideways: the fluid takes the shortest way down and looks
 * around only where it cannot go further down. Without that a waterfall would soak the walls beside it
 * on its way, and a lake would grow a ring in the air at every edge.
 * <p>
 * Everything here is arithmetic on positions, so the spread of a fluid can be checked without a window
 * and without a chunk, see {@code FluidSpreadTest}. The world side - which cells are allowed, how fast
 * a ring follows the one before and what happens when a source is taken away - lives in
 * {@link FluidFlow}.
 */
public final class FluidSpread {

    /** Bits one coordinate of a packed cell uses, so a world of this size fits in three of them. */
    private static final int BITS = 21;

    /** Mask of one packed coordinate. */
    private static final int MASK = (1 << BITS) - 1;

    /** Bits the height is shifted by inside a packed cell. */
    private static final int SHIFT_Y = BITS;

    /** Bits the first horizontal axis is shifted by inside a packed cell. */
    private static final int SHIFT_X = 2 * BITS;

    /**
     * Answers whether a fluid may stand in a cell.
     * <p>
     * A fluid stands in an empty cell, in a cell that already holds the same fluid and in a cell that
     * holds something it floods, such as tall grass. A tree trunk, a wall, a machine and a different
     * fluid refuse it. A cell of a chunk that is not loaded refuses it as well, which keeps the water
     * of the world inside the part of it that is in memory, see {@link FluidFlow}.
     */
    @FunctionalInterface
    public interface Cells {

        /**
         * @param x block X coordinate
         * @param y block Y coordinate, the height
         * @param z block Z coordinate
         * @return {@code true} when the fluid may stand in that cell
         */
        boolean canHold(int x, int y, int z);
    }

    private FluidSpread() {
        // Utility class: never instantiated.
    }

    /**
     * Packs a cell into one number.
     * <p>
     * A walk works on thousands of cells and a set of them is asked over and over: one number per cell
     * instead of three keeps that cheap, and {@link #xOf(long)} and the two beside it read the
     * coordinates back.
     *
     * @param x block X coordinate
     * @param y block Y coordinate, the height
     * @param z block Z coordinate
     * @return the packed cell
     */
    public static long key(int x, int y, int z) {
        return ((long) (x & MASK) << SHIFT_X) | ((long) (y & MASK) << SHIFT_Y) | (z & MASK);
    }

    /** Block X coordinate of a cell packed by {@link #key(int, int, int)}. */
    public static int xOf(long cell) {
        return sign((int) (cell >>> SHIFT_X));
    }

    /** Block Y coordinate, the height, of a cell packed by {@link #key(int, int, int)}. */
    public static int yOf(long cell) {
        return sign((int) ((cell >>> SHIFT_Y) & MASK));
    }

    /** Block Z coordinate of a cell packed by {@link #key(int, int, int)}. */
    public static int zOf(long cell) {
        return sign((int) (cell & MASK));
    }

    /** Reads a packed coordinate back as a number that may be negative. */
    private static int sign(int value) {
        return value << (Integer.SIZE - BITS) >> (Integer.SIZE - BITS);
    }

    /** The cell below this one: where a fluid goes first. */
    public static long below(long cell) {
        return key(xOf(cell), yOf(cell) - 1, zOf(cell));
    }

    /** The cell above this one: where the fluid of a fallen cell came from. */
    public static long above(long cell) {
        return key(xOf(cell), yOf(cell) + 1, zOf(cell));
    }

    /**
     * The four cells a fluid may run into from here.
     * <p>
     * Only the sides: the height is asked apart from them, because a fluid reaches it under rules of
     * its own - falling is free and climbing never happens. The order is the northern, the southern,
     * the eastern and the western neighbour.
     *
     * @param cell cell in the middle
     * @return the cells beside it
     */
    public static long[] sides(long cell) {
        int x = xOf(cell);
        int y = yOf(cell);
        int z = zOf(cell);
        return new long[] {key(x, y, z + 1), key(x, y, z - 1), key(x + 1, y, z), key(x - 1, y, z)};
    }

    /**
     * States of every cell a fluid reaches from its sources.
     * <p>
     * A source that is refused by {@code cells} is dropped, which keeps a source that was buried by a
     * wall from feeding water through it. A cell that two sources reach keeps the state the walk found
     * first, which is the stronger one: the walk visits a cell of level {@code n} before any cell of
     * level {@code n + 1}, so the first state that reaches a cell is the one closest to a source.
     *
     * @param sources cells the fluid grows from
     * @param fluid fluid that runs, decides how far it reaches sideways
     * @param cells what the fluid may stand in
     * @return the state of every reached cell, never {@code null}
     */
    public static LongMap<FluidState> settle(LongSet sources, Fluid fluid, Cells cells) {
        LongMap<FluidState> reached = new LongMap<>();
        Deque<Long> frontier = new ArrayDeque<>();

        for (LongSet.LongSetIterator iterator = sources.iterator(); iterator.hasNext;) {
            long source = iterator.next();
            if (reached.containsKey(source) || !canHold(cells, source)) {
                continue;
            }
            reached.put(source, FluidState.SOURCE);
            frontier.addLast(source);
        }

        while (!frontier.isEmpty()) {
            long cell = frontier.pollFirst();
            FluidState state = reached.get(cell);
            long down = below(cell);
            if (!reached.containsKey(down) && canHold(cells, down)) {
                // Falling costs nothing, so the foot of the fall is as strong as the cell it came from.
                // The cell does not spread sideways while it can fall: the fluid takes the shortest way
                // down and looks around where it lands.
                reached.put(down, FluidState.fallen(state.level()));
                frontier.addFirst(down);
                continue;
            }
            if (state.level() >= fluid.range()) {
                continue;
            }
            for (long side : sides(cell)) {
                if (reached.containsKey(side) || !canHold(cells, side)) {
                    continue;
                }
                reached.put(side, FluidState.flowing(state.level() + 1));
                frontier.addLast(side);
            }
        }
        return reached;
    }

    /** {@code true} when the fluid may stand in a packed cell. */
    private static boolean canHold(Cells cells, long cell) {
        return cells.canHold(xOf(cell), yOf(cell), zOf(cell));
    }
}
