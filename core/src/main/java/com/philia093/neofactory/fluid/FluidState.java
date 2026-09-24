package com.philia093.neofactory.fluid;

/**
 * How a cell carries a fluid: how far it is from the source that feeds it, whether it is that
 * source itself and whether it fell into the cell.
 * <p>
 * The state travels in the block state, the 32 bit number a chunk keeps next to every block id,
 * see {@link com.philia093.neofactory.world.Chunk#meta(int, int, int)}. Three fields are stored:
 * the level - {@code 0} for a cell the fluid stands in at the strength of its source and one step
 * for every cell it ran sideways from it - the flag that marks the source and the flag that marks a
 * cell the fluid fell into. The rest of the number stays free.
 * <p>
 * <b>Why falling is its own flag:</b> a fluid that falls keeps the strength it had above, so a
 * waterfall arrives at the bottom with the level of its source, which is zero. Without the flag such
 * a cell could not be told from the source itself: it would feed the fluid around it forever and
 * never run dry, and a player who took the source away would leave a column of water hanging in the
 * air.
 * <p>
 * A source never runs dry: nothing in the game takes a fluid away except a bucket or a cell, and
 * those take the source itself, see {@code FluidInteraction}. A cell that was reached by the fluid
 * carries a level of at least one, or fell into place, and is removed again as soon as no source
 * feeds it, see {@code FluidFlow}.
 *
 * @param level distance to the source that feeds this cell, {@code 0} for a source and for a cell
 *              the fluid fell into
 * @param source {@code true} when this cell is a source
 * @param falling {@code true} when the fluid fell into this cell from above
 */
public record FluidState(int level, boolean source, boolean falling) {

    /** Bits the level uses inside the block state. */
    private static final int LEVEL_MASK = 0xFF;

    /** Bit that marks a source. */
    private static final int SOURCE_BIT = 1 << 8;

    /** Bit that marks a cell the fluid fell into. */
    private static final int FALLING_BIT = 1 << 9;

    /** The state of a source, the cell a bucket pours its fluid into. */
    public static final FluidState SOURCE = new FluidState(0, true, false);

    /** Checks the fields, so a broken state fails where it is built. */
    public FluidState {
        if (level < 0 || level > LEVEL_MASK) {
            throw new IllegalArgumentException("A fluid level lies between 0 and " + LEVEL_MASK
                    + ": " + level);
        }
        if (source && level != 0) {
            throw new IllegalArgumentException("A source is never away from itself: " + level);
        }
        if (source && falling) {
            throw new IllegalArgumentException("A source does not fall into its own cell");
        }
    }

    /**
     * The state of a cell the fluid reached sideways.
     *
     * @param level distance to the source, at least one
     * @return the state
     */
    public static FluidState flowing(int level) {
        if (level < 1) {
            throw new IllegalArgumentException("A flowing cell is at least one step from its"
                    + " source: " + level);
        }
        return new FluidState(level, false, false);
    }

    /**
     * The state of a cell the fluid fell into.
     * <p>
     * The level is the one the fluid had in the cell above: falling costs nothing, so a waterfall is
     * as strong at its foot as at its lip.
     *
     * @param level level the fluid had above, {@code 0} for a fall from a source
     * @return the state
     */
    public static FluidState fallen(int level) {
        return new FluidState(level, false, true);
    }

    /** {@code true} when this cell is the source that feeds the fluid around it. */
    public boolean isSource() {
        return source;
    }

    /** {@code true} when the fluid fell into this cell instead of running into it. */
    public boolean isFalling() {
        return falling;
    }

    /**
     * Packs this state into the number a chunk keeps next to a block id.
     *
     * @return the packed state
     */
    public int pack() {
        return (level & LEVEL_MASK) | (source ? SOURCE_BIT : 0) | (falling ? FALLING_BIT : 0);
    }

    /**
     * Reads a state back out of the number a chunk keeps.
     *
     * @param state packed state, {@code 0} for a cell no fluid ever touched
     * @return the state
     */
    public static FluidState unpack(int state) {
        return new FluidState(state & LEVEL_MASK, (state & SOURCE_BIT) != 0,
                (state & FALLING_BIT) != 0);
    }

    @Override
    public String toString() {
        if (source) {
            return "FluidState(source)";
        }
        return falling ? "FluidState(falling, level " + level + ")" : "FluidState(level " + level + ")";
    }
}
