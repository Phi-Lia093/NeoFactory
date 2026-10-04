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
 * not made for is not slower, it is fatal - the energy is not handed over at all and the machine is taken out
 * of the world, see {@link EnergyAcceptor} and {@code EnergyGrid.Line#pull}.
 * <p>
 * <b>A machine takes one ampere of its own tier.</b> The two limits of the buffer are the amount one tick of
 * a line of that tier carries, so a machine of the low voltage takes thirty two units a tick and no more
 * however wide the line at it is, while the capacity is how many ticks of work it may store - which is what a
 * machine does with the energy while it waits for the next craft.
 * <p>
 * A machine whose buffer takes power in is a machine a line may feed and a machine whose buffer gives power
 * out is one that feeds a line, which is what gives such a machine the two plugs of {@link FaceConfig}: the
 * sides a player sets with the wrench, see {@code MachineBlockEntity#updateEnergy}.
 * <p>
 * <b>A machine that only works</b> - every machine of the electrical age that pays for a recipe - <b>is given
 * a buffer that may not be emptied</b>, a {@code maxExtract} of zero: it takes power in through its one plug
 * and is never a source of the line it stands on, which is what keeps a line of machines that work from
 * feeding one another. A machine that makes power is the mirror of it: it fills its own buffer and gives it
 * out, and the line around it takes what it made, see {@code EnergyGrid.Line#pull}.
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

    /**
     * Amount of energy one side of a machine holds, which is what the other side of it holds as well.
     * <p>
     * A block of the line that holds energy on two sides - a transformer, a diode - holds what a machine of
     * the age of that side holds inside itself: {@value com.philia093.neofactory.machine.ElectricMachine#BUFFER_TICKS}
     * ticks of the voltage of the tier, which is the buffer a machine of the line is built with, see
     * {@code ElectricMachine#bufferOf}. A transformer of the low voltage therefore holds two thousand and
     * forty eight units on the side of the low voltage and eight thousand one hundred and ninety two on the
     * side of the middle one, which is what a machine of either age keeps inside itself.
     *
     * @param tier tier the side was built for
     * @return energy that side holds
     */
    public static int capacityOf(Voltage tier) {
        Objects.requireNonNull(tier, "tier");
        return ElectricMachine.BUFFER_TICKS * tier.euPerTick();
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

    /**
     * Spends the energy of a machine that works, the way {@link #make} fills the buffer of one that makes it.
     * <p>
     * <b>A machine that pays for a recipe works out of its own buffer, and the buffer of such a machine is
     * one a line may not empty.</b> The two are the same ring seen from both sides: a line draws energy out
     * of a machine that makes it, so that buffer answers {@link #extract(int, boolean)} - while a machine
     * that works only takes, and its own spending never goes through the contract a line meets. This is that
     * door, and it is the mirror of {@link #make}: the machine takes what it spent and nothing hands it back.
     *
     * @param amount energy the machine is about to spend, at least zero
     * @return the amount the buffer could give
     */
    public int spend(int amount) {
        int spent = Math.max(0, Math.min(amount, amount()));
        setAmount(amount() - spent);
        return spent;
    }

    @Override
    public String toString() {
        return "MachineEnergyStorage(" + amount() + "/" + capacity() + ", " + tier + ")";
    }
}
