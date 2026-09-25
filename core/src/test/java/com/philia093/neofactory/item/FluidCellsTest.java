package com.philia093.neofactory.item;

import com.badlogic.gdx.graphics.Color;
import com.philia093.neofactory.fluid.Fluid;
import com.philia093.neofactory.fluid.Fluids;
import com.philia093.neofactory.support.TestRegistries;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the table that maps a fluid to the cell that carries it.
 * <p>
 * The table is spelled out by hand, so it is checked from both sides: every fluid the game knows has a
 * cell that carries it, a fluid without one reports so instead of handing over the wrong item, and the
 * cell of a fluid draws the one grey scale picture of the game in the colour of its fluid.
 * <p>
 * The game has one container and no bucket: a bucket belonged to the fluids that stood in the world as
 * a block, and those are gone.
 */
class FluidCellsTest {

    /** A fluid of the industry, one the game has no cell for yet. */
    private static final Fluid OIL = oilOfTheIndustry();

    /**
     * Builds a fluid of the industry.
     * <p>
     * The tables of the game are filled first, because the cells are registered with every other item.
     *
     * @return the fluid
     */
    private static Fluid oilOfTheIndustry() {
        TestRegistries.ensure();
        return new Fluid("oil", new Color(0.1f, 0.1f, 0.1f, 1.0f));
    }

    @BeforeAll
    static void register() {
        TestRegistries.ensure();
    }

    @Test
    void theEmptyCellIsTheFormWithoutAFluid() {
        assertEquals(Items.FLUID_CELL, FluidCells.empty());
    }

    @Test
    void everyFluidOfTheGameHasACell() {
        for (Fluid fluid : Fluids.all()) {
            Item cell = FluidCells.filled(fluid);

            assertNotNull(cell, "the cell of " + fluid.name());
            assertTrue(cell.container().holds(fluid), "and it carries that fluid");
            assertEquals(Color.WHITE, cell.tint(),
                    "the colour is painted into the window of the cell, not over the whole item");
            assertEquals(Items.FLUID_CELL.texture(), cell.texture(),
                    "every cell draws the one picture of the game");
        }
    }

    @Test
    void aFluidOfTheIndustryHasNoCellYet() {
        assertNull(FluidCells.filled(OIL),
                "a fluid that arrives without an item says so instead of handing over another one");
    }
}
