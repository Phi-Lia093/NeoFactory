package com.philia093.neofactory.render;

import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.ObjectMap;
import com.philia093.neofactory.util.Constants;
import com.philia093.neofactory.world.Chunk;
import com.philia093.neofactory.world.Section;
import com.philia093.neofactory.world.World;

import java.util.List;

/**
 * The meshes of the sections around the player, built while the world runs.
 * <p>
 * A section is meshed when it is first asked for and kept until something in it changes, see
 * {@link Section#isDirty()}. A chunk that leaves memory and a section that changed lose their meshes, so
 * the work of a frame follows what the player sees instead of the size of the world.
 * <p>
 * <b>A change at a border reaches into the section next to it</b>, because the face of a block at a
 * border is decided by the cell on the other side. Meshing a section therefore drops the meshes of the
 * sections that touch it, and each of them is built again when it is drawn next: one frame of extra work
 * per change and never a hole in the world.
 */
public class SectionMeshCache implements Disposable {

    private final BlockPictures pictures;

    /** Meshes of every section that was built, keyed by chunk and section. */
    private final ObjectMap<Long, Array<Mesh>> built = new ObjectMap<>();

    /** Meshes of a section that is waiting for its turn, drawn by nobody and holding nothing. */
    private final Array<Mesh> notYet = new Array<>(0);

    /** Sections the frame that is running may still mesh, see {@link #beginFrame()}. */
    private int buildsLeft;

    /**
     * Sections one frame may mesh.
     * <p>
     * Meshing a section costs a few milliseconds, and turning the view or walking into new terrain asks
     * for one section after the other: without a limit a player who turns around asks for the work of a
     * whole second in the frames of a moment. A section that has to wait is not drawn for a frame or two
     * and appears as soon as it is its turn, which nobody notices - while a frame that runs twenty times
     * as long is felt at once.
     */
    private static final int BUILDS_PER_FRAME = 2;

    /**
     * Creates a cache.
     *
     * @param pictures pictures of the world, asked for the layer of a picture
     */
    public SectionMeshCache(BlockPictures pictures) {
        this.pictures = pictures;
    }

    /**
     * Starts a frame, and with it the budget of sections this frame may mesh.
     * <p>
     * The renderer calls this once per frame. A section that changed is meshed whatever the budget says,
     * because a hole a player just dug has to disappear at once; the budget is about the terrain that
     * comes into view, which may wait.
     */
    public void beginFrame() {
        buildsLeft = BUILDS_PER_FRAME;
    }

    /**
     * Meshes of one section, built when they are missing or behind the blocks that changed.
     *
     * @param world world the section belongs to
     * @param chunk chunk holding the section
     * @param sectionY index of the section along the vertical axis
     * @return the meshes, empty for a section that shows nothing
     */
    public Array<Mesh> meshesOf(World world, Chunk chunk, int sectionY) {
        long key = key(chunk.chunkX(), sectionY, chunk.chunkZ());
        Section section = chunk.section(sectionY);
        Array<Mesh> meshes = built.get(key);
        if (meshes != null && !section.isDirty()) {
            return meshes;
        }
        boolean changed = section.isDirty();
        if (meshes == null && buildsLeft <= 0) {
            // The budget of this frame is spent: the section waits for its turn and is not drawn until
            // then, see BUILDS_PER_FRAME. A section that changed never waits, it is rebuilt right here.
            return notYet;
        }
        buildsLeft--;
        if (meshes != null) {
            dispose(meshes);
        }
        Array<Mesh> fresh = build(world, chunk, sectionY, section);
        built.put(key, fresh);
        if (changed) {
            // Only a section whose blocks really changed reaches into its neighbours, see the class
            // comment. A section that was merely missing has nothing to tell them - and telling them
            // would make every rebuild drop twenty six more meshes, which is a cascade that remeshes the
            // whole loaded world again and again.
            forgetNeighbours(chunk, sectionY);
        }
        return fresh;
    }

    /** Meshes one section and uploads the meshes it hands back. */
    private Array<Mesh> build(World world, Chunk chunk, int sectionY, Section section) {
        int originX = chunk.originX();
        int originY = Constants.MIN_Y + sectionY * Section.SIZE;
        int originZ = chunk.originZ();
        SectionMesher.Blocks blocks =
                (x, y, z) -> world.peekBlock(originX + x, originY + y, originZ + z);
        List<MeshData> data = SectionMesher.build(section, originX, originY, originZ, blocks,
                pictures::layer);
        section.clearDirty();

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

    /** Drops the meshes of the sections that touch the one that was meshed. */
    private void forgetNeighbours(Chunk chunk, int sectionY) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                for (int dy = -1; dy <= 1; dy++) {
                    if (dx == 0 && dy == 0 && dz == 0) {
                        continue;
                    }
                    Array<Mesh> meshes = built.remove(
                            key(chunk.chunkX() + dx, sectionY + dy, chunk.chunkZ() + dz));
                    if (meshes != null) {
                        dispose(meshes);
                    }
                }
            }
        }
    }

    /**
     * Drops the meshes of a chunk, called when the chunk leaves memory.
     *
     * @param chunkX chunk coordinate along the first horizontal axis
     * @param chunkZ chunk coordinate along the second horizontal axis
     */
    public void forget(int chunkX, int chunkZ) {
        for (int sectionY = 0; sectionY < Constants.SECTION_COUNT; sectionY++) {
            Array<Mesh> meshes = built.remove(key(chunkX, sectionY, chunkZ));
            if (meshes != null) {
                dispose(meshes);
            }
        }
    }

    /** Amount of sections whose meshes are held right now. */
    public int sectionCount() {
        return built.size;
    }

    /** Releases the meshes of one section. */
    private static void dispose(Array<Mesh> meshes) {
        for (Mesh mesh : meshes) {
            mesh.dispose();
        }
        meshes.clear();
    }

    /** Packs the position of a section into one key. */
    private static long key(int chunkX, int sectionY, int chunkZ) {
        return ((long) (chunkX & 0x1FFFFF) << 42) | ((long) (sectionY & 0x3FF) << 32)
                | ((long) (chunkZ & 0x1FFFFF) << 11);
    }

    @Override
    public void dispose() {
        for (Array<Mesh> meshes : built.values()) {
            dispose(meshes);
        }
        built.clear();
    }
}
