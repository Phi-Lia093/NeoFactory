package com.philia093.neofactory.gui.container;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.philia093.neofactory.item.Item;
import com.philia093.neofactory.item.ItemStack;
import com.philia093.neofactory.item.Items;
import com.philia093.neofactory.render.PixelFont;

import java.util.ArrayList;
import java.util.List;

/**
 * Draws the little box that names what stands under the mouse.
 * <p>
 * The box follows the pointer, stays inside the window and holds one line per thing the caller
 * has to say about the item. The lines come from the item itself, see {@link Item#tooltipLines()}:
 * the name first and the chemical formula of the material after it, so a plate of iron reads as
 * {@code "Iron Plate"} over {@code "Fe"}. A screen that has more to say - a machine naming the
 * energy it holds, a tab of the creative inventory naming what it lists - hands its own lines in.
 * <p>
 * Everything about the look lives here as well, because two screens draw the same box: the window
 * of the player and the creative inventory, see {@code ContainerView} and {@code
 * CreativeInventoryGui}. The arithmetic is plain integers, so the size of a box can be checked
 * without a window, see {@code ItemTooltipTest}.
 * <p>
 * A stack that wears out names what is left of it under the formula: a pickaxe reads its name, the
 * formula of its material and how much life is still in it, which is the same number the bar of its
 * slot shows, see {@link com.philia093.neofactory.item.Damageable}.
 */
public final class ItemTooltip {

    /** Word the line about the life of a piece starts with. */
    public static final String LIFE_LABEL = "Durability: ";

    /** Distance between the mouse and the nearest corner of the box. */
    public static final int OFFSET = 12;

    /** Pixels of background between the frame of the box and its text. */
    public static final int PADDING = 3;

    /** Thickness of the frame around the box. */
    public static final int BORDER = 1;

    /** Colour of the background of the box, shared and never mutated. */
    public static final Color FILL = new Color(0.062f, 0.0f, 0.062f, 0.94f);

    /** Colour of the frame of the box, shared and never mutated. */
    public static final Color FRAME = new Color(0.313f, 0.313f, 0.0f, 1.0f);

    private ItemTooltip() {
        // Utility class: never instantiated.
    }

    /**
     * Lines the tooltip of a stack is drawn from.
     * <p>
     * A stack that wears out adds what is left of it, so a pickaxe names its material and its life
     * while a stone names itself, see {@link com.philia093.neofactory.item.Damageable}.
     *
     * @param stack stack under the mouse, may be {@code null}
     * @return the lines, empty for a stack that holds nothing
     */
    public static List<String> linesOf(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return List.of();
        }
        List<String> lines = linesOf(stack.item());
        if (lines.isEmpty() || !stack.isDamageable()) {
            return lines;
        }
        List<String> withLife = new ArrayList<>(lines);
        withLife.add(LIFE_LABEL + stack.remainingDamage() + " / " + stack.maxDamage());
        return withLife;
    }

    /**
     * Lines the tooltip of an item is drawn from.
     *
     * @param item item under the mouse, may be {@code null}
     * @return the lines, empty for air and for no item at all
     */
    public static List<String> linesOf(Item item) {
        if (item == null || item == Items.AIR || !item.hasTexture()) {
            return List.of();
        }
        return item.tooltipLines();
    }

    /**
     * Width the box of these lines needs, the text without frame and padding.
     *
     * @param font font the lines are measured with
     * @param lines lines of the tooltip
     * @return the width of the widest line in pixels
     */
    public static int width(PixelFont font, List<String> lines) {
        float widest = 0.0f;
        for (String line : lines) {
            widest = Math.max(widest, font.width(line));
        }
        return Math.round(widest);
    }

    /**
     * Height the box of these lines needs, the text without frame and padding.
     *
     * @param font font the lines are measured with
     * @param lines lines of the tooltip
     * @return the height of every line together in pixels
     */
    public static int height(PixelFont font, List<String> lines) {
        return Math.round(font.lineHeight()) * lines.size();
    }

    /**
     * Draws the box beside the mouse.
     *
     * @param batch batch switched to the interface projection
     * @param font font the lines are drawn with
     * @param pixel single white pixel the box is filled with, {@code null} draws nothing
     * @param lines lines of the tooltip, an empty list draws nothing
     * @param mouseX X coordinate of the mouse inside the interface
     * @param mouseY Y coordinate of the mouse inside the interface, from the bottom
     * @param guiWidth width of the interface, the box stays inside it
     * @param guiHeight height of the interface, the box stays inside it
     */
    public static void draw(SpriteBatch batch, PixelFont font, TextureRegion pixel,
            List<String> lines, float mouseX, float mouseY, float guiWidth, float guiHeight) {
        if (lines.isEmpty() || pixel == null) {
            return;
        }
        int border = BORDER + PADDING;
        int lineHeight = Math.round(font.lineHeight());
        int boxWidth = width(font, lines) + 2 * border;
        int boxHeight = lineHeight * lines.size() + 2 * border;

        int boxX = clamp(Math.round(mouseX) + OFFSET, Math.round(guiWidth) - boxWidth);
        int boxY = clamp(Math.round(mouseY) + OFFSET, Math.round(guiHeight) - boxHeight);

        batch.setColor(FRAME);
        batch.draw(pixel, boxX, boxY, boxWidth, boxHeight);
        batch.setColor(FILL);
        batch.draw(pixel, boxX + BORDER, boxY + BORDER, boxWidth - 2 * BORDER,
                boxHeight - 2 * BORDER);

        font.setColor(Color.WHITE);
        for (int i = 0; i < lines.size(); i++) {
            // The first line fills the top of the box, every line below it one line lower.
            font.drawShadowed(batch, lines.get(i), boxX + border,
                    boxY + boxHeight - border - i * lineHeight);
        }
        batch.setColor(Color.WHITE);
    }

    /** Keeps a coordinate inside the window, never below zero. */
    private static int clamp(int value, int maximum) {
        return Math.max(0, Math.min(value, maximum));
    }
}
