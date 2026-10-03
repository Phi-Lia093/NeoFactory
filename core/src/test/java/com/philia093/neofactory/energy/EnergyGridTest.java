package com.philia093.neofactory.energy;

import com.philia093.neofactory.block.Block;
import com.philia093.neofactory.block.BlockFace;
import com.philia093.neofactory.block.Blocks;
import com.philia093.neofactory.cable.CableKind;
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

    /** The cells of the test: a map of blocks, of masks and of the machines that hang on them. */
    private static final class Cells implements EnergyGrid.Cells {

        private final Map<String, Block> blocks = new HashMap<>();
        private final Map<String, Integer> states = new HashMap<>();
        private final Map<String, EnergyStorage> buffers = new HashMap<>();
        private final Map<String, List<BlockFace>> plugs = new HashMap<>();
        private final List<String> removed = new ArrayList<>();

        private void cable(int x, int y, int z, int mask) {
            blocks.put(at(x, y, z), Cables.of(CableMaterials.COPPER, CableSize.SINGLE, CableKind.CABLE)
                    .block());
            states.put(at(x, y, z), mask);
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
        assertFalse(line.overvolts(machine), "a machine of high voltage takes a line of the middle one");
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
        assertEquals(0, line.push(cells, machine, 100), "so nothing is handed over");
        assertTrue(cells.removed.isEmpty(), "and nothing burns");
        assertEquals(1000, machine.amount(), "the buffer of the machine is untouched");
    }

    @Test
    void aLineTooStrongForAMachineBurnsTheLineAndTheMachine() {
        Cells cells = new Cells();
        Machine small = new Machine(Voltage.LOW, 100);
        Machine big = new Machine(Voltage.HIGH, 100);
        cells.cable(0, 64, 0, Cables.mask(BlockFace.EAST, BlockFace.WEST));
        cells.machine(1, 64, 0, small, BlockFace.WEST);
        cells.machine(-1, 64, 0, big, BlockFace.EAST);

        EnergyGrid.Line line = EnergyGrid.line(cells, 0, 64, 0);
        assertTrue(line.overvolts(small), "a line of the middle voltage is too much for a machine of low");
        assertFalse(line.overvolts(big));
        assertEquals(0, line.push(cells, big, 100), "nothing is handed over");
        assertTrue(cells.removed.contains("0,64,0"), "the cable of the line is gone");
        assertTrue(cells.removed.contains("1,64,0"), "and the machine it was too much for");
        assertFalse(cells.removed.contains("-1,64,0"), "the machine that could take it survives");
    }

    @Test
    void aLineThatFitsHandsTheEnergyOver() {
        Cells cells = new Cells();
        Machine source = new Machine(Voltage.HIGH, 1000);
        Machine sink = new Machine(Voltage.HIGH, 100);
        source.fill(1000);
        cells.cable(0, 64, 0, Cables.mask(BlockFace.EAST, BlockFace.WEST));
        cells.machine(1, 64, 0, sink, BlockFace.WEST);
        cells.machine(-1, 64, 0, source, BlockFace.EAST);

        EnergyGrid.Line line = EnergyGrid.line(cells, 0, 64, 0);
        assertEquals(100, line.push(cells, source, 100), "what fits arrives");
        assertEquals(100, sink.amount());
        assertEquals(1000 - 100 - line.net().totalLoss(), source.amount(),
                "and the source paid the loss of the line");
        assertTrue(cells.removed.isEmpty(), "nothing burned");
    }
}
