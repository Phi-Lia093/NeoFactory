package com.philia093.neofactory.gui.panel;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;

/**
 * The pictures a container is drawn from.
 * <p>
 * A container is a panel with slots in it, and where those two pictures come from is a
 * question of the screen: the inventory of the player is drawn from
 * {@code gui/inventory_icons.png}, a machine from {@code gui/machine_icons.png}, which
 * carries a panel that is empty where a machine stands its own slots. Everything else
 * about drawing a container is the same for both, see
 * {@link com.philia093.neofactory.gui.container.ContainerView}.
 * <p>
 * A missing picture never breaks a screen: an implementation falls back to a plain
 * rectangle and to no bevel at all, so the container stays usable while the art is
 * being worked on.
 */
public interface ContainerAppearance {

    /**
     * Column and row a slot names when it wants the plain picture of this appearance.
     * <p>
     * An appearance that owns more than one picture of a slot - a machine screen has one per
     * kind of slot, see {@link MachineTextures#icon(int, int)} - draws the cell a slot names
     * and its own default otherwise.
     */
    int DEFAULT_ICON = -1;

    /**
     * Draws the panel a container stands in.
     *
     * @param batch batch switched to the projection of the interface viewport
     * @param x left edge in interface pixels
     * @param y lower edge in interface pixels, the interface measures upwards
     * @param width width the panel is stretched to
     * @param height height the panel is stretched to
     */
    void drawPanel(SpriteBatch batch, float x, float y, float width, float height);

    /**
     * Draws the bevel of one slot, so an empty slot still reads as a place an item goes
     * into.
     *
     * @param batch batch switched to the projection of the interface viewport
     * @param x left edge of the cell the slot belongs to
     * @param y lower edge of that cell
     * @param column column of the cell of the sheet to draw, {@link #DEFAULT_ICON} for the
     *               plain picture of this appearance
     * @param row row of that cell
     */
    void drawSlot(SpriteBatch batch, float x, float y, int column, int row);
}
