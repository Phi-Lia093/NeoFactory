package com.philia093.neofactory.util.nbt;

/**
 * Thrown when stored tag data cannot be read or written.
 * <p>
 * The exception is unchecked on purpose: a damaged save game is handled where it
 * is opened, which is one place, and the callers in between have nothing useful to
 * add. It always carries the file or the tag that failed in its message.
 */
public class NbtException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates an exception with a message.
     *
     * @param message description of the problem
     */
    public NbtException(String message) {
        super(message);
    }

    /**
     * Creates an exception with a message and a cause.
     *
     * @param message description of the problem
     * @param cause exception that caused the problem
     */
    public NbtException(String message, Throwable cause) {
        super(message, cause);
    }
}
