package com.philia093.neofactory.chemistry;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * One reaction of an organic molecule, written as the pattern of what goes in and the pattern of what
 * comes out.
 * <p>
 * An organic reaction cannot be balanced the way an inorganic one is. Two substances of the same elements
 * and the same formula may be two different molecules - ethanol and dimethyl ether are both
 * {@code C2H6O} - so a balance sees nothing where a chemist sees a reaction, and the writing has to say
 * which atom sits where. A template is that writing: the left side is the pattern the reacting atoms have
 * to stand in, the right side is the pattern they end up in, and a number written on an atom - the map
 * number {@code [C:1]} - says which atom of the left side is the same atom as which atom of the right side.
 * <p>
 * <b>An atom without a map number does not survive the reaction.</b> An atom of the left side that carries
 * no number is one the reaction takes apart and is gone from the product; an atom of the right side that
 * carries no number is one the reaction builds and was not there before. Everything else is carried: the
 * atoms of one number on the left and of the same number on the right are one and the same atom, and the
 * reaction only changes what stands around it.
 * <p>
 * <b>The hydrogens are not written and not carried.</b> A template is a pattern of the skeleton of a
 * molecule and not of every hydrogen it holds; when a double bond becomes a single one the carbon really
 * does take on two more hydrogens, and that follows from the bonds of the rewritten molecule and never from
 * a count a template had to spell out. This is why the module can write a hydrogenation in one short line
 * and still come out with the right formula, see {@link TemplateEngine}.
 */
public final class ReactionTemplate {

    /** What splits what goes in of a template from what comes out of it. */
    public static final String ARROW = ">>";

    private final String name;
    private final List<Molecule> reactants;
    private final List<Molecule> products;

    private ReactionTemplate(String name, List<Molecule> reactants, List<Molecule> products) {
        this.name = Objects.requireNonNull(name, "name");
        this.reactants = List.copyOf(Objects.requireNonNull(reactants, "reactants"));
        this.products = List.copyOf(Objects.requireNonNull(products, "products"));
    }

    /**
     * Reads a template from the line a reaction is written in.
     *
     * @param name the name of the template, for a log line and a test
     * @param reaction what goes in and what comes out, split by {@value #ARROW}, each side one or more
     *        molecules split by a dot
     * @return the template
     * @throws SmilesException when the line cannot be read
     */
    public static ReactionTemplate parse(String name, String reaction) {
        Objects.requireNonNull(reaction, "reaction");
        int arrow = reaction.indexOf(ARROW);
        if (arrow < 0) {
            throw new SmilesException("A template writes what goes in and what comes out, split by '"
                    + ARROW + "': " + reaction);
        }
        if (reaction.indexOf(ARROW, arrow + ARROW.length()) >= 0) {
            throw new SmilesException("A template carries one '" + ARROW + "': " + reaction);
        }
        List<Molecule> reactants = parseSide(reaction.substring(0, arrow));
        List<Molecule> products = parseSide(reaction.substring(arrow + ARROW.length()));
        if (reactants.isEmpty()) {
            throw new SmilesException("A template takes at least one substance in: " + reaction);
        }
        if (products.isEmpty()) {
            throw new SmilesException("A template hands at least one substance out: " + reaction);
        }
        return new ReactionTemplate(name, reactants, products);
    }

    /** Reads one side of a template, one molecule per dot. */
    private static List<Molecule> parseSide(String side) {
        String text = side.trim();
        if (text.isEmpty()) {
            return List.of();
        }
        List<Molecule> molecules = new ArrayList<>();
        for (String part : text.split("\\.", -1)) {
            if (part.isEmpty()) {
                throw new SmilesException("A template names an empty substance: " + side);
            }
            molecules.add(SmilesParser.parse(part));
        }
        return molecules;
    }

    /** The name of this template. */
    public String name() {
        return name;
    }

    /** The pattern of what goes in: one molecule per substance the reaction needs. */
    public List<Molecule> reactants() {
        return reactants;
    }

    /** The pattern of what comes out: one molecule per substance the reaction hands over. */
    public List<Molecule> products() {
        return products;
    }

    @Override
    public String toString() {
        return "ReactionTemplate(" + name + ")";
    }
}
