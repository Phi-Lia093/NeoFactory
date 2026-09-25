package com.philia093.neofactory.render.model;

import com.philia093.neofactory.block.BlockFace;
import com.philia093.neofactory.render.BlockPictures;
import com.philia093.neofactory.render.SkinRegions;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The body of a humanoid: a head, a body, two arms and two legs, hanging on each other.
 * <p>
 * The boxes are the ones the original game draws a player with, and so are the joints they turn
 * around: an arm swings from the shoulder at {@code y = 1.375}, a leg from the hip at
 * {@code y = 0.75}, and a head turns on the neck at {@code y = 1.5}. The numbers are written in
 * blocks, the feet on {@code y = 0} and the top of the head at {@code y = 2}.
 * <p>
 * <b>A body is smaller than it looks.</b> The boxes make a figure of two blocks while a player fills
 * one and four fifths of one, so the whole model is drawn at {@link #SCALE}, which is the factor the
 * original game uses as well.
 * <p>
 * <b>Every face comes from the skin.</b> The picture of a face is cut out of
 * {@code assets/entity/steve.png} by {@link SkinRegions}, and the window the face reads inside that
 * picture is the rectangle the skin holds: a face of four pixels across reads the first four of the
 * sixteen columns of its layer, which is what makes one pixel of the skin one pixel of a block.
 */
public final class HumanoidModel {

    /** Factor the whole body is drawn at, the height a player really fills, see the class comment. */
    public static final float SCALE = 0.9375f;

    /** Height of the model in blocks before the scale, which is the size of a figure of cubes. */
    public static final float HEIGHT = 2.0f;

    /** Name of the bone that carries the head. */
    public static final String HEAD = "head";

    /** Name of the bone that carries the body. */
    public static final String BODY = "body";

    /** Name of the right arm, the one a view from inside a body shows. */
    public static final String ARM_RIGHT = "arm_right";

    /** Name of the left arm. */
    public static final String ARM_LEFT = "arm_left";

    /** Name of the right leg. */
    public static final String LEG_RIGHT = "leg_right";

    /** Name of the left leg. */
    public static final String LEG_LEFT = "leg_left";

    /** Y of the hand of an arm, where an item is held, in blocks. */
    public static final float HAND_Y = 0.78f;

    private final List<Bone> bones;

    private final Map<String, Bone> byName = new LinkedHashMap<>();

    private HumanoidModel(List<Bone> bones) {
        this.bones = List.copyOf(bones);
        for (Bone bone : this.bones) {
            byName.put(bone.name(), bone);
        }
    }

    /**
     * The body of the game, built from the skin of the assets.
     *
     * @return the model, with the boxes and the picture windows of every bone
     */
    public static HumanoidModel of() {
        List<Bone> bones = new ArrayList<>();
        bones.add(bone(HEAD, "head", 0.0f, 1.5f, 0.0f, -0.25f, 1.5f, -0.25f, 0.25f, 2.0f, 0.25f));
        bones.add(bone(BODY, "body", 0.0f, 1.5f, 0.0f, -0.25f, 0.75f, -0.125f, 0.25f, 1.5f, 0.125f));
        bones.add(bone(ARM_RIGHT, "arm_right", -0.3125f, 1.375f, 0.0f,
                -0.5f, 0.75f, -0.125f, -0.25f, 1.5f, 0.125f));
        bones.add(bone(ARM_LEFT, "arm_left", 0.3125f, 1.375f, 0.0f,
                0.25f, 0.75f, -0.125f, 0.5f, 1.5f, 0.125f));
        bones.add(bone(LEG_RIGHT, "leg_right", -0.11875f, 0.75f, 0.0f,
                -0.24375f, 0.0f, -0.125f, 0.00625f, 0.75f, 0.125f));
        bones.add(bone(LEG_LEFT, "leg_left", 0.11875f, 0.75f, 0.0f,
                -0.00625f, 0.0f, -0.125f, 0.24375f, 0.75f, 0.125f));
        return new HumanoidModel(bones);
    }

    /** One bone, with the picture of each of its faces read from the skin. */
    private static Bone bone(String name, String skinBone, float pivotX, float pivotY, float pivotZ,
            float fromX, float fromY, float fromZ, float toX, float toY, float toZ) {
        List<Bone.Face> faces = new ArrayList<>(BlockFace.ALL.length);
        for (BlockFace face : BlockFace.ALL) {
            int[] rect = SkinRegions.rect(skinBone, face);
            if (rect[2] > BlockPictures.TILE || rect[3] > BlockPictures.TILE) {
                throw new IllegalStateException("The face " + face + " of " + skinBone + " is "
                        + rect[2] + " by " + rect[3] + " pixels and does not fit into a picture of "
                        + BlockPictures.TILE + " by " + BlockPictures.TILE + " pixels");
            }
            float u1 = rect[2] / (float) BlockPictures.TILE;
            float v1 = rect[3] / (float) BlockPictures.TILE;
            boolean mirrored = SkinRegions.bone(skinBone).mirrored();
            faces.add(new Bone.Face(face, SkinRegions.name(skinBone, face),
                    mirrored ? u1 : 0.0f, 0.0f, mirrored ? 0.0f : u1, v1));
        }
        return new Bone(name, pivotX, pivotY, pivotZ, fromX, fromY, fromZ, toX, toY, toZ, faces);
    }

    /** Every bone of this model, in the order the body was built from them. */
    public List<Bone> bones() {
        return bones;
    }

    /**
     * One bone by name.
     *
     * @param name name such as {@link #HEAD} or {@link #ARM_RIGHT}
     * @return the bone
     * @throws IllegalArgumentException when this body has no bone of that name
     */
    public Bone bone(String name) {
        Bone bone = byName.get(name);
        if (bone == null) {
            throw new IllegalArgumentException("The body has no bone called '" + name + "'");
        }
        return bone;
    }

    @Override
    public String toString() {
        return "HumanoidModel(" + bones.size() + " bones, scaled by " + SCALE + ")";
    }
}
