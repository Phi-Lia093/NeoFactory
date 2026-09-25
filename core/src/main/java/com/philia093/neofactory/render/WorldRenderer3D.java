package com.philia093.neofactory.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.philia093.neofactory.block.Block;
import com.philia093.neofactory.entity.Entity;
import com.philia093.neofactory.entity.ItemEntity;
import com.philia093.neofactory.util.Constants;
import com.philia093.neofactory.world.Chunk;
import com.philia093.neofactory.world.Section;
import com.philia093.neofactory.world.World;
import com.philia093.neofactory.world.interaction.BlockTarget;

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

    /** Frame drawn around the cell an action would touch, owned by this renderer. */
    private final ShapeRenderer frame = new ShapeRenderer();

    /** Little cubes the items on the ground are drawn as, one per kind of block. */
    private final ItemCubeMeshes itemCubes;

    /** Reused matrix placing one item cube, so a frame does not fill the heap with matrices. */
    private final Matrix4 model = new Matrix4();

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
        this.itemCubes = new ItemCubeMeshes(pictures);
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
        cache.beginFrame();
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
        renderItems(world);
        shader.end();
    }

    /**
     * Draws the items lying on the ground as small cubes of their block.
     * <p>
     * An item has to be seen to be picked up, and a world of cubes is read by its shapes, so an item is
     * drawn as the block it stands for rather than as a flat picture. The cube is meshed once per kind of
     * block and placed by a model matrix, see {@link ItemCubeMeshes}, so a pile of a hundred stones costs
     * one mesh and one draw per item.
     * <p>
     * An item that is not a block - a material, a tool - has no cube to show and is skipped; the game has
     * no picture for it yet, see the class comment of {@link ItemCubeMeshes}.
     *
     * @param world world whose entities are drawn
     */
    private void renderItems(World world) {
        for (Entity entity : world.entities().all()) {
            if (!(entity instanceof ItemEntity)) {
                continue;
            }
            ItemEntity item = (ItemEntity) entity;
            if (item.stack().isEmpty()) {
                continue;
            }
            Block block = item.stack().item().block();
            if (block == null) {
                continue;
            }
            float half = ItemCubeMeshes.SIZE * 0.5f;
            model.idt()
                    .translate(entity.position().x - half,
                            entity.position().y + ItemCubeMeshes.SIZE * 0.25f,
                            entity.position().z - half)
                    .scale(ItemCubeMeshes.SIZE, ItemCubeMeshes.SIZE, ItemCubeMeshes.SIZE);
            for (Mesh mesh : itemCubes.cubeOf(block)) {
                shader.render(mesh, model);
                drawnMeshes++;
            }
        }
    }

    /** Sections drawn by the most recent frame, which is what the status line reports. */
    public int drawnSectionCount() {
        return drawnSections;
    }

    /** Cubes of the items, so the hand of a view can hold one, see {@link FirstPersonHand}. */
    public ItemCubeMeshes itemCubes() {
        return itemCubes;
    }

    /**
     * Draws the frame around the cell an action would touch.
     * <p>
     * The frame is the twelve edges of the cell, drawn a hair outside the block so its lines are not
     * swallowed by the block they surround, and it is drawn after the world with the depth test still on,
     * so a wall in front of it hides it the way it hides the block itself. It is drawn in black with a
     * little transparency: a black line reads on a floor of any colour, which is the same reason the flat
     * view drew a black frame.
     *
     * @param camera camera the world is seen through
     * @param target cell to frame, {@code null} draws nothing
     */
    public void renderSelection(Camera camera, BlockTarget target) {
        if (target == null) {
            return;
        }
        float grow = 0.002f;
        float minX = target.x() - grow;
        float minY = target.y() - grow;
        float minZ = target.z() - grow;
        float maxX = target.x() + 1.0f + grow;
        float maxY = target.y() + 1.0f + grow;
        float maxZ = target.z() + 1.0f + grow;

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        Gdx.gl.glLineWidth(2.0f);
        frame.setProjectionMatrix(camera.combined);
        frame.begin(ShapeRenderer.ShapeType.Line);
        frame.setColor(0.0f, 0.0f, 0.0f, 0.75f);

        frame.line(minX, minY, minZ, maxX, minY, minZ);
        frame.line(maxX, minY, minZ, maxX, minY, maxZ);
        frame.line(maxX, minY, maxZ, minX, minY, maxZ);
        frame.line(minX, minY, maxZ, minX, minY, minZ);

        frame.line(minX, maxY, minZ, maxX, maxY, minZ);
        frame.line(maxX, maxY, minZ, maxX, maxY, maxZ);
        frame.line(maxX, maxY, maxZ, minX, maxY, maxZ);
        frame.line(minX, maxY, maxZ, minX, maxY, minZ);

        frame.line(minX, minY, minZ, minX, maxY, minZ);
        frame.line(maxX, minY, minZ, maxX, maxY, minZ);
        frame.line(maxX, minY, maxZ, maxX, maxY, maxZ);
        frame.line(minX, minY, maxZ, minX, maxY, maxZ);

        frame.end();
        Gdx.gl.glLineWidth(1.0f);
        Gdx.gl.glDisable(GL20.GL_BLEND);
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
        frame.dispose();
        itemCubes.dispose();
    }

    @Override
    public String toString() {
        return "WorldRenderer3D(" + drawnSections + " sections, " + drawnMeshes + " meshes)";
    }
}
