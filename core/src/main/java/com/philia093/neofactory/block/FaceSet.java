package com.philia093.neofactory.block;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.TreeSet;

/**
 * The pictures of one block, one per face.
 * <p>
 * Most blocks of the art pack are one picture, and a set of one picture is what the flat engine had
 * as a single {@link Block#texture()}. A block that is more than one picture - the grass with its
 * top, its shaded side and the layer the biome paints, the log with its bark and its rings, the
 * furnace with its mouth - names each face here instead.
 * <p>
 * <b>A face falls back instead of being repeated.</b> A block that looks the same from every side is
 * written once as {@link Builder#all(String)} and never again; the four sides of a block whose top
 * differs are written once as {@link Builder#side(String)}. Only the face that really differs names
 * its own picture, which is how a block of six pictures stays three lines:
 * <pre>
 * FaceSet.builder()
 *         .all("grass_side")
 *         .top("grass_top")
 *         .bottom("dirt")
 *         .overlay(BlockFace.TOP, "grass_side_overlay")
 *         .build();
 * </pre>
 * <p>
 * <b>A second layer is allowed per face.</b> {@link #overlay(BlockFace)} is the picture drawn over
 * a face, which is how the grass keeps the colour of its biome out of the shaded side below it and
 * how a fine wire of a material keeps the core it was drawn around, see
 * {@link com.philia093.neofactory.item.Item#overlayTexture()}. The overlay is drawn in its own
 * colours while the face below it takes the tint of the block.
 * <p>
 * <b>Every name is a file of {@code assets/blocks}.</b> A name that carries a folder, an extension
 * or nothing at all is refused where it is written, so a mistake is found then instead of turning
 * into a missing picture while the game runs, see {@code TextureAuditTest}.
 */
public final class FaceSet {

    /** A set that draws nothing at all, for a block the world never shows, such as air. */
    public static final FaceSet NONE = builder().build();

    private final String[] faces = new String[BlockFace.ALL.length];
    private final String[] overlays = new String[BlockFace.ALL.length];

    private FaceSet(Builder builder) {
        for (BlockFace face : BlockFace.ALL) {
            this.faces[face.ordinal()] = builder.pictureOf(face);
            this.overlays[face.ordinal()] = builder.overlayOf(face);
        }
    }

    /**
     * The picture of one face.
     *
     * @param face face to read
     * @return a name relative to {@code blocks/} without extension, or {@link Block#NO_TEXTURE} for
     *         a face that is not drawn
     */
    public String face(BlockFace face) {
        return faces[Objects.requireNonNull(face, "face").ordinal()];
    }

    /**
     * The picture drawn over one face, in its own colours.
     *
     * @param face face to read
     * @return the name of the overlay, or {@link Block#NO_TEXTURE} when the face has none
     */
    public String overlay(BlockFace face) {
        return overlays[Objects.requireNonNull(face, "face").ordinal()];
    }

    /**
     * {@code true} when a face carries a second layer, see {@link #overlay(BlockFace)}.
     *
     * @param face face to test
     * @return {@code true} when a picture is drawn over it
     */
    public boolean hasOverlay(BlockFace face) {
        return !overlay(face).isEmpty();
    }

    /** {@code true} when the world would show no picture at all for this block. */
    public boolean isEmpty() {
        for (String name : faces) {
            if (!name.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /**
     * {@code true} when every face carries the very same picture.
     * <p>
     * Such a block is drawn by the flat engine without a change, which is what lets the blocks that
     * are one picture keep working while the world grows its third axis.
     */
    public boolean isUniform() {
        String first = faces[0];
        if (first.isEmpty()) {
            return false;
        }
        for (String name : faces) {
            if (!first.equals(name)) {
                return false;
            }
        }
        return true;
    }

    /**
     * The one picture of a block whose faces are all the same.
     *
     * @return the name every face carries, or {@link Block#NO_TEXTURE} when the faces differ
     */
    public String uniformTexture() {
        return isUniform() ? faces[0] : Block.NO_TEXTURE;
    }

    /**
     * Every distinct picture this set names, sorted.
     * <p>
     * Used to check that the art of a block is really there, see {@code TextureAuditTest}.
     *
     * @return the names without repeats, empty for a block that is never drawn
     */
    public List<String> names() {
        TreeSet<String> distinct = new TreeSet<>();
        for (String name : faces) {
            if (!name.isEmpty()) {
                distinct.add(name);
            }
        }
        for (String name : overlays) {
            if (!name.isEmpty()) {
                distinct.add(name);
            }
        }
        return new ArrayList<>(distinct);
    }

    /**
     * A set of one picture, the shape a block of the flat engine had.
     *
     * @param texture name relative to {@code blocks/} without extension
     * @return a set that shows that picture on all six faces
     */
    public static FaceSet of(String texture) {
        return builder().all(texture).build();
    }

    /**
     * Starts building a set.
     *
     * @return the builder, see {@link Builder}
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String toString() {
        StringBuilder text = new StringBuilder("FaceSet(");
        boolean first = true;
        for (BlockFace face : BlockFace.ALL) {
            String name = face(face);
            if (name.isEmpty()) {
                continue;
            }
            if (!first) {
                text.append(", ");
            }
            text.append(face).append('=').append(name);
            first = false;
        }
        return text.append(')').toString();
    }

    /** Fluent builder for {@link FaceSet} instances. */
    public static final class Builder {

        private final String[] explicit = new String[BlockFace.ALL.length];
        private final String[] explicitOverlay = new String[BlockFace.ALL.length];
        private String all = Block.NO_TEXTURE;
        private String side = Block.NO_TEXTURE;
        private String sideOverlay = Block.NO_TEXTURE;

        private Builder() {
        }

        /**
         * Sets the picture of every face that does not name one of its own.
         *
         * @param texture name relative to {@code blocks/} without extension
         */
        public Builder all(String texture) {
            this.all = check(texture, "all");
            return this;
        }

        /**
         * Sets the picture of the four faces around the block.
         *
         * @param texture name relative to {@code blocks/} without extension
         */
        public Builder side(String texture) {
            this.side = check(texture, "side");
            return this;
        }

        /** Sets the picture of the face looking up. */
        public Builder top(String texture) {
            return face(BlockFace.TOP, texture);
        }

        /** Sets the picture of the face looking down. */
        public Builder bottom(String texture) {
            return face(BlockFace.BOTTOM, texture);
        }

        /** Sets the picture of the face looking north. */
        public Builder north(String texture) {
            return face(BlockFace.NORTH, texture);
        }

        /** Sets the picture of the face looking south. */
        public Builder south(String texture) {
            return face(BlockFace.SOUTH, texture);
        }

        /** Sets the picture of the face looking west. */
        public Builder west(String texture) {
            return face(BlockFace.WEST, texture);
        }

        /** Sets the picture of the face looking east. */
        public Builder east(String texture) {
            return face(BlockFace.EAST, texture);
        }

        /**
         * Sets the picture of the face a block shows to the player.
         * <p>
         * Until a block can turn, see the state system, that is the face looking south: a furnace
         * stands with its mouth towards the bottom of the screen, the way the flat engine drew it.
         *
         * @param texture name relative to {@code blocks/} without extension
         */
        public Builder front(String texture) {
            return face(BlockFace.SOUTH, texture);
        }

        /** Sets the picture of the face a block turns away from the player. */
        public Builder back(String texture) {
            return face(BlockFace.NORTH, texture);
        }

        /**
         * Sets the picture of one face.
         *
         * @param face face to name
         * @param texture name relative to {@code blocks/} without extension
         */
        public Builder face(BlockFace face, String texture) {
            Objects.requireNonNull(face, "face");
            explicit[face.ordinal()] = check(texture, face.toString());
            return this;
        }

        /**
         * Sets the picture drawn over one face, in its own colours.
         *
         * @param face face to cover
         * @param texture name relative to {@code blocks/} without extension
         */
        public Builder overlay(BlockFace face, String texture) {
            Objects.requireNonNull(face, "face");
            explicitOverlay[face.ordinal()] = check(texture, face + " overlay");
            return this;
        }

        /**
         * Sets the picture drawn over the four faces around the block.
         *
         * @param texture name relative to {@code blocks/} without extension
         */
        public Builder sideOverlay(String texture) {
            this.sideOverlay = check(texture, "side overlay");
            return this;
        }

        /** Sets the picture drawn over the face looking up. */
        public Builder topOverlay(String texture) {
            return overlay(BlockFace.TOP, texture);
        }

        /**
         * Builds the set, resolving the faces that named no picture of their own.
         *
         * @return the set, ready to be handed to a block
         */
        public FaceSet build() {
            return new FaceSet(this);
        }

        /** Picture of one face: the one it named, the one of its sides, or the one of all. */
        private String pictureOf(BlockFace face) {
            String name = explicit[face.ordinal()];
            if (name != null) {
                return name;
            }
            if (!face.isVertical() && !side.isEmpty()) {
                return side;
            }
            return all;
        }

        /** Picture drawn over one face: the one it named, the one of its sides, or none. */
        private String overlayOf(BlockFace face) {
            String name = explicitOverlay[face.ordinal()];
            if (name != null) {
                return name;
            }
            if (!face.isVertical() && !sideOverlay.isEmpty()) {
                return sideOverlay;
            }
            return Block.NO_TEXTURE;
        }

        /**
         * Checks a picture name where it is written.
         *
         * @param texture name the caller gave
         * @param where face the name belongs to, used in the message
         * @return the trimmed name
         * @throws IllegalArgumentException when the name is empty or carries a folder or extension
         */
        private static String check(String texture, String where) {
            String name = Objects.requireNonNull(texture, where).trim();
            if (name.isEmpty()) {
                throw new IllegalArgumentException("A picture of " + where + " needs a name");
            }
            if (name.indexOf('/') >= 0 || name.indexOf('.') >= 0) {
                throw new IllegalArgumentException("The picture of " + where + " is '" + name
                        + "', which is not a bare name relative to blocks/");
            }
            return name;
        }
    }
}
