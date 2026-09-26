package com.philia093.neofactory.pipe;

import com.philia093.neofactory.fluid.Fluids;
import com.philia093.neofactory.pipe.Pipes.Pipe;
import com.philia093.neofactory.support.TestRegistries;
import com.philia093.neofactory.world.TickClock;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the arithmetic of the transport: what one tick of a rate moves, what a neighbour weighs, how a
 * junction divides what it has and which fluid bursts which pipe.
 * <p>
 * The rules are checked without a world, because that is what {@link PipeTransport} is: numbers that the
 * entity of a pipe carries out. What the entity really does with them is checked next door, see
 * {@code PipeFlowTest}.
 */
class PipeTransportTest {

    @BeforeAll
    static void registerGameData() {
        TestRegistries.ensure();
    }

    /** The pipes of the tests, read after the tables of the game were built. */
    private static Pipe pipe(PipeMaterial material, PipeSize size) {
        return Pipes.of(material, size);
    }

    @Test
    void aTickOfARateIsAShareOfASecond() {
        assertEquals(TickClock.TICKS_PER_SECOND, PipeTransport.TICKS_PER_SECOND,
                "the transport runs on the ticks of the world");
        assertEquals(20, PipeTransport.TICKS_PER_SECOND);
        assertEquals(3, PipeTransport.perTick(60), "a copper tiny pipe moves sixty a second");
        assertEquals(20, PipeTransport.perTick(400));
        assertEquals(960, PipeTransport.perTick(19200));
        assertEquals(1, PipeTransport.perTick(1), "a rate below a tick still moves a millibucket");
        assertEquals(1, PipeTransport.perTick(0), "and so does a rate of nothing at all");
        assertEquals(400.0f, PipeTransport.inTime(400, 1.0f), 0.001f);
        assertEquals(20.0f, PipeTransport.inTime(400, TickClock.TICK_SECONDS), 0.001f);
        assertEquals(0.0f, PipeTransport.inTime(400, -1.0f), 0.001f, "time never runs backwards");
    }

    @Test
    void aNeighbourIsWeighedByWhatItMovesASecond() {
        assertEquals(PipeMaterials.COPPER.flow(PipeSize.MEDIUM),
                PipeTransport.weightOf(pipe(PipeMaterials.COPPER, PipeSize.MEDIUM)));
        assertTrue(PipeTransport.weightOf(pipe(PipeMaterials.STEEL, PipeSize.HUGE))
                > PipeTransport.weightOf(pipe(PipeMaterials.COPPER, PipeSize.TINY)),
                "a wide pipe draws more than a narrow one");
        assertEquals(1, PipeTransport.weightOfMachine(0),
                "a machine always draws a share, however slowly the line runs");
        assertEquals(PipeMaterials.COPPER.flow(PipeSize.MEDIUM),
                PipeTransport.weightOfMachine(PipeMaterials.COPPER.flow(PipeSize.MEDIUM)),
                "a machine draws what the pipe that reaches it moves");
    }

    @Test
    void aMachineBesideAPipeIsHandedAShareOfItsOwn() {
        // The bug this covers: a pipe that both pours into a machine and carries on down the line handed the
        // machine a weight of one against the rate of the branch, which rounded to nothing - so the machine of
        // the middle pipe of a line of three stayed dry while the machines at its two ends were served.
        int rate = PipeMaterials.COPPER.flow(PipeSize.MEDIUM);
        int[] shares = new int[2];
        PipeTransport.split(20,
                new int[] { PipeTransport.weightOfMachine(rate), PipeTransport.weightOf(pipe(
                        PipeMaterials.COPPER, PipeSize.MEDIUM)) },
                shares);

        assertEquals(10, shares[0], "the machine takes half of what the pipe offers");
        assertEquals(10, shares[1], "and the branch behind it the other half");
    }

    @Test
    void aJunctionDividesWhatItHasTheWayItsNeighboursStandToEachOther() {
        int[] shares = new int[2];

        // The narrow pipe behind a junction takes a quarter of the fluid and the wide one three quarters of
        // it, because that is how their rates stand to each other.
        PipeTransport.split(1000, new int[] { 100, 300 }, shares);
        assertEquals(250, shares[0]);
        assertEquals(750, shares[1]);

        // Every millibucket that is handed out is handed out once: the shares add up to the amount and the
        // odd one left over by rounding goes to the last neighbour.
        PipeTransport.split(999, new int[] { 100, 300 }, shares);
        assertEquals(250, shares[0]);
        assertEquals(749, shares[1]);
        int[] three = new int[3];
        PipeTransport.split(1000, new int[] { 1, 1, 1 }, three);
        assertEquals(1000, three[0] + three[1] + three[2]);
        assertTrue(three[2] >= three[0], "the last neighbour takes what rounding leaves over");
    }

    @Test
    void aNeighbourWhoseWeightIsNothingTakesNothing() {
        int[] shares = new int[3];
        PipeTransport.split(100, new int[] { 0, 300, 0 }, shares);
        assertEquals(0, shares[0]);
        assertEquals(100, shares[1]);
        assertEquals(0, shares[2]);

        PipeTransport.split(100, new int[] { 400, 400 }, shares);
        assertEquals(50, shares[0], "two pipes of the same rate share a junction equally");

        PipeTransport.split(0, new int[] { 100, 300 }, shares);
        assertEquals(0, shares[0], "an empty pipe hands out nothing");
        assertEquals(0, shares[1]);

        PipeTransport.split(100, new int[] { 400 }, shares);
        assertEquals(100, shares[0], "a single neighbour takes all of it");
        assertEquals(0, shares[1], "and a share without a neighbour stays empty");
    }

    @Test
    void nothingRunsUphill() {
        assertEquals(0.0f, PipeTransport.level(0, 400), 0.001f);
        assertEquals(1.0f, PipeTransport.level(400, 400), 0.001f);
        assertEquals(0.25f, PipeTransport.level(100, 400), 0.001f);
        assertEquals(1.0f, PipeTransport.level(10, 0), 0.001f, "a tube without a capacity is full or empty");
        assertEquals(0.0f, PipeTransport.level(0, 0), 0.001f);

        // Pressure is what a pipe is filled with and not what stands in it: a small pipe that is half full
        // pushes into a huge one that holds a drop, even though the huge one holds more of it.
        assertTrue(PipeTransport.flowsTo(200, 400, 10, 19200));
        assertFalse(PipeTransport.flowsTo(10, 19200, 200, 400));
        assertFalse(PipeTransport.flowsTo(200, 400, 200, 400), "a line under one pressure stands still");
        assertFalse(PipeTransport.flowsTo(400, 400, 400, 400));
        assertTrue(PipeTransport.flowsTo(400, 400, 0, 400), "a full pipe fills an empty one");
        assertFalse(PipeTransport.flowsTo(0, 400, 0, 400), "an empty line stays where it is");
    }

    @Test
    void aFluidHotterThanTheMaterialBurstsThePipe() {
        Pipe wooden = pipe(PipeMaterials.WOOD, PipeSize.MEDIUM);
        Pipe copper = pipe(PipeMaterials.COPPER, PipeSize.MEDIUM);
        Pipe bronze = pipe(PipeMaterials.BRONZE, PipeSize.MEDIUM);

        assertTrue(PipeTransport.bursts(wooden, Fluids.STEAM), "steam bursts a wooden pipe");
        assertFalse(PipeTransport.bursts(copper, Fluids.STEAM), "and runs in a copper one");
        assertTrue(PipeTransport.bursts(copper, Fluids.LAVA), "lava bursts a copper pipe");
        assertFalse(PipeTransport.bursts(bronze, Fluids.LAVA), "and runs in a bronze one");
        assertFalse(PipeTransport.bursts(wooden, Fluids.WATER), "water bursts nothing");
        assertFalse(PipeTransport.bursts(copper, null), "an empty pipe bursts from nothing");
        assertFalse(PipeTransport.bursts(null, Fluids.LAVA), "and neither does a cell without a pipe");
    }
}
