package com.philia093.neofactory.machine;

import com.philia093.neofactory.block.BlockFace;
import com.philia093.neofactory.cable.Voltage;
import com.philia093.neofactory.util.nbt.NbtCompound;
import com.philia093.neofactory.world.interaction.FaceRole;
import com.philia093.neofactory.world.save.SaveTags;

import java.util.ArrayList;
import java.util.List;

/**
 * A transformer: the machine that joins two ages of the line of the power.
 * <p>
 * <b>A transformer is two buffers of energy and a tick that moves the energy of one into the other.</b> The
 * side of the high voltage is one buffer and the five sides of the low one are the other, and a tick of the
 * machine hands what stands in the buffer that takes it over to the buffer that gives it out - nothing is
 * lost and nothing is made, so <b>one ampere of the high voltage leaves as four amperes of the low one</b>,
 * because the tiers of the line are four apart, see {@link Voltage}: the energy is the energy and the
 * current is what changes, see {@link Transformers}.
 * <p>
 * <b>Which end takes the power in is what a player decides.</b> A transformer that steps down takes it in on
 * the side of the high voltage and hands it out of its five low sides, and one that steps up does the
 * opposite: a player knocks on the block with a mallet and the machine turns around, see
 * {@code TransformerBlockEntity}. <b>Which side is the high one never changes</b> - it is the front of the
 * machine, and the wrench turns the whole block to aim it - so what a player reads at a transformer is one
 * side of the high voltage and five of the low one, whichever way the power runs.
 * <p>
 * A transformer holds no slot and no tank and has no panel: what a player reads off it is the casing of its
 * tier, the colour of the terminal at every one of its sides and the arrows the grid of faces draws there,
 * see {@link MachineTerminals} and {@code Machine#opensPanel}.
 */
public final class TransformerMachine extends Machine {

    /** Ticks of its rating one buffer of a transformer holds. */
    private static final int CAPACITY_TICKS = 2;

    private final Voltage low;

    private final Voltage high;

    private final int highAmperage;

    private final int lowAmperage;

    private final MachineEnergyStorage highSide;

    private final MachineEnergyStorage lowSide;

    /** {@code true} while the power of this transformer runs from its high side to its low one. */
    private boolean steppingDown = true;

    /**
     * Creates a transformer of a tier and a size.
     *
     * @param tier tier of the low side of the transformer, one of {@link Transformers#TIERS}
     * @param size amperes of its high side, one of {@link Transformers#SIZES}
     * @throws IllegalArgumentException when the game holds no such transformer
     */
    public TransformerMachine(Voltage tier, int size) {
        super(new MachineScreen(Transformers.titleOf(tier, size), ProgressKind.NONE, List.of(), List.of(), 0, 0,
                        false),
                new MachineInventory(),
                highSideOf(tier, size),
                List.of());
        this.low = tier;
        this.high = Transformers.highOf(tier);
        this.highAmperage = Transformers.amperageOf(size);
        this.lowAmperage = Transformers.lowAmperageOf(size);
        this.highSide = (MachineEnergyStorage) energy();
        this.lowSide = lowSideOf(tier, size);
    }

    /** The buffer of the high side of a transformer, which is the one a line of that voltage is reached at. */
    private static MachineEnergyStorage highSideOf(Voltage tier, int size) {
        Voltage high = Transformers.highOf(tier);
        int rating = ratingOf(high, Transformers.amperageOf(size));
        return new MachineEnergyStorage(CAPACITY_TICKS * rating, rating, rating, high);
    }

    /** The buffer of the low side of a transformer, which is the one its five sides are reached at. */
    private static MachineEnergyStorage lowSideOf(Voltage tier, int size) {
        int rating = ratingOf(tier, Transformers.lowAmperageOf(size));
        return new MachineEnergyStorage(CAPACITY_TICKS * rating, rating, rating, tier);
    }

    /** Energy one tick of a line of a tier at an amperage carries. */
    private static int ratingOf(Voltage tier, int amperage) {
        return tier.euPerTick() * amperage;
    }

    /** Tier of the low side of this transformer, which is the age it is named for. */
    public Voltage lowTier() {
        return low;
    }

    /** Tier of the high side of this transformer, which is the age above its own. */
    public Voltage highTier() {
        return high;
    }

    /** Amperes the high side of this transformer carries. */
    public int highAmperage() {
        return highAmperage;
    }

    /** Amperes the low side of this transformer carries, four times what its high side does. */
    public int lowAmperage() {
        return lowAmperage;
    }

    /** {@code true} while the power of this transformer runs from its high side to its low one. */
    public boolean isSteppingDown() {
        return steppingDown;
    }

    /**
     * Turns this transformer around.
     * <p>
     * The side of the high voltage and the five of the low one are what the block was built for and never
     * move; what a knock of a mallet changes is which end of them <b>takes the power in</b>, see
     * {@code TransformerBlockEntity}.
     */
    public void toggle() {
        steppingDown = !steppingDown;
    }

    /** The one side of this transformer that carries the high voltage, which is the front of the machine. */
    public BlockFace highSide() {
        return faces().facing();
    }

    /** The five sides of this transformer that carry the low voltage. */
    public List<BlockFace> lowSides() {
        List<BlockFace> sides = new ArrayList<>(BlockFace.ALL.length);
        for (BlockFace side : BlockFace.ALL) {
            if (side != highSide()) {
                sides.add(side);
            }
        }
        return List.copyOf(sides);
    }

    /** The buffer the power of a line comes in through, which is the high side of a transformer that steps down. */
    public MachineEnergyStorage inputBuffer() {
        return steppingDown ? highSide : lowSide;
    }

    /** The buffer the power is handed out of, which is the low side of a transformer that steps down. */
    public MachineEnergyStorage outputBuffer() {
        return steppingDown ? lowSide : highSide;
    }

    /** The side that takes the power in carries it in and the other one hands it out. */
    @Override
    public FaceRole roleOn(BlockFace side) {
        return side == highSide() == steppingDown ? FaceRole.ENERGY_IN : FaceRole.ENERGY_OUT;
    }

    @Override
    public EnergyStorage energyOn(BlockFace side) {
        return side == highSide() ? highSide : lowSide;
    }

    /** A transformer that steps down takes its power in on one side and one that steps up on five. */
    @Override
    public List<BlockFace> inputSides() {
        return steppingDown ? List.of(highSide()) : lowSides();
    }

    /** The high side of a transformer carries a line of the high voltage and its five low sides another. */
    @Override
    public Voltage lineTier(BlockFace side) {
        return side == highSide() ? high : low;
    }

    @Override
    public Voltage lineTier() {
        return low;
    }

    /** What a transformer wants a tick is all it may take in, which is the rating of the side that takes it. */
    @Override
    public int requestEu() {
        return steppingDown ? ratingOf(high, highAmperage) : ratingOf(low, lowAmperage);
    }

    /** Casing this transformer is built of, the one of the age it is named for. */
    @Override
    public String casing() {
        return MachineCasing.pictureOf(low);
    }

    /** {@code false}: a transformer has nothing a panel could show, see the note on this class. */
    @Override
    public boolean opensPanel() {
        return false;
    }

    /**
     * Hands what the buffer that takes the power holds over to the buffer that gives it out.
     * <p>
     * Nothing is lost and nothing is made: what stands in one of the two buffers of a transformer is the
     * energy of the other one, so one ampere of the high voltage leaves as four amperes of the low one, see
     * the note on this class.
     *
     * @param delta time since the last frame in seconds, which a transformer does not read
     */
    @Override
    protected void update(float delta) {
        MachineEnergyStorage in = inputBuffer();
        MachineEnergyStorage out = outputBuffer();
        int room = out.capacity() - out.amount();
        int moved = Math.min(in.amount(), room);
        if (moved <= 0) {
            return;
        }
        out.make(in.spend(moved));
    }

    @Override
    protected void saveState(NbtCompound state) {
        state.putBoolean(SaveTags.STEPPING_DOWN, steppingDown);
    }

    @Override
    protected void loadState(NbtCompound state) {
        steppingDown = state.getBoolean(SaveTags.STEPPING_DOWN, true);
    }

    @Override
    public String toString() {
        return "TransformerMachine(" + name() + ", " + (steppingDown ? "step down" : "step up") + ')';
    }
}
