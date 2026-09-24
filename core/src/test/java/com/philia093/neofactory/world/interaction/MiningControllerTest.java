package com.philia093.neofactory.world.interaction;

import com.philia093.neofactory.block.BlockFace;
import com.philia093.neofactory.block.Blocks;
import com.philia093.neofactory.item.ItemDrops;
import com.philia093.neofactory.item.ItemStack;
import com.philia093.neofactory.item.Items;
import com.philia093.neofactory.support.TestRegistries;
import com.philia093.neofactory.world.World;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks what a break is allowed to touch.
 * <p>
 * The view from above only ever let a player name the cell over a column, and a rule that was written
 * for it asked for that cell to be empty before the one below could go: in the plane of X and Z the
 * top of a column was always air, so the rule never refused anything. A world of cubes has walls,
 * slopes and trunks, and every cell of those has something over it, so the same rule would leave
 * nothing but the surface breakable - which is what a player feels as "only the top works". A line of
 * sight names a cell from any face, and a break takes that cell and nothing else.
 */
class MiningControllerTest {

    /** Seed the cases are built with. */
    private static final int SEED = 4711;

    /** Time a case holds the button for: long enough for any rule, see {@code MINIMUM_BREAK_TIME}. */
    private static final float HOLD_SECONDS = 1.0f;

    private World world;
    private MiningController mining;

    /** Items the broken blocks handed over. */
    private final List<ItemStack> collected = new ArrayList<>();

    /** Cell above the ground of the spawn column: the air a case works in. */
    private int air;

    @BeforeAll
    static void registerGameData() {
        TestRegistries.ensure();
    }

    @BeforeEach
    void freshWorld() {
        world = new World(SEED);
        air = world.surfaceY(0, 0);
        collected.clear();
        ItemDrops drops = (stack, worldX, worldZ) -> collected.add(stack);
        mining = new MiningController(new InstantMining(), drops);
    }

    @Test
    void aBlockInsideAWallIsBrokenFromTheSide() {
        // A wall two blocks high, entered through the side of the lower block: this is the case the
        // rule of the flat view refused, because something stood on the cell that was aimed at.
        world.setBlock(1, air, 1, Blocks.STONE);
        world.setBlock(1, air + 1, 1, Blocks.STONE);

        assertTrue(breakCell(BlockTarget.of(1, air, 1, BlockFace.WEST)),
                "only the aimed cell has to go");

        assertEquals(Blocks.AIR, world.getBlock(1, air, 1), "the aimed cell is gone");
        assertEquals(Blocks.STONE, world.getBlock(1, air + 1, 1), "the block over it stays in the air");
        assertEquals(1, collected.size(), "the block handed its item over");
        assertEquals(Items.STONE, collected.get(0).item());
    }

    @Test
    void theGroundUnderAColumnIsCarriedAwayAsWell() {
        // A cell with something over it and something under it: entered through the face of the cell
        // beside it, so neither of the two is a reason to refuse.
        world.setBlock(1, air, 2, Blocks.DIRT);
        world.setBlock(1, air + 1, 2, Blocks.DIRT);

        assertTrue(breakCell(BlockTarget.of(1, air, 2, BlockFace.EAST)),
                "a cell of a slope comes apart like any other");

        assertEquals(Blocks.AIR, world.getBlock(1, air, 2));
        assertEquals(Blocks.DIRT, world.getBlock(1, air + 1, 2));
    }

    @Test
    void aBlockThatCannotBeBrokenStays() {
        world.setBlock(2, air, 2, Blocks.BEDROCK);

        assertFalse(breakCell(BlockTarget.of(2, air, 2, BlockFace.TOP)),
                "bedrock is declared unbreakable");
        assertEquals(Blocks.BEDROCK, world.getBlock(2, air, 2));
        assertTrue(collected.isEmpty(), "an unbreakable block hands nothing over");
    }

    @Test
    void nothingIsBrokenWithoutATarget() {
        world.setBlock(3, air, 3, Blocks.STONE);

        assertFalse(mining.update(HOLD_SECONDS, world, null, ItemStack.EMPTY, true));
        assertEquals(Blocks.STONE, world.getBlock(3, air, 3));
    }

    @Test
    void aReleasedButtonBreaksNothing() {
        world.setBlock(4, air, 4, Blocks.STONE);

        assertFalse(mining.update(HOLD_SECONDS, world, BlockTarget.of(4, air, 4), ItemStack.EMPTY, false));
        assertEquals(Blocks.STONE, world.getBlock(4, air, 4));
    }

    /** Holds the button on one cell for {@link #HOLD_SECONDS}. */
    private boolean breakCell(BlockTarget target) {
        return mining.update(HOLD_SECONDS, world, target, ItemStack.EMPTY, true);
    }
}
