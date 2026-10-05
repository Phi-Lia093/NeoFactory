package com.philia093.neofactory.render;

import com.badlogic.gdx.graphics.Color;
import com.philia093.neofactory.chemistry.SkeletalLayout;
import com.philia093.neofactory.chemistry.SkeletalStructure;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the drawing of a molecule by the pixels it paints and not by eye.
 * <p>
 * What a renderer owes its caller is arithmetic: a double bond has to paint more than a single one and a
 * triple more than a double, the ring of an aromatic molecule has to carry the circle that a plain ring has
 * not, and an atom that is not a carbon has to come back as a name while a ring of carbon comes back as no
 * name at all. Every one of those is counted here, so a picture that was painted wrong fails a test rather
 * than a pair of eyes.
 */
class SkeletalRendererTest {

    /** Side of the square the drawings are painted into. */
    private static final int SIZE = 64;

    /** Colour the bonds are painted in. */
    private static final Color INK = new Color(0f, 0f, 0f, 1f);

    @Test
    void aDoubleBondIsMoreThanASingleOneAndATripleMoreThanADouble() {
        int single = painted("CC");
        int doubled = painted("C=C");
        int tripled = painted("C#C");

        assertTrue(doubled > single, "a double bond is painted with more than one line");
        assertTrue(tripled > doubled, "a triple bond is painted with more than two lines");
    }

    @Test
    void anAromaticRingCarriesItsCircle() {
        assertTrue(painted("c1ccccc1") > painted("C1CCCCC1"),
                "the ring of benzene carries a circle a plain ring of the same shape has not");
    }

    @Test
    void aHeteroatomIsNamedAndACarbonIsNot() {
        List<SkeletalRenderer.Label> labels = SkeletalRenderer.labels(SkeletalLayout.layout("CCO"), SIZE);

        assertEquals(1, labels.size(), "only the oxygen of ethanol is named");
        assertEquals("OH", labels.get(0).text());

        assertTrue(SkeletalRenderer.labels(SkeletalLayout.layout("c1ccccc1"), SIZE).isEmpty(),
                "a ring of carbon carries no name at all");
    }

    @Test
    void anAtomThatStandsAloneIsNamedByItsFormula() {
        List<SkeletalRenderer.Label> labels = SkeletalRenderer.labels(SkeletalLayout.layout("C"), SIZE);

        assertEquals(1, labels.size(), "a lone atom is named");
        assertEquals("CH4", labels.get(0).text());
    }

    /** How many pixels of a molecule are painted at all. */
    private static int painted(String smiles) {
        SkeletalStructure structure = SkeletalLayout.layout(smiles);
        int[] pixels = SkeletalRenderer.draw(structure, SIZE, INK, INK);
        int count = 0;
        for (int pixel : pixels) {
            if ((pixel & 0xFF) != 0) {
                count++;
            }
        }
        return count;
    }
}
