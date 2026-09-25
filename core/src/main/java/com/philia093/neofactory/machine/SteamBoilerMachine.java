package com.philia093.neofactory.machine;

import com.philia093.neofactory.fluid.Fluids;
import com.philia093.neofactory.fluid.SimpleFluidStorage;
import com.philia093.neofactory.item.ItemStack;
import com.philia093.neofactory.util.nbt.NbtCompound;
import com.philia093.neofactory.world.save.SaveTags;

import java.util.List;

/**
 * The bronze boiler, the machine that turns water into steam.
 * <p>
 * It is the first machine of the industry and the one the machines of the bronze age are built around: a
 * fire heats water until it becomes steam, and the steam is what those machines run on, see
 * {@link MachineTank}. The boiler holds one slot and two tanks:
 * <ul>
 *     <li>the fuel slot, which burns what {@link Fuels} says burns - a piece of coal keeps a boiler going
 *         for eighty seconds;</li>
 *     <li>the tank of water it boils, which a fluid pipe fills;</li>
 *     <li>the tank of steam it makes, which a pipe takes away or a cell is filled from.</li>
 * </ul>
 * <p>
 * <b>One unit of water becomes {@link #STEAM_PER_WATER} units of steam</b>, and a second of burning boils
 * {@link #WATER_PER_SECOND} of water, so a boiler that burns coal makes
 * {@code WATER_PER_SECOND * STEAM_PER_WATER} units of steam a second while its tanks allow it. Water that
 * does not make a whole unit yet is kept for the frames after it, so nothing is lost to the arithmetic of
 * a single frame.
 * <p>
 * <b>A boiler does not waste fuel.</b> It takes a piece of fuel only when it has water to boil and room for
 * the steam, and a flame that is already burning waits while the tank of water is empty or the tank of
 * steam is full - which is what makes a boiler that ran dry sit quiet instead of chewing through a stack of
 * coal.
 * <p>
 * The machine has no product of its own, because what it makes is steam: its screen carries the one slot
 * and the two tanks, see {@link MachineScreen}.
 */
public final class SteamBoilerMachine extends Machine implements FuelMachine, ProgressMachine {

    /** Slot that holds what keeps the boiler burning. */
    public static final int FUEL = 0;

    /** Water one second of burning boils, in units of the game. */
    public static final float WATER_PER_SECOND = 20.0f;

    /** Units of steam one unit of water becomes. */
    public static final int STEAM_PER_WATER = 16;

    /** Amount of water the tank of the boiler holds, sixteen cells. */
    public static final int WATER_CAPACITY = 16_000;

    /** Amount of steam the tank of the boiler holds, sixteen cells. */
    public static final int STEAM_CAPACITY = 16_000;

    /**
     * Screen of the boiler: the fuel slot on the left and a tank on either side of the progress bar, which
     * is what a machine without a product looks like.
     */
    public static final MachineScreen SCREEN = new MachineScreen("Bronze Boiler",
            ProgressKind.GENERIC, List.of(SlotKind.SMELTING), List.of(), 1, 1, false);

    private final SimpleFluidStorage water;
    private final SimpleFluidStorage steam;

    private float burnSeconds;
    private float burnTotal;

    /** Water that was earned but is not a whole unit yet, always below one. */
    private float waterDebt;

    /** Creates an empty boiler. */
    public SteamBoilerMachine() {
        super(SCREEN, new MachineInventory(MachineInventory.Role.FUEL), new SimpleEnergyStorage(0),
                List.of(),
                new MachineTank(new SimpleFluidStorage(WATER_CAPACITY), MachineTank.Role.INPUT),
                new MachineTank(new SimpleFluidStorage(STEAM_CAPACITY), MachineTank.Role.OUTPUT));
        // The machine hands its tanks back, so the boiler works on the very storages it was built with.
        this.water = (SimpleFluidStorage) tank(0).storage();
        this.steam = (SimpleFluidStorage) tank(1).storage();
    }

    /** Tank of water this boiler boils. */
    public SimpleFluidStorage water() {
        return water;
    }

    /** Tank of steam this boiler makes. */
    public SimpleFluidStorage steam() {
        return steam;
    }

    @Override
    protected void update(float delta) {
        if (burnSeconds <= 0.0f && !light()) {
            // Without a flame the boiler waits, and a piece of fuel is not taken until there is
            // something to boil, see light.
            return;
        }
        if (water.isEmpty() || steam.isFull()) {
            // Nothing to boil or nowhere to put the steam: the flame waits as well, so no fuel is lost.
            return;
        }
        float burning = Math.min(delta, burnSeconds);
        boil(burning);
        burnSeconds = Math.max(0.0f, burnSeconds - delta);
    }

    /**
     * Takes a piece of fuel out of the slot and lights it.
     *
     * @return {@code true} when a flame started
     */
    private boolean light() {
        if (water.isEmpty() || steam.isFull()) {
            return false;
        }
        ItemStack fuel = inventory().get(FUEL);
        if (fuel.isEmpty()) {
            return false;
        }
        float seconds = Fuels.secondsOf(fuel.item());
        if (seconds <= 0.0f) {
            return false;
        }
        fuel.setCount(fuel.count() - 1);
        burnTotal = seconds;
        burnSeconds = seconds;
        return true;
    }

    /**
     * Turns the water of one stretch of burning into steam.
     * <p>
     * How much water that is follows from the time the flame burned this frame, and only whole units are
     * moved: the rest is kept in {@link #waterDebt}, so a slow frame loses nothing. What the tanks allow
     * limits the amount as well - water that is not there cannot be boiled and steam that does not fit
     * cannot be made, which keeps the tanks from being written past their edge.
     *
     * @param burning seconds the flame burned this frame
     */
    private void boil(float burning) {
        waterDebt += burning * WATER_PER_SECOND;
        int earned = (int) waterDebt;
        if (earned <= 0) {
            return;
        }
        int room = steam.capacity() - steam.amount();
        int water = Math.min(earned, Math.min(this.water.amount(), room / STEAM_PER_WATER));
        if (water <= 0) {
            return;
        }
        this.water.drain(water, false);
        steam.fill(Fluids.STEAM, water * STEAM_PER_WATER, false);
        waterDebt -= water;
    }

    /**
     * {@link MachineError#NO_WATER} while the boiler has no water to boil.
     * <p>
     * The icon of the machine screen is what tells a player why nothing happens although the fuel slot is
     * full, the same way the furnace reports the energy it is missing.
     */
    @Override
    public MachineError error() {
        return water.isEmpty() ? MachineError.NO_WATER : MachineError.NONE;
    }

    @Override
    public float fuelSeconds() {
        return burnSeconds;
    }

    /**
     * Share of the tank of steam that is full.
     * <p>
     * The boiler has no craft to report, so the arrow of its screen shows how much steam it holds: a
     * machine that a pipe feeds has to be read by its tanks.
     */
    @Override
    public float craftProgress() {
        return steam.capacity() <= 0 ? 0.0f : (float) steam.amount() / steam.capacity();
    }

    @Override
    public float burnProgress() {
        return burnTotal <= 0.0f ? 0.0f : Math.min(1.0f, burnSeconds / burnTotal);
    }

    /** {@code true} while a flame is burning, which is what lights the front of the block. */
    @Override
    public boolean isRunning() {
        return burnSeconds > 0.0f;
    }

    @Override
    protected void saveState(NbtCompound state) {
        state.putFloat(SaveTags.BURN_SECONDS, burnSeconds);
        state.putFloat(SaveTags.BURN_TOTAL, burnTotal);
        state.putFloat(SaveTags.WATER_DEBT, waterDebt);
    }

    @Override
    protected void loadState(NbtCompound state) {
        burnSeconds = state.getFloat(SaveTags.BURN_SECONDS, 0.0f);
        burnTotal = state.getFloat(SaveTags.BURN_TOTAL, 0.0f);
        waterDebt = state.getFloat(SaveTags.WATER_DEBT, 0.0f);
    }

    @Override
    public String toString() {
        return "SteamBoilerMachine(water " + water + ", steam " + steam + ", burning "
                + isRunning() + ")";
    }
}
