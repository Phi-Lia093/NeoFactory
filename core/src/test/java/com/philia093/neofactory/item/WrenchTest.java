package com.philia093.neofactory.item;

import com.philia093.neofactory.gui.creative.CreativeRegistry;
import com.philia093.neofactory.gui.creative.CreativeTab;
import com.philia093.neofactory.support.TestRegistries;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the wrench: the tool of the workshop, the one a player builds the industry with.
 * <p>
 * The wrench is the first item of the game that is held at a face instead of being swung at a block, see
 * {@link FaceTool}: it opens no block of its own and shows the grid of nine cells on whatever a player
 * looks at, which is what turns a side of a pipe, see {@code PipeBlockEntity}. This test keeps its number,
 * its kind and its picture where they belong - an item that a stored inventory can hold has to keep them.
 */
class WrenchTest {

    @BeforeAll
    static void registerGameData() {
        TestRegistries.ensure();
    }

    /** The tab of the creative inventory that lists the tools. */
    private static CreativeTab tools() {
        for (CreativeTab tab : CreativeRegistry.tabs()) {
            if (tab.name().equals("tools")) {
                return tab;
            }
        }
        throw new AssertionError("the creative inventory has no tab of tools");
    }

    @Test
    void theWrenchTakesANumberThatStoodFree() {
        assertEquals(Items.WRENCH_ID, Items.WRENCH.id());
        assertEquals(88, Items.WRENCH.id(),
                "the first number of the window between the boiler and the pipes");
        assertTrue(Items.WRENCH_ID < Items.PIPE_ID_FROM,
                "the wrench stands before the run of the pipes, which is where the materials follow");
        assertEquals(Items.WRENCH, ItemRegistry.byId(Items.WRENCH_ID));
        assertEquals(Items.WRENCH, ItemRegistry.byName("wrench"));
    }

    @Test
    void theWrenchIsTheToolOfAFace() {
        assertTrue(Items.WRENCH.isTool(), "the workshop tool is a tool");
        assertEquals(ToolType.WRENCH, Items.WRENCH.toolType());
        assertEquals(FaceTool.WRENCH, Items.WRENCH.faceTool(), "it addresses a face, not a block");
        assertEquals(FaceTool.WRENCH, FaceTool.of(ItemStack.of(Items.WRENCH, 1), false),
                "a player who holds it addresses the face they look at");
        assertEquals(Item.SINGLE_ITEM_STACK, Items.WRENCH.maxStackSize(), "a tool is one piece");
        assertEquals(Items.WRENCH_DURABILITY, Items.WRENCH.maxDamage());
    }

    @Test
    void theWrenchOpensNoBlockOfItsOwn() {
        assertEquals(Items.HAND_TOOL_LEVEL, Items.WRENCH.toolLevel(), "it works as well as a bare hand");
        assertEquals(Item.HAND_MINING_SPEED, Items.WRENCH.miningSpeed(), 1.0e-6f,
                "and no faster than one: the work it is made for is the turn of a face");
    }

    @Test
    void theWrenchStandsWithTheToolsOfTheCreativeInventory() {
        assertTrue(tools().matches(Items.WRENCH), "the tab of the tools holds the wrench");
        assertFalse(tools().matches(Items.STICK), "and a material is no tool");
        assertFalse(tools().matches(Items.BRONZE_BOILER), "neither is a machine");
    }
}
