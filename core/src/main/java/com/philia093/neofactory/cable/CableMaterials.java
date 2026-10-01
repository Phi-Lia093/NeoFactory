package com.philia093.neofactory.cable;

import com.philia093.neofactory.material.Materials;

import java.util.List;

/**
 * Every cable of the game, with the voltage it takes, the current it carries, what it loses and the heat
 * it stands.
 * <p>
 * <b>The table is the one place a cable is declared.</b> A cable of the industry is a material of the
 * game - copper, silver, annealed copper, a superconductor - and this table says what a line of that
 * material does: how high its voltage may climb, how much current one cable of it takes, what it loses
 * over every block an energy travels and how much heat it stands. The blocks are built from this table
 * and the sizes of the line, exactly like the pipes of the fluid system are built from
 * {@code PipeMaterials} and {@code PipeSize}.
 * <p>
 * <b>A metal is a cable before anything else.</b> Almost every material of the table is a material the
 * game already holds - the iron of a machine, the copper of a wire, the silver of a better one - and a
 * handful of them arrive with the cable line: the annealed copper that loses less than the copper it is
 * made of, the cupronickel that takes four times the current of it, the superconductors that lose
 * nothing at all. The colours come from {@link Materials}, so a cable is the very colour of its ingot.
 * <p>
 * <b>The order of the table is the ladder of the voltages</b> and a line only ever climbs: the first line
 * of a workshop is the red alloy of the ultra low voltage, the line of the age of steam is the tin and
 * the lead of low voltage, and the silver of high voltage is the last metal before a line has to be a
 * superconductor to be worth building. Two materials of the table carry nearly the same name and are two
 * different lines: <b>{@link #RED_ALLOY} is the line of eight units a tick</b> of a workshop that has
 * just learned to move energy, while {@link #REDSTONE_ALLOY} is a line of thirty two units a tick that
 * loses nothing and is cheap to build.
 * <p>
 * <b>What the table does not hold.</b> The line of the industry names cables of materials the game does
 * not bring yet - the blue alloy that carries twice the current of a high voltage line, the charged
 * alloy, the pulsating alloy, the ternary metal, the silicon and the alloys of it, the uranium of a
 * shield and the titanium platinum vanadium of a later age - and not one of them is a line of this game
 * yet, so the table stops at the materials a workshop can really build. A material that arrives later is
 * one line below and a colour in {@code Materials}.
 * <p>
 * <b>A superconductor is a cable that loses nothing.</b> {@link CableMaterial#isSuperconductor()} is the
 * question the transport asks before it moves energy through a line, and the rows at the end of every age
 * of the table are the ones that answer yes: from the middle voltage up, the industry builds a wire that
 * carries what it is given, and the coarser piece of that wire - the blank it is drawn out of - is an
 * item of the material and not a cable of this table.
 */
public final class CableMaterials {
    // ------------------------------------------------------------------
    // The ropes of the first age: the red alloy of a workshop, and the metals of low voltage that the
    // age of steam builds its lines from.
    // ------------------------------------------------------------------

    /** The red alloy, the cheapest line of all and the one a workshop learns to move energy with. */
    public static final CableMaterial RED_ALLOY = new CableMaterial("red_alloy", "Red Alloy",
            Materials.RED_ALLOY_COLOR, Voltage.ULTRA_LOW, 1, 1, CableMaterial.NO_HEAT);

    /** Cobalt, the tough metal of a line that takes heat. */
    public static final CableMaterial COBALT = new CableMaterial("cobalt", "Cobalt",
            Materials.COBALT_COLOR, Voltage.LOW, 2, 2, 1);

    /** Lead, the heavy metal of the first line of a boiler. */
    public static final CableMaterial LEAD = new CableMaterial("lead", "Lead",
            Materials.LEAD_COLOR, Voltage.LOW, 2, 4, 2);

    /** Tin, the soft metal of the line the age of steam runs on. */
    public static final CableMaterial TIN = new CableMaterial("tin", "Tin", Materials.TIN_COLOR,
            Voltage.LOW, 1, 2, 1);

    /** Zinc, the metal that protects the line it is drawn around. */
    public static final CableMaterial ZINC = new CableMaterial("zinc", "Zinc",
            Materials.ZINC_COLOR, Voltage.LOW, 1, 2, 1);

    /** Solder, the soft alloy that holds a line together where it is joined. */
    public static final CableMaterial SOLDER = new CableMaterial("solder", "Solder",
            Materials.SOLDER_COLOR, Voltage.LOW, 1, 2, 1);

    /**
     * The redstone alloy, the cheap line of the age of steam.
     * <p>
     * <b>Not to be mixed up with {@link #RED_ALLOY}.</b> The red alloy is the line of the ultra low
     * voltage a workshop starts with, one ampere wide and the heaviest loss of the table; the redstone
     * alloy is the line of low voltage that costs a workshop nothing to build and is the one the table of
     * the industry calls its cheap superconductor - the cheap line of that age, and no superconductor of
     * the game, which are the rows that lose nothing, see {@link CableMaterial#isSuperconductor()}.
     */
    public static final CableMaterial REDSTONE_ALLOY = new CableMaterial("redstone_alloy",
            "Redstone Alloy", Materials.REDSTONE_ALLOY_COLOR, Voltage.LOW, 1, 2, CableMaterial.NO_HEAT);

    // ------------------------------------------------------------------
    // The age of the first machines: a line of a hundred and twenty eight units a tick, from the copper a
    // wire is drawn out of to the cupronickel that carries four amperes of it.
    // ------------------------------------------------------------------

    /** Iron, the metal of every machine and the plain line of the age. */
    public static final CableMaterial IRON = new CableMaterial("iron", "Iron", Materials.IRON_COLOR,
            Voltage.MEDIUM, 2, 6, 3);

    /** Nickel, the metal of a line that takes heat without giving way. */
    public static final CableMaterial NICKEL = new CableMaterial("nickel", "Nickel",
            Materials.NICKEL_COLOR, Voltage.MEDIUM, 3, 6, 3);

    /** Cupronickel, the alloy of a line that carries four times the current of a plain one. */
    public static final CableMaterial CUPRONICKEL = new CableMaterial("cupronickel", "Cupronickel",
            Materials.CUPRONICKEL_COLOR, Voltage.MEDIUM, 4, 6, 3);

    /** Copper, the metal a wire is drawn out of, the plain line of the age of the machines. */
    public static final CableMaterial COPPER = new CableMaterial("copper", "Copper",
            Materials.COPPER_COLOR, Voltage.MEDIUM, 1, 4, 2);

    /** Annealed copper, the same metal softened, which loses less over the same length. */
    public static final CableMaterial ANNEALED_COPPER = new CableMaterial("annealed_copper",
            "Annealed Copper", Materials.ANNEALED_COPPER_COLOR, Voltage.MEDIUM, 1, 2, 1);

    /**
     * The first superconductor of the industry.
     * <p>
     * The wire of the middle voltage and the blank it is drawn out of are one material: the cable of the
     * table is the wire, the four amperes it takes are what a line of that age may carry without a loss,
     * and the blank is an item of the material, see {@code MaterialForm}.
     */
    public static final CableMaterial MV_SUPERCONDUCTOR = new CableMaterial("mv_superconductor",
            "MV Superconductor", Materials.MV_SUPERCONDUCTOR_COLOR, Voltage.MEDIUM, 4,
            CableMaterial.NO_LOSS, CableMaterial.NO_HEAT);

    /** Magnetic steel, the steel a line of the age is wound around. */
    public static final CableMaterial MAGNETIC_STEEL = new CableMaterial("magnetic_steel",
            "Magnetic Steel", Materials.MAGNETIC_STEEL_COLOR, Voltage.MEDIUM, 3, 2, 1);

    // ------------------------------------------------------------------
    // The age of the larger workshop: five hundred and twelve units a tick, the voltage a line of gold or
    // of silver is built for.
    // ------------------------------------------------------------------

    /** Kanthal, the alloy of a line that takes the heat of a furnace and keeps its shape. */
    public static final CableMaterial KANTHAL = new CableMaterial("kanthal", "Kanthal",
            Materials.KANTHAL_COLOR, Voltage.HIGH, 5, 6, 3);

    /** Gold, the metal of a line that carries three amperes without a loss worth naming. */
    public static final CableMaterial GOLD = new CableMaterial("gold", "Gold", Materials.GOLD_COLOR,
            Voltage.HIGH, 3, 4, 2);

    /** Electrum, the alloy of gold and silver, a line that takes more heat than either of them. */
    public static final CableMaterial ELECTRUM = new CableMaterial("electrum", "Electrum",
            Materials.ELECTRUM_COLOR, Voltage.HIGH, 2, 2, 1);

    /** Silver, the best conductor of the metals and the plain line of the age. */
    public static final CableMaterial SILVER = new CableMaterial("silver", "Silver",
            Materials.SILVER_COLOR, Voltage.HIGH, 1, 2, 1);

    /** The superconductor of the high voltage, six amperes and not one unit lost. */
    public static final CableMaterial HV_SUPERCONDUCTOR = new CableMaterial("hv_superconductor",
            "HV Superconductor", Materials.HV_SUPERCONDUCTOR_COLOR, Voltage.HIGH, 6,
            CableMaterial.NO_LOSS, CableMaterial.NO_HEAT);

    // ------------------------------------------------------------------
    // The age of the heavy industry: two thousand and forty eight units a tick, where a line is built of
    // the alloys that take a white heat.
    // ------------------------------------------------------------------

    /** Nichrome, the alloy of a heating element, the line that loses the most and takes the most heat. */
    public static final CableMaterial NICHROME = new CableMaterial("nichrome", "Nichrome",
            Materials.NICHROME_COLOR, Voltage.EXTREME, 6, 8, 4);

    /** Steel, the metal of the machines of the age of steam and a line of the age behind it. */
    public static final CableMaterial STEEL = new CableMaterial("steel", "Steel", Materials.STEEL_COLOR,
            Voltage.EXTREME, 2, 6, 3);

    /** Black steel, the dark steel of a line that carries four amperes and loses little. */
    public static final CableMaterial BLACK_STEEL = new CableMaterial("black_steel", "Black Steel",
            Materials.BLACK_STEEL_COLOR, Voltage.EXTREME, 4, 2, 1);

    /** Titanium, the light and strong metal of a line of the age. */
    public static final CableMaterial TITANIUM = new CableMaterial("titanium", "Titanium",
            Materials.TITANIUM_COLOR, Voltage.EXTREME, 4, 4, 2);

    /** Aluminium, the cheap line of the first year of the age. */
    public static final CableMaterial ALUMINIUM = new CableMaterial("aluminium", "Aluminium",
            Materials.ALUMINIUM_COLOR, Voltage.EXTREME, 1, 2, 1);

    /** The superconductor of the extreme voltage, eight amperes wide. */
    public static final CableMaterial EV_SUPERCONDUCTOR = new CableMaterial("ev_superconductor",
            "EV Superconductor", Materials.EV_SUPERCONDUCTOR_COLOR, Voltage.EXTREME, 8,
            CableMaterial.NO_LOSS, CableMaterial.NO_HEAT);

    // ------------------------------------------------------------------
    // The age of the fine work: eight thousand one hundred and ninety two units a tick. Graphene is the
    // cheap line of that age and it is brittle - a player cuts one up and takes the motor out of it.
    // ------------------------------------------------------------------

    /** Graphene, the cheap and brittle line of the age, sheet by sheet. */
    public static final CableMaterial GRAPHENE = new CableMaterial("graphene", "Graphene",
            Materials.GRAPHENE_COLOR, Voltage.INSANE, 1, 2, CableMaterial.NO_HEAT);

    /** Platinum, the noble metal of a line that nothing attacks. */
    public static final CableMaterial PLATINUM = new CableMaterial("platinum", "Platinum",
            Materials.PLATINUM_COLOR, Voltage.INSANE, 2, 2, 1);

    /** Tungsten steel, the steel of a tool that keeps its edge white hot. */
    public static final CableMaterial TUNGSTEN_STEEL = new CableMaterial("tungsten_steel",
            "Tungsten Steel", Materials.TUNGSTEN_STEEL_COLOR, Voltage.INSANE, 4, 8, 4);

    /** Tungsten, the metal of the highest melting point of them all. */
    public static final CableMaterial TUNGSTEN = new CableMaterial("tungsten", "Tungsten",
            Materials.TUNGSTEN_COLOR, Voltage.INSANE, 6, 4, 2);

    /** The superconductor of the fine age, twelve amperes wide. */
    public static final CableMaterial IV_SUPERCONDUCTOR = new CableMaterial("iv_superconductor",
            "IV Superconductor", Materials.IV_SUPERCONDUCTOR_COLOR, Voltage.INSANE, 12,
            CableMaterial.NO_LOSS, CableMaterial.NO_HEAT);

    // ------------------------------------------------------------------
    // The age of the large lines: thirty two thousand seven hundred and sixty eight units a tick, where the
    // table of the industry has already stopped naming a loss for half of its materials.
    // ------------------------------------------------------------------

    /** Osmium, the densest metal of the line. */
    public static final CableMaterial OSMIUM = new CableMaterial("osmium", "Osmium",
            Materials.OSMIUM_COLOR, Voltage.LUDICROUS, 4, 4, 2);

    /** High speed steel of grade G, the steel of a tool that cuts a line to size. */
    public static final CableMaterial HSS_G = new CableMaterial("hss_g", "HSS-G",
            Materials.HSS_G_COLOR, Voltage.LUDICROUS, 4, 4, 2);

    /** Niobium titanium, the alloy of a superconducting wire of the age. */
    public static final CableMaterial NIOBIUM_TITANIUM = new CableMaterial("niobium_titanium",
            "Niobium Titanium", Materials.NIOBIUM_TITANIUM_COLOR, Voltage.LUDICROUS, 4, 4, 2);

    /** Vanadium gallium, the intermetallic compound a line of the age is wound from. */
    public static final CableMaterial VANADIUM_GALLIUM = new CableMaterial("vanadium_gallium",
            "Vanadium Gallium", Materials.VANADIUM_GALLIUM_COLOR, Voltage.LUDICROUS, 4, 8, 4);

    /** The ceramic superconductor of the age, the oxide of yttrium, barium and copper. */
    public static final CableMaterial YTTRIUM_BARIUM_COPPER_OXIDE = new CableMaterial(
            "yttrium_barium_copper_oxide", "Yttrium Barium Copper Oxide",
            Materials.YTTRIUM_BARIUM_COPPER_OXIDE_COLOR, Voltage.LUDICROUS, 6, 6, 3);

    /** The superconductor of the large age, sixteen amperes wide. */
    public static final CableMaterial LUV_SUPERCONDUCTOR = new CableMaterial("luv_superconductor",
            "LuV Superconductor", Materials.LUV_SUPERCONDUCTOR_COLOR, Voltage.LUDICROUS, 16,
            CableMaterial.NO_LOSS, CableMaterial.NO_HEAT);

    /**
     * High speed steel of grade E, the steel of the last metal line of the age.
     * <p>
     * It is written down after the superconductor of its age because the table of the industry does: the
     * line of a workshop of that size is built of a superconductor anyway, and the metal is what carries
     * the heat of a machine that melts one.
     */
    public static final CableMaterial HSS_E = new CableMaterial("hss_e", "HSS-E",
            Materials.HSS_E_COLOR, Voltage.LUDICROUS, 6, 8, 4);

    // ------------------------------------------------------------------
    // The ages above the large lines: every one of them is a superconductor from here on, and the table of
    // the industry stops naming a metal line for the last three of them. The amperage grows with the age
    // and the loss stays nothing, which is what makes a line of those sizes worth its cost.
    // ------------------------------------------------------------------

    /** The superconductor of the zero point module, twenty four amperes wide. */
    public static final CableMaterial ZPM_SUPERCONDUCTOR = new CableMaterial("zpm_superconductor",
            "ZPM Superconductor", Materials.ZPM_SUPERCONDUCTOR_COLOR, Voltage.ZERO_POINT_MODULE, 24,
            CableMaterial.NO_LOSS, CableMaterial.NO_HEAT);

    /** Osmiridium, the alloy of osmium and iridium: the highest current at the lowest loss. */
    public static final CableMaterial OSMIRIDIUM = new CableMaterial("osmiridium", "Osmiridium",
            Materials.OSMIRIDIUM_COLOR, Voltage.ZERO_POINT_MODULE, 16, 2, 1);

    /** The superconductor of the ultimate voltage, thirty two amperes wide. */
    public static final CableMaterial UV_SUPERCONDUCTOR = new CableMaterial("uv_superconductor",
            "UV Superconductor", Materials.UV_SUPERCONDUCTOR_COLOR, Voltage.ULTIMATE, 32,
            CableMaterial.NO_LOSS, CableMaterial.NO_HEAT);

    /** The superconductor of the ultra high voltage, forty eight amperes wide. */
    public static final CableMaterial UHV_SUPERCONDUCTOR = new CableMaterial("uhv_superconductor",
            "UHV Superconductor", Materials.UHV_SUPERCONDUCTOR_COLOR, Voltage.ULTRA_HIGH, 48,
            CableMaterial.NO_LOSS, CableMaterial.NO_HEAT);

    /** High speed steel of grade S, the steel of the last line of the table that is no superconductor. */
    public static final CableMaterial HSS_S = new CableMaterial("hss_s", "HSS-S",
            Materials.HSS_S_COLOR, Voltage.ULTRA_HIGH, 8, 8, 4);

    /** The superconductor of the ultra excessive voltage, sixty four amperes wide. */
    public static final CableMaterial UEV_SUPERCONDUCTOR = new CableMaterial("uev_superconductor",
            "UEV Superconductor", Materials.UEV_SUPERCONDUCTOR_COLOR, Voltage.ULTRA_EXCESSIVE, 64,
            CableMaterial.NO_LOSS, CableMaterial.NO_HEAT);

    /** The superconductor of the ultra immense voltage, sixty four amperes wide. */
    public static final CableMaterial UIV_SUPERCONDUCTOR = new CableMaterial("uiv_superconductor",
            "UIV Superconductor", Materials.UIV_SUPERCONDUCTOR_COLOR, Voltage.ULTRA_IMMENSE, 64,
            CableMaterial.NO_LOSS, CableMaterial.NO_HEAT);

    /** The superconductor of the ultra massive voltage, the widest line of the table. */
    public static final CableMaterial UMV_SUPERCONDUCTOR = new CableMaterial("umv_superconductor",
            "UMV Superconductor", Materials.UMV_SUPERCONDUCTOR_COLOR, Voltage.ULTRA_MASSIVE, 64,
            CableMaterial.NO_LOSS, CableMaterial.NO_HEAT);

    private static final List<CableMaterial> ALL = List.of(RED_ALLOY, COBALT, LEAD, TIN, ZINC, SOLDER,
            REDSTONE_ALLOY, IRON, NICKEL, CUPRONICKEL, COPPER, ANNEALED_COPPER, MV_SUPERCONDUCTOR,
            MAGNETIC_STEEL, KANTHAL, GOLD, ELECTRUM, SILVER, HV_SUPERCONDUCTOR, NICHROME, STEEL,
            BLACK_STEEL, TITANIUM, ALUMINIUM, EV_SUPERCONDUCTOR, GRAPHENE, PLATINUM, TUNGSTEN_STEEL,
            TUNGSTEN, IV_SUPERCONDUCTOR, OSMIUM, HSS_G, NIOBIUM_TITANIUM, VANADIUM_GALLIUM,
            YTTRIUM_BARIUM_COPPER_OXIDE, LUV_SUPERCONDUCTOR, HSS_E, ZPM_SUPERCONDUCTOR, OSMIRIDIUM,
            UV_SUPERCONDUCTOR, UHV_SUPERCONDUCTOR, HSS_S, UEV_SUPERCONDUCTOR, UIV_SUPERCONDUCTOR,
            UMV_SUPERCONDUCTOR);

    private CableMaterials() {
        // Utility class: never instantiated.
    }

    /**
     * Every cable of the game, in the order it is declared in.
     * <p>
     * The order is the ladder of the voltages and what the blocks of the cable line are numbered by, so a
     * material has to be appended and never inserted in the middle, see {@code Cables}.
     *
     * @return the materials
     */
    public static List<CableMaterial> all() {
        return ALL;
    }

    /**
     * The cable of a material name.
     *
     * @param name technical name, such as {@code annealed_copper}
     * @return the material, or {@code null} when the game has no such cable
     */
    public static CableMaterial byName(String name) {
        if (name == null) {
            return null;
        }
        for (CableMaterial material : ALL) {
            if (material.name().equals(name)) {
                return material;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return "CableMaterials(" + ALL.size() + " materials)";
    }
}
