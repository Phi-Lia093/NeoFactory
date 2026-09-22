package com.philia093.neofactory.gui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.philia093.neofactory.item.Item;
import com.philia093.neofactory.item.ItemStack;
import com.philia093.neofactory.render.BlockTextureCache;
import com.philia093.neofactory.render.PixelFont;
import com.philia093.neofactory.util.Constants;

/**
 * Draws an item stack the way a slot of the interface shows it: the icon of the
 * item and, when the stack holds more than one, the amount in the lower right
 * corner.
 * <p>
 * A block item carries the colour of its block, so a grey scale sheet such as
 * {@code grass_top.png} appears green inside a slot as well.
 */
public class GuiItemRenderer {

    /**
     * Distance between the upper edge of a slot and the top of the amount.
     * <p>
     * The amount sits in the lower right corner of the slot and reaches one pixel
     * below it, which is how the original interface draws it.
     */
    private static final int COUNT_TOP_OFFSET = 9;

    private final BlockTextureCache textures;
    private final PixelFont font;

    /**
     * Creates a renderer.
     *
     * @param textures texture cache providing the item icons
     * @param font font used for the amount of a stack
     */
    public GuiItemRenderer(BlockTextureCache textures, PixelFont font) {
        this.textures = textures;
        this.font = font;
    }

    /**
     * Draws a stack into a slot.
     *
     * @param batch batch switched to the interface projection
     * @param stack stack to draw, an empty stack draws nothing
     * @param x left edge of the slot
     * @param y lower edge of the slot
     */
    public void render(SpriteBatch batch, ItemStack stack, float x, float y) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        Item item = stack.item();
        TextureRegion icon = textures.itemIcon(item);
        if (icon != null) {
            batch.setColor(item.tint());
            batch.draw(icon, x, y, Constants.ITEM_ICON_SIZE, Constants.ITEM_ICON_SIZE);
        }
        // A shape that covers something carries a second layer, see Item#overlayTexture: it is
        // drawn in its own colours over the icon and under the amount of the stack.
        if (item.hasOverlay()) {
            TextureRegion overlay = textures.iconRegion(item.overlayTexture(), 0);
            if (overlay != null) {
                batch.setColor(Color.WHITE);
                batch.draw(overlay, x, y, Constants.ITEM_ICON_SIZE, Constants.ITEM_ICON_SIZE);
            }
        }

        if (stack.count() > 1) {
            String amount = Integer.toString(stack.count());
            font.setColor(Color.WHITE);
            font.drawShadowed(batch, amount,
                    x + InventoryLayout.SLOT_SIZE + 1 - font.width(amount),
                    y + InventoryLayout.SLOT_SIZE - COUNT_TOP_OFFSET);
        }
        batch.setColor(Color.WHITE);
    }
}
