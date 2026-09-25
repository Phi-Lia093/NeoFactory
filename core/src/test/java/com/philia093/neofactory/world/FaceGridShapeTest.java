package com.philia093.neofactory.world;

import com.philia093.neofactory.block.BlockFace;
import com.philia093.neofactory.block.Blocks;
import com.philia093.neofactory.blockentity.BlockEntity;
import com.philia093.neofactory.blockentity.BlockEntityType;
import com.philia093.neofactory.entity.Player;
import com.philia093.neofactory.item.FaceTool;
import com.philia093.neofactory.item.ItemStack;
import com.philia093.neofactory.support.TestRegistries;
import com.philia093.neofactory.util.Aabb;
import com.philia093.neofactory.util.nbt.NbtCompound;
import com.philia093.neofactory.world.interaction.FaceOperable;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks what the grid of faces does to the shape of a cell.
 * <p>
 * While a grid is open every block that shows one is a whole cube, so a player stands on a thin pipe
 * instead of inside it - and a block that shows no grid keeps the shape of its own model. The rule lives
 * in {@link World#shape(int, int, int, Aabb)}, which is the very method the box of a body is measured
 * against, see {@link BlockAccess#overlaps(Aabb, Aabb)}, so what is checked here is the box of a cell.
 * <p>
 * The cell of the test lies far above the terrain, so nothing but the block that was put there is in the
 * way of a body.
 */
class FaceGridShapeTest {

    private static final int SEED = 4242;

    private static final int CELL_X = 4;
    private static final int CELL_Y = 200;
    private static final int CELL_Z = -7;

    /** Type of the block entity of the test, which is the one that answers for the grid. */
    private static final BlockEntityType GRID_TYPE = new BlockEntityType("test_face_grid",
            TestGridEntity::new);

    private World world;
    private TestGridEntity grid;

    @BeforeAll
    static void registerGameData() {
        TestRegistries.ensure();
    }

    @BeforeEach
    void setUp() {
        world = new World(SEED, 0, 0);
        world.setBlock(CELL_X, CELL_Y, CELL_Z, Blocks.TORCH);
        grid = new TestGridEntity(GRID_TYPE);
        grid.setPosition(CELL_X, CELL_Y, CELL_Z);
        world.addBlockEntity(grid);
    }

    @Test
    void aThinBlockIsAWholeCubeWhileAGridIsOpen() {
        Aabb shape = new Aabb();

        world.shape(CELL_X, CELL_Y, CELL_Z, shape);
        assertTrue(shape.isEmpty(), "a torch holds nothing back on its own");

        world.setFaceGrid(grid);

        world.shape(CELL_X, CELL_Y, CELL_Z, shape);
        assertEquals(CELL_X, shape.minX(), 1.0e-6f);
        assertEquals(CELL_Y, shape.minY(), 1.0e-6f);
        assertEquals(CELL_Z, shape.minZ(), 1.0e-6f);
        assertEquals(CELL_X + 1.0f, shape.maxX(), 1.0e-6f);
        assertEquals(CELL_Y + 1.0f, shape.maxY(), 1.0e-6f);
        assertEquals(CELL_Z + 1.0f, shape.maxZ(), 1.0e-6f);
    }

    @Test
    void aBodyIsStoppedByTheCubeAndNotByTheThinBlock() {
        Aabb body = Aabb.of(CELL_X + 0.2f, CELL_Y + 0.2f, CELL_Z + 0.2f,
                CELL_X + 0.8f, CELL_Y + 0.8f, CELL_Z + 0.8f);
        Aabb cell = new Aabb();

        assertFalse(world.overlaps(body, cell), "a body walks through a thin block");

        world.setFaceGrid(grid);

        assertTrue(world.overlaps(body, cell), "and is stopped by the whole cube of the grid");
    }

    @Test
    void theWholeCubeIsGivenBackWhenTheGridCloses() {
        Aabb shape = new Aabb();

        world.setFaceGrid(grid);
        world.clearFaceGrid();

        assertFalse(world.isFaceGridOpen());
        assertNull(world.faceGrid(), "a closed grid belongs to no block");
        world.shape(CELL_X, CELL_Y, CELL_Z, shape);
        assertTrue(shape.isEmpty(), "the shape of the block is back");
    }

    @Test
    void aBlockThatShowsNoGridKeepsItsOwnShape() {
        int nextToIt = CELL_X + 1;
        world.setBlock(nextToIt, CELL_Y, CELL_Z, Blocks.SAPLING_OAK);
        Aabb shape = new Aabb();

        world.setFaceGrid(grid);

        world.shape(nextToIt, CELL_Y, CELL_Z, shape);
        assertTrue(shape.isEmpty(), "a block without a grid is entered by a body as before");
    }

    @Test
    void theGridIsNotWrittenIntoASaveGame() {
        Aabb shape = new Aabb();

        world.setFaceGrid(grid);

        // The grid belongs to the session and not to the world: a file that is written while it is open
        // carries the blocks and the entities only, and the shape a body meets is the one of the blocks.
        assertEquals(1, world.modifiedChunkCount(), "only the block that was put there changed a chunk");
        world.shape(CELL_X, CELL_Y, CELL_Z, shape);
        assertEquals(CELL_Y, shape.minY(), 1.0e-6f);
    }

    /** Block entity of the test that answers for the grid of faces. */
    private static final class TestGridEntity extends BlockEntity implements FaceOperable {

        TestGridEntity(BlockEntityType type) {
            super(type);
        }

        @Override
        public boolean showsFaceGrid(World world, int x, int y, int z) {
            return true;
        }

        @Override
        public boolean operateFace(World world, int x, int y, int z, BlockFace face, FaceTool tool,
                Player player, ItemStack held) {
            return true;
        }

        @Override
        protected void update(World world, float delta) {
            // Nothing works in the block entity of the test.
        }

        @Override
        protected void writeOwnData(NbtCompound data) {
            // Nothing is written.
        }

        @Override
        protected void readOwnData(NbtCompound data) {
            // Nothing is read.
        }
    }
}
