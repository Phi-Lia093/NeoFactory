package com.philia093.neofactory.chemistry;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks that the groups of a molecule are found by their shape and not by its name.
 * <p>
 * The pairs that are easy to confuse are the ones asked for: an acid is not an ester although both carry a
 * carbonyl, an aldehyde is not a ketone although both are carbonyls, a hydrogen left to its valence counts
 * as much as one written out, and the blurred bonds of a benzene ring are not the double bond of an alkene.
 * A finder that answered any of those wrongly would hand a rule a molecule it was never written for.
 */
class SitesTest {

    @Test
    void aCarbonylIsFoundWhereverItStands() {
        assertEquals(1, Sites.carbonyls(SmilesParser.parse("CC=O")).size());
        assertEquals(1, Sites.carbonyls(SmilesParser.parse("CC(=O)C")).size());
        assertEquals(1, Sites.carbonyls(SmilesParser.parse("CC(=O)O")).size());
        assertEquals(0, Sites.carbonyls(SmilesParser.parse("CCO")).size());
    }

    @Test
    void anAldehydeIsNotAKetoneAndTheOtherWayRound() {
        assertEquals(1, Sites.aldehydes(SmilesParser.parse("CC=O")).size(), "ethanal");
        assertEquals(0, Sites.ketones(SmilesParser.parse("CC=O")).size(), "ethanal is no ketone");

        assertEquals(0, Sites.aldehydes(SmilesParser.parse("CC(=O)C")).size(), "propanone is no aldehyde");
        assertEquals(1, Sites.ketones(SmilesParser.parse("CC(=O)C")).size(), "propanone");
    }

    @Test
    void anAcidIsNotAnEster() {
        List<Site> acids = Sites.acids(SmilesParser.parse("CC(=O)O"));
        assertEquals(1, acids.size(), "ethanoic acid");
        assertEquals(3, acids.get(0).size(), "the carbon, its oxygen and the hydroxyl oxygen");
        assertEquals(0, Sites.esters(SmilesParser.parse("CC(=O)O")).size(), "an acid is no ester");

        List<Site> esters = Sites.esters(SmilesParser.parse("CC(=O)OC"));
        assertEquals(1, esters.size(), "methyl ethanoate");
        assertEquals(4, esters.get(0).size(), "the carbon, both oxygens and the carbon on the second one");
        assertEquals(0, Sites.acids(SmilesParser.parse("CC(=O)OC")).size(), "an ester is no acid");
    }

    @Test
    void aHydrogenLeftToItsValenceCountsAsMuchAsOneWrittenOut() {
        assertEquals(1, Sites.hydroxyls(SmilesParser.parse("CCO")).size(), "ethanol");
        assertEquals(0, Sites.hydroxyls(SmilesParser.parse("CC[O-]")).size(), "an alkoxide is no hydroxyl");
        assertEquals(1, Sites.hydroxyls(SmilesParser.parse("CO[H]")).size(), "methanol written out");
    }

    @Test
    void aBlurredRingIsNoAlkeneAndAHalideHangsOnACarbon() {
        assertEquals(0, Sites.alkenes(SmilesParser.parse("c1ccccc1")).size(), "benzene is no alkene");
        assertEquals(1, Sites.alkenes(SmilesParser.parse("CC=C")).size(), "propene");

        List<Site> halides = Sites.halides(SmilesParser.parse("CCBr"));
        assertEquals(1, halides.size(), "bromoethane");
        assertEquals("C", SmilesParser.parse("CCBr").atom(halides.get(0).atom(0)).element(), "the carbon");
        assertEquals("Br", SmilesParser.parse("CCBr").atom(halides.get(0).atom(1)).element(), "the bromine");
    }

    @Test
    void everyFinderAnswersWithTheAtomsItNamed() {
        assertTrue(Sites.all(SmilesParser.parse("CC(=O)O")).stream()
                .anyMatch(site -> site.group() == FunctionalGroup.CARBOXYLIC_ACID));
        assertEquals(1, Sites.nitriles(SmilesParser.parse("[C-]#N")).size(), "cyanide");
    }
}
