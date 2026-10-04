package com.philia093.neofactory.machine;

import java.util.List;
import java.util.Objects;

/**
 * The screen of a machine, named when the machine is registered.
 * <p>
 * A machine describes how it wants to be shown and the layout does the arithmetic, so a new
 * machine never mentions a pixel:
 * <ul>
 *     <li>{@link #title()} is written in the upper left corner of the panel, the name the
 *         player reads;</li>
 *     <li>{@link #style()} picks the panel the machine is drawn in - the grey one of the age of electricity or
 *         the bronze one of the age of steam, see {@link MachineStyle};</li>
 *     <li>{@link #progress()} picks the pair of arrows the bar is drawn from;</li>
 *     <li>{@link #inputs()} and {@link #outputs()} name the kind of every slot a machine
 *         works with, one entry per slot. Their amount decides how the slots are arranged,
 *         see {@link #columns(int)} and {@link #rows(int)}: one slot stands alone, two
 *         stand beside each other, four form a square and six fill two rows of three;</li>
 *     <li>{@link #fluidInputs()} and {@link #fluidOutputs()} are the tanks at the foot of
 *         the panel, up to two of each, drawn in the lower left and next to it;</li>
 *     <li>{@link #configureSlot()} reserves the lower right corner for the slot that will
 *         later configure a machine which serves one input from many;</li>
 *     <li>{@link #gridSlots()} says that the panel of a machine is nothing but a square grid of plain slots -
 *         one, four, nine or sixteen of them, the shape a box of cells is drawn in. <b>Such a panel carries
 *         nothing else</b>: no bar, no tank and no cell of energy, because the grid takes the place they
 *         would have stood in, see {@link MachineMenu} and {@code MachineGui}.</li>
 * </ul>
 *
 * @param title name shown in the upper left corner of the screen
 * @param style panel the machine is drawn in, see {@link MachineStyle}
 * @param progress pair of arrows the progress bar is drawn from
 * @param inputs kind of every input slot, in the order they are drawn
 * @param outputs kind of every output slot, in the order they are drawn
 * @param fluidInputs amount of tanks a recipe drains, {@code 0} to {@code 2}
 * @param fluidOutputs amount of tanks a machine fills, {@code 0} to {@code 2}
 * @param configureSlot {@code true} to reserve the lower right corner for a configure slot
 * @param gridSlots amount of plain slots a panel that is a grid holds, {@code 0} for a panel of the usual
 *        kind, see {@link #GRID_SHAPES}
 */
public record MachineScreen(String title, MachineStyle style, ProgressKind progress,
        List<SlotKind> inputs, List<SlotKind> outputs, int fluidInputs, int fluidOutputs,
        boolean configureSlot, int gridSlots) {

    /**
     * Describes a panel of the usual kind: slots on the left and on the right of a bar, tanks at the foot of
     * it and no grid of plain slots.
     * <p>
     * A machine that names no grid has a panel like every other machine of the game, so this is the shape
     * almost every mark of the game is written with, see {@link MachineScreen#MachineScreen(String,
     * MachineStyle, ProgressKind, List, List, int, int, boolean, int)}.
     *
     * @param title name shown in the upper left corner of the screen
     * @param style panel the machine is drawn in, see {@link MachineStyle}
     * @param progress pair of arrows the progress bar is drawn from
     * @param inputs kind of every input slot, in the order they are drawn
     * @param outputs kind of every output slot, in the order they are drawn
     * @param fluidInputs amount of tanks a recipe drains, {@code 0} to {@code 2}
     * @param fluidOutputs amount of tanks a machine fills, {@code 0} to {@code 2}
     * @param configureSlot {@code true} to reserve the lower right corner for a configure slot
     */
    public MachineScreen(String title, MachineStyle style, ProgressKind progress, List<SlotKind> inputs,
            List<SlotKind> outputs, int fluidInputs, int fluidOutputs, boolean configureSlot) {
        this(title, style, progress, inputs, outputs, fluidInputs, fluidOutputs, configureSlot, 0);
    }

    /**
     * Describes a machine screen of the age of electricity.
     * <p>
     * A machine that names no style of its own is drawn with the grey panel, the one every machine of the
     * game was drawn with before the age of steam brought its own, see {@link MachineStyle#NORMAL}.
     *
     * @param title name shown in the upper left corner of the screen
     * @param progress pair of arrows the progress bar is drawn from
     * @param inputs kind of every input slot, in the order they are drawn
     * @param outputs kind of every output slot, in the order they are drawn
     * @param fluidInputs amount of tanks a recipe drains, {@code 0} to {@code 2}
     * @param fluidOutputs amount of tanks a machine fills, {@code 0} to {@code 2}
     * @param configureSlot {@code true} to reserve the lower right corner for a configure slot
     */
    public MachineScreen(String title, ProgressKind progress, List<SlotKind> inputs,
            List<SlotKind> outputs, int fluidInputs, int fluidOutputs, boolean configureSlot) {
        this(title, MachineStyle.NORMAL, progress, inputs, outputs, fluidInputs, fluidOutputs,
                configureSlot);
    }

    /**
     * Describes the panel of a machine that is nothing but a grid of plain slots.
     * <p>
     * A box of cells is not a machine a player works a recipe in: what it shows is the cells that stand in
     * it and nothing else, so its panel carries no bar and no tank, see {@link #gridSlots()}.
     *
     * @param title name shown in the upper left corner of the screen
     * @param gridSlots amount of plain slots the grid holds, {@code 1}, {@code 4}, {@code 9} or {@code 16}
     */
    public MachineScreen(String title, int gridSlots) {
        this(title, MachineStyle.NORMAL, ProgressKind.NONE, List.of(), List.of(), 0, 0, false, gridSlots);
    }

    /**
     * Amounts of slots a block of slots may hold, one of the shapes of the game.
     * <p>
     * A machine may have no slot of a side at all: a boiler holds no product, only a tank, and a tank of
     * fluid holds no item. Such a side is left out of the panel, see {@link #columns(int)}.
     */
    private static final List<Integer> SHAPES = List.of(1, 2, 4, 6);

    /**
     * Amounts of plain slots a panel that is a grid may hold, one of the shapes of a box of cells.
     * <p>
     * A grid is a square, so the four shapes are the four squares the panel of a machine has room for: one
     * cell in the middle of it, four in a block, nine in a block and sixteen, which fills the upper half of
     * the panel from its left edge to its right one, see {@link MachineMenu#gridLeft(int)}.
     */
    public static final List<Integer> GRID_SHAPES = List.of(1, 4, 9, 16);

    /** Largest amount of tanks at the foot of a panel. */
    private static final int MAX_TANKS = 2;

    /** Checks the description, so a broken machine fails while it is registered. */
    public MachineScreen {
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(style, "style");
        Objects.requireNonNull(progress, "progress");
        Objects.requireNonNull(inputs, "inputs");
        Objects.requireNonNull(outputs, "outputs");
        if (title.isBlank()) {
            throw new IllegalArgumentException("The title of a machine must not be blank");
        }
        if (!hasAShape(inputs.size())) {
            throw new IllegalArgumentException("A machine takes " + inputs.size()
                    + " input slots, but only 0, 1, 2, 4 or 6 are arranged by the layout");
        }
        if (!hasAShape(outputs.size())) {
            throw new IllegalArgumentException("A machine makes " + outputs.size()
                    + " output slots, but only 0, 1, 2, 4 or 6 are arranged by the layout");
        }
        for (SlotKind kind : inputs) {
            if (kind.isFluid()) {
                throw new IllegalArgumentException("A tank of fluid is no input slot: " + kind);
            }
        }
        for (SlotKind kind : outputs) {
            if (kind.isFluid()) {
                throw new IllegalArgumentException("A tank of fluid is no output slot: " + kind);
            }
        }
        if (fluidInputs < 0 || fluidInputs > MAX_TANKS) {
            throw new IllegalArgumentException("A machine holds " + fluidInputs
                    + " input tanks, but only " + MAX_TANKS + " fit into the panel");
        }
        if (fluidOutputs < 0 || fluidOutputs > MAX_TANKS) {
            throw new IllegalArgumentException("A machine holds " + fluidOutputs
                    + " output tanks, but only " + MAX_TANKS + " fit into the panel");
        }
        if (!hasAGridShape(gridSlots)) {
            throw new IllegalArgumentException("A panel that is a grid holds " + gridSlots
                    + " slots, but a grid is one, four, nine or sixteen of them, see GRID_SHAPES");
        }
        if (gridSlots > 0 && (progress.hasBar() || !inputs.isEmpty() || !outputs.isEmpty()
                || fluidInputs > 0 || fluidOutputs > 0 || configureSlot)) {
            throw new IllegalArgumentException("The panel of " + title
                    + " is a grid of slots, so it carries nothing else: no bar, no tank and no other slot");
        }
    }

    /** {@code true} when the panel of a machine is a grid of plain slots, see {@link #gridSlots()}. */
    public boolean isGrid() {
        return gridSlots > 0;
    }

    /** Columns of the grid of a panel, which is its rows as well because a grid is a square. */
    public int gridColumns() {
        return gridColumns(gridSlots);
    }

    /** Rows of the grid of a panel, which is its columns as well because a grid is a square. */
    public int gridRows() {
        return gridRows(gridSlots);
    }

    /**
     * {@code true} when that many slots form a grid a panel may carry.
     *
     * @param slots amount of plain slots
     * @return {@code true} when the shape is a grid of the game
     */
    private static boolean hasAGridShape(int slots) {
        return slots == 0 || GRID_SHAPES.contains(slots);
    }

    /**
     * Columns a grid of that many slots takes, which is its rows as well.
     *
     * @param slots amount of plain slots, {@code 1}, {@code 4}, {@code 9} or {@code 16}
     * @return the amount of columns
     * @throws IllegalArgumentException when that many slots form no grid
     */
    public static int gridColumns(int slots) {
        return switch (slots) {
            case 1 -> 1;
            case 4 -> 2;
            case 9 -> 3;
            case 16 -> 4;
            default -> throw new IllegalArgumentException("No grid of " + slots + " slots");
        };
    }

    /**
     * Rows a grid of that many slots takes, which is its columns as well.
     *
     * @param slots amount of plain slots, {@code 1}, {@code 4}, {@code 9} or {@code 16}
     * @return the amount of rows
     * @throws IllegalArgumentException when that many slots form no grid
     */
    public static int gridRows(int slots) {
        return gridColumns(slots);
    }

    /** Amount of input slots of this machine. */
    public int inputSlots() {
        return inputs.size();
    }

    /** Amount of output slots of this machine. */
    public int outputSlots() {
        return outputs.size();
    }

    /**
     * {@code true} when a block of that many slots has a shape the layout arranges.
     *
     * @param slots amount of slots of a side of a machine
     * @return {@code true} when the side may be laid out
     */
    private static boolean hasAShape(int slots) {
        return slots == 0 || SHAPES.contains(slots);
    }

    /**
     * Columns a block of slots of this size takes.
     *
     * @param slots amount of slots, {@code 0}, {@code 1}, {@code 2}, {@code 4} or {@code 6}
     * @return the amount of columns, {@code 0} for a machine that holds no slot of that side
     */
    /** Amount of columns a block of slots of this size takes. */
    public static int columns(int slots) {
        return switch (slots) {
            case 0 -> 0;
            case 1 -> 1;
            case 2 -> 2;
            case 4 -> 2;
            case 6 -> 3;
            default -> throw new IllegalArgumentException("No shape for " + slots + " slots");
        };
    }

    /**
     * Rows a block of slots of this size takes.
     *
     * @param slots amount of slots, {@code 0}, {@code 1}, {@code 2}, {@code 4} or {@code 6}
     * @return the amount of rows, {@code 0} for a machine that holds no slot of that side
     */
    public static int rows(int slots) {
        return switch (slots) {
            case 0 -> 0;
            case 1, 2 -> 1;
            case 4, 6 -> 2;
            default -> throw new IllegalArgumentException("No shape for " + slots + " slots");
        };
    }

    @Override
    public String toString() {
        if (isGrid()) {
            return "MachineScreen(" + title + ", " + style + ", a grid of " + gridSlots + ")";
        }
        return "MachineScreen(" + title + ", " + style + ", " + progress + ", " + inputSlots() + " in, "
                + outputSlots() + " out, " + fluidInputs + "+" + fluidOutputs + " tanks)";
    }
}
