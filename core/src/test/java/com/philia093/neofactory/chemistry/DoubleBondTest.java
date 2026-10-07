package com.philia093.neofactory.chemistry;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Checks that a reaction which makes a double bond writes which way round the groups at its two ends settle.
 * <p>
 * A double bond does not turn about its own axis, so an elimination that takes water out of an alcohol and a
 * Wittig that takes the oxygen off a carbonyl both leave a pair of groups fixed where they were put, and the
 * trade knows which of the two ways round each of them leaves them. What is asked here is that the engine
 * writes that down: the alkene an elimination leaves, the alkene a Wittig is used for, and the marks that say
 * so, which are the whole of what makes E-butene and Z-butene two substances and not one.
 */
class DoubleBondTest {

    /** The engine over the rules the organic side ships with. */
    private static final PolarEngine ENGINE = new PolarEngine(PolarReactions.all());

    /** A piece of the trade's own reagent: the ylide of triphenylphosphine with an ethyl group on the carbon. */
    private static final String YLIDE = "CC=P(c1ccccc1)(c1ccccc1)c1ccccc1";

    @Test
    void aDoubleBondIsWrittenTheWayAStepLeavesIt() {
        Molecule plain = SmilesParser.parse("CC=CC");
        Bond bond = theDoubleBond(plain);

        assertEquals('E', descriptor(DoubleBond.set(plain, bond, 'E')), "the two methyls settle apart");
        assertEquals('Z', descriptor(DoubleBond.set(plain, bond, 'Z')), "the two methyls settle on one side");
        assertEquals('E',
                descriptor(SmilesParser.parse(SmilesWriter.write(DoubleBond.set(plain, bond, 'E')))),
                "and the way the molecule is written down reads back the same");
    }

    @Test
    void aDoubleBondWithNothingToBeOnASideOfIsLeftAlone() {
        // Ethene: one group at each end, so there is no pair of groups to settle either way and no mark to be
        // written. A mark put there would name a configuration the molecule does not have.
        Molecule ethene = SmilesParser.parse("C=C");

        assertEquals(ethene.canonicalKey(),
                DoubleBond.set(ethene, theDoubleBond(ethene), 'E').canonicalKey(),
                "ethene has no two ways round and nothing is written on it");
    }

    @Test
    void aDehydrationLeavesTheMoreStableAlkene() {
        Reaction reaction = best("CCC(C)O");

        assertNotNull(reaction, "butan-2-ol over a flame");
        assertEquals(Set.of("C4H8", "H2O"), formulas(reaction.products()), "butene and the water it gave up");
        assertEquals('E', descriptor(productWith(reaction, "C4H8")), "and the two methyls settle apart");
    }

    @Test
    void aWittigLeavesTheAlkeneTheReagentIsUsedFor() {
        Reaction reaction = best(Warmth.AMBIENT, YLIDE, "CCC=O");

        assertNotNull(reaction, "an ethylidene ylide and propanal");
        assertEquals('Z', descriptor(productWith(reaction, "C5H10")),
                "the two carbons the double bond was closed between keep their groups on one side");
    }

    /** The first double bond of a molecule, which is the one a test marked. */
    private static Bond theDoubleBond(Molecule molecule) {
        for (Bond bond : molecule.bonds()) {
            if (bond.order() == 2 && !bond.isAromatic()) {
                return bond;
            }
        }
        throw new IllegalStateException("The molecule holds no double bond");
    }

    /** The configuration a molecule's double bond reads, or {@code 0} when it reads none. */
    private static char descriptor(Molecule molecule) {
        for (Bond bond : molecule.bonds()) {
            char descriptor = Cip.descriptor(molecule, bond);
            if (descriptor != 0) {
                return descriptor;
            }
        }
        return 0;
    }

    /** The one substance of a pile of a named formula, so that the water and the oxide are left aside. */
    private static Molecule productWith(Reaction reaction, String formula) {
        for (Chemical chemical : reaction.products().components().keySet()) {
            if (chemical.composition().formula().equals(formula)) {
                return chemical.structure();
            }
        }
        throw new IllegalStateException("No product of the formula " + formula);
    }

    /** The one reaction an engine would run at a flame in a vessel of the substances named. */
    private static Reaction best(String... smiles) {
        return best(Warmth.HEATED, smiles);
    }

    /** The one reaction an engine would run at a setting in a vessel of the substances named. */
    private static Reaction best(Warmth warmth, String... smiles) {
        Mixture pot = Mixture.empty();
        for (String one : smiles) {
            pot = pot.plus(Mixture.of(Chemical.parse(one), 1));
        }
        return ENGINE.best(System.of(pot, Phase.LIQUID).with(Conditions.at(warmth)));
    }

    /** The formulas of the substances of a pile. */
    private static Set<String> formulas(Mixture mixture) {
        Set<String> formulas = new TreeSet<>();
        for (Chemical chemical : mixture.components().keySet()) {
            formulas.add(chemical.composition().formula());
        }
        return formulas;
    }
}
