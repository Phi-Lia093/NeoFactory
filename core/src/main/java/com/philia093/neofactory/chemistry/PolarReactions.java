package com.philia093.neofactory.chemistry;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/**
 * The rules of the polar families: the reactions of a double bond, of a carbonyl, of an alcohol and of a
 * saturated carbon.
 * <p>
 * Every rule is the groups it needs and the steps it is drawn with, and the steps are the same handful the
 * mechanism layer already knows: two ligands across a double bond, a nucleophile into a flat centre, a
 * leaving group out of a saturated one, a leaving group and a hydrogen going together. What the rule adds is
 * which atoms of the vessel those steps are drawn on and which substances the vessel therefore loses and
 * gains, see {@link Pot#react}.
 * <p>
 * <b>The rule table is the priority, and the sharper rule is written first.</b> A rule that names a
 * particular reagent is a better answer for a vessel than one that would fit many, so the rules that find a
 * reagent of their own stand above the ones that would take anything; and a rule that finds no reagent of
 * its own answers nothing rather than guessing, which is what keeps a vessel that could have reacted many
 * ways from reacting the wrong one.
 */
public final class PolarReactions {

    private PolarReactions() {
        // Utility class: never instantiated.
    }

    /**
     * The rules of the polar families, the sharper ones first.
     *
     * @return the table
     */
    public static List<ReactionRule> all() {
        List<ReactionRule> rules = new ArrayList<>();
        rules.add(hydrolysis());
        rules.add(cyanohydrin());
        rules.add(hemiacetal());
        rules.add(hydrogenation());
        rules.add(halogenation());
        rules.add(hydrohalogenation());
        rules.add(hydration());
        rules.add(dehydration());
        return List.copyOf(rules);
    }

    /**
     * A hydroxide taking the place of a halide on a saturated carbon.
     * <p>
     * The carbon is turned over by it - the nucleophile must come in from behind the group that leaves - and
     * that is the whole of what the rule says about the configuration.
     */
    private static ReactionRule hydrolysis() {
        return new ReactionRule("hydrolysis", "substitution",
                EnumSet.of(FunctionalGroup.HALIDE), PolarReactions::hydrolyse);
    }

    private static Reaction hydrolyse(Pot pot) {
        Site halide = first(Sites.halides(pot.molecule()));
        int hydroxide = hydroxide(pot.molecule());
        if (halide == null || hydroxide < 0) {
            return null;
        }
        int carbon = halide.atom(0);
        int halogen = halide.atom(1);
        Molecule product = ElementaryStep.substitution("hydrolysis", hydroxide, carbon, halogen)
                .apply(pot.molecule());
        return pot.react(List.of(pot.substanceOf(carbon), pot.substanceOf(hydroxide)), product, 0);
    }

    /** A cyanide into the carbonyl of an aldehyde or a ketone, the pair of the double bond to the oxygen. */
    private static ReactionRule cyanohydrin() {
        return new ReactionRule("cyanohydrin", "carbonyl addition",
                EnumSet.of(FunctionalGroup.CARBONYL), PolarReactions::cyanohydrin);
    }

    private static Reaction cyanohydrin(Pot pot) {
        Site carbonyl = first(Sites.carbonyls(pot.molecule()));
        int cyanide = cyanide(pot.molecule());
        if (carbonyl == null || cyanide < 0) {
            return null;
        }
        int carbon = carbonyl.atom(0);
        int oxygen = carbonyl.atom(1);
        Molecule product = ElementaryStep.addition("cyanohydrin", cyanide, carbon, oxygen, Face.RE)
                .apply(pot.molecule());
        return pot.react(List.of(pot.substanceOf(carbon), pot.substanceOf(cyanide)), product, 0);
    }

    /**
     * The hydrogen of an alcohol added across the double bond of a carbonyl, which is what a hemiacetal is.
     * <p>
     * Four arrows are drawn: the lone pair of the alcohol on its way into the bond, the pair of the double
     * bond on its way to the oxygen, the bond of the alcohol's own hydrogen breaking, and that hydrogen
     * going to the oxygen the pair went to - so that the carbon comes out a centre whose hand the face
     * decided, and the vessel neither gains nor loses an atom.
     */
    private static ReactionRule hemiacetal() {
        return new ReactionRule("hemiacetal", "carbonyl addition",
                EnumSet.of(FunctionalGroup.ALDEHYDE, FunctionalGroup.HYDROXYL),
                PolarReactions::hemiacetal);
    }

    private static Reaction hemiacetal(Pot pot) {
        Site aldehyde = first(Sites.aldehydes(pot.molecule()));
        Site hydroxyl = first(Sites.hydroxyls(pot.molecule()));
        if (aldehyde == null || hydroxyl == null) {
            return null;
        }
        int carbon = aldehyde.atom(0);
        int oxygen = aldehyde.atom(1);
        int alcoholOxygen = hydroxyl.atom(0);
        int hydrogen = hydrogenOn(pot.molecule(), alcoholOxygen);
        if (hydrogen < 0 || alcoholOxygen == oxygen || pot.substanceOf(alcoholOxygen)
                .equals(pot.substanceOf(carbon))) {
            return null;
        }
        List<Arrow> arrows = List.of(
                Arrow.fromLonePair(alcoholOxygen, alcoholOxygen, carbon),
                Arrow.toLonePair(carbon, oxygen),
                Arrow.toLonePair(hydrogen, alcoholOxygen),
                Arrow.fromLonePair(oxygen, oxygen, hydrogen));
        Molecule product = ElementaryStep.of("hemiacetal", arrows, carbon, alcoholOxygen, Face.RE)
                .apply(pot.molecule());
        return pot.react(List.of(pot.substanceOf(carbon), pot.substanceOf(alcoholOxygen)), product, 0);
    }

    /** Hydrogen across the double bond of an alkene. */
    private static ReactionRule hydrogenation() {
        return new ReactionRule("hydrogenation", "addition",
                EnumSet.of(FunctionalGroup.ALKENE), PolarReactions::hydrogenate);
    }

    private static Reaction hydrogenate(Pot pot) {
        Site alkene = first(Sites.alkenes(pot.molecule()));
        int[] hydrogen = hydrogenPair(pot.molecule());
        if (alkene == null || hydrogen == null) {
            return null;
        }
        Molecule product = ElementaryStep
                .across("hydrogenation", alkene.atom(0), alkene.atom(1), hydrogen[0], hydrogen[1])
                .apply(pot.molecule());
        return pot.react(List.of(pot.substanceOf(alkene.atom(0)), pot.substanceOf(hydrogen[0])), product,
                0);
    }

    /** A halogen across the double bond of an alkene, one halogen to each carbon of it. */
    private static ReactionRule halogenation() {
        return new ReactionRule("halogenation", "addition",
                EnumSet.of(FunctionalGroup.ALKENE), PolarReactions::halogenate);
    }

    private static Reaction halogenate(Pot pot) {
        Site alkene = first(Sites.alkenes(pot.molecule()));
        int[] halogen = halogenPair(pot.molecule());
        if (alkene == null || halogen == null) {
            return null;
        }
        Molecule product = ElementaryStep
                .across("halogenation", alkene.atom(0), alkene.atom(1), halogen[0], halogen[1])
                .apply(pot.molecule());
        return pot.react(List.of(pot.substanceOf(alkene.atom(0)), pot.substanceOf(halogen[0])), product,
                0);
    }

    /**
     * The hydrogen and the halogen of an acid across a double bond, the hydrogen to the carbon that carries
     * more hydrogens of the two - which is the whole of what the rule of Markovnikov says.
     */
    private static ReactionRule hydrohalogenation() {
        return new ReactionRule("hydrohalogenation", "addition",
                EnumSet.of(FunctionalGroup.ALKENE), PolarReactions::hydrohalogenate);
    }

    private static Reaction hydrohalogenate(Pot pot) {
        Site alkene = first(Sites.alkenes(pot.molecule()));
        int[] acid = hydrogenHalide(pot.molecule());
        if (alkene == null || acid == null) {
            return null;
        }
        int first = plainerCarbonOf(pot.molecule(), alkene);
        int second = first == alkene.atom(0) ? alkene.atom(1) : alkene.atom(0);
        Molecule product = ElementaryStep.across("hydrohalogenation", first, second, acid[0], acid[1])
                .apply(pot.molecule());
        return pot.react(List.of(pot.substanceOf(first), pot.substanceOf(acid[0])), product, 0);
    }

    /**
     * The hydrogen and the hydroxyl of water across a double bond, the hydrogen to the carbon that carries
     * more hydrogens of the two.
     */
    private static ReactionRule hydration() {
        return new ReactionRule("hydration", "addition", EnumSet.of(FunctionalGroup.ALKENE),
                PolarReactions::hydrate);
    }

    private static Reaction hydrate(Pot pot) {
        Site alkene = first(Sites.alkenes(pot.molecule()));
        int[] water = water(pot.molecule());
        if (alkene == null || water == null) {
            return null;
        }
        int first = plainerCarbonOf(pot.molecule(), alkene);
        int second = first == alkene.atom(0) ? alkene.atom(1) : alkene.atom(0);
        Molecule product = ElementaryStep.across("hydration", first, second, water[0], water[1])
                .apply(pot.molecule());
        return pot.react(List.of(pot.substanceOf(first), pot.substanceOf(water[1])), product, 0);
    }

    /** The carbon of an alkene that carries more hydrogens, which is the one a hydrogen is added to. */
    private static int plainerCarbonOf(Molecule molecule, Site alkene) {
        int first = alkene.atom(0);
        int second = alkene.atom(1);
        return molecule.atom(first).hydrogens() >= molecule.atom(second).hydrogens() ? first : second;
    }

    /** An alcohol losing water and leaving a double bond behind, the hydrogen of the carbon beside it going. */
    private static ReactionRule dehydration() {
        return new ReactionRule("dehydration", "elimination", EnumSet.of(FunctionalGroup.HYDROXYL),
                PolarReactions::dehydrate);
    }

    private static Reaction dehydrate(Pot pot) {
        Molecule molecule = pot.molecule();
        for (Site hydroxyl : Sites.hydroxyls(molecule)) {
            int alcoholOxygen = hydroxyl.atom(0);
            int alpha = hydroxyl.atom(1);
            for (int beta : molecule.neighbours(alpha)) {
                if (beta == alcoholOxygen) {
                    continue;
                }
                int hydrogen = hydrogenOn(molecule, beta);
                if (hydrogen < 0) {
                    continue;
                }
                Molecule product = ElementaryStep
                        .elimination("dehydration", alpha, alcoholOxygen, beta, hydrogen)
                        .apply(molecule);
                return pot.react(List.of(pot.substanceOf(alpha)), product, 0);
            }
        }
        return null;
    }

    /** The first of a list, or {@code null} when it is empty. */
    private static Site first(List<Site> sites) {
        return sites.isEmpty() ? null : sites.get(0);
    }

    /** A hydrogen atom that hangs on an atom, or {@code -1} - an arrow may name an atom and never a count. */
    private static int hydrogenOn(Molecule molecule, int atom) {
        for (int neighbour : molecule.neighbours(atom)) {
            if (isElement(molecule, neighbour, "H")) {
                return neighbour;
            }
        }
        return -1;
    }

    /** The two atoms of a molecule of hydrogen, or {@code null} when there is none. */
    private static int[] hydrogenPair(Molecule molecule) {
        for (Bond bond : molecule.bonds()) {
            if (isElement(molecule, bond.first(), "H") && isElement(molecule, bond.second(), "H")) {
                return new int[] {bond.first(), bond.second()};
            }
        }
        return null;
    }

    /** The two atoms of a molecule of a halogen, or {@code null} when there is none. */
    private static int[] halogenPair(Molecule molecule) {
        for (Bond bond : molecule.bonds()) {
            int first = bond.first();
            int second = bond.second();
            if (isHalogen(molecule, first) && isHalogen(molecule, second)
                    && molecule.atom(first).element().equals(molecule.atom(second).element())) {
                return new int[] {first, second};
            }
        }
        return null;
    }

    /** The hydrogen and the halogen of a molecule of an acid, or {@code null} when there is none. */
    private static int[] hydrogenHalide(Molecule molecule) {
        for (Bond bond : molecule.bonds()) {
            if (isElement(molecule, bond.first(), "H") && isHalogen(molecule, bond.second())) {
                return new int[] {bond.first(), bond.second()};
            }
            if (isHalogen(molecule, bond.first()) && isElement(molecule, bond.second(), "H")) {
                return new int[] {bond.second(), bond.first()};
            }
        }
        return null;
    }

    /** A water molecule that was written with both its hydrogens, or {@code null} when there is none. */
    private static int[] water(Molecule molecule) {
        for (int atom = 0; atom < molecule.atomCount(); atom++) {
            if (!isElement(molecule, atom, "O") || molecule.atom(atom).charge() != 0
                    || molecule.neighbours(atom).size() != 2) {
                continue;
            }
            int first = -1;
            int second = -1;
            for (int neighbour : molecule.neighbours(atom)) {
                if (!isElement(molecule, neighbour, "H") || molecule.atom(neighbour).charge() != 0) {
                    first = -1;
                    break;
                }
                if (first < 0) {
                    first = neighbour;
                } else {
                    second = neighbour;
                }
            }
            if (first >= 0 && second >= 0) {
                return new int[] {first, atom};
            }
        }
        return null;
    }

    /** The oxygen of a hydroxide ion, or {@code -1} when there is none. */
    private static int hydroxide(Molecule molecule) {
        for (int atom = 0; atom < molecule.atomCount(); atom++) {
            if (isElement(molecule, atom, "O") && molecule.atom(atom).charge() == -1
                    && molecule.neighbours(atom).isEmpty()) {
                return atom;
            }
        }
        return -1;
    }

    /** The carbon of a cyanide ion, or {@code -1} when there is none. */
    private static int cyanide(Molecule molecule) {
        for (Site nitrile : Sites.nitriles(molecule)) {
            if (molecule.atom(nitrile.atom(0)).charge() == -1) {
                return nitrile.atom(0);
            }
        }
        return -1;
    }

    /** {@code true} when an atom of a molecule is of a named element. */
    private static boolean isElement(Molecule molecule, int atom, String element) {
        return molecule.atom(atom).element().equals(element);
    }

    /** {@code true} when an atom of a molecule is a halogen. */
    private static boolean isHalogen(Molecule molecule, int atom) {
        switch (molecule.atom(atom).element()) {
            case "F":
            case "Cl":
            case "Br":
            case "I":
                return true;
            default:
                return false;
        }
    }
}
