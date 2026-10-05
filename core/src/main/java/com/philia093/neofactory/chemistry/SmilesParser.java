package com.philia093.neofactory.chemistry;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads a SMILES string into a {@link Molecule}, the gate every species of the game walks through.
 * <p>
 * SMILES writes a molecule as a walk over its atoms: an atom is a symbol, a bond is a mark between two
 * symbols, a branch is a walk that steps aside and comes back, and a ring is a walk whose two ends are
 * tied together by a matching number. This class reads that walk without a window and without a stack that
 * grows with the length of the string - it keeps its own small stack of the atoms a branch stepped away
 * from - so a chain of a thousand atoms is read as cheaply as a chain of ten and never overflows the stack
 * of the thread. The result is the graph, and nothing but the graph: which atom is which element, what it
 * carries, and what it is bonded to.
 * <p>
 * <b>What is read and what is refused.</b> The organic subset of elements is written bare and every other
 * element between brackets with its charge, its isotope and its hydrogens spelled out; a double bond, a
 * triple bond, the marks of a stereo double bond, a branch, a ring closure and the dot of several pieces
 * are read. A symbol that names no element, a bracket that is never closed, a ring number that is opened
 * twice or closed without being opened - every one of them is refused where it stands, with the position
 * of the character, so that a broken line of the catalog can be walked to, see {@link SmilesException}.
 * <p>
 * <b>Reading a string is not yet knowing the molecule.</b> A bare atom arrives without its hydrogens, which
 * follow from the bonds it turns out to have, and an aromatic ring may be written as a ring of plain and
 * double bonds that has to be recognised, see {@link Aromatizer}. The parse therefore ends by handing the
 * graph to those two, in that order, and answering with the finished molecule: bonds first, then the
 * aromatic rings, then the hydrogens.
 */
public final class SmilesParser {

    private SmilesParser() {
        // Utility class: never instantiated.
    }

    /**
     * Reads a SMILES string into a molecule.
     *
     * @param smiles the string, never empty
     * @return the molecule
     * @throws SmilesException when the string cannot be read
     */
    public static Molecule parse(String smiles) {
        if (smiles == null) {
            throw new SmilesException("A SMILES string must not be null");
        }
        if (smiles.isEmpty()) {
            throw new SmilesException("A SMILES string must not be empty");
        }
        List<Atom> atoms = new ArrayList<>();
        List<Bond> bonds = new ArrayList<>();
        Map<Integer, RingOpening> rings = new LinkedHashMap<>();
        Deque<Integer> branches = new ArrayDeque<>();
        int previous = -1;
        PendingBond pending = null;
        int index = 0;
        while (index < smiles.length()) {
            char symbol = smiles.charAt(index);
            int start = index;
            if (symbol == '(') {
                branches.push(previous);
                index++;
            } else if (symbol == ')') {
                if (branches.isEmpty()) {
                    throw new SmilesException("A branch is closed that was never opened", start);
                }
                previous = branches.pop();
                index++;
            } else if (symbol == '.') {
                if (previous < 0) {
                    throw new SmilesException("A dot stands between two pieces and here between none",
                            start);
                }
                previous = -1;
                pending = null;
                index++;
            } else if (isBondSymbol(symbol)) {
                pending = readBond(symbol, start);
                index++;
            } else if (symbol == '[') {
                index = readBracketAtom(smiles, start, atoms);
                previous = connect(bonds, atoms, previous, atoms.size() - 1, pending, start);
                pending = null;
            } else if (isRingDigit(symbol) || symbol == '%') {
                int[] ring = readRingNumber(smiles, start);
                index = ring[1];
                previous = closeOrOpenRing(bonds, atoms, rings, ring[0], previous, pending, start);
                pending = null;
            } else {
                index = readBareAtom(smiles, start, atoms);
                previous = connect(bonds, atoms, previous, atoms.size() - 1, pending, start);
                pending = null;
            }
        }
        if (!rings.isEmpty()) {
            throw new SmilesException("A ring number is opened and never closed");
        }
        if (!branches.isEmpty()) {
            throw new SmilesException("A branch is opened and never closed");
        }
        Aromatizer.aromatize(atoms, bonds);
        return new Molecule(atoms, bonds);
    }

    /** {@code true} when a character draws a bond. */
    private static boolean isBondSymbol(char symbol) {
        return symbol == '-' || symbol == '=' || symbol == '#' || symbol == ':' || symbol == '/'
                || symbol == '\\';
    }

    /** {@code true} when a character is a one digit ring number. */
    private static boolean isRingDigit(char symbol) {
        return symbol >= '0' && symbol <= '9';
    }

    /**
     * Reads a bond mark into what it asks for.
     *
     * @param symbol the mark, one of {@code - = # : / \}
     * @param position where it stands, for the message of a broken string
     * @return the bond the mark asks for
     */
    private static PendingBond readBond(char symbol, int position) {
        switch (symbol) {
            case '-':
                return new PendingBond(1, false, Bond.NO_STEREO);
            case '=':
                return new PendingBond(2, false, Bond.NO_STEREO);
            case '#':
                return new PendingBond(3, false, Bond.NO_STEREO);
            case ':':
                return new PendingBond(1, true, Bond.NO_STEREO);
            case '/':
                return new PendingBond(1, false, '/');
            case '\\':
                return new PendingBond(1, false, '\\');
            default:
                throw new SmilesException("Unknown bond mark '" + symbol + "'", position);
        }
    }

    /**
     * Joins an atom that was just read to the atom before it.
     * <p>
     * The bond is the one the string asked for - its mark, or the plain bond of a string that named none -
     * and there is none when the atom begins a piece of the molecule. A bond mark with no atom before it is
     * a mark with nothing to hold on to, which is refused.
     *
     * @param bonds bonds of the molecule so far
     * @param atoms atoms of the molecule so far
     * @param previous index of the atom before this one, {@code -1} at the start of a piece
     * @param atom index of the atom that was just read
     * @param pending bond the string asked for, {@code null} when it named none
     * @param position where the atom stands, for the message of a broken string
     * @return the index of the atom, to stand as the previous one from now on
     */
    private static int connect(List<Bond> bonds, List<Atom> atoms, int previous, int atom,
            PendingBond pending, int position) {
        if (previous >= 0) {
            bonds.add(bondBetween(previous, atom, pending, atoms));
        } else if (pending != null) {
            throw new SmilesException("A bond is drawn before any atom", position);
        }
        return atom;
    }

    /**
     * Reads a ring number, one digit or two after a percent.
     *
     * @param smiles the string
     * @param start index of the first character of the number
     * @return the number and the index behind it
     * @throws SmilesException when the number is cut short
     */
    private static int[] readRingNumber(String smiles, int start) {
        if (smiles.charAt(start) == '%') {
            if (start + 2 >= smiles.length() || !isRingDigit(smiles.charAt(start + 1))
                    || !isRingDigit(smiles.charAt(start + 2))) {
                throw new SmilesException("A two digit ring number needs two digits", start);
            }
            int number = (smiles.charAt(start + 1) - '0') * 10 + (smiles.charAt(start + 2) - '0');
            return new int[] {number, start + 3};
        }
        return new int[] {smiles.charAt(start) - '0', start + 1};
    }

    /**
     * Opens a ring at the atom before the number, or closes it at the atom behind it.
     * <p>
     * The first time a number is read it ties the atom the walk stands on to nothing yet and is remembered;
     * the second time it ties that stored atom to the current one, which is the bond that closes the ring.
     * The bond of a ring follows the same marks as any other, taken from the nearer side when both sides
     * name one.
     *
     * @param bonds bonds of the molecule so far
     * @param atoms atoms of the molecule so far
     * @param rings ring numbers that stand open
     * @param number the ring number that was read
     * @param previous index of the atom the walk stands on, {@code -1} before any atom
     * @param pending bond the string asked for, {@code null} when it named none
     * @param position where the number stands, for the message of a broken string
     * @return the index of the atom the walk stands on, unchanged
     */
    private static int closeOrOpenRing(List<Bond> bonds, List<Atom> atoms,
            Map<Integer, RingOpening> rings, int number, int previous, PendingBond pending,
            int position) {
        RingOpening opening = rings.remove(number);
        if (opening == null) {
            if (previous < 0) {
                throw new SmilesException("A ring is opened before any atom", position);
            }
            rings.put(number, new RingOpening(previous, pending));
            return previous;
        }
        if (previous < 0) {
            throw new SmilesException("A ring is closed before any atom", position);
        }
        PendingBond bond = pending != null ? pending : opening.bond;
        bonds.add(bondBetween(opening.atom, previous, bond, atoms));
        return previous;
    }

    /**
     * The bond between two atoms, as the string asked for it or as the two atoms make it plain.
     * <p>
     * A string that names no bond leaves the two atoms to decide: two atoms of an aromatic ring are joined
     * by a bond of that ring, and any other pair by a single bond. Naming one - a double bond, a stereo mark
     * - is taken as it stands.
     *
     * @param first index of one atom
     * @param second index of the other atom
     * @param pending bond the string asked for, {@code null} when it named none
     * @param atoms atoms of the molecule
     * @return the bond
     */
    private static Bond bondBetween(int first, int second, PendingBond pending, List<Atom> atoms) {
        if (pending == null) {
            if (atoms.get(first).isAromatic() && atoms.get(second).isAromatic()) {
                return Bond.aromatic(first, second);
            }
            return Bond.of(first, second, 1);
        }
        if (pending.aromatic) {
            return Bond.aromatic(first, second);
        }
        return Bond.of(first, second, pending.order, pending.stereo);
    }

    /**
     * Reads one atom of the organic subset, written without brackets.
     * <p>
     * Two of the ten symbols are two letters long - chlorine and bromine - so the character after an upper
     * case letter is looked at before the symbol is settled. A lower case letter is an aromatic atom of the
     * element the upper case would name, which is how the six carbons of a ring differ from six ordinary
     * ones. Anything else is a character that names no atom and is refused.
     *
     * @param smiles the string
     * @param start index of the symbol
     * @param atoms atoms of the molecule so far, the new atom is added to them
     * @return the index behind the symbol
     * @throws SmilesException when the symbol names no atom of the organic subset
     */
    private static int readBareAtom(String smiles, int start, List<Atom> atoms) {
        char symbol = smiles.charAt(start);
        if (smiles.startsWith("Cl", start)) {
            atoms.add(Atom.bare("Cl", false));
            return start + 2;
        }
        if (smiles.startsWith("Br", start)) {
            atoms.add(Atom.bare("Br", false));
            return start + 2;
        }
        switch (symbol) {
            case 'B':
                atoms.add(Atom.bare("B", false));
                return start + 1;
            case 'C':
                atoms.add(Atom.bare("C", false));
                return start + 1;
            case 'N':
                atoms.add(Atom.bare("N", false));
                return start + 1;
            case 'O':
                atoms.add(Atom.bare("O", false));
                return start + 1;
            case 'P':
                atoms.add(Atom.bare("P", false));
                return start + 1;
            case 'S':
                atoms.add(Atom.bare("S", false));
                return start + 1;
            case 'F':
                atoms.add(Atom.bare("F", false));
                return start + 1;
            case 'I':
                atoms.add(Atom.bare("I", false));
                return start + 1;
            case 'b':
                atoms.add(Atom.bare("B", true));
                return start + 1;
            case 'c':
                atoms.add(Atom.bare("C", true));
                return start + 1;
            case 'n':
                atoms.add(Atom.bare("N", true));
                return start + 1;
            case 'o':
                atoms.add(Atom.bare("O", true));
                return start + 1;
            case 'p':
                atoms.add(Atom.bare("P", true));
                return start + 1;
            case 's':
                atoms.add(Atom.bare("S", true));
                return start + 1;
            default:
                throw new SmilesException("Unknown atom symbol '" + symbol + "'", start);
        }
    }

    /**
     * Reads one atom written between brackets, with its isotope, its charge and its hydrogens.
     * <p>
     * Everything of a bracket atom is spelled out and nothing is filled in later: the isotope of a heavier
     * atom, the charge of an ion, and the exact count of the hydrogens the atom carries. A rare or heavy
     * element that is no part of the organic subset only ever arrives this way, which is why this is the
     * one place a metal - a sodium ion, a copper ion - enters a molecule.
     *
     * @param smiles the string
     * @param start index of the opening bracket
     * @param atoms atoms of the molecule so far, the new atom is added to them
     * @return the index behind the closing bracket
     * @throws SmilesException when the bracket is broken or names no element
     */
    private static int readBracketAtom(String smiles, int start, List<Atom> atoms) {
        int length = smiles.length();
        int index = start + 1;
        int isotope = 0;
        int digitsFrom = index;
        while (index < length && isRingDigit(smiles.charAt(index))) {
            index++;
        }
        if (index > digitsFrom) {
            isotope = Integer.parseInt(smiles.substring(digitsFrom, index));
        }
        if (index >= length) {
            throw new SmilesException("A bracket atom is never closed", start);
        }
        char symbol = smiles.charAt(index);
        String element;
        boolean aromatic = false;
        if (Character.isLowerCase(symbol)) {
            element = aromaticElement(symbol, index);
            aromatic = true;
            index++;
        } else if (Character.isUpperCase(symbol)) {
            int from = index;
            index++;
            if (index < length && Character.isLowerCase(smiles.charAt(index))) {
                index++;
            }
            element = smiles.substring(from, index);
            if (!Elements.isKnown(element)) {
                throw new SmilesException("Unknown element symbol '" + element + "'", from);
            }
        } else {
            throw new SmilesException("A bracket atom names no element", index);
        }
        while (index < length && smiles.charAt(index) == '@') {
            index++;
        }
        int hydrogens = 0;
        if (index < length && smiles.charAt(index) == 'H') {
            index++;
            int from = index;
            while (index < length && isRingDigit(smiles.charAt(index))) {
                index++;
            }
            hydrogens = index > from ? Integer.parseInt(smiles.substring(from, index)) : 1;
        }
        int charge = 0;
        if (index < length && (smiles.charAt(index) == '+' || smiles.charAt(index) == '-')) {
            char sign = smiles.charAt(index);
            index++;
            int magnitude = 1;
            int from = index;
            while (index < length && smiles.charAt(index) == sign) {
                index++;
            }
            if (index > from) {
                magnitude = index - from + 1;
            } else if (index < length && isRingDigit(smiles.charAt(index))) {
                from = index;
                while (index < length && isRingDigit(smiles.charAt(index))) {
                    index++;
                }
                magnitude = Integer.parseInt(smiles.substring(from, index));
            }
            charge = sign == '+' ? magnitude : -magnitude;
        }
        if (index < length && smiles.charAt(index) == ':') {
            index++;
            while (index < length && isRingDigit(smiles.charAt(index))) {
                index++;
            }
        }
        if (index >= length || smiles.charAt(index) != ']') {
            throw new SmilesException("A bracket atom is never closed", start);
        }
        atoms.add(Atom.bracketed(element, charge, isotope, aromatic, hydrogens));
        return index + 1;
    }

    /** The element an aromatic lower case symbol of a bracket atom names. */
    private static String aromaticElement(char symbol, int position) {
        switch (symbol) {
            case 'b':
                return "B";
            case 'c':
                return "C";
            case 'n':
                return "N";
            case 'o':
                return "O";
            case 'p':
                return "P";
            case 's':
                return "S";
            default:
                throw new SmilesException("Unknown aromatic atom symbol '" + symbol + "'", position);
        }
    }

    /** A bond a string asked for before the atom it holds on to was read. */
    private static final class PendingBond {

        private final int order;
        private final boolean aromatic;
        private final char stereo;

        private PendingBond(int order, boolean aromatic, char stereo) {
            this.order = order;
            this.aromatic = aromatic;
            this.stereo = stereo;
        }
    }

    /** An atom a ring number is waiting to be closed at. */
    private static final class RingOpening {

        private final int atom;
        private final PendingBond bond;

        private RingOpening(int atom, PendingBond bond) {
            this.atom = atom;
            this.bond = bond;
        }
    }
}
