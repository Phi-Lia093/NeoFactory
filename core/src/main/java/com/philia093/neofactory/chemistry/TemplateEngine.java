package com.philia093.neofactory.chemistry;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Finds organic reactions by matching a template and rewriting the molecule it matched.
 * <p>
 * An organic reaction is not balanced, it is rewritten. Two molecules of the same elements are not the same
 * molecule - ethanol and dimethyl ether are both {@code C2H6O} - so nothing can be found by counting atoms,
 * and the reaction has to be written down as the shape of what goes in and the shape of what comes out, see
 * {@link ReactionTemplate}. This engine is handed those writings and does one thing with them: it looks for
 * the shape of the left side inside what stands in the vessel, and when it finds it, it bends the molecule
 * into the shape of the right side.
 * <p>
 * <b>The match is a subgraph, not a whole molecule.</b> A template of a double bond matches any molecule
 * that holds a double bond, however much else hangs off it, so the pattern of a template is looked for as a
 * piece of what is there and not as the whole of it. The atoms the pattern names are tied to the atoms they
 * matched, the atoms it does not name are left where they were, and an atom the pattern matches without a
 * number is one the reaction takes away - which is how one writing serves every molecule of a family.
 * <p>
 * <b>The rewrite only changes bonds; the hydrogens follow.</b> An atom that is carried through keeps the
 * element, the charge and the ring it had and is put back into the molecule with no hydrogens counted, so
 * that the count falls out of the bonds it really ends up with: a carbon that loses a double bond and gains
 * a single one takes on two hydrogens of its own accord and no template has to say so. That is what makes a
 * hydrogenation of any alkene one short line and still a right formula, see {@link Molecule}.
 * <p>
 * <b>A product the catalog does not hold is no product.</b> The engine writes new molecules freely, because
 * a template is a writing and not a table, but it hands a reaction over only when everything it made is a
 * substance the catalog knows: the closed world of {@link Substances} is what tells a real reaction from a
 * rewriting that happened to be arithmetically possible.
 */
public final class TemplateEngine implements ReactionEngine {

    private final Substances catalog;
    private final List<ReactionTemplate> templates;

    /**
     * Creates an engine over a catalog and a handful of writings.
     *
     * @param catalog the substances a reaction may hand over
     * @param templates the reactions this engine knows how to write
     */
    public TemplateEngine(Substances catalog, List<ReactionTemplate> templates) {
        this.catalog = Objects.requireNonNull(catalog, "catalog");
        this.templates = List.copyOf(Objects.requireNonNull(templates, "templates"));
    }

    @Override
    public List<Reaction> infer(System system) {
        if (system.content().isEmpty() || templates.isEmpty()) {
            return List.of();
        }
        List<Chemical> substances = new ArrayList<>(system.content().components().keySet());
        Substrate substrate = Substrate.of(substances);
        List<Reaction> found = new ArrayList<>();
        for (ReactionTemplate template : templates) {
            Reaction reaction = apply(template, substrate, system);
            if (reaction != null) {
                found.add(reaction);
            }
        }
        found.sort(bestFirst());
        return List.copyOf(found);
    }

    /**
     * The reaction the engine would run in a system, {@code null} when no template matched.
     *
     * @param system what stands in the vessel
     * @return the best reaction, or {@code null}
     */
    public Reaction best(System system) {
        List<Reaction> found = infer(system);
        return found.isEmpty() ? null : found.get(0);
    }

    /** More template names first is meaningless, so the answers are settled by their writing alone. */
    private Comparator<Reaction> bestFirst() {
        return (first, second) -> (first.reactants() + "->" + first.products())
                .compareTo(second.reactants() + "->" + second.products());
    }

    /** Matches the left side of a template and bends what it matched into the right side. */
    private Reaction apply(ReactionTemplate template, Substrate substrate, System system) {
        Molecule pattern = flatten(template.reactants());
        int[] mapping = new int[pattern.atomCount()];
        for (int atom = 0; atom < mapping.length; atom++) {
            mapping[atom] = -1;
        }
        boolean[] used = new boolean[substrate.molecule.atomCount()];
        if (!match(pattern, substrate.molecule, mapping, used, 0)) {
            return null;
        }
        return rewrite(template, pattern, substrate, mapping, system);
    }

    /** The substances of a pot laid out as one molecule, with the record of where each atom came from. */
    private static final class Substrate {

        private final Molecule molecule;
        private final List<Chemical> substances;
        private final int[] substanceOfAtom;

        private Substrate(Molecule molecule, List<Chemical> substances, int[] substanceOfAtom) {
            this.molecule = molecule;
            this.substances = substances;
            this.substanceOfAtom = substanceOfAtom;
        }

        static Substrate of(List<Chemical> substances) {
            List<Atom> atoms = new ArrayList<>();
            List<Bond> bonds = new ArrayList<>();
            int total = 0;
            for (Chemical chemical : substances) {
                total += chemical.structure().atomCount();
            }
            int[] substanceOfAtom = new int[total];
            int next = 0;
            for (int index = 0; index < substances.size(); index++) {
                Molecule structure = substances.get(index).structure();
                int offset = atoms.size();
                for (int atom = 0; atom < structure.atomCount(); atom++) {
                    atoms.add(rebuild(structure.atom(atom)));
                    substanceOfAtom[next++] = index;
                }
                for (Bond bond : structure.bonds()) {
                    bonds.add(move(bond, offset));
                }
            }
            return new Substrate(new Molecule(atoms, bonds), substances, substanceOfAtom);
        }
    }

    /** Lays several molecules out as one, keeping what each atom is and the map number it carries. */
    private static Molecule flatten(List<Molecule> molecules) {
        List<Atom> atoms = new ArrayList<>();
        List<Bond> bonds = new ArrayList<>();
        for (Molecule molecule : molecules) {
            int offset = atoms.size();
            for (int atom = 0; atom < molecule.atomCount(); atom++) {
                Atom value = molecule.atom(atom);
                atoms.add(Atom.rebuilt(value.element(), value.charge(), value.isotope(),
                        value.isAromatic(), value.mapClass()));
            }
            for (Bond bond : molecule.bonds()) {
                bonds.add(move(bond, offset));
            }
        }
        return new Molecule(atoms, bonds);
    }

    /** An atom of a molecule, put back with no hydrogens counted so the new bonds decide them. */
    private static Atom rebuild(Atom atom) {
        return Atom.rebuilt(atom.element(), atom.charge(), atom.isotope(), atom.isAromatic());
    }

    /** A bond of one molecule, moved to where that molecule stands in a laid out one. */
    private static Bond move(Bond bond, int offset) {
        if (bond.isAromatic()) {
            return Bond.aromatic(offset + bond.first(), offset + bond.second());
        }
        return Bond.of(offset + bond.first(), offset + bond.second(), bond.order(), bond.stereo());
    }

    /**
     * Looks for the pattern of a template inside the laid out pot, tying pattern atoms to real ones.
     * <p>
     * The atoms of the pattern are walked one at a time and each is given an atom of the pot it could be -
     * the same element, the same charge, the same ring - that no earlier pattern atom took and that agrees
     * with the bonds already tied. A pattern atom that finds none sends the walk back to the atom before it,
     * which is what lets a pattern be found as a piece of a molecule however much else hangs off it.
     *
     * @param pattern the laid out left side of the template
     * @param substrate the laid out pot
     * @param mapping where each pattern atom is tied, filled while walking
     * @param used which atoms of the pot are already spoken for
     * @param index the pattern atom to place
     * @return {@code true} when every atom of the pattern found a home
     */
    private static boolean match(Molecule pattern, Molecule substrate, int[] mapping, boolean[] used,
            int index) {
        if (index == pattern.atomCount()) {
            return true;
        }
        for (int candidate = 0; candidate < substrate.atomCount(); candidate++) {
            if (used[candidate] || !compatible(pattern.atom(index), substrate.atom(candidate))) {
                continue;
            }
            if (!classAgrees(pattern, index, mapping, candidate)
                    || !bondsAgree(pattern, substrate, index, mapping, candidate)) {
                continue;
            }
            mapping[index] = candidate;
            used[candidate] = true;
            if (match(pattern, substrate, mapping, used, index + 1)) {
                return true;
            }
            mapping[index] = -1;
            used[candidate] = false;
        }
        return false;
    }

    /** {@code true} when an atom of the pot could stand for an atom of the pattern. */
    private static boolean compatible(Atom pattern, Atom substrate) {
        return pattern.element().equals(substrate.element())
                && pattern.charge() == substrate.charge()
                && pattern.isotope() == substrate.isotope()
                && pattern.isAromatic() == substrate.isAromatic();
    }

    /** {@code true} when two atoms of one map number are not about to be tied to two different atoms. */
    private static boolean classAgrees(Molecule pattern, int index, int[] mapping, int candidate) {
        int mapClass = pattern.atom(index).mapClass();
        if (mapClass == 0) {
            return true;
        }
        for (int other = 0; other < index; other++) {
            if (mapping[other] >= 0 && pattern.atom(other).mapClass() == mapClass) {
                return mapping[other] == candidate;
            }
        }
        return true;
    }

    /** {@code true} when the bonds a pattern asks for are the bonds the pot really has. */
    private static boolean bondsAgree(Molecule pattern, Molecule substrate, int index, int[] mapping,
            int candidate) {
        for (int other = 0; other < index; other++) {
            if (mapping[other] < 0) {
                continue;
            }
            Bond wanted = bondBetween(pattern, other, index);
            if (wanted == null) {
                continue;
            }
            Bond found = bondBetween(substrate, mapping[other], candidate);
            if (found == null || found.order() != wanted.order()
                    || found.isAromatic() != wanted.isAromatic()) {
                return false;
            }
        }
        return true;
    }

    /** The bond between two atoms of a molecule, {@code null} when they share none. */
    private static Bond bondBetween(Molecule molecule, int first, int second) {
        for (int bondIndex : molecule.bondsOf(first)) {
            Bond bond = molecule.bonds().get(bondIndex);
            if (bond.other(first) == second) {
                return bond;
            }
        }
        return null;
    }

    /** A bond of a rewritten molecule, before it is built. */
    private record Link(int order, boolean aromatic) {
    }

    /**
     * Bends what a template matched into the shape of its right side.
     * <p>
     * The atoms that carry a number are carried over, the atoms the pattern matched without one are taken
     * away, and the atoms the right side names without one are built. The bonds are decided in two halves:
     * the bonds of the pot that touch an atom the reaction does not carry are kept as they stand, and every
     * bond between two carried atoms is dropped and written again from the right side - a bond the right
     * side does not name is a bond the reaction broke, which is how a double bond becomes a single one. The
     * hydrogens are not written at all and follow from the bonds that come out, see {@link Molecule}.
     *
     * @param template the template that matched
     * @param pattern the laid out left side of the template
     * @param substrate the laid out pot
     * @param mapping where each pattern atom was tied
     * @param system the vessel the reaction runs in
     * @return the reaction, or {@code null} when it hands over a substance the catalog does not hold
     */
    private Reaction rewrite(ReactionTemplate template, Molecule pattern, Substrate substrate,
            int[] mapping, System system) {
        Molecule product = flatten(template.products());
        boolean[] carried = new boolean[substrate.molecule.atomCount()];
        boolean[] removed = new boolean[substrate.molecule.atomCount()];
        Map<Integer, Integer> byClass = new HashMap<>();
        for (int atom = 0; atom < pattern.atomCount(); atom++) {
            int mapClass = pattern.atom(atom).mapClass();
            if (mapClass == 0) {
                removed[mapping[atom]] = true;
            } else {
                byClass.put(mapClass, mapping[atom]);
                carried[mapping[atom]] = true;
            }
        }
        for (int atom = 0; atom < product.atomCount(); atom++) {
            int mapClass = product.atom(atom).mapClass();
            if (mapClass != 0 && !byClass.containsKey(mapClass)) {
                return null;
            }
        }
        List<Atom> atoms = new ArrayList<>();
        int[] outputOfSubstrate = new int[substrate.molecule.atomCount()];
        for (int atom = 0; atom < outputOfSubstrate.length; atom++) {
            outputOfSubstrate[atom] = -1;
        }
        for (int atom = 0; atom < substrate.molecule.atomCount(); atom++) {
            if (removed[atom]) {
                continue;
            }
            outputOfSubstrate[atom] = atoms.size();
            atoms.add(rebuild(substrate.molecule.atom(atom)));
        }
        int[] outputOfProduct = new int[product.atomCount()];
        for (int atom = 0; atom < product.atomCount(); atom++) {
            int mapClass = product.atom(atom).mapClass();
            if (mapClass == 0) {
                outputOfProduct[atom] = atoms.size();
                atoms.add(rebuild(product.atom(atom)));
            } else {
                outputOfProduct[atom] = outputOfSubstrate[byClass.get(mapClass)];
            }
        }
        Map<Long, Link> links = new LinkedHashMap<>();
        for (Bond bond : substrate.molecule.bonds()) {
            if (removed[bond.first()] || removed[bond.second()]
                    || (carried[bond.first()] && carried[bond.second()])) {
                continue;
            }
            link(links, outputOfSubstrate[bond.first()], outputOfSubstrate[bond.second()], bond);
        }
        for (Bond bond : product.bonds()) {
            link(links, outputOfProduct[bond.first()], outputOfProduct[bond.second()], bond);
        }
        List<Bond> bonds = new ArrayList<>();
        for (Map.Entry<Long, Link> entry : links.entrySet()) {
            int first = (int) (entry.getKey() >>> 32);
            int second = (int) (long) entry.getKey();
            Link value = entry.getValue();
            bonds.add(value.aromatic() ? Bond.aromatic(first, second)
                    : Bond.of(first, second, value.order()));
        }
        List<Chemical> products = split(new Molecule(atoms, bonds));
        for (Chemical chemical : products) {
            if (!catalog.holds(chemical)) {
                return null;
            }
        }
        Mixture result = Mixture.empty();
        for (Chemical chemical : products) {
            result = result.plus(Mixture.of(chemical, 1));
        }
        Reaction reaction = Reaction.of(matched(pattern, substrate, mapping), result, 0,
                system.background());
        return Conservation.balanced(reaction) ? reaction : null;
    }

    /** Writes one bond of a rewritten molecule into the set, the lower atom first. */
    private static void link(Map<Long, Link> links, int first, int second, Bond bond) {
        if (first == second) {
            return;
        }
        int low = Math.min(first, second);
        int high = Math.max(first, second);
        links.putIfAbsent(((long) low << 32) | (high & 0xffffffffL),
                new Link(bond.order(), bond.isAromatic()));
    }

    /** The substances the pattern really reached, one of each. */
    private static Mixture matched(Molecule pattern, Substrate substrate, int[] mapping) {
        Set<Chemical> reached = new LinkedHashSet<>();
        for (int atom = 0; atom < pattern.atomCount(); atom++) {
            reached.add(substrate.substances.get(substrate.substanceOfAtom[mapping[atom]]));
        }
        Mixture reactants = Mixture.empty();
        for (Chemical chemical : reached) {
            reactants = reactants.plus(Mixture.of(chemical, 1));
        }
        return reactants;
    }

    /** Splits a rewritten molecule into the substances it turned out to be. */
    private static List<Chemical> split(Molecule written) {
        boolean[] seen = new boolean[written.atomCount()];
        List<Chemical> chemicals = new ArrayList<>();
        for (int start = 0; start < written.atomCount(); start++) {
            if (seen[start]) {
                continue;
            }
            List<Integer> group = new ArrayList<>();
            Deque<Integer> pending = new ArrayDeque<>();
            pending.push(start);
            seen[start] = true;
            while (!pending.isEmpty()) {
                int atom = pending.pop();
                group.add(atom);
                for (int neighbour : written.neighbours(atom)) {
                    if (!seen[neighbour]) {
                        seen[neighbour] = true;
                        pending.push(neighbour);
                    }
                }
            }
            Collections.sort(group);
            chemicals.add(Chemical.of(component(written, group)));
        }
        return chemicals;
    }

    /** One piece of a rewritten molecule, cut out as a molecule of its own. */
    private static Molecule component(Molecule written, List<Integer> group) {
        Map<Integer, Integer> renumbered = new HashMap<>();
        List<Atom> atoms = new ArrayList<>(group.size());
        for (int index = 0; index < group.size(); index++) {
            renumbered.put(group.get(index), index);
            atoms.add(rebuild(written.atom(group.get(index))));
        }
        List<Bond> bonds = new ArrayList<>();
        for (Bond bond : written.bonds()) {
            Integer first = renumbered.get(bond.first());
            Integer second = renumbered.get(bond.second());
            if (first == null || second == null) {
                continue;
            }
            bonds.add(bond.isAromatic() ? Bond.aromatic(first, second)
                    : Bond.of(first, second, bond.order()));
        }
        return new Molecule(atoms, bonds);
    }
}
