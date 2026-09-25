package com.philia093.neofactory.render.model;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.philia093.neofactory.block.BlockFace;
import com.philia093.neofactory.render.FaceLayout;
import com.philia093.neofactory.render.MeshData;

import java.util.List;

/**
 * One bone of a body: a box of the model and the place it turns around.
 * <p>
 * A body is not one shape but a handful of boxes that hang on each other - a head on the neck, an arm
 * on the shoulder - and every box turns around its own joint, which is what makes a walk a walk. This
 * record is one of those boxes: the joint it turns around, the box itself, and the picture of each of
 * its faces, cut out of the skin of the body, see
 * {@link com.philia093.neofactory.render.SkinRegions}.
 * <p>
 * <b>The unit is one block.</b> The feet of a body stand on {@code y = 0} and its head reaches to
 * {@code y = 2}, which is the size the original game draws a body in; the game scales it down to the
 * height a player really fills, see {@link HumanoidModel#SCALE}.
 * <p>
 * <b>A bone carries its own mesh.</b> The faces of the box are written into one {@link MeshData} while
 * the model is built, because every bone is drawn with a matrix of its own: the mesh of a bone stands
 * around its joint, and the matrix that carries the joint is what moves it, see
 * {@link #matrixOf}.
 */
public record Bone(String name, float pivotX, float pivotY, float pivotZ, float fromX, float fromY,
        float fromZ, float toX, float toY, float toZ, List<Face> faces) {

    /**
     * One face of a bone.
     *
     * @param face face of the box
     * @param picture name of the picture in the texture array
     * @param u0 left edge of the window inside that picture
     * @param v0 upper edge of the window inside that picture
     * @param u1 right edge of the window inside that picture
     * @param v1 lower edge of the window inside that picture
     */
    public record Face(BlockFace face, String picture, float u0, float v0, float u1, float v1) {
    }

    /**
     * Builds the mesh of this bone.
     * <p>
     * Every face is written where it belongs on the box in the corner order {@link BlockFace} walks
     * in, so the winding of a bone is the winding of a block, and its picture is read with the rule of
     * {@link FaceLayout}, so the face of a body stands upright.
     *
     * @param layers layer of a picture inside the texture array
     * @return the mesh of this bone, one quad per face
     */
    public MeshData build(Layers layers) {
        MeshData mesh = new MeshData();
        float[] window = new float[2];
        for (Face face : faces) {
            int layer = layers.layerOf(face.picture());
            if (layer < 0) {
                continue;
            }
            int[] corners = new int[4];
            for (int corner = 0; corner < 4; corner++) {
                float x = face.face().cornerX(corner) == 1 ? toX : fromX;
                float y = face.face().cornerY(corner) == 1 ? toY : fromY;
                float z = face.face().cornerZ(corner) == 1 ? toZ : fromZ;
                float across = FaceLayout.across(face.face(), face.face().cornerX(corner),
                        face.face().cornerY(corner), face.face().cornerZ(corner));
                float up = FaceLayout.up(face.face(), face.face().cornerX(corner),
                        face.face().cornerY(corner), face.face().cornerZ(corner));
                FaceLayout.turn(0, across, 1.0f - up, window);
                corners[corner] = mesh.addVertex(x, y, z,
                        MathUtils.lerp(face.u0(), face.u1(), window[0]),
                        MathUtils.lerp(face.v0(), face.v1(), window[1]),
                        layer, 1.0f, 1.0f, 1.0f);
            }
            mesh.addTriangle(corners[0], corners[1], corners[2]);
            mesh.addTriangle(corners[0], corners[2], corners[3]);
        }
        return mesh;
    }

    /**
     * The matrix that carries this bone, given the matrix of the bone it hangs on.
     * <p>
     * A bone is the box around its joint: the matrix moves the joint to where it stands, turns it by
     * the pose, and moves the box back, which is what makes the box swing around the joint instead of
     * around its own middle.
     *
     * @param parent matrix of the bone this one hangs on, {@code null} for a bone on the body itself
     * @param pitchX degrees this bone is turned by around the first horizontal axis, the swing of a
     *               walk
     * @param yawY degrees this bone is turned by around the vertical axis, the turn of a head
     * @param rollZ degrees this bone is turned by around the axis it looks along
     * @param into matrix to write into
     * @return {@code into}, so calls can be chained
     */
    public Matrix4 matrixOf(Matrix4 parent, float pitchX, float yawY, float rollZ, Matrix4 into) {
        if (parent == null) {
            into.idt();
        } else {
            into.set(parent);
        }
        into.translate(pivotX, pivotY, pivotZ);
        if (yawY != 0.0f) {
            into.rotate(0.0f, 1.0f, 0.0f, yawY);
        }
        if (pitchX != 0.0f) {
            into.rotate(1.0f, 0.0f, 0.0f, pitchX);
        }
        if (rollZ != 0.0f) {
            into.rotate(0.0f, 0.0f, 1.0f, rollZ);
        }
        into.translate(-pivotX, -pivotY, -pivotZ);
        return into;
    }

    /** Tells the layer of a picture inside the texture array. */
    @FunctionalInterface
    public interface Layers {

        /**
         * Layer of one picture.
         *
         * @param picture name of the picture
         * @return the layer, or a negative value when the array does not hold it
         */
        int layerOf(String picture);
    }

    @Override
    public String toString() {
        return "Bone(" + name + ", box [" + fromX + ", " + fromY + ", " + fromZ + "] to ["
                + toX + ", " + toY + ", " + toZ + "], " + faces.size() + " faces)";
    }
}
