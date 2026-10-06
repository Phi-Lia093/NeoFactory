package com.philia093.neofactory.chemistry;

/**
 * The groups a molecule of the organic side is read by: the pieces a reaction is written against.
 * <p>
 * An organic reaction is not written against a whole molecule but against the group in it that reacts - the
 * double bond a hydrogen adds across, the carbonyl a nucleophile attacks, the hydroxyl that is esterified.
 * The group is therefore what a rule of a reaction asks for, and finding the groups of what stands in a
 * vessel is the first thing a rule does, see {@link Sites}.
 * <p>
 * <b>A group is a shape and not a name.</b> Nothing here is looked up in a catalog: a carbonyl is a carbon
 * held to an oxygen by a double bond, wherever it stands and whatever hangs off it, so a rule written for a
 * ketone fits a molecule nobody has written down before. Which is the whole reason the organic side can be
 * left free of a table of substances, see {@link PolarEngine}.
 */
public enum FunctionalGroup {

    /** Two carbons held together by a double bond. */
    ALKENE,

    /** Two carbons held together by a triple bond. */
    ALKYNE,

    /** A carbon held to an oxygen by a double bond. */
    CARBONYL,

    /** A carbonyl whose carbon carries a hydrogen, the carbonyl of an aldehyde. */
    ALDEHYDE,

    /** A carbonyl whose carbon carries two carbons, the carbonyl of a ketone. */
    KETONE,

    /** A carbonyl whose carbon carries a hydroxyl, the acid of the organic side. */
    CARBOXYLIC_ACID,

    /** A carbonyl whose carbon carries an oxygen that carries a carbon, an ester. */
    ESTER,

    /** An oxygen that carries a hydrogen, an alcohol or the acid of an alcohol. */
    HYDROXYL,

    /** A carbon that carries a halogen. */
    HALIDE,

    /** A carbon held to a hydrogen by a plain bond, the hydrogen a radical reaction takes away. */
    ALKYL,

    /** A carbon held to a nitrogen by a triple bond. */
    NITRILE,

    /** A nitrogen that carries a hydrogen and hangs on a carbon, the amine a carbonyl is condensed with. */
    AMINE,

    /** An atom of a molecule that carries a negative charge, the anion a workup gives its proton back to. */
    ANION,

    /** A hydroxyl standing on a carbon that is held by a double bond, the enol of a carbonyl. */
    ENOL,

    /** A carbon held to a nitrogen by a double bond, the imine a carbonyl and an amine condense into. */
    IMINE,

    /** A carbonyl whose carbon carries a nitrogen, the amide an acid and an amine are joined into. */
    AMIDE,

    /** A carbonyl whose carbon carries a halogen, the acid chloride a ring is acylated by. */
    ACYL_HALIDE,

    /** A nitrogen held to two oxygens, the nitro group a ring is given by nitric acid. */
    NITRO,

    /** A nitrogen held to one oxygen by a double bond, the nitroso group a nitro one loses an oxygen into. */
    NITROSO,

    /** A ring whose bonds are the blurred ones of an aromatic ring. */
    AROMATIC_RING
}
