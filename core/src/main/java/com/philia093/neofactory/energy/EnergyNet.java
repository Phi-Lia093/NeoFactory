package com.philia093.neofactory.energy;

import com.philia093.neofactory.cable.CableKind;
import com.philia093.neofactory.cable.CableMaterial;
import com.philia093.neofactory.cable.CableSize;
import com.philia093.neofactory.cable.Cables;
import com.philia093.neofactory.cable.Voltage;
import com.philia093.neofactory.machine.EnergyStorage;

import java.util.List;
import java.util.Objects;

/**
 * One line of the power: the cables a net is built of, and what they carry a tick.
 * <p>
 * A line is one kind of cable end to end and what it carries is what its cables allow, see
 * {@code CableMaterial}. A line of two cables of different materials is a line of the <b>worse</b> of the
 * two, which is the rule the industry is built on: a copper cable between two superconductors carries no
 * more than a copper cable.
 * <ul>
 *     <li>{@link #voltage()} the tier of the line, the <b>lowest</b> voltage of its cables. A machine that
 *         asks for more than this never runs, whatever the amount of energy it is offered;</li>
 *     <li>{@link #amperage()} the current of the line, the <b>lowest</b> amperage of its cables;</li>
 *     <li>{@link #loss()} what one block of the line takes away, the <b>highest</b> loss of its cables - a
 *         run of a cheap cable through a good one loses what the cheap one loses, because one block of it
 *         stands in the way of the whole run;</li>
 *     <li>{@link #blocks()} how many blocks of cable the run is long.</li>
 * </ul>
 * <b>What a line carries a tick is its voltage times its current</b>, see {@link #capacity()}. A line of
 * medium voltage of one ampere carries a hundred and twenty eight units a tick, and the same line of the
 * four fold width carries four times that much, see {@code CableSize}. A line is not slower when it is long,
 * it simply costs more to run, see {@link #carry}.
 * <p>
 * <b>The loss is paid by the line and not by the machine.</b> {@link #carry} takes what a source offers and
 * hands the sink what is left after every block of the run took its share. A line of a superconductor loses
 * nothing at all, whatever it is wrapped in.
 * <p>
 * <b>What a line burns is weighed on two scales.</b> The tier of a line is the one the machines that give it
 * their power are built for, see {@link #overvolts(Voltage, EnergyStorage)}: a machine at the end of a line
 * that was built for a worse one is not fed at all and is destroyed by it, while a piece of the run that
 * cannot take that much - a cable of an earlier age on a line of a later one - melts where the power enters
 * the line. The two are weighed apart, so a run of a cheap cable outlives the machine it could never feed,
 * and a machine of the right tier outlives the run that was too weak for it.
 * <p>
 * <b>How a line is found.</b> The net of the world collects the cables that are joined to one another - the
 * mask of a cable says which sides it joins, see {@link Cables} - and hands the ones between two machines to
 * this class. Building that net out of a world is the step after this one; what stands here is the
 * arithmetic every line of the game is measured with, and it needs no world to be checked.
 */
public final class EnergyNet {

    private final Voltage voltage;
    private final int amperage;
    private final int loss;
    private final int blocks;

    private EnergyNet(Voltage voltage, int amperage, int loss, int blocks) {
        this.voltage = Objects.requireNonNull(voltage, "voltage");
        if (amperage < 1) {
            throw new IllegalArgumentException("A line carries no current at all, an amperage has to be "
                    + "at least one");
        }
        if (loss < 0 || blocks < 0) {
            throw new IllegalArgumentException("A line loses or runs less than nothing");
        }
        this.amperage = amperage;
        this.loss = loss;
        this.blocks = blocks;
    }

    /**
     * The line the given cables come to.
     * <p>
     * What one cable of a material and a width carries and loses is asked of the table of the cables, see
     * {@link Cables.Cable}, and the line takes the worst of every one of them.
     *
     * @param cables cables the run is built of, at least one
     * @return the line
     * @throws IllegalArgumentException when no cable was given
     */
    public static EnergyNet of(List<Cables.Cable> cables) {
        Objects.requireNonNull(cables, "cables");
        if (cables.isEmpty()) {
            throw new IllegalArgumentException("A line is built of at least one cable");
        }
        Voltage voltage = null;
        int amperage = Integer.MAX_VALUE;
        int loss = 0;
        for (Cables.Cable cable : cables) {
            Objects.requireNonNull(cable, "cable");
            if (voltage == null || voltage.euPerTick() > cable.voltage().euPerTick()) {
                voltage = cable.voltage();
            }
            amperage = Math.min(amperage, cable.amperage());
            loss = Math.max(loss, cable.loss());
        }
        return new EnergyNet(voltage, amperage, loss, cables.size());
    }

    /**
     * The line one kind of cable comes to over a run of a length.
     *
     * @param material material the line is made of
     * @param size width of the line
     * @param kind what the line is wrapped in
     * @param blocks amount of cable blocks the run is long, at least one
     * @return the line
     * @throws IllegalArgumentException when the game holds no such cable
     */
    public static EnergyNet of(CableMaterial material, CableSize size, CableKind kind, int blocks) {
        Cables.Cable cable = Cables.of(material, size, kind);
        if (cable == null) {
            throw new IllegalArgumentException("The game holds no cable of " + material + ", " + size
                    + " and " + kind);
        }
        return new EnergyNet(cable.voltage(), cable.amperage(), cable.loss(), blocks);
    }

    /** Tier of the line, the lowest voltage of its cables. */
    public Voltage voltage() {
        return voltage;
    }

    /** Current of the line, the lowest amperage of its cables. */
    public int amperage() {
        return amperage;
    }

    /** Energy one block of the line takes away from what travels through it. */
    public int loss() {
        return loss;
    }

    /** What the whole run takes away, the loss of one block times the length of the line. */
    public int totalLoss() {
        return loss * blocks;
    }

    /** Amount of cable blocks the run is long. */
    public int blocks() {
        return blocks;
    }

    /** Energy a line of this tier and current carries a tick, the voltage times the amperage. */
    public int capacity() {
        return voltage.euPerTick() * amperage;
    }

    /** {@code true} when the line loses nothing on the way, a line of superconductors. */
    public boolean isLossless() {
        return totalLoss() == 0;
    }

    /**
     * {@code true} when a machine that asks for a tier runs on this line.
     *
     * @param machine tier the machine asks for
     * @return {@code true} when the line carries at least that voltage
     */
    public boolean feeds(Voltage machine) {
        return voltage.isAtLeast(machine);
    }

    /**
     * Moves energy from one storage to another through this line.
     * <p>
     * What is carried is asked of the source and offered to the sink, and the line keeps what it loses on
     * the way: the source pays what the sink receives <b>plus</b> {@link #totalLoss()}, so a machine at the
     * end of a long line is fed only while its source can spare the loss as well. Only what the sink takes
     * is paid for, and nothing moves at all when the source is empty, when the sink is full, or when either
     * of them refuses to take part - a line never takes energy out of a source to lose it on the way.
     *
     * @param source storage the energy comes from
     * @param sink storage the energy goes to
     * @param wanted amount the sink is meant to receive, at least one
     * @return the amount the sink received, {@code 0} when nothing moved
     */
    public int carry(EnergyStorage source, EnergyStorage sink, int wanted) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(sink, "sink");
        if (wanted <= 0 || !source.canExtract() || !sink.canReceive()) {
            return 0;
        }
        int arrival = Math.min(wanted, capacity());
        int drawn = source.extract(arrival + totalLoss(), true);
        int arrives = drawn - totalLoss();
        if (arrives <= 0) {
            return 0;
        }
        int accepted = sink.receive(arrives, true);
        if (accepted <= 0) {
            return 0;
        }
        int paid = source.extract(accepted + totalLoss(), false);
        return sink.receive(paid - totalLoss(), false);
    }

    /**
     * {@code true} when a line of a tier destroys the machine that reaches for it.
     * <p>
     * A line of a higher tier than the one a machine was built for is no line that machine survives: the
     * energy is not handed over at all and the machine is taken out of the world, see
     * {@link EnergyAcceptor#accepts(Voltage)}. What the tier of a line is and what becomes of the cables of
     * it is the business of the line and not of this question, see {@link EnergyGrid.Line#pull}.
     * <p>
     * A buffer that names no tier is no machine of the industry - the plain buffer a test holds - and a line
     * of any tier leaves it alone.
     *
     * @param line tier the machine is reached by
     * @param sink buffer that wants the energy
     * @return {@code true} when that buffer was built for a worse line than this
     */
    public static boolean overvolts(Voltage line, EnergyStorage sink) {
        Objects.requireNonNull(line, "line");
        Objects.requireNonNull(sink, "sink");
        return sink instanceof EnergyAcceptor taking && !taking.accepts(line);
    }

    /**
     * {@code true} when the energy of one buffer destroys the machine that asks it for power over a gap.
     * <p>
     * <b>Two machines that stand next to each other need no cable, and what feeds them is then a line of the
     * tier of the machine that gives.</b> A machine of the game is built for one tier of the power, and a line
     * of a higher tier does not feed it - it destroys it, see {@link EnergyAcceptor} and
     * {@link #overvolts(Voltage, EnergyStorage)}. Where there is no cable between the two, the tier of that
     * line is the one the storage that gives names: a furnace of the low voltage that reaches for what a box
     * of the high voltage holds is destroyed by it, exactly as it would be destroyed by a line of the high
     * voltage.
     * <p>
     * <b>A storage that names no tier, or that has nothing to give through, is no line at all.</b> What a
     * line burns is a machine that was built for a worse line, so a buffer that is no machine of the industry
     * - the plain buffer a test holds - destroys nothing, and neither does a box that holds no cell in it and
     * therefore gives nothing away.
     *
     * @param source buffer the energy would come from, the machine that gives
     * @param sink buffer that wants the energy, the machine that asks
     * @return {@code true} when asking that machine for power destroys the one that asks
     */
    public static boolean overvolts(EnergyStorage source, EnergyStorage sink) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(sink, "sink");
        if (!source.canExtract() || !(source instanceof EnergyAcceptor giving)) {
            return false;
        }
        return overvolts(giving.accepted(), sink);
    }

    /**
     * Moves energy between two storages that stand next to each other.
     * <p>
     * The case of two machines with no cable between them: the energy crosses the gap with no loss and with
     * nothing but the two storages limiting the amount, which is what the first steps of the industry are
     * built on.
     *
     * @param source storage the energy comes from
     * @param sink storage the energy goes to
     * @param wanted amount the sink is meant to receive
     * @return the amount the sink received
     */
    public static int hand(EnergyStorage source, EnergyStorage sink, int wanted) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(sink, "sink");
        if (wanted <= 0 || !source.canExtract() || !sink.canReceive()) {
            return 0;
        }
        int offered = source.extract(wanted, true);
        if (offered <= 0) {
            return 0;
        }
        int accepted = sink.receive(offered, true);
        if (accepted <= 0) {
            return 0;
        }
        return sink.receive(source.extract(accepted, false), false);
    }

    @Override
    public String toString() {
        return "EnergyNet(" + voltage.fileName() + ", " + amperage + "A, loss " + loss + " over " + blocks
                + " blocks)";
    }
}
