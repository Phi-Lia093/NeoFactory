package com.philia093.neofactory.machine;

import com.philia093.neofactory.block.BlockFace;
import com.philia093.neofactory.cable.Voltage;
import com.philia093.neofactory.energy.LineNode;

import java.util.List;
import java.util.Objects;

/**
 * A diode: one piece of a line of the power that carries it one way.
 * <p>
 * <b>A diode is no machine that works and no cable either: it is a piece of a line.</b> It holds no tank, no
 * slot and no energy of its own - the buffer of it is a buffer that holds nothing at all, which is what keeps
 * a diode out of the two places a machine stands in: it never asks a line for power, because a buffer of no
 * capacity is never filled, and it is never a source of one, because that buffer may not be emptied. What it
 * does is what a {@link LineNode} does: a line that is walked through it is handed on the way the power of
 * the diode runs and refused the other way, and what it carries and loses rates the line it stands in, see
 * {@code Conductor} and {@code EnergyGrid#line}.
 * <p>
 * <b>The two sides a diode carries a line through are what it was built for.</b> The power of the industry
 * runs in by the <b>left flank</b> of a machine and out by its <b>right</b> one - the two sides a player
 * standing in front of it sees - and no player moves them: what the wrench does at a diode is <b>turn the
 * whole block</b>, which is how a player aims the way a line runs through it, see {@code FaceConfig#withPlugs}
 * and {@code MachineBlockEntity#operateFace}. The front of a diode carries no job like the front of every
 * machine, so the way a line runs through a diode always lies across it.
 * <p>
 * <b>A diode has no panel.</b> There is nothing in it to look at and nothing to set: what a player reads off
 * it is the colour of the terminal of its tier and which way the block is turned, see
 * {@link MachineTerminals} and {@code Machine#opensPanel}.
 */
public final class DiodeMachine extends Machine implements LineNode {

    private final Voltage tier;

    private final int amperage;

    /**
     * Creates a diode of a tier and a width.
     *
     * @param tier tier the diode was built for, which is the highest voltage it carries
     * @param width current it carries in amperes, one of {@link Diodes#WIDTHS}
     * @throws IllegalArgumentException when the game holds no such diode
     */
    public DiodeMachine(Voltage tier, int width) {
        super(new MachineScreen(Diodes.titleOf(tier, width), ProgressKind.NONE, List.of(), List.of(), 0, 0,
                        false),
                new MachineInventory(),
                new SimpleEnergyStorage(0),
                List.of());
        this.tier = Objects.requireNonNull(tier, "tier");
        this.amperage = Diodes.amperageOf(width);
        // A diode carries a line through two of its sides although its buffer holds nothing: the power runs in
        // by the left flank of the machine and out by the right one, and those two sides are what the block
        // was built for and never what a player sets, see the note on this class.
        faces().withPlugs();
        faces().setEnergyIn(MachineSides.leftOf(MachineSides.DEFAULT_FRONT));
        faces().setEnergyOut(MachineSides.rightOf(MachineSides.DEFAULT_FRONT));
    }

    /** Tier this diode was built for, which is the highest voltage it carries. */
    public Voltage tier() {
        return tier;
    }

    /** Current this diode carries, in amperes. */
    @Override
    public int amperage() {
        return amperage;
    }

    /** Highest tier this diode carries, see {@link #tier()}. */
    @Override
    public Voltage voltage() {
        return tier;
    }

    /**
     * Energy this diode takes away from what travels through it, which is nothing at all.
     * <p>
     * A diode of the industry is a superconductor with a door in it: what is handed to it arrives whole, which
     * is why a line of a diode is a line of its cables and its diodes and of no loss of its own.
     */
    @Override
    public int loss() {
        return 0;
    }

    /**
     * The side a line leaves this diode through, entered from one of its sides.
     * <p>
     * The plugs of a diode are the two flanks of the block, so a line that is walked in by the side the power
     * leaves the diode by - its right flank - is handed on through the side it enters by, and a line that
     * arrives at that side is refused, see {@link LineNode#through(BlockFace)}.
     */
    @Override
    public BlockFace through(BlockFace from) {
        return from == faces().energyOut() ? faces().energyIn() : null;
    }

    /** Tier this diode was built for, which is the colour of its terminal as well. */
    @Override
    public Voltage lineTier() {
        return tier;
    }

    /** Casing this diode is built of, the one of its tier, see {@link MachineCasing}. */
    @Override
    public String casing() {
        return MachineCasing.pictureOf(tier);
    }

    /** {@code false}: a diode has nothing a panel could show, see the note on this class. */
    @Override
    public boolean opensPanel() {
        return false;
    }

    /**
     * A diode does no work of its own.
     * <p>
     * Everything that happens at a diode happens while a line is walked through it: the block that carries a
     * line is read by the walk of the machine that asks for the power, so a diode with no machine around it
     * does nothing at all and waits, see {@link LineNode}.
     *
     * @param delta time since the last frame in seconds, which a diode does not read
     */
    @Override
    protected void update(float delta) {
        // Nothing of a diode happens by itself, see the note above.
    }

    @Override
    public String toString() {
        return "DiodeMachine(" + name() + ", " + amperage + " A, " + tier + ")";
    }
}
