package com.philia093.neofactory.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.philia093.neofactory.entity.Entity;
import com.philia093.neofactory.entity.ItemEntity;
import com.philia093.neofactory.item.ItemStack;
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

    /** Little bodies the items on the ground are drawn as, one per kind of item. */
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
     * Draws the items that lie on the ground.
     * <p>
     * Every item is one little body of its own, see {@link ItemCubeMeshes}: the cube of the block it stands
     * for, or - when it places no block, a tool or a material - one upright board of its picture, one pixel
     * thick and turning with the item. The body is placed by a model matrix - the very same cube is used for
     * every stone on the ground - and it fills the box the item is stopped by, see {@code ItemEntity#boxOf}:
     * an item that rests on the ground stands on it and one that leans against a wall touches it.
     *
     * @param world world whose entities are drawn
     */
    private void renderItems(World world) {
        for (Entity entity : world.entities().all()) {
            if (!(entity instanceof ItemEntity)) {
                continue;
            }
            ItemEntity item = (ItemEntity) entity;
            ItemStack stack = item.stack();
            if (stack.isEmpty()) {
                continue;
            }
            float half = ItemCubeMeshes.SIZE * 0.5f;
            // An item on the ground turns around its own axis, which is what makes it read as something lying
            // there and not as a block: the turn is taken from the age of the item, so a frame that is drawn
            // again shows the very same item at the very same angle.
            float turn = item.age() * ITEM_TURN_DEGREES_PER_SECOND;
            // The body stands on the middle of its box and turns around that middle, so what is drawn and
            // what stops the item are the same box.
            model.idt()
                    .translate(entity.position().x, entity.position().y, entity.position().z)
                    .rotate(Vector3.Y, turn)
                    .translate(-half, 0.0f, -half)
                    .scale(ItemCubeMeshes.SIZE, ItemCubeMeshes.SIZE, ItemCubeMeshes.SIZE);
            for (Mesh mesh : itemCubes.meshOf(stack.item())) {
                shader.render(mesh, model);
                drawnMeshes++;
            }
        }
    }

    /** Sections drawn by the most recent frame, which is what the status line reports. */
    public int drawnSectionCount() {
        return drawnSections;
    }

    /**
     * Degrees one second adds to the turn of a lying item.
     * <p>
     * A dropped item turns around its own vertical axis, which is what tells a player at a glance that this
     * little cube is something to pick up and not a block of the world. A quarter turn per second is slow
     * enough to read the picture on its faces and fast enough to be seen from across a room.
     */
    private static final float ITEM_TURN_DEGREES_PER_SECOND = 90.0f;

    /** Cubes of the items, so the hand of a view can hold one, see {@link HumanoidRenderer}. */
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
