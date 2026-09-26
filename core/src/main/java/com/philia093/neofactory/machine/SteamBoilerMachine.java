package com.philia093.neofactory.machine;

import com.philia093.neofactory.fluid.Fluids;
import com.philia093.neofactory.fluid.SimpleFluidStorage;
import com.philia093.neofactory.item.ItemStack;
import com.philia093.neofactory.util.nbt.NbtCompound;
import com.philia093.neofactory.world.save.SaveTags;

import java.util.List;

/**
 * The bronze boiler, the machine that turns water into steam with a fire under it.
 * <p>
 * It is the first machine of the industry and the one the machines of the bronze age are built around: a
 * fire heats water until the water becomes steam, and the steam is what those machines run on, see
 * {@link MachineTank}. The boiler holds one slot each way and two tanks:
 * <ul>
 *     <li>the fuel slot, which burns what {@link Fuels} says burns - a piece of coal keeps a boiler going
 *         for eighty seconds;</li>
 *     <li>the slot of what the fire leaves behind - the ash of a boiler, which is not part of the game
 *         yet, so the slot stands empty;</li>
 *     <li>the tank of water and the tank of steam, which take no slot at all: a player pours a cell of
 *         water into the first and fills an empty cell from the second by clicking the tank, see
 *         {@link com.philia093.neofactory.item.CellTransfer}.</li>
 * </ul>
 * <p>
 * <b>The boiler is read by its temperature.</b> It starts at the standard temperature of the game,
 * {@link #STANDARD_TEMPERATURE} - 25 degrees Celsius - and a flame adds {@link #HEAT_PER_SECOND} kelvin a
 * second to it while no flame takes {@link #COOL_PER_SECOND} away, so a boiler with fuel in it climbs and a
 * boiler without one falls back. At {@link #BOILING_TEMPERATURE} - 100 degrees Celsius - the water starts to
 * boil, and from then on <b>the water carries the heat away</b>: a boiler that boils holds the temperature
 * of boiling water and makes {@code WATER_PER_SECOND * STEAM_PER_WATER} units of steam a second no matter
 * what it burns. The fuel therefore decides how long a boiler stays hot, not how fast it boils.
 * <p>
 * <b>A boiler that cannot boil gets hotter.</b> One whose tank of water ran dry - or whose tank of steam is
 * full and has nowhere to put more - keeps climbing at {@link #HEAT_PER_SECOND} a second while the fire
 * lasts, and at {@link #RUIN_TEMPERATURE} (200 degrees Celsius) it is destroyed together with everything it
 * holds, see {@link Machine#isExploded()}. A boiler that boiled dry while it was hot is marked as well:
 * water that reaches it while it is still that hot cracks the metal, so filling a red hot boiler by hand is
 * as dangerous as leaving it alone. A marked boiler that had time to cool below the boiling point takes
 * water again. Watching the tank of water is what a boiler asks of a player.
 */
public final class SteamBoilerMachine extends Machine implements StatusMachine, ProgressMachine {

    /** Slot that holds what keeps the boiler burning. */
    public static final int FUEL = 0;

    /** Slot the fire leaves its ash in, unused while the game has no ash. */
    public static final int OUTPUT = 1;

    /** Standard temperature of the game, 25 degrees Celsius. */
    public static final float STANDARD_TEMPERATURE = 298.0f;

    /** Temperature water starts to boil at, 100 degrees Celsius. */
    public static final float BOILING_TEMPERATURE = 373.0f;

    /** Temperature a boiler is ruined at, 200 degrees Celsius. */
    public static final float RUIN_TEMPERATURE = 473.0f;

    /** Heat a second of burning adds to the boiler, in kelvin. */
    public static final float HEAT_PER_SECOND = 10.0f;

    /** Heat a second without a flame takes away, in kelvin. */
    public static final float COOL_PER_SECOND = 5.0f;

    /** Water a second of boiling turns into steam, in units of the game. */
    public static final float WATER_PER_SECOND = 20.0f;

    /** Units of steam one unit of water becomes. */
    public static final int STEAM_PER_WATER = 16;

    /** Amount of water the tank of the boiler holds, sixteen cells. */
    public static final int WATER_CAPACITY = 16_000;

    /** Amount of steam the tank of the boiler holds, sixteen cells. */
    public static final int STEAM_CAPACITY = 16_000;

    /**
     * Screen of the boiler: the fuel on the left, the slot of the ash on the right, and the two tanks at the
     * foot of the panel - the water it takes in and the steam it makes.
     */
    public static final MachineScreen SCREEN = new MachineScreen("Bronze Boiler",
            ProgressKind.GENERIC, List.of(SlotKind.SMELTING),
            List.of(SlotKind.GENERIC), 1, 1, false);

    private final SimpleFluidStorage water;
    private final SimpleFluidStorage steam;

    /** Temperature of the boiler, in kelvin. */
    private float temperature = STANDARD_TEMPERATURE;

    private float burnSeconds;
    private float burnTotal;

    /** Water that was earned but is not a whole unit yet, always below one. */
    private float waterDebt;

    /** {@code true} once the boiler boiled dry while it was hot. */
    private boolean scorched;

    /** Creates an empty boiler. */
    public SteamBoilerMachine() {
        super(SCREEN, new MachineInventory(MachineInventory.Role.FUEL, MachineInventory.Role.OUTPUT),
                new SimpleEnergyStorage(0), List.of(),
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

    /** Temperature of the boiler, in kelvin. */
    public float temperature() {
        return temperature;
    }

    @Override
    protected void update(float delta) {
        if (burnSeconds <= 0.0f) {
            // A piece of fuel is taken whenever the flame is out, whether or not there is water: a boiler
            // that runs dry is exactly the boiler that gets too hot, see checkForRuin.
            light();
        }
        adjustTemperature(delta);
        checkForRuin();
        if (isExploded()) {
            return;
        }
        boil(delta);
    }

    /**
     * Takes a piece of fuel out of the slot and lights it.
     *
     * @return {@code true} when a flame started
     */
    private boolean light() {
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
     * Adds the heat of one frame or takes it away.
     * <p>
     * <b>Water that boils carries the heat away.</b> A boiler that is boiling holds the temperature of
     * boiling water, so a boiler with a tank of water runs as long as it is fed and never gets hotter than
     * that. Only a boiler that cannot boil - because the tank of water is empty or because its steam has
     * nowhere to go - keeps climbing, and that is the boiler that reaches {@link #RUIN_TEMPERATURE}. The part
     * of a frame that reaches past the end of the flame is a frame without one, so a boiler whose fuel runs
     * out in the middle of a frame only gets the heat the flame was still able to give.
     *
     * @param delta time since the last frame in seconds
     */
    private void adjustTemperature(float delta) {
        if (burnSeconds <= 0.0f) {
            cool(delta);
            return;
        }
        float burning = Math.min(delta, burnSeconds);
        burnSeconds -= burning;
        if (isBoiling()) {
            temperature = Math.max(BOILING_TEMPERATURE, temperature);
        } else {
            temperature += burning * HEAT_PER_SECOND;
        }
        float rest = delta - burning;
        if (rest > 0.0f) {
            cool(rest);
        }
    }

    /**
     * {@code true} while the boiler is turning water into steam right now.
     * <p>
     * The water has to be there and the steam has to have somewhere to go: a boiler whose tank of steam is
     * full has no more use for heat than one that ran dry, so both of them climb.
     *
     * @return {@code true} while the boiler boils
     */
    private boolean isBoiling() {
        return temperature >= BOILING_TEMPERATURE && !water.isEmpty()
                && steam.capacity() - steam.amount() >= STEAM_PER_WATER;
    }

    /** Lets the boiler lose heat, down to the temperature of the room. */
    private void cool(float delta) {
        temperature = Math.max(STANDARD_TEMPERATURE, temperature - delta * COOL_PER_SECOND);
    }

    /**
     * Ruins the boiler when it was pushed past what it can take.
     * <p>
     * Three things follow from the same rule:
     * <ul>
     *     <li>a boiler that got hotter than {@link #RUIN_TEMPERATURE} goes at once;</li>
     *     <li>a boiler that boils dry while it is hot is marked - it ran with nothing to carry the heat
     *         away;</li>
     *     <li>water that reaches a marked boiler while it is still hot cracks the metal, which makes filling
     *         a red hot boiler by hand as dangerous as leaving it alone.</li>
     * </ul>
     * A marked boiler that cooled below the boiling point in the meantime held, so the water is harmless
     * and the mark goes with the boiling that caused it.
     */
    private void checkForRuin() {
        if (temperature >= RUIN_TEMPERATURE) {
            explode();
            return;
        }
        if (water.isEmpty() && temperature >= BOILING_TEMPERATURE) {
            scorched = true;
            return;
        }
        if (scorched) {
            // Water that reaches a marked boiler while it is still hot cracks the metal.
            if (temperature >= BOILING_TEMPERATURE) {
                explode();
                return;
            }
            // It cooled down in the meantime, so the metal held and the mark goes with it.
            scorched = false;
        }
    }

    /**
     * Turns water into steam while the boiler is hot enough to boil.
     * <p>
     * The rate is a constant of the boiler and not of what it burns: a second of boiling takes
     * {@link #WATER_PER_SECOND} units of water and makes {@code WATER_PER_SECOND * STEAM_PER_WATER} units of
     * steam. Only whole units are moved and the rest is kept in {@link #waterDebt}, so a slow frame loses
     * nothing; the water that is there and the room the steam has limit the amount, which keeps a tank from
     * being written past its edge.
     *
     * @param delta time since the last frame in seconds
     */
    private void boil(float delta) {
        if (temperature < BOILING_TEMPERATURE || water.isEmpty()) {
            // Cold water does not boil and dry heat makes no steam, so no work is owed either.
            waterDebt = 0.0f;
            return;
        }
        if (steam.capacity() - steam.amount() < STEAM_PER_WATER) {
            // Not even one unit of water fits as steam, so the boiler waits without owing work.
            waterDebt = 0.0f;
            return;
        }
        waterDebt += delta * WATER_PER_SECOND;
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
     * Temperature of the boiler, which is the number a player has to watch, see {@link StatusMachine}.
     *
     * @return the line {@code "Temp: 373K"} for a boiling boiler
     */
    @Override
    public String statusText() {
        return "Temp: " + Math.round(temperature) + "K";
    }

    /**
     * {@link MachineError#NO_WATER} while the boiler is hot enough to boil and has nothing to boil.
     * <p>
     * The icon of the machine screen is what tells a player why the tank of steam stays empty although the
     * fire burns.
     */
    @Override
    public MachineError error() {
        return water.isEmpty() && temperature >= BOILING_TEMPERATURE
                ? MachineError.NO_WATER : MachineError.NONE;
    }

    /**
     * Share of the current piece of fuel that has burned, which is what the bar of the boiler fills with.
     * <p>
     * The bar grows from the left as the piece burns and is full when the piece is used up, so it reads the
     * same way a furnace reads - the temperature of the boiler stands in the corner of the panel instead,
     * see {@link #statusText()}.
     */
    @Override
    public float craftProgress() {
        if (burnTotal <= 0.0f) {
            return 0.0f;
        }
        return Math.min(1.0f, Math.max(0.0f, (burnTotal - burnSeconds) / burnTotal));
    }

    /** The same number as {@link #craftProgress()}, the way a furnace reports its flame. */
    @Override
    public float burnProgress() {
        return craftProgress();
    }

    /** {@code true} while a flame is burning, which is what lights the front of the block. */
    @Override
    public boolean isRunning() {
        return burnSeconds > 0.0f;
    }

    @Override
    protected void saveState(NbtCompound state) {
        state.putFloat(SaveTags.TEMPERATURE, temperature);
        state.putFloat(SaveTags.BURN_SECONDS, burnSeconds);
        state.putFloat(SaveTags.BURN_TOTAL, burnTotal);
        state.putFloat(SaveTags.WATER_DEBT, waterDebt);
        state.putBoolean(SaveTags.SCORCHED, scorched);
    }

    @Override
    protected void loadState(NbtCompound state) {
        temperature = state.getFloat(SaveTags.TEMPERATURE, STANDARD_TEMPERATURE);
        burnSeconds = state.getFloat(SaveTags.BURN_SECONDS, 0.0f);
        burnTotal = state.getFloat(SaveTags.BURN_TOTAL, 0.0f);
        waterDebt = state.getFloat(SaveTags.WATER_DEBT, 0.0f);
        scorched = state.getBoolean(SaveTags.SCORCHED, false);
    }

    @Override
    public String toString() {
        return "SteamBoilerMachine(water " + water + ", steam " + steam + ", temp " + temperature
                + "K, burning " + isRunning() + ")";
    }
}
