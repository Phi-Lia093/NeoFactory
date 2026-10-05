package com.philia093.neofactory.chemistry;

import java.util.List;

/**
 * One place a functional group stands in a molecule: which group it is and which atoms make it up.
 * <p>
 * A rule of a reaction is written against atoms and not against a name, so a group that was found has to
 * say which atoms it is: the two carbons of an alkene, the carbon and the oxygen of a carbonyl, the four
 * atoms of an ester. The atoms come in a settled order, the one the finders of {@link Sites} write them in,
 * so a rule may read {@link #atom(int)} by number and know what it is holding.
 *
 * @param group which group this is
 * @param atoms the atoms that make it up, in the order its finder writes them
 */
public record Site(FunctionalGroup group, List<Integer> atoms) {

    /** Checks the fields, so a group that names nothing fails where it is built. */
    public Site {
        java.util.Objects.requireNonNull(group, "group");
        atoms = List.copyOf(java.util.Objects.requireNonNull(atoms, "atoms"));
        if (atoms.isEmpty()) {
            throw new IllegalArgumentException("A site is made of at least one atom: " + group);
        }
    }

    /**
     * One atom of this site.
     *
     * @param index which of them, in the order the finder wrote them
     * @return the index of the atom in its molecule
     */
    public int atom(int index) {
        return atoms.get(index);
    }

    /** How many atoms make up this site. */
    public int size() {
        return atoms.size();
    }
}
