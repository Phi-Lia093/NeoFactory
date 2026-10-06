package com.philia093.neofactory.chemistry;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Checks the reactions a ring is given a group by, and the one that is only done to a ring that carries
 * something pulling electrons out of it.
 * <p>
 * The whole of the aromatic side of the table is one shape drawn five times over, so what is asked here is
 * the shape and the one thing that differs between the rules: that the group an electrophile is sent to is
 * the one the group already on the ring sends it to, and that a plain ring is left alone by the one
 * substitution that needs an electron-poor ring to go at all.
 */
class AromaticReactionsTest {

    /** The engine over the rules the organic side ships with. */
    private static final PolarEngine ENGINE = new PolarEngine(PolarReactions.all());

    /** The acid a nitration is run over, as the vessel the reaction is offered knows it. */
    private static final Conditions OVER_SULFURIC_ACID = Conditions.builder()
            .warmth(Warmth.HEATED).catalyst(Chemical.parse("OS(=O)(=O)O")).build();

    /** The acid a Friedel-Crafts reaction is run over. */
    private static final Conditions OVER_ALUMINIUM_CHLORIDE = Conditions.builder()
            .warmth(Warmth.HEATED).catalyst(Chemical.parse("[Al](Cl)(Cl)Cl")).build();

    @Test
    void nitricAcidGivesARingItsNitroGroup() {
        Reaction reaction = best(OVER_SULFURIC_ACID, "c1ccccc1", "O[N+](=O)[O-]", "OS(=O)(=O)O");

        assertNotNull(reaction, "benzene, nitric acid and the acid it is run over");
        assertEquals(Set.of("C6H5NO2", "H2O"), formulas(reaction.products()),
                "nitrobenzene and water, and the sulfuric acid on neither side of it");
        assertEquals(1, Sites.nitros(onlyCarbonProduct(reaction).structure()).size(),
                "the ring came out with one nitro group on it");
    }

    @Test
    void aGroupOnTheRingSendsTheNitroGroupWhereItAlwaysGoes() {
        // Toluene: the methyl lends electrons, so the nitro group lands beside it and not past a carbon.
        Reaction reaction = best(OVER_SULFURIC_ACID, "Cc1ccccc1", "O[N+](=O)[O-]", "OS(=O)(=O)O");

        assertNotNull(reaction, "toluene and nitric acid");
        assertEquals(Set.of("C7H7NO2", "H2O"), formulas(reaction.products()));
        assertEquals(1, ringDistance(onlyCarbonProduct(reaction).structure(), "N", "C"),
                "the nitro group stands beside the methyl");
    }

    @Test
    void aLewisAcidPutsAnAlkylGroupOnARing() {
        Reaction reaction = best(OVER_ALUMINIUM_CHLORIDE, "c1ccccc1", "CBr", "[Al](Cl)(Cl)Cl");

        assertNotNull(reaction, "benzene, bromomethane and aluminium chloride");
        assertEquals(Set.of("C7H8", "BrH"), formulas(reaction.products()), "toluene and the acid");
    }

    @Test
    void aLewisAcidPutsAnAcylGroupOnARing() {
        Reaction reaction = best(OVER_ALUMINIUM_CHLORIDE, "c1ccccc1", "CC(=O)Cl", "[Al](Cl)(Cl)Cl");

        assertNotNull(reaction, "benzene, an acid chloride and aluminium chloride");
        assertEquals(Set.of("C8H8O", "ClH"), formulas(reaction.products()), "the ketone and the acid");
        assertEquals(1, Sites.carbonyls(onlyCarbonProduct(reaction).structure()).size(),
                "what hangs on the ring is a carbonyl");
    }

    @Test
    void aHalogenTakesThePlaceOfAHydrogenOverIronBromide() {
        Reaction reaction = best(Conditions.builder().warmth(Warmth.HEATED)
                .catalyst(Chemical.parse("[Fe](Br)(Br)Br")).build(),
                "c1ccccc1", "BrBr", "[Fe](Br)(Br)Br");

        assertNotNull(reaction, "benzene, bromine and the bromide of iron");
        assertEquals(Set.of("C6H5Br", "BrH"), formulas(reaction.products()), "bromobenzene and the acid");
    }

    @Test
    void aRingThatPullsElectronsIsSubstitutedAndAPlainOneIsNot() {
        // Nitrochlorobenzene and lye: the chlorine goes. Chlorobenzene and lye: nothing at all.
        Reaction pulled = best(Conditions.at(Warmth.HEATED), "O=[N+]([O-])c1ccc(Cl)cc1", "[OH-]");

        assertNotNull(pulled, "a ring with a nitro group beside its chlorine");
        assertEquals(Set.of("C6H5NO3", "Cl"), formulas(pulled.products()),
                "the phenol and the chloride that left");

        Reaction plain = best(Conditions.at(Warmth.HEATED), "Clc1ccccc1", "[OH-]");
        assertNull(plain, "and the same chloride on a ring that pulls nothing is left alone");
    }

    /** The one reaction an engine would run in a vessel of the conditions and the substances named. */
    private static Reaction best(Conditions conditions, String... smiles) {
        Mixture pot = Mixture.empty();
        for (String one : smiles) {
            pot = pot.plus(Mixture.of(Chemical.parse(one), 1));
        }
        return ENGINE.best(System.of(pot, Phase.LIQUID).with(conditions));
    }

    /** The formulas of the substances of a pile. */
    private static Set<String> formulas(Mixture mixture) {
        Set<String> formulas = new TreeSet<>();
        for (Chemical chemical : mixture.components().keySet()) {
            formulas.add(chemical.composition().formula());
        }
        return formulas;
    }

    /** The one substance of a pile that holds carbon. */
    private static Chemical onlyCarbonProduct(Reaction reaction) {
        for (Chemical chemical : reaction.products().components().keySet()) {
            if (chemical.composition().formula().startsWith("C")) {
                return chemical;
            }
        }
        throw new IllegalStateException("no carbon in " + reaction.products());
    }

    /** How far apart around a ring the atoms carrying two elements stand. */
    private static int ringDistance(Molecule molecule, String first, String second) {
        List<Integer> ring = Rings.cycles(molecule).get(0);
        int at = -1;
        int other = -1;
        for (int index = 0; index < ring.size(); index++) {
            int atom = ring.get(index);
            for (int neighbour : molecule.neighbours(atom)) {
                if (ring.contains(neighbour)) {
                    continue;
                }
                if (molecule.atom(neighbour).element().equals(first)) {
                    at = index;
                }
                if (molecule.atom(neighbour).element().equals(second)) {
                    other = index;
                }
            }
        }
        int apart = Math.abs(at - other);
        return Math.min(apart, ring.size() - apart);
    }
}
