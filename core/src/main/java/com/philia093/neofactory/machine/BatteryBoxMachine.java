package com.philia093.neofactory.machine;

import com.philia093.neofactory.cable.Voltage;
import com.philia093.neofactory.item.Batteries;
import com.philia093.neofactory.item.Battery;
import com.philia093.neofactory.item.ItemStack;

/**
 * A box of cells: the machine that keeps the power of a line of cables for later.
 * <p>
 * <b>A box holds cells and nothing else.</b> What a player puts into it is a cell of its own tier - one, four,
 * nine or sixteen of them, see {@link BatteryBoxes} - and the cells are the buffer of the machine: what a line
 * feeds the box with is spread over them and what the machines around it take comes out of them, see
 * {@link BatteryBank}. A box therefore has no recipe, no product and no tank, and its panel is a grid of plain
 * slots and nothing else, see {@link MachineScreen#gridSlots()}.
 * <p>
 * <b>A box may be charged and emptied at the same time.</b> One cell of it takes two amperes of the tier a
 * tick in and gives one ampere of it out, so a box that is fed harder than it is drained charges its cells and
 * one that is drained harder empties them; a box with no cell in it holds nothing, gives nothing and asks for
 * nothing at all. Nothing of that is a tick of its own: what the box takes is what its block reaches for
 * through the plug a player gave it - the block asks {@link #requestEu()} every tick, which is exactly what
 * the box may take in - and what it gives is what the line around it takes, see
 * {@code MachineBlockEntity#updateEnergy} and {@code EnergyGrid.Line#pull}.
 * <p>
 * <b>A box has no front.</b> A player fills it from wherever they stand, so there is no side of it a job may
 * not be put on and the wrench never turns it, see {@link FaceConfig#withoutFront()}. Its two plugs are the
 * two sides a player gives the power in and out, and it carries both of them whether or not a cell stands in
 * it, see {@link FaceConfig#withPlugs()}: a player builds the box and the line around it and puts the cells in
 * afterwards.
 */
public class BatteryBoxMachine extends Machine {

    private final Voltage tier;

    private final BatteryBank bank;

    /**
     * Creates a box of cells.
     *
     * @param tier tier the box was built for, which is the tier of the cells it takes
     * @param cells amount of cells the box holds, one of {@link BatteryBoxes#CELLS}
     * @throws IllegalArgumentException when no such box of the game exists
     */
    public BatteryBoxMachine(Voltage tier, int cells) {
        this(tier, BatteryBoxes.inventoryOf(cells));
    }

    /** Creates a box whose inventory was built before this constructor, so that the storage is set once. */
    private BatteryBoxMachine(Voltage tier, MachineInventory inventory) {
        super(BatteryBoxes.screenOf(tier, inventory.size()), inventory, new BatteryBank(inventory, tier),
                java.util.List.of());
        this.tier = tier;
        this.bank = (BatteryBank) energy();
        // The box is built for what a player does with it and not for what lies in it: it has no front, and
        // it carries both plugs even while every slot of it is empty, see FaceConfig. The plugs are put where
        // a machine that looks north has them and are then read as the sides of the world they stand on, which
        // is the south and the east of the box.
        faces().withPlugs().withoutFront();
    }

    /** Tier this box was built for, which is the tier of the cells it takes and the line it stands on. */
    public Voltage tier() {
        return tier;
    }

    /** Amount of cells this box holds, which is the shape of its panel as well. */
    public int cells() {
        return inventory().size();
    }

    /** The buffer of this box, which is the charge of the cells standing in it. */
    public BatteryBank bank() {
        return bank;
    }

    /**
     * A box takes the cells of its own tier and nothing else.
     * <p>
     * <b>A cell of another tier is no cell of this box.</b> A line of the middle voltage destroys a machine of
     * the low voltage and a cell of the high voltage would burn a line of the middle one, so what stands in a
     * box is a cell of the box's own tier and every other stack stays where a player picked it up, see
     * {@link Battery#voltage()}. Nothing is said about the chemistry: whether a cell may be filled again is
     * the business of the cell and not of the box, see {@link com.philia093.neofactory.item.BatteryChemistry}.
     *
     * @param slot index of the slot inside {@link #inventory()}
     * @param stack stack that would go into it, never empty
     * @return {@code true} when the stack is a cell of the tier of this box
     */
    @Override
    public boolean acceptsItem(int slot, ItemStack stack) {
        Battery cell = Batteries.of(stack);
        return cell != null && cell.voltage() == tier;
    }

    /**
     * Energy this box wants from a line this tick, which is all it may take.
     * <p>
     * A box is no machine that works: there is no recipe that decides how much it needs, so what it asks for
     * is what its cells allow - two amperes of its tier for every cell standing in it, nothing at all while it
     * holds none. The block of the machine hands that question to the line of the plug a player gave the box,
     * and what the line does not bring simply does not arrive: the cells the box already holds keep it fed
     * while a line is busy, see {@code MachineBlockEntity#updateEnergy}.
     */
    @Override
    public int requestEu() {
        return bank.maxReceive();
    }

    /**
     * A box does no work of its own.
     * <p>
     * Everything that moves through a box is moved by somebody else: the block of the machine reaches for what
     * the box asks for, see {@link #requestEu()}, and the machines of the line around it take what they need
     * out of the cells, see {@code EnergyGrid.Line#pull}. What is left is the charge of the cells, which waits
     * where it is for as long as a player leaves them there.
     *
     * @param delta time since the last frame in seconds, which a box does not read
     */
    @Override
    protected void update(float delta) {
        // Nothing of a box happens by itself, see the note above.
    }

    /**
     * A box is a box of the casing of its own tier, whichever side of it a player works.
     * <p>
     * The two sides a player gave the power to stand on the casing of the tier of the machine, the way they do
     * at every machine of the line, see {@link MachineCasing}.
     */
    @Override
    public String casing() {
        return MachineCasing.pictureOf(tier);
    }

    @Override
    public String toString() {
        return "BatteryBoxMachine(" + name() + ", " + bank + ")";
    }
}
