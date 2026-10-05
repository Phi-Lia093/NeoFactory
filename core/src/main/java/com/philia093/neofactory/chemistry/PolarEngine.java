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
        Pot pot = lay(substances);
        Set<FunctionalGroup> present = presence(pot.molecule());
        List<Reaction> found = new ArrayList<>();
        for (ReactionRule rule : rules) {
            if (!present.containsAll(rule.needs())) {
                continue;
            }
            Reaction reaction = rule.write(pot);
            if (reaction != null && Conservation.balanced(reaction)) {
                found.add(reaction);
            }
        }
        return List.copyOf(found);
    }

    /** A vessel laid out as one molecule, every substance numbered after the one before it. */
    private static Pot lay(List<Chemical> substances) {
        List<Molecule> molecules = new ArrayList<>(substances.size());
        for (Chemical chemical : substances) {
            molecules.add(chemical.structure());
        }
        Molecule joined = Assemblies.join(molecules);
        int[] origin = new int[joined.atomCount()];
        int at = 0;
        for (int index = 0; index < substances.size(); index++) {
            for (int step = 0; step < molecules.get(index).atomCount(); step++) {
                origin[at++] = index;
            }
        }
        return new Pot(joined, origin, substances);
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
