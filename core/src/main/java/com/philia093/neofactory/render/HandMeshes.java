package com.philia093.neofactory.render;

import com.philia093.neofactory.block.BlockFace;

/**
 * The arm of a first-person hand, as the triangles it is drawn from.
 * <p>
 * The arm is one box of the size a skin draws it in: four pixels across, four pixels thick and twelve
 * pixels long, see {@link #WIDTH}, {@link #THICKNESS} and {@link #LENGTH}. It reaches along the
 * negative Z axis, which is the direction the eye looks, and it starts at the origin - the elbow - so
 * that the pose of the hand turns it around its own joint, see {@link HandPose}.
 * <p>
 * <b>Where the picture is.</b> The box shows the arm of the body picture: the layer the hand lives in
 * holds the sixteen by sixteen pixels of the skin that carry the arm, see
 * {@link BlockPictures#ARM_REGION}, so the sides of the box take the strip of four by twelve pixels
 * inside it and its two ends the square of four by four. The colour of a corner is the shading of the
 * face it belongs to, the same fixed shading a block of the world carries - which is what makes the
 * box read as a box and not as a bright card.
 * <p>
 * The corners are written the way {@link SectionMesher} writes them, so the arm is drawn by the very
 * shader and the very vertex layout the world uses, and a box of the world could be meshed by this
 * class as well.
 */
public final class HandMeshes {

    /** Width of the arm, in blocks: four pixels of a skin. */
    public static final float WIDTH = 4.0f / BlockPictures.TILE;

    /** Thickness of the arm, in blocks: four pixels of a skin. */
    public static final float THICKNESS = 4.0f / BlockPictures.TILE;

    /** Length of the arm into the view, in blocks: twelve pixels of a skin. */
    public static final float LENGTH = 12.0f / BlockPictures.TILE;

    /**
     * Half a texel, the gap kept between two regions of the skin.
     * <p>
     * The arm of the skin touches the pixels of the face and of the leg around it, and a spot exactly on
     * their border would let the filtering of the card reach into them: the picture is sampled a little
     * inside its own region instead.
     */
    private static final float INSET = 1.0f / (2.0f * BlockPictures.TILE);

    /** First spot across the arm inside the layer of the hand, the edge of the four pixel side. */
    private static final float SIDE_U0 = 4.0f / 16.0f + INSET;

    /** Second spot across the arm, one arm of four pixels further. */
    private static final float SIDE_U1 = 8.0f / 16.0f - INSET;

    /** First spot along the arm, the start of the twelve pixel side. */
    private static final float SIDE_V0 = 4.0f / 16.0f + INSET;

    /** Second spot along the arm, the end of the twelve pixel side. */
    private static final float SIDE_V1 = 1.0f - INSET;

    /** The square of the arm that closes one end of it. */
    private static final float[] END = {SIDE_U0, INSET, SIDE_U1, 4.0f / 16.0f - INSET};

    /** The four sides of the arm, twelve pixels long and four across. */
    private static final float[] SIDE = {SIDE_U0, SIDE_V0, SIDE_U1, SIDE_V1};

    private HandMeshes() {
        // Utility class: never instantiated.
    }

    /**
     * Meshes the arm of the player.
     * <p>
     * The box spans {@link #WIDTH} and {@link #THICKNESS} around the origin and reaches {@link #LENGTH}
     * into the view, so a pose that turns it does that around the joint at the origin.
     *
     * @param layer layer the picture of the hand lives in, see {@link BlockPictures#layer(String)}
     * @return the triangles of the arm
     */
    public static MeshData arm(int layer) {
        MeshData mesh = new MeshData();
        for (BlockFace face : BlockFace.ALL) {
            // The two ends of the arm show the square, its four sides the strip. A face is walked the
            // way the mesher walks it: corner 0 first, then along the first tangent, across the face
            // and one step along the second tangent, see BlockFace#cornerX(int).
            writeQuad(mesh, face, layer, face.isVertical() ? END : SIDE);
        }
        return mesh;
    }

    /**
     * Writes the four corners and the two triangles of one face of the arm.
     *
     * @param mesh mesh to write into
     * @param face face of the box
     * @param layer layer the picture of the hand lives in
     * @param uv spots of the picture: {@code u0, v0, u1, v1}
     */
    private static void writeQuad(MeshData mesh, BlockFace face, int layer, float[] uv) {
        int[] corners = new int[4];
        for (int corner = 0; corner < 4; corner++) {
            boolean alongFirst = corner == 1 || corner == 2;
            boolean alongSecond = corner == 2 || corner == 3;
            float shade = face.shade();
            corners[corner] = mesh.addVertex(
                    face.cornerX(corner) * WIDTH - WIDTH * 0.5f,
                    face.cornerY(corner) * THICKNESS - THICKNESS * 0.5f,
                    face.cornerZ(corner) * LENGTH - LENGTH,
                    alongFirst ? uv[2] : uv[0],
                    alongSecond ? uv[3] : uv[1],
                    layer, shade, shade, shade);
        }
        mesh.addTriangle(corners[0], corners[1], corners[2]);
        mesh.addTriangle(corners[0], corners[2], corners[3]);
    }
}
