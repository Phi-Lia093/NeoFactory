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
        rules.add(imineFormation());
        rules.add(sulfonation());
        rules.add(aldol());
        rules.add(claisenCondensation());
        rules.add(michaelAddition());
        rules.add(enolateAlkylation());
        rules.add(dielsAlder());
        rules.add(esterification());
        rules.add(esterHydrolysis());
        rules.add(hydrogenation());
        rules.add(halogenation());
        rules.add(hydrohalogenation());
        rules.add(hydration());
        rules.add(carbonylHydrogenation());
        rules.add(alkaneHalogenation());
        rules.add(dehydration());
        rules.add(dehydrogenation());
        return List.copyOf(rules);
    }

    /**
     * A carbon beside a carbonyl taking the carbon of another carbonyl, which is the one reaction that builds
     * a carbon to carbon bond out of two ordinary molecules and the reason the family is worth having.
     * <p>
     * Three arrows are drawn: the pair of the carbon to hydrogen bond makes the new carbon to carbon bond,
     * the pair of the double bond goes to the oxygen, and the hydrogen lands on that oxygen - so that the one
     * molecule loses a hydrogen and gains a carbon, and nothing else about it moves.
     */
    private static ReactionRule aldol() {
        return new ReactionRule("aldol", "condensation", EnumSet.of(FunctionalGroup.CARBONYL),
                PolarReactions::aldol);
    }

    private static Reaction aldol(Pot pot) {
        Molecule molecule = pot.molecule();
        List<Site> carbonyls = Sites.carbonyls(molecule);
        for (Site giving : carbonyls) {
            int alpha = alphaCarbonOf(molecule, giving.atom(0));
            if (alpha < 0) {
                continue;
            }
            int hydrogen = hydrogenOn(molecule, alpha);
            if (hydrogen < 0) {
                continue;
            }
            for (Site taking : carbonyls) {
                int carbon = taking.atom(0);
                int oxygen = taking.atom(1);
                // The carbon that is attacked has to stand in another molecule: beside the one that gives
                // the hydrogen it is a bond that is already there and not a reaction at all.
                if (carbon == alpha || bondBetween(molecule, carbon, alpha) != null) {
                    continue;
                }
                List<Arrow> arrows = List.of(
                        Arrow.betweenBonds(alpha, hydrogen, alpha, carbon),
                        Arrow.toLonePair(carbon, oxygen),
                        Arrow.fromLonePair(oxygen, oxygen, hydrogen));
                Molecule product = ElementaryStep.of("aldol", arrows).apply(molecule);
                Mixture consumed = Mixture.of(pot.substanceOf(alpha), 1);
                if (pot.substanceOf(alpha).equals(pot.substanceOf(carbon))) {
                    consumed = Mixture.of(pot.substanceOf(alpha), 2);
                } else {
                    consumed = consumed.plus(Mixture.of(pot.substanceOf(carbon), 1));
                }
                return pot.react(consumed, product, 0);
            }
        }
        return null;
    }

    /**
     * A diene and a double bond closing into a ring of six, the reaction that builds the rings a drug is made
     * of out of two flat molecules.
     * <p>
     * Three arrows are drawn for the one moment: the pair of the first double bond of the diene makes the
     * bond to the first carbon of the other molecule, the pair of that molecule makes the bond to the far end
     * of the diene, and the pair of the far double bond makes the bond between the two middle carbons - which
     * is what closes the ring.
     */
    private static ReactionRule dielsAlder() {
        return new ReactionRule("dielsAlder", "pericyclic", EnumSet.of(FunctionalGroup.ALKENE),
                PolarReactions::dielsAlder);
    }

    private static Reaction dielsAlder(Pot pot) {
        Molecule molecule = pot.molecule();
        int[][] diene = dieneOf(molecule);
        if (diene == null) {
            return null;
        }
        for (Site alkene : Sites.alkenes(molecule)) {
            int first = alkene.atom(0);
            int second = alkene.atom(1);
            if (isIn(diene[0], first) || isIn(diene[0], second) || isIn(diene[1], first)
                    || isIn(diene[1], second)) {
                continue;
            }
            List<Arrow> arrows = List.of(
                    Arrow.betweenBonds(diene[0][0], diene[0][1], diene[0][0], first),
                    Arrow.betweenBonds(first, second, second, diene[1][1]),
                    Arrow.betweenBonds(diene[1][0], diene[1][1], diene[0][1], diene[1][0]));
            Molecule product = ElementaryStep.of("dielsAlder", arrows).apply(molecule);
            Mixture consumed = Mixture.of(pot.substanceOf(diene[0][0]), 1);
            if (pot.substanceOf(diene[0][0]).equals(pot.substanceOf(first))) {
                consumed = Mixture.of(pot.substanceOf(diene[0][0]), 2);
            } else {
                consumed = consumed.plus(Mixture.of(pot.substanceOf(first), 1));
            }
            return pot.react(consumed, product, 0);
        }
        return null;
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
        Site halide = bestHalide(pot.molecule());
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
        Site carbonyl = bestCarbonyl(pot.molecule());
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
        Site aldehyde = bestAldehyde(pot.molecule());
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
        Site alkene = bestAlkene(pot.molecule());
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
        Site alkene = bestAlkene(pot.molecule());
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
        Site alkene = bestAlkene(pot.molecule());
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
        Site alkene = bestAlkene(pot.molecule());
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
        return Sites.hydrogensOn(molecule, first) >= Sites.hydrogensOn(molecule, second) ? first : second;
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

    /** Hydrogen across the double bond of a carbonyl, which is what takes an aldehyde down to an alcohol. */
    private static ReactionRule carbonylHydrogenation() {
        return new ReactionRule("carbonylHydrogenation", "reduction",
                EnumSet.of(FunctionalGroup.CARBONYL), PolarReactions::hydrogenateCarbonyl);
    }

    private static Reaction hydrogenateCarbonyl(Pot pot) {
        Site carbonyl = bestCarbonyl(pot.molecule());
        int[] hydrogen = hydrogenPair(pot.molecule());
        if (carbonyl == null || hydrogen == null) {
            return null;
        }
        Molecule product = ElementaryStep
                .across("carbonylHydrogenation", carbonyl.atom(0), carbonyl.atom(1), hydrogen[0],
                        hydrogen[1])
                .apply(pot.molecule());
        return pot.react(List.of(pot.substanceOf(carbonyl.atom(0)), pot.substanceOf(hydrogen[0])),
                product, 0);
    }

    /**
     * An alcohol losing hydrogen and coming out a carbonyl, which is the oxidation of the industry drawn the
     * way it is run: a copper surface takes the hydrogen off and the bond left behind becomes the double one.
     */
    private static ReactionRule dehydrogenation() {
        return new ReactionRule("dehydrogenation", "oxidation",
                EnumSet.of(FunctionalGroup.HYDROXYL), PolarReactions::dehydrogenate);
    }

    private static Reaction dehydrogenate(Pot pot) {
        Molecule molecule = pot.molecule();
        for (Site hydroxyl : Sites.hydroxyls(molecule)) {
            int oxygen = hydroxyl.atom(0);
            int carbon = hydroxyl.atom(1);
            int hydroxylHydrogen = hydrogenOn(molecule, oxygen);
            int carbonHydrogen = hydrogenOn(molecule, carbon);
            Bond link = bondBetween(molecule, carbon, oxygen);
            if (hydroxylHydrogen < 0 || carbonHydrogen < 0 || link == null || link.order() != 1
                    || carriesCarbonyl(molecule, carbon)) {
                continue;
            }
            List<Arrow> arrows = List.of(
                    Arrow.betweenBonds(carbon, carbonHydrogen, carbonHydrogen, hydroxylHydrogen),
                    Arrow.toLonePair(hydroxylHydrogen, oxygen),
                    Arrow.fromLonePair(oxygen, oxygen, carbon));
            Molecule product = ElementaryStep.of("dehydrogenation", arrows).apply(molecule);
            return pot.react(List.of(pot.substanceOf(carbon)), product, 0);
        }
        return null;
    }

    /**
     * An acid and an alcohol giving an ester and water, the reaction a polyester is made by.
     * <p>
     * Six arrows are drawn: the lone pair of the alcohol into the carbonyl, the pair of the double bond to
     * the carbonyl oxygen, the acid's own hydroxyl off with the pair, the pair back into the double bond, the
     * alcohol's hydrogen off, and that hydrogen onto the group that just left - so that the group leaves as
     * water and not as a hydroxide.
     */
    private static ReactionRule esterification() {
        return new ReactionRule("esterification", "acyl substitution",
                EnumSet.of(FunctionalGroup.CARBOXYLIC_ACID, FunctionalGroup.HYDROXYL),
                PolarReactions::esterify);
    }

    private static Reaction esterify(Pot pot) {
        Molecule molecule = pot.molecule();
        Site acid = first(Sites.acids(molecule));
        if (acid == null) {
            return null;
        }
        int carbon = acid.atom(0);
        int carbonylOxygen = acid.atom(1);
        int acidOxygen = acid.atom(2);
        for (Site hydroxyl : Sites.hydroxyls(molecule)) {
            int alcoholOxygen = hydroxyl.atom(0);
            int alcoholHydrogen = hydrogenOn(molecule, alcoholOxygen);
            if (alcoholOxygen == acidOxygen || alcoholHydrogen < 0
                    || pot.substanceOf(alcoholOxygen).equals(pot.substanceOf(carbon))) {
                continue;
            }
            List<Arrow> arrows = List.of(
                    Arrow.fromLonePair(alcoholOxygen, alcoholOxygen, carbon),
                    Arrow.toLonePair(carbon, carbonylOxygen),
                    Arrow.toLonePair(carbon, acidOxygen),
                    Arrow.fromLonePair(carbonylOxygen, carbonylOxygen, carbon),
                    Arrow.toLonePair(alcoholHydrogen, alcoholOxygen),
                    Arrow.fromLonePair(acidOxygen, acidOxygen, alcoholHydrogen));
            Molecule product = ElementaryStep.of("esterification", arrows).apply(molecule);
            Mixture consumed = Mixture.of(pot.substanceOf(carbon), 1)
                    .plus(Mixture.of(pot.substanceOf(alcoholOxygen), 1));
            return pot.react(consumed, product, 0);
        }
        return null;
    }

    /** An ester and water giving the acid and the alcohol back, the same six arrows the other way round. */
    private static ReactionRule esterHydrolysis() {
        return new ReactionRule("esterHydrolysis", "acyl substitution",
                EnumSet.of(FunctionalGroup.ESTER), PolarReactions::hydrolyseEster);
    }

    private static Reaction hydrolyseEster(Pot pot) {
        Molecule molecule = pot.molecule();
        Site ester = first(Sites.esters(molecule));
        int[] water = water(molecule);
        if (ester == null || water == null) {
            return null;
        }
        int carbon = ester.atom(0);
        int carbonylOxygen = ester.atom(1);
        int esterOxygen = ester.atom(2);
        int waterHydrogen = water[0];
        int waterOxygen = water[1];
        List<Arrow> arrows = List.of(
                Arrow.fromLonePair(waterOxygen, waterOxygen, carbon),
                Arrow.toLonePair(carbon, carbonylOxygen),
                Arrow.toLonePair(carbon, esterOxygen),
                Arrow.fromLonePair(carbonylOxygen, carbonylOxygen, carbon),
                Arrow.toLonePair(waterHydrogen, waterOxygen),
                Arrow.fromLonePair(esterOxygen, esterOxygen, waterHydrogen));
        Molecule product = ElementaryStep.of("esterHydrolysis", arrows).apply(molecule);
        Mixture consumed = Mixture.of(pot.substanceOf(carbon), 1)
                .plus(Mixture.of(pot.substanceOf(waterOxygen), 1));
        return pot.react(consumed, product, 0);
    }

    /**
     * A halogen taking the place of a hydrogen on a saturated carbon, the first step of the radical chemistry
     * of the industry.
     * <p>
     * The two arrows are the chain written as one move: the pair of the halogen to halogen bond makes the
     * bond to the carbon and the pair of the carbon to hydrogen bond makes the acid - so that one halogen
     * ends up on the carbon and the other leaves with the hydrogen.
     */
    private static ReactionRule alkaneHalogenation() {
        return new ReactionRule("alkaneHalogenation", "radical", EnumSet.of(FunctionalGroup.ALKYL),
                PolarReactions::halogenateAlkane);
    }

    private static Reaction halogenateAlkane(Pot pot) {
        Site alkyl = first(Sites.alkyls(pot.molecule()));
        int[] halogen = halogenPair(pot.molecule());
        if (alkyl == null || halogen == null) {
            return null;
        }
        int carbon = alkyl.atom(0);
        int hydrogen = alkyl.atom(1);
        List<Arrow> arrows = List.of(
                Arrow.betweenBonds(halogen[0], halogen[1], halogen[0], carbon),
                Arrow.betweenBonds(carbon, hydrogen, hydrogen, halogen[1]));
        Molecule product = ElementaryStep.of("alkaneHalogenation", arrows).apply(pot.molecule());
        return pot.react(List.of(pot.substanceOf(carbon), pot.substanceOf(halogen[0])), product, 0);
    }

    /** The carbon beside a carbonyl that carries the hydrogens an aldol takes one of, or {@code -1}. */
    private static int alphaCarbonOf(Molecule molecule, int carbonyl) {
        for (int neighbour : molecule.neighbours(carbonyl)) {
            if (isElement(molecule, neighbour, "C") && Sites.hydrogensOn(molecule, neighbour) > 0) {
                return neighbour;
            }
        }
        return -1;
    }

    /** {@code true} when a carbon already carries a double bond to an oxygen. */
    private static boolean carriesCarbonyl(Molecule molecule, int carbon) {
        for (Bond bond : molecule.bonds()) {
            if (bond.order() <= 1 || !bond.touches(carbon)) {
                continue;
            }
            int other = bond.other(carbon);
            if (isElement(molecule, other, "O")) {
                return true;
            }
        }
        return false;
    }

    /** The two double bonds of a conjugated diene, or {@code null} when the molecule holds none. */
    private static int[][] dieneOf(Molecule molecule) {
        List<Site> alkenes = Sites.alkenes(molecule);
        for (int first = 0; first < alkenes.size(); first++) {
            for (int second = first + 1; second < alkenes.size(); second++) {
                for (int inner : alkenes.get(first).atoms()) {
                    for (int other : alkenes.get(second).atoms()) {
                        Bond link = bondBetween(molecule, inner, other);
                        if (link == null || link.order() != 1) {
                            continue;
                        }
                        int outer = alkenes.get(first).atom(0) == inner ? alkenes.get(first).atom(1)
                                : alkenes.get(first).atom(0);
                        int far = alkenes.get(second).atom(0) == other ? alkenes.get(second).atom(1)
                                : alkenes.get(second).atom(0);
                        return new int[][] {{outer, inner}, {other, far}};
                    }
                }
            }
        }
        return null;
    }

    /** {@code true} when an index stands among a pair. */
    private static boolean isIn(int[] pair, int atom) {
        return pair[0] == atom || pair[1] == atom;
    }

    /** The bond between two atoms, or {@code null} when they share none. */
    private static Bond bondBetween(Molecule molecule, int first, int second) {
        for (int bondIndex : molecule.bondsOf(first)) {
            Bond bond = molecule.bonds().get(bondIndex);
            if (bond.other(first) == second) {
                return bond;
            }
        }
        return null;
    }

    /**
     * A carbonyl beside another taking an alkyl group off a halide, which is how a carbon is put where a
     * hydrogen stood - the reaction a chemist reaches for when a skeleton has to be built up.
     * <p>
     * Three arrows are drawn: the pair of the carbon to hydrogen bond beside the first carbonyl makes the
     * bond to the carbon of the halide, the pair of the carbon to halogen bond leaves as a halide, and the
     * hydrogen goes to it. A base is what the reaction is run over and is not drawn here; the rule is written
     * above the arrow the way a chemist writes it, and the vessel names the two substances it joins.
     */
    private static ReactionRule enolateAlkylation() {
        return new ReactionRule("enolateAlkylation", "condensation", 5,
                EnumSet.of(FunctionalGroup.CARBONYL, FunctionalGroup.HALIDE, FunctionalGroup.ALKYL),
                PolarReactions::alkylateEnolate);
    }

    private static Reaction alkylateEnolate(Pot pot) {
        Molecule molecule = pot.molecule();
        Site halide = bestHalide(molecule);
        if (halide == null) {
            return null;
        }
        int alkylCarbon = halide.atom(0);
        int halogen = halide.atom(1);
        for (Site carbonyl : Sites.carbonyls(molecule)) {
            int alpha = alphaCarbonOf(molecule, carbonyl.atom(0));
            if (alpha < 0 || bondBetween(molecule, alpha, alkylCarbon) != null) {
                continue;
            }
            int hydrogen = hydrogenOn(molecule, alpha);
            if (hydrogen < 0 || pot.substanceOf(alpha).equals(pot.substanceOf(alkylCarbon))) {
                continue;
            }
            List<Arrow> arrows = List.of(
                    Arrow.betweenBonds(alpha, hydrogen, alpha, alkylCarbon),
                    Arrow.toLonePair(alkylCarbon, halogen),
                    Arrow.fromLonePair(halogen, halogen, hydrogen));
            Molecule product = ElementaryStep.of("enolateAlkylation", arrows).apply(molecule);
            Mixture consumed = Mixture.of(pot.substanceOf(alpha), 1)
                    .plus(Mixture.of(pot.substanceOf(alkylCarbon), 1));
            return pot.react(consumed, product, 0);
        }
        return null;
    }

    /**
     * Two esters joining at a carbon and giving a keto ester, which is where the carbon of a Claisen comes
     * from and the way a chain of carbons is lengthened by two at a time.
     * <p>
     * Five arrows are drawn, the same shape the making of an ester has: the pair of the carbon to hydrogen
     * bond beside the first ester makes the bond to the second, the pair of the double bond goes to its
     * oxygen, its own alkoxide leaves with the pair, the double bond comes back, and the hydrogen lands on
     * the group that left - so that an alcohol stands beside the keto ester on the right.
     */
    private static ReactionRule claisenCondensation() {
        return new ReactionRule("claisenCondensation", "condensation", 4,
                EnumSet.of(FunctionalGroup.ESTER), PolarReactions::condenseClaisen);
    }

    private static Reaction condenseClaisen(Pot pot) {
        Molecule molecule = pot.molecule();
        List<Site> esters = Sites.esters(molecule);
        for (Site giving : esters) {
            int alpha = alphaCarbonOf(molecule, giving.atom(0));
            if (alpha < 0) {
                continue;
            }
            int hydrogen = hydrogenOn(molecule, alpha);
            if (hydrogen < 0) {
                continue;
            }
            for (Site taking : esters) {
                int carbon = taking.atom(0);
                int oxygen = taking.atom(1);
                int alkoxide = taking.atom(2);
                if (alkoxide == giving.atom(2) || bondBetween(molecule, carbon, alpha) != null) {
                    continue;
                }
                List<Arrow> arrows = List.of(
                        Arrow.betweenBonds(alpha, hydrogen, alpha, carbon),
                        Arrow.toLonePair(carbon, oxygen),
                        Arrow.toLonePair(carbon, alkoxide),
                        Arrow.fromLonePair(oxygen, oxygen, carbon),
                        Arrow.fromLonePair(alkoxide, alkoxide, hydrogen));
                Molecule product = ElementaryStep.of("claisenCondensation", arrows).apply(molecule);
                Mixture consumed = Mixture.of(pot.substanceOf(alpha), 1);
                if (pot.substanceOf(alpha).equals(pot.substanceOf(carbon))) {
                    consumed = Mixture.of(pot.substanceOf(alpha), 2);
                } else {
                    consumed = consumed.plus(Mixture.of(pot.substanceOf(carbon), 1));
                }
                return pot.react(consumed, product, 0);
            }
        }
        return null;
    }

    /**
     * The carbon beside a carbonyl joining the far end of a double bond that is beside another carbonyl -
     * the conjugate addition, where a molecule is built up without the double bond being lost.
     * <p>
     * Four arrows are drawn: the pair of the carbon to hydrogen bond beside the first carbonyl makes the bond
     * to the far carbon of the double bond, the pair of that double bond makes the bond to the middle carbon,
     * the pair of the carbonyl goes to its oxygen, and the hydrogen lands on that oxygen - so that the
     * molecule comes out with the two carbonyls three carbons apart.
     */
    private static ReactionRule michaelAddition() {
        return new ReactionRule("michaelAddition", "conjugate addition", 4,
                EnumSet.of(FunctionalGroup.CARBONYL, FunctionalGroup.ALKENE),
                PolarReactions::addMichael);
    }

    private static Reaction addMichael(Pot pot) {
        Molecule molecule = pot.molecule();
        int[] enone = enoneOf(molecule);
        if (enone == null) {
            return null;
        }
        int far = enone[0];
        int near = enone[1];
        int carbonEnone = enone[2];
        int oxygenEnone = enone[3];
        for (Site carbonyl : Sites.carbonyls(molecule)) {
            int alpha = alphaCarbonOf(molecule, carbonyl.atom(0));
            if (alpha < 0 || carbonyl.atom(0) == carbonEnone || alpha == near || alpha == far) {
                continue;
            }
            int hydrogen = hydrogenOn(molecule, alpha);
            if (hydrogen < 0 || bondBetween(molecule, alpha, far) != null
                    || pot.substanceOf(alpha).equals(pot.substanceOf(far))) {
                continue;
            }
            List<Arrow> arrows = List.of(
                    Arrow.betweenBonds(alpha, hydrogen, alpha, far),
                    Arrow.betweenBonds(far, near, near, carbonEnone),
                    Arrow.toLonePair(carbonEnone, oxygenEnone),
                    Arrow.fromLonePair(oxygenEnone, oxygenEnone, hydrogen));
            Molecule product = ElementaryStep.of("michaelAddition", arrows).apply(molecule);
            Mixture consumed = Mixture.of(pot.substanceOf(alpha), 1)
                    .plus(Mixture.of(pot.substanceOf(far), 1));
            return pot.react(consumed, product, 0);
        }
        return null;
    }

    /**
     * An amine and a carbonyl coming together and losing water, which is what turns a flat carbonyl into the
     * carbon to nitrogen double bond a great many drugs are hung on.
     * <p>
     * Six arrows are drawn: the lone pair of the nitrogen into the carbonyl, the pair of the double bond to
     * the oxygen, the two hydrogens of the amine each going over to that oxygen - which leaves it a molecule
     * of water - the bond to it breaking, and the lone pair of the nitrogen closing the double bond. Both
     * hydrogens of the amine end up in the water, which is why a primary amine gives an imine with no
     * hydrogen on its nitrogen.
     */
    private static ReactionRule imineFormation() {
        return new ReactionRule("imineFormation", "condensation", 6,
                EnumSet.of(FunctionalGroup.CARBONYL, FunctionalGroup.AMINE),
                PolarReactions::formImine);
    }

    private static Reaction formImine(Pot pot) {
        Molecule molecule = pot.molecule();
        Site carbonyl = bestCarbonyl(molecule);
        int[] amine = amineOf(molecule);
        if (carbonyl == null || amine == null) {
            return null;
        }
        int carbon = carbonyl.atom(0);
        int oxygen = carbonyl.atom(1);
        int nitrogen = amine[0];
        int first = amine[1];
        int second = amine[2];
        if (pot.substanceOf(carbon).equals(pot.substanceOf(nitrogen))
                || bondBetween(molecule, carbon, nitrogen) != null) {
            return null;
        }
        List<Arrow> arrows = List.of(
                Arrow.fromLonePair(nitrogen, nitrogen, carbon),
                Arrow.toLonePair(carbon, oxygen),
                Arrow.toLonePair(first, nitrogen),
                Arrow.fromLonePair(oxygen, oxygen, first),
                Arrow.toLonePair(second, nitrogen),
                Arrow.fromLonePair(oxygen, oxygen, second),
                Arrow.toLonePair(carbon, oxygen),
                Arrow.fromLonePair(nitrogen, nitrogen, carbon));
        Molecule product = ElementaryStep.of("imineFormation", arrows).apply(molecule);
        Mixture consumed = Mixture.of(pot.substanceOf(carbon), 1)
                .plus(Mixture.of(pot.substanceOf(nitrogen), 1));
        return pot.react(consumed, product, 0);
    }

    /**
     * The sulfur of an acid taking the place of a hydrogen on an aromatic ring, which is the way a ring is
     * given a group at all - and the reaction the whole of aromatic chemistry is written against.
     * <p>
     * <b>A ring cannot be drawn on as it stands.</b> The bonds of an aromatic ring stand for the electrons of
     * the whole of it and not for one pair of them, so there is no pair written on one of those bonds for an
     * arrow to move; the ring is written the way Kekulé wrote it, the substitution is drawn on that, and the
     * ring is read as a ring again at the end, see {@link Assemblies#kekulized}. Which is the whole reason
     * the engine has those two readings at all.
     * <p>
     * Four arrows are drawn: the pair of the double bond beside the carbon that is attacked makes the bond to
     * the sulfur while one pair of a sulfur oxygen is left behind on that oxygen, the pair of the carbon to
     * hydrogen bond comes back into the ring - which is what closes it again - and the hydrogen that left
     * lands on the oxygen that was left holding the pair. A ring that already carries a group of its own is
     * attacked where that group sends the electrophile, see {@link Reactivity#directsToTheSides}.
     */
    private static ReactionRule sulfonation() {
        return new ReactionRule("sulfonation", "substitution", 4,
                EnumSet.of(FunctionalGroup.AROMATIC_RING), PolarReactions::sulfonate);
    }

    private static Reaction sulfonate(Pot pot) {
        Molecule molecule = pot.molecule();
        int sulfur = sulfurTrioxideOf(molecule);
        if (sulfur < 0) {
            return null;
        }
        int oxygen = sulfurTrioxideOxygen(molecule, sulfur);
        if (oxygen < 0) {
            return null;
        }
        Molecule drawn = Assemblies.kekulized(molecule);
        for (Site ring : Sites.aromaticRings(molecule)) {
            int[] place = attackPlace(drawn, ring.atoms());
            if (place == null) {
                continue;
            }
            int position = place[0];
            int beside = place[1];
            int hydrogen = hydrogenOn(molecule, position);
            if (hydrogen < 0) {
                continue;
            }
            List<Arrow> arrows = List.of(
                    Arrow.betweenBonds(position, beside, position, sulfur),
                    Arrow.toLonePair(sulfur, oxygen),
                    Arrow.betweenBonds(position, hydrogen, position, beside),
                    Arrow.fromLonePair(oxygen, oxygen, hydrogen));
            Molecule product = Assemblies.aromatized(
                    ElementaryStep.of("sulfonation", arrows).apply(drawn));
            Mixture consumed = Mixture.of(pot.substanceOf(position), 1)
                    .plus(Mixture.of(pot.substanceOf(sulfur), 1));
            return pot.react(consumed, product, 0);
        }
        return null;
    }

    /**
     * Where on a ring an electrophile goes: the carbon that carries a hydrogen and that the group already on
     * the ring sends it to, together with the neighbour that carbon takes the pair of the ring from.
     *
     * @param molecule the ring written out as single and double bonds
     * @param ring the atoms of the ring in bond order
     * @return the carbon attacked and the neighbour its pair comes from, or {@code null} when the ring has no
     *         place to give
     */
    private static int[] attackPlace(Molecule molecule, List<Integer> ring) {
        int size = ring.size();
        int director = -1;
        for (int step = 0; step < size; step++) {
            if (substituentOf(molecule, ring, ring.get(step)) >= 0) {
                director = step;
                break;
            }
        }
        boolean sides = director < 0
                || Reactivity.directsToTheSides(molecule, substituentOf(molecule, ring, ring.get(director)));
        for (int step = 1; step <= size / 2; step++) {
            // Beside the group and across from it are the positions a group that lends electrons fills, and
            // the ones past a carbon are the two a group that pulls them leaves - see Reactivity. A ring of
            // anything but six is not read that way: its positions are of five kinds and not of four.
            if (size == 6 && director >= 0 && (step % 2 == 1) != sides) {
                continue;
            }
            int index = Math.floorMod(director < 0 ? step - 1 : director + step, size);
            int position = ring.get(index);
            if (!isElement(molecule, position, "C") || hydrogenOn(molecule, position) < 0
                    || substituentOf(molecule, ring, position) >= 0) {
                continue;
            }
            int next = ring.get((index + 1) % size);
            int previous = ring.get(Math.floorMod(index - 1, size));
            Bond forward = bondBetween(molecule, position, next);
            int beside = forward != null && forward.order() == 2 ? next : previous;
            Bond pair = bondBetween(molecule, position, beside);
            if (pair == null || pair.order() != 2) {
                continue;
            }
            return new int[] {position, beside};
        }
        return null;
    }

    /** The atom that hangs on a ring atom and is no atom of the ring, or {@code -1}. */
    private static int substituentOf(Molecule molecule, List<Integer> ring, int atom) {
        for (int neighbour : molecule.neighbours(atom)) {
            if (!isElement(molecule, neighbour, "H") && !ring.contains(neighbour)) {
                return neighbour;
            }
        }
        return -1;
    }

    /** The sulfur of a molecule of sulfur trioxide, or {@code -1} when the vessel holds none. */
    private static int sulfurTrioxideOf(Molecule molecule) {
        for (int atom = 0; atom < molecule.atomCount(); atom++) {
            if (!isElement(molecule, atom, "S") || molecule.atom(atom).charge() != 0
                    || molecule.neighbours(atom).size() != 3) {
                continue;
            }
            int oxygens = 0;
            for (int neighbour : molecule.neighbours(atom)) {
                Bond bond = bondBetween(molecule, atom, neighbour);
                if (isElement(molecule, neighbour, "O") && bond != null && bond.order() == 2) {
                    oxygens++;
                }
            }
            if (oxygens == 3) {
                return atom;
            }
        }
        return -1;
    }

    /** An oxygen of sulfur trioxide free to take the hydrogen the ring gives up, or {@code -1}. */
    private static int sulfurTrioxideOxygen(Molecule molecule, int sulfur) {
        for (int neighbour : molecule.neighbours(sulfur)) {
            Bond bond = bondBetween(molecule, sulfur, neighbour);
            if (isElement(molecule, neighbour, "O") && bond != null && bond.order() == 2
                    && Sites.hydrogensOn(molecule, neighbour) == 0) {
                return neighbour;
            }
        }
        return -1;
    }

    /** The carbonyl a nucleophile attacks first: the readier one, see {@link Reactivity}. */
    private static Site bestCarbonyl(Molecule molecule) {
        Site best = null;
        int highest = Integer.MIN_VALUE;
        for (Site carbonyl : Sites.carbonyls(molecule)) {
            int score = Reactivity.electrophilicity(molecule, carbonyl.atom(0));
            if (score > highest) {
                highest = score;
                best = carbonyl;
            }
        }
        return best;
    }

    /** The aldehyde a nucleophile attacks first, or {@code null} when the molecule holds none. */
    private static Site bestAldehyde(Molecule molecule) {
        Site best = null;
        int highest = Integer.MIN_VALUE;
        for (Site aldehyde : Sites.aldehydes(molecule)) {
            int score = Reactivity.electrophilicity(molecule, aldehyde.atom(0));
            if (score > highest) {
                highest = score;
                best = aldehyde;
            }
        }
        return best;
    }

    /** The double bond an electrophile adds across first: the better nucleophile, see {@link Reactivity}. */
    private static Site bestAlkene(Molecule molecule) {
        Site best = null;
        int highest = Integer.MIN_VALUE;
        for (Site alkene : Sites.alkenes(molecule)) {
            int score = Reactivity.nucleophilicity(molecule, alkene);
            if (score > highest) {
                highest = score;
                best = alkene;
            }
        }
        return best;
    }

    /**
     * The halogen a nucleophile takes the place of first.
     * <p>
     * Two orders are read at once and they are not the same order: the halogen that leaves most easily is the
     * one the reaction runs at, and among halogens that leave alike the plainer carbon is the one reached
     * first. The ease of leaving settles it before the crowding does, which is why the former is counted ten
     * times and the latter once.
     */
    private static Site bestHalide(Molecule molecule) {
        Site best = null;
        int highest = Integer.MIN_VALUE;
        for (Site halide : Sites.halides(molecule)) {
            int score = Reactivity.leavingAbility(molecule, halide.atom(1)) * 10
                    - Reactivity.crowding(molecule, halide.atom(0));
            if (score > highest) {
                highest = score;
                best = halide;
            }
        }
        return best;
    }

    /**
     * The four atoms of a double bond that stands beside a carbonyl, or {@code null} when there is none.
     *
     * @param molecule molecule to read
     * @return the far carbon, the one beside the carbonyl, the carbonyl carbon and its oxygen
     */
    private static int[] enoneOf(Molecule molecule) {
        for (Site alkene : Sites.alkenes(molecule)) {
            int first = alkene.atom(0);
            int second = alkene.atom(1);
            for (int near : new int[] {first, second}) {
                int far = near == first ? second : first;
                for (int neighbour : molecule.neighbours(near)) {
                    if (neighbour == far || !isElement(molecule, neighbour, "C")) {
                        continue;
                    }
                    Bond link = bondBetween(molecule, near, neighbour);
                    if (link == null || link.order() != 1) {
                        continue;
                    }
                    for (int oxygen : molecule.neighbours(neighbour)) {
                        Bond doubleBond = bondBetween(molecule, neighbour, oxygen);
                        if (doubleBond != null && doubleBond.order() == 2
                                && isElement(molecule, oxygen, "O")) {
                            return new int[] {far, near, neighbour, oxygen};
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * The nitrogen of a primary amine, its two hydrogens with it, or {@code null} when there is none.
     *
     * @param molecule molecule to read
     * @return the nitrogen and its two hydrogens
     */
    private static int[] amineOf(Molecule molecule) {
        for (int atom = 0; atom < molecule.atomCount(); atom++) {
            if (!isElement(molecule, atom, "N") || molecule.atom(atom).charge() != 0) {
                continue;
            }
            List<Integer> hydrogens = new ArrayList<>();
            boolean carbon = false;
            for (int neighbour : molecule.neighbours(atom)) {
                if (isElement(molecule, neighbour, "H") && molecule.atom(neighbour).charge() == 0) {
                    hydrogens.add(neighbour);
                } else if (isElement(molecule, neighbour, "C")) {
                    carbon = true;
                }
            }
            if (carbon && hydrogens.size() == 2) {
                return new int[] {atom, hydrogens.get(0), hydrogens.get(1)};
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
            if (!isElement(molecule, atom, "O") || molecule.atom(atom).charge() != -1) {
                continue;
            }
            boolean onlyHydrogens = true;
            for (int neighbour : molecule.neighbours(atom)) {
                if (!isElement(molecule, neighbour, "H")) {
                    onlyHydrogens = false;
                    break;
                }
            }
            if (onlyHydrogens) {
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
