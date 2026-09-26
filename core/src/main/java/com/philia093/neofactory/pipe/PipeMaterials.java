package com.philia093.neofactory.pipe;

import com.badlogic.gdx.graphics.Color;
import com.philia093.neofactory.material.Materials;

import java.util.List;

/**
 * Every material of the game, with the colour, the heat it takes and the flow of each of its sizes.
 * <p>
 * The table is short on purpose and it is the one place a new pipe is declared: four materials - wood,
 * copper, bronze and steel - and the sizes of {@link PipeSize} each of them is made in, so the game
 * holds one block of every kind of pipe and the blocks are built from the two tables, see {@link Pipes}.
 * <p>
 * <b>The metals take their colour from their material.</b> Copper, bronze and steel are materials of the
 * game, see {@link Materials}, and the pipes of them are painted with the very colour their ingots and
 * plates carry - the constants below are the colours of those materials, which is what keeps a copper
 * pipe the colour of a copper ingot. Wood is used by no other part of the game yet, so its colour lives
 * here.
 * <p>
 * <b>The numbers come from the table of the industry.</b> Every row names the seven sizes in the order
 * {@link PipeSize} declares them - tiny, small, medium, large, huge, quadruple, nonuple - in millibuckets
 * a second, and a cell left at {@link PipeMaterial#NOT_MADE} says that this material is not made in that
 * size: <b>wood is the first of them</b>, which comes as a small, a medium and a large pipe and as no
 * bundle at all, exactly as the wooden pipes of the industry do. The temperature behind every size is
 * the heat the material itself takes, in kelvin.
 * <p>
 * A larger pipe of a material carries more than a smaller one, and a bundle carries less than the single
 * pipe of the same cell - nine tubes through one cell are narrow and no bargain. Both are properties of
 * the table and not rules of the game: a later material may carry less than an earlier one and a row may
 * stop at the huge pipe, see {@link PipeMaterial}.
 */
public final class PipeMaterials {

    /** Colour of the pipes of wood, the very colour those planks of a build carry, see {@code Materials}. */
    public static final Color WOOD_COLOR = Materials.WOOD_COLOR;

    /**
     * Wood, which carries the water of a river and bursts with the steam of a boiler.
     * <p>
     * Three tubes and no bundle: a small pipe moves two hundred millibuckets a second, a medium one six
     * hundred and a large one twelve hundred, and the material gives way at three hundred and fifty
     * kelvin - which is why the steam of a boiler of three hundred and seventy three kelvin bursts it.
     */
    public static final PipeMaterial WOOD = new PipeMaterial("wood", "Wood", WOOD_COLOR,
            PipeTexture.WOOD, 350.0f,
            PipeMaterial.NOT_MADE, 200, 600, 1200, PipeMaterial.NOT_MADE, PipeMaterial.NOT_MADE,
            PipeMaterial.NOT_MADE);

    /** Copper, the metal that carries the water of the first machines. */
    public static final PipeMaterial COPPER = new PipeMaterial("copper", "Copper",
            Materials.COPPER_COLOR, PipeTexture.METAL, 1000.0f,
            60, 120, 400, 800, 1600, 400, 120);

    /** Bronze, the metal of the boiler and of the pipes that feed it. */
    public static final PipeMaterial BRONZE = new PipeMaterial("bronze", "Bronze",
            Materials.BRONZE_COLOR, PipeTexture.METAL, 2000.0f,
            400, 800, 2400, 4800, 9600, 2400, 800);

    /** Steel, the metal that takes the heat of the steam of a later age. */
    public static final PipeMaterial STEEL = new PipeMaterial("steel", "Steel",
            Materials.STEEL_COLOR, PipeTexture.METAL, 2500.0f,
            800, 1600, 4800, 9600, 19200, 4800, 1600);

    // ------------------------------------------------------------------
    // The rest of the table, in the order of the table of the industry: a material per line, the seven sizes
    // in the order PipeSize declares them and a cell left at NOT_MADE for a size the material is not made in.
    // They are appended after the four above so that the pipes of wood, copper, bronze and steel keep the
    // numbers a save game already holds, see Pipes and SaveFormat.
    // ------------------------------------------------------------------

    /** Clay, which is dug, shaped and baked before it carries anything, and takes little heat. */
    public static final PipeMaterial CLAY = new PipeMaterial("clay", "Clay",
            Materials.CLAY_COLOR, PipeTexture.METAL, 500.0f,
            200, 400, 1200, 2400, 4800, PipeMaterial.NOT_MADE, PipeMaterial.NOT_MADE);

    /** Wrought iron, the soft iron of a bloomery, which is what a workshop of bronze reaches next. */
    public static final PipeMaterial WROUGHT_IRON = new PipeMaterial("wrought_iron", "Wrought Iron",
            Materials.WROUGHT_IRON_COLOR, PipeTexture.METAL, 2250.0f,
            600, 1200, 3600, 7200, 14400, 3600, 1200);

    /** Lead, the heavy metal of a line that takes little heat. */
    public static final PipeMaterial LEAD = new PipeMaterial("lead", "Lead",
            Materials.LEAD_COLOR, PipeTexture.METAL, 1200.0f,
            680, 1360, 4080, 8160, 16320, PipeMaterial.NOT_MADE, PipeMaterial.NOT_MADE);

    /** Polyethylene, the plastic of the first lines, which no heat is asked of. */
    public static final PipeMaterial POLYETHYLENE = new PipeMaterial("polyethylene", "Polyethylene",
            Materials.POLYETHYLENE_COLOR, PipeTexture.METAL, 350.0f,
            1200, 2400, 7200, 14400, 28800, 7200, 2400);

    /** Stainless steel, the steel that does not rust. */
    public static final PipeMaterial STAINLESS_STEEL = new PipeMaterial("stainless_steel", "Stainless Steel",
            Materials.STAINLESS_STEEL_COLOR, PipeTexture.METAL, 3000.0f,
            1200, 2400, 7200, 14400, 28800, 7200, 2400);

    /** Titanium, the light and strong metal of a line that takes real heat. */
    public static final PipeMaterial TITANIUM = new PipeMaterial("titanium", "Titanium",
            Materials.TITANIUM_COLOR, PipeTexture.METAL, 5000.0f,
            1600, 3200, 9600, 19200, 38400, 9600, 3200);

    /** Polytetrafluoroethylene, the plastic that nothing sticks to and almost no heat is asked of. */
    public static final PipeMaterial PTFE = new PipeMaterial("ptfe", "Polytetrafluoroethylene",
            Materials.PTFE_COLOR, PipeTexture.METAL, 600.0f,
            1600, 3200, 9600, 19200, 38400, 9600, 3200);

    /** Tungsten steel, the steel of a tool that keeps its edge white hot. */
    public static final PipeMaterial TUNGSTEN_STEEL = new PipeMaterial("tungsten_steel", "Tungsten Steel",
            Materials.TUNGSTEN_STEEL_COLOR, PipeTexture.METAL, 7500.0f,
            2000, 4000, 12000, 24000, 48000, 12000, 4000);

    /** Polybenzimidazole, the amber plastic that takes the heat of a flame. */
    public static final PipeMaterial PBI = new PipeMaterial("pbi", "Polybenzimidazole",
            Materials.PBI_COLOR, PipeTexture.METAL, 1000.0f,
            2000, 4000, 12000, 24000, 48000, 12000, 4000);

    /** Niobium titanium, the alloy of a superconducting wire. */
    public static final PipeMaterial NIOBIUM_TITANIUM = new PipeMaterial("niobium_titanium",
            "Niobium Titanium", Materials.NIOBIUM_TITANIUM_COLOR, PipeTexture.METAL, 2900.0f,
            3000, 6000, 18000, 36000, 72000, 18000, 6000);

    /** Tungsten, the metal with the highest melting point of them all, and no bundle of it. */
    public static final PipeMaterial TUNGSTEN = new PipeMaterial("tungsten", "Tungsten",
            Materials.TUNGSTEN_COLOR, PipeTexture.METAL, 7200.0f,
            8640, 17280, 51840, 103680, 207360, PipeMaterial.NOT_MADE, PipeMaterial.NOT_MADE);

    /** The tantalum tungsten alloy of grade sixty. */
    public static final PipeMaterial TANTALUM_TUNGSTEN_60 = new PipeMaterial("tantalum_tungsten_60",
            "Tantalum Tungsten 60", Materials.TANTALUM_TUNGSTEN_60_COLOR, PipeTexture.METAL, 4250.0f,
            20000, 40000, 120000, 240000, 480000, PipeMaterial.NOT_MADE, PipeMaterial.NOT_MADE);

    /** The tantalum tungsten alloy of grade sixty one. */
    public static final PipeMaterial TANTALUM_TUNGSTEN_61 = new PipeMaterial("tantalum_tungsten_61",
            "Tantalum Tungsten 61", Materials.TANTALUM_TUNGSTEN_61_COLOR, PipeTexture.METAL, 5800.0f,
            24000, 48000, 144000, 288000, 576000, PipeMaterial.NOT_MADE, PipeMaterial.NOT_MADE);

    /** Europium, the rare earth of a screen that glows. */
    public static final PipeMaterial EUROPIUM = new PipeMaterial("europium", "Europium",
            Materials.EUROPIUM_COLOR, PipeTexture.METAL, 7500.0f,
            24000, 48000, 144000, 288000, 576000, PipeMaterial.NOT_MADE, PipeMaterial.NOT_MADE);

    /** Depleted uranium, the heavy metal of a shield. */
    public static final PipeMaterial DEPLETED_URANIUM = new PipeMaterial("depleted_uranium",
            "Depleted Uranium", Materials.DEPLETED_URANIUM_COLOR, PipeTexture.METAL, 7500.0f,
            25000, 50000, 150000, 300000, 600000, PipeMaterial.NOT_MADE, PipeMaterial.NOT_MADE);

    /** Maraging steel of grade three hundred. */
    public static final PipeMaterial MARAGING_STEEL_300 = new PipeMaterial("maraging_steel_300",
            "Maraging Steel 300", Materials.MARAGING_STEEL_300_COLOR, PipeTexture.METAL, 2500.0f,
            28000, 56000, 168000, 336000, 672000, PipeMaterial.NOT_MADE, PipeMaterial.NOT_MADE);

    /** The nickel chromium alloy of grade six hundred and ninety. */
    public static final PipeMaterial INCONEL_690 = new PipeMaterial("inconel_690", "Inconel 690",
            Materials.INCONEL_690_COLOR, PipeTexture.METAL, 4800.0f,
            30000, 60000, 180000, 360000, 720000, PipeMaterial.NOT_MADE, PipeMaterial.NOT_MADE);

    /** The nickel chromium alloy of grade seven hundred and ninety two. */
    public static final PipeMaterial INCONEL_792 = new PipeMaterial("inconel_792", "Inconel 792",
            Materials.INCONEL_792_COLOR, PipeTexture.METAL, 5500.0f,
            32000, 64000, 192000, 384000, 768000, PipeMaterial.NOT_MADE, PipeMaterial.NOT_MADE);

    /** Maraging steel of grade three hundred and fifty. */
    public static final PipeMaterial MARAGING_STEEL_350 = new PipeMaterial("maraging_steel_350",
            "Maraging Steel 350", Materials.MARAGING_STEEL_350_COLOR, PipeTexture.METAL, 2500.0f,
            32000, 64000, 192000, 384000, 768000, PipeMaterial.NOT_MADE, PipeMaterial.NOT_MADE);

    /** The superalloy known as Hastelloy X. */
    public static final PipeMaterial HASTELLOY_X = new PipeMaterial("hastelloy_x", "Hastelloy X",
            Materials.HASTELLOY_X_COLOR, PipeTexture.METAL, 4200.0f,
            40000, 80000, 240000, 480000, 960000, PipeMaterial.NOT_MADE, PipeMaterial.NOT_MADE);

    /** The heat resistant chromium iron alloy of grade nine hundred and three. */
    public static final PipeMaterial INCOLOY_903 = new PipeMaterial("incoloy_903", "Incoloy 903",
            Materials.INCOLOY_903_COLOR, PipeTexture.METAL, 8000.0f,
            50000, 100000, 300000, 600000, 1200000, PipeMaterial.NOT_MADE, PipeMaterial.NOT_MADE);

    private static final List<PipeMaterial> ALL = List.of(WOOD, COPPER, BRONZE, STEEL, CLAY, WROUGHT_IRON,
            LEAD, POLYETHYLENE, STAINLESS_STEEL, TITANIUM, PTFE, TUNGSTEN_STEEL, PBI, NIOBIUM_TITANIUM,
            TUNGSTEN, TANTALUM_TUNGSTEN_60, TANTALUM_TUNGSTEN_61, EUROPIUM, DEPLETED_URANIUM,
            MARAGING_STEEL_300, INCONEL_690, INCONEL_792, MARAGING_STEEL_350, HASTELLOY_X, INCOLOY_903);

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
