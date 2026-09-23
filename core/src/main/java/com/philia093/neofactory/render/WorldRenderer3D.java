package com.philia093.neofactory.render;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.philia093.neofactory.util.Constants;
import com.philia093.neofactory.world.Chunk;
import com.philia093.neofactory.world.Section;
import com.philia093.neofactory.world.World;

/**
 * Draws the world as cubes, seen through a camera that stands in it.
 * <p>
 * For every chunk in memory and every section of it that holds something, the meshes of that section are
 * asked for - they are built once and kept, see {@link SectionMeshCache} - and drawn if the box of the
 * section is inside the view. A section behind the player, under a mountain or above the clouds is
 * therefore skipped before its triangles are sent to the card, which is what keeps the number of draws a
 * frame follows the view instead of the world.
 * <p>
 * The distance fades into the colour of the sky, so the world ends in the sky instead of at a hard edge.
 * <p>
 * <b>What is not here yet.</b> A section's faces are in one mesh, so the faces of the blocks one can see
 * through are drawn among the solid ones while they should be drawn after them without writing depth.
 * The meshes of a chunk that leaves memory are kept until the game closes, because the world does not
 * tell this renderer that it dropped a chunk; both are the work of the pass that adds transparent
 * geometry and of the streamer that owns the lifetime of a chunk.
 */
public class WorldRenderer3D implements Disposable {

    /** Share of the view distance the fog starts at, the rest is faded into the sky. */
    private static final float FOG_START_SHARE = 0.55f;

    private final SectionMeshCache cache;
    private final BlockShader shader;
    private final BlockPictures pictures;

    private int drawnSections;
    private int drawnMeshes;

    /**
     * Creates a renderer.
     *
     * @param cache meshes of the sections
     * @param shader program the meshes are drawn with
     * @param pictures pictures of the world the shader samples
     */
    public WorldRenderer3D(SectionMeshCache cache, BlockShader shader, BlockPictures pictures) {
        this.cache = cache;
        this.shader = shader;
        this.pictures = pictures;
    }

    /**
     * Draws the world.
     *
     * @param world world to draw
     * @param camera camera the world is seen through
     * @param sky colour of the sky, also the colour the distance fades into
     */
    public void render(World world, PerspectiveCamera camera, Color sky) {
        drawnSections = 0;
        drawnMeshes = 0;
        shader.begin(camera, pictures, sky, camera.far * FOG_START_SHARE, camera.far);
        for (Chunk chunk : world.chunks()) {
            int originX = chunk.originX();
            int originZ = chunk.originZ();
            for (int sectionY = 0; sectionY < Constants.SECTION_COUNT; sectionY++) {
                if (chunk.isEmptySection(sectionY)) {
                    continue;
                }
                int originY = Constants.MIN_Y + sectionY * Section.SIZE;
                float half = Section.SIZE * 0.5f;
                if (!camera.frustum.boundsInFrustum(originX + half, originY + half, originZ + half,
                        half, half, half)) {
                    continue;
                }
                Array<Mesh> meshes = cache.meshesOf(world, chunk, sectionY);
                if (meshes.size == 0) {
                    continue;
                }
                drawnSections++;
                for (Mesh mesh : meshes) {
                    shader.render(mesh);
                    drawnMeshes++;
                }
            }
        }
        shader.end();
    }

    /** Sections drawn by the most recent frame, which is what the status line reports. */
    public int drawnSectionCount() {
        return drawnSections;
    }

    /** Meshes drawn by the most recent frame. */
    public int drawnMeshCount() {
        return drawnMeshes;
    }

    /** Sections whose meshes are held right now. */
    public int cachedSectionCount() {
        return cache.sectionCount();
    }

    @Override
    public void dispose() {
        cache.dispose();
        shader.dispose();
        pictures.dispose();
    }

    @Override
    public String toString() {
        return "WorldRenderer3D(" + drawnSections + " sections, " + drawnMeshes + " meshes)";
    }
}
