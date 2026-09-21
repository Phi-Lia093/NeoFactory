package com.philia093.neofactory.gui.container;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.philia093.neofactory.gui.GuiItemRenderer;
import com.philia093.neofactory.gui.GuiViewport;
import com.philia093.neofactory.gui.panel.ArrowElement;
import com.philia093.neofactory.gui.panel.PanelTextures;
import com.philia093.neofactory.item.ItemStack;
import com.philia093.neofactory.render.BlockTextureCache;
import com.philia093.neofactory.render.PixelFont;
import com.philia093.neofactory.util.Constants;

/**
 * Draws a {@link ContainerMenu}: the panel, its slots, the items in them and the stack
 * the mouse carries.
 * <p>
 * The panel comes from {@link PanelTextures} and is stretched to whatever size the
 * layout of the menu asks for, which is what lets one picture serve the small
 * inventory of the player and the tall screen of a machine. The items themselves are
 * drawn by {@link GuiItemRenderer}, so a block item shows the same small cube here as
 * it does in the hotbar.
 * <p>
 * Coordinates of a menu are measured from the upper left corner of the panel with the
 * Y axis pointing down, the interface is drawn upwards from the lower left one; this
 * class is where the two meet.
 */
public final class ContainerView {

    /** Distance between the mouse and the nearest corner of a tooltip. */
    private static final int TOOLTIP_OFFSET = 12;

    /** Pixels of background between the frame of a tooltip and its text. */
    private static final int TOOLTIP_PADDING = 3;

    /** Thickness of the frame around a tooltip. */
    private static final int TOOLTIP_BORDER = 1;

    /** Colour of a tooltip background, shared and never mutated. */
    private static final Color TOOLTIP_FILL = new Color(0.062f, 0.0f, 0.062f, 0.94f);

    /** Colour of the frame around a tooltip, shared and never mutated. */
    private static final Color TOOLTIP_FRAME = new Color(0.313f, 0.313f, 0.0f, 1.0f);

    /**
     * Overlay drawn on the slot under the mouse, shared and never mutated.
     * <p>
     * A half transparent white, which is what brightens the slot in the original game
     * and makes it obvious where a click would land.
     */
    private static final Color HOVER_COLOR = new Color(1.0f, 1.0f, 1.0f, 0.5f);

    private final BlockTextureCache textures;
    private final PixelFont font;
    private final GuiItemRenderer items;

    /** Viewport of the interface, also the source of its size for the tooltip. */
    private final GuiViewport viewport;

    /** Pictures of the panel and of its elements. */
    private final PanelTextures panel;

    /** Arrow of the interface, used by a container that shows a progress. */
    private final ArrowElement arrows;

    /**
     * Creates a view.
     *
     * @param textures texture cache providing the panel and the item icons
     * @param font font used for the amounts and for the name of an item
     * @param viewport viewport of the interface, see {@link GuiViewport}
     */
    public ContainerView(BlockTextureCache textures, PixelFont font, GuiViewport viewport) {
        this.textures = textures;
        this.font = font;
        this.viewport = viewport;
        this.items = new GuiItemRenderer(textures, font);
        this.panel = new PanelTextures(textures);
        this.arrows = new ArrowElement(panel);
    }

    /** Pictures of the panel, so a screen can draw its own extras. */
    public PanelTextures panelTextures() {
        return panel;
    }

    /** Arrow that fills up with a progress, used by a machine or by the crafting arrow. */
    public ArrowElement arrows() {
        return arrows;
    }

    /** {@code true} when the pictures of the panel could be loaded. */
    public boolean isComplete() {
        return panel.isComplete();
    }

    /**
     * Something a container draws between its slots and the items in them.
     * <p>
     * A screen uses this for the decorations of a container: the arrow of a crafting
     * field, the fire of a furnace. Drawing happens before the items and before the
     * tooltip of a slot, so a decoration never covers the name of an item.
     */
    public interface PanelDecorator {

        /**
         * Draws the decoration of one container.
         *
         * @param batch batch switched to the projection of the interface viewport
         * @param panelX left edge of the panel in interface pixels
         * @param panelY lower edge of the panel in interface pixels
         * @param panelWidth width of the panel in pixels
         * @param panelHeight height of the panel in pixels
         */
        void draw(SpriteBatch batch, float panelX, float panelY, int panelWidth, int panelHeight);
    }

    /**
     * Draws the whole container.
     *
     * @param batch batch switched to the projection of the interface viewport
     * @param menu container to draw
     * @param x left edge of the panel in interface pixels
     * @param y lower edge of the panel in interface pixels
     * @param mouseX X coordinate of the mouse inside the interface
     * @param mouseY Y coordinate of the mouse inside the interface, from the bottom
     */
    public void render(SpriteBatch batch, ContainerMenu menu, float x, float y, float mouseX,
            float mouseY) {
        render(batch, menu, x, y, mouseX, mouseY, null);
    }

    /**
     * Draws the whole container with its decorations.
     *
     * @param batch batch switched to the projection of the interface viewport
     * @param menu container to draw
     * @param x left edge of the panel in interface pixels
     * @param y lower edge of the panel in interface pixels
     * @param mouseX X coordinate of the mouse inside the interface
     * @param mouseY Y coordinate of the mouse inside the interface, from the bottom
     * @param decorator decoration drawn between the slots and the items, may be {@code null}
     */
    public void render(SpriteBatch batch, ContainerMenu menu, float x, float y, float mouseX,
            float mouseY, PanelDecorator decorator) {
        ContainerLayout layout = menu.layout();
        int width = layout.panelWidth();
        int height = layout.panelHeight();

        batch.setColor(Color.WHITE);
        panel.drawPanel(batch, x, y, width, height);
        for (Slot slot : layout.slots()) {
            panel.drawSlot(batch, x + slot.x(), slotY(y, height, slot));
        }
        if (decorator != null) {
            decorator.draw(batch, x, y, width, height);
        }

        Slot hovered = hoveredSlot(menu, x, y, mouseX, mouseY);
        if (hovered != null) {
            drawHover(batch, x + hovered.x(), slotY(y, height, hovered));
        }

        for (Slot slot : layout.slots()) {
            items.render(batch, slot.stack(), x + slot.x(), slotY(y, height, slot));
        }

        if (!menu.cursorStack().isEmpty()) {
            items.render(batch, menu.cursorStack(),
                    mouseX - Constants.ITEM_ICON_SIZE * 0.5f,
                    mouseY - Constants.ITEM_ICON_SIZE * 0.5f);
        }
        drawTooltip(batch, hovered, mouseX, mouseY);
    }

    /**
     * Slot under the mouse.
     *
     * @param menu container that is drawn
     * @param x left edge of the panel in interface pixels
     * @param y lower edge of the panel in interface pixels
     * @param mouseX X coordinate of the mouse inside the interface
     * @param mouseY Y coordinate of the mouse inside the interface, from the bottom
     * @return the slot, or {@code null} when the mouse is beside every slot
     */
    public Slot hoveredSlot(ContainerMenu menu, float x, float y, float mouseX, float mouseY) {
        int height = menu.layout().panelHeight();
        return menu.slotAt(Math.round(mouseX) - Math.round(x), Math.round(y + height - mouseY));
    }

    /**
     * Lower edge of a slot, in the coordinates of the interface.
     *
     * @param panelY lower edge of the panel in interface pixels
     * @param panelHeight height of the panel in pixels
     * @param slot slot to place
     * @return the coordinate of its lower edge
     */
    public static float slotY(float panelY, int panelHeight, Slot slot) {
        return panelY + panelHeight - slot.y() - ContainerLayout.SLOT_SIZE;
    }

    /** Lightens the slot under the mouse. */
    private void drawHover(SpriteBatch batch, float x, float y) {
        TextureRegion pixel = textures.whitePixel();
        if (pixel == null) {
            return;
        }
        batch.setColor(HOVER_COLOR);
        batch.draw(pixel, x, y, ContainerLayout.SLOT_SIZE, ContainerLayout.SLOT_SIZE);
        batch.setColor(Color.WHITE);
    }

    /** Draws the name of the hovered item next to the mouse. */
    private void drawTooltip(SpriteBatch batch, Slot slot, float mouseX, float mouseY) {
        if (slot == null) {
            return;
        }
        TextureRegion pixel = textures.whitePixel();
        if (pixel == null) {
            return;
        }
        ItemStack stack = slot.stack();
        if (stack.isEmpty()) {
            return;
        }

        String name = stack.item().displayName();
        int border = TOOLTIP_BORDER + TOOLTIP_PADDING;
        int boxWidth = Math.round(font.width(name)) + 2 * border;
        int boxHeight = Math.round(font.lineHeight()) + 2 * border;

        // The tooltip sits above and right of the mouse and stays inside the window.
        int boxX = clamp(Math.round(mouseX) + TOOLTIP_OFFSET,
                Math.round(viewport.guiWidth()) - boxWidth);
        int boxY = clamp(Math.round(mouseY) + TOOLTIP_OFFSET,
                Math.round(viewport.guiHeight()) - boxHeight);

        batch.setColor(TOOLTIP_FRAME);
        batch.draw(pixel, boxX, boxY, boxWidth, boxHeight);
        batch.setColor(TOOLTIP_FILL);
        batch.draw(pixel, boxX + TOOLTIP_BORDER, boxY + TOOLTIP_BORDER,
                boxWidth - 2 * TOOLTIP_BORDER, boxHeight - 2 * TOOLTIP_BORDER);

        font.setColor(Color.WHITE);
        font.drawShadowed(batch, name, boxX + border, boxY + boxHeight - border);
        batch.setColor(Color.WHITE);
    }

    /** Keeps a coordinate inside the window, never below zero. */
    private static int clamp(int value, int maximum) {
        return Math.max(0, Math.min(value, maximum));
    }
}
