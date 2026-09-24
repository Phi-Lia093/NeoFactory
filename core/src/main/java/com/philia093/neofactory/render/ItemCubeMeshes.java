package com.philia093.neofactory.render;

import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.IntMap;
import com.philia093.neofactory.block.Block;
import com.philia093.neofactory.block.BlockRegistry;
import com.philia093.neofactory.block.Blocks;
import com.philia093.neofactory.world.Section;

import java.util.List;

/**
 * The little cube every dropped item is drawn as, one cube per kind of block.
 * <p>
 * An item that lies on the ground has to be seen to be picked up, so it is drawn as a small cube of the
 * block it stands for - the cube of a stone looks like stone from every side, because it is meshed from
 * the same pictures the world uses, see {@link SectionMesher}. The cube is meshed once per kind of block
 * and kept, so a hundred stones lying around cost one mesh, three draws each and nothing else: the place
 * of an item is a model matrix, not a mesh, see {@link BlockShader#render(Mesh, com.badlogic.gdx.math.Matrix4)}.
 * <p>
 * <b>What cannot be drawn.</b> A material, a tool or anything else that is not a block has no cube to
 * show, so such an item is not drawn at all until the game has a picture for it - which is a bale of
 * wheat lying invisible, and a line in the log the first time it happens.
 */
public class ItemCubeMeshes implements Disposable {

    /** Side length of the cube of an item, in blocks, a little over a third of a block. */
    public static final float SIZE = 0.4f;

    private final BlockPictures pictures;

    /** The cube of every kind of block that was asked for, keyed by block id. */
    private final IntMap<Array<Mesh>> cubes = new IntMap<>();

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

    /** Meshes one block that stands alone in an empty section, which is the cube of an item. */
    private Array<Mesh> build(Block block) {
        Section section = new Section(0);
        section.setRawId(0, 0, 0, block.id());
        SectionMesher.Blocks blocks = (x, y, z) -> x == 0 && y == 0 && z == 0
                ? BlockRegistry.byId(block.id())
                : Blocks.AIR;
        List<MeshData> data = SectionMesher.build(section, 0, 0, 0, blocks, pictures::layer);

        Array<Mesh> meshes = new Array<>();
        for (MeshData mesh : data) {
            Mesh uploaded = new Mesh(true, mesh.vertexCount(), mesh.indexCount(),
                    BlockShader.ATTRIBUTES);
            uploaded.setVertices(mesh.vertexFloats(), 0,
                    mesh.vertexCount() * MeshData.FLOATS_PER_VERTEX);
            uploaded.setIndices(mesh.indexShorts(), 0, mesh.indexCount());
            meshes.add(uploaded);
        }
        return meshes;
    }

    /** Amount of kinds of blocks whose cube is held right now. */
    public int cubeCount() {
        return cubes.size;
    }

    @Override
    public void dispose() {
        for (Array<Mesh> cube : cubes.values()) {
            for (Mesh mesh : cube) {
                mesh.dispose();
            }
            cube.clear();
        }
        cubes.clear();
    }
}
