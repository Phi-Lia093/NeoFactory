package com.philia093.neofactory.chemistry;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * One route of the industry, written down by hand instead of inferred.
 * <p>
 * An inorganic reaction is a narrow one. It happens at a temperature and not at another, it needs a metal
 * and not the one beside it, and the difference between two products that are both arithmetically right is
 * a fact of the trade and not of the balance. Trying to find such a reaction by balancing what stands in a
 * pot gives an answer that is true and useless - two hydrogens and an oxygen into hydrogen peroxide is as
 * balanced as into water - so a route of the industry is written down instead: what goes in, in what
 * amounts, what comes out, under what conditions, and which of the products is the one the route is run
 * for. The module then matches a pot against these writings and never invents one, see
 * {@link InorganicRecipeBook}.
 * <p>
 * <b>A route is a pile in and a pile out, in millibuckets.</b> Two molecules of hydrogen are two hundred
 * of the millibuckets a balance is measured in, and an ingot of an alloy is its metals side by side, so a
 * route is written the way a machine is fed and never the way a chemist would draw it. The medium - the
 * water a route is carried out in, the proton it takes from it - stands beside the piles and is allowed to
 * move whole and unnumbered, so a route does not have to count the solvent it never measured.
 * <p>
 * <b>A route that does not balance cannot be written down.</b> The rule of a pot is asked once, while the
 * route is built, and a writing that leaves an atom behind or a charge unspent is refused where it is
 * written - which is the one place a wrong route can still be stopped, since no balance will ever be asked
 * about it again, see {@link BlendConservation}.
 */
public final class InorganicRecipe {

    private final String id;
    private final Blend inputs;
    private final Blend outputs;
    private final Chemical primary;
    private final List<Chemical> medium;
    private final int electrons;
    private final Conditions conditions;
    private final int priority;
    private final String comment;

    private InorganicRecipe(Builder builder) {
        this.id = builder.id;
        this.inputs = builder.inputs;
        this.outputs = builder.outputs;
        this.primary = builder.primary;
        this.medium = List.copyOf(builder.medium);
        this.electrons = builder.electrons;
        this.conditions = builder.conditions;
        this.priority = builder.priority;
        this.comment = builder.comment;
    }

    /** Begins a route. */
    public static Builder builder(String id) {
        return new Builder(id);
    }

    /** The name of this route, for a log and a screen. */
    public String id() {
        return id;
    }

    /** What the route is fed, in millibuckets of every substance. */
    public Blend inputs() {
        return inputs;
    }

    /** What the route hands over, in millibuckets of every substance. */
    public Blend outputs() {
        return outputs;
    }

    /** The product the route is run for, {@code null} when its writing names none. */
    public Chemical primary() {
        return primary;
    }

    /** The substances of the medium, which may move whole and unnumbered around this route. */
    public List<Chemical> medium() {
        return medium;
    }

    /** Electrons one run of the route takes in, negative when it gives them out. */
    public int electrons() {
        return electrons;
    }

    /** What the route asks of the vessel. */
    public Conditions conditions() {
        return conditions;
    }

    /** How important this route is where several of them match a pot, higher first. */
    public int priority() {
        return priority;
    }

    /** What the route is for, in the words of whoever wrote it. */
    public String comment() {
        return comment;
    }

    /**
     * How many whole runs of this route a pot can pay for, {@code 0} when it cannot pay for one.
     * <p>
     * A route is written for one run, and a pot that holds twice what it asks for runs it twice: the
     * number is the largest one that fits every substance the route needs at once, which is the bottom of
     * the quotients - a pot that holds enough sulfur for three runs and enough oxygen for one runs once,
     * because the second thing it does with the sulfur it has not got the oxygen for. A substance the pot
     * does not hold at all is no run at all, whatever the rest of it holds.
     *
     * @param pile what stands in the pot
     * @return the amount of runs, {@code 0} when the pot cannot pay for one
     */
    public int runs(Blend pile) {
        long runs = Long.MAX_VALUE;
        for (Map.Entry<Chemical, Fraction> entry : inputs.components().entrySet()) {
            Fraction have = pile.amountOf(entry.getKey());
            Fraction need = entry.getValue();
            if (have.compareTo(need) < 0) {
                return 0;
            }
            long whole = Math.multiplyExact(have.numerator(), need.denominator())
                    / Math.multiplyExact(need.numerator(), have.denominator());
            runs = Math.min(runs, whole);
        }
        return (int) Math.min(Integer.MAX_VALUE, runs);
    }

    /**
     * {@code true} when a pot holds everything this route needs and the vessel is what it asks for.
     *
     * @param pile what stands in the pot
     * @param offered what the machine reads of the vessel
     * @return {@code true} when {@link #run(Blend, Conditions)} may be called
     */
    public boolean matches(Blend pile, Conditions offered) {
        return runs(pile) >= 1 && conditions.within(offered);
    }

    /**
     * Runs this route over a pot, taking as many runs as the pot can pay for.
     *
     * @param pile what stands in the pot
     * @param offered what the machine reads of the vessel
     * @return what the pot loses and gains, or {@code null} when this route does not run there
     */
    public Outcome run(Blend pile, Conditions offered) {
        int runs = runs(pile);
        if (runs < 1 || !conditions.within(offered)) {
            return null;
        }
        return new Outcome(id, inputs.times(runs), outputs.times(runs),
                (int) Math.min(Integer.MAX_VALUE, (long) electrons * runs), medium);
    }

    /**
     * How narrowly this route is written: the route that names more substances and asks more of the vessel
     * is the more particular reading of a pot, and is taken before a broader route that also fits it.
     *
     * @return the width, higher for the more particular route
     */
    public int specificity() {
        int named = 0;
        if (conditions.minTemperature() != null) {
            named++;
        }
        if (conditions.minPressure() != null) {
            named++;
        }
        if (!conditions.catalysts().isEmpty()) {
            named++;
        }
        if (conditions.phase() != null) {
            named++;
        }
        if (conditions.current()) {
            named++;
        }
        return inputs.components().size() + outputs.components().size() + named;
    }

    @Override
    public String toString() {
        return "InorganicRecipe(" + id + ", " + inputs + " -> " + outputs + ")";
    }

    /** Builds a route a field at a time. */
    public static final class Builder {

        private final String id;
        private Blend inputs;
        private Blend outputs;
        private Chemical primary;
        private final List<Chemical> medium = new ArrayList<>();
        private int electrons;
        private Conditions conditions = Conditions.NONE;
        private int priority;
        private String comment = "";

        private Builder(String id) {
            this.id = Objects.requireNonNull(id, "id");
        }

        /**
         * Names what the route is fed.
         *
         * @param inputs the pile that goes in, in millibuckets
         * @return this builder
         */
        public Builder inputs(Blend inputs) {
            this.inputs = Objects.requireNonNull(inputs, "inputs");
            return this;
        }

        /**
         * Names what the route hands over.
         *
         * @param outputs the pile that comes out, in millibuckets
         * @return this builder
         */
        public Builder outputs(Blend outputs) {
            this.outputs = Objects.requireNonNull(outputs, "outputs");
            return this;
        }

        /**
         * Names the product the route is run for.
         *
         * @param primary the main product, one of the substances of the output pile
         * @return this builder
         */
        public Builder primary(Chemical primary) {
            this.primary = Objects.requireNonNull(primary, "primary");
            return this;
        }

        /**
         * Names substances of the medium that may move whole and unnumbered around the route.
         *
         * @param medium the substances of the medium
         * @return this builder
         */
        public Builder medium(Chemical... medium) {
            for (Chemical chemical : medium) {
                this.medium.add(Objects.requireNonNull(chemical, "medium"));
            }
            return this;
        }

        /**
         * Names the electrons one run of the route takes in, negative when it gives them out.
         *
         * @param electrons the electrons
         * @return this builder
         */
        public Builder electrons(int electrons) {
            this.electrons = electrons;
            return this;
        }

        /**
         * Names what the route asks of the vessel.
         *
         * @param conditions the conditions
         * @return this builder
         */
        public Builder conditions(Conditions conditions) {
            this.conditions = Objects.requireNonNull(conditions, "conditions");
            return this;
        }

        /**
         * Names how important the route is where several of them fit a pot.
         *
         * @param priority the importance, higher first
         * @return this builder
         */
        public Builder priority(int priority) {
            this.priority = priority;
            return this;
        }

        /**
         * Writes what the route is for.
         *
         * @param comment the words of whoever wrote it
         * @return this builder
         */
        public Builder comment(String comment) {
            this.comment = Objects.requireNonNull(comment, "comment");
            return this;
        }

        /**
         * Builds the route.
         *
         * @return the route
         * @throws IllegalStateException when the route names nothing in or out
         * @throws IllegalStateException when the route does not keep the rule of a pot
         */
        public InorganicRecipe build() {
            if (inputs == null || outputs == null) {
                throw new IllegalStateException("A route names what goes in and what comes out: " + id);
            }
            if (inputs.isEmpty() || outputs.isEmpty()) {
                throw new IllegalStateException("A route changes something: " + id);
            }
            BlendConservation.check(inputs, outputs, electrons, medium);
            return new InorganicRecipe(this);
        }
    }
}
