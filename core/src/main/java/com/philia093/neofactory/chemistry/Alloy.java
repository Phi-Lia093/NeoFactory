package com.philia093.neofactory.chemistry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * An alloy: several metals standing beside one another, which is what bronze is and what no molecule is.
 * <p>
 * <b>The kernel is built on the line between a substance that is one thing and a substance that is
 * several,</b> see {@link Chemical}: water is one compound and bronze is not - its copper and its tin are not
 * bonded, and a reaction that touches the bronze touches the copper and the tin one at a time. An alloy is
 * therefore a mixture of substances and never a molecule, and a string like {@code Cu3Sn} is refused exactly
 * because it reads like one, see {@link Chemical#parse}.
 * <p>
 * <b>An alloy is written as its parts and the share of each,</b> and the dot of a SMILES string is how the
 * trade says a mixture: {@code [Cu].[Sn]} is copper and tin beside one another. The shares are what make it
 * an alloy rather than a list - bronze is some eleven parts of copper to one of tin - and they are kept as
 * the small whole numbers the trade uses and read as fractions of the whole, see {@link #blend}.
 * <p>
 * <b>What the catalog keeps of an alloy is the whole of that:</b> the name a screen prints, the parts as
 * substances of the catalog, and how much of each the alloy is made of. A part that names no substance of the
 * catalog is refused where it is written, the way every other line of the catalog is, so an alloy can never
 * be made of something the game does not know.
 */
public record Alloy(String name, List<Part> parts, Phase phase) {

    /** One substance of an alloy and how many parts of it the alloy is made of. */
    public record Part(Substance substance, Fraction share) {

        /** Checks the fields, so a broken part fails where it is written. */
        public Part {
            Objects.requireNonNull(substance, "substance");
            Objects.requireNonNull(share, "share");
            if (share.compareTo(Fraction.ZERO) <= 0) {
                throw new IllegalArgumentException("A part of an alloy is a positive amount: " + share);
            }
        }

        /** The substance this part is, which is what a balance is kept in. */
        public Chemical chemical() {
            return substance.chemical();
        }

        /** The part written as a substance and its share, the same way every time. */
        String writtenPart() {
            return share.numerator() + "/" + share.denominator() + ":" + substance.canonicalKey();
        }
    }

    /** Checks the fields, so a broken alloy fails where it is written. */
    public Alloy {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(phase, "phase");
        parts = List.copyOf(Objects.requireNonNull(parts, "parts"));
        if (name.isBlank()) {
            throw new IllegalArgumentException("An alloy needs a name");
        }
        if (parts.isEmpty()) {
            throw new IllegalArgumentException("An alloy is made of at least one substance: " + name);
        }
    }

    /**
     * An alloy of parts, which is how one is written down.
     *
     * @param name name a screen prints
     * @param phase state the alloy stands in
     * @param parts the substances it is made of, with the share of each
     * @return the alloy
     */
    public static Alloy of(String name, Phase phase, Part... parts) {
        return new Alloy(name, List.of(parts), phase);
    }

    /**
     * One part of an alloy, a substance of the catalog and how many parts of it the alloy is made of.
     *
     * @param substance substance of the catalog
     * @param share parts of it in the whole, a small whole number such as the eleven of bronze
     * @return the part
     */
    public static Part part(Substance substance, long share) {
        return new Part(substance, Fraction.of(share));
    }

    /** The substances this alloy is made of, in the order they were written. */
    public List<Chemical> chemicals() {
        List<Chemical> chemicals = new ArrayList<>();
        for (Part part : parts) {
            chemicals.add(part.chemical());
        }
        return chemicals;
    }

    /** The alloy written the way the trade writes a mixture: the parts of it with dots between them. */
    public String smiles() {
        StringBuilder text = new StringBuilder();
        for (Part part : parts) {
            if (text.length() > 0) {
                text.append('.');
            }
            text.append(part.chemical().smiles());
        }
        return text.toString();
    }

    /**
     * A key that tells two writings of one alloy apart: its parts in a settled order beside their shares and
     * the state it stands in.
     * <p>
     * The order of the parts is not the order somebody happened to type them in - bronze is copper and tin
     * whichever way round it is written - so the key sorts them, and one alloy written two ways is one entry
     * of the catalog, see {@link Substances#registerAlloy}.
     *
     * @return the key
     */
    public String key() {
        List<String> written = new ArrayList<>();
        for (Part part : parts) {
            written.add(part.writtenPart());
        }
        Collections.sort(written);
        return String.join(";", written) + "|" + phase;
    }

    /**
     * The pile one amount of this alloy stands for, every part of it by its share of the whole.
     * <p>
     * This is the whole of why an alloy is a mixture and not a molecule: what stands in a pot of it is a
     * quantity of each of its metals, so a hundred millibuckets of bronze is some ninety-two of copper and
     * eight of tin, and a reaction that takes the whole of it takes those two amounts and no other.
     *
     * @param amount how much of the alloy stands in the pot
     * @return the pile of its parts, in millibuckets
     */
    public Blend blend(Fraction amount) {
        Objects.requireNonNull(amount, "amount");
        Fraction total = Fraction.ZERO;
        for (Part part : parts) {
            total = total.plus(part.share());
        }
        Blend pile = Blend.empty();
        for (Part part : parts) {
            pile = pile.plus(Blend.of(part.chemical(), part.share().dividedBy(total)).times(amount));
        }
        return pile;
    }

    @Override
    public String toString() {
        return name + "(" + smiles() + ", " + phase + ")";
    }
}