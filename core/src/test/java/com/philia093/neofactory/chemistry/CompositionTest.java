package com.philia093.neofactory.chemistry;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the count of elements every balance is written in.
 * <p>
 * The rules that carry the rest of the module are three: a count of zero is nothing and is dropped, the
 * difference of two compositions may count below zero because a reaction is written as one subtraction,
 * and the formula is written in Hill order so that two compositions of the same makeup print as one string.
 */
class CompositionTest {

    @Test
    void aFormulaIsWrittenInHillOrder() {
        assertEquals("CH4", Composition.of(Map.of("C", 1, "H", 4)).formula());
        assertEquals("C2H6O", Composition.of(Map.of("C", 2, "H", 6, "O", 1)).formula());

        // Without carbon every element is alphabetical, hydrogen included.
        assertEquals("H2O", Composition.of(Map.of("H", 2, "O", 1)).formula());
        assertEquals("H2O4S", Composition.of(Map.of("H", 2, "S", 1, "O", 4)).formula());

        assertEquals("", Composition.empty().formula());
        assertEquals("O2", Composition.of("O", 2).formula());
        assertEquals("Na", Composition.of("Na", 1).formula());
    }

    @Test
    void aCountOfZeroIsNothing() {
        Composition value = Composition.of(Map.of("C", 1, "H", 0));

        assertEquals(1, value.amountOf("C"));
        assertEquals(0, value.amountOf("H"));
        assertEquals(1, value.elements().size(), "the zero of hydrogen was dropped");
        assertTrue(Composition.of("O", 0).isEmpty());
        assertTrue(Composition.empty().isEmpty());
    }

    @Test
    void addingSubtractingAndScalingCounts() {
        Composition water = Composition.of(Map.of("H", 2, "O", 1));

        assertEquals("H4O2", water.times(2).formula());
        assertEquals(water, Composition.of(Map.of("H", 4, "O", 2)).minus(water));
        assertTrue(water.minus(water).isEmpty(), "a substance against itself changes nothing");
        assertEquals(water, water.plus(Composition.empty()));
        assertTrue(water.times(0).isEmpty());
    }

    @Test
    void aDifferenceMayCountBelowZero() {
        Composition difference = Composition.of("H", 2).minus(Composition.of("H", 3));

        assertEquals(-1, difference.amountOf("H"));
        assertFalse(difference.isPositive(), "a side written the wrong way round is not a real species");
        assertTrue(Composition.of("H", 1).isPositive());
    }

    @Test
    void anUnknownElementIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> Composition.of("Xx", 1));
        assertThrows(IllegalArgumentException.class, () -> Composition.of("iron", 1));
    }

    @Test
    void aCompositionDescribesItself() {
        assertEquals("Composition(C2H6O)", Composition.of(Map.of("C", 2, "H", 6, "O", 1)).toString());
    }
}
