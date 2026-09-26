package com.philia093.neofactory.render;

import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.IntMap;
import com.badlogic.gdx.utils.ObjectSet;
import com.philia093.neofactory.block.Block;
import com.philia093.neofactory.block.BlockFace;
import com.philia093.neofactory.block.BlockRegistry;
import com.philia093.neofactory.block.Blocks;
import com.philia093.neofactory.item.Item;
import com.philia093.neofactory.util.Constants;
import com.philia093.neofactory.world.Section;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

/**
 * The little body every dropped item is drawn as, one body per kind of item.
 * <p>
 * An item that lies on the ground has to be seen to be picked up, so it is drawn as a small cube of the
 * block it stands for - the cube of a stone looks like stone from every side, because it is meshed from
 * the same pictures the world uses, see {@link SectionMesher}. The cube is meshed once per kind of block
 * and kept, so a hundred stones lying around cost one mesh, three draws each and nothing else: the place
 * of an item is a model matrix, not a mesh, see {@link BlockShader#render(Mesh, com.badlogic.gdx.math.Matrix4)}.
 * <p>
 * <b>An item that is no block is drawn as a board of its picture.</b> A tool, a material or anything else
 * that places no block has no cube to show, and its picture is the only art it has: one upright board across
 * the middle of the block, carrying that picture on both of its large faces, the one behind showing it
 * mirrored the way it comes through the board. One pixel of the picture thick is all it needs to be a thing
 * and not a plane - a plane of no thickness is a line seen from the side and nothing at all seen from behind,
 * while the rims of a board show the edge of the picture that meets them. That one pixel is also what lets
 * the turn of a dropped item read as a whole turn rather than as a quarter of one that looks the same.
 * <p>
 * <b>What cannot be drawn.</b> An item whose picture the array of the world does not hold is not drawn at
 * all: it is named in the log once, instead of leaving a player with a drop they cannot see.
 */
public class ItemCubeMeshes implements Disposable {

    private static final Logger LOGGER = LogManager.getLogger();

    /**
     * Brightness a board is drawn with, out of the brightness of its picture.
     * <p>
     * White leaves the picture the colours it was drawn in: a board carries no face of a block, so there is
     * no fixed shading for it to take, and a drop of a tool has to read on a floor of any colour.
     */
    private static final float BOARD_LIGHT = 1.0f;

    /**
     * Thickness of the board of an item, in pixels of its own picture.
     * <p>
     * One pixel is what keeps a board from being a plane of no thickness, which would be a line seen from the
     * side and nothing at all seen from behind: a picture cut out and stood up has an edge.
     */
    private static final int BOARD_PIXELS = 1;

    /** Thickness of that board in the room of a block, where the whole picture is one block wide. */
    private static final float BOARD_THICKNESS = BOARD_PIXELS / (float) Constants.ITEM_ICON_SIZE;

    /** Half of it, the distance the board reaches to either side of the middle of the block. */
    private static final float BOARD_HALF = BOARD_THICKNESS * 0.5f;

    /** Middle of the first pixel of a picture across or up, which is the pixel a rim shows. */
    private static final float FIRST_PIXEL = 0.5f / Constants.ITEM_ICON_SIZE;

    /** Middle of the last pixel of a picture across or up, the pixel the opposite rim shows. */
    private static final float LAST_PIXEL = 1.0f - FIRST_PIXEL;

    /** Side length of the cube of an item, in blocks, a little over a third of a block. */
    public static final float SIZE = Constants.ITEM_SIZE;

    private final BlockPictures pictures;

    /** The cube of every kind of block that was asked for, keyed by block id. */
    private final IntMap<Array<Mesh>> cubes = new IntMap<>();

    /** The board of every kind of item that is no block, keyed by item id. */
    private final IntMap<Array<Mesh>> boards = new IntMap<>();

    /** Names of the items with no picture, so each of them is reported once and not per frame. */
    private final ObjectSet<String> withoutPicture = new ObjectSet<>();

    /**
     * Creates the cache.
     *
     * @param pictures pictures of the world, asked for the layer of a picture
     */
    public ItemCubeMeshes(BlockPictures pictures) {
        this.pictures = pictures;
    }

    /**
     * Cube of a block, meshed on first use.
     *
     * @param block block an item stands for
     * @return the meshes of its cube, spanning the block from the origin, to be placed by a model matrix
     */
    public Array<Mesh> cubeOf(Block block) {
        Array<Mesh> cube = cubes.get(block.id());
        if (cube != null) {
            return cube;
        }
        Array<Mesh> built = build(block);
        cubes.put(block.id(), built);
        return built;
    }

    /**
     * Body of a dropped item, meshed on first use.
     * <p>
     * An item that places a block is drawn as the cube of that block, everything else as the board of its
     * picture, see the class comment. Both kinds of body span the room of one block from the origin, so a
     * caller places them the same way.
     *
     * @param item item that lies on the ground
     * @return the meshes of its body, empty for an item whose picture the array does not hold
     */
    public Array<Mesh> meshOf(Item item) {
        Block block = item.block();
        if (block != null) {
            return cubeOf(block);
        }
        Array<Mesh> cached = boards.get(item.id());
        if (cached != null) {
            return cached;
        }
        int layer = layerOf(item);
        if (layer < 0 && withoutPicture.add(item.name())) {
            // Nothing to draw is a drop a player walks past without knowing it is there, which is worth
            // saying once rather than on every frame that draws the world.
            LOGGER.warn("The picture of '{}' is not part of the world, so a drop of it stays invisible",
                    item.name());
        }
        Array<Mesh> built = new Array<>();
        if (layer >= 0) {
            built.add(upload(board(layer)));
        }
        boards.put(item.id(), built);
        return built;
    }

    /**
     * Layer of the picture of an item inside the array of the world.
     * <p>
     * An item names its picture below {@code items/}, and a name without that folder is offered as well,
     * see {@link BlockPictures#pictureNames}: whichever of the two names the array holds is the one to
     * draw.
     *
     * @param item item whose picture is asked for
     * @return the layer, or {@code -1} when the array holds neither name
     */
    private int layerOf(Item item) {
        int layer = pictures.layer(BlockPictures.itemPicture(item));
        return layer >= 0 ? layer : pictures.layer(item.texture());
    }

    /** Meshes one block that stands alone in an empty section, which is the cube of an item. */
    private Array<Mesh> build(Block block) {
        Section section = new Section(0);
        section.setRawId(0, 0, 0, block.id());
        // The block stands in the state its item is drawn in, which is state zero for everything that is a
        // cube and the state whose model shows a piece of the block for everything else - a pipe is meshed
        // as a straight length of itself, see Block#itemState.
        section.setState(0, 0, 0, block.itemState());
        SectionMesher.Blocks blocks = (x, y, z) -> x == 0 && y == 0 && z == 0
                ? BlockRegistry.byId(block.id())
                : Blocks.AIR;
        List<MeshData> data = SectionMesher.build(section, 0, 0, 0, blocks, pictures::layer);

        Array<Mesh> meshes = new Array<>();
        for (MeshData mesh : data) {
            meshes.add(upload(mesh));
        }
        return meshes;
    }

    /** Uploads the triangles of one mesh, where they stay until the game closes. */
    private static Mesh upload(MeshData data) {
        Mesh mesh = new Mesh(true, data.vertexCount(), data.indexCount(), BlockShader.ATTRIBUTES);
        mesh.setVertices(data.vertexFloats(), 0, data.vertexCount() * MeshData.FLOATS_PER_VERTEX);
        mesh.setIndices(data.indexShorts(), 0, data.indexCount());
        return mesh;
    }

    /**
     * The board a dropped item that is no block is drawn as, in the room of one block.
     * <p>
     * The board stands upright across the X axis in the middle of the block and carries the whole picture of
     * the item on both of its large faces, upright on either of them. The face behind carries it with the
     * same side of the picture on the same side of the board, so what a reader behind the board sees is the
     * picture as it comes through the board, mirrored: that is what makes a turn of the item a whole turn -
     * a sword points one way at the start, away at the quarter and the other way halfway - instead of a
     * quarter of a turn that keeps looking the same. It is {@value #BOARD_PIXELS} pixel of the picture thick,
     * so it never turns into nothing while it turns: a plane of no thickness is a line seen from the side,
     * and the four rims that the thickness brings with it show the row or the column of the picture that
     * meets them, which is what the edge of a picture cut out and stood up looks like.
     * <p>
     * Every face is wound the way {@link BlockFace} walks a face of a block - counter clockwise seen from
     * outside - so the back faces of a turning board are culled the way the back faces of a block are.
     *
     * @param layer layer the picture of the item lives in
     * @return the triangles of the board
     */
    static MeshData board(int layer) {
        MeshData mesh = new MeshData();
        float near = 0.5f - BOARD_HALF;
        float far = 0.5f + BOARD_HALF;
        // The face the item is read from, across the X axis: the picture lies on it as it was drawn.
        addFace(mesh, layer,
                new float[] {0.0f, 0.0f, far, 1.0f, 0.0f, far, 1.0f, 1.0f, far, 0.0f, 1.0f, far},
                new float[] {0.0f, 1.0f, 1.0f, 1.0f, 1.0f, 0.0f, 0.0f, 0.0f});
        // The face behind it: it shows the very same picture with the very same side of it on the same side
        // of the board, so a reader behind the board sees the item the way it comes through the board -
        // mirrored. A sword therefore points one way at the start of a turn, away at the quarter of it and
        // the other way halfway through, which is what makes the turn of an item a whole turn instead of a
        // quarter of one that keeps looking the same.
        addFace(mesh, layer,
                new float[] {0.0f, 0.0f, near, 0.0f, 1.0f, near, 1.0f, 1.0f, near, 1.0f, 0.0f, near},
                new float[] {0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 1.0f, 1.0f});
        // The rims: the last column of the picture on the east side and the first one on the west side.
        addFace(mesh, layer,
                new float[] {1.0f, 0.0f, near, 1.0f, 1.0f, near, 1.0f, 1.0f, far, 1.0f, 0.0f, far},
                new float[] {LAST_PIXEL, 1.0f, LAST_PIXEL, 0.0f, LAST_PIXEL, 0.0f, LAST_PIXEL, 1.0f});
        addFace(mesh, layer,
                new float[] {0.0f, 0.0f, near, 0.0f, 0.0f, far, 0.0f, 1.0f, far, 0.0f, 1.0f, near},
                new float[] {FIRST_PIXEL, 1.0f, FIRST_PIXEL, 1.0f, FIRST_PIXEL, 0.0f, FIRST_PIXEL, 0.0f});
        // The first row of the picture is the row that meets the top of the board, the last one the bottom.
        addFace(mesh, layer,
                new float[] {0.0f, 1.0f, near, 0.0f, 1.0f, far, 1.0f, 1.0f, far, 1.0f, 1.0f, near},
                new float[] {0.0f, FIRST_PIXEL, 0.0f, FIRST_PIXEL, 1.0f, FIRST_PIXEL, 1.0f, FIRST_PIXEL});
        addFace(mesh, layer,
                new float[] {0.0f, 0.0f, near, 1.0f, 0.0f, near, 1.0f, 0.0f, far, 0.0f, 0.0f, far},
                new float[] {0.0f, LAST_PIXEL, 1.0f, LAST_PIXEL, 1.0f, LAST_PIXEL, 0.0f, LAST_PIXEL});
        return mesh;
    }

    /**
     * Writes one face of the board: four corners and the two triangles between them.
     *
     * @param mesh mesh to write into
     * @param layer layer the picture of the item lives in
     * @param corners twelve floats, the X, Y and Z of the four corners, counter clockwise seen from outside
     * @param uv eight floats, the coordinates of the picture the four corners show
     */
    private static void addFace(MeshData mesh, int layer, float[] corners, float[] uv) {
        int[] written = new int[4];
        for (int corner = 0; corner < 4; corner++) {
            written[corner] = mesh.addVertex(corners[corner * 3], corners[corner * 3 + 1],
                    corners[corner * 3 + 2], uv[corner * 2], uv[corner * 2 + 1], layer, BOARD_LIGHT,
                    BOARD_LIGHT, BOARD_LIGHT);
        }
        mesh.addTriangle(written[0], written[1], written[2]);
        mesh.addTriangle(written[0], written[2], written[3]);
    }

    /** Amount of kinds of blocks whose cube is held right now. */
    public int cubeCount() {
        return cubes.size;
    }

    /** Amount of kinds of items whose board is held right now, see {@link #meshOf(Item)}. */
    public int boardCount() {
        return boards.size;
    }

    @Override
    public void dispose() {
        dispose(cubes);
        dispose(boards);
        withoutPicture.clear();
    }

    /** Releases every mesh of one cache and empties it. */
    private static void dispose(IntMap<Array<Mesh>> cached) {
        for (Array<Mesh> body : cached.values()) {
            for (Mesh mesh : body) {
                mesh.dispose();
            }
            body.clear();
        }
        cached.clear();
    }
}
