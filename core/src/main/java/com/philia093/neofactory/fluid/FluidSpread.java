package com.philia093.neofactory.fluid;

import com.philia093.neofactory.world.BlockPos;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Works out where a fluid stands once it has run.
 * <p>
 * The class owns no world and keeps no state: it is handed the cells the fluid grows from and
 * a question - may the fluid stand here? - and answers with the level of every cell it reaches.
 * A level is the amount of steps to the nearest source, so two sources that stand close to each
 * other share the water between them instead of arguing.
 * <p>
 * The walk is a plain breadth first search over the four neighbours of a cell, which is the
 * shape a fluid has when the world is seen from above, and it stops at
 * {@link Fluid#range()}: water reaches seven cells, lava only three, see {@link Fluids}.
 * <p>
 * Everything here is arithmetic on positions, so the spread of a fluid can be checked without a
 * window and without a chunk, see {@code FluidSpreadTest}. The world side - which cells are
 * allowed, when a ring is applied and what happens when a source is taken away - lives in
 * {@link FluidFlow}.
 */
public final class FluidSpread {

    /**
     * Answers whether a fluid may stand in a cell.
     * <p>
     * A fluid stands in an empty cell, in a cell that already holds the same fluid and in a cell
     * that holds something it floods, such as tall grass. A tree trunk, a wall, a machine and a
     * different fluid refuse it.
     */
    @FunctionalInterface
    public interface Cells {

        /**
         * @param x block coordinate along the first horizontal axis
         * @param y block coordinate along the second horizontal axis
         * @return {@code true} when the fluid may stand in that cell
         */
        boolean canHold(int x, int y);
    }

    private FluidSpread() {
        // Utility class: never instantiated.
    }

    /**
     * Levels of every cell a fluid reaches from its sources.
     * <p>
     * A source that is refused by {@code cells} is dropped, which keeps a source that was buried
     * by a wall from feeding water through it. A cell that two sources reach keeps the smaller
     * level, and no cell is listed twice.
     *
     * @param sources cells the fluid grows from
     * @param fluid fluid that runs, decides how far it reaches
     * @param cells what the fluid may stand in
     * @return level of every reached cell, {@code 0} for a source, never {@code null}
     */
    public static Map<BlockPos, Integer> settle(Collection<BlockPos> sources, Fluid fluid,
            Cells cells) {
        Map<BlockPos, Integer> levels = new LinkedHashMap<>();
        Deque<BlockPos> frontier = new ArrayDeque<>();

        for (BlockPos source : sources) {
            if (levels.containsKey(source) || !cells.canHold(source.x(), source.y())) {
                continue;
            }
            levels.put(source, 0);
            frontier.add(source);
        }

        int range = fluid.range();
        while (!frontier.isEmpty()) {
            BlockPos cell = frontier.poll();
            int level = levels.get(cell);
            if (level >= range) {
                continue;
            }
            for (BlockPos neighbour : neighbours(cell)) {
                if (levels.containsKey(neighbour) || !cells.canHold(neighbour.x(), neighbour.y())) {
                    continue;
                }
                levels.put(neighbour, level + 1);
                frontier.add(neighbour);
            }
        }
        return levels;
    }

    /**
     * The four cells a fluid may run into from here.
     * <p>
     * The world has no depth, so a fluid spreads in the plane of the view and never climbs or
     * falls: down and up are the two directions towards the bottom and the top of the screen.
     *
     * @param cell cell in the middle
     * @return the northern, southern, eastern and western neighbour, in that order
     */
    public static List<BlockPos> neighbours(BlockPos cell) {
        return List.of(cell.up(), cell.down(), cell.east(), cell.west());
    }
}
