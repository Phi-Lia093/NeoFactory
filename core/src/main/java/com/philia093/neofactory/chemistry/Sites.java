package com.philia093.neofactory.chemistry;

import java.util.ArrayList;
import java.util.List;

/**
 * Finds the functional groups of a molecule, the places a rule of a reaction is written against.
 * <p>
 * Nothing here is looked up and nothing is guessed from a name: a carbonyl is a carbon held to an oxygen by
 * a double bond, an ester is such a carbonyl whose carbon also holds an oxygen that holds a carbon, and a
 * hydroxyl is an oxygen that carries a hydrogen. Every finder therefore walks the bonds of the molecule and
 * answers with the atoms it found, in a settled order a rule may read, see {@link Site}.
 */
public final class Sites {

    private Sites() {
        // Utility class: never instantiated.
    }

    /**
     * Every group a molecule holds.
     *
     * @param molecule molecule to read
     * @return the sites, in the order the finders write them
     */
    public static List<Site> all(Molecule molecule) {
        List<Site> sites = new ArrayList<>();
        sites.addAll(alkenes(molecule));
        sites.addAll(alkynes(molecule));
        sites.addAll(carbonyls(molecule));
        sites.addAll(aldehydes(molecule));
        sites.addAll(ketones(molecule));
        sites.addAll(acids(molecule));
        sites.addAll(esters(molecule));
        sites.addAll(hydroxyls(molecule));
        sites.addAll(halides(molecule));
        sites.addAll(alkyls(molecule));
        sites.addAll(nitriles(molecule));
        sites.addAll(amines(molecule));
        sites.addAll(aromaticRings(molecule));
        return sites;
    }

    /**
     * The nitrogens that carry a hydrogen and hang on a carbon, the amines of the organic side.
     *
     * @param molecule molecule to read
     * @return the sites, the nitrogen first and the carbon it hangs on second
     */
    public static List<Site> amines(Molecule molecule) {
        List<Site> sites = new ArrayList<>();
        for (int atom = 0; atom < molecule.atomCount(); atom++) {
            if (!isNitrogen(molecule, atom) || molecule.atom(atom).charge() != 0
                    || !carriesHydrogen(molecule, atom)) {
                continue;
            }
            int carbon = carbonOn(molecule, atom);
            if (carbon < 0) {
                continue;
            }
            sites.add(new Site(FunctionalGroup.AMINE, List.of(atom, carbon)));
        }
        return sites;
    }

    /**
     * The plain carbon to hydrogen bonds of a molecule, the hydrogens a radical reaction takes away.
     *
     * @param molecule molecule to read
     * @return the sites, the carbon first and the hydrogen second
     */
    public static List<Site> alkyls(Molecule molecule) {
        List<Site> sites = new ArrayList<>();
        for (Bond bond : molecule.bonds()) {
            if (bond.order() != 1 || !isCarbon(molecule, bond.first())
                    || !molecule.atom(bond.second()).element().equals("H")
                    || carriesMultipleBond(molecule, bond.first())) {
                continue;
            }
            sites.add(new Site(FunctionalGroup.ALKYL, List.of(bond.first(), bond.second())));
        }
        return sites;
    }

    /** {@code true} when an atom hangs on a bond of more than the first order. */
    private static boolean carriesMultipleBond(Molecule molecule, int atom) {
        for (Bond bond : molecule.bonds()) {
            if (bond.order() > 1 && bond.touches(atom)) {
                return true;
            }
        }
        return false;
    }

    /**
     * The double bonds between two carbons.
     *
     * @param molecule molecule to read
     * @return one site per alkene, the two carbons in the order the bond was written
     */
    public static List<Site> alkenes(Molecule molecule) {
        List<Site> sites = new ArrayList<>();
        for (Bond bond : molecule.bonds()) {
            if (bond.order() == 2 && !bond.isAromatic()
                    && isCarbon(molecule, bond.first()) && isCarbon(molecule, bond.second())) {
                sites.add(new Site(FunctionalGroup.ALKENE, List.of(bond.first(), bond.second())));
            }
        }
        return sites;
    }

    /**
     * The triple bonds between two carbons.
     *
     * @param molecule molecule to read
     * @return one site per alkyne, the two carbons in the order the bond was written
     */
    public static List<Site> alkynes(Molecule molecule) {
        List<Site> sites = new ArrayList<>();
        for (Bond bond : molecule.bonds()) {
            if (bond.order() == 3 && isCarbon(molecule, bond.first())
                    && isCarbon(molecule, bond.second())) {
                sites.add(new Site(FunctionalGroup.ALKYNE, List.of(bond.first(), bond.second())));
            }
        }
        return sites;
    }

    /**
     * The carbons held to an oxygen by a double bond.
     *
     * @param molecule molecule to read
     * @return one site per carbonyl, the carbon first and the oxygen second
     */
    public static List<Site> carbonyls(Molecule molecule) {
        List<Site> sites = new ArrayList<>();
        for (Bond bond : molecule.bonds()) {
            if (bond.order() != 2 || bond.isAromatic()) {
                continue;
            }
            int first = bond.first();
            int second = bond.second();
            if (isOxygen(molecule, second) && !isOxygen(molecule, first)) {
                sites.add(new Site(FunctionalGroup.CARBONYL, List.of(first, second)));
            } else if (isOxygen(molecule, first) && !isOxygen(molecule, second)) {
                sites.add(new Site(FunctionalGroup.CARBONYL, List.of(second, first)));
            }
        }
        return sites;
    }

    /**
     * The carbonyls whose carbon carries a hydrogen, the carbonyl of an aldehyde.
     *
     * @param molecule molecule to read
     * @return the sites, the carbon first and the oxygen second
     */
    public static List<Site> aldehydes(Molecule molecule) {
        List<Site> sites = new ArrayList<>();
        for (Site carbonyl : carbonyls(molecule)) {
            if (hydrogensOn(molecule, carbonyl.atom(0)) > 0) {
                sites.add(new Site(FunctionalGroup.ALDEHYDE, carbonyl.atoms()));
            }
        }
        return sites;
    }

    /**
     * The carbonyls whose carbon carries two carbons, the carbonyl of a ketone.
     *
     * @param molecule molecule to read
     * @return the sites, the carbon first and the oxygen second
     */
    public static List<Site> ketones(Molecule molecule) {
        List<Site> sites = new ArrayList<>();
        for (Site carbonyl : carbonyls(molecule)) {
            int carbons = 0;
            for (int neighbour : molecule.neighbours(carbonyl.atom(0))) {
                if (isCarbon(molecule, neighbour)) {
                    carbons++;
                }
            }
            if (carbons == 2) {
                sites.add(new Site(FunctionalGroup.KETONE, carbonyl.atoms()));
            }
        }
        return sites;
    }

    /**
     * The carbonyls whose carbon carries a hydroxyl, the acids of the organic side.
     *
     * @param molecule molecule to read
     * @return the sites, the carbonyl carbon, the carbonyl oxygen and the hydroxyl oxygen in that order
     */
    public static List<Site> acids(Molecule molecule) {
        List<Site> sites = new ArrayList<>();
        for (Site carbonyl : carbonyls(molecule)) {
            int oxygen = hydroxylOn(molecule, carbonyl.atom(0));
            if (oxygen >= 0) {
                sites.add(new Site(FunctionalGroup.CARBOXYLIC_ACID,
                        List.of(carbonyl.atom(0), carbonyl.atom(1), oxygen)));
            }
        }
        return sites;
    }

    /**
     * The carbonyls whose carbon carries an oxygen that carries a carbon, the esters.
     *
     * @param molecule molecule to read
     * @return the sites, the carbonyl carbon, the carbonyl oxygen, the ester oxygen and its carbon
     */
    public static List<Site> esters(Molecule molecule) {
        List<Site> sites = new ArrayList<>();
        for (Site carbonyl : carbonyls(molecule)) {
            int carbon = carbonyl.atom(0);
            for (int neighbour : molecule.neighbours(carbon)) {
                if (!isOxygen(molecule, neighbour) || neighbour == carbonyl.atom(1)) {
                    continue;
                }
                int alkyl = carbonOn(molecule, neighbour, carbonyl.atom(0));
                if (alkyl >= 0) {
                    sites.add(new Site(FunctionalGroup.ESTER,
                            List.of(carbon, carbonyl.atom(1), neighbour, alkyl)));
                    break;
                }
            }
        }
        return sites;
    }

    /**
     * The oxygens that carry a hydrogen, an alcohol or the acid of an alcohol.
     *
     * @param molecule molecule to read
     * @return the sites, the oxygen first and the atom it hangs on second
     */
    public static List<Site> hydroxyls(Molecule molecule) {
        List<Site> sites = new ArrayList<>();
        for (int atom = 0; atom < molecule.atomCount(); atom++) {
            if (!isOxygen(molecule, atom) || !carriesHydrogen(molecule, atom)) {
                continue;
            }
            int carbon = carbonOn(molecule, atom);
            if (carbon < 0) {
                continue;
            }
            sites.add(new Site(FunctionalGroup.HYDROXYL, List.of(atom, carbon)));
        }
        return sites;
    }

    /** {@code true} when an atom carries a hydrogen, spelled out as an atom or left to its valence. */
    private static boolean carriesHydrogen(Molecule molecule, int atom) {
        return hydrogensOn(molecule, atom) > 0;
    }

    /**
     * How many hydrogens hang on an atom, written out as atoms or left to its valence.
     * <p>
     * A molecule that stands in a vessel has had its hydrogens written out, so that an arrow may name one,
     * see {@link Assemblies#materialize}; a molecule that was read from a string has them left to the valence
     * instead. A finder that wants to know how plain an atom is cannot tell the two apart, so it asks here.
     *
     * @param molecule molecule to read
     * @param atom index of the atom
     * @return the number of hydrogens, however they are written
     */
    public static int hydrogensOn(Molecule molecule, int atom) {
        int count = molecule.atom(atom).hydrogens();
        for (int neighbour : molecule.neighbours(atom)) {
            if (molecule.atom(neighbour).element().equals("H")) {
                count++;
            }
        }
        return count;
    }

    /**
     * The carbons that carry a halogen.
     *
     * @param molecule molecule to read
     * @return the sites, the carbon first and the halogen second
     */
    public static List<Site> halides(Molecule molecule) {
        List<Site> sites = new ArrayList<>();
        for (Bond bond : molecule.bonds()) {
            if (isCarbon(molecule, bond.first()) && isHalogen(molecule, bond.second())) {
                sites.add(new Site(FunctionalGroup.HALIDE, List.of(bond.first(), bond.second())));
            } else if (isHalogen(molecule, bond.first()) && isCarbon(molecule, bond.second())) {
                sites.add(new Site(FunctionalGroup.HALIDE, List.of(bond.second(), bond.first())));
            }
        }
        return sites;
    }

    /**
     * The carbons held to a nitrogen by a triple bond.
     *
     * @param molecule molecule to read
     * @return the sites, the carbon first and the nitrogen second
     */
    public static List<Site> nitriles(Molecule molecule) {
        List<Site> sites = new ArrayList<>();
        for (Bond bond : molecule.bonds()) {
            if (bond.order() != 3) {
                continue;
            }
            if (isCarbon(molecule, bond.first()) && isNitrogen(molecule, bond.second())) {
                sites.add(new Site(FunctionalGroup.NITRILE, List.of(bond.first(), bond.second())));
            } else if (isNitrogen(molecule, bond.first()) && isCarbon(molecule, bond.second())) {
                sites.add(new Site(FunctionalGroup.NITRILE, List.of(bond.second(), bond.first())));
            }
        }
        return sites;
    }

    /** A hydroxyl oxygen that hangs on a carbon and on nothing else but its hydrogens, or {@code -1}. */
    private static int hydroxylOn(Molecule molecule, int atom) {
        for (int neighbour : molecule.neighbours(atom)) {
            if (!isOxygen(molecule, neighbour) || !carriesHydrogen(molecule, neighbour)) {
                continue;
            }
            boolean onlyHydrogens = true;
            for (int other : molecule.neighbours(neighbour)) {
                if (other != atom && !molecule.atom(other).element().equals("H")) {
                    onlyHydrogens = false;
                    break;
                }
            }
            if (onlyHydrogens) {
                return neighbour;
            }
        }
        return -1;
    }

    /** A carbon that hangs on an atom and is not the one named, or {@code -1}. */
    private static int carbonOn(Molecule molecule, int atom, int except) {
        for (int neighbour : molecule.neighbours(atom)) {
            if (neighbour != except && isCarbon(molecule, neighbour)) {
                return neighbour;
            }
        }
        return -1;
    }

    /** A carbon that hangs on an atom, or {@code -1}. */
    private static int carbonOn(Molecule molecule, int atom) {
        return carbonOn(molecule, atom, -1);
    }

    /**
     * The rings of a molecule whose bonds are all the blurred ones of an aromatic ring.
     *
     * @param molecule molecule to read
     * @return one site per ring, the atoms of it in bond order
     */
    public static List<Site> aromaticRings(Molecule molecule) {
        List<Site> sites = new ArrayList<>();
        for (List<Integer> ring : Rings.cycles(molecule)) {
            boolean aromatic = true;
            for (int step = 0; step < ring.size(); step++) {
                Bond bond = bondBetween(molecule, ring.get(step), ring.get((step + 1) % ring.size()));
                if (bond == null || !bond.isAromatic()) {
                    aromatic = false;
                    break;
                }
            }
            if (aromatic) {
                sites.add(new Site(FunctionalGroup.AROMATIC_RING, ring));
            }
        }
        return sites;
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

    /** {@code true} when an atom of a molecule is a nitrogen. */
    private static boolean isNitrogen(Molecule molecule, int atom) {
        return molecule.atom(atom).element().equals("N");
    }

    /** {@code true} when an atom of a molecule is a carbon. */
    private static boolean isCarbon(Molecule molecule, int atom) {
        return molecule.atom(atom).element().equals("C");
    }

    /** {@code true} when an atom of a molecule is an oxygen. */
    private static boolean isOxygen(Molecule molecule, int atom) {
        return molecule.atom(atom).element().equals("O");
    }

    /** {@code true} when an atom of a molecule is a halogen. */
    private static boolean isHalogen(Molecule molecule, int atom) {
        switch (molecule.atom(atom).element()) {
            case "F":
            case "Cl":
            case "Br":
            case "I":
                return true;
            default:
                return false;
        }
    }
}
