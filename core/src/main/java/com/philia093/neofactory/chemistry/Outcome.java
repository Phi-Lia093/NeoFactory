package com.philia093.neofactory.chemistry;

import java.util.Objects;

/**
 * What happens to a pot: what is taken out of it, what is put back, and where the answer came from.
 * <p>
 * One shape serves both ways the module reacts, so that a machine never has to know which way it went. A
 * recipe of the catalog answers with one of these, and so does a rewriting of an organic molecule, and the
 * machine does the same thing with either: it takes what {@link #consumed()} names out of its pot, it puts
 * what {@link #produced()} names back in, and it spends or gains the electrons of the answer. What the two
 * ways do not share is quiet: an answer that does not keep the rule of the pot is no answer at all, see
 * {@link BlendConservation}, and both of them are made to keep it where they are found.
 * <p>
 * <b>The amounts are in millibuckets</b>, because a machine holds measured amounts and not molecules, see
 * {@link Blend}. A recipe that was written for one run is scaled to what the pot really holds before it is
 * handed over, so the answer always says what the very pot in question loses and gains.
 *
 * @param source the name of the recipe or of the template this answer came from, for a log and a screen
 * @param consumed what the pot loses
 * @param produced what the pot gains
 * @param electrons electrons the reaction takes in, negative when it gives them out
 */
public record Outcome(String source, Blend consumed, Blend produced, int electrons) {

    /** Checks the fields, so an answer that names nothing fails where it is built. */
    public Outcome {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(consumed, "consumed");
        Objects.requireNonNull(produced, "produced");
        if (consumed.isEmpty() && produced.isEmpty()) {
            throw new IllegalArgumentException("An outcome that changes nothing is no reaction: " + source);
        }
    }

    /** {@code true} when this answer takes nothing out of the pot but puts something in. */
    public boolean onlyAdds() {
        return consumed.isEmpty();
    }

    @Override
    public String toString() {
        return "Outcome(" + source + ", " + consumed + " -> " + produced + ", " + electrons + "e-)";
    }
}
