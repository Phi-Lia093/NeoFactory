package com.philia093.neofactory.block.model;

import com.philia093.neofactory.block.BlockFace;
import com.philia093.neofactory.util.Aabb;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * The shape of a block: one or more boxes, each with a picture per face.
 * <p>
 * A model is read from {@code assets/models/block/<name>.json} and named by the block that shows it,
 * see {@link ModelRegistry}. A block that names no model of its own is drawn as the one picture
 * {@link com.philia093.neofactory.block.Block#texture()} points at, which is what most of the art
 * pack is.
 * <p>
 * <b>A model is immutable once it is read.</b> The boxes it holds are written while the file is
 * parsed and are then shared by every cell that shows the model, so a world of ten thousand stones
 * holds one model and not ten thousand.
 */
public final class BlockModel {

    /** A model that shows nothing at all, the one a block without a picture gets. */
    public static final BlockModel EMPTY = new BlockModel("empty", List.of());

    private final String name;
    private final List<ModelBox> boxes;
    private final Aabb shape;

    /**
     * Creates a model.
     *
     * @param name name of the model, the name of its file without the extension
     * @param boxes boxes this model is made of
     */
    public BlockModel(String name, List<ModelBox> boxes) {
        this.name = Objects.requireNonNull(name, "name");
        this.boxes = List.copyOf(Objects.requireNonNull(boxes, "boxes"));
        this.shape = union(this.boxes);
    }

    /**
     * The part of its cell this model fills, in the coordinates of one block.
     * <p>
     * The union of the boxes of the model, as {@code (0, 0, 0)} to {@code (1, 1, 1)} is one whole cell.
     * It is what a body runs into: a slab fills the lower or the upper half of its cell and an anvil a body
     * of its own, see
     * {@link com.philia093.neofactory.block.Block#shape(int, com.philia093.neofactory.util.Aabb)}.
     * <p>
     * <b>The turn of a box is left out.</b> Only the crossed planes of a plant carry one, and a plant is
     * entered by a body whatever shape it is drawn with - the box itself already reaches from edge to
     * edge of the cell, so turning it changes nothing a body would feel.
     *
     * @return the box, empty for a model that draws nothing
     */
    public Aabb shape() {
        return shape;
    }

    /** The union of the boxes of a model, in the coordinates of one block. */
    private static Aabb union(List<ModelBox> boxes) {
        if (boxes.isEmpty()) {
            return new Aabb();
        }
        float minX = Float.POSITIVE_INFINITY;
        float minY = Float.POSITIVE_INFINITY;
        float minZ = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY;
        float maxY = Float.NEGATIVE_INFINITY;
        float maxZ = Float.NEGATIVE_INFINITY;
        for (ModelBox box : boxes) {
            minX = Math.min(minX, box.fromX());
            minY = Math.min(minY, box.fromY());
            minZ = Math.min(minZ, box.fromZ());
            maxX = Math.max(maxX, box.toX());
            maxY = Math.max(maxY, box.toY());
            maxZ = Math.max(maxZ, box.toZ());
        }
        float units = ModelBox.UNITS;
        return Aabb.of(minX / units, minY / units, minZ / units, maxX / units, maxY / units,
                maxZ / units);
    }

    /** Name of this model, the name of its file without the extension. */
    public String name() {
        return name;
    }

    /** Boxes this model is made of, in the order the file lists them. */
    public List<ModelBox> boxes() {
        return boxes;
    }

    /** {@code true} when this model draws nothing. */
    public boolean isEmpty() {
        return boxes.isEmpty();
    }

    /**
     * {@code true} when this model is exactly the block itself.
     * <p>
     * Only such a model may have its faces hidden by a neighbour and take the shadow a neighbour
     * drops into a corner, because only then does every face of it lie on the border of the block,
     * see {@link ModelBox#isWholeCube()}.
     */
    public boolean isWholeCube() {
        return boxes.size() == 1 && boxes.get(0).isWholeCube();
    }

    /** Amount of faces this model draws, the number of quads one cell shows. */
    public int faceCount() {
        int count = 0;
        for (ModelBox box : boxes) {
            count += box.faceCount();
        }
        return count;
    }

    /**
     * Every picture this model needs, overlays included.
     * <p>
     * The texture array of the world is built from this list, so a picture a model names is loaded
     * even while the block that names it is not in the world yet.
     *
     * @return the names, in the order the model names them
     */
    public Set<String> pictures() {
        Set<String> names = new LinkedHashSet<>();
        for (ModelBox box : boxes) {
            for (BlockFace face : BlockFace.ALL) {
                ModelFace picture = box.face(face);
                if (picture == null) {
                    continue;
                }
                names.add(picture.picture());
                if (picture.hasOverlay()) {
                    names.add(picture.overlay());
                }
            }
        }
        return names;
    }

    /**
     * Faces of this model that a neighbour may hide.
     * <p>
     * Used by the audit of the art: a whole cube is expected to carry a cull face on each of its
     * six faces, because a face of a cube that names none is drawn inside a wall.
     *
     * @return the directions, one entry per face that names one
     */
    public List<BlockFace> culledFaces() {
        List<BlockFace> found = new ArrayList<>();
        for (ModelBox box : boxes) {
            if (!box.isWholeCube()) {
                continue;
            }
            for (BlockFace face : BlockFace.ALL) {
                ModelFace picture = box.face(face);
                if (picture != null && picture.isCulled()) {
                    found.add(face);
                }
            }
        }
        return found;
    }

    @Override
    public String toString() {
        return "BlockModel(" + name + ", " + boxes.size() + " boxes, " + faceCount() + " faces)";
    }
}
