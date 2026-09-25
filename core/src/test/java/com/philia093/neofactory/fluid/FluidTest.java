package com.philia093.neofactory.fluid;

import com.badlogic.gdx.graphics.Color;
import com.philia093.neofactory.support.TestRegistries;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the table of the fluids and what a fluid promises about itself.
 * <p>
 * A fluid is data, so everything it claims can be read back: the name a save file stores and the colour
 * a tank and a cell are painted in. The tests need no window, which is
 * the point of keeping a fluid apart from the picture it uses.
 * <p>
 * Nothing here asks for a block: the game has no fluid block, so a fluid is a material of the industry
 * and never a cell of the world.
 */
class FluidTest {

    @BeforeAll
    static void register() {
        TestRegistries.ensure();
    }

    @Test
    void theTableKnowsTheFluidsOfTheGame() {
        assertEquals(Fluids.WATER, Fluids.byName("water"));
        assertEquals(Fluids.LAVA, Fluids.byName("lava"));
        assertEquals(Fluids.STEAM, Fluids.byName("steam"));
        assertNull(Fluids.byName("oil"), "an unknown name finds nothing");
        assertNull(Fluids.byName(null), "and so does no name at all");
        assertEquals(3, Fluids.all().size(), "two fluids of the world and the steam of the industry");
    }

    @Test
    void aFluidHasANameAndAColour() {
        for (Fluid fluid : Fluids.all()) {
            assertFalse(fluid.name().isBlank(), fluid + " has a name a file can store");
            assertTrue(fluid.color().a > 0.0f, fluid + " is painted in a colour that is not invisible");
            assertEquals(Fluids.byName(fluid.name()), fluid, "the table finds it by that name again");
        }
    }

    @Test
    void aBrokenFluidIsRefused() {
        Color color = new Color(1.0f, 1.0f, 1.0f, 1.0f);

        assertThrows(NullPointerException.class, () -> new Fluid(null, color));
        assertThrows(NullPointerException.class, () -> new Fluid("oil", null));
        assertThrows(IllegalArgumentException.class, () -> new Fluid(" ", color));
        assertThrows(IllegalArgumentException.class, () -> new Fluid("", color));
    }

    @Test
    void theColourOfAFluidIsACopy() {
        Color given = new Color(0.1f, 0.2f, 0.3f, 1.0f);
        Fluid fluid = new Fluid("oil", given);

        given.set(0.9f, 0.9f, 0.9f, 1.0f);

        assertEquals(0.1f, fluid.color().r, 1.0e-6f, "the fluid keeps the colour it was given");
        assertEquals(1.0f, fluid.color().a, 1.0e-6f);
    }
}
