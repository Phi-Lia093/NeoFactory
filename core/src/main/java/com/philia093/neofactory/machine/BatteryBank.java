package com.philia093.neofactory.machine;

import com.philia093.neofactory.cable.Voltage;
import com.philia093.neofactory.energy.EnergyAcceptor;
import com.philia093.neofactory.item.Batteries;
import com.philia093.neofactory.item.Battery;
import com.philia093.neofactory.item.ItemStack;

import java.util.Objects;

/**
 * The energy a box of cells holds, which is the charge of the cells standing in it.
 * <p>
 * <b>A box of cells is a buffer a line of cables may fill and empty, and the buffer is the cells.</b> What a
 * line hands over is spread over the cells in the box, and what a line takes comes out of them in the same
 * order: there is no second place inside a box where energy is kept, so a player who opens a box and takes a
 * cell out of it takes away exactly the charge that cell carried, and a spent cell stays a spent cell. That
 * is what makes a box a box and not a battery: what the game stores is the stack, which travels with the
 * inventory of the machine, see {@link Battery}.
 * <p>
 * <b>Every cell of a box is a mouth of its own.</b> One cell adds {@link #AMPS_IN} amperes to what the box may
 * take in a tick and {@link #AMPS_OUT} to what it may give out, because a cell of the industry is filled and
 * emptied at an ampere of its tier: a box of four cells of the low voltage takes two hundred and fifty six
 * units a tick and gives a hundred and twenty eight, while a box with no cell in it holds nothing, gives
 * nothing and asks a line for nothing at all.
 * <p>
 * <b>The box is the buffer and not the tick.</b> Nothing here moves energy by itself: what arrives is what
 * the block of the machine reaches for through the plug of the box, see
 * {@code MachineBlockEntity#updateEnergy} and {@link BatteryBoxMachine#requestEu()}, and what leaves is what
 * the machines of the line around it take, see {@code EnergyGrid.Line#pull}. A box therefore holds what a
 * player left in it for as long as the world lasts.
 */
public final class BatteryBank extends SimpleEnergyStorage implements EnergyAcceptor {

    /** Amperes one cell of a box adds to what the box may take in, a tick. */
    public static final int AMPS_IN = 2;

    /** Amperes one cell of a box adds to what the box may give out, a tick. */
    public static final int AMPS_OUT = 1;

    /** Inventory of the box, which is where the cells are and where the charge lives. */
    private final MachineInventory inventory;

    private final Voltage tier;

    /**
     * Creates the storage of a box.
     * <p>
     * The limits of the base class are left at nothing on purpose: every amount and every limit of a box is
     * read from the cells of its inventory, see {@link #amount()} and {@link #maxReceive()}.
     *
     * @param inventory inventory of the box, whose slots all hold a cell or nothing
     * @param tier tier the box was built for, which is the tier of the cells it takes
     */
    public BatteryBank(MachineInventory inventory, Voltage tier) {
        super(0);
        this.inventory = Objects.requireNonNull(inventory, "inventory");
        this.tier = Objects.requireNonNull(tier, "tier");
    }

    /** Tier this box was built for, which is the fastest line that may fill it. */
    @Override
    public Voltage accepted() {
        return tier;
    }

    @Override
    public int amount() {
        int stored = 0;
        for (int slot = 0; slot < inventory.size(); slot++) {
            Battery cell = cellAt(slot);
            if (cell != null) {
                stored += cell.chargeOf(inventory.get(slot));
            }
        }
        return stored;
    }

    @Override
    public int capacity() {
        int room = 0;
        for (int slot = 0; slot < inventory.size(); slot++) {
            Battery cell = cellAt(slot);
            if (cell != null) {
                room += cell.capacity();
            }
        }
        return room;
    }

    /**
     * Energy this box may take in a tick, which is what one cell of it is filled with a tick for every cell
     * standing in it.
     *
     * @return the limit in units of the game, {@code 0} for a box without a cell
     */
    public int maxReceive() {
        return AMPS_IN * cells() * tier.euPerTick();
    }

    /**
     * Energy this box may give out in a tick, which is what one cell of it hands over a tick for every cell
     * standing in it.
     *
     * @return the limit in units of the game, {@code 0} for a box without a cell
     */
    public int maxExtract() {
        return AMPS_OUT * cells() * tier.euPerTick();
    }

    /** Amount of cells standing in this box, which is what its two currents come from. */
    public int cells() {
        int standing = 0;
        for (int slot = 0; slot < inventory.size(); slot++) {
            if (cellAt(slot) != null) {
                standing++;
            }
        }
        return standing;
    }

    /**
     * {@code true} when this box has a mouth to be filled through, a cell in a slot.
     * <p>
     * <b>What a box names is what it was built with, not what its cells hold at the moment.</b> A box with a
     * cell in it may be filled and emptied whatever the charge of that cell is, so the block of a box whose
     * cells are full is stopped by {@link #isFull()} and not by this question, exactly the way a buffer of a
     * machine is, see {@link SimpleEnergyStorage#canReceive()}.
     */
    @Override
    public boolean canReceive() {
        return maxReceive() > 0;
    }

    /** {@code true} when this box has a mouth to be emptied through, see {@link #canReceive()}. */
    @Override
    public boolean canExtract() {
        return maxExtract() > 0;
    }

    @Override
    public int receive(int maxReceive, boolean simulate) {
        int offered = Math.min(maxReceive, maxReceive());
        int taken = 0;
        for (int slot = 0; slot < inventory.size() && taken < offered; slot++) {
            Battery cell = cellAt(slot);
            if (cell == null) {
                continue;
            }
            ItemStack stack = inventory.get(slot);
            int fits = Math.min(offered - taken, roomIn(cell, stack));
            if (fits <= 0) {
                continue;
            }
            // A cell that is asked what it would take is not written to: what one of them takes is what it
            // has room for, see Battery#insert.
            taken += simulate ? fits : cell.insert(stack, fits);
        }
        return taken;
    }

    @Override
    public int extract(int maxExtract, boolean simulate) {
        int wanted = Math.min(maxExtract, maxExtract());
        int given = 0;
        for (int slot = 0; slot < inventory.size() && given < wanted; slot++) {
            Battery cell = cellAt(slot);
            if (cell == null) {
                continue;
            }
            ItemStack stack = inventory.get(slot);
            int left = Math.min(wanted - given, cell.chargeOf(stack));
            if (left <= 0) {
                continue;
            }
            given += simulate ? left : cell.extract(stack, left);
        }
        return given;
    }

    /**
     * Energy a cell of one slot would take, which is nothing at all for a cell that never takes a charge.
     *
     * @param cell battery of that slot
     * @param stack stack that stands in it
     * @return the room left in the cell, in units of the game
     */
    private static int roomIn(Battery cell, ItemStack stack) {
        return cell.isRechargeable() ? Math.max(0, cell.capacity() - cell.chargeOf(stack)) : 0;
    }

    /** The cell in one slot, {@code null} for a slot that holds no cell at all. */
    private Battery cellAt(int slot) {
        return Batteries.of(inventory.get(slot));
    }

    /**
     * Writes an amount into this storage, which is what nothing of a box is.
     * <p>
     * <b>A box counts no amount of its own, and this is the door a save game uses.</b> What a box holds is
     * the charge of the cells in it, and the cells are read out of the inventory of the machine one line
     * before that amount is handed over, see {@code Machine#load}: a stored amount would therefore be added
     * to the very charge it was read from. The base class is kept for what a machine that counts an amount of
     * its own does with it; what a box writes into it is nothing, and this is where that is said.
     *
     * @param amount amount to write, which is left where it came from
     */
    @Override
    public void setAmount(int amount) {
        // Nothing of a box is counted here: the charge of its cells is what it holds, see the note above.
    }

    @Override
    public String toString() {
        return "BatteryBank(" + amount() + "/" + capacity() + ", " + cells() + " cells of " + tier + ", "
                + maxReceive() + " in, " + maxExtract() + " out)";
    }
}
