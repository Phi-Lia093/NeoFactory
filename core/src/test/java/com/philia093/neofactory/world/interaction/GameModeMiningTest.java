package com.philia093.neofactory.world.interaction;

import com.philia093.neofactory.block.Blocks;
import com.philia093.neofactory.item.Item;
import com.philia093.neofactory.item.ItemStack;
import com.philia093.neofactory.item.Items;
import com.philia093.neofactory.support.TestRegistries;
import com.philia093.neofactory.world.GameMode;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks how a break answers in each game mode.
 * <p>
 * A creative player owns every item of the game already, so a block goes the moment it is hit and leaves
 * nothing behind, while a survival break takes the hardness, the mining level and the speed of the held tool
 * into account, see {@link HardnessMining}. One block is the same in both modes: what cannot be broken stays
 * where it is. The mode is read while a break runs, so {@code /gamemode} takes effect on the next hit.
 */
class GameModeMiningTest {

    /** Mode the world is played in, switched by a case while it runs. */
    private GameMode mode = GameMode.SURVIVAL;

    /** Rule under test, which reads that mode on every break. */
    private final MiningRule rule = new GameModeMining(() -> mode, new HardnessMining());

    @BeforeAll
    static void registerGameData() {
        TestRegistries.ensure();
    }

    @Test
    void creativeBreaksEveryBlockAtOnce() {
        mode = GameMode.CREATIVE;

        assertEquals(0.0f, rule.breakSeconds(Blocks.STONE, ItemStack.EMPTY),
                "a bare hand breaks stone in creative mode");
        assertEquals(0.0f, rule.breakSeconds(Blocks.OBSIDIAN, ItemStack.of(Items.DIAMOND_PICKAXE, 1)),
                "and obsidian needs no tool at all");
    }

    @Test
    void creativeLeavesNothingBehind() {
        mode = GameMode.CREATIVE;

        assertFalse(rule.canHarvest(Blocks.STONE, ItemStack.of(Items.DIAMOND_PICKAXE, 1)),
                "a player who owns every item collects nothing from a block");
    }

    @Test
    void survivalAsksForTheTimeOfTheBlock() {
        assertEquals(Blocks.STONE.hardness() * 1.5f / Items.IRON_TOOL_SPEED,
                rule.breakSeconds(Blocks.STONE, ItemStack.of(Items.IRON_PICKAXE, 1)), 1.0e-5f,
                "a block takes hardness * 1.5 / speed with a tool that may harvest it");
    }

    @Test
    void survivalLeavesNothingBehindForAToolThatIsTooWeak() {
        assertTrue(rule.canHarvest(Blocks.STONE, ItemStack.of(Items.IRON_PICKAXE, 1)),
                "an iron pickaxe reaches the mining level of stone");
        assertFalse(rule.canHarvest(Blocks.OBSIDIAN, ItemStack.of(Items.IRON_PICKAXE, 1)),
                "obsidian asks for the level of a diamond tool");
        assertTrue(rule.canHarvest(Blocks.OBSIDIAN, ItemStack.of(Items.DIAMOND_PICKAXE, 1)));
    }

    @Test
    void nothingBreaksWhatCannotBeBroken() {
        ItemStack pickaxe = ItemStack.of(Items.DIAMOND_PICKAXE, 1);
        for (GameMode played : GameMode.values()) {
            mode = played;
            assertTrue(rule.breakSeconds(Blocks.BEDROCK, pickaxe) < 0.0f,
                    "bedrock stays in " + played.modeName() + " mode");
        }
    }

    @Test
    void survivalAsksForTheKindOfTheToolAsWell() {
        ItemStack axe = ItemStack.of(Items.IRON_AXE, 1);

        assertEquals(Blocks.LOG_OAK.hardness() * 1.5f / Items.IRON_TOOL_SPEED,
                rule.breakSeconds(Blocks.LOG_OAK, axe), 1.0e-5f,
                "an axe is the kind of tool a trunk is worked with");
        assertEquals(Blocks.STONE.hardness() * 5.0f / Item.HAND_MINING_SPEED,
                rule.breakSeconds(Blocks.STONE, axe), 1.0e-5f,
                "while stone is no faster under an axe than under a bare hand");
        assertFalse(rule.canHarvest(Blocks.STONE, axe),
                "and stone hands its item to the kind of tool it names and to nothing else");
    }

    @Test
    void theModeIsReadOnEveryBreak() {
        ItemStack pickaxe = ItemStack.of(Items.DIAMOND_PICKAXE, 1);
        assertTrue(rule.canHarvest(Blocks.STONE, pickaxe), "in survival the block hands its item over");

        mode = GameMode.CREATIVE;

        assertFalse(rule.canHarvest(Blocks.STONE, pickaxe),
                "and in creative it is broken the moment it is hit and hands nothing over");
        assertEquals(0.0f, rule.breakSeconds(Blocks.STONE, pickaxe));
    }
}
