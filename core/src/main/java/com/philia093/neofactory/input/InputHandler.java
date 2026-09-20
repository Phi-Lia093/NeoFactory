package com.philia093.neofactory.input;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Vector3;
import com.philia093.neofactory.entity.Player;

/**
 * Translates raw mouse and keyboard state into player actions.
 * <p>
 * The keyboard only produces a movement direction. The mouse is unprojected
 * through the camera, which turns the cursor position into a world position and
 * therefore sets the facing direction of the player.
 * <p>
 * The class extends {@link InputAdapter} because several actions cannot be polled
 * safely: a key that is still held down would toggle fullscreen on every frame,
 * open and close the inventory in the same frame or cycle the hotbar without end.
 * They are therefore collected from events and handed to the game loop through
 * {@link #consumeZoomSteps()}, {@link #consumeFullscreenToggle()},
 * {@link #consumeInventoryToggle()} and {@link #consumeHotbarSelection()}.
 */
public class InputHandler extends InputAdapter {

    /** Movement key for walking towards positive X. */
    private static final int KEY_RIGHT = Input.Keys.D;

    /** Movement key for walking towards negative X. */
    private static final int KEY_LEFT = Input.Keys.A;

    /** Movement key for walking towards positive Y. */
    private static final int KEY_UP = Input.Keys.W;

    /** Movement key for walking towards negative Y. */
    private static final int KEY_DOWN = Input.Keys.S;

    /** Key that opens and closes the pause menu of the world. */
    private static final int KEY_PAUSE = Input.Keys.ESCAPE;

    /** Key that switches between windowed and fullscreen mode. */
    private static final int KEY_FULLSCREEN = Input.Keys.F11;

    /** Key that opens and closes the inventory screen. */
    private static final int KEY_INVENTORY = Input.Keys.E;

    /** First of the nine keys that select a hotbar slot. */
    private static final int KEY_HOTBAR_FIRST = Input.Keys.NUM_1;

    /** Last of the nine keys that select a hotbar slot. */
    private static final int KEY_HOTBAR_LAST = Input.Keys.NUM_9;

    /** Value of {@link #hotbarSelection} while no hotbar key was pressed. */
    private static final int NO_HOTBAR_SELECTION = -1;

    /** First of the keys that address the layer below the feet. */
    private static final int KEY_GROUND_LAYER_LEFT = Input.Keys.SHIFT_LEFT;

    /** Second of the keys that address the layer below the feet. */
    private static final int KEY_GROUND_LAYER_RIGHT = Input.Keys.SHIFT_RIGHT;

    /** Reused vector holding the unprojected mouse position. */
    private final Vector3 cursor = new Vector3();

    /** Mouse wheel notches collected since the last frame, positive means zoom in. */
    private float zoomSteps;

    /** Set when the fullscreen key was pressed, cleared by the consumer. */
    private boolean fullscreenToggle;

    /** Set when the pause key was pressed, cleared by the consumer. */
    private boolean pauseToggle;

    /** Set when the inventory key was pressed, cleared by the consumer. */
    private boolean inventoryToggle;

    /** Hotbar slot selected by a number key, cleared by the consumer. */
    private int hotbarSelection = NO_HOTBAR_SELECTION;

    /**
     * Reads the current input state and applies it to the player.
     *
     * @param player player receiving the movement and looking direction
     * @param camera camera used to unproject the mouse position
     */
    public void update(Player player, OrthographicCamera camera) {
        player.setMoveInput(readAxis(KEY_LEFT, KEY_RIGHT), readAxis(KEY_DOWN, KEY_UP));
        player.lookAt(mouseWorldX(camera), mouseWorldY(camera));
    }

    @Override
    public boolean scrolled(float amountX, float amountY) {
        // Browsers and touchpads report fractional values, only the direction
        // matters because the zoom is applied in fixed multiplicative steps.
        if (amountY > 0.0f) {
            zoomSteps += 1.0f;
        } else if (amountY < 0.0f) {
            zoomSteps -= 1.0f;
        }
        return false;
    }

    @Override
    public boolean keyDown(int keycode) {
        if (keycode == KEY_FULLSCREEN) {
            fullscreenToggle = true;
            return true;
        }
        if (keycode == KEY_PAUSE) {
            pauseToggle = true;
            return true;
        }
        if (keycode == KEY_INVENTORY) {
            inventoryToggle = true;
            return true;
        }
        if (keycode >= KEY_HOTBAR_FIRST && keycode <= KEY_HOTBAR_LAST) {
            hotbarSelection = keycode - KEY_HOTBAR_FIRST;
            return true;
        }
        return false;
    }

    /**
     * Returns the wheel notches collected since the last call and resets them.
     *
     * @return positive when the wheel was rolled forwards, negative otherwise
     */
    public float consumeZoomSteps() {
        float steps = zoomSteps;
        zoomSteps = 0.0f;
        return steps;
    }

    /**
     * Returns and clears the fullscreen request, if the key was pressed.
     *
     * @return {@code true} when the window mode should be switched
     */
    public boolean consumeFullscreenToggle() {
        boolean requested = fullscreenToggle;
        fullscreenToggle = false;
        return requested;
    }

    /**
     * Returns and clears the inventory request, if the key was pressed.
     *
     * @return {@code true} when the inventory screen should be toggled
     */
    public boolean consumeInventoryToggle() {
        boolean requested = inventoryToggle;
        inventoryToggle = false;
        return requested;
    }

    /**
     * Returns and clears the hotbar slot selected by a number key.
     *
     * @return the slot, {@code 0} to {@code 8}, or {@code -1} when no key was pressed
     */
    public int consumeHotbarSelection() {
        int selection = hotbarSelection;
        hotbarSelection = NO_HOTBAR_SELECTION;
        return selection;
    }

    /**
     * Returns and clears the pause request, if the key was pressed.
     *
     * @return {@code true} when the pause menu should be toggled
     */
    public boolean consumePauseToggle() {
        boolean requested = pauseToggle;
        pauseToggle = false;
        return requested;
    }

    /**
     * {@code true} while the player asks for the layer below the feet.
     * <p>
     * The key is read instead of collected, because it is a modifier and not an
     * action: while it is held, breaking and building apply to the ground the
     * player walks on instead of the layer the player stands in.
     *
     * @return {@code true} while a shift key is held
     */
    public boolean isGroundLayerDown() {
        return Gdx.input.isKeyPressed(KEY_GROUND_LAYER_LEFT)
                || Gdx.input.isKeyPressed(KEY_GROUND_LAYER_RIGHT);
    }

    /**
     * {@code true} while the mouse button that breaks blocks is held.
     * <p>
     * Breaking repeats as long as the button stays down, so the state is read
     * every frame instead of being collected from an event.
     *
     * @return {@code true} while the left button is down
     */
    public boolean isBreakingDown() {
        return Gdx.input.isButtonPressed(Input.Buttons.LEFT);
    }

    /**
     * {@code true} when a mouse button builds the held block.
     *
     * @param button mouse button that was pressed
     * @return {@code true} for the right button
     */
    public static boolean isBuildButton(int button) {
        return button == Input.Buttons.RIGHT;
    }

    /**
     * Unprojects the mouse position and returns its world X coordinate.
     *
     * @param camera camera describing the visible world area
     * @return world X coordinate of the mouse cursor
     */
    public float mouseWorldX(OrthographicCamera camera) {
        updateCursor(camera);
        return cursor.x;
    }

    /**
     * Unprojects the mouse position and returns its world Y coordinate.
     *
     * @param camera camera describing the visible world area
     * @return world Y coordinate of the mouse cursor
     */
    public float mouseWorldY(OrthographicCamera camera) {
        updateCursor(camera);
        return cursor.y;
    }

    /**
     * Reads a single movement axis.
     *
     * @param negative key moving towards negative coordinates
     * @param positive key moving towards positive coordinates
     * @return {@code -1}, {@code 0} or {@code 1}
     */
    private float readAxis(int negative, int positive) {
        float axis = 0.0f;
        if (Gdx.input.isKeyPressed(negative)) {
            axis -= 1.0f;
        }
        if (Gdx.input.isKeyPressed(positive)) {
            axis += 1.0f;
        }
        return axis;
    }

    /**
     * Unprojects the current mouse position into world space.
     * <p>
     * The camera uses a viewport that maps screen pixels to world units, so the
     * unprojection has to use the screen size of that viewport.
     *
     * @param camera camera describing the visible world area
     */
    private void updateCursor(OrthographicCamera camera) {
        cursor.set(Gdx.input.getX(), Gdx.input.getY(), 0.0f);
        camera.unproject(cursor);
    }
}
