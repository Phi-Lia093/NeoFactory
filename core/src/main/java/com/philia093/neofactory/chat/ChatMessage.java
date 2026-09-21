package com.philia093.neofactory.chat;

/**
 * One line of the chat.
 * <p>
 * A message is only the text and where it comes from. How long it stays on screen
 * and which of the lines are drawn is decided by {@link ChatLog}, and the colour
 * follows from {@link Kind}, so a message itself is read only.
 *
 * @param text text of the line, empty when it holds nothing
 * @param kind where the line comes from
 */
public record ChatMessage(String text, Kind kind) {

    /** Where a line comes from, which decides the colour it is drawn with. */
    public enum Kind {

        /** Something the player typed. */
        PLAYER,

        /** A note of the game itself, for example the answer of a command. */
        SYSTEM,

        /** A command that could not do what it was asked to. */
        ERROR
    }

    /**
     * Creates a message.
     *
     * @param text text of the line, {@code null} becomes empty
     * @param kind source of the line, {@code null} becomes {@link Kind#SYSTEM}
     */
    public ChatMessage {
        text = text == null ? "" : text;
        kind = kind == null ? Kind.SYSTEM : kind;
    }

    /**
     * Creates a line the player typed.
     *
     * @param text text of the line
     * @return the message
     */
    public static ChatMessage player(String text) {
        return new ChatMessage(text, Kind.PLAYER);
    }

    /**
     * Creates a line of the game itself.
     *
     * @param text text of the line
     * @return the message
     */
    public static ChatMessage system(String text) {
        return new ChatMessage(text, Kind.SYSTEM);
    }

    /**
     * Creates a line that reports a failure.
     *
     * @param text text of the line
     * @return the message
     */
    public static ChatMessage error(String text) {
        return new ChatMessage(text, Kind.ERROR);
    }

    @Override
    public String toString() {
        return kind + ": " + text;
    }
}
