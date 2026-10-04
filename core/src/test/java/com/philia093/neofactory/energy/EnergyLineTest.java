package com.philia093.neofactory.energy;

import com.philia093.neofactory.block.Block;
import com.philia093.neofactory.block.BlockFace;
import com.philia093.neofactory.block.BlockRegistry;
import com.philia093.neofactory.block.Blocks;
import com.philia093.neofactory.blockentity.BlockEntityRegistry;
import com.philia093.neofactory.blockentity.BlockEntityTypes;
import com.philia093.neofactory.blockentity.DiodeBlockEntity;
import com.philia093.neofactory.blockentity.MachineBlockEntity;
import com.philia093.neofactory.blockentity.TransformerBlockEntity;
import com.philia093.neofactory.cable.CableKind;
import com.philia093.neofactory.cable.CableMaterial;
import com.philia093.neofactory.cable.CableMaterials;
import com.philia093.neofactory.cable.CableSize;
import com.philia093.neofactory.cable.Cables;
import com.philia093.neofactory.cable.Voltage;
import com.philia093.neofactory.machine.DiodeMachine;
import com.philia093.neofactory.machine.Diodes;
import com.philia093.neofactory.machine.Machine;
import com.philia093.neofactory.machine.MachineEnergyStorage;
import com.philia093.neofactory.machine.MachineInventory;
import com.philia093.neofactory.machine.MachineScreen;
import com.philia093.neofactory.machine.ProgressKind;
import com.philia093.neofactory.machine.SlotKind;
import com.philia093.neofactory.machine.SteamBoilerMachine;
import com.philia093.neofactory.machine.TransformerMachine;
import com.philia093.neofactory.machine.Transformers;
import com.philia093.neofactory.support.TestRegistries;
import com.philia093.neofactory.world.TickClock;
import com.philia093.neofactory.world.World;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks what a machine of the power network does with the line of cables it stands on.
 * <p>
 * A cable carries nothing of its own: the machine that works draws what it needs out of the line it stands
 * on, and the machine that makes power only fills its own buffer. Both halves of that are checked here
 * against a world of the game, because a machine reads a line off the blocks around it: a machine that wants
 * power walks the line of the side a player gave to the plug it takes power in through, see
 * {@code MachineBlockEntity#updateEnergy}, and the line brings what its ends can give, see
 * {@link EnergyGrid.Line#pull}.
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
        cable(world, CableMaterials.COPPER, x, y, z, joined);
    }

    /**
     * Places one cable of a material, joined to the sides that were named.
     *
     * @param world world the cable is placed in
     * @param material metal the cable is drawn from, which is the tier of the line it is part of
     * @param x block X coordinate
     * @param y block Y coordinate, the height
     * @param z block Z coordinate
     * @param joined sides of the cable that carry the join
     */
    private static void cable(World world, CableMaterial material, int x, int y, int z, BlockFace... joined) {
        world.setBlock(x, y, z, cableBlock(material));
        world.setState(x, y, z, Cables.stateOf(Cables.mask(joined)));
    }

    /** The block one cable of a material stands as. */
    private static Block cableBlock(CableMaterial material) {
        return Cables.of(material, CableSize.SINGLE, CableKind.CABLE).block();
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

    /** A machine of the power network of a tier that fills its buffer and hands it to the line. */
    private static TestMachine machine(Voltage tier) {
        return new TestMachine(CAPACITY, tier, tier.euPerTick());
    }

    /**
     * A machine of the power network of a tier that only works: it takes power in and gives none over.
     * <p>
     * A machine that works spends out of its own buffer and is never a source of the line it stands on, which
     * is what keeps a line of machines that work from feeding one another, see {@link MachineEnergyStorage}.
     *
     * @param tier tier the machine was built for
     * @return the machine
     */
    private static TestMachine consumer(Voltage tier) {
        return new TestMachine(CAPACITY, tier, 0);
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
        sink.machine().faces().setEnergyIn(BlockFace.WEST);
        EnergyGrid.Line line = lineOf(world, 1, Y, 0);

        settle(world, 1);

        // The machine that works draws one ampere of its tier a tick out of the line, and the loss of the run
        // is paid out of what it draws: the machine that asked receives a tick of the line less that loss.
        int perTick = line.net().capacity() - line.net().totalLoss();
        assertEquals(Voltage.MEDIUM, line.net().voltage(), "a single copper cable is a line of its material");
        assertEquals(1, line.net().amperage());
        assertEquals(perTick, bufferOf(sink).amount(), "one tick of the line arrived in the buffer of it");
        assertEquals(CAPACITY - line.net().capacity(), bufferOf(generator).amount(),
                "and the machine that stood behind it gave one tick of the line up");
        assertTrue(perTick > 0);
    }

    @Test
    void aLineCarriesOneTickOfItsTierAtATime() {
        World world = new World(SEED, 6, 0);
        MachineBlockEntity generator = place(world, machine(Voltage.MEDIUM), 0, Y, 0);
        bufferOf(generator).setAmount(CAPACITY);
        cable(world, 1, Y, 0, BlockFace.WEST, BlockFace.EAST);
        MachineBlockEntity sink = place(world, machine(Voltage.MEDIUM), 2, Y, 0);
        sink.machine().faces().setEnergyIn(BlockFace.WEST);
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
        sink.machine().faces().setEnergyIn(BlockFace.SOUTH);

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
        sink.machine().faces().setEnergyIn(BlockFace.SOUTH);

        settle(world, 2);

        assertEquals(0, bufferOf(sink).amount(), "nothing arrived");
        assertEquals(CAPACITY, bufferOf(generator).amount(), "and the generator kept its energy");
    }

    @Test
    void aLineThatIsTooStrongForAMachineTakesTheMachineAway() {
        World world = new World(SEED, 3, 0);
        MachineBlockEntity generator = place(world, machine(Voltage.MEDIUM), 0, Y, 0);
        bufferOf(generator).setAmount(CAPACITY);
        // The copper cable of the run is a line of the very tier the machine that gives it its power is built
        // for, while the machine at its far end was built for the low one: the machine goes and the cable of
        // the line stays, see EnergyGrid.Line#pull.
        cable(world, 1, Y, 0, BlockFace.WEST, BlockFace.EAST);
        MachineBlockEntity sink = place(world, consumer(Voltage.LOW), 2, Y, 0);
        sink.machine().faces().setEnergyIn(BlockFace.WEST);

        settle(world, 1);

        assertEquals(Blocks.AIR, world.getBlock(2, Y, 0), "the machine the line was too much for is gone");
        assertEquals(cableBlock(CableMaterials.COPPER), world.getBlock(1, Y, 0),
                "while the copper cable of the line takes the middle voltage that feeds it and stands");
        assertEquals(Blocks.FURNACE, world.getBlock(0, Y, 0), "the machine that made the power survives");
        assertEquals(CAPACITY, bufferOf(generator).amount(), "and it handed nothing over");
    }

    @Test
    void aRunOfACableThatCannotTakeItsFeedMeltsWhereThePowerEntersIt() {
        World world = new World(SEED, 8, 0);
        MachineBlockEntity generator = place(world, machine(Voltage.HIGH), 0, Y, 0);
        bufferOf(generator).setAmount(CAPACITY);
        // A run of tin, which is a line of the low voltage, fed by a machine of the high voltage: the piece at
        // the machine that gives melts while the rest of the run stands, see EnergyGrid.Line#melt.
        cable(world, CableMaterials.TIN, 1, Y, 0, BlockFace.WEST, BlockFace.EAST);
        cable(world, CableMaterials.TIN, 2, Y, 0, BlockFace.WEST, BlockFace.EAST);
        MachineBlockEntity sink = place(world, consumer(Voltage.HIGH), 3, Y, 0);
        sink.machine().faces().setEnergyIn(BlockFace.WEST);

        settle(world, 1);

        assertEquals(Blocks.AIR, world.getBlock(1, Y, 0),
                "the piece of the run at the machine that gives it its power");
        assertEquals(cableBlock(CableMaterials.TIN), world.getBlock(2, Y, 0), "while the rest of the run stands");
        assertEquals(Blocks.FURNACE, world.getBlock(3, Y, 0),
                "and the machine that was built for that tier survives, as its run melted before it");
        assertEquals(Blocks.FURNACE, world.getBlock(0, Y, 0), "the machine that gives survives as well");
        assertEquals(CAPACITY, bufferOf(generator).amount(), "and nothing was handed over while the run melted");
    }

    @Test
    void aDiodeCarriesALineTheWayItsPowerRuns() {
        World world = new World(SEED, 9, 0);
        MachineBlockEntity generator = place(world, machine(Voltage.MEDIUM), 0, Y, 0);
        bufferOf(generator).setAmount(CAPACITY);
        cable(world, 1, Y, 0, BlockFace.WEST, BlockFace.EAST);
        // The diode lies in the run with its input towards the machine that gives and its output towards the
        // machine that works, which a player builds by turning the block: a machine that looks south has its
        // left flank towards the west, so the power of the diode runs in from there and out towards the east,
        // see DiodeMachine.
        diode(world, Voltage.MEDIUM, 4, 2, 0, BlockFace.SOUTH);
        cable(world, 3, Y, 0, BlockFace.WEST, BlockFace.EAST);
        MachineBlockEntity sink = place(world, consumer(Voltage.MEDIUM), 4, Y, 0);
        sink.machine().faces().setEnergyIn(BlockFace.WEST);

        settle(world, 1);

        // The facing of a diode is read off its cell while it ticks, so the line is walked after the first
        // tick of the world, see MachineBlockEntity#settleFacing.
        EnergyGrid.Line line = lineOf(world, 3, Y, 0);
        int perTick = line.net().capacity() - line.net().totalLoss();
        assertEquals(3, line.length(), "the two cables and the diode between them");
        assertEquals(1, line.net().amperage(), "the narrowest piece of the run carries one ampere");
        assertEquals(perTick, bufferOf(sink).amount(), "and what the machine asked for arrived");
        assertEquals(CAPACITY - line.net().capacity(), bufferOf(generator).amount(),
                "while the machine that gives paid a tick of the line");
        assertEquals(Diodes.nameOf(Voltage.MEDIUM, 4),
                world.getBlock(2, Y, 0).name(), "and the diode stands where it was built");
    }

    @Test
    void aDiodeRefusesALineThatArrivesTheOtherWay() {
        World world = new World(SEED, 11, 0);
        MachineBlockEntity generator = place(world, machine(Voltage.MEDIUM), 0, Y, 0);
        bufferOf(generator).setAmount(CAPACITY);
        cable(world, 1, Y, 0, BlockFace.WEST, BlockFace.EAST);
        // The same run with the diode turned the other way: its power now runs in from the east and out
        // towards the west, so the machine that works reaches it the way its power does not run and no line
        // is walked at all, see LineNode#through.
        diode(world, Voltage.MEDIUM, 4, 2, 0, BlockFace.NORTH);
        cable(world, 3, Y, 0, BlockFace.WEST, BlockFace.EAST);
        MachineBlockEntity sink = place(world, consumer(Voltage.MEDIUM), 4, Y, 0);
        sink.machine().faces().setEnergyIn(BlockFace.WEST);

        settle(world, 1);

        assertEquals(0, bufferOf(sink).amount(), "the walk of the machine was refused, so nothing arrived");
        assertEquals(CAPACITY, bufferOf(generator).amount(), "and the machine that gives kept what it holds");
        assertEquals(Diodes.nameOf(Voltage.MEDIUM, 4), world.getBlock(2, Y, 0).name(),
                "while the diode stands where it was built");
    }

    @Test
    void aDiodeThatCannotTakeItsLineMeltsWhereItStands() {
        World world = new World(SEED, 12, 0);
        MachineBlockEntity generator = place(world, machine(Voltage.HIGH), 0, Y, 0);
        bufferOf(generator).setAmount(CAPACITY);
        cable(world, CableMaterials.SILVER, 1, Y, 0, BlockFace.WEST, BlockFace.EAST);
        // A diode of the low voltage in a line a machine of the high voltage feeds, with silver cables around
        // it, which take that voltage: what a line is fed with is what every piece of it has to take, and the
        // diode does not take it, see EnergyGrid.Line#melt.
        diode(world, Voltage.LOW, 16, 2, 0, BlockFace.SOUTH);
        cable(world, CableMaterials.SILVER, 3, Y, 0, BlockFace.WEST, BlockFace.EAST);
        MachineBlockEntity sink = place(world, consumer(Voltage.HIGH), 4, Y, 0);
        sink.machine().faces().setEnergyIn(BlockFace.WEST);

        settle(world, 1);

        assertEquals(Blocks.AIR, world.getBlock(2, Y, 0), "the diode, which cannot take what the line is fed with");
        assertEquals(cableBlock(CableMaterials.SILVER), world.getBlock(1, Y, 0),
                "while the cables of the run, which take it, stand");
        assertEquals(Blocks.FURNACE, world.getBlock(4, Y, 0), "and so does the machine that was built for it");
        assertEquals(0, bufferOf(sink).amount(), "and nothing was handed over while it melted");
    }

    @Test
    void aLineThatNobodyTakesFromLeavesTheGeneratorAlone() {
        World world = new World(SEED, 5, 0);
        MachineBlockEntity generator = place(world, machine(Voltage.MEDIUM), 0, Y, 0);
        bufferOf(generator).setAmount(CAPACITY);
        cable(world, 1, Y, 0, BlockFace.WEST, BlockFace.EAST);
        MachineBlockEntity sink = place(world, machine(Voltage.MEDIUM), 2, Y, 0);
        bufferOf(sink).setAmount(sink.machine().energy().capacity());
        sink.machine().faces().setEnergyIn(BlockFace.WEST);

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
        // The machine that works is of the tier of the cable, so the line is not too much for it: what is
        // checked here is that a machine with no buffer feeds nothing, not that a wrong tier burns a machine.
        MachineBlockEntity sink = place(world, machine(Voltage.MEDIUM), 2, Y, 0);
        sink.machine().faces().setEnergyIn(BlockFace.WEST);

        settle(world, 3);

        assertEquals(0, boiler.machine().energy().capacity(), "a machine of steam has no buffer");
        assertEquals(0, bufferOf(sink).amount(), "so nothing was made and nothing was carried");
        assertEquals(Blocks.FURNACE, world.getBlock(2, Y, 0),
                "and a line that was never fed burns nothing");
        assertNotNull(world.getBlock(1, Y, 0), "the cable of that line is still there");
    }

    /** Places one diode of the game, turned towards a side, and hands its block entity back. */
    private static DiodeBlockEntity diode(World world, Voltage tier, int width, int x, int z, BlockFace facing) {
        String name = Diodes.nameOf(tier, width);
        Block block = BlockRegistry.byName(name);
        world.setBlock(x, Y, z, block);
        world.setState(x, Y, z, block.states().stateOf(Map.of("facing", facing.toString())));
        DiodeBlockEntity entity = new DiodeBlockEntity(BlockEntityRegistry.byName(name),
                new DiodeMachine(tier, width));
        entity.setPosition(x, Y, z);
        world.addBlockEntity(entity);
        return entity;
    }

    @Test
    void aTransformerStepsALineDownForAMachineOfAnEarlierAge() {
        World world = new World(SEED, 13, 0);
        MachineBlockEntity generator = place(world, machine(Voltage.MEDIUM), 0, Y, 0);
        bufferOf(generator).setAmount(CAPACITY);
        cable(world, 1, Y, 0, BlockFace.WEST, BlockFace.EAST);
        // The front of a transformer is the one side of it that carries the high voltage, so the block is
        // turned towards the machine that gives it the power: the middle voltage arrives on its front and the
        // low voltage leaves it through the five other sides, see TransformerMachine.
        transformer(world, Voltage.LOW, 1, 2, 0, BlockFace.WEST);
        cable(world, 3, Y, 0, BlockFace.WEST, BlockFace.EAST);
        MachineBlockEntity sink = place(world, consumer(Voltage.LOW), 4, Y, 0);
        sink.machine().faces().setEnergyIn(BlockFace.WEST);

        settle(world, 4);

        // One ampere of the middle voltage arrives on the high side every tick and leaves the low one as four
        // amperes of the low voltage, of which the machine takes the one it asks for.
        int arrived = bufferOf(sink).amount();
        assertTrue(arrived >= 2 * Voltage.LOW.euPerTick(),
                "the machine of the low voltage was fed by the transformer, tick after tick");
        assertEquals(0, arrived % Voltage.LOW.euPerTick(), "in whole amperes of its own age");
        assertTrue(bufferOf(generator).amount() < CAPACITY, "and the machine that gives paid for it");
    }

    @Test
    void aTransformerThatWasTurnedStepsALineUpForAMachineOfALaterAge() {
        World world = new World(SEED, 14, 0);
        MachineBlockEntity generator = place(world, machine(Voltage.LOW), 0, Y, 0);
        bufferOf(generator).setAmount(CAPACITY);
        cable(world, 1, Y, 0, BlockFace.WEST, BlockFace.EAST);
        TransformerBlockEntity transformer = transformer(world, Voltage.LOW, 1, 2, 0, BlockFace.EAST);
        cable(world, 3, Y, 0, BlockFace.WEST, BlockFace.EAST);
        // The same transformer, turned around by a knock of a mallet: the five sides of the low voltage take
        // the power in and the one of the high voltage hands it out, see TransformerMachine#toggle.
        transformer.transformer().toggle();
        MachineBlockEntity sink = place(world, consumer(Voltage.MEDIUM), 4, Y, 0);
        sink.machine().faces().setEnergyIn(BlockFace.WEST);

        settle(world, 4);

        // The low side brings an ampere of the low voltage a tick, which is a quarter of an ampere of the
        // middle one: what the machine takes in is that energy and no more, and it arrives a tick at a time.
        int arrived = bufferOf(sink).amount();
        assertTrue(arrived >= Voltage.LOW.euPerTick(),
                "the machine of the middle voltage was fed through the side of the high voltage");
        assertTrue(bufferOf(generator).amount() < CAPACITY, "and something was paid for it");
    }

    /** Places one transformer of the game, turned towards a side, and hands its block entity back. */
    private static TransformerBlockEntity transformer(World world, Voltage tier, int size, int x, int z,
            BlockFace facing) {
        String name = Transformers.nameOf(tier, size);
        Block block = BlockRegistry.byName(name);
        world.setBlock(x, Y, z, block);
        world.setState(x, Y, z, block.states().stateOf(Map.of("facing", facing.toString())));
        TransformerBlockEntity entity = new TransformerBlockEntity(BlockEntityRegistry.byName(name),
                new TransformerMachine(tier, size));
        entity.setPosition(x, Y, z);
        world.addBlockEntity(entity);
        return entity;
    }

    /** A machine of the power network: a buffer of a tier and one slot that nothing is ever put into. */
    private static final class TestMachine extends Machine {

        private final Voltage tier;

        /**
         * Creates a machine of the network of the tests.
         *
         * @param capacity capacity of its buffer
         * @param tier tier it was built for
         * @param maxExtract what it may hand a line a tick, {@code 0} for a machine that only works
         */
        private TestMachine(int capacity, Voltage tier, int maxExtract) {
            super(new MachineScreen("Test", ProgressKind.GENERIC, List.of(SlotKind.SMELTING), List.of(), 0, 0,
                            false),
                    new MachineInventory(MachineInventory.Role.INPUT),
                    new MachineEnergyStorage(capacity, tier.euPerTick(), maxExtract, tier), List.of());
            this.tier = tier;
        }

        @Override
        public int requestEu() {
            // A machine of the test draws one ampere of its tier a tick, the way a machine of the power
            // network tops up its buffer while it waits, see Machine#requestEu and MachineBlockEntity.
            return tier.euPerTick();
        }

        @Override
        protected void update(float delta) {
            // A machine of the test makes no power of its own: the test fills the buffer by hand.
        }
    }
}
