package com.philia093.neofactory.world.interaction;

import com.philia093.neofactory.block.Block;
import com.philia093.neofactory.block.BlockFace;
import com.philia093.neofactory.block.Blocks;
import com.philia093.neofactory.item.Item;
import com.philia093.neofactory.item.ItemDrops;
import com.philia093.neofactory.item.ItemRegistry;
import com.philia093.neofactory.item.ItemStack;
import com.philia093.neofactory.item.ItemWear;
import com.philia093.neofactory.item.Items;
import com.philia093.neofactory.loot.LootTable;
import com.philia093.neofactory.loot.LootTableLoader;
import com.philia093.neofactory.loot.LootTableRegistry;
import com.philia093.neofactory.support.TestRegistries;
import com.philia093.neofactory.util.Constants;
import com.philia093.neofactory.world.GameMode;
import com.philia093.neofactory.world.World;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

    /** Tools that were used up while breaking, see {@link ItemWear}. */
    private final List<ItemStack> wornOut = new ArrayList<>();

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
        wornOut.clear();
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

    @Test
    void aBlockNamesWhatItLeavesBehind() {
        // A stone hands over cobblestone, which is what its loot table says and not what the block is.
        useSurvival(Blocks.STONE, LootTableLoader.parse("stone",
                "{ \"drops\": [ { \"item\": \"cobblestone\" } ] }"));
        world.setBlock(5, air, 5, Blocks.STONE);

        assertTrue(hold(BlockTarget.of(5, air, 5, BlockFace.TOP), HOLD_SECONDS,
                ItemStack.of(Items.DIAMOND_PICKAXE, 1)));
        assertEquals(1, collected.size(), "one line of the table, one stack");
        assertEquals(ItemRegistry.byBlock(Blocks.COBBLESTONE), collected.get(0).item());
        assertEquals(1, collected.get(0).count());
    }

    @Test
    void aTableThatNamesNothingLeavesNothingBehind() {
        // Glass breaks into nothing, which is a table with an empty list and not the same as no table:
        // a block without a table hands over itself.
        useSurvival(Blocks.GLASS, LootTableLoader.parse("glass", "{ \"drops\": [] }"));
        world.setBlock(6, air, 6, Blocks.GLASS);

        assertTrue(breakCell(BlockTarget.of(6, air, 6, BlockFace.TOP)));
        assertEquals(Blocks.AIR, world.getBlock(6, air, 6), "the block goes either way");
        assertTrue(collected.isEmpty(), "and it hands nothing over");
    }

    @Test
    void aBlockWithoutATableHandsOverItself() {
        world.setBlock(7, air, 7, Blocks.SAND);

        assertTrue(breakCell(BlockTarget.of(7, air, 7, BlockFace.TOP)));
        assertEquals(1, collected.size());
        assertEquals(ItemRegistry.byBlock(Blocks.SAND), collected.get(0).item());
    }

    @Test
    void aToolThatIsTooWeakBreaksTheBlockAndLeavesNothing() {
        // Stone asks for mining level 1, so a bare hand may break it - five times as slow as a pickaxe -
        // and it leaves nothing behind, see HardnessMining.
        mining = new MiningController(new HardnessMining(), (stack, worldX, worldZ) -> collected.add(stack),
                LootTableRegistry::byBlock);
        world.setBlock(8, air, 8, Blocks.STONE);
        BlockTarget target = BlockTarget.of(8, air, 8, BlockFace.TOP);
        // 1.5 blocks of hardness take 1.5 * 5 seconds with a bare hand and 1.5 * 1.5 / 8 with a pickaxe.
        float byHand = Blocks.STONE.hardness() * 5.0f;

        assertTrue(hold(target, byHand, ItemStack.EMPTY), "the block comes apart under a bare hand too");
        assertEquals(Blocks.AIR, world.getBlock(8, air, 8));
        assertTrue(collected.isEmpty(), "it only leaves nothing behind");

        world.setBlock(8, air, 8, Blocks.STONE);
        assertTrue(hold(target, HOLD_SECONDS, ItemStack.of(Items.DIAMOND_PICKAXE, 1)),
                "the tool of the right level takes it");
        assertEquals(1, collected.size());
        assertEquals(ItemRegistry.byBlock(Blocks.COBBLESTONE), collected.get(0).item(),
                "and it hands over what the table of stone says");
    }

    @Test
    void aHardBlockTakesTheTimeTheRuleAsksFor() {
        mining = new MiningController(new HardnessMining(), (stack, worldX, worldZ) -> collected.add(stack),
                LootTableRegistry::byBlock);
        world.setBlock(9, air, 9, Blocks.STONE);
        BlockTarget target = BlockTarget.of(9, air, 9, BlockFace.TOP);
        ItemStack pickaxe = ItemStack.of(Items.DIAMOND_PICKAXE, 1);
        // Stone of hardness 1.5 needs 1.5 * 1.5 / 8 seconds of a diamond pickaxe, see HardnessMining.
        float needed = Blocks.STONE.hardness() * 1.5f / Items.DIAMOND_TOOL_SPEED;

        assertFalse(hold(target, needed * 0.5f, pickaxe), "half of the time is not enough for stone");
        assertEquals(Blocks.STONE, world.getBlock(9, air, 9));
        assertFalse(hold(target, needed * 0.25f, pickaxe), "and the collected time keeps growing");
        assertTrue(hold(target, needed, pickaxe), "the rest of the time is");
        assertEquals(1, collected.size());
    }

    @Test
    void aClickInCreativeBreaksTheBlockRightAway() {
        GameMode mode = GameMode.CREATIVE;
        mining = new MiningController(new GameModeMining(() -> mode, new HardnessMining()),
                (stack, worldX, worldZ) -> collected.add(stack), LootTableRegistry::byBlock);
        world.setBlock(10, air, 10, Blocks.OBSIDIAN);

        assertTrue(hold(BlockTarget.of(10, air, 10, BlockFace.TOP), 1.0f / 60.0f, ItemStack.EMPTY),
                "one frame of holding the button is a click, and a click breaks the block");
        assertEquals(Blocks.AIR, world.getBlock(10, air, 10));
        assertTrue(collected.isEmpty(), "and a creative player collects nothing");
    }

    @Test
    void theBlocksOfOnePressWaitBetweenEachOther() {
        // A rule that needs no time at all: without the floor a held button would eat a row of blocks in
        // one frame, so the second block of the same press waits, while the first one goes right away.
        world.setBlock(11, air, 11, Blocks.GRAVEL);
        world.setBlock(12, air, 12, Blocks.GRAVEL);
        float frame = 1.0f / 60.0f;

        assertTrue(hold(BlockTarget.of(11, air, 11, BlockFace.TOP), frame, ItemStack.EMPTY),
                "the first block of a press goes at once");
        assertFalse(hold(BlockTarget.of(12, air, 12, BlockFace.TOP), frame, ItemStack.EMPTY),
                "the next block waits the minimum of a break");
        assertTrue(hold(BlockTarget.of(12, air, 12, BlockFace.TOP), Constants.MINIMUM_BREAK_TIME,
                ItemStack.EMPTY), "and then it goes as well");
    }

    @Test
    void aHarvestedBlockCostsTheToolOneUse() {
        useSurvivalWithWear();
        world.setBlock(13, air, 13, Blocks.STONE);
        ItemStack pickaxe = ItemStack.of(Items.DIAMOND_PICKAXE, 1);

        assertTrue(hold(BlockTarget.of(13, air, 13, BlockFace.TOP), HOLD_SECONDS, pickaxe));
        assertEquals(1, pickaxe.damage(), "the block took one use from the tool");
        assertEquals(Items.DIAMOND_TOOL_DURABILITY - 1, pickaxe.remainingDamage());
        assertTrue(wornOut.isEmpty(), "the pickaxe still has a life ahead of it");
    }

    @Test
    void aBlockThatMayNotBeHarvestedCostsNothing() {
        // Stone wants a pickaxe, so an axe breaks it - five times as slow, without its item and without
        // costing the axe anything, which is what the original game does as well.
        useSurvivalWithWear();
        world.setBlock(14, air, 14, Blocks.STONE);
        ItemStack axe = ItemStack.of(Items.IRON_AXE, 1);
        float byAnAxe = Blocks.STONE.hardness() * 5.0f / Item.HAND_MINING_SPEED;

        assertTrue(hold(BlockTarget.of(14, air, 14, BlockFace.TOP), byAnAxe, axe),
                "a block comes apart under any tool, only slower");
        assertTrue(collected.isEmpty(), "the block leaves nothing behind");
        assertEquals(0, axe.damage(), "and it cost the axe nothing");
    }

    @Test
    void aToolThatIsUsedUpIsReportedAndBreaksOneLastBlock() {
        useSurvivalWithWear();
        world.setBlock(15, air, 15, Blocks.DIRT);
        ItemStack shovel = ItemStack.of(Items.IRON_SHOVEL, 1);
        shovel.setDamage(Items.IRON_TOOL_DURABILITY - 1);

        assertTrue(hold(BlockTarget.of(15, air, 15, BlockFace.TOP), HOLD_SECONDS, shovel),
                "the last use still breaks the block");
        assertTrue(shovel.isBroken(), "and it is what used the shovel up");
        assertEquals(List.of(shovel), wornOut, "which is reported to the sink of worn out tools");
    }

    @Test
    void aBareHandWearsNothing() {
        useSurvivalWithWear();
        world.setBlock(16, air, 16, Blocks.DIRT);

        assertTrue(hold(BlockTarget.of(16, air, 16, BlockFace.TOP), HOLD_SECONDS, ItemStack.EMPTY));
        assertTrue(wornOut.isEmpty(), "a hand is not a piece that wears out");
    }

    /** Holds the button on one cell for a given time with a given tool. */
    private boolean hold(BlockTarget target, float seconds, ItemStack tool) {
        return mining.update(seconds, world, target, tool, true);
    }

    /** Makes the controller run in survival mode and hand over what the table of one block says. */
    private void useSurvival(Block block, LootTable table) {
        mining = new MiningController(new HardnessMining(), (stack, worldX, worldZ) -> collected.add(stack),
                broken -> broken == block ? table : null);
    }

    /** Makes the controller run in survival mode, with the loot tables of the game and a wear sink. */
    private void useSurvivalWithWear() {
        ItemWear wear = wornOut::add;
        mining = new MiningController(new HardnessMining(), (stack, worldX, worldZ) -> collected.add(stack),
                LootTableRegistry::byBlock, wear);
    }

    /** Holds the button on one cell for {@link #HOLD_SECONDS}. */
    private boolean breakCell(BlockTarget target) {
        return mining.update(HOLD_SECONDS, world, target, ItemStack.EMPTY, true);
    }
}
