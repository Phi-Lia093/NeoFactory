package com.philia093.neofactory.energy;

import com.philia093.neofactory.block.BlockFace;
import com.philia093.neofactory.cable.Conductor;

/**
 * A block a line of the power runs through, the way it runs through a cable.
 * <p>
 * <b>A cable is one piece of a line and the diode is another.</b> What a line of cables is walked by is
 * {@code EnergyGrid#line}: the walk follows the sides a cable is joined to and reads the machines that hang
 * on it. A block that carries a line through itself - the diode of the industry - is no machine at the end of
 * a line and no cable either: it is a piece of the line, and it answers two questions about itself, the
 * numbers a {@link Conductor} of the line has and the side a line leaves it through.
 * <p>
 * <b>Which way a line runs is the whole of what a diode decides.</b> A line is walked by the machine that
 * asks for the power and it walks <b>towards</b> whoever gives it, so a diode hands the walk on exactly when
 * it is entered from the side its power flows out of, and refuses it when it is entered the other way - which
 * is what makes the diode one-way and the line it stands in one-way with it, see
 * {@code EnergyGrid.Cells#node}.
 */
public interface LineNode extends Conductor {

    /**
     * The side a line leaves this block through, entered from one of its sides.
     * <p>
     * A diode names the side its power leaves it by - the output of the diode - and the side its power enters
     * it by: a line that is entered from the output is handed on through the input, and a line that arrives
     * at the input is refused, because the power of this block never runs that way.
     *
     * @param from side of this block the line entered it from
     * @return the side the line goes on through, {@code null} when no line runs through this block that way
     */
    BlockFace through(BlockFace from);
}
