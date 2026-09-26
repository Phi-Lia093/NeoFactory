package com.philia093.neofactory.pipe;

/**
 * What one side of a pipe does with the fluid: whether it takes it, gives it, or both.
 * <p>
 * A side of a pipe is joined or it is not, see {@link Pipes}, and that is the shape of the tube. Whether
 * the fluid may run through it is a second question and it is answered here, one side at a time: a side
 * that only takes is an inlet, a side that only gives is an outlet, and a side that does neither way is
 * the ordinary joined side of a line. It is the valve a player sets with the wrench while holding the
 * modifier key: <b>every click of the grid turns the side one step further,</b> both ways, in only, out
 * only, and then both ways again, see {@link #next()}.
 * <p>
 * <b>Why the loop has no end.</b> A pipe may be a mouth of a machine, a cooling loop that has to run one
 * way round, or the crossing of two lines that must not mix; a setting that could be reached but not
 * undone would turn a mistake into a block a player has to break. The three settings are therefore a
 * cycle and not a list.
 * <p>
 * <b>The valve is stored next to the pipe and not in its state.</b> The state of a pipe is its connection
 * mask and the six bits of it are taken by the six directions, so there is no room left for a second
 * property - the setting lives in the block entity of the pipe, see
 * {@code PipeBlockEntity#flowOf(com.philia093.neofactory.block.BlockFace)}, and the model of the pipe
 * looks the same whichever way it is set, which is why the grid of faces draws a small arrow on the sides
 * that are one way, see {@code FaceOverlay}.
 */
public enum PipeFlow {

    /** The ordinary side of a line: it takes fluid from what stands next to it and gives fluid to it. */
    BOTH("both ways"),

    /** A mouth: fluid only enters the pipe through this side, and never leaves it there. */
    IN("in only"),

    /** An outlet: fluid only leaves the pipe through this side, and never enters it there. */
    OUT("out only");

    private final String displayName;

    PipeFlow(String displayName) {
        this.displayName = displayName;
    }

    /** Name of this flow as a word, used where a tooltip names what a side is set to. */
    public String displayName() {
        return displayName;
    }

    /**
     * The flow one click of the wrench turns this one into.
     * <p>
     * Both ways, in only, out only, and both ways again, see the class comment.
     *
     * @return the next setting of the cycle
     */
    public PipeFlow next() {
        switch (this) {
            case BOTH:
                return IN;
            case IN:
                return OUT;
            default:
                return BOTH;
        }
    }

    /** {@code true} when fluid may enter a pipe through a side of this kind. */
    public boolean takes() {
        return this != OUT;
    }

    /** {@code true} when fluid may leave a pipe through a side of this kind. */
    public boolean gives() {
        return this != IN;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
