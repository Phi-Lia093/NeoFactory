package com.philia093.neofactory.world.interaction;

import com.philia093.neofactory.block.BlockFace;
import com.philia093.neofactory.block.Blocks;
import com.philia093.neofactory.entity.Player;
import com.philia093.neofactory.item.ItemStack;
import com.philia093.neofactory.item.Items;
import com.philia093.neofactory.item.PlayerInventory;
import com.philia093.neofactory.pipe.PipeMaterials;
import com.philia093.neofactory.pipe.PipeSize;
import com.philia093.neofactory.pipe.Pipes;
import com.philia093.neofactory.support.TestRegistries;
import com.philia093.neofactory.world.GameMode;
import com.philia093.neofactory.world.World;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks what a build is allowed to hold on to.
 * <p>
 * A block is attached to whatever it is built against, and a body that stands in the world brings the
 * side it aimed at with it: a wall is built against and a bridge grows sideways from the block it is
 * laid on. The rule of the view from above - the ground below the cell - is only for a cell the mouse
 * named, which knows no side. The difference is the whole point of these cases: without it a player
 * could build on the ground and nowhere else, which is what a world of cubes must not do.
 */
class BlockPlacerTest {

    /** Seed the cases are built with. */
    private static final int SEED = 777;

    private World world;
    private Player player;
    private PlayerInventory inventory;

    /** Cell above the ground of the spawn column: the air a case builds in. */
    private int air;

    @BeforeAll
    static void registerGameData() {
        TestRegistries.ensure();
    }

    @BeforeEach
    void freshWorld() {
        world = new World(SEED);
        air = world.surfaceY(0, 0);
        // The body stands a few blocks away, so a case never builds into itself.
        player = Player.spawnOnGround(world, 4, 4);
        inventory = new PlayerInventory();
        inventory.set(inventory.selectedSlot(), ItemStack.of(Items.STONE, 8));
    }

    @Test
    void aBlockIsBuiltAgainstTheSideOfAWallHangingInTheAir() {
        // A wall that stands on nothing, one block above the terrain: only the side of it can carry
        // anything, and that is exactly what the aim of a body brings along.
        world.setBlock(1, air, 1, Blocks.STONE);

        assertTrue(BlockPlacer.place(world, player, BlockTarget.of(1, air, 1, BlockFace.WEST), inventory, GameMode.SURVIVAL),
                "the side of the wall carries the block");

        assertEquals(Blocks.STONE, world.getBlock(0, air, 1), "the block was built beside the wall");
        assertEquals(Blocks.STONE, world.getBlock(1, air, 1), "and the wall is still there");
    }

    @Test
    void aBlockIsRefusedWhereNothingCanCarryIt() {
        // The cell behind the face is empty as well, so the block would hang in the air.
        assertFalse(BlockPlacer.place(world, player, BlockTarget.of(1, air, 1, BlockFace.WEST), inventory, GameMode.SURVIVAL),
                "a block with nothing behind the face is refused");

        assertEquals(Blocks.AIR, world.getBlock(1, air, 1), "and nothing was written");
    }

    @Test
    void aCellTheMouseNamedStillStandsOnTheGroundBelow() {
        // A cell the mouse named knows no side, so the rule of the view from above answers: the air
        // right above the terrain is carried by the terrain.
        assertTrue(BlockPlacer.place(world, player, BlockTarget.of(2, air, 2), inventory, GameMode.SURVIVAL),
                "the ground below carries it");
        assertEquals(Blocks.STONE, world.getBlock(2, air, 2));

        assertFalse(BlockPlacer.place(world, player, BlockTarget.of(2, air + 2, 2), inventory, GameMode.SURVIVAL),
                "two blocks higher there is nothing to stand on");
        assertEquals(Blocks.AIR, world.getBlock(2, air + 2, 2), "and nothing was written");
    }

    @Test
    void aSlabIsBuiltIntoTheHalfTheAimEnteredThrough() {
        // A floor of its own under the target, so the case does not depend on the shape of the terrain.
        world.setBlock(5, air - 1, 5, Blocks.STONE);
        inventory.set(inventory.selectedSlot(), ItemStack.of(Items.STONE_SLAB, 8));

        // An aim that came down onto the floor: the lower half is the half the floor carries.
        assertTrue(BlockPlacer.place(world, player, BlockTarget.of(5, air, 5, BlockFace.TOP), inventory, GameMode.SURVIVAL),
                "the floor carries the slab");

        assertEquals(halfState(BlockPlacer.LOWER_HALF), world.getState(5, air, 5),
                "the aim came down, so the lower half is filled");

        // An aim that went up into the cell under a ceiling: the upper half is the one it hangs on.
        world.setBlock(6, air - 1, 6, Blocks.STONE);
        world.setBlock(6, air + 1, 6, Blocks.STONE);

        assertTrue(BlockPlacer.place(world, player, BlockTarget.of(6, air, 6, BlockFace.BOTTOM), inventory, GameMode.SURVIVAL),
                "the ceiling carries the slab");

        assertEquals(halfState(BlockPlacer.UPPER_HALF), world.getState(6, air, 6),
                "the aim went up, so the upper half is filled");
    }

    @Test
    void aSlabTakesTheHalfTheViewLooksAt() {
        // The side of a block carries no half of its own, so the view answers for it.
        world.setBlock(7, air - 1, 7, Blocks.STONE);
        world.setBlock(8, air, 7, Blocks.STONE);
        inventory.set(inventory.selectedSlot(), ItemStack.of(Items.STONE_SLAB, 8));
        player.setView(0.0f, 30.0f);

        assertTrue(BlockPlacer.place(world, player, BlockTarget.of(7, air, 7, BlockFace.WEST), inventory, GameMode.SURVIVAL),
                "the wall beside it carries the slab");

        assertEquals(halfState(BlockPlacer.UPPER_HALF), world.getState(7, air, 7),
                "a player who looks up builds the upper half");
    }

    @Test
    void aLadderIsBuiltAgainstTheWallItHangsOn() {
        // A ladder goes into the cell behind the face the aim entered, and it faces the player who built it:
        // the rungs look away from the wall, which is what a body climbing it walks into.
        world.setBlock(1, air, 1, Blocks.STONE);
        inventory.set(inventory.selectedSlot(), ItemStack.of(Items.LADDER, 8));

        assertTrue(BlockPlacer.place(world, player, BlockTarget.of(1, air, 1, BlockFace.WEST), inventory, GameMode.SURVIVAL),
                "the face of the wall carries the ladder");

        assertEquals(Blocks.LADDER, world.getBlock(0, air, 1), "the ladder hangs on the wall");
        assertEquals(Blocks.LADDER.states().stateOf(Map.of(BlockPlacer.FACING, BlockFace.WEST.toString())),
                world.getState(0, air, 1), "and its rungs face away from the wall, towards the builder");
    }

    @Test
    void aLadderIsRefusedWhereNoWallHoldsIt() {
        inventory.set(inventory.selectedSlot(), ItemStack.of(Items.LADDER, 8));

        assertFalse(BlockPlacer.place(world, player, BlockTarget.of(3, air, 3), inventory, GameMode.SURVIVAL),
                "a ladder without a wall behind it would hang in the air");

        assertEquals(Blocks.AIR, world.getBlock(3, air, 3));
    }

    @Test
    void aCreativePlayerBuildsForNothing() {
        // A creative player owns every item of the game already, so a build costs nothing: the block goes
        // into the world and the stack in the hand is left exactly as it was, which is what lets a player
        // lay a whole line with the one piece they hold, see BlockPlacer#place.
        inventory.set(inventory.selectedSlot(), ItemStack.of(Items.STONE, 1));
        world.setBlock(1, air, 1, Blocks.STONE);

        assertTrue(BlockPlacer.place(world, player, BlockTarget.of(1, air, 1, BlockFace.WEST), inventory,
                GameMode.CREATIVE), "the side of the wall carries the block");
        assertTrue(BlockPlacer.place(world, player, BlockTarget.of(1, air, 1, BlockFace.SOUTH), inventory,
                GameMode.CREATIVE), "and the next one is built from the same single piece");
        assertTrue(BlockPlacer.place(world, player, BlockTarget.of(1, air, 1, BlockFace.NORTH), inventory,
                GameMode.CREATIVE), "and so is the one after that");

        assertEquals(Blocks.STONE, world.getBlock(0, air, 1), "the first block was built beside the wall");
        assertEquals(Blocks.STONE, world.getBlock(1, air, 2), "and the second one as well");
        assertEquals(Blocks.STONE, world.getBlock(1, air, 0), "and the third");
        assertEquals(1, inventory.heldStack().count(), "the one piece in the hand is still there");
    }

    @Test
    void aSurvivalPlayerPaysOneItemPerBuild() {
        // The other way round: survival is where a build costs what the player carries.
        inventory.set(inventory.selectedSlot(), ItemStack.of(Items.STONE, 2));

        assertTrue(BlockPlacer.place(world, player, BlockTarget.of(2, air, 2), inventory,
                GameMode.SURVIVAL));
        assertTrue(BlockPlacer.place(world, player, BlockTarget.of(3, air, 3), inventory,
                GameMode.SURVIVAL));
        assertTrue(inventory.heldStack().isEmpty(), "two builds used the two pieces up");
        assertFalse(BlockPlacer.place(world, player, BlockTarget.of(4, air, 4), inventory,
                GameMode.SURVIVAL), "an empty hand builds nothing");
    }

    @Test
    void aPipeIsJoinedToThePipeItWasBuiltAgainst() {
        // The one connection a player is given for free: a pipe that is put down while the eyes name a pipe of
        // its own material is joined to that very pipe, on both sides at once, see Pipes#connectOnPlacement.
        Pipes.Pipe bronze = Pipes.of(PipeMaterials.BRONZE, PipeSize.MEDIUM);
        world.setBlock(1, air, 1, bronze.block());
        inventory.set(inventory.selectedSlot(), ItemStack.of(bronze.item(), 8));

        assertTrue(BlockPlacer.place(world, player, BlockTarget.of(1, air, 1, BlockFace.WEST), inventory,
                GameMode.SURVIVAL), "the pipe was built beside the pipe it was aimed at");

        assertEquals(Pipes.mask(BlockFace.EAST), Pipes.maskOf(world.getState(0, air, 1)),
                "the new pipe joins the pipe it was built against");
        assertEquals(Pipes.mask(BlockFace.WEST), Pipes.maskOf(world.getState(1, air, 1)),
                "and that pipe joins the new one back");
    }

    @Test
    void aPipeOfAnotherMaterialIsNotJoinedByItself() {
        // The rule counts the material and nothing else: another metal waits for the wrench, whatever size the
        // pipe is, see Pipes#connectOnPlacement.
        Pipes.Pipe bronze = Pipes.of(PipeMaterials.BRONZE, PipeSize.MEDIUM);
        Pipes.Pipe steel = Pipes.of(PipeMaterials.STEEL, PipeSize.MEDIUM);
        world.setBlock(1, air, 1, steel.block());
        inventory.set(inventory.selectedSlot(), ItemStack.of(bronze.item(), 8));

        assertTrue(BlockPlacer.place(world, player, BlockTarget.of(1, air, 1, BlockFace.WEST), inventory,
                GameMode.SURVIVAL));

        assertEquals(0, Pipes.maskOf(world.getState(0, air, 1)), "another material waits for the wrench");
        assertEquals(0, Pipes.maskOf(world.getState(1, air, 1)), "on both sides");
    }

    @Test
    void aPipeOfAnotherSizeOfTheSameMaterialIsJoined() {
        // The size says how much travels through a line and not what a line is, so a tiny pipe reaches a huge
        // one of the same material by itself.
        Pipes.Pipe tiny = Pipes.of(PipeMaterials.BRONZE, PipeSize.TINY);
        Pipes.Pipe huge = Pipes.of(PipeMaterials.BRONZE, PipeSize.HUGE);
        world.setBlock(1, air, 1, huge.block());
        inventory.set(inventory.selectedSlot(), ItemStack.of(tiny.item(), 8));

        assertTrue(BlockPlacer.place(world, player, BlockTarget.of(1, air, 1, BlockFace.WEST), inventory,
                GameMode.SURVIVAL));

        assertEquals(Pipes.mask(BlockFace.EAST), Pipes.maskOf(world.getState(0, air, 1)));
        assertEquals(Pipes.mask(BlockFace.WEST), Pipes.maskOf(world.getState(1, air, 1)));
    }

    @Test
    void aPipeBuiltWithNoFaceInSightJoinsNothing() {
        Pipes.Pipe bronze = Pipes.of(PipeMaterials.BRONZE, PipeSize.MEDIUM);
        world.setBlock(1, air, 1, bronze.block());
        inventory.set(inventory.selectedSlot(), ItemStack.of(bronze.item(), 8));

        // A cell the aim named carries no face: the block goes into the named cell and there is no pipe it was
        // built against, even when the pipe stands right beside it.
        world.setBlock(0, air - 1, 1, Blocks.STONE);
        assertTrue(BlockPlacer.place(world, player, BlockTarget.of(0, air, 1), inventory, GameMode.SURVIVAL));

        assertEquals(0, Pipes.maskOf(world.getState(0, air, 1)), "the pipe stands alone");
        assertEquals(0, Pipes.maskOf(world.getState(1, air, 1)), "and its neighbour keeps its sides");
    }

    /** The state of a slab of the given half. */
    private static int halfState(String half) {
        return Blocks.STONE_SLAB.states().stateOf(Map.of(BlockPlacer.TYPE, half));
    }
}
