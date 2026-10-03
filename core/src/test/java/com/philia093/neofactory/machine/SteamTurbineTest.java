package com.philia093.neofactory.machine;

import com.philia093.neofactory.block.BlockFace;
import com.philia093.neofactory.cable.Voltage;
import com.philia093.neofactory.fluid.Fluids;
import com.philia093.neofactory.item.PlayerInventory;
import com.philia093.neofactory.support.TestRegistries;
import com.philia093.neofactory.util.nbt.NbtCompound;
import com.philia093.neofactory.world.TickClock;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the steam turbine, the first machine of the game that makes power.
 * <p>
 * The numbers of the three tiers are pinned here - the ampère of the tier a tick and the steam it costs, see
 * {@link TurbineTier} - and so are the three things a turbine needs before it turns: the steam of a tick in its
 * tank, room in its buffer and a fluid that really is steam. The last test of the class drives a whole line of
 * the world, because that is what the machine is for, see {@link TurbineLineTest}.
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
    void aTurbineWithoutSteamStandsStill() {
        SteamTurbineMachine turbine = new SteamTurbineMachine(TurbineTier.LV);

        turbine.tick(TickClock.TICK_SECONDS);

        // A generator reports nothing at all: an empty tank is no error of a machine that works on nothing but
        // a fluid, it simply stands still until a line fills it, see MachineError.
        assertEquals(MachineError.NONE, turbine.error());
        assertEquals(0, turbine.energy().amount());
        assertFalse(turbine.isRunning());

        // Water is no steam either: a turbine turns nothing but the steam of a boiler.
        turbine.steam().fill(Fluids.WATER, STEAM, false);
        turbine.tick(TickClock.TICK_SECONDS);

        assertEquals(MachineError.NONE, turbine.error());
        assertEquals(STEAM, turbine.steam().amount(), "and nothing was drunk");
    }

    @Test
    void aGeneratorHasNoVentAndNoWayIn() {
        // A generator is no machine of recipes: the steam it drinks becomes the power of its line and nothing
        // is left of it to blow out, so the machine has no vent at all and no side but the one its line hangs
        // on. It takes no power in either, so that side is the only one of its own, see FaceConfig.
        SteamTurbineMachine turbine = new SteamTurbineMachine(TurbineTier.LV);

        assertFalse(turbine.faces().blowsSteam(), "a generator blows no steam out");
        assertNull(turbine.faces().exhaust(), "so it has no vent side");
        assertNull(turbine.faces().energyIn(), "and no side to take power in through");
        assertFalse(turbine.faces().takesPower());
        assertEquals(BlockFace.WEST, turbine.faces().faceOfTank(0),
                "the steam of a machine that looks north runs in on the right flank");
        assertEquals(BlockFace.EAST, turbine.faces().energyOut(),
                "the power leaves through the left flank");
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
        assertFalse(buffer.canReceive(), "a generator hands power over and takes none");
        assertFalse(turbine.faces().takesPower(), "so it has no plug to take power in through");
        assertNull(turbine.faces().energyIn(), "and no side of it carries one");
        assertFalse(turbine.faces().blowsSteam(), "and no vent either: the steam it drank is spent");
        assertNull(turbine.faces().exhaust());
    }

    @Test
    void theScreenOfAGeneratorHoldsNoSlotAndNoBar() {
        // A generator is no machine that works on an item: its screen holds no slot at all, so no stack may be
        // put into it, and no bar either - the steam it drinks stands where the bar of a machine stands, and
        // the power it holds is read on the cell of energy at the foot of the panel, see ProgressKind#NONE.
        SteamTurbineMachine turbine = new SteamTurbineMachine(TurbineTier.LV);
        MachineMenu menu = new MachineMenu(turbine, new PlayerInventory());

        assertEquals(0, turbine.inventory().size(), "a turbine reads no item");
        assertEquals(ProgressKind.NONE, menu.progressKind());
        assertFalse(menu.progressKind().hasBar(), "there is no craft to fill a bar towards");
        assertEquals(SlotKind.BATTERY, menu.energySlotKind(),
                "so what a player reads its power on is the cell of the electricity");
        assertEquals(1, menu.fluidSlots().size(), "and the one tank of the panel is its steam");
        assertEquals(MachineMenu.ARROW_Y, menu.fluidSlots().get(0).y(),
                "the steam stands on the row the bar of a machine stands on");
        assertEquals(PlayerInventory.SLOT_COUNT, menu.container().layout().slots().size(),
                "the panel holds the slots of the player and none of the machine");

        // The box of the cell of energy names the power and where it leaves the machine, and it names no plug
        // to take power in, because a generator has none.
        assertEquals(List.of(MachineMenu.ENERGY, "0 / " + TurbineTier.LV.capacity() + " "
                        + MachineMenu.ENERGY_UNIT, MachineMenu.POWER_OUT_PREFIX + MachineSides.LEFT),
                menu.energyTooltip(true));
    }

    @Test
    void aTurbineTravelsWithTheWorld() {
        SteamTurbineMachine turbine = new SteamTurbineMachine(TurbineTier.MV);
        turbine.steam().fill(Fluids.STEAM, STEAM, false);
        turbine.tick(TickClock.TICK_SECONDS);
        NbtCompound data = new NbtCompound("");
        turbine.save(data);

        SteamTurbineMachine loaded = new SteamTurbineMachine(TurbineTier.MV);
        loaded.load(data);

        assertEquals(STEAM - TurbineTier.MV.steamPerTick(), loaded.steam().amount(),
                "the steam of the tank is the one the save game was written with");
        assertEquals(turbine.energy().amount(), loaded.energy().amount(), "and so is the power it made");
        assertEquals(TurbineTier.MV.steamPerTick(), loaded.steamPerTick(), "and it keeps its tier");
    }
}
