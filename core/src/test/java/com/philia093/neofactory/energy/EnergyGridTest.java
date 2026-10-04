package com.philia093.neofactory.energy;

import com.philia093.neofactory.block.Block;
import com.philia093.neofactory.block.BlockFace;
import com.philia093.neofactory.block.Blocks;
import com.philia093.neofactory.cable.CableKind;
import com.philia093.neofactory.cable.CableMaterial;
import com.philia093.neofactory.cable.CableMaterials;
import com.philia093.neofactory.cable.CableSize;
import com.philia093.neofactory.cable.Cables;
import com.philia093.neofactory.cable.Voltage;
import com.philia093.neofactory.machine.EnergyStorage;
import com.philia093.neofactory.machine.SimpleEnergyStorage;
import com.philia093.neofactory.support.TestRegistries;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the net of a world: which cables a walk reaches, which machines hang on the line and what happens
 * when a line is too much for a machine.
 * <p>
 * The cells are a map of the test, so the whole walk runs without a world, see {@link EnergyGrid.Cells}. A
 * cable of the map is a real block of the game with a real mask, because the mask is the whole of what a walk
 * follows.
 */
class EnergyGridTest {

    /** A machine of the test: a buffer with a tier the line has to respect. */
    private static final class Machine implements EnergyAcceptor {

        private final Voltage tier;
        private final SimpleEnergyStorage inner;

        private Machine(Voltage tier, int capacity) {
            this.tier = tier;
            this.inner = new SimpleEnergyStorage(capacity, capacity, capacity);
        }

        @Override
        public Voltage accepted() {
            return tier;
        }

        @Override
        public int amount() {
            return inner.amount();
        }

        @Override
        public int capacity() {
            return inner.capacity();
        }

        @Override
        public int receive(int maxReceive, boolean simulate) {
            return inner.receive(maxReceive, simulate);
        }

        @Override
        public int extract(int maxExtract, boolean simulate) {
            return inner.extract(maxExtract, simulate);
        }

        @Override
        public boolean canReceive() {
            return inner.canReceive();
        }

        @Override
        public boolean canExtract() {
            return inner.canExtract();
        }

        /** Fills the buffer of this machine, the way a generator of the world would. */
        private void fill(int amount) {
            inner.setAmount(amount);
        }
    }

    /**
     * A block of the test that carries a line through it, the way a diode of the game does.
     * <p>
     * A diode is one piece of a line that lets the power run one way: a line is walked by the machine that
     * asks for it and it walks towards whoever gives it, so a line that is entered from the side the power
     * leaves this block by is handed on through the side it enters by, and a line that arrives the other way
     * is refused, see {@link LineNode#through(BlockFace)}.
     *
     * @param voltage tier this piece may carry
     * @param amperage current this piece carries
     * @param in side the power of this block enters it by
     * @param out side the power of this block leaves it by
     */
    private record Gate(Voltage voltage, int amperage, BlockFace in, BlockFace out) implements LineNode {

        @Override
        public BlockFace through(BlockFace from) {
            return from == out ? in : null;
        }

        @Override
        public int loss() {
            // A diode of the game is a superconductor: it carries what it is given and loses nothing of it.
            return 0;
        }
    }

    /** The cells of the test: a map of blocks, of masks and of the machines that hang on them. */
    private static final class Cells implements EnergyGrid.Cells {

        private final Map<String, Block> blocks = new HashMap<>();
        private final Map<String, Integer> states = new HashMap<>();
        private final Map<String, EnergyStorage> buffers = new HashMap<>();
        private final Map<String, List<BlockFace>> plugs = new HashMap<>();
        private final Map<String, LineNode> nodes = new HashMap<>();
        private final List<String> removed = new ArrayList<>();

        private void cable(int x, int y, int z, int mask) {
            cable(x, y, z, CableMaterials.COPPER, CableSize.SINGLE, mask);
        }

        /** Places one cable of a material, joined to the sides its mask names. */
        private void cable(int x, int y, int z, CableMaterial material, int mask) {
            cable(x, y, z, material, CableSize.SINGLE, mask);
        }

        /** Places one cable of a material and a width, joined to the sides its mask names. */
        private void cable(int x, int y, int z, CableMaterial material, CableSize size, int mask) {
            blocks.put(at(x, y, z), Cables.of(material, size, CableKind.CABLE).block());
            states.put(at(x, y, z), mask);
        }

        /**
         * Puts a block that carries a line through a cell, the way a diode does.
         *
         * @param x x of the cell
         * @param y y of the cell
         * @param z z of the cell
         * @param node the block a line runs through
         */
        private void node(int x, int y, int z, LineNode node) {
            blocks.put(at(x, y, z), Blocks.STONE);
            nodes.put(at(x, y, z), node);
        }

        /**
         * Puts a machine of the test in a cell.
         *
         * @param x x of the cell
         * @param y y of the cell
         * @param z z of the cell
         * @param storage buffer of the machine
         * @param plugSides sides of the machine its power is reached through, none for a machine that carries
         *        no plug at all
         */
        private void machine(int x, int y, int z, EnergyStorage storage, BlockFace... plugSides) {
            blocks.put(at(x, y, z), Blocks.STONE);
            buffers.put(at(x, y, z), storage);
            plugs.put(at(x, y, z), List.of(plugSides));
        }

        @Override
        public Block block(int x, int y, int z) {
            return blocks.get(at(x, y, z));
        }

        @Override
        public int state(int x, int y, int z) {
            return states.getOrDefault(at(x, y, z), 0);
        }

        @Override
        public EnergyStorage buffer(int x, int y, int z, BlockFace from) {
            if (!plugs.getOrDefault(at(x, y, z), List.of()).contains(from)) {
                return null;
            }
            return buffers.get(at(x, y, z));
        }

        @Override
        public void remove(int x, int y, int z) {
            removed.add(at(x, y, z));
            blocks.remove(at(x, y, z));
            buffers.remove(at(x, y, z));
            plugs.remove(at(x, y, z));
            nodes.remove(at(x, y, z));
        }

        @Override
        public LineNode node(int x, int y, int z) {
            return nodes.get(at(x, y, z));
        }

        private static String at(int x, int y, int z) {
            return x + "," + y + "," + z;
        }
    }

    @BeforeAll
    static void registerGameData() {
        TestRegistries.ensure();
    }

    @Test
    void theWalkFollowsEveryJoinedSide() {
        Cells cells = new Cells();
        cells.cable(0, 64, 0, Cables.mask(BlockFace.SOUTH));
        cells.cable(0, 64, 1, Cables.mask(BlockFace.NORTH, BlockFace.SOUTH));
        cells.cable(0, 64, 2, Cables.mask(BlockFace.NORTH));

        EnergyGrid.Line line = EnergyGrid.line(cells, 0, 64, 1);
        assertEquals(3, line.length(), "three cables in a row");
        assertEquals(0, line.ends().size(), "no machine hangs on the line");
        assertNull(EnergyGrid.line(cells, 1, 64, 0), "no cable stands there");
        assertNull(EnergyGrid.line(cells, 0, 64, 3), "the walk stops where the line ends");
    }

    @Test
    void aMachineOnTheLineIsAnEndOfIt() {
        Cells cells = new Cells();
        Machine machine = new Machine(Voltage.HIGH, 1000);
        cells.cable(0, 64, 0, Cables.mask(BlockFace.EAST, BlockFace.WEST));
        cells.cable(1, 64, 0, Cables.mask(BlockFace.WEST, BlockFace.SOUTH, BlockFace.EAST));
        cells.cable(1, 64, 1, Cables.mask(BlockFace.NORTH));
        cells.machine(2, 64, 0, machine, BlockFace.WEST);

        EnergyGrid.Line line = EnergyGrid.line(cells, 0, 64, 0);
        assertEquals(3, line.length(), "the walk turned the corner");
        assertEquals(1, line.ends().size(), "the machine at the end of the line");
        assertEquals(machine, line.ends().get(0));
        assertTrue(line.reaches(machine));
        assertEquals(2, line.endPositions().get(0)[0], "at the cell it stands in");
        assertFalse(line.overvolts(machine), "the machine that gives it is built for the very tier it feeds");
        assertEquals(Voltage.MEDIUM, line.net().voltage());
    }

    @Test
    void aMachineWhosePlugFacesAwayIsNoEndOfTheLine() {
        Cells cells = new Cells();
        Machine machine = new Machine(Voltage.HIGH, 1000);
        machine.fill(1000);
        cells.cable(0, 64, 0, Cables.mask(BlockFace.EAST, BlockFace.WEST));
        // The cable reaches the machine from the west, but the power of the machine is reached through its
        // north side: a line hangs on the side a player set and not on every side a cable meets.
        cells.machine(1, 64, 0, machine, BlockFace.NORTH);

        EnergyGrid.Line line = EnergyGrid.line(cells, 0, 64, 0);

        assertTrue(line.ends().isEmpty(), "the machine is no end of that line");
        assertFalse(line.reaches(machine));
        assertEquals(0, line.pull(cells, machine, 100), "so nothing is handed over");
        assertTrue(cells.removed.isEmpty(), "and nothing burns");
        assertEquals(1000, machine.amount(), "the buffer of the machine is untouched");
    }

    @Test
    void aLineTooStrongForAMachineBurnsTheLineAndTheMachine() {
        Cells cells = new Cells();
        Machine small = new Machine(Voltage.LOW, 100);
        Machine big = new Machine(Voltage.HIGH, 100);
        // A machine of the high voltage that holds energy: a line is fed by the power that stands at its end,
        // see EnergyGrid.Line#liveVoltage.
        big.fill(100);
        cells.cable(0, 64, 0, Cables.mask(BlockFace.EAST, BlockFace.WEST));
        cells.machine(1, 64, 0, small, BlockFace.WEST);
        cells.machine(-1, 64, 0, big, BlockFace.EAST);

        EnergyGrid.Line line = EnergyGrid.line(cells, 0, 64, 0);
        assertEquals(Voltage.HIGH, line.liveVoltage(small),
                "what feeds the line is the machine that gives, and that one is of the high voltage");
        assertTrue(line.overvolts(small), "which is too much for a machine of low");
        assertFalse(line.overvolts(big), "while the machine that gives it takes its own tier");
        // The machine that asks and the run of cables are weighed apart: the machine was built for a worse
        // line than the one it reached for, and the copper cable of the run cannot take the high voltage of
        // the machine that gives either, so the two of them go, see EnergyGrid.Line#pull.
        assertEquals(0, line.pull(cells, small, 100), "nothing is handed over");
        assertTrue(cells.removed.contains("0,64,0"), "the cable, which cannot take what the line is fed with");
        assertTrue(cells.removed.contains("1,64,0"), "and the machine that asked, which was too small for it");
        assertFalse(cells.removed.contains("-1,64,0"), "the machine that could take it survives");
    }

    @Test
    void aMachineTooSmallForItsLineDoesNotTakeTheCablesWithIt() {
        Cells cells = new Cells();
        Machine small = new Machine(Voltage.LOW, 100);
        Machine giving = new Machine(Voltage.MEDIUM, 100);
        giving.fill(100);
        cells.cable(0, 64, 0, Cables.mask(BlockFace.EAST, BlockFace.WEST));
        cells.machine(1, 64, 0, small, BlockFace.WEST);
        cells.machine(-1, 64, 0, giving, BlockFace.EAST);

        EnergyGrid.Line line = EnergyGrid.line(cells, 0, 64, 0);

        assertEquals(0, line.pull(cells, small, 100), "nothing is handed over");
        assertTrue(cells.removed.contains("1,64,0"), "the machine the line was too much for is gone");
        assertFalse(cells.removed.contains("0,64,0"),
                "while the copper cable takes the middle voltage that feeds it and stays");
        assertFalse(cells.removed.contains("-1,64,0"), "and so does the machine that gave it the power");
    }

    @Test
    void aCableThatCannotTakeWhatItsLineIsFedWithMeltsWhereThePowerEntersIt() {
        Cells cells = new Cells();
        Machine machine = new Machine(Voltage.MEDIUM, 100);
        Machine giving = new Machine(Voltage.MEDIUM, 100);
        giving.fill(100);
        // A run of a tin cable at the machine that gives and a copper one at the far end: the tin cable is a
        // line of the low voltage and cannot take what a machine of the middle one feeds it, so the piece at
        // that machine goes while the rest of the run stands, see EnergyGrid.Line#melt.
        cells.cable(0, 64, 0, CableMaterials.TIN, Cables.mask(BlockFace.EAST, BlockFace.WEST));
        cells.cable(1, 64, 0, Cables.mask(BlockFace.WEST, BlockFace.EAST));
        cells.machine(2, 64, 0, machine, BlockFace.WEST);
        cells.machine(-1, 64, 0, giving, BlockFace.EAST);

        EnergyGrid.Line line = EnergyGrid.line(cells, 1, 64, 0);
        assertEquals(2, line.length(), "a copper cable between the machine that gives and a tin one");
        assertEquals(Voltage.MEDIUM, line.liveVoltage(machine), "the run is fed with the middle voltage");
        assertFalse(line.overvolts(machine), "which the machine at its far end was built for");

        assertEquals(0, line.pull(cells, machine, 100), "nothing is handed over while the run melts");
        assertTrue(cells.removed.contains("0,64,0"),
                "the tin cable, which cannot take the middle voltage, melted where the power enters the line");
        assertFalse(cells.removed.contains("1,64,0"), "the copper cable of the run can take it and stays");
        assertFalse(cells.removed.contains("2,64,0"), "and so does the machine that was built for it");
    }

    @Test
    void aLineThatFitsHandsTheEnergyOver() {
        Cells cells = new Cells();
        Machine source = new Machine(Voltage.MEDIUM, 1000);
        Machine sink = new Machine(Voltage.HIGH, 100);
        source.fill(1000);
        cells.cable(0, 64, 0, Cables.mask(BlockFace.EAST, BlockFace.WEST));
        cells.machine(1, 64, 0, sink, BlockFace.WEST);
        cells.machine(-1, 64, 0, source, BlockFace.EAST);

        EnergyGrid.Line line = EnergyGrid.line(cells, 0, 64, 0);
        assertEquals(100, line.pull(cells, sink, 100), "what the machine asked for arrives");
        assertEquals(100, sink.amount());
        assertEquals(1000 - 100 - line.net().totalLoss(), source.amount(),
                "and the source paid the loss of the line");
        assertTrue(cells.removed.isEmpty(), "nothing burned");
    }

    @Test
    void aMachineAsksALineOfAnEmptySourceForNothing() {
        Cells cells = new Cells();
        Machine sink = new Machine(Voltage.HIGH, 100);
        cells.cable(0, 64, 0, Cables.mask(BlockFace.EAST, BlockFace.WEST));
        cells.machine(1, 64, 0, sink, BlockFace.WEST);
        // The machine at the other end may give, but it holds nothing: a line nobody feeds leaves the machine
        // that asks waiting instead of handing it energy from nowhere. It is built for the middle voltage, so
        // the copper cable of the run takes what it would feed and nothing of the line is destroyed.
        cells.machine(-1, 64, 0, new Machine(Voltage.MEDIUM, 100), BlockFace.EAST);

        EnergyGrid.Line line = EnergyGrid.line(cells, 0, 64, 0);

        assertEquals(0, line.pull(cells, sink, 100), "an empty source gives nothing");
        assertEquals(0, sink.amount());
        assertTrue(cells.removed.isEmpty(), "and nothing burns");
    }

    @Test
    void aLineIsFedByWhatItsMachinesHoldAndNotByWhatTheyAreBuiltFor() {
        Cells cells = new Cells();
        Machine sink = new Machine(Voltage.LOW, 100);
        cells.cable(0, 64, 0, Cables.mask(BlockFace.EAST, BlockFace.WEST));
        cells.machine(1, 64, 0, sink, BlockFace.WEST);
        // The machine that gives holds nothing at all and is built for the high voltage: a line is fed with the
        // power that stands at its end and not with the age of the block there, so what the machine of low
        // voltage reached for is no line at all and nothing is destroyed by it, see EnergyGrid.Line#liveVoltage.
        cells.machine(-1, 64, 0, new Machine(Voltage.HIGH, 100), BlockFace.EAST);

        EnergyGrid.Line line = EnergyGrid.line(cells, 0, 64, 0);

        assertNull(line.liveVoltage(sink), "no machine at the end of it holds anything to give");
        assertEquals(0, line.pull(cells, sink, 100), "so nothing is handed over");
        assertTrue(cells.removed.isEmpty(),
                "and nothing burns: an empty machine of a later age is no line, however much its age is worth");
    }

    @Test
    void theSameLineBurnsTheMachineTheMomentItsMachinesHoldEnergy() {
        Cells cells = new Cells();
        Machine sink = new Machine(Voltage.LOW, 100);
        Machine big = new Machine(Voltage.HIGH, 100);
        big.fill(100);
        cells.cable(0, 64, 0, Cables.mask(BlockFace.EAST, BlockFace.WEST));
        cells.machine(1, 64, 0, sink, BlockFace.WEST);
        cells.machine(-1, 64, 0, big, BlockFace.EAST);

        EnergyGrid.Line line = EnergyGrid.line(cells, 0, 64, 0);

        assertEquals(Voltage.HIGH, line.liveVoltage(sink), "the machine that gives holds the high voltage");
        assertEquals(0, line.pull(cells, sink, 100), "nothing is handed over while the line is too strong");
        assertTrue(cells.removed.contains("1,64,0"), "the machine of low voltage was built for a worse line");
        assertFalse(cells.removed.contains("-1,64,0"), "while the machine that gives survives");
    }

    @Test
    void aDiodeHandsALineOnTheWayItsPowerRuns() {
        Cells cells = new Cells();
        Machine sink = new Machine(Voltage.MEDIUM, 100);
        Machine source = new Machine(Voltage.MEDIUM, 100);
        source.fill(100);
        // The gate lies in the line with its input towards the machine that gives and its output towards the
        // machine that works: a line is walked by the machine that asks, towards whoever gives it, so the
        // walk comes in from the output side and is handed on through the input, see EnergyGrid#line.
        cells.machine(-1, 64, 0, source, BlockFace.EAST);
        cells.node(0, 64, 0, new Gate(Voltage.MEDIUM, 1, BlockFace.WEST, BlockFace.EAST));
        cells.machine(1, 64, 0, sink, BlockFace.WEST);

        EnergyGrid.Line line = EnergyGrid.line(cells, 0, 64, 0, BlockFace.EAST);

        assertEquals(1, line.length(), "the gate is one piece of the line");
        assertTrue(line.reaches(sink), "and the machine that asks is one of its ends");
        assertTrue(line.reaches(source), "so is the machine that gives");
        assertEquals(100, line.pull(cells, sink, 100), "and what it asked for arrives");
        assertEquals(100, sink.amount());
        assertEquals(0, source.amount(), "paid out of the buffer of the machine that gives");
        assertTrue(cells.removed.isEmpty(), "and nothing burned");
    }

    @Test
    void aDiodeRefusesALineThatArrivesAgainstItsPower() {
        Cells cells = new Cells();
        Machine sink = new Machine(Voltage.MEDIUM, 100);
        Machine source = new Machine(Voltage.MEDIUM, 100);
        source.fill(100);
        cells.machine(-1, 64, 0, sink, BlockFace.EAST);
        cells.node(0, 64, 0, new Gate(Voltage.MEDIUM, 1, BlockFace.WEST, BlockFace.EAST));
        cells.machine(1, 64, 0, source, BlockFace.WEST);

        // The machine that asks stands at the input side of the gate, so the walk it sends arrives the way
        // the power of the gate does not run: there is no line for it to reach the machine that gives it.
        assertNull(EnergyGrid.line(cells, 0, 64, 0, BlockFace.WEST),
                "the walk is refused and no line is walked at all");
        assertEquals(0, sink.amount(), "so nothing arrives");
        assertTrue(cells.removed.isEmpty(), "and nothing burns");
    }

    @Test
    void aDiodeOfOneAmpereMakesTheLineALineOfOneAmpere() {
        Cells cells = new Cells();
        Machine sink = new Machine(Voltage.MEDIUM, 1000);
        Machine source = new Machine(Voltage.MEDIUM, 1000);
        source.fill(1000);
        // A cable of sixteen amperes and a gate of one in one line: what a line carries is what the worst of
        // its pieces allows, the way a narrow cable in a wide run is, see EnergyNet#of.
        cells.machine(-1, 64, 0, source, BlockFace.EAST);
        cells.node(0, 64, 0, new Gate(Voltage.MEDIUM, 1, BlockFace.WEST, BlockFace.EAST));
        cells.cable(1, 64, 0, CableMaterials.COPPER, CableSize.SIXTEEN,
                Cables.mask(BlockFace.WEST, BlockFace.EAST));
        cells.machine(2, 64, 0, sink, BlockFace.WEST);

        EnergyGrid.Line line = EnergyGrid.line(cells, 1, 64, 0, BlockFace.EAST);

        assertEquals(2, line.length(), "the gate and the cable are the two pieces of it");
        assertEquals(Voltage.MEDIUM, line.net().voltage());
        assertEquals(1, line.net().amperage(), "the gate carries one ampere and the cable sixteen");
        assertEquals(Voltage.MEDIUM.euPerTick(), line.pull(cells, sink, 1000),
                "so one tick of the line is one ampere of its tier, however wide the cable is");
        assertEquals(Voltage.MEDIUM.euPerTick(), sink.amount());
        assertEquals(1000 - Voltage.MEDIUM.euPerTick() - line.net().totalLoss(), source.amount(),
                "and the source paid the tick and the loss of the run");
    }

    @Test
    void aDiodeThatCannotTakeItsLineMeltsWhereItStands() {
        Cells cells = new Cells();
        Machine sink = new Machine(Voltage.HIGH, 100);
        Machine source = new Machine(Voltage.HIGH, 1000);
        source.fill(1000);
        // A gate of the low voltage in a line a machine of the high voltage feeds: what a line is fed with is
        // what every piece of it has to take, and the gate cannot take it, see EnergyGrid.Line#melt.
        cells.machine(-1, 64, 0, source, BlockFace.EAST);
        cells.cable(0, 64, 0, CableMaterials.SILVER, Cables.mask(BlockFace.WEST, BlockFace.EAST));
        cells.node(1, 64, 0, new Gate(Voltage.LOW, 16, BlockFace.WEST, BlockFace.EAST));
        cells.machine(2, 64, 0, sink, BlockFace.WEST);

        EnergyGrid.Line line = EnergyGrid.line(cells, 1, 64, 0, BlockFace.EAST);
        assertEquals(Voltage.HIGH, line.liveVoltage(sink), "the line is fed with the high voltage");
        assertFalse(line.overvolts(sink), "which the machine at its far end was built for");

        assertEquals(0, line.pull(cells, sink, 100), "nothing is handed over while the run melts");
        assertTrue(cells.removed.contains("1,64,0"), "the gate, which cannot take what the line is fed with");
        assertFalse(cells.removed.contains("0,64,0"), "the silver cable takes it and stands");
        assertFalse(cells.removed.contains("2,64,0"), "and the machine that was built for it survives");
    }
}
