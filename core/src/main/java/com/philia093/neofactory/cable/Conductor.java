package com.philia093.neofactory.cable;

import com.philia093.neofactory.energy.EnergyNet;

import java.util.List;

/**
 * One piece a line of the power is built of and carries its energy through.
 * <p>
 * <b>A line is built of conductors and its numbers are the worst of them.</b> The cable of a material is the
 * conductor the industry is built on: what a line of it carries is its {@link #voltage()} and its
 * {@link #amperage()}, and every block of the run takes its {@link #loss()} away from what travels through
 * it. A diode is the second kind of conductor of the game - one piece of a line that takes the tier and the
 * current of the cable at it and carries them in one direction only, see
 * {@code com.philia093.neofactory.energy.LineNode} and {@code EnergyGrid#line}.
 * <p>
 * <b>The numbers are read the same way whatever the kind is.</b> {@link EnergyNet#of(List)} takes the
 * <b>lowest</b> voltage and the <b>lowest</b> amperage of the run and the <b>highest</b> loss of it, so a
 * diode of one ampere in a run of a wide cable is a line of one ampere, exactly the way a narrow cable in a
 * wide run is. That is what makes a diode a piece of a line and not a machine at the end of one.
 */
public interface Conductor {

    /**
     * Highest tier this piece of a line may carry.
     *
     * @return the tier, see {@link CableMaterial#voltage()}
     */
    Voltage voltage();

    /**
     * Current this piece carries, in amperes.
     *
     * @return the amperage of the piece
     */
    int amperage();

    /**
     * Energy one block of this piece takes away from what travels through it.
     *
     * @return the loss in units of the game, {@code 0} for a piece that loses nothing
     */
    int loss();
}
