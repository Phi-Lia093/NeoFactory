package com.philia093.neofactory.chemistry;

/**
 * How ready each place of a molecule is to react, the table a rule reads when more than one place would do.
 * <p>
 * A vessel rarely holds one thing that could react. A molecule may carry an aldehyde beside a ketone, two
 * halogens on carbons of different crowding, a double bond beside an ester; the nucleophile of the medium
 * attacks the aldehyde first, the one that comes in from behind goes to the plainer carbon, and the halogen
 * that leaves is the one that leaves most easily. Those are not guesses - they are the declared orders of the
 * trade - and they are written down here, once, so that a rule does not have to choose a place of its own and
 * two rules cannot choose in two different ways.
 * <p>
 * <b>What is written here is an order and not a number of its own.</b> A ranking is a claim about which of
 * two places reacts first and never about how much faster, so the values are only ever compared with one
 * another: an aldehyde outranks a ketone, a ketone outranks an ester, an ester outranks an acid, and an amide
 * is last. Chemistry more finely graded than that - a rate, a yield, a temperature - is what this module
 * deliberately does not pretend to know.
 */
public final class Reactivity {

    private Reactivity() {
        // Utility class: never instantiated.
    }

    /**
     * How ready the carbon of a carbonyl is to be attacked by a nucleophile, higher for the readier.
     * <p>
     * The order is the order every course teaches: an aldehyde first, then a ketone, then an ester, then an
     * acid, and an amide last - because what hangs beside the carbonyl takes the electrophile away from it: a
     * carbon not at all, an oxygen that holds a carbon a little, an oxygen that holds a hydrogen more, and a
     * nitrogen most of all.
     *
     * @param molecule molecule to read
     * @param carbon the carbon of a carbonyl
     * @return the ranking, higher for the readier
     */
    public static int electrophilicity(Molecule molecule, int carbon) {
        int beside = 0;
        boolean nitrogen = false;
        for (int neighbour : molecule.neighbours(carbon)) {
            String element = molecule.atom(neighbour).element();
            if (element.equals("N")) {
                nitrogen |= true;
                continue;
            }
            if (element.equals("O") || element.equals("S") || element.equals("P")) {
                Bond bond = bondBetween(molecule, carbon, neighbour);
                if (bond != null && bond.order() > 1) {
                    continue;
                }
                // An oxygen holding a hydrogen is the hydroxyl of an acid, one holding a carbon the oxygen
                // of an ester, and the same is read for a sulfur or a phosphorus beside the carbonyl.
                beside = Math.max(beside, Sites.hydrogensOn(molecule, neighbour) > 0 ? 1 : 2);
            }
        }
        if (nitrogen) {
            return 0;
        }
        if (beside == 1) {
            return 1;
        }
        if (beside == 2) {
            return 2;
        }
        return Sites.hydrogensOn(molecule, carbon) > 0 ? 4 : 3;
    }

    /**
     * How ready a double bond is to be added across, higher for the readier.
     * <p>
     * A double bond that carries carbons instead of hydrogens is the better nucleophile: a methyl group
     * pushes electrons into the bond, so a double bond between two carbons that each carry others stands
     * above one with a hydrogen beside it. The count is the whole of the table, because the trade teaches the
     * order and not the rate.
     *
     * @param molecule molecule to read
     * @param alkene the two carbons of a double bond
     * @return the ranking, higher for the readier
     */
    public static int nucleophilicity(Molecule molecule, Site alkene) {
        return heavyNeighbours(molecule, alkene.atom(0)) + heavyNeighbours(molecule, alkene.atom(1));
    }

    /**
     * How easily a halogen leaves when a bond to it is broken, higher for the easier.
     * <p>
     * Iodine leaves most easily, then bromine, then chlorine, and fluorine hardly at all - which is why a
     * bromide is the substrate of a substitution that is taught and a fluoride never is.
     *
     * @param molecule molecule to read
     * @param halogen the atom that would leave
     * @return the ranking, higher for the easier
     */
    public static int leavingAbility(Molecule molecule, int halogen) {
        switch (molecule.atom(halogen).element()) {
            case "I":
                return 4;
            case "Br":
                return 3;
            case "Cl":
                return 2;
            case "F":
                return 1;
            default:
                return 0;
        }
    }

    /**
     * How crowded the carbon of a bond is, lower for the plainer.
     * <p>
     * A nucleophile that comes in from behind the group that leaves reaches the plainer carbon first, so the
     * carbon with the fewest others hanging on it is the one a substitution runs at. Atoms that are no
     * hydrogen are counted, since a hydrogen is the smallest thing that can hang there.
     *
     * @param molecule molecule to read
     * @param carbon the carbon
     * @return the number of atoms beside the carbon, besides hydrogens
     */
    public static int crowding(Molecule molecule, int carbon) {
        return heavyNeighbours(molecule, carbon);
    }

    /**
     * Where the group already on a ring sends an electrophile: to the positions beside it, or to the ones past
     * a carbon of them.
     * <p>
     * A ring is not attacked at random. A group that holds a pair of electrons it can lend to the ring - an
     * oxygen, a nitrogen, a halogen, a plain carbon - pushes electrons into it and makes the positions beside
     * itself and across from itself the richer ones, so the electrophile goes there; a group that pulls
     * electrons out of the ring - a nitro, a carbonyl, a nitrile - empties the same positions and leaves the
     * ones past a carbon as the only choice. That is the whole of what the trade means by a ring being
     * directed, and it is the one part of aromatic chemistry that is a rule of thumb rather than a rate: what
     * is written here is which of two positions reacts, and never by how much.
     *
     * @param molecule molecule to read
     * @param substituent the atom that hangs on the ring
     * @return {@code true} when the electrophile is sent beside the group, {@code false} when past one carbon
     */
    public static boolean directsToTheSides(Molecule molecule, int substituent) {
        switch (molecule.atom(substituent).element()) {
            case "N":
            case "S":
            case "C":
                // An amine lends the pair of its nitrogen to the ring and a thiol the one of its sulfur,
                // while a nitro, a sulfonyl, a carbonyl and a nitrile take the electrons of the ring away
                // instead: the difference is whether the atom beside the ring is held by a double bond. A
                // carbon left short of electrons by the atoms on it - a trifluoromethyl, a trichloromethyl -
                // takes them away as well rather than lending them.
                return !carriesWithdrawingDoubleBond(molecule, substituent)
                        && !shortOfElectrons(molecule, substituent);
            case "O":
            case "F":
            case "Cl":
            case "Br":
            case "I":
                return true;
            default:
                return true;
        }
    }

    /** {@code true} when an atom is held by a double bond to an oxygen or a nitrogen, which takes electrons. */
    private static boolean carriesWithdrawingDoubleBond(Molecule molecule, int atom) {
        for (Bond bond : molecule.bonds()) {
            if (bond.order() > 1 && bond.touches(atom)) {
                String other = molecule.atom(bond.other(atom)).element();
                if (other.equals("O") || other.equals("N")) {
                    return true;
                }
            }
        }
        return false;
    }

    /** {@code true} when the atoms hanging on a carbon take its electrons away - a CF3, a CCl3. */
    private static boolean shortOfElectrons(Molecule molecule, int atom) {
        int takers = 0;
        for (int neighbour : molecule.neighbours(atom)) {
            String element = molecule.atom(neighbour).element();
            if (element.equals("O") || element.equals("N") || element.equals("F")
                    || element.equals("Cl") || element.equals("Br") || element.equals("I")) {
                takers++;
            }
        }
        return takers >= 2;
    }

    /** How many atoms that are no hydrogen hang on an atom. */
    private static int heavyNeighbours(Molecule molecule, int atom) {
        int count = 0;
        for (int neighbour : molecule.neighbours(atom)) {
            if (!isElement(molecule, neighbour, "H")) {
                count++;
            }
        }
        return count;
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

    /** {@code true} when an atom of a molecule is of a named element. */
    private static boolean isElement(Molecule molecule, int atom, String element) {
        return molecule.atom(atom).element().equals(element);
    }
}
