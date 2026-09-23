package com.philia093.neofactory.render;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.philia093.neofactory.util.Constants;
import com.philia093.neofactory.world.World;
import com.philia093.neofactory.world.interaction.BlockTarget;

/**
 * Draws the frame around the block an action would touch.
 * <p>
 * The frame is drawn as four thin bars instead of a filled rectangle, so the block
 * stays visible while the player aims at it.
 * <p>
 * Its colour describes the column that is framed, and it does not depend on the
 * layer the player asked for with the shift key:
 * <ul>
 *     <li>white while the column carries a block in the layer the player stands in,
 *         so there is something to act on right there</li>
 *     <li>yellow while the column carries nothing but its ground, so the layer below
 *         the feet is the only one that holds anything</li>
 * </ul>
 * A column with something in the player layer cannot be dug below, and a column
 * without ground cannot be built on: the colour tells which case the player is
 * looking at before the mouse button is pressed.
 */
public class SelectionRenderer {

    /** Thickness of the bars in world units. */
    private static final float THICKNESS = 2.0f;

    /** Colour of a column that holds a block in the layer the player stands in. */
    private static final Color OCCUPIED_COLOR = new Color(1.0f, 1.0f, 1.0f, 0.85f);

    /** Colour of a column that holds nothing but its ground. */
    private static final Color GROUND_COLOR = new Color(1.0f, 0.85f, 0.25f, 0.85f);

    private final SpriteBatch batch;
    private final BlockTextureCache textures;

    /**
     * Creates a renderer.
     *
     * @param batch sprite batch used for drawing, disposed by its owner
     * @param textures texture cache providing the white pixel the bars are made of
     */
    public SelectionRenderer(SpriteBatch batch, BlockTextureCache textures) {
        this.batch = batch;
        this.textures = textures;
    }

    /**
     * Draws the frame of a target.
     * <p>
     * The caller is responsible for setting the projection matrix and for calling
     * {@link SpriteBatch#begin()} and {@link SpriteBatch#end()}.
     *
     * @param world world holding the framed column
     * @param target cell to frame, {@code null} draws nothing
     */
    public void render(World world, BlockTarget target) {
        if (target == null) {
            return;
        }
        TextureRegion pixel = textures.whitePixel();
        if (pixel == null) {
            return;
        }

        float size = Constants.TILE_SIZE;
        float x = target.x() * size;
        float y = target.z() * size;
        batch.setColor(colorOf(world, target));
        batch.draw(pixel, x, y, size, THICKNESS);
        batch.draw(pixel, x, y + size - THICKNESS, size, THICKNESS);
        batch.draw(pixel, x, y, THICKNESS, size);
        batch.draw(pixel, x + size - THICKNESS, y, THICKNESS, size);
        batch.setColor(Color.WHITE);
    }

    /**
     * Colour the frame of a column is drawn with.
     *
     * @param world world holding the column
     * @param target cell that is framed
     * @return white while the column holds a block in the layer of the player,
     *         yellow while it holds nothing but its ground
     */
    public static Color colorOf(World world, BlockTarget target) {
        return world.hasBlock(target.x(), target.y(), target.z()) ? OCCUPIED_COLOR : GROUND_COLOR;
    }
}
