package com.philia093.neofactory.energy;

import com.philia093.neofactory.block.BlockFace;
import com.philia093.neofactory.block.Blocks;
import com.philia093.neofactory.blockentity.BlockEntityTypes;
import com.philia093.neofactory.blockentity.MachineBlockEntity;
import com.philia093.neofactory.cable.CableKind;
import com.philia093.neofactory.cable.CableMaterials;
import com.philia093.neofactory.cable.CableSize;
import com.philia093.neofactory.cable.Cables;
import com.philia093.neofactory.cable.Voltage;
import com.philia093.neofactory.machine.Machine;
import com.philia093.neofactory.machine.MachineEnergyStorage;
import com.philia093.neofactory.machine.MachineInventory;
import com.philia093.neofactory.machine.MachineScreen;
import com.philia093.neofactory.machine.ProgressKind;
import com.philia093.neofactory.machine.SlotKind;
import com.philia093.neofactory.machine.SteamBoilerMachine;
import com.philia093.neofactory.support.TestRegistries;
import com.philia093.neofactory.world.TickClock;
import com.philia093.neofactory.world.World;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks what a machine of the power network does with the line of cables it stands on.
 * <p>
 * A cable carries nothing of its own: a line moves what a machine hands it and what it brings to the machines
 * at its ends. Both halves of that are checked here against a world of the game, because a machine reads a
 * line off the blocks around it: a machine that makes power walks the line of the side a player gave to its
 * plug, see {@code MachineBlockEntity#updateEnergy}, and the line shares what it was handed out over the
 * buffers that hang on it, see {@link EnergyGrid.Line#push}.
 * <p>
 * The machines of these tests are of the test - a buffer of a tier and no slots at all - but the blocks and
 * the cables are the ones of the game, so what is checked is the whole way from a block entity to the buffer
 * of the next machine, the mask of the cables included.
 */
class EnergyLineTest {

    private static final int SEED = 21;

    /** Height the machines and the cables of these tests stand at. */
    private static final int Y = 64;

    /** Capacity of the buffers of the tests, which is several ticks of work. */
    private static final int CAPACITY = 4000;

    @BeforeAll
    static void registerGameData() {
        TestRegistries.ensure();
    }

    /**
     * Places a machine of the power network in a world, with the entity the world ticks.
     * <p>
     * The block is the furnace of the game, which is a machine that hangs on no line: what is checked here is
     * the entity of a machine of the test on a block of the game, the way a block is given the entity of the
     * block it declares.
     *
     * @param world world the machine is placed in
     * @param machine machine behind the block
     * @param x block X coordinate
     * @param y block Y coordinate, the height
     * @param z block Z coordinate
     * @return the entity of the block that was placed
     */
    private static MachineBlockEntity place(World world, Machine machine, int x, int y, int z) {
        world.setBlock(x, y, z, Blocks.FURNACE);
        MachineBlockEntity entity = new MachineBlockEntity(BlockEntityTypes.FURNACE, machine);
        entity.setPosition(x, y, z);
        world.addBlockEntity(entity);
        return entity;
    }

    /**
     * Places one cable of copper, joined to the sides that were named.
     *
     * @param world world the cable is placed in
     * @param x block X coordinate
     * @param y block Y coordinate, the height
     * @param z block Z coordinate
     * @param joined sides of the cable that carry the join
     */
    private static void cable(World world, int x, int y, int z, BlockFace... joined) {
        world.setBlock(x, y, z, Cables.of(CableMaterials.COPPER, CableSize.SINGLE, CableKind.CABLE)
                .block());
        world.setState(x, y, z, Cables.stateOf(Cables.mask(joined)));
    }

    /** Lets the world tick a while, which is what moves the energy of a line. */
    private static void settle(World world, int ticks) {
        for (int tick = 0; tick < ticks; tick++) {
            world.tick(TickClock.TICK_SECONDS);
        }
    }

    /** Buffer of a machine behind a block, as the machine was built with it. */
    private static MachineEnergyStorage bufferOf(MachineBlockEntity entity) {
        return (MachineEnergyStorage) entity.machine().energy();
    }

    /** The line of cables the machine of a cell stands on, through the cable beside it. */
    private static EnergyGrid.Line lineOf(World world, int x, int y, int z) {
        EnergyGrid.Line line = EnergyGrid.line(EnergyGrid.of(world), x, y, z);
        assertNotNull(line, "a cable stands in that cell");
        return line;
    }

    /** A machine of the power network of a tier, with a buffer of the standard size of these tests. */
    private static TestMachine machine(Voltage tier) {
        return new TestMachine(CAPACITY, tier);
    }

    @Test
    void aMachineThatMakesPowerFeedsTheLineAtItsPlug() {
        World world = new World(SEED, 0, 0);
        MachineBlockEntity generator = place(world, machine(Voltage.MEDIUM), 0, Y, 0);
        bufferOf(generator).setAmount(CAPACITY);
        // The plug a machine gives its power out of is the left flank of a machine that looks north, and the
        // cable is built there: a machine feeds the line of the side a player set, see FaceConfig.
        cable(world, 1, Y, 0, BlockFace.WEST, BlockFace.EAST);
        MachineBlockEntity sink = place(world, machine(Voltage.MEDIUM), 2, Y, 0);
        sink.machine().faces().setEnergyIn(BlockFace.WEST, sink.facing());
        EnergyGrid.Line line = lineOf(world, 1, Y, 0);

        settle(world, 1);

        // A machine hands one ampere of its tier over a tick and the loss of the run is paid out of the very
        // amperes it hands over, so the machine at the other end of a line receives a tick of it less the loss.
        int perTick = line.net().capacity() - line.net().totalLoss();
        assertEquals(Voltage.MEDIUM, line.net().voltage(), "a single copper cable is a line of its material");
        assertEquals(1, line.net().amperage());
        assertEquals(perTick, bufferOf(sink).amount(), "one tick of the line arrived in the buffer of it");
        assertEquals(CAPACITY - line.net().capacity(), bufferOf(generator).amount(),
                "and the machine that made the power handed one tick of the line over");
        assertTrue(perTick > 0);
    }

    @Test
    void aLineCarriesOneTickOfItsTierAtATime() {
        World world = new World(SEED, 6, 0);
        MachineBlockEntity generator = place(world, machine(Voltage.MEDIUM), 0, Y, 0);
        bufferOf(generator).setAmount(CAPACITY);
        cable(world, 1, Y, 0, BlockFace.WEST, BlockFace.EAST);
        MachineBlockEntity sink = place(world, machine(Voltage.MEDIUM), 2, Y, 0);
        sink.machine().faces().setEnergyIn(BlockFace.WEST, sink.facing());
        EnergyGrid.Line line = lineOf(world, 1, Y, 0);
        int perTick = line.net().capacity() - line.net().totalLoss();

        settle(world, 3);

        assertEquals(3 * perTick, bufferOf(sink).amount(), "three ticks of the line, one a tick");
        assertEquals(CAPACITY - 3 * line.net().capacity(), bufferOf(generator).amount(),
                "and the machine handed over a tick of it every tick");
    }

    @Test
    void aCableThatIsNotJoinedTowardsTheMachineCarriesNothing() {
        World world = new World(SEED, 1, 0);
        MachineBlockEntity generator = place(world, machine(Voltage.MEDIUM), 0, Y, 0);
        bufferOf(generator).setAmount(CAPACITY);
        // The cable stands at the plug of the generator but joins towards the machine beside it: the walk of
        // the line never reaches the generator, which is what a player who cut the join asked for.
        cable(world, 1, Y, 0, BlockFace.NORTH);
        MachineBlockEntity sink = place(world, machine(Voltage.MEDIUM), 1, Y, -1);
        sink.machine().faces().setEnergyIn(BlockFace.SOUTH, sink.facing());

        settle(world, 2);

        assertEquals(0, bufferOf(sink).amount(), "the line reaches the sink and is fed by nobody");
        assertEquals(CAPACITY, bufferOf(generator).amount(), "the generator handed nothing over");
        assertNotNull(world.getBlock(1, Y, 0), "and nothing burned");
    }

    @Test
    void aMachineThatTakesPowerThroughAnotherSideIsNotFed() {
        World world = new World(SEED, 2, 0);
        MachineBlockEntity generator = place(world, machine(Voltage.MEDIUM), 0, Y, 0);
        bufferOf(generator).setAmount(CAPACITY);
        cable(world, 1, Y, 0, BlockFace.WEST, BlockFace.EAST);
        MachineBlockEntity sink = place(world, machine(Voltage.MEDIUM), 2, Y, 0);
        // The cable reaches the sink from the west, but the power of the sink is taken in through its back: a
        // line hangs on the side a player set and on no other, see EnergyGrid.Cells#buffer.
        sink.machine().faces().setEnergyIn(BlockFace.SOUTH, sink.facing());

        settle(world, 2);

        assertEquals(0, bufferOf(sink).amount(), "nothing arrived");
        assertEquals(CAPACITY, bufferOf(generator).amount(), "and the generator kept its energy");
    }

    @Test
    void aLineThatIsTooStrongForAMachineBurnsIt() {
        World world = new World(SEED, 3, 0);
        MachineBlockEntity generator = place(world, machine(Voltage.MEDIUM), 0, Y, 0);
        bufferOf(generator).setAmount(CAPACITY);
        // A copper cable is a line of the middle voltage and the machine at its other end was built for the
        // low one: the machine and every cable of the line go, see EnergyAcceptor.
        cable(world, 1, Y, 0, BlockFace.WEST, BlockFace.EAST);
        MachineBlockEntity sink = place(world, machine(Voltage.LOW), 2, Y, 0);
        sink.machine().faces().setEnergyIn(BlockFace.WEST, sink.facing());

        settle(world, 1);

        assertEquals(Blocks.AIR, world.getBlock(1, Y, 0), "the cable of the line is gone");
        assertEquals(Blocks.AIR, world.getBlock(2, Y, 0), "and the machine the line was too strong for");
        assertEquals(Blocks.FURNACE, world.getBlock(0, Y, 0), "the machine that made the power survives");
        assertEquals(CAPACITY, bufferOf(generator).amount(), "and it handed nothing over");
    }

    @Test
    void aLineThatNobodyTakesFromLeavesTheGeneratorAlone() {
        World world = new World(SEED, 5, 0);
        MachineBlockEntity generator = place(world, machine(Voltage.MEDIUM), 0, Y, 0);
        bufferOf(generator).setAmount(CAPACITY);
        cable(world, 1, Y, 0, BlockFace.WEST, BlockFace.EAST);
        MachineBlockEntity sink = place(world, machine(Voltage.MEDIUM), 2, Y, 0);
        bufferOf(sink).setAmount(sink.machine().energy().capacity());
        sink.machine().faces().setEnergyIn(BlockFace.WEST, sink.facing());

        settle(world, 2);

        assertTrue(bufferOf(sink).isFull(), "the buffer of the machine is full");
        assertEquals(CAPACITY, bufferOf(generator).amount(),
                "nothing moved, so nothing was taken out of the generator to be lost on the way");
    }

    @Test
    void aMachineThatHoldsNoBufferFeedsNoLine() {
        World world = new World(SEED, 4, 0);
        // A boiler makes steam and holds no buffer, so it has no plug and no line to feed: a machine of the
        // age of steam stands along the cables of a workshop and does nothing with them.
        world.setBlock(0, Y, 0, Blocks.BRONZE_BOILER);
        MachineBlockEntity boiler = new MachineBlockEntity(BlockEntityTypes.BRONZE_BOILER,
                new SteamBoilerMachine());
        boiler.setPosition(0, Y, 0);
        world.addBlockEntity(boiler);
        cable(world, 1, Y, 0, BlockFace.WEST, BlockFace.EAST);
        MachineBlockEntity sink = place(world, machine(Voltage.LOW), 2, Y, 0);
        sink.machine().faces().setEnergyIn(BlockFace.WEST, sink.facing());

        settle(world, 3);

        assertEquals(0, boiler.machine().energy().capacity(), "a machine of steam has no buffer");
        assertEquals(0, bufferOf(sink).amount(), "so nothing was made and nothing was carried");
        assertEquals(Blocks.FURNACE, world.getBlock(2, Y, 0),
                "and a line that was never fed burns nothing");
        assertNotNull(world.getBlock(1, Y, 0), "the cable of that line is still there");
    }

    /** A machine of the power network: a buffer of a tier and one slot that nothing is ever put into. */
    private static final class TestMachine extends Machine {

        private TestMachine(int capacity, Voltage tier) {
            super(new MachineScreen("Test", ProgressKind.GENERIC, List.of(SlotKind.SMELTING), List.of(), 0, 0,
                            false),
                    new MachineInventory(MachineInventory.Role.INPUT),
                    new MachineEnergyStorage(capacity, tier), List.of());
        }

        @Override
        protected void update(float delta) {
            // A machine of the test makes no power of its own: the test fills the buffer by hand.
        }
    }
}
