package com.philia093.neofactory.blockentity;

import com.philia093.neofactory.block.BlockFace;
import com.philia093.neofactory.block.Blocks;
import com.philia093.neofactory.item.FaceTool;
import com.philia093.neofactory.item.ItemStack;
import com.philia093.neofactory.item.Items;
import com.philia093.neofactory.pipe.PipeMaterials;
import com.philia093.neofactory.pipe.PipeSize;
import com.philia093.neofactory.pipe.Pipes;
import com.philia093.neofactory.support.TestRegistries;
import com.philia093.neofactory.world.TickClock;
import com.philia093.neofactory.world.World;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks what joins a pipe: nothing on its own, and whatever the wrench was put to.
 * <p>
 * A pipe that looked at the six cells around it would join whatever it found - two lines that meet at a
 * wall, a line that runs along a machine - so the game looks at nothing at all: the mask of a pipe is what
 * a player made it with the wrench and stays that way through any number of ticks, see
 * {@link PipeBlockEntity}. This test builds the lines a player would build, turns the sides of them the way
 * a click on a cell of the grid of faces does, and reads the states of the cells back.
 */
class PipeConnectionTest {

    private static final int SEED = 4242;
    private static final int Y = 200;

    /** Ticks the world is given while a mask has to stay as it is, two seconds of them. */
    private static final int TICKS = 40;

    @BeforeAll
    static void registerGameData() {
        TestRegistries.ensure();
    }

    /** The pipe most of these checks are built from. */
    private static Pipes.Pipe pipe() {
        return Pipes.of(PipeMaterials.BRONZE, PipeSize.MEDIUM);
    }

    /**
     * Places a pipe with the entity the world ticks, the way a built block carries one.
     *
     * @param world world the pipe is built in
     * @param pipe pipe to place
     * @param x block X coordinate
     * @param y block Y coordinate, the height
     * @param z block Z coordinate
     * @return the entity of the block that was placed
     */
    private static PipeBlockEntity place(World world, Pipes.Pipe pipe, int x, int y, int z) {
        world.setBlock(x, y, z, pipe.block());
        PipeBlockEntity entity = new PipeBlockEntity(BlockEntityTypes.PIPE);
        entity.setPosition(x, y, z);
        world.addBlockEntity(entity);
        return entity;
    }

    /**
     * Turns one side of a pipe with the wrench, the way a click on a cell of the grid does.
     * <p>
     * The player is left out: a pipe turns a side for whoever holds the wrench, so the operation is not
     * given one, see {@link PipeBlockEntity#operateFace}.
     *
     * @param world world the pipe lies in
     * @param pipe entity of the pipe
     * @param face side of the pipe to turn
     * @return {@code true} when the pipe turned that side
     */
    private static boolean turn(World world, PipeBlockEntity pipe, BlockFace face,
            boolean modifier) {
        return pipe.operateFace(world, pipe.x(), pipe.y(), pipe.z(), face, FaceTool.WRENCH, null,
                ItemStack.of(Items.WRENCH, 1), modifier);
    }

    /** Turns one side of a pipe with the wrench and no modifier key, the way a plain click does. */
    private static boolean turn(World world, PipeBlockEntity pipe, BlockFace face) {
        return turn(world, pipe, face, false);
    }

    /** Lets the world tick for a while, which is what must not change any mask of any pipe. */
    private static void settle(World world) {
        for (int tick = 0; tick < TICKS; tick++) {
            world.tick(TickClock.TICK_SECONDS);
        }
    }

    /** Connections of the pipe at a cell. */
    private static int maskOf(World world, int x, int y, int z) {
        return Pipes.maskOf(world.getState(x, y, z));
    }

    @Test
    void aPipeThatIsPlacedJoinsNothing() {
        World world = new World(SEED, 0, 0);
        place(world, pipe(), 0, Y, 0);
        place(world, pipe(), 1, Y, 0);
        place(world, pipe(), 2, Y, 0);

        settle(world);

        for (int x = 0; x < 3; x++) {
            assertEquals(0, maskOf(world, x, Y, 0),
                    "a pipe that stands next to other pipes joins nothing by itself");
        }
    }

    @Test
    void aMaskThatWasStoredStaysAsItWasStored() {
        // A world of an older version carries masks that were joined by looking around, and a player may
        // have closed sides. Nothing heals a mask: a side that is open stays open and a closed one stays
        // closed, whatever stands next to the pipe.
        World world = new World(SEED, 1, 1);
        place(world, pipe(), 0, Y, 0);
        world.setState(0, Y, 0, Pipes.stateOf(Pipes.ALL_MASK));

        settle(world);

        assertEquals(Pipes.ALL_MASK, maskOf(world, 0, Y, 0), "six sides were open and six stay open");
    }

    @Test
    void theWrenchOpensAndClosesASide() {
        World world = new World(SEED, 2, 2);
        PipeBlockEntity west = place(world, pipe(), 0, Y, 0);
        place(world, pipe(), 1, Y, 0);

        assertTrue(turn(world, west, BlockFace.EAST), "the wrench turned the side towards the other pipe");
        assertEquals(Pipes.mask(BlockFace.EAST), maskOf(world, 0, Y, 0));

        assertTrue(turn(world, west, BlockFace.EAST), "and turned it back");
        assertEquals(0, maskOf(world, 0, Y, 0), "a closed pipe is alone again");
    }

    @Test
    void theWrenchTurnsEverySideOfAPipe() {
        for (BlockFace face : Pipes.DIRECTIONS) {
            World world = new World(SEED, 3, 3);
            PipeBlockEntity centre = place(world, pipe(), 0, Y, 0);
            place(world, pipe(), face.x(), Y + face.y(), face.z());

            assertTrue(turn(world, centre, face), "the wrench turned " + face);
            assertEquals(Pipes.mask(face), maskOf(world, 0, Y, 0), "the side towards " + face);
            assertEquals(0, maskOf(world, face.x(), Y + face.y(), face.z()),
                    "and the pipe beside it was not joined by that");
        }
    }

    @Test
    void aSideIsOpenedTowardsEveryMaterialAndSize() {
        World world = new World(SEED, 4, 4);
        PipeBlockEntity small = place(world, Pipes.of(PipeMaterials.WOOD, PipeSize.SMALL), 0, Y, 0);
        PipeBlockEntity bundle = place(world, Pipes.of(PipeMaterials.STEEL, PipeSize.NONUPLE), 1, Y, 0);

        assertTrue(turn(world, small, BlockFace.EAST), "a small pipe joins a bundle");
        assertTrue(turn(world, bundle, BlockFace.WEST), "and the bundle joins the small pipe back");

        assertEquals(Pipes.mask(BlockFace.EAST), maskOf(world, 0, Y, 0));
        assertEquals(Pipes.mask(BlockFace.WEST), maskOf(world, 1, Y, 0));
    }

    @Test
    void aLineIsJoinedWhereTheWrenchWent() {
        World world = new World(SEED, 5, 5);
        List<PipeBlockEntity> line = List.of(place(world, pipe(), 0, Y, 0),
                place(world, pipe(), 1, Y, 0), place(world, pipe(), 2, Y, 0));

        assertTrue(turn(world, line.get(0), BlockFace.EAST));
        assertTrue(turn(world, line.get(1), BlockFace.WEST));
        assertTrue(turn(world, line.get(1), BlockFace.EAST));
        assertTrue(turn(world, line.get(2), BlockFace.WEST));

        settle(world);

        assertEquals(Pipes.mask(BlockFace.EAST), maskOf(world, 0, Y, 0), "the west end joins east only");
        assertEquals(Pipes.mask(BlockFace.EAST, BlockFace.WEST), maskOf(world, 1, Y, 0),
                "the middle joins both ways");
        assertEquals(Pipes.mask(BlockFace.WEST), maskOf(world, 2, Y, 0), "the east end joins west only");
    }

    @Test
    void aSideIsOpenedWhateverStandsNextToThePipe() {
        World world = new World(SEED, 6, 6);
        PipeBlockEntity pipe = place(world, pipe(), 0, Y, 0);
        world.setBlock(1, Y, 0, Blocks.STONE);

        // A player builds what they mean to build: an ending that leads into the open is a shape the models
        // draw - the plate of the size on the ending of the tube - and a wall may be gone a moment later.
        assertTrue(turn(world, pipe, BlockFace.WEST), "the side towards the open is turned");
        assertTrue(turn(world, pipe, BlockFace.EAST), "and so is the side towards the stone");
        assertEquals(Pipes.mask(BlockFace.EAST, BlockFace.WEST), maskOf(world, 0, Y, 0));

        assertTrue(turn(world, pipe, BlockFace.WEST), "and both are turned back");
        assertTrue(turn(world, pipe, BlockFace.EAST), "one after the other");
        assertEquals(0, maskOf(world, 0, Y, 0), "a pipe may be left alone again");
    }

    @Test
    void aPipeReachesAPipeAndNothingElse() {
        World world = new World(SEED, 6, 7);
        place(world, pipe(), 1, Y, 0);
        world.setBlock(2, Y, 0, Blocks.STONE);

        // The rule of the transport, and not one the wrench asks: a pipe may take fluid from a pipe, and
        // nothing of the game gives fluid from a stone.
        assertTrue(Pipes.connects(pipe().block()), "two pipes reach each other");
        assertTrue(Pipes.connects(Pipes.of(PipeMaterials.STEEL, PipeSize.NONUPLE).block()),
                "of whatever material and size");
        assertFalse(Pipes.connects(Blocks.STONE), "a stone is no source of fluid");
        assertFalse(Pipes.connects(Blocks.AIR), "and neither is the open air");
        assertFalse(Pipes.connects(null), "nothing at all reaches nothing");
    }

    @Test
    void anotherToolLeavesAPipeAlone() {
        World world = new World(SEED, 7, 7);
        PipeBlockEntity pipe = place(world, pipe(), 0, Y, 0);
        place(world, pipe(), 1, Y, 0);

        for (FaceTool tool : List.of(FaceTool.CROWBAR, FaceTool.WIRE_CUTTER, FaceTool.SCREWDRIVER,
                FaceTool.EMPTY_HAND)) {
            assertFalse(pipe.operateFace(world, 0, Y, 0, BlockFace.EAST, tool, null, ItemStack.EMPTY,
                    false), tool + " turned a side of a pipe");
        }
        assertEquals(0, maskOf(world, 0, Y, 0), "only the wrench turns a pipe");
    }

    @Test
    void aCellThatIsNoPipeAnyMoreTurnsNothing() {
        World world = new World(SEED, 8, 8);
        PipeBlockEntity entity = place(world, pipe(), 0, Y, 0);
        place(world, pipe(), 1, Y, 0);
        world.setBlock(0, Y, 0, Blocks.STONE);

        assertFalse(turn(world, entity, BlockFace.EAST), "a stone has no side towards a pipe");
        assertEquals(0, world.getState(0, Y, 0), "and its state stays as it is");
    }

    @Test
    void theGridOfAPipeIsAlwaysOpen() {
        World world = new World(SEED, 9, 9);
        PipeBlockEntity pipe = place(world, pipe(), 0, Y, 0);

        assertNotNull(world.blockEntity(0, Y, 0), "the pipe carries its entity");
        assertTrue(pipe.showsFaceGrid(world, 0, Y, 0),
                "a pipe offers its six sides to the wrench, whatever it is joined to");
    }
}
