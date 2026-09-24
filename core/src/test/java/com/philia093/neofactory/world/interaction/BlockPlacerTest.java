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
}
