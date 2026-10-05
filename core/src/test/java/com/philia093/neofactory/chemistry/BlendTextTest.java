package com.philia093.neofactory.chemistry;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the line a pile travels as, the way a stack of something keeps what it holds.
 * <p>
 * A stack of a mixture has to survive a save game and come back as the same pile, and two stacks that were
 * mixed in a different order have to come back as one and the same line, or a machine that was saved mid
 * craft would look for one pile and find another. The fractions have to survive too: a nugget of an alloy
 * is seven and a half millibuckets of copper and the line has to carry the half and not a rounded seven.
 */
class BlendTextTest {

    private static final Chemical COPPER = Chemical.parse("[Cu]");
    private static final Chemical TIN = Chemical.parse("[Sn]");
    private static final Chemical WATER = Chemical.parse("O");

    @Test
    void aMixtureTravelsAsOneLine() {
        Blend bronze = Blend.of(COPPER, 75).plus(Blend.of(TIN, 25));

        String text = BlendText.write(bronze);

        assertEquals(bronze, BlendText.read(text));
        assertTrue(text.contains("75"), text);
        assertTrue(text.contains("25"), text);
    }

    @Test
    void theLineDoesNotDependOnTheOrderThePiecesWereMixed() {
        Blend first = Blend.of(COPPER, 75).plus(Blend.of(TIN, 25));
        Blend second = Blend.of(TIN, 25).plus(Blend.of(COPPER, 75));

        assertEquals(BlendText.write(first), BlendText.write(second));
    }

    @Test
    void aFractionSurvivesTheLineWhole() {
        Blend nugget = Blend.of(COPPER, Fraction.of(15, 2)).plus(Blend.of(TIN, Fraction.of(5, 2)));

        Blend back = BlendText.read(BlendText.write(nugget));

        assertEquals(nugget, back);
        assertEquals(Fraction.of(10), back.total(), "a nugget of alloy is ten millibuckets again");
        assertEquals(Fraction.of(15, 2), back.amountOf(COPPER), "and not a hair of the half lost");
    }

    @Test
    void anEmptyPileIsAnEmptyLine() {
        assertTrue(BlendText.write(Blend.empty()).isEmpty());
        assertTrue(BlendText.read("").isEmpty());
        assertTrue(BlendText.read(null).isEmpty());
    }

    @Test
    void aLineThatIsNoPileIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> BlendText.read("75"), "no substance");
        assertThrows(SmilesException.class, () -> BlendText.read("75;not_a_substance"),
                "a substance that cannot be read");
    }

    @Test
    void aCellOfWaterIsWrittenAsItsThousandMillibuckets() {
        String text = BlendText.write(Blend.of(WATER, Amounts.FLUID_CELL));

        assertEquals("1000;O", text);
        assertEquals(Amounts.FLUID_CELL, BlendText.read(text).total());
    }
}
