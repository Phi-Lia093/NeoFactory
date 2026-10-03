package com.philia093.neofactory.blockentity;

import com.philia093.neofactory.block.Block;
import com.philia093.neofactory.block.BlockFace;
import com.philia093.neofactory.block.Blocks;
import com.philia093.neofactory.fluid.Fluids;
import com.philia093.neofactory.item.FaceTool;
import com.philia093.neofactory.item.ItemStack;
import com.philia093.neofactory.item.Items;
import com.philia093.neofactory.machine.SteamBoilerMachine;
import com.philia093.neofactory.pipe.PipeMaterials;
import com.philia093.neofactory.pipe.PipeSize;
import com.philia093.neofactory.pipe.Pipes;
import com.philia093.neofactory.support.TestRegistries;
import com.philia093.neofactory.world.TickClock;
import com.philia093.neofactory.world.World;
import com.philia093.neofactory.world.interaction.FaceClick;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks what a machine and a line of pipes say to each other.
 * <p>
 * A machine is the pump of a line and a pipe is not, so the two meet in the two directions of
 * {@code FluidNode}: the side a tank that is filled is reached through is where a pipe pours into, and the
 * side of a tank the machine empties is where it gives what it made to the pipes that stand there. Which
 * side that is is what a player configured, see {@code FaceConfig}. The boiler of the game is the machine
 * the tests are built on, because it is the one machine that both takes a fluid and makes one: it is built
 * with its front to the north, so its water is reached from the west, its steam from the east and its vent
 * blows out of the back.
 */
class MachinePipeTest {

    private static final int SEED = 913;

    /** Cell the boiler of every test stands in. */
    private static final int X = 4;

    /** Height the boiler of every test stands at. */
    private static final int Y = 200;

    /** Cell the boiler of every test stands in. */
    private static final int Z = 6;

    /** Ticks a fired boiler of a test is run for, which is long enough for it to boil. */
    private static final int BOILING_TICKS = 200;

    @BeforeAll
    static void registerGameData() {
        TestRegistries.ensure();
    }

    /** Places the boiler with the entity the world ticks, the way a built block carries one. */
    private static MachineBlockEntity placeBoiler(World world, SteamBoilerMachine boiler) {
        world.setBlock(X, Y, Z, Blocks.BRONZE_BOILER);
        MachineBlockEntity entity = new MachineBlockEntity(BlockEntityTypes.BRONZE_BOILER, boiler);
        entity.setPosition(X, Y, Z);
        world.addBlockEntity(entity);
        return entity;
    }

    /** Places a pipe with the entity the world ticks, with the sides that were named joined. */
    private static PipeBlockEntity placePipe(World world, Pipes.Pipe pipe, int x, int y, int z,
            BlockFace... joined) {
        world.setBlock(x, y, z, pipe.block());
        world.setState(x, y, z, Pipes.mask(joined));
        PipeBlockEntity entity = new PipeBlockEntity(BlockEntityTypes.PIPE);
        entity.setPosition(x, y, z);
        world.addBlockEntity(entity);
        return entity;
    }

    /** The pipe of the tests: bronze, which carries the steam of a boiler without bursting. */
    private static Pipes.Pipe bronze() {
        return Pipes.of(PipeMaterials.BRONZE, PipeSize.MEDIUM);
    }

    /** Fires the boiler and fills it, the way a player does before it starts to boil. */
    private static void fire(SteamBoilerMachine boiler, int water) {
        boiler.water().fill(Fluids.WATER, water, false);
        boiler.inventory().set(SteamBoilerMachine.FUEL, ItemStack.of(Items.COAL, 1));
    }

    /** Lets the world tick, which is what runs the machine and the line. */
    private static void settle(World world, int ticks) {
        for (int tick = 0; tick < ticks; tick++) {
            world.tick(TickClock.TICK_SECONDS);
        }
    }

    @Test
    void aPipeReachesAMachine() {
        assertTrue(Pipes.connects(Blocks.BRONZE_BOILER), "a line may be built towards a machine");
        assertFalse(Pipes.connects(Blocks.FURNACE), "a machine without a tank is no place for fluid");
        assertFalse(Pipes.connects(Blocks.STONE), "and neither is a stone");
    }

    @Test
    void theSideOfAFilledTankTakesTheWaterOfALine() {
        World world = new World(SEED, 0, 0);
        SteamBoilerMachine boiler = new SteamBoilerMachine();
        placeBoiler(world, boiler);
        // The tank of water of a boiler that was built with its front to the north is reached from the west,
        // so the pipe stands at its west side and joins the east: the water of the line runs into the kettle.
        PipeBlockEntity line = placePipe(world, bronze(), X - 1, Y, Z, BlockFace.EAST);
        line.storage(world).fill(Fluids.WATER, 400, false);

        settle(world, 20);

        assertEquals(400, boiler.water().amount(), "the water of the line ran into the boiler");
        assertEquals(Fluids.WATER, boiler.water().fluid());
        assertEquals(0, line.storage(world).amount(), "and the line handed it over");
    }

    @Test
    void theSideOfAnEmptiedTankGivesItsSteamToALine() {
        World world = new World(SEED, 1, 0);
        SteamBoilerMachine boiler = new SteamBoilerMachine();
        placeBoiler(world, boiler);
        // The tank of steam is reached from the east, so the steam leaves the kettle through the pipe that
        // stands on that side of it.
        PipeBlockEntity line = placePipe(world, bronze(), X + 1, Y, Z, BlockFace.WEST);
        fire(boiler, 8000);

        settle(world, BOILING_TICKS);

        assertEquals(Fluids.STEAM, line.storage(world).fluid(), "the steam of the kettle ran into the line");
        assertTrue(line.storage(world).amount() > 0, "and the line carries it");
        assertTrue(boiler.isRunning(), "the boiler boiled all the while");
    }

    @Test
    void aSideOfAFilledTankDoesNotGiveAndASteamSideDoesNotTake() {
        World world = new World(SEED, 2, 0);
        SteamBoilerMachine boiler = new SteamBoilerMachine();
        placeBoiler(world, boiler);
        // A line at the side the steam of the kettle is reached from: that side gives, so the water of the
        // line is not taken by it.
        PipeBlockEntity line = placePipe(world, bronze(), X + 1, Y, Z, BlockFace.WEST);
        line.storage(world).fill(Fluids.WATER, 400, false);

        settle(world, 40);

        assertEquals(0, boiler.water().amount(), "the side of the steam tank is no mouth");
        assertEquals(400, line.storage(world).amount(), "and the line keeps its water");
        assertTrue(boiler.steam().isEmpty(), "the kettle made nothing that could leave either");
    }

    @Test
    void aMachineDoesNotPourIntoAPipeThatHasItsSideClosed() {
        World world = new World(SEED, 4, 0);
        SteamBoilerMachine boiler = new SteamBoilerMachine();
        placeBoiler(world, boiler);
        // The pipe stands at the side the steam is reached from, but its side towards the boiler is not
        // joined: a line is joined where a player joined it and nowhere else, so nothing is poured into it.
        PipeBlockEntity line = placePipe(world, bronze(), X + 1, Y, Z, BlockFace.EAST);
        fire(boiler, 8000);

        settle(world, BOILING_TICKS);

        assertEquals(0, line.storage(world).amount(), "the kettle poured into a side that is closed");
        assertTrue(boiler.steam().amount() > 0, "so the steam stays in the kettle");
    }

    @Test
    void aLineOfWoodenPipesAtABoilerBurstsWholeAndLeavesNothingHalfBroken() {
        World world = new World(SEED, 5, 0);
        SteamBoilerMachine boiler = new SteamBoilerMachine();
        placeBoiler(world, boiler);
        boiler.steam().fill(Fluids.STEAM, SteamBoilerMachine.STEAM_CAPACITY / 2, false);

        // The two wooden pipes were built beside the boiler - along the side its steam is reached from - and
        // joined to each other, and the player only then joins the near one to the kettle with the wrench.
        Pipes.Pipe wooden = Pipes.of(PipeMaterials.WOOD, PipeSize.MEDIUM);
        placePipe(world, wooden, X + 1, Y, Z, BlockFace.SOUTH);
        placePipe(world, wooden, X + 1, Y, Z + 1, BlockFace.NORTH);
        join(world, X + 1, Y, Z, BlockFace.WEST);

        settle(world, 40);

        for (int z = Z; z <= Z + 1; z++) {
            assertWholeOrBurst(world, X + 1, z);
        }
    }

    @Test
    void everyPipeThatIsLeftAfterABurstCanStillBeWorkedOn() {
        World world = new World(SEED, 6, 0);
        SteamBoilerMachine boiler = new SteamBoilerMachine();
        placeBoiler(world, boiler);
        boiler.steam().fill(Fluids.STEAM, SteamBoilerMachine.STEAM_CAPACITY, false);

        // Three wooden pipes in a line: the kettle empties itself into them and every one of them gives way
        // to the steam or stays a pipe a player can still work on.
        Pipes.Pipe wooden = Pipes.of(PipeMaterials.WOOD, PipeSize.MEDIUM);
        placePipe(world, wooden, X + 1, Y, Z, BlockFace.SOUTH);
        placePipe(world, wooden, X + 1, Y, Z + 1, BlockFace.SOUTH, BlockFace.NORTH);
        placePipe(world, wooden, X + 1, Y, Z + 2, BlockFace.SOUTH);
        join(world, X + 1, Y, Z, BlockFace.WEST);

        settle(world, 60);

        for (int step = 0; step <= 2; step++) {
            assertWholeOrBurst(world, X + 1, Z + step);
        }
    }

    /** Joins one side of a pipe with the wrench, the way a click on a cell of the grid does. */
    private static void join(World world, int x, int y, int z, BlockFace face) {
        PipeBlockEntity pipe = (PipeBlockEntity) world.blockEntity(x, y, z);
        pipe.operateFace(world, x, y, z, face, FaceTool.WRENCH, null,
                ItemStack.of(Items.WRENCH, 1), FaceClick.RIGHT);
    }

    /**
     * Asserts that a cell of a line is either air that kept nothing or a pipe that can still be worked on.
     * <p>
     * This is the shape of the bug a player reported: a pipe that burst away its block entity and kept its
     * block cannot be turned with the wrench any more and shows no grid of faces, while it still joins a
     * line that is built against it - a cell that is half broken.
     */
    private static void assertWholeOrBurst(World world, int x, int z) {
        Block block = world.getBlock(x, Y, z);
        if (block == Blocks.AIR) {
            assertNull(world.blockEntity(x, Y, z), "a burst pipe kept its tube at z=" + z);
            return;
        }
        assertTrue(Pipes.of(block) != null, "what is left at z=" + z + " is no pipe");
        assertTrue(world.blockEntity(x, Y, z) instanceof PipeBlockEntity,
                "the pipe at z=" + z + " lost its block entity and cannot be worked on");
    }

    @Test
    void theSteamOfAMachineBurstsAWoodenLine() {
        World world = new World(SEED, 3, 0);
        SteamBoilerMachine boiler = new SteamBoilerMachine();
        placeBoiler(world, boiler);
        // The steam of the kettle is reached from its east side, so the wooden pipe stands there and joins
        // the west: the steam of three hundred and seventy three kelvin runs into a pipe that gives way.
        placePipe(world, Pipes.of(PipeMaterials.WOOD, PipeSize.MEDIUM), X + 1, Y, Z, BlockFace.WEST);
        fire(boiler, 8000);

        settle(world, BOILING_TICKS);

        assertEquals(Blocks.AIR, world.getBlock(X + 1, Y, Z),
                "the steam of three hundred and seventy three kelvin bursts a wooden pipe");
        assertNull(world.blockEntity(X + 1, Y, Z), "and takes the tube with it");
    }
}
