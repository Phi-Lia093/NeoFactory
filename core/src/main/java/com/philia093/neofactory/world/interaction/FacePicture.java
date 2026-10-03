package com.philia093.neofactory.world.interaction;

import java.util.Objects;

/**
 * The two pictures one side of a block is drawn with when a block entity owns that side.
 * <p>
 * A block entity that carries items, fluids or power decides what its sides look like: a side that moves
 * something is drawn with the casing of the block and the overlay of the job of that side, and a side that
 * moves nothing keeps the picture of the model, see {@link FaceRole}. This record is that answer - a
 * picture and the second picture drawn over it - and the mesher writes both over the whole face, a hair
 * apart, the way the layer of a biome colour is written over the side of the grass, see
 * {@code SectionMesher}.
 * <p>
 * <b>The pictures cover the whole face.</b> A side owned by a block entity is a side of the machine itself
 * and not a piece of a sheet, so the window of both pictures is the whole tile and no model file is
 * written for it.
 *
 * @param texture picture of the side, a name relative to {@code blocks/}, such as the casing of a tier
 * @param overlay second picture drawn over it, empty when the side carries none
 */
public record FacePicture(String texture, String overlay) {

    /** Checks the names, so a broken one fails where it is written and not while a world runs. */
    public FacePicture {
        Objects.requireNonNull(texture, "texture");
        if (texture.isEmpty()) {
            throw new IllegalArgumentException("A side of a block needs a picture");
        }
        check(texture, "picture");
        overlay = overlay == null ? "" : overlay;
        if (!overlay.isEmpty()) {
            check(overlay, "overlay");
        }
    }

    /** The casing of a block with nothing over it, what a side that moves nothing is drawn with. */
    public static FacePicture of(String texture) {
        return new FacePicture(texture, "");
    }

    /**
     * The casing of a block with the overlay of what one of its sides does over it.
     *
     * @param texture picture of the side, normally the casing of the machine
     * @param role job of that side, see {@link FaceRole}
     * @return the two pictures
     */
    public static FacePicture of(String texture, FaceRole role) {
        return new FacePicture(texture, Objects.requireNonNull(role, "role").overlay());
    }

    /** {@code true} when a second picture is drawn over this side. */
    public boolean hasOverlay() {
        return !overlay.isEmpty();
    }

    /** A picture name is a name relative to the asset root and never a path of a file. */
    private static void check(String picture, String what) {
        if (picture.indexOf('.') >= 0 || picture.indexOf(' ') >= 0 || picture.startsWith("/")) {
            throw new IllegalArgumentException("The " + what + " '" + picture
                    + "' is not a name relative to the asset root");
        }
    }

    @Override
    public String toString() {
        return "FacePicture(" + texture + (hasOverlay() ? " over " + overlay : "") + ")";
    }
}
