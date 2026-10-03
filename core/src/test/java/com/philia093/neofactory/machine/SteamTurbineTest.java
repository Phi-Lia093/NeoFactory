package com.philia093.neofactory.machine;

import com.philia093.neofactory.block.BlockFace;
import com.philia093.neofactory.cable.Voltage;
import com.philia093.neofactory.fluid.Fluids;
import com.philia093.neofactory.support.TestRegistries;
import com.philia093.neofactory.util.nbt.NbtCompound;
import com.philia093.neofactory.world.TickClock;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the steam turbine, the first machine of the game that makes power.
 * <p>
 * The numbers of the three tiers are pinned here - the ampère of the tier a tick and the steam it costs, see
 * {@link TurbineTier} - and so are the four things a turbine needs before it turns: a free vent, the steam of
 * a tick in its tank, room in its buffer and a fluid that really is steam. The last test of the class drives a
 * whole line of the world, because that is what the machine is for, see {@link TurbineLineTest}.
 */
class SteamTurbineTest {

    /** Capacity of the tank of the turbine, the size every machine of the age of steam holds. */
    private static final int STEAM = SteamMachine.STEAM_CAPACITY;

    @BeforeAll
    static void registerGameData() {
        TestRegistries.ensure();
    }

    @Test
    void aTurbineHandsAnAmpereOfItsTierOverATick() {
        assertEquals(76, TurbineTier.LV.steamPerTick(), "the steam of a tick of the low turbine");
        assertEquals(342, TurbineTier.MV.steamPerTick());
        assertEquals(1552, TurbineTier.HV.steamPerTick());

        for (TurbineTier tier : TurbineTier.values()) {
            assertEquals(tier.voltage().euPerTick(), tier.euPerTick(),
                    "the energy of a tick is one ampère of the tier of the machine, see " + tier);
            assertTrue(tier.euPerTick() * TurbineTier.STEAM_PER_EU <= tier.steamPerTick() * tier.efficiency(),
                    "the steam of a tick is worth the energy it makes, the efficiency counted in");
        }
    }

    @Test
    void aTurbineTurnsSteamIntoPower() {
        SteamTurbineMachine turbine = new SteamTurbineMachine(TurbineTier.LV);
        turbine.steam().fill(Fluids.STEAM, STEAM, false);

        turbine.tick(TickClock.TICK_SECONDS);

        assertEquals(TurbineTier.LV.euPerTick(), turbine.energy().amount(),
                "the buffer of the machine holds the ampère of the tick");
        assertEquals(STEAM - TurbineTier.LV.steamPerTick(), turbine.steam().amount(),
                "and the steam of the tick is gone");
        assertTrue(turbine.isRunning(), "the machine is turning");
        assertEquals(MachineError.NONE, turbine.error());
    }

    @Test
    void aTurbineThatNobodyTakesFromStandsStill() {
        SteamTurbineMachine turbine = new SteamTurbineMachine(TurbineTier.LV);
        turbine.steam().fill(Fluids.STEAM, STEAM, false);
        MachineEnergyStorage buffer = (MachineEnergyStorage) turbine.energy();
        // A buffer is filled one ampère at a time, so the test fills it to the brim by hand, the way a save
        // game that is read back does.
        buffer.setAmount(buffer.capacity());
        assertTrue(buffer.isFull(), "the buffer of the machine is full");

        turbine.tick(TickClock.TICK_SECONDS);

        assertEquals(STEAM, turbine.steam().amount(),
                "nothing is drunk that nobody wants: the tank is as full as it was");
        assertFalse(turbine.isRunning());
        assertEquals(MachineError.NONE, turbine.error(), "a full buffer is no error");
    }

    @Test
    void aTurbineWithoutSteamSaysSo() {
        SteamTurbineMachine turbine = new SteamTurbineMachine(TurbineTier.LV);

        turbine.tick(TickClock.TICK_SECONDS);

        assertEquals(MachineError.NO_STEAM, turbine.error());
        assertEquals(0, turbine.energy().amount());
        assertFalse(turbine.isRunning());

        // Water is no steam either: a turbine turns nothing but the steam of a boiler.
        turbine.steam().fill(Fluids.WATER, STEAM, false);
        turbine.tick(TickClock.TICK_SECONDS);

        assertEquals(MachineError.NO_STEAM, turbine.error());
        assertEquals(STEAM, turbine.steam().amount(), "and nothing was drunk");
    }

    @Test
    void aTurbineThatCannotBlowItsSteamOutWaits() {
        SteamTurbineMachine turbine = new SteamTurbineMachine(TurbineTier.LV);
        turbine.steam().fill(Fluids.STEAM, STEAM, false);
        turbine.reportExhaust(true);

        turbine.tick(TickClock.TICK_SECONDS);

        assertEquals(STEAM, turbine.steam().amount(), "the machine waits with the steam it has");
        assertEquals(0, turbine.energy().amount());
        assertEquals(MachineError.NO_EXHAUST, turbine.error());
        assertTrue(turbine.isWaitingForExhaust(), "so the block keeps looking at the vent");
        assertFalse(turbine.takesAnExhaustCheck(), "a machine that stands still blew nothing out");

        turbine.reportExhaust(false);
        turbine.tick(TickClock.TICK_SECONDS);

        assertTrue(turbine.isRunning(), "and it starts again the moment the way out is open");
    }

    @Test
    void theVentOfATurbineIsASideOfItsBlock() {
        SteamTurbineMachine turbine = new SteamTurbineMachine(TurbineTier.LV);

        assertTrue(turbine.faces().blowsSteam(), "a turbine has a vent");
        assertEquals(BlockFace.WEST, turbine.faces().faceOfTank(0),
                "the steam of a machine that looks north runs in on the right flank");
        assertEquals(BlockFace.EAST, turbine.faces().energyOut(),
                "the power leaves through the left flank");
        assertEquals(BlockFace.SOUTH, turbine.faces().exhaust(), "and the vent blows out of the back");
    }

    @Test
    void theBufferOfATurbineIsOfItsTier() {
        SteamTurbineMachine turbine = new SteamTurbineMachine(TurbineTier.HV);
        MachineEnergyStorage buffer = (MachineEnergyStorage) turbine.energy();

        assertEquals(Voltage.HIGH, buffer.accepted(), "a line of the high voltage is the one it runs on");
        assertTrue(buffer.accepts(Voltage.LOW), "a worse line feeds it as well");
        assertFalse(buffer.accepts(Voltage.EXTREME), "a better one destroys it");
        assertEquals(TurbineTier.HV.capacity(), buffer.capacity(),
                "the buffer holds what the table of the tiers names");
        assertEquals(TurbineTier.HV.casing(), turbine.casing(),
                "and the machine is built of the casing of its age");
    }

    @Test
    void aTurbineTravelsWithTheWorld() {
        SteamTurbineMachine turbine = new SteamTurbineMachine(TurbineTier.MV);
        turbine.reportExhaust(true);
        NbtCompound data = new NbtCompound("");
        turbine.save(data);

        SteamTurbineMachine loaded = new SteamTurbineMachine(TurbineTier.MV);
        loaded.load(data);

        assertTrue(loaded.isWaitingForExhaust(), "the machine waits for its vent after a save game as before");
        assertEquals(TurbineTier.MV.steamPerTick(), loaded.steamPerTick(), "and keeps its tier");
    }
}
