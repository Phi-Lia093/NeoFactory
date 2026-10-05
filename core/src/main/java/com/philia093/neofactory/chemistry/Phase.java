package com.philia093.neofactory.chemistry;

/**
 * The state a substance is in, which is the half of a substance its formula does not name.
 * <p>
 * Water and water vapour are the same molecule and are not the same substance to a factory: one is poured
 * and the other is drawn off, one is a liquid a pump moves and the other a gas a compressor presses, and a
 * reaction that takes one of them and not the other has to say which. A formula cannot tell the two apart -
 * it is one molecule either way - so the phase travels beside the formula, and the catalog matches a
 * substance on the molecule <em>and</em> on the phase and never on the molecule alone.
 * <p>
 * <b>Dissolved is a phase of its own.</b> An ion in water - a sodium, a chloride - is neither the solid
 * metal nor a liquid; it is carried by the water, and it is what the reactions of a solution are written
 * between. Keeping it beside the three states of matter is what lets a catalog hold the ions of a solution
 * and the metals of a workshop as different substances of the same element.
 */
public enum Phase {

    /** A solid, the state a metal is drawn in and a salt lies in. */
    SOLID,

    /** A liquid, the state water is poured in and an acid is kept in. */
    LIQUID,

    /** A gas, the state oxygen is bottled in and a vapour is drawn off in. */
    GAS,

    /** Carried by a solvent, the state the ions of a solution stand in. */
    AQUEOUS
}
