package com.philia093.neofactory.gui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.philia093.neofactory.chat.ChatController;
import com.philia093.neofactory.chat.ChatMessage;
import com.philia093.neofactory.chat.ChatText;
import com.philia093.neofactory.render.BlockTextureCache;
import com.philia093.neofactory.render.PixelFont;

import java.util.List;

/**
 * Draws the chat: the input line in the corner and the messages above it.
 * <p>
 * The line sits on the lower left edge, right above the hotbar, and reaches half
 * across the interface at most. It is a light frame on a dark plate, and the text
 * inside is cut at the front when it grows longer than the box: a player who types
 * a long command sees the end of it, where the cursor is, and not the beginning
 * that is already written down.
 * <p>
 * The messages and the input line share one plate, so the chat is a single block
 * and not a stack of boxes that do not line up: the line is the lowest row, the
 * newest message sits right above it and older ones above that one, at most
 * {@link com.philia093.neofactory.chat.ChatLog#VISIBLE_LINES} of them. A message
 * that is longer than the block is cut at its end, the block never grows with it.
 * <p>
 * All coordinates are virtual pixels of {@link GuiViewport}.
 */
public class ChatOverlay {

    /** Distance between the interface edge and a box. */
    private static final float MARGIN = 2.0f;

    /** Distance between the frame of a box and its text. */
    private static final float PADDING = 4.0f;

    /** Height of one row of a message, the box its text is centred in. */
    private static final float ROW_HEIGHT = 11.0f;

    /** Height of the input line, its frame included. */
    private static final float INPUT_HEIGHT = 12.0f;

    /** Thickness of the frame around the input line. */
    private static final float FRAME = 1.0f;

    /** Smallest width of the input line. */
    private static final float MIN_WIDTH = 120.0f;

    /** Seconds the cursor stays visible, and hidden, before it blinks again. */
    private static final float BLINK_INTERVAL = 0.5f;

    private static final Color BACKGROUND = new Color(0.0f, 0.0f, 0.0f, 0.5f);
    private static final Color FRAME_COLOR = new Color(1.0f, 1.0f, 1.0f, 0.35f);
    private static final Color INPUT_COLOR = new Color(1.0f, 1.0f, 1.0f, 1.0f);
    private static final Color CURSOR_COLOR = new Color(0.9f, 0.9f, 0.9f, 1.0f);
    private static final Color SYSTEM_COLOR = new Color(0.85f, 0.85f, 0.85f, 1.0f);
    private static final Color ERROR_COLOR = new Color(1.0f, 0.55f, 0.5f, 1.0f);

    private final BlockTextureCache textures;
    private final PixelFont font;

    /** Seconds since the chat was drawn last, drives the blinking cursor. */
    private float elapsed;

    /**
     * Creates the overlay.
     *
     * @param textures texture cache providing the plain pixel and the frame
     * @param font font the messages are drawn with
     */
    public ChatOverlay(BlockTextureCache textures, PixelFont font) {
        this.textures = textures;
        this.font = font;
    }

    /**
     * Counts the time for the blinking cursor.
     *
     * @param delta time since the last frame in seconds
     */
    public void update(float delta) {
        elapsed += delta;
    }

    /**
     * Draws the messages and, while the player types, the input line.
     *
     * @param batch batch switched to the projection of the interface viewport
     * @param chat chat holding the messages and the line
     * @param viewport viewport of the interface, the source of its width
     */
    public void render(SpriteBatch batch, ChatController chat, GuiViewport viewport) {
        List<ChatMessage> messages = chat.visibleLines();
        boolean typing = chat.isOpen();
        if (messages.isEmpty() && !typing) {
            return;
        }
        float width = blockWidth(chat, messages, viewport);
        // One plate carries the whole chat: the rows of the history and the input
        // line form a single block instead of a stack of loose boxes that do not
        // line up with each other.
        float bottom = bottom();
        drawBackdrop(batch, MARGIN, bottom, width, heightOf(messages.size(), typing));

        // The input line sits at the bottom, the newest message right above it and
        // the older ones above that one.
        float row = bottom;
        if (typing) {
            drawOutline(batch, MARGIN, row, width, INPUT_HEIGHT);
            drawInputLine(batch, chat, width, row);
            row += INPUT_HEIGHT;
        }
        drawMessages(batch, messages, width, row);
        batch.setColor(Color.WHITE);
    }

    /**
     * Height the chat needs.
     *
     * @param messages amount of messages that are drawn
     * @param typing {@code true} when the input line is part of the block
     * @return the height in virtual pixels
     */
    private static float heightOf(int messages, boolean typing) {
        return (typing ? INPUT_HEIGHT : 0.0f) + messages * ROW_HEIGHT;
    }

    /**
     * Width of the input line, half of the interface but never tiny.
     *
     * @param viewport viewport of the interface
     * @return the width in virtual pixels
     */
    private static float inputWidth(GuiViewport viewport) {
        return Math.max(MIN_WIDTH, viewport.guiWidth() * 0.5f);
    }

    /**
     * Width the block of the chat needs.
     * <p>
     * The widest row decides, so a short message does not sit on a long empty
     * plate; the input line never grows past half of the interface, so a long line
     * is cut instead of covering the world.
     *
     * @param chat chat holding the input line
     * @param messages messages that are drawn
     * @param viewport viewport of the interface
     * @return the width in virtual pixels
     */
    private float blockWidth(ChatController chat, List<ChatMessage> messages, GuiViewport viewport) {
        float needed = MIN_WIDTH;
        for (ChatMessage message : messages) {
            needed = Math.max(needed, font.width(message.text()) + 2.0f * PADDING);
        }
        if (chat.isOpen()) {
            // The cursor stands behind the text and has to fit into the plate.
            needed = Math.max(needed, font.width(chat.text()) + 2.0f * PADDING + font.scale());
        }
        return Math.min(inputWidth(viewport), needed);
    }

    /** Y coordinate the chat grows from, right above the hotbar. */
    private static float bottom() {
        return InventoryLayout.HOTBAR_HEIGHT + MARGIN;
    }

    /** Draws the messages, the newest one at the bottom. */
    private void drawMessages(SpriteBatch batch, List<ChatMessage> messages, float width, float firstRow) {
        float available = width - 2.0f * PADDING;
        float row = firstRow;
        for (int index = messages.size() - 1; index >= 0; index--) {
            ChatMessage message = messages.get(index);
            // A row that is longer than the chat is cut at its end, the plate never
            // grows with a message.
            String text = ChatText.head(message.text(), available, font::width);
            font.setColor(colorOf(message.kind()));
            font.drawShadowed(batch, text, MARGIN + PADDING,
                    row + (ROW_HEIGHT + font.glyphHeight()) * 0.5f);
            font.setColor(Color.WHITE);
            row += ROW_HEIGHT;
        }
    }

    /** Draws the line the player types into, with the cursor behind the text. */
    private void drawInputLine(SpriteBatch batch, ChatController chat, float width, float row) {
        // The text is cut at the front: the end of it is where the cursor sits, so
        // that is what the player has to see. When the box is full to the brim not a
        // single character is drawn and the cursor stays at its edge.
        float available = width - 2.0f * PADDING - font.scale();
        String visible = ChatText.tail(chat.text(), available, font::width);
        float textX = MARGIN + PADDING;
        float textTop = row + (INPUT_HEIGHT + font.glyphHeight()) * 0.5f;
        font.setColor(INPUT_COLOR);
        font.drawShadowed(batch, visible, textX, textTop);
        drawCursor(batch, textX + font.width(visible), textTop);
        font.setColor(Color.WHITE);
    }

    /** Draws the blinking cursor behind the visible text. */
    private void drawCursor(SpriteBatch batch, float x, float textTop) {
        TextureRegion pixel = textures.whitePixel();
        if (pixel == null || elapsed % (BLINK_INTERVAL * 2.0f) >= BLINK_INTERVAL) {
            return;
        }
        batch.setColor(CURSOR_COLOR);
        batch.draw(pixel, x, textTop - font.glyphHeight(), font.scale(), font.glyphHeight());
        batch.setColor(Color.WHITE);
    }

    /** Draws the dark plate the whole chat sits on. */
    private void drawBackdrop(SpriteBatch batch, float x, float y, float width, float height) {
        TextureRegion pixel = textures.whitePixel();
        if (pixel == null) {
            return;
        }
        batch.setColor(BACKGROUND);
        batch.draw(pixel, x, y, width, height);
        batch.setColor(Color.WHITE);
    }

    /** Draws the light frame of the input line, the plate shows through inside it. */
    private void drawOutline(SpriteBatch batch, float x, float y, float width, float height) {
        TextureRegion pixel = textures.whitePixel();
        if (pixel == null) {
            return;
        }
        batch.setColor(FRAME_COLOR);
        batch.draw(pixel, x, y, width, FRAME);
        batch.draw(pixel, x, y + height - FRAME, width, FRAME);
        batch.draw(pixel, x, y + FRAME, FRAME, height - 2.0f * FRAME);
        batch.draw(pixel, x + width - FRAME, y + FRAME, FRAME, height - 2.0f * FRAME);
        batch.setColor(Color.WHITE);
    }

    /** Colour a message is drawn with. */
    private static Color colorOf(ChatMessage.Kind kind) {
        if (kind == ChatMessage.Kind.ERROR) {
            return ERROR_COLOR;
        }
        if (kind == ChatMessage.Kind.SYSTEM) {
            return SYSTEM_COLOR;
        }
        return INPUT_COLOR;
    }
}
