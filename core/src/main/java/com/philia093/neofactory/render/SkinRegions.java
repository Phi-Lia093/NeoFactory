package com.philia093.neofactory.render;

import com.philia093.neofactory.block.BlockFace;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The faces of a body, cut out of the skin that draws it.
 * <p>
 * A skin is one picture in which the faces of every limb lie next to each other, unwrapped the way
 * the original game unwrapped them. A block, however, is drawn from one picture per face, so the skin
 * has to be cut: {@link #name(String, BlockFace)} is the name one face of one bone carries in the
 * texture array, and {@link #rect(String, BlockFace)} is the rectangle of the skin it is read from.
 * <p>
 * <b>The rectangle is derived, not written down.</b> A bone names the corner of the skin its picture
 * starts at and the size of its box, and the six faces follow from that with the arithmetic the
 * original game uses:
 *
 * <pre>
 * top      u + depth              .. u + depth + width        v            .. v + depth
 * bottom   u + depth + width      .. u + depth + 2 * width    v            .. v + depth
 * east     u                      .. u + depth                v + depth    .. v + depth + height
 * north    u + depth              .. u + depth + width        v + depth    .. v + depth + height
 * west     u + depth + width      .. u + depth + 2 * depth    v + depth    .. v + depth + height
 * south    u + 2 * depth + width  .. u + 2 * (depth + width)  v + depth    .. v + depth + height
 * </pre>
 *
 * The head of a skin of sixty four by thirty two pixels therefore starts at {@code 0, 0} with a box
 * of eight on every side, and its top is the square from {@code 8, 0} to {@code 16, 8} - the very
 * square that skin holds there.
 * <p>
 * <b>The left limbs are the picture of the right one, mirrored.</b> The classic skin holds one arm
 * and one leg and draws the other side by mirroring them, which is why {@link Bone#mirrored()}
 * reports the bones whose picture is read backwards.
 */
public final class SkinRegions {

    /** File the faces are cut from, relative to the asset root. */
    public static final String SKIN = "entity/steve.png";

    /** Prefix of the name every face carries in the texture array. */
    public static final String PREFIX = "entity/body";

    /** Side length of one picture of the array in pixels, also the side of a block. */
    public static final int TILE = BlockPictures.TILE;

    /**
     * One bone of a body: where its picture starts on the skin and how big its box is.
     *
     * @param name name of the bone
     * @param u column of the skin the unwrapped box starts at
     * @param v row of the skin the unwrapped box starts at
     * @param width width of the box, in pixels of the skin
     * @param height height of the box, in pixels of the skin
     * @param depth depth of the box, in pixels of the skin
     * @param mirrored {@code true} when the picture is read backwards, which the left limbs are
     */
    public record Bone(String name, int u, int v, int width, int height, int depth,
            boolean mirrored) {
    }

    /** Every bone of a body, in the order the classic skin lays them down. */
    private static final List<Bone> BONES = List.of(
            new Bone("head", 0, 0, 8, 8, 8, false),
            new Bone("body", 16, 16, 8, 12, 4, false),
            new Bone("arm_right", 40, 16, 4, 12, 4, false),
            new Bone("arm_left", 40, 16, 4, 12, 4, true),
            new Bone("leg_right", 0, 16, 4, 12, 4, false),
            new Bone("leg_left", 0, 16, 4, 12, 4, true));

    /** Bones by name. */
    private static final Map<String, Bone> BY_NAME = new LinkedHashMap<>();

    /** Every face name the array holds, in the order the bones were written down. */
    private static final List<String> NAMES = new ArrayList<>();

    static {
        for (Bone bone : BONES) {
            BY_NAME.put(bone.name(), bone);
            for (BlockFace face : BlockFace.ALL) {
                NAMES.add(name(bone.name(), face));
            }
        }
    }

    private SkinRegions() {
        // Utility class: never instantiated.
    }

    /** Every bone of a body. */
    public static List<Bone> bones() {
        return BONES;
    }

    /**
     * One bone by name.
     *
     * @param name name such as {@code "head"} or {@code "arm_right"}
     * @return the bone, or {@code null} when the body has none of that name
     */
    public static Bone bone(String name) {
        return BY_NAME.get(name);
    }

    /** Every face name the texture array has to hold, one per face of every bone. */
    public static List<String> names() {
        return List.copyOf(NAMES);
    }

    /**
     * Name one face of one bone carries in the texture array.
     *
     * @param bone name of the bone
     * @param face face of its box
     * @return the name of the picture
     */
    public static String name(String bone, BlockFace face) {
        return PREFIX + "/" + bone + "_" + face;
    }

    /** {@code true} when a name of the array is a face of a body. */
    public static boolean isSkinFace(String name) {
        return name != null && name.startsWith(PREFIX + "/");
    }

    /**
     * The rectangle of the skin one face of one bone is cut from.
     *
     * @param bone name of the bone
     * @param face face of its box
     * @return X, Y, width and height in pixels of the skin
     * @throws IllegalArgumentException when the body has no bone of that name
     */
    public static int[] rect(String bone, BlockFace face) {
        Bone found = bone(bone);
        if (found == null) {
            throw new IllegalArgumentException("A body has no bone called '" + bone + "'");
        }
        int u = found.u();
        int v = found.v();
        int width = found.width();
        int depth = found.depth();
        int height = found.height();
        return switch (face) {
            case TOP -> new int[] {u + depth, v, width, depth};
            case BOTTOM -> new int[] {u + depth + width, v, width, depth};
            case EAST -> new int[] {u, v + depth, depth, height};
            case NORTH -> new int[] {u + depth, v + depth, width, height};
            case WEST -> new int[] {u + depth + width, v + depth, depth, height};
            case SOUTH -> new int[] {u + 2 * depth + width, v + depth, width, height};
        };
    }
}
