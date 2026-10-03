package com.philia093.neofactory.machine;

import com.philia093.neofactory.fluid.Fluids;
import com.philia093.neofactory.fluid.SimpleFluidStorage;
import com.philia093.neofactory.util.nbt.NbtCompound;
import com.philia093.neofactory.world.save.SaveTags;

import java.util.List;

/**
 * The steam turbine: the machine that turns the steam of a boiler into the power of a line of cables.
 * <p>
 * This is the first machine of the game that <b>makes</b> energy, and the first one a line of cables hangs on
 * as a source: a tank of steam a line of pipes fills, a buffer of the tier it was built for, and a vent the
 * spent steam goes out of. Every tick it drinks the steam of its tier, fills its buffer with the energy that
 * steam is worth and drives the line at its plug, see {@code MachineBlockEntity#updateEnergy} and
 * {@link TurbineTier} for the numbers of the three of them.
 * <p>
 * <b>The turbine is the pump of its line.</b> A cable carries nothing of its own, so the machine that makes
 * the power is the one that moves it: what it hands its buffer is picked up by the entity of the block and
 * walked along the line that stands at the side a player gave to its plug. <b>Nothing is drunk that nobody
 * wants</b>: a buffer that has no room for another ampère stops the machine instead of boiling the steam of
 * the tank away, which is what lets a player run a turbine and a bank of machines on the same line without
 * watching it.
 * <p>
 * <b>The vent has to be free.</b> A turbine blows steam out every tick that it turns, so it asks the block to
 * look at the vent of that tick and refuses to turn while a wall stands there, the way a machine of recipes
 * asks once a craft, see {@link ExhaustMachine}. A turbine that is waiting starts again by itself the moment
 * the way is open.
 * <p>
 * <b>Every tick is the same tick.</b> A turbine has no craft to run and no item to work on, so it turns at a
 * fixed rate whatever the frame took and hands over a whole ampère of its tier a tick; the slot it holds is
 * the one the rotor of a later age will sit in, which is why the screen of a turbine draws one slot and one
 * tank of steam.
 */
public class SteamTurbineMachine extends Machine implements ExhaustMachine {

    private final TurbineTier tier;

    private final SimpleFluidStorage steam;

    /** {@code true} while this machine blew steam out and made energy in the tick that just ran. */
    private boolean running;

    /** {@code true} while the next tick waits for the vent of this machine to be free. */
    private boolean exhaustBlocked;

    /**
     * Creates a turbine of a tier.
     *
     * @param tier tier the machine was built for, never {@code null}
     */
    public SteamTurbineMachine(TurbineTier tier) {
        this(tier, screen(tier), new SimpleFluidStorage(SteamMachine.STEAM_CAPACITY));
    }

    /**
     * Creates a turbine of a tier with a tank of its own.
     *
     * @param tier tier the machine was built for, never {@code null}
     * @param screen how this machine is shown
     * @param tank tank that holds the steam it drinks
     */
    protected SteamTurbineMachine(TurbineTier tier, MachineScreen screen, SimpleFluidStorage tank) {
        super(screen,
                // A turbine reads no item: the one slot it holds is the one a rotor will sit in, see the
                // javadoc of the class.
                new MachineInventory(MachineInventory.Role.INPUT),
                new MachineEnergyStorage(tier.capacity(), tier.voltage()),
                List.of(),
                new MachineTank(tank, MachineTank.Role.INPUT));
        this.tier = tier;
        this.steam = tank;
        // A turbine blows its steam out, so it has a vent like every machine of its age, see FaceConfig.
        faces().withExhaust();
    }

    /**
     * Screen of a turbine of a tier.
     * <p>
     * The name is the one a player reads in the inventory, the panel is the age the casing of the machine
     * belongs to, and the one tank at the foot of the panel is the steam it drinks.
     *
     * @param tier tier the machine was built for
     * @return the screen of that turbine
     */
    public static MachineScreen screen(TurbineTier tier) {
        return new MachineScreen(tier.displayName(), tier.style(), ProgressKind.BRONZE,
                List.of(SlotKind.GENERIC), List.of(), 1, 0, false);
    }

    /** Tier this machine was built for, which is what its numbers come from. */
    public TurbineTier tier() {
        return tier;
    }

    /** Steam this machine drinks a tick, in millibuckets. */
    public int steamPerTick() {
        return tier.steamPerTick();
    }

    /** Energy this machine hands to its buffer a tick, one ampère of its tier. */
    public int euPerTick() {
        return tier.euPerTick();
    }

    /** Tank of steam this machine drinks, the one a line of pipes fills. */
    public SimpleFluidStorage steam() {
        return steam;
    }

    @Override
    public String casing() {
        return tier.casing();
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    /**
     * Does one tick of a turbine.
     * <p>
     * The machine turns while four things hold at once: the vent is free, the tank holds the steam of a tick,
     * the buffer has room for the ampère it makes, and the fluid in the tank really is steam. Every one of them
     * that fails leaves the machine standing still with the steam it has, which is what makes a turbine a
     * machine a player may build before the line it feeds exists.
     * <p>
     * <b>The frame is not counted.</b> A turbine works by the tick and not by the second: the world ticks
     * twenty times a second whatever the frames do, so the steam of a tick and the energy of a tick are what
     * this machine is built around, see {@code World#tick}.
     *
     * @param delta time since the last frame in seconds, which a turbine does not read
     */
    @Override
    protected void update(float delta) {
        running = false;
        if (exhaustBlocked) {
            return;
        }
        int wanted = tier.steamPerTick();
        if (steam.fluid() != Fluids.STEAM || steam.amount() < wanted) {
            return;
        }
        EnergyStorage buffer = energy();
        if (buffer.capacity() - buffer.amount() < tier.euPerTick()) {
            // Nobody is taking the power of this machine at the moment: a turbine stands still instead of
            // boiling the steam of its tank away, see MachineBlockEntity#updateEnergy.
            return;
        }
        if (steam.drain(wanted, false) < wanted) {
            return;
        }
        buffer.receive(tier.euPerTick(), false);
        running = true;
    }

    @Override
    public MachineError error() {
        if (exhaustBlocked) {
            return MachineError.NO_EXHAUST;
        }
        if (steam.fluid() != Fluids.STEAM || steam.amount() < tier.steamPerTick()) {
            return MachineError.NO_STEAM;
        }
        return MachineError.NONE;
    }

    @Override
    public boolean isWaitingForExhaust() {
        return exhaustBlocked;
    }

    @Override
    public void reportExhaust(boolean blocked) {
        exhaustBlocked = blocked;
    }

    /**
     * {@code true} while this machine has to be looked at, which is after every tick that it turned.
     * <p>
     * A turbine blows steam out of its vent every tick it works and not once a craft, so it asks for the look
     * of the tick it just ran; a machine that is waiting keeps asking, so the way out is looked at until it is
     * free again, see {@link ExhaustMachine}.
     */
    @Override
    public boolean takesAnExhaustCheck() {
        return running;
    }

    @Override
    protected void saveState(NbtCompound state) {
        state.putBoolean(SaveTags.EXHAUST_BLOCKED, exhaustBlocked);
    }

    @Override
    protected void loadState(NbtCompound state) {
        exhaustBlocked = state.getBoolean(SaveTags.EXHAUST_BLOCKED, false);
    }

    @Override
    public String toString() {
        return "SteamTurbineMachine(" + name() + ", steam " + steam + ", "
                + (running ? "turning" : "still") + (exhaustBlocked ? ", exhaust blocked" : "") + ")";
    }
}
