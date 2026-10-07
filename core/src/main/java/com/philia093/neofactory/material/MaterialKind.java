package com.philia093.neofactory.material;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * What a material is made of, which is what decides the shapes it comes in.
 * <p>
 * A factory holds hundreds of materials, and writing the same list of shapes for every one of
 * them would be as repetitive as writing an item and a picture for every plate: a metal is cast,
 * rolled, drawn, cut and wound, so it comes in a dust, an ingot, a plate, a rod, a gear and the
 * rest of the trade. A kind spells that list once and every material of the kind inherits it,
 * see {@link Material.Builder#kind(MaterialKind)}.
 * <p>
 * The list of a kind is a default and never a rule: {@link Material.Builder#forms(MaterialForm...)}
 * adds a shape the kind does not know, and a material that owns none of the shapes of its kind
 * simply declares the ones it has. A form that is not declared is not registered, which is what
 * makes the game hold a single item per material and shape and not a matrix of nulls.
 */
public enum MaterialKind {

    /**
     * A metal, the material every machine of the industry is built from.
     * <p>
     * The shapes are the ones a metal works are able to make out of a molten ingot: a dust that
     * comes out of a grinder, nuggets of a small pile, plates and foils of a rolling mill, rods,
     * bolts and screws of a lathe, rings and rounds of a press, fine wire, springs, gears and a
     * rotor of a wire mill and a press.
     */
    METAL(EnumSet.of(MaterialForm.DUST, MaterialForm.SMALL_DUST, MaterialForm.TINY_DUST,
            MaterialForm.INGOT, MaterialForm.NUGGET, MaterialForm.PLATE, MaterialForm.FOIL,
            MaterialForm.ROD, MaterialForm.LONG_ROD, MaterialForm.BOLT, MaterialForm.SCREW,
            MaterialForm.RING, MaterialForm.ROUND, MaterialForm.FINE_WIRE, MaterialForm.SPRING,
            MaterialForm.SMALL_SPRING, MaterialForm.GEAR, MaterialForm.SMALL_GEAR,
            MaterialForm.ROTOR)),

    /**
     * A stone of the ground, which is ground, crushed and cut and is never cast into a machine.
     * <p>
     * A mineral is met the way it lies: the ore of it is crushed, the dust of it is what a grinder leaves,
     * and the stones of it that are worth it are cut. It is not worked - no plate, no gear, no wire comes
     * out of a stone - which is the whole difference between a mineral and a metal.
     */
    MINERAL(EnumSet.of(MaterialForm.DUST, MaterialForm.SMALL_DUST, MaterialForm.TINY_DUST,
            MaterialForm.CRUSHED, MaterialForm.GEM, MaterialForm.NUGGET)),

    /** A stone cut and polished into a jewel, which is held and not built with. */
    GEM(EnumSet.of(MaterialForm.GEM, MaterialForm.CRUSHED, MaterialForm.DUST)),

    /**
     * A polymer, drawn out of a reactor as a bar and rolled from there.
     * <p>
     * A polymer is not cast: it comes out of a vessel as a bar or a long rod and is rolled into a plate or a
     * foil, which is why a plastic is never an ingot - the shapes of this kind are the ones a workshop of
     * rubber and plastic really makes.
     */
    POLYMER(EnumSet.of(MaterialForm.ROD, MaterialForm.LONG_ROD, MaterialForm.PLATE,
            MaterialForm.FOIL, MaterialForm.DUST, MaterialForm.GEAR, MaterialForm.SMALL_GEAR));

    private final Set<MaterialForm> defaultForms;

    MaterialKind(EnumSet<MaterialForm> defaultForms) {
        this.defaultForms = Collections.unmodifiableSet(defaultForms);
    }

    /**
     * The shapes a material of this kind comes in.
     *
     * @return an unmodifiable set, in the order of {@link MaterialForm}
     */
    public Set<MaterialForm> defaultForms() {
        return defaultForms;
    }
}
