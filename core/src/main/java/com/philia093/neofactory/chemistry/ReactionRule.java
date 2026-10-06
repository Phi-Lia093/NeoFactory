package com.philia093.neofactory.chemistry;

import java.util.Objects;
import java.util.Set;

/**
 * One reaction of the organic side, written as the groups it needs and the change it makes.
 * <p>
 * A rule is what a chemist writes above an arrow and nothing more: the groups that have to be there for it
 * to happen at all, and what is done once they are. It is not a template of a molecule - a rule for the
 * hydrogenation of a double bond fits every alkene there is - and it is not a substance of a catalog, which
 * is what lets a molecule nobody has ever written down be reacted with all the same.
 * <p>
 * <b>The groups are a filter and not the whole of it.</b> Naming the groups that have to stand in the vessel
 * keeps a rule from being tried on a pot it could not possibly fit, and the writing itself is asked
 * afterwards and may still answer that it does not fit; a rule that answered with a reaction whose
 * substances were not there would be a rule that lied about what stands in the vessel.
 */
public final class ReactionRule {

    /** How a rule carries its change out on a vessel that was laid out for it. */
    public interface Writing {

        /**
         * Carries the rule out.
         *
         * @param pot the vessel, laid out as one molecule
         * @return the reaction, or {@code null} when the rule does not fit after all
         */
        Reaction write(Pot pot);
    }

    private final String name;
    private final String category;
    private final int selectivity;
    private final Set<FunctionalGroup> needs;
    private final Conditions conditions;
    private final Writing writing;

    /**
     * Creates a rule whose selectivity is what it names: the more groups a rule asks for, the more particular
     * it is, and a particular rule is a better answer for a vessel than one that would fit many.
     *
     * @param name the name of the rule, for a log line and a test
     * @param category the family of reactions the rule belongs to
     * @param needs the groups that have to stand in the vessel
     * @param writing what the rule does when they do
     */
    public ReactionRule(String name, String category, Set<FunctionalGroup> needs, Writing writing) {
        this(name, category, needs.size(), needs, writing);
    }

    /**
     * Creates a rule with a selectivity of its own.
     * <p>
     * The count of the groups is the right measure for most rules and the wrong one for a few: a rule that
     * names one group and asks for a particular reagent inside itself is more particular than another that
     * names the same one group, and only the rule knows it. What a rule says here is therefore read before
     * what it names, see {@link PolarEngine#infer}.
     *
     * @param name the name of the rule, for a log line and a test
     * @param category the family of reactions the rule belongs to
     * @param selectivity how particular the rule is, higher for the more particular
     * @param needs the groups that have to stand in the vessel
     * @param writing what the rule does when they do
     */
    public ReactionRule(String name, String category, int selectivity, Set<FunctionalGroup> needs,
            Writing writing) {
        this(name, category, selectivity, needs, Conditions.NONE, writing);
    }

    /**
     * Creates a rule that only runs in a vessel of the conditions it names.
     * <p>
     * <b>The groups are not the whole of what a reaction needs.</b> An alkene and hydrogen are an alkane
     * only over a metal, an alcohol loses water only over a flame, and an ester is hydrolysed in water and
     * not in the alcohol that would put it back. Those are conditions of the vessel and not groups in it, so
     * they are named here and asked of the vessel before the rule is tried, see {@link PolarEngine#infer}.
     * A dimension a rule does not name is one it does not care about, and a dimension a vessel does not name
     * does not block, see {@link Conditions#within(Conditions)}.
     *
     * @param name the name of the rule, for a log line and a test
     * @param category the family of reactions the rule belongs to
     * @param selectivity how particular the rule is, higher for the more particular
     * @param needs the groups that have to stand in the vessel
     * @param conditions the heat, the medium and the catalysts the vessel has to offer
     * @param writing what the rule does when both are there
     */
    public ReactionRule(String name, String category, int selectivity, Set<FunctionalGroup> needs,
            Conditions conditions, Writing writing) {
        this.name = Objects.requireNonNull(name, "name");
        this.category = Objects.requireNonNull(category, "category");
        this.selectivity = selectivity;
        this.needs = Set.copyOf(Objects.requireNonNull(needs, "needs"));
        this.conditions = Objects.requireNonNull(conditions, "conditions");
        this.writing = Objects.requireNonNull(writing, "writing");
    }

    /** How particular this rule is, higher for the more particular; the engine reads it before the table. */
    public int selectivity() {
        return selectivity;
    }

    /** The name of this rule. */
    public String name() {
        return name;
    }

    /** The family of reactions this rule belongs to. */
    public String category() {
        return category;
    }

    /** The groups that have to stand in a vessel for this rule to be tried at all. */
    public Set<FunctionalGroup> needs() {
        return needs;
    }

    /** The heat, the medium and the catalysts this rule needs of a vessel, {@link Conditions#NONE} for none. */
    public Conditions conditions() {
        return conditions;
    }

    /**
     * Carries this rule out on a vessel.
     *
     * @param pot the vessel, laid out as one molecule
     * @return the reaction, or {@code null} when it does not fit
     */
    public Reaction write(Pot pot) {
        return writing.write(pot);
    }

    @Override
    public String toString() {
        return "ReactionRule(" + name + ")";
    }
}
