package com.philia093.neofactory.block.model;

import com.philia093.neofactory.block.Block;
import com.philia093.neofactory.block.BlockFace;

import java.util.List;
import java.util.Objects;

/**
 * One face of one box of a model: which picture it shows and how it is drawn.
 * <p>
 * A world of cubes shows six faces of a block, and a block is not one picture: the grass has a
 * bright top over a side the colour of its biome is painted through, a log is bark around rings,
 * a furnace is a plain top over the mouth of its front. A model file names the picture of every
 * face of every box, and this record is one of those names together with what the mesher needs
 * to draw it.
 * <p>
 * <b>The window.</b> {@link #u0()}, {@link #v0()}, {@link #u1()} and {@link #v1()} cut the part of
 * the picture this face shows. A face that covers a whole tile names {@code 0, 0, 1, 1}, which is
 * what a picture of a block uses; a face drawn from a skin or from a sheet names the smaller
 * rectangle it lives in. {@code v0} is the upper edge and {@code v1} the lower one, because the
 * texture array holds every picture the way it is drawn - the first row of a file is the first
 * row of the picture, see {@link com.philia093.neofactory.render.BlockPictures}.
 * <p>
 * <b>The overlay.</b> {@link #overlay()} is a second picture drawn over this face. It is how the
 * grass keeps the colour of its biome out of the side below it: the side is drawn in the colours
 * of the art and the overlay of the same shape is multiplied with the tint of the block, see
 * {@link #tinted()}.
 * <p>
 * <b>The cull face.</b> {@link #cullface()} is the direction in which the face lies on the border
 * of its block. A face that names one is not drawn while a block that hides it stands on the other
 * side of that border, which is what keeps the inside of a hill out of the picture.
 *
 * @param picture picture of this face, a name relative to {@code blocks/} or a name that carries
 *                its own folder, such as {@code items/…} or {@code entity/…}
 * @param overlay second picture drawn over this face, {@link Block#NO_TEXTURE} when there is none
 * @param u0 left edge of the window inside the picture
 * @param v0 upper edge of the window inside the picture
 * @param u1 right edge of the window inside the picture
 * @param v1 lower edge of the window inside the picture
 * @param cullface direction in which this face lies on the border of its block, {@code null} for a
 *                 face inside a model that no neighbour can hide
 * @param rotation quarter turns the picture is turned by, {@code 0}, {@code 90}, {@code 180} or
 *                 {@code 270}, counter clockwise as seen from outside
 * @param tinted {@code true} when the tint of the block is multiplied over this face
 */
public record ModelFace(String picture, String overlay, float u0, float v0, float u1, float v1,
        BlockFace cullface, int rotation, boolean tinted) {

    /** The four quarter turns a picture may be turned by. */
    private static final List<Integer> ROTATIONS = List.of(0, 90, 180, 270);

    /** A face that covers its whole picture, in the colours of the art. */
    public static ModelFace of(String picture) {
        return new ModelFace(picture, Block.NO_TEXTURE, 0.0f, 0.0f, 1.0f, 1.0f, null, 0, false);
    }

    /** Checks the fields, so a broken model file fails while it is read and not while a world runs. */
    public ModelFace {
        Objects.requireNonNull(picture, "picture");
        if (picture.isEmpty()) {
            throw new IllegalArgumentException("A face of a model needs a picture");
        }
        if (picture.indexOf('.') >= 0 || picture.indexOf('/') == 0
                || picture.indexOf(' ') >= 0) {
            throw new IllegalArgumentException("The picture '" + picture
                    + "' is not a name relative to the asset root");
        }
        overlay = overlay == null ? Block.NO_TEXTURE : overlay;
        if (!overlay.isEmpty() && (overlay.indexOf('.') >= 0 || overlay.indexOf(' ') >= 0)) {
            throw new IllegalArgumentException("The overlay '" + overlay
                    + "' is not a name relative to the asset root");
        }
        if (u0 == u1 || v0 == v1) {
            throw new IllegalArgumentException("The window of '" + picture + "' is empty");
        }
        if (!ROTATIONS.contains(rotation)) {
            throw new IllegalArgumentException("A picture is turned by a quarter turn, not by "
                    + rotation + " degrees");
        }
    }

    /** {@code true} when a second picture is drawn over this face. */
    public boolean hasOverlay() {
        return !overlay.isEmpty();
    }

    /** {@code true} when a block on the other side of this face hides it. */
    public boolean isCulled() {
        return cullface != null;
    }

    /** The window together with the picture, so a broken name is reported where it is written. */
    @Override
    public String toString() {
        return "ModelFace(" + picture + (hasOverlay() ? " over " + overlay : "")
                + (isCulled() ? " culled by " + cullface : "")
                + (rotation == 0 ? "" : " turned " + rotation) + (tinted ? " tinted" : "") + ")";
    }
}
