package com.philia093.neofactory.gui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.philia093.neofactory.item.PlayerInventory;
import com.philia093.neofactory.render.BlockTextureCache;
import com.philia093.neofactory.render.PixelFont;

/**
 * Draws the hotbar, the row of nine slots that stays visible at the bottom of the
 * window while the player walks around.
 * <p>
 * Background and selection frame come from {@code gui/widgets.png}. The frame is
 * one pixel larger than the background on every side and is drawn before the
 * items, so a selected item is never covered by it. The slot under the mouse is
 * lightened, which is the same feedback the inventory screen gives.
 * <p>
 * The bar sits on the bottom edge of the interface and all coordinates are virtual
 * pixels of {@link GuiViewport}.
 */
public class HotbarGui {

    /**
     * Overlay drawn on the slot under the mouse, shared and never mutated.
     * <p>
     * A half transparent white, which is what brightens the slot in the original
     * game and makes it obvious where a click would land.
     */
    private static final Color HOVER_COLOR = new Color(1.0f, 1.0f, 1.0f, 0.5f);

    private final BlockTextureCache textures;
    private final GuiItemRenderer itemRenderer;

    /** Viewport of the interface, also the source of its width in virtual pixels. */
    private final GuiViewport viewport;

    /** Background of the hotbar, {@code null} when the picture is missing. */
    private final TextureRegion background;

    /** Frame around the selected slot, {@code null} when the picture is missing. */
    private final TextureRegion selection;

    /**
     * Creates the hotbar.
     *
     * @param textures texture cache providing the widget
     * @param font font used for the amounts of the stacks
     * @param viewport viewport of the interface, see {@link GuiViewport}
     */
    public HotbarGui(BlockTextureCache textures, PixelFont font, GuiViewport viewport) {
        this.textures = textures;
        this.viewport = viewport;
        this.itemRenderer = new GuiItemRenderer(textures, font);
        this.background = textures.region(InventoryLayout.HOTBAR_TEXTURE, 0, 0,
                InventoryLayout.HOTBAR_WIDTH, InventoryLayout.HOTBAR_HEIGHT);
        this.selection = textures.region(InventoryLayout.HOTBAR_TEXTURE,
                InventoryLayout.HOTBAR_SELECTION_X, InventoryLayout.HOTBAR_SELECTION_Y,
                InventoryLayout.HOTBAR_SELECTION_SIZE, InventoryLayout.HOTBAR_SELECTION_SIZE);
    }

    /**
     * Draws the hotbar at the bottom of the window.
     *
     * @param batch batch switched to the projection of the interface viewport
     * @param inventory inventory holding the nine stacks
     * @param mouseX X coordinate of the mouse inside the interface
     * @param mouseY Y coordinate of the mouse inside the interface, from the bottom
     * @param hoverEnabled {@code false} while another screen covers the world, so the
     *                     bar does not react to the mouse behind it
     */
    public void render(SpriteBatch batch, PlayerInventory inventory,
            float mouseX, float mouseY, boolean hoverEnabled) {
        if (background == null) {
            return;
        }
        int originX = originX(viewport.guiWidth());
        int originY = originY();

        batch.setColor(Color.WHITE);
        batch.draw(background, originX, originY);
        drawSelection(batch, inventory.selectedSlot(), originX, originY);

        for (int slot = 0; slot < PlayerInventory.HOTBAR_SLOTS; slot++) {
            itemRenderer.render(batch, inventory.get(slot),
                    slotIconX(originX, slot), originY + InventoryLayout.HOTBAR_INSET);
        }

        // The highlight goes on top of the icons, so a filled slot is highlighted too.
        int hovered = hoverEnabled ? slotAt(mouseX, mouseY) : -1;
        drawHoverHighlight(batch, hovered, originX, originY);
    }

    /**
     * X coordinate of the left edge of the hotbar background.
     *
     * @param screenWidth width of the interface in virtual pixels
     */
    public static int originX(float screenWidth) {
        return Math.round(screenWidth * 0.5f) - InventoryLayout.HOTBAR_WIDTH / 2;
    }

    /**
     * Y coordinate of the lower edge of the hotbar background.
     * <p>
     * The bar sits on the bottom edge of the window, which is why the coordinate is
     * zero: libGDX measures interface coordinates from the bottom left corner
     * upwards, so a larger value would lift the bar into the world.
     */
    public static int originY() {
        return 0;
    }

    /** X coordinate of the icon of a hotbar slot. */
    private static int slotIconX(int originX, int slot) {
        return originX + InventoryLayout.HOTBAR_INSET + slot * InventoryLayout.HOTBAR_PITCH;
    }

    /**
     * Hotbar slot under the mouse.
     * <p>
     * Coordinates are the virtual pixels of the interface viewport, the same space
     * {@link #render} draws in. The query is public because the screen also uses it
     * to route clicks and wheel notches to the bar.
     *
     * @param mouseX X coordinate of the mouse inside the interface
     * @param mouseY Y coordinate of the mouse inside the interface, from the bottom
     * @return the slot index, or {@code -1} when the mouse is beside the icons
     */
    public int slotAt(float mouseX, float mouseY) {
        int originX = originX(viewport.guiWidth());
        int iconBottom = originY() + InventoryLayout.HOTBAR_INSET;
        if (mouseY < iconBottom || mouseY >= iconBottom + InventoryLayout.SLOT_SIZE) {
            return -1;
        }
        for (int slot = 0; slot < PlayerInventory.HOTBAR_SLOTS; slot++) {
            int iconLeft = slotIconX(originX, slot);
            if (mouseX >= iconLeft && mouseX < iconLeft + InventoryLayout.SLOT_SIZE) {
                return slot;
            }
        }
        return -1;
    }

    /** Lightens the hotbar slot under the mouse. */
    private void drawHoverHighlight(SpriteBatch batch, int slot, int originX, int originY) {
        TextureRegion pixel = textures.whitePixel();
        if (slot < 0 || pixel == null) {
            return;
        }
        batch.setColor(HOVER_COLOR);
        batch.draw(pixel, slotIconX(originX, slot), originY + InventoryLayout.HOTBAR_INSET,
                InventoryLayout.SLOT_SIZE, InventoryLayout.SLOT_SIZE);
        batch.setColor(Color.WHITE);
    }

    /** Draws the frame that marks the selected slot. */
    private void drawSelection(SpriteBatch batch, int selectedSlot, int originX, int originY) {
        if (selection == null || !InventoryLayout.isHotbarSlot(selectedSlot)) {
            return;
        }
        batch.setColor(Color.WHITE);
        batch.draw(selection,
                slotIconX(originX, selectedSlot) - 4,
                originY - 1,
                InventoryLayout.HOTBAR_SELECTION_SIZE,
                InventoryLayout.HOTBAR_SELECTION_SIZE);
    }
}
