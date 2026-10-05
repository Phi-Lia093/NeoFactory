package com.philia093.neofactory.chemistry;

import java.util.Set;

/**
 * The element symbols the chemistry module knows, and the ten of them SMILES writes bare.
 * <p>
 * A molecule is written as the symbols of the elements it is built from, so somewhere the module has to
 * know which strings are real: {@code "Na"} is a symbol and {@code "Naa"} is a typo, and a typo that slips
 * through becomes a species of its own and poisons every balance it touches. The table here is therefore
 * the one the parser validates against, and it is the whole of the periodic table rather than the handful
 * of elements a first reaction needs, because the catalog of a factory grows towards every element there
 * is and a table that has to be widened later is a table that was wrong before.
 * <p>
 * <b>The organic subset is the other half of the table.</b> SMILES writes ten elements without brackets
 * and fills them with implicit hydrogen - {@link DefaultValence} says how many - while every other element,
 * a metal above all, is written between brackets with its charge spelled out. That line is what keeps
 * {@code C} (a carbon of an organic molecule) and {@code [C]} (a bare carbon atom) apart, and it is the
 * reason no element of the metal side is ever handed a hydrogen it did not ask for.
 */
public final class Elements {

    /**
     * The ten elements SMILES writes without brackets and fills with implicit hydrogen.
     * <p>
     * This is the "organic subset" of the SMILES specification, and it is a rule and not a suggestion: a
     * bare symbol of any other element - a metal of the industry, a rare gas - is refused by the parser,
     * because the implicit hydrogen it would be handed is a hydrogen the molecule never had. A rare gas
     * that carries a hydrogen is exactly the kind of quiet nonsense a chemistry module must not produce.
     */
    private static final Set<String> ORGANIC_SUBSET =
            Set.of("B", "C", "N", "O", "P", "S", "F", "Cl", "Br", "I");

    /**
     * Every element symbol of the periodic table, the ones a bracket atom may name.
     * <p>
     * The list is written out in full on purpose. A module that accepts the element a reaction happens to
     * need today and refuses the rest would have to be widened by every later age of the industry, and the
     * widening is exactly where a typo slips in unnoticed; a complete table fails a bad symbol at once.
     */
    private static final Set<String> SYMBOLS = Set.of(
            "H", "He", "Li", "Be", "B", "C", "N", "O", "F", "Ne",
            "Na", "Mg", "Al", "Si", "P", "S", "Cl", "Ar", "K", "Ca",
            "Sc", "Ti", "V", "Cr", "Mn", "Fe", "Co", "Ni", "Cu", "Zn",
            "Ga", "Ge", "As", "Se", "Br", "Kr", "Rb", "Sr", "Y", "Zr",
            "Nb", "Mo", "Tc", "Ru", "Rh", "Pd", "Ag", "Cd", "In", "Sn",
            "Sb", "Te", "I", "Xe", "Cs", "Ba", "La", "Ce", "Pr", "Nd",
            "Pm", "Sm", "Eu", "Gd", "Tb", "Dy", "Ho", "Er", "Tm", "Yb",
            "Lu", "Hf", "Ta", "W", "Re", "Os", "Ir", "Pt", "Au", "Hg",
            "Tl", "Pb", "Bi", "Po", "At", "Rn", "Fr", "Ra", "Ac", "Th",
            "Pa", "U", "Np", "Pu", "Am", "Cm", "Bk", "Cf", "Es", "Fm",
            "Md", "No", "Lr", "Rf", "Db", "Sg", "Bh", "Hs", "Mt", "Ds",
            "Rg", "Cn", "Nh", "Fl", "Mc", "Lv", "Ts", "Og");

    private Elements() {
        // Utility class: never instantiated.
    }

    /**
     * {@code true} when a string is the symbol of an element the module knows.
     *
     * @param symbol symbol to test, may be {@code null}
     * @return {@code true} when the symbol names an element
     */
    public static boolean isKnown(String symbol) {
        return symbol != null && SYMBOLS.contains(symbol);
    }

    /**
     * {@code true} when an element is one SMILES writes bare and fills with implicit hydrogen.
     *
     * @param symbol symbol to test, may be {@code null}
     * @return {@code true} when the element belongs to the organic subset
     */
    public static boolean isOrganicSubset(String symbol) {
        return symbol != null && ORGANIC_SUBSET.contains(symbol);
    }
}
