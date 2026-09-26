package com.philia093.neofactory.render;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.philia093.neofactory.block.Block;
import com.philia093.neofactory.block.BlockFace;
import com.philia093.neofactory.block.BlockRegistry;
import com.philia093.neofactory.block.model.BlockModel;
import com.philia093.neofactory.block.model.ModelBox;
import com.philia093.neofactory.block.model.ModelFace;
import com.philia093.neofactory.block.state.BlockStateRegistry;
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
 * <b>What a cell shows is a model.</b> The mesher asks the block for the shape one of its states is
 * drawn with, see {@link BlockStateRegistry#shown(Block, int)}, and walks the boxes of that model: a
 * whole cube shows the faces a neighbour does not hide, a slab, a plant or a machine shows the faces
 * its own shape names, and a model of more than one box is drawn box by box.
 * <p>
 * <b>A face carries the window of its picture.</b> A face names the part of its picture it shows,
 * which is what lets a model draw a piece of a sheet: the skin of a body, an icon, a tile that holds
 * four pictures. A face that carries a second picture - the layer of the biome colour over the side
 * of the grass - is written twice, the second time a hair outside the first, because two cards in
 * the same place are one card to a graphics card.
 * <p>
 * <b>What a corner carries.</b> The position is a world coordinate, so the mesh of a section needs no
 * model matrix: the origin of the section is added while the corner is written. The picture comes
 * from the model of the block and its layer from the texture array, see {@link Pictures}. The colour
 * is the tint of the block - over the faces the model marks as tinted and only over those - darkened
 * by the fixed shading of the face, see {@link BlockFace#shade()}, and by the shadow its neighbours
 * drop into the corner, which is the ambient occlusion that makes an inside corner read as one. Both
 * of those need a whole cube: the faces of a plant or of a slab do not lie on the border of their
 * block, so nothing neighbouring can hide or shade them.
 * <p>
 * <b>A build hands out a list of meshes.</b> Sixteen bit indices address sixty five thousand five
 * hundred and thirty five corners and a section can show more, so a build that runs out of room
 * starts the next mesh instead of failing, see {@link MeshData}.
 */
public final class SectionMesher {

    /** Brightness a corner keeps for the number of occluders around it, see {@link #occlusion}. */
    private static final float[] OCCLUSION = {0.5f, 0.66f, 0.83f, 1.0f};

    /**
     * Distance a second layer is lifted off the face below it, in blocks.
     * <p>
     * Two cards in the same place are one card to a graphics card, which keeps whichever of them it
     * happened to draw; a hair of distance is what the frame around a block uses for the same reason.
     */
    private static final float OVERLAY_OFFSET = 0.002f;

    /** The colour a face takes that carries no tint, the white a picture is multiplied with. */
    private static final Color NO_TINT = new Color(1.0f, 1.0f, 1.0f, 1.0f);

    private SectionMesher() {
        // Utility class: never instantiated.
    }

    /**
     * The blocks a mesh is built from: the section itself and the shell of cells around it.
     * <p>
     * A coordinate is local to the section and reaches from {@code -1} to {@link Section#SIZE},
     * because the face of a block at the border is decided by the cell on the other side of it. The
     * caller adds the origin of the section, so the mesher never talks to a world.
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

    /**
     * The states of the cells of a section.
     * <p>
     * A cell carries a number beside the id of its block, and the model that number selects is what
     * decides the direction a furnace looks in or the shape a pipe is drawn with, see
     * {@link com.philia093.neofactory.block.state.BlockStateTable}. A section that never set a state
     * answers with zero, which is the state every property at its first value.
     */
    @FunctionalInterface
    public interface States {

        /**
         * Returns the state of a cell of the section.
         *
         * @param x local X coordinate, {@code 0} to {@link Section#SIZE} minus one
         * @param y local Y coordinate, {@code 0} to {@link Section#SIZE} minus one
         * @param z local Z coordinate, {@code 0} to {@link Section#SIZE} minus one
         * @return the number of the state, {@code 0} for a cell that carries none
         */
        int stateAt(int x, int y, int z);
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
     * Meshes the cells of a section, with the state every one of them carries.
     * <p>
     * <b>The states come from the section itself.</b> A section stores the number beside the id of every
     * cell, and that number is what decides the direction a furnace looks in or the shape a pipe is drawn
     * with, see {@link com.philia093.neofactory.block.state.BlockStateTable}. Reading them here - instead
     * of meshing every cell as state zero, which this overload used to do - is what makes the item of a
     * shaped block the piece of it a player holds: the cube a slot, a hand and a drop are drawn from is
     * meshed from the very state {@code Block#itemState} names, and a pipe comes out as the straight length
     * of itself and not as the bare stub of state zero, see {@code ItemCubeMeshes} and
     * {@code PipeIconMeshTest}.
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
        return build(section, originX, originY, originZ, blocks, section::state, pictures);
    }

    /**
     * Meshes the cells of a section.
     *
     * @param section section to mesh
     * @param originX world X coordinate of the column of the section
     * @param originY world Y coordinate the section starts at
     * @param originZ world Z coordinate of the column of the section
     * @param blocks blocks of the section and of the shell around it
     * @param states states of the cells of the section
     * @param pictures layers the pictures live in
     * @return the meshes to draw, empty when the section shows nothing
     */
    public static List<MeshData> build(Section section, int originX, int originY, int originZ,
            Blocks blocks, States states, Pictures pictures) {
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
                    BlockStateRegistry.Shown shown = block.shown(states.stateAt(x, y, z));
                    BlockModel model = shown.model();
                    if (model.isEmpty()) {
                        continue;
                    }
                    // Only a whole cube may have a face hidden or shaded by a neighbour, because only
                    // then does every face of it lie on the border of the block, see BlockModel.
                    boolean[] shadow = model.isWholeCube() ? hiding : null;
                    for (ModelBox box : model.boxes()) {
                        for (BlockFace face : BlockFace.ALL) {
                            ModelFace picture = box.face(face);
                            if (picture == null) {
                                continue;
                            }
                            if (shadow != null && picture.isCulled()
                                    && hiding(hiding, x + face.x(), y + face.y(), z + face.z())) {
                                continue;
                            }
                            int layer = pictures.layer(picture.picture());
                            if (layer < 0) {
                                continue;
                            }
                            if (mesh.isFull()) {
                                meshes.add(mesh);
                                mesh = new MeshData();
                            }
                            addFace(mesh, block, box, face, picture, layer, shown.rotateY(), x, y, z,
                                    originX, originY, originZ, shadow, 0.0f);
                            if (picture.hasOverlay()) {
                                int overlay = pictures.layer(picture.overlay());
                                if (overlay >= 0) {
                                    if (mesh.isFull()) {
                                        meshes.add(mesh);
                                        mesh = new MeshData();
                                    }
                                    addFace(mesh, block, box, face, picture, overlay,
                                            shown.rotateY(), x, y, z, originX, originY, originZ, null,
                                            OVERLAY_OFFSET);
                                }
                            }
                        }
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
     * Writes the four corners of one face and the two triangles between them.
     * <p>
     * A corner of the box is placed by {@link #axis}, turned by the rotation of its own box and by the
     * quarter turn of the state, and lifted along the direction of the face when a second layer is
     * written over it.
     *
     * @param mesh mesh to write into
     * @param block block the face belongs to, asked for its tint
     * @param box box the face belongs to
     * @param face face to write
     * @param picture picture and window of the face
     * @param layer layer the picture lives in inside the texture array
     * @param rotateY quarter turns the whole model is turned by
     * @param x local X coordinate of the block
     * @param y local Y coordinate of the block
     * @param z local Z coordinate of the block
     * @param originX world X coordinate of the column of the section
     * @param originY world Y coordinate the section starts at
     * @param originZ world Z coordinate of the column of the section
     * @param shadow blocks around the face, {@code null} when this model is no whole cube
     * @param offset distance the face is lifted along its own direction, {@code 0} for the face itself
     */
    private static void addFace(MeshData mesh, Block block, ModelBox box, BlockFace face,
            ModelFace picture, int layer, int rotateY, int x, int y, int z, int originX, int originY,
            int originZ, boolean[] shadow, float offset) {
        Color tint = picture.tinted() ? block.tint() : NO_TINT;
        float pushX = face.x() * offset;
        float pushY = face.y() * offset;
        float pushZ = face.z() * offset;
        float[] window = new float[2];
        int[] corners = new int[4];
        for (int corner = 0; corner < 4; corner++) {
            float localX = axis(box, 0, face.cornerX(corner));
            float localY = axis(box, 1, face.cornerY(corner));
            float localZ = axis(box, 2, face.cornerZ(corner));
            ModelBox.Rotation turn = box.rotation();
            if (turn != null && !turn.isStraight()) {
                float cos = MathUtils.cosDeg(turn.angle());
                float sin = MathUtils.sinDeg(turn.angle());
                float originX1 = turn.originX() / ModelBox.UNITS;
                float originY1 = turn.originY() / ModelBox.UNITS;
                float originZ1 = turn.originZ() / ModelBox.UNITS;
                float px = localX - originX1;
                float py = localY - originY1;
                float pz = localZ - originZ1;
                switch (turn.axis()) {
                    case 'x' -> {
                        localY = originY1 + py * cos - pz * sin;
                        localZ = originZ1 + py * sin + pz * cos;
                    }
                    case 'y' -> {
                        localX = originX1 + px * cos + pz * sin;
                        localZ = originZ1 - px * sin + pz * cos;
                    }
                    default -> {
                        localX = originX1 + px * cos - py * sin;
                        localY = originY1 + px * sin + py * cos;
                    }
                }
            }
            if (rotateY != 0) {
                // The quarter turn of a state goes around the middle of the block, which is the axis
                // a furnace looks along: a model that faces north turns to face east at 90 degrees.
                float cos = MathUtils.cosDeg(rotateY);
                float sin = MathUtils.sinDeg(rotateY);
                float px = localX - 0.5f;
                float pz = localZ - 0.5f;
                localX = 0.5f + px * cos + pz * sin;
                localZ = 0.5f - px * sin + pz * cos;
            }
            float light = light(box, face, shadow, x, y, z, corner);
            uv(picture, box, face, corner, window);
            corners[corner] = mesh.addVertex(
                    originX + x + localX + pushX,
                    originY + y + localY + pushY,
                    originZ + z + localZ + pushZ,
                    window[0], window[1], layer,
                    tint.r * light, tint.g * light, tint.b * light);
        }
        // The corners are walked counter clockwise as seen from outside the block, so both triangles
        // face the viewer with the winding a graphics card keeps. A turn of the box or of the state
        // moves the corners but never swaps two of them, so the winding survives both.
        mesh.addTriangle(corners[0], corners[1], corners[2]);
        mesh.addTriangle(corners[0], corners[2], corners[3]);
    }

    /**
     * How much light reaches one corner of a face.
     * <p>
     * A box that named {@code "shade": false} - the crossed planes of a plant - is drawn at the full
     * light of the world, whatever direction it looks in. Every other box falls off with the direction
     * of its face, and a box that is the whole block takes the shadow its neighbours drop into the
     * corner on top of that, see {@link #occlusion}.
     *
     * @param box box the face belongs to
     * @param face face the corner belongs to
     * @param shadow blocks around the face, {@code null} when this model is no whole cube
     * @param x local X coordinate of the block
     * @param y local Y coordinate of the block
     * @param z local Z coordinate of the block
     * @param corner corner of the face, {@code 0} to {@code 3}
     * @return the brightness, {@code 0.5} to {@code 1}
     */
    private static float light(ModelBox box, BlockFace face, boolean[] shadow, int x, int y, int z,
            int corner) {
        float shade = box.shaded() ? face.shade() : 1.0f;
        return shadow == null ? shade : shade * occlusion(shadow, x, y, z, face, corner);
    }

    /**
     * The spot inside its picture a corner of a face takes.
     * <p>
     * The two coordinates of a corner inside the face are read from the box, not from the order the
     * corners are walked in, and that is what keeps a picture upright on every face: across a face the
     * picture runs the way a viewer in front of it reads it, and up the face the picture starts at its
     * top.
     * <p>
     * <b>The four sides read left to right, seen from outside.</b> Looking at the north face from
     * outside means looking along {@code +Z}, where the {@code +X} axis runs to the left, so the
     * picture of that face counts its own {@code u} the other way round; the east face counts the
     * {@code -Z} axis for the same reason, see {@link #across}.
     * <p>
     * <b>A face that looks up or down lies as the picture lies.</b> There the two axes of the face are
     * the two axes of the world, so a floor of grass and the top of a log read the way they were
     * drawn, which is what the view from above already relied on.
     *
     * @param picture picture and window of the face
     * @param box box the face belongs to
     * @param face face the corner belongs to
     * @param corner corner of the face, {@code 0} to {@code 3}
     * @param into place the two coordinates are written into, so a build allocates nothing per corner
     */
    private static void uv(ModelFace picture, ModelBox box, BlockFace face, int corner,
            float[] into) {
        float x = axis(box, 0, face.cornerX(corner));
        float y = axis(box, 1, face.cornerY(corner));
        float z = axis(box, 2, face.cornerZ(corner));
        // The corner is read as a spot on the picture: across to the right, down from its top. The
        // rule itself lives in FaceLayout, so a block and a body read their pictures the same way.
        float across = FaceLayout.across(face, x, y, z);
        float up = FaceLayout.up(face, x, y, z);
        FaceLayout.turn(picture.rotation(), across, 1.0f - up, into);
        into[0] = picture.u0() + (picture.u1() - picture.u0()) * into[0];
        into[1] = picture.v0() + (picture.v1() - picture.v0()) * into[1];
    }

    /**
     * One coordinate of a corner of a box, in blocks.
     *
     * @param box box to read
     * @param axis axis to read, {@code 0} for X, {@code 1} for Y and {@code 2} for Z
     * @param side corner of the face along that axis, {@code 0} for the lower corner of the box and
     *             {@code 1} for the upper one
     * @return the coordinate, in blocks
     */
    private static float axis(ModelBox box, int axis, int side) {
        float units = switch (axis) {
            case 0 -> side == 1 ? box.toX() : box.fromX();
            case 1 -> side == 1 ? box.toY() : box.fromY();
            default -> side == 1 ? box.toZ() : box.fromZ();
        };
        return units / ModelBox.UNITS;
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
     * How much light reaches one corner of a face.
     * <p>
     * The three cells that touch the corner outside the face decide it: the two beside the corner and
     * the one across it. A block that fills a cell hides what is behind it, so the corner is darkened by
     * the amount of them that stand there, which is the shading every voxel engine uses to make a
     * corner between two walls read as a corner.
     *
     * @param hiding blocks around the face, one flag per cell of the shell
     * @param x local X coordinate of the block
     * @param y local Y coordinate of the block
     * @param z local Z coordinate of the block
     * @param face face the corner belongs to
     * @param corner corner of the face, {@code 0} to {@code 3}
     * @return the brightness, {@code OCCLUSION[0]} to {@code 1}
     */
    private static float occlusion(boolean[] hiding, int x, int y, int z, BlockFace face,
            int corner) {
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
     * <p>
     * <b>Only a block that fills its cell hides a face.</b> A block that is thinner than a cube - a slab,
     * an anvil, the hollow pot of a cauldron - leaves a piece of the cell open, so the face of the ground
     * under it is still seen through that gap and has to be drawn: a cell that hid it would make the
     * ground under a slab look like a hole.
     *
     * @param block block standing in the cell next to a face
     * @return {@code true} when the face behind it is not drawn
     */
    private static boolean hides(Block block) {
        return !block.isAir() && !block.isTransparent() && block.model().isWholeCube();
    }
}
