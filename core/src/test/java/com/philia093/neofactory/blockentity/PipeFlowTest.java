package com.philia093.neofactory.blockentity;

import com.philia093.neofactory.block.BlockFace;
import com.philia093.neofactory.block.Blocks;
import com.philia093.neofactory.fluid.Fluid;
import com.philia093.neofactory.fluid.FluidStorage;
import com.philia093.neofactory.fluid.Fluids;
import com.philia093.neofactory.item.FaceTool;
import com.philia093.neofactory.item.ItemStack;
import com.philia093.neofactory.item.Items;
import com.philia093.neofactory.machine.GrinderMachine;
import com.philia093.neofactory.pipe.PipeFlow;
import com.philia093.neofactory.pipe.PipeMaterials;
import com.philia093.neofactory.pipe.PipeSize;
import com.philia093.neofactory.pipe.Pipes;
import com.philia093.neofactory.support.TestRegistries;
import com.philia093.neofactory.util.nbt.NbtCompound;
import com.philia093.neofactory.world.TickClock;
import com.philia093.neofactory.world.World;
import com.philia093.neofactory.world.interaction.FaceMark;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks what a pipe does with the fluid it is given.
 * <p>
 * The arithmetic of the transport is checked next door, see {@code PipeTransportTest}; what is checked here
 * is the pipe itself: how large the tube of a pipe is, what the valve of a side does, how fluid runs from a
 * pipe to the one next to it, and that a pipe which is given a fluid it cannot take is gone afterwards.
 */
class PipeFlowTest {

    private static final int SEED = 11;

    /** Height the pipes of the tests stand at. */
    private static final int Y = 64;

    @BeforeAll
    static void registerGameData() {
        TestRegistries.ensure();
    }

    /** The pipe most of the tests are built from: copper of the standard size. */
    private static Pipes.Pipe copper() {
        return Pipes.of(PipeMaterials.COPPER, PipeSize.MEDIUM);
    }

    /**
     * Places a pipe with the entity the world ticks, with the sides that were named joined.
     *
     * @param world world the pipe is built in
     * @param pipe pipe to place
     * @param x block X coordinate
     * @param y block Y coordinate, the height
     * @param z block Z coordinate
     * @param joined sides of the pipe that are joined, none for a pipe that stands alone
     * @return the entity of the block that was placed
     */
    private static PipeBlockEntity place(World world, Pipes.Pipe pipe, int x, int y, int z,
            BlockFace... joined) {
        world.setBlock(x, y, z, pipe.block());
        world.setState(x, y, z, Pipes.mask(joined));
        PipeBlockEntity entity = new PipeBlockEntity(BlockEntityTypes.PIPE);
        entity.setPosition(x, y, z);
        world.addBlockEntity(entity);
        return entity;
    }

    /**
     * Fills the tube of a pipe.
     * <p>
     * This is what a machine at the end of a line does, see {@code FluidNode}: a pipe is no pump and no
     * source, it only carries what somebody put into it.
     */
    private static void fill(World world, PipeBlockEntity pipe, Fluid fluid, int amount) {
        FluidStorage storage = pipe.storage(world);
        assertNotNull(storage, "the cell of a pipe carries a tube");
        assertEquals(amount, storage.fill(fluid, amount, false), "the tube is as large as its rate");
    }

    /** Amount of fluid one pipe of a line holds. */
    private static int amountOf(World world, PipeBlockEntity pipe) {
        FluidStorage storage = pipe.storage(world);
        return storage == null ? 0 : storage.amount();
    }

    /** Lets the world tick for a while, which is what moves the fluid of a line. */
    private static void settle(World world, int ticks) {
        for (int tick = 0; tick < ticks; tick++) {
            world.tick(TickClock.TICK_SECONDS);
        }
    }

    @Test
    void theTubeOfAPipeHoldsWhatThePipeMovesASecond() {
        World world = new World(SEED, 0, 0);
        PipeBlockEntity wooden = place(world, Pipes.of(PipeMaterials.WOOD, PipeSize.SMALL), 0, Y, 0);
        PipeBlockEntity copper = place(world, copper(), 1, Y, 0);
        PipeBlockEntity steel = place(world, Pipes.of(PipeMaterials.STEEL, PipeSize.HUGE), 2, Y, 0);

        assertEquals(200, wooden.storage(world).capacity(), "a small wooden pipe holds two hundred");
        assertEquals(400, copper.storage(world).capacity(), "a medium copper pipe four hundred");
        assertEquals(19200, steel.storage(world).capacity(), "and a huge steel pipe the most of them");
        assertTrue(copper.storage(world).isEmpty(), "a pipe that was never filled is empty");

        fill(world, copper, Fluids.WATER, 400);
        assertEquals(Fluids.WATER, copper.storage(world).fluid());
        assertEquals(0, copper.storage(world).fill(Fluids.WATER, 10, false),
                "a full tube takes no more fluid");
        assertEquals(0, copper.storage(world).fill(Fluids.LAVA, 10, false),
                "and it never mixes two fluids");
    }

    @Test
    void theValveOfASideWalksTheCycleOfItsThreeSettings() {
        World world = new World(SEED, 1, 0);
        PipeBlockEntity pipe = place(world, copper(), 0, Y, 0);

        assertEquals(PipeFlow.BOTH, pipe.flowOf(BlockFace.NORTH), "a new pipe takes and gives");
        assertEquals(PipeFlow.BOTH, pipe.flowOf(BlockFace.SOUTH), "on every side of it");

        // The wrench with the modifier key held walks the cycle and never runs out of settings.
        assertTrue(turn(world, pipe, BlockFace.NORTH, true));
        assertEquals(PipeFlow.IN, pipe.flowOf(BlockFace.NORTH), "a mouth takes only");
        turn(world, pipe, BlockFace.NORTH, true);
        assertEquals(PipeFlow.OUT, pipe.flowOf(BlockFace.NORTH), "an outlet gives only");
        turn(world, pipe, BlockFace.NORTH, true);
        assertEquals(PipeFlow.BOTH, pipe.flowOf(BlockFace.NORTH), "and the cycle starts again");

        assertTrue(pipe.flowOf(BlockFace.NORTH).takes());
        assertTrue(pipe.flowOf(BlockFace.NORTH).gives());
        assertFalse(PipeFlow.IN.gives(), "a mouth never gives");
        assertFalse(PipeFlow.OUT.takes(), "and an outlet never takes");
        assertEquals(PipeFlow.BOTH, pipe.flowOf(BlockFace.SOUTH), "the other sides were left alone");
    }

    @Test
    void aTripOfTheWrenchWithoutTheModifierStillTurnsTheJoin() {
        World world = new World(SEED, 2, 0);
        PipeBlockEntity pipe = place(world, copper(), 0, Y, 0);

        assertTrue(turn(world, pipe, BlockFace.EAST, false));
        assertEquals(Pipes.mask(BlockFace.EAST), Pipes.maskOf(world.getState(0, Y, 0)),
                "the join of the side was turned");
        assertEquals(PipeFlow.BOTH, pipe.flowOf(BlockFace.EAST), "and the valve was left alone");

        pipe.setFlow(BlockFace.WEST, PipeFlow.IN);
        assertTrue(turn(world, pipe, BlockFace.WEST, false));
        assertEquals(PipeFlow.IN, pipe.flowOf(BlockFace.WEST),
                "turning the join of a side leaves its valve where it was");
    }

    @Test
    void theValveOfAPipeSurvivesASaveGame() {
        World world = new World(SEED, 3, 0);
        PipeBlockEntity pipe = place(world, copper(), 0, Y, 0);
        pipe.setFlow(BlockFace.EAST, PipeFlow.IN);
        pipe.setFlow(BlockFace.TOP, PipeFlow.OUT);

        NbtCompound data = new NbtCompound("pipe");
        pipe.writeData(data);
        PipeBlockEntity loaded = new PipeBlockEntity(BlockEntityTypes.PIPE);
        loaded.setPosition(0, Y, 0);
        loaded.readData(data);

        assertEquals(PipeFlow.IN, loaded.flowOf(BlockFace.EAST));
        assertEquals(PipeFlow.OUT, loaded.flowOf(BlockFace.TOP));
        assertEquals(PipeFlow.BOTH, loaded.flowOf(BlockFace.NORTH));

        PipeBlockEntity fresh = new PipeBlockEntity(BlockEntityTypes.PIPE);
        fresh.readData(new NbtCompound("pipe"));
        assertEquals(PipeFlow.BOTH, fresh.flowOf(BlockFace.EAST), "a pipe without data takes and gives");
    }

    @Test
    void everyCellOfTheGridKnowsWhatItsSideDoes() {
        World world = new World(SEED, 10, 0);
        PipeBlockEntity pipe = place(world, copper(), 0, Y, 0, BlockFace.EAST);

        // A side that is not joined is crossed out, a side that is joined and lets the fluid run both ways
        // is plain, and a side that carries a valve answers with the arrow of it - the grid draws the three
        // of them, see FaceMark.
        assertEquals(FaceMark.NOTHING, pipe.faceMark(world, 0, Y, 0, BlockFace.EAST));
        assertEquals(FaceMark.CLOSED, pipe.faceMark(world, 0, Y, 0, BlockFace.WEST));
        assertEquals(FaceMark.CLOSED, pipe.faceMark(world, 0, Y, 0, BlockFace.TOP));
        assertEquals(FaceMark.CLOSED, pipe.faceMark(world, 0, Y, 0, BlockFace.BOTTOM));

        pipe.setFlow(BlockFace.EAST, PipeFlow.IN);
        assertEquals(FaceMark.IN, pipe.faceMark(world, 0, Y, 0, BlockFace.EAST));
        assertTrue(FaceMark.IN.isArrow());
        assertFalse(FaceMark.IN.pointsOut(), "a mouth takes the fluid of a line");

        pipe.setFlow(BlockFace.EAST, PipeFlow.OUT);
        assertEquals(FaceMark.OUT, pipe.faceMark(world, 0, Y, 0, BlockFace.EAST));
        assertTrue(FaceMark.OUT.pointsOut(), "an outlet gives it away");

        pipe.setFlow(BlockFace.EAST, PipeFlow.BOTH);
        assertEquals(FaceMark.NOTHING, pipe.faceMark(world, 0, Y, 0, BlockFace.EAST),
                "a side that runs both ways carries no mark");

        pipe.setFlow(BlockFace.WEST, PipeFlow.OUT);
        assertEquals(FaceMark.CLOSED, pipe.faceMark(world, 0, Y, 0, BlockFace.WEST),
                "the valve of a side that is not joined changes nothing");
        assertTrue(FaceMark.CLOSED.isCrossed());
    }

    @Test
    void aPipeThatLostItsBlockEntityIsBuiltAgain() {
        World world = new World(SEED, 11, 0);
        PipeBlockEntity west = place(world, copper(), 0, Y, 0, BlockFace.EAST);
        PipeBlockEntity east = place(world, copper(), 1, Y, 0, BlockFace.WEST);

        // A player reported a pipe whose block entity was gone: the block was still a pipe and still joined
        // a line that was built against it, but it could not be turned with the wrench, showed no grid of
        // faces and never burst. Every reader of a cell asks for its entity with the block, so such a cell is
        // built again and the line runs through it, see World#ensureBlockEntity.
        assertNotNull(world.removeBlockEntity(1, Y, 0), "the pipe of the test had an entity");
        assertNull(world.blockEntity(1, Y, 0), "and it is gone now");

        fill(world, west, Fluids.WATER, 400);
        settle(world, 8);

        assertNotNull(world.blockEntity(1, Y, 0), "the pipe was built again");
        assertTrue(world.blockEntity(1, Y, 0) instanceof PipeBlockEntity, "and it is a pipe");
        assertTrue(amountOf(world, (PipeBlockEntity) world.blockEntity(1, Y, 0)) > 0,
                "and the line runs into it");
    }

    /** Turns one side of a pipe with the wrench, the way a click on a cell of the grid does. */
    private static boolean turn(World world, PipeBlockEntity pipe, BlockFace face, boolean modifier) {
        return pipe.operateFace(world, pipe.x(), pipe.y(), pipe.z(), face, FaceTool.WRENCH, null,
                ItemStack.of(Items.WRENCH, 1), modifier);
    }

    @Test
    void fluidRunsFromAPipeToTheOneNextToIt() {
        World world = new World(SEED, 4, 0);
        PipeBlockEntity west = place(world, copper(), 0, Y, 0, BlockFace.EAST);
        PipeBlockEntity middle = place(world, copper(), 1, Y, 0, BlockFace.WEST, BlockFace.EAST);
        PipeBlockEntity east = place(world, copper(), 2, Y, 0, BlockFace.WEST);

        fill(world, west, Fluids.WATER, 400);
        settle(world, 8);

        // A pipe is no pump: what one pipe hands over is what the next one took, so the line holds what was
        // put into it and nothing more.
        assertEquals(400, amountOf(world, west) + amountOf(world, middle) + amountOf(world, east),
                "the fluid of a line stays in the line");
        assertTrue(amountOf(world, east) > 0, "and it reached the far end of it");
    }

    @Test
    void everyMachineBesideALineIsServedAndNotOnlyTheEnds() {
        World world = new World(SEED, 12, 0);
        // A line of three pipes, each of them with a machine on a side of its own: one at the head of the
        // line, one in its middle and one at its end.
        PipeBlockEntity head = place(world, copper(), 0, Y, 0, BlockFace.EAST, BlockFace.NORTH);
        PipeBlockEntity centre = place(world, copper(), 1, Y, 0, BlockFace.WEST, BlockFace.EAST,
                BlockFace.NORTH);
        PipeBlockEntity tail = place(world, copper(), 2, Y, 0, BlockFace.WEST, BlockFace.NORTH);
        MachineBlockEntity near = machine(world, 0);
        MachineBlockEntity middle = machine(world, 1);
        MachineBlockEntity far = machine(world, 2);

        // Every pipe of the line is given what one tick of it moves, the way a machine pouring into it gives
        // its fluid away, and the line runs for a while.
        fill(world, head, Fluids.STEAM, 20);
        fill(world, centre, Fluids.STEAM, 20);
        fill(world, tail, Fluids.STEAM, 20);
        settle(world, 20);

        // A pipe hands its fluid out by weight, and a machine used to weigh one against the four hundred of
        // the pipe behind the branch: its share rounded away, so the machine of the middle pipe of a line of
        // three stayed dry while the machines at the two ends were served, see
        // PipeTransport#weightOfMachine. Every machine beside a line is served today.
        assertTrue(steamOf(near) > 0, "the machine at the head of the line is served");
        assertTrue(steamOf(middle) > 0, "the machine in the middle of the line is served as well");
        assertTrue(steamOf(far) > 0, "and the machine at the end of it");
        assertEquals(60, steamOf(near) + steamOf(middle) + steamOf(far),
                "and the fluid the line was given is what the machines took");
    }

    /** Places a machine of the age of steam beside a pipe, on the north side of it. */
    private static MachineBlockEntity machine(World world, int x) {
        world.setBlock(x, Y, -1, Blocks.GRINDER);
        MachineBlockEntity entity = new MachineBlockEntity(BlockEntityTypes.GRINDER, new GrinderMachine());
        entity.setPosition(x, Y, -1);
        world.addBlockEntity(entity);
        return entity;
    }

    /** Steam the tank of a machine of the age of steam holds. */
    private static int steamOf(MachineBlockEntity machine) {
        return machine.machine().tank(0).storage().amount();
    }

    @Test
    void aSideThatOnlyTakesHandsNothingOn() {
        World world = new World(SEED, 5, 0);
        PipeBlockEntity west = place(world, copper(), 0, Y, 0, BlockFace.EAST);
        PipeBlockEntity east = place(world, copper(), 1, Y, 0, BlockFace.WEST);

        // The mouth of the west pipe: fluid may only enter it through that side and never leave it there.
        west.setFlow(BlockFace.EAST, PipeFlow.IN);
        fill(world, west, Fluids.WATER, 400);
        settle(world, 8);

        assertEquals(400, amountOf(world, west), "the mouth kept the fluid");
        assertEquals(0, amountOf(world, east), "and handed nothing over");
    }

    @Test
    void anOutletOfAPipeRefusesWhatIsOfferedToIt() {
        World world = new World(SEED, 6, 0);
        PipeBlockEntity west = place(world, copper(), 0, Y, 0, BlockFace.EAST);
        PipeBlockEntity east = place(world, copper(), 1, Y, 0, BlockFace.WEST);

        // The valve of the pipe that is offered the fluid: its west side only gives, so nothing enters there.
        east.setFlow(BlockFace.WEST, PipeFlow.OUT);
        fill(world, west, Fluids.WATER, 400);
        settle(world, 8);

        assertEquals(400, amountOf(world, west), "the fluid stayed where it was");
        assertEquals(0, amountOf(world, east), "and the outlet took nothing");
    }

    @Test
    void steamBurstsAWoodenPipe() {
        World world = new World(SEED, 7, 0);
        PipeBlockEntity wooden = place(world, Pipes.of(PipeMaterials.WOOD, PipeSize.MEDIUM), 0, Y, 0);
        fill(world, wooden, Fluids.STEAM, 200);

        settle(world, 1);

        assertEquals(Blocks.AIR, world.getBlock(0, Y, 0), "the wooden pipe gave way");
        assertNull(world.blockEntity(0, Y, 0), "and took its tube with it");
        assertNull(wooden.storage(world), "the cell of a burst pipe carries no fluid");
    }

    @Test
    void steamRunsInACopperPipe() {
        World world = new World(SEED, 8, 0);
        PipeBlockEntity copper = place(world, copper(), 0, Y, 0);
        fill(world, copper, Fluids.STEAM, 400);

        settle(world, 4);

        assertEquals(copper(), Pipes.of(world.getBlock(0, Y, 0)), "the copper pipe carried the steam");
        assertEquals(400, amountOf(world, copper), "and kept it, because no side is joined");
    }

    @Test
    void aPipeThatIsGoneHoldsNothing() {
        World world = new World(SEED, 9, 0);
        PipeBlockEntity pipe = place(world, copper(), 0, Y, 0);
        fill(world, pipe, Fluids.WATER, 400);

        // A player broke the pipe while the fluid stood in it.
        world.setBlock(0, Y, 0, Blocks.AIR);
        settle(world, 2);

        assertNull(pipe.storage(world), "a cell without a pipe holds no fluid");
        assertNotNull(world.getBlock(0, Y, 0), "and the tick of the entity changed nothing");
    }
}
