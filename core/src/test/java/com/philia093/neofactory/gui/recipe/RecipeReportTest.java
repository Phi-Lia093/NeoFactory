package com.philia093.neofactory.gui.recipe;

import com.philia093.neofactory.recipe.EnergyRecipe;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks what a screen of recipes reports about a recipe.
 * <p>
 * The report is written where a recipe is known and printed in the rows the inventory of a player would
 * stand in, so what it says has to be exactly what the recipe carries: a recipe of the age of steam burns
 * steam and takes time, a recipe that draws energy says how much of it and at which voltage, and a number
 * nobody handed over is a line nobody writes. The lines are asked without a window.
 */
class RecipeReportTest {

    @Test
    void aRecipeOfTheAgeOfSteamReportsItsTimeAndWhatItBurns() {
        RecipeReport report = new RecipeReport().time(12.0f).steam(1500);

        assertEquals(List.of("Time 12.0s", "Steam 1500mB"), report.lines());
        assertEquals(2, report.size());
        assertFalse(report.isEmpty());
    }

    @Test
    void aRecipeThatDrawsNoEnergyReportsNoVoltage() {
        RecipeReport report = new RecipeReport().time(8.0f).energy(null);

        assertEquals(List.of("Time 8.0s"), report.lines(),
                "a recipe of another age promises no volts it does not have");
    }

    @Test
    void aRecipeOfTheElectricAgeReportsWhatItCosts() {
        EnergyRecipe recipe = new TestEnergyRecipe(32, 128, 12.0f);
        RecipeReport report = new RecipeReport().time(recipe.seconds()).energy(recipe);

        assertEquals(List.of("Time 12.0s", "Energy 7680EU", "Use 32EU/t", "Voltage 128V"),
                report.lines(), "the energy of a craft is what it draws a tick times the ticks it runs");
    }

    @Test
    void aReportNeverTakesMoreLinesThanTheScreenKeepsRoomFor() {
        RecipeReport report = new RecipeReport().time(12.0f).steam(1500)
                .energy(new TestEnergyRecipe(32, 128, 12.0f));

        assertEquals(5, report.size(), "a line of time, one of steam and the three of the energy");
        assertTrue(report.size() <= RecipeReport.MAX_LINES,
                "the lines of a report fit the rows the layout of the screen keeps for it");
        assertEquals(RecipeBrowserLayout.TEMPLATE_INFO_LINES, RecipeReport.MAX_LINES);
    }

    /** A recipe that draws energy, as a machine of the electric age would carry it. */
    private static final class TestEnergyRecipe implements EnergyRecipe {

        private final int euPerTick;
        private final int voltage;
        private final float seconds;

        private TestEnergyRecipe(int euPerTick, int voltage, float seconds) {
            this.euPerTick = euPerTick;
            this.voltage = voltage;
            this.seconds = seconds;
        }

        @Override
        public int euPerTick() {
            return euPerTick;
        }

        @Override
        public int voltage() {
            return voltage;
        }

        @Override
        public float seconds() {
            return seconds;
        }
    }
}
