package com.philia093.neofactory.gui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.philia093.neofactory.gui.container.ContainerLayout;
import com.philia093.neofactory.gui.container.ContainerMenu;
import com.philia093.neofactory.gui.container.ContainerView;
import com.philia093.neofactory.gui.panel.ArrowElement;
import com.philia093.neofactory.item.Inventory;
import com.philia093.neofactory.item.ItemStack;
import com.philia093.neofactory.item.PlayerInventory;
import com.philia093.neofactory.recipe.InventoryGrid;
import com.philia093.neofactory.recipe.Recipe;
import com.philia093.neofactory.recipe.RecipeGrid;
import com.philia093.neofactory.recipe.RecipeRegistry;
import com.philia093.neofactory.recipe.RecipeType;
import com.philia093.neofactory.render.BlockTextureCache;
import com.philia093.neofactory.render.PixelFont;

/**
 * The inventory screen of the player.
 * <p>
 * It shows what the player owns and what may be made of it: a two by two crafting field
 * with the result of the matching recipe, the three storage rows and the hotbar. The
 * panel, the slots and the arrow come from {@code gui/inventory_icons.png} and are
 * stretched to whatever size the slots ask for, see
 * {@link com.philia093.neofactory.gui.panel.PanelTextures}.
 * <p>
 * The screen is opened and closed with the inventory key of
 * {@link com.philia093.neofactory.input.InputHandler}. All coordinates are virtual pixels
 * of {@link GuiViewport}, which is also what turns the position of the mouse into them,
 * so the drawn slots and the slots a click can hit are always the same and stay so in
 * fullscreen or after a resize.
 * <p>
 * Clicking a slot moves stacks around the way the original game does, which is done by
 * {@link ContainerMenu}: the left button takes or drops a whole stack, the right button
 * takes half a stack or drops a single item, and holding shift moves a stack between the
 * crafting field and the player. A click beside the panel, or a drag that ends there,
 * throws the carried stack into the world, see {@link ContainerMenu#dropCursor()}, and a
 * click on the empty space inside the panel puts it back into the inventory.
 * <p>
 * While the screen is open the world keeps running and the player keeps standing: the
 * interface owns the input, so a click never digs or builds, but a drop next to the player
 * is still picked up, which is what lets a full inventory take an item once one was lifted
 * onto the mouse, see
 * {@link com.philia093.neofactory.screen.GameScreen#handleInputAndUpdate(float)}.
 * <p>
 * Which recipe the field makes comes from {@code assets/recipes}, see
 * {@link com.philia093.neofactory.recipe.RecipeLoader}. Taking a result gives up the
 * ingredients it was made of, which is why the screen watches the container instead of
 * the container knowing about recipes.
 */
public final class InventoryGui {

    /** Amount of slots of the two by two crafting field. */
    private static final int CRAFT_SLOTS = 4;

    private final GuiViewport viewport;

    /** Two by two field the player crafts in, its contents are shown by the screen. */
    private final Inventory crafting = new Inventory(CRAFT_SLOTS);

    /** Holds the result of the recipe that matches the crafting field. */
    private final Inventory result = new Inventory(1);

    /** View of the crafting field, the shape a recipe is offered. */
    private final InventoryGrid craftingGrid = new InventoryGrid(crafting, 0, 2, 2);

    private final ContainerMenu menu;
    private final ContainerView view;

    /** Recipe that matches the field right now, {@code null} when nothing matches. */
    private Recipe recipe;

    /**
     * Creates the screen.
     *
     * @param textures texture cache providing the panel and the item icons
     * @param font font used for the amounts and for the name of an item
     * @param inventory inventory of the player, shown by the screen
     * @param viewport viewport of the interface, see {@link GuiViewport}
     */
    public InventoryGui(BlockTextureCache textures, PixelFont font, PlayerInventory inventory,
            GuiViewport viewport) {
        this.viewport = viewport;
        ContainerLayout layout = InventoryLayout.of(inventory, crafting, result);
        this.menu = new ContainerMenu(layout, inventory);
        this.view = new ContainerView(textures, font, viewport);
        // Every click that changes the field asks the recipes again, and taking a result
        // gives up the ingredients it was made of.
        menu.setChangeListener(container -> refreshResult());
        menu.setResultFiller(slot -> {
            consumeIngredients();
            return result.get(0);
        });
        refreshResult();
    }

    /** {@code true} while the screen covers the world. */
    public boolean isOpen() {
        return menu.isOpen();
    }

    /** Opens the screen when it is closed and closes it otherwise. */
    public void toggle() {
        menu.toggle();
    }

    /**
     * Closes the screen.
     * <p>
     * A stack the mouse carries is put back into the inventory, so a player never loses
     * items by closing the screen. Whatever is left in the crafting field is dropped into
     * the world, see {@link #setDropper(ContainerMenu.StackDropper)}: the field is a place
     * to work, not a place to store.
     */
    public void close() {
        menu.close();
    }

    /**
     * Sets the sink the crafting field is dropped into when the screen closes.
     *
     * @param dropper sink to use, {@code null} to hand the items back to the player
     */
    public void setDropper(ContainerMenu.StackDropper dropper) {
        menu.setDropper(dropper);
    }

    /**
     * Drops what the mouse points at, the action of the drop key while the screen is open.
     *
     * @param guiX X coordinate of the mouse inside the interface
     * @param guiY Y coordinate of the mouse inside the interface, from the bottom
     * @param wholeStack {@code true} to drop the whole stack, {@code false} for one item
     * @return {@code true} when something was dropped
     */
    public boolean dropAt(float guiX, float guiY, boolean wholeStack) {
        if (!menu.isOpen()) {
            return false;
        }
        if (!isOnPanel(guiX, guiY)) {
            // Beside the panel the drop key works on whatever the mouse carries.
            return menu.dropCursor();
        }
        return menu.dropFrom(menu.slotAt(localX(guiX), localY(guiY)), wholeStack);
    }

    /** Stack the mouse currently carries. */
    public ItemStack cursorStack() {
        return menu.cursorStack();
    }

    /**
     * Handles a mouse button press.
     * <p>
     * The coordinates are virtual pixels of the interface, the very same ones the screen
     * is drawn in, so a click always lands on the slot the player sees. A press beside the
     * panel throws the carried stack away instead of opening anything.
     *
     * @param guiX X coordinate of the mouse inside the interface
     * @param guiY Y coordinate of the mouse inside the interface, from the bottom
     * @param button mouse button that was pressed
     * @return {@code true} when the press was consumed
     */
    public boolean touchDown(float guiX, float guiY, int button) {
        if (!menu.isOpen()) {
            return false;
        }
        if (!isOnPanel(guiX, guiY)) {
            // A click beside the panel throws the carried stack into the world, so a player
            // gets rid of a stack without closing the screen first.
            menu.dropCursor();
            return true;
        }
        menu.touchDown(localX(guiX), localY(guiY), button, isShiftHeld());
        return true;
    }

    /**
     * Handles the mouse moving while a button is held.
     * <p>
     * Every slot the mouse reaches gets one item of the carried stack, and what is left
     * over when the button is released is shared out over the same slots, so a stack can
     * be spread over a row of slots in one move.
     *
     * @param guiX X coordinate of the mouse inside the interface
     * @param guiY Y coordinate of the mouse inside the interface, from the bottom
     */
    public void touchDragged(float guiX, float guiY) {
        if (menu.isOpen()) {
            menu.touchDragged(localX(guiX), localY(guiY));
        }
    }

    /**
     * Handles the release of a mouse button.
     * <p>
     * Beside the panel the release drops what a drag left on the mouse, which is the way
     * a player throws away a stack without closing the screen.
     *
     * @param guiX X coordinate of the mouse inside the interface
     * @param guiY Y coordinate of the mouse inside the interface, from the bottom
     * @param button mouse button that was released
     * @return {@code true} when the release was consumed
     */
    public boolean touchUp(float guiX, float guiY, int button) {
        if (!menu.isOpen()) {
            return false;
        }
        if (!isOnPanel(guiX, guiY)) {
            // Letting go beside the panel drops what the mouse still carries, a drag that
            // ended outside included.
            return menu.dropDragRemainder();
        }
        return menu.touchUp(localX(guiX), localY(guiY), button, isShiftHeld());
    }

    /** {@code true} when a point of the interface lies on the panel of the container. */
    private boolean isOnPanel(float guiX, float guiY) {
        return menu.layout().contains(localX(guiX), localY(guiY));
    }

    /**
     * Draws the panel, its slots, the items in them and the crafting arrow.
     *
     * @param batch batch switched to the projection of the interface viewport
     * @param mouseX X coordinate of the mouse inside the interface
     * @param mouseY Y coordinate of the mouse inside the interface, from the bottom
     */
    public void render(SpriteBatch batch, float mouseX, float mouseY) {
        if (!menu.isOpen()) {
            return;
        }
        // The arrow is a decoration of the container: it is drawn together with the slots
        // and below the items, so it never covers the name of an item.
        view.render(batch, menu, panelX(), panelY(), mouseX, mouseY, this::drawCraftingArrow);
    }

    /** X coordinate of the left edge of the panel, centred in the interface. */
    public float panelX() {
        return MenuLayout.centeredX(viewport.guiWidth(), menu.layout().panelWidth());
    }

    /** Y coordinate of the lower edge of the panel, centred in the interface. */
    public float panelY() {
        return Math.round((viewport.guiHeight() - menu.layout().panelHeight()) * 0.5f);
    }

    /**
     * Draws the arrow that points from the crafting field to its result.
     * <p>
     * The arrow stays empty: it shows where a result appears and not how far a craft has
     * come. The bright part of the picture is kept for a machine, which fills it with the
     * progress of its work, see {@link com.philia093.neofactory.machine.ProgressMachine}.
     */
    private void drawCraftingArrow(SpriteBatch batch, float panelX, float panelY, int panelWidth,
            int panelHeight) {
        view.arrows().draw(batch, panelX + InventoryLayout.ARROW_X,
                panelY + panelHeight - InventoryLayout.ARROW_Y - ArrowElement.HEIGHT, 0.0f);
    }

    /** Looks for the recipe that matches the crafting field and shows its result. */
    private void refreshResult() {
        recipe = findRecipe(craftingGrid);
        result.set(0, recipe == null ? ItemStack.EMPTY : recipe.result());
    }

    /** Gives up the ingredients of the shown recipe and asks for the next result. */
    private void consumeIngredients() {
        if (recipe != null) {
            recipe.consume(craftingGrid);
        }
        refreshResult();
    }

    /**
     * The recipe a field matches, the shaped ones first.
     *
     * @param grid items the player put into the field
     * @return the recipe, or {@code null} when nothing can be made of them
     */
    private static Recipe findRecipe(RecipeGrid grid) {
        Recipe shaped = RecipeRegistry.find(RecipeType.CRAFTING_SHAPED, grid);
        return shaped != null ? shaped : RecipeRegistry.find(RecipeType.CRAFTING_SHAPELESS, grid);
    }

    /** X coordinate of the mouse inside the panel. */
    private int localX(float guiX) {
        return Math.round(guiX) - Math.round(panelX());
    }

    /** Y coordinate of the mouse inside the panel, measured from its upper edge. */
    private int localY(float guiY) {
        return Math.round(panelY() + menu.layout().panelHeight() - guiY);
    }

    /** {@code true} while a key that moves a stack to the other side is held. */
    private static boolean isShiftHeld() {
        return Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT)
                || Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT);
    }
}
