package com.philia093.neofactory.entity;

import com.philia093.neofactory.block.Block;
import com.philia093.neofactory.block.Blocks;
import com.philia093.neofactory.support.TestRegistries;
import com.philia093.neofactory.util.Aabb;
import com.philia093.neofactory.world.BlockAccess;
import com.philia093.neofactory.world.World;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks what a body of the world runs into and what it rests on.
 * <p>
 * A block of the world is not always a whole cube. A slab fills the lower or the upper half of its cell
 * and an anvil a body of its own, so the collision of a cell is the shape of the model it is drawn with,
 * see {@link BlockAccess#shape(int, int, int, Aabb)}. The rule the whole code rests on is the half open
 * one: a body that touches a block is not inside it, which is what lets a body rest on the very top face
 * of what it stands on and on the top of a half filled cell at the same time.
 * <p>
 * Everything here is arithmetic on boxes: no window and no graphics card is involved.
 */
class PlayerShapeTest {

    /** Seed the cases are built with; any world has ground under its spawn. */
    private static final int SEED = 777;

    /** Height of an anvil, sixteen sixteenths of a block: its plate reaches the top of the cell. */
    private static final float ANVIL_HEIGHT = 1.0f;

    @BeforeAll
    static void registerGameData() {
        TestRegistries.ensure();
    }

    @Test
    void aBodyIsStoppedByTheHalfASlabFills() {
        World world = flatWorld();
        int x = world.spawnX();
        int z = world.spawnZ();
        Player player = Player.spawnOnGround(world, x, z);
        int cell = world.surfaceY(x, z);
        fill(world, x, cell, z, Blocks.STONE_SLAB, Map.of("type", "bottom"));

        assertTrue(player.collides(world, x + 0.5f, cell, z + 0.5f),
                "the half a slab fills is a wall a body walks into");
        assertFalse(player.collides(world, x + 0.5f, cell + 0.5f, z + 0.5f),
                "and its top carries the body, because touching is not overlapping");
    }

    @Test
    void aBodyIsStoppedByTheHalfOfASlabAboveIt() {
        World world = flatWorld();
        int x = world.spawnX();
        int z = world.spawnZ();
        Player player = Player.spawnOnGround(world, x, z);
        int cell = world.surfaceY(x, z);
        fill(world, x, cell, z, Blocks.STONE_SLAB, Map.of("type", "top"));

        assertTrue(player.collides(world, x + 0.5f, cell, z + 0.5f),
                "the upper half of the cell is filled, so no body fits into it");
        assertTrue(player.collides(world, x + 0.5f, cell + 0.5f, z + 0.5f),
                "and a body that tries to stand at the height of the plate is stopped as well");
    }

    @Test
    void aBodyStepsOntoThePlateOfAnAnvil() {
        World world = flatWorld();
        int x = world.spawnX();
        int z = world.spawnZ();
        Player player = Player.spawnOnGround(world, x, z);
        int cell = world.surfaceY(x, z);
        fill(world, x, cell, z, Blocks.ANVIL, Map.of("facing", "north"));

        assertTrue(player.collides(world, x + 0.5f, cell, z + 0.5f),
                "the body of an anvil is filled as well");
        assertFalse(player.collides(world, x + 0.5f, cell + ANVIL_HEIGHT, z + 0.5f),
                "the plate of the anvil carries a body that stands on it");
    }

    @Test
    void aBodyThatLandsOnASlabComesToRest() {
        World world = flatWorld();
        int x = world.spawnX();
        int z = world.spawnZ();
        Player player = Player.spawnOnGround(world, x, z);
        int cell = world.surfaceY(x, z);
        fill(world, x, cell, z, Blocks.STONE_SLAB, Map.of("type", "bottom"));
        player.position().y = cell + 3.0f;

        for (int frame = 0; frame < 40; frame++) {
            player.update(world, 1.0f / 20.0f);
        }

        assertEquals(cell + 0.5f, player.position().y, 1.0e-3f, "the feet rest on the top of the slab");
        assertTrue(player.isOnGround(), "and the body has landed");

        for (int frame = 0; frame < 40; frame++) {
            player.update(world, 1.0f / 20.0f);
        }

        assertEquals(cell + 0.5f, player.position().y, 1.0e-3f,
                "the body stays where it landed instead of shaking on the spot");
    }

    @Test
    void everythingThatIsNotSolidIsWalkedThrough() {
        World world = flatWorld();
        int x = world.spawnX();
        int z = world.spawnZ();
        Player player = Player.spawnOnGround(world, x, z);
        int cell = world.surfaceY(x, z);
        fill(world, x, cell, z, Blocks.SAPLING_OAK, Map.of());

        assertFalse(player.collides(world, x + 0.5f, cell, z + 0.5f),
                "a plant is drawn as two planes and holds nothing back");

        fill(world, x, cell, z, Blocks.STONE, Map.of());

        assertTrue(player.collides(world, x + 0.5f, cell, z + 0.5f),
                "a whole cube of stone fills its cell, which is what the shape of its model says");
    }

    @Test
    void theShapeOfAStateIsTheBoxOfItsModel() {
        Aabb box = new Aabb();

        // A slab is half a cell, and which half it fills is the state of the cell, not the block.
        Blocks.STONE_SLAB.shape(slabState("bottom"), box);
        assertEquals(0.0f, box.minY(), 1.0e-6f, "a lower slab starts at the floor of its cell");
        assertEquals(0.5f, box.maxY(), 1.0e-6f, "and reaches halfway up");
        assertEquals(0.0f, box.minX(), 1.0e-6f, "it fills the cell from side to side");
        assertEquals(1.0f, box.maxX(), 1.0e-6f);

        Blocks.STONE_SLAB.shape(slabState("top"), box);
        assertEquals(0.5f, box.minY(), 1.0e-6f, "an upper slab starts halfway up its cell");
        assertEquals(1.0f, box.maxY(), 1.0e-6f, "and reaches the ceiling");

        // An anvil is a shape of its own, and its base is inset while its plate is not.
        Blocks.ANVIL.shape(Blocks.ANVIL.states().stateOf(Map.of("facing", "north")), box);
        assertEquals(0.0f, box.minY(), 1.0e-6f);
        assertEquals(ANVIL_HEIGHT, box.maxY(), 1.0e-6f, "the plate of the anvil reaches the top");
        assertEquals(2.0f / 16.0f, box.minX(), 1.0e-6f, "the base of the anvil is inset");
        assertEquals(0.0f, box.minZ(), 1.0e-6f, "while its plate reaches the edge of the cell");

        // A block that carries no state of its own is still the cube its picture is, which is what the
        // world was before a block could be thinner than a cell.
        Blocks.STONE.shape(0, box);
        assertEquals(0.0f, box.minY(), 1.0e-6f);
        assertEquals(1.0f, box.maxY(), 1.0e-6f);
    }

    /** A flat world with its spawn in the middle and no decoration to trip over. */
    private static World flatWorld() {
        return new World(SEED, 0, 0);
    }

    /** Fills one cell of the object layer above the ground with a block of the given state. */
    private static void fill(World world, int x, int y, int z, Block block,
            Map<String, String> state) {
        world.setBlock(x, y, z, block);
        world.setState(x, y, z, state.isEmpty() ? 0 : block.states().stateOf(state));
    }

    /** The state of a slab of the given half. */
    private static int slabState(String half) {
        return Blocks.STONE_SLAB.states().stateOf(Map.of("type", half));
    }
}
