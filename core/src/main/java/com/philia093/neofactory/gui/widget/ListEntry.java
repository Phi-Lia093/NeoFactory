package com.philia093.neofactory.gui.widget;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.philia093.neofactory.render.PixelFont;

/**
 * One row of a {@link ScrollListWidget}.
 * <p>
 * The list does not know what its rows show, it only knows how tall one is and
 * where it lies. A screen therefore implements this interface for its own rows,
 * which keeps a list of worlds and a list of anything else from needing two widget
 * classes.
 */
public interface ListEntry {

    /**
     * Draws the row.
     *
     * @param batch batch switched to the projection of the interface viewport
     * @param font font to draw the text with, its colour is set by the row
     * @param x left edge of the row in interface pixels
     * @param y lower edge of the row in interface pixels
     * @param width width of the row in interface pixels
     * @param height height of the row in interface pixels
     * @param hovered {@code true} while the mouse is over the row
     * @param selected {@code true} while the row is the selected one
     */
    void render(SpriteBatch batch, PixelFont font, float x, float y, float width, float height,
            boolean hovered, boolean selected);

    /** Short text naming the row, used for logging. */
    String label();
}
