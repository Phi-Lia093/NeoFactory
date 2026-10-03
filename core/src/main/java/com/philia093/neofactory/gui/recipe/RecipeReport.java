package com.philia093.neofactory.gui.recipe;

import com.philia093.neofactory.recipe.EnergyRecipe;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * What a recipe costs, written in as many lines as the screen of recipes holds for it.
 * <p>
 * A recipe is shown in the panel of the machine that makes it and the rows the inventory of a player stands
 * in carry what it takes instead: how long it runs and, when the age of a recipe says so, what it burns or
 * how much energy it draws. <b>Nothing is invented</b> - a recipe that says nothing about energy reports no
 * energy, which is what keeps a screen of the age of steam from promising volts it does not have, see
 * {@link EnergyRecipe} and {@link com.philia093.neofactory.recipe.ProcessingRecipe}.
 * <p>
 * The report is built where the recipe is known and not here: the screen of recipes hands it the numbers it
 * read out of the recipe, and a number it does not hand over is a line that is not written.
 */
public final class RecipeReport {

    /** Amount of lines the report may take, the room the layout of the screen keeps for it. */
    public static final int MAX_LINES = RecipeBrowserLayout.TEMPLATE_INFO_LINES;

    private final List<String> lines = new ArrayList<>();

    /**
     * Writes how long the recipe takes.
     *
     * @param seconds seconds one craft takes, no line for a recipe that takes no time
     * @return this report
     */
    public RecipeReport time(float seconds) {
        if (seconds > 0.0f) {
            lines.add(String.format(Locale.ROOT, "Time %.1fs", seconds));
        }
        return this;
    }

    /**
     * Writes what the recipe burns, the fluid a machine of the age of steam runs on.
     *
     * @param millibuckets steam one craft takes, no line when nothing is burnt
     * @return this report
     */
    public RecipeReport steam(int millibuckets) {
        if (millibuckets > 0) {
            lines.add(String.format(Locale.ROOT, "Steam %dmB", millibuckets));
        }
        return this;
    }

    /**
     * Writes what the recipe costs a machine of the electric age.
     * <p>
     * Three lines are written: the energy of the whole craft, the energy of a tick and the voltage the recipe
     * asks for. A recipe that draws nothing writes nothing.
     *
     * @param recipe recipe that draws energy, may be {@code null} for a recipe of another age
     * @return this report
     */
    public RecipeReport energy(EnergyRecipe recipe) {
        if (recipe == null || recipe.euPerTick() <= 0) {
            return this;
        }
        lines.add(String.format(Locale.ROOT, "Energy %dEU", recipe.totalEu()));
        lines.add(String.format(Locale.ROOT, "Use %dEU/t", recipe.euPerTick()));
        lines.add(String.format(Locale.ROOT, "Voltage %dV", recipe.voltage()));
        return this;
    }

    /**
     * The lines the report is made of.
     *
     * @return the lines in the order they are written, at most {@link #MAX_LINES} of them
     */
    public List<String> lines() {
        return List.copyOf(lines.subList(0, Math.min(lines.size(), MAX_LINES)));
    }

    /** Amount of lines the report holds, at most {@link #MAX_LINES}. */
    public int size() {
        return lines().size();
    }

    /** {@code true} when the recipe reports nothing at all, which is a recipe with nothing to say. */
    public boolean isEmpty() {
        return lines.isEmpty();
    }

    @Override
    public String toString() {
        return "RecipeReport" + lines();
    }
}
