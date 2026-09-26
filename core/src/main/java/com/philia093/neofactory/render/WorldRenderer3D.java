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
import com.philia093.neofactory.block.BlockFace;
import com.philia093.neofactory.world.interaction.FaceGrid;
import com.philia093.neofactory.world.interaction.FaceMark;
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

    /** Pictures of a block coming apart, drawn over the cell that is being broken. */
    private final BreakOverlay breaking;

    /** Reused matrix placing one item cube, so a frame does not fill the heap with matrices. */
    private final Matrix4 model = new Matrix4();

    /** Lines of the grid of faces, written every frame a grid is drawn. */
    private final float[] gridLines = new float[FaceOverlay.LINES_FLOATS];

    /** Corners of the cell of the grid under the mouse, written with the lines. */
    private final float[] gridCell = new float[FaceOverlay.CELL_FLOATS];

    /** Crossing of the cell of the grid that is being written, one cell at a time. */
    private final float[] gridDiagonals = new float[FaceOverlay.DIAGONAL_FLOATS];

    /** Arrow of the cell of the grid that is being written, one cell at a time. */
    private final float[] gridArrow = new float[FaceOverlay.ARROW_FLOATS];

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
        this.breaking = new BreakOverlay(pictures);
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

    /**
     * Draws the grid of faces on the block a player is working on.
     * <p>
     * The lines cut the face the eyes met into nine and the cell the mouse points at is filled, so a
     * player sees both the faces they may reach and which one a click would take. Both are worked out by
     * {@link FaceOverlay} and drawn a hair outside the block with the depth test still on, so a wall in
     * front of the block hides the grid the way it hides the block itself.
     * <p>
     * <b>Every cell says what its side does.</b> The marks the block answered with are drawn with the
     * grid: a side that is not joined is crossed out by its two diagonals and a side that carries the valve
     * of a one way line carries the small arrow of it, out of the block or into it, see {@link FaceMark}.
     * The grid is drawn after the world and before the frame of the targeted cell, so the frame stays the
     * outermost mark of what an action would touch.
     *
     * @param camera camera the world is seen through
     * @param target cell the grid is drawn on, {@code null} draws nothing
     * @param viewerFacing side the player faces, only read for a face that lies flat
     * @param cell cell of the grid under the mouse, {@code 0} to
     *        {@link com.philia093.neofactory.world.interaction.FaceGrid#CELLS} minus one
     * @param marks mark of every cell of the grid, {@code null} or a missing entry for a plain cell
     */
    public void renderFaceGrid(Camera camera, BlockTarget target, BlockFace viewerFacing, int cell,
            FaceMark[] marks) {
        if (target == null || target.face() == null) {
            return;
        }
        FaceOverlay.lines(target.x(), target.y(), target.z(), target.face(), gridLines);
        FaceOverlay.cellCorners(target.x(), target.y(), target.z(), target.face(), viewerFacing, cell,
                gridCell);

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        frame.setProjectionMatrix(camera.combined);

        Gdx.gl.glLineWidth(2.0f);
        frame.begin(ShapeRenderer.ShapeType.Line);
        frame.setColor(0.0f, 0.0f, 0.0f, 0.65f);
        for (int line = 0; line < FaceOverlay.LINES; line++) {
            int at = line * 2 * FaceOverlay.POINT_FLOATS;
            frame.line(gridLines[at], gridLines[at + 1], gridLines[at + 2],
                    gridLines[at + 3], gridLines[at + 4], gridLines[at + 5]);
        }
        // The sides that are not joined are crossed out in the same dark line the grid is drawn with, so the
        // shape of a line reads at a glance, and the arrows of the sides that only run one way stand out.
        frame.setColor(0.0f, 0.0f, 0.0f, 0.45f);
        for (int at = 0; at < FaceGrid.CELLS; at++) {
            if (marks == null || at >= marks.length || marks[at] == null || !marks[at].isCrossed()) {
                continue;
            }
            FaceOverlay.diagonals(target.x(), target.y(), target.z(), target.face(), viewerFacing, at,
                    gridDiagonals);
            drawLines(gridDiagonals, FaceOverlay.DIAGONAL_LINES);
        }
        frame.setColor(0.98f, 0.78f, 0.24f, 0.95f);
        for (int at = 0; at < FaceGrid.CELLS; at++) {
            if (marks == null || at >= marks.length || marks[at] == null || !marks[at].isArrow()) {
                continue;
            }
            FaceOverlay.arrow(target.x(), target.y(), target.z(), target.face(), viewerFacing, at,
                    marks[at].pointsOut(), gridArrow);
            drawLines(gridArrow, FaceOverlay.ARROW_LINES);
        }
        // The cell under the mouse is traced in white, so a player sees which face a click would take.
        frame.setColor(1.0f, 1.0f, 1.0f, 0.9f);
        for (int corner = 0; corner < FaceOverlay.CELL_CORNERS; corner++) {
            int at = corner * FaceOverlay.POINT_FLOATS;
            int next = (corner + 1) % FaceOverlay.CELL_CORNERS * FaceOverlay.POINT_FLOATS;
            frame.line(gridCell[at], gridCell[at + 1], gridCell[at + 2],
                    gridCell[next], gridCell[next + 1], gridCell[next + 2]);
        }
        frame.end();
        Gdx.gl.glLineWidth(1.0f);
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    /** Draws the first lines of a block of points the overlay wrote, two points to a line. */
    private void drawLines(float[] points, int lines) {
        for (int line = 0; line < lines; line++) {
            int at = line * 2 * FaceOverlay.POINT_FLOATS;
            frame.line(points[at], points[at + 1], points[at + 2],
                    points[at + 3], points[at + 4], points[at + 5]);
        }
    }

    /**
     * Draws the cracks of the cell that is being broken.
     * <p>
     * The picture of the stage the break has reached is laid over every face of the cell, a hair outside it,
     * so the block shows through the cracks and a player sees what they are working on and how far they are,
     * see {@link BreakOverlay}. It is drawn after the world with the depth test still on, so a wall in front
     * of the cell hides the cracks the way it hides the block itself, and it is blended instead of written
     * over the picture: the cracks are a dark drawing on a transparent ground.
     * <p>
     * The pass runs under the shader of the world with the fog of the frame, so the cracks fade into the sky
     * with the block they sit on and nothing here has to know about the distance.
     *
     * @param camera camera the world is seen through
     * @param target cell that is being broken, {@code null} draws nothing
     * @param progress progress of that break, {@code 0} to {@code 1}, {@code 0} draws nothing
     * @param sky colour of the sky, also the colour the distance fades into
     */
    public void renderBreaking(Camera camera, BlockTarget target, float progress, Color sky) {
        if (target == null) {
            return;
        }
        Mesh mesh = breaking.meshFor(progress);
        if (mesh == null) {
            return;
        }
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shader.begin(camera, pictures, sky, camera.far * FOG_START_SHARE, camera.far);
        model.idt().translate(target.x(), target.y(), target.z());
        shader.render(mesh, model);
        shader.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    /** Stages of a break whose cracks are meshed right now, which is what the status line may report. */
    public int cachedBreakStageCount() {
        return breaking.stageCount();
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
        breaking.dispose();
    }

    @Override
    public String toString() {
        return "WorldRenderer3D(" + drawnSections + " sections, " + drawnMeshes + " meshes)";
    }
}
