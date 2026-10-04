package com.philia093.neofactory.energy;

import com.philia093.neofactory.cable.CableKind;
import com.philia093.neofactory.cable.CableMaterials;
import com.philia093.neofactory.cable.CableSize;
import com.philia093.neofactory.cable.Cables;
import com.philia093.neofactory.cable.Voltage;
import com.philia093.neofactory.machine.EnergyStorage;
import com.philia093.neofactory.machine.MachineEnergyStorage;
import com.philia093.neofactory.machine.SimpleEnergyStorage;
import com.philia093.neofactory.support.TestRegistries;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks one line of the power: what it carries, what it loses and what it moves between two storages.
 * <p>
 * A line is the worst of its cables end to end and a machine is fed what is left after every block of the
 * run took its share, so what is checked here is that rule and the two ends of it: a line of one kind of
 * cable carries what the table of the cables says, a line of two kinds carries what the worse one allows,
 * and nothing is ever taken out of a source to be lost on the way.
 */
class EnergyNetTest {

    @BeforeAll
    static void registerGameData() {
        TestRegistries.ensure();
    }

    @Test
    void theWeakestCableRulesTheLine() {
        Cables.Cable copper = Cables.of(CableMaterials.COPPER, CableSize.SINGLE, CableKind.WIRE);
        Cables.Cable annealed = Cables.of(CableMaterials.ANNEALED_COPPER, CableSize.SIXTEEN,
                CableKind.CABLE);
        EnergyNet line = EnergyNet.of(List.of(copper, annealed));

        assertEquals(Voltage.MEDIUM, line.voltage(), "both are lines of the middle voltage");
        assertEquals(copper.amperage(), line.amperage(), "the lower amperage of the two");
        assertEquals(copper.loss(), line.loss(), "the higher loss of the two: the bare copper line");
        assertEquals(2, line.blocks(), "two blocks of cable");
        assertEquals(2 * copper.loss(), line.totalLoss());
    }

    @Test
    void aLineCarriesWhatItsVoltageAndCurrentComeTo() {
        EnergyNet single = EnergyNet.of(CableMaterials.COPPER, CableSize.SINGLE, CableKind.WIRE, 1);
        EnergyNet wide = EnergyNet.of(CableMaterials.COPPER, CableSize.SIXTEEN, CableKind.WIRE, 1);

        assertEquals(Voltage.MEDIUM, single.voltage());
        assertEquals(1, single.amperage());
        assertEquals(128, single.capacity(), "one ampere of the middle voltage");
        assertEquals(16, wide.amperage());
        assertEquals(2048, wide.capacity(), "the sixteen fold width");
        assertTrue(single.feeds(Voltage.MEDIUM));
        assertFalse(single.feeds(Voltage.HIGH), "a line of the middle voltage never feeds a better one");
        assertTrue(wide.feeds(Voltage.MEDIUM));
    }

    @Test
    void aSuperconductorCarriesItWithoutALoss() {
        EnergyNet line = EnergyNet.of(CableMaterials.MV_SUPERCONDUCTOR, CableSize.SINGLE, CableKind.WIRE,
                10);
        assertTrue(line.isLossless(), "a line of a superconductor loses nothing");
        assertEquals(0, line.totalLoss());
        assertEquals(4, line.amperage());

        SimpleEnergyStorage source = new SimpleEnergyStorage(1000, 1000, 1000);
        SimpleEnergyStorage sink = new SimpleEnergyStorage(1000);
        source.setAmount(500);
        assertEquals(64, line.carry(source, sink, 64), "what the sink asked for arrives");
        assertEquals(436, source.amount(), "and nothing was kept on the way");
        assertEquals(64, sink.amount());
    }

    @Test
    void theLossOfARunIsPaidByTheSource() {
        EnergyNet line = EnergyNet.of(CableMaterials.COPPER, CableSize.SINGLE, CableKind.WIRE, 3);
        assertEquals(4, line.loss());
        assertEquals(12, line.totalLoss(), "three blocks of a copper wire");

        SimpleEnergyStorage source = new SimpleEnergyStorage(1000, 1000, 1000);
        SimpleEnergyStorage sink = new SimpleEnergyStorage(1000);
        source.setAmount(500);
        assertEquals(64, line.carry(source, sink, 64));
        assertEquals(64, sink.amount(), "sixty four units arrive");
        assertEquals(500 - 64 - 12, source.amount(), "and the source paid the loss of the run");
    }

    @Test
    void aWrappedLineTakesHalfOfTheLossAway() {
        EnergyNet bare = EnergyNet.of(CableMaterials.COPPER, CableSize.SINGLE, CableKind.WIRE, 2);
        EnergyNet wrapped = EnergyNet.of(CableMaterials.COPPER, CableSize.SINGLE, CableKind.CABLE, 2);
        assertEquals(8, bare.totalLoss());
        assertEquals(4, wrapped.totalLoss(), "a skin takes half of it");
        assertTrue(EnergyNet.of(CableMaterials.RED_ALLOY, CableSize.SINGLE, CableKind.CABLE, 8)
                .isLossless(), "half of one unit a block rounds down to nothing");
    }

    @Test
    void oneTickCarriesNoMoreThanTheLineAllows() {
        EnergyNet line = EnergyNet.of(CableMaterials.COPPER, CableSize.SINGLE, CableKind.WIRE, 1);
        SimpleEnergyStorage source = new SimpleEnergyStorage(100000, 100000, 100000);
        SimpleEnergyStorage sink = new SimpleEnergyStorage(100000);
        source.setAmount(100000);
        assertEquals(line.capacity(), line.carry(source, sink, 100000),
                "a tick carries the capacity of the line and no more");
    }

    @Test
    void nothingMovesWhenAnEndIsClosed() {
        EnergyNet line = EnergyNet.of(CableMaterials.COPPER, CableSize.SINGLE, CableKind.WIRE, 1);
        SimpleEnergyStorage empty = new SimpleEnergyStorage(100);
        SimpleEnergyStorage full = new SimpleEnergyStorage(64);
        full.setAmount(64);
        // A storage that takes energy in and gives none out is a machine that only ever fills up.
        SimpleEnergyStorage mouth = new SimpleEnergyStorage(100, 100, 0);
        mouth.setAmount(100);

        assertEquals(0, line.carry(empty, new SimpleEnergyStorage(100), 32), "an empty source");
        assertEquals(0, line.carry(mouth, new SimpleEnergyStorage(100), 32), "a source that may not give");
        assertEquals(0, line.carry(new SimpleEnergyStorage(100, 100, 100), full, 32), "a full sink");
        assertEquals(0, line.carry(new SimpleEnergyStorage(100, 100, 100), new SimpleEnergyStorage(100), 0),
                "nothing was asked for");
        assertEquals(64, full.amount(), "and nothing was lost on the way");
    }

    @Test
    void twoMachinesSideBySideHandItOverWithoutALine() {
        SimpleEnergyStorage source = new SimpleEnergyStorage(1000, 1000, 1000);
        SimpleEnergyStorage sink = new SimpleEnergyStorage(1000);
        source.setAmount(1000);
        assertEquals(500, EnergyNet.hand(source, sink, 500),
                "no cable carries as much as the two machines allow");
        assertEquals(500, source.amount());
        assertEquals(500, sink.amount());
    }

    @Test
    void whatFeedsTwoMachinesThatStandSideBySideIsTheTierOfTheOneThatGives() {
        MachineEnergyStorage high = new MachineEnergyStorage(1000, Voltage.HIGH);
        MachineEnergyStorage low = new MachineEnergyStorage(1000, Voltage.LOW);

        assertTrue(EnergyNet.overvolts(high, low),
                "a machine beside a buffer of the high voltage is fed by a line of the high voltage");
        assertFalse(EnergyNet.overvolts(low, low), "the tier of its own line feeds it");
        assertFalse(EnergyNet.overvolts(low, high), "and a better machine takes a worse line");
        assertFalse(EnergyNet.overvolts(new SimpleEnergyStorage(1000), low),
                "a buffer that names no tier is no line at all");
        assertFalse(EnergyNet.overvolts(high, new SimpleEnergyStorage(1000)),
                "and neither is a machine that names none destroyed by one");
        MachineEnergyStorage working = new MachineEnergyStorage(1000, 100, 0, Voltage.HIGH);
        assertFalse(EnergyNet.overvolts(working, low),
                "a machine that only works gives nothing away and destroys nothing beside it");

        assertThrows(NullPointerException.class, () -> EnergyNet.overvolts((EnergyStorage) null, low));
        assertThrows(NullPointerException.class, () -> EnergyNet.overvolts(high, null));
    }

    @Test
    void aLineOfATierTakesTheMachineThatWasBuiltForAWorseOne() {
        MachineEnergyStorage high = new MachineEnergyStorage(1000, Voltage.HIGH);
        MachineEnergyStorage low = new MachineEnergyStorage(1000, Voltage.LOW);

        assertTrue(EnergyNet.overvolts(Voltage.HIGH, low), "a line of the high voltage takes a machine of low");
        assertFalse(EnergyNet.overvolts(Voltage.LOW, low), "a line of its own tier feeds it");
        assertFalse(EnergyNet.overvolts(Voltage.LOW, high), "and a better machine takes a worse line");
        assertFalse(EnergyNet.overvolts(Voltage.HIGH, new SimpleEnergyStorage(1000)),
                "a buffer that names no tier is no machine of the industry");

        assertThrows(NullPointerException.class, () -> EnergyNet.overvolts((Voltage) null, low));
        assertThrows(NullPointerException.class, () -> EnergyNet.overvolts(Voltage.HIGH, null));
    }

    @Test
    void aLineNeedsACable() {
        assertThrows(IllegalArgumentException.class, () -> EnergyNet.of(List.of()));
        assertThrows(IllegalArgumentException.class,
                () -> EnergyNet.of(CableMaterials.COPPER, CableSize.SINGLE, CableKind.WIRE, -1));
        assertThrows(NullPointerException.class, () -> EnergyNet.hand(null, null, 1));
    }
}
