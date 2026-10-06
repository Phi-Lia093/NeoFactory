package com.philia093.neofactory.chemistry;

import java.util.List;
import java.util.Objects;

/**
 * What stands in a vessel: the substances that were put in, the medium they stand in and the state of it.
 * <p>
 * A reaction is never written from nothing. Something is in the vessel - an ore, a solution, a gas - and
 * around it stands a medium that a balance is allowed to draw on and give back to, see
 * {@link Conservation}. A system is the two of them together: the content that is measured out and the
 * background that just stands there, in the phase the whole of it is in. A solver is offered a system and
 * answers with the reactions that may run inside it, which is what keeps the medium out of the input a
 * player filled and out of the products a reaction hands back.
 * <p>
 * <b>A solution is the common case and has a name of its own.</b> Solutes in a solvent - ions in water, an
 * acid in water - is a system whose phase is {@link Phase#AQUEOUS} and whose background is the solvent, and
 * it is written so often that {@link #solution(Mixture, Chemical)} spells it out. Nothing else about a
 * system is special: the medium is an ordinary substance that happens to be allowed to move whole.
 */
public final class System {

    private final Mixture content;
    private final List<Chemical> background;
    private final Phase phase;
    private final Conditions conditions;

    private System(Mixture content, List<Chemical> background, Phase phase) {
        this.content = Objects.requireNonNull(content, "content");
        this.background = List.copyOf(Objects.requireNonNull(background, "background"));
        this.phase = Objects.requireNonNull(phase, "phase");
        this.conditions = Conditions.NONE;
    }

    private System(Mixture content, List<Chemical> background, Phase phase, Conditions conditions) {
        this.content = Objects.requireNonNull(content, "content");
        this.background = List.copyOf(Objects.requireNonNull(background, "background"));
        this.phase = Objects.requireNonNull(phase, "phase");
        this.conditions = Objects.requireNonNull(conditions, "conditions");
    }

    /**
     * Creates a system with nothing standing around.
     *
     * @param content what was put in
     * @param phase the state of it
     * @return the system
     */
    public static System of(Mixture content, Phase phase) {
        return new System(content, List.of(), phase);
    }

    /**
     * Creates a system.
     *
     * @param content what was put in
     * @param phase the state of it
     * @param background substances that may move whole and unnumbered
     * @return the system
     */
    public static System of(Mixture content, Phase phase, List<Chemical> background) {
        return new System(content, background, phase);
    }

    /**
     * Creates a solution: solutes in a solvent, in the aqueous phase.
     *
     * @param solutes what is dissolved
     * @param solvent the substance they are dissolved in, the background
     * @return the system
     */
    public static System solution(Mixture solutes, Chemical solvent) {
        return new System(solutes, List.of(Objects.requireNonNull(solvent, "solvent")),
                Phase.AQUEOUS);
    }

    /**
     * Creates a system with the conditions it is run at.
     *
     * @param content what was put in
     * @param phase the state of it
     * @param background substances that may move whole and unnumbered
     * @param conditions how hot the vessel is, what it is filled with and what stands in it
     * @return the system
     */
    public static System of(Mixture content, Phase phase, List<Chemical> background,
            Conditions conditions) {
        return new System(content, background, phase, conditions);
    }

    /**
     * The same system run at other conditions.
     * <p>
     * A vessel is what it holds and how it is run, and a caller usually knows the second only after the
     * first: what stands in the reactor is read off its slots and tanks and the setting of it off the dial.
     *
     * @param conditions how hot the vessel is, what it is filled with and what stands in it
     * @return the same content run at those conditions
     */
    public System with(Conditions conditions) {
        return new System(content, background, phase, Objects.requireNonNull(conditions, "conditions"));
    }

    /** What was put into the vessel. */
    public Mixture content() {
        return content;
    }

    /** The substances that stand around and may move whole and unnumbered. */
    public List<Chemical> background() {
        return background;
    }

    /** The state the whole of the system stands in. */
    public Phase phase() {
        return phase;
    }

    /**
     * How hot the vessel is run, what it is filled with and what stands in it.
     * <p>
     * A system that was built without conditions names none, which is a vessel that is not measured: every
     * dimension of it is one that does not block, see {@link Conditions}.
     *
     * @return the conditions
     */
    public Conditions conditions() {
        return conditions;
    }

    @Override
    public String toString() {
        return "System(" + content + ", " + phase + ", background " + background.size() + ", "
                + conditions + ")";
    }
}
