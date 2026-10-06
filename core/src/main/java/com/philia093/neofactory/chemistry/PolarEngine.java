package com.philia093.neofactory.chemistry;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Finds the reactions of the organic side by the groups a rule asks for, and never by a table of substances.
 * <p>
 * The engine of the age that came before this one matched a molecule against a written pattern, so a
 * reaction had to be written down once for every shape it could take. This one is handed a table of rules -
 * each of them the groups it needs and what it does - and asks every rule of what stands in the vessel: the
 * groups that are really there are found once, the rules that need a group that is not there are passed
 * over, and the ones that fit are carried out and their answers checked against the rule of a pot, see
 * {@link Conservation}.
 * <p>
 * <b>What a rule answers with is a reaction of substances and not of a molecule.</b> A vessel holds
 * substances, the rule carries its steps out on the whole of them laid out as one molecule, and the pieces
 * fall out of the answer as the substances they turned out to be, see {@link Pot#react}. A rule that
 * answered with a molecule would be an answer no machine could be fed.
 * <p>
 * <b>The table is the priority.</b> The rules are asked in the order they are written and the answers come
 * back in the same order, so the sharper rule of a pot is written above the looser one and the first answer
 * is the one a machine would run.
 * <p>
 * <b>What a rule asks for is a group and a condition, and the two are asked of different things.</b> The
 * groups are asked of the molecules the vessel holds, which are laid out and read once for the whole table;
 * the condition - the heat, the medium, the catalysts - is asked of the vessel itself, which knows how it is
 * being run, see {@link Conditions}. A rule whose groups are all there still answers nothing if the vessel
 * is run the wrong way for it, which is what keeps an alcohol from losing its water on a bench that is only
 * standing there.
 */
public final class PolarEngine implements ReactionEngine {

    private final List<ReactionRule> rules;

    /**
     * Creates an engine over a table of rules.
     *
     * @param rules the rules, the sharper ones first
     */
    public PolarEngine(List<ReactionRule> rules) {
        this.rules = List.copyOf(Objects.requireNonNull(rules, "rules"));
    }

    @Override
    public List<Reaction> infer(System system) {
        Objects.requireNonNull(system, "system");
        List<Chemical> substances = new ArrayList<>(system.content().components().keySet());
        if (substances.isEmpty() || rules.isEmpty()) {
            return List.of();
        }
        Pot pot = lay(system.content());
        Set<FunctionalGroup> present = presence(pot.molecule());
        List<Chosen> found = new ArrayList<>();
        for (ReactionRule rule : rules) {
            if (!present.containsAll(rule.needs()) || !rule.conditions().within(system.conditions())) {
                continue;
            }
            Reaction reaction = rule.write(pot);
            if (reaction != null && Conservation.balanced(reaction)) {
                found.add(new Chosen(rule, reaction));
            }
        }
        // The more particular rule is the better answer, and among rules of one particularity the one written
        // higher in the table stands first; the sort is stable, so the table settles every tie.
        found.sort((first, second) -> Integer.compare(second.rule.selectivity(),
                first.rule.selectivity()));
        List<Reaction> answers = new ArrayList<>(found.size());
        for (Chosen chosen : found) {
            answers.add(chosen.reaction);
        }
        return List.copyOf(answers);
    }

    /** A rule and the answer it gave, so that the answers may be ordered by the rule that wrote them. */
    private record Chosen(ReactionRule rule, Reaction reaction) {
    }

    /**
     * A vessel laid out as one molecule, every substance numbered after the one before it.
     * <p>
     * <b>A substance that stands in the vessel more than once is laid out more than once.</b> A reaction
     * between two molecules of one substance - the condensation of an aldehyde with itself, the closing of
     * two of them into a ring - is not a reaction of one molecule with itself, and a vessel that was laid out
     * once would have the two halves of it be the same half. Three copies are laid out, because the rule that
     * needs the most of one substance - the reduction of a nitro group to an amine, which spends three
     * molecules of hydrogen - takes three, and no rule takes more.
     */
    private static Pot lay(Mixture content) {
        List<Chemical> substances = new ArrayList<>(content.components().keySet());
        List<Molecule> molecules = new ArrayList<>();
        List<Integer> origins = new ArrayList<>();
        List<Integer> copies = new ArrayList<>();
        for (int index = 0; index < substances.size(); index++) {
            Chemical chemical = substances.get(index);
            int copiesOfIt = Math.min(3, Math.max(1, content.amountOf(chemical)));
            Molecule molecule = Assemblies.materialize(chemical.structure());
            for (int copy = 0; copy < copiesOfIt; copy++) {
                molecules.add(molecule);
                for (int step = 0; step < molecule.atomCount(); step++) {
                    origins.add(index);
                    copies.add(copy);
                }
            }
        }
        Molecule joined = Assemblies.join(molecules);
        int[] origin = new int[origins.size()];
        int[] copy = new int[copies.size()];
        for (int atom = 0; atom < origin.length; atom++) {
            origin[atom] = origins.get(atom);
            copy[atom] = copies.get(atom);
        }
        return new Pot(joined, origin, copy, substances);
    }

    /** The groups that stand in what the vessel holds. */
    private static Set<FunctionalGroup> presence(Molecule molecule) {
        Set<FunctionalGroup> present = new HashSet<>();
        for (Site site : Sites.all(molecule)) {
            present.add(site.group());
        }
        return present;
    }
}
