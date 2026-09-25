package com.philia093.neofactory.world.interaction;

import com.philia093.neofactory.block.Blocks;
import com.philia093.neofactory.item.Item;
import com.philia093.neofactory.item.ItemStack;
import com.philia093.neofactory.item.Items;
import com.philia093.neofactory.support.TestRegistries;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks how the kind and the level of a tool decide a break.
 * <p>
 * The <b>kind</b> of a tool is what makes a pickaxe quick on stone while an axe is no faster there
 * than a bare hand, and the <b>level</b> of it is what decides whether a block hands its item over.
 * A block that asks for nothing - soil, sand, a canopy - is worked by a hand and by every kind alike;
 * a block that names a kind but asks for no level is harvested by everyone, while only the right kind
 * is quick about it; and a block that asks for a level hands its item to the kind it names and to
 * nothing else, however strong that something else is.
 */
class HardnessMiningTest {

    /** Rule under test. */
    private final MiningRule rule = new HardnessMining();

    @BeforeAll
    static void registerGameData() {
        TestRegistries.ensure();
    }

    @Test
    void theKindOfTheToolDecidesTheSpeed() {
        float stone = Blocks.STONE.hardness();

        assertEquals(stone * 1.5f / Items.IRON_TOOL_SPEED,
                rule.breakSeconds(Blocks.STONE, ItemStack.of(Items.IRON_PICKAXE, 1)), 1.0e-5f,
                "a pickaxe is the kind stone is worked with");
        assertEquals(stone * 5.0f / Item.HAND_MINING_SPEED,
                rule.breakSeconds(Blocks.STONE, ItemStack.of(Items.IRON_AXE, 1)), 1.0e-5f,
                "and an axe is no faster on it than a bare hand");
        assertEquals(stone * 5.0f / Item.HAND_MINING_SPEED,
                rule.breakSeconds(Blocks.STONE, ItemStack.of(Items.DIAMOND_SWORD, 1)), 1.0e-5f,
                "a sword cuts what stands and mines nothing");
    }

    @Test
    void aTrunkIsWorkedWithAnAxe() {
        float trunk = Blocks.LOG_OAK.hardness();

        assertEquals(trunk * 1.5f / Items.DIAMOND_TOOL_SPEED,
                rule.breakSeconds(Blocks.LOG_OAK, ItemStack.of(Items.DIAMOND_AXE, 1)), 1.0e-5f,
                "a trunk wants an axe");
        assertEquals(trunk * 1.5f / Item.HAND_MINING_SPEED,
                rule.breakSeconds(Blocks.LOG_OAK, ItemStack.EMPTY), 1.0e-5f,
                "a bare hand still takes it, only slower");
    }

    @Test
    void soilIsQuickWithAShovelAndSlowWithEverythingElse() {
        float soil = Blocks.DIRT.hardness();

        assertEquals(soil * 1.5f / Items.IRON_TOOL_SPEED,
                rule.breakSeconds(Blocks.DIRT, ItemStack.of(Items.IRON_SHOVEL, 1)), 1.0e-5f);
        assertEquals(soil * 1.5f / Item.HAND_MINING_SPEED,
                rule.breakSeconds(Blocks.DIRT, ItemStack.of(Items.IRON_PICKAXE, 1)), 1.0e-5f,
                "a pickaxe shovels no soil");
    }

    @Test
    void aBlockWithALevelWantsTheRightKindOfToolAtTheRightLevel() {
        assertTrue(rule.canHarvest(Blocks.STONE, ItemStack.of(Items.IRON_PICKAXE, 1)));
        assertTrue(rule.canHarvest(Blocks.STONE, ItemStack.of(Items.DIAMOND_PICKAXE, 1)));
        assertFalse(rule.canHarvest(Blocks.STONE, ItemStack.of(Items.IRON_AXE, 1)),
                "the level of stone is reached by a pickaxe and not by another kind of tool");
        assertFalse(rule.canHarvest(Blocks.STONE, ItemStack.of(Items.DIAMOND_SWORD, 1)),
                "a good sword is still no pickaxe");
        assertFalse(rule.canHarvest(Blocks.STONE, ItemStack.EMPTY), "and a bare hand reaches nothing");
        assertFalse(rule.canHarvest(Blocks.OBSIDIAN, ItemStack.of(Items.IRON_PICKAXE, 1)),
                "obsidian asks for the level of a diamond pickaxe");
        assertTrue(rule.canHarvest(Blocks.OBSIDIAN, ItemStack.of(Items.DIAMOND_PICKAXE, 1)));
    }

    @Test
    void aBlockWithoutALevelHandsItsItemToEveryone() {
        assertTrue(rule.canHarvest(Blocks.DIRT, ItemStack.EMPTY), "soil comes apart under a hand");
        assertTrue(rule.canHarvest(Blocks.SAND, ItemStack.of(Items.DIAMOND_SWORD, 1)),
                "the kind of a tool only decides the speed where no level is asked");
        assertTrue(rule.canHarvest(Blocks.LEAVES_OAK, ItemStack.EMPTY));
        assertTrue(rule.canHarvest(Blocks.LOG_OAK, ItemStack.of(Items.IRON_AXE, 1)));
    }

    @Test
    void aToolThatMayNotHarvestIsStillATool() {
        // Obsidian refuses the item of an iron pickaxe, but an iron pickaxe is what the player holds:
        // the break is five times as slow as the block asks and not as slow as a bare hand, see speedOf.
        assertEquals(Blocks.OBSIDIAN.hardness() * 5.0f / Items.IRON_TOOL_SPEED,
                rule.breakSeconds(Blocks.OBSIDIAN, ItemStack.of(Items.IRON_PICKAXE, 1)), 1.0e-4f);
    }

    @Test
    void aSoftBlockBreaksRightAwayAndAnUnbreakableOneNever() {
        assertEquals(0.0f, rule.breakSeconds(Blocks.TORCH, ItemStack.EMPTY),
                "a block of hardness zero breaks right away");
        assertTrue(rule.breakSeconds(Blocks.BEDROCK, ItemStack.of(Items.DIAMOND_PICKAXE, 1)) < 0.0f,
                "bedrock is declared unbreakable");
    }
}
