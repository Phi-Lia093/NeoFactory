package com.philia093.neofactory.fluid;

/**
 * How a cell carries a fluid: how far it is from the source that feeds it and whether it is
 * that source itself.
 * <p>
 * The state travels in the block state, the 32 bit number a chunk keeps next to every block id,
 * see {@link com.philia093.neofactory.world.Chunk#meta(int, int, int)}. Two fields are stored:
 * the level - {@code 0} for a source and one step for every cell the fluid ran away from it -
 * and the flag that marks the source. The rest of the number stays free, so a fluid that learns
 * to fall or to be counted does not need a new format.
 * <p>
 * A source never runs dry: nothing in the game takes a fluid away except a bucket or a cell, and
 * those take the source itself, see {@code FluidInteraction}. A cell that was reached by the
 * fluid carries a level of at least one and is removed again as soon as no source feeds it, see
 * {@code FluidFlow}.
 *
 * @param level distance to the source that feeds this cell, {@code 0} for a source
 * @param source {@code true} when this cell is a source
 */
public record FluidState(int level, boolean source) {

    /** Bits the level uses inside the block state. */
    private static final int LEVEL_MASK = 0xFF;

    /** Bit that marks a source. */
    private static final int SOURCE_BIT = 1 << 8;

    /** The state of a source, the cell a bucket pours its fluid into. */
    public static final FluidState SOURCE = new FluidState(0, true);

    /** Checks the fields, so a broken state fails where it is built. */
    public FluidState {
        if (level < 0 || level > LEVEL_MASK) {
            throw new IllegalArgumentException("A fluid level lies between 0 and " + LEVEL_MASK
                    + ": " + level);
        }
        if (source && level != 0) {
            throw new IllegalArgumentException("A source is never away from itself: " + level);
        }
    }

    /**
     * The state of a cell the fluid reached.
     *
     * @param level distance to the source, at least one
     * @return the state
     */
    public static FluidState flowing(int level) {
        if (level < 1) {
            throw new IllegalArgumentException("A flowing cell is at least one step from its"
                    + " source: " + level);
        }
        return new FluidState(level, false);
    }

    /** {@code true} when this cell is the source that feeds the fluid around it. */
    public boolean isSource() {
        return source;
    }

    /**
     * Packs this state into the number a chunk keeps next to a block id.
     *
     * @return the packed state
     */
    public int pack() {
        return (level & LEVEL_MASK) | (source ? SOURCE_BIT : 0);
    }

    /**
     * Reads a state back out of the number a chunk keeps.
     *
     * @param state packed state, {@code 0} for a cell no fluid ever touched
     * @return the state
     */
    public static FluidState unpack(int state) {
        return new FluidState(state & LEVEL_MASK, (state & SOURCE_BIT) != 0);
    }

    @Override
    public String toString() {
        return source ? "FluidState(source)" : "FluidState(level " + level + ")";
    }
}
