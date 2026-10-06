package com.philia093.neofactory.chemistry;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

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

    /** The metal the adding of hydrogen is run over, the nickel the catalog carries. */
    private static final Chemical NICKEL = Chemical.parse("[Ni]");

    /** The Lewis acid a Friedel-Crafts reaction is run over. */
    private static final Chemical ALUMINIUM_CHLORIDE = Chemical.parse("[Al](Cl)(Cl)Cl");

    /** The Lewis acid a ring is brominated over. */
    private static final Chemical IRON_BROMIDE = Chemical.parse("[Fe](Br)(Br)Br");

    /** The water an ester is hydrolysed in, which is the substance and not the word. */
    private static final Chemical WATER = Chemical.parse("O");

    /** The acid a ring is sulfonated in, the oleum a chemist pours sulfur trioxide into. */
    private static final Chemical SULFURIC_ACID = Chemical.parse("OS(=O)(=O)O");

    private PolarReactions() {
        // Utility class: never instantiated.
    }

    /**
     * The rules of the polar families, the sharper ones first.
     * <p>
     * Each rule also names how it is run, where a reaction is one that is only run one way: an aldol goes at
     * the temperature the vessel stands at, a Diels-Alder is given a flame, an enolate is made in the cold,
     * hydrogen is added over nickel. A rule that names nothing of the kind is a rule that may be run
     * anywhere, which is how every rule stood before the vessel was asked the question at all.
     *
     * @return the table
     */
    public static List<ReactionRule> all() {
        List<ReactionRule> rules = new ArrayList<>();
        rules.add(hydrolysis());
        rules.add(cyanohydrin());
        rules.add(acetalFormation());
        rules.add(hemiacetal());
        rules.add(imineFormation());
        rules.add(enamineFormation());
        rules.add(sulfonation());
        rules.add(aldol());
        rules.add(claisenCondensation());
        rules.add(michaelAddition());
        rules.add(enolateAlkylation());
        rules.add(dielsAlder());
        rules.add(amideFormation());
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
        rules.add(protonation());
        rules.add(ketoEnolTautomerism());
        rules.add(alkyneHydrogenation());
        rules.add(alkyneHydrohalogenation());
        rules.add(alkyneHydration());
        rules.add(nitrileHydrolysis());
        rules.add(nitrileHydrogenation());
        rules.add(imineReduction());
        rules.add(imineHydrolysis());
        rules.add(amideHydrolysis());
        rules.add(transesterification());
        rules.add(aminolysisOfEster());
        rules.add(nitration());
        rules.add(friedelCraftsAlkylation());
        rules.add(friedelCraftsAcylation());
        rules.add(aromaticHalogenation());
        rules.add(aromaticSubstitution());
        rules.add(knoevenagel());
        rules.add(henry());
        rules.add(mannich());
        rules.add(electrocyclicClosing());
        rules.add(photocycloaddition());
        rules.add(amineAlkylation());
        rules.add(acylationOfAlcohol());
        rules.add(acylationOfAmine());
        rules.add(aldehydeOxidation());
        rules.add(alkaneDehydrogenation());
        rules.add(hydrocyanation());
        rules.add(eneReaction());
        rules.add(imidazoleFormation());
        rules.add(nitroReduction());
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
        return new ReactionRule("aldol", "condensation", 1, EnumSet.of(FunctionalGroup.CARBONYL),
                Conditions.at(Warmth.AMBIENT), PolarReactions::aldol);
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
                if (carbon == alpha || bondBetween(molecule, carbon, alpha) != null
                        || !mayJoin(pot, carbon, alpha)) {
                    continue;
                }
                List<Arrow> arrows = List.of(
                        Arrow.betweenBonds(alpha, hydrogen, alpha, carbon),
                        Arrow.toLonePair(carbon, oxygen),
                        Arrow.fromLonePair(oxygen, oxygen, hydrogen));
                Molecule product = ElementaryStep.of("aldol", arrows).apply(molecule);
                return pot.react(product, 0, alpha, carbon);
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
        return new ReactionRule("dielsAlder", "pericyclic", 1, EnumSet.of(FunctionalGroup.ALKENE),
                Conditions.at(Warmth.HEATED), PolarReactions::dielsAlder);
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
                return pot.react(product, 0, diene[0][0], first);
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
        return new ReactionRule("hydrolysis", "substitution", 1, EnumSet.of(FunctionalGroup.HALIDE),
                Conditions.at(Warmth.AMBIENT), PolarReactions::hydrolyse);
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
        return pot.react(product, 0, carbon, hydroxide);
    }

    /** A cyanide into the carbonyl of an aldehyde or a ketone, the pair of the double bond to the oxygen. */
    private static ReactionRule cyanohydrin() {
        return new ReactionRule("cyanohydrin", "carbonyl addition", 1,
                EnumSet.of(FunctionalGroup.CARBONYL), Conditions.at(Warmth.AMBIENT),
                PolarReactions::cyanohydrin);
    }

    private static Reaction cyanohydrin(Pot pot) {
        Site carbonyl = bestCarbonyl(pot.molecule());
        int cyanide = cyanide(pot.molecule());
        if (carbonyl == null || cyanide < 0) {
            return null;
        }
        int carbon = carbonyl.atom(0);
        int oxygen = carbonyl.atom(1);
        Face face = Steric.faceFor(pot.molecule(), carbon, cyanide);
        Molecule product = ElementaryStep.addition("cyanohydrin", cyanide, carbon, oxygen, face)
                .apply(pot.molecule());
        return pot.react(product, 0, carbon, cyanide);
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
        return new ReactionRule("hemiacetal", "carbonyl addition", 2,
                EnumSet.of(FunctionalGroup.ALDEHYDE, FunctionalGroup.HYDROXYL),
                Conditions.at(Warmth.AMBIENT), PolarReactions::hemiacetal);
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
        if (hydrogen < 0 || alcoholOxygen == oxygen || !mayJoin(pot, carbon, alcoholOxygen)) {
            return null;
        }
        List<Arrow> arrows = List.of(
                Arrow.fromLonePair(alcoholOxygen, alcoholOxygen, carbon),
                Arrow.toLonePair(carbon, oxygen),
                Arrow.toLonePair(hydrogen, alcoholOxygen),
                Arrow.fromLonePair(oxygen, oxygen, hydrogen));
        Molecule product = ElementaryStep
                .of("hemiacetal", arrows, carbon, alcoholOxygen,
                        Steric.faceFor(pot.molecule(), carbon, alcoholOxygen))
                .apply(pot.molecule());
        return pot.react(product, 0, carbon, alcoholOxygen);
    }

    /** Hydrogen across the double bond of an alkene. */
    private static ReactionRule hydrogenation() {
        return new ReactionRule("hydrogenation", "addition", 1,
                EnumSet.of(FunctionalGroup.ALKENE), Conditions.at(Warmth.AMBIENT, Set.of(NICKEL)),
                PolarReactions::hydrogenate);
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
        return pot.react(product, 0, alkene.atom(0), hydrogen[0]);
    }

    /** A halogen across the double bond of an alkene, one halogen to each carbon of it. */
    private static ReactionRule halogenation() {
        return new ReactionRule("halogenation", "addition", 1,
                EnumSet.of(FunctionalGroup.ALKENE), Conditions.at(Warmth.AMBIENT),
                PolarReactions::halogenate);
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
        return pot.react(product, 0, alkene.atom(0), halogen[0]);
    }

    /**
     * The hydrogen and the halogen of an acid across a double bond, the hydrogen to the carbon that carries
     * more hydrogens of the two - which is the whole of what the rule of Markovnikov says.
     */
    private static ReactionRule hydrohalogenation() {
        return new ReactionRule("hydrohalogenation", "addition", 1,
                EnumSet.of(FunctionalGroup.ALKENE), Conditions.at(Warmth.AMBIENT),
                PolarReactions::hydrohalogenate);
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
        return pot.react(product, 0, first, acid[0]);
    }

    /**
     * The hydrogen and the hydroxyl of water across a double bond, the hydrogen to the carbon that carries
     * more hydrogens of the two.
     */
    private static ReactionRule hydration() {
        return new ReactionRule("hydration", "addition", 1, EnumSet.of(FunctionalGroup.ALKENE),
                Conditions.at(Warmth.HEATED), PolarReactions::hydrate);
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
        return pot.react(product, 0, first, water[1]);
    }

    /** The carbon of an alkene that carries more hydrogens, which is the one a hydrogen is added to. */
    private static int plainerCarbonOf(Molecule molecule, Site alkene) {
        int first = alkene.atom(0);
        int second = alkene.atom(1);
        return Sites.hydrogensOn(molecule, first) >= Sites.hydrogensOn(molecule, second) ? first : second;
    }

    /** An alcohol losing water and leaving a double bond behind, the hydrogen of the carbon beside it going. */
    private static ReactionRule dehydration() {
        return new ReactionRule("dehydration", "elimination", 1,
                EnumSet.of(FunctionalGroup.HYDROXYL), Conditions.at(Warmth.HEATED),
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
                return pot.react(product, 0, alpha);
            }
        }
        return null;
    }

    /** Hydrogen across the double bond of a carbonyl, which is what takes an aldehyde down to an alcohol. */
    private static ReactionRule carbonylHydrogenation() {
        return new ReactionRule("carbonylHydrogenation", "reduction", 1,
                EnumSet.of(FunctionalGroup.CARBONYL), Conditions.at(Warmth.AMBIENT, Set.of(NICKEL)),
                PolarReactions::hydrogenateCarbonyl);
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
        return pot.react(product, 0, carbonyl.atom(0), hydrogen[0]);
    }

    /**
     * An alcohol losing hydrogen and coming out a carbonyl, which is the oxidation of the industry drawn the
     * way it is run: a hot metal takes the hydrogen off - the copper of a laboratory and the nickel of a
     * works are the same statement - and the bond left behind becomes the double one.
     * <p>
     * <b>Every alcohol is one of these and the carbon decides which carbonyl comes out.</b> A carbon that
     * carries two hydrogens beside its hydroxyl has one to spare and gives an aldehyde; one that carries a
     * single hydrogen gives a ketone; and one that carries none, or that already stands in a carbonyl, is
     * left alone. Which of the three it is falls out of the molecule and is not something the rule says.
     */
    private static ReactionRule dehydrogenation() {
        return new ReactionRule("dehydrogenation", "oxidation", 1,
                EnumSet.of(FunctionalGroup.HYDROXYL), Conditions.at(Warmth.HEATED, Set.of(NICKEL)),
                PolarReactions::dehydrogenate);
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
            return pot.react(product, 0, carbon);
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
        return new ReactionRule("esterification", "acyl substitution", 2,
                EnumSet.of(FunctionalGroup.CARBOXYLIC_ACID, FunctionalGroup.HYDROXYL),
                Conditions.at(Warmth.HEATED), PolarReactions::esterify);
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
                    || !mayJoin(pot, carbon, alcoholOxygen)) {
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
                return pot.react(product, 0, carbon, alcoholOxygen);
        }
        return null;
    }

    /**
     * An acid and an amine giving an amide and water, the reaction a protein and half the drugs there are are
     * built by.
     * <p>
     * The six arrows are the ones of the making of an ester with a nitrogen in the place of the alcohol's
     * oxygen: the lone pair of the amine into the carbonyl, the pair of the double bond to the carbonyl
     * oxygen, one hydrogen off the nitrogen onto the acid's own hydroxyl - which is what makes that hydroxyl
     * a water and not a hydroxide - the bond to it breaking with the pair, the pair back into the double
     * bond, and the amide left behind. The oxygen of the water is the acid's and the hydrogen is the amine's,
     * which is what a chemist would find if the two were labelled.
     * <p>
     * <b>The amine is written before the alcohol in the table.</b> A vessel that holds an acid with both an
     * amine and an alcohol in it reacts the amine first, because a nitrogen is the stronger nucleophile of the
     * two - and where that is written is here, in the order of the rules, since the engine has no measure of
     * how strong a nucleophile is beyond which of two rules is the sharper.
     */
    private static ReactionRule amideFormation() {
        return new ReactionRule("amideFormation", "acyl substitution", 2,
                EnumSet.of(FunctionalGroup.CARBOXYLIC_ACID, FunctionalGroup.AMINE),
                Conditions.at(Warmth.HEATED), PolarReactions::formAmide);
    }

    private static Reaction formAmide(Pot pot) {
        Molecule molecule = pot.molecule();
        Site acid = first(Sites.acids(molecule));
        int[] amine = amineWithHydrogenOf(molecule);
        if (acid == null || amine == null) {
            return null;
        }
        int carbon = acid.atom(0);
        int carbonylOxygen = acid.atom(1);
        int acidOxygen = acid.atom(2);
        int nitrogen = amine[0];
        int hydrogen = amine[1];
        if (bondBetween(molecule, carbon, nitrogen) != null || !mayJoin(pot, carbon, nitrogen)) {
            return null;
        }
        List<Arrow> arrows = List.of(
                Arrow.fromLonePair(nitrogen, nitrogen, carbon),
                Arrow.toLonePair(carbon, carbonylOxygen),
                Arrow.toLonePair(hydrogen, nitrogen),
                Arrow.fromLonePair(acidOxygen, acidOxygen, hydrogen),
                Arrow.toLonePair(carbon, acidOxygen),
                Arrow.fromLonePair(carbonylOxygen, carbonylOxygen, carbon));
        Molecule product = ElementaryStep.of("amideFormation", arrows).apply(molecule);
        return pot.react(product, 0, carbon, nitrogen);
    }

    /** A nitrogen that carries a hydrogen and hangs on a carbon, the nitrogen and one hydrogen of it. */
    private static int[] amineWithHydrogenOf(Molecule molecule) {
        for (int atom = 0; atom < molecule.atomCount(); atom++) {
            if (!isElement(molecule, atom, "N") || molecule.atom(atom).charge() != 0) {
                continue;
            }
            int hydrogen = -1;
            boolean carbon = false;
            for (int neighbour : molecule.neighbours(atom)) {
                if (isElement(molecule, neighbour, "H") && molecule.atom(neighbour).charge() == 0) {
                    hydrogen = hydrogen < 0 ? neighbour : hydrogen;
                } else if (isElement(molecule, neighbour, "C")) {
                    carbon = true;
                }
            }
            if (carbon && hydrogen >= 0) {
                return new int[] {atom, hydrogen};
            }
        }
        return null;
    }

    /**
     * A carbonyl and two alcohols giving an acetal and water, the reaction a carbonyl is hidden behind while
     * the rest of a molecule is worked on.
     * <p>
     * Eight arrows are drawn, the four of a hemiacetal and the four that take it the rest of the way: the
     * first alcohol into the carbonyl, the pair of the double bond to the oxygen, its own hydrogen onto that
     * oxygen - which is the hemiacetal - then the second alcohol into the carbon it left behind, the bond to
     * the first one's oxygen breaking with the pair, the second alcohol's own hydrogen off, and that hydrogen
     * onto the oxygen that just left, so that it leaves as water and not as a hydroxide.
     * <p>
     * <b>The two alcohols are the same substance</b>, which is what a vessel holding two molecules of one
     * alcohol is; a carbonyl with two different alcohols about it is left alone rather than answered with a
     * mixed acetal nobody asked for. The rule stands above the hemiacetal in the table, since a vessel that
     * holds two alcohols can go all the way and one that holds one cannot reach it at all.
     */
    private static ReactionRule acetalFormation() {
        return new ReactionRule("acetalFormation", "carbonyl addition", 2,
                EnumSet.of(FunctionalGroup.CARBONYL, FunctionalGroup.HYDROXYL),
                Conditions.at(Warmth.HEATED), PolarReactions::formAcetal);
    }

    private static Reaction formAcetal(Pot pot) {
        Molecule molecule = pot.molecule();
        Site carbonyl = bestCarbonyl(molecule);
        if (carbonyl == null) {
            return null;
        }
        int carbon = carbonyl.atom(0);
        int oxygen = carbonyl.atom(1);
        List<Site> hydroxyls = Sites.hydroxyls(molecule);
        for (Site first : hydroxyls) {
            int firstOxygen = first.atom(0);
            int firstHydrogen = hydrogenOn(molecule, firstOxygen);
            if (firstHydrogen < 0 || firstOxygen == oxygen || !mayJoin(pot, carbon, firstOxygen)) {
                continue;
            }
            for (Site second : hydroxyls) {
                int secondOxygen = second.atom(0);
                int secondHydrogen = hydrogenOn(molecule, secondOxygen);
                if (secondHydrogen < 0 || secondOxygen == firstOxygen || secondOxygen == oxygen
                        || pot.sharesAMolecule(secondOxygen, firstOxygen)
                        || !mayJoin(pot, carbon, secondOxygen)) {
                    continue;
                }
                List<Arrow> arrows = List.of(
                        Arrow.fromLonePair(firstOxygen, firstOxygen, carbon),
                        Arrow.toLonePair(carbon, oxygen),
                        Arrow.toLonePair(firstHydrogen, firstOxygen),
                        Arrow.fromLonePair(oxygen, oxygen, firstHydrogen),
                        Arrow.fromLonePair(secondOxygen, secondOxygen, carbon),
                        Arrow.toLonePair(carbon, oxygen),
                        Arrow.toLonePair(secondHydrogen, secondOxygen),
                        Arrow.fromLonePair(oxygen, oxygen, secondHydrogen));
                Molecule product = ElementaryStep.of("acetalFormation", arrows).apply(molecule);
                return pot.react(product, 0, carbon, firstOxygen, secondOxygen);
            }
        }
        return null;
    }

    /**
     * A ketone and an amine of two carbons giving an enamine and water, the reaction a carbonyl is made into
     * the soft nucleophile the trade builds rings with.
     * <p>
     * Seven arrows are drawn, the six of an imine as far as the carbinolamine and the one that takes it
     * away: the amine into the carbonyl, the pair of the double bond to the oxygen, the one hydrogen the
     * nitrogen carries off onto that oxygen, the second carbon of the nitrogen losing the hydrogen beside it
     * - which is what makes the double bond of the enamine, since an amine of two carbons has no hydrogen
     * left to lose off the nitrogen itself - the bond to the hydroxyl breaking with the pair, and that
     * hydrogen onto the hydroxyl so that it leaves as water.
     */
    private static ReactionRule enamineFormation() {
        return new ReactionRule("enamineFormation", "condensation", 6,
                EnumSet.of(FunctionalGroup.CARBONYL, FunctionalGroup.AMINE),
                Conditions.at(Warmth.HEATED), PolarReactions::formEnamine);
    }

    private static Reaction formEnamine(Pot pot) {
        Molecule molecule = pot.molecule();
        Site carbonyl = bestCarbonyl(molecule);
        int[] amine = secondaryAmineOf(molecule);
        if (carbonyl == null || amine == null) {
            return null;
        }
        int carbon = carbonyl.atom(0);
        int oxygen = carbonyl.atom(1);
        int nitrogen = amine[0];
        int hydrogen = amine[1];
        int alpha = alphaCarbonOf(molecule, carbon);
        if (alpha < 0) {
            return null;
        }
        int alphaHydrogen = hydrogenOn(molecule, alpha);
        if (alphaHydrogen < 0 || bondBetween(molecule, carbon, nitrogen) != null
                || !mayJoin(pot, carbon, nitrogen)) {
            return null;
        }
        List<Arrow> arrows = List.of(
                Arrow.fromLonePair(nitrogen, nitrogen, carbon),
                Arrow.toLonePair(carbon, oxygen),
                Arrow.toLonePair(hydrogen, nitrogen),
                Arrow.fromLonePair(oxygen, oxygen, hydrogen),
                Arrow.betweenBonds(alpha, alphaHydrogen, alpha, carbon),
                Arrow.toLonePair(carbon, oxygen),
                Arrow.fromLonePair(oxygen, oxygen, alphaHydrogen));
        Molecule product = ElementaryStep.of("enamineFormation", arrows).apply(molecule);
        return pot.react(product, 0, carbon, nitrogen);
    }

    /**
     * The nitrogen of an amine of two carbons, with the one hydrogen it carries.
     *
     * @param molecule molecule to read
     * @return the nitrogen and its hydrogen, or {@code null} when there is none
     */
    private static int[] secondaryAmineOf(Molecule molecule) {
        for (int atom = 0; atom < molecule.atomCount(); atom++) {
            if (!isElement(molecule, atom, "N") || molecule.atom(atom).charge() != 0) {
                continue;
            }
            int hydrogen = -1;
            int carbons = 0;
            boolean other = false;
            for (int neighbour : molecule.neighbours(atom)) {
                if (isElement(molecule, neighbour, "H") && molecule.atom(neighbour).charge() == 0) {
                    hydrogen = hydrogen < 0 ? neighbour : -2;
                } else if (isElement(molecule, neighbour, "C")) {
                    carbons++;
                } else {
                    other = true;
                }
            }
            if (hydrogen >= 0 && carbons == 2 && !other) {
                return new int[] {atom, hydrogen};
            }
        }
        return null;
    }

    /** An ester and water giving the acid and the alcohol back, the same six arrows the other way round. */
    private static ReactionRule esterHydrolysis() {
        return new ReactionRule("esterHydrolysis", "acyl substitution", 1,
                EnumSet.of(FunctionalGroup.ESTER), Conditions.at(Warmth.HEATED, WATER),
                PolarReactions::hydrolyseEster);
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
        return pot.react(product, 0, carbon, waterOxygen);
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
        return new ReactionRule("alkaneHalogenation", "radical", 1,
                EnumSet.of(FunctionalGroup.ALKYL), Conditions.at(Warmth.HEATED),
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
        return pot.react(product, 0, carbon, halogen[0]);
    }

    /**
     * An anion of a molecule taking a proton from water, the last thing that happens to a reaction of the
     * organic side.
     * <p>
     * <b>A reaction that leaves a charge behind is a reaction that is not finished.</b> The cyanide that comes
     * into a carbonyl leaves the oxygen of it holding the pair, and the aldol leaves a carbon beside the
     * carbonyl holding one; what a chemist writes on the arrow is the anion, and what a chemist then does is
     * pour the vessel into water and let it take a proton. That step is written here as a step of its own,
     * with two arrows: the water's own bond to one of its hydrogens breaking and the pair staying on the
     * oxygen - which is what makes the water a hydroxide - and the pair the anion held making the bond to that
     * hydrogen. So the anion comes out neutral and the vessel gains a hydroxide, which is the whole of what
     * an acid and a base do to one another.
     * <p>
     * <b>It stands last in the table of the rules.</b> A workup is what happens when nothing else can, and a
     * rule that named a group is a better answer for a vessel than one that names a charge - so this rule is
     * written with a selectivity of nothing and stands lowest of all, and a vessel that could have condensed
     * something condenses it first and is worked up second, which is the order a chemist works in.
     */
    private static ReactionRule protonation() {
        return new ReactionRule("protonation", "acid and base", 0, EnumSet.of(FunctionalGroup.ANION),
                Conditions.at(Warmth.AMBIENT), PolarReactions::protonate);
    }

    private static Reaction protonate(Pot pot) {
        Molecule molecule = pot.molecule();
        Site anion = first(Sites.anions(molecule));
        int[] water = water(molecule);
        if (anion == null || water == null) {
            return null;
        }
        int charged = anion.atom(0);
        int hydrogen = water[0];
        int oxygen = water[1];
        List<Arrow> arrows = List.of(
                Arrow.toLonePair(hydrogen, oxygen),
                Arrow.fromLonePair(charged, charged, hydrogen));
        Molecule product = ElementaryStep.of("protonation", arrows).apply(molecule);
        return pot.react(product, 0, charged, oxygen);
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
                Conditions.at(Warmth.COLD), PolarReactions::alkylateEnolate);
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
            if (hydrogen < 0 || !mayJoin(pot, alpha, alkylCarbon)) {
                continue;
            }
            List<Arrow> arrows = List.of(
                    Arrow.betweenBonds(alpha, hydrogen, alpha, alkylCarbon),
                    Arrow.toLonePair(alkylCarbon, halogen),
                    Arrow.fromLonePair(halogen, halogen, hydrogen));
            Molecule product = ElementaryStep.of("enolateAlkylation", arrows).apply(molecule);
                return pot.react(product, 0, alpha, alkylCarbon);
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
                EnumSet.of(FunctionalGroup.ESTER), Conditions.at(Warmth.HEATED),
                PolarReactions::condenseClaisen);
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
                if (alkoxide == giving.atom(2) || bondBetween(molecule, carbon, alpha) != null
                        || !mayJoin(pot, alpha, carbon)) {
                    continue;
                }
                List<Arrow> arrows = List.of(
                        Arrow.betweenBonds(alpha, hydrogen, alpha, carbon),
                        Arrow.toLonePair(carbon, oxygen),
                        Arrow.toLonePair(carbon, alkoxide),
                        Arrow.fromLonePair(oxygen, oxygen, carbon),
                        Arrow.fromLonePair(alkoxide, alkoxide, hydrogen));
                Molecule product = ElementaryStep.of("claisenCondensation", arrows).apply(molecule);
                return pot.react(product, 0, alpha, carbon);
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
                Conditions.at(Warmth.AMBIENT), PolarReactions::addMichael);
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
                    || !mayJoin(pot, alpha, far)) {
                continue;
            }
            List<Arrow> arrows = List.of(
                    Arrow.betweenBonds(alpha, hydrogen, alpha, far),
                    Arrow.betweenBonds(far, near, near, carbonEnone),
                    Arrow.toLonePair(carbonEnone, oxygenEnone),
                    Arrow.fromLonePair(oxygenEnone, oxygenEnone, hydrogen));
            Molecule product = ElementaryStep.of("michaelAddition", arrows).apply(molecule);
                return pot.react(product, 0, alpha, far);
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
                Conditions.at(Warmth.AMBIENT), PolarReactions::formImine);
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
        return pot.react(product, 0, carbon, nitrogen);
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
                EnumSet.of(FunctionalGroup.AROMATIC_RING),
                Conditions.at(Warmth.HEATED, SULFURIC_ACID), PolarReactions::sulfonate);
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
                return pot.react(product, 0, position, sulfur);
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

    /**
     * The carbonyl a nucleophile attacks first: the readier one, see {@link Reactivity}.
     * <p>
     * <b>A carbonyl that carries an oxygen of its own is not one of these.</b> An acid and an ester are
     * attacked by a nucleophile too, but what that attack gives is not what the addition to a plain carbonyl
     * gives, and the rules of those two are written against the acid and the ester as whole groups - so an
     * amine in search of a carbonyl to condense with walks past an acid instead of answering with an imine of
     * it, which no chemist has ever made.
     */
    private static Site bestCarbonyl(Molecule molecule) {
        Site best = null;
        int highest = Integer.MIN_VALUE;
        for (Site carbonyl : Sites.carbonyls(molecule)) {
            if (carriesAnOxygenOfItsOwn(molecule, carbonyl.atom(0), carbonyl.atom(1))) {
                continue;
            }
            int score = Reactivity.electrophilicity(molecule, carbonyl.atom(0));
            if (score > highest) {
                highest = score;
                best = carbonyl;
            }
        }
        return best;
    }

    /** {@code true} when a carbonyl carbon carries an oxygen that is no part of its double bond. */
    private static boolean carriesAnOxygenOfItsOwn(Molecule molecule, int carbon, int carbonylOxygen) {
        for (int neighbour : molecule.neighbours(carbon)) {
            if (neighbour == carbonylOxygen || !isElement(molecule, neighbour, "O")) {
                continue;
            }
            Bond bond = bondBetween(molecule, carbon, neighbour);
            if (bond != null && bond.order() == 1) {
                return true;
            }
        }
        return false;
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

    /**
     * An enol falling into the carbonyl it stands for, which is the last step of every addition that went
     * through one.
     * <p>
     * An enol is not a substance a vessel holds for long: the hydrogen that found its way to the oxygen turns
     * round and goes back to the far carbon, and the pair the double bond left behind closes the oxygen onto
     * the carbonyl. Three arrows are drawn, and what comes out is the carbonyl the enol was the shape of - a
     * ketone if the carbon the hydroxyl hangs on carries another carbon, an aldehyde if it carries a
     * hydrogen. The rule stands high in the table because the trade never writes an enol down as a product:
     * what a vessel holds is what the enol becomes.
     */
    private static ReactionRule ketoEnolTautomerism() {
        return new ReactionRule("ketoEnolTautomerism", "tautomerism", 5,
                EnumSet.of(FunctionalGroup.ENOL), Conditions.at(Warmth.AMBIENT),
                PolarReactions::tautomerise);
    }

    private static Reaction tautomerise(Pot pot) {
        Molecule molecule = pot.molecule();
        for (Site enol : Sites.enols(molecule)) {
            int oxygen = enol.atom(0);
            int carbon = enol.atom(1);
            int far = enol.atom(2);
            int hydrogen = hydrogenOn(molecule, oxygen);
            if (hydrogen < 0) {
                continue;
            }
            List<Arrow> arrows = List.of(
                    Arrow.toLonePair(hydrogen, oxygen),
                    Arrow.betweenBonds(carbon, far, far, hydrogen),
                    Arrow.fromLonePair(oxygen, oxygen, carbon));
            Molecule product = ElementaryStep.of("ketoEnolTautomerism", arrows).apply(molecule);
            return pot.react(product, 0, carbon, far);
        }
        return null;
    }

    /** Hydrogen across the triple bond of an alkyne, which leaves the double bond of an alkene behind. */
    private static ReactionRule alkyneHydrogenation() {
        return new ReactionRule("alkyneHydrogenation", "addition", 1, EnumSet.of(FunctionalGroup.ALKYNE),
                Conditions.at(Warmth.AMBIENT, Set.of(NICKEL)), PolarReactions::hydrogenateAlkyne);
    }

    private static Reaction hydrogenateAlkyne(Pot pot) {
        Site alkyne = first(Sites.alkynes(pot.molecule()));
        int[] hydrogen = hydrogenPair(pot.molecule());
        if (alkyne == null || hydrogen == null) {
            return null;
        }
        Molecule product = ElementaryStep
                .across("alkyneHydrogenation", alkyne.atom(0), alkyne.atom(1), hydrogen[0], hydrogen[1])
                .apply(pot.molecule());
        return pot.react(product, 0, alkyne.atom(0), hydrogen[0]);
    }

    /** The hydrogen and the halogen of an acid across the triple bond, the hydrogen to the plainer carbon. */
    private static ReactionRule alkyneHydrohalogenation() {
        return new ReactionRule("alkyneHydrohalogenation", "addition", 1,
                EnumSet.of(FunctionalGroup.ALKYNE), Conditions.at(Warmth.AMBIENT),
                PolarReactions::hydrohalogenateAlkyne);
    }

    private static Reaction hydrohalogenateAlkyne(Pot pot) {
        Site alkyne = first(Sites.alkynes(pot.molecule()));
        int[] acid = hydrogenHalide(pot.molecule());
        if (alkyne == null || acid == null) {
            return null;
        }
        int first = plainerCarbonOf(pot.molecule(), alkyne);
        int second = first == alkyne.atom(0) ? alkyne.atom(1) : alkyne.atom(0);
        Molecule product = ElementaryStep
                .across("alkyneHydrohalogenation", first, second, acid[0], acid[1])
                .apply(pot.molecule());
        return pot.react(product, 0, first, acid[0]);
    }

    /**
     * The hydrogen of water across the triple bond of an alkyne, which leaves an enol behind.
     * <p>
     * The water goes on the Markovnikov way, the hydrogen to the carbon that carries more of them, so the
     * hydroxyl lands on the carbon that carries the carbon of the chain - and what stands there for a moment
     * is an enol, which the tautomerism of the table turns into the ketone. That is why hydrating an alkyne
     * gives a ketone and hydrating an alkene gives an alcohol.
     */
    private static ReactionRule alkyneHydration() {
        return new ReactionRule("alkyneHydration", "addition", 1, EnumSet.of(FunctionalGroup.ALKYNE),
                Conditions.at(Warmth.HEATED), PolarReactions::hydrateAlkyne);
    }

    private static Reaction hydrateAlkyne(Pot pot) {
        Site alkyne = first(Sites.alkynes(pot.molecule()));
        int[] water = water(pot.molecule());
        if (alkyne == null || water == null) {
            return null;
        }
        int first = plainerCarbonOf(pot.molecule(), alkyne);
        int second = first == alkyne.atom(0) ? alkyne.atom(1) : alkyne.atom(0);
        Molecule product = ElementaryStep
                .across("alkyneHydration", first, second, water[0], water[1]).apply(pot.molecule());
        return pot.react(product, 0, first, water[1]);
    }

    /**
     * The water across a nitrile, which leaves the shape the amide is a breath away from.
     * <p>
     * Four arrows are drawn: the lone pair of the water into the carbon of the nitrile, one pair of the triple
     * bond onto the nitrogen - which is what makes the nitrogen hold a charge - the water's own hydrogen off
     * with the pair staying on its oxygen, and that hydrogen onto the nitrogen. What comes out is a hydroxyl
     * on a carbon held to a nitrogen by a double bond, and the tautomerism of the table turns that into the
     * amide: hydrolysing a nitrile is two steps, and the engine runs it as two.
     */
    private static ReactionRule nitrileHydrolysis() {
        return new ReactionRule("nitrileHydrolysis", "acyl substitution", 2,
                EnumSet.of(FunctionalGroup.NITRILE), Conditions.at(Warmth.HEATED, WATER),
                PolarReactions::hydrolyseNitrile);
    }

    private static Reaction hydrolyseNitrile(Pot pot) {
        Molecule molecule = pot.molecule();
        Site nitrile = first(Sites.nitriles(molecule));
        int[] water = water(molecule);
        if (nitrile == null || water == null) {
            return null;
        }
        int carbon = nitrile.atom(0);
        int nitrogen = nitrile.atom(1);
        int hydrogen = water[0];
        int oxygen = water[1];
        List<Arrow> arrows = List.of(
                Arrow.fromLonePair(oxygen, oxygen, carbon),
                Arrow.toLonePair(carbon, nitrogen),
                Arrow.toLonePair(hydrogen, oxygen),
                Arrow.fromLonePair(nitrogen, nitrogen, hydrogen));
        Molecule product = ElementaryStep.of("nitrileHydrolysis", arrows).apply(molecule);
        return pot.react(product, 0, carbon, oxygen);
    }

    /** Hydrogen across the triple bond of a nitrile, which leaves the imine behind. */
    private static ReactionRule nitrileHydrogenation() {
        return new ReactionRule("nitrileHydrogenation", "addition", 1,
                EnumSet.of(FunctionalGroup.NITRILE), Conditions.at(Warmth.AMBIENT, Set.of(NICKEL)),
                PolarReactions::hydrogenateNitrile);
    }

    private static Reaction hydrogenateNitrile(Pot pot) {
        Site nitrile = first(Sites.nitriles(pot.molecule()));
        int[] hydrogen = hydrogenPair(pot.molecule());
        if (nitrile == null || hydrogen == null) {
            return null;
        }
        Molecule product = ElementaryStep
                .across("nitrileHydrogenation", nitrile.atom(0), nitrile.atom(1), hydrogen[0],
                        hydrogen[1])
                .apply(pot.molecule());
        return pot.react(product, 0, nitrile.atom(0), hydrogen[0]);
    }

    /**
     * Hydrogen across the double bond of an imine, which is the second half of making an amine out of a
     * carbonyl - and the whole reason a ketone is worth condensing with an amine in the first place.
     */
    private static ReactionRule imineReduction() {
        return new ReactionRule("imineReduction", "reduction", 1, EnumSet.of(FunctionalGroup.IMINE),
                Conditions.at(Warmth.AMBIENT, Set.of(NICKEL)), PolarReactions::reduceImine);
    }

    private static Reaction reduceImine(Pot pot) {
        Site imine = first(Sites.imines(pot.molecule()));
        int[] hydrogen = hydrogenPair(pot.molecule());
        if (imine == null || hydrogen == null) {
            return null;
        }
        Molecule product = ElementaryStep
                .across("imineReduction", imine.atom(1), imine.atom(0), hydrogen[0], hydrogen[1])
                .apply(pot.molecule());
        return pot.react(product, 0, imine.atom(0), hydrogen[0]);
    }

    /**
     * The water across an imine, which takes it back to the carbonyl it came from and leaves the amine.
     * <p>
     * Eight arrows are drawn and they are the condensation read backwards: the water into the carbon, one
     * pair of the double bond onto the nitrogen, the water's first hydrogen onto the oxygen and the pair of
     * the nitrogen onto it - which is the carbinolamine - the hydroxyl's own hydrogen off and onto the
     * nitrogen so that the nitrogen leaves holding four bonds, the bond between them breaking, and the pair
     * the oxygen holds closing the carbonyl. Both hydrogens of the water end up on the nitrogen, which is why
     * the imine of an aldehyde gives ammonia.
     */
    private static ReactionRule imineHydrolysis() {
        return new ReactionRule("imineHydrolysis", "hydrolysis", 2, EnumSet.of(FunctionalGroup.IMINE),
                Conditions.at(Warmth.HEATED, WATER), PolarReactions::hydrolyseImine);
    }

    private static Reaction hydrolyseImine(Pot pot) {
        Molecule molecule = pot.molecule();
        Site imine = first(Sites.imines(molecule));
        int[] water = water(molecule);
        if (imine == null || water == null) {
            return null;
        }
        int carbon = imine.atom(0);
        int nitrogen = imine.atom(1);
        int oxygen = water[1];
        int[] hydrogens = waterHydrogens(molecule, oxygen);
        if (hydrogens == null) {
            return null;
        }
        int first = hydrogens[0];
        int second = hydrogens[1];
        List<Arrow> arrows = List.of(
                Arrow.fromLonePair(oxygen, oxygen, carbon),
                Arrow.toLonePair(carbon, nitrogen),
                Arrow.toLonePair(first, oxygen),
                Arrow.fromLonePair(nitrogen, nitrogen, first),
                Arrow.toLonePair(second, oxygen),
                Arrow.fromLonePair(nitrogen, nitrogen, second),
                Arrow.toLonePair(carbon, nitrogen),
                Arrow.fromLonePair(oxygen, oxygen, carbon));
        Molecule product = ElementaryStep.of("imineHydrolysis", arrows).apply(molecule);
        return pot.react(product, 0, carbon, oxygen);
    }

    /**
     * The water across an amide, which gives the acid and the amine back.
     * <p>
     * It is drawn as the imine's hydrolysis is, with the nitrogen of the amide in the place of the one of the
     * imine: the water into the carbonyl, the pair of the double bond onto the oxygen that held it, the
     * water's first hydrogen onto that oxygen - which is the intermediate an acid and an amine are made
     * through - its second hydrogen onto the nitrogen so that the nitrogen leaves holding four bonds, the
     * bond between them breaking, and the pair the water's oxygen holds closing the carbonyl. So the acid
     * comes out of the water's oxygen and the amine of the amide's nitrogen, which is what a chemist with
     * labelled atoms would find.
     */
    private static ReactionRule amideHydrolysis() {
        return new ReactionRule("amideHydrolysis", "hydrolysis", 2, EnumSet.of(FunctionalGroup.AMIDE),
                Conditions.at(Warmth.HEATED, WATER), PolarReactions::hydrolyseAmide);
    }

    private static Reaction hydrolyseAmide(Pot pot) {
        Molecule molecule = pot.molecule();
        Site amide = first(Sites.amides(molecule));
        int[] water = water(molecule);
        if (amide == null || water == null) {
            return null;
        }
        int carbon = amide.atom(0);
        int carbonylOxygen = amide.atom(1);
        int nitrogen = amide.atom(2);
        int oxygen = water[1];
        int[] hydrogens = waterHydrogens(molecule, oxygen);
        if (hydrogens == null) {
            return null;
        }
        int first = hydrogens[0];
        int second = hydrogens[1];
        List<Arrow> arrows = List.of(
                Arrow.fromLonePair(oxygen, oxygen, carbon),
                Arrow.toLonePair(carbon, carbonylOxygen),
                Arrow.toLonePair(first, oxygen),
                Arrow.fromLonePair(carbonylOxygen, carbonylOxygen, first),
                Arrow.toLonePair(second, oxygen),
                Arrow.fromLonePair(nitrogen, nitrogen, second),
                Arrow.toLonePair(carbon, nitrogen),
                Arrow.fromLonePair(oxygen, oxygen, carbon));
        Molecule product = ElementaryStep.of("amideHydrolysis", arrows).apply(molecule);
        return pot.react(product, 0, carbon, oxygen);
    }

    /**
     * An ester and another alcohol, which is how an ester is changed into another one without ever going back
     * to the acid: the same six arrows the making of an ester is drawn with, and what leaves is the alcohol
     * the ester was made of.
     */
    private static ReactionRule transesterification() {
        return new ReactionRule("transesterification", "acyl substitution", 2,
                EnumSet.of(FunctionalGroup.ESTER, FunctionalGroup.HYDROXYL),
                Conditions.at(Warmth.HEATED), PolarReactions::transesterify);
    }

    private static Reaction transesterify(Pot pot) {
        Molecule molecule = pot.molecule();
        Site ester = first(Sites.esters(molecule));
        if (ester == null) {
            return null;
        }
        int carbon = ester.atom(0);
        int carbonylOxygen = ester.atom(1);
        int esterOxygen = ester.atom(2);
        for (Site hydroxyl : Sites.hydroxyls(molecule)) {
            int alcoholOxygen = hydroxyl.atom(0);
            int alcoholHydrogen = hydrogenOn(molecule, alcoholOxygen);
            if (alcoholHydrogen < 0 || alcoholOxygen == esterOxygen || alcoholOxygen == carbonylOxygen
                    || pot.sharesAMolecule(alcoholOxygen, carbon)
                    || bondBetween(molecule, carbon, alcoholOxygen) != null) {
                continue;
            }
            List<Arrow> arrows = List.of(
                    Arrow.fromLonePair(alcoholOxygen, alcoholOxygen, carbon),
                    Arrow.toLonePair(carbon, carbonylOxygen),
                    Arrow.toLonePair(carbon, esterOxygen),
                    Arrow.fromLonePair(carbonylOxygen, carbonylOxygen, carbon),
                    Arrow.toLonePair(alcoholHydrogen, alcoholOxygen),
                    Arrow.fromLonePair(esterOxygen, esterOxygen, alcoholHydrogen));
            Molecule product = ElementaryStep.of("transesterification", arrows).apply(molecule);
            return pot.react(product, 0, carbon, alcoholOxygen);
        }
        return null;
    }

    /**
     * An ester and an amine, which gives the amide and the alcohol: the same six arrows once more, with a
     * nitrogen where the oxygen that comes in was - which is how a protein is made when the acid of it will
     * not stand being heated.
     */
    private static ReactionRule aminolysisOfEster() {
        return new ReactionRule("aminolysisOfEster", "acyl substitution", 2,
                EnumSet.of(FunctionalGroup.ESTER, FunctionalGroup.AMINE),
                Conditions.at(Warmth.HEATED), PolarReactions::aminolyseEster);
    }

    private static Reaction aminolyseEster(Pot pot) {
        Molecule molecule = pot.molecule();
        Site ester = first(Sites.esters(molecule));
        int[] amine = amineWithHydrogenOf(molecule);
        if (ester == null || amine == null) {
            return null;
        }
        int carbon = ester.atom(0);
        int carbonylOxygen = ester.atom(1);
        int esterOxygen = ester.atom(2);
        int nitrogen = amine[0];
        int hydrogen = amine[1];
        if (pot.sharesAMolecule(nitrogen, carbon)
                || bondBetween(molecule, carbon, nitrogen) != null) {
            return null;
        }
        List<Arrow> arrows = List.of(
                Arrow.fromLonePair(nitrogen, nitrogen, carbon),
                Arrow.toLonePair(carbon, carbonylOxygen),
                Arrow.toLonePair(carbon, esterOxygen),
                Arrow.fromLonePair(carbonylOxygen, carbonylOxygen, carbon),
                Arrow.toLonePair(hydrogen, nitrogen),
                Arrow.fromLonePair(esterOxygen, esterOxygen, hydrogen));
        Molecule product = ElementaryStep.of("aminolysisOfEster", arrows).apply(molecule);
        return pot.react(product, 0, carbon, nitrogen);
    }

    /**
     * Draws a substitution on a ring: the ring written out as Kekulé wrote it, whatever the rule draws first
     * drawn on that, the electrophile taking the place of a hydrogen, and the ring read as a ring again.
     * <p>
     * Five rules of the aromatic side are this one shape - the sulfur of an acid, the nitrogen of nitric acid,
     * the carbon of an alkyl halide and of an acid chloride, and a halogen of a molecule of one - and they
     * differ in nothing but what is drawn before the attack and what is drawn at the moment of it. The place
     * on the ring is the one the group already standing there sends an electrophile to, so the regiochemistry
     * of a ring comes out of one table for all of them, see {@link Reactivity#directsToTheSides}.
     *
     * @param pot the vessel
     * @param electrophile the atom that takes the place of the hydrogen
     * @param before the arrows drawn before the ring is touched
     * @param onAttack the arrows drawn at the moment the ring attacks
     * @param landing the atom the hydrogen that leaves goes to
     * @param touched the atoms the rule touched besides the two of the ring
     * @return the reaction, or {@code null} when the ring has no place to give
     */
    private static Reaction onTheRing(Pot pot, int electrophile, List<Arrow> before, List<Arrow> onAttack,
            int landing, int... touched) {
        Molecule molecule = pot.molecule();
        Molecule drawn = Assemblies.kekulized(molecule);
        for (Site ring : Sites.aromaticRings(molecule)) {
            Molecule ready = drawn;
            for (Arrow arrow : before) {
                ready = arrow.apply(ready);
            }
            int[] place = attackPlace(ready, ring.atoms());
            if (place == null) {
                continue;
            }
            int position = place[0];
            int beside = place[1];
            if (!mayJoin(pot, position, electrophile)) {
                // Two places of one molecule may close a ring of five or six - the ketone a ring is closed
                // on itself into, the imidazole a diamine is closed into - and two places of two molecules
                // may always be joined; a ring of three or four is what mayJoin refuses.
                continue;
            }
            int hydrogen = hydrogenOn(ready, position);
            if (hydrogen < 0) {
                continue;
            }
            Molecule step = Arrow.betweenBonds(position, beside, position, electrophile).apply(ready);
            for (Arrow arrow : onAttack) {
                step = arrow.apply(step);
            }
            step = Arrow.betweenBonds(position, hydrogen, position, beside).apply(step);
            if (landing >= 0) {
                step = Arrow.fromLonePair(landing, landing, hydrogen).apply(step);
            }
            int[] atoms = new int[touched.length + 2];
            atoms[0] = position;
            atoms[1] = electrophile;
            for (int index = 0; index < touched.length; index++) {
                atoms[index + 2] = touched[index];
            }
            return pot.react(Assemblies.aromatized(step), 0, atoms);
        }
        return null;
    }

    /**
     * The nitrogen of nitric acid taking the place of a hydrogen on a ring, which is what gives a ring its
     * nitro group.
     * <p>
     * The sulfuric acid the reaction is run over is a catalyst and never a substance the reaction spends: the
     * rule asks for it among the conditions, and the arrow that gives the nitric acid its proton is followed
     * by one that takes that proton back off the ring, so the acid comes out of the vessel as it went in and
     * the balance of the answer is the ring and the nitric acid alone.
     */
    private static ReactionRule nitration() {
        return new ReactionRule("nitration", "substitution", 4,
                EnumSet.of(FunctionalGroup.AROMATIC_RING),
                Conditions.builder().warmth(Warmth.HEATED).catalyst(SULFURIC_ACID).build(),
                PolarReactions::nitrate);
    }

    private static Reaction nitrate(Pot pot) {
        Molecule molecule = pot.molecule();
        int[] nitric = nitricAcid(molecule);
        if (nitric == null) {
            return null;
        }
        int nitrogen = nitric[0];
        int hydroxyl = nitric[1];
        int charged = nitric[2];
        int hydroxylHydrogen = hydrogenOn(molecule, hydroxyl);
        if (hydroxylHydrogen < 0) {
            return null;
        }
        List<Arrow> before = List.of(
                Arrow.toLonePair(hydroxylHydrogen, hydroxyl),
                Arrow.fromLonePair(charged, charged, hydroxylHydrogen));
        List<Arrow> onAttack = List.of(
                Arrow.toLonePair(nitrogen, charged),
                Arrow.fromLonePair(hydroxyl, hydroxyl, nitrogen));
        return onTheRing(pot, nitrogen, before, onAttack, charged, nitrogen, charged);
    }

    /**
     * The nitrogen of a nitric acid, its hydroxyl oxygen, and the oxygen that carries the charge of it.
     *
     * @param molecule molecule to read
     * @return the three atoms, or {@code null} when the vessel holds no nitric acid
     */
    private static int[] nitricAcid(Molecule molecule) {
        for (int atom = 0; atom < molecule.atomCount(); atom++) {
            if (!isElement(molecule, atom, "N") || molecule.neighbours(atom).size() != 3) {
                continue;
            }
            int hydroxyl = -1;
            int charged = -1;
            boolean allOxygen = true;
            for (int neighbour : molecule.neighbours(atom)) {
                if (!isElement(molecule, neighbour, "O")) {
                    allOxygen = false;
                    break;
                }
                if (molecule.atom(neighbour).charge() < 0) {
                    charged = neighbour;
                } else if (hydrogenOn(molecule, neighbour) >= 0) {
                    hydroxyl = neighbour;
                }
            }
            if (allOxygen && hydroxyl >= 0 && charged >= 0) {
                return new int[] {atom, hydroxyl, charged};
            }
        }
        return null;
    }

    /** An alkyl group of a halide taking the place of a hydrogen on a ring, over a Lewis acid. */
    private static ReactionRule friedelCraftsAlkylation() {
        return new ReactionRule("friedelCraftsAlkylation", "substitution", 3,
                EnumSet.of(FunctionalGroup.AROMATIC_RING, FunctionalGroup.HALIDE),
                Conditions.builder().warmth(Warmth.HEATED).catalyst(ALUMINIUM_CHLORIDE).build(),
                PolarReactions::alkylateRing);
    }

    private static Reaction alkylateRing(Pot pot) {
        Site halide = bestHalide(pot.molecule());
        if (halide == null) {
            return null;
        }
        int carbon = halide.atom(0);
        int halogen = halide.atom(1);
        if (carriesDoubleBond(pot.molecule(), carbon)) {
            // An aryl halide is no alkyl halide: its carbon is held flat by the ring and a ring is never
            // given an alkyl group by one of its own.
            return null;
        }
        return onTheRing(pot, carbon, List.of(Arrow.toLonePair(carbon, halogen)), List.of(), halogen,
                carbon, halogen);
    }

    /** {@code true} when an atom is held by a double or a triple bond, which makes it no saturated one. */
    private static boolean carriesDoubleBond(Molecule molecule, int atom) {
        for (int index : molecule.bondsOf(atom)) {
            if (molecule.bonds().get(index).order() > 1) {
                return true;
            }
        }
        return molecule.atom(atom).isAromatic();
    }

    /** The acyl group of an acid chloride taking the place of a hydrogen on a ring, over a Lewis acid. */
    private static ReactionRule friedelCraftsAcylation() {
        return new ReactionRule("friedelCraftsAcylation", "substitution", 3,
                EnumSet.of(FunctionalGroup.AROMATIC_RING, FunctionalGroup.ACYL_HALIDE),
                Conditions.builder().warmth(Warmth.HEATED).catalyst(ALUMINIUM_CHLORIDE).build(),
                PolarReactions::acylateRing);
    }

    private static Reaction acylateRing(Pot pot) {
        Site acyl = first(Sites.acylHalides(pot.molecule()));
        if (acyl == null) {
            return null;
        }
        int carbon = acyl.atom(0);
        int halogen = acyl.atom(2);
        // The carbon of an acid chloride keeps its oxygen held by two pairs all the way through: what makes
        // it an electrophile is the acid that leaves it, and what the ring attacks is that carbon as it is.
        return onTheRing(pot, carbon, List.of(Arrow.toLonePair(carbon, halogen)), List.of(), halogen,
                carbon, halogen);
    }

    /** A halogen of a molecule of a halogen taking the place of a hydrogen on a ring, over a Lewis acid. */
    private static ReactionRule aromaticHalogenation() {
        return new ReactionRule("aromaticHalogenation", "substitution", 3,
                EnumSet.of(FunctionalGroup.AROMATIC_RING),
                Conditions.builder().warmth(Warmth.HEATED).catalyst(IRON_BROMIDE).build(),
                PolarReactions::halogenateRing);
    }

    private static Reaction halogenateRing(Pot pot) {
        int[] halogen = halogenPair(pot.molecule());
        if (halogen == null) {
            return null;
        }
        return onTheRing(pot, halogen[0], List.of(), List.of(Arrow.toLonePair(halogen[0], halogen[1])),
                halogen[1], halogen[0], halogen[1]);
    }

    /**
     * A nucleophile taking the place of a halogen on a ring that carries something pulling electrons out of
     * it, which is the one substitution a ring does by itself.
     * <p>
     * <b>A plain ring does not do this.</b> Chlorobenzene stands in a bottle of lye and nothing happens to it;
     * what makes the difference is a nitro group beside the halogen - or across from it - because that is what
     * pulls the electrons of the ring towards it and leaves the carbon the halogen hangs on open to an attack.
     * So the rule asks for such a group and for the halide to stand beside it or across from it, which is the
     * regiochemistry of the reaction and not something a rule may leave out.
     */
    private static ReactionRule aromaticSubstitution() {
        return new ReactionRule("snAr", "substitution", 3,
                EnumSet.of(FunctionalGroup.AROMATIC_RING, FunctionalGroup.HALIDE),
                Conditions.at(Warmth.HEATED), PolarReactions::substituteAromatic);
    }

    private static Reaction substituteAromatic(Pot pot) {
        Molecule molecule = pot.molecule();
        int[] nucleophile = aromaticNucleophile(molecule);
        if (nucleophile == null) {
            return null;
        }
        int incoming = nucleophile[0];
        int hydrogen = nucleophile[1];
        Molecule drawn = Assemblies.kekulized(molecule);
        for (Site halide : Sites.halides(molecule)) {
            int carbon = halide.atom(0);
            int halogen = halide.atom(1);
            if (!pulledOn(molecule, carbon)) {
                continue;
            }
            int[] beside = ringNeighbours(drawn, carbon);
            if (beside == null) {
                continue;
            }
            List<Arrow> arrows = new ArrayList<>();
            arrows.add(Arrow.fromLonePair(incoming, incoming, carbon));
            arrows.add(Arrow.betweenBonds(carbon, beside[0], beside[0], beside[1]));
            arrows.add(Arrow.toLonePair(carbon, halogen));
            arrows.add(Arrow.betweenBonds(beside[0], beside[1], carbon, beside[0]));
            if (hydrogen >= 0) {
                // A nucleophile that came in neutral is left charged by the attack, so the hydrogen it was
                // carrying goes onto the halogen that left and the two come out as substances.
                arrows.add(Arrow.toLonePair(hydrogen, incoming));
                arrows.add(Arrow.fromLonePair(halogen, halogen, hydrogen));
            }
            Molecule product = ElementaryStep.of("snAr", arrows).apply(drawn);
            return pot.react(Assemblies.aromatized(product), 0, carbon, incoming);
        }
        return null;
    }

    /**
     * The atom a ring that pulls electrons out of itself is attacked by, with a hydrogen of it when there is
     * one to hand over.
     * <p>
     * Three kinds of reagent add a group to such a ring and they are looked for here, sharpest first: the
     * hydroxide of the trade, which is charged and hands over nothing; a nitrogen with a hydrogen on it,
     * which is an amine and gives an arylamine; and an oxygen with a hydrogen on it, which is an alcohol or a
     * phenol and gives an aryl ether. A charged reagent is answered with it and {@code -1} for the hydrogen,
     * since the attack leaves it neutral and there is nothing to finish off; the other two hand one hydrogen
     * over so that the halogen that left comes out an acid.
     *
     * @param molecule molecule to read
     * @return the attacking atom and a hydrogen of it, or the attacking atom and {@code -1} when it is
     *         charged, or {@code null} when the vessel holds none
     */
    private static int[] aromaticNucleophile(Molecule molecule) {
        int hydroxide = hydroxide(molecule);
        if (hydroxide >= 0) {
            return new int[] {hydroxide, -1};
        }
        int[] amine = amineWithHydrogenOf(molecule);
        if (amine != null) {
            return new int[] {amine[0], amine[1]};
        }
        Site hydroxyl = first(Sites.hydroxyls(molecule));
        if (hydroxyl != null) {
            int hydrogen = hydrogenOn(molecule, hydroxyl.atom(0));
            if (hydrogen >= 0) {
                return new int[] {hydroxyl.atom(0), hydrogen};
            }
        }
        return null;
    }

    /** {@code true} when a ring carbon stands beside or across from a group that pulls electrons out of it. */
    private static boolean pulledOn(Molecule molecule, int carbon) {
        List<Integer> ring = null;
        for (List<Integer> cycle : Rings.cycles(molecule)) {
            if (cycle.contains(carbon)) {
                ring = cycle;
                break;
            }
        }
        if (ring == null) {
            return false;
        }
        int size = ring.size();
        int index = ring.indexOf(carbon);
        for (int step : new int[] {1, size - 1, size / 2}) {
            int beside = ring.get((index + step) % size);
            for (int neighbour : molecule.neighbours(beside)) {
                if (neighbour != carbon && !ring.contains(neighbour)
                        && !Reactivity.directsToTheSides(molecule, neighbour)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** The two ring neighbours of an atom of a ring, the one it is held to by a double bond first. */
    private static int[] ringNeighbours(Molecule molecule, int atom) {
        List<Integer> ring = null;
        for (List<Integer> cycle : Rings.cycles(molecule)) {
            if (cycle.contains(atom)) {
                ring = cycle;
                break;
            }
        }
        if (ring == null) {
            return null;
        }
        int size = ring.size();
        int index = ring.indexOf(atom);
        int next = ring.get((index + 1) % size);
        int previous = ring.get((index + size - 1) % size);
        Bond forward = bondBetween(molecule, atom, next);
        return forward != null && forward.order() == 2 ? new int[] {next, previous}
                : new int[] {previous, next};
    }

    /**
     * The carbon between two carbonyls of one molecule joining the carbon of an aldehyde, which is the
     * reaction a handle is put on a molecule with.
     * <p>
     * Six arrows are drawn, and they are the aldol and the losing of water one after the other: the pair of
     * the carbon to hydrogen bond between the two carbonyls makes the bond to the aldehyde, the pair of the
     * aldehyde's double bond onto its oxygen, the hydrogen onto that oxygen - which is the aldol shape - and
     * then the hydrogen off the carbon that was the aldehyde, the pair the other carbon holds closing the
     * double bond between them, and that hydrogen onto the oxygen so that what leaves is water. The double
     * bond that comes out is held between two groups that pull electrons, which is what makes it worth
     * having: everything is added across it afterwards.
     */
    private static ReactionRule knoevenagel() {
        return new ReactionRule("knoevenagel", "condensation", 3,
                EnumSet.of(FunctionalGroup.ALDEHYDE), Conditions.at(Warmth.HEATED),
                PolarReactions::condenseKnoevenagel);
    }

    private static Reaction condenseKnoevenagel(Pot pot) {
        Molecule molecule = pot.molecule();
        Site aldehyde = bestAldehyde(molecule);
        if (aldehyde == null) {
            return null;
        }
        int carbon = aldehyde.atom(0);
        int oxygen = aldehyde.atom(1);
        int[] active = activeMethylene(molecule, carbon);
        if (active == null) {
            return null;
        }
        int giving = active[0];
        int givingHydrogen = active[1];
        int aldehydeHydrogen = hydrogenOn(molecule, carbon);
        if (aldehydeHydrogen < 0 || !mayJoin(pot, giving, carbon)) {
            return null;
        }
        List<Arrow> arrows = List.of(
                Arrow.betweenBonds(giving, givingHydrogen, giving, carbon),
                Arrow.toLonePair(carbon, oxygen),
                Arrow.fromLonePair(oxygen, oxygen, givingHydrogen),
                Arrow.betweenBonds(carbon, aldehydeHydrogen, carbon, giving),
                Arrow.toLonePair(carbon, oxygen),
                Arrow.fromLonePair(oxygen, oxygen, aldehydeHydrogen));
        Molecule product = ElementaryStep.of("knoevenagel", arrows).apply(molecule);
        return pot.react(product, 0, carbon, giving);
    }

    /**
     * The carbon between two groups that pull electrons, with one of the hydrogens it carries.
     *
     * @param molecule molecule to read
     * @param notThis an atom that may not be the one taken, which is the carbon being attacked
     * @return the carbon and its hydrogen, or {@code null} when the vessel holds none
     */
    private static int[] activeMethylene(Molecule molecule, int notThis) {
        for (int atom = 0; atom < molecule.atomCount(); atom++) {
            if (!isElement(molecule, atom, "C") || atom == notThis) {
                continue;
            }
            int pulling = 0;
            for (int neighbour : molecule.neighbours(atom)) {
                if (neighbour != notThis && !Reactivity.directsToTheSides(molecule, neighbour)) {
                    pulling++;
                }
            }
            int hydrogen = hydrogenOn(molecule, atom);
            if (pulling >= 2 && hydrogen >= 0) {
                return new int[] {atom, hydrogen};
            }
        }
        return null;
    }

    /**
     * A carbon beside a nitro group joining the carbon of an aldehyde, which is the Henry reaction.
     * <p>
     * The carbon beside a nitro group is a carbon a pair of electrons can be taken off, so it is drawn as the
     * aldol is: the pair of the carbon to hydrogen bond makes the bond to the aldehyde, the pair of the double
     * bond goes to its oxygen, and the hydrogen follows it there. What comes out is a nitro alcohol, and a
     * molecule of water taken off it afterwards leaves the double bond a nitro group holds.
     */
    private static ReactionRule henry() {
        return new ReactionRule("henry", "condensation", 2,
                EnumSet.of(FunctionalGroup.ALDEHYDE, FunctionalGroup.NITRO), Conditions.at(Warmth.COLD),
                PolarReactions::addHenry);
    }

    private static Reaction addHenry(Pot pot) {
        Molecule molecule = pot.molecule();
        Site aldehyde = bestAldehyde(molecule);
        Site nitro = first(Sites.nitros(molecule));
        if (aldehyde == null || nitro == null) {
            return null;
        }
        int carbon = aldehyde.atom(0);
        int oxygen = aldehyde.atom(1);
        int nitrogen = nitro.atom(0);
        int giving = -1;
        int givingHydrogen = -1;
        for (int neighbour : molecule.neighbours(nitrogen)) {
            if (!isElement(molecule, neighbour, "C")) {
                continue;
            }
            int hydrogen = hydrogenOn(molecule, neighbour);
            if (hydrogen >= 0 && mayJoin(pot, neighbour, carbon)) {
                giving = neighbour;
                givingHydrogen = hydrogen;
            }
        }
        if (giving < 0) {
            return null;
        }
        List<Arrow> arrows = List.of(
                Arrow.betweenBonds(giving, givingHydrogen, giving, carbon),
                Arrow.toLonePair(carbon, oxygen),
                Arrow.fromLonePair(oxygen, oxygen, givingHydrogen));
        Molecule product = ElementaryStep.of("henry", arrows).apply(molecule);
        return pot.react(product, 0, carbon, giving);
    }

    /**
     * An aldehyde and an amine and the carbon beside a carbonyl of a third molecule in one vessel: the three
     * part reaction a great many drugs are started with.
     * <p>
     * Eight arrows are drawn, and they are the condensation and the aldol one after the other: the amine into
     * the aldehyde, the pair of the double bond onto its oxygen, a hydrogen off the nitrogen onto that oxygen,
     * the pair of the carbon to hydrogen bond beside a carbonyl onto the carbon of the aldehyde, the bond to
     * the hydroxyl breaking with the pair, the second hydrogen off the nitrogen onto that oxygen - so that it
     * leaves as water and the nitrogen leaves holding what it came in with - and the carbonyl the pair came
     * from closing again. What comes out holds an amine at one end and a carbonyl at the other.
     */
    private static ReactionRule mannich() {
        return new ReactionRule("mannich", "condensation", 4,
                EnumSet.of(FunctionalGroup.ALDEHYDE, FunctionalGroup.AMINE),
                Conditions.at(Warmth.HEATED), PolarReactions::reactMannich);
    }

    private static Reaction reactMannich(Pot pot) {
        Molecule molecule = pot.molecule();
        Site aldehyde = bestAldehyde(molecule);
        int[] amine = amineWithHydrogenOf(molecule);
        if (aldehyde == null || amine == null) {
            return null;
        }
        int carbon = aldehyde.atom(0);
        int oxygen = aldehyde.atom(1);
        int nitrogen = amine[0];
        int first = amine[1];
        int giving = -1;
        int givingHydrogen = -1;
        for (Site site : Sites.carbonyls(molecule)) {
            int alpha = alphaCarbonOf(molecule, site.atom(0));
            if (alpha < 0 || alpha == carbon) {
                continue;
            }
            int hydrogen = hydrogenOn(molecule, alpha);
            if (hydrogen >= 0 && alpha != first && mayJoin(pot, alpha, carbon)) {
                giving = alpha;
                givingHydrogen = hydrogen;
            }
        }
        if (giving < 0) {
            return null;
        }
        List<Arrow> arrows = List.of(
                Arrow.fromLonePair(nitrogen, nitrogen, carbon),
                Arrow.toLonePair(carbon, oxygen),
                Arrow.toLonePair(first, nitrogen),
                Arrow.fromLonePair(oxygen, oxygen, first),
                Arrow.betweenBonds(giving, givingHydrogen, giving, carbon),
                Arrow.toLonePair(carbon, oxygen),
                Arrow.fromLonePair(oxygen, oxygen, givingHydrogen));
        Molecule product = ElementaryStep.of("mannich", arrows).apply(molecule);
        return pot.react(product, 0, carbon, nitrogen, giving);
    }

    /**
     * The two ends of a chain of three double bonds joining, which is what an electrocyclic closing is.
     * <p>
     * One arrow is drawn twice over: the pair of the double bond at one end of the chain makes the bond to the
     * far end, and the pair of the middle double bond takes its place, one bond along. What comes out is a
     * ring of six with two double bonds in it, and - this being the one shape of the table that no pair of
     * groups can be said to react - the rule asks the ring of the molecule instead: a chain only closes if
     * the two ends of it stand far enough apart, see {@link #mayJoin}.
     */
    private static ReactionRule electrocyclicClosing() {
        return new ReactionRule("electrocyclicClosing", "pericyclic", 2,
                EnumSet.of(FunctionalGroup.ALKENE), Conditions.at(Warmth.HEATED),
                PolarReactions::closeElectrocyclic);
    }

    private static Reaction closeElectrocyclic(Pot pot) {
        Molecule molecule = pot.molecule();
        int[] chain = trieneOf(molecule);
        if (chain == null) {
            return null;
        }
        int first = chain[0];
        int last = chain[5];
        if (bondBetween(molecule, first, last) != null || !mayJoin(pot, first, last)) {
            return null;
        }
        List<Arrow> arrows = List.of(
                Arrow.betweenBonds(chain[0], chain[1], chain[0], chain[5]),
                Arrow.betweenBonds(chain[2], chain[3], chain[1], chain[2]));
        Molecule product = ElementaryStep.of("electrocyclicClosing", arrows).apply(molecule);
        return pot.react(product, 0, first, last);
    }

    /**
     * The six carbons of a chain of three double bonds, in the order they are joined, or {@code null}.
     *
     * @param molecule molecule to read
     * @return the six atoms, the outermost two first and last
     */
    private static int[] trieneOf(Molecule molecule) {
        List<Site> alkenes = Sites.alkenes(molecule);
        for (int first = 0; first < alkenes.size(); first++) {
            for (int second = 0; second < alkenes.size(); second++) {
                if (second == first) {
                    continue;
                }
                int[] link = singleBondLink(molecule, alkenes.get(first), alkenes.get(second));
                if (link == null) {
                    continue;
                }
                for (int third = 0; third < alkenes.size(); third++) {
                    if (third == first || third == second) {
                        continue;
                    }
                    int[] far = singleBondLink(molecule, alkenes.get(second), alkenes.get(third));
                    if (far == null || far[0] != link[1]) {
                        continue;
                    }
                    return new int[] {link[2], link[0], link[1], far[0], far[1], far[2]};
                }
            }
        }
        return null;
    }

    /**
     * The two atoms by which two double bonds are joined, or {@code null} when they are not.
     *
     * @param molecule molecule to read
     * @param one one double bond
     * @param other the other
     * @return the atom of the first, the atom of the second, and the far end of the first
     */
    private static int[] singleBondLink(Molecule molecule, Site one, Site other) {
        for (int near : one.atoms()) {
            for (int far : other.atoms()) {
                Bond link = bondBetween(molecule, near, far);
                if (link == null || link.order() != 1) {
                    continue;
                }
                int outer = one.atom(0) == near ? one.atom(1) : one.atom(0);
                int beyond = other.atom(0) == far ? other.atom(1) : other.atom(0);
                return new int[] {near, far, outer, beyond};
            }
        }
        return null;
    }

    /**
     * Two double bonds closing into a ring of four under a light, which is the one reaction of the table a
     * light is needed for.
     * <p>
     * Two arrows are drawn: the pair of one double bond makes the bond to the far carbon of the other, and the
     * pair of that other makes the bond back to the far carbon of the first. A light is not a reagent and is
     * asked for as a condition, and it is demanded rather than wondered at, so the same vessel in the dark
     * answers nothing at all - which is what the test of it says, both ways round.
     */
    private static ReactionRule photocycloaddition() {
        return new ReactionRule("photocycloaddition", "pericyclic", 2,
                EnumSet.of(FunctionalGroup.ALKENE),
                Conditions.builder().warmth(Warmth.AMBIENT).lighted(true).build(),
                PolarReactions::closePhotocycloaddition);
    }

    private static Reaction closePhotocycloaddition(Pot pot) {
        Molecule molecule = pot.molecule();
        List<Site> alkenes = Sites.alkenes(molecule);
        for (int index = 0; index < alkenes.size(); index++) {
            for (int other = index + 1; other < alkenes.size(); other++) {
                Site one = alkenes.get(index);
                Site two = alkenes.get(other);
                int first = one.atom(0);
                int second = one.atom(1);
                int third = two.atom(0);
                int fourth = two.atom(1);
                if (pot.sharesAMolecule(first, third) || bondBetween(molecule, first, third) != null) {
                    continue;
                }
                List<Arrow> arrows = List.of(
                        Arrow.betweenBonds(third, fourth, third, second),
                        Arrow.betweenBonds(first, second, first, fourth));
                Molecule product = ElementaryStep.of("photocycloaddition", arrows).apply(molecule);
                return pot.react(product, 0, first, third);
            }
        }
        return null;
    }

    /**
     * {@code true} when two places a rule would join may be joined at all.
     * <p>
     * <b>Two places of two molecules are two molecules standing beside one another, and two places of one
     * molecule close a ring.</b> The first is the reaction the trade runs by the thousand and the second is
     * the one a ring is made by - the lactone an acid closes with its own alcohol, the ketone a ring is
     * closed on itself into, the imidazole a diamine is closed into - and both are wanted. What is not
     * wanted is a ring of three: a bond joining two places only two atoms apart is a triangle nobody's
     * geometry allows, so those two are refused and a ring of four or more is not.
     *
     * @param pot the vessel
     * @param first index of one atom the rule would join
     * @param second index of the other
     * @return {@code true} when the two may be joined
     */
    private static boolean mayJoin(Pot pot, int first, int second) {
        return !pot.sharesAMolecule(first, second)
                || Rings.distance(pot.molecule(), first, second) >= 3;
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
    /**
     * The two hydrogens of a molecule of water, or {@code null} when it carries fewer than two.
     * <p>
     * A water is asked for both of its hydrogens by the two rules that take a molecule of it apart, and the
     * two have to be two different atoms: a rule that asked for the hydrogen of a water twice would draw the
     * second arrow on a bond the first arrow had already broken.
     *
     * @param molecule molecule to read
     * @param oxygen the oxygen of the water
     * @return the two hydrogens
     */
    private static int[] waterHydrogens(Molecule molecule, int oxygen) {
        List<Integer> hydrogens = new ArrayList<>();
        for (int neighbour : molecule.neighbours(oxygen)) {
            if (isElement(molecule, neighbour, "H")) {
                hydrogens.add(neighbour);
            }
        }
        return hydrogens.size() < 2 ? null : new int[] {hydrogens.get(0), hydrogens.get(1)};
    }

    /**
     * The carbon of a cyanide ion, or {@code -1} when the vessel holds none.
     * <p>
     * A cyanide is not a nitrile of the trade and is not found as one, see {@link Sites#nitriles}: it is a
     * carbon and a nitrogen with nothing else on the carbon and a charge of its own, so it is looked for as
     * what it is.
     *
     * @param molecule molecule to read
     * @return the carbon of the ion, or {@code -1}
     */
    private static int cyanide(Molecule molecule) {
        for (int atom = 0; atom < molecule.atomCount(); atom++) {
            if (!isElement(molecule, atom, "C") || molecule.atom(atom).charge() != -1) {
                continue;
            }
            for (int neighbour : molecule.neighbours(atom)) {
                Bond bond = bondBetween(molecule, atom, neighbour);
                if (bond != null && bond.order() == 3 && isElement(molecule, neighbour, "N")) {
                    return atom;
                }
            }
        }
        return -1;
    }

    /**
     * An amine taking the place of the halogen of an alkyl halide, the reaction almost every modern medicine
     * is put together with.
     * <p>
     * The two arrows of a substitution are drawn and two more finish it off: the nitrogen into the carbon and
     * the halogen out with the pair, which is the whole of the step, and then the hydrogen the nitrogen was
     * carrying goes onto the halogen - so that what leaves the vessel is hydrogen chloride and a free amine,
     * and not the bromide of an ammonium that nobody asked for. The carbon is turned over, as every carbon a
     * nucleophile comes in behind is.
     * <p>
     * <b>The carbon has to be a saturated one and the two have to stand in two molecules.</b> An aryl halide
     * is no alkyl halide - its carbon is held flat by the ring - and a nitrogen held to the carbon already is
     * a bond that stands there; what this rule draws is two molecules meeting and never a chain folding back
     * on itself.
     */
    private static ReactionRule amineAlkylation() {
        return new ReactionRule("amineAlkylation", "substitution", 3,
                EnumSet.of(FunctionalGroup.AMINE, FunctionalGroup.HALIDE),
                Conditions.at(Warmth.HEATED), PolarReactions::alkylateAmine);
    }

    private static Reaction alkylateAmine(Pot pot) {
        Molecule molecule = pot.molecule();
        int[] amine = amineWithHydrogenOf(molecule);
        if (amine == null) {
            return null;
        }
        int nitrogen = amine[0];
        int hydrogen = amine[1];
        for (Site halide : Sites.halides(molecule)) {
            int carbon = halide.atom(0);
            int halogen = halide.atom(1);
            if (carriesDoubleBond(molecule, carbon) || pot.sharesAMolecule(nitrogen, carbon)
                    || bondBetween(molecule, nitrogen, carbon) != null) {
                continue;
            }
            Molecule product = ElementaryStep
                    .substitution("amineAlkylation", nitrogen, carbon, halogen,
                            List.of(Arrow.toLonePair(hydrogen, nitrogen),
                                    Arrow.fromLonePair(halogen, halogen, hydrogen)))
                    .apply(molecule);
            return pot.react(product, 0, carbon, nitrogen);
        }
        return null;
    }

    /**
     * An acid chloride and an alcohol giving an ester and hydrogen chloride, which is how a polyester is
     * made when the acid itself is too slow to be worth waiting for.
     * <p>
     * Six arrows are drawn: the alcohol's oxygen into the carbonyl, the pair of the double bond to the
     * carbonyl oxygen, the halogen off with the pair, the pair back into the double bond - so that the carbon
     * keeps its oxygen and takes the alcohol in its place - then the alcohol's hydrogen off and onto the
     * halogen, so that the halogen leaves as an acid and never as an ion.
     */
    private static ReactionRule acylationOfAlcohol() {
        return new ReactionRule("acylationOfAlcohol", "acyl substitution", 3,
                EnumSet.of(FunctionalGroup.ACYL_HALIDE, FunctionalGroup.HYDROXYL),
                Conditions.at(Warmth.HEATED), PolarReactions::acylateAlcohol);
    }

    private static Reaction acylateAlcohol(Pot pot) {
        Molecule molecule = pot.molecule();
        Site acyl = first(Sites.acylHalides(molecule));
        if (acyl == null) {
            return null;
        }
        int carbon = acyl.atom(0);
        int carbonylOxygen = acyl.atom(1);
        int halogen = acyl.atom(2);
        for (Site hydroxyl : Sites.hydroxyls(molecule)) {
            int alcoholOxygen = hydroxyl.atom(0);
            int alcoholHydrogen = hydrogenOn(molecule, alcoholOxygen);
            if (alcoholHydrogen < 0 || alcoholOxygen == carbonylOxygen
                    || pot.sharesAMolecule(alcoholOxygen, carbon)
                    || bondBetween(molecule, carbon, alcoholOxygen) != null) {
                continue;
            }
            List<Arrow> arrows = List.of(
                    Arrow.fromLonePair(alcoholOxygen, alcoholOxygen, carbon),
                    Arrow.toLonePair(carbon, carbonylOxygen),
                    Arrow.toLonePair(carbon, halogen),
                    Arrow.fromLonePair(carbonylOxygen, carbonylOxygen, carbon),
                    Arrow.toLonePair(alcoholHydrogen, alcoholOxygen),
                    Arrow.fromLonePair(halogen, halogen, alcoholHydrogen));
            Molecule product = ElementaryStep.of("acylationOfAlcohol", arrows).apply(molecule);
            return pot.react(product, 0, carbon, alcoholOxygen);
        }
        return null;
    }

    /**
     * An acid chloride and an amine giving an amide and hydrogen chloride, the industrial way a fibre is
     * spun: two acid chlorides and a diamine meeting over and over.
     * <p>
     * The six arrows are the ones of the making of an ester with a nitrogen in the place of the alcohol's
     * oxygen, and the hydrogen the nitrogen was carrying goes onto the halogen so that the acid leaves whole.
     */
    private static ReactionRule acylationOfAmine() {
        return new ReactionRule("acylationOfAmine", "acyl substitution", 3,
                EnumSet.of(FunctionalGroup.ACYL_HALIDE, FunctionalGroup.AMINE),
                Conditions.at(Warmth.HEATED), PolarReactions::acylateAmine);
    }

    private static Reaction acylateAmine(Pot pot) {
        Molecule molecule = pot.molecule();
        Site acyl = first(Sites.acylHalides(molecule));
        int[] amine = amineWithHydrogenOf(molecule);
        if (acyl == null || amine == null) {
            return null;
        }
        int carbon = acyl.atom(0);
        int carbonylOxygen = acyl.atom(1);
        int halogen = acyl.atom(2);
        int nitrogen = amine[0];
        int hydrogen = amine[1];
        if (pot.sharesAMolecule(nitrogen, carbon) || bondBetween(molecule, carbon, nitrogen) != null) {
            return null;
        }
        List<Arrow> arrows = List.of(
                Arrow.fromLonePair(nitrogen, nitrogen, carbon),
                Arrow.toLonePair(carbon, carbonylOxygen),
                Arrow.toLonePair(carbon, halogen),
                Arrow.fromLonePair(carbonylOxygen, carbonylOxygen, carbon),
                Arrow.toLonePair(hydrogen, nitrogen),
                Arrow.fromLonePair(halogen, halogen, hydrogen));
        Molecule product = ElementaryStep.of("acylationOfAmine", arrows).apply(molecule);
        return pot.react(product, 0, carbon, nitrogen);
    }

    /**
     * An aldehyde and water giving an acid and hydrogen, the oxidation the industry runs to turn a
     * petrochemical into the acid a fibre is made of.
     * <p>
     * Three arrows are drawn, and they are the ones of the losing of hydrogen the other way round: the pair
     * of the aldehyde's carbon to hydrogen bond makes a bond to a hydrogen of the water, the water's oxygen
     * takes that bond's pair as a lone pair, and the lone pair comes back as the bond to the carbon - so that
     * the aldehyde takes the water's oxygen for its own acid and the two hydrogens leave together as a
     * molecule of hydrogen.
     */
    private static ReactionRule aldehydeOxidation() {
        return new ReactionRule("aldehydeOxidation", "oxidation", 1,
                EnumSet.of(FunctionalGroup.ALDEHYDE), Conditions.at(Warmth.HEATED),
                PolarReactions::oxidiseAldehyde);
    }

    private static Reaction oxidiseAldehyde(Pot pot) {
        Molecule molecule = pot.molecule();
        Site aldehyde = bestAldehyde(molecule);
        int[] water = water(molecule);
        if (aldehyde == null || water == null) {
            return null;
        }
        int carbon = aldehyde.atom(0);
        int aldehydeHydrogen = hydrogenOn(molecule, carbon);
        int waterHydrogen = water[0];
        int waterOxygen = water[1];
        if (aldehydeHydrogen < 0 || pot.sharesAMolecule(carbon, waterOxygen)) {
            return null;
        }
        List<Arrow> arrows = List.of(
                Arrow.betweenBonds(carbon, aldehydeHydrogen, aldehydeHydrogen, waterHydrogen),
                Arrow.toLonePair(waterHydrogen, waterOxygen),
                Arrow.fromLonePair(waterOxygen, waterOxygen, carbon));
        Molecule product = ElementaryStep.of("aldehydeOxidation", arrows).apply(molecule);
        return pot.react(product, 0, carbon, waterOxygen);
    }

    /**
     * A carbon to carbon bond of an alkane losing a hydrogen off each of its two carbons and coming out a
     * double bond: the dehydrogenation a works runs to turn butane into the diene of a rubber and
     * ethylbenzene into styrene.
     * <p>
     * Two arrows are drawn, and the pair of electrons the new double bond is made of is no lone pair but the
     * very bond to a hydrogen that is leaving: the pair of the first carbon to hydrogen bond makes a bond
     * between the two hydrogens, and the pair of the second carbon to hydrogen bond closes the double bond -
     * so that the two hydrogens leave as a molecule of hydrogen and nothing carries a charge at any moment.
     * <p>
     * <b>Both carbons have to be plain ones.</b> A carbon that already carries a double bond has no room for
     * the one that comes, and a carbon held to an oxygen or a nitrogen is not the pair of a dehydrogenation
     * at all: an alcohol in that place loses its hydrogen into a carbonyl instead, which is a rule of its
     * own.
     */
    private static ReactionRule alkaneDehydrogenation() {
        return new ReactionRule("alkaneDehydrogenation", "oxidation", 1,
                EnumSet.of(FunctionalGroup.ALKYL), Conditions.at(Warmth.HEATED, Set.of(NICKEL)),
                PolarReactions::dehydrogenateAlkane);
    }

    private static Reaction dehydrogenateAlkane(Pot pot) {
        Molecule molecule = pot.molecule();
        for (Bond bond : molecule.bonds()) {
            if (bond.order() != 1 || bond.isAromatic()) {
                continue;
            }
            int first = bond.first();
            int second = bond.second();
            if (!isElement(molecule, first, "C") || !isElement(molecule, second, "C")
                    || carriesDoubleBond(molecule, first) || carriesDoubleBond(molecule, second)
                    || !plainCarbon(molecule, first) || !plainCarbon(molecule, second)) {
                continue;
            }
            int firstHydrogen = hydrogenOn(molecule, first);
            int secondHydrogen = hydrogenOn(molecule, second);
            if (firstHydrogen < 0 || secondHydrogen < 0) {
                continue;
            }
            List<Arrow> arrows = List.of(
                    Arrow.betweenBonds(first, firstHydrogen, firstHydrogen, secondHydrogen),
                    Arrow.betweenBonds(second, secondHydrogen, second, first));
            Molecule product = ElementaryStep.of("alkaneDehydrogenation", arrows).apply(molecule);
            return pot.react(product, 0, first, second);
        }
        return null;
    }

    /** {@code true} when a carbon is held to nothing but carbons and hydrogens. */
    private static boolean plainCarbon(Molecule molecule, int atom) {
        for (int neighbour : molecule.neighbours(atom)) {
            String element = molecule.atom(neighbour).element();
            if (!element.equals("C") && !element.equals("H")) {
                return false;
            }
        }
        return true;
    }

    /**
     * The hydrogen and the cyanide group of hydrogen cyanide added across the triple bond of an alkyne, the
     * reaction that turns acetylene into the acrylonitrile a plastic is made of.
     * <p>
     * Two arrows are drawn, exactly as the adding of an acid across a double bond is, with the hydrogen of
     * the hydrogen cyanide where the hydrogen of the acid stood: the pair of the hydrogen to cyanide bond
     * makes the bond to the carbon that carries more hydrogens of the two, and the pair of the triple bond
     * makes the bond to the cyanide - so that the hydrogen takes the end the rule of Markovnikov sends it to
     * and the carbon that is left takes the group, which is the very reaction the trade runs over a copper
     * salt.
     */
    private static ReactionRule hydrocyanation() {
        return new ReactionRule("hydrocyanation", "addition", 2,
                EnumSet.of(FunctionalGroup.ALKYNE), Conditions.at(Warmth.HEATED),
                PolarReactions::hydrocyanate);
    }

    private static Reaction hydrocyanate(Pot pot) {
        Site alkyne = first(Sites.alkynes(pot.molecule()));
        int[] cyanohydrin = hydrogenCyanide(pot.molecule());
        if (alkyne == null || cyanohydrin == null) {
            return null;
        }
        int first = plainerCarbonOf(pot.molecule(), alkyne);
        int second = first == alkyne.atom(0) ? alkyne.atom(1) : alkyne.atom(0);
        Molecule product = ElementaryStep
                .across("hydrocyanation", first, second, cyanohydrin[0], cyanohydrin[1])
                .apply(pot.molecule());
        return pot.react(product, 0, first, cyanohydrin[0]);
    }

    /** The hydrogen and the carbon of a molecule of hydrogen cyanide, or {@code null} when there is none. */
    private static int[] hydrogenCyanide(Molecule molecule) {
        for (int atom = 0; atom < molecule.atomCount(); atom++) {
            if (!isElement(molecule, atom, "C") || molecule.atom(atom).charge() != 0) {
                continue;
            }
            int hydrogen = -1;
            boolean nitrile = false;
            for (int neighbour : molecule.neighbours(atom)) {
                if (isElement(molecule, neighbour, "H")) {
                    hydrogen = neighbour;
                } else {
                    Bond bond = bondBetween(molecule, atom, neighbour);
                    if (bond != null && bond.order() == 3 && isElement(molecule, neighbour, "N")) {
                        nitrile = true;
                    }
                }
            }
            if (nitrile && hydrogen >= 0) {
                return new int[] {hydrogen, atom};
            }
        }
        return null;
    }

    /**
     * An alkene with a hydrogen on the carbon beside its double bond and a second alkene, giving one longer
     * alkene with the double bond of the first shifted along it: the ene reaction.
     * <p>
     * Two arrows are drawn, because the pair of electrons of a six-membered ring that the reaction is
     * sometimes drawn with would have to begin as a lone pair and a double bond has none to spare: the pair
     * of the allylic carbon to hydrogen bond makes the bond to the far carbon of the other alkene, and the
     * pair of that alkene's own double bond makes the bond back to the allylic carbon. So the hydrogen goes
     * to the far carbon, the near one joins the allylic carbon, and the double bond of the alkene that gave
     * the hydrogen never moves.
     * <p>
     * <b>The allylic carbon is a saturated one beside a double bond</b> - it must carry the hydrogen the
     * reaction takes and must not itself stand in a double bond - and the two halves meet across a bond that
     * is not already there, which is what a ring the pair would close is refused by.
     */
    private static ReactionRule eneReaction() {
        return new ReactionRule("eneReaction", "pericyclic", 2,
                EnumSet.of(FunctionalGroup.ALKENE), Conditions.at(Warmth.HEATED),
                PolarReactions::reactEne);
    }

    private static Reaction reactEne(Pot pot) {
        Molecule molecule = pot.molecule();
        List<Site> alkenes = Sites.alkenes(molecule);
        for (Site alkene : alkenes) {
            for (int near : alkene.atoms()) {
                int far = near == alkene.atom(0) ? alkene.atom(1) : alkene.atom(0);
                for (int allylic : molecule.neighbours(near)) {
                    if (allylic == far) {
                        continue;
                    }
                    int hydrogen = hydrogenOn(molecule, allylic);
                    if (hydrogen < 0 || carriesDoubleBond(molecule, allylic)) {
                        continue;
                    }
                    for (Site enophile : alkenes) {
                        if (enophile == alkene) {
                            continue;
                        }
                        for (int theirNear : enophile.atoms()) {
                            int theirFar = theirNear == enophile.atom(0) ? enophile.atom(1)
                                    : enophile.atom(0);
                            if (!mayJoin(pot, allylic, theirNear)
                                    || molecule.neighbours(theirFar).contains(allylic)) {
                                continue;
                            }
                            List<Arrow> arrows = List.of(
                                    Arrow.betweenBonds(allylic, hydrogen, hydrogen, theirFar),
                                    Arrow.betweenBonds(theirNear, theirFar, theirNear, allylic));
                            Molecule product = ElementaryStep.of("eneReaction", arrows).apply(molecule);
                            return pot.react(product, 0, allylic, theirNear);
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * An acid and an ortho diamine closing into a benzimidazole with two molecules of water, the reaction the
     * repeating unit of a fire-proof fibre and half the anthelmintics there are share.
     * <p>
     * Two condensations are drawn one after the other. The first is the making of an amide, six arrows: the
     * first nitrogen into the acid's carbonyl, the pair of the double bond to its oxygen, one hydrogen off
     * that nitrogen onto the acid's own hydroxyl, the hydroxyl off with the pair and the pair back into the
     * double bond - so the ring keeps an amide at that nitrogen and a molecule of water leaves. The second is
     * the same again turned on the amide's own carbonyl: the second nitrogen into it, the pair of the double
     * bond to the oxygen, and now <em>both</em> the second nitrogen's hydrogens onto that oxygen - so that
     * the oxygen leaves as the second water, the carbon is left a bare one, and the lone pair of the nitrogen
     * closes the ring as the double bond between them.
     * <p>
     * <b>The two nitrogens have to be primary and to hang on carbons that stand side by side on a ring.</b>
     * That is what an ortho diamine is: only a nitrogen with two hydrogens of its own can give one to the
     * first water and one to the second, and only two carbons one bond apart close the five-membered ring the
     * two nitrogens and the acid's carbon make.
     */
    private static ReactionRule imidazoleFormation() {
        return new ReactionRule("imidazoleFormation", "condensation", 3,
                EnumSet.of(FunctionalGroup.CARBOXYLIC_ACID, FunctionalGroup.AMINE,
                        FunctionalGroup.AROMATIC_RING),
                Conditions.at(Warmth.HEATED), PolarReactions::closeImidazole);
    }

    private static Reaction closeImidazole(Pot pot) {
        Molecule molecule = pot.molecule();
        Site acid = first(Sites.acids(molecule));
        int[] diamines = orthoDiamines(molecule);
        if (acid == null || diamines == null) {
            return null;
        }
        int carbon = acid.atom(0);
        int carbonylOxygen = acid.atom(1);
        int acidOxygen = acid.atom(2);
        int acidHydrogen = hydrogenOn(molecule, acidOxygen);
        int firstNitrogen = diamines[0];
        int firstHydrogen = diamines[1];
        int secondNitrogen = diamines[2];
        int secondHydrogen = diamines[3];
        int secondOther = diamines[4];
        if (acidHydrogen < 0 || pot.sharesAMolecule(carbon, firstNitrogen)
                || bondBetween(molecule, carbon, firstNitrogen) != null
                || bondBetween(molecule, carbon, secondNitrogen) != null) {
            return null;
        }
        List<Arrow> arrows = List.of(
                Arrow.fromLonePair(firstNitrogen, firstNitrogen, carbon),
                Arrow.toLonePair(carbon, carbonylOxygen),
                Arrow.toLonePair(firstHydrogen, firstNitrogen),
                Arrow.fromLonePair(acidOxygen, acidOxygen, firstHydrogen),
                Arrow.toLonePair(carbon, acidOxygen),
                Arrow.fromLonePair(carbonylOxygen, carbonylOxygen, carbon),
                Arrow.fromLonePair(secondNitrogen, secondNitrogen, carbon),
                Arrow.toLonePair(carbon, carbonylOxygen),
                Arrow.toLonePair(secondHydrogen, secondNitrogen),
                Arrow.fromLonePair(carbonylOxygen, carbonylOxygen, secondHydrogen),
                Arrow.toLonePair(secondOther, secondNitrogen),
                Arrow.fromLonePair(carbonylOxygen, carbonylOxygen, secondOther),
                Arrow.toLonePair(carbon, carbonylOxygen),
                Arrow.fromLonePair(secondNitrogen, secondNitrogen, carbon));
        Molecule product = ElementaryStep.of("imidazoleFormation", arrows).apply(molecule);
        return pot.react(product, 0, carbon, firstNitrogen, secondNitrogen);
    }

    /**
     * Two nitrogens of an aromatic ring, each of them primary and hanging on carbons that stand side by side,
     * the ortho diamine a benzimidazole is closed out of.
     *
     * @param molecule molecule to read
     * @return the first nitrogen and one of its hydrogens, then the second nitrogen, one of its hydrogens and
     *         the other, or {@code null} when the vessel holds no such pair
     */
    private static int[] orthoDiamines(Molecule molecule) {
        for (Site ring : Sites.aromaticRings(molecule)) {
            List<Integer> atoms = ring.atoms();
            for (int index = 0; index < atoms.size(); index++) {
                int here = atoms.get(index);
                int next = atoms.get((index + 1) % atoms.size());
                int[] first = primaryAmineOn(molecule, here);
                int[] second = primaryAmineOn(molecule, next);
                if (first != null && second != null) {
                    return new int[] {first[0], first[1], second[0], second[1], second[2]};
                }
            }
        }
        return null;
    }

    /** The nitrogen of a primary amine hanging on an atom, with its two hydrogens, or {@code null}. */
    private static int[] primaryAmineOn(Molecule molecule, int atom) {
        for (int neighbour : molecule.neighbours(atom)) {
            if (!isElement(molecule, neighbour, "N") || molecule.atom(neighbour).charge() != 0) {
                continue;
            }
            List<Integer> hydrogens = new ArrayList<>();
            for (int other : molecule.neighbours(neighbour)) {
                if (isElement(molecule, other, "H")) {
                    hydrogens.add(other);
                }
            }
            if (hydrogens.size() == 2) {
                return new int[] {neighbour, hydrogens.get(0), hydrogens.get(1)};
            }
        }
        return null;
    }

    /**
     * The nitro group of a ring taken all the way down to an amine with three molecules of hydrogen, the
     * reaction that turns a ring the nitration gave a nitro group into the aniline half the dyes, the fibres
     * and the medicines of the trade are built on.
     * <p>
     * Three steps are drawn one after the other and they need three molecules of hydrogen between them. The
     * first is the loss of the anionic oxygen of the group: the bond to it breaks with the pair going onto
     * the nitrogen, and the oxygen takes a molecule of hydrogen and leaves as water - which is the nitro
     * group come down to the nitroso one. The second is hydrogen across the nitroso double bond, exactly as
     * a molecule of hydrogen goes across the double bond of an imine: one hydrogen onto the nitrogen and one
     * onto the oxygen, which leaves a hydroxylamine. The third takes the hydroxylamine apart: the bond to
     * the oxygen becomes the second bond to a hydrogen of the third molecule of hydrogen - so the nitrogen
     * comes out with two hydrogens and the oxygen leaves as the second water.
     * <p>
     * <b>Three molecules of hydrogen are spent and two waters leave</b>, which is the balance a works runs:
     * the group loses both its oxygens and the nitrogen is left the plain amine of an aniline.
     */
    private static ReactionRule nitroReduction() {
        return new ReactionRule("nitroReduction", "reduction", 3, EnumSet.of(FunctionalGroup.NITRO),
                Conditions.at(Warmth.AMBIENT, Set.of(NICKEL)), PolarReactions::reduceNitro);
    }

    private static Reaction reduceNitro(Pot pot) {
        Molecule molecule = pot.molecule();
        int[] nitro = nitroGroup(molecule);
        List<int[]> hydrogens = hydrogenMolecules(molecule);
        if (nitro == null || hydrogens.size() < 3) {
            return null;
        }
        int nitrogen = nitro[0];
        int carbonyl = nitro[1];
        int anionic = nitro[2];
        int[] first = hydrogens.get(0);
        int[] second = hydrogens.get(1);
        int[] third = hydrogens.get(2);
        List<Arrow> arrows = List.of(
                Arrow.toLonePair(anionic, nitrogen),
                Arrow.betweenBonds(first[0], first[1], anionic, first[1]),
                Arrow.fromLonePair(anionic, anionic, first[0]),
                Arrow.betweenBonds(second[0], second[1], second[0], carbonyl),
                Arrow.betweenBonds(carbonyl, nitrogen, nitrogen, second[1]),
                Arrow.betweenBonds(nitrogen, carbonyl, nitrogen, third[0]),
                Arrow.betweenBonds(third[0], third[1], carbonyl, third[1]));
        Molecule product = ElementaryStep.of("nitroReduction", arrows).apply(molecule);
        return pot.react(product, 0, nitrogen, first[0], first[1], second[0], second[1], third[0],
                third[1]);
    }

    /**
     * The nitro group of a molecule as the two oxygens and the nitrogen of it, the double one first.
     *
     * @param molecule molecule to read
     * @return the nitrogen, the oxygen of its double bond and the charged oxygen that keeps the single one,
     *         or {@code null} when the vessel holds no such group
     */
    private static int[] nitroGroup(Molecule molecule) {
        for (Site nitro : Sites.nitros(molecule)) {
            int nitrogen = nitro.atom(0);
            int doubled = -1;
            int single = -1;
            for (int index = 1; index < nitro.atoms().size(); index++) {
                int oxygen = nitro.atom(index);
                Bond bond = bondBetween(molecule, nitrogen, oxygen);
                if (bond != null && bond.order() == 2) {
                    doubled = oxygen;
                } else if (molecule.atom(oxygen).charge() == -1) {
                    single = oxygen;
                }
            }
            if (doubled >= 0 && single >= 0) {
                return new int[] {nitrogen, doubled, single};
            }
        }
        return null;
    }

    /** Every molecule of hydrogen of a molecule, as the two atoms of each. */
    private static List<int[]> hydrogenMolecules(Molecule molecule) {
        List<int[]> found = new ArrayList<>();
        for (Bond bond : molecule.bonds()) {
            if (isElement(molecule, bond.first(), "H") && isElement(molecule, bond.second(), "H")) {
                found.add(new int[] {bond.first(), bond.second()});
            }
        }
        return found;
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
