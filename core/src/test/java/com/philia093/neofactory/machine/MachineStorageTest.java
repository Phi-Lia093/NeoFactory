package com.philia093.neofactory.machine;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the two contracts a machine is built on: energy and fluid.
 * <p>
 * Both are plain arithmetic on numbers, so the limits, the simulated calls and the
 * refusal of a second fluid are checked here, without a window and without a world.
 */
class MachineStorageTest {

    @Test
    void aBufferRespectsItsCapacity() {
        SimpleEnergyStorage buffer = new SimpleEnergyStorage(100);

        assertEquals(0, buffer.amount());
        assertEquals(100, buffer.capacity());
        assertTrue(buffer.isEmpty());
        assertFalse(buffer.isFull());

        assertEquals(40, buffer.receive(40, false));
        assertEquals(40, buffer.amount());
        assertEquals(60, buffer.receive(80, false), "only what fits was taken");
        assertTrue(buffer.isFull());
        assertEquals(0, buffer.receive(10, false), "a full buffer takes nothing");
    }

    @Test
    void aSimulatedCallChangesNothing() {
        SimpleEnergyStorage buffer = new SimpleEnergyStorage(100);
        buffer.setAmount(50);

        assertEquals(30, buffer.receive(30, true), "the offer would be taken");
        assertEquals(50, buffer.amount(), "but nothing happened");

        assertEquals(20, buffer.extract(20, true), "the request would be filled");
        assertEquals(50, buffer.amount(), "and again nothing happened");

        assertEquals(20, buffer.extract(20, false));
        assertEquals(30, buffer.amount());
    }

    @Test
    void theLimitsOfABufferAreRespected() {
        SimpleEnergyStorage buffer = new SimpleEnergyStorage(100, 10, 5);

        assertEquals(10, buffer.receive(50, false), "never more than the input limit");
        assertEquals(5, buffer.extract(50, false), "never more than the output limit");
        assertTrue(buffer.canReceive());
        assertTrue(buffer.canExtract());
    }

    @Test
    void aTankNeverMixesTwoFluids() {
        SimpleFluidStorage tank = new SimpleFluidStorage(100);

        assertTrue(tank.isEmpty());
        assertEquals(30, tank.fill(FluidType.WATER, 30, false));
        assertEquals(FluidType.WATER, tank.fluid());
        assertEquals(30, tank.amount());

        assertEquals(0, tank.fill(FluidType.LAVA, 10, false), "lava does not enter a water tank");
        assertEquals(30, tank.amount());
        assertEquals(70, tank.fill(FluidType.WATER, 90, false), "only what fits was taken");
        assertTrue(tank.isFull());
    }

    @Test
    void aTankGivesItsFluidBack() {
        SimpleFluidStorage tank = new SimpleFluidStorage(50);
        tank.fill(FluidType.LAVA, 20, false);

        assertEquals(20, tank.drain(30, true), "the request would be filled");
        assertEquals(20, tank.amount());

        assertEquals(20, tank.drain(30, false));
        assertEquals(0, tank.amount());
        assertEquals(null, tank.fluid(), "an empty tank holds no kind of fluid");
        assertTrue(tank.isEmpty());
    }

    @Test
    void aStorageRefusesNonsenseValues() {
        assertThrows(IllegalArgumentException.class, () -> new SimpleEnergyStorage(-1));
        assertThrows(IllegalArgumentException.class, () -> new SimpleFluidStorage(-1));

        SimpleFluidStorage tank = new SimpleFluidStorage(10);
        assertEquals(0, tank.fill(null, 5, false), "no fluid was offered");
        assertEquals(0, tank.fill(FluidType.WATER, 0, false), "nothing was offered");
        assertEquals(0, tank.drain(0, false), "nothing was wanted");
    }

    @Test
    void theFluidsOfTheGameHaveNames() {
        assertEquals(FluidType.WATER, FluidType.byName("water"));
        assertEquals(FluidType.LAVA, FluidType.byName("lava"));
        assertEquals(null, FluidType.byName("oil"));
        assertEquals(null, FluidType.byName(null));
        assertEquals(2, FluidType.all().size());
    }
}
