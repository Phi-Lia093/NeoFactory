package com.philia093.neofactory.render;

import com.badlogic.gdx.graphics.Color;
import com.philia093.neofactory.block.Block;
import com.philia093.neofactory.block.BlockFace;
import com.philia093.neofactory.block.BlockRegistry;
import com.philia093.neofactory.world.Section;

import java.util.ArrayList;
import java.util.List;

/**
 * Turns one section of a chunk into the triangles that draw it.
 * <p>
 * A world of cubes shows no more than the faces that are seen, so the mesher walks the cells of a
 * section and writes a quad only where a face looks into something that does not hide it - the empty
 * air, a fluid or a picture one can see through. A block that stands in the middle of stone therefore
 * costs nothing at all, which is what keeps a chunk of a mined world cheap to draw.
 * <p>
 * <b>What a corner carries.</b> The position is a world coordinate, so the mesh of a section needs no
 * model matrix: the origin of the section is added while the corner is written. The picture comes from
 * {@link Block#faces()} and its layer from the texture array, see {@link Pictures}. The colour is the
 * tint of the block, darkened by the fixed shading of the face - see {@link BlockFace#shade()} - and by
 * the shadow its neighbours drop into the corner, which is the ambient occlusion that makes an inside
 * corner read as one.
 * <p>
 * <b>A build hands out a list of meshes.</b> Sixteen bit indices address sixty five thousand five
 * hundred and thirty five corners and a section can show more, so a build that runs out of room starts
 * the next mesh instead of failing, see {@link MeshData}.
 * <p>
 * <b>An animated block is drawn with one frame.</b> The mesher writes the picture it is handed as a
 * whole face, so water and lava show the first frame of their sheet until the layer of a frame is
 * looked up per tick, see {@link BlockPictures}.
 */
public final class SectionMesher {

    /**
     * The blocks a mesh is built from: the section itself and the shell of cells around it.
     * <p>
     * A coordinate is local to the section and reaches from {@code -1} to {@link Section#SIZE}, because
     * the face of a block at the border is decided by the cell on the other side of it. The caller adds
     * the origin of the section, so the mesher never talks to a world.
     */
    @FunctionalInterface
    public interface Blocks {

        /**
         * Returns a block next to or inside the section.
         *
         * @param x local X coordinate, {@code -1} to {@link Section#SIZE}
         * @param y local Y coordinate, {@code -1} to {@link Section#SIZE}
         * @param z local Z coordinate, {@code -1} to {@link Section#SIZE}
         * @return the block there, never {@code null}, air for a cell that holds nothing
         */
        Block blockAt(int x, int y, int z);
    }

    /** Tells the layer a picture lives in inside the texture array. */
    @FunctionalInterface
    public interface Pictures {

        /**
         * Layer of a picture.
         *
         * @param picture name of the picture, relative to {@code blocks/} without extension
         * @return the layer, or a negative value when the picture is not in the array
         */
        int layer(String picture);
    }

    /**
     * Brightness a corner keeps for the number of occluders around it.
     * <p>
     * Three of them leave the corner in the open, two shade it a little, one more and none is what an
     * inside corner of a room looks like. Two occluders that stand side by side close the corner
     * completely, which is why that case is kept apart from the count.
     */
    private static final float[] OCCLUSION = {0.55f, 0.70f, 0.85f, 1.00f};

    private SectionMesher() {
        // Utility class: never instantiated.
    }

    /**
     * Meshes the cells of a section.
     *
     * @param section section to mesh
     * @param originX world X coordinate of the column of the section
     * @param originY world Y coordinate the section starts at
     * @param originZ world Z coordinate of the column of the section
     * @param blocks blocks of the section and of the shell around it
     * @param pictures layers the pictures live in
     * @return the meshes to draw, empty when the section shows nothing
     */
    public static List<MeshData> build(Section section, int originX, int originY, int originZ,
            Blocks blocks, Pictures pictures) {
        // The blocks around the section are read once and kept as one flag per cell: whether that cell
        // hides the face of the block behind it. The mesher asks that question thousands of times - once
        // per face and three times per corner of a face - and every single ask used to be a read of the
        // world, which made a section cost more than the whole rest of a frame.
        boolean[] hiding = hidingAround(blocks);
        List<MeshData> meshes = new ArrayList<>();
        MeshData mesh = new MeshData();
        for (int x = 0; x < Section.SIZE; x++) {
            for (int y = 0; y < Section.SIZE; y++) {
                for (int z = 0; z < Section.SIZE; z++) {
                    Block block = BlockRegistry.byId(section.rawId(x, y, z));
                    if (!block.isDrawable()) {
                        continue;
                    }
                    for (BlockFace face : BlockFace.ALL) {
                        if (hiding(hiding, x + face.x(), y + face.y(), z + face.z())) {
                            continue;
                        }
                        String picture = block.faces().face(face);
                        if (picture.isEmpty()) {
                            continue;
                        }
                        int layer = pictures.layer(picture);
                        if (layer < 0) {
                            continue;
                        }
                        if (mesh.isFull()) {
                            meshes.add(mesh);
                            mesh = new MeshData();
                        }
                        addFace(mesh, block, face, layer, x, y, z, originX, originY, originZ, hiding);
                    }
                }
            }
        }
        if (!mesh.isEmpty()) {
            meshes.add(mesh);
        }
        return meshes;
    }

    /**
     * Reads the cell of the shell around the section once, as the one question the mesher asks over and
     * over, see {@link #build}.
     *
     * @param blocks blocks of the section and of the shell around it
     * @return one flag per cell of the shell, laid out by {@link #shellIndex(int, int, int)}
     */
    private static boolean[] hidingAround(Blocks blocks) {
        boolean[] hiding = new boolean[SHELL * SHELL * SHELL];
        for (int x = -1; x <= Section.SIZE; x++) {
            for (int y = -1; y <= Section.SIZE; y++) {
                for (int z = -1; z <= Section.SIZE; z++) {
                    hiding[shellIndex(x, y, z)] = hides(blocks.blockAt(x, y, z));
                }
            }
        }
        return hiding;
    }

    /** {@code true} when the cell of the shell at a local coordinate hides what is behind it. */
    private static boolean hiding(boolean[] hiding, int x, int y, int z) {
        return hiding[shellIndex(x, y, z)];
    }

    /** Index of a cell of the shell, whose centre is the cell {@code (0, 0, 0)} of the section. */
    private static int shellIndex(int x, int y, int z) {
        return ((x + 1) * SHELL + (y + 1)) * SHELL + (z + 1);
    }

    /**
     * Writes the four corners of one face and the two triangles between them.
     *
     * @param mesh mesh to write into
     * @param block block the face belongs to
     * @param face face to write
     * @param layer layer the picture of the face lives in
     * @param x local X coordinate of the block
     * @param y local Y coordinate of the block
     * @param z local Z coordinate of the block
     * @param originX world X coordinate of the column of the section
     * @param originY world Y coordinate the section starts at
     * @param originZ world Z coordinate of the column of the section
     * @param blocks blocks around the face, needed for the shadow of a corner
     */
    private static void addFace(MeshData mesh, Block block, BlockFace face, int layer, int x, int y,
            int z, int originX, int originY, int originZ, boolean[] hiding) {
        Color tint = block.tint();
        int[] corners = new int[4];
        for (int corner = 0; corner < 4; corner++) {
            float light = face.shade() * occlusion(hiding, x, y, z, face, corner);
            corners[corner] = mesh.addVertex(
                    originX + x + face.cornerX(corner),
                    originY + y + face.cornerY(corner),
                    originZ + z + face.cornerZ(corner),
                    u(corner), v(face, corner), layer,
                    tint.r * light, tint.g * light, tint.b * light);
        }
        // The corners are walked counter clockwise as seen from outside the block, so both triangles
        // face the viewer with the winding a graphics card keeps.
        mesh.addTriangle(corners[0], corners[1], corners[2]);
        mesh.addTriangle(corners[0], corners[2], corners[3]);
    }

    /**
     * Texture coordinate of one corner across the picture.
     *
     * @param corner corner of the face, {@code 0} to {@code 3}
     * @return {@code 0} for the two corners at the first tangent, {@code 1} for the two across it
     */
    private static float u(int corner) {
        return corner == 1 || corner == 2 ? 1.0f : 0.0f;
    }

    /**
     * Texture coordinate of one corner up the picture.
     * <p>
     * A face that stands upright is drawn with the top of its picture at the top of the block: the first
     * row of a picture is the coordinate zero of the shader and that row is its top, so the upper
     * corners take the smaller coordinate. A face that looks up or down is drawn as the picture lies,
     * which keeps a floor of grass the way its author drew it.
     *
     * @param face face the corner belongs to
     * @param corner corner of the face, {@code 0} to {@code 3}
     * @return the coordinate, {@code 0} or {@code 1}
     */
    private static float v(BlockFace face, int corner) {
        boolean upper = corner == 2 || corner == 3;
        if (face.isVertical()) {
            return upper ? 0.0f : 1.0f;
        }
        return upper ? 1.0f : 0.0f;
    }

    /**
     * How much light reaches one corner of a face.
     * <p>
     * The three cells that touch the corner outside the face decide it: the two beside the corner and
     * the one across it. A block that fills a cell hides what is behind it, so the corner is darkened by
     * the amount of them that stand there, which is the shading every voxel engine uses to make a
     * corner between two walls read as a corner.
     *
     * @param blocks blocks around the face
     * @param x local X coordinate of the block
     * @param y local Y coordinate of the block
     * @param z local Z coordinate of the block
     * @param face face the corner belongs to
     * @param corner corner of the face, {@code 0} to {@code 3}
     * @return the brightness, {@code OCCLUSION[0]} to {@code 1}
     */
    private static float occlusion(boolean[] hiding, int x, int y, int z, BlockFace face, int corner) {
        int nx = face.x();
        int ny = face.y();
        int nz = face.z();
        // The step to the corner outside the face: the normal on the axis the face looks along, and the
        // side the corner lies on along the two axes the face runs along.
        int dx = nx != 0 ? nx : (face.cornerX(corner) == 1 ? 1 : -1);
        int dy = ny != 0 ? ny : (face.cornerY(corner) == 1 ? 1 : -1);
        int dz = nz != 0 ? nz : (face.cornerZ(corner) == 1 ? 1 : -1);

        boolean sideA;
        boolean sideB;
        boolean diagonal;
        if (nx != 0) {
            sideA = hiding(hiding, x + nx, y + dy, z);
            sideB = hiding(hiding, x + nx, y, z + dz);
            diagonal = hiding(hiding, x + nx, y + dy, z + dz);
        } else if (ny != 0) {
            sideA = hiding(hiding, x + dx, y + ny, z);
            sideB = hiding(hiding, x, y + ny, z + dz);
            diagonal = hiding(hiding, x + dx, y + ny, z + dz);
        } else {
            sideA = hiding(hiding, x + dx, y, z + nz);
            sideB = hiding(hiding, x, y + dy, z + nz);
            diagonal = hiding(hiding, x + dx, y + dy, z + nz);
        }

        if (sideA && sideB) {
            return OCCLUSION[0];
        }
        int occluders = (sideA ? 1 : 0) + (sideB ? 1 : 0) + (diagonal ? 1 : 0);
        return OCCLUSION[3 - occluders];
    }

    private static final int SHELL = Section.SIZE + 2;

    /**
     * {@code true} when a block hides the face of the block behind it.
     * <p>
     * A cell that holds nothing or a picture one can see through hides nothing: the face behind it is
     * drawn, which is what lets a canopy of leaves or a wall of glass show what stands behind it.
     *
     * @param block block standing in the cell next to a face
     * @return {@code true} when the face behind it is not drawn
     */
    private static boolean hides(Block block) {
        return !block.isAir() && !block.isTransparent();
    }
}
