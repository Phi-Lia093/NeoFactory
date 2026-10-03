package com.philia093.neofactory.machine;

import com.philia093.neofactory.cable.Voltage;
import com.philia093.neofactory.energy.EnergyAcceptor;

import java.util.Objects;

/**
 * The buffer of a machine of the power network: a {@link SimpleEnergyStorage} that knows the tier it was
 * built for.
 * <p>
 * A machine of the electrical age is filled and emptied by the line of cables it stands on, and the tier of
 * that line is the one question the buffer has to answer beyond the amounts: a line of a tier a machine was
 * not made for is not slower, it is fatal - the energy is not handed over at all and the line takes the
 * machine and every cable of it away, see {@link EnergyAcceptor} and {@code EnergyGrid.Line#push}.
 * <p>
 * <b>A machine takes one ampere of its own tier.</b> The two limits of the buffer are the amount one tick of
 * a line of that tier carries, so a machine of the low voltage takes thirty two units a tick and no more
 * however wide the line at it is, while the capacity is how many ticks of work it may store - which is what a
 * machine does with the energy while it waits for the next craft.
 * <p>
 * A machine whose buffer takes power in is a machine a line may feed and a machine whose buffer gives power
 * out is one that feeds a line, which is what gives such a machine the two plugs of {@link FaceConfig}: the
 * sides a player sets with the wrench, see {@code MachineBlockEntity#updateEnergy}.
 */
public final class MachineEnergyStorage extends SimpleEnergyStorage implements EnergyAcceptor {

    private final Voltage tier;

    /**
     * Creates the buffer of a machine of a tier, which fills and empties as fast as one ampere of it.
     *
     * @param capacity largest amount the buffer holds
     * @param tier tier the machine was built for, which is the fastest line that may feed it
     */
    public MachineEnergyStorage(int capacity, Voltage tier) {
        this(capacity, Objects.requireNonNull(tier, "tier").euPerTick(), tier.euPerTick(), tier);
    }

    /**
     * Creates the buffer of a machine of a tier with limits of its own.
     *
     * @param capacity largest amount the buffer holds
     * @param maxReceive largest amount one call may add
     * @param maxExtract largest amount one call may take
     * @param tier tier the machine was built for, which is the fastest line that may feed it
     */
    public MachineEnergyStorage(int capacity, int maxReceive, int maxExtract, Voltage tier) {
        super(capacity, maxReceive, maxExtract);
        this.tier = Objects.requireNonNull(tier, "tier");
    }

    @Override
    public Voltage accepted() {
        return tier;
    }

    /**
     * Fills the buffer with the energy the machine behind it just made.
     * <p>
     * <b>A generator makes its power and hands it to its own buffer</b>, which is why the amount does not
     * travel through {@link #receive(int, boolean)}: that limit is the one a <b>line</b> meets, and no line may
     * feed a machine that only hands power over, see {@link #canReceive()} and
     * {@link com.philia093.neofactory.machine.SteamTurbineMachine}. What is made is what fits into the buffer.
     *
     * @param amount energy the machine made, at least zero
     * @return the amount the buffer took
     */
    public int make(int amount) {
        int made = Math.max(0, Math.min(amount, capacity() - amount()));
        setAmount(amount() + made);
        return made;
    }

    @Override
    public String toString() {
        return "MachineEnergyStorage(" + amount() + "/" + capacity() + ", " + tier + ")";
    }
}
