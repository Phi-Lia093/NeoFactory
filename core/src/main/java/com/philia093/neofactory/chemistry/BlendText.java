package com.philia093.neofactory.chemistry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Writes a pile as one line of text and reads it back, the way a stack keeps what it holds.
 * <p>
 * A stack of dust and a stack of a cell cannot hold a molecule of copper in one number, because a mixture
 * is not one substance but several: a bronze ingot is copper and tin in one pile, a salt is a lattice of
 * two ions, and both are piles of substances that have to travel together. What a stack therefore carries
 * is the whole pile written out - the amount and the substance of every piece of it, one behind the other -
 * so that an item that holds a mixture is still one item and a few hundred millibuckets of something
 * somebody mixed, see {@link Blend}.
 * <p>
 * <b>The line is meant to survive a save game and a look at it.</b> The substances are written by their
 * SMILES and the amounts as fractions, the pieces are split by a bar and an amount stands before its
 * substance, and the pieces are written in a settled order so that the same pile is always the same line:
 * a stack that was stored and read again, and a stack that was mixed the other way round, come back as one
 * and the same.
 * <p>
 * <b>A substance with no SMILES cannot be written.</b> A molecule that a template built was never spelled
 * out, so a pile that holds one cannot travel this way yet; the game gives such a substance a name of its
 * own and a stack is stored again the moment one is made, which is a later stage and not this one, see
 * {@link ChemicalHolder}.
 */
public final class BlendText {

    /** What stands between two substances of a pile. */
    private static final char BETWEEN_SUBSTANCES = '|';

    /** What stands between the amount of a substance and the substance itself. */
    private static final char BEFORE_SUBSTANCE = ';';

    private BlendText() {
        // Utility class: never instantiated.
    }

    /**
     * Writes a pile as one line.
     *
     * @param blend the pile
     * @return the line, empty for a pile that holds nothing
     */
    public static String write(Blend blend) {
        List<String> pieces = new ArrayList<>(blend.components().size());
        for (Map.Entry<Chemical, Fraction> entry : blend.components().entrySet()) {
            pieces.add(entry.getValue().toString() + BEFORE_SUBSTANCE + entry.getKey().smiles());
        }
        Collections.sort(pieces);
        return String.join(String.valueOf(BETWEEN_SUBSTANCES), pieces);
    }

    /**
     * Reads a pile from a line written by {@link #write(Blend)}.
     *
     * @param text the line, {@code null} and empty for a pile that holds nothing
     * @return the pile
     * @throws IllegalArgumentException when the line is not written the way a pile is
     * @throws SmilesException when a substance of the line cannot be read
     */
    public static Blend read(String text) {
        if (text == null || text.isEmpty()) {
            return Blend.empty();
        }
        Map<Chemical, Fraction> amounts = new LinkedHashMap<>();
        for (String piece : text.split(Pattern.quote(String.valueOf(BETWEEN_SUBSTANCES)), -1)) {
            int cut = piece.indexOf(BEFORE_SUBSTANCE);
            if (cut < 0) {
                throw new IllegalArgumentException("A piece of a pile writes an amount and a substance: "
                        + piece);
            }
            amounts.merge(Chemical.parse(piece.substring(cut + 1)),
                    readFraction(piece.substring(0, cut).trim()), Fraction::plus);
        }
        return Blend.of(amounts);
    }

    /** Reads one amount, a whole number or one over another. */
    private static Fraction readFraction(String text) {
        int slash = text.indexOf('/');
        if (slash < 0) {
            return Fraction.of(Long.parseLong(text));
        }
        return Fraction.of(Long.parseLong(text.substring(0, slash).trim()),
                Long.parseLong(text.substring(slash + 1).trim()));
    }
}
