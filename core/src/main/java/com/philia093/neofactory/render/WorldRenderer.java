package com.philia093.neofactory.render;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;
import com.philia093.neofactory.block.Block;
import com.philia093.neofactory.util.Constants;
import com.philia093.neofactory.world.Chunk;
import com.philia093.neofactory.world.World;

import java.util.ArrayList;
import java.util.List;

/**
 * Draws the visible part of a {@link World} from above.
 * <p>
 * Every block is a single, whole tile: the ground layer is drawn first, then the
 * object layer with the trees and plants on top of it. Two passes over the
 * visible chunks are used instead of one so that a canopy is never hidden behind
 * the ground of the chunk next to it.
 * <p>
 * The renderer only reads chunks that are already loaded, drawing a frame never
 * triggers terrain generation.
 */
public class WorldRenderer implements Disposable {

    /** Extra blocks drawn outside the camera view to hide popping at the edges. */
    private static final int VIEW_MARGIN = 2;

    /**
     * Colour a hole in the ground is filled with, shared and never mutated.
     * <p>
     * The world has no depth, so a dug out cell would show the sky through the
     * floor. Pure black reads as a hole instead of as a missing tile.
     */
    private static final Color HOLE_COLOR = new Color(0.0f, 0.0f, 0.0f, 1.0f);

    /** SpriteBatch shared with the caller, it is not owned by this class. */
    private final SpriteBatch batch;
    private final BlockTextureCache textures;

    /** Reused buffer, allocating it per frame would create garbage. */
    private final List<Chunk> visibleChunks = new ArrayList<>();

    private int drawnTiles;

    /**
     * Creates a renderer.
     *
     * @param batch sprite batch used for drawing, disposed by its owner
     * @param textures texture cache providing the block regions
     */
    public WorldRenderer(SpriteBatch batch, BlockTextureCache textures) {
        this.batch = batch;
        this.textures = textures;
    }

    /**
     * Draws the world.
     * <p>
     * The caller is responsible for setting the projection matrix and for calling
     * {@link SpriteBatch#begin()} and {@link SpriteBatch#end()}.
     *
     * @param world world to draw
     * @param camera camera describing the visible area
     */
    public void render(World world, OrthographicCamera camera) {
        drawnTiles = 0;

        // The camera and the player live in world units, the culling works with
        // block coordinates, so the visible range has to be converted first.
        float tileSize = Constants.TILE_SIZE;
        float halfWidth = camera.viewportWidth * camera.zoom * 0.5f;
        float halfHeight = camera.viewportHeight * camera.zoom * 0.5f;
        int minX = (int) Math.floor((camera.position.x - halfWidth) / tileSize) - VIEW_MARGIN;
        int maxX = (int) Math.ceil((camera.position.x + halfWidth) / tileSize) + VIEW_MARGIN;
        int minY = (int) Math.floor((camera.position.y - halfHeight) / tileSize) - VIEW_MARGIN;
        int maxY = (int) Math.ceil((camera.position.y + halfHeight) / tileSize) + VIEW_MARGIN;

        collectVisibleChunks(world, minX, maxX, minY, maxY);

        // Pass one: the ground of every visible chunk.
        for (Chunk chunk : visibleChunks) {
            renderLayer(chunk, minX, maxX, minY, maxY, Chunk.LAYER_FLOOR);
        }

        // Pass two: trees, plants and everything the player placed.
        for (Chunk chunk : visibleChunks) {
            renderLayer(chunk, minX, maxX, minY, maxY, Chunk.LAYER_OBJECT);
        }

        batch.setColor(Color.WHITE);
    }

    /** Amount of tiles drawn by the last {@link #render(World, OrthographicCamera)} call. */
    public int drawnTileCount() {
        return drawnTiles;
    }

    /** Collects the loaded chunks that intersect the visible block range. */
    private void collectVisibleChunks(World world, int minX, int maxX, int minY, int maxY) {
        visibleChunks.clear();

        int firstChunkX = Chunk.chunkOf(minX);
        int lastChunkX = Chunk.chunkOf(maxX);
        int firstChunkY = Chunk.chunkOf(minY);
        int lastChunkY = Chunk.chunkOf(maxY);

        for (int chunkY = firstChunkY; chunkY <= lastChunkY; chunkY++) {
            for (int chunkX = firstChunkX; chunkX <= lastChunkX; chunkX++) {
                Chunk chunk = world.chunkIfLoaded(chunkX, chunkY);
                if (chunk != null) {
                    visibleChunks.add(chunk);
                }
            }
        }
    }

    /**
     * Draws one layer of a chunk inside the visible block range.
     *
     * @param chunk chunk to draw
     * @param minX lowest visible block X coordinate
     * @param maxX highest visible block X coordinate
     * @param minY lowest visible block Y coordinate
     * @param maxY highest visible block Y coordinate
     * @param layer layer to draw, see {@link Chunk#LAYER_FLOOR} and
     *              {@link Chunk#LAYER_OBJECT}
     */
    private void renderLayer(Chunk chunk, int minX, int maxX, int minY, int maxY, int layer) {
        int fromLocalX = Math.max(0, minX - chunk.originX());
        int toLocalX = Math.min(Constants.CHUNK_SIZE - 1, maxX - chunk.originX());
        int fromLocalY = Math.max(0, minY - chunk.originY());
        int toLocalY = Math.min(Constants.CHUNK_SIZE - 1, maxY - chunk.originY());
        if (fromLocalX > toLocalX || fromLocalY > toLocalY) {
            return;
        }

        int originX = chunk.originX();
        int originY = chunk.originY();
        for (int localY = fromLocalY; localY <= toLocalY; localY++) {
            for (int localX = fromLocalX; localX <= toLocalX; localX++) {
                Block block = chunk.getBlock(localX, localY, layer);
                if (block.isAir()) {
                    if (layer == Chunk.LAYER_FLOOR && chunk.isCellGenerated(localX, localY)) {
                        // The ground of a finished cell is always filled by the
                        // generator, so an empty one was dug out by the player.
                        drawHole(originX + localX, originY + localY);
                    }
                    continue;
                }
                drawTile(block, originX + localX, originY + localY);
            }
        }
    }

    /**
     * Draws the black gap of a hole in the ground.
     * <p>
     * The player can walk over a hole like over any other floor, only the look
     * changes: without the gap the sky colour would shine through the ground.
     *
     * @param x block X coordinate of the hole
     * @param y block Y coordinate of the hole
     */
    private void drawHole(int x, int y) {
        TextureRegion pixel = textures.whitePixel();
        if (pixel == null) {
            return;
        }
        float size = Constants.TILE_SIZE;
        batch.setColor(HOLE_COLOR);
        batch.draw(pixel, x * size, y * size, size, size);
        drawnTiles++;
    }

    /**
     * Draws a single block as a whole tile.
     * <p>
     * The tint of the block is applied as the batch colour, which is how the grey
     * scale ground and leaf sheets of the art pack receive their colours.
     *
     * @param block block to draw
     * @param x block X coordinate
     * @param y block Y coordinate
     */
    private void drawTile(Block block, int x, int y) {
        TextureRegion region = textures.region(block.texture());
        if (region == null) {
            return;
        }
        float size = Constants.TILE_SIZE;
        batch.setColor(block.tint());
        batch.draw(region, x * size, y * size, size, size);
        drawnTiles++;
    }

    @Override
    public void dispose() {
        // The sprite batch and the textures belong to the game, nothing to free here.
    }
}

