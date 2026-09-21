package com.philia093.neofactory.chat;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * The list of chat lines the game keeps and the few of them that are on screen.
 * <p>
 * Two limits shape what the player sees: a message is only drawn while the input
 * line is closed and it is younger than {@link #MESSAGE_SECONDS}, and only the last
 * {@link #VISIBLE_LINES} messages are drawn at once. Older messages are kept until
 * the list reaches {@link #MAX_LINES}, so opening the input line shows the recent
 * conversation again - the same behaviour the original game has.
 * <p>
 * The class holds no pictures and no colours, it only counts time and cuts the
 * list, which makes it testable without a window.
 */
public final class ChatLog {

    /** Amount of lines the log keeps before the oldest one is forgotten. */
    public static final int MAX_LINES = 100;

    /** Amount of lines that are drawn at once. */
    public static final int VISIBLE_LINES = 8;

    /** Seconds a line stays on screen after the input line was closed. */
    public static final float MESSAGE_SECONDS = 10.0f;

    private final List<Entry> entries = new ArrayList<>();

    /** Adds a message, forgetting the oldest line when the log is full. */
    public void add(ChatMessage message) {
        entries.add(new Entry(Objects.requireNonNull(message, "message")));
        while (entries.size() > MAX_LINES) {
            entries.remove(0);
        }
    }

    /**
     * Adds a line the player typed.
     *
     * @param text text of the line
     */
    public void addPlayer(String text) {
        add(ChatMessage.player(text));
    }

    /**
     * Adds a line of the game itself.
     *
     * @param text text of the line
     */
    public void addSystem(String text) {
        add(ChatMessage.system(text));
    }

    /**
     * Adds a line that reports a failure.
     *
     * @param text text of the line
     */
    public void addError(String text) {
        add(ChatMessage.error(text));
    }

    /**
     * Ages the lines, which is what makes them leave the screen.
     *
     * @param delta time since the last frame in seconds
     */
    public void update(float delta) {
        if (delta <= 0.0f) {
            return;
        }
        for (Entry entry : entries) {
            entry.age += delta;
        }
    }

    /**
     * Lines to draw, oldest first.
     *
     * @param includeExpired {@code true} to show the whole list, which the open input
     *                       line does, {@code false} to draw only fresh lines
     * @return at most {@link #VISIBLE_LINES} messages
     */
    public List<ChatMessage> visibleLines(boolean includeExpired) {
        List<ChatMessage> visible = new ArrayList<>(VISIBLE_LINES);
        for (int index = entries.size() - 1; index >= 0 && visible.size() < VISIBLE_LINES; index--) {
            Entry entry = entries.get(index);
            if (includeExpired || entry.age < MESSAGE_SECONDS) {
                visible.add(0, entry.message);
            }
        }
        return visible;
    }

    /** Amount of lines the log currently holds. */
    public int size() {
        return entries.size();
    }

    /** {@code true} when no line is left. */
    public boolean isEmpty() {
        return entries.isEmpty();
    }

    /** Drops every line. */
    public void clear() {
        entries.clear();
    }

    @Override
    public String toString() {
        return "ChatLog(" + entries.size() + " lines)";
    }

    /** A message together with the time it has been shown for. */
    private static final class Entry {

        private final ChatMessage message;

        /** Seconds the message has been shown. */
        private float age;

        private Entry(ChatMessage message) {
            this.message = message;
        }
    }
}
