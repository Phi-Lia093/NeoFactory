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
        return directingStrength(molecule, substituent) >= 0;
    }

    /**
     * The whole of what a group on a ring does to it, counted in electrons.
     * <p>
     * A ring is read by every way its group can reach the electrons of it, and they are four. <b>Resonance</b>
     * is the pair a nitrogen or an oxygen or a halogen lends straight into the ring, which is the strongest
     * of the four and sends an electrophile beside the group and across from it. <b>The other resonance</b>
     * is a double bond beside the ring: it takes the ring's electrons away when it ends on an oxygen or a
     * nitrogen - a carbonyl, a nitro, a nitrile, a sulfonyl - and lends its own back when it ends on a carbon,
     * which is what makes a vinyl or a phenyl a weaker friend of the ring than an enemy of it. <b>Induction</b>
     * is the drag of an electronegative atom on the electrons of the atom beside the ring, and it is read for
     * the atom itself and for the atoms hanging on a carbon beside it: a trifluoromethyl leaves that carbon
     * short enough to take from the ring. <b>Hyperconjugation</b> is the pair of a carbon to hydrogen bond of
     * a methyl lent to the ring, which is the whole of why a methyl is a friend of the ring at all.
     * <p>
     * The count is positive for a group that leaves the ring the richer, and an electrophile then goes beside
     * the group and across from it; it is negative for one that leaves it the poorer and the electrophile goes
     * past a carbon instead. A group that comes out at nothing is read as the friend, which is what a group
     * nobody has written a rule for falls back on.
     *
     * @param molecule molecule to read
     * @param substituent the atom that hangs on the ring
     * @return the count, positive for a group that lends the ring electrons
     */
    public static int directingStrength(Molecule molecule, int substituent) {
        Atom value = molecule.atom(substituent);
        int strength = 0;
        if (lendsAPair(molecule, substituent)) {
            strength += value.element().equals("N") ? 3 : 2;
        }
        for (Bond bond : molecule.bonds()) {
            if (bond.order() > 1 && bond.touches(substituent)) {
                String other = molecule.atom(bond.other(substituent)).element();
                if (other.equals("O") || other.equals("N")) {
                    strength -= 3;
                } else if (other.equals("C")) {
                    strength += 1;
                }
            }
        }
        strength -= 2 * value.charge();
        if (value.element().equals("C")) {
            strength += Sites.hydrogensOn(molecule, substituent) > 0 ? 1 : 0;
            strength -= shortOfElectrons(molecule, substituent) ? 3 : 0;
        }
        switch (value.element()) {
            case "O":
            case "F":
            case "Cl":
            case "Br":
            case "I":
                strength -= 1;
                break;
            default:
                break;
        }
        return strength;
    }

    /**
     * {@code true} when an atom holds a pair it may lend a ring it hangs on.
     * <p>
     * A nitrogen, an oxygen, a sulfur, a phosphorus and a halogen all bring such a pair, but only while the
     * pair is their own: a nitrogen of a nitro group has spent both of its hands on double bonds to oxygen
     * and has none left, and a positively charged atom has handed the pair over already.
     */
    private static boolean lendsAPair(Molecule molecule, int atom) {
        String element = molecule.atom(atom).element();
        boolean holds = element.equals("N") || element.equals("O") || element.equals("S")
                || element.equals("P") || element.equals("F") || element.equals("Cl")
                || element.equals("Br") || element.equals("I");
        if (!holds || molecule.atom(atom).charge() > 0) {
            return false;
        }
        for (Bond bond : molecule.bonds()) {
            if (bond.order() > 1 && bond.touches(atom)) {
                String other = molecule.atom(bond.other(atom)).element();
                if (other.equals("N") || other.equals("O")) {
                    return false;
                }
            }
        }
        return true;
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
