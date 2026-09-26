package com.philia093.neofactory.pipe;

import com.badlogic.gdx.graphics.Color;
import com.philia093.neofactory.material.Materials;

import java.util.List;

/**
 * Every material of the game, with the colour, the heat it takes and the flow of each of its sizes.
 * <p>
 * The table is short on purpose and it is the one place a new pipe is declared: four materials - wood,
 * copper, bronze and steel - and the seven sizes of {@link PipeSize} behind each of them, so the game
 * holds four of every kind of pipe and the blocks are built from the two tables, see {@link Pipes}.
 * <p>
 * <b>The metals take their colour from their material.</b> Copper, bronze and steel are materials of the
 * game, see {@link Materials}, and the pipes of them are painted with the very colour their ingots and
 * plates carry - the constants below are the colours of those materials, which is what keeps a copper
 * pipe the colour of a copper ingot. Wood is used by no other part of the game yet, so its colour lives
 * here.
 * <p>
 * <b>The numbers are placeholders.</b> Temperatures and flow rates are a first guess that keeps the
 * materials and the sizes in a sane order - a larger pipe carries more, a better material takes more heat
 * - and are to be replaced by the final data of the game; nothing but the order of the table depends on
 * them, see {@link PipeMaterial}.
 */
public final class PipeMaterials {

    /** Colour of the pipes of wood, a warm brown that reads next to the planks of a build. */
    public static final Color WOOD_COLOR = new Color(0.62f, 0.45f, 0.26f, 1f);

    /** Wood, which carries the water of a river and bursts with anything hot. */
    public static final PipeMaterial WOOD = new PipeMaterial("wood", "Wood", WOOD_COLOR,
            PipeTexture.WOOD, 373.0f, 40, 80, 160, 320, 640, 800, 1800);

    /** Copper, the metal that carries the water of the first machines. */
    public static final PipeMaterial COPPER = new PipeMaterial("copper", "Copper",
            Materials.COPPER_COLOR, PipeTexture.METAL, 1373.0f,
            100, 200, 400, 800, 1600, 2000, 4500);

    /** Bronze, the metal of the boiler and of the pipes that feed it. */
    public static final PipeMaterial BRONZE = new PipeMaterial("bronze", "Bronze",
            Materials.BRONZE_COLOR, PipeTexture.METAL, 1473.0f,
            150, 300, 600, 1200, 2400, 3000, 6750);

    /** Steel, the metal that takes the heat of the steam of a later age. */
    public static final PipeMaterial STEEL = new PipeMaterial("steel", "Steel",
            Materials.STEEL_COLOR, PipeTexture.METAL, 1773.0f,
            250, 500, 1000, 2000, 4000, 5000, 11250);

    private static final List<PipeMaterial> ALL = List.of(WOOD, COPPER, BRONZE, STEEL);

    private PipeMaterials() {
        // Utility class: never instantiated.
    }

    /**
     * Every pipe material of the game, in the order it is declared in.
     * <p>
     * The order is what the blocks of {@link Pipes} are numbered by, so a material has to be appended and
     * never inserted in the middle, see {@link Pipes}.
     *
     * @return the materials
     */
    public static List<PipeMaterial> all() {
        return ALL;
    }

    /**
     * The pipe material of a name.
     *
     * @param name technical name, such as {@code bronze}
     * @return the material, or {@code null} when the game has no such pipe
     */
    public static PipeMaterial byName(String name) {
        for (PipeMaterial material : ALL) {
            if (material.name().equals(name)) {
                return material;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return "PipeMaterials(" + ALL.size() + " materials)";
    }
}
