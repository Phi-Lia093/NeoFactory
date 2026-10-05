package com.philia093.neofactory.chemistry;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the engine that rewrites an organic molecule by a template.
 * <p>
 * An organic reaction is checked by its two molecules and not by its atoms: what goes in, what comes out,
 * and above all the formula of the product, which is where a wrong rewriting shows itself. A hydrogenation
 * that forgot the hydrogens would come back with a formula two hydrogen short, and no count of bonds would
 * catch that - the formula does. The engine is also checked for what it must refuse: a molecule without the
 * pattern of the template, and a rewriting that makes a substance the catalog never heard of.
 */
class TemplateEngineTest {

    private static Substances organicCatalog() {
        return new Substances()
                .register("ethene", "C=C", Phase.GAS)
                .register("hydrogen", "[H][H]", Phase.GAS)
                .register("ethane", "CC", Phase.GAS)
                .register("hydrogen chloride", "Cl", Phase.GAS)
                .register("chloroethane", "CCCl", Phase.GAS);
    }

    private static ReactionTemplate hydrogenation() {
        return ReactionTemplate.parse("hydrogenation", "[C:1]=[C:2].[H][H]>>[C:1][C:2]");
    }

    @Test
    void aTemplateRemembersWhichAtomIsWhich() {
        Molecule pattern = SmilesParser.parse("[C:1]=[C:2]");

        assertEquals(1, pattern.atom(0).mapClass());
        assertEquals(2, pattern.atom(1).mapClass());
        assertEquals(0, SmilesParser.parse("C=C").atom(0).mapClass(), "a molecule carries no map number");
    }

    @Test
    void aHydrogenationIsOneLineAndTheHydrogensFollow() {
        Substances catalog = organicCatalog();
        TemplateEngine engine = new TemplateEngine(catalog, List.of(hydrogenation()));
        Mixture pot = Mixture.of(catalog.byName("ethene").chemical(), 1)
                .plus(Mixture.of(catalog.byName("hydrogen").chemical(), 1));

        Reaction reaction = engine.best(System.of(pot, Phase.GAS));

        assertNotNull(reaction, "an alkene and hydrogen are an alkane");
        assertEquals(pot, reaction.reactants());
        assertEquals(Mixture.of(catalog.byName("ethane").chemical(), 1), reaction.products());
        assertEquals("C2H6", reaction.products().composition().formula(),
                "the two carbons take on two hydrogens each of their own accord");
        assertTrue(Conservation.balanced(reaction));
    }

    @Test
    void anAdditionFollowsTheSameShape() {
        Substances catalog = organicCatalog();
        TemplateEngine engine = new TemplateEngine(catalog,
                List.of(ReactionTemplate.parse("hydrochlorination",
                        "[C:1]=[C:2].[Cl:3]>>[C:1][C:2][Cl:3]")));
        Mixture pot = Mixture.of(catalog.byName("ethene").chemical(), 1)
                .plus(Mixture.of(catalog.byName("hydrogen chloride").chemical(), 1));

        Reaction reaction = engine.best(System.of(pot, Phase.GAS));

        assertNotNull(reaction, "an alkene and an acid are a halogenated alkane");
        assertEquals(Mixture.of(catalog.byName("chloroethane").chemical(), 1), reaction.products());
        assertEquals("C2H5Cl", reaction.products().composition().formula());
    }

    @Test
    void aMoleculeWithoutThePatternIsLeftAlone() {
        Substances catalog = organicCatalog();
        TemplateEngine engine = new TemplateEngine(catalog, List.of(hydrogenation()));
        System pot = System.of(Mixture.of(catalog.byName("ethane").chemical(), 1), Phase.GAS);

        assertTrue(engine.infer(pot).isEmpty(), "ethane holds no double bond to hydrogenate");
        assertNull(engine.best(pot));
    }

    @Test
    void aProductTheCatalogDoesNotHoldIsNoProduct() {
        Substances catalog = new Substances()
                .register("ethene", "C=C", Phase.GAS)
                .register("hydrogen", "[H][H]", Phase.GAS);
        TemplateEngine engine = new TemplateEngine(catalog, List.of(hydrogenation()));
        Mixture pot = Mixture.of(catalog.byName("ethene").chemical(), 1)
                .plus(Mixture.of(catalog.byName("hydrogen").chemical(), 1));

        assertTrue(engine.infer(System.of(pot, Phase.GAS)).isEmpty(),
                "ethane is not a substance of this catalog");
    }

    @Test
    void aTemplateThatCannotBeReadIsRefused() {
        assertThrows(SmilesException.class, () -> ReactionTemplate.parse("broken", "C=C"),
                "a template without an arrow");
        assertThrows(SmilesException.class, () -> ReactionTemplate.parse("broken", ">>CC"),
                "a template with nothing going in");
        assertThrows(SmilesException.class, () -> ReactionTemplate.parse("broken", "C=C>>"),
                "a template with nothing coming out");
        assertThrows(SmilesException.class, () -> ReactionTemplate.parse("broken", "C=C>>CC>>CC"),
                "a template with two arrows");
    }
}
