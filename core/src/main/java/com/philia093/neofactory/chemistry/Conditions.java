package com.philia093.neofactory.chemistry;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * How hot, how pressed and over what a reaction runs, as far as a recipe cares and a machine knows.
 * <p>
 * A reaction of the industry is not only a matter of what stands in the pot. Iron and steam make hydrogen
 * at a red heat and stand still at room temperature; nitrogen and hydrogen need a pressure and a catalyst
 * before they are ammonia; and the same acid attacks a metal it would not touch when it is dilute. A recipe
 * that forgot those would be a wrong recipe, so a recipe names the conditions it needs and a machine names
 * the conditions it offers, and a reaction runs where the two meet.
 * <p>
 * <b>A dimension a recipe does not name is a dimension it does not care about.</b> A recipe that says
 * nothing of pressure may run at any pressure. And <b>a dimension a machine does not name does not block</b>:
 * the machines of the game do not measure a temperature yet, so a route written for a red heat still runs -
 * it runs as the very same route it would have been before anybody wrote a temperature down, and the moment
 * a machine measures one, the range of the recipe is enforced on it, see {@link #within(Conditions)}.
 * <p>
 * <b>A range is written as a bottom and a top</b>, and a measurement is a range whose two ends are the same
 * number, so one value serves both what a recipe asks for and what a machine reads.
 */
public final class Conditions {

    /** The conditions that name nothing: a recipe that asks for nothing, a machine that measures nothing. */
    public static final Conditions NONE = new Conditions(null, null, null, null, Set.of(), null, false);

    private final Fraction minTemperature;
    private final Fraction maxTemperature;
    private final Fraction minPressure;
    private final Fraction maxPressure;
    private final Set<Chemical> catalysts;
    private final Phase phase;
    private final boolean current;

    private Conditions(Fraction minTemperature, Fraction maxTemperature, Fraction minPressure,
            Fraction maxPressure, Set<Chemical> catalysts, Phase phase, boolean current) {
        this.minTemperature = minTemperature;
        this.maxTemperature = maxTemperature;
        this.minPressure = minPressure;
        this.maxPressure = maxPressure;
        this.catalysts = Collections.unmodifiableSet(new LinkedHashSet<>(catalysts));
        this.phase = phase;
        this.current = current;
    }

    /** The conditions that name nothing. */
    public static Conditions none() {
        return NONE;
    }

    /**
     * Conditions of one temperature and a pressure, the shape a machine reads a vessel with.
     *
     * @param temperature the temperature in kelvin, {@code null} for one not measured
     * @param pressure the pressure, {@code null} for one not measured
     * @param phase the state of the vessel, {@code null} for one not seen
     * @return the conditions
     */
    public static Conditions of(Fraction temperature, Fraction pressure, Phase phase) {
        return builder().temperature(temperature).pressure(pressure).phase(phase).build();
    }

    /**
     * Conditions of a vessel a current is driving or not.
     *
     * @param temperature the temperature in kelvin, {@code null} for one not measured
     * @param pressure the pressure, {@code null} for one not measured
     * @param phase the state of the vessel, {@code null} for one not seen
     * @param current {@code true} when a current drives the vessel
     * @return the conditions
     */
    public static Conditions of(Fraction temperature, Fraction pressure, Phase phase,
            boolean current) {
        return builder().temperature(temperature).pressure(pressure).phase(phase).current(current)
                .build();
    }

    /** A builder of conditions. */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * {@code true} when these conditions of a machine fall inside what a recipe asks for.
     * <p>
     * A dimension the recipe does not name is not looked at, and a dimension the machine does not name is
     * not looked at either, see the class comment. A catalyst the recipe asks for has to be among the
     * catalysts the machine offers, and a machine that names none offers none.
     *
     * @param offered what the machine reads of the vessel
     * @return {@code true} when a recipe with these conditions may run there
     */
    public boolean within(Conditions offered) {
        Objects.requireNonNull(offered, "offered");
        if (current && !offered.current) {
            return false;
        }
        if (!inside(minTemperature, maxTemperature, offered.minTemperature, offered.maxTemperature)) {
            return false;
        }
        if (!inside(minPressure, maxPressure, offered.minPressure, offered.maxPressure)) {
            return false;
        }
        if (phase != null && offered.phase != null && offered.phase != phase) {
            return false;
        }
        return catalysts.isEmpty() || offered.catalysts.isEmpty()
                || offered.catalysts.containsAll(catalysts);
    }

    /** {@code true} when a range a machine reads lies inside a range a recipe asks for. */
    private static boolean inside(Fraction wantedFrom, Fraction wantedTo, Fraction readFrom,
            Fraction readTo) {
        if (wantedFrom == null || wantedTo == null || readFrom == null || readTo == null) {
            return true;
        }
        return readFrom.compareTo(wantedFrom) >= 0 && readTo.compareTo(wantedTo) <= 0;
    }

    /** The bottom of the temperature a recipe asks for, {@code null} when it asks for none. */
    public Fraction minTemperature() {
        return minTemperature;
    }

    /** The top of the temperature a recipe asks for, {@code null} when it asks for none. */
    public Fraction maxTemperature() {
        return maxTemperature;
    }

    /** The bottom of the pressure a recipe asks for, {@code null} when it asks for none. */
    public Fraction minPressure() {
        return minPressure;
    }

    /** The top of the pressure a recipe asks for, {@code null} when it asks for none. */
    public Fraction maxPressure() {
        return maxPressure;
    }

    /** The catalysts a recipe asks for, empty when it asks for none. */
    public Set<Chemical> catalysts() {
        return catalysts;
    }

    /** The state a recipe asks for, {@code null} when it asks for any. */
    public Phase phase() {
        return phase;
    }

    /**
     * {@code true} when a current drives this vessel, or when a route of this kind is one a current drives.
     * <p>
     * <b>A current is the one condition that is asked and not merely wondered at.</b> A route that needs a
     * current is not run by a pot that has none - water does not split into its gases over a flame however
     * hot the flame - so a route of that kind demands a current of what it is offered, and a route that
     * names none never looks at this at all. A machine that drives a pot says so; a pot that is only heated
     * says nothing and is never matched against a route a current is needed for.
     *
     * @return {@code true} when a current is needed or is driving
     */
    public boolean current() {
        return current;
    }

    @Override
    public String toString() {
        return "Conditions(temperature " + minTemperature + ".." + maxTemperature + ", pressure "
                + minPressure + ".." + maxPressure + ", catalysts " + catalysts.size() + ", phase "
                + phase + (current ? ", a current" : "") + ")";
    }

    /** Builds a set of conditions a range at a time. */
    public static final class Builder {

        private Fraction minTemperature;
        private Fraction maxTemperature;
        private Fraction minPressure;
        private Fraction maxPressure;
        private final Set<Chemical> catalysts = new LinkedHashSet<>();
        private Phase phase;
        private boolean current;

        private Builder() {
            // A builder holds nothing until it is told.
        }

        /**
         * Asks for a temperature, or reads one.
         *
         * @param temperature the temperature in kelvin, {@code null} for one not named
         * @return this builder
         */
        public Builder temperature(Fraction temperature) {
            return temperature(temperature, temperature);
        }

        /**
         * Asks for a range of temperature.
         *
         * @param from the coldest, {@code null} for no bottom
         * @param to the hottest, {@code null} for no top
         * @return this builder
         */
        public Builder temperature(Fraction from, Fraction to) {
            this.minTemperature = from;
            this.maxTemperature = to;
            return this;
        }

        /**
         * Asks for a pressure, or reads one.
         *
         * @param pressure the pressure, {@code null} for one not named
         * @return this builder
         */
        public Builder pressure(Fraction pressure) {
            return pressure(pressure, pressure);
        }

        /**
         * Asks for a range of pressure.
         *
         * @param from the least, {@code null} for no bottom
         * @param to the most, {@code null} for no top
         * @return this builder
         */
        public Builder pressure(Fraction from, Fraction to) {
            this.minPressure = from;
            this.maxPressure = to;
            return this;
        }

        /**
         * Asks for a catalyst, or names one that stands there.
         *
         * @param catalyst the catalyst
         * @return this builder
         */
        public Builder catalyst(Chemical catalyst) {
            catalysts.add(Objects.requireNonNull(catalyst, "catalyst"));
            return this;
        }

        /**
         * Names the state of the vessel.
         *
         * @param phase the state, {@code null} for any
         * @return this builder
         */
        public Builder phase(Phase phase) {
            this.phase = phase;
            return this;
        }

        /**
         * Says that a current drives this vessel, or that a route of this kind needs one.
         *
         * @param current {@code true} when a current is driving or is needed
         * @return this builder
         */
        public Builder current(boolean current) {
            this.current = current;
            return this;
        }

        /** Builds the conditions. */
        public Conditions build() {
            return new Conditions(minTemperature, maxTemperature, minPressure, maxPressure, catalysts,
                    phase, current);
        }
    }
}
