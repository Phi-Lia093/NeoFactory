package com.philia093.neofactory.render;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.philia093.neofactory.entity.Entity;
import com.philia093.neofactory.entity.ItemEntity;
import com.philia093.neofactory.item.Item;
import com.philia093.neofactory.item.ItemStack;
import com.philia093.neofactory.util.Constants;

/**
 * Draws an item that lies on the ground.
 * <p>
 * The icon is the one the inventory shows, shrunk to {@code 1 / sqrt(2)} of a block, which
 * is the size the original game gives a dropped item: the smaller square sits inside the
 * block it covers and leaves the ground around it visible, so a drop never looks like a
 * block of its own. A bright outline is drawn behind the icon, which keeps the item
 * readable on a floor of any colour.
 * <p>
 * The item does not turn: it floats gently up and down. A turning icon of a view from
 * above always read as a wheel instead of as an object lying on the ground, while the
 * rising and falling motion is what the original game uses to say "you may take this".
 */
public class ItemEntityRenderer implements EntityRenderer {

    /**
     * Side length of the icon, as a share of a block.
     * <p>
     * {@code 1 / sqrt(2)} is the diagonal of a half block: the icon covers the circle that
     * fits into the block, so its corners stay inside the cell and the outline never
     * reaches into a neighbouring tile.
     */
    private static final float ICON_SCALE = (float) (1.0 / Math.sqrt(2.0));

    /** Height in blocks a dropped item floats up and down. */
    public static final float BOB_BLOCKS = 0.06f;

    /** Times a dropped item floats up and down per second. */
    public static final float BOB_SPEED = 1.6f;

    private final SpriteBatch batch;
    private final BlockTextureCache textures;

    /**
     * Creates a renderer.
     *
     * @param batch sprite batch used for drawing, disposed by its owner
     * @param textures texture cache providing the item icons
     */
    public ItemEntityRenderer(SpriteBatch batch, BlockTextureCache textures) {
        this.batch = batch;
        this.textures = textures;
    }

    /**
     * Height a dropped item floats above the place it lies on.
     *
     * @param age seconds since the item was dropped
     * @return the offset in world units, positive means higher
     */
    public static float bobOffset(float age) {
        return MathUtils.sin(age * BOB_SPEED * MathUtils.PI2) * BOB_BLOCKS * Constants.TILE_SIZE;
    }

    /**
     * Side length of a dropped item icon, in world units.
     * <p>
     * One block is {@link Constants#TILE_SIZE} units, so the icon covers
     * {@code 1 / sqrt(2)} of the cell it lies in.
     *
     * @return the side length in world units
     */
    public static float iconSize() {
        return Constants.ITEM_ICON_SIZE * ICON_SCALE;
    }

    @Override
    public void render(Entity entity) {
        if (!(entity instanceof ItemEntity itemEntity)) {
            return;
        }
        ItemStack stack = itemEntity.stack();
        if (stack.isEmpty()) {
            return;
        }
        Item item = stack.item();
        TextureRegion icon = textures.itemIcon(item);
        if (icon == null) {
            return;
        }

        float size = iconSize();
        float half = size * 0.5f;
        // One pixel of the icon as it is drawn in the world, the outline is that large.
        float pixel = size / Constants.ITEM_ICON_SIZE;
        float left = itemEntity.position().x - half;
        float bottom = itemEntity.position().z - half + bobOffset(itemEntity.age());

        TextureRegion outline = textures.itemOutline(item);
        if (outline != null) {
            // The outline stays white, only the icon behind it carries the tint of the block.
            batch.setColor(Color.WHITE);
            batch.draw(outline, left - pixel, bottom - pixel, size + 2 * pixel, size + 2 * pixel);
        }
        batch.setColor(item.tint());
        batch.draw(icon, left, bottom, size, size);
        // A shape that covers something carries a second layer, see Item#overlayTexture: it lies
        // on the icon in its own colours, which is how a wire keeps the core it was drawn around.
        if (item.hasOverlay()) {
            TextureRegion overlay = textures.iconRegion(item.overlayTexture(), 0);
            if (overlay != null) {
                batch.setColor(Color.WHITE);
                batch.draw(overlay, left, bottom, size, size);
            }
        }
        batch.setColor(Color.WHITE);
    }
}
