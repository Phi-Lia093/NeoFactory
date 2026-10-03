package com.philia093.neofactory.world.interaction;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * What one side of a block does: the job a player gave that side.
 * <p>
 * A block of the industry moves three things - items, fluids and the power of a line of cables - and it
 * never moves them every way at once. The steam of a boiler leaves it through one side, the water runs in
 * through another and the power of a generator leaves through a third, and which side does what is what
 * this enum names. <b>A side carries exactly one role:</b> a face that takes fluid in can not give power
 * out at the same time, so setting a role on a side takes the role that stood there away, see
 * {@code FaceConfig}.
 * <p>
 * <b>The art shows the role.</b> A side that moves something is drawn with the casing of the machine and
 * the overlay of its role over it - the stub of a pipe for a fluid, the plug of the line of cables for the
 * power, the vent of an exhaust - and a side that moves nothing keeps the picture of the machine itself,
 * see {@link FacePicture} and {@link FaceAppearance}.
 * <p>
 * <b>The front of a machine carries no role at all.</b> The side a machine looks in shows the machine
 * itself - the mouth of a furnace, the door of a boiler - and nothing is ever built against it: no pipe,
 * no cable, no hopper. {@link #NONE} is therefore the only role the front may answer with, and every
 * operation of the game refuses to put another one there, see {@code FaceConfig#set}.
 * <p>
 * <b>A role owns a picture and not a shape.</b> Every side that carries a pipe wears the very same
 * picture, whichever way the fluid runs in it: which way a line flows is read from the line itself and
 * from the arrows the grid of faces draws, see {@code FaceMark}, and the art of the pack draws one stub of
 * a pipe for the two directions.
 */
public enum FaceRole {

    /** The side moves nothing: it shows the machine and carries the picture of the machine itself. */
    NONE(""),

    /** The side a fluid runs into the block through, the mouth of a line of pipes. */
    FLUID_IN("machine_overlay/pipe"),

    /** The side a fluid leaves the block through, what a machine made or what it spent. */
    FLUID_OUT("machine_overlay/pipe"),

    /** The side the power of a line of cables is taken in through, the plug of a machine that works. */
    ENERGY_IN("machine_overlay/energy_in"),

    /** The side the power a machine makes is given out through, the plug of a generator. */
    ENERGY_OUT("machine_overlay/energy_out"),

    /**
     * The side a machine of the age of steam blows its spent steam out of.
     * <p>
     * An exhaust is a role of the world and not of a tank: the steam that leaves a machine this way is
     * gone and is caught by nothing, which is why the side carries a vent and no pipe.
     */
    EXHAUST("machine_overlay/vent"),

    /** The side an item is put into the block through, which no machine of the game offers yet. */
    ITEM_IN(""),

    /** The side the items a machine made are taken out through, which no machine offers yet. */
    ITEM_OUT("");

    /** Overlay this role is drawn with, empty for a side that moves nothing. */
    private final String overlay;

    FaceRole(String overlay) {
        this.overlay = overlay;
    }

    /**
     * Overlay this role is drawn with.
     *
     * @return the name of a picture relative to {@code blocks/}, empty when the side carries none
     */
    public String overlay() {
        return overlay;
    }

    /** {@code true} when this role wears a picture of its own over the casing of a machine. */
    public boolean hasOverlay() {
        return !overlay.isEmpty();
    }

    /** {@code true} when this side moves anything at all. */
    public boolean transfers() {
        return this != NONE;
    }

    /** {@code true} when something enters the block through this side. */
    public boolean takesIn() {
        return this == FLUID_IN || this == ENERGY_IN || this == ITEM_IN;
    }

    /** {@code true} when something leaves the block through this side. */
    public boolean givesOut() {
        return this == FLUID_OUT || this == ENERGY_OUT || this == EXHAUST || this == ITEM_OUT;
    }

    /** {@code true} when this side is a plug of the line of the power. */
    public boolean isEnergy() {
        return this == ENERGY_IN || this == ENERGY_OUT;
    }

    /** {@code true} when this side carries a pipe. */
    public boolean isFluid() {
        return this == FLUID_IN || this == FLUID_OUT;
    }

    /** {@code true} when this side carries the belt of the items. */
    public boolean isItem() {
        return this == ITEM_IN || this == ITEM_OUT;
    }

    /**
     * Every overlay a role of the game is drawn with, each of them once.
     * <p>
     * A side that is owned by a block entity is drawn with the picture of its role over the casing of the
     * block, and no model file names it: the mesher finds the layer of that picture in the array of
     * pictures, which is built from the models of the blocks and from the list this method answers, see
     * {@code BlockPictures#pictureNames}.
     *
     * @return the pictures of the roles, in the order the roles are declared in
     */
    public static List<String> overlays() {
        List<String> pictures = new ArrayList<>();
        for (FaceRole role : values()) {
            if (role.hasOverlay() && !pictures.contains(role.overlay)) {
                pictures.add(role.overlay);
            }
        }
        return List.copyOf(pictures);
    }

    @Override
    public String toString() {
        return name().toLowerCase(Locale.ROOT);
    }
}
