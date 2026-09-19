package com.philia093.neofactory.gui;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.philia093.neofactory.item.Item;
import com.philia093.neofactory.item.ItemStack;
import com.philia093.neofactory.item.PlayerInventory;
import com.philia093.neofactory.render.BlockTextureCache;
import com.philia093.neofactory.render.PixelFont;
import com.philia093.neofactory.util.Constants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * The inventory screen of the player.
 * <p>
 * It shows the container picture {@code gui/container/inventory.png}, the contents
 * of the {@link PlayerInventory}, the slot under the mouse and the stack the mouse
 * carries. The screen is opened and closed with the inventory key of {@link
 * com.philia093.neofactory.input.InputHandler}.
 * <p>
 * All coordinates are virtual pixels of {@link GuiViewport}, which is also what
 * turns the position of the mouse into them, so the drawn container and the slots a
 * click can hit are always the same and stay so in fullscreen or after a resize.
 * <p>
 * Clicking a slot moves stacks around the way the original game does: the left
 * button takes or drops a whole stack and the right button takes half a stack or
 * drops a single item. A click outside the grid puts the carried stack back into
 * the inventory, because the game has no dropped items yet.
 * <p>
 * The armour column and the crafting group are drawn, but they hold nothing: the
 * pictures of the empty armour slots come from the {@code items} folder, and
 * {@link InventoryLayout} knows where they are.
 */
public class InventoryGui {

    private static final Logger LOGGER = LogManager.getLogger();

    /** Label drawn above the two by two crafting grid. */
    private static final String CRAFTING_LABEL = "Crafting";

    /** Label drawn above the storage grid. */
    private static final String INVENTORY_LABEL = "Inventory";

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

    /** Colour of the two labels inside the container, shared and never mutated. */
    private static final Color LABEL_COLOR = new Color(0.25f, 0.25f, 0.25f, 1.0f);

    /**
     * Overlay drawn on the slot under the mouse, shared and never mutated.
     * <p>
     * A half transparent white, which is what brightens the slot in the original
     * game and makes it obvious where a click would land.
     */
    private static final Color HOVER_COLOR = new Color(1.0f, 1.0f, 1.0f, 0.5f);

    private final BlockTextureCache textures;
    private final PixelFont font;
    private final GuiItemRenderer itemRenderer;
    private final PlayerInventory inventory;

    /** Viewport of the interface, also the source of its size in virtual pixels. */
    private final GuiViewport viewport;

    /** Frame of the container, {@code null} when the picture is missing. */
    private final TextureRegion container;

    /** Faint pictures of the empty armour slots, in the order of the column. */
    private final TextureRegion[] armorPlaceholders;

    private boolean open;

    /** Stack the mouse carries, {@link ItemStack#EMPTY} when nothing is held. */
    private ItemStack cursorStack = ItemStack.EMPTY;

    /**
     * Creates the inventory screen.
     *
     * @param textures texture cache providing the container and the icons
     * @param font font used for the labels, the amounts and the tooltip
     * @param inventory inventory the screen shows and changes
     * @param viewport viewport of the interface, see {@link GuiViewport}
     */
    public InventoryGui(BlockTextureCache textures, PixelFont font, PlayerInventory inventory,
            GuiViewport viewport) {
        this.textures = textures;
        this.font = font;
        this.inventory = inventory;
        this.viewport = viewport;
        this.itemRenderer = new GuiItemRenderer(textures, font);
        this.container = textures.region(InventoryLayout.CONTAINER_TEXTURE, 0, 0,
                InventoryLayout.CONTAINER_WIDTH, InventoryLayout.CONTAINER_HEIGHT);
        this.armorPlaceholders = new TextureRegion[] {
                textures.iconRegion(Item.ITEM_FOLDER + "empty_armor_slot_helmet", 0),
                textures.iconRegion(Item.ITEM_FOLDER + "empty_armor_slot_chestplate", 0),
                textures.iconRegion(Item.ITEM_FOLDER + "empty_armor_slot_leggings", 0),
                textures.iconRegion(Item.ITEM_FOLDER + "empty_armor_slot_boots", 0),
        };
    }

    /** {@code true} while the screen covers the world. */
    public boolean isOpen() {
        return open;
    }

    /** Opens the screen when it is closed and closes it otherwise. */
    public void toggle() {
        if (open) {
            close();
        } else {
            open = true;
        }
    }

    /**
     * Closes the screen.
     * <p>
     * A stack the mouse is carrying is put back into the inventory, so a player
     * never loses items by closing the screen. Should the inventory be full, the
     * screen stays open and the stack stays on the mouse until there is room for
     * it again.
     */
    public void close() {
        if (!open) {
            return;
        }
        if (!cursorStack.isEmpty()) {
            int leftover = inventory.add(cursorStack);
            if (leftover > 0) {
                cursorStack = ItemStack.of(cursorStack.item(), leftover);
                LOGGER.warn("The inventory is full, {} x {} stay on the mouse",
                        leftover, cursorStack.item().name());
                return;
            }
            cursorStack = ItemStack.EMPTY;
        }
        open = false;
    }

    /** Stack the mouse currently carries. */
    public ItemStack cursorStack() {
        return cursorStack;
    }

    /**
     * Handles a mouse button press.
     * <p>
     * The coordinates are virtual pixels of the interface, the very same ones the
     * screen is drawn in, so a click always lands on the slot the player sees. Every
     * button is swallowed while the screen is open, so a click never reaches the
     * world. The left button moves a whole stack, the right button half a stack or a
     * single item.
     *
     * @param guiX X coordinate of the mouse inside the interface
     * @param guiY Y coordinate of the mouse inside the interface, from the bottom
     * @param button mouse button that was pressed
     * @return {@code true} when the press was consumed
     */
    public boolean touchDown(float guiX, float guiY, int button) {
        if (!open) {
            return false;
        }
        int slot = InventoryLayout.slotAt(Math.round(guiX) - containerX(), toLayoutY(guiY));
        if (slot < 0) {
            putCursorStackBack();
            return true;
        }
        if (button == Input.Buttons.RIGHT) {
            moveSingleItem(slot);
        } else {
            moveWholeStack(slot);
        }
        return true;
    }

    /**
     * Moves a whole stack, which is what the left button does.
     * <p>
     * An empty mouse takes the stack out of the slot, a full mouse drops it into an
     * empty slot, tops up a stack of the same item or swaps two different items.
     *
     * @param slot slot that was clicked
     */
    private void moveWholeStack(int slot) {
        ItemStack slotStack = inventory.get(slot);
        if (cursorStack.isEmpty()) {
            cursorStack = inventory.remove(slot);
            return;
        }
        if (slotStack.isEmpty()) {
            inventory.set(slot, cursorStack);
            cursorStack = ItemStack.EMPTY;
            return;
        }
        if (slotStack.isStackableWith(cursorStack)) {
            int leftover = slotStack.grow(cursorStack.count());
            cursorStack = ItemStack.of(cursorStack.item(), leftover);
            return;
        }
        // Two different items change places.
        inventory.set(slot, cursorStack);
        cursorStack = slotStack;
    }

    /**
     * Moves half a stack or a single item, which is what the right button does.
     *
     * @param slot slot that was clicked
     */
    private void moveSingleItem(int slot) {
        ItemStack slotStack = inventory.get(slot);
        if (cursorStack.isEmpty()) {
            if (slotStack.isEmpty()) {
                return;
            }
            // Half of an odd amount rounds up.
            ItemStack taken = slotStack.split((slotStack.count() + 1) / 2);
            if (slotStack.isEmpty()) {
                inventory.set(slot, ItemStack.EMPTY);
            }
            cursorStack = taken;
            return;
        }
        if (slotStack.isEmpty()) {
            inventory.set(slot, ItemStack.of(cursorStack.item(), 1));
            takeSingleItem();
            return;
        }
        if (slotStack.isStackableWith(cursorStack) && !slotStack.isFull()) {
            slotStack.grow(1);
            takeSingleItem();
        }
        // A right click on a different item does nothing, like in the original game.
    }

    /** Removes a single item from the carried stack. */
    private void takeSingleItem() {
        cursorStack.split(1);
        if (cursorStack.count() <= 0) {
            cursorStack = ItemStack.EMPTY;
        }
    }

    /** Puts the carried stack back into the inventory, a click beside the grid asks for it. */
    private void putCursorStackBack() {
        if (cursorStack.isEmpty()) {
            return;
        }
        int leftover = inventory.add(cursorStack);
        cursorStack = ItemStack.of(cursorStack.item(), leftover);
    }

    /** X coordinate of the left edge of the container inside the interface. */
    private int containerX() {
        return Math.round(viewport.guiWidth() * 0.5f) - InventoryLayout.CONTAINER_WIDTH / 2;
    }

    /** Y coordinate of the lower edge of the container inside the interface. */
    private int containerY() {
        return Math.round(viewport.guiHeight() * 0.5f) - InventoryLayout.CONTAINER_HEIGHT / 2;
    }

    /**
     * Converts the Y coordinate of the mouse into a coordinate of the container.
     * <p>
     * The layout is measured from the top edge of the picture downwards, while the
     * interface measures from the bottom upwards, so the coordinate is mirrored here.
     *
     * @param guiY Y coordinate of the mouse inside the interface, from the bottom
     * @return the matching coordinate inside the container
     */
    private int toLayoutY(float guiY) {
        int fromBottom = Math.round(guiY) - containerY();
        return InventoryLayout.CONTAINER_HEIGHT - 1 - fromBottom;
    }

    /**
     * Converts a coordinate of the container into the lower edge of a slot.
     *
     * @param layoutY Y coordinate of the slot, measured from the top of the container
     * @return the Y coordinate of the lower edge of that slot on screen
     */
    private int slotBottom(int layoutY) {
        return containerY() + InventoryLayout.CONTAINER_HEIGHT - layoutY - InventoryLayout.SLOT_SIZE;
    }

    /**
     * Draws the screen.
     *
     * @param batch batch switched to the projection of the interface viewport
     * @param mouseX X coordinate of the mouse inside the interface
     * @param mouseY Y coordinate of the mouse inside the interface, from the bottom
     */
    public void render(SpriteBatch batch, float mouseX, float mouseY) {
        if (!open || container == null) {
            return;
        }
        batch.setColor(Color.WHITE);
        batch.draw(container, containerX(), containerY());

        int hovered = hoveredSlot(mouseX, mouseY);
        drawArmorPlaceholders(batch);
        drawLabels(batch);
        drawStorage(batch);
        drawHoverHighlight(batch, hovered);

        drawCursorStack(batch, mouseX, mouseY);
        drawTooltip(batch, hovered, mouseX, mouseY);
    }

    /**
     * Lightens the slot under the mouse, the way the original interface does.
     * <p>
     * The overlay is drawn on top of the items, so a slot that holds something is
     * highlighted as well.
     */
    private void drawHoverHighlight(SpriteBatch batch, int slot) {
        TextureRegion pixel = textures.whitePixel();
        if (slot < 0 || pixel == null) {
            return;
        }
        batch.setColor(HOVER_COLOR);
        batch.draw(pixel,
                containerX() + InventoryLayout.slotX(slot),
                slotBottom(InventoryLayout.slotY(slot)),
                InventoryLayout.SLOT_SIZE,
                InventoryLayout.SLOT_SIZE);
        batch.setColor(Color.WHITE);
    }

    /** Draws every stack of the player inventory into its slot. */
    private void drawStorage(SpriteBatch batch) {
        for (int slot = 0; slot < inventory.size(); slot++) {
            itemRenderer.render(batch, inventory.get(slot),
                    containerX() + InventoryLayout.slotX(slot),
                    slotBottom(InventoryLayout.slotY(slot)));
        }
    }

    /** Draws the faint pictures of the empty armour slots. */
    private void drawArmorPlaceholders(SpriteBatch batch) {
        for (int index = 0; index < armorPlaceholders.length; index++) {
            TextureRegion icon = armorPlaceholders[index];
            if (icon == null) {
                continue;
            }
            batch.setColor(Color.WHITE);
            batch.draw(icon, containerX() + InventoryLayout.ARMOR_COLUMN_X,
                    slotBottom(InventoryLayout.armorSlotY(index)),
                    Constants.ITEM_ICON_SIZE, Constants.ITEM_ICON_SIZE);
        }
    }

    /** Draws the two labels of the container, centred above the grids they belong to. */
    private void drawLabels(SpriteBatch batch) {
        font.setColor(LABEL_COLOR);

        int craftingCenter = InventoryLayout.CRAFT_GRID_X
                + (InventoryLayout.SLOT_PITCH + InventoryLayout.SLOT_SIZE) / 2;
        font.draw(batch, CRAFTING_LABEL,
                containerX() + font.centeredX(CRAFTING_LABEL, craftingCenter),
                labelTop(InventoryLayout.CRAFTING_LABEL_Y));

        int storageCenter = (2 * InventoryLayout.GRID_X
                + (InventoryLayout.GRID_COLUMNS - 1) * InventoryLayout.SLOT_PITCH
                + InventoryLayout.SLOT_SIZE) / 2;
        font.draw(batch, INVENTORY_LABEL,
                containerX() + font.centeredX(INVENTORY_LABEL, storageCenter)+20,
                labelTop(InventoryLayout.INVENTORY_LABEL_Y));
    }

    /** Y coordinate of the top of a label, measured from the top of the container. */
    private int labelTop(int layoutY) {
        return containerY() + InventoryLayout.CONTAINER_HEIGHT - layoutY;
    }

    /** Draws the stack the mouse carries, centred on the mouse. */
    private void drawCursorStack(SpriteBatch batch, float mouseX, float mouseY) {
        if (cursorStack.isEmpty()) {
            return;
        }
        itemRenderer.render(batch, cursorStack,
                mouseX - Constants.ITEM_ICON_SIZE * 0.5f,
                mouseY - Constants.ITEM_ICON_SIZE * 0.5f);
    }

    /**
     * Slot under the mouse.
     *
     * @param mouseX X coordinate of the mouse inside the interface
     * @param mouseY Y coordinate of the mouse inside the interface, from the bottom
     * @return the slot index, or {@code -1} when the mouse is not on a slot
     */
    private int hoveredSlot(float mouseX, float mouseY) {
        return InventoryLayout.slotAt(Math.round(mouseX) - containerX(), toLayoutY(mouseY));
    }

    /** Draws the name of the hovered item next to the mouse. */
    private void drawTooltip(SpriteBatch batch, int slot, float mouseX, float mouseY) {
        if (slot < 0) {
            return;
        }
        ItemStack stack = inventory.get(slot);
        if (stack.isEmpty()) {
            return;
        }
        TextureRegion pixel = textures.whitePixel();
        if (pixel == null) {
            return;
        }

        String name = stack.item().displayName();
        int border = TOOLTIP_BORDER + TOOLTIP_PADDING;
        int boxWidth = Math.round(font.width(name)) + 2 * border;
        int boxHeight = Math.round(font.lineHeight()) + 2 * border;

        // The tooltip sits above and right of the mouse and stays inside the interface.
        int boxX = clamp(Math.round(mouseX) + TOOLTIP_OFFSET, Math.round(viewport.guiWidth()) - boxWidth);
        int boxY = clamp(Math.round(mouseY) + TOOLTIP_OFFSET, Math.round(viewport.guiHeight()) - boxHeight);

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

