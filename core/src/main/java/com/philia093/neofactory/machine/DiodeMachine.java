package com.philia093.neofactory.machine;

import com.philia093.neofactory.block.BlockFace;
import com.philia093.neofactory.cable.Voltage;
import com.philia093.neofactory.util.nbt.NbtCompound;
import com.philia093.neofactory.world.save.SaveTags;

import java.util.List;
import java.util.Objects;

/**
 * A diode: a machine of one direction, a transformer whose two ends are of the same age.
 * <p>
 * <b>A diode is no machine that works.</b> It holds no tank and no slot, and what it is built of is the two
 * sides of a machine - the side of its <b>left flank</b> takes the power in and the side of its <b>right</b>
 * one hands it out - with a buffer of the size of its own age standing between them, see
 * {@link Diodes#capacityOf(Voltage)}. A tick of a diode moves what stands in the side that takes the power
 * over to the side that hands it out and loses nothing of it, which is what a transformer does as well: a
 * diode is a transformer with the two of its ends of one age, so the line that ends at its left flank and the
 * line that starts at its right one carry the same energy between them, see {@link TransformerMachine}.
 * <p>
 * <b>What a line does at a diode is what it does at any machine.</b> A line is walked by the machine that asks
 * for the power, and it stops at a diode the way it stops at the furnace beside it: the left flank of a diode
 * is an end of the line that feeds it and its right flank is an end of the line it feeds. Nothing runs back
 * through it because the two sides of it never trade places - the side that takes the power in is what the
 * block was built for, see {@link MachineEnergyStorage} and {@code EnergyGrid#line}.
 * <p>
 * <b>The two sides a diode hands the power between are what it was built for.</b> The power of the industry
 * runs in by the <b>left flank</b> of a machine and out by its <b>right</b> one - the two sides a player
 * standing in front of it sees - and no player moves them: what the wrench does at a diode is <b>turn the
 * whole block</b>, which is how a player aims the direction the power crosses it in, see {@code FaceConfig}
 * and {@code MachineBlockEntity#operateFace}. The front of a diode carries no job like the front of every
 * machine, so the power always crosses a diode sideways.
 * <p>
 * <b>A diode has no panel.</b> There is nothing in it to look at and nothing to set: what a player reads off
 * it is the colour of the terminal of its tier and which way the block is turned, see
 * {@link MachineTerminals} and {@code Machine#opensPanel}.
 */
public final class DiodeMachine extends Machine {

    private final Voltage tier;

    private final int amperage;

    private final MachineEnergyStorage inputSide;

    private final MachineEnergyStorage outputSide;

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
                inputSideOf(tier, width),
                List.of());
        this.tier = Objects.requireNonNull(tier, "tier");
        this.amperage = Diodes.amperageOf(width);
        this.inputSide = (MachineEnergyStorage) energy();
        this.outputSide = outputSideOf(tier, width);
        // The power of the industry runs in by the left flank of a machine and out by its right one, and those
        // two sides are what a diode was built for and never what a player sets, see the note on this class:
        // what the wrench does at a diode is turn the whole block, see DiodeBlockEntity#operateFace.
        faces().withPlugs();
        faces().setEnergyIn(MachineSides.leftOf(MachineSides.DEFAULT_FRONT));
        faces().setEnergyOut(MachineSides.rightOf(MachineSides.DEFAULT_FRONT));
    }

    /**
     * The buffer of the side a diode takes the power in by, which is the one a line feeds it through.
     * <p>
     * A side of a diode holds what a machine of its own age holds inside itself, see
     * {@link Diodes#capacityOf(Voltage)}, and it takes what the width of the diode allows a tick: a diode of
     * one ampere takes what one machine of the line hands over, a wider one what a bundle of that many
     * machines would, see {@link Diodes#WIDTHS}.
     */
    private static MachineEnergyStorage inputSideOf(Voltage tier, int width) {
        int rating = ratingOf(tier, width);
        return new MachineEnergyStorage(Diodes.capacityOf(tier), rating, rating, tier);
    }

    /** The buffer of the side a diode hands the power out by, which is the one a line is fed from. */
    private static MachineEnergyStorage outputSideOf(Voltage tier, int width) {
        int rating = ratingOf(tier, width);
        return new MachineEnergyStorage(Diodes.capacityOf(tier), rating, rating, tier);
    }

    /** Energy one tick of a line of a tier at an amperage carries. */
    private static int ratingOf(Voltage tier, int width) {
        return tier.euPerTick() * Diodes.amperageOf(width);
    }

    /** Tier this diode was built for, which is the highest voltage it carries. */
    public Voltage tier() {
        return tier;
    }

    /** Current this diode carries from one side of it to the other, in amperes. */
    public int amperage() {
        return amperage;
    }

    /** The buffer the power of a line arrives in, which is the side of the left flank of this diode. */
    public MachineEnergyStorage inputSide() {
        return inputSide;
    }

    /** The buffer the power of a line leaves through, which is the side of the right flank of this diode. */
    public MachineEnergyStorage outputSide() {
        return outputSide;
    }

    /** What a diode wants a tick is the whole width of it, which is what it carries from one side to the other. */
    @Override
    public int requestEu() {
        return ratingOf(tier, amperage);
    }

    /**
     * The buffer one side of a diode reaches: the one that takes the power in and the one that hands it out.
     * <p>
     * <b>A diode answers for its two flanks and for nothing else.</b> The power of the industry runs in by the
     * left flank of a machine and out by its right one, so a line that stands at the left flank of a diode
     * feeds it and a line that stands at its right one is fed by it - while a line or a machine at the front of
     * the block, or at its back, reaches nothing at all, see {@code EnergyGrid.Cells#buffer} and
     * {@code Machine#energyOn}. A side that reaches a buffer is an end of the line at it, which is where the
     * one direction of a diode comes from: the side that hands the power out is no side a line may hand power
     * to it by, so nothing ever runs back through it.
     *
     * @param side side of this machine in the world
     * @return the buffer of that side, {@code null} for the front and the back of the block
     */
    @Override
    public EnergyStorage energyOn(BlockFace side) {
        Objects.requireNonNull(side, "side");
        if (side == faces().energyIn()) {
            return inputSide;
        }
        return side == faces().energyOut() ? outputSide : null;
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
     * Hands what the side that takes the power holds over to the side that hands it out.
     * <p>
     * Nothing is lost and nothing is made, which is what makes a diode a transformer whose two ends are of one
     * age: what a line hands to the left flank of a diode is what the line at its right flank finds standing
     * there a tick later. How much crosses it a tick is what its two sides take and give, which is the width it
     * was built for, see {@link #inputSide()}.
     *
     * @param delta time since the last frame in seconds, which a diode does not read: it carries whatever
     *              stands in the side that takes the power over a tick and no more
     */
    @Override
    protected void update(float delta) {
        int room = outputSide.capacity() - outputSide.amount();
        int moved = Math.min(inputSide.amount(), room);
        if (moved <= 0) {
            return;
        }
        outputSide.make(inputSide.spend(moved));
    }

    /** Writes what the side that hands the power out of this diode holds, see Machine#save. */
    @Override
    protected void saveState(NbtCompound state) {
        state.putInt(SaveTags.ENERGY_HELD, outputSide.amount());
    }

    @Override
    protected void loadState(NbtCompound state) {
        outputSide.setAmount(state.getInt(SaveTags.ENERGY_HELD, 0));
    }

    @Override
    public String toString() {
        return "DiodeMachine(" + name() + ", " + amperage + " A, " + tier + ")";
    }
}
