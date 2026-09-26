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
 *         stand above each other, four form a square and six fill two rows of three;</li>
 *     <li>{@link #fluidInputs()} and {@link #fluidOutputs()} are the tanks at the foot of
 *         the panel, up to two of each, drawn in the lower left and next to it;</li>
 *     <li>{@link #configureSlot()} reserves the lower right corner for the slot that will
 *         later configure a machine which serves one input from many.</li>
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
 */
public record MachineScreen(String title, MachineStyle style, ProgressKind progress,
        List<SlotKind> inputs, List<SlotKind> outputs, int fluidInputs, int fluidOutputs,
        boolean configureSlot) {

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
     * Amounts of slots a block of slots may hold, one of the shapes of the game.
     * <p>
     * A machine may have no slot of a side at all: a boiler holds no product, only a tank, and a tank of
     * fluid holds no item. Such a side is left out of the panel, see {@link #columns(int)}.
     */
    private static final List<Integer> SHAPES = List.of(1, 2, 4, 6);

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
    public static int columns(int slots) {
        return switch (slots) {
            case 0 -> 0;
            case 1, 2 -> 1;
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
            case 1 -> 1;
            case 2, 4, 6 -> 2;
            default -> throw new IllegalArgumentException("No shape for " + slots + " slots");
        };
    }

    @Override
    public String toString() {
        return "MachineScreen(" + title + ", " + style + ", " + progress + ", " + inputSlots() + " in, "
                + outputSlots() + " out, " + fluidInputs + "+" + fluidOutputs + " tanks)";
    }
}
