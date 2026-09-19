package com.philia093.neofactory.render;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.philia093.neofactory.entity.Player;
import com.philia093.neofactory.util.Constants;

/**
 * Draws the player marker on top of the world.
 * <p>
 * The marker is the small arrow of the top left cell of {@code map/map_icons.png}.
 * That cell is 8 by 8 pixels, much smaller than the 16 pixel block texture, so
 * {@link Constants#PLAYER_ICON_SCALE} is used to blow it up to exactly one block.
 * The marker is centered on the player position and turns with the facing
 * direction, which is what makes it readable where the player is heading.
 */
public class PlayerRenderer {

    /**
     * Correction between the direction the icon points at and the angle libGDX
     * expects.
     * <p>
     * The arrow points towards the top edge of its cell and libGDX measures
     * rotation counter clockwise from the positive X axis, so a facing of
     * {@code (0, 1)} has to be rotated by -90 degrees to keep the tip up.
     */
    private static final float ICON_TIP_OFFSET_DEGREES = -90.0f;

    private final SpriteBatch batch;
    private final BlockTextureCache textures;

    /**
     * Creates a renderer.
     *
     * @param batch sprite batch used for drawing, disposed by its owner
     * @param textures texture cache providing the map icons
     */
    public PlayerRenderer(SpriteBatch batch, BlockTextureCache textures) {
        this.batch = batch;
        this.textures = textures;
    }

    /**
     * Draws the player marker at the player position.
     * <p>
     * The caller is responsible for setting the projection matrix and for calling
     * {@link SpriteBatch#begin()} and {@link SpriteBatch#end()}.
     *
     * @param player player providing the position and the facing direction
     */
    public void render(Player player) {
        TextureRegion icon = textures.mapIcon(Constants.MAP_ICON_PLAYER);
        if (icon == null) {
            return;
        }

        // The icon cell is 8 pixels wide, scaling it by two makes it fill one block.
        float size = Constants.MAP_ICON_CELL_SIZE * Constants.PLAYER_ICON_SCALE;
        float half = size * 0.5f;
        float centerX = player.position().x;
        float centerY = player.position().y;

        float angle = MathUtils.atan2(player.facing().y, player.facing().x)
                * MathUtils.radiansToDegrees + ICON_TIP_OFFSET_DEGREES;

        batch.draw(icon,
                centerX - half, centerY - half,
                half, half,
                size, size,
                1.0f, 1.0f,
                angle);
    }
}
