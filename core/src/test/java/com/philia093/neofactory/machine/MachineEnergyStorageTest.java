package com.philia093.neofactory.machine;

import com.philia093.neofactory.cable.Voltage;
import com.philia093.neofactory.support.TestRegistries;
import com.philia093.neofactory.util.nbt.NbtCompound;
import com.philia093.neofactory.world.save.SaveTags;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the buffer of a machine of the power network.
 * <p>
 * The buffer is what a line of cables is measured against: it takes one ampere of the tier its machine was
 * built for and no more however wide the line at it is, it survives a line of its own tier and of every worse
 * one, and it is destroyed together with that line by a better one, see {@link MachineEnergyStorage} and
 * {@link com.philia093.neofactory.energy.EnergyAcceptor}. What it holds travels with the machine like the rest
 * of it, which is the part {@code Machine#restoreEnergy} has to know about.
 */
class MachineEnergyStorageTest {

    /** Capacity of the buffers of these tests, which is several ticks of work. */
    private static final int CAPACITY = 4000;

    @BeforeAll
    static void registerGameData() {
        TestRegistries.ensure();
    }

    @Test
    void aMachineTakesOneAmpereOfItsOwnTier() {
        MachineEnergyStorage machine = new MachineEnergyStorage(CAPACITY, Voltage.LOW);

        assertEquals(Voltage.LOW, machine.accepted());
        assertEquals(32, machine.receive(CAPACITY, false),
                "one ampere of the low voltage is what fits in a tick, whatever is offered");
        assertEquals(32, machine.amount());
        assertEquals(32, machine.receive(CAPACITY, false), "and the tick after that as well");
        assertEquals(64, machine.amount());

        MachineEnergyStorage ulv = new MachineEnergyStorage(CAPACITY, Voltage.ULTRA_LOW);
        assertEquals(8, ulv.receive(CAPACITY, false), "a machine of the first tier takes eight units a tick");
        assertTrue(machine.canReceive());
        assertTrue(machine.canExtract(), "a machine of the power network feeds a line from the same buffer");
        assertFalse(machine.isFull());
    }

    @Test
    void aMachineTakesItsOwnTierAndEveryLineBelowIt() {
        MachineEnergyStorage machine = new MachineEnergyStorage(CAPACITY, Voltage.HIGH);

        assertTrue(machine.accepts(Voltage.HIGH), "the line it was built for");
        assertTrue(machine.accepts(Voltage.LOW), "a worse line feeds a better machine");
        assertTrue(machine.accepts(Voltage.ULTRA_LOW));
        assertFalse(machine.accepts(Voltage.EXTREME), "a better line destroys it");
        assertFalse(machine.accepts(Voltage.LUDICROUS));
    }

    @Test
    void aBufferOfItsOwnLimitsTakesWhatItWasBuiltFor() {
        // A machine that takes energy slower than its tier carries - a machine of later ages that charges in
        // steps - names the limits of its own buffer, see MachineEnergyStorage.
        MachineEnergyStorage slow = new MachineEnergyStorage(CAPACITY, 4, 2, Voltage.HIGH);

        assertEquals(4, slow.receive(CAPACITY, false), "the buffer takes what its machine asks for");
        assertEquals(2, slow.extract(CAPACITY, false));
        assertEquals(2, slow.amount());
        assertEquals(Voltage.HIGH, slow.accepted(), "and it is still a machine of its own tier");
    }

    @Test
    void aBufferThatWasFilledTravelsWithTheMachine() {
        // The limits of a buffer say how fast it is filled and not how full it may be after a save game was
        // read, so a machine that is stored full is read back full, see Machine#restoreEnergy.
        TestMachine before = new TestMachine(Voltage.MEDIUM);
        before.buffer().setAmount(CAPACITY);
        NbtCompound data = new NbtCompound("");
        before.save(data);

        TestMachine after = new TestMachine(Voltage.MEDIUM);
        after.load(data);

        assertEquals(CAPACITY, after.buffer().amount(), "the buffer was stored and read back full");
        assertEquals(Voltage.MEDIUM, after.buffer().accepted(), "and the machine keeps the tier it was built for");
    }

    @Test
    void aMachineStoredWithMoreThanFitsTakesWhatFits() {
        // A world that gave a machine a larger buffer keeps the machine instead of failing to read it.
        TestMachine machine = new TestMachine(Voltage.LOW);
        NbtCompound data = new NbtCompound("");
        machine.save(data);
        data.putInt(SaveTags.ENERGY, CAPACITY * 2);

        machine.load(data);

        assertEquals(CAPACITY, machine.buffer().amount(), "the amount is clamped to the capacity");
    }

    /** A machine of the power network of a tier, with one slot that nothing is ever put into. */
    private static final class TestMachine extends Machine {

        private TestMachine(Voltage tier) {
            super(new MachineScreen("Test", ProgressKind.GENERIC, List.of(SlotKind.SMELTING), List.of(), 0, 0,
                            false),
                    new MachineInventory(MachineInventory.Role.INPUT),
                    new MachineEnergyStorage(CAPACITY, tier), List.of());
        }

        @Override
        protected void update(float delta) {
            // A machine of the test does no work of its own.
        }

        /** Buffer of this machine, as the machine was built with it. */
        private MachineEnergyStorage buffer() {
            return (MachineEnergyStorage) energy();
        }
    }
}
