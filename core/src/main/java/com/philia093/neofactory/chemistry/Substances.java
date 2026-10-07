package com.philia093.neofactory.chemistry;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The catalog of substances the module knows, the closed world every reaction is written inside.
 * <p>
 * A reaction is never allowed to invent a substance. The catalog is the list of what exists, and a solver
 * looks for its products among the entries and nowhere else: an unfamiliar combination of elements produces
 * no reaction rather than a fabricated compound, which is the line that keeps a balance from being
 * arithmetically right and chemically nonsense. A substance the game wants and the catalog lacks is a line
 * that has to be added, which is a moment of thought and not a moment of guessing, see {@link #register}.
 * <p>
 * <b>A substance is looked up by its molecule and its phase and never by its name.</b> Two writings of
 * benzene - the aromatic one and the Kekulé one - have to find one entry and not two, and water and water
 * vapour have to be two entries and not one; neither question can be answered by a name, which is a label a
 * screen prints and nothing more. The lookup key is therefore the canonical labeling of the molecule
 * together with the phase, and the name is kept only so that the matching entry can be read back out for a
 * player, see {@link Substance}.
 * <p>
 * The catalog is an object and not a table of the whole program, so a test builds the handful of substances
 * it asks about and a game builds the ones it plays with; nothing here reaches for a global that a second
 * copy of the catalog would collide with.
 */
public final class Substances {

    private final Map<Key, Substance> byKey = new LinkedHashMap<>();
    private final Map<String, Substance> byName = new LinkedHashMap<>();

    /** Creates an empty catalog. */
    public Substances() {
        // An empty catalog is a valid one: a solver inside it finds no reaction at all.
    }

    /**
     * Adds a substance to the catalog.
     *
     * @param substance substance to add
     * @return this catalog, for chaining
     * @throws IllegalArgumentException when another substance of the same molecule and phase is there
     *         already under a different name
     */
    public Substances register(Substance substance) {
        Key key = new Key(substance.canonicalKey(), substance.phase());
        Substance existing = byKey.get(key);
        if (existing != null && !existing.name().equals(substance.name())) {
            throw new IllegalArgumentException("The catalog already holds " + existing
                    + " for the molecule and phase of " + substance);
        }
        byKey.put(key, substance);
        byName.put(substance.name(), substance);
        return this;
    }

    /**
     * Reads a substance from a SMILES string and adds it to the catalog.
     *
     * @param name the name a screen prints
     * @param smiles the molecule
     * @param phase the state it stands in
     * @return this catalog, for chaining
     * @throws SmilesException when the string cannot be read
     */
    public Substances register(String name, String smiles, Phase phase) {
        return register(new Substance(name, Chemical.parse(smiles), phase));
    }

    /**
     * Looks a substance up by its name.
     *
     * @param name the name a screen prints
     * @return the substance, or {@code null} when no entry uses that name
     */
    public Substance byName(String name) {
        return name == null ? null : byName.get(name);
    }

    /**
     * Looks a substance up by the key of its molecule and its phase.
     *
     * @param canonicalKey the canonical key of the molecule
     * @param phase the state it stands in
     * @return the substance, or {@code null} when the catalog holds none
     */
    public Substance byKey(String canonicalKey, Phase phase) {
        return canonicalKey == null || phase == null ? null : byKey.get(new Key(canonicalKey, phase));
    }

    /**
     * Looks a substance up by a molecule and a phase, whichever way the molecule was written.
     *
     * @param chemical the molecule, in any of its writings
     * @param phase the state it stands in
     * @return the substance, or {@code null} when the catalog holds none
     */
    public Substance byChemical(Chemical chemical, Phase phase) {
        return chemical == null ? null : byKey.get(new Key(chemical.structure().canonicalKey(), phase));
    }

    /**
     * {@code true} when the catalog holds a molecule in a phase.
     *
     * @param chemical the molecule, in any of its writings
     * @param phase the state it stands in
     * @return {@code true} when an entry matches
     */
    public boolean contains(Chemical chemical, Phase phase) {
        return byChemical(chemical, phase) != null;
    }

    /**
     * {@code true} when the catalog holds a molecule in any of its phases.
     *
     * @param chemical the molecule, in any of its writings
     * @return {@code true} when an entry matches
     */
    public boolean holds(Chemical chemical) {
        for (Phase phase : Phase.values()) {
            if (contains(chemical, phase)) {
                return true;
            }
        }
        return false;
    }

    /** Every substance of the catalog, in the order it was added. */
    public List<Substance> all() {
        return List.copyOf(byKey.values());
    }

    /** Amount of substances the catalog holds. */
    public int count() {
        return byKey.size();
    }

    /**
     * A catalog of the substances the reactions of the industry are built from.
     * <p>
     * The list is short on purpose and is meant to be widened by the content stage that names every element
     * the game holds. What it pins is the shape of the catalog and not its size: the elements in the state
     * each is met in, the ions a solution is written between, and the common compounds a first reaction is
     * made of. A substance that is missing is a reaction that cannot be written, which is the closed world
     * working as it should, see the class comment.
     *
     * @return the catalog
     */
    public static Substances starter() {
        Substances catalog = new Substances();
        // The elements, each in the state it is met in.
        catalog.register("hydrogen", "[H][H]", Phase.GAS);
        catalog.register("oxygen", "O=O", Phase.GAS);
        catalog.register("nitrogen", "N#N", Phase.GAS);
        catalog.register("fluorine", "FF", Phase.GAS);
        catalog.register("chlorine", "ClCl", Phase.GAS);
        catalog.register("bromine", "BrBr", Phase.LIQUID);
        catalog.register("carbon", "[C]", Phase.SOLID);
        catalog.register("silicon", "[Si]", Phase.SOLID);
        catalog.register("sulfur", "[S]", Phase.SOLID);
        catalog.register("phosphorus", "[P]", Phase.SOLID);
        catalog.register("iron", "[Fe]", Phase.SOLID);
        catalog.register("copper", "[Cu]", Phase.SOLID);
        catalog.register("tin", "[Sn]", Phase.SOLID);
        catalog.register("lead", "[Pb]", Phase.SOLID);
        catalog.register("silver", "[Ag]", Phase.SOLID);
        catalog.register("gold", "[Au]", Phase.SOLID);
        catalog.register("nickel", "[Ni]", Phase.SOLID);
        catalog.register("aluminium", "[Al]", Phase.SOLID);
        catalog.register("platinum", "[Pt]", Phase.SOLID);
        catalog.register("tungsten", "[W]", Phase.SOLID);
        catalog.register("zinc", "[Zn]", Phase.SOLID);
        catalog.register("cobalt", "[Co]", Phase.SOLID);
        catalog.register("titanium", "[Ti]", Phase.SOLID);
        catalog.register("chromium", "[Cr]", Phase.SOLID);
        catalog.register("manganese", "[Mn]", Phase.SOLID);
        catalog.register("magnesium", "[Mg]", Phase.SOLID);
        catalog.register("sodium", "[Na]", Phase.SOLID);
        catalog.register("potassium", "[K]", Phase.SOLID);
        catalog.register("calcium", "[Ca]", Phase.SOLID);
        // The ions a solution is written between.
        // The salts and the oxides the routes of the industry are written with. Each one is read as a
        // molecule and never as two ions: a route of the game is a hard recipe with measured amounts, so
        // nothing here has to balance a charge, and what a tank holds is a substance a machine can weigh.
        catalog.register("sodium chloride", "[Na]Cl", Phase.SOLID);
        catalog.register("sodium hydroxide", "[Na]O", Phase.SOLID);
        catalog.register("calcium oxide", "[Ca]=O", Phase.SOLID);
        catalog.register("calcium carbide", "[Ca][C]#[C]", Phase.SOLID);
        catalog.register("calcium hydroxide", "[Ca](O)O", Phase.SOLID);
        catalog.register("iron(II) oxide", "O=[Fe]", Phase.SOLID);
        // The compounds the first reactions are written with.
        catalog.register("water", "O", Phase.LIQUID);
        catalog.register("carbon monoxide", "[C-]#[O+]", Phase.GAS);
        catalog.register("carbon dioxide", "O=C=O", Phase.GAS);
        catalog.register("methane", "C", Phase.GAS);
        catalog.register("acetylene", "C#C", Phase.GAS);
        catalog.register("ammonia", "N", Phase.GAS);
        catalog.register("nitrogen monoxide", "[N]=O", Phase.GAS);
        catalog.register("nitrogen dioxide", "[N](=O)=O", Phase.GAS);
        catalog.register("nitric acid", "O[N+](=O)[O-]", Phase.LIQUID);
        catalog.register("hydrogen sulfide", "S", Phase.GAS);
        catalog.register("sulfur dioxide", "O=S=O", Phase.GAS);
        catalog.register("sulfur trioxide", "O=S(=O)=O", Phase.GAS);
        catalog.register("hydrogen chloride", "Cl", Phase.GAS);
        catalog.register("methanol", "CO", Phase.LIQUID);
        catalog.register("dimethyl ether", "COC", Phase.GAS);
        catalog.register("ethanol", "CCO", Phase.LIQUID);
        catalog.register("acetic acid", "CC(=O)O", Phase.LIQUID);
        catalog.register("benzene", "c1ccccc1", Phase.LIQUID);
        catalog.register("sulfuric acid", "OS(=O)(=O)O", Phase.LIQUID);
        // The reagents the organic side draws its reactions with: each one is a substance a player has to
        // hold before the rule that asks for it can run, which is why a rule and its reagent are written
        // down at the same time.
        catalog.register("hydrogen cyanide", "C#N", Phase.LIQUID);
        catalog.register("hydroxylamine", "NO", Phase.LIQUID);
        catalog.register("hydrogen peroxide", "OO", Phase.LIQUID);
        catalog.register("nitrous acid", "ON=O", Phase.LIQUID);
        catalog.register("aluminium chloride", "[Al](Cl)(Cl)Cl", Phase.SOLID);
        catalog.register("iron(III) bromide", "[Fe](Br)(Br)Br", Phase.SOLID);
        // The petrochemicals and the building blocks of a modern fibre and a modern medicine: the small
        // molecules a player starts the organic side from - the gases of a refinery, the aromatics an
        // alkylation or a dehydrogenation leaves, the amines and the acid chlorides a ring is built into a
        // fibre or a drug out of. Every one of them is here because a route of the table names it as a
        // reagent or comes out with it, and none of them is a guess at a shape the chemistry already knows.
        catalog.register("butane", "CCCC", Phase.GAS);
        catalog.register("1,3-butadiene", "C=CC=C", Phase.GAS);
        catalog.register("propene", "CC=C", Phase.GAS);
        catalog.register("methylamine", "CN", Phase.GAS);
        catalog.register("ethylbenzene", "CCc1ccccc1", Phase.LIQUID);
        catalog.register("styrene", "C=Cc1ccccc1", Phase.LIQUID);
        catalog.register("o-xylene", "Cc1ccccc1C", Phase.LIQUID);
        catalog.register("m-xylene", "Cc1cccc(C)c1", Phase.LIQUID);
        catalog.register("p-xylene", "Cc1ccc(C)cc1", Phase.LIQUID);
        catalog.register("benzaldehyde", "O=Cc1ccccc1", Phase.LIQUID);
        catalog.register("benzyl alcohol", "OCc1ccccc1", Phase.LIQUID);
        catalog.register("aniline", "Nc1ccccc1", Phase.LIQUID);
        catalog.register("nitrobenzene", "O=[N+]([O-])c1ccccc1", Phase.LIQUID);
        catalog.register("acetyl chloride", "CC(=O)Cl", Phase.LIQUID);
        catalog.register("acrylonitrile", "C=CC#N", Phase.LIQUID);
        catalog.register("phenol", "Oc1ccccc1", Phase.SOLID);
        catalog.register("benzoic acid", "OC(=O)c1ccccc1", Phase.SOLID);
        catalog.register("terephthalic acid", "OC(=O)c1ccc(C(=O)O)cc1", Phase.SOLID);
        catalog.register("isophthalic acid", "OC(=O)c1cccc(C(=O)O)c1", Phase.SOLID);
        catalog.register("o-phenylenediamine", "Nc1ccccc1N", Phase.SOLID);
        catalog.register("p-phenylenediamine", "Nc1ccc(N)cc1", Phase.SOLID);
        catalog.register("p-nitroaniline", "Nc1ccc([N+](=O)[O-])cc1", Phase.SOLID);
        // The reagents a double bond and a carbon skeleton are built with: the phosphorus a chemist puts a
        // double bond where they want it with, and the magnesium reagents a carbon is put on a carbonyl
        // with. A metal that carries a carbon is written as the plain molecule it is, exactly as the oxides
        // and the salts of the industry are.
        catalog.register("triphenylphosphine", "P(c1ccccc1)(c1ccccc1)c1ccccc1", Phase.SOLID);
        catalog.register("methylenetriphenylphosphorane",
                "C=P(c1ccccc1)(c1ccccc1)c1ccccc1", Phase.SOLID);
        catalog.register("methylmagnesium bromide", "C[Mg]Br", Phase.SOLID);
        catalog.register("phenylmagnesium bromide", "[Mg](Br)c1ccccc1", Phase.SOLID);
        return catalog;
    }

    /** The molecule and the phase a substance is looked up by. */
    private record Key(String canonicalKey, Phase phase) {
    }
}
