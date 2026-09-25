package com.philia093.neofactory.world.interaction;

import com.philia093.neofactory.block.BlockFace;
import com.philia093.neofactory.block.Blocks;
import com.philia093.neofactory.entity.Player;
import com.philia093.neofactory.item.ItemStack;
import com.philia093.neofactory.item.Items;
import com.philia093.neofactory.item.PlayerInventory;
import com.philia093.neofactory.support.TestRegistries;
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

        assertTrue(BlockPlacer.place(world, player, BlockTarget.of(1, air, 1, BlockFace.WEST), inventory),
                "the side of the wall carries the block");

        assertEquals(Blocks.STONE, world.getBlock(0, air, 1), "the block was built beside the wall");
        assertEquals(Blocks.STONE, world.getBlock(1, air, 1), "and the wall is still there");
    }

    @Test
    void aBlockIsRefusedWhereNothingCanCarryIt() {
        // The cell behind the face is empty as well, so the block would hang in the air.
        assertFalse(BlockPlacer.place(world, player, BlockTarget.of(1, air, 1, BlockFace.WEST), inventory),
                "a block with nothing behind the face is refused");

        assertEquals(Blocks.AIR, world.getBlock(1, air, 1), "and nothing was written");
    }

    @Test
    void aCellTheMouseNamedStillStandsOnTheGroundBelow() {
        // A cell the mouse named knows no side, so the rule of the view from above answers: the air
        // right above the terrain is carried by the terrain.
        assertTrue(BlockPlacer.place(world, player, BlockTarget.of(2, air, 2), inventory),
                "the ground below carries it");
        assertEquals(Blocks.STONE, world.getBlock(2, air, 2));

        assertFalse(BlockPlacer.place(world, player, BlockTarget.of(2, air + 2, 2), inventory),
                "two blocks higher there is nothing to stand on");
        assertEquals(Blocks.AIR, world.getBlock(2, air + 2, 2), "and nothing was written");
    }

    @Test
    void aSlabIsBuiltIntoTheHalfTheAimEnteredThrough() {
        // A floor of its own under the target, so the case does not depend on the shape of the terrain.
        world.setBlock(5, air - 1, 5, Blocks.STONE);
        inventory.set(inventory.selectedSlot(), ItemStack.of(Items.STONE_SLAB, 8));

        // An aim that came down onto the floor: the lower half is the half the floor carries.
        assertTrue(BlockPlacer.place(world, player, BlockTarget.of(5, air, 5, BlockFace.TOP), inventory),
                "the floor carries the slab");

        assertEquals(halfState(BlockPlacer.LOWER_HALF), world.getState(5, air, 5),
                "the aim came down, so the lower half is filled");

        // An aim that went up into the cell under a ceiling: the upper half is the one it hangs on.
        world.setBlock(6, air - 1, 6, Blocks.STONE);
        world.setBlock(6, air + 1, 6, Blocks.STONE);

        assertTrue(BlockPlacer.place(world, player, BlockTarget.of(6, air, 6, BlockFace.BOTTOM), inventory),
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

        assertTrue(BlockPlacer.place(world, player, BlockTarget.of(7, air, 7, BlockFace.WEST), inventory),
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

        assertTrue(BlockPlacer.place(world, player, BlockTarget.of(1, air, 1, BlockFace.WEST), inventory),
                "the face of the wall carries the ladder");

        assertEquals(Blocks.LADDER, world.getBlock(0, air, 1), "the ladder hangs on the wall");
        assertEquals(Blocks.LADDER.states().stateOf(Map.of(BlockPlacer.FACING, BlockFace.WEST.toString())),
                world.getState(0, air, 1), "and its rungs face away from the wall, towards the builder");
    }

    @Test
    void aLadderIsRefusedWhereNoWallHoldsIt() {
        inventory.set(inventory.selectedSlot(), ItemStack.of(Items.LADDER, 8));

        assertFalse(BlockPlacer.place(world, player, BlockTarget.of(3, air, 3), inventory),
                "a ladder without a wall behind it would hang in the air");

        assertEquals(Blocks.AIR, world.getBlock(3, air, 3));
    }

    /** The state of a slab of the given half. */
    private static int halfState(String half) {
        return Blocks.STONE_SLAB.states().stateOf(Map.of(BlockPlacer.TYPE, half));
    }
}
