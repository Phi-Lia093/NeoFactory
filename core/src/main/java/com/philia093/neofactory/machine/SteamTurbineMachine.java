package com.philia093.neofactory.machine;

import com.philia093.neofactory.fluid.Fluids;
import com.philia093.neofactory.fluid.SimpleFluidStorage;

import java.util.List;

/**
 * The steam turbine: the machine that turns the steam of a boiler into the power of a line of cables.
 * <p>
 * This is the first machine of the game that <b>makes</b> energy, and the first one a line of cables hangs on
 * as a source: a tank of steam a line of pipes fills, a buffer of the tier it was built for, and one plug the
 * power leaves by. Every tick it drinks the steam of its tier, fills its buffer with the energy that steam is
 * worth and drives the line at that plug, see {@code MachineBlockEntity#updateEnergy} and {@link TurbineTier}
 * for the numbers of the three of them.
 * <p>
 * <b>The turbine is the pump of its line.</b> A cable carries nothing of its own, so the machine that makes
 * the power is the one that moves it: what it hands its buffer is picked up by the entity of the block and
 * walked along the line that stands at the side a player gave to its plug. <b>Nothing is drunk that nobody
 * wants</b>: a buffer that has no room for another ampère stops the machine instead of boiling the steam of
 * the tank away, which is what lets a player run a turbine and a bank of machines on the same line without
 * watching it.
 * <p>
 * <b>The steam is spent, not blown out.</b> A turbine takes the steam in and gives power back, so there is
 * nothing left of it to blow out of a vent: the steam of its tank is gone the moment its energy stands in the
 * buffer of the machine. That is why a generator has no side of its own for steam to leave by - one plug and
 * nothing else - and why the interface of the vent, {@link ExhaustMachine}, is left to the machines of the age
 * of steam that work away a recipe at a time and really do blow their spent steam into the world.
 * <p>
 * <b>Every tick is the same tick.</b> A turbine has no craft to run and no item to work on, so it turns at a
 * fixed rate whatever the frame took and hands over a whole ampère of its tier a tick. <b>A machine that makes
 * power is no machine that works on an item</b>: the screen of a turbine holds no slot at all - there is no
 * cell a player could put a stack into - it shows no bar, because there is no craft to fill one towards, and
 * the steam it drinks stands on the row the bar of a machine stands on, see {@link ProgressKind#NONE} and
 * {@code MachineMenu}.
 */
public class SteamTurbineMachine extends Machine {

    private final TurbineTier tier;

    private final SimpleFluidStorage steam;

    /** {@code true} while this machine made energy in the tick that just ran. */
    private boolean running;

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
                // A turbine reads no item at all: its screen holds no slot, so there is no cell a player could
                // put a stack into, and what it needs is the steam a line of pipes fills its tank with.
                new MachineInventory(),
                // A generator hands power over and takes none: its buffer may be emptied and never filled, so
                // the machine has one plug - the one its line hangs on - and no other side of its own, see
                // MachineEnergyStorage and FaceConfig.
                new MachineEnergyStorage(tier.capacity(), 0, tier.euPerTick(), tier.voltage()),
                List.of(),
                new MachineTank(tank, MachineTank.Role.INPUT));
        this.tier = tier;
        this.steam = tank;
        // No vent is given to a generator: the steam it drinks becomes the power of its line and nothing is
        // left to blow out, so its only side of its own is the plug of the power, see FaceConfig.
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
        // The panel of every machine that makes power is the grey one of the age of electricity, whatever the
        // casing of the machine is built of: what a player reads the age of a turbine off is its block, see
        // TurbineTier#casing. A generator has no bar either - it works by the tick and has no progress to
        // show - and the steam it drinks stands where the bar of a machine stands, see MachineMenu.
        return new MachineScreen(tier.displayName(), MachineStyle.NORMAL, ProgressKind.NONE,
                List.of(), List.of(), 1, 0, false);
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
     * The machine turns while three things hold at once: the tank holds the steam of a tick, the buffer has
     * room for the ampère it makes, and the fluid in the tank really is steam. Every one of them that fails
     * leaves the machine standing still with the steam it has, which is what makes a turbine a machine a player
     * may build before the line it feeds exists.
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
        int wanted = tier.steamPerTick();
        if (steam.fluid() != Fluids.STEAM || steam.amount() < wanted) {
            return;
        }
        MachineEnergyStorage buffer = (MachineEnergyStorage) energy();
        if (buffer.capacity() - buffer.amount() < tier.euPerTick()) {
            // Nobody is taking the power of this machine at the moment: a turbine stands still instead of
            // boiling the steam of its tank away, see MachineBlockEntity#updateEnergy.
            return;
        }
        if (steam.drain(wanted, false) < wanted) {
            return;
        }
        buffer.make(tier.euPerTick());
        running = true;
    }

    @Override
    public MachineError error() {
        // A generator reports nothing: a machine that makes power is not a machine that works on an item, so
        // an empty tank is no error of its own - it simply stands still until a line of pipes fills it. A
        // turbine has no vent either, so no side of it can ever be blocked, see MachineError.
        return MachineError.NONE;
    }

    @Override
    public String toString() {
        return "SteamTurbineMachine(" + name() + ", steam " + steam + ", "
                + (running ? "turning" : "still") + ")";
    }
}
